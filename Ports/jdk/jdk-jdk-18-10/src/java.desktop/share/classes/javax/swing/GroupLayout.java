/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.*;
import static java.awt.Component.BaselineResizeBehavior;
import static javax.swing.LayoutStyle.ComponentPlacement;
import static javax.swing.SwingConstants.HORIZONTAL;
import static javax.swing.SwingConstants.VERTICAL;

/**
 * {@code GroupLayout} is a {@code LayoutManager} that hierarchically
 * groups components in order to position them in a {@code Container}.
 * {@code GroupLayout} is intended for use by builders, but may be
 * hand-coded as well.
 * Grouping is done by instances of the {@link Group Group} class. {@code
 * GroupLayout} supports two types of groups. A sequential group
 * positions its child elements sequentially, one after another. A
 * parallel group aligns its child elements in one of four ways.
 * <p>
 * Each group may contain any number of elements, where an element is
 * a {@code Group}, {@code Component}, or gap. A gap can be thought
 * of as an invisible component with a minimum, preferred and maximum
 * size. In addition {@code GroupLayout} supports a preferred gap,
 * whose value comes from {@code LayoutStyle}.
 * <p>
 * Elements are similar to a spring. Each element has a range as
 * specified by a minimum, preferred and maximum.  Gaps have either a
 * developer-specified range, or a range determined by {@code
 * LayoutStyle}. The range for {@code Component}s is determined from
 * the {@code Component}'s {@code getMinimumSize}, {@code
 * getPreferredSize} and {@code getMaximumSize} methods. In addition,
 * when adding {@code Component}s you may specify a particular range
 * to use instead of that from the component. The range for a {@code
 * Group} is determined by the type of group. A {@code ParallelGroup}'s
 * range is the maximum of the ranges of its elements. A {@code
 * SequentialGroup}'s range is the sum of the ranges of its elements.
 * <p>
 * {@code GroupLayout} treats each axis independently.  That is, there
 * is a group representing the horizontal axis, and a group
 * representing the vertical axis.  The horizontal group is
 * responsible for determining the minimum, preferred and maximum size
 * along the horizontal axis as well as setting the x and width of the
 * components contained in it. The vertical group is responsible for
 * determining the minimum, preferred and maximum size along the
 * vertical axis as well as setting the y and height of the
 * components contained in it. Each {@code Component} must exist in both
 * a horizontal and vertical group, otherwise an {@code IllegalStateException}
 * is thrown during layout, or when the minimum, preferred or
 * maximum size is requested.
 * <p>
 * The following diagram shows a sequential group along the horizontal
 * axis. The sequential group contains three components. A parallel group
 * was used along the vertical axis.
 * <p style="text-align:center">
 * <img src="doc-files/groupLayout.1.gif" alt="Sequential group along the horizontal axis in three components">
 * <p>
 * To reinforce that each axis is treated independently the diagram shows
 * the range of each group and element along each axis. The
 * range of each component has been projected onto the axes,
 * and the groups are rendered in blue (horizontal) and red (vertical).
 * For readability there is a gap between each of the elements in the
 * sequential group.
 * <p>
 * The sequential group along the horizontal axis is rendered as a solid
 * blue line. Notice the sequential group is the sum of the children elements
 * it contains.
 * <p>
 * Along the vertical axis the parallel group is the maximum of the height
 * of each of the components. As all three components have the same height,
 * the parallel group has the same height.
 * <p>
 * The following diagram shows the same three components, but with the
 * parallel group along the horizontal axis and the sequential group along
 * the vertical axis.
 *
 * <p style="text-align:center">
 * <img src="doc-files/groupLayout.2.gif" alt="Sequential group along the vertical axis in three components">
 * <p>
 * As {@code c1} is the largest of the three components, the parallel
 * group is sized to {@code c1}. As {@code c2} and {@code c3} are smaller
 * than {@code c1} they are aligned based on the alignment specified
 * for the component (if specified) or the default alignment of the
 * parallel group. In the diagram {@code c2} and {@code c3} were created
 * with an alignment of {@code LEADING}. If the component orientation were
 * right-to-left then {@code c2} and {@code c3} would be positioned on
 * the opposite side.
 * <p>
 * The following diagram shows a sequential group along both the horizontal
 * and vertical axis.
 * <p style="text-align:center">
 * <img src="doc-files/groupLayout.3.gif" alt="Sequential group along both the horizontal and vertical axis in three components">
 * <p>
 * {@code GroupLayout} provides the ability to insert gaps between
 * {@code Component}s. The size of the gap is determined by an
 * instance of {@code LayoutStyle}. This may be turned on using the
 * {@code setAutoCreateGaps} method.  Similarly, you may use
 * the {@code setAutoCreateContainerGaps} method to insert gaps
 * between components that touch the edge of the parent container and the
 * container.
 * <p>
 * The following builds a panel consisting of two labels in
 * one column, followed by two textfields in the next column:
 * <pre>
 *   JComponent panel = ...;
 *   GroupLayout layout = new GroupLayout(panel);
 *   panel.setLayout(layout);
 *
 *   // Turn on automatically adding gaps between components
 *   layout.setAutoCreateGaps(true);
 *
 *   // Turn on automatically creating gaps between components that touch
 *   // the edge of the container and the container.
 *   layout.setAutoCreateContainerGaps(true);
 *
 *   // Create a sequential group for the horizontal axis.
 *
 *   GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();
 *
 *   // The sequential group in turn contains two parallel groups.
 *   // One parallel group contains the labels, the other the text fields.
 *   // Putting the labels in a parallel group along the horizontal axis
 *   // positions them at the same x location.
 *   //
 *   // Variable indentation is used to reinforce the level of grouping.
 *   hGroup.addGroup(layout.createParallelGroup().
 *            addComponent(label1).addComponent(label2));
 *   hGroup.addGroup(layout.createParallelGroup().
 *            addComponent(tf1).addComponent(tf2));
 *   layout.setHorizontalGroup(hGroup);
 *
 *   // Create a sequential group for the vertical axis.
 *   GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();
 *
 *   // The sequential group contains two parallel groups that align
 *   // the contents along the baseline. The first parallel group contains
 *   // the first label and text field, and the second parallel group contains
 *   // the second label and text field. By using a sequential group
 *   // the labels and text fields are positioned vertically after one another.
 *   vGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).
 *            addComponent(label1).addComponent(tf1));
 *   vGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).
 *            addComponent(label2).addComponent(tf2));
 *   layout.setVerticalGroup(vGroup);
 * </pre>
 * <p>
 * When run the following is produced.
 * <p style="text-align:center">
 * <img src="doc-files/groupLayout.example.png" alt="Produced horizontal/vertical form">
 * <p>
 * This layout consists of the following.
 * <ul><li>The horizontal axis consists of a sequential group containing two
 *         parallel groups.  The first parallel group contains the labels,
 *         and the second parallel group contains the text fields.
 *     <li>The vertical axis consists of a sequential group
 *         containing two parallel groups.  The parallel groups are configured
 *         to align their components along the baseline. The first parallel
 *         group contains the first label and first text field, and
 *         the second group consists of the second label and second
 *         text field.
 * </ul>
 * There are a couple of things to notice in this code:
 * <ul>
 *   <li>You need not explicitly add the components to the container; this
 *       is indirectly done by using one of the {@code add} methods of
 *       {@code Group}.
 *   <li>The various {@code add} methods return
 *       the caller.  This allows for easy chaining of invocations.  For
 *       example, {@code group.addComponent(label1).addComponent(label2);} is
 *       equivalent to
 *       {@code group.addComponent(label1); group.addComponent(label2);}.
 *   <li>There are no public constructors for {@code Group}s; instead
 *       use the create methods of {@code GroupLayout}.
 * </ul>
 *
 * @author Tomas Pavek
 * @author Jan Stola
 * @author Scott Violet
 * @since 1.6
 */
public class GroupLayout implements LayoutManager2 {
    // Used in size calculations
    private static final int MIN_SIZE = 0;

    private static final int PREF_SIZE = 1;

    private static final int MAX_SIZE = 2;

    // Used by prepare, indicates min, pref or max isn't going to be used.
    private static final int SPECIFIC_SIZE = 3;

    private static final int UNSET = Integer.MIN_VALUE;

    // Maximum spring size constrain to avoid integer overflow
    private static final int INFINITE = Integer.MAX_VALUE >> 1;

    /**
     * Indicates the size from the component or gap should be used for a
     * particular range value.
     *
     * @see Group
     */
    public static final int DEFAULT_SIZE = -1;

    /**
     * Indicates the preferred size from the component or gap should
     * be used for a particular range value.
     *
     * @see Group
     */
    public static final int PREFERRED_SIZE = -2;

    // Whether or not we automatically try and create the preferred
    // padding between components.
    private boolean autocreatePadding;

    // Whether or not we automatically try and create the preferred
    // padding between components the touch the edge of the container and
    // the container.
    private boolean autocreateContainerPadding;

    /**
     * Group responsible for layout along the horizontal axis.  This is NOT
     * the user specified group, use getHorizontalGroup to dig that out.
     */
    private Group horizontalGroup;

    /**
     * Group responsible for layout along the vertical axis.  This is NOT
     * the user specified group, use getVerticalGroup to dig that out.
     */
    private Group verticalGroup;

    // Maps from Component to ComponentInfo.  This is used for tracking
    // information specific to a Component.
    private Map<Component,ComponentInfo> componentInfos;

    // Container we're doing layout for.
    private Container host;

    // Used by areParallelSiblings, cached to avoid excessive garbage.
    private Set<Spring> tmpParallelSet;

    // Indicates Springs have changed in some way since last change.
    private boolean springsChanged;

    // Indicates invalidateLayout has been invoked.
    private boolean isValid;

    // Whether or not any preferred padding (or container padding) springs
    // exist
    private boolean hasPreferredPaddingSprings;

    /**
     * The LayoutStyle instance to use, if null the sharedInstance is used.
     */
    private LayoutStyle layoutStyle;

    /**
     * If true, components that are not visible are treated as though they
     * aren't there.
     */
    private boolean honorsVisibility;


    /**
     * Enumeration of the possible ways {@code ParallelGroup} can align
     * its children.
     *
     * @see #createParallelGroup(Alignment)
     * @since 1.6
     */
    public enum Alignment {
        /**
         * Indicates the elements should be
         * aligned to the origin.  For the horizontal axis with a left to
         * right orientation this means aligned to the left edge. For the
         * vertical axis leading means aligned to the top edge.
         *
         * @see #createParallelGroup(Alignment)
         */
        LEADING,

        /**
         * Indicates the elements should be aligned to the end of the
         * region.  For the horizontal axis with a left to right
         * orientation this means aligned to the right edge. For the
         * vertical axis trailing means aligned to the bottom edge.
         *
         * @see #createParallelGroup(Alignment)
         */
        TRAILING,

        /**
         * Indicates the elements should be centered in
         * the region.
         *
         * @see #createParallelGroup(Alignment)
         */
        CENTER,

        /**
         * Indicates the elements should be aligned along
         * their baseline.
         *
         * @see #createParallelGroup(Alignment)
         * @see #createBaselineGroup(boolean,boolean)
         */
        BASELINE
    }


    private static void checkSize(int min, int pref, int max,
            boolean isComponentSpring) {
        checkResizeType(min, isComponentSpring);
        if (!isComponentSpring && pref < 0) {
            throw new IllegalArgumentException("Pref must be >= 0");
        } else if (isComponentSpring) {
            checkResizeType(pref, true);
        }
        checkResizeType(max, isComponentSpring);
        checkLessThan(min, pref);
        checkLessThan(pref, max);
    }

    private static void checkResizeType(int type, boolean isComponentSpring) {
        if (type < 0 && ((isComponentSpring && type != DEFAULT_SIZE &&
                type != PREFERRED_SIZE) ||
                (!isComponentSpring && type != PREFERRED_SIZE))) {
            throw new IllegalArgumentException("Invalid size");
        }
    }

    private static void checkLessThan(int min, int max) {
        if (min >= 0 && max >= 0 && min > max) {
            throw new IllegalArgumentException(
                    "Following is not met: min<=pref<=max");
        }
    }

    /**
     * Creates a {@code GroupLayout} for the specified {@code Container}.
     *
     * @param host the {@code Container} the {@code GroupLayout} is
     *        the {@code LayoutManager} for
     * @throws IllegalArgumentException if host is {@code null}
     */
    public GroupLayout(Container host) {
        if (host == null) {
            throw new IllegalArgumentException("Container must be non-null");
        }
        honorsVisibility = true;
        this.host = host;
        setHorizontalGroup(createParallelGroup(Alignment.LEADING, true));
        setVerticalGroup(createParallelGroup(Alignment.LEADING, true));
        componentInfos = new HashMap<Component,ComponentInfo>();
        tmpParallelSet = new HashSet<Spring>();
    }

    /**
     * Sets whether component visibility is considered when sizing and
     * positioning components. A value of {@code true} indicates that
     * non-visible components should not be treated as part of the
     * layout. A value of {@code false} indicates that components should be
     * positioned and sized regardless of visibility.
     * <p>
     * A value of {@code false} is useful when the visibility of components
     * is dynamically adjusted and you don't want surrounding components and
     * the sizing to change.
     * <p>
     * The specified value is used for components that do not have an
     * explicit visibility specified.
     * <p>
     * The default is {@code true}.
     *
     * @param honorsVisibility whether component visibility is considered when
     *                         sizing and positioning components
     * @see #setHonorsVisibility(Component,Boolean)
     */
    public void setHonorsVisibility(boolean honorsVisibility) {
        if (this.honorsVisibility != honorsVisibility) {
            this.honorsVisibility = honorsVisibility;
            springsChanged = true;
            isValid = false;
            invalidateHost();
        }
    }

    /**
     * Returns whether component visibility is considered when sizing and
     * positioning components.
     *
     * @return whether component visibility is considered when sizing and
     *         positioning components
     */
    public boolean getHonorsVisibility() {
        return honorsVisibility;
    }

    /**
     * Sets whether the component's visibility is considered for
     * sizing and positioning. A value of {@code Boolean.TRUE}
     * indicates that if {@code component} is not visible it should
     * not be treated as part of the layout. A value of {@code false}
     * indicates that {@code component} is positioned and sized
     * regardless of its visibility.  A value of {@code null}
     * indicates the value specified by the single argument method {@code
     * setHonorsVisibility} should be used.
     * <p>
     * If {@code component} is not a child of the {@code Container} this
     * {@code GroupLayout} is managing, it will be added to the
     * {@code Container}.
     *
     * @param component the component
     * @param honorsVisibility whether visibility of this {@code component} should be
     *              considered for sizing and positioning
     * @throws IllegalArgumentException if {@code component} is {@code null}
     * @see #setHonorsVisibility(Component,Boolean)
     */
    public void setHonorsVisibility(Component component,
            Boolean honorsVisibility) {
        if (component == null) {
            throw new IllegalArgumentException("Component must be non-null");
        }
        getComponentInfo(component).setHonorsVisibility(honorsVisibility);
        springsChanged = true;
        isValid = false;
        invalidateHost();
    }

    /**
     * Sets whether a gap between components should automatically be
     * created.  For example, if this is {@code true} and you add two
     * components to a {@code SequentialGroup} a gap between the
     * two components is automatically be created.  The default is
     * {@code false}.
     *
     * @param autoCreatePadding whether a gap between components is
     *        automatically created
     */
    public void setAutoCreateGaps(boolean autoCreatePadding) {
        if (this.autocreatePadding != autoCreatePadding) {
            this.autocreatePadding = autoCreatePadding;
            invalidateHost();
        }
    }

    /**
     * Returns {@code true} if gaps between components are automatically
     * created.
     *
     * @return {@code true} if gaps between components are automatically
     *         created
     */
    public boolean getAutoCreateGaps() {
        return autocreatePadding;
    }

    /**
     * Sets whether a gap between the container and components that
     * touch the border of the container should automatically be
     * created. The default is {@code false}.
     *
     * @param autoCreateContainerPadding whether a gap between the container and
     *        components that touch the border of the container should
     *        automatically be created
     */
    public void setAutoCreateContainerGaps(boolean autoCreateContainerPadding){
        if (this.autocreateContainerPadding != autoCreateContainerPadding) {
            this.autocreateContainerPadding = autoCreateContainerPadding;
            horizontalGroup = createTopLevelGroup(getHorizontalGroup());
            verticalGroup = createTopLevelGroup(getVerticalGroup());
            invalidateHost();
        }
    }

    /**
     * Returns {@code true} if gaps between the container and components that
     * border the container are automatically created.
     *
     * @return {@code true} if gaps between the container and components that
     *         border the container are automatically created
     */
    public boolean getAutoCreateContainerGaps() {
        return autocreateContainerPadding;
    }

    /**
     * Sets the {@code Group} that positions and sizes
     * components along the horizontal axis.
     *
     * @param group the {@code Group} that positions and sizes
     *        components along the horizontal axis
     * @throws IllegalArgumentException if group is {@code null}
     */
    public void setHorizontalGroup(Group group) {
        if (group == null) {
            throw new IllegalArgumentException("Group must be non-null");
        }
        horizontalGroup = createTopLevelGroup(group);
        invalidateHost();
    }

    /**
     * Returns the {@code Group} that positions and sizes components
     * along the horizontal axis.
     *
     * @return the {@code Group} responsible for positioning and
     *         sizing component along the horizontal axis
     */
    private Group getHorizontalGroup() {
        int index = 0;
        if (horizontalGroup.springs.size() > 1) {
            index = 1;
        }
        return (Group)horizontalGroup.springs.get(index);
    }

    /**
     * Sets the {@code Group} that positions and sizes
     * components along the vertical axis.
     *
     * @param group the {@code Group} that positions and sizes
     *        components along the vertical axis
     * @throws IllegalArgumentException if group is {@code null}
     */
    public void setVerticalGroup(Group group) {
        if (group == null) {
            throw new IllegalArgumentException("Group must be non-null");
        }
        verticalGroup = createTopLevelGroup(group);
        invalidateHost();
    }

    /**
     * Returns the {@code Group} that positions and sizes components
     * along the vertical axis.
     *
     * @return the {@code Group} responsible for positioning and
     *         sizing component along the vertical axis
     */
    private Group getVerticalGroup() {
        int index = 0;
        if (verticalGroup.springs.size() > 1) {
            index = 1;
        }
        return (Group)verticalGroup.springs.get(index);
    }

    /**
     * Wraps the user specified group in a sequential group.  If
     * container gaps should be generated the necessary springs are
     * added.
     */
    private Group createTopLevelGroup(Group specifiedGroup) {
        SequentialGroup group = createSequentialGroup();
        if (getAutoCreateContainerGaps()) {
            group.addSpring(new ContainerAutoPreferredGapSpring());
            group.addGroup(specifiedGroup);
            group.addSpring(new ContainerAutoPreferredGapSpring());
        } else {
            group.addGroup(specifiedGroup);
        }
        return group;
    }

    /**
     * Creates and returns a {@code SequentialGroup}.
     *
     * @return a new {@code SequentialGroup}
     */
    public SequentialGroup createSequentialGroup() {
        return new SequentialGroup();
    }

    /**
     * Creates and returns a {@code ParallelGroup} with an alignment of
     * {@code Alignment.LEADING}.  This is a cover method for the more
     * general {@code createParallelGroup(Alignment)} method.
     *
     * @return a new {@code ParallelGroup}
     * @see #createParallelGroup(Alignment)
     */
    public ParallelGroup createParallelGroup() {
        return createParallelGroup(Alignment.LEADING);
    }

    /**
     * Creates and returns a {@code ParallelGroup} with the specified
     * alignment.  This is a cover method for the more general {@code
     * createParallelGroup(Alignment,boolean)} method with {@code true}
     * supplied for the second argument.
     *
     * @param alignment the alignment for the elements of the group
     * @throws IllegalArgumentException if {@code alignment} is {@code null}
     * @return a new {@code ParallelGroup}
     * @see #createBaselineGroup
     * @see ParallelGroup
     */
    public ParallelGroup createParallelGroup(Alignment alignment) {
        return createParallelGroup(alignment, true);
    }

    /**
     * Creates and returns a {@code ParallelGroup} with the specified
     * alignment and resize behavior. The {@code
     * alignment} argument specifies how children elements are
     * positioned that do not fill the group. For example, if a {@code
     * ParallelGroup} with an alignment of {@code TRAILING} is given
     * 100 and a child only needs 50, the child is
     * positioned at the position 50 (with a component orientation of
     * left-to-right).
     * <p>
     * Baseline alignment is only useful when used along the vertical
     * axis. A {@code ParallelGroup} created with a baseline alignment
     * along the horizontal axis is treated as {@code LEADING}.
     * <p>
     * Refer to {@link GroupLayout.ParallelGroup ParallelGroup} for details on
     * the behavior of baseline groups.
     *
     * @param alignment the alignment for the elements of the group
     * @param resizable {@code true} if the group is resizable; if the group
     *        is not resizable the preferred size is used for the
     *        minimum and maximum size of the group
     * @throws IllegalArgumentException if {@code alignment} is {@code null}
     * @return a new {@code ParallelGroup}
     * @see #createBaselineGroup
     * @see GroupLayout.ParallelGroup
     */
    public ParallelGroup createParallelGroup(Alignment alignment,
            boolean resizable){
        if (alignment == null) {
            throw new IllegalArgumentException("alignment must be non null");
        }

        if (alignment == Alignment.BASELINE) {
            return new BaselineGroup(resizable);
        }
        return new ParallelGroup(alignment, resizable);
    }

    /**
     * Creates and returns a {@code ParallelGroup} that aligns its
     * elements along the baseline.
     *
     * @param resizable whether the group is resizable
     * @param anchorBaselineToTop whether the baseline is anchored to
     *        the top or bottom of the group
     * @return the {@code ParallelGroup}
     * @see #createBaselineGroup
     * @see ParallelGroup
     */
    public ParallelGroup createBaselineGroup(boolean resizable,
            boolean anchorBaselineToTop) {
        return new BaselineGroup(resizable, anchorBaselineToTop);
    }

    /**
     * Forces the specified components to have the same size
     * regardless of their preferred, minimum or maximum sizes. Components that
     * are linked are given the maximum of the preferred size of each of
     * the linked components. For example, if you link two components with
     * a preferred width of 10 and 20, both components are given a width of 20.
     * <p>
     * This can be used multiple times to force any number of
     * components to share the same size.
     * <p>
     * Linked Components are not be resizable.
     *
     * @param components the {@code Component}s that are to have the same size
     * @throws IllegalArgumentException if {@code components} is
     *         {@code null}, or contains {@code null}
     * @see #linkSize(int,Component[])
     */
    public void linkSize(Component... components) {
        linkSize(SwingConstants.HORIZONTAL, components);
        linkSize(SwingConstants.VERTICAL, components);
    }

    /**
     * Forces the specified components to have the same size along the
     * specified axis regardless of their preferred, minimum or
     * maximum sizes. Components that are linked are given the maximum
     * of the preferred size of each of the linked components. For
     * example, if you link two components along the horizontal axis
     * and the preferred width is 10 and 20, both components are given
     * a width of 20.
     * <p>
     * This can be used multiple times to force any number of
     * components to share the same size.
     * <p>
     * Linked {@code Component}s are not be resizable.
     *
     * @param axis the axis to link the size along; one of
     *             {@code SwingConstants.HORIZONTAL} or
     *             {@code SwingConstants.VERTICAL}
     * @param components the {@code Component}s that are to have the same size
     * @throws IllegalArgumentException if {@code components} is
     *         {@code null}, or contains {@code null}; or {@code axis}
     *          is not {@code SwingConstants.HORIZONTAL} or
     *          {@code SwingConstants.VERTICAL}
     */
    public void linkSize(int axis, Component... components) {
        if (components == null) {
            throw new IllegalArgumentException("Components must be non-null");
        }
        for (int counter = components.length - 1; counter >= 0; counter--) {
            Component c = components[counter];
            if (components[counter] == null) {
                throw new IllegalArgumentException(
                        "Components must be non-null");
            }
            // Force the component to be added
            getComponentInfo(c);
        }
        int glAxis;
        if (axis == SwingConstants.HORIZONTAL) {
            glAxis = HORIZONTAL;
        } else if (axis == SwingConstants.VERTICAL) {
            glAxis = VERTICAL;
        } else {
            throw new IllegalArgumentException("Axis must be one of " +
                    "SwingConstants.HORIZONTAL or SwingConstants.VERTICAL");
        }
        LinkInfo master = getComponentInfo(
                components[components.length - 1]).getLinkInfo(glAxis);
        for (int counter = components.length - 2; counter >= 0; counter--) {
            master.add(getComponentInfo(components[counter]));
        }
        invalidateHost();
    }

    /**
     * Replaces an existing component with a new one.
     *
     * @param existingComponent the component that should be removed
     *        and replaced with {@code newComponent}
     * @param newComponent the component to put in
     *        {@code existingComponent}'s place
     * @throws IllegalArgumentException if either of the components are
     *         {@code null} or {@code existingComponent} is not being managed
     *         by this layout manager
     */
    public void replace(Component existingComponent, Component newComponent) {
        if (existingComponent == null || newComponent == null) {
            throw new IllegalArgumentException("Components must be non-null");
        }
        // Make sure all the components have been registered, otherwise we may
        // not update the correct Springs.
        if (springsChanged) {
            registerComponents(horizontalGroup, HORIZONTAL);
            registerComponents(verticalGroup, VERTICAL);
        }
        ComponentInfo info = componentInfos.remove(existingComponent);
        if (info == null) {
            throw new IllegalArgumentException("Component must already exist");
        }
        host.remove(existingComponent);
        if (newComponent.getParent() != host) {
            host.add(newComponent);
        }
        info.setComponent(newComponent);
        componentInfos.put(newComponent, info);
        invalidateHost();
    }

    /**
     * Sets the {@code LayoutStyle} used to calculate the preferred
     * gaps between components. A value of {@code null} indicates the
     * shared instance of {@code LayoutStyle} should be used.
     *
     * @param layoutStyle the {@code LayoutStyle} to use
     * @see LayoutStyle
     */
    public void setLayoutStyle(LayoutStyle layoutStyle) {
        this.layoutStyle = layoutStyle;
        invalidateHost();
    }

    /**
     * Returns the {@code LayoutStyle} used for calculating the preferred
     * gap between components. This returns the value specified to
     * {@code setLayoutStyle}, which may be {@code null}.
     *
     * @return the {@code LayoutStyle} used for calculating the preferred
     *         gap between components
     */
    public LayoutStyle getLayoutStyle() {
        return layoutStyle;
    }

    private LayoutStyle getLayoutStyle0() {
        LayoutStyle layoutStyle = getLayoutStyle();
        if (layoutStyle == null) {
            layoutStyle = LayoutStyle.getInstance();
        }
        return layoutStyle;
    }

    private void invalidateHost() {
        if (host instanceof JComponent) {
            ((JComponent)host).revalidate();
        } else {
            host.invalidate();
        }
        host.repaint();
    }

    //
    // LayoutManager
    //
    /**
     * Notification that a {@code Component} has been added to
     * the parent container.  You should not invoke this method
     * directly, instead you should use one of the {@code Group}
     * methods to add a {@code Component}.
     *
     * @param name the string to be associated with the component
     * @param component the {@code Component} to be added
     */
    public void addLayoutComponent(String name, Component component) {
    }

    /**
     * Notification that a {@code Component} has been removed from
     * the parent container.  You should not invoke this method
     * directly, instead invoke {@code remove} on the parent
     * {@code Container}.
     *
     * @param component the component to be removed
     * @see java.awt.Component#remove
     */
    public void removeLayoutComponent(Component component) {
        ComponentInfo info = componentInfos.remove(component);
        if (info != null) {
            info.dispose();
            springsChanged = true;
            isValid = false;
        }
    }

    /**
     * Returns the preferred size for the specified container.
     *
     * @param parent the container to return the preferred size for
     * @return the preferred size for {@code parent}
     * @throws IllegalArgumentException if {@code parent} is not
     *         the same {@code Container} this was created with
     * @throws IllegalStateException if any of the components added to
     *         this layout are not in both a horizontal and vertical group
     * @see java.awt.Container#getPreferredSize
     */
    public Dimension preferredLayoutSize(Container parent) {
        checkParent(parent);
        prepare(PREF_SIZE);
        return adjustSize(horizontalGroup.getPreferredSize(HORIZONTAL),
                verticalGroup.getPreferredSize(VERTICAL));
    }

    /**
     * Returns the minimum size for the specified container.
     *
     * @param parent the container to return the size for
     * @return the minimum size for {@code parent}
     * @throws IllegalArgumentException if {@code parent} is not
     *         the same {@code Container} that this was created with
     * @throws IllegalStateException if any of the components added to
     *         this layout are not in both a horizontal and vertical group
     * @see java.awt.Container#getMinimumSize
     */
    public Dimension minimumLayoutSize(Container parent) {
        checkParent(parent);
        prepare(MIN_SIZE);
        return adjustSize(horizontalGroup.getMinimumSize(HORIZONTAL),
                verticalGroup.getMinimumSize(VERTICAL));
    }

    /**
     * Lays out the specified container.
     *
     * @param parent the container to be laid out
     * @throws IllegalStateException if any of the components added to
     *         this layout are not in both a horizontal and vertical group
     */
    public void layoutContainer(Container parent) {
        // Step 1: Prepare for layout.
        prepare(SPECIFIC_SIZE);
        Insets insets = parent.getInsets();
        int width = parent.getWidth() - insets.left - insets.right;
        int height = parent.getHeight() - insets.top - insets.bottom;
        boolean ltr = isLeftToRight();
        if (getAutoCreateGaps() || getAutoCreateContainerGaps() ||
                hasPreferredPaddingSprings) {
            // Step 2: Calculate autopadding springs
            calculateAutopadding(horizontalGroup, HORIZONTAL, SPECIFIC_SIZE, 0,
                    width);
            calculateAutopadding(verticalGroup, VERTICAL, SPECIFIC_SIZE, 0,
                    height);
        }
        // Step 3: set the size of the groups.
        horizontalGroup.setSize(HORIZONTAL, 0, width);
        verticalGroup.setSize(VERTICAL, 0, height);
        // Step 4: apply the size to the components.
        for (ComponentInfo info : componentInfos.values()) {
            info.setBounds(insets, width, ltr);
        }
    }

    //
    // LayoutManager2
    //
    /**
     * Notification that a {@code Component} has been added to
     * the parent container.  You should not invoke this method
     * directly, instead you should use one of the {@code Group}
     * methods to add a {@code Component}.
     *
     * @param component the component added
     * @param constraints description of where to place the component
     */
    public void addLayoutComponent(Component component, Object constraints) {
    }

    /**
     * Returns the maximum size for the specified container.
     *
     * @param parent the container to return the size for
     * @return the maximum size for {@code parent}
     * @throws IllegalArgumentException if {@code parent} is not
     *         the same {@code Container} that this was created with
     * @throws IllegalStateException if any of the components added to
     *         this layout are not in both a horizontal and vertical group
     * @see java.awt.Container#getMaximumSize
     */
    public Dimension maximumLayoutSize(Container parent) {
        checkParent(parent);
        prepare(MAX_SIZE);
        return adjustSize(horizontalGroup.getMaximumSize(HORIZONTAL),
                verticalGroup.getMaximumSize(VERTICAL));
    }

    /**
     * Returns the alignment along the x axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     *
     * @param parent the {@code Container} hosting this {@code LayoutManager}
     * @throws IllegalArgumentException if {@code parent} is not
     *         the same {@code Container} that this was created with
     * @return the alignment; this implementation returns {@code .5}
     */
    public float getLayoutAlignmentX(Container parent) {
        checkParent(parent);
        return .5f;
    }

    /**
     * Returns the alignment along the y axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     *
     * @param parent the {@code Container} hosting this {@code LayoutManager}
     * @throws IllegalArgumentException if {@code parent} is not
     *         the same {@code Container} that this was created with
     * @return alignment; this implementation returns {@code .5}
     */
    public float getLayoutAlignmentY(Container parent) {
        checkParent(parent);
        return .5f;
    }

    /**
     * Invalidates the layout, indicating that if the layout manager
     * has cached information it should be discarded.
     *
     * @param parent the {@code Container} hosting this LayoutManager
     * @throws IllegalArgumentException if {@code parent} is not
     *         the same {@code Container} that this was created with
     */
    public void invalidateLayout(Container parent) {
        checkParent(parent);
        // invalidateLayout is called from Container.invalidate, which
        // does NOT grab the treelock.  All other methods do.  To make sure
        // there aren't any possible threading problems we grab the tree lock
        // here.
        synchronized(parent.getTreeLock()) {
            isValid = false;
        }
    }

    private void prepare(int sizeType) {
        boolean visChanged = false;
        // Step 1: If not-valid, clear springs and update visibility.
        if (!isValid) {
            isValid = true;
            horizontalGroup.setSize(HORIZONTAL, UNSET, UNSET);
            verticalGroup.setSize(VERTICAL, UNSET, UNSET);
            for (ComponentInfo ci : componentInfos.values()) {
                if (ci.updateVisibility()) {
                    visChanged = true;
                }
                ci.clearCachedSize();
            }
        }
        // Step 2: Make sure components are bound to ComponentInfos
        if (springsChanged) {
            registerComponents(horizontalGroup, HORIZONTAL);
            registerComponents(verticalGroup, VERTICAL);
        }
        // Step 3: Adjust the autopadding. This removes existing
        // autopadding, then recalculates where it should go.
        if (springsChanged || visChanged) {
            checkComponents();
            horizontalGroup.removeAutopadding();
            verticalGroup.removeAutopadding();
            if (getAutoCreateGaps()) {
                insertAutopadding(true);
            } else if (hasPreferredPaddingSprings ||
                    getAutoCreateContainerGaps()) {
                insertAutopadding(false);
            }
            springsChanged = false;
        }
        // Step 4: (for min/pref/max size calculations only) calculate the
        // autopadding. This invokes for unsetting the calculated values, then
        // recalculating them.
        // If sizeType == SPECIFIC_SIZE, it indicates we're doing layout, this
        // step will be done later on.
        if (sizeType != SPECIFIC_SIZE && (getAutoCreateGaps() ||
                getAutoCreateContainerGaps() || hasPreferredPaddingSprings)) {
            calculateAutopadding(horizontalGroup, HORIZONTAL, sizeType, 0, 0);
            calculateAutopadding(verticalGroup, VERTICAL, sizeType, 0, 0);
        }
    }

    private void calculateAutopadding(Group group, int axis, int sizeType,
            int origin, int size) {
        group.unsetAutopadding();
        switch(sizeType) {
            case MIN_SIZE:
                size = group.getMinimumSize(axis);
                break;
            case PREF_SIZE:
                size = group.getPreferredSize(axis);
                break;
            case MAX_SIZE:
                size = group.getMaximumSize(axis);
                break;
            default:
                break;
        }
        group.setSize(axis, origin, size);
        group.calculateAutopadding(axis);
    }

    private void checkComponents() {
        for (ComponentInfo info : componentInfos.values()) {
            if (info.horizontalSpring == null) {
                throw new IllegalStateException(info.component +
                        " is not attached to a horizontal group");
            }
            if (info.verticalSpring == null) {
                throw new IllegalStateException(info.component +
                        " is not attached to a vertical group");
            }
        }
    }

    private void registerComponents(Group group, int axis) {
        List<Spring> springs = group.springs;
        for (int counter = springs.size() - 1; counter >= 0; counter--) {
            Spring spring = springs.get(counter);
            if (spring instanceof ComponentSpring) {
                ((ComponentSpring)spring).installIfNecessary(axis);
            } else if (spring instanceof Group) {
                registerComponents((Group)spring, axis);
            }
        }
    }

    private Dimension adjustSize(int width, int height) {
        Insets insets = host.getInsets();
        return new Dimension(width + insets.left + insets.right,
                height + insets.top + insets.bottom);
    }

    private void checkParent(Container parent) {
        if (parent != host) {
            throw new IllegalArgumentException(
                    "GroupLayout can only be used with one Container at a time");
        }
    }

    /**
     * Returns the {@code ComponentInfo} for the specified Component,
     * creating one if necessary.
     */
    private ComponentInfo getComponentInfo(Component component) {
        ComponentInfo info = componentInfos.get(component);
        if (info == null) {
            info = new ComponentInfo(component);
            componentInfos.put(component, info);
            if (component.getParent() != host) {
                host.add(component);
            }
        }
        return info;
    }

    /**
     * Adjusts the autopadding springs for the horizontal and vertical
     * groups.  If {@code insert} is {@code true} this will insert auto padding
     * springs, otherwise this will only adjust the springs that
     * comprise auto preferred padding springs.
     */
    private void insertAutopadding(boolean insert) {
        horizontalGroup.insertAutopadding(HORIZONTAL,
                new ArrayList<AutoPreferredGapSpring>(1),
                new ArrayList<AutoPreferredGapSpring>(1),
                new ArrayList<ComponentSpring>(1),
                new ArrayList<ComponentSpring>(1), insert);
        verticalGroup.insertAutopadding(VERTICAL,
                new ArrayList<AutoPreferredGapSpring>(1),
                new ArrayList<AutoPreferredGapSpring>(1),
                new ArrayList<ComponentSpring>(1),
                new ArrayList<ComponentSpring>(1), insert);
    }

    /**
     * Returns {@code true} if the two Components have a common ParallelGroup
     * ancestor along the particular axis.
     */
    private boolean areParallelSiblings(Component source, Component target,
            int axis) {
        ComponentInfo sourceInfo = getComponentInfo(source);
        ComponentInfo targetInfo = getComponentInfo(target);
        Spring sourceSpring;
        Spring targetSpring;
        if (axis == HORIZONTAL) {
            sourceSpring = sourceInfo.horizontalSpring;
            targetSpring = targetInfo.horizontalSpring;
        } else {
            sourceSpring = sourceInfo.verticalSpring;
            targetSpring = targetInfo.verticalSpring;
        }
        Set<Spring> sourcePath = tmpParallelSet;
        sourcePath.clear();
        Spring spring = sourceSpring.getParent();
        while (spring != null) {
            sourcePath.add(spring);
            spring = spring.getParent();
        }
        spring = targetSpring.getParent();
        while (spring != null) {
            if (sourcePath.contains(spring)) {
                sourcePath.clear();
                while (spring != null) {
                    if (spring instanceof ParallelGroup) {
                        return true;
                    }
                    spring = spring.getParent();
                }
                return false;
            }
            spring = spring.getParent();
        }
        sourcePath.clear();
        return false;
    }

    private boolean isLeftToRight() {
        return host.getComponentOrientation().isLeftToRight();
    }

    /**
     * Returns a string representation of this {@code GroupLayout}.
     * This method is intended to be used for debugging purposes,
     * and the content and format of the returned string may vary
     * between implementations.
     *
     * @return a string representation of this {@code GroupLayout}
     **/
    public String toString() {
        if (springsChanged) {
            registerComponents(horizontalGroup, HORIZONTAL);
            registerComponents(verticalGroup, VERTICAL);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("HORIZONTAL\n");
        createSpringDescription(sb, horizontalGroup, "  ", HORIZONTAL);
        sb.append("\nVERTICAL\n");
        createSpringDescription(sb, verticalGroup, "  ", VERTICAL);
        return sb.toString();
    }

    private void createSpringDescription(StringBuilder sb, Spring spring,
            String indent, int axis) {
        String origin = "";
        String padding = "";
        if (spring instanceof ComponentSpring) {
            ComponentSpring cSpring = (ComponentSpring)spring;
            origin = Integer.toString(cSpring.getOrigin()) + " ";
            String name = cSpring.getComponent().getName();
            if (name != null) {
                origin = "name=" + name + ", ";
            }
        }
        if (spring instanceof AutoPreferredGapSpring) {
            AutoPreferredGapSpring paddingSpring =
                    (AutoPreferredGapSpring)spring;
            padding = ", userCreated=" + paddingSpring.getUserCreated() +
                    ", matches=" + paddingSpring.getMatchDescription();
        }
        sb.append(indent).append(spring.getClass().getName()).append(' ')
                .append(Integer.toHexString(spring.hashCode())).append(' ')
                .append(origin).append(", size=").append(spring.getSize())
                .append(", alignment=").append(spring.getAlignment())
                .append(" prefs=[").append(spring.getMinimumSize(axis))
                .append(' ').append(spring.getPreferredSize(axis)).append(' ')
                .append(spring.getMaximumSize(axis)).append(padding)
                .append("]\n");
        if (spring instanceof Group) {
            List<Spring> springs = ((Group)spring).springs;
            indent += "  ";
            for (int counter = 0; counter < springs.size(); counter++) {
                createSpringDescription(sb, springs.get(counter), indent,
                        axis);
            }
        }
    }


    /**
     * Spring consists of a range: min, pref and max, a value some where in
     * the middle of that, and a location. Spring caches the
     * min/max/pref.  If the min/pref/max has internally changes, or needs
     * to be updated you must invoke clear.
     */
    private abstract class Spring {
        private int size;
        private int min;
        private int max;
        private int pref;
        private Spring parent;

        private Alignment alignment;

        Spring() {
            min = pref = max = UNSET;
        }

        /**
         * Calculates and returns the minimum size.
         *
         * @param axis the axis of layout; one of HORIZONTAL or VERTICAL
         * @return the minimum size
         */
        abstract int calculateMinimumSize(int axis);

        /**
         * Calculates and returns the preferred size.
         *
         * @param axis the axis of layout; one of HORIZONTAL or VERTICAL
         * @return the preferred size
         */
        abstract int calculatePreferredSize(int axis);

        /**
         * Calculates and returns the minimum size.
         *
         * @param axis the axis of layout; one of HORIZONTAL or VERTICAL
         * @return the minimum size
         */
        abstract int calculateMaximumSize(int axis);

        /**
         * Sets the parent of this Spring.
         */
        void setParent(Spring parent) {
            this.parent = parent;
        }

        /**
         * Returns the parent of this spring.
         */
        Spring getParent() {
            return parent;
        }

        // This is here purely as a convenience for ParallelGroup to avoid
        // having to track alignment separately.
        void setAlignment(Alignment alignment) {
            this.alignment = alignment;
        }

        /**
         * Alignment for this Spring, this may be null.
         */
        Alignment getAlignment() {
            return alignment;
        }

        /**
         * Returns the minimum size.
         */
        final int getMinimumSize(int axis) {
            if (min == UNSET) {
                min = constrain(calculateMinimumSize(axis));
            }
            return min;
        }

        /**
         * Returns the preferred size.
         */
        final int getPreferredSize(int axis) {
            if (pref == UNSET) {
                pref = constrain(calculatePreferredSize(axis));
            }
            return pref;
        }

        /**
         * Returns the maximum size.
         */
        final int getMaximumSize(int axis) {
            if (max == UNSET) {
                max = constrain(calculateMaximumSize(axis));
            }
            return max;
        }

        /**
         * Sets the value and location of the spring.  Subclasses
         * will want to invoke super, then do any additional sizing.
         *
         * @param axis HORIZONTAL or VERTICAL
         * @param origin of this Spring
         * @param size of the Spring.  If size is UNSET, this invokes
         *        clear.
         */
        void setSize(int axis, int origin, int size) {
            this.size = size;
            if (size == UNSET) {
                unset();
            }
        }

        /**
         * Resets the cached min/max/pref.
         */
        void unset() {
            size = min = pref = max = UNSET;
        }

        /**
         * Returns the current size.
         */
        int getSize() {
            return size;
        }

        int constrain(int value) {
            return Math.min(value, INFINITE);
        }

        int getBaseline() {
            return -1;
        }

        BaselineResizeBehavior getBaselineResizeBehavior() {
            return BaselineResizeBehavior.OTHER;
        }

        final boolean isResizable(int axis) {
            int min = getMinimumSize(axis);
            int pref = getPreferredSize(axis);
            return (min != pref || pref != getMaximumSize(axis));
        }

        /**
         * Returns {@code true} if this spring will ALWAYS have a zero
         * size. This should NOT check the current size, rather it's
         * meant to quickly test if this Spring will always have a
         * zero size.
         *
         * @param treatAutopaddingAsZeroSized if {@code true}, auto padding
         *        springs should be treated as having a size of {@code 0}
         * @return {@code true} if this spring will have a zero size,
         *         {@code false} otherwise
         */
        abstract boolean willHaveZeroSize(boolean treatAutopaddingAsZeroSized);
    }

    /**
     * {@code Group} provides the basis for the two types of
     * operations supported by {@code GroupLayout}: laying out
     * components one after another ({@link SequentialGroup SequentialGroup})
     * or aligned ({@link ParallelGroup ParallelGroup}). {@code Group} and
     * its subclasses have no public constructor; to create one use
     * one of {@code createSequentialGroup} or
     * {@code createParallelGroup}. Additionally, taking a {@code Group}
     * created from one {@code GroupLayout} and using it with another
     * will produce undefined results.
     * <p>
     * Various methods in {@code Group} and its subclasses allow you
     * to explicitly specify the range. The arguments to these methods
     * can take two forms, either a value greater than or equal to 0,
     * or one of {@code DEFAULT_SIZE} or {@code PREFERRED_SIZE}. A
     * value greater than or equal to {@code 0} indicates a specific
     * size. {@code DEFAULT_SIZE} indicates the corresponding size
     * from the component should be used.  For example, if {@code
     * DEFAULT_SIZE} is passed as the minimum size argument, the
     * minimum size is obtained from invoking {@code getMinimumSize}
     * on the component. Likewise, {@code PREFERRED_SIZE} indicates
     * the value from {@code getPreferredSize} should be used.
     * The following example adds {@code myComponent} to {@code group}
     * with specific values for the range. That is, the minimum is
     * explicitly specified as 100, preferred as 200, and maximum as
     * 300.
     * <pre>
     *   group.addComponent(myComponent, 100, 200, 300);
     * </pre>
     * The following example adds {@code myComponent} to {@code group} using
     * a combination of the forms. The minimum size is forced to be the
     * same as the preferred size, the preferred size is determined by
     * using {@code myComponent.getPreferredSize} and the maximum is
     * determined by invoking {@code getMaximumSize} on the component.
     * <pre>
     *   group.addComponent(myComponent, GroupLayout.PREFERRED_SIZE,
     *             GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE);
     * </pre>
     * <p>
     * Unless otherwise specified all the methods of {@code Group} and
     * its subclasses that allow you to specify a range throw an
     * {@code IllegalArgumentException} if passed an invalid range. An
     * invalid range is one in which any of the values are &lt; 0 and
     * not one of {@code PREFERRED_SIZE} or {@code DEFAULT_SIZE}, or
     * the following is not met (for specific values): {@code min}
     * &lt;= {@code pref} &lt;= {@code max}.
     * <p>
     * Similarly any methods that take a {@code Component} throw a
     * {@code IllegalArgumentException} if passed {@code null} and any methods
     * that take a {@code Group} throw an {@code NullPointerException} if
     * passed {@code null}.
     *
     * @see #createSequentialGroup
     * @see #createParallelGroup
     * @since 1.6
     */
    public abstract class Group extends Spring {
        // private int origin;
        // private int size;
        List<Spring> springs;

        Group() {
            springs = new ArrayList<Spring>();
        }

        /**
         * Adds a {@code Group} to this {@code Group}.
         *
         * @param group the {@code Group} to add
         * @return this {@code Group}
         */
        public Group addGroup(Group group) {
            return addSpring(group);
        }

        /**
         * Adds a {@code Component} to this {@code Group}.
         *
         * @param component the {@code Component} to add
         * @return this {@code Group}
         */
        public Group addComponent(Component component) {
            return addComponent(component, DEFAULT_SIZE, DEFAULT_SIZE,
                    DEFAULT_SIZE);
        }

        /**
         * Adds a {@code Component} to this {@code Group}
         * with the specified size.
         *
         * @param component the {@code Component} to add
         * @param min the minimum size or one of {@code DEFAULT_SIZE} or
         *            {@code PREFERRED_SIZE}
         * @param pref the preferred size or one of {@code DEFAULT_SIZE} or
         *            {@code PREFERRED_SIZE}
         * @param max the maximum size or one of {@code DEFAULT_SIZE} or
         *            {@code PREFERRED_SIZE}
         * @return this {@code Group}
         */
        public Group addComponent(Component component, int min, int pref,
                int max) {
            return addSpring(new ComponentSpring(component, min, pref, max));
        }

        /**
         * Adds a rigid gap to this {@code Group}.
         *
         * @param size the size of the gap
         * @return this {@code Group}
         * @throws IllegalArgumentException if {@code size} is less than
         *         {@code 0}
         */
        public Group addGap(int size) {
            return addGap(size, size, size);
        }

        /**
         * Adds a gap to this {@code Group} with the specified size.
         *
         * @param min the minimum size of the gap
         * @param pref the preferred size of the gap
         * @param max the maximum size of the gap
         * @throws IllegalArgumentException if any of the values are
         *         less than {@code 0}
         * @return this {@code Group}
         */
        public Group addGap(int min, int pref, int max) {
            return addSpring(new GapSpring(min, pref, max));
        }

        Spring getSpring(int index) {
            return springs.get(index);
        }

        int indexOf(Spring spring) {
            return springs.indexOf(spring);
        }

        /**
         * Adds the Spring to the list of {@code Spring}s and returns
         * the receiver.
         */
        Group addSpring(Spring spring) {
            springs.add(spring);
            spring.setParent(this);
            if (!(spring instanceof AutoPreferredGapSpring) ||
                    !((AutoPreferredGapSpring)spring).getUserCreated()) {
                springsChanged = true;
            }
            return this;
        }

        //
        // Spring methods
        //

        void setSize(int axis, int origin, int size) {
            super.setSize(axis, origin, size);
            if (size == UNSET) {
                for (int counter = springs.size() - 1; counter >= 0;
                counter--) {
                    getSpring(counter).setSize(axis, origin, size);
                }
            } else {
                setValidSize(axis, origin, size);
            }
        }

        /**
         * This is invoked from {@code setSize} if passed a value
         * other than UNSET.
         */
        abstract void setValidSize(int axis, int origin, int size);

        int calculateMinimumSize(int axis) {
            return calculateSize(axis, MIN_SIZE);
        }

        int calculatePreferredSize(int axis) {
            return calculateSize(axis, PREF_SIZE);
        }

        int calculateMaximumSize(int axis) {
            return calculateSize(axis, MAX_SIZE);
        }

        /**
         * Calculates the specified size.  This is called from
         * one of the {@code getMinimumSize0},
         * {@code getPreferredSize0} or
         * {@code getMaximumSize0} methods.  This will invoke
         * to {@code operator} to combine the values.
         */
        int calculateSize(int axis, int type) {
            int count = springs.size();
            if (count == 0) {
                return 0;
            }
            if (count == 1) {
                return getSpringSize(getSpring(0), axis, type);
            }
            int size = constrain(operator(getSpringSize(getSpring(0), axis,
                    type), getSpringSize(getSpring(1), axis, type)));
            for (int counter = 2; counter < count; counter++) {
                size = constrain(operator(size, getSpringSize(
                        getSpring(counter), axis, type)));
            }
            return size;
        }

        int getSpringSize(Spring spring, int axis, int type) {
            switch(type) {
                case MIN_SIZE:
                    return spring.getMinimumSize(axis);
                case PREF_SIZE:
                    return spring.getPreferredSize(axis);
                case MAX_SIZE:
                    return spring.getMaximumSize(axis);
            }
            assert false;
            return 0;
        }

        /**
         * Used to compute how the two values representing two springs
         * will be combined.  For example, a group that layed things out
         * one after the next would return {@code a + b}.
         */
        abstract int operator(int a, int b);

        //
        // Padding
        //

        /**
         * Adjusts the autopadding springs in this group and its children.
         * If {@code insert} is true this will insert auto padding
         * springs, otherwise this will only adjust the springs that
         * comprise auto preferred padding springs.
         *
         * @param axis the axis of the springs; HORIZONTAL or VERTICAL
         * @param leadingPadding List of AutopaddingSprings that occur before
         *                       this Group
         * @param trailingPadding any trailing autopadding springs are added
         *                        to this on exit
         * @param leading List of ComponentSprings that occur before this Group
         * @param trailing any trailing ComponentSpring are added to this
         *                 List
         * @param insert Whether or not to insert AutopaddingSprings or just
         *               adjust any existing AutopaddingSprings.
         */
        abstract void insertAutopadding(int axis,
                List<AutoPreferredGapSpring> leadingPadding,
                List<AutoPreferredGapSpring> trailingPadding,
                List<ComponentSpring> leading, List<ComponentSpring> trailing,
                boolean insert);

        /**
         * Removes any AutopaddingSprings for this Group and its children.
         */
        void removeAutopadding() {
            unset();
            for (int counter = springs.size() - 1; counter >= 0; counter--) {
                Spring spring = springs.get(counter);
                if (spring instanceof AutoPreferredGapSpring) {
                    if (((AutoPreferredGapSpring)spring).getUserCreated()) {
                        ((AutoPreferredGapSpring)spring).reset();
                    } else {
                        springs.remove(counter);
                    }
                } else if (spring instanceof Group) {
                    ((Group)spring).removeAutopadding();
                }
            }
        }

        void unsetAutopadding() {
            // Clear cached pref/min/max.
            unset();
            for (int counter = springs.size() - 1; counter >= 0; counter--) {
                Spring spring = springs.get(counter);
                if (spring instanceof AutoPreferredGapSpring) {
                    spring.unset();
                } else if (spring instanceof Group) {
                    ((Group)spring).unsetAutopadding();
                }
            }
        }

        void calculateAutopadding(int axis) {
            for (int counter = springs.size() - 1; counter >= 0; counter--) {
                Spring spring = springs.get(counter);
                if (spring instanceof AutoPreferredGapSpring) {
                    // Force size to be reset.
                    spring.unset();
                    ((AutoPreferredGapSpring)spring).calculatePadding(axis);
                } else if (spring instanceof Group) {
                    ((Group)spring).calculateAutopadding(axis);
                }
            }
            // Clear cached pref/min/max.
            unset();
        }

        @Override
        boolean willHaveZeroSize(boolean treatAutopaddingAsZeroSized) {
            for (int i = springs.size() - 1; i >= 0; i--) {
                Spring spring = springs.get(i);
                if (!spring.willHaveZeroSize(treatAutopaddingAsZeroSized)) {
                    return false;
                }
            }
            return true;
        }
    }


    /**
     * A {@code Group} that positions and sizes its elements
     * sequentially, one after another.  This class has no public
     * constructor, use the {@code createSequentialGroup} method
     * to create one.
     * <p>
     * In order to align a {@code SequentialGroup} along the baseline
     * of a baseline aligned {@code ParallelGroup} you need to specify
     * which of the elements of the {@code SequentialGroup} is used to
     * determine the baseline.  The element used to calculate the
     * baseline is specified using one of the {@code add} methods that
     * take a {@code boolean}. The last element added with a value of
     * {@code true} for {@code useAsBaseline} is used to calculate the
     * baseline.
     *
     * @see #createSequentialGroup
     * @since 1.6
     */
    public class SequentialGroup extends Group {
        private Spring baselineSpring;

        SequentialGroup() {
        }

        /**
         * {@inheritDoc}
         */
        public SequentialGroup addGroup(Group group) {
            return (SequentialGroup)super.addGroup(group);
        }

        /**
         * Adds a {@code Group} to this {@code Group}.
         *
         * @param group the {@code Group} to add
         * @param useAsBaseline whether the specified {@code Group} should
         *        be used to calculate the baseline for this {@code Group}
         * @return this {@code Group}
         */
        public SequentialGroup addGroup(boolean useAsBaseline, Group group) {
            super.addGroup(group);
            if (useAsBaseline) {
                baselineSpring = group;
            }
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public SequentialGroup addComponent(Component component) {
            return (SequentialGroup)super.addComponent(component);
        }

        /**
         * Adds a {@code Component} to this {@code Group}.
         *
         * @param useAsBaseline whether the specified {@code Component} should
         *        be used to calculate the baseline for this {@code Group}
         * @param component the {@code Component} to add
         * @return this {@code Group}
         */
        public SequentialGroup addComponent(boolean useAsBaseline,
                Component component) {
            super.addComponent(component);
            if (useAsBaseline) {
                baselineSpring = springs.get(springs.size() - 1);
            }
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public SequentialGroup addComponent(Component component, int min,
                int pref, int max) {
            return (SequentialGroup)super.addComponent(
                    component, min, pref, max);
        }

        /**
         * Adds a {@code Component} to this {@code Group}
         * with the specified size.
         *
         * @param useAsBaseline whether the specified {@code Component} should
         *        be used to calculate the baseline for this {@code Group}
         * @param component the {@code Component} to add
         * @param min the minimum size or one of {@code DEFAULT_SIZE} or
         *            {@code PREFERRED_SIZE}
         * @param pref the preferred size or one of {@code DEFAULT_SIZE} or
         *            {@code PREFERRED_SIZE}
         * @param max the maximum size or one of {@code DEFAULT_SIZE} or
         *            {@code PREFERRED_SIZE}
         * @return this {@code Group}
         */
        public SequentialGroup addComponent(boolean useAsBaseline,
                Component component, int min, int pref, int max) {
            super.addComponent(component, min, pref, max);
            if (useAsBaseline) {
                baselineSpring = springs.get(springs.size() - 1);
            }
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public SequentialGroup addGap(int size) {
            return (SequentialGroup)super.addGap(size);
        }

        /**
         * {@inheritDoc}
         */
        public SequentialGroup addGap(int min, int pref, int max) {
            return (SequentialGroup)super.addGap(min, pref, max);
        }

        /**
         * Adds an element representing the preferred gap between two
         * components. The element created to represent the gap is not
         * resizable.
         *
         * @param comp1 the first component
         * @param comp2 the second component
         * @param type the type of gap; one of the constants defined by
         *        {@code LayoutStyle}
         * @return this {@code SequentialGroup}
         * @throws IllegalArgumentException if {@code type}, {@code comp1} or
         *         {@code comp2} is {@code null}
         * @see LayoutStyle
         */
        public SequentialGroup addPreferredGap(JComponent comp1,
                JComponent comp2, ComponentPlacement type) {
            return addPreferredGap(comp1, comp2, type, DEFAULT_SIZE,
                    PREFERRED_SIZE);
        }

        /**
         * Adds an element representing the preferred gap between two
         * components.
         *
         * @param comp1 the first component
         * @param comp2 the second component
         * @param type the type of gap
         * @param pref the preferred size of the grap; one of
         *        {@code DEFAULT_SIZE} or a value &gt;= 0
         * @param max the maximum size of the gap; one of
         *        {@code DEFAULT_SIZE}, {@code PREFERRED_SIZE}
         *        or a value &gt;= 0
         * @return this {@code SequentialGroup}
         * @throws IllegalArgumentException if {@code type}, {@code comp1} or
         *         {@code comp2} is {@code null}
         * @see LayoutStyle
         */
        public SequentialGroup addPreferredGap(JComponent comp1,
                JComponent comp2, ComponentPlacement type, int pref,
                int max) {
            if (type == null) {
                throw new IllegalArgumentException("Type must be non-null");
            }
            if (comp1 == null || comp2 == null) {
                throw new IllegalArgumentException(
                        "Components must be non-null");
            }
            checkPreferredGapValues(pref, max);
            return (SequentialGroup)addSpring(new PreferredGapSpring(
                    comp1, comp2, type, pref, max));
        }

        /**
         * Adds an element representing the preferred gap between the
         * nearest components.  During layout, neighboring
         * components are found, and the size of the added gap is set
         * based on the preferred gap between the components.  If no
         * neighboring components are found the gap has a size of {@code 0}.
         * <p>
         * The element created to represent the gap is not
         * resizable.
         *
         * @param type the type of gap; one of
         *        {@code LayoutStyle.ComponentPlacement.RELATED} or
         *        {@code LayoutStyle.ComponentPlacement.UNRELATED}
         * @return this {@code SequentialGroup}
         * @see LayoutStyle
         * @throws IllegalArgumentException if {@code type} is not one of
         *         {@code LayoutStyle.ComponentPlacement.RELATED} or
         *         {@code LayoutStyle.ComponentPlacement.UNRELATED}
         */
        public SequentialGroup addPreferredGap(ComponentPlacement type) {
            return addPreferredGap(type, DEFAULT_SIZE, DEFAULT_SIZE);
        }

        /**
         * Adds an element representing the preferred gap between the
         * nearest components.  During layout, neighboring
         * components are found, and the minimum of this
         * gap is set based on the size of the preferred gap between the
         * neighboring components.  If no neighboring components are found the
         * minimum size is set to 0.
         *
         * @param type the type of gap; one of
         *        {@code LayoutStyle.ComponentPlacement.RELATED} or
         *        {@code LayoutStyle.ComponentPlacement.UNRELATED}
         * @param pref the preferred size of the grap; one of
         *        {@code DEFAULT_SIZE} or a value &gt;= 0
         * @param max the maximum size of the gap; one of
         *        {@code DEFAULT_SIZE}, {@code PREFERRED_SIZE}
         *        or a value &gt;= 0
         * @return this {@code SequentialGroup}
         * @throws IllegalArgumentException if {@code type} is not one of
         *         {@code LayoutStyle.ComponentPlacement.RELATED} or
         *         {@code LayoutStyle.ComponentPlacement.UNRELATED}
         * @see LayoutStyle
         */
        public SequentialGroup addPreferredGap(ComponentPlacement type,
                int pref, int max) {
            if (type != ComponentPlacement.RELATED &&
                    type != ComponentPlacement.UNRELATED) {
                throw new IllegalArgumentException(
                        "Type must be one of " +
                        "LayoutStyle.ComponentPlacement.RELATED or " +
                        "LayoutStyle.ComponentPlacement.UNRELATED");
            }
            checkPreferredGapValues(pref, max);
            hasPreferredPaddingSprings = true;
            return (SequentialGroup)addSpring(new AutoPreferredGapSpring(
                    type, pref, max));
        }

        /**
         * Adds an element representing the preferred gap between an edge
         * the container and components that touch the border of the
         * container. This has no effect if the added gap does not
         * touch an edge of the parent container.
         * <p>
         * The element created to represent the gap is not
         * resizable.
         *
         * @return this {@code SequentialGroup}
         */
        public SequentialGroup addContainerGap() {
            return addContainerGap(DEFAULT_SIZE, DEFAULT_SIZE);
        }

        /**
         * Adds an element representing the preferred gap between one
         * edge of the container and the next or previous {@code
         * Component} with the specified size. This has no
         * effect if the next or previous element is not a {@code
         * Component} and does not touch one edge of the parent
         * container.
         *
         * @param pref the preferred size; one of {@code DEFAULT_SIZE} or a
         *              value &gt;= 0
         * @param max the maximum size; one of {@code DEFAULT_SIZE},
         *        {@code PREFERRED_SIZE} or a value &gt;= 0
         * @return this {@code SequentialGroup}
         */
        public SequentialGroup addContainerGap(int pref, int max) {
            if ((pref < 0 && pref != DEFAULT_SIZE) ||
                    (max < 0 && max != DEFAULT_SIZE && max != PREFERRED_SIZE)||
                    (pref >= 0 && max >= 0 && pref > max)) {
                throw new IllegalArgumentException(
                        "Pref and max must be either DEFAULT_VALUE " +
                        "or >= 0 and pref <= max");
            }
            hasPreferredPaddingSprings = true;
            return (SequentialGroup)addSpring(
                    new ContainerAutoPreferredGapSpring(pref, max));
        }

        int operator(int a, int b) {
            return constrain(a) + constrain(b);
        }

        void setValidSize(int axis, int origin, int size) {
            int pref = getPreferredSize(axis);
            if (size == pref) {
                // Layout at preferred size
                for (Spring spring : springs) {
                    int springPref = spring.getPreferredSize(axis);
                    spring.setSize(axis, origin, springPref);
                    origin += springPref;
                }
            } else if (springs.size() == 1) {
                Spring spring = getSpring(0);
                spring.setSize(axis, origin, Math.min(
                        Math.max(size, spring.getMinimumSize(axis)),
                        spring.getMaximumSize(axis)));
            } else if (springs.size() > 1) {
                // Adjust between min/pref
                setValidSizeNotPreferred(axis, origin, size);
            }
        }

        private void setValidSizeNotPreferred(int axis, int origin, int size) {
            int delta = size - getPreferredSize(axis);
            assert delta != 0;
            boolean useMin = (delta < 0);
            int springCount = springs.size();
            if (useMin) {
                delta *= -1;
            }

            // The following algorithm if used for resizing springs:
            // 1. Calculate the resizability of each spring (pref - min or
            //    max - pref) into a list.
            // 2. Sort the list in ascending order
            // 3. Iterate through each of the resizable Springs, attempting
            //    to give them (pref - size) / resizeCount
            // 4. For any Springs that can not accommodate that much space
            //    add the remainder back to the amount to distribute and
            //    recalculate how must space the remaining springs will get.
            // 5. Set the size of the springs.

            // First pass, sort the resizable springs into the List resizable
            List<SpringDelta> resizable = buildResizableList(axis, useMin);
            int resizableCount = resizable.size();

            if (resizableCount > 0) {
                // How much we would like to give each Spring.
                int sDelta = delta / resizableCount;
                // Remaining space.
                int slop = delta - sDelta * resizableCount;
                int[] sizes = new int[springCount];
                int sign = useMin ? -1 : 1;
                // Second pass, accumulate the resulting deltas (relative to
                // preferred) into sizes.
                for (int counter = 0; counter < resizableCount; counter++) {
                    SpringDelta springDelta = resizable.get(counter);
                    if ((counter + 1) == resizableCount) {
                        sDelta += slop;
                    }
                    springDelta.delta = Math.min(sDelta, springDelta.delta);
                    delta -= springDelta.delta;
                    if (springDelta.delta != sDelta && counter + 1 <
                            resizableCount) {
                        // Spring didn't take all the space, reset how much
                        // each spring will get.
                        sDelta = delta / (resizableCount - counter - 1);
                        slop = delta - sDelta * (resizableCount - counter - 1);
                    }
                    sizes[springDelta.index] = sign * springDelta.delta;
                }

                // And finally set the size of each spring
                for (int counter = 0; counter < springCount; counter++) {
                    Spring spring = getSpring(counter);
                    int sSize = spring.getPreferredSize(axis) + sizes[counter];
                    spring.setSize(axis, origin, sSize);
                    origin += sSize;
                }
            } else {
                // Nothing resizable, use the min or max of each of the
                // springs.
                for (int counter = 0; counter < springCount; counter++) {
                    Spring spring = getSpring(counter);
                    int sSize;
                    if (useMin) {
                        sSize = spring.getMinimumSize(axis);
                    } else {
                        sSize = spring.getMaximumSize(axis);
                    }
                    spring.setSize(axis, origin, sSize);
                    origin += sSize;
                }
            }
        }

        /**
         * Returns the sorted list of SpringDelta's for the current set of
         * Springs. The list is ordered based on the amount of flexibility of
         * the springs.
         */
        private List<SpringDelta> buildResizableList(int axis,
                boolean useMin) {
            // First pass, figure out what is resizable
            int size = springs.size();
            List<SpringDelta> sorted = new ArrayList<SpringDelta>(size);
            for (int counter = 0; counter < size; counter++) {
                Spring spring = getSpring(counter);
                int sDelta;
                if (useMin) {
                    sDelta = spring.getPreferredSize(axis) -
                            spring.getMinimumSize(axis);
                } else {
                    sDelta = spring.getMaximumSize(axis) -
                            spring.getPreferredSize(axis);
                }
                if (sDelta > 0) {
                    sorted.add(new SpringDelta(counter, sDelta));
                }
            }
            Collections.sort(sorted);
            return sorted;
        }

        private int indexOfNextNonZeroSpring(
                int index, boolean treatAutopaddingAsZeroSized) {
            while (index < springs.size()) {
                Spring spring = springs.get(index);
                if (!spring.willHaveZeroSize(treatAutopaddingAsZeroSized)) {
                    return index;
                }
                index++;
            }
            return index;
        }

        @Override
        void insertAutopadding(int axis,
                List<AutoPreferredGapSpring> leadingPadding,
                List<AutoPreferredGapSpring> trailingPadding,
                List<ComponentSpring> leading, List<ComponentSpring> trailing,
                boolean insert) {
            List<AutoPreferredGapSpring> newLeadingPadding =
                    new ArrayList<AutoPreferredGapSpring>(leadingPadding);
            List<AutoPreferredGapSpring> newTrailingPadding =
                    new ArrayList<AutoPreferredGapSpring>(1);
            List<ComponentSpring> newLeading =
                    new ArrayList<ComponentSpring>(leading);
            List<ComponentSpring> newTrailing = null;
            int counter = 0;
            // Warning, this must use springs.size, as it may change during the
            // loop.
            while (counter < springs.size()) {
                Spring spring = getSpring(counter);
                if (spring instanceof AutoPreferredGapSpring) {
                    if (newLeadingPadding.size() == 0) {
                        // Autopadding spring. Set the sources of the
                        // autopadding spring based on newLeading.
                        AutoPreferredGapSpring padding =
                            (AutoPreferredGapSpring)spring;
                        padding.setSources(newLeading);
                        newLeading.clear();
                        counter = indexOfNextNonZeroSpring(counter + 1, true);
                        if (counter == springs.size()) {
                            // Last spring in the list, add it to
                            // trailingPadding.
                            if (!(padding instanceof
                                  ContainerAutoPreferredGapSpring)) {
                                trailingPadding.add(padding);
                            }
                        } else {
                            newLeadingPadding.clear();
                            newLeadingPadding.add(padding);
                        }
                    } else {
                        counter = indexOfNextNonZeroSpring(counter + 1, true);
                    }
                } else {
                    // Not a padding spring
                    if (newLeading.size() > 0 && newLeadingPadding.isEmpty() && insert) {
                        // There's leading ComponentSprings, create an
                        // autopadding spring.
                        AutoPreferredGapSpring padding =
                                new AutoPreferredGapSpring();
                        // Force the newly created spring to be considered
                        // by NOT incrementing counter
                        springs.add(counter, padding);
                        continue;
                    }
                    if (spring instanceof ComponentSpring) {
                        // Spring is a Component, make it the target of any
                        // leading AutopaddingSpring.
                        ComponentSpring cSpring = (ComponentSpring)spring;
                        if (!cSpring.isVisible()) {
                            counter++;
                            continue;
                        }
                        for (AutoPreferredGapSpring gapSpring : newLeadingPadding) {
                            gapSpring.addTarget(cSpring, axis);
                        }
                        newLeading.clear();
                        newLeadingPadding.clear();
                        counter = indexOfNextNonZeroSpring(counter + 1, false);
                        if (counter == springs.size()) {
                            // Last Spring, add it to trailing
                            trailing.add(cSpring);
                        } else {
                            // Not that last Spring, add it to leading
                            newLeading.add(cSpring);
                        }
                    } else if (spring instanceof Group) {
                        // Forward call to child Group
                        if (newTrailing == null) {
                            newTrailing = new ArrayList<ComponentSpring>(1);
                        } else {
                            newTrailing.clear();
                        }
                        newTrailingPadding.clear();
                        ((Group)spring).insertAutopadding(axis,
                                newLeadingPadding, newTrailingPadding,
                                newLeading, newTrailing, insert);
                        newLeading.clear();
                        newLeadingPadding.clear();
                        counter = indexOfNextNonZeroSpring(
                                    counter + 1, (newTrailing.size() == 0));
                        if (counter == springs.size()) {
                            trailing.addAll(newTrailing);
                            trailingPadding.addAll(newTrailingPadding);
                        } else {
                            newLeading.addAll(newTrailing);
                            newLeadingPadding.addAll(newTrailingPadding);
                        }
                    } else {
                        // Gap
                        newLeadingPadding.clear();
                        newLeading.clear();
                        counter++;
                    }
                }
            }
        }

        int getBaseline() {
            if (baselineSpring != null) {
                int baseline = baselineSpring.getBaseline();
                if (baseline >= 0) {
                    int size = 0;
                    for (Spring spring : springs) {
                        if (spring == baselineSpring) {
                            return size + baseline;
                        } else {
                            size += spring.getPreferredSize(VERTICAL);
                        }
                    }
                }
            }
            return -1;
        }

        BaselineResizeBehavior getBaselineResizeBehavior() {
            if (isResizable(VERTICAL)) {
                if (!baselineSpring.isResizable(VERTICAL)) {
                    // Spring to use for baseline isn't resizable. In this case
                    // baseline resize behavior can be determined based on how
                    // preceding springs resize.
                    boolean leadingResizable = false;
                    for (Spring spring : springs) {
                        if (spring == baselineSpring) {
                            break;
                        } else if (spring.isResizable(VERTICAL)) {
                            leadingResizable = true;
                            break;
                        }
                    }
                    boolean trailingResizable = false;
                    for (int i = springs.size() - 1; i >= 0; i--) {
                        Spring spring = springs.get(i);
                        if (spring == baselineSpring) {
                            break;
                        }
                        if (spring.isResizable(VERTICAL)) {
                            trailingResizable = true;
                            break;
                        }
                    }
                    if (leadingResizable && !trailingResizable) {
                        return BaselineResizeBehavior.CONSTANT_DESCENT;
                    } else if (!leadingResizable && trailingResizable) {
                        return BaselineResizeBehavior.CONSTANT_ASCENT;
                    }
                    // If we get here, both leading and trailing springs are
                    // resizable. Fall through to OTHER.
                } else {
                    BaselineResizeBehavior brb = baselineSpring.getBaselineResizeBehavior();
                    if (brb == BaselineResizeBehavior.CONSTANT_ASCENT) {
                        for (Spring spring : springs) {
                            if (spring == baselineSpring) {
                                return BaselineResizeBehavior.CONSTANT_ASCENT;
                            }
                            if (spring.isResizable(VERTICAL)) {
                                return BaselineResizeBehavior.OTHER;
                            }
                        }
                    } else if (brb == BaselineResizeBehavior.CONSTANT_DESCENT) {
                        for (int i = springs.size() - 1; i >= 0; i--) {
                            Spring spring = springs.get(i);
                            if (spring == baselineSpring) {
                                return BaselineResizeBehavior.CONSTANT_DESCENT;
                            }
                            if (spring.isResizable(VERTICAL)) {
                                return BaselineResizeBehavior.OTHER;
                            }
                        }
                    }
                }
                return BaselineResizeBehavior.OTHER;
            }
            // Not resizable, treat as constant_ascent
            return BaselineResizeBehavior.CONSTANT_ASCENT;
        }

        private void checkPreferredGapValues(int pref, int max) {
            if ((pref < 0 && pref != DEFAULT_SIZE && pref != PREFERRED_SIZE) ||
                    (max < 0 && max != DEFAULT_SIZE && max != PREFERRED_SIZE)||
                    (pref >= 0 && max >= 0 && pref > max)) {
                throw new IllegalArgumentException(
                        "Pref and max must be either DEFAULT_SIZE, " +
                        "PREFERRED_SIZE, or >= 0 and pref <= max");
            }
        }
    }


    /**
     * Used by SequentialGroup in calculating resizability of springs.
     */
    private static final class SpringDelta implements Comparable<SpringDelta> {
        // Original index.
        public final int index;
        // Delta, one of pref - min or max - pref.
        public int delta;

        public SpringDelta(int index, int delta) {
            this.index = index;
            this.delta = delta;
        }

        public int compareTo(SpringDelta o) {
            return delta - o.delta;
        }

        public String toString() {
            return super.toString() + "[index=" + index + ", delta=" +
                    delta + "]";
        }
    }


    /**
     * A {@code Group} that aligns and sizes its children.
     * {@code ParallelGroup} aligns its children in
     * four possible ways: along the baseline, centered, anchored to the
     * leading edge, or anchored to the trailing edge.
     * <h2>Baseline</h2>
     * A {@code ParallelGroup} that aligns its children along the
     * baseline must first decide where the baseline is
     * anchored. The baseline can either be anchored to the top, or
     * anchored to the bottom of the group. That is, the distance between the
     * baseline and the beginning of the group can be a constant
     * distance, or the distance between the end of the group and the
     * baseline can be a constant distance. The possible choices
     * correspond to the {@code BaselineResizeBehavior} constants
     * {@link
     * java.awt.Component.BaselineResizeBehavior#CONSTANT_ASCENT CONSTANT_ASCENT} and
     * {@link
     * java.awt.Component.BaselineResizeBehavior#CONSTANT_DESCENT CONSTANT_DESCENT}.
     * <p>
     * The baseline anchor may be explicitly specified by the
     * {@code createBaselineGroup} method, or determined based on the elements.
     * If not explicitly specified, the baseline will be anchored to
     * the bottom if all the elements with a baseline, and that are
     * aligned to the baseline, have a baseline resize behavior of
     * {@code CONSTANT_DESCENT}; otherwise the baseline is anchored to the top
     * of the group.
     * <p>
     * Elements aligned to the baseline are resizable if they have
     * a baseline resize behavior of {@code CONSTANT_ASCENT} or
     * {@code CONSTANT_DESCENT}. Elements with a baseline resize
     * behavior of {@code OTHER} or {@code CENTER_OFFSET} are not resizable.
     * <p>
     * The baseline is calculated based on the preferred height of each
     * of the elements that have a baseline. The baseline is
     * calculated using the following algorithm:
     * {@code max(maxNonBaselineHeight, maxAscent + maxDescent)}, where the
     * {@code maxNonBaselineHeight} is the maximum height of all elements
     * that do not have a baseline, or are not aligned along the baseline.
     * {@code maxAscent} is the maximum ascent (baseline) of all elements that
     * have a baseline and are aligned along the baseline.
     * {@code maxDescent} is the maximum descent (preferred height - baseline)
     * of all elements that have a baseline and are aligned along the baseline.
     * <p>
     * A {@code ParallelGroup} that aligns its elements along the baseline
     * is only useful along the vertical axis. If you create a
     * baseline group and use it along the horizontal axis an
     * {@code IllegalStateException} is thrown when you ask
     * {@code GroupLayout} for the minimum, preferred or maximum size or
     * attempt to layout the components.
     * <p>
     * Elements that are not aligned to the baseline and smaller than the size
     * of the {@code ParallelGroup} are positioned in one of three
     * ways: centered, anchored to the leading edge, or anchored to the
     * trailing edge.
     *
     * <h2>Non-baseline {@code ParallelGroup}</h2>
     * {@code ParallelGroup}s created with an alignment other than
     * {@code BASELINE} align elements that are smaller than the size
     * of the group in one of three ways: centered, anchored to the
     * leading edge, or anchored to the trailing edge.
     * <p>
     * The leading edge is based on the axis and {@code
     * ComponentOrientation}.  For the vertical axis the top edge is
     * always the leading edge, and the bottom edge is always the
     * trailing edge. When the {@code ComponentOrientation} is {@code
     * LEFT_TO_RIGHT}, the leading edge is the left edge and the
     * trailing edge the right edge. A {@code ComponentOrientation} of
     * {@code RIGHT_TO_LEFT} flips the left and right edges. Child
     * elements are aligned based on the specified alignment the
     * element was added with. If you do not specify an alignment, the
     * alignment specified for the {@code ParallelGroup} is used.
     * <p>
     * To align elements along the baseline you {@code createBaselineGroup},
     * or {@code createParallelGroup} with an alignment of {@code BASELINE}.
     * If the group was not created with a baseline alignment, and you attempt
     * to add an element specifying a baseline alignment, an
     * {@code IllegalArgumentException} is thrown.
     *
     * @see #createParallelGroup()
     * @see #createBaselineGroup(boolean,boolean)
     * @since 1.6
     */
    public class ParallelGroup extends Group {
        // How children are layed out.
        private final Alignment childAlignment;
        // Whether or not we're resizable.
        private final boolean resizable;

        ParallelGroup(Alignment childAlignment, boolean resizable) {
            this.childAlignment = childAlignment;
            this.resizable = resizable;
        }

        /**
         * {@inheritDoc}
         */
        public ParallelGroup addGroup(Group group) {
            return (ParallelGroup)super.addGroup(group);
        }

        /**
         * {@inheritDoc}
         */
        public ParallelGroup addComponent(Component component) {
            return (ParallelGroup)super.addComponent(component);
        }

        /**
         * {@inheritDoc}
         */
        public ParallelGroup addComponent(Component component, int min, int pref,
                int max) {
            return (ParallelGroup)super.addComponent(component, min, pref, max);
        }

        /**
         * {@inheritDoc}
         */
        public ParallelGroup addGap(int pref) {
            return (ParallelGroup)super.addGap(pref);
        }

        /**
         * {@inheritDoc}
         */
        public ParallelGroup addGap(int min, int pref, int max) {
            return (ParallelGroup)super.addGap(min, pref, max);
        }

        /**
         * Adds a {@code Group} to this {@code ParallelGroup} with the
         * specified alignment. If the child is smaller than the
         * {@code Group} it is aligned based on the specified
         * alignment.
         *
         * @param alignment the alignment
         * @param group the {@code Group} to add
         * @return this {@code ParallelGroup}
         * @throws IllegalArgumentException if {@code alignment} is
         *         {@code null}
         */
        public ParallelGroup addGroup(Alignment alignment, Group group) {
            checkChildAlignment(alignment);
            group.setAlignment(alignment);
            return (ParallelGroup)addSpring(group);
        }

        /**
         * Adds a {@code Component} to this {@code ParallelGroup} with
         * the specified alignment.
         *
         * @param alignment the alignment
         * @param component the {@code Component} to add
         * @return this {@code Group}
         * @throws IllegalArgumentException if {@code alignment} is
         *         {@code null}
         */
        public ParallelGroup addComponent(Component component,
                Alignment alignment) {
            return addComponent(component, alignment, DEFAULT_SIZE, DEFAULT_SIZE,
                    DEFAULT_SIZE);
        }

        /**
         * Adds a {@code Component} to this {@code ParallelGroup} with the
         * specified alignment and size.
         *
         * @param alignment the alignment
         * @param component the {@code Component} to add
         * @param min the minimum size
         * @param pref the preferred size
         * @param max the maximum size
         * @throws IllegalArgumentException if {@code alignment} is
         *         {@code null}
         * @return this {@code Group}
         */
        public ParallelGroup addComponent(Component component,
                Alignment alignment, int min, int pref, int max) {
            checkChildAlignment(alignment);
            ComponentSpring spring = new ComponentSpring(component,
                    min, pref, max);
            spring.setAlignment(alignment);
            return (ParallelGroup)addSpring(spring);
        }

        boolean isResizable() {
            return resizable;
        }

        int operator(int a, int b) {
            return Math.max(a, b);
        }

        int calculateMinimumSize(int axis) {
            if (!isResizable()) {
                return getPreferredSize(axis);
            }
            return super.calculateMinimumSize(axis);
        }

        int calculateMaximumSize(int axis) {
            if (!isResizable()) {
                return getPreferredSize(axis);
            }
            return super.calculateMaximumSize(axis);
        }

        void setValidSize(int axis, int origin, int size) {
            for (Spring spring : springs) {
                setChildSize(spring, axis, origin, size);
            }
        }

        void setChildSize(Spring spring, int axis, int origin, int size) {
            Alignment alignment = spring.getAlignment();
            int springSize = Math.min(
                    Math.max(spring.getMinimumSize(axis), size),
                    spring.getMaximumSize(axis));
            if (alignment == null) {
                alignment = childAlignment;
            }
            switch (alignment) {
                case TRAILING:
                    spring.setSize(axis, origin + size - springSize,
                            springSize);
                    break;
                case CENTER:
                    spring.setSize(axis, origin +
                            (size - springSize) / 2,springSize);
                    break;
                default: // LEADING, or BASELINE
                    spring.setSize(axis, origin, springSize);
                    break;
            }
        }

        @Override
        void insertAutopadding(int axis,
                List<AutoPreferredGapSpring> leadingPadding,
                List<AutoPreferredGapSpring> trailingPadding,
                List<ComponentSpring> leading, List<ComponentSpring> trailing,
                boolean insert) {
            for (Spring spring : springs) {
                if (spring instanceof ComponentSpring) {
                    if (((ComponentSpring)spring).isVisible()) {
                        for (AutoPreferredGapSpring gapSpring :
                                 leadingPadding) {
                            gapSpring.addTarget((ComponentSpring)spring, axis);
                        }
                        trailing.add((ComponentSpring)spring);
                    }
                } else if (spring instanceof Group) {
                    ((Group)spring).insertAutopadding(axis, leadingPadding,
                            trailingPadding, leading, trailing, insert);
                } else if (spring instanceof AutoPreferredGapSpring) {
                    ((AutoPreferredGapSpring)spring).setSources(leading);
                    trailingPadding.add((AutoPreferredGapSpring)spring);
                }
            }
        }

        private void checkChildAlignment(Alignment alignment) {
            checkChildAlignment(alignment, (this instanceof BaselineGroup));
        }

        private void checkChildAlignment(Alignment alignment,
                boolean allowsBaseline) {
            if (alignment == null) {
                throw new IllegalArgumentException("Alignment must be non-null");
            }
            if (!allowsBaseline && alignment == Alignment.BASELINE) {
                throw new IllegalArgumentException("Alignment must be one of:" +
                        "LEADING, TRAILING or CENTER");
            }
        }
    }


    /**
     * An extension of {@code ParallelGroup} that aligns its
     * constituent {@code Spring}s along the baseline.
     */
    private class BaselineGroup extends ParallelGroup {
        // Whether or not all child springs have a baseline
        private boolean allSpringsHaveBaseline;

        // max(spring.getBaseline()) of all springs aligned along the baseline
        // that have a baseline
        private int prefAscent;

        // max(spring.getPreferredSize().height - spring.getBaseline()) of all
        // springs aligned along the baseline that have a baseline
        private int prefDescent;

        // Whether baselineAnchoredToTop was explicitly set
        private boolean baselineAnchorSet;

        // Whether the baseline is anchored to the top or the bottom.
        // If anchored to the top the baseline is always at prefAscent,
        // otherwise the baseline is at (height - prefDescent)
        private boolean baselineAnchoredToTop;

        // Whether or not the baseline has been calculated.
        private boolean calcedBaseline;

        BaselineGroup(boolean resizable) {
            super(Alignment.LEADING, resizable);
            prefAscent = prefDescent = -1;
            calcedBaseline = false;
        }

        BaselineGroup(boolean resizable, boolean baselineAnchoredToTop) {
            this(resizable);
            this.baselineAnchoredToTop = baselineAnchoredToTop;
            baselineAnchorSet = true;
        }

        void unset() {
            super.unset();
            prefAscent = prefDescent = -1;
            calcedBaseline = false;
        }

        void setValidSize(int axis, int origin, int size) {
            checkAxis(axis);
            if (prefAscent == -1) {
                super.setValidSize(axis, origin, size);
            } else {
                // do baseline layout
                baselineLayout(origin, size);
            }
        }

        int calculateSize(int axis, int type) {
            checkAxis(axis);
            if (!calcedBaseline) {
                calculateBaselineAndResizeBehavior();
            }
            if (type == MIN_SIZE) {
                return calculateMinSize();
            }
            if (type == MAX_SIZE) {
                return calculateMaxSize();
            }
            if (allSpringsHaveBaseline) {
                return prefAscent + prefDescent;
            }
            return Math.max(prefAscent + prefDescent,
                    super.calculateSize(axis, type));
        }

        private void calculateBaselineAndResizeBehavior() {
            // calculate baseline
            prefAscent = 0;
            prefDescent = 0;
            int baselineSpringCount = 0;
            BaselineResizeBehavior resizeBehavior = null;
            for (Spring spring : springs) {
                if (spring.getAlignment() == null ||
                        spring.getAlignment() == Alignment.BASELINE) {
                    int baseline = spring.getBaseline();
                    if (baseline >= 0) {
                        if (spring.isResizable(VERTICAL)) {
                            BaselineResizeBehavior brb = spring.
                                    getBaselineResizeBehavior();
                            if (resizeBehavior == null) {
                                resizeBehavior = brb;
                            } else if (brb != resizeBehavior) {
                                resizeBehavior = BaselineResizeBehavior.
                                        CONSTANT_ASCENT;
                            }
                        }
                        prefAscent = Math.max(prefAscent, baseline);
                        prefDescent = Math.max(prefDescent, spring.
                                getPreferredSize(VERTICAL) - baseline);
                        baselineSpringCount++;
                    }
                }
            }
            if (!baselineAnchorSet) {
                if (resizeBehavior == BaselineResizeBehavior.CONSTANT_DESCENT){
                    this.baselineAnchoredToTop = false;
                } else {
                    this.baselineAnchoredToTop = true;
                }
            }
            allSpringsHaveBaseline = (baselineSpringCount == springs.size());
            calcedBaseline = true;
        }

        private int calculateMaxSize() {
            int maxAscent = prefAscent;
            int maxDescent = prefDescent;
            int nonBaselineMax = 0;
            for (Spring spring : springs) {
                int baseline;
                int springMax = spring.getMaximumSize(VERTICAL);
                if ((spring.getAlignment() == null ||
                        spring.getAlignment() == Alignment.BASELINE) &&
                        (baseline = spring.getBaseline()) >= 0) {
                    int springPref = spring.getPreferredSize(VERTICAL);
                    if (springPref != springMax) {
                        switch (spring.getBaselineResizeBehavior()) {
                            case CONSTANT_ASCENT:
                                if (baselineAnchoredToTop) {
                                    maxDescent = Math.max(maxDescent,
                                            springMax - baseline);
                                }
                                break;
                            case CONSTANT_DESCENT:
                                if (!baselineAnchoredToTop) {
                                    maxAscent = Math.max(maxAscent,
                                            springMax - springPref + baseline);
                                }
                                break;
                            default: // CENTER_OFFSET and OTHER, not resizable
                                break;
                        }
                    }
                } else {
                    // Not aligned along the baseline, or no baseline.
                    nonBaselineMax = Math.max(nonBaselineMax, springMax);
                }
            }
            return Math.max(nonBaselineMax, maxAscent + maxDescent);
        }

        private int calculateMinSize() {
            int minAscent = 0;
            int minDescent = 0;
            int nonBaselineMin = 0;
            if (baselineAnchoredToTop) {
                minAscent = prefAscent;
            } else {
                minDescent = prefDescent;
            }
            for (Spring spring : springs) {
                int springMin = spring.getMinimumSize(VERTICAL);
                int baseline;
                if ((spring.getAlignment() == null ||
                        spring.getAlignment() == Alignment.BASELINE) &&
                        (baseline = spring.getBaseline()) >= 0) {
                    int springPref = spring.getPreferredSize(VERTICAL);
                    BaselineResizeBehavior brb = spring.
                            getBaselineResizeBehavior();
                    switch (brb) {
                        case CONSTANT_ASCENT:
                            if (baselineAnchoredToTop) {
                                minDescent = Math.max(springMin - baseline,
                                        minDescent);
                            } else {
                                minAscent = Math.max(baseline, minAscent);
                            }
                            break;
                        case CONSTANT_DESCENT:
                            if (!baselineAnchoredToTop) {
                                minAscent = Math.max(
                                        baseline - (springPref - springMin),
                                        minAscent);
                            } else {
                                minDescent = Math.max(springPref - baseline,
                                        minDescent);
                            }
                            break;
                        default:
                            // CENTER_OFFSET and OTHER are !resizable, use
                            // the preferred size.
                            minAscent = Math.max(baseline, minAscent);
                            minDescent = Math.max(springPref - baseline,
                                    minDescent);
                            break;
                    }
                } else {
                    // Not aligned along the baseline, or no baseline.
                    nonBaselineMin = Math.max(nonBaselineMin, springMin);
                }
            }
            return Math.max(nonBaselineMin, minAscent + minDescent);
        }

        /**
         * Lays out springs that have a baseline along the baseline.  All
         * others are centered.
         */
        private void baselineLayout(int origin, int size) {
            int ascent;
            int descent;
            if (baselineAnchoredToTop) {
                ascent = prefAscent;
                descent = size - ascent;
            } else {
                ascent = size - prefDescent;
                descent = prefDescent;
            }
            for (Spring spring : springs) {
                Alignment alignment = spring.getAlignment();
                if (alignment == null || alignment == Alignment.BASELINE) {
                    int baseline = spring.getBaseline();
                    if (baseline >= 0) {
                        int springMax = spring.getMaximumSize(VERTICAL);
                        int springPref = spring.getPreferredSize(VERTICAL);
                        int height = springPref;
                        int y;
                        switch(spring.getBaselineResizeBehavior()) {
                            case CONSTANT_ASCENT:
                                y = origin + ascent - baseline;
                                height = Math.min(descent, springMax -
                                        baseline) + baseline;
                                break;
                            case CONSTANT_DESCENT:
                                height = Math.min(ascent, springMax -
                                        springPref + baseline) +
                                        (springPref - baseline);
                                y = origin + ascent +
                                        (springPref - baseline) - height;
                                break;
                            default: // CENTER_OFFSET & OTHER, not resizable
                                y = origin + ascent - baseline;
                                break;
                        }
                        spring.setSize(VERTICAL, y, height);
                    } else {
                        setChildSize(spring, VERTICAL, origin, size);
                    }
                } else {
                    setChildSize(spring, VERTICAL, origin, size);
                }
            }
        }

        int getBaseline() {
            if (springs.size() > 1) {
                // Force the baseline to be calculated
                getPreferredSize(VERTICAL);
                return prefAscent;
            } else if (springs.size() == 1) {
                return springs.get(0).getBaseline();
            }
            return -1;
        }

        BaselineResizeBehavior getBaselineResizeBehavior() {
            if (springs.size() == 1) {
                return springs.get(0).getBaselineResizeBehavior();
            }
            if (baselineAnchoredToTop) {
                return BaselineResizeBehavior.CONSTANT_ASCENT;
            }
            return BaselineResizeBehavior.CONSTANT_DESCENT;
        }

        // If the axis is VERTICAL, throws an IllegalStateException
        private void checkAxis(int axis) {
            if (axis == HORIZONTAL) {
                throw new IllegalStateException(
                        "Baseline must be used along vertical axis");
            }
        }
    }


    private final class ComponentSpring extends Spring {
        private Component component;
        private int origin;

        // min/pref/max are either a value >= 0 or one of
        // DEFAULT_SIZE or PREFERRED_SIZE
        private final int min;
        private final int pref;
        private final int max;

        // Baseline for the component, computed as necessary.
        private int baseline = -1;

        // Whether or not the size has been requested yet.
        private boolean installed;

        private ComponentSpring(Component component, int min, int pref,
                int max) {
            this.component = component;
            if (component == null) {
                throw new IllegalArgumentException(
                        "Component must be non-null");
            }

            checkSize(min, pref, max, true);

            this.min = min;
            this.max = max;
            this.pref = pref;

            // getComponentInfo makes sure component is a child of the
            // Container GroupLayout is the LayoutManager for.
            getComponentInfo(component);
        }

        int calculateMinimumSize(int axis) {
            if (isLinked(axis)) {
                return getLinkSize(axis, MIN_SIZE);
            }
            return calculateNonlinkedMinimumSize(axis);
        }

        int calculatePreferredSize(int axis) {
            if (isLinked(axis)) {
                return getLinkSize(axis, PREF_SIZE);
            }
            int min = getMinimumSize(axis);
            int pref = calculateNonlinkedPreferredSize(axis);
            int max = getMaximumSize(axis);
            return Math.min(max, Math.max(min, pref));
        }

        int calculateMaximumSize(int axis) {
            if (isLinked(axis)) {
                return getLinkSize(axis, MAX_SIZE);
            }
            return Math.max(getMinimumSize(axis),
                    calculateNonlinkedMaximumSize(axis));
        }

        boolean isVisible() {
            return getComponentInfo(getComponent()).isVisible();
        }

        int calculateNonlinkedMinimumSize(int axis) {
            if (!isVisible()) {
                return 0;
            }
            if (min >= 0) {
                return min;
            }
            if (min == PREFERRED_SIZE) {
                return calculateNonlinkedPreferredSize(axis);
            }
            assert (min == DEFAULT_SIZE);
            return getSizeAlongAxis(axis, component.getMinimumSize());
        }

        int calculateNonlinkedPreferredSize(int axis) {
            if (!isVisible()) {
                return 0;
            }
            if (pref >= 0) {
                return pref;
            }
            assert (pref == DEFAULT_SIZE || pref == PREFERRED_SIZE);
            return getSizeAlongAxis(axis, component.getPreferredSize());
        }

        int calculateNonlinkedMaximumSize(int axis) {
            if (!isVisible()) {
                return 0;
            }
            if (max >= 0) {
                return max;
            }
            if (max == PREFERRED_SIZE) {
                return calculateNonlinkedPreferredSize(axis);
            }
            assert (max == DEFAULT_SIZE);
            return getSizeAlongAxis(axis, component.getMaximumSize());
        }

        private int getSizeAlongAxis(int axis, Dimension size) {
            return (axis == HORIZONTAL) ? size.width : size.height;
        }

        private int getLinkSize(int axis, int type) {
            if (!isVisible()) {
                return 0;
            }
            ComponentInfo ci = getComponentInfo(component);
            return ci.getLinkSize(axis, type);
        }

        void setSize(int axis, int origin, int size) {
            super.setSize(axis, origin, size);
            this.origin = origin;
            if (size == UNSET) {
                baseline = -1;
            }
        }

        int getOrigin() {
            return origin;
        }

        void setComponent(Component component) {
            this.component = component;
        }

        Component getComponent() {
            return component;
        }

        int getBaseline() {
            if (baseline == -1) {
                Spring horizontalSpring = getComponentInfo(component).
                        horizontalSpring;
                int width = horizontalSpring.getPreferredSize(HORIZONTAL);
                int height = getPreferredSize(VERTICAL);
                if (width > 0 && height > 0) {
                    baseline = component.getBaseline(width, height);
                }
            }
            return baseline;
        }

        BaselineResizeBehavior getBaselineResizeBehavior() {
            return getComponent().getBaselineResizeBehavior();
        }

        private boolean isLinked(int axis) {
            return getComponentInfo(component).isLinked(axis);
        }

        void installIfNecessary(int axis) {
            if (!installed) {
                installed = true;
                if (axis == HORIZONTAL) {
                    getComponentInfo(component).horizontalSpring = this;
                } else {
                    getComponentInfo(component).verticalSpring = this;
                }
            }
        }

        @Override
        boolean willHaveZeroSize(boolean treatAutopaddingAsZeroSized) {
            return !isVisible();
        }
    }


    /**
     * Spring representing the preferred distance between two components.
     */
    private class PreferredGapSpring extends Spring {
        private final JComponent source;
        private final JComponent target;
        private final ComponentPlacement type;
        private final int pref;
        private final int max;

        PreferredGapSpring(JComponent source, JComponent target,
                ComponentPlacement type, int pref, int max) {
            this.source = source;
            this.target = target;
            this.type = type;
            this.pref = pref;
            this.max = max;
        }

        int calculateMinimumSize(int axis) {
            return getPadding(axis);
        }

        int calculatePreferredSize(int axis) {
            if (pref == DEFAULT_SIZE || pref == PREFERRED_SIZE) {
                return getMinimumSize(axis);
            }
            int min = getMinimumSize(axis);
            int max = getMaximumSize(axis);
            return Math.min(max, Math.max(min, pref));
        }

        int calculateMaximumSize(int axis) {
            if (max == PREFERRED_SIZE || max == DEFAULT_SIZE) {
                return getPadding(axis);
            }
            return Math.max(getMinimumSize(axis), max);
        }

        private int getPadding(int axis) {
            int position;
            if (axis == HORIZONTAL) {
                position = SwingConstants.EAST;
            } else {
                position = SwingConstants.SOUTH;
            }
            return getLayoutStyle0().getPreferredGap(source,
                    target, type, position, host);
        }

        @Override
        boolean willHaveZeroSize(boolean treatAutopaddingAsZeroSized) {
            return false;
        }
    }


    /**
     * Spring represented a certain amount of space.
     */
    private class GapSpring extends Spring {
        private final int min;
        private final int pref;
        private final int max;

        GapSpring(int min, int pref, int max) {
            checkSize(min, pref, max, false);
            this.min = min;
            this.pref = pref;
            this.max = max;
        }

        int calculateMinimumSize(int axis) {
            if (min == PREFERRED_SIZE) {
                return getPreferredSize(axis);
            }
            return min;
        }

        int calculatePreferredSize(int axis) {
            return pref;
        }

        int calculateMaximumSize(int axis) {
            if (max == PREFERRED_SIZE) {
                return getPreferredSize(axis);
            }
            return max;
        }

        @Override
        boolean willHaveZeroSize(boolean treatAutopaddingAsZeroSized) {
            return false;
        }
    }


    /**
     * Spring reprensenting the distance between any number of sources and
     * targets.  The targets and sources are computed during layout.  An
     * instance of this can either be dynamically created when
     * autocreatePadding is true, or explicitly created by the developer.
     */
    private class AutoPreferredGapSpring extends Spring {
        List<ComponentSpring> sources;
        ComponentSpring source;
        private List<AutoPreferredGapMatch> matches;
        int size;
        int lastSize;
        private final int pref;
        private final int max;
        // Type of gap
        private ComponentPlacement type;
        private boolean userCreated;

        private AutoPreferredGapSpring() {
            this.pref = PREFERRED_SIZE;
            this.max = PREFERRED_SIZE;
            this.type = ComponentPlacement.RELATED;
        }

        AutoPreferredGapSpring(int pref, int max) {
            this.pref = pref;
            this.max = max;
        }

        AutoPreferredGapSpring(ComponentPlacement type, int pref, int max) {
            this.type = type;
            this.pref = pref;
            this.max = max;
            this.userCreated = true;
        }

        public void setSource(ComponentSpring source) {
            this.source = source;
        }

        public void setSources(List<ComponentSpring> sources) {
            this.sources = new ArrayList<ComponentSpring>(sources);
        }

        public void setUserCreated(boolean userCreated) {
            this.userCreated = userCreated;
        }

        public boolean getUserCreated() {
            return userCreated;
        }

        void unset() {
            lastSize = getSize();
            super.unset();
            size = 0;
        }

        public void reset() {
            size = 0;
            sources = null;
            source = null;
            matches = null;
        }

        public void calculatePadding(int axis) {
            size = UNSET;
            int maxPadding = UNSET;
            if (matches != null) {
                LayoutStyle p = getLayoutStyle0();
                int position;
                if (axis == HORIZONTAL) {
                    if (isLeftToRight()) {
                        position = SwingConstants.EAST;
                    } else {
                        position = SwingConstants.WEST;
                    }
                } else {
                    position = SwingConstants.SOUTH;
                }
                for (int i = matches.size() - 1; i >= 0; i--) {
                    AutoPreferredGapMatch match = matches.get(i);
                    maxPadding = Math.max(maxPadding,
                            calculatePadding(p, position, match.source,
                            match.target));
                }
            }
            if (size == UNSET) {
                size = 0;
            }
            if (maxPadding == UNSET) {
                maxPadding = 0;
            }
            if (lastSize != UNSET) {
                size += Math.min(maxPadding, lastSize);
            }
        }

        private int calculatePadding(LayoutStyle p, int position,
                ComponentSpring source,
                ComponentSpring target) {
            int delta = target.getOrigin() - (source.getOrigin() +
                    source.getSize());
            if (delta >= 0) {
                int padding;
                if ((source.getComponent() instanceof JComponent) &&
                        (target.getComponent() instanceof JComponent)) {
                    padding = p.getPreferredGap(
                            (JComponent)source.getComponent(),
                            (JComponent)target.getComponent(), type, position,
                            host);
                } else {
                    padding = 10;
                }
                if (padding > delta) {
                    size = Math.max(size, padding - delta);
                }
                return padding;
            }
            return 0;
        }

        public void addTarget(ComponentSpring spring, int axis) {
            int oAxis = (axis == HORIZONTAL) ? VERTICAL : HORIZONTAL;
            if (source != null) {
                if (areParallelSiblings(source.getComponent(),
                        spring.getComponent(), oAxis)) {
                    addValidTarget(source, spring);
                }
            } else {
                Component component = spring.getComponent();
                for (int counter = sources.size() - 1; counter >= 0;
                         counter--){
                    ComponentSpring source = sources.get(counter);
                    if (areParallelSiblings(source.getComponent(),
                            component, oAxis)) {
                        addValidTarget(source, spring);
                    }
                }
            }
        }

        private void addValidTarget(ComponentSpring source,
                ComponentSpring target) {
            if (matches == null) {
                matches = new ArrayList<AutoPreferredGapMatch>(1);
            }
            matches.add(new AutoPreferredGapMatch(source, target));
        }

        int calculateMinimumSize(int axis) {
            return size;
        }

        int calculatePreferredSize(int axis) {
            if (pref == PREFERRED_SIZE || pref == DEFAULT_SIZE) {
                return size;
            }
            return Math.max(size, pref);
        }

        int calculateMaximumSize(int axis) {
            if (max >= 0) {
                return Math.max(getPreferredSize(axis), max);
            }
            return size;
        }

        String getMatchDescription() {
            return (matches == null) ? "" : matches.toString();
        }

        public String toString() {
            return super.toString() + getMatchDescription();
        }

        @Override
        boolean willHaveZeroSize(boolean treatAutopaddingAsZeroSized) {
            return treatAutopaddingAsZeroSized;
        }
    }


    /**
     * Represents two springs that should have autopadding inserted between
     * them.
     */
    private static final class AutoPreferredGapMatch {
        public final ComponentSpring source;
        public final ComponentSpring target;

        AutoPreferredGapMatch(ComponentSpring source, ComponentSpring target) {
            this.source = source;
            this.target = target;
        }

        private String toString(ComponentSpring spring) {
            return spring.getComponent().getName();
        }

        public String toString() {
            return "[" + toString(source) + "-" + toString(target) + "]";
        }
    }


    /**
     * An extension of AutopaddingSpring used for container level padding.
     */
    private class ContainerAutoPreferredGapSpring extends
            AutoPreferredGapSpring {
        private List<ComponentSpring> targets;

        ContainerAutoPreferredGapSpring() {
            super();
            setUserCreated(true);
        }

        ContainerAutoPreferredGapSpring(int pref, int max) {
            super(pref, max);
            setUserCreated(true);
        }

        public void addTarget(ComponentSpring spring, int axis) {
            if (targets == null) {
                targets = new ArrayList<ComponentSpring>(1);
            }
            targets.add(spring);
        }

        public void calculatePadding(int axis) {
            LayoutStyle p = getLayoutStyle0();
            int maxPadding = 0;
            int position;
            size = 0;
            if (targets != null) {
                // Leading
                if (axis == HORIZONTAL) {
                    if (isLeftToRight()) {
                        position = SwingConstants.WEST;
                    } else {
                        position = SwingConstants.EAST;
                    }
                } else {
                    position = SwingConstants.SOUTH;
                }
                for (int i = targets.size() - 1; i >= 0; i--) {
                    ComponentSpring targetSpring = targets.get(i);
                    int padding = 10;
                    if (targetSpring.getComponent() instanceof JComponent) {
                        padding = p.getContainerGap(
                                (JComponent)targetSpring.getComponent(),
                                position, host);
                        maxPadding = Math.max(padding, maxPadding);
                        padding -= targetSpring.getOrigin();
                    } else {
                        maxPadding = Math.max(padding, maxPadding);
                    }
                    size = Math.max(size, padding);
                }
            } else {
                // Trailing
                if (axis == HORIZONTAL) {
                    if (isLeftToRight()) {
                        position = SwingConstants.EAST;
                    } else {
                        position = SwingConstants.WEST;
                    }
                } else {
                    position = SwingConstants.SOUTH;
                }
                if (sources != null) {
                    for (int i = sources.size() - 1; i >= 0; i--) {
                        ComponentSpring sourceSpring = sources.get(i);
                        maxPadding = Math.max(maxPadding,
                                updateSize(p, sourceSpring, position));
                    }
                } else if (source != null) {
                    maxPadding = updateSize(p, source, position);
                }
            }
            if (lastSize != UNSET) {
                size += Math.min(maxPadding, lastSize);
            }
        }

        private int updateSize(LayoutStyle p, ComponentSpring sourceSpring,
                int position) {
            int padding = 10;
            if (sourceSpring.getComponent() instanceof JComponent) {
                padding = p.getContainerGap(
                        (JComponent)sourceSpring.getComponent(), position,
                        host);
            }
            int delta = Math.max(0, getParent().getSize() -
                    sourceSpring.getSize() - sourceSpring.getOrigin());
            size = Math.max(size, padding - delta);
            return padding;
        }

        String getMatchDescription() {
            if (targets != null) {
                return "leading: " + targets.toString();
            }
            if (sources != null) {
                return "trailing: " + sources.toString();
            }
            return "--";
        }
    }


    // LinkInfo contains the set of ComponentInfosthat are linked along a
    // particular axis.
    private static class LinkInfo {
        private final int axis;
        private final List<ComponentInfo> linked;
        private int size;

        LinkInfo(int axis) {
            linked = new ArrayList<ComponentInfo>();
            size = UNSET;
            this.axis = axis;
        }

        public void add(ComponentInfo child) {
            LinkInfo childMaster = child.getLinkInfo(axis, false);
            if (childMaster == null) {
                linked.add(child);
                child.setLinkInfo(axis, this);
            } else if (childMaster != this) {
                linked.addAll(childMaster.linked);
                for (ComponentInfo childInfo : childMaster.linked) {
                    childInfo.setLinkInfo(axis, this);
                }
            }
            clearCachedSize();
        }

        public void remove(ComponentInfo info) {
            linked.remove(info);
            info.setLinkInfo(axis, null);
            if (linked.size() == 1) {
                linked.get(0).setLinkInfo(axis, null);
            }
            clearCachedSize();
        }

        public void clearCachedSize() {
            size = UNSET;
        }

        public int getSize(int axis) {
            if (size == UNSET) {
                size = calculateLinkedSize(axis);
            }
            return size;
        }

        private int calculateLinkedSize(int axis) {
            int size = 0;
            for (ComponentInfo info : linked) {
                ComponentSpring spring;
                if (axis == HORIZONTAL) {
                    spring = info.horizontalSpring;
                } else {
                    assert (axis == VERTICAL);
                    spring = info.verticalSpring;
                }
                size = Math.max(size,
                        spring.calculateNonlinkedPreferredSize(axis));
            }
            return size;
        }
    }

    /**
     * Tracks the horizontal/vertical Springs for a Component.
     * This class is also used to handle Springs that have their sizes
     * linked.
     */
    private class ComponentInfo {
        // Component being layed out
        private Component component;

        ComponentSpring horizontalSpring;
        ComponentSpring verticalSpring;

        // If the component's size is linked to other components, the
        // horizontalMaster and/or verticalMaster reference the group of
        // linked components.
        private LinkInfo horizontalMaster;
        private LinkInfo verticalMaster;

        private boolean visible;
        private Boolean honorsVisibility;

        ComponentInfo(Component component) {
            this.component = component;
            updateVisibility();
        }

        public void dispose() {
            // Remove horizontal/vertical springs
            removeSpring(horizontalSpring);
            horizontalSpring = null;
            removeSpring(verticalSpring);
            verticalSpring = null;
            // Clean up links
            if (horizontalMaster != null) {
                horizontalMaster.remove(this);
            }
            if (verticalMaster != null) {
                verticalMaster.remove(this);
            }
        }

        void setHonorsVisibility(Boolean honorsVisibility) {
            this.honorsVisibility = honorsVisibility;
        }

        private void removeSpring(Spring spring) {
            if (spring != null) {
                ((Group)spring.getParent()).springs.remove(spring);
            }
        }

        public boolean isVisible() {
            return visible;
        }

        /**
         * Updates the cached visibility.
         *
         * @return true if the visibility changed
         */
        boolean updateVisibility() {
            boolean honorsVisibility;
            if (this.honorsVisibility == null) {
                honorsVisibility = GroupLayout.this.getHonorsVisibility();
            } else {
                honorsVisibility = this.honorsVisibility;
            }
            boolean newVisible = (honorsVisibility) ?
                component.isVisible() : true;
            if (visible != newVisible) {
                visible = newVisible;
                return true;
            }
            return false;
        }

        public void setBounds(Insets insets, int parentWidth, boolean ltr) {
            int x = horizontalSpring.getOrigin();
            int w = horizontalSpring.getSize();
            int y = verticalSpring.getOrigin();
            int h = verticalSpring.getSize();

            if (!ltr) {
                x = parentWidth - x - w;
            }
            component.setBounds(x + insets.left, y + insets.top, w, h);
        }

        public void setComponent(Component component) {
            this.component = component;
            if (horizontalSpring != null) {
                horizontalSpring.setComponent(component);
            }
            if (verticalSpring != null) {
                verticalSpring.setComponent(component);
            }
        }

        public Component getComponent() {
            return component;
        }

        /**
         * Returns true if this component has its size linked to
         * other components.
         */
        public boolean isLinked(int axis) {
            if (axis == HORIZONTAL) {
                return horizontalMaster != null;
            }
            assert (axis == VERTICAL);
            return (verticalMaster != null);
        }

        private void setLinkInfo(int axis, LinkInfo linkInfo) {
            if (axis == HORIZONTAL) {
                horizontalMaster = linkInfo;
            } else {
                assert (axis == VERTICAL);
                verticalMaster = linkInfo;
            }
        }

        public LinkInfo getLinkInfo(int axis) {
            return getLinkInfo(axis, true);
        }

        private LinkInfo getLinkInfo(int axis, boolean create) {
            if (axis == HORIZONTAL) {
                if (horizontalMaster == null && create) {
                    // horizontalMaster field is directly set by adding
                    // us to the LinkInfo.
                    new LinkInfo(HORIZONTAL).add(this);
                }
                return horizontalMaster;
            } else {
                assert (axis == VERTICAL);
                if (verticalMaster == null && create) {
                    // verticalMaster field is directly set by adding
                    // us to the LinkInfo.
                    new LinkInfo(VERTICAL).add(this);
                }
                return verticalMaster;
            }
        }

        public void clearCachedSize() {
            if (horizontalMaster != null) {
                horizontalMaster.clearCachedSize();
            }
            if (verticalMaster != null) {
                verticalMaster.clearCachedSize();
            }
        }

        int getLinkSize(int axis, int type) {
            if (axis == HORIZONTAL) {
                return horizontalMaster.getSize(axis);
            } else {
                assert (axis == VERTICAL);
                return verticalMaster.getSize(axis);
            }
        }

    }
}
