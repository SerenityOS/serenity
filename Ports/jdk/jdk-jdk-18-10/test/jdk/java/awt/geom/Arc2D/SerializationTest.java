/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7040717 6522514
 * @summary Verifies that subclasses of Arc2D can be serialized.
 * @run main SerializationTest
 */

import java.awt.geom.Rectangle2D;
import java.awt.geom.Arc2D;
import java.io.Serializable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SerializationTest {
    static boolean failed;
    public static void main(String args[]) {
        test(new Arc());
        test(new ArcF());
        test(new ArcD());
        if (failed) throw new RuntimeException("some tests failed");
    }

    public static void test(Object a) {
        try {
            File objectbin = new File("object.bin");
            FileOutputStream fos = new FileOutputStream(objectbin);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(a);
            fos.close();
            FileInputStream fis = new FileInputStream(objectbin);
            ObjectInputStream in = new ObjectInputStream(fis);
            Object o = in.readObject();
            fis.close();
            System.err.println(o);
        } catch (Throwable ex) {
            ex.printStackTrace();
            failed = true;
        }
    }

    static class Arc extends Arc2D implements Serializable {
        public Arc() {
            super(Arc2D.OPEN);
        }

        public Rectangle2D makeBounds(double x, double y, double w, double h) {
            return new Rectangle2D.Double(x, y, w, h);
        }
        public double getX() { return 0; }
        public double getY() { return 0; }
        public double getWidth() { return 0; }
        public double getHeight() { return 0; }
        public double getAngleExtent() { return 0; }
        public double getAngleStart() { return 0; }
        public void setAngleExtent(double angExt) { }
        public void setAngleStart(double angExt) { }
        public void setFrame(double x, double y, double w, double h) {}
        public void setArc(double x, double y, double w, double h,
                           double s, double e, int c)
        {
        }
        public boolean isEmpty() { return false; };
    }

    static class ArcF extends Arc2D.Float implements Serializable {
        public ArcF() {
        }
    }

    static class ArcD extends Arc2D.Double implements Serializable {
        public ArcD() {
        }
    }
}
