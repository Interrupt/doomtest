package com.interrupt.doomtest.editor.ui.menu;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;

import java.awt.event.ActionListener;

public class MenuItem extends TextButton {
    public Scene2dMenu subMenu;
    public Skin skin;
    public Scene2dMenu parent;
    public ActionListener actionListener;
    public ClickListener clickListener;

    private Label acceleratorLabel = null;
    public boolean showExpandArrow = true;

    public static Array<MenuItem> acceleratorItems = new Array<MenuItem>();

    private Vector2 tempVec2 = new Vector2();

    public MenuItem(CharSequence text, Skin skin) {
        super(text.toString(), skin);
        this.skin = skin;

        getLabel().setAlignment(Align.left);
        setStyle(skin.get("menu", TextButtonStyle.class));
        setSkin(skin);

        clear();
        add(getLabel()).expand().fill();

        final MenuItem thisItem = this;
        clickListener = new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if(getParentMenu() != null) getParentMenu().setExpanded(thisItem);
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                Scene2dMenu p = getParentMenu();
                while(p.parentMenuItem != null && p.parentMenuItem.parent != null) {
                    p = p.parentMenuItem.parent;
                }

                if(actionListener != null && p != null) p.close();
                if(actionListener != null) actionListener.actionPerformed(null);
            }
        };

        refresh();
        setWidth(400f);
    }

    public MenuItem(CharSequence text, Skin skin, ActionListener actionListener) {
        this(text, skin);
        this.actionListener = actionListener;
    }

    public MenuItem addItem(MenuItem item) {
        if(subMenu == null) {
            subMenu = new Scene2dMenu(skin);
            subMenu.parentMenuItem = this;
            subMenu.setVisible(false);

            refresh();
        }

        subMenu.addItem(item);
        return this;
    }

    public MenuItem addSeparator() {
        if(subMenu != null)
            subMenu.addSeparator();
        return this;
    }

    @Override
    public void act(float delta) {
        if(subMenu != null && subMenu.menuTable != null) {
            float yLocation = getY() - subMenu.menuTable.getHeight() + getHeight();
            subMenu.setY(yLocation);

            // where on the stage is the menu being drawn?
            Vector2 stageLocation = localToStageCoordinates(tempVec2.set(0,yLocation));

            // might need to push it back up some to keep it on the screen
            if(stageLocation.y < getHeight()) {
                subMenu.setY(subMenu.getY() - (stageLocation.y - getY()));
            }

            if(parent != null && parent instanceof Scene2dMenuBar) {
                subMenu.setY(-subMenu.menuTable.getHeight());
            }
        }

        if(parent == null) {
            Actor pw = this;
            while(pw != null) {
                pw = pw.getParent();
                if(pw instanceof Scene2dMenu) {
                    parent = (Scene2dMenu)pw;
                    break;
                }
            }
        }
    }

    public void updateStyle(boolean selected) {
        if(!selected) {
            setStyle(skin.get("menu", TextButton.TextButtonStyle.class));
        }
        else {
            setStyle(skin.get("menu-selected", TextButton.TextButtonStyle.class));
        }
    }

    public void addActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void setParentMenu(Scene2dMenu parent) {
        this.parent = parent;
    }

    public Scene2dMenu getParentMenu() {
        return parent;
    }

    public void refresh() {
        clear();
        addListener(clickListener);
        add(getLabel()).expand().fill();

        boolean madePadding = false;

        if(acceleratorLabel != null) {
            add(acceleratorLabel).align(Align.right).fill().padLeft(madePadding ? 3f : 20f);
        }
        else if(subMenu != null && showExpandArrow) {
            Image arrowImage = new Image(skin, "menu-arrow");
            arrowImage.setScaling(Scaling.none);
            add(arrowImage).align(Align.right).fill().padLeft(20f);
        }

        pack();
    }
}
