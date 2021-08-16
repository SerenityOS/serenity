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

package javax.swing.text;

import java.awt.Graphics;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.EventListener;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JPasswordField;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.TextUI;

import sun.swing.SwingUtilities2;

/**
 * A default implementation of Caret.  The caret is rendered as
 * a vertical line in the color specified by the CaretColor property
 * of the associated JTextComponent.  It can blink at the rate specified
 * by the BlinkRate property.
 * <p>
 * This implementation expects two sources of asynchronous notification.
 * The timer thread fires asynchronously, and causes the caret to simply
 * repaint the most recent bounding box.  The caret also tracks change
 * as the document is modified.  Typically this will happen on the
 * event dispatch thread as a result of some mouse or keyboard event.
 * The caret behavior on both synchronous and asynchronous documents updates
 * is controlled by <code>UpdatePolicy</code> property. The repaint of the
 * new caret location will occur on the event thread in any case, as calls to
 * <code>modelToView</code> are only safe on the event thread.
 * <p>
 * The caret acts as a mouse and focus listener on the text component
 * it has been installed in, and defines the caret semantics based upon
 * those events.  The listener methods can be reimplemented to change the
 * semantics.
 * By default, the first mouse button will be used to set focus and caret
 * position.  Dragging the mouse pointer with the first mouse button will
 * sweep out a selection that is contiguous in the model.  If the associated
 * text component is editable, the caret will become visible when focus
 * is gained, and invisible when focus is lost.
 * <p>
 * The Highlighter bound to the associated text component is used to
 * render the selection by default.
 * Selection appearance can be customized by supplying a
 * painter to use for the highlights.  By default a painter is used that
 * will render a solid color as specified in the associated text component
 * in the <code>SelectionColor</code> property.  This can easily be changed
 * by reimplementing the
 * {@link #getSelectionPainter getSelectionPainter}
 * method.
 * <p>
 * A customized caret appearance can be achieved by reimplementing
 * the paint method.  If the paint method is changed, the damage method
 * should also be reimplemented to cause a repaint for the area needed
 * to render the caret.  The caret extends the Rectangle class which
 * is used to hold the bounding box for where the caret was last rendered.
 * This enables the caret to repaint in a thread-safe manner when the
 * caret moves without making a call to modelToView which is unstable
 * between model updates and view repair (i.e. the order of delivery
 * to DocumentListeners is not guaranteed).
 * <p>
 * The magic caret position is set to null when the caret position changes.
 * A timer is used to determine the new location (after the caret change).
 * When the timer fires, if the magic caret position is still null it is
 * reset to the current caret position. Any actions that change
 * the caret position and want the magic caret position to remain the
 * same, must remember the magic caret position, change the cursor, and
 * then set the magic caret position to its original value. This has the
 * benefit that only actions that want the magic caret position to persist
 * (such as open/down) need to know about it.
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
 * @author  Timothy Prinzing
 * @see     Caret
 */
@SuppressWarnings("serial") // Same-version serialization only
public class DefaultCaret extends Rectangle implements Caret, FocusListener, MouseListener, MouseMotionListener {

    /**
     * Indicates that the caret position is to be updated only when
     * document changes are performed on the Event Dispatching Thread.
     * @see #setUpdatePolicy
     * @see #getUpdatePolicy
     * @since 1.5
     */
    public static final int UPDATE_WHEN_ON_EDT = 0;

    /**
     * Indicates that the caret should remain at the same
     * absolute position in the document regardless of any document
     * updates, except when the document length becomes less than
     * the current caret position due to removal. In that case the caret
     * position is adjusted to the end of the document.
     *
     * @see #setUpdatePolicy
     * @see #getUpdatePolicy
     * @since 1.5
     */
    public static final int NEVER_UPDATE = 1;

    /**
     * Indicates that the caret position is to be <b>always</b>
     * updated accordingly to the document changes regardless whether
     * the document updates are performed on the Event Dispatching Thread
     * or not.
     *
     * @see #setUpdatePolicy
     * @see #getUpdatePolicy
     * @since 1.5
     */
    public static final int ALWAYS_UPDATE = 2;

    /**
     * Constructs a default caret.
     */
    public DefaultCaret() {
    }

    /**
     * Sets the caret movement policy on the document updates. Normally
     * the caret updates its absolute position within the document on
     * insertions occurred before or at the caret position and
     * on removals before the caret position. 'Absolute position'
     * means here the position relative to the start of the document.
     * For example if
     * a character is typed within editable text component it is inserted
     * at the caret position and the caret moves to the next absolute
     * position within the document due to insertion and if
     * <code>BACKSPACE</code> is typed then caret decreases its absolute
     * position due to removal of a character before it. Sometimes
     * it may be useful to turn off the caret position updates so that
     * the caret stays at the same absolute position within the
     * document position regardless of any document updates.
     * <p>
     * The following update policies are allowed:
     * <ul>
     *   <li><code>NEVER_UPDATE</code>: the caret stays at the same
     *       absolute position in the document regardless of any document
     *       updates, except when document length becomes less than
     *       the current caret position due to removal. In that case caret
     *       position is adjusted to the end of the document.
     *       The caret doesn't try to keep itself visible by scrolling
     *       the associated view when using this policy. </li>
     *   <li><code>ALWAYS_UPDATE</code>: the caret always tracks document
     *       changes. For regular changes it increases its position
     *       if an insertion occurs before or at its current position,
     *       and decreases position if a removal occurs before
     *       its current position. For undo/redo updates it is always
     *       moved to the position where update occurred. The caret
     *       also tries to keep itself visible by calling
     *       <code>adjustVisibility</code> method.</li>
     *   <li><code>UPDATE_WHEN_ON_EDT</code>: acts like <code>ALWAYS_UPDATE</code>
     *       if the document updates are performed on the Event Dispatching Thread
     *       and like <code>NEVER_UPDATE</code> if updates are performed on
     *       other thread. </li>
     * </ul> <p>
     * The default property value is <code>UPDATE_WHEN_ON_EDT</code>.
     *
     * @param policy one of the following values : <code>UPDATE_WHEN_ON_EDT</code>,
     * <code>NEVER_UPDATE</code>, <code>ALWAYS_UPDATE</code>
     * @throws IllegalArgumentException if invalid value is passed
     *
     * @see #getUpdatePolicy
     * @see #adjustVisibility
     * @see #UPDATE_WHEN_ON_EDT
     * @see #NEVER_UPDATE
     * @see #ALWAYS_UPDATE
     *
     * @since 1.5
     */
    public void setUpdatePolicy(int policy) {
        updatePolicy = policy;
    }

    /**
     * Gets the caret movement policy on document updates.
     *
     * @return one of the following values : <code>UPDATE_WHEN_ON_EDT</code>,
     * <code>NEVER_UPDATE</code>, <code>ALWAYS_UPDATE</code>
     *
     * @see #setUpdatePolicy
     * @see #UPDATE_WHEN_ON_EDT
     * @see #NEVER_UPDATE
     * @see #ALWAYS_UPDATE
     *
     * @since 1.5
     */
    public int getUpdatePolicy() {
        return updatePolicy;
    }

    /**
     * Gets the text editor component that this caret is
     * is bound to.
     *
     * @return the component
     */
    protected final JTextComponent getComponent() {
        return component;
    }

    /**
     * Cause the caret to be painted.  The repaint
     * area is the bounding box of the caret (i.e.
     * the caret rectangle or <em>this</em>).
     * <p>
     * This method is thread safe, although most Swing methods
     * are not. Please see
     * <A HREF="https://docs.oracle.com/javase/tutorial/uiswing/concurrency/index.html">Concurrency
     * in Swing</A> for more information.
     */
    protected final synchronized void repaint() {
        if (component != null) {
            component.repaint(x, y, width, height);
        }
    }

    /**
     * Damages the area surrounding the caret to cause
     * it to be repainted in a new location.  If paint()
     * is reimplemented, this method should also be
     * reimplemented.  This method should update the
     * caret bounds (x, y, width, and height).
     *
     * @param r  the current location of the caret
     * @see #paint
     */
    protected synchronized void damage(Rectangle r) {
        if (r != null) {
            int damageWidth = getCaretWidth(r.height);
            x = r.x - 4 - (damageWidth >> 1);
            y = r.y;
            width = 9 + damageWidth;
            height = r.height;
            repaint();
        }
    }

    /**
     * Scrolls the associated view (if necessary) to make
     * the caret visible.  Since how this should be done
     * is somewhat of a policy, this method can be
     * reimplemented to change the behavior.  By default
     * the scrollRectToVisible method is called on the
     * associated component.
     *
     * @param nloc the new position to scroll to
     */
    protected void adjustVisibility(Rectangle nloc) {
        if(component == null) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
                component.scrollRectToVisible(nloc);
        } else {
            SwingUtilities.invokeLater(new SafeScroller(nloc));
        }
    }

    /**
     * Gets the painter for the Highlighter.
     *
     * @return the painter
     */
    protected Highlighter.HighlightPainter getSelectionPainter() {
        return DefaultHighlighter.DefaultPainter;
    }

    /**
     * Tries to set the position of the caret from
     * the coordinates of a mouse event, using viewToModel().
     *
     * @param e the mouse event
     */
    @SuppressWarnings("deprecation")
    protected void positionCaret(MouseEvent e) {
        Point pt = new Point(e.getX(), e.getY());
        Position.Bias[] biasRet = new Position.Bias[1];
        int pos = component.getUI().viewToModel(component, pt, biasRet);
        if(biasRet[0] == null)
            biasRet[0] = Position.Bias.Forward;
        if (pos >= 0) {
            setDot(pos, biasRet[0]);
        }
    }

    /**
     * Tries to move the position of the caret from
     * the coordinates of a mouse event, using viewToModel().
     * This will cause a selection if the dot and mark
     * are different.
     *
     * @param e the mouse event
     */
    @SuppressWarnings("deprecation")
    protected void moveCaret(MouseEvent e) {
        Point pt = new Point(e.getX(), e.getY());
        Position.Bias[] biasRet = new Position.Bias[1];
        int pos = component.getUI().viewToModel(component, pt, biasRet);
        if(biasRet[0] == null)
            biasRet[0] = Position.Bias.Forward;
        if (pos >= 0) {
            moveDot(pos, biasRet[0]);
        }
    }

    // --- FocusListener methods --------------------------

    /**
     * Called when the component containing the caret gains
     * focus.  This is implemented to set the caret to visible
     * if the component is editable.
     *
     * @param e the focus event
     * @see FocusListener#focusGained
     */
    public void focusGained(FocusEvent e) {
        if (component.isEnabled()) {
            if (component.isEditable()) {
                setVisible(true);
            }
            setSelectionVisible(true);
            updateSystemSelection();
        }
    }

    /**
     * Called when the component containing the caret loses
     * focus.  This is implemented to set the caret to visibility
     * to false.
     *
     * @param e the focus event
     * @see FocusListener#focusLost
     */
    public void focusLost(FocusEvent e) {
        setVisible(false);
        setSelectionVisible((e.getCause() == FocusEvent.Cause.ACTIVATION ||
                e.getOppositeComponent() instanceof JRootPane) &&
                (ownsSelection || e.isTemporary()));
    }


    /**
     * Selects word based on the MouseEvent
     */
    @SuppressWarnings("deprecation")
    private void selectWord(MouseEvent e) {
        if (selectedWordEvent != null
            && selectedWordEvent.getX() == e.getX()
            && selectedWordEvent.getY() == e.getY()) {
            //we already done selection for this
            return;
        }
                    Action a = null;
                    ActionMap map = getComponent().getActionMap();
                    if (map != null) {
                        a = map.get(DefaultEditorKit.selectWordAction);
                    }
                    if (a == null) {
                        if (selectWord == null) {
                            selectWord = new DefaultEditorKit.SelectWordAction();
                        }
                        a = selectWord;
                    }
                    a.actionPerformed(new ActionEvent(getComponent(),
                                                      ActionEvent.ACTION_PERFORMED, null, e.getWhen(), e.getModifiers()));
        selectedWordEvent = e;
    }

    // --- MouseListener methods -----------------------------------

    /**
     * Called when the mouse is clicked.  If the click was generated
     * from button1, a double click selects a word,
     * and a triple click the current line.
     *
     * @param e the mouse event
     * @see MouseListener#mouseClicked
     */
    @SuppressWarnings("deprecation")
    public void mouseClicked(MouseEvent e) {
        if (getComponent() == null) {
            return;
        }

        int nclicks = SwingUtilities2.getAdjustedClickCount(getComponent(), e);

        if (! e.isConsumed()) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                // mouse 1 behavior
                if(nclicks == 1) {
                    selectedWordEvent = null;
                } else if(nclicks == 2
                          && SwingUtilities2.canEventAccessSystemClipboard(e)) {
                    selectWord(e);
                    selectedWordEvent = null;
                } else if(nclicks == 3
                          && SwingUtilities2.canEventAccessSystemClipboard(e)) {
                    Action a = null;
                    ActionMap map = getComponent().getActionMap();
                    if (map != null) {
                        a = map.get(DefaultEditorKit.selectLineAction);
                    }
                    if (a == null) {
                        if (selectLine == null) {
                            selectLine = new DefaultEditorKit.SelectLineAction();
                        }
                        a = selectLine;
                    }
                    a.actionPerformed(new ActionEvent(getComponent(),
                                                      ActionEvent.ACTION_PERFORMED, null, e.getWhen(), e.getModifiers()));
                }
            } else if (SwingUtilities.isMiddleMouseButton(e)) {
                // mouse 2 behavior
                if (nclicks == 1 && component.isEditable() && component.isEnabled()
                    && SwingUtilities2.canEventAccessSystemClipboard(e)) {
                    // paste system selection, if it exists
                    JTextComponent c = (JTextComponent) e.getSource();
                    if (c != null) {
                        try {
                            Toolkit tk = c.getToolkit();
                            Clipboard buffer = tk.getSystemSelection();
                            if (buffer != null) {
                                // platform supports system selections, update it.
                                adjustCaret(e);
                                TransferHandler th = c.getTransferHandler();
                                if (th != null) {
                                    Transferable trans = null;

                                    try {
                                        trans = buffer.getContents(null);
                                    } catch (IllegalStateException ise) {
                                        // clipboard was unavailable
                                        UIManager.getLookAndFeel().provideErrorFeedback(c);
                                    }

                                    if (trans != null) {
                                        th.importData(c, trans);
                                    }
                                }
                                adjustFocus(true);
                            }
                        } catch (HeadlessException he) {
                            // do nothing... there is no system clipboard
                        }
                    }
                }
            }
        }
    }

    /**
     * If button 1 is pressed, this is implemented to
     * request focus on the associated text component,
     * and to set the caret position. If the shift key is held down,
     * the caret will be moved, potentially resulting in a selection,
     * otherwise the
     * caret position will be set to the new location.  If the component
     * is not enabled, there will be no request for focus.
     *
     * @param e the mouse event
     * @see MouseListener#mousePressed
     */
    public void mousePressed(MouseEvent e) {
        int nclicks = SwingUtilities2.getAdjustedClickCount(getComponent(), e);

        if (SwingUtilities.isLeftMouseButton(e)) {
            if (e.isConsumed()) {
                shouldHandleRelease = true;
            } else {
                shouldHandleRelease = false;
                adjustCaretAndFocus(e);
                if (nclicks == 2
                    && SwingUtilities2.canEventAccessSystemClipboard(e)) {
                    selectWord(e);
                }
            }
        }
    }

    void adjustCaretAndFocus(MouseEvent e) {
        adjustCaret(e);
        adjustFocus(false);
    }

    /**
     * Adjusts the caret location based on the MouseEvent.
     */
    @SuppressWarnings("deprecation")
    private void adjustCaret(MouseEvent e) {
        if ((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0 &&
            getDot() != -1) {
            moveCaret(e);
        } else if (!e.isPopupTrigger()) {
            positionCaret(e);
        }
    }

    /**
     * Adjusts the focus, if necessary.
     *
     * @param inWindow if true indicates requestFocusInWindow should be used
     */
    private void adjustFocus(boolean inWindow) {
        if ((component != null) && component.isEnabled() &&
                                   component.isRequestFocusEnabled()) {
            if (inWindow) {
                component.requestFocusInWindow();
            }
            else {
                component.requestFocus();
            }
        }
    }

    /**
     * Called when the mouse is released.
     *
     * @param e the mouse event
     * @see MouseListener#mouseReleased
     */
    public void mouseReleased(MouseEvent e) {
        if (!e.isConsumed()
                && shouldHandleRelease
                && SwingUtilities.isLeftMouseButton(e)) {

            adjustCaretAndFocus(e);
        }
    }

    /**
     * Called when the mouse enters a region.
     *
     * @param e the mouse event
     * @see MouseListener#mouseEntered
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * Called when the mouse exits a region.
     *
     * @param e the mouse event
     * @see MouseListener#mouseExited
     */
    public void mouseExited(MouseEvent e) {
    }

    // --- MouseMotionListener methods -------------------------

    /**
     * Moves the caret position
     * according to the mouse pointer's current
     * location.  This effectively extends the
     * selection.  By default, this is only done
     * for mouse button 1.
     *
     * @param e the mouse event
     * @see MouseMotionListener#mouseDragged
     */
    public void mouseDragged(MouseEvent e) {
        if ((! e.isConsumed()) && SwingUtilities.isLeftMouseButton(e)) {
            moveCaret(e);
        }
    }

    /**
     * Called when the mouse is moved.
     *
     * @param e the mouse event
     * @see MouseMotionListener#mouseMoved
     */
    public void mouseMoved(MouseEvent e) {
    }

    // ---- Caret methods ---------------------------------

    /**
     * Renders the caret as a vertical line.  If this is reimplemented
     * the damage method should also be reimplemented as it assumes the
     * shape of the caret is a vertical line.  Sets the caret color to
     * the value returned by getCaretColor().
     * <p>
     * If there are multiple text directions present in the associated
     * document, a flag indicating the caret bias will be rendered.
     * This will occur only if the associated document is a subclass
     * of AbstractDocument and there are multiple bidi levels present
     * in the bidi element structure (i.e. the text has multiple
     * directions associated with it).
     *
     * @param g the graphics context
     * @see #damage
     */
    @SuppressWarnings("deprecation")
    public void paint(Graphics g) {
        if(isVisible()) {
            try {
                TextUI mapper = component.getUI();
                Rectangle r = mapper.modelToView(component, dot, dotBias);

                if ((r == null) || ((r.width == 0) && (r.height == 0))) {
                    return;
                }
                if (width > 0 && height > 0 &&
                                !this._contains(r.x, r.y, r.width, r.height)) {
                    // We seem to have gotten out of sync and no longer
                    // contain the right location, adjust accordingly.
                    Rectangle clip = g.getClipBounds();

                    if (clip != null && !clip.contains(this)) {
                        // Clip doesn't contain the old location, force it
                        // to be repainted lest we leave a caret around.
                        repaint();
                    }
                    // This will potentially cause a repaint of something
                    // we're already repainting, but without changing the
                    // semantics of damage we can't really get around this.
                    damage(r);
                }
                g.setColor(component.getCaretColor());
                int paintWidth = getCaretWidth(r.height);
                r.x -= paintWidth  >> 1;
                g.fillRect(r.x, r.y, paintWidth, r.height);

                // see if we should paint a flag to indicate the bias
                // of the caret.
                // PENDING(prinz) this should be done through
                // protected methods so that alternative LAF
                // will show bidi information.
                Document doc = component.getDocument();
                if (doc instanceof AbstractDocument) {
                    Element bidi = ((AbstractDocument)doc).getBidiRootElement();
                    if ((bidi != null) && (bidi.getElementCount() > 1)) {
                        // there are multiple directions present.
                        flagXPoints[0] = r.x + ((dotLTR) ? paintWidth : 0);
                        flagYPoints[0] = r.y;
                        flagXPoints[1] = flagXPoints[0];
                        flagYPoints[1] = flagYPoints[0] + 4;
                        flagXPoints[2] = flagXPoints[0] + ((dotLTR) ? 4 : -4);
                        flagYPoints[2] = flagYPoints[0];
                        g.fillPolygon(flagXPoints, flagYPoints, 3);
                    }
                }
            } catch (BadLocationException e) {
                // can't render I guess
                //System.err.println("Can't render cursor");
            }
        }
    }

    /**
     * Called when the UI is being installed into the
     * interface of a JTextComponent.  This can be used
     * to gain access to the model that is being navigated
     * by the implementation of this interface.  Sets the dot
     * and mark to 0, and establishes document, property change,
     * focus, mouse, and mouse motion listeners.
     *
     * @param c the component
     * @see Caret#install
     */
    public void install(JTextComponent c) {
        component = c;
        Document doc = c.getDocument();
        dot = mark = 0;
        dotLTR = markLTR = true;
        dotBias = markBias = Position.Bias.Forward;
        if (doc != null) {
            doc.addDocumentListener(handler);
        }
        c.addPropertyChangeListener(handler);
        c.addFocusListener(this);
        c.addMouseListener(this);
        c.addMouseMotionListener(this);

        // if the component already has focus, it won't
        // be notified.
        if (component.hasFocus()) {
            focusGained(null);
        }

        Number ratio = (Number) c.getClientProperty("caretAspectRatio");
        if (ratio != null) {
            aspectRatio = ratio.floatValue();
        } else {
            aspectRatio = -1;
        }

        Integer width = (Integer) c.getClientProperty("caretWidth");
        if (width != null) {
            caretWidth = width.intValue();
        } else {
            caretWidth = -1;
        }
    }

    /**
     * Called when the UI is being removed from the
     * interface of a JTextComponent.  This is used to
     * unregister any listeners that were attached.
     *
     * @param c the component
     * @see Caret#deinstall
     */
    public void deinstall(JTextComponent c) {
        c.removeMouseListener(this);
        c.removeMouseMotionListener(this);
        c.removeFocusListener(this);
        c.removePropertyChangeListener(handler);
        Document doc = c.getDocument();
        if (doc != null) {
            doc.removeDocumentListener(handler);
        }
        synchronized(this) {
            component = null;
        }
        if (flasher != null) {
            flasher.stop();
        }


    }

    /**
     * Adds a listener to track whenever the caret position has
     * been changed.
     *
     * @param l the listener
     * @see Caret#addChangeListener
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    /**
     * Removes a listener that was tracking caret position changes.
     *
     * @param l the listener
     * @see Caret#removeChangeListener
     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    /**
     * Returns an array of all the change listeners
     * registered on this caret.
     *
     * @return all of this caret's <code>ChangeListener</code>s
     *         or an empty
     *         array if no change listeners are currently registered
     *
     * @see #addChangeListener
     * @see #removeChangeListener
     *
     * @since 1.4
     */
    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the parameters passed into
     * the fire method.  The listener list is processed last to first.
     *
     * @see EventListenerList
     */
    protected void fireStateChanged() {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ChangeListener.class) {
                // Lazily create the event:
                if (changeEvent == null)
                    changeEvent = new ChangeEvent(this);
                ((ChangeListener)listeners[i+1]).stateChanged(changeEvent);
            }
        }
    }

    /**
     * Returns an array of all the objects currently registered
     * as <code><em>Foo</em>Listener</code>s
     * upon this caret.
     * <code><em>Foo</em>Listener</code>s are registered using the
     * <code>add<em>Foo</em>Listener</code> method.
     *
     * <p>
     *
     * You can specify the <code>listenerType</code> argument
     * with a class literal,
     * such as
     * <code><em>Foo</em>Listener.class</code>.
     * For example, you can query a
     * <code>DefaultCaret</code> <code>c</code>
     * for its change listeners with the following code:
     *
     * <pre>ChangeListener[] cls = (ChangeListener[])(c.getListeners(ChangeListener.class));</pre>
     *
     * If no such listeners exist, this method returns an empty array.
     * @param <T> the listener type
     * @param listenerType the type of listeners requested
     * @return an array of all objects registered as
     *          <code><em>Foo</em>Listener</code>s on this component,
     *          or an empty array if no such
     *          listeners have been added
     * @exception ClassCastException if <code>listenerType</code>
     *          doesn't specify a class or interface that implements
     *          <code>java.util.EventListener</code>
     *
     * @see #getChangeListeners
     *
     * @since 1.3
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    /**
     * Changes the selection visibility.
     *
     * @param vis the new visibility
     */
    public void setSelectionVisible(boolean vis) {
        if (vis != selectionVisible) {
            selectionVisible = vis;
            if (selectionVisible) {
                // show
                Highlighter h = component.getHighlighter();
                if ((dot != mark) && (h != null) && (selectionTag == null)) {
                    int p0 = Math.min(dot, mark);
                    int p1 = Math.max(dot, mark);
                    Highlighter.HighlightPainter p = getSelectionPainter();
                    try {
                        selectionTag = h.addHighlight(p0, p1, p);
                    } catch (BadLocationException bl) {
                        selectionTag = null;
                    }
                }
            } else {
                // hide
                if (selectionTag != null) {
                    Highlighter h = component.getHighlighter();
                    h.removeHighlight(selectionTag);
                    selectionTag = null;
                }
            }
        }
    }

    /**
     * Checks whether the current selection is visible.
     *
     * @return true if the selection is visible
     */
    public boolean isSelectionVisible() {
        return selectionVisible;
    }

    /**
     * Determines if the caret is currently active.
     * <p>
     * This method returns whether or not the <code>Caret</code>
     * is currently in a blinking state. It does not provide
     * information as to whether it is currently blinked on or off.
     * To determine if the caret is currently painted use the
     * <code>isVisible</code> method.
     *
     * @return <code>true</code> if active else <code>false</code>
     * @see #isVisible
     *
     * @since 1.5
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Indicates whether or not the caret is currently visible. As the
     * caret flashes on and off the return value of this will change
     * between true, when the caret is painted, and false, when the
     * caret is not painted. <code>isActive</code> indicates whether
     * or not the caret is in a blinking state, such that it <b>can</b>
     * be visible, and <code>isVisible</code> indicates whether or not
     * the caret <b>is</b> actually visible.
     * <p>
     * Subclasses that wish to render a different flashing caret
     * should override paint and only paint the caret if this method
     * returns true.
     *
     * @return true if visible else false
     * @see Caret#isVisible
     * @see #isActive
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Sets the caret visibility, and repaints the caret.
     * It is important to understand the relationship between this method,
     * <code>isVisible</code> and <code>isActive</code>.
     * Calling this method with a value of <code>true</code> activates the
     * caret blinking. Setting it to <code>false</code> turns it completely off.
     * To determine whether the blinking is active, you should call
     * <code>isActive</code>. In effect, <code>isActive</code> is an
     * appropriate corresponding "getter" method for this one.
     * <code>isVisible</code> can be used to fetch the current
     * visibility status of the caret, meaning whether or not it is currently
     * painted. This status will change as the caret blinks on and off.
     * <p>
     * Here's a list showing the potential return values of both
     * <code>isActive</code> and <code>isVisible</code>
     * after calling this method:
     * <p>
     * <b><code>setVisible(true)</code></b>:
     * <ul>
     *     <li>isActive(): true</li>
     *     <li>isVisible(): true or false depending on whether
     *         or not the caret is blinked on or off</li>
     * </ul>
     * <p>
     * <b><code>setVisible(false)</code></b>:
     * <ul>
     *     <li>isActive(): false</li>
     *     <li>isVisible(): false</li>
     * </ul>
     *
     * @param e the visibility specifier
     * @see #isActive
     * @see Caret#setVisible
     */
    @SuppressWarnings("deprecation")
    public void setVisible(boolean e) {
        // focus lost notification can come in later after the
        // caret has been deinstalled, in which case the component
        // will be null.
        active = e;
        if (component != null) {
            TextUI mapper = component.getUI();
            if (visible != e) {
                visible = e;
                // repaint the caret
                try {
                    Rectangle loc = mapper.modelToView(component, dot,dotBias);
                    damage(loc);
                } catch (BadLocationException badloc) {
                    // hmm... not legally positioned
                }
            }
        }
        if (flasher != null) {
            if (visible) {
                flasher.start();
            } else {
                flasher.stop();
            }
        }
    }

    /**
     * Sets the caret blink rate.
     *
     * @param rate the rate in milliseconds, 0 to stop blinking
     * @see Caret#setBlinkRate
     */
    public void setBlinkRate(int rate) {
        if (rate != 0) {
            if (flasher == null) {
                flasher = new Timer(rate, handler);
            }
            flasher.setDelay(rate);
        } else {
            if (flasher != null) {
                flasher.stop();
                flasher.removeActionListener(handler);
                flasher = null;
            }
        }
    }

    /**
     * Gets the caret blink rate.
     *
     * @return the delay in milliseconds.  If this is
     *  zero the caret will not blink.
     * @see Caret#getBlinkRate
     */
    public int getBlinkRate() {
        return (flasher == null) ? 0 : flasher.getDelay();
    }

    /**
     * Fetches the current position of the caret.
     *
     * @return the position &gt;= 0
     * @see Caret#getDot
     */
    public int getDot() {
        return dot;
    }

    /**
     * Fetches the current position of the mark.  If there is a selection,
     * the dot and mark will not be the same.
     *
     * @return the position &gt;= 0
     * @see Caret#getMark
     */
    public int getMark() {
        return mark;
    }

    /**
     * Sets the caret position and mark to the specified position,
     * with a forward bias. This implicitly sets the
     * selection range to zero.
     *
     * @param dot the position &gt;= 0
     * @see #setDot(int, Position.Bias)
     * @see Caret#setDot
     */
    public void setDot(int dot) {
        setDot(dot, Position.Bias.Forward);
    }

    /**
     * Moves the caret position to the specified position,
     * with a forward bias.
     *
     * @param dot the position &gt;= 0
     * @see #moveDot(int, javax.swing.text.Position.Bias)
     * @see Caret#moveDot
     */
    public void moveDot(int dot) {
        moveDot(dot, Position.Bias.Forward);
    }

    // ---- Bidi methods (we could put these in a subclass)

    /**
     * Moves the caret position to the specified position, with the
     * specified bias.
     *
     * @param dot the position &gt;= 0
     * @param dotBias the bias for this position, not <code>null</code>
     * @throws IllegalArgumentException if the bias is <code>null</code>
     * @see Caret#moveDot
     * @since 1.6
     */
    public void moveDot(int dot, Position.Bias dotBias) {
        if (dotBias == null) {
            throw new IllegalArgumentException("null bias");
        }

        if (! component.isEnabled()) {
            // don't allow selection on disabled components.
            setDot(dot, dotBias);
            return;
        }
        if (dot != this.dot) {
            NavigationFilter filter = component.getNavigationFilter();

            if (filter != null) {
                filter.moveDot(getFilterBypass(), dot, dotBias);
            }
            else {
                handleMoveDot(dot, dotBias);
            }
        }
    }

    void handleMoveDot(int dot, Position.Bias dotBias) {
        changeCaretPosition(dot, dotBias);

        if (selectionVisible) {
            Highlighter h = component.getHighlighter();
            if (h != null) {
                int p0 = Math.min(dot, mark);
                int p1 = Math.max(dot, mark);

                // if p0 == p1 then there should be no highlight, remove it if necessary
                if (p0 == p1) {
                    if (selectionTag != null) {
                        h.removeHighlight(selectionTag);
                        selectionTag = null;
                    }
                // otherwise, change or add the highlight
                } else {
                    try {
                        if (selectionTag != null) {
                            h.changeHighlight(selectionTag, p0, p1);
                        } else {
                            Highlighter.HighlightPainter p = getSelectionPainter();
                            selectionTag = h.addHighlight(p0, p1, p);
                        }
                    } catch (BadLocationException e) {
                        throw new StateInvariantError("Bad caret position");
                    }
                }
            }
        }
    }

    /**
     * Sets the caret position and mark to the specified position, with the
     * specified bias. This implicitly sets the selection range
     * to zero.
     *
     * @param dot the position &gt;= 0
     * @param dotBias the bias for this position, not <code>null</code>
     * @throws IllegalArgumentException if the bias is <code>null</code>
     * @see Caret#setDot
     * @since 1.6
     */
    public void setDot(int dot, Position.Bias dotBias) {
        if (dotBias == null) {
            throw new IllegalArgumentException("null bias");
        }

        NavigationFilter filter = component.getNavigationFilter();

        if (filter != null) {
            filter.setDot(getFilterBypass(), dot, dotBias);
        }
        else {
            handleSetDot(dot, dotBias);
        }
    }

    void handleSetDot(int dot, Position.Bias dotBias) {
        // move dot, if it changed
        Document doc = component.getDocument();
        if (doc != null) {
            dot = Math.min(dot, doc.getLength());
        }
        dot = Math.max(dot, 0);

        // The position (0,Backward) is out of range so disallow it.
        if( dot == 0 )
            dotBias = Position.Bias.Forward;

        mark = dot;
        if (this.dot != dot || this.dotBias != dotBias ||
            selectionTag != null || forceCaretPositionChange) {
            changeCaretPosition(dot, dotBias);
        }
        this.markBias = this.dotBias;
        this.markLTR = dotLTR;
        Highlighter h = component.getHighlighter();
        if ((h != null) && (selectionTag != null)) {
            h.removeHighlight(selectionTag);
            selectionTag = null;
        }
    }

    /**
     * Returns the bias of the caret position.
     *
     * @return the bias of the caret position
     * @since 1.6
     */
    public Position.Bias getDotBias() {
        return dotBias;
    }

    /**
     * Returns the bias of the mark.
     *
     * @return the bias of the mark
     * @since 1.6
     */
    public Position.Bias getMarkBias() {
        return markBias;
    }

    boolean isDotLeftToRight() {
        return dotLTR;
    }

    boolean isMarkLeftToRight() {
        return markLTR;
    }

    boolean isPositionLTR(int position, Position.Bias bias) {
        Document doc = component.getDocument();
        if(bias == Position.Bias.Backward && --position < 0)
            position = 0;
        return AbstractDocument.isLeftToRight(doc, position, position);
    }

    Position.Bias guessBiasForOffset(int offset, Position.Bias lastBias,
                                     boolean lastLTR) {
        // There is an abiguous case here. That if your model looks like:
        // abAB with the cursor at abB]A (visual representation of
        // 3 forward) deleting could either become abB] or
        // ab[B. I'ld actually prefer abB]. But, if I implement that
        // a delete at abBA] would result in aBA] vs a[BA which I
        // think is totally wrong. To get this right we need to know what
        // was deleted. And we could get this from the bidi structure
        // in the change event. So:
        // PENDING: base this off what was deleted.
        if(lastLTR != isPositionLTR(offset, lastBias)) {
            lastBias = Position.Bias.Backward;
        }
        else if(lastBias != Position.Bias.Backward &&
                lastLTR != isPositionLTR(offset, Position.Bias.Backward)) {
            lastBias = Position.Bias.Backward;
        }
        if (lastBias == Position.Bias.Backward && offset > 0) {
            try {
                Segment s = new Segment();
                component.getDocument().getText(offset - 1, 1, s);
                if (s.count > 0 && s.array[s.offset] == '\n') {
                    lastBias = Position.Bias.Forward;
                }
            }
            catch (BadLocationException ble) {}
        }
        return lastBias;
    }

    // ---- local methods --------------------------------------------

    /**
     * Sets the caret position (dot) to a new location.  This
     * causes the old and new location to be repainted.  It
     * also makes sure that the caret is within the visible
     * region of the view, if the view is scrollable.
     */
    void changeCaretPosition(int dot, Position.Bias dotBias) {
        // repaint the old position and set the new value of
        // the dot.
        repaint();


        // Make sure the caret is visible if this window has the focus.
        if (flasher != null && flasher.isRunning()) {
            visible = true;
            flasher.restart();
        }

        // notify listeners at the caret moved
        this.dot = dot;
        this.dotBias = dotBias;
        dotLTR = isPositionLTR(dot, dotBias);
        fireStateChanged();

        updateSystemSelection();

        setMagicCaretPosition(null);

        // We try to repaint the caret later, since things
        // may be unstable at the time this is called
        // (i.e. we don't want to depend upon notification
        // order or the fact that this might happen on
        // an unsafe thread).
        Runnable callRepaintNewCaret = new Runnable() {
            public void run() {
                repaintNewCaret();
            }
        };
        SwingUtilities.invokeLater(callRepaintNewCaret);
    }

    /**
     * Repaints the new caret position, with the
     * assumption that this is happening on the
     * event thread so that calling <code>modelToView</code>
     * is safe.
     */
    @SuppressWarnings("deprecation")
    void repaintNewCaret() {
        if (component != null) {
            TextUI mapper = component.getUI();
            Document doc = component.getDocument();
            if ((mapper != null) && (doc != null)) {
                // determine the new location and scroll if
                // not visible.
                Rectangle newLoc;
                try {
                    newLoc = mapper.modelToView(component, this.dot, this.dotBias);
                } catch (BadLocationException e) {
                    newLoc = null;
                }
                if (newLoc != null) {
                    adjustVisibility(newLoc);
                    // If there is no magic caret position, make one
                    if (getMagicCaretPosition() == null) {
                        setMagicCaretPosition(new Point(newLoc.x, newLoc.y));
                    }
                }

                // repaint the new position
                damage(newLoc);
            }
        }
    }

    private void updateSystemSelection() {
        if ( ! SwingUtilities2.canCurrentEventAccessSystemClipboard() ) {
            return;
        }
        if (this.dot != this.mark && component != null && component.hasFocus()) {
            Clipboard clip = getSystemSelection();
            if (clip != null) {
                String selectedText;
                if (component instanceof JPasswordField
                    && component.getClientProperty("JPasswordField.cutCopyAllowed") !=
                    Boolean.TRUE) {
                    //fix for 4793761
                    StringBuilder txt = null;
                    char echoChar = ((JPasswordField)component).getEchoChar();
                    int p0 = Math.min(getDot(), getMark());
                    int p1 = Math.max(getDot(), getMark());
                    for (int i = p0; i < p1; i++) {
                        if (txt == null) {
                            txt = new StringBuilder();
                        }
                        txt.append(echoChar);
                    }
                    selectedText = (txt != null) ? txt.toString() : null;
                } else {
                    selectedText = component.getSelectedText();
                }
                try {
                    clip.setContents(
                        new StringSelection(selectedText), getClipboardOwner());

                    ownsSelection = true;
                } catch (IllegalStateException ise) {
                    // clipboard was unavailable
                    // no need to provide error feedback to user since updating
                    // the system selection is not a user invoked action
                }
            }
        }
    }

    private Clipboard getSystemSelection() {
        try {
            return component.getToolkit().getSystemSelection();
        } catch (HeadlessException he) {
            // do nothing... there is no system clipboard
        } catch (SecurityException se) {
            // do nothing... there is no allowed system clipboard
        }
        return null;
    }

    private ClipboardOwner getClipboardOwner() {
        return handler;
    }

    /**
     * This is invoked after the document changes to verify the current
     * dot/mark is valid. We do this in case the <code>NavigationFilter</code>
     * changed where to position the dot, that resulted in the current location
     * being bogus.
     */
    private void ensureValidPosition() {
        int length = component.getDocument().getLength();
        if (dot > length || mark > length) {
            // Current location is bogus and filter likely vetoed the
            // change, force the reset without giving the filter a
            // chance at changing it.
            handleSetDot(length, Position.Bias.Forward);
        }
    }


    /**
     * Saves the current caret position.  This is used when
     * caret up/down actions occur, moving between lines
     * that have uneven end positions.
     *
     * @param p the position
     * @see #getMagicCaretPosition
     */
    public void setMagicCaretPosition(Point p) {
        magicCaretPosition = p;
    }

    /**
     * Gets the saved caret position.
     *
     * @return the position
     * see #setMagicCaretPosition
     */
    public Point getMagicCaretPosition() {
        return magicCaretPosition;
    }

    /**
     * Compares this object to the specified object.
     * The superclass behavior of comparing rectangles
     * is not desired, so this is changed to the Object
     * behavior.
     *
     * @param     obj   the object to compare this font with
     * @return    <code>true</code> if the objects are equal;
     *            <code>false</code> otherwise
     */
    public boolean equals(Object obj) {
        return (this == obj);
    }

    public String toString() {
        String s = "Dot=(" + dot + ", " + dotBias + ")";
        s += " Mark=(" + mark + ", " + markBias + ")";
        return s;
    }

    private NavigationFilter.FilterBypass getFilterBypass() {
        if (filterBypass == null) {
            filterBypass = new DefaultFilterBypass();
        }
        return filterBypass;
    }

    // Rectangle.contains returns false if passed a rect with a w or h == 0,
    // this won't (assuming X,Y are contained with this rectangle).
    private boolean _contains(int X, int Y, int W, int H) {
        int w = this.width;
        int h = this.height;
        if ((w | h | W | H) < 0) {
            // At least one of the dimensions is negative...
            return false;
        }
        // Note: if any dimension is zero, tests below must return false...
        int x = this.x;
        int y = this.y;
        if (X < x || Y < y) {
            return false;
        }
        if (W > 0) {
            w += x;
            W += X;
            if (W <= X) {
                // X+W overflowed or W was zero, return false if...
                // either original w or W was zero or
                // x+w did not overflow or
                // the overflowed x+w is smaller than the overflowed X+W
                if (w >= x || W > w) return false;
            } else {
                // X+W did not overflow and W was not zero, return false if...
                // original w was zero or
                // x+w did not overflow and x+w is smaller than X+W
                if (w >= x && W > w) return false;
            }
        }
        else if ((x + w) < X) {
            return false;
        }
        if (H > 0) {
            h += y;
            H += Y;
            if (H <= Y) {
                if (h >= y || H > h) return false;
            } else {
                if (h >= y && H > h) return false;
            }
        }
        else if ((y + h) < Y) {
            return false;
        }
        return true;
    }

    int getCaretWidth(int height) {
        if (aspectRatio > -1) {
            return (int) (aspectRatio * height) + 1;
        }

        if (caretWidth > -1) {
            return caretWidth;
        } else {
            Object property = UIManager.get("Caret.width");
            if (property instanceof Integer) {
                return ((Integer) property).intValue();
            } else {
                return 1;
            }
        }
    }

    // --- serialization ---------------------------------------------

    @Serial
    private void readObject(ObjectInputStream s)
      throws ClassNotFoundException, IOException
    {
        ObjectInputStream.GetField f = s.readFields();

        EventListenerList newListenerList = (EventListenerList) f.get("listenerList", null);
        if (newListenerList == null) {
            throw new InvalidObjectException("Null listenerList");
        }
        listenerList = newListenerList;
        component = (JTextComponent) f.get("component", null);
        updatePolicy = f.get("updatePolicy", 0);
        visible = f.get("visible", false);
        active = f.get("active", false);
        dot = f.get("dot", 0);
        mark = f.get("mark", 0);
        selectionTag = f.get("selectionTag", null);
        selectionVisible = f.get("selectionVisible", false);
        flasher = (Timer) f.get("flasher", null);
        magicCaretPosition = (Point) f.get("magicCaretPosition", null);
        dotLTR = f.get("dotLTR", false);
        markLTR = f.get("markLTR", false);
        ownsSelection = f.get("ownsSelection", false);
        forceCaretPositionChange = f.get("forceCaretPositionChange", false);
        caretWidth = f.get("caretWidth", 0);
        aspectRatio = f.get("aspectRatio", 0.0f);

        handler = new Handler();
        if (!s.readBoolean()) {
            dotBias = Position.Bias.Forward;
        }
        else {
            dotBias = Position.Bias.Backward;
        }
        if (!s.readBoolean()) {
            markBias = Position.Bias.Forward;
        }
        else {
            markBias = Position.Bias.Backward;
        }
    }

    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeBoolean((dotBias == Position.Bias.Backward));
        s.writeBoolean((markBias == Position.Bias.Backward));
    }

    // ---- member variables ------------------------------------------

    /**
     * The event listener list.
     */
    protected EventListenerList listenerList = new EventListenerList();

    /**
     * The change event for the model.
     * Only one ChangeEvent is needed per model instance since the
     * event's only (read-only) state is the source property.  The source
     * of events generated here is always "this".
     */
    protected transient ChangeEvent changeEvent = null;

    // package-private to avoid inner classes private member
    // access bug
    JTextComponent component;

    int updatePolicy = UPDATE_WHEN_ON_EDT;
    boolean visible;
    boolean active;
    int dot;
    int mark;
    Object selectionTag;
    boolean selectionVisible;
    Timer flasher;
    Point magicCaretPosition;
    transient Position.Bias dotBias;
    transient Position.Bias markBias;
    boolean dotLTR;
    boolean markLTR;
    transient Handler handler = new Handler();
    private transient int[] flagXPoints = new int[3];
    private transient int[] flagYPoints = new int[3];
    private transient NavigationFilter.FilterBypass filterBypass;
    private static transient Action selectWord = null;
    private static transient Action selectLine = null;
    /**
     * This is used to indicate if the caret currently owns the selection.
     * This is always false if the system does not support the system
     * clipboard.
     */
    private boolean ownsSelection;

    /**
     * If this is true, the location of the dot is updated regardless of
     * the current location. This is set in the DocumentListener
     * such that even if the model location of dot hasn't changed (perhaps do
     * to a forward delete) the visual location is updated.
     */
    private boolean forceCaretPositionChange;

    /**
     * Whether or not mouseReleased should adjust the caret and focus.
     * This flag is set by mousePressed if it wanted to adjust the caret
     * and focus but couldn't because of a possible DnD operation.
     */
    private transient boolean shouldHandleRelease;


    /**
     * holds last MouseEvent which caused the word selection
     */
    private transient MouseEvent selectedWordEvent = null;

    /**
     * The width of the caret in pixels.
     */
    private int caretWidth = -1;
    private float aspectRatio = -1;

    class SafeScroller implements Runnable {

        SafeScroller(Rectangle r) {
            this.r = r;
        }

        public void run() {
            if (component != null) {
                component.scrollRectToVisible(r);
            }
        }

        Rectangle r;
    }


    class Handler implements PropertyChangeListener, DocumentListener, ActionListener, ClipboardOwner {

        // --- ActionListener methods ----------------------------------

        /**
         * Invoked when the blink timer fires.  This is called
         * asynchronously.  The simply changes the visibility
         * and repaints the rectangle that last bounded the caret.
         *
         * @param e the action event
         */
        @SuppressWarnings("deprecation")
        public void actionPerformed(ActionEvent e) {
            if (width == 0 || height == 0) {
                // setVisible(true) will cause a scroll, only do this if the
                // new location is really valid.
                if (component != null) {
                    TextUI mapper = component.getUI();
                    try {
                        Rectangle r = mapper.modelToView(component, dot,
                                                         dotBias);
                        if (r != null && r.width != 0 && r.height != 0) {
                            damage(r);
                        }
                    } catch (BadLocationException ble) {
                    }
                }
            }
            visible = !visible;
            repaint();
        }

        // --- DocumentListener methods --------------------------------

        /**
         * Updates the dot and mark if they were changed by
         * the insertion.
         *
         * @param e the document event
         * @see DocumentListener#insertUpdate
         */
        public void insertUpdate(DocumentEvent e) {
            if (getUpdatePolicy() == NEVER_UPDATE ||
                    (getUpdatePolicy() == UPDATE_WHEN_ON_EDT &&
                    !SwingUtilities.isEventDispatchThread())) {

                if ((e.getOffset() <= dot || e.getOffset() <= mark)
                        && selectionTag != null) {
                    try {
                        component.getHighlighter().changeHighlight(selectionTag,
                                Math.min(dot, mark), Math.max(dot, mark));
                    } catch (BadLocationException e1) {
                        e1.printStackTrace();
                    }
                }
                return;
            }
            int offset = e.getOffset();
            int length = e.getLength();
            int newDot = dot;
            short changed = 0;

            if (e instanceof AbstractDocument.UndoRedoDocumentEvent) {
                setDot(offset + length);
                return;
            }
            if (newDot >= offset) {
                newDot += length;
                changed |= 1;
            }
            int newMark = mark;
            if (newMark >= offset) {
                newMark += length;
                changed |= 2;
            }

            if (changed != 0) {
                Position.Bias dotBias = DefaultCaret.this.dotBias;
                if (dot == offset) {
                    Document doc = component.getDocument();
                    boolean isNewline;
                    try {
                        Segment s = new Segment();
                        doc.getText(newDot - 1, 1, s);
                        isNewline = (s.count > 0 &&
                                s.array[s.offset] == '\n');
                    } catch (BadLocationException ble) {
                        isNewline = false;
                    }
                    if (isNewline) {
                        dotBias = Position.Bias.Forward;
                    } else {
                        dotBias = Position.Bias.Backward;
                    }
                }
                if (newMark == newDot) {
                    setDot(newDot, dotBias);
                    ensureValidPosition();
                }
                else {
                    setDot(newMark, markBias);
                    if (getDot() == newMark) {
                        // Due this test in case the filter vetoed the
                        // change in which case this probably won't be
                        // valid either.
                        moveDot(newDot, dotBias);
                    }
                    ensureValidPosition();
                }
            }
        }

        /**
         * Updates the dot and mark if they were changed
         * by the removal.
         *
         * @param e the document event
         * @see DocumentListener#removeUpdate
         */
        public void removeUpdate(DocumentEvent e) {
            if (getUpdatePolicy() == NEVER_UPDATE ||
                    (getUpdatePolicy() == UPDATE_WHEN_ON_EDT &&
                    !SwingUtilities.isEventDispatchThread())) {

                int length = component.getDocument().getLength();
                dot = Math.min(dot, length);
                mark = Math.min(mark, length);
                if ((e.getOffset() < dot || e.getOffset() < mark)
                        && selectionTag != null) {
                    try {
                        component.getHighlighter().changeHighlight(selectionTag,
                                Math.min(dot, mark), Math.max(dot, mark));
                    } catch (BadLocationException e1) {
                        e1.printStackTrace();
                    }
                }
                return;
            }
            int offs0 = e.getOffset();
            int offs1 = offs0 + e.getLength();
            int newDot = dot;
            boolean adjustDotBias = false;
            int newMark = mark;
            boolean adjustMarkBias = false;

            if(e instanceof AbstractDocument.UndoRedoDocumentEvent) {
                setDot(offs0);
                return;
            }
            if (newDot >= offs1) {
                newDot -= (offs1 - offs0);
                if(newDot == offs1) {
                    adjustDotBias = true;
                }
            } else if (newDot >= offs0) {
                newDot = offs0;
                adjustDotBias = true;
            }
            if (newMark >= offs1) {
                newMark -= (offs1 - offs0);
                if(newMark == offs1) {
                    adjustMarkBias = true;
                }
            } else if (newMark >= offs0) {
                newMark = offs0;
                adjustMarkBias = true;
            }
            if (newMark == newDot) {
                forceCaretPositionChange = true;
                try {
                    setDot(newDot, guessBiasForOffset(newDot, dotBias,
                            dotLTR));
                } finally {
                    forceCaretPositionChange = false;
                }
                ensureValidPosition();
            } else {
                Position.Bias dotBias = DefaultCaret.this.dotBias;
                Position.Bias markBias = DefaultCaret.this.markBias;
                if(adjustDotBias) {
                    dotBias = guessBiasForOffset(newDot, dotBias, dotLTR);
                }
                if(adjustMarkBias) {
                    markBias = guessBiasForOffset(mark, markBias, markLTR);
                }
                setDot(newMark, markBias);
                if (getDot() == newMark) {
                    // Due this test in case the filter vetoed the change
                    // in which case this probably won't be valid either.
                    moveDot(newDot, dotBias);
                }
                ensureValidPosition();
            }
        }

        /**
         * Gives notification that an attribute or set of attributes changed.
         *
         * @param e the document event
         * @see DocumentListener#changedUpdate
         */
        public void changedUpdate(DocumentEvent e) {
            if (getUpdatePolicy() == NEVER_UPDATE ||
                    (getUpdatePolicy() == UPDATE_WHEN_ON_EDT &&
                    !SwingUtilities.isEventDispatchThread())) {
                return;
            }
            if(e instanceof AbstractDocument.UndoRedoDocumentEvent) {
                setDot(e.getOffset() + e.getLength());
            }
        }

        // --- PropertyChangeListener methods -----------------------

        /**
         * This method gets called when a bound property is changed.
         * We are looking for document changes on the editor.
         */
        public void propertyChange(PropertyChangeEvent evt) {
            Object oldValue = evt.getOldValue();
            Object newValue = evt.getNewValue();
            if ((oldValue instanceof Document) || (newValue instanceof Document)) {
                setDot(0);
                if (oldValue != null) {
                    ((Document)oldValue).removeDocumentListener(this);
                }
                if (newValue != null) {
                    ((Document)newValue).addDocumentListener(this);
                }
            } else if("enabled".equals(evt.getPropertyName())) {
                Boolean enabled = (Boolean) evt.getNewValue();
                if(component.isFocusOwner()) {
                    if(enabled == Boolean.TRUE) {
                        if(component.isEditable()) {
                            setVisible(true);
                        }
                        setSelectionVisible(true);
                    } else {
                        setVisible(false);
                        setSelectionVisible(false);
                    }
                }
            } else if("caretWidth".equals(evt.getPropertyName())) {
                Integer newWidth = (Integer) evt.getNewValue();
                if (newWidth != null) {
                    caretWidth = newWidth.intValue();
                } else {
                    caretWidth = -1;
                }
                repaint();
            } else if("caretAspectRatio".equals(evt.getPropertyName())) {
                Number newRatio = (Number) evt.getNewValue();
                if (newRatio != null) {
                    aspectRatio = newRatio.floatValue();
                } else {
                    aspectRatio = -1;
                }
                repaint();
            }
        }


        //
        // ClipboardOwner
        //
        /**
         * Toggles the visibility of the selection when ownership is lost.
         */
        public void lostOwnership(Clipboard clipboard,
                                      Transferable contents) {
            if (ownsSelection) {
                ownsSelection = false;
                if (component != null && !component.hasFocus()) {
                    setSelectionVisible(false);
                }
            }
        }
    }


    private class DefaultFilterBypass extends NavigationFilter.FilterBypass {
        public Caret getCaret() {
            return DefaultCaret.this;
        }

        public void setDot(int dot, Position.Bias bias) {
            handleSetDot(dot, bias);
        }

        public void moveDot(int dot, Position.Bias bias) {
            handleMoveDot(dot, bias);
        }
    }
}
