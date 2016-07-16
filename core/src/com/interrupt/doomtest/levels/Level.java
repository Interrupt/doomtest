package com.interrupt.doomtest.levels;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Level {
    public Array<Sector> sectors = new Array<Sector>();
    public Array<Line> lines = new Array<Line>();
    public Array<Vector2> vertices = new Array<Vector2>();

    public Level() { }

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
