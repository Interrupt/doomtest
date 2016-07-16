package com.interrupt.doomtest.levels;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.interrupt.doomtest.gfx.Art;

public class Surface {
    public String texture;
    public int culling = GL20.GL_BACK;

    public Surface() { }

    public Surface(String texture) {
        this.texture = texture;
    }

    public Surface(String texture, int culling) {
        this.texture = texture;
        this.culling = culling;
    }

    public Material createMaterial(String id) {
        return new Material(id,
                ColorAttribute.createDiffuse(Color.WHITE),
                TextureAttribute.createDiffuse(getTextureRegion()),
                IntAttribute.createCullFace(culling));
    }

    public TextureRegion getTextureRegion() {
        return new TextureRegion(Art.getTexture(texture));
    }

    public void match(Surface other) {
        texture = other.texture;
    }
}
