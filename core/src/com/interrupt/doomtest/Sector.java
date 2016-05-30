package com.interrupt.doomtest;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import org.lwjgl.util.glu.GLUtessellator;

import static org.lwjgl.util.glu.GLU.*;

public class Sector {
    Array<Vector3> points = new Array<Vector3>();
    public Array<Sector> subsectors = new Array<Sector>();
    Material material = new Material(ColorAttribute.createDiffuse(Color.GRAY), IntAttribute.createCullFace(GL20.GL_FALSE));


    public void addVertex(float x, float y, float z) {
        points.add(new Vector3(x, y, z));
    }

    public void addSubSector(Sector s) {
        subsectors.add(s);
    }

    public Array<Vector3> getPoints() {
        return points;
    }

    public Model tesselate() {
        GLUtessellator tesselator = gluNewTess();

        TessCallback callback = new TessCallback(material);
        tesselator.gluTessCallback(GLU_TESS_VERTEX, callback);
        tesselator.gluTessCallback(GLU_TESS_BEGIN, callback);
        tesselator.gluTessCallback(GLU_TESS_END, callback);
        tesselator.gluTessCallback(GLU_TESS_COMBINE, callback);

        tesselator.gluTessProperty(GLU_TESS_WINDING_RULE, GLU_TESS_WINDING_ODD);
        tesselator.gluTessBeginPolygon(null);

        tesselateContour(this, tesselator, callback);

        tesselator.gluEndPolygon();

        tesselator.gluDeleteTess();

        return callback.getModel();
    }

    private void tesselateContour(Sector sector, GLUtessellator tesselator, TessCallback callback) {
        // Tesselate the current contour
        Array<Vector3> vertices = sector.getPoints();
        tesselator.gluTessBeginContour();
        for (int x = 0; x < vertices.size; x++) //loop through the vertices
        {
            double[] data = vertexToDoubles(vertices.get(x));
            tesselator.gluTessVertex(data, 0, new VertexData(data)); //store the vertex
        }
        tesselator.gluTessEndContour();

        // Now, carve out all the sub sectors
        for(int i = 0; i < sector.subsectors.size; i++) {
            tesselateContour(sector.subsectors.get(i), tesselator, callback);
        }
    }

    private double[] vertexToDoubles(Vector3 vertex) {
        double[] data = new double[3];
        data[0] = vertex.x;
        data[1] = vertex.y;
        data[2] = vertex.z;
        return data;
    }
}
