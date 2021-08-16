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


package javax.swing;

import java.awt.event.*;
import java.awt.*;
import java.util.Objects;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;

/**
 * Manages all the <code>ToolTips</code> in the system.
 * <p>
 * ToolTipManager contains numerous properties for configuring how long it
 * will take for the tooltips to become visible, and how long till they
 * hide. Consider a component that has a different tooltip based on where
 * the mouse is, such as JTree. When the mouse moves into the JTree and
 * over a region that has a valid tooltip, the tooltip will become
 * visible after <code>initialDelay</code> milliseconds. After
 * <code>dismissDelay</code> milliseconds the tooltip will be hidden. If
 * the mouse is over a region that has a valid tooltip, and the tooltip
 * is currently visible, when the mouse moves to a region that doesn't have
 * a valid tooltip the tooltip will be hidden. If the mouse then moves back
 * into a region that has a valid tooltip within <code>reshowDelay</code>
 * milliseconds, the tooltip will immediately be shown, otherwise the
 * tooltip will be shown again after <code>initialDelay</code> milliseconds.
 *
 * @see JComponent#createToolTip
 * @author Dave Moore
 * @author Rich Schiavi
 * @since 1.2
 */
public class ToolTipManager extends MouseAdapter implements MouseMotionListener  {
    Timer enterTimer, exitTimer, insideTimer;
    String toolTipText;
    Point  preferredLocation;
    JComponent insideComponent;
    MouseEvent mouseEvent;
    boolean showImmediately;
    private static final Object TOOL_TIP_MANAGER_KEY = new Object();
    transient Popup tipWindow;
    /** The Window tip is being displayed in. This will be non-null if
     * the Window tip is in differs from that of insideComponent's Window.
     */
    private Window window;
    JToolTip tip;

    private Rectangle popupRect = null;
    private Rectangle popupFrameRect = null;

    boolean enabled = true;
    private boolean tipShowing = false;

    private FocusListener focusChangeListener = null;
    private MouseMotionListener moveBeforeEnterListener = null;
    private KeyListener accessibilityKeyListener = null;

    private KeyStroke postTip;
    private KeyStroke hideTip;

    /**
     * Lightweight popup enabled.
     */
    protected boolean lightWeightPopupEnabled = true;
    /**
     * Heavyweight popup enabled.
     */
    protected boolean heavyWeightPopupEnabled = false;

    @SuppressWarnings("deprecation")
    ToolTipManager() {
        enterTimer = new Timer(750, new insideTimerAction());
        enterTimer.setRepeats(false);
        exitTimer = new Timer(500, new outsideTimerAction());
        exitTimer.setRepeats(false);
        insideTimer = new Timer(4000, new stillInsideTimerAction());
        insideTimer.setRepeats(false);

        moveBeforeEnterListener = new MoveBeforeEnterListener();
        accessibilityKeyListener = new AccessibilityKeyListener();

        postTip = KeyStroke.getKeyStroke(KeyEvent.VK_F1, InputEvent.CTRL_MASK);
        hideTip =  KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    }

    /**
     * Enables or disables the tooltip.
     *
     * @param flag  true to enable the tip, false otherwise
     */
    public void setEnabled(boolean flag) {
        enabled = flag;
        if (!flag) {
            hideTipWindow();
        }
    }

    /**
     * Returns true if this object is enabled.
     *
     * @return true if this object is enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * When displaying the <code>JToolTip</code>, the
     * <code>ToolTipManager</code> chooses to use a lightweight
     * <code>JPanel</code> if it fits. This method allows you to
     * disable this feature. You have to do disable it if your
     * application mixes light weight and heavy weights components.
     *
     * @param aFlag true if a lightweight panel is desired, false otherwise
     *
     */
    public void setLightWeightPopupEnabled(boolean aFlag){
        lightWeightPopupEnabled = aFlag;
    }

    /**
     * Returns true if lightweight (all-Java) <code>Tooltips</code>
     * are in use, or false if heavyweight (native peer)
     * <code>Tooltips</code> are being used.
     *
     * @return true if lightweight <code>ToolTips</code> are in use
     */
    public boolean isLightWeightPopupEnabled() {
        return lightWeightPopupEnabled;
    }


    /**
     * Specifies the initial delay value.
     *
     * @param milliseconds  the number of milliseconds to delay
     *        (after the cursor has paused) before displaying the
     *        tooltip
     * @see #getInitialDelay
     */
    public void setInitialDelay(int milliseconds) {
        enterTimer.setInitialDelay(milliseconds);
    }

    /**
     * Returns the initial delay value.
     *
     * @return an integer representing the initial delay value,
     *          in milliseconds
     * @see #setInitialDelay
     */
    public int getInitialDelay() {
        return enterTimer.getInitialDelay();
    }

    /**
     * Specifies the dismissal delay value.
     *
     * @param milliseconds  the number of milliseconds to delay
     *        before taking away the tooltip
     * @see #getDismissDelay
     */
    public void setDismissDelay(int milliseconds) {
        insideTimer.setInitialDelay(milliseconds);
    }

    /**
     * Returns the dismissal delay value.
     *
     * @return an integer representing the dismissal delay value,
     *          in milliseconds
     * @see #setDismissDelay
     */
    public int getDismissDelay() {
        return insideTimer.getInitialDelay();
    }

    /**
     * Used to specify the amount of time before the user has to wait
     * <code>initialDelay</code> milliseconds before a tooltip will be
     * shown. That is, if the tooltip is hidden, and the user moves into
     * a region of the same Component that has a valid tooltip within
     * <code>milliseconds</code> milliseconds the tooltip will immediately
     * be shown. Otherwise, if the user moves into a region with a valid
     * tooltip after <code>milliseconds</code> milliseconds, the user
     * will have to wait an additional <code>initialDelay</code>
     * milliseconds before the tooltip is shown again.
     *
     * @param milliseconds time in milliseconds
     * @see #getReshowDelay
     */
    public void setReshowDelay(int milliseconds) {
        exitTimer.setInitialDelay(milliseconds);
    }

    /**
     * Returns the reshow delay property.
     *
     * @return reshown delay property
     * @see #setReshowDelay
     */
    public int getReshowDelay() {
        return exitTimer.getInitialDelay();
    }

    // Returns GraphicsConfiguration instance that toFind belongs to or null
    // if drawing point is set to a point beyond visible screen area (e.g.
    // Point(20000, 20000))
    private GraphicsConfiguration getDrawingGC(Point toFind) {
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = env.getScreenDevices();
        for (GraphicsDevice device : devices) {
            GraphicsConfiguration config = device.getDefaultConfiguration();
            Rectangle rect = config.getBounds();
            if (rect.contains(toFind)) {
                return config;
            }
        }

        return null;
    }

    void showTipWindow() {
        if(insideComponent == null || !insideComponent.isShowing())
            return;
        String mode = UIManager.getString("ToolTipManager.enableToolTipMode");
        if ("activeApplication".equals(mode)) {
            KeyboardFocusManager kfm =
                    KeyboardFocusManager.getCurrentKeyboardFocusManager();
            if (kfm.getFocusedWindow() == null) {
                return;
            }
        }
        if (enabled) {
            Dimension size;
            Point screenLocation = insideComponent.getLocationOnScreen();
            Point location;

            Point toFind;
            if (preferredLocation != null) {
                toFind = new Point(screenLocation.x + preferredLocation.x,
                        screenLocation.y + preferredLocation.y);
            } else {
                toFind = mouseEvent.getLocationOnScreen();
            }

            GraphicsConfiguration gc = getDrawingGC(toFind);
            if (gc == null) {
                toFind = mouseEvent.getLocationOnScreen();
                gc = getDrawingGC(toFind);
                if (gc == null) {
                    gc = insideComponent.getGraphicsConfiguration();
                }
            }

            Rectangle sBounds = gc.getBounds();
            Insets screenInsets = Toolkit.getDefaultToolkit()
                                             .getScreenInsets(gc);
            // Take into account screen insets, decrease viewport
            sBounds.x += screenInsets.left;
            sBounds.y += screenInsets.top;
            sBounds.width -= (screenInsets.left + screenInsets.right);
            sBounds.height -= (screenInsets.top + screenInsets.bottom);
        boolean leftToRight
                = SwingUtilities.isLeftToRight(insideComponent);

            // Just to be paranoid
            hideTipWindow();

            tip = insideComponent.createToolTip();
            tip.setTipText(toolTipText);
            size = tip.getPreferredSize();

            if(preferredLocation != null) {
                location = toFind;
        if (!leftToRight) {
            location.x -= size.width;
        }
            } else {
                location = new Point(screenLocation.x + mouseEvent.getX(),
                        screenLocation.y + mouseEvent.getY() + 20);
        if (!leftToRight) {
            if(location.x - size.width>=0) {
                location.x -= size.width;
            }
        }

            }

        // we do not adjust x/y when using awt.Window tips
        if (popupRect == null){
        popupRect = new Rectangle();
        }
        popupRect.setBounds(location.x,location.y,
                size.width,size.height);

        // Fit as much of the tooltip on screen as possible
            if (location.x < sBounds.x) {
                location.x = sBounds.x;
            }
            else if (location.x - sBounds.x + size.width > sBounds.width) {
                location.x = sBounds.x + Math.max(0, sBounds.width - size.width)
;
            }
            if (location.y < sBounds.y) {
                location.y = sBounds.y;
            }
            else if (location.y - sBounds.y + size.height > sBounds.height) {
                location.y = sBounds.y + Math.max(0, sBounds.height - size.height);
            }

            PopupFactory popupFactory = PopupFactory.getSharedInstance();

            if (lightWeightPopupEnabled) {
        int y = getPopupFitHeight(popupRect, insideComponent);
        int x = getPopupFitWidth(popupRect,insideComponent);
        if (x>0 || y>0) {
            popupFactory.setPopupType(PopupFactory.MEDIUM_WEIGHT_POPUP);
        } else {
            popupFactory.setPopupType(PopupFactory.LIGHT_WEIGHT_POPUP);
        }
            }
            else {
                popupFactory.setPopupType(PopupFactory.MEDIUM_WEIGHT_POPUP);
            }
        tipWindow = popupFactory.getPopup(insideComponent, tip,
                          location.x,
                          location.y);
            popupFactory.setPopupType(PopupFactory.LIGHT_WEIGHT_POPUP);

        tipWindow.show();

            Window componentWindow = SwingUtilities.windowForComponent(
                                                    insideComponent);

            window = SwingUtilities.windowForComponent(tip);
            if (window != null && window != componentWindow) {
                window.addMouseListener(this);
            }
            else {
                window = null;
            }

            insideTimer.start();
        tipShowing = true;
        }
    }

    void hideTipWindow() {
        if (tipWindow != null) {
            if (window != null) {
                window.removeMouseListener(this);
                window = null;
            }
            tipWindow.hide();
            tipWindow = null;
            tipShowing = false;
            tip = null;
            insideTimer.stop();
        }
    }

    /**
     * Returns a shared <code>ToolTipManager</code> instance.
     *
     * @return a shared <code>ToolTipManager</code> object
     */
    public static ToolTipManager sharedInstance() {
        Object value = SwingUtilities.appContextGet(TOOL_TIP_MANAGER_KEY);
        if (value instanceof ToolTipManager) {
            return (ToolTipManager) value;
        }
        ToolTipManager manager = new ToolTipManager();
        SwingUtilities.appContextPut(TOOL_TIP_MANAGER_KEY, manager);
        return manager;
    }

    // add keylistener here to trigger tip for access
    /**
     * Registers a component for tooltip management.
     * <p>
     * This will register key bindings to show and hide the tooltip text
     * only if <code>component</code> has focus bindings. This is done
     * so that components that are not normally focus traversable, such
     * as <code>JLabel</code>, are not made focus traversable as a result
     * of invoking this method.
     *
     * @param component  a <code>JComponent</code> object to add
     * @see JComponent#isFocusTraversable
     */
    public void registerComponent(JComponent component) {
        component.removeMouseListener(this);
        component.addMouseListener(this);
        component.removeMouseMotionListener(moveBeforeEnterListener);
        component.addMouseMotionListener(moveBeforeEnterListener);
        // use MenuKeyListener for menu items/elements
        if (component instanceof JMenuItem) {
            ((JMenuItem) component).removeMenuKeyListener((MenuKeyListener) accessibilityKeyListener);
            ((JMenuItem) component).addMenuKeyListener((MenuKeyListener) accessibilityKeyListener);
        } else {
            component.removeKeyListener(accessibilityKeyListener);
            component.addKeyListener(accessibilityKeyListener);
        }
    }

    /**
     * Removes a component from tooltip control.
     *
     * @param component  a <code>JComponent</code> object to remove
     */
    public void unregisterComponent(JComponent component) {
        component.removeMouseListener(this);
        component.removeMouseMotionListener(moveBeforeEnterListener);
        if (component instanceof JMenuItem) {
            ((JMenuItem) component).removeMenuKeyListener((MenuKeyListener) accessibilityKeyListener);
        } else {
            component.removeKeyListener(accessibilityKeyListener);
        }
    }

    // implements java.awt.event.MouseListener
    /**
     *  Called when the mouse enters the region of a component.
     *  This determines whether the tool tip should be shown.
     *
     *  @param event  the event in question
     */
    public void mouseEntered(MouseEvent event) {
        initiateToolTip(event);
    }

    private void initiateToolTip(MouseEvent event) {
        if (event.getSource() == window) {
            return;
        }
        JComponent component = (JComponent)event.getSource();
        component.removeMouseMotionListener(moveBeforeEnterListener);

        exitTimer.stop();

        Point location = event.getPoint();
        // ensure tooltip shows only in proper place
        if (location.x < 0 ||
            location.x >=component.getWidth() ||
            location.y < 0 ||
            location.y >= component.getHeight()) {
            return;
        }

        if (insideComponent != null) {
            enterTimer.stop();
        }
        // A component in an unactive internal frame is sent two
        // mouseEntered events, make sure we don't end up adding
        // ourselves an extra time.
        component.removeMouseMotionListener(this);
        component.addMouseMotionListener(this);

        boolean sameComponent = (insideComponent == component);

        insideComponent = component;
    if (tipWindow != null){
            mouseEvent = event;
            if (showImmediately) {
                String newToolTipText = component.getToolTipText(event);
                Point newPreferredLocation = component.getToolTipLocation(
                                                         event);
                boolean sameLoc = (preferredLocation != null) ?
                            preferredLocation.equals(newPreferredLocation) :
                            (newPreferredLocation == null);

                if (!sameComponent || !Objects.equals(toolTipText, newToolTipText)
                        || !sameLoc) {
                    toolTipText = newToolTipText;
                    preferredLocation = newPreferredLocation;
                    showTipWindow();
                }
            } else {
                enterTimer.start();
            }
        }
    }

    // implements java.awt.event.MouseListener
    /**
     *  Called when the mouse exits the region of a component.
     *  Any tool tip showing should be hidden.
     *
     *  @param event  the event in question
     */
    public void mouseExited(MouseEvent event) {
        boolean shouldHide = true;
        if (insideComponent == null) {
            // Drag exit
        }
        if (window != null && event.getSource() == window && insideComponent != null) {
          // if we get an exit and have a heavy window
          // we need to check if it if overlapping the inside component
            Container insideComponentWindow = insideComponent.getTopLevelAncestor();
            // insideComponent may be removed after tooltip is made visible
            if (insideComponentWindow != null) {
                Point location = event.getPoint();
                SwingUtilities.convertPointToScreen(location, window);

                location.x -= insideComponentWindow.getX();
                location.y -= insideComponentWindow.getY();

                location = SwingUtilities.convertPoint(null, location, insideComponent);
                if (location.x >= 0 && location.x < insideComponent.getWidth() &&
                        location.y >= 0 && location.y < insideComponent.getHeight()) {
                    shouldHide = false;
                } else {
                    shouldHide = true;
                }
            }
        } else if(event.getSource() == insideComponent && tipWindow != null) {
            Window win = SwingUtilities.getWindowAncestor(insideComponent);
            if (win != null) {  // insideComponent may have been hidden (e.g. in a menu)
                Point location = SwingUtilities.convertPoint(insideComponent,
                                                             event.getPoint(),
                                                             win);
                Rectangle bounds = insideComponent.getTopLevelAncestor().getBounds();
                location.x += bounds.x;
                location.y += bounds.y;

                Point loc = new Point(0, 0);
                SwingUtilities.convertPointToScreen(loc, tip);
                bounds.x = loc.x;
                bounds.y = loc.y;
                bounds.width = tip.getWidth();
                bounds.height = tip.getHeight();

                if (location.x >= bounds.x && location.x < (bounds.x + bounds.width) &&
                    location.y >= bounds.y && location.y < (bounds.y + bounds.height)) {
                    shouldHide = false;
                } else {
                    shouldHide = true;
                }
            }
        }

        if (shouldHide) {
            enterTimer.stop();
        if (insideComponent != null) {
                insideComponent.removeMouseMotionListener(this);
            }
            insideComponent = null;
            toolTipText = null;
            mouseEvent = null;
            hideTipWindow();
            exitTimer.restart();
        }
    }

    // implements java.awt.event.MouseListener
    /**
     *  Called when the mouse is pressed.
     *  Any tool tip showing should be hidden.
     *
     *  @param event  the event in question
     */
    public void mousePressed(MouseEvent event) {
        hideTipWindow();
        enterTimer.stop();
        showImmediately = false;
        insideComponent = null;
        mouseEvent = null;
    }

    // implements java.awt.event.MouseMotionListener
    /**
     *  Called when the mouse is pressed and dragged.
     *  Does nothing.
     *
     *  @param event  the event in question
     */
    public void mouseDragged(MouseEvent event) {
    }

    // implements java.awt.event.MouseMotionListener
    /**
     *  Called when the mouse is moved.
     *  Determines whether the tool tip should be displayed.
     *
     *  @param event  the event in question
     */
    public void mouseMoved(MouseEvent event) {
        if (tipShowing) {
            checkForTipChange(event);
        }
        else if (showImmediately) {
            JComponent component = (JComponent)event.getSource();
            toolTipText = component.getToolTipText(event);
            if (toolTipText != null) {
                preferredLocation = component.getToolTipLocation(event);
                mouseEvent = event;
                insideComponent = component;
                exitTimer.stop();
                showTipWindow();
            }
        }
        else {
            // Lazily lookup the values from within insideTimerAction
            insideComponent = (JComponent)event.getSource();
            mouseEvent = event;
            toolTipText = null;
            enterTimer.restart();
        }
    }

    /**
     * Checks to see if the tooltip needs to be changed in response to
     * the MouseMoved event <code>event</code>.
     */
    private void checkForTipChange(MouseEvent event) {
        JComponent component = (JComponent)event.getSource();
        String newText = component.getToolTipText(event);
        Point  newPreferredLocation = component.getToolTipLocation(event);

        if (newText != null || newPreferredLocation != null) {
            mouseEvent = event;
            if (((newText != null && newText.equals(toolTipText)) || newText == null) &&
                ((newPreferredLocation != null && newPreferredLocation.equals(preferredLocation))
                 || newPreferredLocation == null)) {
                if (tipWindow != null) {
                    insideTimer.restart();
                } else {
                    enterTimer.restart();
                }
            } else {
                toolTipText = newText;
                preferredLocation = newPreferredLocation;
                if (showImmediately) {
                    hideTipWindow();
                    showTipWindow();
                    exitTimer.stop();
                } else {
                    enterTimer.restart();
                }
            }
        } else {
            toolTipText = null;
            preferredLocation = null;
            mouseEvent = null;
            insideComponent = null;
            hideTipWindow();
            enterTimer.stop();
            exitTimer.restart();
        }
    }

    /**
     * Inside timer action.
     */
    protected class insideTimerAction implements ActionListener {

        /**
         * Constructs an {@code insideTimerAction}.
         */
        protected insideTimerAction() {}

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent e) {
            if(insideComponent != null && insideComponent.isShowing()) {
                // Lazy lookup
                if (toolTipText == null && mouseEvent != null) {
                    toolTipText = insideComponent.getToolTipText(mouseEvent);
                    preferredLocation = insideComponent.getToolTipLocation(
                                              mouseEvent);
                }
                if(toolTipText != null) {
                    showImmediately = true;
                    showTipWindow();
                }
                else {
                    insideComponent = null;
                    toolTipText = null;
                    preferredLocation = null;
                    mouseEvent = null;
                    hideTipWindow();
                }
            }
        }
    }

    /**
     * Outside timer action.
     */
    protected class outsideTimerAction implements ActionListener {

        /**
         * Constructs an {@code outsideTimerAction}.
         */
        protected outsideTimerAction() {}

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent e) {
            showImmediately = false;
        }
    }

    /**
     * Still inside timer action.
     */
    protected class stillInsideTimerAction implements ActionListener {

        /**
         * Constructs a {@code stillInsideTimerAction}.
         */
        protected stillInsideTimerAction() {}

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent e) {
            hideTipWindow();
            enterTimer.stop();
            showImmediately = false;
            insideComponent = null;
            mouseEvent = null;
        }
    }

  /* This listener is registered when the tooltip is first registered
   * on a component in order to catch the situation where the tooltip
   * was turned on while the mouse was already within the bounds of
   * the component.  This way, the tooltip will be initiated on a
   * mouse-entered or mouse-moved, whichever occurs first.  Once the
   * tooltip has been initiated, we can remove this listener and rely
   * solely on mouse-entered to initiate the tooltip.
   */
    private class MoveBeforeEnterListener extends MouseMotionAdapter {
        public void mouseMoved(MouseEvent e) {
            initiateToolTip(e);
        }
    }

    static Frame frameForComponent(Component component) {
        while (!(component instanceof Frame)) {
            component = component.getParent();
        }
        return (Frame)component;
    }

  private FocusListener createFocusChangeListener(){
    return new FocusAdapter(){
      public void focusLost(FocusEvent evt){
        hideTipWindow();
        insideComponent = null;
        JComponent c = (JComponent)evt.getSource();
        c.removeFocusListener(focusChangeListener);
      }
    };
  }

  // Returns: 0 no adjust
  //         -1 can't fit
  //         >0 adjust value by amount returned
 @SuppressWarnings("removal")
  private int getPopupFitWidth(Rectangle popupRectInScreen, Component invoker){
    if (invoker != null){
      Container parent;
      for (parent = invoker.getParent(); parent != null; parent = parent.getParent()){
        // fix internal frame size bug: 4139087 - 4159012
        if(parent instanceof JFrame || parent instanceof JDialog ||
           parent instanceof JWindow) { // no check for awt.Frame since we use Heavy tips
          return getWidthAdjust(parent.getBounds(),popupRectInScreen);
        } else if (parent instanceof JApplet || parent instanceof JInternalFrame) {
          if (popupFrameRect == null){
            popupFrameRect = new Rectangle();
          }
          Point p = parent.getLocationOnScreen();
          popupFrameRect.setBounds(p.x,p.y,
                                   parent.getBounds().width,
                                   parent.getBounds().height);
          return getWidthAdjust(popupFrameRect,popupRectInScreen);
        }
      }
    }
    return 0;
  }

  // Returns:  0 no adjust
  //          >0 adjust by value return
  @SuppressWarnings("removal")
  private int getPopupFitHeight(Rectangle popupRectInScreen, Component invoker){
    if (invoker != null){
      Container parent;
      for (parent = invoker.getParent(); parent != null; parent = parent.getParent()){
        if(parent instanceof JFrame || parent instanceof JDialog ||
           parent instanceof JWindow) {
          return getHeightAdjust(parent.getBounds(),popupRectInScreen);
        } else if (parent instanceof JApplet || parent instanceof JInternalFrame) {
          if (popupFrameRect == null){
            popupFrameRect = new Rectangle();
          }
          Point p = parent.getLocationOnScreen();
          popupFrameRect.setBounds(p.x,p.y,
                                   parent.getBounds().width,
                                   parent.getBounds().height);
          return getHeightAdjust(popupFrameRect,popupRectInScreen);
        }
      }
    }
    return 0;
  }

  private int getHeightAdjust(Rectangle a, Rectangle b){
    if (b.y >= a.y && (b.y + b.height) <= (a.y + a.height))
      return 0;
    else
      return (((b.y + b.height) - (a.y + a.height)) + 5);
  }

  // Return the number of pixels over the edge we are extending.
  // If we are over the edge the ToolTipManager can adjust.
  // REMIND: what if the Tooltip is just too big to fit at all - we currently will just clip
  private int getWidthAdjust(Rectangle a, Rectangle b){
    //    System.out.println("width b.x/b.width: " + b.x + "/" + b.width +
    //                 "a.x/a.width: " + a.x + "/" + a.width);
    if (b.x >= a.x && (b.x + b.width) <= (a.x + a.width)){
      return 0;
    }
    else {
      return (((b.x + b.width) - (a.x +a.width)) + 5);
    }
  }


    //
    // Actions
    //
    private void show(JComponent source) {
        if (tipWindow != null) { // showing we unshow
            hideTipWindow();
            insideComponent = null;
        }
        else {
            hideTipWindow(); // be safe
            enterTimer.stop();
            exitTimer.stop();
            insideTimer.stop();
            insideComponent = source;
            if (insideComponent != null){
                toolTipText = insideComponent.getToolTipText();
                preferredLocation = new Point(10,insideComponent.getHeight()+
                                              10);  // manual set
                showTipWindow();
                // put a focuschange listener on to bring the tip down
                if (focusChangeListener == null){
                    focusChangeListener = createFocusChangeListener();
                }
                insideComponent.addFocusListener(focusChangeListener);
            }
        }
    }

    private void hide(JComponent source) {
        hideTipWindow();
        source.removeFocusListener(focusChangeListener);
        preferredLocation = null;
        insideComponent = null;
    }

    /* This listener is registered when the tooltip is first registered
     * on a component in order to process accessibility keybindings.
     * This will apply globally across L&F
     *
     * Post Tip: Ctrl+F1
     * Unpost Tip: Esc and Ctrl+F1
     */
    private class AccessibilityKeyListener extends KeyAdapter implements MenuKeyListener {
        public void keyPressed(KeyEvent e) {
            if (!e.isConsumed()) {
                JComponent source = (JComponent) e.getComponent();
                KeyStroke keyStrokeForEvent = KeyStroke.getKeyStrokeForEvent(e);
                if (hideTip.equals(keyStrokeForEvent)) {
                    if (tipWindow != null) {
                        hide(source);
                        e.consume();
                    }
                } else if (postTip.equals(keyStrokeForEvent)) {
                    // Shown tooltip will be hidden
                    ToolTipManager.this.show(source);
                    e.consume();
                }
            }
        }

        @Override
        public void menuKeyTyped(MenuKeyEvent e) {}

        @Override
        public void menuKeyPressed(MenuKeyEvent e) {
            if (postTip.equals(KeyStroke.getKeyStrokeForEvent(e))) {
                // get element for the event
                MenuElement path[] = e.getPath();
                MenuElement element = path[path.length - 1];

                // retrieve currently highlighted element
                MenuSelectionManager msm = e.getMenuSelectionManager();
                MenuElement selectedPath[] = msm.getSelectedPath();
                MenuElement selectedElement = selectedPath[selectedPath.length - 1];

                if (element.equals(selectedElement)) {
                    // show/hide tooltip message
                    JComponent source = (JComponent) element.getComponent();
                    ToolTipManager.this.show(source);
                    e.consume();
                }
            }
        }

        @Override
        public void menuKeyReleased(MenuKeyEvent e) {}
    }
}
