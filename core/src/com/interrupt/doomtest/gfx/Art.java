package com.interrupt.doomtest.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.ArrayMap;

public class Art {

    public static ArrayMap<String, Texture> loadedTextures = new ArrayMap<String, Texture>();

    public static Texture getTexture(String filename) {
        // Use the cached texture if already loaded
        if(loadedTextures.containsKey(filename)) return loadedTextures.get(filename);

        // Load the texture
        Texture texture = new Texture(Gdx.files.internal(filename));
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        loadedTextures.put(filename, texture);

        return texture;
    }
}
