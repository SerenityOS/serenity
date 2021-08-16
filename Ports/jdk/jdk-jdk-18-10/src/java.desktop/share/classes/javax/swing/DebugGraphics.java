/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javax.swing;

import java.awt.*;
import java.awt.image.*;
import java.text.AttributedCharacterIterator;

/**
 * Graphics subclass supporting graphics debugging. Overrides most methods
 * from Graphics.  DebugGraphics objects are rarely created by hand.  They
 * are most frequently created automatically when a JComponent's
 * debugGraphicsOptions are changed using the setDebugGraphicsOptions()
 * method.
 * <p>
 * NOTE: You must turn off double buffering to use DebugGraphics:
 *       RepaintManager repaintManager = RepaintManager.currentManager(component);
 *       repaintManager.setDoubleBufferingEnabled(false);
 *
 * @see JComponent#setDebugGraphicsOptions
 * @see RepaintManager#currentManager
 * @see RepaintManager#setDoubleBufferingEnabled
 *
 * @author Dave Karlton
 * @since 1.2
 */
public class DebugGraphics extends Graphics {
    Graphics                    graphics;
    Image                       buffer;
    int                         debugOptions;
    int                         graphicsID = graphicsCount++;
    int                         xOffset, yOffset;
    private static int          graphicsCount = 0;
    private static ImageIcon    imageLoadingIcon = new ImageIcon();

    /** Log graphics operations. */
    public static final int     LOG_OPTION   = 1 << 0;
    /** Flash graphics operations. */
    public static final int     FLASH_OPTION = 1 << 1;
    /** Show buffered operations in a separate <code>Frame</code>. */
    public static final int     BUFFERED_OPTION = 1 << 2;
    /** Don't debug graphics operations. */
    public static final int     NONE_OPTION = -1;

    static {
        JComponent.DEBUG_GRAPHICS_LOADED = true;
    }

    /**
     * Constructs a new debug graphics context that supports slowed
     * down drawing.
     */
    public DebugGraphics() {
        super();
        buffer = null;
        xOffset = yOffset = 0;
    }

    /**
     * Constructs a debug graphics context from an existing graphics
     * context that slows down drawing for the specified component.
     *
     * @param graphics  the Graphics context to slow down
     * @param component the JComponent to draw slowly
     */
    public DebugGraphics(Graphics graphics, JComponent component) {
        this(graphics);
        setDebugOptions(component.shouldDebugGraphics());
    }

    /**
     * Constructs a debug graphics context from an existing graphics
     * context that supports slowed down drawing.
     *
     * @param graphics  the Graphics context to slow down
     */
    public DebugGraphics(Graphics graphics) {
        this();
        this.graphics = graphics;
    }

    /**
     * Overrides <code>Graphics.create</code> to return a DebugGraphics object.
     */
    public Graphics create() {
        DebugGraphics debugGraphics;

        debugGraphics = new DebugGraphics();
        debugGraphics.graphics = graphics.create();
        debugGraphics.debugOptions = debugOptions;
        debugGraphics.buffer = buffer;

        return debugGraphics;
    }

    /**
     * Overrides <code>Graphics.create</code> to return a DebugGraphics object.
     */
    public Graphics create(int x, int y, int width, int height) {
        DebugGraphics debugGraphics;

        debugGraphics = new DebugGraphics();
        debugGraphics.graphics = graphics.create(x, y, width, height);
        debugGraphics.debugOptions = debugOptions;
        debugGraphics.buffer = buffer;
        debugGraphics.xOffset = xOffset + x;
        debugGraphics.yOffset = yOffset + y;

        return debugGraphics;
    }


    //------------------------------------------------
    //  NEW METHODS
    //------------------------------------------------

    /**
     * Sets the Color used to flash drawing operations.
     *
     * @param flashColor the Color used to flash drawing operations
     */
    public static void setFlashColor(Color flashColor) {
        info().flashColor = flashColor;
    }

    /**
     * Returns the Color used to flash drawing operations.
     *
     * @return the Color used to flash drawing operations
     * @see #setFlashColor
     */
    public static Color flashColor() {
        return info().flashColor;
    }

    /**
     * Sets the time delay of drawing operation flashing.
     *
     * @param flashTime the time delay of drawing operation flashing
     */
    public static void setFlashTime(int flashTime) {
        info().flashTime = flashTime;
    }

    /**
     * Returns the time delay of drawing operation flashing.
     *
     * @return the time delay of drawing operation flashing
     * @see #setFlashTime
     */
    public static int flashTime() {
        return info().flashTime;
    }

    /**
     * Sets the number of times that drawing operations will flash.
     *
     * @param flashCount number of times that drawing operations will flash
     */
    public static void setFlashCount(int flashCount) {
        info().flashCount = flashCount;
    }

    /**
     * Returns the number of times that drawing operations will flash.
     *
     * @return the number of times that drawing operations will flash
     * @see #setFlashCount
     */
    public static int flashCount() {
        return info().flashCount;
    }

    /**
     * Sets the stream to which the DebugGraphics logs drawing operations.
     *
     * @param stream the stream to which the DebugGraphics logs drawing operations
     */
    public static void setLogStream(java.io.PrintStream stream) {
        info().stream = stream;
    }

    /**
     * Returns the stream to which the DebugGraphics logs drawing operations.
     *
     * @return the stream to which the DebugGraphics logs drawing operations
     * @see #setLogStream
     */
    public static java.io.PrintStream logStream() {
        return info().stream;
    }

    /** Sets the Font used for text drawing operations.
      */
    public void setFont(Font aFont) {
        if (debugLog()) {
            info().log(toShortString() + " Setting font: " + aFont);
        }
        graphics.setFont(aFont);
    }

    /** Returns the Font used for text drawing operations.
      * @see #setFont
      */
    public Font getFont() {
        return graphics.getFont();
    }

    /** Sets the color to be used for drawing and filling lines and shapes.
      */
    public void setColor(Color aColor) {
        if (debugLog()) {
            info().log(toShortString() + " Setting color: " + aColor);
        }
        graphics.setColor(aColor);
    }

    /** Returns the Color used for text drawing operations.
      * @see #setColor
      */
    public Color getColor() {
        return graphics.getColor();
    }


    //-----------------------------------------------
    // OVERRIDDEN METHODS
    //------------------------------------------------

    /**
     * Overrides <code>Graphics.getFontMetrics</code>.
     */
    public FontMetrics getFontMetrics() {
        return graphics.getFontMetrics();
    }

    /**
     * Overrides <code>Graphics.getFontMetrics</code>.
     */
    public FontMetrics getFontMetrics(Font f) {
        return graphics.getFontMetrics(f);
    }

    /**
     * Overrides <code>Graphics.translate</code>.
     */
    public void translate(int x, int y) {
        if (debugLog()) {
            info().log(toShortString() +
                " Translating by: " + new Point(x, y));
        }
        xOffset += x;
        yOffset += y;
        graphics.translate(x, y);
    }

    /**
     * Overrides <code>Graphics.setPaintMode</code>.
     */
    public void setPaintMode() {
        if (debugLog()) {
            info().log(toShortString() + " Setting paint mode");
        }
        graphics.setPaintMode();
    }

    /**
     * Overrides <code>Graphics.setXORMode</code>.
     */
    public void setXORMode(Color aColor) {
        if (debugLog()) {
            info().log(toShortString() + " Setting XOR mode: " + aColor);
        }
        graphics.setXORMode(aColor);
    }

    /**
     * Overrides <code>Graphics.getClipBounds</code>.
     */
    public Rectangle getClipBounds() {
        return graphics.getClipBounds();
    }

    /**
     * Overrides <code>Graphics.clipRect</code>.
     */
    public void clipRect(int x, int y, int width, int height) {
        graphics.clipRect(x, y, width, height);
        if (debugLog()) {
            info().log(toShortString() +
                " Setting clipRect: " + (new Rectangle(x, y, width, height)) +
                " New clipRect: " + graphics.getClip());
        }
    }

    /**
     * Overrides <code>Graphics.setClip</code>.
     */
    public void setClip(int x, int y, int width, int height) {
        graphics.setClip(x, y, width, height);
        if (debugLog()) {
            info().log(toShortString() +
                        " Setting new clipRect: " + graphics.getClip());
        }
    }

    /**
     * Overrides <code>Graphics.getClip</code>.
     */
    public Shape getClip() {
        return graphics.getClip();
    }

    /**
     * Overrides <code>Graphics.setClip</code>.
     */
    public void setClip(Shape clip) {
        graphics.setClip(clip);
        if (debugLog()) {
            info().log(toShortString() +
                       " Setting new clipRect: " +  graphics.getClip());
        }
    }

    /**
     * Overrides <code>Graphics.drawRect</code>.
     */
    public void drawRect(int x, int y, int width, int height) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info().log(toShortString() +
                      " Drawing rect: " +
                      new Rectangle(x, y, width, height));
        }

        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.drawRect(x, y, width, height);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            Color oldColor = getColor();
            int i, count = (info.flashCount * 2) - 1;

            for (i = 0; i < count; i++) {
                graphics.setColor((i % 2) == 0 ? info.flashColor : oldColor);
                graphics.drawRect(x, y, width, height);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
            graphics.setColor(oldColor);
        }
        graphics.drawRect(x, y, width, height);
    }

    /**
     * Overrides <code>Graphics.fillRect</code>.
     */
    public void fillRect(int x, int y, int width, int height) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info().log(toShortString() +
                      " Filling rect: " +
                      new Rectangle(x, y, width, height));
        }

        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.fillRect(x, y, width, height);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            Color oldColor = getColor();
            int i, count = (info.flashCount * 2) - 1;

            for (i = 0; i < count; i++) {
                graphics.setColor((i % 2) == 0 ? info.flashColor : oldColor);
                graphics.fillRect(x, y, width, height);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
            graphics.setColor(oldColor);
        }
        graphics.fillRect(x, y, width, height);
    }

    /**
     * Overrides <code>Graphics.clearRect</code>.
     */
    public void clearRect(int x, int y, int width, int height) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info().log(toShortString() +
                      " Clearing rect: " +
                      new Rectangle(x, y, width, height));
        }

        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.clearRect(x, y, width, height);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            Color oldColor = getColor();
            int i, count = (info.flashCount * 2) - 1;

            for (i = 0; i < count; i++) {
                graphics.setColor((i % 2) == 0 ? info.flashColor : oldColor);
                graphics.clearRect(x, y, width, height);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
            graphics.setColor(oldColor);
        }
        graphics.clearRect(x, y, width, height);
    }

    /**
     * Overrides <code>Graphics.drawRoundRect</code>.
     */
    public void drawRoundRect(int x, int y, int width, int height,
                              int arcWidth, int arcHeight) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info().log(toShortString() +
                      " Drawing round rect: " +
                      new Rectangle(x, y, width, height) +
                      " arcWidth: " + arcWidth +
                      " archHeight: " + arcHeight);
        }
        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.drawRoundRect(x, y, width, height,
                                            arcWidth, arcHeight);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            Color oldColor = getColor();
            int i, count = (info.flashCount * 2) - 1;

            for (i = 0; i < count; i++) {
                graphics.setColor((i % 2) == 0 ? info.flashColor : oldColor);
                graphics.drawRoundRect(x, y, width, height,
                                       arcWidth, arcHeight);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
            graphics.setColor(oldColor);
        }
        graphics.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    /**
     * Overrides <code>Graphics.fillRoundRect</code>.
     */
    public void fillRoundRect(int x, int y, int width, int height,
                              int arcWidth, int arcHeight) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info().log(toShortString() +
                      " Filling round rect: " +
                      new Rectangle(x, y, width, height) +
                      " arcWidth: " + arcWidth +
                      " archHeight: " + arcHeight);
        }
        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.fillRoundRect(x, y, width, height,
                                            arcWidth, arcHeight);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            Color oldColor = getColor();
            int i, count = (info.flashCount * 2) - 1;

            for (i = 0; i < count; i++) {
                graphics.setColor((i % 2) == 0 ? info.flashColor : oldColor);
                graphics.fillRoundRect(x, y, width, height,
                                       arcWidth, arcHeight);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
            graphics.setColor(oldColor);
        }
        graphics.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    /**
     * Overrides <code>Graphics.drawLine</code>.
     */
    public void drawLine(int x1, int y1, int x2, int y2) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info().log(toShortString() +
                       " Drawing line: from " + pointToString(x1, y1) +
                       " to " +  pointToString(x2, y2));
        }

        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.drawLine(x1, y1, x2, y2);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            Color oldColor = getColor();
            int i, count = (info.flashCount * 2) - 1;

            for (i = 0; i < count; i++) {
                graphics.setColor((i % 2) == 0 ? info.flashColor : oldColor);
                graphics.drawLine(x1, y1, x2, y2);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
            graphics.setColor(oldColor);
        }
        graphics.drawLine(x1, y1, x2, y2);
    }

    /**
     * Overrides <code>Graphics.draw3DRect</code>.
     */
    public void draw3DRect(int x, int y, int width, int height,
                           boolean raised) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info().log(toShortString() +
                       " Drawing 3D rect: " +
                       new Rectangle(x, y, width, height) +
                       " Raised bezel: " + raised);
        }
        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.draw3DRect(x, y, width, height, raised);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            Color oldColor = getColor();
            int i, count = (info.flashCount * 2) - 1;

            for (i = 0; i < count; i++) {
                graphics.setColor((i % 2) == 0 ? info.flashColor : oldColor);
                graphics.draw3DRect(x, y, width, height, raised);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
            graphics.setColor(oldColor);
        }
        graphics.draw3DRect(x, y, width, height, raised);
    }

    /**
     * Overrides <code>Graphics.fill3DRect</code>.
     */
    public void fill3DRect(int x, int y, int width, int height,
                           boolean raised) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info().log(toShortString() +
                       " Filling 3D rect: " +
                       new Rectangle(x, y, width, height) +
                       " Raised bezel: " + raised);
        }
        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.fill3DRect(x, y, width, height, raised);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            Color oldColor = getColor();
            int i, count = (info.flashCount * 2) - 1;

            for (i = 0; i < count; i++) {
                graphics.setColor((i % 2) == 0 ? info.flashColor : oldColor);
                graphics.fill3DRect(x, y, width, height, raised);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
            graphics.setColor(oldColor);
        }
        graphics.fill3DRect(x, y, width, height, raised);
    }

    /**
     * Overrides <code>Graphics.drawOval</code>.
     */
    public void drawOval(int x, int y, int width, int height) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info().log(toShortString() +
                      " Drawing oval: " +
                      new Rectangle(x, y, width, height));
        }
        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.drawOval(x, y, width, height);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            Color oldColor = getColor();
            int i, count = (info.flashCount * 2) - 1;

            for (i = 0; i < count; i++) {
                graphics.setColor((i % 2) == 0 ? info.flashColor : oldColor);
                graphics.drawOval(x, y, width, height);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
            graphics.setColor(oldColor);
        }
        graphics.drawOval(x, y, width, height);
    }

    /**
     * Overrides <code>Graphics.fillOval</code>.
     */
    public void fillOval(int x, int y, int width, int height) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info().log(toShortString() +
                      " Filling oval: " +
                      new Rectangle(x, y, width, height));
        }
        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.fillOval(x, y, width, height);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            Color oldColor = getColor();
            int i, count = (info.flashCount * 2) - 1;

            for (i = 0; i < count; i++) {
                graphics.setColor((i % 2) == 0 ? info.flashColor : oldColor);
                graphics.fillOval(x, y, width, height);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
            graphics.setColor(oldColor);
        }
        graphics.fillOval(x, y, width, height);
    }

    /**
     * Overrides <code>Graphics.drawArc</code>.
     */
    public void drawArc(int x, int y, int width, int height,
                        int startAngle, int arcAngle) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info().log(toShortString() +
                      " Drawing arc: " +
                      new Rectangle(x, y, width, height) +
                      " startAngle: " + startAngle +
                      " arcAngle: " + arcAngle);
        }
        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.drawArc(x, y, width, height,
                                      startAngle, arcAngle);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            Color oldColor = getColor();
            int i, count = (info.flashCount * 2) - 1;

            for (i = 0; i < count; i++) {
                graphics.setColor((i % 2) == 0 ? info.flashColor : oldColor);
                graphics.drawArc(x, y, width, height, startAngle, arcAngle);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
            graphics.setColor(oldColor);
        }
        graphics.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    /**
     * Overrides <code>Graphics.fillArc</code>.
     */
    public void fillArc(int x, int y, int width, int height,
                        int startAngle, int arcAngle) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info().log(toShortString() +
                      " Filling arc: " +
                      new Rectangle(x, y, width, height) +
                      " startAngle: " + startAngle +
                      " arcAngle: " + arcAngle);
        }
        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.fillArc(x, y, width, height,
                                      startAngle, arcAngle);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            Color oldColor = getColor();
            int i, count = (info.flashCount * 2) - 1;

            for (i = 0; i < count; i++) {
                graphics.setColor((i % 2) == 0 ? info.flashColor : oldColor);
                graphics.fillArc(x, y, width, height, startAngle, arcAngle);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
            graphics.setColor(oldColor);
        }
        graphics.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    /**
     * Overrides <code>Graphics.drawPolyline</code>.
     */
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info().log(toShortString() +
                      " Drawing polyline: " +
                      " nPoints: " + nPoints +
                      " X's: " + xPoints +
                      " Y's: " + yPoints);
        }
        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.drawPolyline(xPoints, yPoints, nPoints);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            Color oldColor = getColor();
            int i, count = (info.flashCount * 2) - 1;

            for (i = 0; i < count; i++) {
                graphics.setColor((i % 2) == 0 ? info.flashColor : oldColor);
                graphics.drawPolyline(xPoints, yPoints, nPoints);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
            graphics.setColor(oldColor);
        }
        graphics.drawPolyline(xPoints, yPoints, nPoints);
    }

    /**
     * Overrides <code>Graphics.drawPolygon</code>.
     */
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info().log(toShortString() +
                      " Drawing polygon: " +
                      " nPoints: " + nPoints +
                      " X's: " + xPoints +
                      " Y's: " + yPoints);
        }
        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.drawPolygon(xPoints, yPoints, nPoints);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            Color oldColor = getColor();
            int i, count = (info.flashCount * 2) - 1;

            for (i = 0; i < count; i++) {
                graphics.setColor((i % 2) == 0 ? info.flashColor : oldColor);
                graphics.drawPolygon(xPoints, yPoints, nPoints);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
            graphics.setColor(oldColor);
        }
        graphics.drawPolygon(xPoints, yPoints, nPoints);
    }

    /**
     * Overrides <code>Graphics.fillPolygon</code>.
     */
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info().log(toShortString() +
                      " Filling polygon: " +
                      " nPoints: " + nPoints +
                      " X's: " + xPoints +
                      " Y's: " + yPoints);
        }
        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.fillPolygon(xPoints, yPoints, nPoints);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            Color oldColor = getColor();
            int i, count = (info.flashCount * 2) - 1;

            for (i = 0; i < count; i++) {
                graphics.setColor((i % 2) == 0 ? info.flashColor : oldColor);
                graphics.fillPolygon(xPoints, yPoints, nPoints);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
            graphics.setColor(oldColor);
        }
        graphics.fillPolygon(xPoints, yPoints, nPoints);
    }

    /**
     * Overrides <code>Graphics.drawString</code>.
     */
    public void drawString(String aString, int x, int y) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info().log(toShortString() +
                       " Drawing string: \"" + aString +
                       "\" at: " + new Point(x, y));
        }

        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.drawString(aString, x, y);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            Color oldColor = getColor();
            int i, count = (info.flashCount * 2) - 1;

            for (i = 0; i < count; i++) {
                graphics.setColor((i % 2) == 0 ? info.flashColor
                                  : oldColor);
                graphics.drawString(aString, x, y);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
            graphics.setColor(oldColor);
        }
        graphics.drawString(aString, x, y);
    }

    /**
     * Overrides <code>Graphics.drawString</code>.
     */
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info().log(toShortString() +
                       " Drawing text: \"" + iterator +
                       "\" at: " + new Point(x, y));
        }

        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.drawString(iterator, x, y);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            Color oldColor = getColor();
            int i, count = (info.flashCount * 2) - 1;

            for (i = 0; i < count; i++) {
                graphics.setColor((i % 2) == 0 ? info.flashColor
                                  : oldColor);
                graphics.drawString(iterator, x, y);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
            graphics.setColor(oldColor);
        }
        graphics.drawString(iterator, x, y);
    }

    /**
     * Overrides <code>Graphics.drawBytes</code>.
     */
    public void drawBytes(byte[] data, int offset, int length, int x, int y) {
        DebugGraphicsInfo info = info();

        Font font = graphics.getFont();

        if (debugLog()) {
            info().log(toShortString() +
                       " Drawing bytes at: " + new Point(x, y));
        }

        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.drawBytes(data, offset, length, x, y);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            Color oldColor = getColor();
            int i, count = (info.flashCount * 2) - 1;

            for (i = 0; i < count; i++) {
                graphics.setColor((i % 2) == 0 ? info.flashColor
                                  : oldColor);
                graphics.drawBytes(data, offset, length, x, y);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
            graphics.setColor(oldColor);
        }
        graphics.drawBytes(data, offset, length, x, y);
    }

    /**
     * Overrides <code>Graphics.drawChars</code>.
     */
    public void drawChars(char[] data, int offset, int length, int x, int y) {
        DebugGraphicsInfo info = info();

        Font font = graphics.getFont();

        if (debugLog()) {
            info().log(toShortString() +
                       " Drawing chars at " +  new Point(x, y));
        }

        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.drawChars(data, offset, length, x, y);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            Color oldColor = getColor();
            int i, count = (info.flashCount * 2) - 1;

            for (i = 0; i < count; i++) {
                graphics.setColor((i % 2) == 0 ? info.flashColor
                                  : oldColor);
                graphics.drawChars(data, offset, length, x, y);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
            graphics.setColor(oldColor);
        }
        graphics.drawChars(data, offset, length, x, y);
    }

    /**
     * Overrides <code>Graphics.drawImage</code>.
     */
    public boolean drawImage(Image img, int x, int y,
                             ImageObserver observer) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info.log(toShortString() +
                     " Drawing image: " + img +
                     " at: " + new Point(x, y));
        }

        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.drawImage(img, x, y, observer);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            int i, count = (info.flashCount * 2) - 1;
            ImageProducer oldProducer = img.getSource();
            ImageProducer newProducer
                = new FilteredImageSource(oldProducer,
                                new DebugGraphicsFilter(info.flashColor));
            Image newImage
                = Toolkit.getDefaultToolkit().createImage(newProducer);
            DebugGraphicsObserver imageObserver
                = new DebugGraphicsObserver();

            Image imageToDraw;
            for (i = 0; i < count; i++) {
                imageToDraw = (i % 2) == 0 ? newImage : img;
                loadImage(imageToDraw);
                graphics.drawImage(imageToDraw, x, y,
                                   imageObserver);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
        }
        return graphics.drawImage(img, x, y, observer);
    }

    /**
     * Overrides <code>Graphics.drawImage</code>.
     */
    public boolean drawImage(Image img, int x, int y, int width, int height,
                             ImageObserver observer) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info.log(toShortString() +
                     " Drawing image: " + img +
                     " at: " + new Rectangle(x, y, width, height));
        }

        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.drawImage(img, x, y, width, height, observer);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            int i, count = (info.flashCount * 2) - 1;
            ImageProducer oldProducer = img.getSource();
            ImageProducer newProducer
                = new FilteredImageSource(oldProducer,
                                new DebugGraphicsFilter(info.flashColor));
            Image newImage
                = Toolkit.getDefaultToolkit().createImage(newProducer);
            DebugGraphicsObserver imageObserver
                = new DebugGraphicsObserver();

            Image imageToDraw;
            for (i = 0; i < count; i++) {
                imageToDraw = (i % 2) == 0 ? newImage : img;
                loadImage(imageToDraw);
                graphics.drawImage(imageToDraw, x, y,
                                   width, height, imageObserver);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
        }
        return graphics.drawImage(img, x, y, width, height, observer);
    }

    /**
     * Overrides <code>Graphics.drawImage</code>.
     */
    public boolean drawImage(Image img, int x, int y,
                             Color bgcolor,
                             ImageObserver observer) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info.log(toShortString() +
                     " Drawing image: " + img +
                     " at: " + new Point(x, y) +
                     ", bgcolor: " + bgcolor);
        }

        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.drawImage(img, x, y, bgcolor, observer);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            int i, count = (info.flashCount * 2) - 1;
            ImageProducer oldProducer = img.getSource();
            ImageProducer newProducer
                = new FilteredImageSource(oldProducer,
                                new DebugGraphicsFilter(info.flashColor));
            Image newImage
                = Toolkit.getDefaultToolkit().createImage(newProducer);
            DebugGraphicsObserver imageObserver
                = new DebugGraphicsObserver();

            Image imageToDraw;
            for (i = 0; i < count; i++) {
                imageToDraw = (i % 2) == 0 ? newImage : img;
                loadImage(imageToDraw);
                graphics.drawImage(imageToDraw, x, y,
                                   bgcolor, imageObserver);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
        }
        return graphics.drawImage(img, x, y, bgcolor, observer);
    }

    /**
     * Overrides <code>Graphics.drawImage</code>.
     */
    public boolean drawImage(Image img, int x, int y,int width, int height,
                             Color bgcolor,
                             ImageObserver observer) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info.log(toShortString() +
                     " Drawing image: " + img +
                     " at: " + new Rectangle(x, y, width, height) +
                     ", bgcolor: " + bgcolor);
        }

        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.drawImage(img, x, y, width, height,
                                        bgcolor, observer);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            int i, count = (info.flashCount * 2) - 1;
            ImageProducer oldProducer = img.getSource();
            ImageProducer newProducer
                = new FilteredImageSource(oldProducer,
                                new DebugGraphicsFilter(info.flashColor));
            Image newImage
                = Toolkit.getDefaultToolkit().createImage(newProducer);
            DebugGraphicsObserver imageObserver
                = new DebugGraphicsObserver();

            Image imageToDraw;
            for (i = 0; i < count; i++) {
                imageToDraw = (i % 2) == 0 ? newImage : img;
                loadImage(imageToDraw);
                graphics.drawImage(imageToDraw, x, y,
                                   width, height, bgcolor, imageObserver);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
        }
        return graphics.drawImage(img, x, y, width, height, bgcolor, observer);
    }

    /**
     * Overrides <code>Graphics.drawImage</code>.
     */
    public boolean drawImage(Image img,
                             int dx1, int dy1, int dx2, int dy2,
                             int sx1, int sy1, int sx2, int sy2,
                             ImageObserver observer) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info.log(toShortString() +
                     " Drawing image: " + img +
                     " destination: " + new Rectangle(dx1, dy1, dx2, dy2) +
                     " source: " + new Rectangle(sx1, sy1, sx2, sy2));
        }

        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.drawImage(img, dx1, dy1, dx2, dy2,
                                        sx1, sy1, sx2, sy2, observer);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            int i, count = (info.flashCount * 2) - 1;
            ImageProducer oldProducer = img.getSource();
            ImageProducer newProducer
                = new FilteredImageSource(oldProducer,
                                new DebugGraphicsFilter(info.flashColor));
            Image newImage
                = Toolkit.getDefaultToolkit().createImage(newProducer);
            DebugGraphicsObserver imageObserver
                = new DebugGraphicsObserver();

            Image imageToDraw;
            for (i = 0; i < count; i++) {
                imageToDraw = (i % 2) == 0 ? newImage : img;
                loadImage(imageToDraw);
                graphics.drawImage(imageToDraw,
                                   dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2,
                                   imageObserver);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
        }
        return graphics.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2,
                                  observer);
    }

    /**
     * Overrides <code>Graphics.drawImage</code>.
     */
    public boolean drawImage(Image img,
                             int dx1, int dy1, int dx2, int dy2,
                             int sx1, int sy1, int sx2, int sy2,
                             Color bgcolor,
                             ImageObserver observer) {
        DebugGraphicsInfo info = info();

        if (debugLog()) {
            info.log(toShortString() +
                     " Drawing image: " + img +
                     " destination: " + new Rectangle(dx1, dy1, dx2, dy2) +
                     " source: " + new Rectangle(sx1, sy1, sx2, sy2) +
                     ", bgcolor: " + bgcolor);
        }

        if (isDrawingBuffer()) {
            if (debugBuffered()) {
                Graphics debugGraphics = debugGraphics();

                debugGraphics.drawImage(img, dx1, dy1, dx2, dy2,
                                        sx1, sy1, sx2, sy2, bgcolor, observer);
                debugGraphics.dispose();
            }
        } else if (debugFlash()) {
            int i, count = (info.flashCount * 2) - 1;
            ImageProducer oldProducer = img.getSource();
            ImageProducer newProducer
                = new FilteredImageSource(oldProducer,
                                new DebugGraphicsFilter(info.flashColor));
            Image newImage
                = Toolkit.getDefaultToolkit().createImage(newProducer);
            DebugGraphicsObserver imageObserver
                = new DebugGraphicsObserver();

            Image imageToDraw;
            for (i = 0; i < count; i++) {
                imageToDraw = (i % 2) == 0 ? newImage : img;
                loadImage(imageToDraw);
                graphics.drawImage(imageToDraw,
                                   dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2,
                                   bgcolor, imageObserver);
                Toolkit.getDefaultToolkit().sync();
                sleep(info.flashTime);
            }
        }
        return graphics.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2,
                                  bgcolor, observer);
    }

    static void loadImage(Image img) {
        imageLoadingIcon.loadImage(img);
    }


    /**
     * Overrides <code>Graphics.copyArea</code>.
     */
    public void copyArea(int x, int y, int width, int height,
                         int destX, int destY) {
        if (debugLog()) {
            info().log(toShortString() +
                      " Copying area from: " +
                      new Rectangle(x, y, width, height) +
                      " to: " + new Point(destX, destY));
        }
        graphics.copyArea(x, y, width, height, destX, destY);
    }

    final void sleep(int mSecs) {
        try {
            Thread.sleep(mSecs);
        } catch (Exception e) {
        }
    }

    /**
     * Overrides <code>Graphics.dispose</code>.
     */
    public void dispose() {
        graphics.dispose();
        graphics = null;
    }

    // ALERT!
    /**
     * Returns the drawingBuffer value.
     *
     * @return true if this object is drawing from a Buffer
     */
    public boolean isDrawingBuffer() {
        return buffer != null;
    }

    String toShortString() {
        return "Graphics" + (isDrawingBuffer() ? "<B>" : "") + "(" + graphicsID + "-" + debugOptions + ")";
    }

    String pointToString(int x, int y) {
        return "(" + x + ", " + y + ")";
    }

    /** Enables/disables diagnostic information about every graphics
      * operation. The value of <b>options</b> indicates how this information
      * should be displayed. LOG_OPTION causes a text message to be printed.
      * FLASH_OPTION causes the drawing to flash several times. BUFFERED_OPTION
      * creates a new Frame that shows each operation on an
      * offscreen buffer. The value of <b>options</b> is bitwise OR'd into
      * the current value. To disable debugging use NONE_OPTION.
      *
      * @param options indicates how diagnostic information should be displayed
      */
    public void setDebugOptions(int options) {
        if (options != 0) {
            if (options == NONE_OPTION) {
                if (debugOptions != 0) {
                    System.err.println(toShortString() + " Disabling debug");
                    debugOptions = 0;
                }
            } else {
                if (debugOptions != options) {
                    debugOptions |= options;
                    if (debugLog()) {
                        System.err.println(toShortString() + " Enabling debug");
                    }
                }
            }
        }
    }

    /**
     * Returns the current debugging options for this DebugGraphics.
     *
     * @return the current debugging options for this DebugGraphics
     * @see #setDebugOptions
     */
    public int getDebugOptions() {
        return debugOptions;
    }

    /** Static wrapper method for DebugGraphicsInfo.setDebugOptions(). Stores
      * options on a per component basis.
      */
    static void setDebugOptions(JComponent component, int options) {
        info().setDebugOptions(component, options);
    }

    /** Static wrapper method for DebugGraphicsInfo.getDebugOptions().
      */
    static int getDebugOptions(JComponent component) {
        DebugGraphicsInfo debugGraphicsInfo = info();
        if (debugGraphicsInfo == null) {
            return 0;
        } else {
            return debugGraphicsInfo.getDebugOptions(component);
        }
    }

    /** Returns non-zero if <b>component</b> should display with DebugGraphics,
      * zero otherwise. Walks the JComponent's parent tree to determine if
      * any debugging options have been set.
      */
    static int shouldComponentDebug(JComponent component) {
        DebugGraphicsInfo info = info();
        if (info == null) {
            return 0;
        } else {
            Container container = (Container)component;
            int debugOptions = 0;

            while (container != null && (container instanceof JComponent)) {
                debugOptions |= info.getDebugOptions((JComponent)container);
                container = container.getParent();
            }

            return debugOptions;
        }
    }

    /** Returns the number of JComponents that have debugging options turned
      * on.
      */
    static int debugComponentCount() {
        DebugGraphicsInfo debugGraphicsInfo = info();
        if (debugGraphicsInfo != null &&
                    debugGraphicsInfo.componentToDebug != null) {
            return debugGraphicsInfo.componentToDebug.size();
        } else {
            return 0;
        }
    }

    boolean debugLog() {
        return (debugOptions & LOG_OPTION) == LOG_OPTION;
    }

    boolean debugFlash() {
        return (debugOptions & FLASH_OPTION) == FLASH_OPTION;
    }

    boolean debugBuffered() {
        return (debugOptions & BUFFERED_OPTION) == BUFFERED_OPTION;
    }

    /** Returns a DebugGraphics for use in buffering window.
      */
    @SuppressWarnings("deprecation")
    private Graphics debugGraphics() {
        DebugGraphics        debugGraphics;
        DebugGraphicsInfo    info = info();
        JFrame               debugFrame;

        if (info.debugFrame == null) {
            info.debugFrame = new JFrame();
            info.debugFrame.setSize(500, 500);
        }
        debugFrame = info.debugFrame;
        debugFrame.show();
        debugGraphics = new DebugGraphics(debugFrame.getGraphics());
        debugGraphics.setFont(getFont());
        debugGraphics.setColor(getColor());
        debugGraphics.translate(xOffset, yOffset);
        debugGraphics.setClip(getClipBounds());
        if (debugFlash()) {
            debugGraphics.setDebugOptions(FLASH_OPTION);
        }
        return debugGraphics;
    }

    /** Returns DebugGraphicsInfo, or creates one if none exists.
      */
    static DebugGraphicsInfo info() {
        DebugGraphicsInfo debugGraphicsInfo = (DebugGraphicsInfo)
            SwingUtilities.appContextGet(debugGraphicsInfoKey);
        if (debugGraphicsInfo == null) {
            debugGraphicsInfo = new DebugGraphicsInfo();
            SwingUtilities.appContextPut(debugGraphicsInfoKey,
                                         debugGraphicsInfo);
        }
        return debugGraphicsInfo;
    }
    private static final Class<DebugGraphicsInfo> debugGraphicsInfoKey = DebugGraphicsInfo.class;
}
