package com.interrupt.doomtest.gfx.renderer;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.utils.Array;

public class RendererBackend {
    public Array<ModelInstance> models = new Array<ModelInstance>();

    ModelBatch batch = new ModelBatch();

    public void begin() {
        models.clear();
    }

    public void draw(Array<ModelInstance> toDraw) {
        models.addAll(toDraw);
    }

    public void draw(ModelInstance toDraw) {
        models.add(toDraw);
    }

    public void end(Camera camera) {
        render(camera);
    }

    private void render(Camera camera) {
        if(models.size > 0) {
            batch.begin(camera);
            batch.render(models);
            batch.end();
        }
    }
}