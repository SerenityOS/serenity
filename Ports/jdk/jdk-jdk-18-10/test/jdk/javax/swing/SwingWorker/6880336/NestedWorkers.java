/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6880336
 * @summary Test for nested SwingWorkers, i.e. when the second worker is
started from the first's doInBackground() method. A timeout when running
* this test is an indication of failure.
 * @author Artem Ananiev
 * @run main/timeout=32 NestedWorkers
 */

import javax.swing.*;

public class NestedWorkers extends SwingWorker<String, Void> {

    private final static int MAX_LEVEL = 2;

    private int level;

    public NestedWorkers(int level) {
        super();
        this.level = level;
    }

    @Override
    public String doInBackground() throws Exception {
        if (level < MAX_LEVEL) {
            SwingWorker<String, Void> nested = new NestedWorkers(level + 1);
            nested.execute();
            nested.get();
        }
        System.out.println("doInBackground " + level + " is complete");
        return String.valueOf(level);
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                SwingWorker<String, Void> sw = new NestedWorkers(0);
                sw.execute();
                try {
                    System.err.println(sw.get());
                } catch (Exception z) {
                    throw new RuntimeException(z);
                }
            }
        });
    }

}
