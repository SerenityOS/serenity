/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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



package javax.swing.plaf.basic;



import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.border.Border;
import java.beans.*;
import sun.swing.DefaultLookup;



/**
 * Divider used by BasicSplitPaneUI. Subclassers may wish to override
 * paint to do something more interesting.
 * The border effect is drawn in BasicSplitPaneUI, so if you don't like
 * that border, reset it there.
 * To conditionally drag from certain areas subclass mousePressed and
 * call super when you wish the dragging to begin.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @author Scott Violet
 */
@SuppressWarnings("serial") // Same-version serialization only
public class BasicSplitPaneDivider extends Container
    implements PropertyChangeListener
{
    /**
     * Width or height of the divider based on orientation
     * {@code BasicSplitPaneUI} adds two to this.
     */
    protected static final int ONE_TOUCH_SIZE = 6;

    /**
     * The offset of the divider.
     */
    protected static final int ONE_TOUCH_OFFSET = 2;

    /**
     * Handles mouse dragging message to do the actual dragging.
     */
    protected DragController dragger;

    /**
     * UI this instance was created from.
     */
    protected BasicSplitPaneUI splitPaneUI;

    /**
     * Size of the divider.
     */
    protected int dividerSize = 0; // default - SET TO 0???

    /**
     * Divider that is used for noncontinuous layout mode.
     */
    protected Component hiddenDivider;

    /**
     * JSplitPane the receiver is contained in.
     */
    protected JSplitPane splitPane;

    /**
     * Handles mouse events from both this class, and the split pane.
     * Mouse events are handled for the splitpane since you want to be able
     * to drag when clicking on the border of the divider, which is not
     * drawn by the divider.
     */
    protected MouseHandler mouseHandler;

    /**
     * Orientation of the JSplitPane.
     */
    protected int orientation;

    /**
     * Button for quickly toggling the left component.
     */
    protected JButton leftButton;

    /**
     * Button for quickly toggling the right component.
     */
    protected JButton rightButton;

    /** Border. */
    private Border border;

    /**
     * Is the mouse over the divider?
     */
    private boolean mouseOver;

    private int oneTouchSize;
    private int oneTouchOffset;

    /**
     * If true the one touch buttons are centered on the divider.
     */
    private boolean centerOneTouchButtons;


    /**
     * Creates an instance of {@code BasicSplitPaneDivider}. Registers this
     * instance for mouse events and mouse dragged events.
     *
     * @param ui an instance of {@code BasicSplitPaneUI}
     */
    public BasicSplitPaneDivider(BasicSplitPaneUI ui) {
        oneTouchSize = DefaultLookup.getInt(ui.getSplitPane(), ui,
                "SplitPane.oneTouchButtonSize", ONE_TOUCH_SIZE);
        oneTouchOffset = DefaultLookup.getInt(ui.getSplitPane(), ui,
                "SplitPane.oneTouchButtonOffset", ONE_TOUCH_OFFSET);
        centerOneTouchButtons = DefaultLookup.getBoolean(ui.getSplitPane(),
                 ui, "SplitPane.centerOneTouchButtons", true);
        setLayout(new DividerLayout());
        setBasicSplitPaneUI(ui);
        orientation = splitPane.getOrientation();
        setCursor((orientation == JSplitPane.HORIZONTAL_SPLIT) ?
                  Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR) :
                  Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
        setBackground(UIManager.getColor("SplitPane.background"));
    }

    private void revalidateSplitPane() {
        invalidate();
        if (splitPane != null) {
            splitPane.revalidate();
        }
    }

    /**
     * Sets the {@code SplitPaneUI} that is using the receiver.
     *
     * @param newUI the new {@code SplitPaneUI}
     */
    public void setBasicSplitPaneUI(BasicSplitPaneUI newUI) {
        if (splitPane != null) {
            splitPane.removePropertyChangeListener(this);
           if (mouseHandler != null) {
               splitPane.removeMouseListener(mouseHandler);
               splitPane.removeMouseMotionListener(mouseHandler);
               removeMouseListener(mouseHandler);
               removeMouseMotionListener(mouseHandler);
               mouseHandler = null;
           }
        }
        splitPaneUI = newUI;
        if (newUI != null) {
            splitPane = newUI.getSplitPane();
            if (splitPane != null) {
                if (mouseHandler == null) mouseHandler = new MouseHandler();
                splitPane.addMouseListener(mouseHandler);
                splitPane.addMouseMotionListener(mouseHandler);
                addMouseListener(mouseHandler);
                addMouseMotionListener(mouseHandler);
                splitPane.addPropertyChangeListener(this);
                if (splitPane.isOneTouchExpandable()) {
                    oneTouchExpandableChanged();
                }
            }
        }
        else {
            splitPane = null;
        }
    }


    /**
     * Returns the {@code SplitPaneUI} the receiver is currently in.
     *
     * @return the {@code SplitPaneUI} the receiver is currently in
     */
    public BasicSplitPaneUI getBasicSplitPaneUI() {
        return splitPaneUI;
    }


    /**
     * Sets the size of the divider to {@code newSize}. That is
     * the width if the splitpane is {@code HORIZONTAL_SPLIT}, or
     * the height of {@code VERTICAL_SPLIT}.
     *
     * @param newSize a new size
     */
    public void setDividerSize(int newSize) {
        dividerSize = newSize;
    }


    /**
     * Returns the size of the divider, that is the width if the splitpane
     * is HORIZONTAL_SPLIT, or the height of VERTICAL_SPLIT.
     *
     * @return the size of the divider
     */
    public int getDividerSize() {
        return dividerSize;
    }


    /**
     * Sets the border of this component.
     *
     * @param border a new border
     * @since 1.3
     */
    public void setBorder(Border border) {
        Border         oldBorder = this.border;

        this.border = border;
    }

    /**
     * Returns the border of this component or null if no border is
     * currently set.
     *
     * @return the border object for this component
     * @see #setBorder
     * @since 1.3
     */
    public Border getBorder() {
        return border;
    }

    /**
     * If a border has been set on this component, returns the
     * border's insets, else calls super.getInsets.
     *
     * @return the value of the insets property.
     * @see #setBorder
     */
    public Insets getInsets() {
        Border    border = getBorder();

        if (border != null) {
            return border.getBorderInsets(this);
        }
        return super.getInsets();
    }

    /**
     * Sets whether or not the mouse is currently over the divider.
     *
     * @param mouseOver whether or not the mouse is currently over the divider
     * @since 1.5
     */
    protected void setMouseOver(boolean mouseOver) {
        this.mouseOver = mouseOver;
    }

    /**
     * Returns whether or not the mouse is currently over the divider
     *
     * @return whether or not the mouse is currently over the divider
     * @since 1.5
     */
    public boolean isMouseOver() {
        return mouseOver;
    }

    /**
     * Returns the preferred size of the divider.
     * @implNote In current implementation,
     * if the splitpane is HORIZONTAL_SPLIT, the preferred size is obtained from
     * width of {@code getDividerSize} pixels and height of 1 pixel
     * If the splitpane is VERTICAL_SPLIT, the preferred size is obtained from
     * height of {@code getDividerSize} pixels and width of 1 pixel
     *
     * @return a {@code Dimension} object containing the preferred size of
     *         {@code BasicSplitPaneDivider}
     */
    public Dimension getPreferredSize() {
        // Ideally this would return the size from the layout manager,
        // but that could result in the layed out size being different from
        // the dividerSize, which may break developers as well as
        // BasicSplitPaneUI.
        if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
            return new Dimension(getDividerSize(), 1);
        }
        return new Dimension(1, getDividerSize());
    }

    /**
     * Returns the minimum size of the divider.
     * @implNote In current implementation,
     * if the splitpane is HORIZONTAL_SPLIT, the minimum size is obtained from
     * width of {@code getDividerSize} pixels and height of 1 pixel
     * If the splitpane is VERTICAL_SPLIT, the minimum size is obtained from
     * height of {@code getDividerSize} pixels and width of 1 pixel
     *
     * @return a {@code Dimension} object containing the minimum size of
     *         {@code BasicSplitPaneDivider}
     */
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }


    /**
     * Property change event, presumably from the JSplitPane, will message
     * updateOrientation if necessary.
     */
    public void propertyChange(PropertyChangeEvent e) {
        if (e.getSource() == splitPane) {
            if (e.getPropertyName() == JSplitPane.ORIENTATION_PROPERTY) {
                orientation = splitPane.getOrientation();
                setCursor((orientation == JSplitPane.HORIZONTAL_SPLIT) ?
                          Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR) :
                          Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                revalidateSplitPane();
            }
            else if (e.getPropertyName() == JSplitPane.
                      ONE_TOUCH_EXPANDABLE_PROPERTY) {
                oneTouchExpandableChanged();
            }
        }
    }


    /**
     * Paints the divider.
     */
    public void paint(Graphics g) {
      super.paint(g);

      // Paint the border.
      Border   border = getBorder();

      if (border != null) {
          Dimension     size = getSize();

          border.paintBorder(this, g, 0, 0, size.width, size.height);
      }
    }


    /**
     * Messaged when the oneTouchExpandable value of the JSplitPane the
     * receiver is contained in changes. Will create the
     * <code>leftButton</code> and <code>rightButton</code> if they
     * are null. invalidates the receiver as well.
     */
    protected void oneTouchExpandableChanged() {
        if (!DefaultLookup.getBoolean(splitPane, splitPaneUI,
                           "SplitPane.supportsOneTouchButtons", true)) {
            // Look and feel doesn't want to support one touch buttons, bail.
            return;
        }
        if (splitPane.isOneTouchExpandable() &&
            leftButton == null &&
            rightButton == null) {
            /* Create the left button and add an action listener to
               expand/collapse it. */
            leftButton = createLeftOneTouchButton();
            if (leftButton != null)
                leftButton.addActionListener(new OneTouchActionHandler(true));


            /* Create the right button and add an action listener to
               expand/collapse it. */
            rightButton = createRightOneTouchButton();
            if (rightButton != null)
                rightButton.addActionListener(new OneTouchActionHandler
                    (false));

            if (leftButton != null && rightButton != null) {
                add(leftButton);
                add(rightButton);
            }
        }
        revalidateSplitPane();
    }


    /**
     * Creates and return an instance of {@code JButton} that can be used to
     * collapse the left component in the split pane.
     *
     * @return an instance of {@code JButton}
     */
    protected JButton createLeftOneTouchButton() {
        JButton b = new JButton() {
            public void setBorder(Border b) {
            }
            public void paint(Graphics g) {
                if (splitPane != null) {
                    int[]   xs = new int[3];
                    int[]   ys = new int[3];
                    int     blockSize;

                    // Fill the background first ...
                    g.setColor(this.getBackground());
                    g.fillRect(0, 0, this.getWidth(),
                               this.getHeight());

                    // ... then draw the arrow.
                    g.setColor(Color.black);
                    if (orientation == JSplitPane.VERTICAL_SPLIT) {
                        blockSize = Math.min(getHeight(), oneTouchSize);
                        xs[0] = blockSize;
                        xs[1] = 0;
                        xs[2] = blockSize << 1;
                        ys[0] = 0;
                        ys[1] = ys[2] = blockSize;
                        g.drawPolygon(xs, ys, 3); // Little trick to make the
                                                  // arrows of equal size
                    }
                    else {
                        blockSize = Math.min(getWidth(), oneTouchSize);
                        xs[0] = xs[2] = blockSize;
                        xs[1] = 0;
                        ys[0] = 0;
                        ys[1] = blockSize;
                        ys[2] = blockSize << 1;
                    }
                    g.fillPolygon(xs, ys, 3);
                }
            }
            // Don't want the button to participate in focus traversable.
            @SuppressWarnings("deprecation")
            public boolean isFocusTraversable() {
                return false;
            }
        };
        b.setMinimumSize(new Dimension(oneTouchSize, oneTouchSize));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setRequestFocusEnabled(false);
        return b;
    }


    /**
     * Creates and return an instance of {@code JButton} that can be used to
     * collapse the right component in the split pane.
     *
     * @return an instance of {@code JButton}
     */
    protected JButton createRightOneTouchButton() {
        JButton b = new JButton() {
            public void setBorder(Border border) {
            }
            public void paint(Graphics g) {
                if (splitPane != null) {
                    int[]          xs = new int[3];
                    int[]          ys = new int[3];
                    int            blockSize;

                    // Fill the background first ...
                    g.setColor(this.getBackground());
                    g.fillRect(0, 0, this.getWidth(),
                               this.getHeight());

                    // ... then draw the arrow.
                    if (orientation == JSplitPane.VERTICAL_SPLIT) {
                        blockSize = Math.min(getHeight(), oneTouchSize);
                        xs[0] = blockSize;
                        xs[1] = blockSize << 1;
                        xs[2] = 0;
                        ys[0] = blockSize;
                        ys[1] = ys[2] = 0;
                    }
                    else {
                        blockSize = Math.min(getWidth(), oneTouchSize);
                        xs[0] = xs[2] = 0;
                        xs[1] = blockSize;
                        ys[0] = 0;
                        ys[1] = blockSize;
                        ys[2] = blockSize << 1;
                    }
                    g.setColor(Color.black);
                    g.fillPolygon(xs, ys, 3);
                }
            }
            // Don't want the button to participate in focus traversable.
            @SuppressWarnings("deprecation")
            public boolean isFocusTraversable() {
                return false;
            }
        };
        b.setMinimumSize(new Dimension(oneTouchSize, oneTouchSize));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setRequestFocusEnabled(false);
        return b;
    }


    /**
     * Message to prepare for dragging. This messages the BasicSplitPaneUI
     * with startDragging.
     */
    protected void prepareForDragging() {
        splitPaneUI.startDragging();
    }


    /**
     * Messages the BasicSplitPaneUI with dragDividerTo that this instance
     * is contained in.
     *
     * @param location a location
     */
    protected void dragDividerTo(int location) {
        splitPaneUI.dragDividerTo(location);
    }


    /**
     * Messages the BasicSplitPaneUI with finishDraggingTo that this instance
     * is contained in.
     *
     * @param location a location
     */
    protected void finishDraggingTo(int location) {
        splitPaneUI.finishDraggingTo(location);
    }


    /**
     * MouseHandler is responsible for converting mouse events
     * (released, dragged...) into the appropriate DragController
     * methods.
     *
     */
    protected class MouseHandler extends MouseAdapter
            implements MouseMotionListener
    {
        /**
         * Constructs a {@code MouseHandler}.
         */
        protected MouseHandler() {}

        /**
         * Starts the dragging session by creating the appropriate instance
         * of DragController.
         */
        public void mousePressed(MouseEvent e) {
            if ((e.getSource() == BasicSplitPaneDivider.this ||
                 e.getSource() == splitPane) &&
                dragger == null &&splitPane.isEnabled()) {
                Component            newHiddenDivider = splitPaneUI.
                                     getNonContinuousLayoutDivider();

                if (hiddenDivider != newHiddenDivider) {
                    if (hiddenDivider != null) {
                        hiddenDivider.removeMouseListener(this);
                        hiddenDivider.removeMouseMotionListener(this);
                    }
                    hiddenDivider = newHiddenDivider;
                    if (hiddenDivider != null) {
                        hiddenDivider.addMouseMotionListener(this);
                        hiddenDivider.addMouseListener(this);
                    }
                }
                if (splitPane.getLeftComponent() != null &&
                    splitPane.getRightComponent() != null) {
                    if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
                        dragger = new DragController(e);
                    }
                    else {
                        dragger = new VerticalDragController(e);
                    }
                    if (!dragger.isValid()) {
                        dragger = null;
                    }
                    else {
                        prepareForDragging();
                        dragger.continueDrag(e);
                    }
                }
                e.consume();
            }
        }


        /**
         * If dragger is not null it is messaged with completeDrag.
         */
        public void mouseReleased(MouseEvent e) {
            if (dragger != null) {
                if (e.getSource() == splitPane) {
                    dragger.completeDrag(e.getX(), e.getY());
                }
                else if (e.getSource() == BasicSplitPaneDivider.this) {
                    Point   ourLoc = getLocation();

                    dragger.completeDrag(e.getX() + ourLoc.x,
                                         e.getY() + ourLoc.y);
                }
                else if (e.getSource() == hiddenDivider) {
                    Point   hDividerLoc = hiddenDivider.getLocation();
                    int     ourX = e.getX() + hDividerLoc.x;
                    int     ourY = e.getY() + hDividerLoc.y;

                    dragger.completeDrag(ourX, ourY);
                }
                dragger = null;
                e.consume();
            }
        }


        //
        // MouseMotionListener
        //

        /**
         * If dragger is not null it is messaged with continueDrag.
         */
        public void mouseDragged(MouseEvent e) {
            if (dragger != null) {
                if (e.getSource() == splitPane) {
                    dragger.continueDrag(e.getX(), e.getY());
                }
                else if (e.getSource() == BasicSplitPaneDivider.this) {
                    Point   ourLoc = getLocation();

                    dragger.continueDrag(e.getX() + ourLoc.x,
                                         e.getY() + ourLoc.y);
                }
                else if (e.getSource() == hiddenDivider) {
                    Point   hDividerLoc = hiddenDivider.getLocation();
                    int     ourX = e.getX() + hDividerLoc.x;
                    int     ourY = e.getY() + hDividerLoc.y;

                    dragger.continueDrag(ourX, ourY);
                }
                e.consume();
            }
        }


        /**
         *  Resets the cursor based on the orientation.
         */
        public void mouseMoved(MouseEvent e) {
        }

        /**
         * Invoked when the mouse enters a component.
         *
         * @param e MouseEvent describing the details of the enter event.
         * @since 1.5
         */
        public void mouseEntered(MouseEvent e) {
            if (e.getSource() == BasicSplitPaneDivider.this) {
                setMouseOver(true);
            }
        }

        /**
         * Invoked when the mouse exits a component.
         *
         * @param e MouseEvent describing the details of the exit event.
         * @since 1.5
         */
        public void mouseExited(MouseEvent e) {
            if (e.getSource() == BasicSplitPaneDivider.this) {
                setMouseOver(false);
            }
        }
    }


    /**
     * Handles the events during a dragging session for a
     * HORIZONTAL_SPLIT oriented split pane. This continually
     * messages <code>dragDividerTo</code> and then when done messages
     * <code>finishDraggingTo</code>. When an instance is created it should be
     * messaged with <code>isValid</code> to insure that dragging can happen
     * (dragging won't be allowed if the two views can not be resized).
     * <p>
     * <strong>Warning:</strong>
     * Serialized objects of this class will not be compatible with
     * future Swing releases. The current serialization support is
     * appropriate for short term storage or RMI between applications running
     * the same version of Swing.  As of 1.4, support for long term storage
     * of all JavaBeans
     * has been added to the <code>java.beans</code> package.
     * Please see {@link java.beans.XMLEncoder}.
     */
    @SuppressWarnings("serial") // Same-version serialization only
    protected class DragController
    {
        /**
         * Initial location of the divider.
         */
        int initialX;

        /**
         * Maximum and minimum positions to drag to.
         */
        int maxX, minX;

        /**
         * Initial location the mouse down happened at.
         */
        int offset;

        /**
         * Constructs a new instance of {@code DragController}.
         *
         * @param e a mouse event
         */
        protected DragController(MouseEvent e) {
            JSplitPane  splitPane = splitPaneUI.getSplitPane();
            Component   leftC = splitPane.getLeftComponent();
            Component   rightC = splitPane.getRightComponent();

            initialX = getLocation().x;
            if (e.getSource() == BasicSplitPaneDivider.this) {
                offset = e.getX();
            }
            else { // splitPane
                offset = e.getX() - initialX;
            }
            if (leftC == null || rightC == null || offset < -1 ||
                offset >= getSize().width) {
                // Don't allow dragging.
                maxX = -1;
            }
            else {
                Insets      insets = splitPane.getInsets();

                if (leftC.isVisible()) {
                    minX = leftC.getMinimumSize().width;
                    if (insets != null) {
                        minX += insets.left;
                    }
                }
                else {
                    minX = 0;
                }
                if (rightC.isVisible()) {
                    int right = (insets != null) ? insets.right : 0;
                    maxX = Math.max(0, splitPane.getSize().width -
                                    (getSize().width + right) -
                                    rightC.getMinimumSize().width);
                }
                else {
                    int right = (insets != null) ? insets.right : 0;
                    maxX = Math.max(0, splitPane.getSize().width -
                                    (getSize().width + right));
                }
                if (maxX < minX) minX = maxX = 0;
            }
        }


        /**
         * Returns {@code true} if the dragging session is valid.
         *
         * @return {@code true} if the dragging session is valid
         */
        protected boolean isValid() {
            return (maxX > 0);
        }


        /**
         * Returns the new position to put the divider at based on
         * the passed in MouseEvent.
         *
         * @param e a mouse event
         * @return the new position
         */
        protected int positionForMouseEvent(MouseEvent e) {
            int newX = (e.getSource() == BasicSplitPaneDivider.this) ?
                        (e.getX() + getLocation().x) : e.getX();

            newX = Math.min(maxX, Math.max(minX, newX - offset));
            return newX;
        }


        /**
         * Returns the x argument, since this is used for horizontal
         * splits.
         *
         * @param x an X coordinate
         * @param y an Y coordinate
         * @return the X argument
         */
        protected int getNeededLocation(int x, int y) {
            int newX;

            newX = Math.min(maxX, Math.max(minX, x - offset));
            return newX;
        }

        /**
         * Messages dragDividerTo with the new location for the mouse
         * event.
         *
         * @param newX an X coordinate
         * @param newY an Y coordinate
         */
        protected void continueDrag(int newX, int newY) {
            dragDividerTo(getNeededLocation(newX, newY));
        }


        /**
         * Messages dragDividerTo with the new location for the mouse
         * event.
         *
         * @param e a mouse event
         */
        protected void continueDrag(MouseEvent e) {
            dragDividerTo(positionForMouseEvent(e));
        }

        /**
         * Messages finishDraggingTo with the new location for the mouse
         * event.
         *
         * @param x an X coordinate
         * @param y an Y coordinate
         */
        protected void completeDrag(int x, int y) {
            finishDraggingTo(getNeededLocation(x, y));
        }


        /**
         * Messages finishDraggingTo with the new location for the mouse
         * event.
         *
         * @param e a mouse event
         */
        protected void completeDrag(MouseEvent e) {
            finishDraggingTo(positionForMouseEvent(e));
        }
    } // End of BasicSplitPaneDivider.DragController


    /**
     * Handles the events during a dragging session for a
     * VERTICAL_SPLIT oriented split pane. This continually
     * messages <code>dragDividerTo</code> and then when done messages
     * <code>finishDraggingTo</code>. When an instance is created it should be
     * messaged with <code>isValid</code> to insure that dragging can happen
     * (dragging won't be allowed if the two views can not be resized).
     */
    protected class VerticalDragController extends DragController
    {
        /* DragControllers ivars are now in terms of y, not x. */
        /**
         * Constructs a new instance of {@code VerticalDragController}.
         *
         * @param e a mouse event
         */
        protected VerticalDragController(MouseEvent e) {
            super(e);
            JSplitPane splitPane = splitPaneUI.getSplitPane();
            Component  leftC = splitPane.getLeftComponent();
            Component  rightC = splitPane.getRightComponent();

            initialX = getLocation().y;
            if (e.getSource() == BasicSplitPaneDivider.this) {
                offset = e.getY();
            }
            else {
                offset = e.getY() - initialX;
            }
            if (leftC == null || rightC == null || offset < -1 ||
                offset > getSize().height) {
                // Don't allow dragging.
                maxX = -1;
            }
            else {
                Insets     insets = splitPane.getInsets();

                if (leftC.isVisible()) {
                    minX = leftC.getMinimumSize().height;
                    if (insets != null) {
                        minX += insets.top;
                    }
                }
                else {
                    minX = 0;
                }
                if (rightC.isVisible()) {
                    int    bottom = (insets != null) ? insets.bottom : 0;

                    maxX = Math.max(0, splitPane.getSize().height -
                                    (getSize().height + bottom) -
                                    rightC.getMinimumSize().height);
                }
                else {
                    int    bottom = (insets != null) ? insets.bottom : 0;

                    maxX = Math.max(0, splitPane.getSize().height -
                                    (getSize().height + bottom));
                }
                if (maxX < minX) minX = maxX = 0;
            }
        }


        /**
         * Returns the y argument, since this is used for vertical
         * splits.
         */
        protected int getNeededLocation(int x, int y) {
            int newY;

            newY = Math.min(maxX, Math.max(minX, y - offset));
            return newY;
        }


        /**
         * Returns the new position to put the divider at based on
         * the passed in MouseEvent.
         */
        protected int positionForMouseEvent(MouseEvent e) {
            int newY = (e.getSource() == BasicSplitPaneDivider.this) ?
                        (e.getY() + getLocation().y) : e.getY();


            newY = Math.min(maxX, Math.max(minX, newY - offset));
            return newY;
        }
    } // End of BasicSplitPaneDividier.VerticalDragController


    /**
     * Used to layout a <code>BasicSplitPaneDivider</code>.
     * Layout for the divider
     * involves appropriately moving the left/right buttons around.
     *
     */
    protected class DividerLayout implements LayoutManager
    {
        /**
         * Constructs a {@code DividerLayout}.
         */
        protected DividerLayout() {}

        public void layoutContainer(Container c) {
            if (leftButton != null && rightButton != null &&
                c == BasicSplitPaneDivider.this) {
                if (splitPane.isOneTouchExpandable()) {
                    Insets insets = getInsets();

                    if (orientation == JSplitPane.VERTICAL_SPLIT) {
                        int extraX = (insets != null) ? insets.left : 0;
                        int blockSize = getHeight();

                        if (insets != null) {
                            blockSize -= (insets.top + insets.bottom);
                            blockSize = Math.max(blockSize, 0);
                        }
                        blockSize = Math.min(blockSize, oneTouchSize);

                        int y = (c.getSize().height - blockSize) / 2;

                        if (!centerOneTouchButtons) {
                            y = (insets != null) ? insets.top : 0;
                            extraX = 0;
                        }
                        leftButton.setBounds(extraX + oneTouchOffset, y,
                                             blockSize * 2, blockSize);
                        rightButton.setBounds(extraX + oneTouchOffset +
                                              oneTouchSize * 2, y,
                                              blockSize * 2, blockSize);
                    }
                    else {
                        int extraY = (insets != null) ? insets.top : 0;
                        int blockSize = getWidth();

                        if (insets != null) {
                            blockSize -= (insets.left + insets.right);
                            blockSize = Math.max(blockSize, 0);
                        }
                        blockSize = Math.min(blockSize, oneTouchSize);

                        int x = (c.getSize().width - blockSize) / 2;

                        if (!centerOneTouchButtons) {
                            x = (insets != null) ? insets.left : 0;
                            extraY = 0;
                        }

                        leftButton.setBounds(x, extraY + oneTouchOffset,
                                             blockSize, blockSize * 2);
                        rightButton.setBounds(x, extraY + oneTouchOffset +
                                              oneTouchSize * 2, blockSize,
                                              blockSize * 2);
                    }
                }
                else {
                    leftButton.setBounds(-5, -5, 1, 1);
                    rightButton.setBounds(-5, -5, 1, 1);
                }
            }
        }


        public Dimension minimumLayoutSize(Container c) {
            // NOTE: This isn't really used, refer to
            // BasicSplitPaneDivider.getPreferredSize for the reason.
            // I leave it in hopes of having this used at some point.
            if (c != BasicSplitPaneDivider.this || splitPane == null) {
                return new Dimension(0,0);
            }
            Dimension buttonMinSize = null;

            if (splitPane.isOneTouchExpandable() && leftButton != null) {
                buttonMinSize = leftButton.getMinimumSize();
            }

            Insets insets = getInsets();
            int width = getDividerSize();
            int height = width;

            if (orientation == JSplitPane.VERTICAL_SPLIT) {
                if (buttonMinSize != null) {
                    int size = buttonMinSize.height;
                    if (insets != null) {
                        size += insets.top + insets.bottom;
                    }
                    height = Math.max(height, size);
                }
                width = 1;
            }
            else {
                if (buttonMinSize != null) {
                    int size = buttonMinSize.width;
                    if (insets != null) {
                        size += insets.left + insets.right;
                    }
                    width = Math.max(width, size);
                }
                height = 1;
            }
            return new Dimension(width, height);
        }


        public Dimension preferredLayoutSize(Container c) {
            return minimumLayoutSize(c);
        }


        public void removeLayoutComponent(Component c) {}

        public void addLayoutComponent(String string, Component c) {}
    } // End of class BasicSplitPaneDivider.DividerLayout


    /**
     * Listeners installed on the one touch expandable buttons.
     */
    private class OneTouchActionHandler implements ActionListener {
        /** True indicates the resize should go the minimum (top or left)
         * vs false which indicates the resize should go to the maximum.
         */
        private boolean toMinimum;

        OneTouchActionHandler(boolean toMinimum) {
            this.toMinimum = toMinimum;
        }

        public void actionPerformed(ActionEvent e) {
            Insets  insets = splitPane.getInsets();
            int     lastLoc = splitPane.getLastDividerLocation();
            int     currentLoc = splitPaneUI.getDividerLocation(splitPane);
            int     newLoc;

            // We use the location from the UI directly, as the location the
            // JSplitPane itself maintains is not necessarly correct.
            if (toMinimum) {
                if (orientation == JSplitPane.VERTICAL_SPLIT) {
                    if (currentLoc >= (splitPane.getHeight() -
                                       insets.bottom - getHeight())) {
                        int maxLoc = splitPane.getMaximumDividerLocation();
                        newLoc = Math.min(lastLoc, maxLoc);
                        splitPaneUI.setKeepHidden(false);
                    }
                    else {
                        newLoc = insets.top;
                        splitPaneUI.setKeepHidden(true);
                    }
                }
                else {
                    if (currentLoc >= (splitPane.getWidth() -
                                       insets.right - getWidth())) {
                        int maxLoc = splitPane.getMaximumDividerLocation();
                        newLoc = Math.min(lastLoc, maxLoc);
                        splitPaneUI.setKeepHidden(false);
                    }
                    else {
                        newLoc = insets.left;
                        splitPaneUI.setKeepHidden(true);
                    }
                }
            }
            else {
                if (orientation == JSplitPane.VERTICAL_SPLIT) {
                    if (currentLoc == insets.top) {
                        int maxLoc = splitPane.getMaximumDividerLocation();
                        newLoc = Math.min(lastLoc, maxLoc);
                        splitPaneUI.setKeepHidden(false);
                    }
                    else {
                        newLoc = splitPane.getHeight() - getHeight() -
                                 insets.top;
                        splitPaneUI.setKeepHidden(true);
                    }
                }
                else {
                    if (currentLoc == insets.left) {
                        int maxLoc = splitPane.getMaximumDividerLocation();
                        newLoc = Math.min(lastLoc, maxLoc);
                        splitPaneUI.setKeepHidden(false);
                    }
                    else {
                        newLoc = splitPane.getWidth() - getWidth() -
                                 insets.left;
                        splitPaneUI.setKeepHidden(true);
                    }
                }
            }
            if (currentLoc != newLoc) {
                splitPane.setDividerLocation(newLoc);
                // We do this in case the dividers notion of the location
                // differs from the real location.
                splitPane.setLastDividerLocation(currentLoc);
            }
        }
    } // End of class BasicSplitPaneDivider.LeftActionListener
}
