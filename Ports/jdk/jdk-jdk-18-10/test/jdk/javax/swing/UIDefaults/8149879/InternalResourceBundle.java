/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.EventQueue;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

/**
 * @test
 * @bug 8149879
 * @summary The users should not register internal resource bundles.
 */
public class InternalResourceBundle {

    public static void main(final String[] args) throws Exception {
        EventQueue.invokeAndWait(() -> {
            // Indirectly register resource bundle from Nimbus, it will be used
            // by default
            try {
                UIManager.setLookAndFeel(new NimbusLookAndFeel());
            } catch (final UnsupportedLookAndFeelException e) {
                throw new RuntimeException(e);
            }
            UIDefaults defaults = UIManager.getDefaults();
            // Try to register resource bundle from Metal, which is
            // not enabled by default. This request should be skipped.
            defaults.addResourceBundle("com.sun.swing.internal.plaf.metal.resources.metal");

            Object value = getValue(defaults);
            if (value != null) {
                throw new RuntimeException("value is not null = " + value);
            }

            // Indirectly register resource bundle from Metal
            try {
                UIManager.setLookAndFeel(new MetalLookAndFeel());
            } catch (final UnsupportedLookAndFeelException e) {
                throw new RuntimeException(e);
            }
            value = getValue(defaults);
            if (value == null) {
                throw new RuntimeException("value is null");
            }
        });
    }

    private static Object getValue(UIDefaults defaults) {
        return defaults.get("MetalTitlePane.restore.titleAndMnemonic");
    }
}
