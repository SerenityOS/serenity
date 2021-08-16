/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug 8080395
  @summary consider making sun.awt.CausedFocusEvent functionality public
  @run main FocusCauseTest
*/


import java.awt.*;
import java.awt.event.FocusEvent.Cause;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.IllegalArgumentException;
import java.lang.Override;
import java.lang.RuntimeException;
import java.util.Arrays;

public class FocusCauseTest {

    private static Cause[] causes1 = {Cause.ACTIVATION,
            Cause.UNKNOWN, Cause.UNKNOWN, Cause.TRAVERSAL_FORWARD,
            Cause.TRAVERSAL_FORWARD, Cause.TRAVERSAL_BACKWARD,
            Cause.TRAVERSAL_BACKWARD, Cause.TRAVERSAL_UP,
            Cause.TRAVERSAL_DOWN, Cause.CLEAR_GLOBAL_FOCUS_OWNER};
    private static Cause[] causes2 = new Cause[10];
    private static int cnt;

    static byte[] data =
            {-84, -19, 0, 5, 115, 114, 0, 24, 115, 117, 110, 46, 97, 119,
            116, 46, 67, 97, 117, 115, 101, 100, 70, 111, 99, 117, 115, 69, 118,
            101, 110, 116, -51, 98, 39, -75, 86, 52, 107, 30, 2, 0, 1, 76, 0, 5,
            99, 97, 117, 115, 101, 116, 0, 32, 76, 115, 117, 110, 47, 97, 119,
            116, 47, 67, 97, 117, 115, 101, 100, 70, 111, 99, 117, 115, 69, 118,
            101, 110, 116, 36, 67, 97, 117, 115, 101, 59, 120, 114, 0, 25, 106,
            97, 118, 97, 46, 97, 119, 116, 46, 101, 118, 101, 110, 116, 46, 70,
            111, 99, 117, 115, 69, 118, 101, 110, 116, 7, 68, -65, 75, 55, -113,
            98, -52, 2, 0, 1, 90, 0, 9, 116, 101, 109, 112, 111, 114, 97, 114,
            121, 120, 114, 0, 29, 106, 97, 118, 97, 46, 97, 119, 116, 46, 101,
            118, 101, 110, 116, 46, 67, 111, 109, 112, 111, 110, 101, 110, 116,
            69, 118, 101, 110, 116, 112, 109, -6, -107, 79, -87, -38, 69, 2, 0,
            0, 120, 114, 0, 17, 106, 97, 118, 97, 46, 97, 119, 116, 46, 65, 87,
            84, 69, 118, 101, 110, 116, -26, -85, 45, -31, 24, -33, -118, -61,
            2, 0, 3, 90, 0, 8, 99, 111, 110, 115, 117, 109, 101, 100, 73, 0, 2,
            105, 100, 91, 0, 5, 98, 100, 97, 116, 97, 116, 0, 2, 91, 66, 120,
            114, 0, 21, 106, 97, 118, 97, 46, 117, 116, 105, 108, 46, 69, 118,
            101, 110, 116, 79, 98, 106, 101, 99, 116, 76, -115, 9, 78, 24, 109,
            125, -88, 2, 0, 0, 120, 112, 0, 0, 0, 3, -20, 112, 0, 126, 114, 0,
            30, 115, 117, 110, 46, 97, 119, 116, 46, 67, 97, 117, 115, 101, 100,
            70, 111, 99, 117, 115, 69, 118, 101, 110, 116, 36, 67, 97, 117, 115,
            101, 0, 0, 0, 0, 0, 0, 0, 0, 18, 0, 0, 120, 114, 0, 14, 106, 97,
            118, 97, 46, 108, 97, 110, 103, 46, 69, 110, 117, 109, 0, 0, 0, 0,
            0, 0, 0, 0, 18, 0, 0, 120, 112, 116, 0};

    static byte[] dataOld =
            {-84, -19, 0, 5, 115, 114, 0, 25, 106, 97, 118, 97, 46, 97, 119,
            116, 46, 101, 118, 101, 110, 116, 46, 70, 111, 99, 117, 115, 69,
            118, 101, 110, 116, 7, 68, -65, 75, 55, -113, 98, -52, 2, 0, 1, 90,
            0, 9, 116, 101, 109, 112, 111, 114, 97, 114, 121, 120, 114, 0, 29,
            106, 97, 118, 97, 46, 97, 119, 116, 46, 101, 118, 101, 110, 116, 46,
            67, 111, 109, 112, 111, 110, 101, 110, 116, 69, 118, 101, 110, 116,
            112, 109, -6, -107, 79, -87, -38, 69, 2, 0, 0, 120, 114, 0, 17, 106,
            97, 118, 97, 46, 97, 119, 116, 46, 65, 87, 84, 69, 118, 101, 110,
            116, -26, -85, 45, -31, 24, -33, -118, -61, 2, 0, 3, 90, 0, 8, 99,
            111, 110, 115, 117, 109, 101, 100, 73, 0, 2, 105, 100, 91, 0, 5, 98,
            100, 97, 116, 97, 116, 0, 2, 91, 66, 120, 114, 0, 21, 106, 97, 118,
            97, 46, 117, 116, 105, 108, 46, 69, 118, 101, 110, 116, 79, 98, 106,
            101, 99, 116, 76, -115, 9, 78, 24, 109, 125, -88, 2, 0, 0, 120, 112,
            0, 0, 0, 0, 100, 112, 0};

    static String[] causesIn = {"UNKNOWN", "MOUSE_EVENT", "TRAVERSAL",
            "TRAVERSAL_UP", "TRAVERSAL_DOWN", "TRAVERSAL_FORWARD",
            "TRAVERSAL_BACKWARD", "MANUAL_REQUEST", "AUTOMATIC_TRAVERSE"
            ,"ROLLBACK", "NATIVE_SYSTEM", "ACTIVATION",
            "CLEAR_GLOBAL_FOCUS_OWNER", "RETARGETED"};

    static FocusEvent.Cause[] causesOut = {FocusEvent.Cause.UNKNOWN,
            FocusEvent.Cause.MOUSE_EVENT,
            FocusEvent.Cause.TRAVERSAL, FocusEvent.Cause.TRAVERSAL_UP,
            FocusEvent.Cause.TRAVERSAL_DOWN, FocusEvent.Cause.TRAVERSAL_FORWARD,
            FocusEvent.Cause.TRAVERSAL_BACKWARD, FocusEvent.Cause.UNKNOWN,
            FocusEvent.Cause.UNKNOWN, FocusEvent.Cause.ROLLBACK,
            FocusEvent.Cause.UNEXPECTED, FocusEvent.Cause.ACTIVATION,
            FocusEvent.Cause.CLEAR_GLOBAL_FOCUS_OWNER, FocusEvent.Cause.UNKNOWN
    };

    public static void main(String[] args) throws Exception {
        testCauses();
        testNullCause();
        testCausedFocusEventDeserialization();
        testFocusEventDeserialization();
        System.out.println("ok");
    }

    private static void testNullCause() {
        try {
            new FocusEvent(new Frame(), FocusEvent.FOCUS_GAINED, true,
                    null, null);
            throw new RuntimeException("Exception is not thrown when the " +
                    "cause is null");
        } catch (IllegalArgumentException e) {
        }
    }

    private static void testCauses() throws Exception {
        cnt = 0;
        Frame frame = new Frame();
        TextField comp1 = new TextField();
        comp1.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                System.out.println(e.getCause());
                causes2[cnt++] = e.getCause();
            }

            @Override
            public void focusLost(FocusEvent e) {
                System.out.println(e.getCause());
                causes2[cnt++] = e.getCause();
            }
        });
        TextField comp2 = new TextField();
        comp2.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                System.out.println(e.getCause());
                causes2[cnt++] = e.getCause();
            }

            @Override
            public void focusLost(FocusEvent e) {
                System.out.println(e.getCause());
                causes2[cnt++] = e.getCause();
            }
        });
        frame.add(comp1, BorderLayout.NORTH);
        frame.add(comp2, BorderLayout.SOUTH);
        frame.setVisible(true);

        Robot robot = new Robot();
        robot.delay(200);
        robot.waitForIdle();
        comp2.requestFocus();
        robot.waitForIdle();
        comp2.transferFocus();
        robot.waitForIdle();
        comp1.transferFocusBackward();
        robot.waitForIdle();
        comp2.transferFocusUpCycle();
        robot.waitForIdle();
        frame.transferFocusDownCycle();
        robot.waitForIdle();
        frame.dispose();
        robot.waitForIdle();
        if (!Arrays.equals(causes1, causes2)) {
            throw new RuntimeException("wrong cause " + causes2);
        }
    }

    private static void  testCausedFocusEventDeserialization() throws
            Exception {
        for (int i = 0; i < causesIn.length; i++) {
            final String causeIn = causesIn[i];
            ObjectInputStream oi = new ObjectInputStream(new InputStream() {
                int cnt = 0;
                @Override
                public int read() throws IOException {
                    if(cnt < data.length) {
                        return data[cnt++];
                    } else if(cnt == data.length){
                        cnt++;
                        return causeIn.length();
                    } else if(cnt - data.length - 1 < causeIn.length()) {
                        return causeIn.getBytes()[cnt++ - data.length - 1];
                    }
                    return -1;
                }
            });
            FocusEvent ev = (FocusEvent) oi.readObject();
            System.out.println(ev);
            if(ev.getCause() != causesOut[i]) {
                throw new RuntimeException("Wrong cause read :" +ev.getCause());
            }
        }
    }

    private static void testFocusEventDeserialization() throws
            Exception {
        ObjectInputStream oi = new ObjectInputStream(
                new ByteArrayInputStream(dataOld));
        FocusEvent ev = (FocusEvent)oi.readObject();
        if(ev.getCause() != FocusEvent.Cause.UNKNOWN) {
            throw new RuntimeException("Wrong cause in deserialized FocusEvent "
                    + ev.getCause());
        }
    }

}
