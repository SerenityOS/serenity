/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.ldap;

import jdk.internal.access.SharedSecrets;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

public final class VersionHelper {

    private static final VersionHelper helper = new VersionHelper();

    /**
     * Determines whether classes may be loaded from an arbitrary URL code base.
     */
    private static final boolean trustURLCodebase;

    /**
     * Determines whether objects may be deserialized from the content of
     * 'javaSerializedData' attribute.
     */
    private static final boolean trustSerialData;

    static {
        // System property to control whether classes may be loaded from an
        // arbitrary URL code base
        String trust = getPrivilegedProperty(
                "com.sun.jndi.ldap.object.trustURLCodebase", "false");
        trustURLCodebase = "true".equalsIgnoreCase(trust);

        // System property to control whether classes is allowed to be loaded from
        // 'javaSerializedData' attribute
        String trustSerialDataSp = getPrivilegedProperty(
                "com.sun.jndi.ldap.object.trustSerialData", "true");
        trustSerialData = "true".equalsIgnoreCase(trustSerialDataSp);
    }

    @SuppressWarnings("removal")
    private static String getPrivilegedProperty(String propertyName, String defaultVal) {
        PrivilegedAction<String> action = () -> System.getProperty(propertyName, defaultVal);
        if (System.getSecurityManager() == null) {
            return action.run();
        } else {
            return AccessController.doPrivileged(action);
        }
    }

    private VersionHelper() {
    }

    static VersionHelper getVersionHelper() {
        return helper;
    }

    /**
     * Returns true if deserialization of objects from 'javaSerializedData'
     * LDAP attribute is allowed.
     *
     * @return true if deserialization is allowed; false - otherwise
     */
    public static boolean isSerialDataAllowed() {
        return trustSerialData;
    }

    ClassLoader getURLClassLoader(String[] url) throws MalformedURLException {
        ClassLoader parent = getContextClassLoader();
        /*
         * Classes may only be loaded from an arbitrary URL code base when
         * the system property com.sun.jndi.ldap.object.trustURLCodebase
         * has been set to "true".
         */
        if (url != null && trustURLCodebase) {
            return URLClassLoader.newInstance(getUrlArray(url), parent);
        } else {
            return parent;
        }
    }

    Class<?> loadClass(String className) throws ClassNotFoundException {
        return Class.forName(className, true, getContextClassLoader());
    }

    @SuppressWarnings("removal")
    Thread createThread(Runnable r) {
        AccessControlContext acc = AccessController.getContext();
        // 4290486: doPrivileged is needed to create a thread in
        // an environment that restricts "modifyThreadGroup".
        PrivilegedAction<Thread> act =
                () -> SharedSecrets.getJavaLangAccess().newThreadWithAcc(r, acc);
        return AccessController.doPrivileged(act);
    }

    @SuppressWarnings("removal")
    private ClassLoader getContextClassLoader() {
        PrivilegedAction<ClassLoader> act =
                Thread.currentThread()::getContextClassLoader;
        return AccessController.doPrivileged(act);
    }

    private static URL[] getUrlArray(String[] url) throws MalformedURLException {
        URL[] urlArray = new URL[url.length];
        for (int i = 0; i < urlArray.length; i++) {
            urlArray[i] = new URL(url[i]);
        }
        return urlArray;
    }
}
