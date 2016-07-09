package com.interrupt.doomtest;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.interrupt.doomtest.collisions.WorldIntersection;
import com.interrupt.doomtest.collisions.WorldIntersector;
import com.interrupt.doomtest.gfx.renderer.RendererFrontend;
import com.interrupt.doomtest.input.EditorCameraController;
import com.interrupt.doomtest.levels.Level;
import com.interrupt.doomtest.levels.Line;
import com.interrupt.doomtest.levels.Sector;
import com.interrupt.doomtest.levels.editor.Editor;

public class DoomTestEditor extends ApplicationAdapter {

    Camera camera;
    EditorCameraController camController;
    ShapeRenderer lineRenderer;
    RendererFrontend renderer;

    public Level level = new Level();
    public Editor editor = new Editor(level);
    public Sector current;
    Plane editPlane = new Plane(Vector3.Y, Vector3.Zero);

    Vector3 tempVec3 = new Vector3();
    Vector3 tempVec3_2 = new Vector3();

    Vector3 lastIntersection = new Vector3();
    Vector3 editPlaneIntersection = new Vector3();

    Vector3 intersection = new Vector3();
    Vector3 pickedGridPoint = new Vector3();
    Vector2 pickedPoint2d = new Vector2();
    WorldIntersection worldIntersection = new WorldIntersection();

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
        renderer = new RendererFrontend();
	}

    public void refreshRenderer() {
        renderer.setLevel(level);
    }

    public void update() {
        Ray r = camera.getPickRay(Gdx.input.getX(), Gdx.input.getY());

        if(lastIntersection != null)
            lastIntersection.set(editPlaneIntersection);

        boolean intersectsWorld = WorldIntersector.intersectsWorld(level, r, worldIntersection);
        boolean intersectsEditPlane = Intersector.intersectRayPlane(r, editPlane, editPlaneIntersection);

        if(intersectsWorld || intersectsEditPlane) {
            // Where is the intersection point with the world?
            if(intersectsWorld) intersection.set(worldIntersection.intersectionPoint);
            else intersection.set(editPlaneIntersection);

            // What sectors / lines are being picked now?
            hoveredLine = worldIntersection.hitLine;
            hoveredSector = worldIntersection.hitSector;

            if(lastIntersection == null) lastIntersection = new Vector3(editPlaneIntersection);

            intersection.y = ((int)(intersection.y * 8f) / 8f);

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
                    editor.refreshLineSolidity(picked);
                    refreshRenderer();
                }

                if(Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                    float mod = 0.1f;
                    if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) mod *= -1f;

                    Sector picked = hoveredSector;
                    picked.floorHeight += mod;
                    refreshRenderer();
                }

                if(Gdx.input.isKeyJustPressed(Input.Keys.T)) {
                    float mod = 0.1f;
                    if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) mod *= -1f;

                    Sector picked = hoveredSector;
                    picked.ceilHeight += mod;
                    refreshRenderer();
                }

                // snap to nearest line
                Line nearLine = editor.findPickedLine(intersection);
                if(nearLine != null) {
                    Vector2 nearest = new Vector2();
                    Intersector.nearestSegmentPoint(nearLine.start, nearLine.end, new Vector2(intersection.x, intersection.z), nearest);
                    pickedGridPoint.x = nearest.x;
                    pickedGridPoint.z = nearest.y;
                }

                // snap to nearest point
                Vector2 hovering = level.getVertexNear(intersection.x, intersection.z, 0.25f);
                if(hovering != null) {
                    pickedGridPoint.x = hovering.x;
                    pickedGridPoint.z = hovering.y;
                }

                // Add a new vertex when clicked
                if (Gdx.input.justTouched()) {

                    Vector2 next = new Vector2(pickedGridPoint.x, pickedGridPoint.z);
                    Vector2 existing = editor.getExistingVertex(next);

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
                            editor.refreshSectorParents(pickedSector, pickedSector.parent);
                        }
                        wasDragging = false;
                    }

                    hoveredPoint = level.getVertexNear(intersection.x, intersection.z, 0.25f);
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
                    refreshRenderer();

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
                        refreshRenderer();
                    }
                    else if(pickedPoint != null) {
                        pickedPoint.add((int)editPlaneIntersection.x - (int)lastIntersection.x, (int)editPlaneIntersection.z - (int)lastIntersection.z);
                        refreshRenderer();
                    }
                    else if(pickedSector != null) {
                        boolean heightMode = Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT);

                        if(!heightMode) {
                            pickedSector.translate((int) editPlaneIntersection.x - (int) lastIntersection.x, (int) editPlaneIntersection.z - (int) lastIntersection.z);
                            editor.updateSectorOwnership(pickedSector);

                            if(pickedSector.isSolid) {
                                editor.refreshLineSolidity(pickedSector);
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

                        refreshRenderer();
                    }
                    wasDragging = true;
                }
                else {
                    editPlane.set(Vector3.Zero, Vector3.Y);
                }

                if(Gdx.input.justTouched() && Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
                    if(pickedSector != null && hoveredSector != null) {
                        hoveredSector.match(pickedSector);
                        refreshRenderer();
                    }
                }

                // Delete sectors or points
                if (Gdx.input.isKeyJustPressed(Input.Keys.DEL)) {
                    if(pickedPoint != null) {
                        editor.deleteVertex(pickedPoint);
                    }
                    else if(pickedSector != null) {
                        editor.deleteSector(pickedSector);
                    }
                    else if(pickedLine != null) {
                        editor.deleteVertex(pickedLine.end);
                    }

                    refreshRenderer();

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
                    Array<Line> checking = new Array<Line>(level.lines);
                    for(Line l : checking) {
                        Vector2 i = l.findIntersection(splitStart, splitEnd);
                        if(i != null) {
                            editor.addPointToLine(l, i);
                        }
                    }
                    refreshRenderer();
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

    private void cancelEditingSector() {
        if (current.parent == null)
            level.sectors.removeValue(current, true);
        else
            current.parent.subsectors.removeValue(current, true);

        // remove the lines without a sector anymore
        Array<Line> orphanLines = new Array<Line>();
        for (Line line : level.lines) {
            if (line.left == current) {
                orphanLines.add(line);
            } else if (line.right == current) {
                line.right = null;
            }
        }
        for (Line orphan : orphanLines) {
            level.lines.removeValue(orphan, true);
        }

        current = null;

        refreshRenderer();
    }

    public void finishSector() {
        if(!editor.isClockwise(current)) current.points.reverse();

        // Add new points to existing lines / sectors where needed
        Array<Vector2> currentPoints = new Array<Vector2>(current.getPoints());
        for(Vector2 p : currentPoints) {
            Line hovered = editor.findPickedLine(new Vector3(p.x, 0, p.y));
            if(hovered != null) {
                editor.addPointToLine(hovered, p);
            }
        }

        if(current.points.size > 2) {

            // find the parent, if there is one
            Sector parent = null;
            for (Sector s : level.sectors) {
                Sector containing = s.getSectorOfSector(current);
                if (containing != null) parent = containing;
            }

            if (parent != null) {
                parent.addSubSector(current);
                current.ceilHeight = parent.ceilHeight;

                // parent's sectors might now be contained by this new sector
                editor.refreshSectorParents(current, parent);
            }

            // add vertices and lines for the new sector
            Array<Vector2> points = current.getPoints();
            for (int i = 0; i < points.size; i++) {
                Vector2 p = points.get(i);
                editor.addVertex(p);

                if (i > 0) {
                    Vector2 prev = points.get(i - 1);
                    editor.addLine(current, prev, p);
                }
            }

            // close the loop, if it isn't
            Vector2 startPoint = points.first();
            Vector2 lastPoint = points.get(points.size - 1);
            if (!lastPoint.equals(startPoint)) {
                editor.addLine(current, lastPoint, startPoint);
            }

            if (parent == null)
                level.sectors.add(current);


            // solid parents mean line solidity might change now
            if (parent != null && parent.isSolid) {
                editor.refreshLineSolidity(parent);
                editor.refreshLineSolidity(current);
            }
        }

        current = null;
        refreshRenderer();

        editHeight = null;
        editPlane.set(Vector3.Zero, Vector3.Y);
    }

    public void setHighlights() {
        for(Sector s : level.sectors) {
            resetSectorHighlights(s);
        }
        for(Line l : level.lines) {
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
                for (ModelInstance m : renderer.getLevelModels()) {
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

            renderer.render(camera);

            for(Sector sector : level.sectors) {
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

                    for(Sector s : editor.getAllSectorsWithVertex(level.sectors, point, new Array<Sector>())) {
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
