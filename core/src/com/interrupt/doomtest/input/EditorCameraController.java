package com.interrupt.doomtest.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by ccuddigan on 6/5/16.
 */
public class EditorCameraController extends GestureDetector {

    protected static class CameraGestureListener extends GestureAdapter {
        @Override
        public boolean touchDown (float x, float y, int pointer, int button) {
            return false;
        }

        @Override
        public boolean tap (float x, float y, int count, int button) {
            return false;
        }

        @Override
        public boolean longPress (float x, float y) {
            return false;
        }

        @Override
        public boolean fling (float velocityX, float velocityY, int button) {
            return false;
        }

        @Override
        public boolean pan (float x, float y, float deltaX, float deltaY) {
            return false;
        }

        @Override
        public boolean zoom (float initialDistance, float distance) {
            return false;
        }

        @Override
        public boolean pinch (Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
            return false;
        }
    };

    public int forwardKey = Input.Keys.W;
    protected boolean forwardPressed;

    public int backwardKey = Input.Keys.S;
    protected boolean backwardPressed;

    public int strafeRightKey = Input.Keys.D;
    protected boolean strafeRightPressed;

    public int strafeLeftKey = Input.Keys.A;
    protected boolean strafeLeftPressed;

    public int rotateLeftKey = Input.Keys.LEFT;
    protected boolean rotateLeftPressed;

    public int rotateRightKey = Input.Keys.RIGHT;
    protected boolean rotateRightPressed;

    public int rotateUpKey = Input.Keys.UP;
    protected boolean rotateUpPressed;

    public int rotateDownKey = Input.Keys.DOWN;
    protected boolean rotateDownPressed;

    private final Camera camera;

    private final Vector3 tmpV1 = new Vector3();
    private final Vector3 tmpV2 = new Vector3();
    private final float translateUnits = 1f;

    // camera rotation
    private Vector2 cameraRotation = new Vector2();

    public EditorCameraController(final Camera camera) {
        super(new CameraGestureListener());
        this.camera = camera;
    }

    private float getTranslateSpeed() {
        return 80f;
    }

    private float getRotateSpeed() {
        return 80f;
    }

    public void update () {
        final float delta = Gdx.graphics.getDeltaTime();

        final float tSpeed = getTranslateSpeed();
        final float rSpeed = getRotateSpeed();
        final float translateX = ((strafeLeftPressed ? tSpeed : 0f) + (strafeRightPressed ? -tSpeed : 0f)) * delta;
        final float translateY = ((forwardPressed ? tSpeed : 0f) + (backwardPressed ? -tSpeed : 0f)) * delta;
        final float rotateX = ((rotateLeftPressed ? rSpeed : 0f) + (rotateRightPressed ? -rSpeed : 0f)) * delta;
        final float rotateY = ((rotateUpPressed ? rSpeed : 0f) + (rotateDownPressed ? -rSpeed : 0f)) * delta;

        camera.translate(tmpV1.set(camera.direction).scl(translateY * translateUnits));
        camera.translate(tmpV1.set(camera.direction).crs(camera.up).nor().scl(-translateX * translateUnits));

        float deltaX = rotateX;
        float deltaY = rotateY;
        camera.direction.rotate(camera.up, deltaX);
        tmpV1.set(camera.direction).crs(camera.up).nor();
        camera.direction.rotate(tmpV1, deltaY);

        camera.update();
    }

    @Override
    public boolean keyDown (int keycode) {
        if(keycode == forwardKey) forwardPressed = true;
        else if(keycode == backwardKey) backwardPressed = true;
        else if(keycode == strafeLeftKey) strafeLeftPressed = true;
        else if(keycode == strafeRightKey) strafeRightPressed = true;
        else if(keycode == rotateLeftKey) rotateLeftPressed = true;
        else if(keycode == rotateRightKey) rotateRightPressed = true;
        else if(keycode == rotateUpKey) rotateUpPressed = true;
        else if(keycode == rotateDownKey) rotateDownPressed= true;

        return false;
    }

    @Override
    public boolean keyUp (int keycode) {
        if(keycode == forwardKey) forwardPressed = false;
        else if(keycode == backwardKey) backwardPressed = false;
        else if(keycode == strafeLeftKey) strafeLeftPressed = false;
        else if(keycode == strafeRightKey) strafeRightPressed = false;
        else if(keycode == rotateLeftKey) rotateLeftPressed = false;
        else if(keycode == rotateRightKey) rotateRightPressed = false;
        else if(keycode == rotateUpKey) rotateUpPressed = false;
        else if(keycode == rotateDownKey) rotateDownPressed= false;

        return false;
    }
}
