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

import java.awt.Component;
import java.awt.Graphics;
import java.beans.BeanProperty;
import java.beans.ConstructorProperties;
import java.beans.JavaBean;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleValue;
import javax.swing.plaf.SplitPaneUI;

/**
 * <code>JSplitPane</code> is used to divide two (and only two)
 * <code>Component</code>s. The two <code>Component</code>s
 * are graphically divided based on the look and feel
 * implementation, and the two <code>Component</code>s can then be
 * interactively resized by the user.
 * Information on using <code>JSplitPane</code> is in
 * <a
 href="https://docs.oracle.com/javase/tutorial/uiswing/components/splitpane.html">How to Use Split Panes</a> in
 * <em>The Java Tutorial</em>.
 * <p>
 * The two <code>Component</code>s in a split pane can be aligned
 * left to right using
 * <code>JSplitPane.HORIZONTAL_SPLIT</code>, or top to bottom using
 * <code>JSplitPane.VERTICAL_SPLIT</code>.
 * The preferred way to change the size of the <code>Component</code>s
 * is to invoke
 * <code>setDividerLocation</code> where <code>location</code> is either
 * the new x or y position, depending on the orientation of the
 * <code>JSplitPane</code>.
 * <p>
 * To resize the <code>Component</code>s to their preferred sizes invoke
 * <code>resetToPreferredSizes</code>.
 * <p>
 * When the user is resizing the <code>Component</code>s the minimum
 * size of the <code>Components</code> is used to determine the
 * maximum/minimum position the <code>Component</code>s
 * can be set to. If the minimum size of the two
 * components is greater than the size of the split pane the divider
 * will not allow you to resize it. To alter the minimum size of a
 * <code>JComponent</code>, see {@link JComponent#setMinimumSize}.
 * <p>
 * When the user resizes the split pane the new space is distributed between
 * the two components based on the <code>resizeWeight</code> property.
 * A value of 0,
 * the default, indicates the right/bottom component gets all the space,
 * where as a value of 1 indicates the left/top component gets all the space.
 * <p>
 * <strong>Warning:</strong> Swing is not thread safe. For more
 * information see <a
 * href="package-summary.html#threading">Swing's Threading
 * Policy</a>.
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
 * @see #setDividerLocation
 * @see #resetToPreferredSizes
 *
 * @author Scott Violet
 * @since 1.2
 */
@JavaBean(defaultProperty = "UI")
@SuppressWarnings("serial") // Same-version serialization only
public class JSplitPane extends JComponent implements Accessible
{
    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "SplitPaneUI";

    /**
     * Vertical split indicates the <code>Component</code>s are
     * split along the y axis.  For example the two
     * <code>Component</code>s will be split one on top of the other.
     */
    public static final int VERTICAL_SPLIT = 0;

    /**
     * Horizontal split indicates the <code>Component</code>s are
     * split along the x axis.  For example the two
     * <code>Component</code>s will be split one to the left of the
     * other.
     */
    public static final int HORIZONTAL_SPLIT = 1;

    /**
     * Used to add a <code>Component</code> to the left of the other
     * <code>Component</code>.
     */
    public static final String LEFT = "left";

    /**
     * Used to add a <code>Component</code> to the right of the other
     * <code>Component</code>.
     */
    public static final String RIGHT = "right";

    /**
     * Used to add a <code>Component</code> above the other
     * <code>Component</code>.
     */
    public static final String TOP = "top";

    /**
     * Used to add a <code>Component</code> below the other
     * <code>Component</code>.
     */
    public static final String BOTTOM = "bottom";

    /**
     * Used to add a <code>Component</code> that will represent the divider.
     */
    public static final String DIVIDER = "divider";

    /**
     * Bound property name for orientation (horizontal or vertical).
     */
    public static final String ORIENTATION_PROPERTY = "orientation";

    /**
     * Bound property name for continuousLayout.
     */
    public static final String CONTINUOUS_LAYOUT_PROPERTY = "continuousLayout";

    /**
     * Bound property name for border.
     */
    public static final String DIVIDER_SIZE_PROPERTY = "dividerSize";

    /**
     * Bound property for oneTouchExpandable.
     */
    public static final String ONE_TOUCH_EXPANDABLE_PROPERTY =
                               "oneTouchExpandable";

    /**
     * Bound property for lastLocation.
     */
    public static final String LAST_DIVIDER_LOCATION_PROPERTY =
                               "lastDividerLocation";

    /**
     * Bound property for the dividerLocation.
     * @since 1.3
     */
    public static final String DIVIDER_LOCATION_PROPERTY = "dividerLocation";

    /**
     * Bound property for weight.
     * @since 1.3
     */
    public static final String RESIZE_WEIGHT_PROPERTY = "resizeWeight";

    /**
     * How the views are split.
     */
    protected int orientation;

    /**
     * Whether or not the views are continuously redisplayed while
     * resizing.
     */
    protected boolean continuousLayout;

    /**
     * The left or top component.
     */
    protected Component leftComponent;

    /**
     * The right or bottom component.
     */
    protected Component rightComponent;

    /**
     * Size of the divider.
     */
    protected int dividerSize;
    private boolean dividerSizeSet = false;

    /**
     * Is a little widget provided to quickly expand/collapse the
     * split pane?
     */
    protected boolean oneTouchExpandable;
    private boolean oneTouchExpandableSet;

    /**
     * Previous location of the split pane.
     */
    protected int lastDividerLocation;

    /**
     * How to distribute extra space.
     */
    private double resizeWeight;

    /**
     * Location of the divider, at least the value that was set, the UI may
     * have a different value.
     */
    private int dividerLocation;


    /**
     * Creates a new <code>JSplitPane</code> configured to arrange the child
     * components side-by-side horizontally, using two buttons for the components.
     */
    public JSplitPane() {
        this(JSplitPane.HORIZONTAL_SPLIT,
                UIManager.getBoolean("SplitPane.continuousLayout"),
                new JButton(UIManager.getString("SplitPane.leftButtonText")),
                new JButton(UIManager.getString("SplitPane.rightButtonText")));
    }


    /**
     * Creates a new <code>JSplitPane</code> configured with the
     * specified orientation.
     *
     * @param newOrientation  <code>JSplitPane.HORIZONTAL_SPLIT</code> or
     *                        <code>JSplitPane.VERTICAL_SPLIT</code>
     * @exception IllegalArgumentException if <code>orientation</code>
     *          is not one of HORIZONTAL_SPLIT or VERTICAL_SPLIT.
     */
    @ConstructorProperties({"orientation"})
    public JSplitPane(int newOrientation) {
        this(newOrientation,
                UIManager.getBoolean("SplitPane.continuousLayout"));
    }


    /**
     * Creates a new <code>JSplitPane</code> with the specified
     * orientation and redrawing style.
     *
     * @param newOrientation  <code>JSplitPane.HORIZONTAL_SPLIT</code> or
     *                        <code>JSplitPane.VERTICAL_SPLIT</code>
     * @param newContinuousLayout  a boolean, true for the components to
     *        redraw continuously as the divider changes position, false
     *        to wait until the divider position stops changing to redraw
     * @exception IllegalArgumentException if <code>orientation</code>
     *          is not one of HORIZONTAL_SPLIT or VERTICAL_SPLIT
     */
    public JSplitPane(int newOrientation,
                      boolean newContinuousLayout) {
        this(newOrientation, newContinuousLayout, null, null);
    }


    /**
     * Creates a new <code>JSplitPane</code> with the specified
     * orientation and the specified components.
     *
     * @param newOrientation  <code>JSplitPane.HORIZONTAL_SPLIT</code> or
     *                        <code>JSplitPane.VERTICAL_SPLIT</code>
     * @param newLeftComponent the <code>Component</code> that will
     *          appear on the left
     *          of a horizontally-split pane, or at the top of a
     *          vertically-split pane
     * @param newRightComponent the <code>Component</code> that will
     *          appear on the right
     *          of a horizontally-split pane, or at the bottom of a
     *          vertically-split pane
     * @exception IllegalArgumentException if <code>orientation</code>
     *          is not one of: HORIZONTAL_SPLIT or VERTICAL_SPLIT
     */
    public JSplitPane(int newOrientation,
                      Component newLeftComponent,
                      Component newRightComponent){
        this(newOrientation,
                UIManager.getBoolean("SplitPane.continuousLayout"),
                newLeftComponent, newRightComponent);
    }


    /**
     * Creates a new <code>JSplitPane</code> with the specified
     * orientation and
     * redrawing style, and with the specified components.
     *
     * @param newOrientation  <code>JSplitPane.HORIZONTAL_SPLIT</code> or
     *                        <code>JSplitPane.VERTICAL_SPLIT</code>
     * @param newContinuousLayout  a boolean, true for the components to
     *        redraw continuously as the divider changes position, false
     *        to wait until the divider position stops changing to redraw
     * @param newLeftComponent the <code>Component</code> that will
     *          appear on the left
     *          of a horizontally-split pane, or at the top of a
     *          vertically-split pane
     * @param newRightComponent the <code>Component</code> that will
     *          appear on the right
     *          of a horizontally-split pane, or at the bottom of a
     *          vertically-split pane
     * @exception IllegalArgumentException if <code>orientation</code>
     *          is not one of HORIZONTAL_SPLIT or VERTICAL_SPLIT
     */
    public JSplitPane(int newOrientation,
                      boolean newContinuousLayout,
                      Component newLeftComponent,
                      Component newRightComponent){
        super();

        dividerLocation = -1;
        setLayout(null);
        setUIProperty("opaque", Boolean.TRUE);
        orientation = newOrientation;
        if (orientation != HORIZONTAL_SPLIT && orientation != VERTICAL_SPLIT)
            throw new IllegalArgumentException("cannot create JSplitPane, " +
                                               "orientation must be one of " +
                                               "JSplitPane.HORIZONTAL_SPLIT " +
                                               "or JSplitPane.VERTICAL_SPLIT");
        continuousLayout = newContinuousLayout;
        if (newLeftComponent != null)
            setLeftComponent(newLeftComponent);
        if (newRightComponent != null)
            setRightComponent(newRightComponent);
        updateUI();

    }


    /**
     * Sets the L&amp;F object that renders this component.
     *
     * @param ui  the <code>SplitPaneUI</code> L&amp;F object
     * @see UIDefaults#getUI
     */
    public void setUI(SplitPaneUI ui) {
        if ((SplitPaneUI)this.ui != ui) {
            super.setUI(ui);
            revalidate();
        }
    }


    /**
     * Returns the <code>SplitPaneUI</code> that is providing the
     * current look and feel.
     *
     * @return the <code>SplitPaneUI</code> object that renders this component
     */
    @BeanProperty(bound = false, expert = true, description
            = "The L&F object that renders this component.")
    public SplitPaneUI getUI() {
        return (SplitPaneUI)ui;
    }


    /**
     * Notification from the <code>UIManager</code> that the L&amp;F has changed.
     * Replaces the current UI object with the latest version from the
     * <code>UIManager</code>.
     *
     * @see JComponent#updateUI
     */
    public void updateUI() {
        setUI((SplitPaneUI)UIManager.getUI(this));
        revalidate();
    }


    /**
     * Returns the name of the L&amp;F class that renders this component.
     *
     * @return the string "SplitPaneUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false, expert = true, description
            = "A string that specifies the name of the L&F class.")
    public String getUIClassID() {
        return uiClassID;
    }


    /**
     * Sets the size of the divider.
     *
     * @param newSize an integer giving the size of the divider in pixels
     */
    @BeanProperty(description
            = "The size of the divider.")
    public void setDividerSize(int newSize) {
        int           oldSize = dividerSize;

        dividerSizeSet = true;
        if (oldSize != newSize) {
            dividerSize = newSize;
            firePropertyChange(DIVIDER_SIZE_PROPERTY, oldSize, newSize);
        }
    }


    /**
     * Returns the size of the divider.
     *
     * @return an integer giving the size of the divider in pixels
     */
    public int getDividerSize() {
        return dividerSize;
    }


    /**
     * Sets the component to the left (or above) the divider.
     *
     * @param comp the <code>Component</code> to display in that position
     */
    public void setLeftComponent(Component comp) {
        if (comp == null) {
            if (leftComponent != null) {
                remove(leftComponent);
                leftComponent = null;
            }
        } else {
            add(comp, JSplitPane.LEFT);
        }
    }


    /**
     * Returns the component to the left (or above) the divider.
     *
     * @return the <code>Component</code> displayed in that position
     */
    @BeanProperty(bound = false, preferred = true, description
            = "The component to the left (or above) the divider.")
    public Component getLeftComponent() {
        return leftComponent;
    }


    /**
     * Sets the component above, or to the left of the divider.
     *
     * @param comp the <code>Component</code> to display in that position
     */
    @BeanProperty(bound = false, description
            = "The component above, or to the left of the divider.")
    public void setTopComponent(Component comp) {
        setLeftComponent(comp);
    }


    /**
     * Returns the component above, or to the left of the divider.
     *
     * @return the <code>Component</code> displayed in that position
     */
    public Component getTopComponent() {
        return leftComponent;
    }


    /**
     * Sets the component to the right (or below) the divider.
     *
     * @param comp the <code>Component</code> to display in that position
     */
    @BeanProperty(bound = false, preferred = true, description
            = "The component to the right (or below) the divider.")
    public void setRightComponent(Component comp) {
        if (comp == null) {
            if (rightComponent != null) {
                remove(rightComponent);
                rightComponent = null;
            }
        } else {
            add(comp, JSplitPane.RIGHT);
        }
    }


    /**
     * Returns the component to the right (or below) the divider.
     *
     * @return the <code>Component</code> displayed in that position
     */
    public Component getRightComponent() {
        return rightComponent;
    }


    /**
     * Sets the component below, or to the right of the divider.
     *
     * @param comp the <code>Component</code> to display in that position
     */
    @BeanProperty(bound = false, description
            = "The component below, or to the right of the divider.")
    public void setBottomComponent(Component comp) {
        setRightComponent(comp);
    }


    /**
     * Returns the component below, or to the right of the divider.
     *
     * @return the <code>Component</code> displayed in that position
     */
    public Component getBottomComponent() {
        return rightComponent;
    }


    /**
     * Sets the value of the <code>oneTouchExpandable</code> property,
     * which must be <code>true</code> for the
     * <code>JSplitPane</code> to provide a UI widget
     * on the divider to quickly expand/collapse the divider.
     * The default value of this property is <code>false</code>.
     * Some look and feels might not support one-touch expanding;
     * they will ignore this property.
     *
     * @param newValue <code>true</code> to specify that the split pane should provide a
     *        collapse/expand widget
     *
     * @see #isOneTouchExpandable
     */
    @BeanProperty(description
            = "UI widget on the divider to quickly expand/collapse the divider.")
    public void setOneTouchExpandable(boolean newValue) {
        boolean           oldValue = oneTouchExpandable;

        oneTouchExpandable = newValue;
        oneTouchExpandableSet = true;
        firePropertyChange(ONE_TOUCH_EXPANDABLE_PROPERTY, oldValue, newValue);
        repaint();
    }


    /**
     * Gets the <code>oneTouchExpandable</code> property.
     *
     * @return the value of the <code>oneTouchExpandable</code> property
     * @see #setOneTouchExpandable
     */
    public boolean isOneTouchExpandable() {
        return oneTouchExpandable;
    }


    /**
     * Sets the last location the divider was at to
     * <code>newLastLocation</code>.
     *
     * @param newLastLocation an integer specifying the last divider location
     *        in pixels, from the left (or upper) edge of the pane to the
     *        left (or upper) edge of the divider
     */
    @BeanProperty(description
            = "The last location the divider was at.")
    public void setLastDividerLocation(int newLastLocation) {
        int               oldLocation = lastDividerLocation;

        lastDividerLocation = newLastLocation;
        firePropertyChange(LAST_DIVIDER_LOCATION_PROPERTY, oldLocation,
                           newLastLocation);
    }


    /**
     * Returns the last location the divider was at.
     *
     * @return an integer specifying the last divider location as a count
     *       of pixels from the left (or upper) edge of the pane to the
     *       left (or upper) edge of the divider
     */
    public int getLastDividerLocation() {
        return lastDividerLocation;
    }


    /**
     * Sets the orientation, or how the splitter is divided. The options
     * are:<ul>
     * <li>JSplitPane.VERTICAL_SPLIT  (above/below orientation of components)
     * <li>JSplitPane.HORIZONTAL_SPLIT  (left/right orientation of components)
     * </ul>
     *
     * @param orientation an integer specifying the orientation
     * @exception IllegalArgumentException if orientation is not one of:
     *        HORIZONTAL_SPLIT or VERTICAL_SPLIT.
     */
    @BeanProperty(enumerationValues = {
            "JSplitPane.HORIZONTAL_SPLIT",
            "JSplitPane.VERTICAL_SPLIT"}, description
            = "The orientation, or how the splitter is divided.")
    public void setOrientation(int orientation) {
        if ((orientation != VERTICAL_SPLIT) &&
            (orientation != HORIZONTAL_SPLIT)) {
           throw new IllegalArgumentException("JSplitPane: orientation must " +
                                              "be one of " +
                                              "JSplitPane.VERTICAL_SPLIT or " +
                                              "JSplitPane.HORIZONTAL_SPLIT");
        }

        int           oldOrientation = this.orientation;

        this.orientation = orientation;
        firePropertyChange(ORIENTATION_PROPERTY, oldOrientation, orientation);
    }


    /**
     * Returns the orientation.
     *
     * @return an integer giving the orientation
     * @see #setOrientation
     */
    public int getOrientation() {
        return orientation;
    }


    /**
     * Sets the value of the <code>continuousLayout</code> property,
     * which must be <code>true</code> for the child components
     * to be continuously
     * redisplayed and laid out during user intervention.
     * The default value of this property is look and feel dependent.
     * Some look and feels might not support continuous layout;
     * they will ignore this property.
     *
     * @param newContinuousLayout  <code>true</code> if the components
     *        should continuously be redrawn as the divider changes position
     * @see #isContinuousLayout
     */
    @BeanProperty(description
            = "Whether the child components are continuously redisplayed and laid out during user intervention.")
    public void setContinuousLayout(boolean newContinuousLayout) {
        boolean           oldCD = continuousLayout;

        continuousLayout = newContinuousLayout;
        firePropertyChange(CONTINUOUS_LAYOUT_PROPERTY, oldCD,
                           newContinuousLayout);
    }


    /**
     * Gets the <code>continuousLayout</code> property.
     *
     * @return the value of the <code>continuousLayout</code> property
     * @see #setContinuousLayout
     */
    public boolean isContinuousLayout() {
        return continuousLayout;
    }

    /**
     * Specifies how to distribute extra space when the size of the split pane
     * changes. A value of 0, the default,
     * indicates the right/bottom component gets all the extra space (the
     * left/top component acts fixed), where as a value of 1 specifies the
     * left/top component gets all the extra space (the right/bottom component
     * acts fixed). Specifically, the left/top component gets (weight * diff)
     * extra space and the right/bottom component gets (1 - weight) * diff
     * extra space.
     *
     * @param value as described above
     * @exception IllegalArgumentException if <code>value</code> is &lt; 0 or &gt; 1
     * @since 1.3
     */
    @BeanProperty(description
            = "Specifies how to distribute extra space when the split pane resizes.")
    public void setResizeWeight(double value) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException("JSplitPane weight must be between 0 and 1");
        }
        double         oldWeight = resizeWeight;

        resizeWeight = value;
        firePropertyChange(RESIZE_WEIGHT_PROPERTY, oldWeight, value);
    }

    /**
     * Returns the number that determines how extra space is distributed.
     * @return how extra space is to be distributed on a resize of the
     *         split pane
     * @since 1.3
     */
    public double getResizeWeight() {
        return resizeWeight;
    }

    /**
     * Lays out the <code>JSplitPane</code> layout based on the preferred size
     * of the children components. This will likely result in changing
     * the divider location.
     */
    public void resetToPreferredSizes() {
        SplitPaneUI         ui = getUI();

        if (ui != null) {
            ui.resetToPreferredSizes(this);
        }
    }


    /**
     * Sets the divider location as a percentage of the
     * <code>JSplitPane</code>'s size.
     * <p>
     * This method is implemented in terms of
     * <code>setDividerLocation(int)</code>.
     * This method immediately changes the size of the split pane based on
     * its current size. If the split pane is not correctly realized and on
     * screen, this method will have no effect (new divider location will
     * become (current size * proportionalLocation) which is 0).
     *
     * @param proportionalLocation  a double-precision floating point value
     *        that specifies a percentage, from zero (top/left) to 1.0
     *        (bottom/right)
     * @exception IllegalArgumentException if the specified location is &lt; 0
     *            or &gt; 1.0
     */
    @BeanProperty(description
            = "The location of the divider.")
    public void setDividerLocation(double proportionalLocation) {
        if (proportionalLocation < 0.0 ||
           proportionalLocation > 1.0) {
            throw new IllegalArgumentException("proportional location must " +
                                               "be between 0.0 and 1.0.");
        }
        if (getOrientation() == VERTICAL_SPLIT) {
            setDividerLocation((int)((double)(getHeight() - getDividerSize()) *
                                     proportionalLocation));
        } else {
            setDividerLocation((int)((double)(getWidth() - getDividerSize()) *
                                     proportionalLocation));
        }
    }


    /**
     * Sets the location of the divider. This is passed off to the
     * look and feel implementation, and then listeners are notified. A value
     * less than 0 implies the divider should be reset to a value that
     * attempts to honor the preferred size of the left/top component.
     * After notifying the listeners, the last divider location is updated,
     * via <code>setLastDividerLocation</code>.
     *
     * @param location an int specifying a UI-specific value (typically a
     *        pixel count)
     */
    @BeanProperty(description
            = "The location of the divider.")
    public void setDividerLocation(int location) {
        int                 oldValue = dividerLocation;

        dividerLocation = location;

        // Notify UI.
        SplitPaneUI         ui = getUI();

        if (ui != null) {
            ui.setDividerLocation(this, location);
        }

        // Then listeners
        firePropertyChange(DIVIDER_LOCATION_PROPERTY, oldValue, location);

        // And update the last divider location.
        setLastDividerLocation(oldValue);
    }


    /**
     * Returns the last value passed to <code>setDividerLocation</code>.
     * The value returned from this method may differ from the actual
     * divider location (if <code>setDividerLocation</code> was passed a
     * value bigger than the current size).
     *
     * @return an integer specifying the location of the divider
     */
    public int getDividerLocation() {
        return dividerLocation;
    }


    /**
     * Returns the minimum location of the divider from the look and feel
     * implementation.
     *
     * @return an integer specifying a UI-specific value for the minimum
     *          location (typically a pixel count); or -1 if the UI is
     *          <code>null</code>
     */
    @BeanProperty(bound = false, description
            = "The minimum location of the divider from the L&F.")
    public int getMinimumDividerLocation() {
        SplitPaneUI         ui = getUI();

        if (ui != null) {
            return ui.getMinimumDividerLocation(this);
        }
        return -1;
    }


    /**
     * Returns the maximum location of the divider from the look and feel
     * implementation.
     *
     * @return an integer specifying a UI-specific value for the maximum
     *          location (typically a pixel count); or -1 if the  UI is
     *          <code>null</code>
     */
    @BeanProperty(bound = false)
    public int getMaximumDividerLocation() {
        SplitPaneUI         ui = getUI();

        if (ui != null) {
            return ui.getMaximumDividerLocation(this);
        }
        return -1;
    }


    /**
     * Removes the child component, <code>component</code> from the
     * pane. Resets the <code>leftComponent</code> or
     * <code>rightComponent</code> instance variable, as necessary.
     *
     * @param component the <code>Component</code> to remove
     */
    public void remove(Component component) {
        if (component == leftComponent) {
            leftComponent = null;
        } else if (component == rightComponent) {
            rightComponent = null;
        }
        super.remove(component);

        // Update the JSplitPane on the screen
        revalidate();
        repaint();
    }


    /**
     * Removes the <code>Component</code> at the specified index.
     * Updates the <code>leftComponent</code> and <code>rightComponent</code>
     * instance variables as necessary, and then messages super.
     *
     * @param index an integer specifying the component to remove, where
     *        1 specifies the left/top component and 2 specifies the
     *        bottom/right component
     */
    public void remove(int index) {
        Component    comp = getComponent(index);

        if (comp == leftComponent) {
            leftComponent = null;
        } else if (comp == rightComponent) {
            rightComponent = null;
        }
        super.remove(index);

        // Update the JSplitPane on the screen
        revalidate();
        repaint();
    }


    /**
     * Removes all the child components from the split pane. Resets the
     * <code>leftComonent</code> and <code>rightComponent</code>
     * instance variables.
     */
    public void removeAll() {
        leftComponent = rightComponent = null;
        super.removeAll();

        // Update the JSplitPane on the screen
        revalidate();
        repaint();
    }


    /**
     * Returns true, so that calls to <code>revalidate</code>
     * on any descendant of this <code>JSplitPane</code>
     * will cause a request to be queued that
     * will validate the <code>JSplitPane</code> and all its descendants.
     *
     * @return true
     * @see JComponent#revalidate
     * @see java.awt.Container#isValidateRoot
     */
    @Override
    @BeanProperty(hidden = true)
    public boolean isValidateRoot() {
        return true;
    }


    /**
     * Adds the specified component to this split pane.
     * If <code>constraints</code> identifies the left/top or
     * right/bottom child component, and a component with that identifier
     * was previously added, it will be removed and then <code>comp</code>
     * will be added in its place. If <code>constraints</code> is not
     * one of the known identifiers the layout manager may throw an
     * <code>IllegalArgumentException</code>.
     * <p>
     * The possible constraints objects (Strings) are:
     * <ul>
     * <li>JSplitPane.TOP
     * <li>JSplitPane.LEFT
     * <li>JSplitPane.BOTTOM
     * <li>JSplitPane.RIGHT
     * </ul>
     * If the <code>constraints</code> object is <code>null</code>,
     * the component is added in the
     * first available position (left/top if open, else right/bottom).
     *
     * @param comp        the component to add
     * @param constraints an <code>Object</code> specifying the
     *                    layout constraints
     *                    (position) for this component
     * @param index       an integer specifying the index in the container's
     *                    list.
     * @exception IllegalArgumentException  if the <code>constraints</code>
     *          object does not match an existing component
     * @see java.awt.Container#addImpl(Component, Object, int)
     */
    protected void addImpl(Component comp, Object constraints, int index)
    {
        Component             toRemove;

        if (constraints != null && !(constraints instanceof String)) {
            throw new IllegalArgumentException("cannot add to layout: " +
                                               "constraint must be a string " +
                                               "(or null)");
        }

        /* If the constraints are null and the left/right component is
           invalid, add it at the left/right component. */
        if (constraints == null) {
            if (getLeftComponent() == null) {
                constraints = JSplitPane.LEFT;
            } else if (getRightComponent() == null) {
                constraints = JSplitPane.RIGHT;
            }
        }

        /* Find the Component that already exists and remove it. */
        if (constraints != null && (constraints.equals(JSplitPane.LEFT) ||
                                   constraints.equals(JSplitPane.TOP))) {
            toRemove = getLeftComponent();
            if (toRemove != null) {
                remove(toRemove);
            }
            leftComponent = comp;
            index = -1;
        } else if (constraints != null &&
                   (constraints.equals(JSplitPane.RIGHT) ||
                    constraints.equals(JSplitPane.BOTTOM))) {
            toRemove = getRightComponent();
            if (toRemove != null) {
                remove(toRemove);
            }
            rightComponent = comp;
            index = -1;
        } else if (constraints != null &&
                constraints.equals(JSplitPane.DIVIDER)) {
            index = -1;
        }
        /* LayoutManager should raise for else condition here. */

        super.addImpl(comp, constraints, index);

        // Update the JSplitPane on the screen
        revalidate();
        repaint();
    }


    /**
     * Subclassed to message the UI with <code>finishedPaintingChildren</code>
     * after super has been messaged, as well as painting the border.
     *
     * @param g the <code>Graphics</code> context within which to paint
     */
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);

        SplitPaneUI        ui = getUI();

        if (ui != null) {
            Graphics           tempG = g.create();
            ui.finishedPaintingChildren(this, tempG);
            tempG.dispose();
        }
    }


    /**
     * See <code>readObject</code> and <code>writeObject</code> in
     * <code>JComponent</code> for more
     * information about serialization in Swing.
     */
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        if (getUIClassID().equals(uiClassID)) {
            byte count = JComponent.getWriteObjCounter(this);
            JComponent.setWriteObjCounter(this, --count);
            if (count == 0 && ui != null) {
                ui.installUI(this);
            }
        }
    }

    void setUIProperty(String propertyName, Object value) {
        if (propertyName == "dividerSize") {
            if (!dividerSizeSet) {
                setDividerSize(((Number)value).intValue());
                dividerSizeSet = false;
            }
        } else if (propertyName == "oneTouchExpandable") {
            if (!oneTouchExpandableSet) {
                setOneTouchExpandable(((Boolean)value).booleanValue());
                oneTouchExpandableSet = false;
            }
        } else {
            super.setUIProperty(propertyName, value);
        }
    }


    /**
     * Returns a string representation of this <code>JSplitPane</code>.
     * This method
     * is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this <code>JSplitPane</code>.
     */
    protected String paramString() {
        String orientationString = (orientation == HORIZONTAL_SPLIT ?
                                    "HORIZONTAL_SPLIT" : "VERTICAL_SPLIT");
        String continuousLayoutString = (continuousLayout ?
                                         "true" : "false");
        String oneTouchExpandableString = (oneTouchExpandable ?
                                           "true" : "false");

        return super.paramString() +
        ",continuousLayout=" + continuousLayoutString +
        ",dividerSize=" + dividerSize +
        ",lastDividerLocation=" + lastDividerLocation +
        ",oneTouchExpandable=" + oneTouchExpandableString +
        ",orientation=" + orientationString;
    }



    ///////////////////////////
    // Accessibility support //
    ///////////////////////////


    /**
     * Gets the AccessibleContext associated with this JSplitPane.
     * For split panes, the AccessibleContext takes the form of an
     * AccessibleJSplitPane.
     * A new AccessibleJSplitPane instance is created if necessary.
     *
     * @return an AccessibleJSplitPane that serves as the
     *         AccessibleContext of this JSplitPane
     */
    @BeanProperty(bound = false, expert = true, description
            = "The AccessibleContext associated with this SplitPane.")
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJSplitPane();
        }
        return accessibleContext;
    }


    /**
     * This class implements accessibility support for the
     * <code>JSplitPane</code> class.  It provides an implementation of the
     * Java Accessibility API appropriate to split pane user-interface elements.
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
    protected class AccessibleJSplitPane extends AccessibleJComponent
        implements AccessibleValue {

        /**
         * Constructs an {@code AccessibleJSplitPane}.
         */
        protected AccessibleJSplitPane() {}

        /**
         * Gets the state set of this object.
         *
         * @return an instance of AccessibleState containing the current state
         * of the object
         * @see AccessibleState
         */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet states = super.getAccessibleStateSet();
            // FIXME: [[[WDW - Should also add BUSY if this implements
            // Adjustable at some point.  If this happens, we probably
            // should also add actions.]]]
            if (getOrientation() == VERTICAL_SPLIT) {
                states.add(AccessibleState.VERTICAL);
            } else {
                states.add(AccessibleState.HORIZONTAL);
            }
            return states;
        }


        /**
         * Get the AccessibleValue associated with this object.  In the
         * implementation of the Java Accessibility API for this class,
         * return this object, which is responsible for implementing the
         * AccessibleValue interface on behalf of itself.
         *
         * @return this object
         */
        public AccessibleValue getAccessibleValue() {
            return this;
        }


        /**
         * Gets the accessible value of this object.
         *
         * @return a localized String describing the value of this object
         */
        public Number getCurrentAccessibleValue() {
            return Integer.valueOf(getDividerLocation());
        }


        /**
         * Sets the value of this object as a Number.
         *
         * @return True if the value was set.
         */
        public boolean setCurrentAccessibleValue(Number n) {
            // TIGER - 4422535
            if (n == null) {
                return false;
            }
            setDividerLocation(n.intValue());
            return true;
        }


        /**
         * Gets the minimum accessible value of this object.
         *
         * @return The minimum value of this object.
         */
        public Number getMinimumAccessibleValue() {
            return Integer.valueOf(getUI().getMinimumDividerLocation(
                                                        JSplitPane.this));
        }


        /**
         * Gets the maximum accessible value of this object.
         *
         * @return The maximum value of this object.
         */
        public Number getMaximumAccessibleValue() {
            return Integer.valueOf(getUI().getMaximumDividerLocation(
                                                        JSplitPane.this));
        }


        /**
         * Gets the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of
         * the object
         * @see AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.SPLIT_PANE;
        }
    } // inner class AccessibleJSplitPane
}
