package com.interrupt.doomtest.editor.ui;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
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
import com.interrupt.doomtest.levels.Level;
import com.interrupt.doomtest.levels.Surface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;

public class Hud {

    private static DoomLikeEditor editor = null;

    private static Skin loadSkin() {
        return new Skin(Gdx.files.local("ui/HoloSkin/Holo-dark-ldpi.json"),
                new TextureAtlas(Gdx.files.local("ui/HoloSkin/Holo-dark-ldpi.atlas")));
    }

    public static Stage create(final Array<Surface> textures, Surface current, final DoomLikeEditor doomLikeEditor) {

        editor = doomLikeEditor;

        final OrthographicCamera hudCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        final Stage stage = new Stage(new ScreenViewport(hudCamera));
        final Skin hudSkin = loadSkin();
        final Scene2dMenuBar menuBar = new Scene2dMenuBar(hudSkin);

        editor.currentTexture = current;

        menuBar.addItem(new MenuItem("File", hudSkin)
                .addItem(new MenuItem("Save", hudSkin, saveAction))
                .addItem(new MenuItem("Save As...", hudSkin, saveAsAction))
                .addItem(new MenuItem("New", hudSkin, newAction))
                .addItem(new MenuItem("Open", hudSkin, openAction)));
        menuBar.pack();

        final Image texturePickerButton = new Image(new TextureRegionDrawable(current.getTextureRegion()));
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
                    public void result(Integer value, Surface region) {
                        setTexture(region, editor);
                        texturePickerButton.setDrawable(new TextureRegionDrawable(region.getTextureRegion()));
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

    public static void setTexture(Surface texture, DoomLikeEditor editor) {
        editor.currentTexture = texture;

        if (editor.pickedLine != null) {
            editor.pickedLine.lowerMaterial.match(texture);
        } else if (editor.pickedSector != null) {
            editor.pickedSector.floorMaterial.match(texture);
        }
        editor.refreshRenderer();
    }

    private static ActionListener saveAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentFileName == null) {
                saveAsAction.actionPerformed(e);
            } else {
                editor.saveLevel(Gdx.files.absolute(currentDirectory + currentFileName));
            }
        }
    };

    private static String currentFileName = null;
    private static ActionListener saveAsAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFrame frame = new JFrame("SaveFrame");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setVisible(false);

            FileDialog dialog = new FileDialog(frame, "Save Level", FileDialog.SAVE);

            class WSFilter implements FilenameFilter {
                @Override
                public boolean accept(File dir, String name) {
                    return (name.endsWith(".lvl"));
                }
            }
            ;
            FilenameFilter wsFilter = new WSFilter();

            if (currentFileName == null)
                dialog.setFile("level.lvl");
            else
                dialog.setFile(currentFileName);

            dialog.setFilenameFilter(wsFilter);
            dialog.setAlwaysOnTop(true);
            dialog.setVisible(true);

            final String file = dialog.getFile();
            final String dir = dialog.getDirectory();

            if (dir != null && file != null && file.trim().length() != 0) {
                currentFileName = file;
                currentDirectory = dir;
                editor.saveLevel(Gdx.files.absolute((dir + file)));
            }

            frame.dispose();
        }
    };

    private static ActionListener newAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            editor.newLevel();
        }
    };

    private static String currentDirectory = null;
    private static ActionListener openAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFrame frame = new JFrame("OpenFrame");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setVisible(false);

            FileDialog dialog = new FileDialog(frame, "Open Level", FileDialog.LOAD);
            class WSFilter implements FilenameFilter {
                @Override
                public boolean accept(File dir, String name) {
                    return (name.endsWith(".lvl"));
                }
            };

            FilenameFilter wsFilter = new WSFilter();

            dialog.setFilenameFilter(wsFilter);
            dialog.setAlwaysOnTop(true);
            dialog.setVisible(true);

            if (dialog.getFile() != null) {
                currentFileName = dialog.getFile();
                currentDirectory = "";
            }

            final String file = dialog.getFile();
            final String dir = dialog.getDirectory();
            if (dir == null || file == null || file.trim().length() == 0) return;

            FileHandle level = Gdx.files.getFileHandle(dir + file, Files.FileType.Absolute);
            if (level.exists()) {
                currentFileName = level.path();
                editor.openLevel(Gdx.files.absolute(dir + file));
                frame.dispose();
            }
        }
    };
}
