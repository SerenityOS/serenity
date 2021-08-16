/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.java.accessibility.internal;

import javax.accessibility.AccessibilityProvider;

/* This class provided methods to identify and activate the mapping from the
 * JavaAccessBridge API to the Java Accessibility API.
 */
public final class ProviderImpl extends AccessibilityProvider {
    /**
     * Typically the service name returned by the name() method would be a simple
     * name such as JavaAccessBridge, but the following name is used for compatibility
     * with prior versions of ${user.home}/.accessibility.properties and
     * ${java.home}/conf/accessibility.properties where the text on the
     * assistive.technologies= line is a fully qualified class name. As of Java 9
     * class names are no longer used to identify assistive technology implementations.
     * If the properties file exists the installer will not replace it thus the
     * need for compatibility.
     */
    private final String name = "com.sun.java.accessibility.AccessBridge";

    public ProviderImpl() {}

    public String getName() {
        return name;
    }

    public void activate() {
        /**
         * Note that the AccessBridge is instantiated here rather than in the
         * constructor.  If the caller determines that this object is named
         * "com.sun.java.accessibility.AccessBridge" then the caller will call
         * start to instantiate the AccessBridge which will in turn activate it.
         */
        new AccessBridge();
    }

}
