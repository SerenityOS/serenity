/*
 * Copyright (c) 1999, 2004, Oracle and/or its affiliates. All rights reserved.
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

// RI import
import javax.management.ObjectName;


/**
 * Used to represent the object name of an MBean and its class name.
 * If the MBean is a Dynamic MBean the class name should be retrieved from
 * the <CODE>MBeanInfo</CODE> it provides.
 *
 * @since 1.5
 */
public class ObjectInstance implements Serializable   {


    /* Serial version */
    private static final long serialVersionUID = -4099952623687795850L;

    /**
     * @serial Object name.
     */
    private ObjectName name;

    /**
     * @serial Class name.
     */
    private String className;

    /**
     * Allows an object instance to be created given a string representation of
     * an object name and the full class name, including the package name.
     *
     * @param objectName  A string representation of the object name.
     * @param className The full class name, including the package
     * name, of the object instance.  If the MBean is a Dynamic MBean
     * the class name corresponds to its {@link
     * DynamicMBean#getMBeanInfo()
     * getMBeanInfo()}<code>.getClassName()</code>.
     *
     * @exception MalformedObjectNameException The string passed as a
     * parameter does not have the right format.
     *
     */
    public ObjectInstance(String objectName, String className)
            throws MalformedObjectNameException {
        this(new ObjectName(objectName), className);
    }

    /**
     * Allows an object instance to be created given an object name and
     * the full class name, including the package name.
     *
     * @param objectName  The object name.
     * @param className  The full class name, including the package
     * name, of the object instance.  If the MBean is a Dynamic MBean
     * the class name corresponds to its {@link
     * DynamicMBean#getMBeanInfo()
     * getMBeanInfo()}<code>.getClassName()</code>.
     * If the MBean is a Dynamic MBean the class name should be retrieved
     * from the <CODE>MBeanInfo</CODE> it provides.
     *
     */
    public ObjectInstance(ObjectName objectName, String className) {
        if (objectName.isPattern()) {
            final IllegalArgumentException iae =
                new IllegalArgumentException("Invalid name->"+
                                             objectName.toString());
            throw new RuntimeOperationsException(iae);
        }
        this.name= objectName;
        this.className= className;
    }


    /**
     * Compares the current object instance with another object instance.
     *
     * @param object  The object instance that the current object instance is
     *     to be compared with.
     *
     * @return  True if the two object instances are equal, otherwise false.
     */
    public boolean equals(Object object)  {
        if (!(object instanceof ObjectInstance)) {
            return false;
        }
        ObjectInstance val = (ObjectInstance) object;
        if (! name.equals(val.getObjectName())) return false;
        if (className == null)
            return (val.getClassName() == null);
        return className.equals(val.getClassName());
    }

    public int hashCode() {
        final int classHash = ((className==null)?0:className.hashCode());
        return name.hashCode() ^ classHash;
    }

    /**
     * Returns the object name part.
     *
     * @return the object name.
     */
    public ObjectName getObjectName()  {
        return name;
    }

    /**
     * Returns the class part.
     *
     * @return the class name.
     */
    public String getClassName()  {
        return className;
    }

    /**
     * Returns a string representing this ObjectInstance object. The format of this string
     * is not specified, but users can expect that two ObjectInstances return the same
     * string if and only if they are equal.
     */
    public String toString() {
        return getClassName() + "[" + getObjectName() + "]";
    }
 }
