/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
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

package org.netbeans.jemmy.util;

import javax.swing.UIManager;

/**
 * Class to provide look and feel related utility APIs
 */
public class LookAndFeel {

    /**
     * Checking whether the current look and feel is Metal L&F
     *
     * @return returns true if current look and feel is Metal L&F,
     *         otherwise false
     */
    public static boolean isMetal() {
        return isLookAndFeel("Metal");
    }

    /**
     * Checking whether the current look and feel is Nimbus L&F
     *
     * @return returns true if current look and feel is Nimbus L&F,
     *         otherwise false
     */
    public static boolean isNimbus() {
        return isLookAndFeel("Nimbus");
    }

    /**
     * Checking whether the current look and feel is Motif L&F
     *
     * @return returns true if current look and feel is Motif L&F,
     *         otherwise false
     */
    public static boolean isMotif() {
        return isLookAndFeel("Motif");
    }

    /**
     * Checking whether the current look and feel is GTK L&F
     *
     * @return returns true if current look and feel is GTK L&F,
     *         otherwise false
     */
    public static boolean isGTK() {
        return isLookAndFeel("GTK");
    }

    /**
     * Checking whether the current look and feel is Aqua L&F
     *
     * @return returns true if current look and feel is Aqua L&F,
     *         otherwise false
     */
    public static boolean isAqua() {
        return isLookAndFeel("Aqua");
    }

    /**
     * Checking whether the current look and feel is Windows L&F
     *
     * @return returns true if current look and feel is Windows L&F,
     *         otherwise false
     */
    public static boolean isWindows() {
        return UIManager.getLookAndFeel().getClass().
                getSimpleName().equals("WindowsLookAndFeel");
    }

    /**
     * Checking whether the current look and feel is WindowsClassic L&F
     *
     * @return returns true if current look and feel is WindowsClassic L&F,
     *         otherwise false
     */
    public static boolean isWindowsClassic() {
        return UIManager.getLookAndFeel().getClass().
                getSimpleName().equals("WindowsClassicLookAndFeel");
    }

    private static boolean isLookAndFeel(String id) {
        return UIManager.getLookAndFeel().getID().equals(id);
    }
}
