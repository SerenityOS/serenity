/*
 * Copyright (c) 2003, 2008, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.beans.BeanDescriptor;
import java.beans.EventSetDescriptor;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;

/**
 * This class contains utilities useful for JavaBeans regression testing.
 */
public final class BeanUtils {
    /**
     * Disables instantiation.
     */
    private BeanUtils() {
    }

    /**
     * Returns a bean descriptor for specified class.
     *
     * @param type  the class to introspect
     * @return a bean descriptor
     */
    public static BeanDescriptor getBeanDescriptor(Class type) {
        try {
            return Introspector.getBeanInfo(type).getBeanDescriptor();
        } catch (IntrospectionException exception) {
            throw new Error("unexpected exception", exception);
        }
    }

    /**
     * Returns an array of property descriptors for specified class.
     *
     * @param type  the class to introspect
     * @return an array of property descriptors
     */
    public static PropertyDescriptor[] getPropertyDescriptors(Class type) {
        try {
            return Introspector.getBeanInfo(type).getPropertyDescriptors();
        } catch (IntrospectionException exception) {
            throw new Error("unexpected exception", exception);
        }
    }

    /**
     * Returns an array of event set descriptors for specified class.
     *
     * @param type  the class to introspect
     * @return an array of event set descriptors
     */
    public static EventSetDescriptor[] getEventSetDescriptors(Class type) {
        try {
            return Introspector.getBeanInfo(type).getEventSetDescriptors();
        } catch (IntrospectionException exception) {
            throw new Error("unexpected exception", exception);
        }
    }

    /**
     * Finds an event set descriptor for the class
     * that matches the event set name.
     *
     * @param type  the class to introspect
     * @param name  the name of the event set to search
     * @return the {@code EventSetDescriptor} or {@code null}
     */
    public static EventSetDescriptor findEventSetDescriptor(Class type, String name) {
        EventSetDescriptor[] esds = getEventSetDescriptors(type);
        for (EventSetDescriptor esd : esds) {
            if (esd.getName().equals(name)) {
                return esd;
            }
        }
        return null;
    }

    /**
     * Finds a property descriptor for the class
     * that matches the property name.
     *
     * @param type the class to introspect
     * @param name the name of the property to search
     * @return the {@code PropertyDescriptor}, {@code IndexedPropertyDescriptor} or {@code null}
     */
    public static PropertyDescriptor findPropertyDescriptor(Class type, String name) {
        PropertyDescriptor[] pds = getPropertyDescriptors(type);
        for (PropertyDescriptor pd : pds) {
            if (pd.getName().equals(name)) {
                return pd;
            }
        }
        return null;
    }

    /**
     * Returns a event set descriptor for the class
     * that matches the property name.
     *
     * @param type the class to introspect
     * @param name the name of the event set to search
     * @return the {@code EventSetDescriptor}
     */
    public static EventSetDescriptor getEventSetDescriptor(Class type, String name) {
        EventSetDescriptor esd = findEventSetDescriptor(type, name);
        if (esd != null) {
            return esd;
        }
        throw new Error("could not find event set '" + name + "' in " + type);
    }

    /**
     * Returns a property descriptor for the class
     * that matches the property name.
     *
     * @param type the class to introspect
     * @param name the name of the property to search
     * @return the {@code PropertyDescriptor}
     */
    public static PropertyDescriptor getPropertyDescriptor(Class type, String name) {
        PropertyDescriptor pd = findPropertyDescriptor(type, name);
        if (pd != null) {
            return pd;
        }
        throw new Error("could not find property '" + name + "' in " + type);
    }

    /**
     * Returns an indexed property descriptor for the class
     * that matches the property name.
     *
     * @param type  the class to introspect
     * @param name  the name of the property to search
     * @return the {@code IndexedPropertyDescriptor}
     */
    public static IndexedPropertyDescriptor getIndexedPropertyDescriptor(Class type, String name) {
        PropertyDescriptor pd = findPropertyDescriptor(type, name);
        if (pd instanceof IndexedPropertyDescriptor) {
            return (IndexedPropertyDescriptor) pd;
        }
        reportPropertyDescriptor(pd);
        throw new Error("could not find indexed property '" + name + "' in " + type);
    }

    /**
     * Reports all the interesting information in an Indexed/PropertyDescrptor.
     */
    public static void reportPropertyDescriptor(PropertyDescriptor pd) {
        System.out.println("property name:  " + pd.getName());
        System.out.println("         type:  " + pd.getPropertyType());
        System.out.println("         read:  " + pd.getReadMethod());
        System.out.println("         write: " + pd.getWriteMethod());
        if (pd instanceof IndexedPropertyDescriptor) {
            IndexedPropertyDescriptor ipd = (IndexedPropertyDescriptor) pd;
            System.out.println(" indexed type: " + ipd.getIndexedPropertyType());
            System.out.println(" indexed read: " + ipd.getIndexedReadMethod());
            System.out.println(" indexed write: " + ipd.getIndexedWriteMethod());
        }
    }

    /**
     * Reports all the interesting information in an EventSetDescriptor
     */
    public static void reportEventSetDescriptor(EventSetDescriptor esd) {
        System.out.println("event set name:   " + esd.getName());
        System.out.println(" listener type:   " + esd.getListenerType());
        System.out.println("   method get:    " + esd.getGetListenerMethod());
        System.out.println("   method add:    " + esd.getAddListenerMethod());
        System.out.println("   method remove: " + esd.getRemoveListenerMethod());
    }

    /**
     * Reports all the interesting information in a MethodDescriptor
     */
    public static void reportMethodDescriptor(MethodDescriptor md) {
        System.out.println("method name: " + md.getName());
        System.out.println("     method: " + md.getMethod());
    }
}
