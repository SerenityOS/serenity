/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.sound.sampled;

import java.util.StringJoiner;

/**
 * A {@code CompoundControl}, such as a graphic equalizer, provides control over
 * two or more related properties, each of which is itself represented as a
 * {@code Control}.
 *
 * @author Kara Kytle
 * @since 1.3
 */
public abstract class CompoundControl extends Control {

    /**
     * The set of member controls.
     */
    private final Control[] controls;

    /**
     * Constructs a new compound control object with the given parameters.
     *
     * @param  type the type of control represented this compound control object
     * @param  memberControls the set of member controls
     */
    protected CompoundControl(Type type, Control[] memberControls) {
        super(type);
        this.controls = memberControls;
    }

    /**
     * Returns the set of member controls that comprise the compound control.
     *
     * @return the set of member controls
     */
    public Control[] getMemberControls() {
        return controls.clone();
    }

    /**
     * Returns a string representation of the compound control.
     *
     * @return a string representation of the compound control
     */
    @Override
    public String toString() {
        StringJoiner controls = new StringJoiner(", ", "[", "]");
        for (Control control : getMemberControls()) {
            controls.add(control.getType().toString());
        }
        return String.format("%s containing %s controls", super.toString(),
                             controls);
    }

    /**
     * An instance of the {@code CompoundControl.Type} inner class identifies
     * one kind of compound control.
     *
     * @author Kara Kytle
     * @since 1.3
     */
    public static class Type extends Control.Type {

        /**
         * Constructs a new compound control type.
         *
         * @param  name the name of the new compound control type
         */
        protected Type(final String name) {
            super(name);
        }
    }
}
