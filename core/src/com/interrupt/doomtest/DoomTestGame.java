package com.interrupt.doomtest;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.utils.Array;
import org.lwjgl.util.glu.GLUtessellator;

import static org.lwjgl.util.glu.GLU.*;


public class DoomTestGame extends ApplicationAdapter {

    ModelBatch b;
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
        b = new ModelBatch();

        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 0f, -5f);
        camera.lookAt(0,0,0);
        camera.near = 1f;
        camera.far = 300f;
        camera.update();

        models.add(new ModelInstance(tesselate()));

        camController = new CameraInputController(camera);
        Gdx.input.setInputProcessor(camController);
	}

    public Model tesselate() {
        GLUtessellator tesselator = gluNewTess();

        TessCallback callback = new TessCallback();
        tesselator.gluTessCallback(GLU_TESS_VERTEX, callback);
        tesselator.gluTessCallback(GLU_TESS_BEGIN, callback);
        tesselator.gluTessCallback(GLU_TESS_END, callback);
        tesselator.gluTessCallback(GLU_TESS_COMBINE, callback);

        tesselator.gluTessProperty(GLU_TESS_WINDING_RULE, GLU_TESS_WINDING_NONZERO);
        tesselator.gluTessBeginPolygon(null);

        tesselator.gluTessBeginContour();
        for (int x = 0; x < star.length; x++) //loop through the vertices
        {
            tesselator.gluTessVertex(star[x], 0, new VertexData(star[x])); //store the vertex
        }
        tesselator.gluTessEndContour();

        tesselator.gluTessBeginContour();
        for (int x = 0; x < quad.length; x++) //loop through the vertices
        {
            tesselator.gluTessVertex(quad[x], 0, new VertexData(quad[x])); //store the vertex
        }
        tesselator.gluTessEndContour();

        tesselator.gluEndPolygon();

        tesselator.gluDeleteTess();

        return callback.getModel();
    }

	@Override
	public void render () {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        camController.update();

        b.begin(camera);
        b.render(models);
        b.end();
	}
}
