/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @key headful
  @bug 6411406
  @summary Components automatically transfer focus on removal, even if developer requests focus elsewhere first
  @author oleg.sukhodolsky, anton.tarasov: area=awt.focus
  @library   ../../regtesthelpers
  @build Util
  @run main RemoveAfterRequest
*/

/**
 * RemoveAfterRequest.java
 *
 * summary: Components automatically transfer focus on removal, even if developer requests focus elsewhere first
 */

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

public class RemoveAfterRequest {
    final static Frame frame = new Frame("test frame");
    final static Button btn1 = new Button("btn1");
    final static Button btn2 = new Button("btn2");
    final static Button btn3 = new Button("btn3");

    public static void main(String[] args) {
        frame.setLayout(new GridLayout(3, 1));
        frame.add(btn1);
        frame.add(btn2);
        frame.add(btn3);
        frame.pack();
        frame.setVisible(true);

        Util.waitForIdle(null);

        if (!btn1.hasFocus()) {
            btn1.requestFocus();
            Util.waitForIdle(null);
            if (!btn1.hasFocus()) {
                throw new TestErrorException("couldn't focus " + btn1);
            }
        }

        if (!Util.trackFocusGained(btn3, new Runnable() {
                public void run() {
                    btn3.requestFocus();
                    frame.remove(btn1);
                    frame.invalidate();
                                frame.validate();
                                frame.repaint();
                }
            }, 2000, true))
        {
            throw new TestFailedException("focus request on removal failed");
        }

        System.out.println("Test passed.");
    }
}

/**
 * Thrown when the behavior being verified is found wrong.
 */
class TestFailedException extends RuntimeException {
    TestFailedException(String msg) {
        super("Test failed: " + msg);
    }
}

/**
 * Thrown when an error not related to the behavior being verified is encountered.
 */
class TestErrorException extends RuntimeException {
    TestErrorException(String msg) {
        super("Unexpected error: " + msg);
    }
}

