package com.interrupt.doomtest.collisions;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.interrupt.doomtest.levels.Level;
import com.interrupt.doomtest.levels.Line;
import com.interrupt.doomtest.levels.Sector;

public class WorldIntersector {

    private static Vector3 sectorIntersection = new Vector3();
    private static Vector3 wallIntersection = new Vector3();
    public static boolean intersectsWorld(Level level, Ray r, WorldIntersection intersects) {
        intersects.hitSector = intersectSectors(level, r, sectorIntersection);
        intersects.hitLine = intersectWalls(level, r, wallIntersection);

        boolean hitSector = intersects.hitSector != null;
        boolean hitWall = intersects.hitLine != null;

        if(hitSector || hitWall) {
            if(hitSector) {
                intersects.intersectionPoint.set(sectorIntersection);
            }
            if(hitWall) {
                if(!hitSector) {
                    intersects.intersectionPoint.set(wallIntersection);
                    intersects.hitSector = null;
                }
                else if(wallIntersection.dst(r.origin) <= sectorIntersection.dst(r.origin)) {
                    intersects.intersectionPoint.set(wallIntersection);
                    intersects.hitSector = null;
                }
                else intersects.hitLine = null;
            }
            return true;
        }

        return false;
    }

    private static Vector3 temp_int = new Vector3();
    private static Array<Sector> allSectors = new Array<Sector>();
    public static Sector intersectSectors(Level level, Ray r, Vector3 closest) {
        // Get all sectors into one list
        allSectors.clear();
        for(Sector s : level.sectors) {
            collectAllSectorsIn(s, allSectors);
        }

        Sector closestSector = null;
        float t_dist = 10000;

        for(Sector s : allSectors) {
            Plane plane = new Plane(Vector3.Y, new Vector3(0, s.floorHeight, 0));
            if(Intersector.intersectRayPlane(r, plane, temp_int)) {
                if(plane.isFrontFacing(r.direction)) {
                    Vector2 t_point = new Vector2(temp_int.x, temp_int.z);
                    float dist = temp_int.dst(r.origin);

                    if (s.isPointInside(t_point) && dist < t_dist) {
                        if(s.getSectorOfPoint(t_point) == s) {
                            t_dist = dist;
                            closest.set(temp_int);
                            closestSector = s;
                        }
                    }
                }
            }
        }

        return closestSector;
    }

    public static Line intersectWalls(Level level, Ray ray, Vector3 closest) {
        float t_dist = 10000;
        Line closestLine = null;

        for(Line l : level.lines) {
            Vector3 p1 = new Vector3(l.start.x, 0, l.start.y);
            Vector3 p2 = new Vector3(l.end.x, 0, l.end.y);
            Vector3 p3 = new Vector3(l.end.x, 1, l.end.y);
            Plane plane = new Plane(p1, p2, p3);

            Vector3 endPoint = ray.getEndPoint(new Vector3(), 10000);
            Vector2 startPoint2d = new Vector2(ray.origin.x, ray.origin.z);
            Vector2 endPoint2d = new Vector2(endPoint.x, endPoint.z);

            if(l.findIntersection(startPoint2d, endPoint2d) != null && Intersector.intersectRayPlane(ray, plane, temp_int)) {

                boolean solidHit = l.solid &&
                        (!l.left.isSolid && !plane.isFrontFacing(ray.direction) || (l.left.isSolid && plane.isFrontFacing(ray.direction))) &&
                        l.left.floorHeight < temp_int.y &&
                        l.left.ceilHeight > temp_int.y;

                boolean nonSolidLowerHit = false;
                boolean nonSolidUpperHit = false;

                if(l.right != null && !l.solid) {
                    boolean nonSolidLowerFrontFacing = (l.left.floorHeight > l.right.floorHeight && plane.isFrontFacing(ray.direction))
                            || (l.left.floorHeight < l.right.floorHeight && !plane.isFrontFacing(ray.direction));

                    nonSolidLowerHit = nonSolidLowerFrontFacing && l.pointInLowerWall(temp_int.y);

                    boolean nonSolidUpperFrontFacing = (l.left.ceilHeight > l.right.ceilHeight && plane.isFrontFacing(ray.direction))
                            || (l.left.ceilHeight < l.right.ceilHeight && !plane.isFrontFacing(ray.direction));

                    nonSolidUpperHit = !nonSolidUpperFrontFacing && l.pointInUpperWall(temp_int.y);
                }

                if((solidHit || nonSolidLowerHit) || nonSolidUpperHit) {

                    float dist = temp_int.dst(ray.origin);
                    if(dist < t_dist) {
                        t_dist = dist;
                        closest.set(temp_int);
                        closestLine = l;
                    }
                }
            }
        }

        return closestLine;
    }

    private static Array<Sector> collectAllSectorsIn(Sector sector, Array<Sector> collected) {
        collected.add(sector);
        for(Sector s : sector.subsectors ) {
            collectAllSectorsIn(s, collected);
        }
        return collected;
    }
}
