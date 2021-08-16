/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/*
 * @test
 * @key headful
 * @bug 8027152
 * @summary Checks that ownedWindowList is serialized and deserialized properly and alwaysOnTop works after deserialization
 * @author Petr Pchelko
 * @run main OwnedWindowsSerialization
 */
public class OwnedWindowsSerialization {

    private static final String TOP_FRAME_LABEL = "Top Frame";
    private static final String DIALOG_LABEL = "Dialog";
    private static final String SUBDIALOG_LABEL = "Subdialog";

    private static volatile Frame topFrame;
    private static volatile Dialog dialog;
    private static volatile Dialog subDialog;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            topFrame = new Frame(TOP_FRAME_LABEL);
            topFrame.setAlwaysOnTop(true);
            dialog = new Dialog(topFrame, DIALOG_LABEL);
            subDialog = new Dialog(dialog, SUBDIALOG_LABEL);
        });

        Robot robot = new Robot();
        robot.waitForIdle();

        if (!topFrame.isAlwaysOnTop() || !dialog.isAlwaysOnTop() || !subDialog.isAlwaysOnTop()) {
            throw new RuntimeException("TEST FAILED: AlwaysOnTop was not set properly");
        }

        //Serialize
        byte[] serializedObject;
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream outputStream = new ObjectOutputStream(bytes)) {
            outputStream.writeObject(topFrame);
            outputStream.flush();
            serializedObject = bytes.toByteArray();
        }

        if (serializedObject == null) {
            throw new RuntimeException("FAILED: Serialized byte array in null");
        }

        //Deserialize
        Frame deserializedFrame;
        try (ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(serializedObject))) {
            deserializedFrame = (Frame) inputStream.readObject();
        }

        //Check the state of the deserialized objects
        if (!TOP_FRAME_LABEL.equals(deserializedFrame.getTitle())) {
            throw new RuntimeException("FAILED: Top frame deserialized incorrectly");
        }

        if (!deserializedFrame.isAlwaysOnTop()) {
            throw new RuntimeException("FAILED: Top frame alwaysOnTop not set after deserialization");
        }

        Window[] topOwnedWindows = topFrame.getOwnedWindows();
        if (topOwnedWindows.length != 1 || !DIALOG_LABEL.equals(((Dialog) topOwnedWindows[0]).getTitle())) {
            throw new RuntimeException("FAILED: Dialog deserialized incorrectly");
        }

        if (!topOwnedWindows[0].isAlwaysOnTop()) {
            throw new RuntimeException("FAILED: Dialog alwaysOnTop not set after deserialization");
        }

        Window dialog = topOwnedWindows[0];
        Window[] dialogOwnedWindows = dialog.getOwnedWindows();
        if (dialogOwnedWindows.length != 1 || !SUBDIALOG_LABEL.equals(((Dialog) dialogOwnedWindows[0]).getTitle())) {
            throw new RuntimeException("FAILED: Subdialog deserialized incorrectly");
        }

        if (!dialogOwnedWindows[0].isAlwaysOnTop()) {
            throw new RuntimeException("FAILED: Subdialog alwaysOnTop not set after deserialization");
        }
    }
}
