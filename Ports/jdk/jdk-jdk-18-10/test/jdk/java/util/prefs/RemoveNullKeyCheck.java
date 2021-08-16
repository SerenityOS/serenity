/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 7160242 7165118 7197662
 * @summary Check if NullPointerException is thrown if the key passed
 *          to remove() is null.
 * @run main/othervm -Djava.util.prefs.userRoot=. RemoveNullKeyCheck
 */

import java.util.prefs.Preferences;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

public class RemoveNullKeyCheck {

    private static boolean failed = false;

    public static void main(String[] args) throws Exception {
        checkPreferencesRemove();
        checkAbstractPreferencesRemove();
        if (failed) {
            throw new RuntimeException("Expected NullPointerException " +
                                       "not thrown");
        }
    }

    public static void checkPreferencesRemove() {
        try {
            Preferences node = Preferences.userRoot().node("N1");
            node.remove(null);
            failed = true;
        } catch (NullPointerException npe) {
        }
    }

    public static void checkAbstractPreferencesRemove() {

        Preferences abstrPrefs = new AbstractPreferences(null, "") {
            @Override
            protected void putSpi(String key, String value) {
            }
            @Override
            protected String getSpi(String key) {
                return null;
            }
            @Override
            protected void removeSpi(String key) {
            }
            @Override
            protected void removeNodeSpi() throws BackingStoreException {
            }
            @Override
            protected String[] keysSpi() throws BackingStoreException {
                return new String[0];
            }
            @Override
            protected String[] childrenNamesSpi() throws BackingStoreException {
                return new String[0];
            }
            @Override
            protected AbstractPreferences childSpi(String name) {
                return null;
            }
            @Override
            protected void syncSpi() throws BackingStoreException {
            }
            @Override
            protected void flushSpi() throws BackingStoreException {
            }
        };

        try {
            abstrPrefs.remove(null);
            failed = true;
        } catch(NullPointerException npe) {
        }
    }
}
