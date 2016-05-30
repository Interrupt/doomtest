package com.interrupt.doomtest;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;


public class DoomTestGame extends ApplicationAdapter {

    ModelBatch batch;
    Camera camera;
    CameraInputController camController;
    ShapeRenderer lineRenderer;
    ShapeRenderer pointRenderer;
    Vector3 intersection = new Vector3();
    public Array<ModelInstance> models = new Array<ModelInstance>();
    public Sector sector;
    public Sector current;
    Plane editPlane = new Plane(Vector3.Y, Vector3.Zero);

	@Override
	public void create () {
        batch = new ModelBatch();

        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 10f, -5f);
        camera.lookAt(0,0,-5f);
        camera.near = 1f;
        camera.far = 300f;
        camera.update();

        // A camera that can be driven around
        camController = new CameraInputController(camera);
        Gdx.input.setInputProcessor(camController);

        lineRenderer = new ShapeRenderer();
        sector = new Sector();
        current = sector;
	}

    public void refreshSector() {
        models.clear();

        // turn the sector into a model
        models.add(new ModelInstance(sector.tesselate()));
    }

    public void update() {
        Ray r = camera.getPickRay(Gdx.input.getX(), Gdx.input.getY());
        if(Intersector.intersectRayPlane(r, editPlane, intersection)) {
            // round to integers
            intersection.x = (int)intersection.x;
            intersection.z = (int)intersection.z;
            if(Gdx.input.justTouched()) {
                current.addVertex(intersection.x, intersection.y, intersection.z);
            }

            if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                current = new Sector();
                sector.addSubSector(current);
            }
         }

        refreshSector();
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

            renderSectorAsLines(sector);
            renderNextLine(sector);
            renderPoints(sector);
        }
        catch(Throwable t) {
            Gdx.app.log("Error", t.getMessage());
        }
	}

    public void renderSectorAsLines(Sector s) {
        Array<Vector3> points = s.getPoints();
        if(points.size >= 2) {
            lineRenderer.begin(ShapeRenderer.ShapeType.Line);
            lineRenderer.setColor(Color.WHITE);
            for (int i = 0; i < points.size - 1; i++) {
                lineRenderer.line(points.get(i), points.get(i + 1));
            }
            lineRenderer.end();
        }

        for(Sector subsector : s.subsectors) {
            renderSectorAsLines(subsector);
        }
    }

    public void renderNextLine(Sector s) {
        Array<Vector3> points = current.getPoints();
        if(points.size > 0) {
            lineRenderer.begin(ShapeRenderer.ShapeType.Line);
            lineRenderer.setColor(Color.YELLOW);
            lineRenderer.line(points.get(points.size - 1), intersection);
            lineRenderer.end();
        }
    }

    public void renderPoints(Sector s) {
        Array<Vector3> points = s.getPoints();
        if(points.size > 0) {
            for(Vector3 point : points) {
                lineRenderer.begin(ShapeRenderer.ShapeType.Filled);
                lineRenderer.setColor(Color.YELLOW);
                lineRenderer.box(point.x - 0.05f, point.y, point.z + 0.05f, 0.1f, 0.01f, 0.1f);
                lineRenderer.end();
            }
        }

        for(Sector subsector : s.subsectors) {
            renderPoints(subsector);
        }
    }
}
