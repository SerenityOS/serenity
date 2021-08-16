/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

package javax.management;


// java import
import java.io.Serializable;


/**
 * Represents an MBean attribute by associating its name with its value.
 * The MBean server and other objects use this class to get and set attributes values.
 *
 * @since 1.5
 */
public class Attribute implements Serializable   {

    /* Serial version */
    private static final long serialVersionUID = 2484220110589082382L;

    /**
     * @serial Attribute name.
     */
    private String name;

    /**
     * @serial Attribute value
     */
    @SuppressWarnings("serial") // Conditionally serializable
    private Object value= null;


    /**
     * Constructs an Attribute object which associates the given attribute name with the given value.
     *
     * @param name A String containing the name of the attribute to be created. Cannot be null.
     * @param value The Object which is assigned to the attribute. This object must be of the same type as the attribute.
     *
     */
    public Attribute(String name, Object value) {

        if (name == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException("Attribute name cannot be null "));
        }

        this.name = name;
        this.value = value;
    }


    /**
     * Returns a String containing the  name of the attribute.
     *
     * @return the name of the attribute.
     */
    public String getName()  {
        return name;
    }

    /**
     * Returns an Object that is the value of this attribute.
     *
     * @return the value of the attribute.
     */
    public Object getValue()  {
        return value;
    }

    /**
     * Compares the current Attribute Object with another Attribute Object.
     *
     * @param object  The Attribute that the current Attribute is to be compared with.
     *
     * @return  True if the two Attribute objects are equal, otherwise false.
     */


    public boolean equals(Object object)  {
        if (!(object instanceof Attribute)) {
            return false;
        }
        Attribute val = (Attribute) object;

        if (value == null) {
            if (val.getValue() == null) {
                return name.equals(val.getName());
            } else {
                return false;
            }
        }

        return ((name.equals(val.getName())) &&
                (value.equals(val.getValue())));
    }

    /**
     * Returns a hash code value for this attribute.
     *
     * @return a hash code value for this attribute.
     */
    public int hashCode() {
        return name.hashCode() ^ (value == null ? 0 : value.hashCode());
    }

    /**
     * Returns a String object representing this Attribute's value. The format of this
     * string is not specified, but users can expect that two Attributes return the
     * same string if and only if they are equal.
     */
    public String toString() {
        return getName() + " = " + getValue();
    }
 }
