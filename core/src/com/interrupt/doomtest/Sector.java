package com.interrupt.doomtest;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.lwjgl.util.glu.GLUtessellator;

import java.util.Random;

import static org.lwjgl.util.glu.GLU.*;

public class Sector {
    Array<Vector2> points = new Array<Vector2>();

    public Sector parent = null;
    public Array<Sector> subsectors = new Array<Sector>();

    Material material = new Material(ColorAttribute.createDiffuse(Color.GRAY), IntAttribute.createCullFace(GL20.GL_FALSE));

    private static GLUtessellator tesselator = gluNewTess();
    TessCallback callback = new TessCallback(material);

    public boolean isSolid = false;

    public Sector() {
        Random r = new Random();
        material = new Material(ColorAttribute.createDiffuse(r.nextFloat(), r.nextFloat(), r.nextFloat(), 1f), IntAttribute.createCullFace(GL20.GL_FALSE));
        callback.material = material;
    }

    public Sector(boolean isSolid) {
        super();
        this.isSolid = isSolid;
    }

    public void addVertex(float x, float y) {
        points.add(new Vector2(x, y));
    }

    public void addVertex(Vector2 vertex) {
        addVertex(vertex.x, vertex.y);
    }

    public void addSubSector(Sector s) {
        subsectors.add(s);
        s.parent = this;
    }

    public Array<Vector2> getPoints() {
        return points;
    }

    public Model tesselate() {
        if(isSolid) return null;

        tesselator.gluTessCallback(GLU_TESS_VERTEX, callback);
        tesselator.gluTessCallback(GLU_TESS_BEGIN, callback);
        tesselator.gluTessCallback(GLU_TESS_END, callback);
        tesselator.gluTessCallback(GLU_TESS_COMBINE, callback);

        tesselator.gluTessProperty(GLU_TESS_WINDING_RULE, GLU_TESS_WINDING_ODD);
        tesselator.gluTessBeginPolygon(null);

        tesselateContour(this, tesselator, callback);

        tesselator.gluEndPolygon();

        Model built = callback.getModel();

        if(subsectors.size > 0) {
            ModelBuilder mb = new ModelBuilder();
            mb.begin();
            mb.node("0", built);
            for (Sector subsector : subsectors) {
                Model m = subsector.tesselate();
                if(m != null)
                    mb.node(new Random().nextInt() + "", m);
            }
            return mb.end();
        }

        return built;
    }

    private void tesselateContour(Sector sector, GLUtessellator tesselator, TessCallback callback) {
        // Tesselate the current contour
        Array<Vector2> vertices = sector.getPoints();
        tesselator.gluTessBeginContour();
        for (int x = 0; x < vertices.size; x++) //loop through the vertices
        {
            double[] data = vertexToDoubles(vertices.get(x));
            tesselator.gluTessVertex(data, 0, new VertexData(data)); //store the avertex
        }
        tesselator.gluTessEndContour();

        // Now, carve out all the sub sectors
        for(int i = 0; i < sector.subsectors.size; i++) {
            tesselateContour(sector.subsectors.get(i), tesselator, callback);
        }
    }

    private double[] vertexToDoubles(Vector2 vertex) {
        double[] data = new double[3];
        data[0] = vertex.x;
        data[1] = 0;
        data[2] = vertex.y;
        return data;
    }

    public Sector getSectorOfPoint(Vector2 point) {
        Sector inSector = null;
        if(Intersector.isPointInPolygon(getPoints(), point)) {
            // this point is IN this sector, know it's at least here
            inSector = this;

            // might also be in one of the subsectors of this sector
            for(Sector subsector : subsectors) {
                Sector found = subsector.getSectorOfPoint(point);
                if(found != null) {
                    inSector = found;
                }
            }
        }

        return inSector;
    }
}
