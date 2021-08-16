/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.accessibility;

/**
 * Class {@code AccessibleRelation} describes a relation between the object that
 * implements the {@code AccessibleRelation} and one or more other objects. The
 * actual relations that an object has with other objects are defined as an
 * {@code AccessibleRelationSet}, which is a composed set of
 * {@code AccessibleRelations}.
 * <p>
 * The {@link #toDisplayString()} method allows you to obtain the localized
 * string for a locale independent key from a predefined {@code ResourceBundle}
 * for the keys defined in this class.
 * <p>
 * The constants in this class present a strongly typed enumeration of common
 * object roles. If the constants in this class are not sufficient to describe
 * the role of an object, a subclass should be generated from this class and it
 * should provide constants in a similar manner.
 *
 * @author Lynn Monsanto
 * @since 1.3
 */
public class AccessibleRelation extends AccessibleBundle {

    /**
     * The group of objects that participate in the relation. The relation may
     * be one-to-one or one-to-many. For example, in the case of a
     * {@code LABEL_FOR} relation, the target vector would contain a list of
     * objects labeled by the object that implements this
     * {@code AccessibleRelation}. In the case of a {@code MEMBER_OF} relation,
     * the target vector would contain all of the components that are members of
     * the same group as the object that implements this
     * {@code AccessibleRelation}.
     */
    private Object [] target = new Object[0];

    /**
     * Indicates an object is a label for one or more target objects.
     *
     * @see #getTarget
     * @see #CONTROLLER_FOR
     * @see #CONTROLLED_BY
     * @see #LABELED_BY
     * @see #MEMBER_OF
     */
    public static final String LABEL_FOR = new String("labelFor");

    /**
     * Indicates an object is labeled by one or more target objects.
     *
     * @see #getTarget
     * @see #CONTROLLER_FOR
     * @see #CONTROLLED_BY
     * @see #LABEL_FOR
     * @see #MEMBER_OF
     */
    public static final String LABELED_BY = new String("labeledBy");

    /**
     * Indicates an object is a member of a group of one or more target objects.
     *
     * @see #getTarget
     * @see #CONTROLLER_FOR
     * @see #CONTROLLED_BY
     * @see #LABEL_FOR
     * @see #LABELED_BY
     */
    public static final String MEMBER_OF = new String("memberOf");

    /**
     * Indicates an object is a controller for one or more target objects.
     *
     * @see #getTarget
     * @see #CONTROLLED_BY
     * @see #LABEL_FOR
     * @see #LABELED_BY
     * @see #MEMBER_OF
     */
    public static final String CONTROLLER_FOR = new String("controllerFor");

    /**
     * Indicates an object is controlled by one or more target objects.
     *
     * @see #getTarget
     * @see #CONTROLLER_FOR
     * @see #LABEL_FOR
     * @see #LABELED_BY
     * @see #MEMBER_OF
     */
    public static final String CONTROLLED_BY = new String("controlledBy");

    /**
     * Indicates an object is logically contiguous with a second object where
     * the second object occurs after the object. An example is a paragraph of
     * text that runs to the end of a page and continues on the next page with
     * an intervening text footer and/or text header. The two parts of the
     * paragraph are separate text elements but are related in that the second
     * element is a continuation of the first element. In other words, the first
     * element "flows to" the second element.
     *
     * @since 1.5
     */
    public static final String FLOWS_TO = "flowsTo";

    /**
     * Indicates an object is logically contiguous with a second object where
     * the second object occurs before the object. An example is a paragraph of
     * text that runs to the end of a page and continues on the next page with
     * an intervening text footer and/or text header. The two parts of the
     * paragraph are separate text elements but are related in that the second
     * element is a continuation of the first element. In other words, the
     * second element "flows from" the second element.
     *
     * @since 1.5
     */
    public static final String FLOWS_FROM = "flowsFrom";

    /**
     * Indicates that an object is a subwindow of one or more objects.
     *
     * @since 1.5
     */
    public static final String SUBWINDOW_OF = "subwindowOf";

    /**
     * Indicates that an object is a parent window of one or more objects.
     *
     * @since 1.5
     */
    public static final String PARENT_WINDOW_OF = "parentWindowOf";

    /**
     * Indicates that an object has one or more objects embedded in it.
     *
     * @since 1.5
     */
    public static final String EMBEDS = "embeds";

    /**
     * Indicates that an object is embedded in one or more objects.
     *
     * @since 1.5
     */
    public static final String EMBEDDED_BY = "embeddedBy";

    /**
     * Indicates that an object is a child node of one or more objects.
     *
     * @since 1.5
     */
    public static final String CHILD_NODE_OF = "childNodeOf";

    /**
     * Identifies that the target group for a label has changed.
     */
    public static final String LABEL_FOR_PROPERTY = "labelForProperty";

    /**
     * Identifies that the objects that are doing the labeling have changed.
     */
    public static final String LABELED_BY_PROPERTY = "labeledByProperty";

    /**
     * Identifies that group membership has changed.
     */
    public static final String MEMBER_OF_PROPERTY = "memberOfProperty";

    /**
     * Identifies that the controller for the target object has changed.
     */
    public static final String CONTROLLER_FOR_PROPERTY = "controllerForProperty";

    /**
     * Identifies that the target object that is doing the controlling has
     * changed.
     */
    public static final String CONTROLLED_BY_PROPERTY = "controlledByProperty";

    /**
     * Indicates the {@code FLOWS_TO} relation between two objects has changed.
     *
     * @since 1.5
     */
    public static final String FLOWS_TO_PROPERTY = "flowsToProperty";

    /**
     * Indicates the {@code FLOWS_FROM} relation between two objects has
     * changed.
     *
     * @since 1.5
     */
    public static final String FLOWS_FROM_PROPERTY = "flowsFromProperty";

    /**
     * Indicates the {@code SUBWINDOW_OF} relation between two or more objects
     * has changed.
     *
     * @since 1.5
     */
    public static final String SUBWINDOW_OF_PROPERTY = "subwindowOfProperty";

    /**
     * Indicates the {@code PARENT_WINDOW_OF} relation between two or more
     * objects has changed.
     *
     * @since 1.5
     */
    public static final String PARENT_WINDOW_OF_PROPERTY = "parentWindowOfProperty";

    /**
     * Indicates the {@code EMBEDS} relation between two or more objects has
     * changed.
     *
     * @since 1.5
     */
    public static final String EMBEDS_PROPERTY = "embedsProperty";

    /**
     * Indicates the {@code EMBEDDED_BY} relation between two or more objects
     * has changed.
     *
     * @since 1.5
     */
    public static final String EMBEDDED_BY_PROPERTY = "embeddedByProperty";

    /**
     * Indicates the {@code CHILD_NODE_OF} relation between two or more objects
     * has changed.
     *
     * @since 1.5
     */
    public static final String CHILD_NODE_OF_PROPERTY = "childNodeOfProperty";

    /**
     * Create a new {@code AccessibleRelation} using the given locale
     * independent key. The key {@code String} should be a locale independent
     * key for the relation. It is not intended to be used as the actual
     * {@code String} to display to the user. To get the localized string, use
     * {@link #toDisplayString()}.
     *
     * @param  key the locale independent name of the relation
     * @see AccessibleBundle#toDisplayString
     */
    public AccessibleRelation(String key) {
        this.key = key;
        this.target = null;
    }

    /**
     * Creates a new {@code AccessibleRelation} using the given locale
     * independent key. The key {@code String} should be a locale independent
     * key for the relation. It is not intended to be used as the actual
     * {@code String} to display to the user. To get the localized string, use
     * {@link #toDisplayString()}.
     *
     * @param  key the locale independent name of the relation
     * @param  target the target object for this relation
     * @see AccessibleBundle#toDisplayString
     */
    public AccessibleRelation(String key, Object target) {
        this.key = key;
        this.target = new Object[1];
        this.target[0] = target;
    }

    /**
     * Creates a new {@code AccessibleRelation} using the given locale
     * independent key. The key {@code String} should be a locale independent
     * key for the relation. It is not intended to be used as the actual
     * {@code String} to display to the user. To get the localized string, use
     * {@link #toDisplayString()}.
     *
     * @param  key the locale independent name of the relation
     * @param  target the target object(s) for this relation
     * @see AccessibleBundle#toDisplayString
     */
    public AccessibleRelation(String key, Object[] target) {
        this.key = key;
        this.target = target;
    }

    /**
     * Returns the key for this relation.
     *
     * @return the key for this relation
     * @see #CONTROLLER_FOR
     * @see #CONTROLLED_BY
     * @see #LABEL_FOR
     * @see #LABELED_BY
     * @see #MEMBER_OF
     */
    public String getKey() {
        return this.key;
    }

    /**
     * Returns the target objects for this relation.
     *
     * @return an array containing the target objects for this relation
     */
    public Object [] getTarget() {
        if (target == null) {
            target = new Object[0];
        }
        Object [] retval = new Object[target.length];
        for (int i = 0; i < target.length; i++) {
            retval[i] = target[i];
        }
        return retval;
    }

    /**
     * Sets the target object for this relation.
     *
     * @param  target the target object for this relation
     */
    public void setTarget(Object target) {
        this.target = new Object[1];
        this.target[0] = target;
    }

    /**
     * Sets the target objects for this relation.
     *
     * @param  target an array containing the target objects for this relation
     */
    public void setTarget(Object [] target) {
        this.target = target;
    }
}
