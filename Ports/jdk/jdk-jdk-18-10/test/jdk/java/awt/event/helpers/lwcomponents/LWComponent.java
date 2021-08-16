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

import java.io.*;
import java.awt.*;
import java.awt.event.*;

/**
 * This is experimental - The idea is to subclass all the LW components
 * from LWComponent to provide for some common capabilities.  The main
 * capability to be provided is the status rectangles as done for LWButton.
 * In particular the Focus and MouseOver rectangles are generically
 * useful, while other rectangles might be useful to other components.<p>
 *
 * To implement that, here is the idea ... borrowed from Win32 ... Each
 * of the LW components has both a client and non-client region.  We
 * call paintNC to paint the non-client region (Focus and MouseOver
 * rectangles), and the subclass might be permitted to implement paintNC
 * but for now they aren't.<p>
 *
 * Then the paint{Enabled,Disabled} methods are called as appropriate.
 * Note that paintDisabled is implemented in LWComponent to call paintEnabled
 * then stipple over the top of it.<p>
 *
 * So it is paintEnabled that the component should implement.  This method
 * needs to know the dimensions of the client area (getClientRegion?) and
 * the Graphics needs to have it's clip region set appropriately.<p>
 *
 * <b>KVETCHING</b>: <i>Kvetch</i> is a Yiddish word which means, basically,
 * to complain very precisely.  The LWComponent family tracks various pieces
 * of information over time that are used to check closely for correct behavior
 * in some circumstances.  The method <i>kvetch</i> is where this code lives
 * and is intended to check a broad range of conditions.<p>
 *
 * To turn off specific kvetch's, one simply specifies a System property
 * as in this table:<p>
 *
 * <table border="1">
 * <tr><th>Property name</th><th>Value</th><th>Discussion</th></tr>
 * <tr>
 *    <th>javasoft.awtsqe.lw.IGNORE_FOCUS_KVETCH</th>
 *    <th>true or false</th>
 *    <td>Specify whether the <i>hasFocus</i> kvetch is checked.</td>
 * </tr>
 * </table><p>
 *
 * <b>XXX To implement</b> - specifying colors.  NCBackground,
 * FocusRectColor, MouseOverColor are the threee colors.  paintNC
 * fills the NC region with NCBackground, and then pains the two
 * colors as appropriate.  There needs to be methods to get/specify
 * these colors.<p>
 *
 * <b>XXX To implement</b> - Specifying the component name and toString().
 * The subclass should only give the base class name, and a method
 * in LWComponent should construct a name from that.  For toString()
 * there needs to be a small amount of infrastructure built.<p>
 */

public abstract class LWComponent extends Component {

  protected static Color ncBackgroundColor;
  protected static Color focusColor;
  protected static Color focusWrongColor;
  protected static Color mouseOverColor;

  static {
    ncBackgroundColor = Color.white;
    focusColor        = Color.black;
    focusWrongColor   = Color.magenta;
    mouseOverColor    = Color.blue;
  }

  /**
   * Flag indicating whether our records indicate that the component
   * should have focus.
   */
  protected boolean _shouldHaveFocus = false;
  protected boolean _shouldBeShowing = false;

  protected boolean mouseB1Pressed = false;
  protected boolean mouseB2Pressed = false;
  protected boolean mouseB3Pressed = false;
  protected boolean mouseInside    = false;

  protected static boolean tracingOn = false;
  protected static PrintStream traceOutput = null;

  // Uncommenting these lines turns on tracing for the package.
  //  static {
  //    tracingOn = true;
  //    traceOutput = System.err;
  //  }

  public LWComponent() {
    enableEvents(AWTEvent.MOUSE_EVENT_MASK
         /*| AWTEvent.MOUSE_MOTION_EVENT_MASK*/
           | AWTEvent.FOCUS_EVENT_MASK
           | AWTEvent.COMPONENT_EVENT_MASK);
  }

  /**
   * Print out an error message.
   * @param msg  the message
   */
  public static void errorMsg(String msg) {
    System.err.println("ERROR: " + msg);
  }

  /**
   * Print out a tracing message
   * @param msg  the message
   */
  public static void traceMsg(String msg) {
    if (LWComponent.tracingOn) {
      LWComponent.traceOutput.println(msg);
    }
  }

  /////////////////////////////////////////////
  /////// FLAGS FOR IGNORING KVETCH's /////////
  /////////////////////////////////////////////

  static boolean bIgnFocus = false;

  static {
    // Initialize the kvetch ignoring flags here.
    String ignFocus = System.getProperty("javasoft.awtsqe.lw.IGNORE_FOCUS_KVETCH",
                                         "false");
    bIgnFocus = ignFocus.trim().toLowerCase().equals("true");
  }

  /**
   * Check the <i>shoulds</i> and return a string indicating which
   * do not match the components actual state.
   *
   * @return  the string indicating which do not match the components actual state
   */
  public String kvetch() {
    String ret = this.toString();
    boolean errors = false;

    if (!bIgnFocus) {
      if (hasFocus()) {
        if (!shouldHaveFocus()) {
          ret += "\nERROR: hasFocus indicates we have Focus, when we shouldn't.";
          errors = true;
        }
      } else {
        if (shouldHaveFocus()) {
          ret += "\nERROR: (see bug#4233658) hasFocus does not indicate we have Focus, when we should.";
          errors = true;
        }
      }
    }

    if (errors) {
      return ret;
    } else {
      return null;
    }
  }

  /**
   * Check the <i>shoulds</i> and return a string indicating which
   * do not match the components actual state.  Prints the output
   * to the given PrintStream.
   * @param out The PrintStream to print to.
   */
  public void kvetch(PrintStream out) {
    if (out != null) {
      String s = kvetch();
      if (s != null) {
        LWComponent.errorMsg(s);
      }
    }
  }

  /**
   * Turn on tracing for the LWComponent family.
   * @param out  the output stream
   */
  public static void startTracing(PrintStream out) {
    tracingOn = true;
    traceOutput = out;
  }

  /**
   * Turn off tracing for the LWComponent family.
   */
  public static void stopTracing() { tracingOn = false; traceOutput = null; }

  /**
   * Indicate whether it is believed the component should have focus.
   * @return {@code true} if the component should have focus
   */
  public boolean shouldHaveFocus() { return _shouldHaveFocus; }

  /**
   * Indicate whether it is believed the component should be showing.
   * @return  {@code true} if the component should be showing
   */
  public boolean shouldBeShowing() { return _shouldBeShowing; }

  @Override
  protected void processFocusEvent(FocusEvent e) {
    super.processFocusEvent(e);
    LWComponent.traceMsg("processFocusEvent " + e.toString());
    switch (e.getID()) {
    case FocusEvent.FOCUS_GAINED:
      _shouldHaveFocus = true;
      repaint();
      break;
    case FocusEvent.FOCUS_LOST:
      _shouldHaveFocus = false;
      repaint();
      break;
    }
  }

  @Override
  protected void processComponentEvent(ComponentEvent e) {
    super.processComponentEvent(e);
    LWComponent.traceMsg("processComponentEvent " + e.toString());
    switch (e.getID()) {
      case ComponentEvent.COMPONENT_MOVED:   break;
      case ComponentEvent.COMPONENT_RESIZED: break;
      case ComponentEvent.COMPONENT_SHOWN:   _shouldBeShowing = true;  break;
      case ComponentEvent.COMPONENT_HIDDEN:  _shouldBeShowing = false; break;
    }
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    int mod = e.getModifiers();
    super.processMouseEvent(e);
    LWComponent.traceMsg("processMouseEvent " + e.toString());
    switch (e.getID()) {
    case MouseEvent.MOUSE_PRESSED:
      if ((mod & MouseEvent.BUTTON1_MASK) != 0) {
        if (mouseB1Pressed) {
          errorMsg("ERROR: MOUSE_PRESSED for B1 when already pressed, on "
              + this.toString());
        }
        mouseB1Pressed = true;
        break;
      }
      if ((mod & MouseEvent.BUTTON2_MASK) != 0) {
        if (mouseB2Pressed) {
          errorMsg("ERROR: MOUSE_PRESSED for B2 when already pressed, on "
              + this.toString());
        }
        mouseB2Pressed = true;
        break;
      }
      if ((mod & MouseEvent.BUTTON3_MASK) != 0) {
        if (mouseB3Pressed) {
          errorMsg("ERROR: MOUSE_PRESSED for B3 when already pressed, on "
              + this.toString());
        }
        mouseB3Pressed = true;
        break;
      }
      repaint();
      break;
    case MouseEvent.MOUSE_RELEASED:
      if ((mod & MouseEvent.BUTTON1_MASK) != 0) {
        if (!mouseB1Pressed) {
          errorMsg("ERROR: MOUSE_RELEASED for B1 when not pressed, on "
              + this.toString());
        }
        mouseB1Pressed = false;
        break;
      }
      if ((mod & MouseEvent.BUTTON2_MASK) != 0) {
        if (!mouseB2Pressed) {
          errorMsg("ERROR: MOUSE_RELEASED for B2 when not pressed, on "
              + this.toString());
        }
        mouseB2Pressed = false;
        break;
      }
      if ((mod & MouseEvent.BUTTON3_MASK) != 0) {
        if (!mouseB3Pressed) {
          errorMsg("ERROR: MOUSE_RELEASED for B3 when not pressed, on "
              + this.toString());
        }
        mouseB3Pressed = false;
        break;
      }
      repaint();
      break;
    case MouseEvent.MOUSE_CLICKED:
      break;
    case MouseEvent.MOUSE_ENTERED:
      if (mouseInside) {
        errorMsg("ERROR: MOUSE_ENTERED when mouse already inside component, on "
            + this.toString());
      }
      mouseInside = true;
      repaint();
      break;
    case MouseEvent.MOUSE_EXITED:
      if (!mouseInside) {
        errorMsg("ERROR: MOUSE_EXITED when mouse not inside component, on "
            + this.toString());
      }
      mouseInside = false;
      repaint();
      break;
    case MouseEvent.MOUSE_MOVED:
      break;
    case MouseEvent.MOUSE_DRAGGED:
      break;
    }
  }

  public Point getClientLocation() {
    return new Point(5, 5);
  }

  public Dimension getClientSize() {
    Dimension dim = getSize();
    dim.width -= 10;
    dim.height -= 10;
    return dim;
  }

  public Rectangle getClientBounds() {
    Dimension dim = getClientSize();
    return new Rectangle(5, 5, dim.width, dim.height);
  }

  public int getClientX() { return 5; }
  public int getClientY() { return 5; }

  /**
   * Set the color used for painting the non-client area of the component.
   * The default for this is Color.white.
   *
   * @param c The new color to use.
   */
  public void setNonClientColor(Color c) {
    LWComponent.ncBackgroundColor = c;
  }

  /**
   * Handle painting for the component.
   */
  @Override
  public void paint(Graphics g) {
    Dimension dim = getSize();

    kvetch(System.err);

    Color saveColor = g.getColor();
    super.paint(g);

    // ------------------- Paint the background -----------------

    // In jdk 1.2 (pre-release) there was a bug using clearRect
    // to paint the background of a lightweight.
    //g.clearRect(0, 0, dim.width, dim.height);
    g.setColor(getBackground());
    g.fillRect(0, 0, dim.width, dim.height);

    // ------------------- Paint the non-client area ------------

    g.setColor(ncBackgroundColor);
    //         x              y                width      height
    g.fillRect(0,             0,               dim.width, 5);
    g.fillRect(0,             5,               5,         dim.height - 10);
    g.fillRect(dim.width - 5, 5,               5,         dim.height - 10);
    g.fillRect(0,             dim.height - 5,  dim.width, 5);

    if (shouldHaveFocus() || hasFocus()) {
      g.setColor(shouldHaveFocus() && hasFocus()
         ? focusColor
         : focusWrongColor);
      g.drawRect(1, 1, dim.width - 3, dim.height - 3);
    }

    if (mouseInside) {
      g.setColor(mouseOverColor);
      g.drawRect(3, 3, dim.width - 7, dim.height - 7);
    }

    // ------------------- Paint disabledness, if true -----------

    if (!isEnabled()) {
      g.setColor(getBackground());
      Dimension size = getSize();
      int borderThickness = 0;
      int startX = borderThickness;
      int startY = borderThickness;
      int endX = startX + size.width  - 2 * borderThickness - 2;
      int endY = startY + size.height - 2 * borderThickness - 2;
      int x, y;
      for (y = startY; y <= endY; y += 1) {
        for (x = startX + (y % 2); x <= endX; x += 2) {
          g.fillRect(x, y, 1, 1);
        } // x
      } // y
    }

    g.setColor(saveColor);
  }

  /**
   * Restricts the Graphics to be within the "client area" of the
   * component.  Recall that the LWComponent series of components has
   * a "non-client area" of 5 pixels wide in which it draws two
   * status rectangles showing mouse-over and has-focus status. <p>
   *
   * Child classes of LWComponent are to call {@code restrictGraphicsToClientArea}
   * at the beginning of their {@code paint} method, and then call
   * {@code unrestrictGraphicsFromClientArea} afterwards.<p>
   *
   * In order to make those paint methods as convenient as possible, these
   * two methods make it appear as if the Graphics available to the
   * component is slightly smaller than it really is, by the amount
   * used in the non-client area (5 pixel wide border).<p>
   *
   * @param g The Graphics to restrict.
   */
  public void restrictGraphicsToClientArea(Graphics g) {
    Dimension dim = getSize();
    g.translate(5, 5);
    g.setClip(0, 0, dim.width - 10, dim.height - 10);
  }

  /**
   * Undo the restriction done in restrictGraphicsToClientArea.
   *
   * @param g The Graphics to unrestrict.
   */
  public void unrestrictGraphicsFromClientArea(Graphics g) {
    g.translate(-5, -5);
    Dimension dim = getSize();
    g.setClip(0, 0, dim.width, dim.height);
  }

}
