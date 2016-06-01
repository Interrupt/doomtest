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
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;


public class DoomTestGame extends ApplicationAdapter {

    ModelBatch batch;
    Camera camera;
    CameraInputController camController;
    ShapeRenderer lineRenderer;
    Vector3 intersection = new Vector3();
    public Array<ModelInstance> models = new Array<ModelInstance>();
    public Array<Sector> sectors;
    public Array<Line> lines;
    public Sector current;
    Plane editPlane = new Plane(Vector3.Y, Vector3.Zero);

    Vector3 tempVec3 = new Vector3();
    Vector3 tempVec3_2 = new Vector3();

	@Override
	public void create () {
        batch = new ModelBatch();

        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 20f, -5f);
        camera.lookAt(0,0,-5f);
        camera.near = 1f;
        camera.far = 300f;
        camera.update();

        // A camera that can be driven around
        camController = new CameraInputController(camera);
        Gdx.input.setInputProcessor(camController);

        lineRenderer = new ShapeRenderer();

        // The level, a list of sectors and lines
        sectors = new Array<Sector>();
        lines = new Array<Line>();
	}

    public void refreshSector() {
        models.clear();

        for(Sector sector : sectors) {
            // turn the sector into a model
            Model m = sector.tesselate();
            ModelInstance floor = new ModelInstance(m);
            models.add(floor);
        }
    }

    public Sector pickSector(Vector2 point) {
        for(Sector sector : sectors) {
            Sector picked = sector.getSectorOfPoint(point);
            if(picked != null)
                return picked;
        }

        return null;
    }

    public void update() {
        Ray r = camera.getPickRay(Gdx.input.getX(), Gdx.input.getY());
        if(Intersector.intersectRayPlane(r, editPlane, intersection)) {
            // round intersection point to the grid
            intersection.x = (int)intersection.x;
            intersection.z = (int)intersection.z;

            // Add a new vertex when clicked
            if(Gdx.input.justTouched()) {

                // Start a new sector if not currently editing one
                if(current == null) {
                    // This sector might have a parent sector
                    Sector parent = pickSector(new Vector2(intersection.x, intersection.z));
                    current = new Sector();

                    if(parent != null)
                        parent.addSubSector(current);
                    else
                        sectors.add(current);
                }

                addVertex(intersection.x, intersection.z);

                refreshSector();
            }

            // Finish current sector
            if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {

                Vector2 startPoint = current.getPoints().first();
                Vector2 lastPoint = current.getPoints().get(current.getPoints().size - 1);

                if(!lastPoint.equals(startPoint))
                    addVertex(startPoint.x, startPoint.y);

                current = null;

                refreshSector();
            }
         }
    }

    public void addVertex(float x, float y) {
        if(current != null) {
            Vector2 next = new Vector2(x, y);
            current.addVertex(next);

            Vector2 previous = current.points.get(current.points.size - 2);
            lines.add(new Line(previous, next, current.parent == null, current, current.parent));
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

            batch.begin(camera);
            batch.render(models);
            batch.end();

            renderGrid();

            for(Sector sector : sectors) {
                renderSectorWireframe(sector);
                renderPoints(sector);
                renderWalls();
            }

            renderNextLine();
            renderNextPoint();
        }
        catch(Throwable t) {
            Gdx.app.log("Error", t.getMessage());
        }
	}

    public void renderSectorWireframe(Sector s) {
        Array<Vector2> points = s.getPoints();
        if(points.size >= 2) {
            lineRenderer.begin(ShapeRenderer.ShapeType.Line);
            lineRenderer.setColor(Color.WHITE);
            for (int i = 0; i < points.size - 1; i++) {
                Vector2 startPoint = points.get(i);
                Vector2 endPoint = points.get(i + 1);

                tempVec3.set(startPoint.x, 0, startPoint.y);
                tempVec3_2.set(endPoint.x, 0, endPoint.y);

                lineRenderer.line(tempVec3, tempVec3_2);
            }
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
                lineRenderer.line(tempVec3.set(endPoint.x, 0, endPoint.y), intersection);
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
        lineRenderer.setColor(Color.WHITE);

        for(Line line : lines) {
            if(line.solid) {
                for (int i = 1; i < 10; i++) {
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
        lineRenderer.box(intersection.x - pointSize / 2, intersection.y, intersection.z + pointSize / 2, pointSize, pointSize / 3, pointSize);
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
}
