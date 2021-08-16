/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.event;

import java.awt.AWTEvent;
import javax.swing.JInternalFrame;

/**
 * An <code>AWTEvent</code> that adds support for
 * <code>JInternalFrame</code> objects as the event source.  This class has the
 * same event types as <code>WindowEvent</code>,
 * although different IDs are used.
 * Help on handling internal frame events
 * is in
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/events/internalframelistener.html" target="_top">How to Write an Internal Frame Listener</a>,
 * a section in <em>The Java Tutorial</em>.
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
 * @see java.awt.event.WindowEvent
 * @see java.awt.event.WindowListener
 * @see JInternalFrame
 * @see InternalFrameListener
 *
 * @author Thomas Ball
 */
@SuppressWarnings("serial") // Same-version serialization only
public class InternalFrameEvent extends AWTEvent {

    /**
     * The first number in the range of IDs used for internal frame events.
     */
    public static final int INTERNAL_FRAME_FIRST        = 25549;

    /**
     * The last number in the range of IDs used for internal frame events.
     */
    public static final int INTERNAL_FRAME_LAST         = 25555;

    /**
     * The "window opened" event.  This event is delivered only
     * the first time the internal frame is made visible.
     *
     * @see JInternalFrame#show
     */
    public static final int INTERNAL_FRAME_OPENED       = INTERNAL_FRAME_FIRST;

    /**
     * The "window is closing" event. This event is delivered when
     * the user attempts to close the internal frame, such as by
     * clicking the internal frame's close button,
     * or when a program attempts to close the internal frame
     * by invoking the <code>setClosed</code> method.
     *
     * @see JInternalFrame#setDefaultCloseOperation
     * @see JInternalFrame#doDefaultCloseAction
     * @see JInternalFrame#setClosed
     */
    public static final int INTERNAL_FRAME_CLOSING      = 1 + INTERNAL_FRAME_FIRST;

    /**
     * The "window closed" event. This event is delivered after
     * the internal frame has been closed as the result of a call to
     * the <code>setClosed</code> or
     * <code>dispose</code> method.
     *
     * @see JInternalFrame#setClosed
     * @see JInternalFrame#dispose
     */
    public static final int INTERNAL_FRAME_CLOSED       = 2 + INTERNAL_FRAME_FIRST;

    /**
     * The "window iconified" event.
     * This event indicates that the internal frame
     * was shrunk down to a small icon.
     *
     * @see JInternalFrame#setIcon
     */
    public static final int INTERNAL_FRAME_ICONIFIED    = 3 + INTERNAL_FRAME_FIRST;

    /**
     * The "window deiconified" event type. This event indicates that the
     * internal frame has been restored to its normal size.
     *
     * @see JInternalFrame#setIcon
     */
    public static final int INTERNAL_FRAME_DEICONIFIED  = 4 + INTERNAL_FRAME_FIRST;

    /**
     * The "window activated" event type. This event indicates that keystrokes
     * and mouse clicks are directed towards this internal frame.
     *
     * @see JInternalFrame#show
     * @see JInternalFrame#setSelected
     */
    public static final int INTERNAL_FRAME_ACTIVATED    = 5 + INTERNAL_FRAME_FIRST;

    /**
     * The "window deactivated" event type. This event indicates that keystrokes
     * and mouse clicks are no longer directed to the internal frame.
     *
     * @see JInternalFrame#setSelected
     */
    public static final int INTERNAL_FRAME_DEACTIVATED  = 6 + INTERNAL_FRAME_FIRST;

    /**
     * Constructs an <code>InternalFrameEvent</code> object.
     * @param source the <code>JInternalFrame</code> object that originated the event
     * @param id     an integer indicating the type of event
     */
    public InternalFrameEvent(JInternalFrame source, int id) {
        super(source, id);
    }

    /**
     * Returns a parameter string identifying this event.
     * This method is useful for event logging and for debugging.
     *
     * @return a string identifying the event and its attributes
     */
    public String paramString() {
        String typeStr;
        switch(id) {
          case INTERNAL_FRAME_OPENED:
              typeStr = "INTERNAL_FRAME_OPENED";
              break;
          case INTERNAL_FRAME_CLOSING:
              typeStr = "INTERNAL_FRAME_CLOSING";
              break;
          case INTERNAL_FRAME_CLOSED:
              typeStr = "INTERNAL_FRAME_CLOSED";
              break;
          case INTERNAL_FRAME_ICONIFIED:
              typeStr = "INTERNAL_FRAME_ICONIFIED";
              break;
          case INTERNAL_FRAME_DEICONIFIED:
              typeStr = "INTERNAL_FRAME_DEICONIFIED";
              break;
          case INTERNAL_FRAME_ACTIVATED:
              typeStr = "INTERNAL_FRAME_ACTIVATED";
              break;
          case INTERNAL_FRAME_DEACTIVATED:
              typeStr = "INTERNAL_FRAME_DEACTIVATED";
              break;
          default:
              typeStr = "unknown type";
        }
        return typeStr;
    }


    /**
     * Returns the originator of the event.
     *
     * @return the <code>JInternalFrame</code> object that originated the event
     * @since 1.3
     */

    public JInternalFrame getInternalFrame () {
      return (source instanceof JInternalFrame)? (JInternalFrame)source : null;
    }


}
