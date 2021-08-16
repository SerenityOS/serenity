/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 8030050 8039082
 * @summary Validate fields on DnD class deserialization
 */

import java.awt.Point;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.stream.Stream;

import javax.swing.JPanel;

import static java.awt.dnd.DnDConstants.ACTION_COPY;

public class BadSerializationTest {

    private static final String[] badSerialized = new String[] {
            "badAction",
            "noEvents",
            "nullComponent",
            "nullDragSource",
            "nullOrigin"
    };

    private static final String goodSerialized = "good";

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("-write")) {
            writeObjects(); //Creates the binary files for the test.
        } else {
            String testSrc = System.getProperty("test.src") + File.separator;
            testReadObject(testSrc + goodSerialized, false);
            Stream.of(badSerialized).forEach(
                    file -> testReadObject(testSrc + file, true));
        }
    }

    private static void testReadObject(String filename, boolean expectException) {
        Exception exceptionCaught = null;
        try (FileInputStream fileInputStream = new FileInputStream(filename);
             ObjectInputStream ois = new ObjectInputStream(fileInputStream)) {
            ois.readObject();
        } catch (InvalidObjectException e) {
            exceptionCaught = e;
        } catch (IOException e) {
            throw new RuntimeException("FAILED: IOException", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("FAILED: ClassNotFoundException", e);
        }
        if (exceptionCaught != null && !expectException) {
            throw new RuntimeException("FAILED: UnexpectedException", exceptionCaught);
        }
        if (exceptionCaught == null && expectException) {
            throw new RuntimeException("FAILED: Invalid object was created with no exception");
        }
    }

    /**
     * Creates the stubs for the test. It is necessary to disable all checks in
     * the constructors of DragGestureEvent/DragGestureRecognizer before run.
     */
    private static void writeObjects() throws Exception {
        ArrayList<InputEvent> evs = new ArrayList<>();
        Point ori = new Point();

        write(new DragGestureEvent(new NothingNull(), ACTION_COPY, ori, evs),
              "noEvents");

        evs.add(new KeyEvent(new JPanel(), 0, 0, 0, 0, 'a', 0));

        write(new DragGestureEvent(new NullComponent(), ACTION_COPY, ori, evs),
              "nullComponent");

        write(new DragGestureEvent(new NothingNull(), 100, ori, evs),
              "badAction");

        write(new DragGestureEvent(new NullDragSource(), ACTION_COPY, ori, evs),
              "nullDragSource");

        write(new DragGestureEvent(new NothingNull(), ACTION_COPY, null, evs),
              "nullOrigin");

        write(new DragGestureEvent(new NothingNull(), ACTION_COPY, ori, evs),
              "good");
    }

    private static void write(Object obj, String file) throws Exception {
        try (FileOutputStream fis = new FileOutputStream(file);
             ObjectOutputStream ois = new ObjectOutputStream(fis)) {
            ois.writeObject(obj);
        }
    }

    public static final class NullDragSource extends DragGestureRecognizer {

        public NullDragSource() {
            super(null, new JPanel());
        }

        protected void registerListeners() {
        }

        protected void unregisterListeners() {
        }
    }

    public static final class NullComponent extends DragGestureRecognizer {

        public NullComponent() {
            super(new DragSource(), null);
        }

        protected void registerListeners() {
        }

        protected void unregisterListeners() {
        }
    }

    public static final class NothingNull extends DragGestureRecognizer {

        public NothingNull() {
            super(new DragSource(), new JPanel());
        }

        protected void registerListeners() {
        }

        protected void unregisterListeners() {
        }
    }
}
