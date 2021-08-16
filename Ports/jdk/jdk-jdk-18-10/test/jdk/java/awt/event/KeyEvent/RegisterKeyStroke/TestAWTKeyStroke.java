/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTKeyStroke;
import java.awt.event.InputEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import javax.swing.KeyStroke;

/*
 * @test
 * @bug 8133453
 * @summary Remove AWTKeyStroke.registerSubclass(Class) method
 * @author Alexander Scherbatiy
 * @run main/othervm TestAWTKeyStroke
 * @run main/othervm/policy=policy -Djava.security.manager TestAWTKeyStroke
 */
public class TestAWTKeyStroke {

    public static void main(String[] args) throws Exception {

        int modifiers = InputEvent.CTRL_MASK | InputEvent.CTRL_DOWN_MASK;
        checkAWTKeyStroke('A', modifiers, true);
        checkKeyStroke('B', modifiers, false);
        checkAWTKeyStroke('C', modifiers, false);
        checkKeyStroke('D', modifiers, true);
        checkSerializedKeyStrokes('E', modifiers, true);
    }

    private static void checkAWTKeyStroke(int keyCode, int modifiers,
            boolean onKeyRelease) throws Exception {

        AWTKeyStroke awtKeyStroke1 = AWTKeyStroke.getAWTKeyStroke(
                keyCode, modifiers, onKeyRelease);

        checkAWTKeyStroke(awtKeyStroke1, keyCode, modifiers, onKeyRelease);

        AWTKeyStroke awtKeyStroke2 = AWTKeyStroke.getAWTKeyStroke(
                keyCode, modifiers, onKeyRelease);

        if (awtKeyStroke1 != awtKeyStroke2) {
            throw new RuntimeException("AWTKeyStroke is not cached!");
        }

        checkSerializedKeyStroke(awtKeyStroke1);
    }

    private static void checkKeyStroke(int keyCode, int modifiers,
            boolean onKeyRelease) throws Exception {

        KeyStroke keyStroke1 = KeyStroke.getKeyStroke(
                keyCode, modifiers, onKeyRelease);
        checkAWTKeyStroke(keyStroke1, keyCode, modifiers, onKeyRelease);

        KeyStroke keyStroke2 = KeyStroke.getKeyStroke(
                keyCode, modifiers, onKeyRelease);

        if (keyStroke1 != keyStroke2) {
            throw new RuntimeException("KeyStroke is not cached!");
        }

        checkSerializedKeyStroke(keyStroke1);
    }

    private static void checkSerializedKeyStrokes(int keyCode, int modifiers,
            boolean onKeyRelease) throws Exception {

        AWTKeyStroke awtKeyStroke = AWTKeyStroke.getAWTKeyStroke(
                keyCode, modifiers, onKeyRelease);

        KeyStroke keyStroke = KeyStroke.getKeyStroke(
                keyCode, modifiers, onKeyRelease);

        if (awtKeyStroke != getSerializedAWTKeyStroke(awtKeyStroke)) {
            throw new RuntimeException("Serialized AWTKeyStroke is not cached!");
        }

        awtKeyStroke = AWTKeyStroke.getAWTKeyStroke(
                keyCode, modifiers, !onKeyRelease);

        if (!keyStroke.equals(getSerializedAWTKeyStroke(keyStroke))) {
            throw new RuntimeException("Serialized KeyStroke is not cached!");
        }
    }

    private static void checkAWTKeyStroke(AWTKeyStroke awtKeyStroke,
            int keyCode, int modifiers, boolean onKeyRelease) {

        if (awtKeyStroke.getKeyCode() != keyCode) {
            throw new RuntimeException("Wrong key code!");
        }

        if (awtKeyStroke.getModifiers() != modifiers) {
            throw new RuntimeException("Wrong modifiers!");
        }

        if (awtKeyStroke.isOnKeyRelease() != onKeyRelease) {
            throw new RuntimeException("Wrong on key release!");
        }
    }

    private static void checkSerializedKeyStroke(AWTKeyStroke keyStroke)
            throws Exception {
        if (keyStroke != getSerializedAWTKeyStroke(keyStroke)) {
            throw new RuntimeException("New instance is returned during"
                    + " serialization!");
        }
    }

    private static AWTKeyStroke getSerializedAWTKeyStroke(AWTKeyStroke keyStroke)
            throws Exception {

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(keyStroke);
            byte[] bytes = bos.toByteArray();

            try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                    ObjectInput in = new ObjectInputStream(bis)) {
                return (AWTKeyStroke) in.readObject();
            }
        }
    }
}
