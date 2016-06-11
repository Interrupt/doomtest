package com.interrupt.doomtest;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
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
    Vector3 intersection = new Vector3();
    Vector3 pickedGridPoint = new Vector3();
    Vector2 pickedPoint2d = new Vector2();

    Sector hoveredSector = null;
    Sector pickedSector = null;
    Vector2 pickedPoint = null;

    public enum EditorModes { SECTOR, POINT };

    public EditorModes editorMode = EditorModes.SECTOR;

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
        for(Sector sector : sectors) {
            Sector picked = sector.getSectorOfPoint(point);
            if(picked != null && !picked.hasVertex(point))
                return picked;
        }

        return null;
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

        lastIntersection.set(intersection);
        if (Intersector.intersectRayPlane(r, editPlane, intersection)) {
            // round grid point to the grid
            pickedGridPoint.set((int) intersection.x, intersection.y, (int) intersection.z);
            pickedPoint2d.set(intersection.x, intersection.z);

            // find which sector is picked
            hoveredSector = pickSector(pickedPoint2d);

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

                // Add a new vertex when clicked
                if (Gdx.input.justTouched()) {

                    Vector2 next = new Vector2(pickedGridPoint.x, pickedGridPoint.z);
                    Vector2 existing = getExistingVertex(next);

                    // Start a new sector if not currently editing one
                    if (current == null) {
                        current = new Sector();
                    }

                    // finish the sector automatically if the line loops
                    if(current.getPoints().size > 0 && next.equals(current.getPoints().first())) {
                        finishSector();
                        refreshSectors();
                        current = null;
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
                    current = null;
                    refreshSectors();
                }
            }
            else if (editorMode == EditorModes.POINT) {
                if(!Gdx.input.isTouched()) {
                    pickedPoint2d.x = (int) pickedPoint2d.x;
                    pickedPoint2d.y = (int) pickedPoint2d.y;

                    // todo: better vertex picking
                    pickedPoint = getExistingVertex(pickedPoint2d);
                    if(pickedPoint == null && hoveredSector != null) pickedSector = hoveredSector;
                }
                else {
                    if(pickedPoint != null) {
                        pickedPoint.add((int)intersection.x - (int)lastIntersection.x, (int)intersection.z - (int)lastIntersection.z);
                        refreshSectors();
                    }
                    else if(pickedSector != null) {
                        pickedSector.translate((int)intersection.x - (int)lastIntersection.x, (int)intersection.z - (int)lastIntersection.z);
                        refreshSectors();
                    }
                }
            }
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            if(current != null) cancelEditingSector();

            if(editorMode == EditorModes.SECTOR) {
                editorMode = EditorModes.POINT;
            }
            else {
                editorMode = EditorModes.SECTOR;
            }
        }
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

        sectors.add(current);
    }

    public boolean isClockwise(Sector s) {
        // (x2 − x1)(y2 + y1)
        float sum = 0;
        for(int i = 0; i < s.points.size - 1; i++) {
            Vector2 start = s.points.get(i);
            Vector2 end = s.points.get(i + 1);
            sum += (end.x - start.x) * (end.y + start.y);
        }
        return sum > 0;
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
            if(existing.left != current) {
                existing.solid = false;
                existing.right = current;
                current.floorHeight = existing.left.floorHeight;
                current.ceilHeight = existing.left.ceilHeight;
            }
        }
    }

	@Override
	public void render () {
        try {
            update();

            Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            camController.update();

            lineRenderer.setProjectionMatrix(camera.combined);

            renderGrid();

            batch.begin(camera);
            batch.render(models);
            batch.end();

            for(Sector sector : sectors) {
                renderSectorWireframe(sector);
                renderPoints(sector);
            }


            if(editorMode == EditorModes.SECTOR) {
                renderNextLine();
                renderNextPoint();

                if(current != null) {
                    renderSectorWireframe(current);
                    renderPoints(current);
                }
            }
            else if(editorMode == EditorModes.POINT) {
                if(pickedPoint != null) {
                    float pointSize = 0.2f;
                    lineRenderer.begin(ShapeRenderer.ShapeType.Filled);
                    lineRenderer.setColor(Color.RED);
                    lineRenderer.box(pickedPoint.x - pointSize / 2, 0, pickedPoint.y + pointSize / 2, pointSize, pointSize / 3, pointSize);
                    lineRenderer.end();
                }
            }
        }
        catch(Throwable t) {
            Gdx.app.log("Error", t.getMessage());
        }
	}

    public void renderSectorWireframe(Sector s) {
        Array<Vector2> points = s.getPoints();
        if(points.size >= 2) {
            lineRenderer.begin(ShapeRenderer.ShapeType.Line);

            if(hoveredSector == s || current == s)
                lineRenderer.setColor(Color.WHITE);
            else
                lineRenderer.setColor(Color.LIGHT_GRAY);

            for (int i = 0; i < points.size - 1; i++) {
                Vector2 startPoint = points.get(i);
                Vector2 endPoint = points.get(i + 1);

                tempVec3.set(startPoint.x, 0, startPoint.y);
                tempVec3_2.set(endPoint.x, 0, endPoint.y);

                lineRenderer.line(tempVec3, tempVec3_2);
            }

            Vector2 startPoint = points.get(0);
            Vector2 endPoint = points.get(points.size - 1);

            tempVec3.set(startPoint.x, 0, startPoint.y);
            tempVec3_2.set(endPoint.x, 0, endPoint.y);

            lineRenderer.line(tempVec3, tempVec3_2);

            lineRenderer.end();
        }

        for(Sector subsector : s.subsectors) {
            renderSectorWireframe(subsector);
        }
    }

    public void renderNextLine() {
        if(current != null) {
            Array<Vector2> points = current.getPoints();
            if (points.size > 0) {
                Vector2 endPoint = points.get(points.size - 1);
                lineRenderer.begin(ShapeRenderer.ShapeType.Line);
                lineRenderer.setColor(Color.YELLOW);
                lineRenderer.line(tempVec3.set(endPoint.x, 0, endPoint.y), pickedGridPoint);
                lineRenderer.end();
            }
        }
    }

    public void renderPoints(Sector s) {
        lineRenderer.begin(ShapeRenderer.ShapeType.Filled);
        lineRenderer.setColor(Color.WHITE);
        Array<Vector2> points = s.getPoints();

        float pointSize = 0.2f;
        if(points.size > 0) {
            for(Vector2 point : points) {
                lineRenderer.box(point.x - pointSize / 2, 0, point.y + pointSize / 2, pointSize, pointSize / 3, pointSize);
            }
        }

        lineRenderer.end();

        for(Sector subsector : s.subsectors) {
            renderPoints(subsector);
        }
    }

    public void renderWalls() {
        lineRenderer.begin(ShapeRenderer.ShapeType.Line);
        lineRenderer.setColor(Color.GRAY);

        for(Line line : lines) {
            if(line.solid) {
                for (int i = 1; i < 5; i++) {
                    lineRenderer.line(line.start.x, i, line.start.y, line.end.x, i, line.end.y);
                }
            }
        }

        lineRenderer.end();
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

        lineRenderer.setColor(Color.DARK_GRAY);

        lineRenderer.begin(ShapeRenderer.ShapeType.Line);
        for(int i = 0; i < size; i++) {
            lineRenderer.line(-half, 0, i - half, half, 0, i - half);
        }
        for(int i = 0; i < size; i++) {
            lineRenderer.line(i - half, 0, -half, i - half, 0, half);
        }
        lineRenderer.end();
    }

    public void pickVertex() {

    }
}
