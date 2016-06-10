package com.interrupt.doomtest;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;

public class WallTesselator {

    public static Model tesselate(Array<Line> walls) {

        Array<Line> linesToDraw = getVisibleLines(walls);

        VertexAttributes attributes = new VertexAttributes(VertexAttribute.Position(), VertexAttribute.TexCoords(0));

        // texture
        Texture texture = new Texture(Gdx.files.internal("textures/wall1.png"));
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        Material material = new Material(ColorAttribute.createDiffuse(Color.WHITE), TextureAttribute.createDiffuse(texture));

        // middle or lower wall
        MeshBuilder meshBuilder = new MeshBuilder();
        meshBuilder.begin(attributes);
        float[] verts = getVertices(linesToDraw);
        meshBuilder.addMesh(verts, getIndices(verts));
        Mesh mesh = meshBuilder.end();

        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.part("0", mesh, GL20.GL_TRIANGLES, material);

        return mb.end();
    }

    private static Array<Line> getVisibleLines(Array<Line> walls) {
        return walls;
    }

    private static Array<Vector3> getWallVerts(Line line) {
        Array<Vector3> wallVerts = new Array<Vector3>();
        if(line.solid) {
            addSolidWallVerts(wallVerts, line);
        }
        else if(line.right != null) {
            if (line.left.getFloorHeight() != line.right.getFloorHeight()) {
                wallVerts.add(new Vector3(line.start.x, line.left.getFloorHeight(), line.start.y));
                wallVerts.add(new Vector3(line.start.x, line.right.getFloorHeight(), line.start.y));
                wallVerts.add(new Vector3(line.end.x, line.left.getFloorHeight(), line.end.y));
                wallVerts.add(new Vector3(line.end.x, line.right.getFloorHeight(), line.end.y));
            }
            if(line.left.getCeilingHeight() != line.right.getCeilingHeight()) {
                wallVerts.add(new Vector3(line.start.x, line.right.getCeilingHeight(), line.start.y));
                wallVerts.add(new Vector3(line.start.x, line.left.getCeilingHeight(), line.start.y));
                wallVerts.add(new Vector3(line.end.x, line.right.getCeilingHeight(), line.end.y));
                wallVerts.add(new Vector3(line.end.x, line.left.getCeilingHeight(), line.end.y));
            }
        }
        return wallVerts;
    }

    private static void addSolidWallVerts(Array<Vector3> wallVerts, Line line) {
        Sector in = line.left;
        if(line.left.isSolid && line.right != null) {
            in = line.right;
        }
        wallVerts.add(new Vector3(line.start.x, in.getFloorHeight(), line.start.y));
        wallVerts.add(new Vector3(line.start.x, in.getCeilingHeight(), line.start.y));
        wallVerts.add(new Vector3(line.end.x, in.getFloorHeight(), line.end.y));
        wallVerts.add(new Vector3(line.end.x, in.getCeilingHeight(), line.end.y));
    }

    private static Array<Vector2> getWallUVs(Line line) {
        float lineLength = line.getLength();

        float sectorHeight = line.left.getCeilingHeight() - line.left.getFloorHeight();
        if(line.left.isSolid && line.right != null) sectorHeight = line.right.getCeilingHeight() - line.right.getFloorHeight();

        if(!line.solid && line.right != null) {
            sectorHeight = line.right.getFloorHeight() - line.left.getFloorHeight();
        }

        sectorHeight = Math.abs(sectorHeight) * -0.5f;

        Array<Vector2> wallUVs = new Array<Vector2>();
        float startV = 0;
        float endV = sectorHeight;

        if(!line.solid && line.right != null && line.left.getFloorHeight() > line.right.getFloorHeight()) {
            startV = sectorHeight;
            endV = 0;
        }

        wallUVs.add(new Vector2(0, startV));
        wallUVs.add(new Vector2(0, endV));
        wallUVs.add(new Vector2(lineLength * 0.5f, startV));
        wallUVs.add(new Vector2(lineLength * 0.5f, endV));

        if(!line.solid && line.right != null && line.left.getCeilingHeight() != line.right.getCeilingHeight()) {
            sectorHeight = Math.abs(line.right.getCeilingHeight() - line.left.getCeilingHeight()) * -0.5f;
            startV = 0;
            endV = sectorHeight;

            if(line.right.getCeilingHeight() > line.left.getCeilingHeight()) {
                endV = 0;
                startV = sectorHeight;
            }

            wallUVs.add(new Vector2(0, startV));
            wallUVs.add(new Vector2(0, endV));
            wallUVs.add(new Vector2(lineLength * 0.5f, startV));
            wallUVs.add(new Vector2(lineLength * 0.5f, endV));
        }

        return wallUVs;
    }

    private static float[] getVertices(Array<Line> walls) {
        // 4 vertices, 5 attributes each
        //float[] vertices = new float[walls.size * 20];
        //int indx = 0;

        FloatArray vertices = new FloatArray();

        for(int i = 0; i < walls.size; i++) {
            Line wall = walls.get(i);
            Array<Vector3> wallVerts = getWallVerts(wall);
            if(wallVerts.size > 0) {
                Array<Vector2> uvs = getWallUVs(wall);
                int uvi = 0;

                for (Vector3 v : wallVerts) {
                    // position
                    vertices.add(v.x);
                    vertices.add(v.y);
                    vertices.add(v.z);

                    // UV
                    Vector2 uv = uvs.get(uvi++);
                    vertices.add(uv.x);
                    vertices.add(uv.y);
                }
            }
        }

        return vertices.toArray();
    }

    private static short[] getIndices(float[] vertices) {
        // turn 4 vertices into 6
        int numWalls = vertices.length / 20;
        short[] indices = new short[numWalls * 6];
        int indx = 0;
        for(int i = 0; i < numWalls; i++) {
            indices[indx++] = (short)(i * 4);
            indices[indx++] = (short)(i * 4 + 1);
            indices[indx++] = (short)(i * 4 + 2);
            indices[indx++] = (short)(i * 4 + 3);
            indices[indx++] = (short)(i * 4 + 2);
            indices[indx++] = (short)(i * 4 + 1);
        }
        return indices;
    }
}
