package com.interrupt.doomtest;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.FloatArray;
import com.interrupt.doomtest.gfx.VertexData;

public class WallTesselator {

    public static Model tesselate(Array<Line> walls) {

        Array<Line> linesToDraw = getVisibleLines(walls);

        VertexAttributes attributes = new VertexAttributes(VertexAttribute.Position(), VertexAttribute.TexCoords(0));

        ModelBuilder mb = new ModelBuilder();
        mb.begin();

        ArrayMap<Material, FloatArray> verts = getVertices(linesToDraw).getData();

        // Make meshes by material
        for(int i = 0; i < verts.size; i++) {
            Material material = verts.getKeyAt(i);
            FloatArray fa = verts.get(material);
            float[] vertices = fa.toArray();

            MeshBuilder meshBuilder = new MeshBuilder();
            meshBuilder.begin(attributes);

            meshBuilder.addMesh(vertices, getIndices(vertices));
            Mesh mesh = meshBuilder.end();

            mb.part(Integer.toString(i), mesh, GL20.GL_TRIANGLES, material);
        }

        return mb.end();
    }

    private static Array<Line> getVisibleLines(Array<Line> walls) {
        return walls;
    }

    private static Array<Vector3> getUpperWallVerts(Line line) {
        Array<Vector3> wallVerts = new Array<Vector3>();

        if(!line.solid && line.right != null && line.left.getCeilingHeight() != line.right.getCeilingHeight()) {
            wallVerts.add(new Vector3(line.start.x, line.right.getCeilingHeight(), line.start.y));
            wallVerts.add(new Vector3(line.start.x, line.left.getCeilingHeight(), line.start.y));
            wallVerts.add(new Vector3(line.end.x, line.right.getCeilingHeight(), line.end.y));
            wallVerts.add(new Vector3(line.end.x, line.left.getCeilingHeight(), line.end.y));
        }

        return wallVerts;
    }

    private static Array<Vector3> getLowerWallVerts(Line line) {
        Array<Vector3> wallVerts = new Array<Vector3>();

        if(line.solid) {
            addSolidWallVerts(line, wallVerts);
        }
        else {
            if (line.left.getFloorHeight() != line.right.getFloorHeight()) {
                wallVerts.add(new Vector3(line.start.x, line.left.getFloorHeight(), line.start.y));
                wallVerts.add(new Vector3(line.start.x, line.right.getFloorHeight(), line.start.y));
                wallVerts.add(new Vector3(line.end.x, line.left.getFloorHeight(), line.end.y));
                wallVerts.add(new Vector3(line.end.x, line.right.getFloorHeight(), line.end.y));
            }
        }

        return wallVerts;
    }

    private static void addSolidWallVerts(Line line, Array<Vector3> wallVerts) {
        Sector in = line.left;
        if(line.left.isSolid && line.right != null) {
            in = line.right;
        }

        // point wall outside, if there is a parent
        if(line.right == null || line.right.isSolid) {
            wallVerts.add(new Vector3(line.start.x, in.getFloorHeight(), line.start.y));
            wallVerts.add(new Vector3(line.start.x, in.getCeilingHeight(), line.start.y));
            wallVerts.add(new Vector3(line.end.x, in.getFloorHeight(), line.end.y));
            wallVerts.add(new Vector3(line.end.x, in.getCeilingHeight(), line.end.y));
        }
        else {
            wallVerts.add(new Vector3(line.end.x, in.getFloorHeight(), line.end.y));
            wallVerts.add(new Vector3(line.end.x, in.getCeilingHeight(), line.end.y));
            wallVerts.add(new Vector3(line.start.x, in.getFloorHeight(), line.start.y));
            wallVerts.add(new Vector3(line.start.x, in.getCeilingHeight(), line.start.y));
        }
    }

    private static Array<Vector2> getLowerWallUVs(Line line) {
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

        if(sectorHeight != 0) {
            if(!line.solid && line.right != null && line.left.getFloorHeight() > line.right.getFloorHeight()) {
                startV = sectorHeight;
                endV = 0;
            }

            wallUVs.add(new Vector2(0, startV));
            wallUVs.add(new Vector2(0, endV));
            wallUVs.add(new Vector2(lineLength * 0.5f, startV));
            wallUVs.add(new Vector2(lineLength * 0.5f, endV));
        }

        return wallUVs;
    }

    private static Array<Vector2> getUpperWallUVs(Line line) {
        float lineLength = line.getLength();

        float sectorHeight = line.left.getCeilingHeight() - line.left.getFloorHeight();
        if(line.left.isSolid && line.right != null) sectorHeight = line.right.getCeilingHeight() - line.right.getFloorHeight();

        if(!line.solid && line.right != null) {
            sectorHeight = line.right.getFloorHeight() - line.left.getFloorHeight();
        }

        sectorHeight = Math.abs(sectorHeight) * -0.5f;

        Array<Vector2> wallUVs = new Array<Vector2>();

        if(!line.solid && line.right != null && line.left.getCeilingHeight() != line.right.getCeilingHeight()) {
            float offset = sectorHeight;

            if(!line.solid && line.right != null) {
                if(line.right.getCeilingHeight() < line.left.getCeilingHeight()) {
                    offset = (line.right.getFloorHeight() - line.left.getFloorHeight()) * -0.5f;
                }
                else {
                    offset = (line.left.getFloorHeight() - line.right.getFloorHeight()) * -0.5f;
                }
                if(line.right.getCeilingHeight() < line.left.getCeilingHeight()) {
                    offset += (line.right.getCeilingHeight() - line.right.getFloorHeight()) * -0.5f;
                }
                else {
                    offset += (line.left.getCeilingHeight() - line.left.getFloorHeight()) * -0.5f;
                }
            }

            sectorHeight = Math.abs(line.right.getCeilingHeight() - line.left.getCeilingHeight()) * -0.5f;
            float startV = 0 + offset;
            float endV = sectorHeight + offset;

            if(line.right.getCeilingHeight() > line.left.getCeilingHeight()) {
                endV = 0 + offset;
                startV = sectorHeight + offset;
            }

            wallUVs.add(new Vector2(0, startV));
            wallUVs.add(new Vector2(0, endV));
            wallUVs.add(new Vector2(lineLength * 0.5f, startV));
            wallUVs.add(new Vector2(lineLength * 0.5f, endV));
        }

        return wallUVs;
    }

    private static VertexData getVertices(Array<Line> walls) {
        // 4 vertices, 5 attributes each
        //float[] vertices = new float[walls.size * 20];
        //int indx = 0;

        VertexData vertexData = new VertexData();

        for(int i = 0; i < walls.size; i++) {

            Line wall = walls.get(i);

            Array<Vector3> lowerWallVerts = getLowerWallVerts(wall);
            if(lowerWallVerts.size > 0) {
                FloatArray vertices = vertexData.getVerticesByMaterial(wall.lowerMaterial);
                Array<Vector2> uvs = getLowerWallUVs(wall);
                int uvi = 0;

                for (Vector3 v : lowerWallVerts) {
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

            Array<Vector3> upperWallVerts = getUpperWallVerts(wall);
            if(upperWallVerts.size > 0) {
                FloatArray vertices = vertexData.getVerticesByMaterial(wall.lowerMaterial);
                Array<Vector2> uvs = getUpperWallUVs(wall);
                int uvi = 0;

                for (Vector3 v : upperWallVerts) {
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

        return vertexData;
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
