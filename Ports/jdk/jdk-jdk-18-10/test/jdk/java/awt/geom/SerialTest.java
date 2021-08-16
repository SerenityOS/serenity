/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4263142 4172661
 * @summary First run verifies that objects serialize and deserialize
 *          correctly against the current release.
 *          Second run verifies that objects from previous releases
 *          still deserialize correctly.  The serial_1_6.out file was
 *          created using the "write" option under release 1.6.
 *          The test was modified after fixing 4172661 to add testing
 *          of Path2D serialization (and to recut the test file with
 *          the new serialVersionUID of GeneralPath).
 * @run main SerialTest
 * @run main SerialTest read serial_1_6.out
 */

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class SerialTest {
    public static Object testobjects[] = {
        // non-shapes...
        new Point2D.Float(37, 42),
        new Point2D.Double(85, 63),
        new AffineTransform(10, 20, 30, 40, 50, 60),

        // shapes...
        new QuadCurve2D.Float(10f, 10f, 50f, 50f, 100f, 10f),
        new QuadCurve2D.Double(20f, 20f, 50f, 50f, 100f, 20f),
        new CubicCurve2D.Float(10f, 10f, 50f, 10f, 10f, 50f, 50f, 50f),
        new CubicCurve2D.Double(0.0, 0.0, 50.0, 0.0, 0.0, 50.0, 50.0, 50.0),
        new GeneralPath(),
        new GeneralPath(PathIterator.WIND_NON_ZERO),
        new GeneralPath(PathIterator.WIND_EVEN_ODD),
        makeGeneralPath(PathIterator.WIND_NON_ZERO, 5f),
        makeGeneralPath(PathIterator.WIND_EVEN_ODD, 23f),
        new Line2D.Float(20f, 20f, 25f, 50f),
        new Line2D.Double(20.0, 20.0, 35.0, 50.0),
        new Rectangle2D.Float(100f, 100f, 50f, 25f),
        new Rectangle2D.Double(200.0, 200.0, 75.0, 35.0),
        new RoundRectangle2D.Float(120f, 120f, 50f, 35f, 5f, 7f),
        new RoundRectangle2D.Double(220.0, 220.0, 85.0, 45.0, 3.0, 9.0),
        new Ellipse2D.Float(110f, 110f, 50f, 55f),
        new Ellipse2D.Double(210.0, 210.0, 75.0, 45.0),
        new Arc2D.Float(10f, 10f, 50f, 40f, 45f, 72f, Arc2D.OPEN),
        new Arc2D.Float(10f, 10f, 40f, 50f, 135f, 72f, Arc2D.PIE),
        new Arc2D.Float(10f, 10f, 40f, 60f, 225f, 72f, Arc2D.CHORD),
        new Arc2D.Double(10.0, 20.0, 50.0, 40.0, 45.0, 72.0, Arc2D.OPEN),
        new Arc2D.Double(10.0, 20.0, 40.0, 50.0, 135.0, 72.0, Arc2D.PIE),
        new Arc2D.Double(10.0, 20.0, 40.0, 60.0, 225.0, 72.0, Arc2D.CHORD),

        // Paths
        new Path2D.Float(),
        new Path2D.Float(PathIterator.WIND_NON_ZERO),
        new Path2D.Float(PathIterator.WIND_EVEN_ODD),
        makePath2DFloat(PathIterator.WIND_NON_ZERO, 5f),
        makePath2DFloat(PathIterator.WIND_EVEN_ODD, 23f),
        new Path2D.Double(),
        new Path2D.Double(PathIterator.WIND_NON_ZERO),
        new Path2D.Double(PathIterator.WIND_EVEN_ODD),
        makePath2DDouble(PathIterator.WIND_NON_ZERO, 5f),
        makePath2DDouble(PathIterator.WIND_EVEN_ODD, 23f),
    };

    public static Shape makeGeneralPath(int winding, float off) {
        return fill(new GeneralPath(winding), off);
    }

    public static Shape makePath2DFloat(int winding, float off) {
        return fill(new Path2D.Float(winding), off);
    }

    public static Shape makePath2DDouble(int winding, float off) {
        return fill(new Path2D.Double(winding), off);
    }

    public static Path2D fill(Path2D p2d, float off) {
        p2d.moveTo(off+10, off+10);
        p2d.lineTo(off+100, off+50);
        p2d.quadTo(off+50, off+100, off+200, off+100);
        p2d.curveTo(off+400, off+20, off+20, off+400, off+100, off+100);
        p2d.closePath();
        return p2d;
    }

    static int numerrors;

    public static void error(Object o1, Object o2, String reason) {
        System.err.println("Failed comparing: "+o1+" to "+o2);
        System.err.println(reason);
        numerrors++;
    }

    public static void usage(int exitcode) {
        System.err.println("usage: java SerialTest [read|write] <filename>");
        System.exit(exitcode);
    }

    public static void main(String argv[]) {
        if (argv.length > 0) {
            if (argv.length < 2) {
                usage(1);
            }

            String arg = argv[0].toLowerCase();
            if (arg.equals("write")) {
                serializeTo(argv[1]);
            } else if (arg.equals("read")) {
                testFrom(argv[1]);
            } else {
                usage(1);
            }
        } else {
            testSerial();
        }
    }

    public static File makeFile(String filename) {
        return new File(System.getProperty("test.src", "."), filename);
    }

    public static void serializeTo(String filename) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(makeFile(filename));
        } catch (IOException ioe) {
            throw new InternalError("bad output filename: "+filename);
        }
        serializeTo(fos);
    }

    public static void testFrom(String filename) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(makeFile(filename));
        } catch (IOException ioe) {
            throw new InternalError("bad input filename: "+filename);
        }
        testFrom(fis);
    }

    public static void testSerial() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializeTo(baos);

        byte buf[] = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);

        testFrom(bais);
    }

    public static void serializeTo(OutputStream os) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(os);

            for (Object o1: testobjects) {
                oos.writeObject(o1);
            }

            oos.close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static void testFrom(InputStream is) {
        try {
            ObjectInputStream ois = new ObjectInputStream(is);

            for (Object o1: testobjects) {
                Object o2 = ois.readObject();
                if (o1 instanceof Shape) {
                    compareShapes((Shape) o1, (Shape) o2);
                } else {
                    if (!o1.equals(o2)) {
                        error(o1, o2, "objects not equal");
                    }
                }
            }

            try {
                ois.readObject();
                throw new RuntimeException("extra data in stream");
            } catch (IOException ioe2) {
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        }
    }

    public static void compareShapes(Shape s1, Shape s2) {
        PathIterator pi1 = s1.getPathIterator(null);
        PathIterator pi2 = s2.getPathIterator(null);

        if (pi1.getWindingRule() != pi2.getWindingRule()) {
            error(s1, s2, "winding rules are different");
        }

        double coords1[] = new double[6];
        double coords2[] = new double[6];

        while (!pi1.isDone()) {
            if (pi2.isDone()) {
                error(s1, s2, "Shape 2 ended prematurely");
                return;
            }

            int t1 = pi1.currentSegment(coords1);
            int t2 = pi2.currentSegment(coords2);

            if (t1 != t2) {
                error(s1, s2, "different segment types");
            }

            int ncoords;
            switch (t1) {
            case PathIterator.SEG_MOVETO:  ncoords = 2; break;
            case PathIterator.SEG_LINETO:  ncoords = 2; break;
            case PathIterator.SEG_QUADTO:  ncoords = 4; break;
            case PathIterator.SEG_CUBICTO: ncoords = 6; break;
            case PathIterator.SEG_CLOSE:   ncoords = 0; break;

            default:
                throw new RuntimeException("unknown segment type");
            }

            for (int i = 0; i < ncoords; i++) {
                if (coords1[i] != coords2[i]) {
                    error(s1, s2, "coordinates differ");
                }
            }
            pi1.next();
            pi2.next();
        }

        if (!pi2.isDone()) {
            error(s1, s2, "Shape 1 ended prematurely");
        }
    }
}
