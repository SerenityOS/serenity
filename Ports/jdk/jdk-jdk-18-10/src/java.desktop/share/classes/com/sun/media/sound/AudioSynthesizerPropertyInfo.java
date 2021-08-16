/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.media.sound;

/**
 * Information about property used in  opening {@code AudioSynthesizer}.
 *
 * @author Karl Helgason
 */
public final class AudioSynthesizerPropertyInfo {

    /**
     * Constructs an {@code AudioSynthesizerPropertyInfo} object with a given
     * name and value. The {@code description} and {@code choices}
     * are initialized by {@code null} values.
     *
     * @param name the name of the property
     * @param value the current value or class used for values.
     *
     */
    public AudioSynthesizerPropertyInfo(String name, Object value) {
        this.name = name;
        if (value instanceof Class)
            valueClass = (Class)value;
        else
        {
            this.value = value;
            if (value != null)
                valueClass = value.getClass();
        }
    }
    /**
     * The name of the property.
     */
    public String name;
    /**
     * A brief description of the property, which may be null.
     */
    public String description = null;
    /**
     * The {@code value} field specifies the current value of
     * the property.
     */
    public Object value = null;
    /**
     * The {@code valueClass} field specifies class
     * used in {@code value} field.
     */
    public Class<?> valueClass = null;
    /**
     * An array of possible values if the value for the field
     * {@code AudioSynthesizerPropertyInfo.value} may be selected
     * from a particular set of values; otherwise null.
     */
    public Object[] choices = null;

}
