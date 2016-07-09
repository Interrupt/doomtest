package com.interrupt.doomtest.levels;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Level {
    public Array<Sector> sectors = new Array<Sector>();
    public Array<Line> lines = new Array<Line>();
    public Array<Vector2> vertices = new Array<Vector2>();

    public Array<Sector> getAllSectorsWithVertex(Array<Sector> search, Vector2 vertex, Array<Sector> found) {
        for(Sector s : search) {
            if(s.points.contains(vertex, true)) {
                found.add(s);
            }
            getAllSectorsWithVertex(s.subsectors, vertex, found);
        }
        return found;
    }

    public void deleteVertex(Vector2 vertex) {
        for(Sector s : sectors) {
            s.removePoint(vertex);
        }

        Array<Line> linesToDelete = new Array<Line>();
        for(Line l : lines) {
            if(l.end == vertex) {
                linesToDelete.add(l);
                Array<Line> nexts = findLinesWithStartVertexInSector(vertex, l.left);
                for(Line next : nexts) {
                    if (next != null) {
                        next.start = l.start;
                    }
                }
            }
        }

        lines.removeAll(linesToDelete, true);
    }

    private Array<Line> findLinesWithStartVertexInSector(Vector2 vertex, Sector sector) {
        Array<Line> r = new Array<Line>();
        for(int i = 0; i < lines.size; i++) {
            Line l = lines.get(i);
            if(l.start == vertex && l.left == sector)
                r.add(l);
        }
        return r;
    }

    private void deleteLinesForSector(Sector sector) {
        Array<Line> linesToRemove = new Array<Line>();

        for(Line l : lines) {
            if(l.left == sector) {
                if(l.right == null) {
                    if(sector.parent != null &&
                            sector.parent.points.contains(l.start, true) &&
                            sector.parent.points.contains(l.end, true)) {
                        l.left = sector.parent;
                    }
                    else {
                        linesToRemove.add(l);
                    }
                }
                else {
                    l.left = l.right;
                    l.right = null;
                }
            }
        }

        lines.removeAll(linesToRemove, true);

        for(Sector s : sector.subsectors) {
            deleteLinesForSector(s);
        }
    }

    public void deleteSector(Sector sector) {
        if(sector.parent != null) {
            sector.parent.subsectors.removeValue(sector, true);
        }
        else {
            sectors.removeValue(sector, true);
        }

        deleteLinesForSector(sector);
    }

    public Vector2 getVertexNear(float x, float y, float distance) {
        for(Vector2 v : vertices) {
            float d = v.dst(x, y);
            if(d < distance) {
                return v;
            }
        }
        return null;
    }
}
