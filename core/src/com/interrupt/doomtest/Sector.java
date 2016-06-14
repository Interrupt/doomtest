package com.interrupt.doomtest;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;
import org.lwjgl.util.glu.GLUtessellator;

import static org.lwjgl.util.glu.GLU.*;

public class Sector {
    Array<Vector2> points = new Array<Vector2>();
    public Sector parent = null;
    public Array<Sector> subsectors = new Array<Sector>();

    public float floorHeight = 0;
    public float ceilHeight = 2;

    private static GLUtessellator tesselator = gluNewTess();

    public boolean isSolid = false;

    Material floorMaterial = new Material(ColorAttribute.createDiffuse(Color.WHITE), TextureAttribute.createDiffuse(getTexture("textures/floor1.png")));
    Material ceilingMaterial = new Material(ColorAttribute.createDiffuse(Color.WHITE), TextureAttribute.createDiffuse(getTexture("textures/ceiling1.png")), IntAttribute.createCullFace(GL20.GL_FRONT));

    public Sector() { }

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

    public Texture getTexture(String filename) {
        Texture texture = new Texture(Gdx.files.internal(filename));
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    public Array<ModelInstance> tesselate() {

        TessCallback callback = new TessCallback(floorMaterial);

        tesselator.gluTessCallback(GLU_TESS_VERTEX, callback);
        tesselator.gluTessCallback(GLU_TESS_BEGIN, callback);
        tesselator.gluTessCallback(GLU_TESS_END, callback);
        tesselator.gluTessCallback(GLU_TESS_COMBINE, callback);

        tesselator.gluTessProperty(GLU_TESS_WINDING_RULE, GLU_TESS_WINDING_ODD);

        tesselator.gluTessNormal(0, 1, 0);

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
        Array<TessCallback.MeshPiece> meshPieces = callback.meshPieces;

        Model model = makeModelFromMeshPieces(meshPieces);
        ModelInstance mi = new ModelInstance(model);
        mi.transform.setTranslation(0, getFloorHeight(), 0);

        Array<ModelInstance> instances = new Array<ModelInstance>();
        instances.add(mi);

        for (Sector subsector : subsectors) {
            instances.addAll(subsector.tesselate());
        }

        return instances;
    }

    private Model makeModelFromMeshPieces(Array<TessCallback.MeshPiece> meshPieces) {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.node("floor", makeFloorModel(meshPieces));
        mb.node("ceiling", makeCeilingModel(meshPieces));
        Model built = mb.end();
        built.getNode("ceiling").translation.set(0, ceilHeight - floorHeight, 0);
        return built;
    }

    private Model makeFloorModel(Array<TessCallback.MeshPiece> meshPieces) {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        int indx = 0;
        for(TessCallback.MeshPiece m : meshPieces) {
            mb.part((indx++) + "", m.mesh, m.drawType, floorMaterial);
        }
        return mb.end();
    }

    private Model makeCeilingModel(Array<TessCallback.MeshPiece> meshPieces) {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        int indx = 0;
        Matrix4 transform = new Matrix4().translate(0, ceilHeight - floorHeight, 0);
        for(TessCallback.MeshPiece m : meshPieces) {
            mb.part((indx++) + "", m.mesh, m.drawType, ceilingMaterial);
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

    public Sector getSectorOfSector(Sector other) {
        Sector inSector = null;
        if(isSectorInside(other)) {
            // this point is IN this sector, know it's at least here
            inSector = this;

            // might also be in one of the subsectors of this sector
            for(Sector subsector : subsectors) {
                Sector found = subsector.getSectorOfSector(other);
                if(found != null) {
                    inSector = found;
                }
            }
        }

        return inSector;
    }

    public boolean isPointInside(Vector2 point) {
        if(getPoints().contains(point, false)) return true;
        return Intersector.isPointInPolygon(getPoints(), point);
    }

    public boolean isSectorInside(Sector other) {
        for(Vector2 p : other.getPoints()) {
            if(!isPointInside(p)) return false;
        }
        return true;
    }

    public void translate(float x, float y) {
        for(Vector2 point : getAllPoints()) {
            point.add(x, y);
        }
    }

    public Array<Vector2> getAllPoints() {
        Array<Vector2> all = new Array<Vector2>();
        all.addAll(getPoints());
        for(Sector s : subsectors) {
            for(Vector2 p : s.getAllPoints()) {
                if(!all.contains(p, true)) all.add(p);
            }
        }
        return all;
    }

    public float getFloorHeight() {
        return floorHeight;
    }

    public float getCeilingHeight() {
        return ceilHeight;
    }

    public Array<Sector> split(Plane plane) {
        Plane.PlaneSide lastSide = null;
        Array<Sector> newSectors = new Array<Sector>();
        Sector current = null;
        Integer firstIndex = null;

        // find the first crossing
        // when we do, start adding to a new sector
        // start and end sectors where they cross the plane
        // make new sectors as we cross
        for(int i = 0; i <= points.size; i++) {
            Vector2 p = points.get(i % points.size);
            Vector3 p3 = new Vector3(p.x, 0, p.y);
            Plane.PlaneSide side = plane.testPoint(p3);

            if(lastSide != null && lastSide != side) {
                Vector3 intersection = new Vector3();
                Vector2 last = points.get((i - 1) % points.size);
                if(Intersector.intersectLinePlane(p.x, 0, p.y, last.x, 0, last.y, plane, intersection) >= 0) {
                    if(firstIndex == null)
                        firstIndex = i;

                    Vector2 newPoint = new Vector2(intersection.x, intersection.z);

                    if(current != null)
                        current.addVertex(newPoint);

                    current = new Sector();
                    current.addVertex(newPoint);
                    newSectors.add(current);
                }
            }

            if(current != null)
                current.addVertex(p);

            lastSide = side;
        }

        // fill in to where we started
        if(firstIndex != null && current != null) {
            for(int i = 0; i < firstIndex; i++) {
                Vector2 p = points.get(i);
                current.addVertex(p);
            }
            current.addVertex(newSectors.get(0).getPoints().get(0));
        }

        return newSectors;
    }
}
