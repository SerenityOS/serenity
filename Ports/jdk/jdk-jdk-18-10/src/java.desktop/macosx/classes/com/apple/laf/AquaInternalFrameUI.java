/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.apple.laf;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicInternalFrameUI;

import apple.laf.*;
import apple.laf.JRSUIConstants.*;

import com.apple.laf.AquaUtils.*;
import com.apple.laf.AquaUtils.Painter;

import sun.lwawt.macosx.CPlatformWindow;

/**
 * From AquaInternalFrameUI
 *
 * InternalFrame implementation for Aqua LAF
 *
 * We want to inherit most of the inner classes, but not the base class,
 * so be very careful about subclassing so you know you get what you want
 *
 */
public class AquaInternalFrameUI extends BasicInternalFrameUI implements SwingConstants {
    protected static final String IS_PALETTE_PROPERTY = "JInternalFrame.isPalette";
    private static final String FRAME_TYPE = "JInternalFrame.frameType";
    private static final String NORMAL_FRAME = "normal";
    private static final String PALETTE_FRAME = "palette";
    private static final String OPTION_DIALOG = "optionDialog";

    // Instance variables
    PropertyChangeListener fPropertyListener;

    protected Color fSelectedTextColor;
    protected Color fNotSelectedTextColor;

    AquaInternalFrameBorder fAquaBorder;
    private ResizeBox resizeBox;

    // for button tracking
    boolean fMouseOverPressedButton;
    int fWhichButtonPressed = -1;
    boolean fRollover = false;
    boolean fDocumentEdited = false; // to indicate whether we should use the dirty document red dot.
    boolean fIsPallet;

    public int getWhichButtonPressed() {
        return fWhichButtonPressed;
    }

    public boolean getMouseOverPressedButton() {
        return fMouseOverPressedButton;
    }

    public boolean getRollover() {
        return fRollover;
    }

    // ComponentUI Interface Implementation methods
    public static ComponentUI createUI(final JComponent b) {
        return new AquaInternalFrameUI((JInternalFrame)b);
    }

    public AquaInternalFrameUI(final JInternalFrame b) {
        super(b);
    }

    /// Inherit  (but be careful to check everything they call):
    @Override
    public void installUI(final JComponent c) {
//        super.installUI(c);  // Swing 1.1.1 has a bug in installUI - it doesn't check for null northPane
        frame = (JInternalFrame)c;
        frame.add(frame.getRootPane(), "Center");

        installDefaults();
        installListeners();
        installComponents();
        installKeyboardActions();

        Object paletteProp = c.getClientProperty(IS_PALETTE_PROPERTY);
        if (paletteProp != null) {
            setPalette(((Boolean)paletteProp).booleanValue());
        } else {
            paletteProp = c.getClientProperty(FRAME_TYPE);
            if (paletteProp != null) {
                setFrameType((String)paletteProp);
            } else {
                setFrameType(NORMAL_FRAME);
            }
        }

        // We only have a southPane, for grow box room, created in setFrameType
        frame.setMinimumSize(new Dimension(fIsPallet ? 120 : 150, fIsPallet ? 39 : 65));
        frame.setOpaque(false);

        c.setBorder(new CompoundUIBorder(fIsPallet ? paletteWindowShadow.get() : documentWindowShadow.get(), c.getBorder()));
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        fSelectedTextColor = UIManager.getColor("InternalFrame.activeTitleForeground");
        fNotSelectedTextColor = UIManager.getColor("InternalFrame.inactiveTitleForeground");
    }

    @Override
    public void setSouthPane(final JComponent c) {
        if (southPane != null) {
            frame.remove(southPane);
            deinstallMouseHandlers(southPane);
        }
        if (c != null) {
            frame.add(c);
            installMouseHandlers(c);
        }
        southPane = c;
    }

    private static final RecyclableSingleton<Icon> closeIcon = new RecyclableSingleton<Icon>() {
        @Override
        protected Icon getInstance() {
            return new AquaInternalFrameButtonIcon(Widget.TITLE_BAR_CLOSE_BOX);
        }
    };
    public static Icon exportCloseIcon() {
        return closeIcon.get();
    }

    private static final RecyclableSingleton<Icon> minimizeIcon = new RecyclableSingleton<Icon>() {
        @Override
        protected Icon getInstance() {
            return new AquaInternalFrameButtonIcon(Widget.TITLE_BAR_COLLAPSE_BOX);
        }
    };
    public static Icon exportMinimizeIcon() {
        return minimizeIcon.get();
    }

    private static final RecyclableSingleton<Icon> zoomIcon = new RecyclableSingleton<Icon>() {
        @Override
        protected Icon getInstance() {
            return new AquaInternalFrameButtonIcon(Widget.TITLE_BAR_ZOOM_BOX);
        }
    };
    public static Icon exportZoomIcon() {
        return zoomIcon.get();
    }

    static class AquaInternalFrameButtonIcon extends AquaIcon.JRSUIIcon {
        public AquaInternalFrameButtonIcon(final Widget widget) {
            painter.state.set(widget);
        }

        @Override
        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            painter.state.set(getStateFor(c));
            super.paintIcon(c, g, x, y);
        }

        State getStateFor(final Component c) {
            return State.ROLLOVER;
        }

        @Override
        public int getIconWidth() {
            return 19;
        }

        @Override
        public int getIconHeight() {
            return 19;
        }
    }

    @Override
    protected void installKeyboardActions() {
    } //$ Not Mac-ish - should we support?

    @Override
    protected void installComponents() {
        final JLayeredPane layeredPane = frame.getLayeredPane();
        resizeBox = new ResizeBox(layeredPane);
        resizeBox.repositionResizeBox();

        layeredPane.add(resizeBox);
        layeredPane.setLayer(resizeBox, JLayeredPane.DRAG_LAYER);
        layeredPane.addComponentListener(resizeBox);

        resizeBox.addListeners();
        resizeBox.setVisible(frame.isResizable());
    }

    /// Inherit all the listeners - that's the main reason we subclass Basic!
    @Override
    protected void installListeners() {
        fPropertyListener = new PropertyListener();
        frame.addPropertyChangeListener(fPropertyListener);
        super.installListeners();
    }

    // uninstallDefaults

    @Override
    protected void uninstallComponents() {
        super.uninstallComponents();
        final JLayeredPane layeredPane = frame.getLayeredPane();
        resizeBox.removeListeners();
        layeredPane.removeComponentListener(resizeBox);
        layeredPane.remove(resizeBox);
        resizeBox = null;
    }

    @Override
    protected void uninstallListeners() {
        super.uninstallListeners();
        frame.removePropertyChangeListener(fPropertyListener);
    }

    @Override
    protected void uninstallKeyboardActions() {
    }

    // Called when a DesktopIcon replaces an InternalFrame & vice versa
    //protected void replacePane(JComponent currentPane, JComponent newPane) {}
    @Override
    protected void installMouseHandlers(final JComponent c) {
        c.addMouseListener(borderListener);
        c.addMouseMotionListener(borderListener);
    }

    @Override
    protected void deinstallMouseHandlers(final JComponent c) {
        c.removeMouseListener(borderListener);
        c.removeMouseMotionListener(borderListener);
    }

    ActionMap createActionMap() {
        final ActionMap map = new ActionMapUIResource();
        // add action for the system menu
        // Set the ActionMap's parent to the Auditory Feedback Action Map
        final AquaLookAndFeel lf = (AquaLookAndFeel)UIManager.getLookAndFeel();
        final ActionMap audioMap = lf.getAudioActionMap();
        map.setParent(audioMap);
        return map;
    }

    @Override
    public Dimension getPreferredSize(JComponent x) {
        Dimension preferredSize = super.getPreferredSize(x);
        Dimension minimumSize = frame.getMinimumSize();
        if (preferredSize.width < minimumSize.width) {
            preferredSize.width = minimumSize.width;
        }
        if (preferredSize.height < minimumSize.height) {
            preferredSize.height = minimumSize.height;
        }
        return preferredSize;
    }

    @Override
    public void setNorthPane(final JComponent c) {
        replacePane(northPane, c);
        northPane = c;
    }

    /**
     * Installs necessary mouse handlers on {@code newPane}
     * and adds it to the frame.
     * Reverse process for the {@code currentPane}.
     */
    @Override
    protected void replacePane(final JComponent currentPane, final JComponent newPane) {
        if (currentPane != null) {
            deinstallMouseHandlers(currentPane);
            frame.remove(currentPane);
        }
        if (newPane != null) {
            frame.add(newPane);
            installMouseHandlers(newPane);
        }
    }

    // Our "Border" listener is shared by the AquaDesktopIcon
    @Override
    protected MouseInputAdapter createBorderListener(final JInternalFrame w) {
        return new AquaBorderListener();
    }

    /**
     * Mac-specific stuff begins here
     */
    void setFrameType(final String frameType) {
        // Basic sets the background of the contentPane to null so it can inherit JInternalFrame.setBackground
        // but if *that's* null, we get the JDesktop, which makes ours look invisible!
        // So JInternalFrame has to have a background color
        // See Sun bugs 4268949 & 4320889
        final Color bg = frame.getBackground();
        final boolean replaceColor = (bg == null || bg instanceof UIResource);

        final Font font = frame.getFont();
        final boolean replaceFont = (font == null || font instanceof UIResource);

        boolean isPalette = false;
        if (frameType.equals(OPTION_DIALOG)) {
            fAquaBorder = AquaInternalFrameBorder.dialog();
            if (replaceColor) frame.setBackground(UIManager.getColor("InternalFrame.optionDialogBackground"));
            if (replaceFont) frame.setFont(UIManager.getFont("InternalFrame.optionDialogTitleFont"));
        } else if (frameType.equals(PALETTE_FRAME)) {
            fAquaBorder = AquaInternalFrameBorder.utility();
            if (replaceColor) frame.setBackground(UIManager.getColor("InternalFrame.paletteBackground"));
            if (replaceFont) frame.setFont(UIManager.getFont("InternalFrame.paletteTitleFont"));
            isPalette = true;
        } else {
            fAquaBorder = AquaInternalFrameBorder.window();
            if (replaceColor) frame.setBackground(UIManager.getColor("InternalFrame.background"));
            if (replaceFont) frame.setFont(UIManager.getFont("InternalFrame.titleFont"));
        }
        // We don't get the borders from UIManager, in case someone changes them - this class will not work with
        // different borders.  If they want different ones, they have to make their own InternalFrameUI class

        fAquaBorder.setColors(fSelectedTextColor, fNotSelectedTextColor);
        frame.setBorder(fAquaBorder);

        fIsPallet = isPalette;
    }

    public void setPalette(final boolean isPalette) {
        setFrameType(isPalette ? PALETTE_FRAME : NORMAL_FRAME);
    }

    public boolean isDocumentEdited() {
        return fDocumentEdited;
    }

    public void setDocumentEdited(final boolean flag) {
        fDocumentEdited = flag;
    }

/*
    // helpful debug drawing, shows component and content bounds
    public void paint(final Graphics g, final JComponent c) {
        super.paint(g, c);

        g.setColor(Color.green);
        g.drawRect(0, 0, frame.getWidth() - 1, frame.getHeight() - 1);

        final Insets insets = frame.getInsets();
        g.setColor(Color.orange);
        g.drawRect(insets.left - 2, insets.top - 2, frame.getWidth() - insets.left - insets.right + 4, frame.getHeight() - insets.top - insets.bottom + 4);
    }
*/

    // Border Listener Class
    /**
     * Listens for border adjustments.
     */
    protected class AquaBorderListener extends MouseInputAdapter {
        // _x & _y are the mousePressed location in absolute coordinate system
        int _x, _y;
        // __x & __y are the mousePressed location in source view's coordinate system
        int __x, __y;
        Rectangle startingBounds;
        boolean fDraggingFrame;
        int resizeDir;

        protected final int RESIZE_NONE = 0;
        private boolean discardRelease = false;

        @Override
        public void mouseClicked(final MouseEvent e) {
            if (didForwardEvent(e)) return;

            if (e.getClickCount() <= 1 || e.getSource() != getNorthPane()) return;

            if (frame.isIconifiable() && frame.isIcon()) {
                try {
                    frame.setIcon(false);
                } catch(final PropertyVetoException e2) {}
            } else if (frame.isMaximizable()) {
                if (!frame.isMaximum()) try {
                    frame.setMaximum(true);
                } catch(final PropertyVetoException e2) {}
                else try {
                    frame.setMaximum(false);
                } catch(final PropertyVetoException e3) {}
            }
        }

        public void updateRollover(final MouseEvent e) {
            final boolean oldRollover = fRollover;
            final Insets i = frame.getInsets();
            fRollover = (isTitleBarDraggableArea(e) && fAquaBorder.getWithinRolloverArea(i, e.getX(), e.getY()));
            if (fRollover != oldRollover) {
                repaintButtons();
            }
        }

        public void repaintButtons() {
            fAquaBorder.repaintButtonArea(frame);
        }

        @Override
        @SuppressWarnings("removal")
        public void mouseReleased(final MouseEvent e) {
            if (didForwardEvent(e)) return;

            fDraggingFrame = false;

            if (fWhichButtonPressed != -1) {
                final int newButton = fAquaBorder.getWhichButtonHit(frame, e.getX(), e.getY());

                final int buttonPresed = fWhichButtonPressed;
                fWhichButtonPressed = -1;
                fMouseOverPressedButton = false;

                if (buttonPresed == newButton) {
                    fMouseOverPressedButton = false;
                    fRollover = false; // not sure if this is needed?

                    fAquaBorder.doButtonAction(frame, buttonPresed);
                }

                updateRollover(e);
                repaintButtons();
                return;
            }

            if (discardRelease) {
                discardRelease = false;
                return;
            }
            if (resizeDir == RESIZE_NONE) getDesktopManager().endDraggingFrame(frame);
            else {
                final Container c = frame.getTopLevelAncestor();
                if (c instanceof JFrame) {
                    ((JFrame)frame.getTopLevelAncestor()).getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

                    ((JFrame)frame.getTopLevelAncestor()).getGlassPane().setVisible(false);
                } else if (c instanceof JApplet) {
                    ((JApplet)c).getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    ((JApplet)c).getGlassPane().setVisible(false);
                } else if (c instanceof JWindow) {
                    ((JWindow)c).getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    ((JWindow)c).getGlassPane().setVisible(false);
                } else if (c instanceof JDialog) {
                    ((JDialog)c).getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    ((JDialog)c).getGlassPane().setVisible(false);
                }
                getDesktopManager().endResizingFrame(frame);
            }
            _x = 0;
            _y = 0;
            __x = 0;
            __y = 0;
            startingBounds = null;
            resizeDir = RESIZE_NONE;
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            if (didForwardEvent(e)) return;

            final Point p = SwingUtilities.convertPoint((Component)e.getSource(), e.getX(), e.getY(), null);
            __x = e.getX();
            __y = e.getY();
            _x = p.x;
            _y = p.y;
            startingBounds = frame.getBounds();
            resizeDir = RESIZE_NONE;

            if (updatePressed(e)) { return; }

            if (!frame.isSelected()) {
                try {
                    frame.setSelected(true);
                } catch(final PropertyVetoException e1) {}
            }

            if (isTitleBarDraggableArea(e)) {
                getDesktopManager().beginDraggingFrame(frame);
                fDraggingFrame = true;
                return;
            }

            if (e.getSource() == getNorthPane()) {
                getDesktopManager().beginDraggingFrame(frame);
                return;
            }

            if (!frame.isResizable()) { return; }

            if (e.getSource() == frame) {
                discardRelease = true;
                return;
            }
        }

        // returns true if we have handled the pressed
        public boolean updatePressed(final MouseEvent e) {
            // get the component.
            fWhichButtonPressed = getButtonHit(e);
            fMouseOverPressedButton = true;
            repaintButtons();
            return (fWhichButtonPressed >= 0);
            // e.getX(), e.getY()
        }

        public int getButtonHit(final MouseEvent e) {
            return fAquaBorder.getWhichButtonHit(frame, e.getX(), e.getY());
        }

        public boolean isTitleBarDraggableArea(final MouseEvent e) {
            if (e.getSource() != frame) return false;

            final Point point = e.getPoint();
            final Insets insets = frame.getInsets();

            if (point.y < insets.top - fAquaBorder.getTitleHeight()) return false;
            if (point.y > insets.top) return false;
            if (point.x < insets.left) return false;
            if (point.x > frame.getWidth() - insets.left - insets.right) return false;

            return true;
        }

        @Override
        @SuppressWarnings("deprecation")
        public void mouseDragged(final MouseEvent e) {
// do not forward drags
//            if (didForwardEvent(e)) return;

            if (startingBounds == null) {
                // (STEVE) Yucky work around for bug ID 4106552
                return;
            }

            if (fWhichButtonPressed != -1) {
                // track the button we started on.
                final int newButton = getButtonHit(e);
                fMouseOverPressedButton = (fWhichButtonPressed == newButton);
                repaintButtons();
                return;
            }

            final Point p = SwingUtilities.convertPoint((Component)e.getSource(), e.getX(), e.getY(), null);
            final int deltaX = _x - p.x;
            final int deltaY = _y - p.y;
            int newX, newY;

            // Handle a MOVE
            if (!fDraggingFrame && e.getSource() != getNorthPane()) return;

            if (frame.isMaximum() || ((e.getModifiers() & InputEvent.BUTTON1_MASK) != InputEvent.BUTTON1_MASK)) {
                // don't allow moving of frames if maximixed or left mouse
                // button was not used.
                return;
            }

            final Dimension s = frame.getParent().getSize();
            final int pWidth = s.width;
            final int pHeight = s.height;

            final Insets i = frame.getInsets();
            newX = startingBounds.x - deltaX;
            newY = startingBounds.y - deltaY;

            // Make sure we stay in-bounds
            if (newX + i.left <= -__x) newX = -__x - i.left;
            if (newY + i.top <= -__y) newY = -__y - i.top;
            if (newX + __x + i.right > pWidth) newX = pWidth - __x - i.right;
            if (newY + __y + i.bottom > pHeight) newY = pHeight - __y - i.bottom;

            getDesktopManager().dragFrame(frame, newX, newY);
            return;
        }

        @Override
        public void mouseMoved(final MouseEvent e) {
            if (didForwardEvent(e)) return;
            updateRollover(e);
        }

        // guards against accidental infinite recursion
        boolean isTryingToForwardEvent = false;
        boolean didForwardEvent(final MouseEvent e) {
            if (isTryingToForwardEvent) return true; // we didn't actually...but we wound up back where we started.

            isTryingToForwardEvent = true;
            final boolean didForwardEvent = didForwardEventInternal(e);
            isTryingToForwardEvent = false;

            return didForwardEvent;
        }
        @SuppressWarnings("deprecation")
        boolean didForwardEventInternal(final MouseEvent e) {
            if (fDraggingFrame) return false;

            final Point originalPoint = e.getPoint();
            if (!isEventInWindowShadow(originalPoint)) return false;

            final Container parent = frame.getParent();
            if (!(parent instanceof JDesktopPane)) return false;
            final JDesktopPane pane = (JDesktopPane)parent;
            final Point parentPoint = SwingUtilities.convertPoint(frame, originalPoint, parent);

        /*     // debug drawing
            Graphics g = parent.getGraphics();
            g.setColor(Color.red);
            g.drawLine(parentPoint.x, parentPoint.y, parentPoint.x, parentPoint.y);
        */

            final Component hitComponent = findComponentToHitBehindMe(pane, parentPoint);
            if (hitComponent == null || hitComponent == frame) return false;

            final Point hitComponentPoint = SwingUtilities.convertPoint(pane, parentPoint, hitComponent);
            hitComponent.dispatchEvent(
                    new MouseEvent(hitComponent, e.getID(), e.getWhen(),
                                   e.getModifiers(), hitComponentPoint.x,
                                   hitComponentPoint.y, e.getClickCount(),
                                   e.isPopupTrigger(), e.getButton()));
            return true;
        }

        Component findComponentToHitBehindMe(final JDesktopPane pane, final Point parentPoint) {
            final JInternalFrame[] allFrames = pane.getAllFrames();

            boolean foundSelf = false;
            for (final JInternalFrame f : allFrames) {
                if (f == frame) { foundSelf = true; continue; }
                if (!foundSelf) continue;

                final Rectangle bounds = f.getBounds();
                if (bounds.contains(parentPoint)) return f;
            }

            return pane;
        }

        boolean isEventInWindowShadow(final Point point) {
            final Rectangle bounds = frame.getBounds();
            final Insets insets = frame.getInsets();
            insets.top -= fAquaBorder.getTitleHeight();

            if (point.x < insets.left) return true;
            if (point.x > bounds.width - insets.right) return true;
            if (point.y < insets.top) return true;
            if (point.y > bounds.height - insets.bottom) return true;

            return false;
        }
    }

    static void updateComponentTreeUIActivation(final Component c, final Object active) {
        if (c instanceof javax.swing.JComponent) {
            ((javax.swing.JComponent)c).putClientProperty(AquaFocusHandler.FRAME_ACTIVE_PROPERTY, active);
        }

        Component[] children = null;

        if (c instanceof javax.swing.JMenu) {
            children = ((javax.swing.JMenu)c).getMenuComponents();
        } else if (c instanceof Container) {
            children = ((Container)c).getComponents();
        }

        if (children != null) {
            for (final Component element : children) {
                updateComponentTreeUIActivation(element, active);
            }
        }
    }

    class PropertyListener implements PropertyChangeListener {
        @Override
        public void propertyChange(final PropertyChangeEvent e) {
            final String name = e.getPropertyName();
            if (FRAME_TYPE.equals(name)) {
                if (e.getNewValue() instanceof String) {
                    setFrameType((String)e.getNewValue());
                }
            } else if (IS_PALETTE_PROPERTY.equals(name)) {
                if (e.getNewValue() != null) {
                    setPalette(((Boolean)e.getNewValue()).booleanValue());
                } else {
                    setPalette(false);
                }
                // TODO: CPlatformWindow?
            } else if ("windowModified".equals(name) || CPlatformWindow.WINDOW_DOCUMENT_MODIFIED.equals(name)) {
                // repaint title bar
                setDocumentEdited(((Boolean)e.getNewValue()).booleanValue());
                frame.repaint(0, 0, frame.getWidth(), frame.getBorder().getBorderInsets(frame).top);
            } else if ("resizable".equals(name) || "state".equals(name) || "iconable".equals(name) || "maximizable".equals(name) || "closable".equals(name)) {
                if ("resizable".equals(name)) {
                    frame.revalidate();
                }
                frame.repaint();
            } else if ("title".equals(name)) {
                frame.repaint();
            } else if ("componentOrientation".equals(name)) {
                frame.revalidate();
                frame.repaint();
            } else if (JInternalFrame.IS_SELECTED_PROPERTY.equals(name)) {
                final Component source = (Component)(e.getSource());
                updateComponentTreeUIActivation(source, frame.isSelected() ? Boolean.TRUE : Boolean.FALSE);
            }

        }
    } // end class PaletteListener

    private static final InternalFrameShadow documentWindowShadow = new InternalFrameShadow() {
        @Override
        Border getForegroundShadowBorder() {
            return new AquaUtils.SlicedShadowBorder(new Painter() {
                @Override
                public void paint(final Graphics g, final int x, final int y, final int w, final int h) {
                    g.setColor(new Color(0, 0, 0, 196));
                    g.fillRoundRect(x, y, w, h, 16, 16);
                    g.fillRect(x, y + h - 16, w, 16);
                }
            }, new Painter() {
                @Override
                public void paint(final Graphics g, int x, int y, int w, int h) {
                    g.setColor(new Color(0, 0, 0, 64));
                    g.drawLine(x + 2, y - 8, x + w - 2, y - 8);
                }
            },
            0, 7, 1.1f, 1.0f, 24, 51, 51, 25, 25, 25, 25);
        }

        @Override
        Border getBackgroundShadowBorder() {
            return new AquaUtils.SlicedShadowBorder(new Painter() {
                @Override
                public void paint(final Graphics g, final int x, final int y, final int w, final int h) {
                    g.setColor(new Color(0, 0, 0, 128));
                    g.fillRoundRect(x - 3, y - 8, w + 6, h, 16, 16);
                    g.fillRect(x - 3, y + h - 20, w + 6, 19);
                }
            }, new Painter() {
                @Override
                public void paint(final Graphics g, int x, int y, int w, int h) {
                    g.setColor(new Color(0, 0, 0, 32));
                    g.drawLine(x, y - 11, x + w - 1, y - 11);
                }
            },
            0, 0, 3.0f, 1.0f, 10, 51, 51, 25, 25, 25, 25);
        }
    };

    private static final InternalFrameShadow paletteWindowShadow = new InternalFrameShadow() {
        @Override
        Border getForegroundShadowBorder() {
            return new AquaUtils.SlicedShadowBorder(new Painter() {
                @Override
                public void paint(final Graphics g, final int x, final int y, final int w, final int h) {
                    g.setColor(new Color(0, 0, 0, 128));
                    g.fillRect(x, y + 3, w, h - 3);
                }
            }, null,
            0, 3, 1.0f, 1.0f, 10, 25, 25, 12, 12, 12, 12);
        }

        @Override
        Border getBackgroundShadowBorder() {
            return getForegroundShadowBorder();
        }
    };

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    static class CompoundUIBorder extends CompoundBorder implements UIResource {
        public CompoundUIBorder(final Border inside, final Border outside) { super(inside, outside); }
    }

    abstract static class InternalFrameShadow extends RecyclableSingleton<Border> {
        abstract Border getForegroundShadowBorder();
        abstract Border getBackgroundShadowBorder();

        @Override
        protected Border getInstance() {
            final Border fgShadow = getForegroundShadowBorder();
            final Border bgShadow = getBackgroundShadowBorder();

            return new Border() {
                @Override
                public Insets getBorderInsets(final Component c) {
                    return fgShadow.getBorderInsets(c);
                }

                @Override
                public boolean isBorderOpaque() {
                    return false;
                }

                @Override
                public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int w, final int h) {
                    if (((JInternalFrame)c).isSelected()) {
                        fgShadow.paintBorder(c, g, x, y, w, h);
                    } else {
                        bgShadow.paintBorder(c, g, x, y, w, h);
                    }
                }
            };
        }
    }

    private static final RecyclableSingleton<Icon> RESIZE_ICON = new RecyclableSingleton<Icon>() {
        @Override
        protected Icon getInstance() {
            return new AquaIcon.ScalingJRSUIIcon(11, 11) {
                @Override
                public void initIconPainter(final AquaPainter<JRSUIState> iconState) {
                    iconState.state.set(Widget.GROW_BOX_TEXTURED);
                    iconState.state.set(WindowType.UTILITY);
                }
            };
        }
    };

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private final class ResizeBox extends JLabel
            implements MouseListener, MouseMotionListener, MouseWheelListener,
            ComponentListener, PropertyChangeListener, UIResource {

        private final JLayeredPane layeredPane;
        private Dimension originalSize;
        private Point originalLocation;

        ResizeBox(final JLayeredPane layeredPane) {
            super(RESIZE_ICON.get());
            setSize(11, 11);
            this.layeredPane = layeredPane;

            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
        }

        void addListeners() {
            frame.addPropertyChangeListener("resizable", this);
        }

        void removeListeners() {
            frame.removePropertyChangeListener("resizable", this);
        }

        void repositionResizeBox() {
            if (frame == null) { setSize(0, 0); } else { setSize(11, 11); }
            setLocation(layeredPane.getWidth() - 12, layeredPane.getHeight() - 12);
        }

        void resizeInternalFrame(final Point pt) {
            if (originalLocation == null || frame == null) return;

            final Container parent = frame.getParent();
            if (!(parent instanceof JDesktopPane)) return;

            final Point newPoint = SwingUtilities.convertPoint(this, pt, frame);
            int deltaX = originalLocation.x - newPoint.x;
            int deltaY = originalLocation.y - newPoint.y;
            final Dimension min = frame.getMinimumSize();
            final Dimension max = frame.getMaximumSize();

            final int newX = frame.getX();
            final int newY = frame.getY();
            int newW = frame.getWidth();
            int newH = frame.getHeight();

            final Rectangle parentBounds = parent.getBounds();

            if (originalSize.width - deltaX < min.width) {
                deltaX = originalSize.width - min.width;
            }  else if (originalSize.width - deltaX > max.width) {
                deltaX = -(max.width - originalSize.width);
            }

            if (newX + originalSize.width - deltaX > parentBounds.width) {
                deltaX = newX + originalSize.width - parentBounds.width;
            }

            if (originalSize.height - deltaY < min.height) {
                deltaY = originalSize.height - min.height;
            }  else if (originalSize.height - deltaY > max.height) {
                deltaY = -(max.height - originalSize.height);
            }

            if (newY + originalSize.height - deltaY > parentBounds.height) {
                deltaY = newY + originalSize.height - parentBounds.height;
            }

            newW = originalSize.width - deltaX;
            newH = originalSize.height - deltaY;

            getDesktopManager().resizeFrame(frame, newX, newY, newW, newH);
        }

        boolean testGrowboxPoint(final int x, final int y, final int w, final int h) {
            return (w - x) + (h - y) < 12;
        }

        @SuppressWarnings("deprecation")
        void forwardEventToFrame(final MouseEvent e) {
            final Point pt = new Point();
            final Component c = getComponentToForwardTo(e, pt);
            if (c == null) return;
            c.dispatchEvent(
                    new MouseEvent(c, e.getID(), e.getWhen(), e.getModifiers(),
                                   pt.x, pt.y, e.getClickCount(),
                                   e.isPopupTrigger(), e.getButton()));
        }

        Component getComponentToForwardTo(final MouseEvent e, final Point dst) {
            if (frame == null) return null;
            final Container contentPane = frame.getContentPane();
            if (contentPane == null) return null;
            Point pt = SwingUtilities.convertPoint(this, e.getPoint(), contentPane);
            final Component c = SwingUtilities.getDeepestComponentAt(contentPane, pt.x, pt.y);
            if (c == null) return null;
            pt = SwingUtilities.convertPoint(contentPane, pt, c);
            if (dst != null) dst.setLocation(pt);
            return c;
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
            forwardEventToFrame(e);
        }

        @Override
        public void mouseEntered(final MouseEvent e) { }

        @Override
        public void mouseExited(final MouseEvent e) { }

        @Override
        public void mousePressed(final MouseEvent e) {
            if (frame == null) return;

            if (frame.isResizable() && !frame.isMaximum() && testGrowboxPoint(e.getX(), e.getY(), getWidth(), getHeight())) {
                originalLocation = SwingUtilities.convertPoint(this, e.getPoint(), frame);
                originalSize = frame.getSize();
                getDesktopManager().beginResizingFrame(frame, SwingConstants.SOUTH_EAST);
                return;
            }

            forwardEventToFrame(e);
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            if (originalLocation != null) {
                resizeInternalFrame(e.getPoint());
                originalLocation = null;
                getDesktopManager().endResizingFrame(frame);
                return;
            }

            forwardEventToFrame(e);
        }

        @Override
        public void mouseDragged(final MouseEvent e) {
            resizeInternalFrame(e.getPoint());
            repositionResizeBox();
        }

        @Override
        public void mouseMoved(final MouseEvent e) { }

        @Override
        @SuppressWarnings("deprecation")
        public void mouseWheelMoved(final MouseWheelEvent e) {
            final Point pt = new Point();
            final Component c = getComponentToForwardTo(e, pt);
            if (c == null) return;
            c.dispatchEvent(new MouseWheelEvent(c, e.getID(), e.getWhen(),
                    e.getModifiers(), pt.x, pt.y, e.getXOnScreen(), e.getYOnScreen(),
                    e.getClickCount(), e.isPopupTrigger(), e.getScrollType(),
                    e.getScrollAmount(), e.getWheelRotation(),
                    e.getPreciseWheelRotation()));
        }

        @Override
        public void componentResized(final ComponentEvent e) {
            repositionResizeBox();
        }

        @Override
        public void componentShown(final ComponentEvent e) {
            repositionResizeBox();
        }

        @Override
        public void componentMoved(final ComponentEvent e) {
            repositionResizeBox();
        }

        @Override
        public void componentHidden(final ComponentEvent e) { }

        @Override
        public void propertyChange(final PropertyChangeEvent evt) {
            if (!"resizable".equals(evt.getPropertyName())) return;
            setVisible(Boolean.TRUE.equals(evt.getNewValue()));
        }
    }
}
