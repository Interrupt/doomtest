package com.interrupt.doomtest;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
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

    Texture texture = new Texture(Gdx.files.internal("textures/floor1.png"));
    Material material = new Material(ColorAttribute.createDiffuse(Color.WHITE), TextureAttribute.createDiffuse(texture), IntAttribute.createCullFace(GL20.GL_FALSE));

    private static GLUtessellator tesselator = gluNewTess();
    TessCallback callback = new TessCallback(material);

    public boolean isSolid = false;

    public Sector() {
        //Random r = new Random();
        //material = new Material(ColorAttribute.createDiffuse(r.nextFloat(), r.nextFloat(), r.nextFloat(), 1f), IntAttribute.createCullFace(GL20.GL_FALSE));
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        callback.material = material;
    }

    public Sector(boolean isSolid) {
        super();
        this.isSolid = isSolid;
    }

    public void addVertex(Vector2 vertex) {
        points.add(vertex);
    }

    public void addSubSector(Sector s) {
        subsectors.add(s);
        s.parent = this;
    }

    public Array<Vector2> getPoints() {
        return points;
    }

    public boolean hasVertex(Vector2 point) {
        for(Vector2 p : points) {
            if(p.x == point.x && p.y == point.y)
                return true;
        }

        // might also be in one of the subsectors
        for(Sector subsector : subsectors) {
            if(subsector.hasVertex(point))
                return true;
        }

        return false;
    }

    public Model tesselate() {

        tesselator.gluTessCallback(GLU_TESS_VERTEX, callback);
        tesselator.gluTessCallback(GLU_TESS_BEGIN, callback);
        tesselator.gluTessCallback(GLU_TESS_END, callback);
        tesselator.gluTessCallback(GLU_TESS_COMBINE, callback);

        tesselator.gluTessProperty(GLU_TESS_WINDING_RULE, GLU_TESS_WINDING_ODD);

        if (!isSolid) {
            tesselator.gluTessBeginPolygon(null);

            // Carve the main sector
            tesselateContour(this, tesselator, callback);

            // Now, carve out all the sub sectors
            for(int i = 0; i < subsectors.size; i++) {
                tesselateContour(subsectors.get(i), tesselator, callback);
            }

            tesselator.gluEndPolygon();
        }

        // Start building the mesh
        ModelBuilder mb = new ModelBuilder();
        mb.begin();

        // Make floor / ceiling
        Model built = callback.getModel();
        mb.node("0", built);

        for (Sector subsector : subsectors) {
            Model m = subsector.tesselate();
            if(m != null)
                mb.node(new Random().nextInt() + "", m);
        }

        return mb.end();
    }

    private void tesselateContour(Sector sector, GLUtessellator tesselator, TessCallback callback) {
        // Tesselate the current contour
        Array<Vector2> vertices = sector.getPoints();
        tesselator.gluTessBeginContour();
        for (int x = 0; x < vertices.size; x++) //loop through the vertices
        {
            double[] data = vertexToDoubles(vertices.get(x));
            tesselator.gluTessVertex(data, 0, new VertexData(data)); //store the vertex
        }
        tesselator.gluTessEndContour();
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
        if(isPointInside(point)) {
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

    public boolean isPointInside(Vector2 point) {
        return Intersector.isPointInPolygon(getPoints(), point);
    }

    public void translate(float x, float y) {
        for(Vector2 point : getPoints()) {
            point.add(x, y);
        }

        for(Sector s : subsectors) {
            s.translate(x, y);
        }
    }
}
