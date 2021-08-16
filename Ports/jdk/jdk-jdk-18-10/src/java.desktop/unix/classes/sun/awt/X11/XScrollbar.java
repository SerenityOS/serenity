/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.X11;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import sun.awt.SunToolkit;
import sun.awt.X11GraphicsConfig;
import sun.util.logging.PlatformLogger;

/**
* A simple vertical scroll bar.
*/
abstract class XScrollbar {

    private static PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11.XScrollbar");
    /**
     * The thread that asynchronously tells the scrollbar to scroll.
     * @see #startScrolling
     */
    private static XScrollRepeater scroller = new XScrollRepeater(null);
    /*
     * The repeater that used for concurrent scrolling of the vertical and horizontal scrollbar
     * And so there is not static keyword
     * See 6243382 for more information
     */
    private XScrollRepeater i_scroller = new XScrollRepeater(null);

    // Thumb length is always >= MIN_THUMB_H
    private static final int MIN_THUMB_H = 5;

    private static final int ARROW_IND = 1;

    XScrollbarClient sb;

    //Use set methods to set scrollbar parameters
    private int val;
    private int min;
    private int max;
    private int vis;

    private int line;
    private int page;
    private boolean needsRepaint = true;
    private boolean pressed = false;
    private boolean dragging = false;

    Polygon firstArrow, secondArrow;

    int width, height; // Dimensions of the visible part of the parent window
    int barWidth, barLength; // Rotation-independent values,
                             // equal to (width, height) for vertical,
                             // rotated by 90 for horizontal.
                             // That is, barLength is always the length between
                             // the tips of the arrows.
    int arrowArea;     // The area reserved for the scroll arrows
    int alignment;
    public static final int ALIGNMENT_VERTICAL = 1, ALIGNMENT_HORIZONTAL = 2;

    int mode;
    Point thumbOffset;
    private Rectangle prevThumb;

    public XScrollbar(int alignment, XScrollbarClient sb) {
        this.sb = sb;
        this.alignment = alignment;
    }

    public boolean needsRepaint() {
        return needsRepaint;
    }

    void notifyValue(int v) {
        notifyValue(v, false);
    }

    void notifyValue(int v, final boolean isAdjusting) {
        if (v < min) {
            v = min;
        } else if (v > max - vis) {
            v = max - vis;
        }
        final int value = v;
        final int mode = this.mode;
        if ((sb != null) && ((value != val)||(!pressed))) {
            SunToolkit.executeOnEventHandlerThread(sb.getEventSource(), new Runnable() {
                    public void run() {
                        sb.notifyValue(XScrollbar.this, mode, value, isAdjusting);
                    }
                });
        }
    }

    protected abstract void rebuildArrows();

    public void setSize(int width, int height) {
        if (log.isLoggable(PlatformLogger.Level.FINER)) {
            log.finer("Setting scroll bar " + this + " size to " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
    }

    /**
     * Creates oriented directed arrow
     */
    protected Polygon createArrowShape(boolean vertical, boolean up) {
        Polygon arrow = new Polygon();
        // TODO: this should be done polymorphically in subclasses
        // FIXME: arrows overlap the thumb for very wide scrollbars
        if (vertical) {
            int x = width / 2 - getArrowWidth()/2;
            int y1 = (up ? ARROW_IND : barLength - ARROW_IND);
            int y2 = (up ? getArrowWidth() : barLength - getArrowWidth() - ARROW_IND);
            arrow.addPoint(x + getArrowWidth()/2, y1);
            arrow.addPoint(x + getArrowWidth(), y2);
            arrow.addPoint(x, y2);
            arrow.addPoint(x + getArrowWidth()/2, y1);
        } else {
            int y = height / 2 - getArrowWidth()/2;
            int x1 = (up ? ARROW_IND : barLength - ARROW_IND);
            int x2 = (up ? getArrowWidth() : barLength - getArrowWidth() - ARROW_IND);
            arrow.addPoint(x1, y + getArrowWidth()/2);
            arrow.addPoint(x2, y + getArrowWidth());
            arrow.addPoint(x2, y);
            arrow.addPoint(x1, y + getArrowWidth()/2);
        }
        return arrow;
    }

    /**
     * Gets the area of the scroll track
     */
    protected abstract Rectangle getThumbArea();

    /**
     * paint the scrollbar
     * @param g the graphics context to paint into
     * @param colors the colors to use when painting the scrollbar
     * @param paintAll paint the whole scrollbar if true, just the thumb is false
     */
    void paint(Graphics g, Color[] colors, boolean paintAll) {
        if (log.isLoggable(PlatformLogger.Level.FINER)) {
            log.finer("Painting scrollbar " + this);
        }

        boolean useBufferedImage = false;
        Graphics2D g2 = null;
        BufferedImage buffer = null;
        if (!(g instanceof Graphics2D)) {
            // Fix for 5045936, 5055171. While printing, g is an instance
            //   of sun.print.ProxyPrintGraphics which extends Graphics.
            //   So we use a separate buffered image and its graphics is
            //   always Graphics2D instance
            X11GraphicsConfig graphicsConfig = (X11GraphicsConfig)(sb.getEventSource().getGraphicsConfiguration());
            buffer = graphicsConfig.createCompatibleImage(width, height);
            g2 = buffer.createGraphics();
            useBufferedImage = true;
        } else {
            g2 = (Graphics2D)g;
        }
        try {
            Rectangle thumbRect = calculateThumbRect();

//              if (prevH == thumbH && prevY == thumbPosY) {
//                  return;
//              }

            prevThumb = thumbRect;

            // TODO: Share Motif colors
            Color back = colors[XComponentPeer.BACKGROUND_COLOR];
            Color selectColor = new Color(MotifColorUtilities.calculateSelectFromBackground(back.getRed(),back.getGreen(),back.getBlue()));
            Color darkShadow = new Color(MotifColorUtilities.calculateBottomShadowFromBackground(back.getRed(),back.getGreen(),back.getBlue()));
            Color lightShadow = new Color(MotifColorUtilities.calculateTopShadowFromBackground(back.getRed(),back.getGreen(),back.getBlue()));

            XToolkit.awtLock();
            try {
                XlibWrapper.XFlush(XToolkit.getDisplay());
            } finally {
                XToolkit.awtUnlock();
            }
            /* paint the background slightly darker */
            if (paintAll) {
                // fill the entire background
                g2.setColor(selectColor);
                if (alignment == ALIGNMENT_HORIZONTAL) {
                    g2.fillRect(0, 0, thumbRect.x, height);
                    g2.fillRect(thumbRect.x + thumbRect.width , 0, width - (thumbRect.x + thumbRect.width), height);
                } else {
                    g2.fillRect(0, 0, width, thumbRect.y);
                    g2.fillRect(0, thumbRect.y + thumbRect.height, width, height - (thumbRect.y + thumbRect.height));
                }

                // Paint edges
                // TODO: Share Motif 3d rect drawing

                g2.setColor(darkShadow);
                g2.drawLine(0, 0, width-1, 0);           // top
                g2.drawLine(0, 0, 0, height-1);          // left

                g2.setColor(lightShadow);
                g2.drawLine(1, height-1, width-1, height-1); // bottom
                g2.drawLine(width-1, 1, width-1, height-1);  // right
            } else {
                // Clear all thumb area
                g2.setColor(selectColor);
                Rectangle thumbArea = getThumbArea();
                g2.fill(thumbArea);
            }

            if (paintAll) {
                // ************ paint the arrows
                 paintArrows(g2, colors[XComponentPeer.BACKGROUND_COLOR], darkShadow, lightShadow );

            }

            // Thumb
            g2.setColor(colors[XComponentPeer.BACKGROUND_COLOR]);
            g2.fillRect(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height);

            g2.setColor(lightShadow);
            g2.drawLine(thumbRect.x, thumbRect.y,
                       thumbRect.x + thumbRect.width, thumbRect.y); // top
            g2.drawLine(thumbRect.x, thumbRect.y,
                       thumbRect.x, thumbRect.y+thumbRect.height); // left

            g2.setColor(darkShadow);
            g2.drawLine(thumbRect.x+1,
                       thumbRect.y+thumbRect.height,
                       thumbRect.x+thumbRect.width,
                       thumbRect.y+thumbRect.height);  // bottom
            g2.drawLine(thumbRect.x+thumbRect.width,
                       thumbRect.y+1,
                       thumbRect.x+thumbRect.width,
                       thumbRect.y+thumbRect.height); // right
        } finally {
            if (useBufferedImage) {
                g2.dispose();
            }
        }
        if (useBufferedImage) {
            g.drawImage(buffer, 0, 0, null);
        }
        XToolkit.awtLock();
        try {
            XlibWrapper.XFlush(XToolkit.getDisplay());
        } finally {
            XToolkit.awtUnlock();
        }
    }

      void paintArrows(Graphics2D g, Color background, Color darkShadow, Color lightShadow) {

          g.setColor(background);

        // paint firstArrow
        if (pressed && (mode == AdjustmentEvent.UNIT_DECREMENT)) {
            g.fill(firstArrow);
            g.setColor(lightShadow);
            g.drawLine(firstArrow.xpoints[0],firstArrow.ypoints[0],
                    firstArrow.xpoints[1],firstArrow.ypoints[1]);
            g.drawLine(firstArrow.xpoints[1],firstArrow.ypoints[1],
                    firstArrow.xpoints[2],firstArrow.ypoints[2]);
            g.setColor(darkShadow);
            g.drawLine(firstArrow.xpoints[2],firstArrow.ypoints[2],
                    firstArrow.xpoints[0],firstArrow.ypoints[0]);

        }
        else {
            g.fill(firstArrow);
            g.setColor(darkShadow);
            g.drawLine(firstArrow.xpoints[0],firstArrow.ypoints[0],
                    firstArrow.xpoints[1],firstArrow.ypoints[1]);
            g.drawLine(firstArrow.xpoints[1],firstArrow.ypoints[1],
                    firstArrow.xpoints[2],firstArrow.ypoints[2]);
            g.setColor(lightShadow);
            g.drawLine(firstArrow.xpoints[2],firstArrow.ypoints[2],
                    firstArrow.xpoints[0],firstArrow.ypoints[0]);

        }

        g.setColor(background);
        // paint second Arrow
        if (pressed && (mode == AdjustmentEvent.UNIT_INCREMENT)) {
            g.fill(secondArrow);
            g.setColor(lightShadow);
            g.drawLine(secondArrow.xpoints[0],secondArrow.ypoints[0],
                    secondArrow.xpoints[1],secondArrow.ypoints[1]);
            g.setColor(darkShadow);
            g.drawLine(secondArrow.xpoints[1],secondArrow.ypoints[1],
                    secondArrow.xpoints[2],secondArrow.ypoints[2]);
            g.drawLine(secondArrow.xpoints[2],secondArrow.ypoints[2],
                    secondArrow.xpoints[0],secondArrow.ypoints[0]);

        }
        else {
            g.fill(secondArrow);
            g.setColor(darkShadow);
            g.drawLine(secondArrow.xpoints[0],secondArrow.ypoints[0],
                    secondArrow.xpoints[1],secondArrow.ypoints[1]);
            g.setColor(lightShadow);
            g.drawLine(secondArrow.xpoints[1],secondArrow.ypoints[1],
                    secondArrow.xpoints[2],secondArrow.ypoints[2]);
            g.drawLine(secondArrow.xpoints[2],secondArrow.ypoints[2],
                    secondArrow.xpoints[0],secondArrow.ypoints[0]);

        }

    }

    /**
     * Tell the scroller to start scrolling.
     */
    void startScrolling() {
        if (log.isLoggable(PlatformLogger.Level.FINER)) {
            log.finer("Start scrolling on " + this);
        }
        // Make sure that we scroll at least once
        scroll();

        // wake up the scroll repeater
        if (scroller == null) {
            // If there isn't a scroller, then create
            // one and start it.
            scroller = new XScrollRepeater(this);
        } else {
            scroller.setScrollbar(this);
        }
        scroller.start();
    }

    /**
     * Tell the instance scroller to start scrolling.
     * See 6243382 for more information
     */
    void startScrollingInstance() {
        if (log.isLoggable(PlatformLogger.Level.FINER)) {
            log.finer("Start scrolling on " + this);
        }
        // Make sure that we scroll at least once
        scroll();

        i_scroller.setScrollbar(this);
        i_scroller.start();
    }

    /**
     * Tell the instance scroller to stop scrolling.
     * See 6243382 for more information
     */
    void stopScrollingInstance() {
        if (log.isLoggable(PlatformLogger.Level.FINER)) {
            log.finer("Stop scrolling on " + this);
        }

        i_scroller.stop();
    }

    /**
     * The set method for mode property.
     * See 6243382 for more information
     */
    public void setMode(int mode){
        this.mode = mode;
    }

    /**
     * Scroll one unit.
     * @see #notifyValue
     */
    void scroll() {
        switch (mode) {
          case AdjustmentEvent.UNIT_DECREMENT:
              notifyValue(val - line);
              return;

          case AdjustmentEvent.UNIT_INCREMENT:
              notifyValue(val + line);
              return;

          case AdjustmentEvent.BLOCK_DECREMENT:
              notifyValue(val - page);
              return;

          case AdjustmentEvent.BLOCK_INCREMENT:
              notifyValue(val + page);
              return;
        }
        return;
    }

    boolean isInArrow(int x, int y) {
        // Mouse is considered to be in the arrow if it is anywhere in the
        // arrow area.
        int coord = (alignment == ALIGNMENT_HORIZONTAL ? x : y);
        int arrAreaH = getArrowAreaWidth();

        if (coord < arrAreaH || coord > barLength - arrAreaH + 1) {
            return true;
        }
        return false;
    }

    /**
     * Is x,y in the scroll thumb?
     *
     * If we ever cache the thumb rect, we may need to clone the result of
     * calculateThumbRect().
     */
    boolean isInThumb(int x, int y) {
        Rectangle thumbRect = calculateThumbRect();

        // If the mouse is in the shadow of the thumb or the shadow of the
        // scroll track, treat it as hitting the thumb.  So, slightly enlarge
        // our rectangle.
        thumbRect.x -= 1;
        thumbRect.width += 3;
        thumbRect.height += 1;
        return thumbRect.contains(x,y);
    }

    abstract boolean beforeThumb(int x, int y);

    /**
     *
     * @see java.awt.event.MouseEvent
     * MouseEvent.MOUSE_CLICKED
     * MouseEvent.MOUSE_PRESSED
     * MouseEvent.MOUSE_RELEASED
     * MouseEvent.MOUSE_MOVED
     * MouseEvent.MOUSE_ENTERED
     * MouseEvent.MOUSE_EXITED
     * MouseEvent.MOUSE_DRAGGED
     */
    @SuppressWarnings("deprecation")
    public void handleMouseEvent(int id, int modifiers, int x, int y) {
        if ((modifiers & InputEvent.BUTTON1_MASK) == 0) {
            return;
        }

        if (log.isLoggable(PlatformLogger.Level.FINER)) {
             String type;
             switch (id) {
                case MouseEvent.MOUSE_PRESSED:
                    type = "press";
                    break;
                case MouseEvent.MOUSE_RELEASED:
                    type = "release";
                    break;
                case MouseEvent.MOUSE_DRAGGED:
                    type = "drag";
                    break;
                default:
                    type = "other";
             }
             log.finer("Mouse " + type + " event in scroll bar " + this +
                                                   "x = " + x + ", y = " + y +
                                                   ", on arrow: " + isInArrow(x, y) +
                                                   ", on thumb: " + isInThumb(x, y) + ", before thumb: " + beforeThumb(x, y)
                                                   + ", thumb rect" + calculateThumbRect());
        }
        switch (id) {
          case MouseEvent.MOUSE_PRESSED:
              if (isInArrow(x, y)) {
                  pressed = true;
                  if (beforeThumb(x, y)) {
                      mode = AdjustmentEvent.UNIT_DECREMENT;
                  } else {
                      mode = AdjustmentEvent.UNIT_INCREMENT;
                  }
                  sb.repaintScrollbarRequest(this);
                  startScrolling();
                  break;
              }

              if (isInThumb(x, y)) {
                  mode = AdjustmentEvent.TRACK;
              } else {
                  if (beforeThumb(x, y)) {
                      mode = AdjustmentEvent.BLOCK_DECREMENT;
                  } else {
                      mode = AdjustmentEvent.BLOCK_INCREMENT;
                  }
                  startScrolling();
              }
              Rectangle pos = calculateThumbRect();
              thumbOffset = new Point(x - pos.x, y - pos.y);
              break;

          case MouseEvent.MOUSE_RELEASED:
              pressed = false;
              sb.repaintScrollbarRequest(this);
              scroller.stop();
              if(dragging){
                  handleTrackEvent(x, y, false);
                  dragging=false;
              }
              break;

          case MouseEvent.MOUSE_DRAGGED:
              dragging = true;
              handleTrackEvent(x, y, true);
        }
    }

    private void handleTrackEvent(int x, int y, boolean isAdjusting){
        if (mode == AdjustmentEvent.TRACK) {
            notifyValue(calculateCursorOffset(x, y), isAdjusting);
        }
    }

    private int calculateCursorOffset(int x, int y){
        if (alignment == ALIGNMENT_HORIZONTAL) {
            if (dragging)
                return Math.max(0,(int)((x - (thumbOffset.x + getArrowAreaWidth()))/getScaleFactor())) + min;
            else
                return Math.max(0,(int)((x - (getArrowAreaWidth()))/getScaleFactor())) + min;
        } else {
            if (dragging)
                return Math.max(0,(int)((y - (thumbOffset.y + getArrowAreaWidth()))/getScaleFactor())) + min;
            else
                return Math.max(0,(int)((y - (getArrowAreaWidth()))/getScaleFactor())) + min;
        }
    }

/*
  private void updateNeedsRepaint() {
        Rectangle thumbRect = calculateThumbRect();
        if (!prevThumb.equals(thumbRect)) {
            needsRepaint = true;
        }
        prevThumb = thumbRect;
    }
    */

    /**
     * Sets the values for this Scrollbar.
     * This method enforces the same constraints as in java.awt.Scrollbar:
     * <UL>
     * <LI> The maximum must be greater than the minimum </LI>
     * <LI> The value must be greater than or equal to the minimum
     *      and less than or equal to the maximum minus the
     *      visible amount </LI>
     * <LI> The visible amount must be greater than 1 and less than or equal
     *      to the difference between the maximum and minimum values. </LI>
     * </UL>
     * Values which do not meet these criteria are quietly coerced to the
     * appropriate boundary value.
     * @param value is the position in the current window.
     * @param visible is the amount visible per page
     * @param minimum is the minimum value of the scrollbar
     * @param maximum is the maximum value of the scrollbar
     */
    synchronized void setValues(int value, int visible, int minimum, int maximum) {
        if (maximum <= minimum) {
            maximum = minimum + 1;
        }
        if (visible > maximum - minimum) {
            visible = maximum - minimum;
        }
        if (visible < 1) {
            visible = 1;
        }
        if (value < minimum) {
            value = minimum;
        }
        if (value > maximum - visible) {
            value = maximum - visible;
        }

        this.val = value;
        this.vis = visible;
        this.min = minimum;
        this.max = maximum;
    }

    /**
     * Sets param of this Scrollbar to the specified values.
     * @param value is the position in the current window.
     * @param visible is the amount visible per page
     * @param minimum is the minimum value of the scrollbar
     * @param maximum is the maximum value of the scrollbar
     * @param unitSize is the unit size for increment or decrement of the value
     * @see #setValues
     */
    synchronized void setValues(int value, int visible, int minimum, int maximum,
                                int unitSize, int blockSize) {
        /* Use setValues so that a consistent policy
         * relating minimum, maximum, and value is enforced.
         */
        setValues(value, visible, minimum, maximum);
        setUnitIncrement(unitSize);
        setBlockIncrement(blockSize);
    }

    /**
     * Returns the current value of this Scrollbar.
     * @see #getMinimum
     * @see #getMaximum
     */
    int getValue() {
        return val;
    }

    /**
     * Sets the value of this Scrollbar to the specified value.
     * @param newValue the new value of the Scrollbar. If this value is
     * below the current minimum or above the current maximum minus
     * the visible amount, it becomes the new one of those values,
     * respectively.
     * @see #getValue
     */
    synchronized void setValue(int newValue) {
        /* Use setValues so that a consistent policy
         * relating minimum, maximum, and value is enforced.
         */
        setValues(newValue, vis, min, max);
    }

    /**
     * Returns the minimum value of this Scrollbar.
     * @see #getMaximum
     * @see #getValue
     */
    int getMinimum() {
        return min;
    }

    /**
     * Sets the minimum value for this Scrollbar.
     * @param newMinimum the minimum value of the scrollbar
     */
    synchronized void setMinimum(int newMinimum) {
        /* Use setValues so that a consistent policy
         * relating minimum, maximum, and value is enforced.
         */
        setValues(val, vis, newMinimum, max);
    }

    /**
     * Returns the maximum value of this Scrollbar.
     * @see #getMinimum
     * @see #getValue
     */
    int getMaximum() {
        return max;
    }

    /**
     * Sets the maximum value for this Scrollbar.
     * @param newMaximum the maximum value of the scrollbar
     */
    synchronized void setMaximum(int newMaximum) {
        /* Use setValues so that a consistent policy
         * relating minimum, maximum, and value is enforced.
         */
        setValues(val, vis, min, newMaximum);
    }

    /**
     * Returns the visible amount of this Scrollbar.
     */
    int getVisibleAmount() {
        return vis;
    }

    /**
     * Sets the visible amount of this Scrollbar, which is the range
     * of values represented by the width of the scroll bar's bubble.
     * @param newAmount the amount visible per page
     */
    synchronized void setVisibleAmount(int newAmount) {
        setValues(val, newAmount, min, max);
    }

    /**
     * Sets the unit increment for this scrollbar. This is the value
     * that will be added (subtracted) when the user hits the unit down
     * (up) gadgets.
     * @param unitSize is the unit size for increment or decrement of the value
     */
    synchronized void setUnitIncrement(int unitSize) {
        line = unitSize;
    }

    /**
     * Gets the unit increment for this scrollbar.
     */
    int getUnitIncrement() {
        return line;
    }

    /**
     * Sets the block increment for this scrollbar. This is the value
     * that will be added (subtracted) when the user hits the block down
     * (up) gadgets.
     * @param blockSize is the block size for increment or decrement of the value
     */
    synchronized void setBlockIncrement(int blockSize) {
        page = blockSize;
    }

    /**
     * Gets the block increment for this scrollbar.
     */
    int getBlockIncrement() {
        return page;
    }

    /**
     * Width of the arrow image
     */
    int getArrowWidth() {
        return getArrowAreaWidth() - 2*ARROW_IND;
    }

    /**
     * Width of the area reserved for arrow
     */
    int getArrowAreaWidth() {
        return arrowArea;
    }

    void calculateArrowWidth() {
        if (barLength < 2*barWidth + MIN_THUMB_H + 2) {
            arrowArea = (barLength - MIN_THUMB_H + 2*ARROW_IND)/2 - 1;
        }
        else {
            arrowArea = barWidth - 1;
        }
    }

    /**
     * Returns the scale factor for the thumbArea ( thumbAreaH / (max - min)).
     * @see #getArrowAreaWidth
     */
    private double getScaleFactor(){
        double f = (double)(barLength - 2*getArrowAreaWidth()) / Math.max(1,(max - min));
        return f;
    }

    /**
     * Method to calculate the scroll thumb's size and position.  This is
     * based on CalcSliderRect in ScrollBar.c of Motif source.
     *
     * If we ever cache the thumb rect, we'll need to use a clone in
     * isInThumb().
     */
    protected Rectangle calculateThumbRect() {
        float range;
        float trueSize;  // Area of scroll track
        float factor;
        float slideSize;
        int minSliderWidth;
        int minSliderHeight;
        int hitTheWall = 0;
        int arrAreaH = getArrowAreaWidth();
        Rectangle retVal = new Rectangle(0,0,0,0);

        trueSize = barLength - 2*arrAreaH - 1;  // Same if vert or horiz

        if (alignment == ALIGNMENT_HORIZONTAL) {
            minSliderWidth = MIN_THUMB_H ;  // Base on user-set vis?
            minSliderHeight = height - 3;
        }
        else {  // Vertical
            minSliderWidth = width - 3;
            minSliderHeight = MIN_THUMB_H ;

        }

        // Total number of user units displayed
            range = max - min;

        // A naive notion of pixels per user unit
            factor = trueSize / range;

            // A naive notion of the size of the slider in pixels
            // in thermo, slider_size is 0 ans is ignored
            slideSize = vis * factor;

        if (alignment == ALIGNMENT_HORIZONTAL) {
            // Simulating MAX_SCROLLBAR_DIMENSION macro
            int localVal = (int) (slideSize + 0.5);
            int localMin = minSliderWidth;
            if (localVal > localMin) {
                retVal.width = localVal;
            }
            else {
                retVal.width = localMin;
                hitTheWall = localMin;
            }
            retVal.height = minSliderHeight;
        }
        else {  // Vertical
            retVal.width = minSliderWidth;

            // Simulating MAX_SCROLLBAR_DIMENSION macro
            int localVal = (int) (slideSize + 0.5);
            int localMin = minSliderHeight;
            if (localVal > localMin) {
                retVal.height = localVal;
            }
            else {
                retVal.height = localMin;
                hitTheWall = localMin;
            }
        }

        if (hitTheWall != 0) {
            trueSize -= hitTheWall;  // Actual pixels available
            range -= vis;            // Actual range
            factor = trueSize / range;
        }

        if (alignment == ALIGNMENT_HORIZONTAL) {
                    retVal.x = ((int) (((((float) val)
                        - ((float) min)) * factor) + 0.5))
                        + arrAreaH;
                    retVal.y = 1;

        }
        else {
            retVal.x = 1;
                    retVal.y = ((int) (((((float) val)
                        - ((float) min)) * factor) + 0.5))
                        + arrAreaH;
        }

        // There was one final adjustment here in the Motif function, which was
        // noted to be for backward-compatibility.  It has been left out for now.

        return retVal;
    }

    public String toString() {
        return getClass() + "[" + width + "x" + height + "," + barWidth + "x" + barLength + "]";
    }
}


class XScrollRepeater implements Runnable {
    /**
     * Time to pause before the first scroll repeat.
     */
    static int beginPause = 500;
    // Reminder - make this a user definable property

    /**
     * Time to pause between each scroll repeat.
     */
    static int repeatPause = 100;
    // Reminder - make this a user definable property

    /**
     * The scrollbar that we sending scrolling.
     */
    XScrollbar sb;

    /**
     * newScroll gets reset when a new scrollbar gets set.
     */
    boolean newScroll;


    boolean shouldSkip;

    /**
     * Creates a new scroll repeater.
     * @param sb the scrollbar that this thread will scroll
     */
    XScrollRepeater(XScrollbar sb) {
        this.setScrollbar(sb);
        newScroll = true;
    }

    public void start() {
        stop();
        shouldSkip = false;
        XToolkit.schedule(this, beginPause);
    }

    public void stop() {
        synchronized(this) {
            shouldSkip = true;
        }
        XToolkit.remove(this);
    }

    /**
     * Sets the scrollbar.
     * @param sb the scrollbar that this thread will scroll
     */
    public synchronized void setScrollbar(XScrollbar sb) {
        this.sb = sb;
        stop();
        newScroll = true;
    }

    public void run () {
        synchronized(this) {
            if (shouldSkip) {
                return;
            }
        }
        sb.scroll();
        XToolkit.schedule(this, repeatPause);
    }

}
