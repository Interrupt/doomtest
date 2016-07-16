package com.interrupt.doomtest.editor.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.interrupt.doomtest.DoomLikeEditor;
import com.interrupt.doomtest.editor.ui.menu.MenuItem;
import com.interrupt.doomtest.editor.ui.menu.Scene2dMenuBar;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Hud {

    private static Skin loadSkin() {
        return new Skin(Gdx.files.local("ui/HoloSkin/Holo-dark-ldpi.json"),
                new TextureAtlas(Gdx.files.local("ui/HoloSkin/Holo-dark-ldpi.atlas")));
    }

    public static Stage create(final Array<TextureRegion> textures, TextureRegion current, final DoomLikeEditor editor) {

        final OrthographicCamera hudCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        final Stage stage = new Stage(new ScreenViewport(hudCamera));
        final Skin hudSkin = loadSkin();
        final Scene2dMenuBar menuBar = new Scene2dMenuBar(hudSkin);

        editor.currentTexture = current;

        ActionListener emptyListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };

        menuBar.addItem(new MenuItem("File", hudSkin)
                .addItem(new MenuItem("Save", hudSkin, emptyListener))
                .addItem(new MenuItem("Save As...", hudSkin, emptyListener))
                .addItem(new MenuItem("New", hudSkin, emptyListener))
                .addItem(new MenuItem("Open", hudSkin, emptyListener)));
        menuBar.pack();

        final Image texturePickerButton = new Image(new TextureRegionDrawable(current));
        texturePickerButton.setScaling(Scaling.stretch);

        Table wallPickerLayoutTable = new Table();
        wallPickerLayoutTable.setFillParent(true);
        wallPickerLayoutTable.align(Align.left | Align.top);

        wallPickerLayoutTable.add(menuBar);
        wallPickerLayoutTable.row();
        wallPickerLayoutTable.add(texturePickerButton).pad(20).padTop(100f).width(50f).height(50f).align(Align.left).padBottom(6f);
        wallPickerLayoutTable.row();

        texturePickerButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                TextureRegionPicker picker = new TextureRegionPicker("Pick Current Texture", hudSkin, textures) {
                    @Override
                    public void result(Integer value, TextureRegion region) {
                        setTexture(region, editor);
                        texturePickerButton.setDrawable(new TextureRegionDrawable(region));
                    }
                };
                stage.addActor(picker);
                picker.show(stage);
                event.handle();
            }
        });

        stage.addActor(wallPickerLayoutTable);

        stage.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                Actor touched = stage.hit(x, y, false);
                if (!(event.getTarget() instanceof TextField)) stage.setKeyboardFocus(null);
                if ((touched == null || !touched.isDescendantOf(menuBar))) {
                    menuBar.close();
                }
                return false;
            }
        });

        return stage;
    }

    public static void setTexture(TextureRegion texture, DoomLikeEditor editor) {
        editor.currentTexture = texture;

        if(editor.pickedLine != null) {
            editor.pickedLine.lowerMaterial.set(TextureAttribute.createDiffuse(texture));
        }
        else if(editor.pickedSector != null) {
            editor.pickedSector.floorMaterial.set(TextureAttribute.createDiffuse(texture));
        }
        editor.refreshRenderer();
    }
}
