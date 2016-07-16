package com.interrupt.doomtest.editor.ui.menu;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

public class Scene2dMenuBar extends Scene2dMenu {
    public Scene2dMenuBar(Skin skin) {
        super(skin);
    }

    @Override
    public float getHeight() {
        if(menuTable != null) return menuTable.getHeight();
        return 40f;
    }

    @Override
    public float getWidth() {
        if(menuTable != null) return menuTable.getWidth();
        return 300f;
    }

    @Override
    public void pack() {
        if(menuTable != null) menuTable.add().width(5000f);
        super.pack();
    }

    @Override
    public void close() {
        for(Actor a : items) {
            if(a instanceof MenuItem) {
                MenuItem i = (MenuItem) a;
                i.updateStyle(false);
                if (i.subMenu != null) i.subMenu.close();
            }
        }
    }

    @Override
    protected void refreshDrawables() {
        clearChildren();

        // make the table
        menuTable = new Table();
        menuTable.setZIndex(10);
        menuTable.setOrigin(0f, 50f);
        menuTable.setSkin(skin);
        addActor(menuTable);

        menuTable.add().width(5f);

        // add the rows
        for(Actor a : items) {
            menuTable.add(a).align(Align.left).fill();
        }

        menuTable.setBackground("menu_default_normal");
        menuTable.add().width(5000f).fill();

        // add all of the sub menus to this space
        for(Actor a : items) {
            if(a instanceof MenuItem) {
                MenuItem i = (MenuItem)a;
                i.setParentMenu(this);
                if(i.subMenu != null) {
                    i.showExpandArrow = false;
                    i.refresh();
                    addActor(i.subMenu);
                }
            }
        }

        menuTable.pack();
    }
}
