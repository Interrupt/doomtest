package com.interrupt.doomtest;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Array;
import org.lwjgl.util.glu.GLUtessellatorCallbackAdapter;

public class TessCallback extends GLUtessellatorCallbackAdapter {

    public Array<VertexData> data = new Array<VertexData>();
    public ModelBuilder modelBuilder = new ModelBuilder();
    Material material = new Material(ColorAttribute.createDiffuse(Color.WHITE));

    int currentType;
    int parts = 0;

    public TessCallback() {
        modelBuilder.begin();
    }

    public TessCallback(Material material) {
        modelBuilder.begin();
        this.material = material;
    }

    public void begin(int type) {
        currentType = type;
        parts++;
    }

    public void combine(double[] coords, Object[] data, float[] weight, Object[] outData) {
        for (int i=0;i<outData.length;i++) {
            double[] combined = new double[6];
            combined[0] = coords[0];
            combined[1] = coords[1];
            combined[2] = coords[2];
            combined[3] = 1;
            combined[4] = 1;
            combined[5] = 1;

            outData[i] = new VertexData(combined);
        }
    }

    public void end() {
        VertexAttributes attributes = new VertexAttributes(VertexAttribute.Position(), VertexAttribute.TexCoords(0));

        MeshBuilder meshBuilder = new MeshBuilder();
        meshBuilder.begin(attributes);
        meshBuilder.addMesh(getVertices(), getIndices());
        Mesh mesh = meshBuilder.end();

        modelBuilder.part(parts + "", mesh, currentType, material);

        data.clear();
    }

    public void vertex(Object vertexData) {
        VertexData vertex = (VertexData) vertexData;
        data.add(vertex);

        /*glVertex3d(vertex.data[0], vertex.data[1], vertex.data[2]);
        glColor3d(vertex.data[3], vertex.data[4], vertex.data[5]);*/
    }

    private float[] getVertices() {
        float[] vertices = new float[data.size * 5];
        for(int i = 0; i < data.size; i++) {
            VertexData vertex = data.get(i);

            // position
            vertices[i * 5] = (float)vertex.data[0];
            vertices[i * 5 + 1] = (float)vertex.data[1];
            vertices[i * 5 + 2] = (float)vertex.data[2];

            // U
            vertices[i * 5 + 3] = (float)vertex.data[0] * 0.5f;
            vertices[i * 5 + 4] = (float)vertex.data[2] * 0.5f;

            // V
            //vertices[i * 3 + 3] = (float)vertex.data[5];
            //vertices[i * 3 + 4] = (float)vertex.data[6];
        }
        return vertices;
    }

    private short[] getIndices() {
        short[] indices = new short[data.size];
        for(int i = 0; i < data.size; i++) {
            indices[i] = (short)i;
        }
        return indices;
    }

    public Model getModel() {
        Model m = modelBuilder.end();
        modelBuilder.begin();
        return m;
    }
}