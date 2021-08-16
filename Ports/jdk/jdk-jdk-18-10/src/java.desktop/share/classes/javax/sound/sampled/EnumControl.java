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

/**
 * An {@code EnumControl} provides control over a set of discrete possible
 * values, each represented by an object. In a graphical user interface, such a
 * control might be represented by a set of buttons, each of which chooses one
 * value or setting. For example, a reverb control might provide several preset
 * reverberation settings, instead of providing continuously adjustable
 * parameters of the sort that would be represented by {@link FloatControl}
 * objects.
 * <p>
 * Controls that provide a choice between only two settings can often be
 * implemented instead as a {@link BooleanControl}, and controls that provide a
 * set of values along some quantifiable dimension might be implemented instead
 * as a {@code FloatControl} with a coarse resolution. However, a key feature of
 * {@code EnumControl} is that the returned values are arbitrary objects, rather
 * than numerical or boolean values. This means that each returned object can
 * provide further information. As an example, the settings of a
 * {@link EnumControl.Type#REVERB REVERB} control are instances of
 * {@link ReverbType} that can be queried for the parameter values used for each
 * setting.
 *
 * @author Kara Kytle
 * @since 1.3
 */
public abstract class EnumControl extends Control {

    /**
     * The set of possible values.
     */
    private final Object[] values;

    /**
     * The current value.
     */
    private Object value;

    /**
     * Constructs a new enumerated control object with the given parameters.
     *
     * @param  type the type of control represented this enumerated control
     *         object
     * @param  values the set of possible values for the control
     * @param  value the initial control value
     */
    protected EnumControl(Type type, Object[] values, Object value) {
        super(type);
        this.values = values;
        this.value = value;
    }

    /**
     * Sets the current value for the control. The default implementation simply
     * sets the value as indicated. If the value indicated is not supported, an
     * {@code IllegalArgumentException} is thrown. Some controls require that
     * their line be open before they can be affected by setting a value.
     *
     * @param  value the desired new value
     * @throws IllegalArgumentException if the value indicated does not fall
     *         within the allowable range
     */
    public void setValue(Object value) {
        if (!isValueSupported(value)) {
            throw new IllegalArgumentException("Requested value " + value + " is not supported.");
        }

        this.value = value;
    }

    /**
     * Obtains this control's current value.
     *
     * @return the current value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Returns the set of possible values for this control.
     *
     * @return the set of possible values
     */
    public Object[] getValues() {
        return values.clone();
    }

    /**
     * Indicates whether the value specified is supported.
     *
     * @param  value the value for which support is queried
     * @return {@code true} if the value is supported, otherwise {@code false}
     */
    private boolean isValueSupported(Object value) {

        for (int i = 0; i < values.length; i++) {
            //$$fb 2001-07-20: Fix for bug 4400392: setValue() in ReverbControl always throws Exception
            //if (values.equals(values[i])) {
            if (value.equals(values[i])) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns a string representation of the enumerated control.
     *
     * @return a string representation of the enumerated control
     */
    @Override
    public String toString() {
        return String.format("%s with current value: %s", super.toString(),
                             getValue());
    }

    /**
     * An instance of the {@code EnumControl.Type} inner class identifies one
     * kind of enumerated control. Static instances are provided for the common
     * types.
     *
     * @author Kara Kytle
     * @see EnumControl
     * @since 1.3
     */
    public static class Type extends Control.Type {

        /**
         * Represents a control over a set of possible reverberation settings.
         * Each reverberation setting is described by an instance of the
         * {@link ReverbType} class. (To access these settings, invoke
         * {@link EnumControl#getValues} on an enumerated control of type
         * {@code REVERB}.)
         */
        public static final Type REVERB = new Type("Reverb");

        /**
         * Constructs a new enumerated control type.
         *
         * @param  name the name of the new enumerated control type
         */
        protected Type(final String name) {
            super(name);
        }
    }
}
