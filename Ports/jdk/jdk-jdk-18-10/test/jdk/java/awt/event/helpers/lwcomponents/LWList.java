/*
 * Copyright (c) 1999, 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Vector;
import java.util.Enumeration;

/**
 * Remarks : Source for LightWeight component - List.
 *
 * Scroll bar support is not available for this component, so if the
 * items exceeds visibility those items will be truncated. Also, here
 * double buffering is not used so there will be little bit flickering
 * while it repaints. Item listener support is not enabled in this
 * component. Listeners handled were Mouse, Key and Focus.
 *
 * @author R.Govindarajan (govind@siptech.co.in), G.N.V.Sekhar (sekharv@siptech.co.in)
 */

public class LWList extends LWComponent implements ItemSelectable {

  // Constants used for component size
  private final int MIN_WIDTH   = 100;
  private final int MIN_HEIGHT  = 100;
  private final int PREF_WIDTH  = 100;
  private final int PREF_HEIGHT = 100;

  // Constants used for setting color for component
  private final Color BACK_COLOR          = Color.white;
  private final Color FRONT_COLOR         = Color.black;
  private final Color BORDER_COLOR        = Color.darkGray;
  private final Color FOCUS_COLOR         = Color.blue;
  private final Color FOCUS_FORECOLOR     = Color.white;
  private final Color FOCUS_ENABLED_COLOR = Color.red;
  private final int BORDER_WIDTH = 2;

  private Vector stringList;  // List of items
  private Vector selList;     // List of selected items
  private int rows;           // Visible rows
  private int focusIndex, prevfocusIndex;
  private Dimension minSize;
  private Dimension prefSize;
  private boolean pressed, eventOccurred, focusEnabled;
  private boolean multipleMode;

  // Listeners handled for this component
  private ActionListener actionListener;
  private KeyListener    keyListener;
  private FocusListener  focusListener;
  private ItemListener   itemListener;

  private static int nameCounter = 0;

  /**
   * Creates a new list.
   */
  public LWList() {
    this(0);
  }

  /**
   * Creates a new list with the specified number of rows;
   * multiple selection mode is disabled.
   *
   * @param i  the number of rows
   */
  public LWList(int i) {
    this(i, false);
  }

  /**
   * Creates a new list with the specified number of rows and multiple selection mode.
   *
   * @param rows  the number of rows
   * @param flag  determines whether the list allows multiple selections
   */
  public LWList(int rows, boolean flag) {
    multipleMode        = flag;
    this.rows           = rows;
    minSize             = new Dimension(MIN_WIDTH, MIN_HEIGHT);
    prefSize            = new Dimension(PREF_WIDTH, PREF_HEIGHT);
    stringList          = new Vector();
    selList             = new Vector();
    selList.addElement(0);
    focusIndex          = -1;
    prevfocusIndex      = focusIndex;
    enableEvents(AWTEvent.MOUSE_EVENT_MASK);
    enableEvents(AWTEvent.KEY_EVENT_MASK);
    enableEvents(AWTEvent.FOCUS_EVENT_MASK);
    enableEvents(AWTEvent.ITEM_EVENT_MASK);
    setName(makeComponentName()); // set the name to the component
  }

  String makeComponentName() {
    String s = "LWList" + nameCounter++;
    return s;
  }

  /**
   * Set whether the component is enabled or not.
   * @param enabled  if {@code true}, the component is to be enabled
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
   * Set the selection mode.
   *
   * @param flag  determines whether the list allows multiple selections
   */
  public void setSelectionMode(boolean flag) {
    multipleMode = flag;
  }

  /**
   * Check if the list allows multiple selections.
   *
   * @return  {@code true} if the list allows multiple selections
   */
  public boolean isMultipleMode() {
    return multipleMode;
  }

  /**
   * Add the specified item.
   *
   * @param listItem  the item
   */
  public void add(String listItem) {
    stringList.addElement(listItem);
    invalidate();
    repaint();
  }

  /**
   * Get minimum dimension for the list.
   *
   * @return  the minimum dimensions for displaying
   */
  @Override
  public Dimension getMinimumSize() {
    return minSize;
  }

  /**
   * Get the preferred size of the list.
   *
   * @return  the preferred dimensions for displaying
   */
  @Override
  public Dimension getPreferredSize() {
    return prefSize;
  }

  /**
   * Get the background color for the component.
   *
   * @return  the background color for the component
   */
  @Override
  public Color getBackground() {
    return BACK_COLOR;
  }

  /**
   * Get the foreground color for the component.
   *
   * @return  the foreground color for the component
   */
  @Override
  public Color getForeground() {
    return FRONT_COLOR;
  }

  /**
   * Get the border color for the component.
   *
   * @return  the border color for the component
   */
  public Color getBorder() {
    return BORDER_COLOR;
  }

  /**
   * Get background color for the selected item.
   *
   * @return  the color for the selected item
   */
  public Color getFocusColor() {
    return FOCUS_COLOR;
  }

  /**
   * Get foreground color for the selected item.
   *
   * @return  the foreground color for the selected item
   */
  public Color getFocusForeColor() {
    return FOCUS_FORECOLOR;
  }

  /**
   * Get a "focus enabled" color - a small rectangle around the item
   * should be drawn when the component got the focus.
   *
   * @return  the "focus enabled" color
   */
  public Color getFocusEnabledColor() {
    return FOCUS_ENABLED_COLOR;
  }

  /**
   * Get border width.
   *
   * @return  the border width
   */
  public int getBorderWidth() {
    return BORDER_WIDTH;
  }

  /**
   * Get the list item count.
   *
   * @return  the count of items
   */
  public int getItemCount() {
    return stringList.size();
  }

  /**
   * Get the specified item from the list.
   *
   * @param index  the index
   * @return  the item string
   */
  public String getItem(int index) {
    return (String)stringList.elementAt(index);
  }

  /**
   * Get array of items from the list.
   *
   * @return  the array of item strings
   */
  public String[] getItems() {
    String str[] = new String[getItemCount()];
    int count = 0;
    for (Enumeration e = stringList.elements(); e.hasMoreElements(); ) {
      str[count++] = (String)e.nextElement();
    }
    return str;
  }

  /**
   * Check whether the component can be a focus owner (explicitly enabled here).
   *
   * @return {@code true} if the component is focusable
   */
  @Override
  public boolean isFocusTraversable() {
    return true;
  }

  /**
   * Check whether mouse click point lies within the list of items.
   *
   * @param pt  the click point
   * @return  {@code true} if the click point lies within the list of items
   */
  @Override
  public boolean contains(Point pt) {
    Rectangle rect = new Rectangle();
    Dimension d = getSize();
    rect.x = getBorderWidth();
    rect.y = getBorderWidth();
    rect.width  = d.width  - (getBorderWidth() * 2);
    rect.height = d.height - (getBorderWidth() * 2);
    return rect.contains(pt);
  }

  /**
   * Given a click point the item that has to be selected is found from the list
   * and focusIndex variable is set accordingly.
   *
   * @param pt  the click point
   */
  private void findSelectedIndex(Point pt) {
    Font f = getFont();
    FontMetrics fm = getFontMetrics(f);
    focusIndex = pt.y / fm.getHeight() - 1;
    if (multipleMode) {
      Integer fi = focusIndex;
      if (selList.contains(fi)) {
        int i = selList.indexOf(fi);
        selList.removeElementAt(i);
      } else {
        selList.addElement(fi);
      }
    }
  }

  /**
   * Set index of the selected item.
   *
   * @param index  the index
   */
  public void setSelectedIndex(int index) {
    prevfocusIndex = focusIndex;
    focusIndex = index;
  }

  /**
   * Get the selected item index.
   *
   * @return  the selected item index.
   */
  public int getSelectedIndex() {
    return focusIndex;
  }

  /**
   * Get an array of the selected Objects.
   *
   * @return  array of the Objects
   */
  @Override
  public Object[] getSelectedObjects() {
    int ai[] = getSelectedIndexes();
    Object aobj[] = new Object[selList.size()];
    for (int i = 0; i < selList.size(); i++) {
      aobj[i] = stringList.elementAt(ai[i]);
    }
    return aobj;
  }

  /**
   * Get an array of the selected item indices.
   *
   * @return  the array of the indices
   */
  public int[] getSelectedIndexes() {
    int ai[] = new int[selList.size()];
    for (int i = 0; i < selList.size(); i++) {
      ai[i] = ((Integer)selList.elementAt(i));
    }
    return ai;
  }

  /**
   * Add the specified item listener to receive item events from the list.
   *
   * @param itemlistener  the item listener
   */
  @Override
  public synchronized void addItemListener(ItemListener itemlistener) {
    itemListener = AWTEventMulticaster.add(itemListener, itemlistener);
    enableEvents(AWTEvent.ITEM_EVENT_MASK);
  }

  /**
   * Remove the specified item listener so
   * that it no longer receives item events from this list.
   *
   * @param itemlistener  the item listener
   */
  @Override
  public synchronized void removeItemListener(ItemListener itemlistener) {
    itemListener = AWTEventMulticaster.remove(itemListener, itemlistener);
  }

  /**
   * Add the specified action listener to receive action events from this list.
   *
   * @param listener  the action listener
   */
  public synchronized void addActionListener(ActionListener listener) {
    actionListener = AWTEventMulticaster.add(actionListener, listener);
    enableEvents(AWTEvent.MOUSE_EVENT_MASK);
  }

  /**
   * Remove the specified action listener so
   * that it no longer receives action events from this list.
   *
   * @param listener  the action listener
   */
  public synchronized void removeActionListener(ActionListener listener) {
    actionListener = AWTEventMulticaster.remove(actionListener, listener);
  }

  /**
   * Add the specified key listener to receive key events from this component.
   *
   * @param listener  the key listener
   */
  @Override
  public synchronized void addKeyListener(KeyListener listener) {
    keyListener = AWTEventMulticaster.add(keyListener, listener);
    enableEvents(AWTEvent.KEY_EVENT_MASK);
  }

  /**
   * Remove the specified key listener so
   * that it no longer receives key events from this component.
   *
   * @param listener  the key listener
   */
  @Override
  public synchronized void removeKeyListener(KeyListener listener) {
    keyListener = AWTEventMulticaster.remove(keyListener, listener);
  }

  /**
   * Add the specified focus listener to receive focus events
   * from this component when it gains input focus.
   *
   * @param listener  the focus listener
   */
  @Override
  public synchronized void addFocusListener(FocusListener listener) {
    focusListener = AWTEventMulticaster.add(focusListener, listener);
    enableEvents(AWTEvent.FOCUS_EVENT_MASK);
  }

  /**
   * Remove the specified focus listener so
   * that it no longer receives focus events from this component.
   *
   * @param listener  the focus listener
   */
  @Override
  public synchronized void removeFocusListener(FocusListener listener) {
    focusListener = AWTEventMulticaster.remove(focusListener, listener);
  }

  @Override
  protected void processEvent(AWTEvent awtevent) {

    if (awtevent instanceof FocusEvent) {
      processFocusEvent((FocusEvent)awtevent);
    } else if (awtevent instanceof ItemEvent) {
      processItemEvent((ItemEvent)awtevent);
    } else if (awtevent instanceof KeyEvent) {
      processKeyEvent((KeyEvent)awtevent);
    } else if (awtevent instanceof MouseEvent) {
      switch (awtevent.getID()) {
      case MouseEvent.MOUSE_CLICKED:
      case MouseEvent.MOUSE_PRESSED:
      case MouseEvent.MOUSE_RELEASED:
      case MouseEvent.MOUSE_ENTERED:
      case MouseEvent.MOUSE_EXITED:
    processMouseEvent((MouseEvent)awtevent);
    break;

      case MouseEvent.MOUSE_MOVED:
      case MouseEvent.MOUSE_DRAGGED:
    super.processEvent((MouseEvent)awtevent);
    break;
      }
    } else {
      if (awtevent instanceof ComponentEvent)
    super.processComponentEvent((ComponentEvent)awtevent);
      else
    super.processEvent(awtevent);
    }
  }

  protected void processItemEvent(ItemEvent itemevent) {
    if (itemListener != null) {
      itemListener.itemStateChanged(itemevent);
    }
  }

  @Override
  protected void processFocusEvent(FocusEvent e) {
    switch (e.getID()) {
    case FocusEvent.FOCUS_GAINED:
      if (focusListener != null) { focusListener.focusGained(e); }
      if (getSelectedIndex() == -1) { setSelectedIndex(0); }
      focusEnabled = true;
      repaint();
      break;
    case FocusEvent.FOCUS_LOST:
      if (focusListener != null) {
        focusListener.focusLost(e);
      }
      focusEnabled = false;
      repaint();
      break;
    }
    super.processFocusEvent(e);
  }

  @Override
  protected void processKeyEvent(KeyEvent e) {
    rows = getItemCount();

    switch (e.getID()) {

    case KeyEvent.KEY_TYPED:
      if (keyListener != null) {
        keyListener.keyTyped(e);
      }
      break;

    case KeyEvent.KEY_PRESSED:
      if (keyListener != null) {
        keyListener.keyPressed(e);
      }
      if (e.getKeyCode() == KeyEvent.VK_DOWN) {
        prevfocusIndex = focusIndex;
        int index = getSelectedIndex() + 1;
        if (index > rows) { break; }
        setSelectedIndex(index);
        processItemEvent(new ItemEvent(this, 0, index, 0));
        eventOccurred = true;
        repaint();
      } else if (e.getKeyCode() == KeyEvent.VK_UP) {
        int index = getSelectedIndex()-1;
        if (index >= 0) {
          setSelectedIndex(index);
          if (e.getID() != 400) {
            processItemEvent(new ItemEvent(this, 0, index, 0));
          }
          eventOccurred = true;
          repaint();
        }
      }
      break;

    case KeyEvent.KEY_RELEASED:
      if (keyListener != null) {
        keyListener.keyReleased(e);
      }
      if (e.getKeyCode() == KeyEvent.VK_ENTER) {
        eventOccurred = true;

        // ActionEvent is fired here
        if (actionListener != null) {
          actionListener.actionPerformed( new ActionEvent(
              this, ActionEvent.ACTION_PERFORMED, null));
        }
        repaint();
      }
      break;
    } // switch
    super.processKeyEvent(e);
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    switch (e.getID()) {
    case MouseEvent.MOUSE_PRESSED:
      pressed = true;
      if (contains(e.getPoint())) {
        findSelectedIndex(e.getPoint());
        processItemEvent(new ItemEvent(this, 0, focusIndex, 0));
        eventOccurred = true;
      }
      repaint();
      break;

    case MouseEvent.MOUSE_RELEASED:
      if (pressed) { requestFocus(); }

      if (contains(e.getPoint())) {
        findSelectedIndex(e.getPoint());
        eventOccurred = true;
      }
      // ActionEvent is fired here
      if (actionListener != null) {
        actionListener.actionPerformed(new ActionEvent(
            this, ActionEvent.ACTION_PERFORMED, null));
      }

      if (pressed) {
        pressed = false;
        repaint();
      }
      break;
    }
    super.processMouseEvent(e);
  }

  @Override
  /**
   * Paint the list.
   *
   * @param g  the graphics context to be used for testing
   */
  public void paint(Graphics g) {
    super.paint(g);
    restrictGraphicsToClientArea(g);

    Point     loc = getClientLocation();
    Dimension dim = getClientSize();
    Color prevColor = g.getColor();

    // List border is drawn here
    g.setColor(getBackground());
    g.fillRect(0, 0, dim.width - 2, dim.height - 2);
    g.setColor(getBorder());
    g.drawRect(0, 0, dim.width - 2, dim.height - 2);

    if (getItemCount() > 0) {
      Font f = getFont();
      if (f != null) {
        String str[] = getItems();
        FontMetrics fm = getFontMetrics(f);
        int drawRow = loc.x + getBorderWidth() + fm.getAscent();
        int drawCol = loc.y + getBorderWidth();
        int rectRow = loc.y + getBorderWidth();
        int i = 0;

        // Draw items (if the items exceeds visibility those items will be truncated
        // as scrollbar support is not enabled

        for (;
             i < str.length && drawRow < (dim.height - getBorderWidth());
             i++) {
               if (fm.stringWidth(str[i]) < (dim.width - (getBorderWidth() * 2))) {
                 drawItem(g, i, drawCol, drawRow, rectRow, fm);
                 drawRow += fm.getHeight();
                 rectRow += fm.getHeight();
               } else {
                 LWComponent.errorMsg("string width exceeds list width");
                 LWComponent.errorMsg("Horizontal scrollbar support is not available");
               }
            } // for

        if ( (drawRow > (dim.height - getBorderWidth())) && (str.length > i) ) {
          //LWComponent.errorMsg("no of strings exceeds list height");
          //LWComponent.errorMsg("Vertical scrollbar support is not available");
        }
      } else { LWComponent.errorMsg("Font not available.."); }
    }

    eventOccurred = false;
    g.setColor(prevColor);
    unrestrictGraphicsFromClientArea(g);
  }

  // Draw String items
  private void drawItem(Graphics g, int listIndex, int drawCol,
      int drawRow, int rectRow, FontMetrics fm) {
    Point     loc = getClientLocation();
    Dimension dim = getClientSize();
    String    str = getItem(listIndex);
    if (multipleMode) {
      for (int i1 = 0; i1 < selList.size(); i1++) {
        if (listIndex == ((Integer)selList.elementAt(i1))) {
          g.setColor(getFocusColor());
          g.fillRect(loc.x + getBorderWidth(),
                     rectRow,
                     dim.width - getBorderWidth() * 2,
                     fm.getHeight());
          g.setColor(getFocusEnabledColor());
          g.drawRect(loc.x + getBorderWidth(),
                     rectRow,
                     dim.width - getBorderWidth() * 2,
                     fm.getHeight());
        }
      } // for
    } else {
      if (listIndex == getSelectedIndex() && !multipleMode) {
        g.setColor(getFocusColor());
        g.fillRect(loc.x + getBorderWidth(),
                   rectRow,
                   dim.width - getBorderWidth() * 2,
                   fm.getHeight());
        g.setColor(getFocusForeColor());
      }
      if ((listIndex == prevfocusIndex) && (prevfocusIndex != getSelectedIndex()) && !multipleMode) {
        g.setColor(getBackground());
        g.fillRect(loc.x + getBorderWidth(),
                   rectRow,
                   dim.width - getBorderWidth() * 2,
                   fm.getHeight());
        prevfocusIndex = getSelectedIndex();
      }
      if (focusEnabled && listIndex == getSelectedIndex() && !multipleMode) {
        g.setColor(getFocusEnabledColor());
        g.drawRect(loc.x + getBorderWidth(),
                   rectRow,
                   dim.width - getBorderWidth() * 2,
                   fm.getHeight());
      }
    }
    g.setColor(getForeground());
    g.drawString(str,drawCol,drawRow);
  }

}

