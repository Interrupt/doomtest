package com.interrupt.doomtest;

import com.badlogic.gdx.math.Vector2;

public class Line {
    public Vector2 start;
    public Vector2 end;
    public boolean solid = true;

    public Sector left = null;
    public Sector right = null;

    public Line(Vector2 start, Vector2 end, boolean solid, Sector left) {
        this.start = start;
        this.end = end;
        this.solid = solid;
        this.left = left;
    }

    public Line(Vector2 start, Vector2 end, boolean solid, Sector left, Sector right) {
        this.start = start;
        this.end = end;
        this.solid = solid;
        this.left = left;
        this.right = right;
    }

    public float getLength() {
        Vector2 tempV2 = new Vector2();
        return tempV2.set(start).sub(end).len();
    }

    public boolean isEqualTo(Line other) {
        if(other == null)
            return false;
        if(other.start.equals(start) && other.end.equals(end))
            return true;
        if(other.end.equals(start) && other.start.equals(end))
            return true;
        return false;
    }
}
