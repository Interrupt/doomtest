package com.interrupt.doomtest.levels.editor;

import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.interrupt.doomtest.levels.Level;
import com.interrupt.doomtest.levels.Line;
import com.interrupt.doomtest.levels.Sector;
import com.interrupt.doomtest.levels.Surface;

public class Editor {
    public Level level;

    public Editor(Level editing) {
        this.level = editing;
    }

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
        for(Sector s : level.sectors) {
            s.removePoint(vertex);
        }

        Array<Line> linesToDelete = new Array<Line>();
        for(Line l : level.lines) {
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

        level.lines.removeAll(linesToDelete, true);
    }

    private Array<Line> findLinesWithStartVertexInSector(Vector2 vertex, Sector sector) {
        Array<Line> r = new Array<Line>();
        for(int i = 0; i < level.lines.size; i++) {
            Line l = level.lines.get(i);
            if(l.start == vertex && l.left == sector)
                r.add(l);
        }
        return r;
    }

    private void deleteLinesForSector(Sector sector) {
        Array<Line> linesToRemove = new Array<Line>();

        for(Line l : level.lines) {
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

        level.lines.removeAll(linesToRemove, true);

        for(Sector s : sector.subsectors) {
            deleteLinesForSector(s);
        }
    }

    public void deleteSector(Sector sector) {
        if(sector.parent != null) {
            sector.parent.subsectors.removeValue(sector, true);
        }
        else {
            level.sectors.removeValue(sector, true);
        }

        deleteLinesForSector(sector);
    }

    public void refreshLineSolidity(Sector sector) {
        for(Line line : level.lines) {
            if(line.left == sector || line.right == sector) {
                if (line.right == null) {
                    line.solid = !sector.isSolid;
                }
                else {
                    if(line.left.isSolid && !line.right.isSolid) line.solid = true;
                    else line.solid = line.right.isSolid && !line.left.isSolid;
                }
            }
        }
    }

    public void addPointToLine(Line l, Vector2 point) {
        if(point == l.start || point == l.end) return;

        Vector2 v = getExistingVertex(point);
        if(v == null) level.vertices.add(point);

        Vector2 oldEnd = l.end;
        l.end = point;

        Line newLine = new Line(point, oldEnd, l.solid, l.left, l.right);
        newLine.match(l);

        level.lines.add(newLine);

        addNewPointToSector(new Line(l.start, oldEnd, l.solid, l.left, l.right), point, level.sectors);
    }

    private void addNewPointToSector(Line line, Vector2 point, Array<Sector> sectors) {
        for(Sector sector : sectors) {

            int startIndex = sector.points.indexOf(line.start, true);
            int endIndex = sector.points.indexOf(line.end, true);

            if(startIndex >= 0 && endIndex >= 0) {
                int diff = startIndex - endIndex;
                if(Math.abs(diff) == 1) {
                    sector.points.insert(startIndex + 1, point);
                }
                else if (Math.abs(diff) == sector.points.size - 1) {
                    sector.points.insert(0, point);
                }
            }

            addNewPointToSector(line, point, sector.subsectors);
        }
    }

    Vector2 findPicked_t = new Vector2();
    public Line findPickedLine(Vector3 hovered) {
        for(Line l : level.lines) {
            findPicked_t.set(hovered.x, hovered.z);
            float dist = Intersector.distanceSegmentPoint(l.start, l.end, findPicked_t);
            if(dist < 0.175f) return l;
        }
        return null;
    }



    private void splitSectors(Vector2 start, Vector2 end) {
        Array<Sector> toRemove = new Array<Sector>();
        Array<Sector> newSplits = new Array<Sector>();
        for(Sector s : level.sectors) {
            if(s.lineIntersects(start, end)) {
                Array<Sector> splits =
                        s.split(new Plane(
                                        new Vector3(start.x, 0, start.y),
                                        new Vector3(end.x, 0, end.y),
                                        new Vector3(end.x, 1, end.y)),
                                level.vertices);

                if (splits.size > 0) {
                    toRemove.add(s);
                    for (Sector split : splits) {
                        newSplits.add(split);
                    }
                }
            }
        }

        // remove the old, unsplit sector
        for(Sector s : toRemove) {
            level.sectors.removeValue(s, true);
        }

        // add the new splits
        for(Sector s : newSplits) {
            level.sectors.add(s);
        }
    }

    public boolean vertexExists(Vector2 vertex) {
        for(Line line : level.lines) {
            if(line.start.equals(vertex) || line.end.equals(vertex))
                return true;
        }
        return false;
    }

    public Vector2 getExistingVertex(Vector2 vertex) {
        int found = level.vertices.indexOf(vertex, false);
        if(found >= 0) return level.vertices.get(found);
        return null;
    }

    public void addVertex(Vector2 vertex) {
        Vector2 existing = getExistingVertex(vertex);
        if(existing == null) level.vertices.add(vertex);
    }

    public void addLine(Sector current, Vector2 start, Vector2 end, Surface texture) {

        // don't duplicate verts
        Vector2 existingStart = getExistingVertex(start);
        Vector2 existingEnd = getExistingVertex(end);

        Line line = new Line(existingStart, existingEnd, current.parent == null, current, current.parent);

        // check if this exists already
        Line existing = null;
        for(Line l : level.lines) {
            if(l.isEqualTo(line)) {
                existing = l;
                break;
            }
        }

        if(existing == null) {
            level.lines.add(line);
            line.lowerMaterial.match(texture);
        }
        else {
            if(existing.left == current.parent) {
                existing.left = current;
            }
            else if(existing.left != current) {
                existing.solid = existing.left.isSolid;
                existing.right = current;
            }
            //current.floorHeight = existing.left.floorHeight;
            current.ceilHeight = existing.left.ceilHeight;
        }
    }

    public void refreshSectorParents(Sector sector, Sector newParent) {
        for (Sector s : newParent.subsectors) {
            if (s != sector && sector.isSectorInside(s)) {
                newParent.subsectors.removeValue(s, true);
                sector.addSubSector(s);

                for(Line l : level.lines) {
                    if(l.left == s || l.right == s) {
                        if (l.right == newParent) {
                            l.right = sector;
                        }
                        if (l.left == newParent) {
                            l.left = sector;
                        }
                    }
                }
            }
        }
    }

    public void updateSectorOwnership(Sector sector) {
        Sector parent = null;
        for(Sector s : level.sectors) {
            if(s != sector && parent == null) {
                parent = s.getSectorOfSector(sector);
            }
        }

        if (parent != null && sector.parent != parent) {
            if (sector.parent != null) {
                sector.parent.subsectors.removeValue(sector, true);
            }
            else {
                level.sectors.removeValue(sector, true);
            }

            Sector oldParent = sector.parent;
            parent.addSubSector(sector);

            for(Line l : level.lines) {
                if(l.left == sector) {
                    if(l.right == null || l.right == oldParent) {
                        l.right = parent;
                        l.solid = false;
                    }
                }
            }
        }
        else if(parent == null && sector.parent != null) {
            Sector oldParent = sector.parent;
            sector.parent.subsectors.removeValue(sector, true);
            sector.parent = null;

            level.sectors.add(sector);

            for(Line l : level.lines) {
                if(l.left == sector) {
                    if(l.right == oldParent) {
                        l.right = null;
                        l.solid = true;
                    }
                }
            }
        }
    }

    public boolean isClockwise(Sector s) {
        // sum of (x2 − x1)(y2 + y1)
        float sum = 0;
        for(int i = 0; i < s.points.size - 1; i++) {
            Vector2 start = s.points.get(i);
            Vector2 end = s.points.get(i + 1);
            sum += (end.x - start.x) * (end.y + start.y);
        }

        // close the loop
        Vector2 start = s.points.get(s.points.size - 1);
        Vector2 end = s.points.get(0);
        sum += (end.x - start.x) * (end.y + start.y);

        return sum >= 0;
    }
}
