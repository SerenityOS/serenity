/*
 * Copyright (c) 2001, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package test.java.awt.event.helpers.lwcomponents;

import java.awt.*;
import java.awt.event.*;

/**
 * Lightweight <i>Button</i> component with some nice features. This
 * component provides the capabilities of Buttons, namely that you it
 * displays a label string and, when clicked, causes the
 * ActionListener method to be called.<p>
 *
 * The look of the button is a little unusual. There are three
 * rectangles drawn at the border that indicate various states
 * of the button.  These are (listed from outside in)<p>
 * <ol>
 * <li><b>Focus</b>: Indicates that the LWButton has the focus.
 * <li><b>Mouse Over</b>: Indicates that the mouse is over the component.
 * <li><b>Mouse Pressed</b>: Indicates that the mouse has been pressed.
 * </ol>
 *
 * In addition, when the button has been activated (mouse clicked or
 * via keyboard activation) the button flashes briefly.
 */

public class LWButton extends LWComponent {

  /*
   * The button's Label.
   * If Label is not specified it will default to "".
   * @serial
   * @see getLabel()
   * @see setLabel()
   */
  private String label;
  private boolean isInClick = false;

  private static final String base = "LWButton";
  private static int nameCounter = 0;

  private transient ActionListener actionListener;

  /*
   * The action to be performaed once a button has been
   * pressed.
   * actionCommand can be null.
   * @serial
   * @see getActionCommand()
   * @see setActionCommand()
   */
  String actionCommand;

  Color colMousePressed;

  public LWButton() { this(""); }

  public LWButton(String label) {
    this(label, Color.red, Color.green, Color.white);
  }

  /**
   * Initialize the LWButton, fully specifying all parameters.
   * @param label The string to display.
   * @param fgnd  The color to draw the label in.
   * @param bkgnd The color of the button itself.
   * @param mousePressed The Color of the MousePressed rectangle.
   */
  public LWButton(String label, Color fgnd, Color bkgnd, Color mousePressed) {
    super();
    this.label = label;
    setBackground(fgnd);
    setForeground(bkgnd);
    colMousePressed = mousePressed;
    setName(makeComponentName());

    enableEvents(  AWTEvent.MOUSE_EVENT_MASK
         | AWTEvent.KEY_EVENT_MASK
         | AWTEvent.ACTION_EVENT_MASK);
    setEnabled(true);
  }

  /**
   * Make the component flash briefly.
   */
  public void flash() {
    isInClick = true;
    repaint();

    class unClicker implements Runnable {
      @Override
      public void run() {
        try { Thread.sleep(100); } catch (InterruptedException ee) {}
        isInClick = false;
        repaint();
      }
    }
    try {
      unClicker uc = new unClicker();
      new Thread(uc).start();
    } catch (Exception e) {
      // In case we're in an applet and the security has not been
      // turned off (in which case we can't start a new thread)
      // we can catch that and set the flag back to how it should be.
      isInClick = false;
      repaint();
    }
  }

  /**
   * Set the MousePressed color (the color shown in the MousePressed rectangle
   * when the mouse is over the component).
   * @param c The color of the MousePressed rectangle.
   */
  public void setMousePressedColor(Color c) { colMousePressed = c; }

  /**
   * Get the MousePressed color.
   * @return The color of the MousePressed rectangle.
   */
  public Color getMousePressedColor() { return colMousePressed; }

  /**
   * Used to dispatch out the ActionEvent for a corresponding InputEvent.
   * @param e The InputEvent that is causing the ActionEvent dispatch.
   */
  private void sendActionEvent(InputEvent e) {

    int modifiers = e.getModifiers();
    int aModifiers = 0;

    if ((modifiers & MouseEvent.SHIFT_MASK) != 0) {
      aModifiers |= ActionEvent.SHIFT_MASK;
    }
    if ((modifiers & MouseEvent.CTRL_MASK) != 0) {
      aModifiers |= ActionEvent.CTRL_MASK;
    }
    if ((modifiers & MouseEvent.META_MASK) != 0) {
      aModifiers |= ActionEvent.META_MASK;
    }
    if ((modifiers & MouseEvent.ALT_MASK) != 0) {
      aModifiers |= ActionEvent.ALT_MASK;
    }

    ActionEvent ae = new ActionEvent(this,
                     ActionEvent.ACTION_PERFORMED,
                     actionCommand,
                     aModifiers);
    // XXX: What's the right way to send out the ActionEvent?
    //   My assumption was to put it into the system event queue
    //   and the it will be dispatched back into <i>processEvent</i>
    //   for us.  However this doesn't happen...?
    if (actionListener != null) {
      actionListener.actionPerformed(ae);
    }
    //Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(ae);
  }

  /**
   * Set whether the component is enabled ({@code true}) or not.
   * @param enabled If {@code true}, the component is to be enabled.
   */
  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);

    if (enabled) {
      enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    } else {
      disableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }
    repaint(1);
  }

  /**
   * Indicates that LWButton component can receive focus.
   * @return  {@code true} if the LWButton component can receive focus
   */
  @Override
  public boolean isFocusTraversable() { return true; }

  /**
   * Construct a name for this component. Called by getName() when the
   * name is null.
   */
  String makeComponentName() {
    synchronized (getClass()) {
      return base + nameCounter++;
    }
  }

  /**
   * Handle painting the enabled version of the component.
   *
   * ASSUMES: g.color may be changed
   */
  @Override
  public void paint(Graphics g) {

    super.paint(g);
    restrictGraphicsToClientArea(g);

    Dimension dim = getClientSize();

    int s = Math.min(dim.width - 1, dim.height - 1);

    if (isInClick) {
      g.setColor(Color.white);
    } else {
      g.setColor(getBackground());
    }

    // In jdk 1.2 (pre-release) there was a bug using clearRect
    // to paint the background of a lightweight.
    //g.clearRect(loc.x, loc.y, dim.width, dim.height);
    g.fillRect(0, 0, dim.width, dim.height);

    if (mouseB1Pressed) {
      g.setColor(colMousePressed);
      //LWComponent.traceMsg("paint mousePressed " + this.toString());
      g.drawRect(1, 1, dim.width - 3, dim.height - 3);
    }

    Font f = getFont();
    if (f != null) {
      FontMetrics fm = getFontMetrics(f);
      g.setColor(getForeground());
      g.drawString(label,
                   s/2 - fm.stringWidth(label)/2,
                   s/2 + fm.getMaxDescent());
    }

    unrestrictGraphicsFromClientArea(g);
  }

  @Override
  public Dimension getPreferredSize() {
    Font f = getFont();
    if (f != null) {
      FontMetrics fm = getFontMetrics(f);
      int max = Math.max(fm.stringWidth(label) + 40, fm.getHeight() + 40);
      return new Dimension(max, max);
    } else {
      return new Dimension(100, 100);
    }
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  /**
   * Get the text displayed in the LWButton.
   * @return  the text displayed in the LWButton
   */
  public String getText() { return label; }

  /**
   * Set the text displayed in the LWButton.
   * @param s The text to be displayed.
   */
  public void setText(String s) {
    Font f = getFont();
    int oWidth = 0;
    int oHeight = 0;
    int nWidth = 0;
    int nHeight = 0;
    int invalidated = 0;
    FontMetrics fm = null;

    if (f != null) {
      fm = getFontMetrics(f);
      oWidth = fm.stringWidth(label);
      oHeight = fm.getHeight();
    }

    this.label = s;

    if (f != null) {
      nWidth = fm.stringWidth(label);
      nHeight = fm.getHeight();

      if ((nWidth > oWidth) || (nHeight > oHeight)) {
        invalidate();
        invalidated = 1;
      }
    }

    if (invalidated == 0) {
      repaint();
    }
  }

  /**
   * Set the command name for the action event fired
   * by this button. By default this action command is
   * set to match the label of the button.
   * @param     command  A string used to set the button's
   *                     action command.
   *            If the string is <code>null</code> then the action command
   *            is set to match the label of the button.
   * @see       java.awt.event.ActionEvent
   * @since     JDK1.1
   */
  public void setActionCommand(String command) {
    actionCommand = command;
  }

  /**
   * Returns the command name of the action event fired by this button.
   * If the command name is {@code null} (default) then this method
   * returns the label of the button.
   *
   * @return the command name of the action event fired by this button
   *         or the label of the button (in case of {@code null})
   */
  public String getActionCommand() {
    return (actionCommand == null? label : actionCommand);
  }

  /**
   * Add the specified action listener to receive action events from
   * this button. Action events occur when a user presses or releases
   * the mouse over this button.
   * @param         l the action listener.
   * @see           java.awt.event.ActionListener
   * @see           #removeActionListener
   * @since         JDK1.1
   */
  public synchronized void addActionListener(ActionListener l) {
    actionListener = AWTEventMulticaster.add(actionListener, l);
    enableEvents(AWTEvent.MOUSE_EVENT_MASK);
  }

  /**
   * Remove the specified action listener so that it no longer
   * receives action events from this button. Action events occur
   * when a user presses or releases the mouse over this button.
   * @param         l     the action listener.
   * @see           java.awt.event.ActionListener
   * @see           #addActionListener
   * @since         JDK1.1
   */
  public synchronized void removeActionListener(ActionListener l) {
    actionListener = AWTEventMulticaster.remove(actionListener, l);
  }

  @Override
  protected void processKeyEvent(KeyEvent e) {
    super.processKeyEvent(e);
    if (!isEnabled()) { return; }
    switch(e.getID()) {
    case KeyEvent.KEY_TYPED:
      switch (e.getKeyCode()) {
        case KeyEvent.VK_ENTER:
        case KeyEvent.VK_SPACE:
          flash();
          sendActionEvent(e);
          break;
      }
      break;
    }
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    super.processMouseEvent(e);
    if (!isEnabled()) { return; }
    switch(e.getID()) {
    case MouseEvent.MOUSE_PRESSED:
      requestFocus();
      repaint();
      break;
    case MouseEvent.MOUSE_RELEASED:
      repaint();
      break;
    case MouseEvent.MOUSE_CLICKED:
      if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
        flash();
        sendActionEvent(e);
      }
      break;
    }
  }

  /**
   * Returns the parameter string representing the state of this
   * button. This string is useful for debugging.
   * @return     the parameter string of this button.
   */
  @Override
  protected String paramString() {
    return super.paramString() + ", label = " + label;
  }

}
