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
  @bug 4390555
  @summary Synopsis: clearGlobalFocusOwner() is not trigerring permanent FOCUS_LOST event
  @author son@sparc.spb.su, anton.tarasov: area=awt.focus
  @library   ../../regtesthelpers
  @build Util
  @run main ClearGlobalFocusOwnerTest
*/

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

public class ClearGlobalFocusOwnerTest {
    static volatile boolean isFocusLost = false;
    static Frame frame = new Frame("Test frame");
    static Button button = new Button("Test button");

    public static void main(String[] args) {
        button.addFocusListener(new FocusAdapter() {
                public void focusLost(FocusEvent fe) {
                    if (fe.isTemporary()) {
                        throw new TestFailedException("the FocusLost event is temporary: " + fe);
                    }
                    isFocusLost = true;
                }
            });

        frame.add(button);
        frame.pack();
        frame.setVisible(true);

        Util.waitForIdle(null);

        if (!button.hasFocus()) {
            button.requestFocus();
            Util.waitForIdle(null);
            if (!button.hasFocus()) {
                throw new TestErrorException("couldn't focus " + button);
            }
        }

        KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();

        Util.waitForIdle(null);

        if (!isFocusLost) {
            throw new TestFailedException("no FocusLost event happened on clearGlobalFocusOwner");
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
