/*
 * Copyright (c) 1996, 2015, Oracle and/or its affiliates. All rights reserved.
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
package java.awt;

import java.awt.event.*;
import java.lang.reflect.Array;
import java.util.EventListener;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.EventListener;


/**
 * {@code AWTEventMulticaster} implements efficient and thread-safe multi-cast
 * event dispatching for the AWT events defined in the {@code java.awt.event}
 * package.
 * <p>
 * The following example illustrates how to use this class:
 *
 * <pre><code>
 * public myComponent extends Component {
 *     ActionListener actionListener = null;
 *
 *     public synchronized void addActionListener(ActionListener l) {
 *         actionListener = AWTEventMulticaster.add(actionListener, l);
 *     }
 *     public synchronized void removeActionListener(ActionListener l) {
 *         actionListener = AWTEventMulticaster.remove(actionListener, l);
 *     }
 *     public void processEvent(AWTEvent e) {
 *         // when event occurs which causes "action" semantic
 *         ActionListener listener = actionListener;
 *         if (listener != null) {
 *             listener.actionPerformed(new ActionEvent());
 *         }
 *     }
 * }
 * </code></pre>
 * The important point to note is the first argument to the {@code
 * add} and {@code remove} methods is the field maintaining the
 * listeners. In addition you must assign the result of the {@code add}
 * and {@code remove} methods to the field maintaining the listeners.
 * <p>
 * {@code AWTEventMulticaster} is implemented as a pair of {@code
 * EventListeners} that are set at construction time. {@code
 * AWTEventMulticaster} is immutable. The {@code add} and {@code
 * remove} methods do not alter {@code AWTEventMulticaster} in
 * anyway. If necessary, a new {@code AWTEventMulticaster} is
 * created. In this way it is safe to add and remove listeners during
 * the process of an event dispatching.  However, event listeners
 * added during the process of an event dispatch operation are not
 * notified of the event currently being dispatched.
 * <p>
 * All of the {@code add} methods allow {@code null} arguments. If the
 * first argument is {@code null}, the second argument is returned. If
 * the first argument is not {@code null} and the second argument is
 * {@code null}, the first argument is returned. If both arguments are
 * {@code non-null}, a new {@code AWTEventMulticaster} is created using
 * the two arguments and returned.
 * <p>
 * For the {@code remove} methods that take two arguments, the following is
 * returned:
 * <ul>
 *   <li>{@code null}, if the first argument is {@code null}, or
 *       the arguments are equal, by way of {@code ==}.
 *   <li>the first argument, if the first argument is not an instance of
 *       {@code AWTEventMulticaster}.
 *   <li>result of invoking {@code remove(EventListener)} on the
 *       first argument, supplying the second argument to the
 *       {@code remove(EventListener)} method.
 * </ul>
 * <p>Swing makes use of
 * {@link javax.swing.event.EventListenerList EventListenerList} for
 * similar logic. Refer to it for details.
 *
 * @see javax.swing.event.EventListenerList
 *
 * @author      John Rose
 * @author      Amy Fowler
 * @since       1.1
 */

public class AWTEventMulticaster implements
    ComponentListener, ContainerListener, FocusListener, KeyListener,
    MouseListener, MouseMotionListener, WindowListener, WindowFocusListener,
    WindowStateListener, ActionListener, ItemListener, AdjustmentListener,
    TextListener, InputMethodListener, HierarchyListener,
    HierarchyBoundsListener, MouseWheelListener {

    /**
     * A variable in the event chain (listener-a)
     */
    protected final EventListener a;

    /**
     * A variable in the event chain (listener-b)
     */
    protected final EventListener b;

    /**
     * Creates an event multicaster instance which chains listener-a
     * with listener-b. Input parameters {@code a} and {@code b}
     * should not be {@code null}, though implementations may vary in
     * choosing whether or not to throw {@code NullPointerException}
     * in that case.
     * @param a listener-a
     * @param b listener-b
     */
    protected AWTEventMulticaster(EventListener a, EventListener b) {
        this.a = a; this.b = b;
    }

    /**
     * Removes a listener from this multicaster.
     * <p>
     * The returned multicaster contains all the listeners in this
     * multicaster with the exception of all occurrences of {@code oldl}.
     * If the resulting multicaster contains only one regular listener
     * the regular listener may be returned.  If the resulting multicaster
     * is empty, then {@code null} may be returned instead.
     * <p>
     * No exception is thrown if {@code oldl} is {@code null}.
     *
     * @param oldl the listener to be removed
     * @return resulting listener
     */
    protected EventListener remove(EventListener oldl) {
        if (oldl == a)  return b;
        if (oldl == b)  return a;
        EventListener a2 = removeInternal(a, oldl);
        EventListener b2 = removeInternal(b, oldl);
        if (a2 == a && b2 == b) {
            return this;        // it's not here
        }
        return addInternal(a2, b2);
    }

    /**
     * Handles the componentResized event by invoking the
     * componentResized methods on listener-a and listener-b.
     * @param e the component event
     */
    public void componentResized(ComponentEvent e) {
        ((ComponentListener)a).componentResized(e);
        ((ComponentListener)b).componentResized(e);
    }

    /**
     * Handles the componentMoved event by invoking the
     * componentMoved methods on listener-a and listener-b.
     * @param e the component event
     */
    public void componentMoved(ComponentEvent e) {
        ((ComponentListener)a).componentMoved(e);
        ((ComponentListener)b).componentMoved(e);
    }

    /**
     * Handles the componentShown event by invoking the
     * componentShown methods on listener-a and listener-b.
     * @param e the component event
     */
    public void componentShown(ComponentEvent e) {
        ((ComponentListener)a).componentShown(e);
        ((ComponentListener)b).componentShown(e);
    }

    /**
     * Handles the componentHidden event by invoking the
     * componentHidden methods on listener-a and listener-b.
     * @param e the component event
     */
    public void componentHidden(ComponentEvent e) {
        ((ComponentListener)a).componentHidden(e);
        ((ComponentListener)b).componentHidden(e);
    }

    /**
     * Handles the componentAdded container event by invoking the
     * componentAdded methods on listener-a and listener-b.
     * @param e the component event
     */
    public void componentAdded(ContainerEvent e) {
        ((ContainerListener)a).componentAdded(e);
        ((ContainerListener)b).componentAdded(e);
    }

    /**
     * Handles the componentRemoved container event by invoking the
     * componentRemoved methods on listener-a and listener-b.
     * @param e the component event
     */
    public void componentRemoved(ContainerEvent e) {
        ((ContainerListener)a).componentRemoved(e);
        ((ContainerListener)b).componentRemoved(e);
    }

    /**
     * Handles the focusGained event by invoking the
     * focusGained methods on listener-a and listener-b.
     * @param e the focus event
     */
    public void focusGained(FocusEvent e) {
        ((FocusListener)a).focusGained(e);
        ((FocusListener)b).focusGained(e);
    }

    /**
     * Handles the focusLost event by invoking the
     * focusLost methods on listener-a and listener-b.
     * @param e the focus event
     */
    public void focusLost(FocusEvent e) {
        ((FocusListener)a).focusLost(e);
        ((FocusListener)b).focusLost(e);
    }

    /**
     * Handles the keyTyped event by invoking the
     * keyTyped methods on listener-a and listener-b.
     * @param e the key event
     */
    public void keyTyped(KeyEvent e) {
        ((KeyListener)a).keyTyped(e);
        ((KeyListener)b).keyTyped(e);
    }

    /**
     * Handles the keyPressed event by invoking the
     * keyPressed methods on listener-a and listener-b.
     * @param e the key event
     */
    public void keyPressed(KeyEvent e) {
        ((KeyListener)a).keyPressed(e);
        ((KeyListener)b).keyPressed(e);
    }

    /**
     * Handles the keyReleased event by invoking the
     * keyReleased methods on listener-a and listener-b.
     * @param e the key event
     */
    public void keyReleased(KeyEvent e) {
        ((KeyListener)a).keyReleased(e);
        ((KeyListener)b).keyReleased(e);
    }

    /**
     * Handles the mouseClicked event by invoking the
     * mouseClicked methods on listener-a and listener-b.
     * @param e the mouse event
     */
    public void mouseClicked(MouseEvent e) {
        ((MouseListener)a).mouseClicked(e);
        ((MouseListener)b).mouseClicked(e);
    }

    /**
     * Handles the mousePressed event by invoking the
     * mousePressed methods on listener-a and listener-b.
     * @param e the mouse event
     */
    public void mousePressed(MouseEvent e) {
        ((MouseListener)a).mousePressed(e);
        ((MouseListener)b).mousePressed(e);
    }

    /**
     * Handles the mouseReleased event by invoking the
     * mouseReleased methods on listener-a and listener-b.
     * @param e the mouse event
     */
    public void mouseReleased(MouseEvent e) {
        ((MouseListener)a).mouseReleased(e);
        ((MouseListener)b).mouseReleased(e);
    }

    /**
     * Handles the mouseEntered event by invoking the
     * mouseEntered methods on listener-a and listener-b.
     * @param e the mouse event
     */
    public void mouseEntered(MouseEvent e) {
        ((MouseListener)a).mouseEntered(e);
        ((MouseListener)b).mouseEntered(e);
    }

    /**
     * Handles the mouseExited event by invoking the
     * mouseExited methods on listener-a and listener-b.
     * @param e the mouse event
     */
    public void mouseExited(MouseEvent e) {
        ((MouseListener)a).mouseExited(e);
        ((MouseListener)b).mouseExited(e);
    }

    /**
     * Handles the mouseDragged event by invoking the
     * mouseDragged methods on listener-a and listener-b.
     * @param e the mouse event
     */
    public void mouseDragged(MouseEvent e) {
        ((MouseMotionListener)a).mouseDragged(e);
        ((MouseMotionListener)b).mouseDragged(e);
    }

    /**
     * Handles the mouseMoved event by invoking the
     * mouseMoved methods on listener-a and listener-b.
     * @param e the mouse event
     */
    public void mouseMoved(MouseEvent e) {
        ((MouseMotionListener)a).mouseMoved(e);
        ((MouseMotionListener)b).mouseMoved(e);
    }

    /**
     * Handles the windowOpened event by invoking the
     * windowOpened methods on listener-a and listener-b.
     * @param e the window event
     */
    public void windowOpened(WindowEvent e) {
        ((WindowListener)a).windowOpened(e);
        ((WindowListener)b).windowOpened(e);
    }

    /**
     * Handles the windowClosing event by invoking the
     * windowClosing methods on listener-a and listener-b.
     * @param e the window event
     */
    public void windowClosing(WindowEvent e) {
        ((WindowListener)a).windowClosing(e);
        ((WindowListener)b).windowClosing(e);
    }

    /**
     * Handles the windowClosed event by invoking the
     * windowClosed methods on listener-a and listener-b.
     * @param e the window event
     */
    public void windowClosed(WindowEvent e) {
        ((WindowListener)a).windowClosed(e);
        ((WindowListener)b).windowClosed(e);
    }

    /**
     * Handles the windowIconified event by invoking the
     * windowIconified methods on listener-a and listener-b.
     * @param e the window event
     */
    public void windowIconified(WindowEvent e) {
        ((WindowListener)a).windowIconified(e);
        ((WindowListener)b).windowIconified(e);
    }

    /**
     * Handles the windowDeiconified event by invoking the
     * windowDeiconified methods on listener-a and listener-b.
     * @param e the window event
     */
    public void windowDeiconified(WindowEvent e) {
        ((WindowListener)a).windowDeiconified(e);
        ((WindowListener)b).windowDeiconified(e);
    }

    /**
     * Handles the windowActivated event by invoking the
     * windowActivated methods on listener-a and listener-b.
     * @param e the window event
     */
    public void windowActivated(WindowEvent e) {
        ((WindowListener)a).windowActivated(e);
        ((WindowListener)b).windowActivated(e);
    }

    /**
     * Handles the windowDeactivated event by invoking the
     * windowDeactivated methods on listener-a and listener-b.
     * @param e the window event
     */
    public void windowDeactivated(WindowEvent e) {
        ((WindowListener)a).windowDeactivated(e);
        ((WindowListener)b).windowDeactivated(e);
    }

    /**
     * Handles the windowStateChanged event by invoking the
     * windowStateChanged methods on listener-a and listener-b.
     * @param e the window event
     * @since 1.4
     */
    public void windowStateChanged(WindowEvent e) {
        ((WindowStateListener)a).windowStateChanged(e);
        ((WindowStateListener)b).windowStateChanged(e);
    }


    /**
     * Handles the windowGainedFocus event by invoking the windowGainedFocus
     * methods on listener-a and listener-b.
     * @param e the window event
     * @since 1.4
     */
    public void windowGainedFocus(WindowEvent e) {
        ((WindowFocusListener)a).windowGainedFocus(e);
        ((WindowFocusListener)b).windowGainedFocus(e);
    }

    /**
     * Handles the windowLostFocus event by invoking the windowLostFocus
     * methods on listener-a and listener-b.
     * @param e the window event
     * @since 1.4
     */
    public void windowLostFocus(WindowEvent e) {
        ((WindowFocusListener)a).windowLostFocus(e);
        ((WindowFocusListener)b).windowLostFocus(e);
    }

    /**
     * Handles the actionPerformed event by invoking the
     * actionPerformed methods on listener-a and listener-b.
     * @param e the action event
     */
    public void actionPerformed(ActionEvent e) {
        ((ActionListener)a).actionPerformed(e);
        ((ActionListener)b).actionPerformed(e);
    }

    /**
     * Handles the itemStateChanged event by invoking the
     * itemStateChanged methods on listener-a and listener-b.
     * @param e the item event
     */
    public void itemStateChanged(ItemEvent e) {
        ((ItemListener)a).itemStateChanged(e);
        ((ItemListener)b).itemStateChanged(e);
    }

    /**
     * Handles the adjustmentValueChanged event by invoking the
     * adjustmentValueChanged methods on listener-a and listener-b.
     * @param e the adjustment event
     */
    public void adjustmentValueChanged(AdjustmentEvent e) {
        ((AdjustmentListener)a).adjustmentValueChanged(e);
        ((AdjustmentListener)b).adjustmentValueChanged(e);
    }
    public void textValueChanged(TextEvent e) {
        ((TextListener)a).textValueChanged(e);
        ((TextListener)b).textValueChanged(e);
    }

    /**
     * Handles the inputMethodTextChanged event by invoking the
     * inputMethodTextChanged methods on listener-a and listener-b.
     * @param e the item event
     */
    public void inputMethodTextChanged(InputMethodEvent e) {
       ((InputMethodListener)a).inputMethodTextChanged(e);
       ((InputMethodListener)b).inputMethodTextChanged(e);
    }

    /**
     * Handles the caretPositionChanged event by invoking the
     * caretPositionChanged methods on listener-a and listener-b.
     * @param e the item event
     */
    public void caretPositionChanged(InputMethodEvent e) {
       ((InputMethodListener)a).caretPositionChanged(e);
       ((InputMethodListener)b).caretPositionChanged(e);
    }

    /**
     * Handles the hierarchyChanged event by invoking the
     * hierarchyChanged methods on listener-a and listener-b.
     * @param e the item event
     * @since 1.3
     */
    public void hierarchyChanged(HierarchyEvent e) {
        ((HierarchyListener)a).hierarchyChanged(e);
        ((HierarchyListener)b).hierarchyChanged(e);
    }

    /**
     * Handles the ancestorMoved event by invoking the
     * ancestorMoved methods on listener-a and listener-b.
     * @param e the item event
     * @since 1.3
     */
    public void ancestorMoved(HierarchyEvent e) {
        ((HierarchyBoundsListener)a).ancestorMoved(e);
        ((HierarchyBoundsListener)b).ancestorMoved(e);
    }

    /**
     * Handles the ancestorResized event by invoking the
     * ancestorResized methods on listener-a and listener-b.
     * @param e the item event
     * @since 1.3
     */
    public void ancestorResized(HierarchyEvent e) {
        ((HierarchyBoundsListener)a).ancestorResized(e);
        ((HierarchyBoundsListener)b).ancestorResized(e);
    }

    /**
     * Handles the mouseWheelMoved event by invoking the
     * mouseWheelMoved methods on listener-a and listener-b.
     * @param e the mouse event
     * @since 1.4
     */
    public void mouseWheelMoved(MouseWheelEvent e) {
        ((MouseWheelListener)a).mouseWheelMoved(e);
        ((MouseWheelListener)b).mouseWheelMoved(e);
    }

    /**
     * Adds component-listener-a with component-listener-b and
     * returns the resulting multicast listener.
     * @param a component-listener-a
     * @param b component-listener-b
     * @return the resulting listener
     */
    public static ComponentListener add(ComponentListener a, ComponentListener b) {
        return (ComponentListener)addInternal(a, b);
    }

    /**
     * Adds container-listener-a with container-listener-b and
     * returns the resulting multicast listener.
     * @param a container-listener-a
     * @param b container-listener-b
     * @return the resulting listener
     */
    public static ContainerListener add(ContainerListener a, ContainerListener b) {
        return (ContainerListener)addInternal(a, b);
    }

    /**
     * Adds focus-listener-a with focus-listener-b and
     * returns the resulting multicast listener.
     * @param a focus-listener-a
     * @param b focus-listener-b
     * @return the resulting listener
     */
    public static FocusListener add(FocusListener a, FocusListener b) {
        return (FocusListener)addInternal(a, b);
    }

    /**
     * Adds key-listener-a with key-listener-b and
     * returns the resulting multicast listener.
     * @param a key-listener-a
     * @param b key-listener-b
     * @return the resulting listener
     */
    public static KeyListener add(KeyListener a, KeyListener b) {
        return (KeyListener)addInternal(a, b);
    }

    /**
     * Adds mouse-listener-a with mouse-listener-b and
     * returns the resulting multicast listener.
     * @param a mouse-listener-a
     * @param b mouse-listener-b
     * @return the resulting listener
     */
    public static MouseListener add(MouseListener a, MouseListener b) {
        return (MouseListener)addInternal(a, b);
    }

    /**
     * Adds mouse-motion-listener-a with mouse-motion-listener-b and
     * returns the resulting multicast listener.
     * @param a mouse-motion-listener-a
     * @param b mouse-motion-listener-b
     * @return the resulting listener
     */
    public static MouseMotionListener add(MouseMotionListener a, MouseMotionListener b) {
        return (MouseMotionListener)addInternal(a, b);
    }

    /**
     * Adds window-listener-a with window-listener-b and
     * returns the resulting multicast listener.
     * @param a window-listener-a
     * @param b window-listener-b
     * @return the resulting listener
     */
    public static WindowListener add(WindowListener a, WindowListener b) {
        return (WindowListener)addInternal(a, b);
    }

    /**
     * Adds window-state-listener-a with window-state-listener-b
     * and returns the resulting multicast listener.
     * @param a window-state-listener-a
     * @param b window-state-listener-b
     * @return the resulting listener
     * @since 1.4
     */
    @SuppressWarnings("overloads")
    public static WindowStateListener add(WindowStateListener a,
                                          WindowStateListener b) {
        return (WindowStateListener)addInternal(a, b);
    }

    /**
     * Adds window-focus-listener-a with window-focus-listener-b
     * and returns the resulting multicast listener.
     * @param a window-focus-listener-a
     * @param b window-focus-listener-b
     * @return the resulting listener
     * @since 1.4
     */
    public static WindowFocusListener add(WindowFocusListener a,
                                          WindowFocusListener b) {
        return (WindowFocusListener)addInternal(a, b);
    }

    /**
     * Adds action-listener-a with action-listener-b and
     * returns the resulting multicast listener.
     * @param a action-listener-a
     * @param b action-listener-b
     * @return the resulting listener
     */
    @SuppressWarnings("overloads")
    public static ActionListener add(ActionListener a, ActionListener b) {
        return (ActionListener)addInternal(a, b);
    }

    /**
     * Adds item-listener-a with item-listener-b and
     * returns the resulting multicast listener.
     * @param a item-listener-a
     * @param b item-listener-b
     * @return the resulting listener
     */
    @SuppressWarnings("overloads")
    public static ItemListener add(ItemListener a, ItemListener b) {
        return (ItemListener)addInternal(a, b);
    }

    /**
     * Adds adjustment-listener-a with adjustment-listener-b and
     * returns the resulting multicast listener.
     * @param a adjustment-listener-a
     * @param b adjustment-listener-b
     * @return the resulting listener
     */
    @SuppressWarnings("overloads")
    public static AdjustmentListener add(AdjustmentListener a, AdjustmentListener b) {
        return (AdjustmentListener)addInternal(a, b);
    }

    /**
     * Adds text-listener-a with text-listener-b and
     * returns the resulting multicast listener.
     *
     * @param  a text-listener-a
     * @param  b text-listener-b
     * @return the resulting listener
     */
    @SuppressWarnings("overloads")
    public static TextListener add(TextListener a, TextListener b) {
        return (TextListener)addInternal(a, b);
    }

    /**
     * Adds input-method-listener-a with input-method-listener-b and
     * returns the resulting multicast listener.
     * @param a input-method-listener-a
     * @param b input-method-listener-b
     * @return the resulting listener
     */
     public static InputMethodListener add(InputMethodListener a, InputMethodListener b) {
        return (InputMethodListener)addInternal(a, b);
     }

    /**
     * Adds hierarchy-listener-a with hierarchy-listener-b and
     * returns the resulting multicast listener.
     * @param a hierarchy-listener-a
     * @param b hierarchy-listener-b
     * @return the resulting listener
     * @since 1.3
     */
    @SuppressWarnings("overloads")
     public static HierarchyListener add(HierarchyListener a, HierarchyListener b) {
        return (HierarchyListener)addInternal(a, b);
     }

    /**
     * Adds hierarchy-bounds-listener-a with hierarchy-bounds-listener-b and
     * returns the resulting multicast listener.
     * @param a hierarchy-bounds-listener-a
     * @param b hierarchy-bounds-listener-b
     * @return the resulting listener
     * @since 1.3
     */
     public static HierarchyBoundsListener add(HierarchyBoundsListener a, HierarchyBoundsListener b) {
        return (HierarchyBoundsListener)addInternal(a, b);
     }

    /**
     * Adds mouse-wheel-listener-a with mouse-wheel-listener-b and
     * returns the resulting multicast listener.
     * @param a mouse-wheel-listener-a
     * @param b mouse-wheel-listener-b
     * @return the resulting listener
     * @since 1.4
     */
    @SuppressWarnings("overloads")
    public static MouseWheelListener add(MouseWheelListener a,
                                         MouseWheelListener b) {
        return (MouseWheelListener)addInternal(a, b);
    }

    /**
     * Removes the old component-listener from component-listener-l and
     * returns the resulting multicast listener.
     * @param l component-listener-l
     * @param oldl the component-listener being removed
     * @return the resulting listener
     */
    public static ComponentListener remove(ComponentListener l, ComponentListener oldl) {
        return (ComponentListener) removeInternal(l, oldl);
    }

    /**
     * Removes the old container-listener from container-listener-l and
     * returns the resulting multicast listener.
     * @param l container-listener-l
     * @param oldl the container-listener being removed
     * @return the resulting listener
     */
    public static ContainerListener remove(ContainerListener l, ContainerListener oldl) {
        return (ContainerListener) removeInternal(l, oldl);
    }

    /**
     * Removes the old focus-listener from focus-listener-l and
     * returns the resulting multicast listener.
     * @param l focus-listener-l
     * @param oldl the focus-listener being removed
     * @return the resulting listener
     */
    public static FocusListener remove(FocusListener l, FocusListener oldl) {
        return (FocusListener) removeInternal(l, oldl);
    }

    /**
     * Removes the old key-listener from key-listener-l and
     * returns the resulting multicast listener.
     * @param l key-listener-l
     * @param oldl the key-listener being removed
     * @return the resulting listener
     */
    public static KeyListener remove(KeyListener l, KeyListener oldl) {
        return (KeyListener) removeInternal(l, oldl);
    }

    /**
     * Removes the old mouse-listener from mouse-listener-l and
     * returns the resulting multicast listener.
     * @param l mouse-listener-l
     * @param oldl the mouse-listener being removed
     * @return the resulting listener
     */
    public static MouseListener remove(MouseListener l, MouseListener oldl) {
        return (MouseListener) removeInternal(l, oldl);
    }

    /**
     * Removes the old mouse-motion-listener from mouse-motion-listener-l
     * and returns the resulting multicast listener.
     * @param l mouse-motion-listener-l
     * @param oldl the mouse-motion-listener being removed
     * @return the resulting listener
     */
    public static MouseMotionListener remove(MouseMotionListener l, MouseMotionListener oldl) {
        return (MouseMotionListener) removeInternal(l, oldl);
    }

    /**
     * Removes the old window-listener from window-listener-l and
     * returns the resulting multicast listener.
     * @param l window-listener-l
     * @param oldl the window-listener being removed
     * @return the resulting listener
     */
    public static WindowListener remove(WindowListener l, WindowListener oldl) {
        return (WindowListener) removeInternal(l, oldl);
    }

    /**
     * Removes the old window-state-listener from window-state-listener-l
     * and returns the resulting multicast listener.
     * @param l window-state-listener-l
     * @param oldl the window-state-listener being removed
     * @return the resulting listener
     * @since 1.4
     */
    @SuppressWarnings("overloads")
    public static WindowStateListener remove(WindowStateListener l,
                                             WindowStateListener oldl) {
        return (WindowStateListener) removeInternal(l, oldl);
    }

    /**
     * Removes the old window-focus-listener from window-focus-listener-l
     * and returns the resulting multicast listener.
     * @param l window-focus-listener-l
     * @param oldl the window-focus-listener being removed
     * @return the resulting listener
     * @since 1.4
     */
    public static WindowFocusListener remove(WindowFocusListener l,
                                             WindowFocusListener oldl) {
        return (WindowFocusListener) removeInternal(l, oldl);
    }

    /**
     * Removes the old action-listener from action-listener-l and
     * returns the resulting multicast listener.
     * @param l action-listener-l
     * @param oldl the action-listener being removed
     * @return the resulting listener
     */
    @SuppressWarnings("overloads")
    public static ActionListener remove(ActionListener l, ActionListener oldl) {
        return (ActionListener) removeInternal(l, oldl);
    }

    /**
     * Removes the old item-listener from item-listener-l and
     * returns the resulting multicast listener.
     * @param l item-listener-l
     * @param oldl the item-listener being removed
     * @return the resulting listener
     */
    @SuppressWarnings("overloads")
    public static ItemListener remove(ItemListener l, ItemListener oldl) {
        return (ItemListener) removeInternal(l, oldl);
    }

    /**
     * Removes the old adjustment-listener from adjustment-listener-l and
     * returns the resulting multicast listener.
     * @param l adjustment-listener-l
     * @param oldl the adjustment-listener being removed
     * @return the resulting listener
     */
    @SuppressWarnings("overloads")
    public static AdjustmentListener remove(AdjustmentListener l, AdjustmentListener oldl) {
        return (AdjustmentListener) removeInternal(l, oldl);
    }

    /**
     * Removes the old text-listener from text-listener-l and
     * returns the resulting multicast listener.
     *
     * @param  l text-listener-l
     * @param  oldl the text-listener being removed
     * @return the resulting listener
     */
    @SuppressWarnings("overloads")
    public static TextListener remove(TextListener l, TextListener oldl) {
        return (TextListener) removeInternal(l, oldl);
    }

    /**
     * Removes the old input-method-listener from input-method-listener-l and
     * returns the resulting multicast listener.
     * @param l input-method-listener-l
     * @param oldl the input-method-listener being removed
     * @return the resulting listener
     */
    public static InputMethodListener remove(InputMethodListener l, InputMethodListener oldl) {
        return (InputMethodListener) removeInternal(l, oldl);
    }

    /**
     * Removes the old hierarchy-listener from hierarchy-listener-l and
     * returns the resulting multicast listener.
     * @param l hierarchy-listener-l
     * @param oldl the hierarchy-listener being removed
     * @return the resulting listener
     * @since 1.3
     */
    @SuppressWarnings("overloads")
    public static HierarchyListener remove(HierarchyListener l, HierarchyListener oldl) {
        return (HierarchyListener) removeInternal(l, oldl);
    }

    /**
     * Removes the old hierarchy-bounds-listener from
     * hierarchy-bounds-listener-l and returns the resulting multicast
     * listener.
     * @param l hierarchy-bounds-listener-l
     * @param oldl the hierarchy-bounds-listener being removed
     * @return the resulting listener
     * @since 1.3
     */
    public static HierarchyBoundsListener remove(HierarchyBoundsListener l, HierarchyBoundsListener oldl) {
        return (HierarchyBoundsListener) removeInternal(l, oldl);
    }

    /**
     * Removes the old mouse-wheel-listener from mouse-wheel-listener-l
     * and returns the resulting multicast listener.
     * @param l mouse-wheel-listener-l
     * @param oldl the mouse-wheel-listener being removed
     * @return the resulting listener
     * @since 1.4
     */
    @SuppressWarnings("overloads")
    public static MouseWheelListener remove(MouseWheelListener l,
                                            MouseWheelListener oldl) {
      return (MouseWheelListener) removeInternal(l, oldl);
    }

    /**
     * Returns the resulting multicast listener from adding listener-a
     * and listener-b together.
     * If listener-a is null, it returns listener-b;
     * If listener-b is null, it returns listener-a
     * If neither are null, then it creates and returns
     * a new AWTEventMulticaster instance which chains a with b.
     * @param a event listener-a
     * @param b event listener-b
     * @return the resulting listener
     */
    protected static EventListener addInternal(EventListener a, EventListener b) {
        if (a == null)  return b;
        if (b == null)  return a;
        return new AWTEventMulticaster(a, b);
    }

    /**
     * Returns the resulting multicast listener after removing the
     * old listener from listener-l.
     * If listener-l equals the old listener OR listener-l is null,
     * returns null.
     * Else if listener-l is an instance of AWTEventMulticaster,
     * then it removes the old listener from it.
     * Else, returns listener l.
     * @param l the listener being removed from
     * @param oldl the listener being removed
     * @return the resulting listener
     */
    protected static EventListener removeInternal(EventListener l, EventListener oldl) {
        if (l == oldl || l == null) {
            return null;
        } else if (l instanceof AWTEventMulticaster) {
            return ((AWTEventMulticaster)l).remove(oldl);
        } else {
            return l;           // it's not here
        }
    }


   /**
    * Serialization support. Saves all Serializable listeners
    * to a serialization stream.
    *
    * @param  s the stream to save to
    * @param  k a prefix stream to put before each serializable listener
    * @throws IOException if serialization fails
    */
    protected void saveInternal(ObjectOutputStream s, String k) throws IOException {
        if (a instanceof AWTEventMulticaster) {
            ((AWTEventMulticaster)a).saveInternal(s, k);
        }
        else if (a instanceof Serializable) {
            s.writeObject(k);
            s.writeObject(a);
        }

        if (b instanceof AWTEventMulticaster) {
            ((AWTEventMulticaster)b).saveInternal(s, k);
        }
        else if (b instanceof Serializable) {
            s.writeObject(k);
            s.writeObject(b);
        }
    }

   /**
    * Saves a Serializable listener chain to a serialization stream.
    *
    * @param  s the stream to save to
    * @param  k a prefix stream to put before each serializable listener
    * @param  l the listener chain to save
    * @throws IOException if serialization fails
    */
    protected static void save(ObjectOutputStream s, String k, EventListener l) throws IOException {
      if (l == null) {
          return;
      }
      else if (l instanceof AWTEventMulticaster) {
          ((AWTEventMulticaster)l).saveInternal(s, k);
      }
      else if (l instanceof Serializable) {
           s.writeObject(k);
           s.writeObject(l);
      }
    }

    /*
     * Recursive method which returns a count of the number of listeners in
     * EventListener, handling the (common) case of l actually being an
     * AWTEventMulticaster.  Additionally, only listeners of type listenerType
     * are counted.  Method modified to fix bug 4513402.  -bchristi
     */
    private static int getListenerCount(EventListener l, Class<?> listenerType) {
        if (l instanceof AWTEventMulticaster) {
            AWTEventMulticaster mc = (AWTEventMulticaster)l;
            return getListenerCount(mc.a, listenerType) +
             getListenerCount(mc.b, listenerType);
        }
        else {
            // Only count listeners of correct type
            return listenerType.isInstance(l) ? 1 : 0;
        }
    }

    /*
     * Recursive method which populates EventListener array a with EventListeners
     * from l.  l is usually an AWTEventMulticaster.  Bug 4513402 revealed that
     * if l differed in type from the element type of a, an ArrayStoreException
     * would occur.  Now l is only inserted into a if it's of the appropriate
     * type.  -bchristi
     */
    private static int populateListenerArray(EventListener[] a, EventListener l, int index) {
        if (l instanceof AWTEventMulticaster) {
            AWTEventMulticaster mc = (AWTEventMulticaster)l;
            int lhs = populateListenerArray(a, mc.a, index);
            return populateListenerArray(a, mc.b, lhs);
        }
        else if (a.getClass().getComponentType().isInstance(l)) {
            a[index] = l;
            return index + 1;
        }
        // Skip nulls, instances of wrong class
        else {
            return index;
        }
    }

    /**
     * Returns an array of all the objects chained as
     * <code><em>Foo</em>Listener</code>s by the specified
     * {@code java.util.EventListener}.
     * <code><em>Foo</em>Listener</code>s are chained by the
     * {@code AWTEventMulticaster} using the
     * <code>add<em>Foo</em>Listener</code> method.
     * If a {@code null} listener is specified, this method returns an
     * empty array. If the specified listener is not an instance of
     * {@code AWTEventMulticaster}, this method returns an array which
     * contains only the specified listener. If no such listeners are chained,
     * this method returns an empty array.
     *
     * @param <T> the listener type
     * @param l the specified {@code java.util.EventListener}
     * @param listenerType the type of listeners requested; this parameter
     *          should specify an interface that descends from
     *          {@code java.util.EventListener}
     * @return an array of all objects chained as
     *          <code><em>Foo</em>Listener</code>s by the specified multicast
     *          listener, or an empty array if no such listeners have been
     *          chained by the specified multicast listener
     * @exception NullPointerException if the specified
     *             {@code listenertype} parameter is {@code null}
     * @exception ClassCastException if {@code listenerType}
     *          doesn't specify a class or interface that implements
     *          {@code java.util.EventListener}
     *
     * @since 1.4
     */
    @SuppressWarnings("unchecked")
    public static <T extends EventListener> T[]
        getListeners(EventListener l, Class<T> listenerType)
    {
        if (listenerType == null) {
            throw new NullPointerException ("Listener type should not be null");
        }

        int n = getListenerCount(l, listenerType);
        T[] result = (T[])Array.newInstance(listenerType, n);
        populateListenerArray(result, l, 0);
        return result;
    }
}
