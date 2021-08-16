/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.java.accessibility.util;

import java.util.*;
import java.beans.*;
import java.awt.*;
import java.awt.event.*;
import javax.accessibility.*;

/**
 * <P>The {@code AccessibilityListenerList} is a copy of the Swing
 * {@link javax.swing.event.EventListenerList EventListerList} class.
 *
 */

public class AccessibilityListenerList {
    /* A null array to be shared by all empty listener lists */
    private final static Object[] NULL_ARRAY = new Object[0];

    /**
     * The list of listener type, listener pairs
     */
    protected transient Object[] listenerList = NULL_ARRAY;

    /**
     * Constructs an {@code AccessibilityListenerList}.
     */
    public AccessibilityListenerList() {}

    /**
     * Passes back the event listener list as an array of listener type, listener pairs.
     * Note that for performance reasons, this implementation passes back the actual
     * data structure in which the listener data is stored internally. This method
     * is guaranteed to pass back a non-null array, so that no null-checking
     * is required in fire methods. A zero-length array of Object is returned if
     * there are currently no listeners.
     * <p>
     * Absolutely no modification of the data contained in this array should be
     * made.  If any such manipulation is necessary, it should be done on a copy
     * of the array returned rather than the array itself.
     *
     * @return an array of listener type, listener pairs.
     */
    public Object[] getListenerList() {
        return listenerList;
    }

    /**
     * Returns the total number of listeners for this listener list.
     *
     * @return the total number of listeners for this listener list.
     */
    public int getListenerCount() {
        return listenerList.length/2;
    }

    /**
     * Return the total number of listeners of the supplied type
     * for this listener list.
     *
     * @param t the type of the listener to be counted
     * @return the number of listeners found
     */
    public int getListenerCount(Class<? extends EventListener> t) {
        int count = 0;
        Object[] lList = listenerList;
        for (int i = 0; i < lList.length; i+=2) {
            if (t == (Class)lList[i])
                count++;
        }
        return count;
    }

    /**
     * Add the listener as a listener of the specified type.
     *
     * @param t the type of the listener to be added
     * @param l the listener to be added
     */
    public synchronized void add(Class<? extends EventListener> t, EventListener l) {
        if (!t.isInstance(l)) {
            throw new IllegalArgumentException("Listener " + l +
                                         " is not of type " + t);
        }
        if (l ==null) {
            throw new IllegalArgumentException("Listener " + l +
                                         " is null");
        }
        if (listenerList == NULL_ARRAY) {
            // if this is the first listener added,
            // initialize the lists
            listenerList = new Object[] { t, l };
        } else {
            // Otherwise copy the array and add the new listener
            int i = listenerList.length;
            Object[] tmp = new Object[i+2];
            System.arraycopy(listenerList, 0, tmp, 0, i);

            tmp[i] = t;
            tmp[i+1] = l;

            listenerList = tmp;
        }
    }

    /**
     * Remove the listener as a listener of the specified type.
     *
     * @param t the type of the listener to be removed
     * @param l the listener to be removed
     */
    public synchronized void remove(Class<? extends EventListener> t, EventListener l) {
        if (!t.isInstance(l)) {
            throw new IllegalArgumentException("Listener " + l +
                                         " is not of type " + t);
        }
        if (l ==null) {
            throw new IllegalArgumentException("Listener " + l +
                                         " is null");
        }

        // Is l on the list?
        int index = -1;
        for (int i = listenerList.length-2; i>=0; i-=2) {
            if ((listenerList[i]==t) && (listenerList[i+1] == l)) {
                index = i;
                break;
            }
        }

        // If so,  remove it
        if (index != -1) {
            Object[] tmp = new Object[listenerList.length-2];
            // Copy the list up to index
            System.arraycopy(listenerList, 0, tmp, 0, index);
            // Copy from two past the index, up to
            // the end of tmp (which is two elements
            // shorter than the old list)
            if (index < tmp.length)
                System.arraycopy(listenerList, index+2, tmp, index,
                                 tmp.length - index);
            // set the listener array to the new array or null
            listenerList = (tmp.length == 0) ? NULL_ARRAY : tmp;
            }
    }

    /**
     * Return a string representation of the {@code AccessibilityListenerList}.
     *
     * @return a string representation of the {@code AccessibilityListenerList}.
     */
    public String toString() {
        Object[] lList = listenerList;
        String s = "EventListenerList: ";
        s += lList.length/2 + " listeners: ";
        for (int i = 0 ; i <= lList.length-2 ; i+=2) {
            s += " type " + ((Class)lList[i]).getName();
            s += " listener " + lList[i+1];
        }
        return s;
    }
}
