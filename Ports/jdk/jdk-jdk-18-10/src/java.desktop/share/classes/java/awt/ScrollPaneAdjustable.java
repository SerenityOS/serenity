/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.peer.ScrollPanePeer;
import java.io.Serial;
import java.io.Serializable;

import sun.awt.AWTAccessor;

/**
 * This class represents the state of a horizontal or vertical
 * scrollbar of a {@code ScrollPane}.  Objects of this class are
 * returned by {@code ScrollPane} methods.
 *
 * @since       1.4
 */
public class ScrollPaneAdjustable implements Adjustable, Serializable {

    /**
     * The {@code ScrollPane} this object is a scrollbar of.
     * @serial
     */
    private ScrollPane sp;

    /**
     * Orientation of this scrollbar.
     *
     * @serial
     * @see #getOrientation
     * @see java.awt.Adjustable#HORIZONTAL
     * @see java.awt.Adjustable#VERTICAL
     */
    private int orientation;

    /**
     * The value of this scrollbar.
     * {@code value} should be greater than {@code minimum}
     * and less than {@code maximum}
     *
     * @serial
     * @see #getValue
     * @see #setValue
     */
    private int value;

    /**
     * The minimum value of this scrollbar.
     * This value can only be set by the {@code ScrollPane}.
     * <p>
     * <strong>ATTN:</strong> In current implementation
     * {@code minimum} is always {@code 0}.  This field can
     * only be altered via {@code setSpan} method and
     * {@code ScrollPane} always calls that method with
     * {@code 0} for the minimum.  {@code getMinimum} method
     * always returns {@code 0} without checking this field.
     *
     * @serial
     * @see #getMinimum
     * @see #setSpan(int, int, int)
     */
    private int minimum;

    /**
     * The maximum value of this scrollbar.
     * This value can only be set by the {@code ScrollPane}.
     *
     * @serial
     * @see #getMaximum
     * @see #setSpan(int, int, int)
     */
    private int maximum;

    /**
     * The size of the visible portion of this scrollbar.
     * This value can only be set by the {@code ScrollPane}.
     *
     * @serial
     * @see #getVisibleAmount
     * @see #setSpan(int, int, int)
     */
    private int visibleAmount;

    /**
     * The adjusting status of the {@code Scrollbar}.
     * True if the value is in the process of changing as a result of
     * actions being taken by the user.
     *
     * @see #getValueIsAdjusting
     * @see #setValueIsAdjusting
     * @since 1.4
     */
    private transient boolean isAdjusting;

    /**
     * The amount by which the scrollbar value will change when going
     * up or down by a line.
     * This value should be a non negative integer.
     *
     * @serial
     * @see #getUnitIncrement
     * @see #setUnitIncrement
     */
    private int unitIncrement  = 1;

    /**
     * The amount by which the scrollbar value will change when going
     * up or down by a page.
     * This value should be a non negative integer.
     *
     * @serial
     * @see #getBlockIncrement
     * @see #setBlockIncrement
     */
    private int blockIncrement = 1;

    /**
     * Specified adjustment listener to receive adjustment events from this
     * {@code ScrollPaneAdjustable}.
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private AdjustmentListener adjustmentListener;

    /**
     * Error message for {@code AWTError} reported when one of
     * the public but unsupported methods is called.
     */
    private static final String SCROLLPANE_ONLY =
        "Can be set by scrollpane only";


    /**
     * Initialize JNI field and method ids.
     */
    private static native void initIDs();

    static {
        Toolkit.loadLibraries();
        if (!GraphicsEnvironment.isHeadless()) {
            initIDs();
        }
        AWTAccessor.setScrollPaneAdjustableAccessor(new AWTAccessor.ScrollPaneAdjustableAccessor() {
            public void setTypedValue(final ScrollPaneAdjustable adj,
                                      final int v, final int type) {
                adj.setTypedValue(v, type);
            }
        });
    }

    /**
     * Use serialVersionUID from JDK 1.1 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -3359745691033257079L;


    /**
     * Constructs a new object to represent specified scrollabar
     * of the specified {@code ScrollPane}.
     * Only ScrollPane creates instances of this class.
     * @param sp           {@code ScrollPane}
     * @param l            {@code AdjustmentListener} to add upon creation.
     * @param orientation  specifies which scrollbar this object represents,
     *                     can be either  {@code Adjustable.HORIZONTAL}
     *                     or {@code Adjustable.VERTICAL}.
     */
    ScrollPaneAdjustable(ScrollPane sp, AdjustmentListener l, int orientation) {
        this.sp = sp;
        this.orientation = orientation;
        addAdjustmentListener(l);
    }

    /**
     * This is called by the scrollpane itself to update the
     * {@code minimum}, {@code maximum} and
     * {@code visible} values.  The scrollpane is the only one
     * that should be changing these since it is the source of these
     * values.
     */
    void setSpan(int min, int max, int visible) {
        // adjust the values to be reasonable
        minimum = min;
        maximum = Math.max(max, minimum + 1);
        visibleAmount = Math.min(visible, maximum - minimum);
        visibleAmount = Math.max(visibleAmount, 1);
        blockIncrement = Math.max((int)(visible * .90), 1);
        setValue(value);
    }

    /**
     * Returns the orientation of this scrollbar.
     * @return    the orientation of this scrollbar, either
     *            {@code Adjustable.HORIZONTAL} or
     *            {@code Adjustable.VERTICAL}
     */
    public int getOrientation() {
        return orientation;
    }

    /**
     * This method should <strong>NOT</strong> be called by user code.
     * This method is public for this class to properly implement
     * {@code Adjustable} interface.
     *
     * @throws AWTError Always throws an error when called.
     */
    public void setMinimum(int min) {
        throw new AWTError(SCROLLPANE_ONLY);
    }

    public int getMinimum() {
        // XXX: This relies on setSpan always being called with 0 for
        // the minimum (which is currently true).
        return 0;
    }

    /**
     * This method should <strong>NOT</strong> be called by user code.
     * This method is public for this class to properly implement
     * {@code Adjustable} interface.
     *
     * @throws AWTError Always throws an error when called.
     */
    public void setMaximum(int max) {
        throw new AWTError(SCROLLPANE_ONLY);
    }

    public int getMaximum() {
        return maximum;
    }

    public synchronized void setUnitIncrement(int u) {
        if (u != unitIncrement) {
            unitIncrement = u;
            if (sp.peer != null) {
                ScrollPanePeer peer = (ScrollPanePeer) sp.peer;
                peer.setUnitIncrement(this, u);
            }
        }
    }

    public int getUnitIncrement() {
        return unitIncrement;
    }

    public synchronized void setBlockIncrement(int b) {
        blockIncrement = b;
    }

    public int getBlockIncrement() {
        return blockIncrement;
    }

    /**
     * This method should <strong>NOT</strong> be called by user code.
     * This method is public for this class to properly implement
     * {@code Adjustable} interface.
     *
     * @throws AWTError Always throws an error when called.
     */
    public void setVisibleAmount(int v) {
        throw new AWTError(SCROLLPANE_ONLY);
    }

    public int getVisibleAmount() {
        return visibleAmount;
    }


    /**
     * Sets the {@code valueIsAdjusting} property.
     *
     * @param b new adjustment-in-progress status
     * @see #getValueIsAdjusting
     * @since 1.4
     */
    public void setValueIsAdjusting(boolean b) {
        if (isAdjusting != b) {
            isAdjusting = b;
            AdjustmentEvent e =
                new AdjustmentEvent(this,
                        AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
                        AdjustmentEvent.TRACK, value, b);
            adjustmentListener.adjustmentValueChanged(e);
        }
    }

    /**
     * Returns true if the value is in the process of changing as a
     * result of actions being taken by the user.
     *
     * @return the value of the {@code valueIsAdjusting} property
     * @see #setValueIsAdjusting
     */
    public boolean getValueIsAdjusting() {
        return isAdjusting;
    }

    /**
     * Sets the value of this scrollbar to the specified value.
     * <p>
     * If the value supplied is less than the current minimum or
     * greater than the current maximum, then one of those values is
     * substituted, as appropriate.
     *
     * @param v the new value of the scrollbar
     */
    public void setValue(int v) {
        setTypedValue(v, AdjustmentEvent.TRACK);
    }

    /**
     * Sets the value of this scrollbar to the specified value.
     * <p>
     * If the value supplied is less than the current minimum or
     * greater than the current maximum, then one of those values is
     * substituted, as appropriate. Also, creates and dispatches
     * the AdjustmentEvent with specified type and value.
     *
     * @param v the new value of the scrollbar
     * @param type the type of the scrolling operation occurred
     */
    private void setTypedValue(int v, int type) {
        v = Math.max(v, minimum);
        v = Math.min(v, maximum - visibleAmount);

        if (v != value) {
            value = v;
            // Synchronously notify the listeners so that they are
            // guaranteed to be up-to-date with the Adjustable before
            // it is mutated again.
            AdjustmentEvent e =
                new AdjustmentEvent(this,
                        AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
                        type, value, isAdjusting);
            adjustmentListener.adjustmentValueChanged(e);
        }
    }

    public int getValue() {
        return value;
    }

    /**
     * Adds the specified adjustment listener to receive adjustment
     * events from this {@code ScrollPaneAdjustable}.
     * If {@code l} is {@code null}, no exception is thrown
     * and no action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    l   the adjustment listener.
     * @see      #removeAdjustmentListener
     * @see      #getAdjustmentListeners
     * @see      java.awt.event.AdjustmentListener
     * @see      java.awt.event.AdjustmentEvent
     */
    public synchronized void addAdjustmentListener(AdjustmentListener l) {
        if (l == null) {
            return;
        }
        adjustmentListener = AWTEventMulticaster.add(adjustmentListener, l);
    }

    /**
     * Removes the specified adjustment listener so that it no longer
     * receives adjustment events from this {@code ScrollPaneAdjustable}.
     * If {@code l} is {@code null}, no exception is thrown
     * and no action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param         l     the adjustment listener.
     * @see           #addAdjustmentListener
     * @see           #getAdjustmentListeners
     * @see           java.awt.event.AdjustmentListener
     * @see           java.awt.event.AdjustmentEvent
     * @since         1.1
     */
    public synchronized void removeAdjustmentListener(AdjustmentListener l){
        if (l == null) {
            return;
        }
        adjustmentListener = AWTEventMulticaster.remove(adjustmentListener, l);
    }

    /**
     * Returns an array of all the adjustment listeners
     * registered on this {@code ScrollPaneAdjustable}.
     *
     * @return all of this {@code ScrollPaneAdjustable}'s
     *         {@code AdjustmentListener}s
     *         or an empty array if no adjustment
     *         listeners are currently registered
     *
     * @see           #addAdjustmentListener
     * @see           #removeAdjustmentListener
     * @see           java.awt.event.AdjustmentListener
     * @see           java.awt.event.AdjustmentEvent
     * @since 1.4
     */
    public synchronized AdjustmentListener[] getAdjustmentListeners() {
        return AWTEventMulticaster.getListeners(adjustmentListener,
                                                AdjustmentListener.class);
    }

    /**
     * Returns a string representation of this scrollbar and its values.
     * @return    a string representation of this scrollbar.
     */
    public String toString() {
        return getClass().getName() + "[" + paramString() + "]";
    }

    /**
     * Returns a string representing the state of this scrollbar.
     * This method is intended to be used only for debugging purposes,
     * and the content and format of the returned string may vary
     * between implementations.  The returned string may be empty but
     * may not be {@code null}.
     *
     * @return      the parameter string of this scrollbar.
     */
    public String paramString() {
        return ((orientation == Adjustable.VERTICAL ? "vertical,"
                                                    :"horizontal,")
                + "[0.."+maximum+"]"
                + ",val=" + value
                + ",vis=" + visibleAmount
                + ",unit=" + unitIncrement
                + ",block=" + blockIncrement
                + ",isAdjusting=" + isAdjusting);
    }
}
