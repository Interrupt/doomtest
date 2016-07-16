package com.interrupt.doomtest.levels;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.interrupt.doomtest.gfx.Art;

import javax.xml.soap.Text;

public class Sector {
    public Array<Vector2> points = new Array<Vector2>();
    public Sector parent = null;
    public Array<Sector> subsectors = new Array<Sector>();

    public float floorHeight = 0;
    public float ceilHeight = 8;

    public boolean isSolid = false;

    public transient Material floorMaterial = new Material(this.hashCode() + "_floor", ColorAttribute.createDiffuse(Color.WHITE), TextureAttribute.createDiffuse(Art.getTexture("textures/floor1.png")));
    public transient Material ceilingMaterial = new Material(this.hashCode() + "_ceiling", ColorAttribute.createDiffuse(Color.WHITE), TextureAttribute.createDiffuse(Art.getTexture("textures/ceiling1.png")), IntAttribute.createCullFace(GL20.GL_FRONT));

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
            // this sector is IN this sector, know it's at least here
            inSector = this;

            // might also be in one of the subsectors of this sector
            for(Sector subsector : subsectors) {
                if(subsector != other) {
                    Sector found = subsector.getSectorOfSector(other);
                    if (found != null) {
                        inSector = found;
                    }
                }
            }
        }

        return inSector;
    }

    public boolean isPointInside(Vector2 point) {
        if(points.size == 0) return false;
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

    public Vector2 getExistingVertex(Vector2 vertex, Array<Vector2> vertices) {
        int found = vertices.indexOf(vertex, false);
        if(found >= 0) return vertices.get(found);
        return null;
    }

    public Array<Sector> split(Plane plane, Array<Vector2> worldPoints) {
        Plane.PlaneSide lastSide = null;
        Array<Sector> newSectors = new Array<Sector>();
        Sector current = null;
        Sector lastSector = null;
        Integer firstIndex = null;
        int crossings = 0;

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
                    crossings++;

                    if(firstIndex == null)
                        firstIndex = i;

                    // round to weld verts
                    Vector2 newPoint = new Vector2((int)(intersection.x * 100) / 100f, (int)(intersection.z * 100) / 100f);

                    // dedupe!
                    Vector2 existing = getExistingVertex(newPoint, worldPoints);
                    if(existing != null)
                        newPoint = existing;
                    else
                        worldPoints.add(newPoint);

                    if(current != null)
                        current.addVertex(newPoint);

                    if(crossings % 2 == 1 && lastSector != null) {
                        current = lastSector;
                        current.addVertex(newPoint);
                        lastSector = null;
                    }
                    else {
                        lastSector = current;
                        current = new Sector();
                        current.floorHeight = floorHeight;
                        current.ceilHeight = ceilHeight;
                        current.addVertex(newPoint);
                        newSectors.add(current);
                    }
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

    public boolean lineIntersects(Vector2 start, Vector2 end) {
        return isPointInside(start) || isPointInside(end);
    }

    public void removePoint(Vector2 vertex) {
        points.removeValue(vertex, true);
        for(Sector s : subsectors) {
            s.removePoint(vertex);
        }
    }

    public void match(Sector other) {
        floorHeight = other.floorHeight;
        ceilHeight = other.ceilHeight;

        floorMaterial.set((other.floorMaterial.get(TextureAttribute.Diffuse)));
        ceilingMaterial.set((other.ceilingMaterial.get(TextureAttribute.Diffuse)));
    }
}
