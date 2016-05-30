package com.interrupt.doomtest;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.utils.Array;


public class DoomTestGame extends ApplicationAdapter {

    ModelBatch batch;
    Camera camera;
    CameraInputController camController;

    public Array<ModelInstance> models = new Array<ModelInstance>();

    // first polygon: a star-5 vertices and color information
    double star[][] = { {0.6f,  -0.1f, 0f, 1.0f, 1.0f, 1.0f},
            {1.35f, 1.4f, 0f, 1.0f, 1.0f, 1.0f},
            {2.1f,  -0.1f, 0f, 1.0f, 1.0f, 1.0f},
            {0.6f, 0.9f, 0f, 1.0f, 1.0f, 1.0f},
            {2.1f, 0.9f, 0f, 1.0f, 1.0f, 1.0f} };

    //second polygon: a quad-4 vertices; first contour
    double quad[][] = { {0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f},
            {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f},
            {1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f},
            {0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f} };

	@Override
	public void create () {
        batch = new ModelBatch();

        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 0f, -5f);
        camera.lookAt(0,0,0);
        camera.near = 1f;
        camera.far = 300f;
        camera.update();

        // A camera that can be driven around
        camController = new CameraInputController(camera);
        Gdx.input.setInputProcessor(camController);

        // Make the star sector
        Sector sector = new Sector();
        sector.setMaterial(new Material(ColorAttribute.createDiffuse(Color.RED)));
        for(int x = 0; x < star.length; x++) {
            sector.addVertex((float)star[x][0], (float)star[x][1], (float)star[x][2]);
        }

        // Add a quad sub sector to the star
        Sector subsector = new Sector();
        for(int x = 0; x < quad.length; x++) {
            subsector.addVertex((float)quad[x][0], (float)quad[x][1], (float)quad[x][2]);
        }

        sector.addSubSector(subsector);

        // turn the sector into a model
        models.add(new ModelInstance(sector.tesselate()));
	}

	@Override
	public void render () {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        camController.update();

        batch.begin(camera);
        batch.render(models);
        batch.end();
	}
}
