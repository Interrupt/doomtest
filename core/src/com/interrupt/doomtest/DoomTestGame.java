package com.interrupt.doomtest;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.interrupt.doomtest.input.EditorCameraController;


public class DoomTestGame extends ApplicationAdapter {

    ModelBatch batch;
    Camera camera;
    EditorCameraController camController;
    ShapeRenderer lineRenderer;

    public Array<ModelInstance> models = new Array<ModelInstance>();
    public Array<Sector> sectors;
    public Array<Line> lines;
    public Array<Vector2> vertices = new Array<Vector2>();

    public Sector current;
    Plane editPlane = new Plane(Vector3.Y, Vector3.Zero);

    Vector3 tempVec3 = new Vector3();
    Vector3 tempVec3_2 = new Vector3();

    Vector3 lastIntersection = new Vector3();
    Vector3 editPlaneIntersection = new Vector3();

    Vector3 intersection = new Vector3();
    Vector3 pickedGridPoint = new Vector3();
    Vector2 pickedPoint2d = new Vector2();

    Float editHeight = null;

    Sector hoveredSector = null;
    Sector pickedSector = null;

    Vector2 hoveredPoint = null;
    Vector2 pickedPoint = null;

    Line hoveredLine = null;
    Line pickedLine = null;

    Vector2 lastMousePoint = new Vector2();
    public float startHeightModeFloorHeight = 0;
    public float startHeightModeCeilHeight = 0;

    boolean wasDragging = false;

    Color wireframeColor = new Color(Color.DARK_GRAY.r, Color.DARK_GRAY.g, Color.DARK_GRAY.b, 0.2f);

    public enum EditorModes { SECTOR, POINT, SPLIT };

    public EditorModes editorMode = EditorModes.SECTOR;

    public Vector2 splitStart = null;
    public Vector2 splitEnd = null;

    Array<Material> selectedMaterials = new Array<Material>();

	@Override
	public void create () {
        batch = new ModelBatch();

        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.up.set(Vector3.Y);
        camera.position.set(0f, 20f, -5f);

        Vector3 tmpV1 = new Vector3(camera.direction).crs(camera.up).nor();
        camera.direction.rotate(tmpV1, -70f);

        //camera.lookAt(0,0,-5f);
        camera.near = 0.1f;
        camera.far = 300f;
        camera.update();

        // A camera that can be driven around
        camController = new EditorCameraController(camera);
        Gdx.input.setInputProcessor(camController);

        lineRenderer = new ShapeRenderer();

        // The level, a list of sectors and lines
        sectors = new Array<Sector>();
        lines = new Array<Line>();
	}

    public void refreshSectors() {
        models.clear();

        for(Sector sector : sectors) {
            // turn the sector into a model
            models.addAll(sector.tesselate());
        }

        // walls!
        models.add(new ModelInstance(WallTesselator.tesselate(lines)));
    }

    public Sector pickSector(Vector2 point) {
        for(Sector s : sectors) {
            if(s.isPointInside(point)) {
                return s.getSectorOfPoint(point);
            }
        }
        return null;
    }

    public Array<Sector> collectAllSectorsIn(Sector sector, Array<Sector> collected) {
        collected.add(sector);
        for(Sector s : sector.subsectors ) {
            collectAllSectorsIn(s, collected);
        }
        return collected;
    }

    Vector3 temp_int = new Vector3();
    public Sector intersectSectors(Ray r, Vector3 closest) {
        // Get all sectors into one list
        Array<Sector> allSectors = new Array<Sector>();
        for(Sector s : sectors) {
            collectAllSectorsIn(s, allSectors);
        }

        Sector closestSector = null;
        float t_dist = 10000;

        for(Sector s : allSectors) {
            Plane plane = new Plane(Vector3.Y, new Vector3(0, s.floorHeight, 0));
            if(Intersector.intersectRayPlane(r, plane, temp_int)) {
                if(plane.isFrontFacing(camera.direction)) {
                    Vector2 t_point = new Vector2(temp_int.x, temp_int.z);
                    float dist = temp_int.dst(camera.position);

                    if (s.isPointInside(t_point) && dist < t_dist) {
                        if(s.getSectorOfPoint(t_point) == s) {
                            t_dist = dist;
                            closest.set(temp_int);
                            closestSector = s;
                        }
                    }
                }
            }
        }

        return closestSector;
    }

    public Line intersectWalls(Ray ray, Vector3 closest) {
        float t_dist = 10000;
        Line closestLine = null;

        for(Line l : lines) {
            Vector3 p1 = new Vector3(l.start.x, 0, l.start.y);
            Vector3 p2 = new Vector3(l.end.x, 0, l.end.y);
            Vector3 p3 = new Vector3(l.end.x, 1, l.end.y);
            Plane plane = new Plane(p1, p2, p3);

            Vector3 endPoint = ray.getEndPoint(new Vector3(), 1000);
            Vector2 startPoint2d = new Vector2(ray.origin.x, ray.origin.z);
            Vector2 endPoint2d = new Vector2(endPoint.x, endPoint.z);

            if(l.findIntersection(startPoint2d, endPoint2d) != null && Intersector.intersectRayPlane(ray, plane, temp_int)) {

                boolean solidHit = l.solid &&
                        !plane.isFrontFacing(camera.direction) &&
                        l.left.floorHeight < temp_int.y &&
                        l.left.ceilHeight > temp_int.y;

                boolean nonSolidLowerHit = false;

                if(l.right != null && !l.solid) {
                    boolean nonSolidLowerFrontFacing = (l.left.floorHeight > l.right.floorHeight && plane.isFrontFacing(camera.direction))
                            || (l.left.floorHeight < l.right.floorHeight && !plane.isFrontFacing(camera.direction));

                    nonSolidLowerHit = nonSolidLowerFrontFacing &&
                            (l.left.floorHeight > l.right.floorHeight && (l.left.floorHeight > temp_int.y && l.right.floorHeight < temp_int.y)) ||
                            (l.left.floorHeight < l.right.floorHeight && (l.left.floorHeight < temp_int.y && l.right.floorHeight > temp_int.y));
                }

                if((solidHit || nonSolidLowerHit)) {

                    float dist = temp_int.dst(camera.position);
                    if(dist < t_dist) {
                        t_dist = dist;
                        closest.set(temp_int);
                        closestLine = l;
                    }
                }
            }
        }

        return closestLine;
    }

    public void refreshLineSolidity(Sector sector) {
        for(Line line : lines) {
            if(line.left == sector || line.right == sector) {
                if (line.right == null) {
                    line.solid = !sector.isSolid;
                }
                else {
                    if(line.left.isSolid && !line.right.isSolid) line.solid = true;
                    else line.solid = line.right.isSolid && !line.left.isSolid;
                }
            }
        }
    }

    public void update() {
        Ray r = camera.getPickRay(Gdx.input.getX(), Gdx.input.getY());

        if(lastIntersection != null)
            lastIntersection.set(editPlaneIntersection);

        boolean intersectsWorld = intersectsWorld(r, intersection);
        boolean intersectsEditPlane = Intersector.intersectRayPlane(r, editPlane, editPlaneIntersection);

        if(intersectsWorld || intersectsEditPlane) {

            if(!intersectsWorld) intersection.set(editPlaneIntersection);
            if(lastIntersection == null) lastIntersection = new Vector3(editPlaneIntersection);

            // round grid point to the grid
            pickedGridPoint.set((int) intersection.x, intersection.y, (int) intersection.z);
            pickedPoint2d.set(intersection.x, intersection.z);

            if(editHeight != null) {
                pickedGridPoint.y = editHeight;
            }

            // which editing mode?
            if (editorMode == EditorModes.SECTOR) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.DEL)) {
                    //Vector2 next = new Vector2(intersection.x, intersection.z);
                    Sector picked = hoveredSector;
                    picked.isSolid = !picked.isSolid;
                    refreshLineSolidity(picked);
                    refreshSectors();
                }

                if(Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                    float mod = 0.1f;
                    if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) mod *= -1f;

                    Sector picked = hoveredSector;
                    picked.floorHeight += mod;
                    refreshSectors();
                }

                if(Gdx.input.isKeyJustPressed(Input.Keys.T)) {
                    float mod = 0.1f;
                    if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) mod *= -1f;

                    Sector picked = hoveredSector;
                    picked.ceilHeight += mod;
                    refreshSectors();
                }

                // snap to nearest line
                Line nearLine = findPickedLine(intersection);
                if(nearLine != null) {
                    Vector2 nearest = new Vector2();
                    Intersector.nearestSegmentPoint(nearLine.start, nearLine.end, new Vector2(intersection.x, intersection.z), nearest);
                    pickedGridPoint.x = nearest.x;
                    pickedGridPoint.z = nearest.y;
                }

                // snap to nearest point
                Vector2 hovering = getVertexNear(intersection.x, intersection.z, 0.25f);
                if(hovering != null) {
                    pickedGridPoint.x = hovering.x;
                    pickedGridPoint.z = hovering.y;
                }

                // Add a new vertex when clicked
                if (Gdx.input.justTouched()) {

                    Vector2 next = new Vector2(pickedGridPoint.x, pickedGridPoint.z);
                    Vector2 existing = getExistingVertex(next);

                    // Start a new sector if not currently editing one
                    if (current == null) {
                        editHeight = intersection.y;
                        editPlane.set(new Vector3(0, editHeight, 0), Vector3.Y);

                        current = new Sector();
                        current.floorHeight = editHeight;
                    }

                    // finish the sector automatically if the line loops
                    if(current.getPoints().size > 0 && next.equals(current.getPoints().first())) {
                        finishSector();
                    }
                    else {
                        current.addVertex(existing != null ? existing : next);
                    }
                }

                // Cancel the current sector
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                    cancelEditingSector();
                }

                // Finish current sector
                if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                    finishSector();
                }
            }
            else if (editorMode == EditorModes.POINT) {
                // Move sectors or points
                if(!Gdx.input.isTouched()) {
                    if(wasDragging) {
                        if(pickedSector != null && pickedSector.parent != null) {
                            refreshSectorParents(pickedSector, pickedSector.parent);
                        }
                        wasDragging = false;
                    }

                    hoveredPoint = getVertexNear(intersection.x, intersection.z, 0.25f);
                    if(hoveredPoint != null) hoveredLine = null;
                }

                if(Gdx.input.justTouched() && Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                    pickedPoint = null;
                    pickedSector = null;
                    pickedLine = null;

                    if(hoveredPoint != null) pickedPoint = hoveredPoint;
                    else if(hoveredLine != null) pickedLine = hoveredLine;
                    else if(hoveredSector != null) pickedSector = hoveredSector;

                    setHighlights();
                    refreshSectors();

                    lastMousePoint.set(Gdx.input.getX(), Gdx.input.getY());
                    if(pickedSector != null) {
                        startHeightModeFloorHeight = pickedSector.floorHeight;
                        startHeightModeCeilHeight = pickedSector.ceilHeight;
                    }

                    editPlane.set(new Vector3(0, intersection.y, 0), Vector3.Y);
                    lastIntersection = null;
                }
                else if(Gdx.input.isTouched()) {
                    if(pickedLine != null) {
                        pickedLine.start.add((int)editPlaneIntersection.x - (int)lastIntersection.x, (int)editPlaneIntersection.z - (int)lastIntersection.z);
                        pickedLine.end.add((int)editPlaneIntersection.x - (int)lastIntersection.x, (int)editPlaneIntersection.z - (int)lastIntersection.z);
                        refreshSectors();
                    }
                    else if(pickedPoint != null) {
                        pickedPoint.add((int)editPlaneIntersection.x - (int)lastIntersection.x, (int)editPlaneIntersection.z - (int)lastIntersection.z);
                        refreshSectors();
                    }
                    else if(pickedSector != null) {
                        boolean heightMode = Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT);

                        if(!heightMode) {
                            pickedSector.translate((int) editPlaneIntersection.x - (int) lastIntersection.x, (int) editPlaneIntersection.z - (int) lastIntersection.z);
                            updateSectorOwnership(pickedSector);

                            if(pickedSector.isSolid) {
                                refreshLineSolidity(pickedSector);
                            }
                        }
                        else {
                            if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                                pickedSector.ceilHeight = startHeightModeCeilHeight - ((Gdx.input.getY() - lastMousePoint.y) / 60f);
                                pickedSector.ceilHeight = (int) (pickedSector.ceilHeight * 8f) / 8f;
                            }
                            else {
                                pickedSector.floorHeight = startHeightModeFloorHeight - ((Gdx.input.getY() - lastMousePoint.y) / 60f);
                                pickedSector.floorHeight = (int) (pickedSector.floorHeight * 8f) / 8f;
                            }
                        }

                        refreshSectors();
                    }
                    wasDragging = true;
                }
                else {
                    editPlane.set(Vector3.Zero, Vector3.Y);
                }

                if(Gdx.input.justTouched() && Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
                    if(pickedSector != null && hoveredSector != null) {
                        hoveredSector.match(pickedSector);
                        refreshSectors();
                    }
                }

                // Delete sectors or points
                if (Gdx.input.isKeyJustPressed(Input.Keys.DEL)) {
                    if(pickedPoint != null) {
                        deleteVertex(pickedPoint);
                    }
                    else if(pickedSector != null) {
                        deleteSector(pickedSector);
                    }
                    else if(pickedLine != null) {
                        deleteVertex(pickedLine.end);
                    }

                    refreshSectors();

                    pickedPoint = null;
                    pickedSector = null;
                    pickedLine = null;
                }
            }
            else if (editorMode == EditorModes.SPLIT) {
                pickedPoint2d.x = (int) pickedPoint2d.x;
                pickedPoint2d.y = (int) pickedPoint2d.y;
                if(Gdx.input.justTouched()) {
                    splitStart = new Vector2(pickedPoint2d);
                    splitEnd = new Vector2(pickedPoint2d);
                }
                else if(Gdx.input.isTouched()) {
                    splitEnd.set(pickedPoint2d);
                }
                else if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                    Array<Line> checking = new Array<Line>(lines);
                    for(Line l : checking) {
                        Vector2 i = l.findIntersection(splitStart, splitEnd);
                        if(i != null) {
                            addPointToLine(l, i);
                        }
                    }
                    refreshSectors();
                }
            }
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if(editorMode != EditorModes.SECTOR) {
                editorMode = EditorModes.SECTOR;
            }
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            if(editorMode != EditorModes.POINT) {
                if(current != null) cancelEditingSector();
                editorMode = EditorModes.POINT;
            }
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            if(current != null) cancelEditingSector();

            if(editorMode == EditorModes.SPLIT) {
                editorMode = EditorModes.SECTOR;
            }
            else {
                editorMode = EditorModes.SPLIT;
            }
        }
    }

    Vector3 sectorIntersection = new Vector3();
    Vector3 wallIntersection = new Vector3();
    private boolean intersectsWorld(Ray r, Vector3 intersects) {
        hoveredSector = intersectSectors(r, sectorIntersection);
        hoveredLine = intersectWalls(r, wallIntersection);

        boolean hitSector = hoveredSector != null;
        boolean hitWall = hoveredLine != null;

        if(hitSector || hitWall) {
            if(hitSector) {
                intersects.set(sectorIntersection);
            }
            if(hitWall) {
                if(sectorIntersection == null) intersects.set(wallIntersection);
                else if(wallIntersection.dst(camera.position) <= sectorIntersection.dst(camera.position)) {
                    intersects.set(wallIntersection);
                }
            }
            return true;
        }

        return false;
    }

    private void addPointToLine(Line l, Vector2 point) {
        if(point == l.start || point == l.end) return;

        Vector2 v = getExistingVertex(point);
        if(v == null) vertices.add(point);

        Vector2 oldEnd = l.end;
        l.end = point;

        Line newLine = new Line(point, oldEnd, l.solid, l.left, l.right);
        lines.add(newLine);

        addNewPointToSector(new Line(l.start, oldEnd, l.solid, l.left, l.right), point, sectors);
    }

    private void addNewPointToSector(Line line, Vector2 point, Array<Sector> sectors) {
        for(Sector sector : sectors) {

            int startIndex = sector.points.indexOf(line.start, true);
            int endIndex = sector.points.indexOf(line.end, true);

            if(startIndex >= 0 && endIndex >= 0) {
                int diff = startIndex - endIndex;
                if(Math.abs(diff) == 1) {
                    sector.points.insert(startIndex + 1, point);
                }
                else if (Math.abs(diff) == sector.points.size - 1) {
                    sector.points.insert(0, point);
                }
            }

            addNewPointToSector(line, point, sector.subsectors);
        }
    }

    Vector2 findPicked_t = new Vector2();
    private Line findPickedLine(Vector3 hovered) {
        for(Line l : lines) {
            findPicked_t.set(hovered.x, hovered.z);
            float dist = Intersector.distanceSegmentPoint(l.start, l.end, findPicked_t);
            if(dist < 0.175f) return l;
        }
        return null;
    }

    private void deleteVertex(Vector2 vertex) {
        for(Sector s : sectors) {
            s.removePoint(vertex);
        }

        Array<Line> linesToDelete = new Array<Line>();
        for(Line l : lines) {
            if(l.end == vertex) {
                linesToDelete.add(l);
                Array<Line> nexts = findLinesWithStartVertexInSector(vertex, l.left);
                for(Line next : nexts) {
                    if (next != null) {
                        next.start = l.start;
                    }
                }
            }
        }

        lines.removeAll(linesToDelete, true);
    }

    private Array<Line> findLinesWithStartVertexInSector(Vector2 vertex, Sector sector) {
        Array<Line> r = new Array<Line>();
        for(int i = 0; i < lines.size; i++) {
            Line l = lines.get(i);
            if(l.start == vertex && l.left == sector)
                r.add(l);
        }
        return r;
    }

    private void deleteLinesForSector(Sector sector) {
        Array<Line> linesToRemove = new Array<Line>();

        for(Line l : lines) {
            if(l.left == sector) {
                if(l.right == null) {
                    if(sector.parent != null &&
                            sector.parent.points.contains(l.start, true) &&
                            sector.parent.points.contains(l.end, true)) {
                        l.left = sector.parent;
                    }
                    else {
                        linesToRemove.add(l);
                    }
                }
                else {
                    l.left = l.right;
                    l.right = null;
                }
            }
        }

        lines.removeAll(linesToRemove, true);

        for(Sector s : sector.subsectors) {
            deleteLinesForSector(s);
        }
    }

    private void deleteSector(Sector sector) {
        if(sector.parent != null) {
            sector.parent.subsectors.removeValue(sector, true);
        }
        else {
            sectors.removeValue(sector, true);
        }

        deleteLinesForSector(sector);
    }

    private Vector2 getVertexNear(float x, float y, float distance) {
        for(Vector2 v : vertices) {
            float d = v.dst(x, y);
            if(d < distance) {
                return v;
            }
        }
        return null;
    }

    private void splitSectors(Vector2 start, Vector2 end) {
        Array<Sector> toRemove = new Array<Sector>();
        Array<Sector> newSplits = new Array<Sector>();
        for(Sector s : sectors) {
            if(s.lineIntersects(start, end)) {
                Array<Sector> splits =
                        s.split(new Plane(
                                        new Vector3(start.x, 0, start.y),
                                        new Vector3(end.x, 0, end.y),
                                        new Vector3(end.x, 1, end.y)),
                                vertices);

                if (splits.size > 0) {
                    toRemove.add(s);
                    for (Sector split : splits) {
                        newSplits.add(split);
                    }
                }
            }
        }

        // remove the old, unsplit sector
        for(Sector s : toRemove) {
            sectors.removeValue(s, true);
        }

        // add the new splits
        for(Sector s : newSplits) {
            sectors.add(s);
        }

        refreshSectors();
    }

    private void cancelEditingSector() {
        if (current.parent == null)
            sectors.removeValue(current, true);
        else
            current.parent.subsectors.removeValue(current, true);

        // remove the lines without a sector anymore
        Array<Line> orphanLines = new Array<Line>();
        for (Line line : lines) {
            if (line.left == current) {
                orphanLines.add(line);
            } else if (line.right == current) {
                line.right = null;
            }
        }
        for (Line orphan : orphanLines) {
            lines.removeValue(orphan, true);
        }

        current = null;

        refreshSectors();
    }

    private boolean isValidSectorForNextPoint(Vector3 pickedGridPoint) {
        if(current.parent == null)
            return true;
        else if(current.parent == hoveredSector)
            return true;
        else {
            return vertexExists(new Vector2(pickedGridPoint.x, pickedGridPoint.z));
        }
    }

    public boolean vertexExists(Vector2 vertex) {
        for(Line line : lines) {
            if(line.start.equals(vertex) || line.end.equals(vertex))
                return true;
        }
        return false;
    }

    public Vector2 getExistingVertex(Vector2 vertex) {
        int found = vertices.indexOf(vertex, false);
        if(found >= 0) return vertices.get(found);
        return null;
    }

    public void addVertex(Vector2 vertex) {
        Vector2 existing = getExistingVertex(vertex);
        if(existing == null) vertices.add(vertex);
    }

    public void finishSector() {
        if(!isClockwise(current)) current.points.reverse();

        // Add new points to existing lines / sectors where needed
        Array<Vector2> currentPoints = new Array<Vector2>(current.getPoints());
        for(Vector2 p : currentPoints) {
            Line hovered = findPickedLine(new Vector3(p.x, 0, p.y));
            if(hovered != null) {
                addPointToLine(hovered, p);
            }
        }

        // find the parent, if there is one
        Sector parent = null;
        for(Sector s : sectors) {
            Sector containing = s.getSectorOfSector(current);
            if(containing != null) parent = containing;
        }

        if(parent != null) {
            parent.addSubSector(current);
            current.ceilHeight = parent.ceilHeight;

            // parent's sectors might now be contained by this new sector
            refreshSectorParents(current, parent);
        }

        // add vertices and lines for the new sector
        Array<Vector2> points = current.getPoints();
        for(int i = 0; i < points.size; i++) {
            Vector2 p = points.get(i);
            addVertex(p);

            if(i > 0) {
                Vector2 prev = points.get(i - 1);
                addLine(prev, p);
            }
        }

        // close the loop, if it isn't
        Vector2 startPoint = points.first();
        Vector2 lastPoint = points.get(points.size - 1);
        if (!lastPoint.equals(startPoint)) {
            addLine(lastPoint, startPoint);
        }

        if(parent == null)
            sectors.add(current);


        // solid parents mean line solidity might change now
        if(parent != null && parent.isSolid) {
            refreshLineSolidity(parent);
            refreshLineSolidity(current);
        }

        current = null;
        refreshSectors();

        editHeight = null;
        editPlane.set(Vector3.Zero, Vector3.Y);
    }

    private void refreshSectorParents(Sector sector, Sector newParent) {
        for (Sector s : newParent.subsectors) {
            if (s != sector && sector.isSectorInside(s)) {
                newParent.subsectors.removeValue(s, true);
                sector.addSubSector(s);

                for(Line l : lines) {
                    if(l.left == s || l.right == s) {
                        if (l.right == newParent) {
                            l.right = sector;
                        }
                        if (l.left == newParent) {
                            l.left = sector;
                        }
                    }
                }
            }
        }
    }

    private void updateSectorOwnership(Sector sector) {
        Sector parent = null;
        for(Sector s : sectors) {
            if(s != sector && parent == null) {
                parent = s.getSectorOfSector(sector);
            }
        }

        if (parent != null && sector.parent != parent) {
            if (sector.parent != null) {
                sector.parent.subsectors.removeValue(sector, true);
            }
            else {
                sectors.removeValue(sector, true);
            }

            Sector oldParent = sector.parent;
            parent.addSubSector(sector);

            for(Line l : lines) {
                if(l.left == sector) {
                    if(l.right == null || l.right == oldParent) {
                        l.right = parent;
                        l.solid = false;
                    }
                }
            }
        }
        else if(parent == null && sector.parent != null) {
            Sector oldParent = sector.parent;
            sector.parent.subsectors.removeValue(sector, true);
            sector.parent = null;

            sectors.add(sector);

            for(Line l : lines) {
                if(l.left == sector) {
                    if(l.right == oldParent) {
                        l.right = null;
                        l.solid = true;
                    }
                }
            }
        }
    }

    public boolean isClockwise(Sector s) {
        // sum of (x2 − x1)(y2 + y1)
        float sum = 0;
        for(int i = 0; i < s.points.size - 1; i++) {
            Vector2 start = s.points.get(i);
            Vector2 end = s.points.get(i + 1);
            sum += (end.x - start.x) * (end.y + start.y);
        }

        // close the loop
        Vector2 start = s.points.get(s.points.size - 1);
        Vector2 end = s.points.get(0);
        sum += (end.x - start.x) * (end.y + start.y);

        return sum >= 0;
    }

    public void addLine(Vector2 start, Vector2 end) {

        // don't duplicate verts
        Vector2 existingStart = getExistingVertex(start);
        Vector2 existingEnd = getExistingVertex(end);

        Line line = new Line(existingStart, existingEnd, current.parent == null, current, current.parent);

        // check if this exists already
        Line existing = null;
        for(Line l : lines) {
           if(l.isEqualTo(line)) {
               existing = l;
               break;
           }
        }

        if(existing == null) {
            lines.add(line);
        }
        else {
            if(existing.left == current.parent) {
                existing.left = current;
            }
            else if(existing.left != current) {
                existing.solid = existing.left.isSolid;
                existing.right = current;
            }
            current.floorHeight = existing.left.floorHeight;
            current.ceilHeight = existing.left.ceilHeight;
        }
    }

    public void setHighlights() {
        for(Sector s : sectors) {
            resetSectorHighlights(s);
        }
        for(Line l : lines) {
            resetWallHighlights(l);
        }

        // update plane colors
        if(pickedSector != null)
            pickedSector.floorMaterial.set(ColorAttribute.createDiffuse(Color.RED));

        if(pickedLine != null)
            pickedLine.lowerMaterial.set(ColorAttribute.createDiffuse(Color.RED));
    }

    public void resetSectorHighlights(Sector sector) {
        sector.floorMaterial.set(ColorAttribute.createDiffuse(Color.WHITE));
        for(Sector s : sector.subsectors) {
            resetSectorHighlights(s);
        }
    }

    public void resetWallHighlights(Line line) {
        line.lowerMaterial.set(ColorAttribute.createDiffuse(Color.WHITE));
    }

	@Override
	public void render () {
        try {
            update();

            for(Material selectedMaterial : selectedMaterials) {
                selectedMaterial.set(ColorAttribute.createDiffuse(Color.WHITE));
            }

            if(hoveredLine != null || pickedLine != null || hoveredSector != null || pickedSector != null) {
                for (ModelInstance m : models) {
                    if(hoveredLine != null) {
                        Material material = m.getMaterial(hoveredLine.hashCode() + "_lower");
                        if (material != null) {
                            material.set(ColorAttribute.createDiffuse(Color.YELLOW));
                            selectedMaterials.add(material);
                        }
                    }
                    if(pickedLine != null) {
                        Material material = m.getMaterial(pickedLine.hashCode() + "_lower");
                        if (material != null) {
                            material.set(ColorAttribute.createDiffuse(Color.RED));
                            selectedMaterials.add(material);
                        }
                    }

                    if(hoveredSector != null) {
                        Material material = m.getMaterial(hoveredSector.hashCode() + "_floor");
                        if (material != null) {
                            material.set(ColorAttribute.createDiffuse(Color.YELLOW));
                            selectedMaterials.add(material);
                        }
                    }
                    if(pickedSector != null) {
                        Material material = m.getMaterial(pickedSector.hashCode() + "_floor");
                        if (material != null) {
                            material.set(ColorAttribute.createDiffuse(Color.RED));
                            selectedMaterials.add(material);
                        }
                    }
                }
            }

            Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            camController.update();

            lineRenderer.setProjectionMatrix(camera.combined);

            renderGrid();

            batch.begin(camera);
            batch.render(models);
            batch.end();

            for(Sector sector : sectors) {
                renderPoints(sector, Color.WHITE);
            }


            if(editorMode == EditorModes.SECTOR) {
                renderNextLine();
                renderNextPoint();

                if(current != null) {
                    renderSectorWireframe(current, Color.YELLOW);
                    renderPoints(current, Color.YELLOW);
                }
            }
            else if(editorMode == EditorModes.POINT) {
                if(pickedPoint != null || hoveredPoint != null) {
                    Vector2 point = pickedPoint;
                    if(point == null) point = hoveredPoint;

                    for(Sector s : getAllSectorsWithVertex(sectors, point, new Array<Sector>())) {
                        float pointSize = 0.2f;
                        lineRenderer.begin(ShapeRenderer.ShapeType.Filled);
                        lineRenderer.setColor(Color.RED);
                        lineRenderer.box(point.x - pointSize / 2, s.floorHeight, point.y + pointSize / 2, pointSize, pointSize / 3, pointSize);
                        lineRenderer.end();
                    }
                }
            }
            else if(editorMode == EditorModes.SPLIT) {
                if(splitStart != null && splitEnd != null) {
                    lineRenderer.begin(ShapeRenderer.ShapeType.Line);
                    lineRenderer.setColor(Color.RED);
                    lineRenderer.line(splitStart.x, 0, splitStart.y, splitEnd.x, 0, splitEnd.y);
                    lineRenderer.end();

                    float pointSize = 0.2f;
                    lineRenderer.begin(ShapeRenderer.ShapeType.Filled);
                    lineRenderer.setColor(Color.RED);
                    lineRenderer.box(pickedPoint2d.x - pointSize / 2, 0, pickedPoint2d.y + pointSize / 2, pointSize, pointSize / 3, pointSize);
                    lineRenderer.end();
                }
            }
        }
        catch(Throwable t) {
            Gdx.app.log("Error", t.getMessage());
        }
	}

    public Array<Sector> getAllSectorsWithVertex(Array<Sector> search, Vector2 vertex, Array<Sector> found) {
        for(Sector s : search) {
            if(s.points.contains(vertex, true)) {
                found.add(s);
            }
            getAllSectorsWithVertex(s.subsectors, vertex, found);
        }
        return found;
    }

    public void renderSectorWireframe(Sector s, Color color) {
        Array<Vector2> points = s.getPoints();
        if(points.size >= 2) {
            lineRenderer.begin(ShapeRenderer.ShapeType.Line);

            if(hoveredSector == s)
                lineRenderer.setColor(Color.WHITE);
            else
                lineRenderer.setColor(color);

            for (int i = 0; i < points.size - 1; i++) {
                Vector2 startPoint = points.get(i);
                Vector2 endPoint = points.get(i + 1);

                tempVec3.set(startPoint.x, s.floorHeight, startPoint.y);
                tempVec3_2.set(endPoint.x, s.floorHeight, endPoint.y);

                lineRenderer.line(tempVec3, tempVec3_2);
            }

            Vector2 startPoint = points.get(0);
            Vector2 endPoint = points.get(points.size - 1);

            tempVec3.set(startPoint.x, s.floorHeight, startPoint.y);
            tempVec3_2.set(endPoint.x, s.floorHeight, endPoint.y);

            lineRenderer.line(tempVec3, tempVec3_2);

            lineRenderer.end();
        }

        for(Sector subsector : s.subsectors) {
            renderSectorWireframe(subsector, color);
        }
    }

    public void renderNextLine() {
        if(current != null) {
            Array<Vector2> points = current.getPoints();
            if (points.size > 0) {
                Vector2 endPoint = points.get(points.size - 1);
                lineRenderer.begin(ShapeRenderer.ShapeType.Line);
                lineRenderer.setColor(Color.YELLOW);
                lineRenderer.line(tempVec3.set(endPoint.x, editHeight, endPoint.y), pickedGridPoint);
                lineRenderer.end();
            }
        }
    }

    public void renderPoints(Sector s, Color color) {
        lineRenderer.begin(ShapeRenderer.ShapeType.Filled);
        lineRenderer.setColor(color);
        Array<Vector2> points = s.getPoints();

        float pointSize = 0.2f;
        if(points.size > 0) {
            for(Vector2 point : points) {
                lineRenderer.box(point.x - pointSize / 2, s.getFloorHeight(), point.y + pointSize / 2, pointSize, pointSize / 3, pointSize);
            }
        }

        lineRenderer.end();

        for(Sector subsector : s.subsectors) {
            renderPoints(subsector, color);
        }
    }

    public void renderNextPoint() {
        float pointSize = 0.2f;
        lineRenderer.begin(ShapeRenderer.ShapeType.Filled);
        lineRenderer.setColor(Color.YELLOW);
        lineRenderer.box(pickedGridPoint.x - pointSize / 2, pickedGridPoint.y, pickedGridPoint.z + pointSize / 2, pointSize, pointSize / 3, pointSize);
        lineRenderer.end();
    }

    public void renderGrid() {

        float size = 80;
        float half = size / 2;

        lineRenderer.setColor(wireframeColor);

        lineRenderer.begin(ShapeRenderer.ShapeType.Line);
        for(int i = 0; i < size; i++) {
            lineRenderer.line(-half + (int)camera.position.x, 0, i - half + (int)camera.position.z, half + (int)camera.position.x, 0, i - half + (int)camera.position.z);
        }
        for(int i = 0; i < size; i++) {
            lineRenderer.line(i - half + (int)camera.position.x, 0, -half + (int)camera.position.z, i - half + (int)camera.position.x, 0, half + (int)camera.position.z);
        }
        lineRenderer.end();
    }
}
