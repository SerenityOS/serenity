/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Choice;
import java.awt.Frame;
import java.awt.Graphics;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @test
 * @bug 4790927 4783342 4930678 8225126
 * @key headful
 * @summary Tests Choice doesn't flash
 */
public final class SetBoundsPaintTest {

    public static void main(String[] args) throws Exception {
        CountDownLatch go = new CountDownLatch(10);
        Choice choice = new Choice();
        choice.addItem("first");
        choice.addItem("second");
        Frame frame = new Frame() {
            @Override
            public void paint(Graphics g) {
                g.fillRect(0, 0, 200, 200);
                choice.setBounds(50, 50, 180, 30);
                go.countDown();
            }
        };
        try {
            frame.setLayout(null);
            frame.add(choice);
            frame.setSize(300, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            if (go.await(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("recursive setBounds paint");
            }
        } finally {
            frame.dispose();
        }
    }
}