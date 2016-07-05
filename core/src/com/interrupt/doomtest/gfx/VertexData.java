package com.interrupt.doomtest.gfx;

import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.FloatArray;

public class VertexData {

    public VertexData() { }

    ArrayMap<Material, FloatArray> verticesByMaterial = new ArrayMap<Material, FloatArray>();

    public FloatArray getVerticesByMaterial(Material material) {
        FloatArray existing = verticesByMaterial.get(material);
        if(existing != null) return existing;

        FloatArray newVerts = new FloatArray();
        verticesByMaterial.put(material, newVerts);

        return newVerts;
    }

    public ArrayMap<Material, FloatArray> getData() {
        return verticesByMaterial;
    }
}