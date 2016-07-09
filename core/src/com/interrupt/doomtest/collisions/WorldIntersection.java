package com.interrupt.doomtest.collisions;

import com.badlogic.gdx.math.Vector3;
import com.interrupt.doomtest.levels.Line;
import com.interrupt.doomtest.levels.Sector;

public class WorldIntersection {
    public Vector3 intersectionPoint = new Vector3();
    public Sector hitSector = null;
    public Line hitLine = null;
}
