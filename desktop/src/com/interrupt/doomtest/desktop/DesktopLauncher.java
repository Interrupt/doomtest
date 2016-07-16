package com.interrupt.doomtest.desktop;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.interrupt.doomtest.DoomLikeEditor;

public class DesktopLauncher {
	public static void main (String[] arg) {
        Graphics.DisplayMode desktop = LwjglApplicationConfiguration.getDesktopDisplayMode();
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.width = desktop.width;
        config.height = desktop.height;
        new LwjglApplication(new DoomLikeEditor(), config);
	}
}
