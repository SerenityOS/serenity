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

/*
 * @test
 * @run main/manual TestVetoableChangeSupport
 * @summary Tests just a benchmark of VetoableChangeSupport performance
 * @author Sergey Malenkov
 */

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;

public class TestVetoableChangeSupport implements VetoableChangeListener {
    private static final String NAME = "property";

    public static void main(String[] args) throws PropertyVetoException {
        for (int i = 1; i <= 3; i++) {
            test(i, 1, 10000000);
            test(i, 10, 1000000);
            test(i, 100, 100000);
            test(i, 1000, 10000);
            test(i, 10000, 1000);
            test(i, 20000, 1000);
        }
    }

    private static void test(int step, int listeners, int attempts) throws PropertyVetoException {
        TestVetoableChangeSupport test = new TestVetoableChangeSupport();
        VetoableChangeSupport vcs = new VetoableChangeSupport(test);
        PropertyChangeEvent eventNull = new PropertyChangeEvent(test, null, null, null);
        PropertyChangeEvent eventName = new PropertyChangeEvent(test, NAME, null, null);
        long time1 = System.currentTimeMillis();
        for (int i = 0; i < listeners; i++) {
            vcs.addVetoableChangeListener(test);
            vcs.addVetoableChangeListener(NAME, test);
        }
        long time2 = System.currentTimeMillis();
        for (int i = 0; i < attempts; i++) {
            vcs.fireVetoableChange(eventNull);
            vcs.fireVetoableChange(eventName);
        }
        long time3 = System.currentTimeMillis();
        time1 = time2 - time1; // time of adding the listeners
        time2 = time3 - time2; // time of firing the events
        System.out.println("Step: " + step
                        + "; Listeners: " + listeners
                        + "; Attempts: " + attempts
                        + "; Time (ms): " + time1 + "/" + time2);
    }

    public void vetoableChange(PropertyChangeEvent event) {
    }
}
