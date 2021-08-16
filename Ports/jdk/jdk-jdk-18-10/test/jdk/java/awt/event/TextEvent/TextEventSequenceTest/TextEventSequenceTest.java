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

/*
 * @test
 * @key headful
 * @bug 4028580
 * @summary TextArea does not send TextEvent when setText. Does for insert
 * @author kdm@sparc.spb.su: area= awt.TextAvent
 * @run main TextEventSequenceTest
 */
import java.awt.*;
import java.awt.event.*;

public class TextEventSequenceTest {

    private static Frame f;
    private static TextField tf;
    private static TextArea t;
    private static int cntEmptyStrings = 0;
    private static int cntNonEmptyStrings = 0;

    public static void main(String[] args) {

        test("non-empty text string");
        test("");
        test(null);
    }

    private static void test(String test) {

        Robot robot;
        try {
            robot = new Robot();
        }catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Unexpected failure");
        }

        createAndShowGUI(test);
        robot.waitForIdle();

        initCounts();
        t.setText("Hello ");
        robot.waitForIdle();
        t.append("World! !");
        robot.waitForIdle();
        t.insert("from Roger Pham", 13);
        robot.waitForIdle();
        t.replaceRange("Java Duke", 18, 28);
        robot.waitForIdle();
        checkCounts(0, 4);

        initCounts();
        t.setText("");
        robot.waitForIdle();
        t.setText("");
        robot.waitForIdle();
        t.setText("");
        robot.waitForIdle();
        checkCounts(1, 0);

        initCounts();
        tf.setText("Hello There!");
        robot.waitForIdle();
        checkCounts(0, 1);

        initCounts();
        tf.setText("");
        robot.waitForIdle();
        tf.setText("");
        robot.waitForIdle();
        tf.setText("");
        robot.waitForIdle();
        checkCounts(1, 0);

        f.dispose();
    }

    private static void createAndShowGUI(String text) {
        f = new Frame("TextEventSequenceTest");
        f.setLayout(new FlowLayout());

        TextListener listener = new MyTextListener();

        tf = new TextField(text);
        tf.addTextListener(listener);
        f.add(tf);

        t = new TextArea(text, 10, 30);
        t.addTextListener(listener);
        f.add(t);

        f.pack();
        f.setVisible(true);
    }

    static class MyTextListener implements TextListener {

        public synchronized void textValueChanged(TextEvent e) {
            TextComponent tc = (TextComponent) e.getSource();
            String text = tc.getText();
            if (text.length() == 0) {
                cntEmptyStrings++;
            } else {
                cntNonEmptyStrings++;
            }
        }
    }

    synchronized static void initCounts() {
        cntEmptyStrings = 0;
        cntNonEmptyStrings = 0;
    }

    synchronized static void checkCounts(int empty, int nonempty) {
        if (empty != cntEmptyStrings || nonempty != cntNonEmptyStrings) {
            throw new RuntimeException(
                    String.format("Expected events: empty = %d, nonempty = %d, "
                    + "actual events: empty = %d, nonempty = %d",
                    empty, nonempty, cntEmptyStrings, cntNonEmptyStrings));
        }
    }
}

