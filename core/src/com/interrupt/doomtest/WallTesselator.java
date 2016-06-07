package com.interrupt.doomtest;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class WallTesselator {

    public static Model tesselate(Array<Line> walls) {

        Array<Line> linesToDraw = getSolidLines(walls);

        VertexAttributes attributes = new VertexAttributes(VertexAttribute.Position(), VertexAttribute.TexCoords(0));

        MeshBuilder meshBuilder = new MeshBuilder();
        meshBuilder.begin(attributes);
        meshBuilder.addMesh(getVertices(linesToDraw), getIndices(linesToDraw));
        Mesh mesh = meshBuilder.end();

        Texture texture = new Texture(Gdx.files.internal("textures/wall1.png"));
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        Material material = new Material(ColorAttribute.createDiffuse(Color.WHITE), TextureAttribute.createDiffuse(texture), IntAttribute.createCullFace(GL20.GL_FALSE));

        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.part("0", mesh, GL20.GL_TRIANGLES, material);

        return mb.end();
    }

    private static Array<Line> getSolidLines(Array<Line> walls) {
        Array<Line> filtered = new Array<Line>();
        for(Line wall : walls) {
            if(wall.solid) filtered.add(wall);
        }
        return filtered;
    }

    private static Array<Vector3> getWallVerts(Line line) {
        Array<Vector3> wallVerts = new Array<Vector3>();
        wallVerts.add(new Vector3(line.start.x, 0, line.start.y));
        wallVerts.add(new Vector3(line.start.x, 2, line.start.y));
        wallVerts.add(new Vector3(line.end.x, 0, line.end.y));
        wallVerts.add(new Vector3(line.end.x, 2, line.end.y));
        return wallVerts;
    }

    private static Array<Vector2> getWallUVs(Line line) {
        float lineLength = line.getLength();
        Array<Vector2> wallUVs = new Array<Vector2>();
        wallUVs.add(new Vector2(0, 1));
        wallUVs.add(new Vector2(0, 0));
        wallUVs.add(new Vector2(lineLength * 0.5f, 1));
        wallUVs.add(new Vector2(lineLength * 0.5f, 0));
        return wallUVs;
    }

    private static float[] getVertices(Array<Line> walls) {
        // 4 vertices, 5 attributes each
        float[] vertices = new float[walls.size * 20];
        int indx = 0;

        for(int i = 0; i < walls.size; i++) {
            Line wall = walls.get(i);
            Array<Vector3> wallVerts = getWallVerts(wall);
            Array<Vector2> uvs = getWallUVs(wall);
            int uvi = 0;

            for(Vector3 v : wallVerts) {
                // position
                vertices[indx++] = v.x;
                vertices[indx++] = v.y;
                vertices[indx++] = v.z;

                // UV
                Vector2 uv = uvs.get(uvi++);
                vertices[indx++] = uv.x;
                vertices[indx++] = uv.y;
            }
        }

        return vertices;
    }

    private static short[] getIndices(Array<Line> walls) {
        // turn 4 vertices into 6
        short[] indices = new short[walls.size * 6];
        int indx = 0;
        for(int i = 0; i < walls.size; i++) {
            indices[indx++] = (short)(i * 4);
            indices[indx++] = (short)(i * 4 + 1);
            indices[indx++] = (short)(i * 4 + 2);
            indices[indx++] = (short)(i * 4 + 1);
            indices[indx++] = (short)(i * 4 + 2);
            indices[indx++] = (short)(i * 4 + 3);
        }
        return indices;
    }
}
