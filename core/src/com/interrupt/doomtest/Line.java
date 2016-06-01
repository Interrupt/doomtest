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
}
