/*
 * Copyright (c) 1999, 2007, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jmx.mbeanserver;

import javax.management.* ;



/**
 * This class is used for storing a pair (name, object) where name is
 * an object name and object is a reference to the object.
 *
 * @since 1.5
 */
public class NamedObject  {


    /**
     * Object name.
     */
    private final ObjectName name;

    /**
     * Object reference.
     */
    private final DynamicMBean object;


    /**
     * Allows a named object to be created.
     *
     *@param objectName The object name of the object.
     *@param object A reference to the object.
     */
    public NamedObject(ObjectName objectName, DynamicMBean object)  {
        if (objectName.isPattern()) {
            throw new RuntimeOperationsException(new IllegalArgumentException("Invalid name->"+ objectName.toString()));
        }
        this.name= objectName;
        this.object= object;
    }

    /**
     * Allows a named object to be created.
     *
     *@param objectName The string representation of the object name of the object.
     *@param object A reference to the object.
     *
     *@exception MalformedObjectNameException The string passed does not have the format of a valid ObjectName
     */
    public NamedObject(String objectName, DynamicMBean object) throws MalformedObjectNameException{
        ObjectName objName= new ObjectName(objectName);
        if (objName.isPattern()) {
            throw new RuntimeOperationsException(new IllegalArgumentException("Invalid name->"+ objName.toString()));
        }
        this.name= objName;
        this.object= object;
    }

    /**
     * Compares the current object name with another object name.
     *
     * @param object  The Named Object that the current object name is to be
     *        compared with.
     *
     * @return  True if the two named objects are equal, otherwise false.
     */
    public boolean equals(Object object)  {
        if (this == object) return true;
        if (object == null) return false;
        if (!(object instanceof NamedObject)) return false;
        NamedObject no = (NamedObject) object;
        return name.equals(no.getName());
    }


    /**
     * Returns a hash code for this named object.
     *
     */
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Get the object name.
     */
    public ObjectName getName()  {
        return name;
    }

    /**
     * Get the object
     */
    public DynamicMBean getObject()  {
        return object;
   }

 }
