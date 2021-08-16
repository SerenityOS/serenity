/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.peer.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import java.awt.geom.AffineTransform;
import java.util.Objects;

import sun.util.logging.PlatformLogger;

class XCheckboxPeer extends XComponentPeer implements CheckboxPeer {

    private static final PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11.XCheckboxPeer");

    private static final Insets focusInsets = new Insets(0,0,0,0);
    private static final Insets borderInsets = new Insets(2,2,2,2);
    private static final int checkBoxInsetFromText = 2;

    //The check mark is less common than a plain "depressed" button,
    //so don't use the checkmark.
    // The checkmark shape:
    private static final double MASTER_SIZE = 128.0;
    private static final Polygon MASTER_CHECKMARK = new Polygon(
        new int[] {1, 25,56,124,124,85, 64},  // X-coords
        new int[] {59,35,67,  0, 12,66,123},  // Y-coords
      7);

    private Shape myCheckMark;

    private Color focusColor = SystemColor.windowText;

    private boolean pressed;
    private boolean armed;
    private boolean selected;

    private Rectangle textRect;
    private Rectangle focusRect;
    private int checkBoxSize;
    private int cbX;
    private int cbY;

    String label;
    CheckboxGroup checkBoxGroup;

    XCheckboxPeer(Checkbox target) {
        super(target);
        pressed = false;
        armed = false;
        selected = target.getState();
        label = target.getLabel();
        if ( label == null ) {
            label = "";
        }
        checkBoxGroup = target.getCheckboxGroup();
        updateMotifColors(getPeerBackground());
    }

    public void preInit(XCreateWindowParams params) {
        // Put this here so it is executed before layout() is called from
        // setFont() in XComponent.postInit()
        textRect = new Rectangle();
        focusRect = new Rectangle();
        super.preInit(params);
    }

    public boolean isFocusable() { return true; }

    public void focusGained(FocusEvent e) {
        // TODO: only need to paint the focus bit
        super.focusGained(e);
        repaint();
    }

    public void focusLost(FocusEvent e) {
        // TODO: only need to paint the focus bit?
        super.focusLost(e);
        repaint();
    }


    void handleJavaKeyEvent(KeyEvent e) {
        int i = e.getID();
        switch (i) {
          case KeyEvent.KEY_PRESSED:
              keyPressed(e);
              break;
          case KeyEvent.KEY_RELEASED:
              keyReleased(e);
              break;
          case KeyEvent.KEY_TYPED:
              keyTyped(e);
              break;
        }
    }

    public void keyTyped(KeyEvent e) {}

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE)
        {
            //pressed=true;
            //armed=true;
            //selected=!selected;
            action(!selected);
            //repaint();  // Gets the repaint from action()
        }

    }

    public void keyReleased(KeyEvent e) {}

    @Override
    public void setLabel(String label) {
        if (label == null) {
            label = "";
        }
        if (!label.equals(this.label)) {
            this.label = label;
            layout();
            repaint();
        }
    }

    void handleJavaMouseEvent(MouseEvent e) {
        super.handleJavaMouseEvent(e);
        int i = e.getID();
        switch (i) {
          case MouseEvent.MOUSE_PRESSED:
              mousePressed(e);
              break;
          case MouseEvent.MOUSE_RELEASED:
              mouseReleased(e);
              break;
          case MouseEvent.MOUSE_ENTERED:
              mouseEntered(e);
              break;
          case MouseEvent.MOUSE_EXITED:
              mouseExited(e);
              break;
          case MouseEvent.MOUSE_CLICKED:
              mouseClicked(e);
              break;
        }
    }

    public void mousePressed(MouseEvent e) {
        if (XToolkit.isLeftMouseButton(e)) {
            Checkbox cb = (Checkbox) e.getSource();

            if (cb.contains(e.getX(), e.getY())) {
                if (log.isLoggable(PlatformLogger.Level.FINER)) {
                    log.finer("mousePressed() on " + target.getName() + " : armed = " + armed + ", pressed = " + pressed
                              + ", selected = " + selected + ", enabled = " + isEnabled());
                }
                if (!isEnabled()) {
                    // Disabled buttons ignore all input...
                    return;
                }
                if (!armed) {
                    armed = true;
                }
                pressed = true;
                repaint();
            }
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (log.isLoggable(PlatformLogger.Level.FINER)) {
            log.finer("mouseReleased() on " + target.getName() + ": armed = " + armed + ", pressed = " + pressed
                      + ", selected = " + selected + ", enabled = " + isEnabled());
        }
        boolean sendEvent = false;
        if (XToolkit.isLeftMouseButton(e)) {
            // TODO: Multiclick Threshold? - see BasicButtonListener.java
            if (armed) {
                //selected = !selected;
                // send action event
                //action(e.getWhen(),e.getModifiers());
                sendEvent = true;
            }
            pressed = false;
            armed = false;
            if (sendEvent) {
                action(!selected);  // Also gets repaint in action()
            }
            else {
                repaint();
            }
        }
    }

    public void mouseEntered(MouseEvent e) {
        if (log.isLoggable(PlatformLogger.Level.FINER)) {
            log.finer("mouseEntered() on " + target.getName() + ": armed = " + armed + ", pressed = " + pressed
                      + ", selected = " + selected + ", enabled = " + isEnabled());
        }
        if (pressed) {
            armed = true;
            repaint();
        }
    }

    public void mouseExited(MouseEvent e) {
        if (log.isLoggable(PlatformLogger.Level.FINER)) {
            log.finer("mouseExited() on " + target.getName() + ": armed = " + armed + ", pressed = " + pressed
                      + ", selected = " + selected + ", enabled = " + isEnabled());
        }
        if (armed) {
            armed = false;
            repaint();
        }
    }

    public void mouseClicked(MouseEvent e) {}

    public Dimension getMinimumSize() {
        /*
         * Spacing (number of pixels between check mark and label text) is
         * currently set to 0, but in case it ever changes we have to add
         * it. 8 is a heuristic number. Indicator size depends on font
         * height, so we don't need to include it in checkbox's height
         * calculation.
         */
        FontMetrics fm = getFontMetrics(getPeerFont());

        int wdth = fm.stringWidth(label) + getCheckboxSize(fm) + (2 * checkBoxInsetFromText) + 8;
        int hght = Math.max(fm.getHeight() + 8, 15);

        return new Dimension(wdth, hght);
    }

    private int getCheckboxSize(FontMetrics fm) {
        // the motif way of sizing is a bit inscutible, but this
        // is a fair approximation
        return (fm.getHeight() * 76 / 100) - 1;
    }

    public void setBackground(Color c) {
        updateMotifColors(c);
        super.setBackground(c);
    }

    /*
     * Layout the checkbox/radio button and text label
     */
    public void layout() {
        Dimension size = getPeerSize();
        Font f = getPeerFont();
        FontMetrics fm = getFontMetrics(f);
        String text = label;

        checkBoxSize = getCheckboxSize(fm);

        // Note - Motif appears to use an left inset that is slightly
        // scaled to the checkbox/font size.
        cbX = borderInsets.left + checkBoxInsetFromText;
        cbY = size.height / 2 - checkBoxSize / 2;
        int minTextX = borderInsets.left + 2 * checkBoxInsetFromText + checkBoxSize;
        // FIXME: will need to account for alignment?
        // FIXME: call layout() on alignment changes
        //textRect.width = fm.stringWidth(text);
        textRect.width = fm.stringWidth(text == null ? "" : text);
        textRect.height = fm.getHeight();

        textRect.x = Math.max(minTextX, size.width / 2 - textRect.width / 2);
        textRect.y = (size.height - textRect.height) / 2;

        focusRect.x = focusInsets.left;
        focusRect.y = focusInsets.top;
        focusRect.width = size.width-(focusInsets.left+focusInsets.right)-1;
        focusRect.height = size.height-(focusInsets.top+focusInsets.bottom)-1;

        double fsize = (double) checkBoxSize;
        myCheckMark = AffineTransform.getScaleInstance(fsize / MASTER_SIZE, fsize / MASTER_SIZE).createTransformedShape(MASTER_CHECKMARK);
    }
    @Override
    void paintPeer(final Graphics g) {
        //layout();
        Dimension size = getPeerSize();
        Font f = getPeerFont();
        flush();
        g.setColor(getPeerBackground());   // erase the existing button
        g.fillRect(0,0, size.width, size.height);
        if (label != null) {
            g.setFont(f);
            paintText(g, textRect, label);
        }

        if (hasFocus()) {
            paintFocus(g,
                       focusRect.x,
                       focusRect.y,
                       focusRect.width,
                       focusRect.height);
        }
        // Paint the checkbox or radio button
        if (checkBoxGroup == null) {
            paintCheckbox(g, cbX, cbY, checkBoxSize, checkBoxSize);
        }
        else {
            paintRadioButton(g, cbX, cbY, checkBoxSize, checkBoxSize);
        }
        flush();
    }

    // You'll note this looks suspiciously like paintBorder
    public void paintCheckbox(Graphics g,
                              int x, int y, int w, int h) {
        boolean useBufferedImage = false;
        BufferedImage buffer = null;
        Graphics2D g2 = null;
        int rx = x;
        int ry = y;
        if (!(g instanceof Graphics2D)) {
            // Fix for 5045936. While printing, g is an instance of
            //   sun.print.ProxyPrintGraphics which extends Graphics. So
            //   we use a separate buffered image and its graphics is
            //   always Graphics2D instance
            buffer = graphicsConfig.createCompatibleImage(w, h);
            g2 = buffer.createGraphics();
            useBufferedImage = true;
            rx = 0;
            ry = 0;
        }
        else {
            g2 = (Graphics2D)g;
        }
        try {
            drawMotif3DRect(g2, rx, ry, w-1, h-1, armed | selected);

            // then paint the check
            g2.setColor((armed | selected) ? selectColor : getPeerBackground());
            g2.fillRect(rx+1, ry+1, w-2, h-2);

            if (armed | selected) {
                //Paint the check

                // FIXME: is this the right color?
                g2.setColor(getPeerForeground());

                AffineTransform af = g2.getTransform();
                g2.setTransform(AffineTransform.getTranslateInstance(rx,ry));
                g2.fill(myCheckMark);
                g2.setTransform(af);
            }
        } finally {
            if (useBufferedImage) {
                g2.dispose();
            }
        }
        if (useBufferedImage) {
            g.drawImage(buffer, x, y, null);
        }
    }

    public void paintRadioButton(Graphics g, int x, int y, int w, int h) {

        g.setColor((armed | selected) ? darkShadow : lightShadow);
        g.drawArc(x-1, y-1, w+2, h+2, 45, 180);

        g.setColor((armed | selected) ? lightShadow : darkShadow);
        g.drawArc(x-1, y-1, w+2, h+2, 45, -180);

        if (armed | selected) {
            g.setColor(selectColor);
            g.fillArc(x+1, y+1, w-1, h-1, 0, 360);
        }
    }

    protected void paintText(Graphics g, Rectangle textRect, String text) {
        FontMetrics fm = g.getFontMetrics();

        int mnemonicIndex = -1;

        if(isEnabled()) {
            /*** paint the text normally */
            g.setColor(getPeerForeground());
            BasicGraphicsUtils.drawStringUnderlineCharAt(g,text,mnemonicIndex , textRect.x , textRect.y + fm.getAscent() );
        }
        else {
            /*** paint the text disabled ***/
            g.setColor(getPeerBackground().brighter());

            BasicGraphicsUtils.drawStringUnderlineCharAt(g,text, mnemonicIndex,
                                                         textRect.x, textRect.y + fm.getAscent());
            g.setColor(getPeerBackground().darker());
            BasicGraphicsUtils.drawStringUnderlineCharAt(g,text, mnemonicIndex,
                                                         textRect.x - 1, textRect.y + fm.getAscent() - 1);
        }
    }

    // TODO: copied directly from XButtonPeer.  Should probabaly be shared
    protected void paintFocus(Graphics g, int x, int y, int w, int h) {
        g.setColor(focusColor);
        g.drawRect(x,y,w,h);
    }

    @Override
    public void setState(boolean state) {
        if (selected != state) {
            selected = state;
            repaint();
        }
    }

    @Override
    public void setCheckboxGroup(final CheckboxGroup g) {
        if (!Objects.equals(g, checkBoxGroup)) {
            // If changed from grouped/ungrouped, need to repaint()
            checkBoxGroup = g;
            repaint();
        }
    }

    // NOTE: This method is called by privileged threads.
    //       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
    // From MCheckboxPeer
    void action(boolean state) {
        final Checkbox cb = (Checkbox)target;
        final boolean newState = state;
        XToolkit.executeOnEventHandlerThread(cb, new Runnable() {
                public void run() {
                    CheckboxGroup cbg = checkBoxGroup;
                    // Bugid 4039594. If this is the current Checkbox in
                    // a CheckboxGroup, then return to prevent deselection.
                    // Otherwise, it's logical state will be turned off,
                    // but it will appear on.
                    if ((cbg != null) && (cbg.getSelectedCheckbox() == cb) &&
                        cb.getState()) {
                        //inUpCall = false;
                        cb.setState(true);
                        return;
                    }
                    // All clear - set the new state
                    cb.setState(newState);
                    notifyStateChanged(newState);
                }
            });
    }

    void notifyStateChanged(boolean state) {
        Checkbox cb = (Checkbox) target;
        ItemEvent e = new ItemEvent(cb,
                                    ItemEvent.ITEM_STATE_CHANGED,
                                    cb.getLabel(),
                                    state ? ItemEvent.SELECTED : ItemEvent.DESELECTED);
        postEvent(e);
    }
}
