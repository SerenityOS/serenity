/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 7143614
 * @summary Issues with Synth Look&Feel
 * @author Pavel Porvatov
 * @modules java.desktop/javax.swing.plaf.synth:open
 * @modules java.desktop/sun.awt
 */

import sun.awt.SunToolkit;

import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.synth.SynthConstants;
import javax.swing.plaf.synth.SynthLookAndFeel;
import java.lang.reflect.Method;

public class bug7143614 {
    private static Method setSelectedUIMethod;

    private static ComponentUI componentUI = new BasicButtonUI();

    public static void main(String[] args) throws Exception {
        setSelectedUIMethod = SynthLookAndFeel.class.getDeclaredMethod("setSelectedUI", ComponentUI.class,
                boolean.class, boolean.class, boolean.class, boolean.class);
        setSelectedUIMethod.setAccessible(true);

        setSelectedUIMethod.invoke(null, componentUI, true, true, true, true);

        validate();

        Thread thread = new ThreadInAnotherAppContext();

        thread.start();
        thread.join();

        validate();

        System.out.println("Test bug7143614 passed.");
    }

    private static void validate() throws Exception {
        Method getSelectedUIMethod = SynthLookAndFeel.class.getDeclaredMethod("getSelectedUI");

        getSelectedUIMethod.setAccessible(true);

        Method getSelectedUIStateMethod = SynthLookAndFeel.class.getDeclaredMethod("getSelectedUIState");

        getSelectedUIStateMethod.setAccessible(true);

        if (getSelectedUIMethod.invoke(null) != componentUI) {
            throw new RuntimeException("getSelectedUI returns invalid value");
        }
        if (((Integer) getSelectedUIStateMethod.invoke(null)).intValue() !=
                (SynthConstants.SELECTED | SynthConstants.FOCUSED)) {
            throw new RuntimeException("getSelectedUIState returns invalid value");
        }

    }

    private static class ThreadInAnotherAppContext extends Thread {
        public ThreadInAnotherAppContext() {
            super(new ThreadGroup("7143614"), "ThreadInAnotherAppContext");
        }

        public void run() {
            SunToolkit.createNewAppContext();

            try {
                setSelectedUIMethod.invoke(null, null, false, false, false, false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
