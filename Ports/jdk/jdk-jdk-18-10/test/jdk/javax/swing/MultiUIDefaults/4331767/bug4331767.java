/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4331767
   @summary Tests that custom implementation of UIDefaults.getUIError() is
            called when an UI error occurs
   @author Peter Zhelezniakov
   @run main bug4331767
*/
import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.util.Locale;

public class bug4331767
{
    private static boolean passed = false;

    public static void main(String[] argv) {
        try {
            UIManager.setLookAndFeel(new BrokenLookAndFeel());
        } catch (Exception e) {
            throw new Error("Failed to set BrokenLookAndFeel, cannot test", e);
        }

        // This should call BrokenUIDefaults.getUIError()
        new JButton();

        if (!passed) {
            throw new RuntimeException("Failed: Custom getUIError() not called");
        }
    }

    static class BrokenUIDefaults extends UIDefaults {
        UIDefaults defaults;

        public BrokenUIDefaults(UIDefaults def) {
            defaults = def;
        }

        public Object get(Object key) {
            if ("ButtonUI".equals(key)) {
                System.err.println("[II]  Called BrokenUIDefaults.get(Object)");
                return "a nonexistent class";
            }
            return defaults.get(key);
        }

        public Object get(Object key, Locale l) {
            if ("ButtonUI".equals(key)) {
                System.err.println("[II]  Called BrokenUIDefaults.get(Object, Locale)");
                return "a nonexistent class";
            }
            return defaults.get(key, l);
        }

        protected void getUIError(String msg) {
            System.err.println("[II]  BrokenUIDefaults.getUIError() called, test passes");
            passed = true;
        }
    }

    static class BrokenLookAndFeel extends MetalLookAndFeel {
        UIDefaults defaults;

        public BrokenLookAndFeel() {
            defaults = new BrokenUIDefaults(super.getDefaults());
        }

        public UIDefaults getDefaults() {
            return defaults;
        }
    }
}
