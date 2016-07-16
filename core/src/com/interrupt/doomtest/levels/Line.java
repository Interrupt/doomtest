package com.interrupt.doomtest.levels;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector2;
import com.interrupt.doomtest.gfx.Art;

public class Line {
    public Vector2 start;
    public Vector2 end;
    public boolean solid = true;

    public Sector left = null;
    public Sector right = null;

    //Material upperMaterial = new Material(this.hashCode() + "_upper", ColorAttribute.createDiffuse(Color.WHITE), TextureAttribute.createDiffuse(Art.getTexture("textures/wall1.png")));
    //public transient Material lowerMaterial = new Material(this.hashCode() + "_lower", ColorAttribute.createDiffuse(Color.WHITE), TextureAttribute.createDiffuse(Art.getTexture("textures/wall1.png")));

    public Surface lowerMaterial = new Surface("textures/wall1.png");

    public Line() { }

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

    public Vector2 findIntersection(Vector2 s, Vector2 e) {
        Vector2 intersection = new Vector2();
        if(Intersector.intersectSegments(s, e, this.start, this.end, intersection)) {
            return intersection;
        }
        return null;
    }

    public boolean pointInLowerWall(float height) {
        if(right != null) {
            float max = Math.max(left.floorHeight, right.floorHeight);
            float min = Math.min(left.floorHeight, right.floorHeight);
            return height > min && height < max;
        }
        return false;
    }

    public boolean pointInUpperWall(float height) {
        if(right != null) {
            float max = Math.max(left.ceilHeight, right.ceilHeight);
            float min = Math.min(left.ceilHeight, right.ceilHeight);
            return height > min && height < max;
        }
        return false;
    }

    public Plane.PlaneSide getUpperWallSide() {
        if(right != null) {
            if(left.ceilHeight > right.ceilHeight) return Plane.PlaneSide.Back;
        }
        return Plane.PlaneSide.Front;
    }

    public Plane.PlaneSide getLowerWallSide() {
        if(right != null) {
            if(left.floorHeight < right.floorHeight) return Plane.PlaneSide.Back;
        }
        return Plane.PlaneSide.Front;
    }

    public void match(Line other) {
        lowerMaterial = other.lowerMaterial;
    }
}
