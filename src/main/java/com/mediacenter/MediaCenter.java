package com.mediacenter;

import de.matthiasmann.twl.*;
import de.matthiasmann.twl.model.SimpleIntegerModel;
import de.matthiasmann.twl.renderer.DynamicImage;
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.renderer.Renderer;
import de.matthiasmann.twl.renderer.lwjgl.LWJGLRenderer;
import de.matthiasmann.twl.textarea.*;
import de.matthiasmann.twl.theme.ThemeManager;
import de.matthiasmann.twl.utils.TextUtil;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hello world!
 */
public class MediaCenter extends DesktopArea {
    public static void main(String[] args) {
        try {
            Display.setDisplayMode(new DisplayMode(800, 600));
            Display.create();
            Display.setTitle("Media Center Test");
            Display.setVSyncEnabled(true);

            LWJGLRenderer renderer = new LWJGLRenderer();
            MediaCenter mediaCenter = new MediaCenter();
            GUI gui = new GUI(mediaCenter, renderer);

            ThemeManager theme = ThemeManager.createThemeManager(
                    MediaCenter.class.getClassLoader().getResource("theme.xml"),
                    renderer
            );
            gui.applyTheme(theme);

            while(!Display.isCloseRequested() && !mediaCenter.isStop()) {
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                gui.update();
                Display.update(false);
                GL11.glGetError();  // force sync with multi threaded GL driver
                Display.sync(60);   // ensure 60Hz even without vsync
                Display.processMessages();  // now process inputs
            }

            theme.destroy();
            gui.destroy();
        } catch (LWJGLException | IOException e) {
            e.printStackTrace();
        }
        Display.destroy();
    }

    private final FPSCounter fpsCounter;
    private final Label mouseCoords;
    private TextArea text;

    private boolean stop = false;
    private Image gridBase;
    private Image gridMask;
    private DynamicImage lightImage;

    public MediaCenter() {
        super();
        fpsCounter = new FPSCounter();
        add(fpsCounter);
        mouseCoords = new Label();
        add(mouseCoords);
        try {
            TextAreaModel model = new SimpleTextAreaModel(new String(Files.readAllBytes(Paths.get(MediaCenter.class.getClassLoader().getResource("text.txt").toURI()))));
            text = new TextArea(model);
            add(text);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public boolean isStop() {
        return stop;
    }

    @Override
    protected void layout() {
        super.layout();
        fpsCounter.adjustSize();
        fpsCounter.setPosition(
                getInnerWidth() - fpsCounter.getWidth(),
                getInnerHeight() - fpsCounter.getHeight()
        );
        mouseCoords.adjustSize();
        mouseCoords.setPosition(0, getInnerHeight() - fpsCounter.getHeight());
        text.setSize(getInnerWidth() - 100, getInnerHeight() - 100);
        text.setPosition((getInnerWidth() - text.getWidth()) / 2, (getInnerHeight() - text.getHeight()) / 2);
    }

    @Override
    protected void applyTheme(ThemeInfo themeInfo) {
        super.applyTheme(themeInfo);
        gridBase = themeInfo.getImage("grid.base");
        gridMask = themeInfo.getImage("grid.mask");
    }

    @Override
    protected void paintBackground(GUI gui) {
        if(lightImage == null) {
            createLightImage(gui.getRenderer());
        }
        if(gridBase != null && gridMask != null) {
            int time = (int)(gui.getCurrentTime() % 2000);
            int offset = (time * (getInnerHeight() + 2*lightImage.getHeight()) / 2000) - lightImage.getHeight();
            gridBase.draw(getAnimationState(), getInnerX(), getInnerY(), getInnerWidth(), getInnerHeight());
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            lightImage.draw(getAnimationState(), getInnerX(), getInnerY() + offset, getInnerWidth(), lightImage.getHeight());
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            gridMask.draw(getAnimationState(), getInnerX(), getInnerY(), getInnerWidth(), getInnerHeight());
        }
    }

    private void createLightImage(Renderer renderer) {
        lightImage = renderer.createDynamicImage(1, 128);
        ByteBuffer bb = ByteBuffer.allocateDirect(128 * 4);
        IntBuffer ib = bb.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        for(int i=0 ; i<128 ; i++) {
            int value = (int)(255 * Math.sin(i * Math.PI / 127.0));
            ib.put(i, (value * 0x010101) | 0xFF000000);
        }
        lightImage.update(bb, DynamicImage.Format.BGRA);
    }

    @Override
    protected boolean handleEvent(Event evt) {
        switch (evt.getType()) {
            case KEY_PRESSED:
                switch (evt.getKeyCode()) {
                    case Event.KEY_ESCAPE:
                        stop = true;
                        return true;
                }
                break;
            case MOUSE_MOVED:
                mouseCoords.setText("x: " + evt.getMouseX() + "  y: " + evt.getMouseY());
                return true;
        }
        return super.handleEvent(evt) || evt.isMouseEventNoWheel();
    }
}
