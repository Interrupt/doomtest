package com.interrupt.doomtest.gfx.renderer;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.utils.Array;
import com.interrupt.doomtest.gfx.WallTesselator;
import com.interrupt.doomtest.levels.Level;
import com.interrupt.doomtest.levels.Sector;

public class RendererFrontend {
    private RendererBackend renderer = new RendererBackend();
    private Array<ModelInstance> levelModels = new Array<ModelInstance>();

    public void setLevel(Level level) {
        levelModels.clear();

        for(Sector sector : level.sectors) {
            // turn the sector into a model
            levelModels.addAll(sector.tesselate());
        }

        // walls!
        levelModels.add(new ModelInstance(WallTesselator.tesselate(level.lines)));
    }

    public void render(Camera camera) {
        renderer.begin();
        renderer.draw(levelModels);
        renderer.end(camera);
    }

    public Array<ModelInstance> getLevelModels() {
        return levelModels;
    }
}
