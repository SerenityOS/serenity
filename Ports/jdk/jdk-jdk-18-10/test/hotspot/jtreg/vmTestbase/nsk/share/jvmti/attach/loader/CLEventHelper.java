/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
/*
 * CLEventHelper.java
 *
 * Created on June 3, 2005, 1:21 PM
 *
 */

package nsk.share.jvmti.attach.loader;
/**
 * This class Object is created to make sure that all the classload
 * and unload events are properly called.
 * Depenening on Requirement This class is Loaded / Unloaded as many times
 * as possible. This gives a simple / and same class to load and unload
 * mutiple times.
 * And the events are called acurately.
 * Insted of writing a same javac class and missings its load operation.
 */
public class CLEventHelper {

    /**
     * Creates a new instance of CLEventHelper
     */
    public CLEventHelper() {

    }
    // This static method is a dummy one. which Opens some
    // class in this package
     static {
         //EventObject object = new EventObject(" Class-Load-Helper");
     }

}
