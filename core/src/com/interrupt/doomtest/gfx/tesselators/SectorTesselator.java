package com.interrupt.doomtest.gfx.tesselators;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.interrupt.doomtest.gfx.Art;
import com.interrupt.doomtest.levels.Sector;
import org.lwjgl.util.glu.GLUtessellator;

import static org.lwjgl.util.glu.GLU.*;
import static org.lwjgl.util.glu.GLU.GLU_TESS_WINDING_ODD;
import static org.lwjgl.util.glu.GLU.GLU_TESS_WINDING_RULE;

public class SectorTesselator {
    private static GLUtessellator tesselator = gluNewTess();

    public static Array<ModelInstance> tesselate(Sector sector) {

        TessCallback callback = new TessCallback();

        tesselator.gluTessCallback(GLU_TESS_VERTEX, callback);
        tesselator.gluTessCallback(GLU_TESS_BEGIN, callback);
        tesselator.gluTessCallback(GLU_TESS_END, callback);
        tesselator.gluTessCallback(GLU_TESS_COMBINE, callback);

        tesselator.gluTessProperty(GLU_TESS_WINDING_RULE, GLU_TESS_WINDING_ODD);

        tesselator.gluTessNormal(0, 1, 0);

        if (!sector.isSolid) {
            tesselator.gluTessBeginPolygon(null);

            // Carve the main sector
            tesselateContour(sector, tesselator, callback);

            // Now, carve out all the sub sectors
            for(int i = 0; i < sector.subsectors.size; i++) {
                tesselateContour(sector.subsectors.get(i), tesselator, callback);
            }

            tesselator.gluEndPolygon();
        }

        // Start building the mesh
        Array<TessCallback.MeshPiece> meshPieces = callback.meshPieces;

        Model model = makeModelFromMeshPieces(sector, meshPieces);
        ModelInstance mi = new ModelInstance(model);
        mi.transform.setTranslation(0, sector.getFloorHeight(), 0);

        Array<ModelInstance> instances = new Array<ModelInstance>();
        instances.add(mi);

        for (Sector subsector : sector.subsectors) {
            instances.addAll(tesselate(subsector));
        }

        return instances;
    }

    private static Model makeModelFromMeshPieces(Sector sector, Array<TessCallback.MeshPiece> meshPieces) {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.node("floor", makeFloorModel(sector, meshPieces));
        mb.node("ceiling", makeCeilingModel(sector, meshPieces));
        Model built = mb.end();
        built.getNode("ceiling").translation.set(0, sector.ceilHeight - sector.floorHeight, 0);
        return built;
    }

    private static Model makeFloorModel(Sector sector, Array<TessCallback.MeshPiece> meshPieces) {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        int indx = 0;

        Material material = sector.floorMaterial.createMaterial(sector.hashCode() + "_floor");

        for(TessCallback.MeshPiece m : meshPieces) {
            mb.part((indx++) + "", m.mesh, m.drawType, material);
        }
        return mb.end();
    }

    private static Model makeCeilingModel(Sector sector, Array<TessCallback.MeshPiece> meshPieces) {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        int indx = 0;

        Material material = sector.ceilingMaterial.createMaterial(sector.hashCode() + "_ceiling");

        for(TessCallback.MeshPiece m : meshPieces) {
            mb.part((indx++) + "", m.mesh, m.drawType, material);
        }
        return mb.end();
    }

    private static void tesselateContour(Sector sector, GLUtessellator tesselator, TessCallback callback) {
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

    private static double[] vertexToDoubles(Vector2 vertex) {
        double[] data = new double[3];
        data[0] = vertex.x;
        data[1] = 0;
        data[2] = vertex.y;
        return data;
    }
}
