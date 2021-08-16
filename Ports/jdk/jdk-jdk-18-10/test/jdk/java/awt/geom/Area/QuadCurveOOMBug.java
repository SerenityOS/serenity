/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6372340
 * @summary Checks for infinite loop and OOM bugs dealing with Quad curves.
 * @run main/timeout=20/othervm QuadCurveOOMBug
 */

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class QuadCurveOOMBug {
    public static final String SEG_CLOSE = " SEG_CLOSE";
    public static final String SEG_CUBICTO = " SEG_CUBICTO";
    public static final String SEG_LINETO = " SEG_LINETO";
    public static final String SEG_MOVETO = " SEG_MOVETO";
    public static final String SEG_QUADTO = " SEG_QUADTO";

    public static final byte[] strokedLineToSubtract = {
        0,  11,  32,  83,  69,  71,  95,  77,  79,  86,
        69,  84,  79,  68,  117,  7,  -63,  67,  15,  6,
        87,  0,  11,  32,  83,  69,  71,  95,  76,  73,
        78,  69,  84,  79,  68,  117,  31,  16,  67,  15,
        23,  5,  0,  11,  32,  83,  69,  71,  95,  81,
        85,  65,  68,  84,  79,  68,  117,  68,  -12,  67,
        15,  50,  54,  68,  117,  103,  16,  67,  15,  50,
        59,  0,  11,  32,  83,  69,  71,  95,  76,  73,
        78,  69,  84,  79,  68,  117,  110,  14,  67,  15,
        50,  59,  0,  11,  32,  83,  69,  71,  95,  81,
        85,  65,  68,  84,  79,  68,  117,  -56,  26,  67,
        15,  48,  -15,  68,  118,  22,  75,  67,  14,  112,
        95,  0,  11,  32,  83,  69,  71,  95,  81,  85,
        65,  68,  84,  79,  68,  118,  61,  115,  67,  14,
        15,  10,  68,  118,  94,  93,  67,  13,  127,  -74,
        0,  11,  32,  83,  69,  71,  95,  76,  73,  78,
        69,  84,  79,  68,  118,  103,  -80,  67,  13,  87,
        15,  0,  11,  32,  83,  69,  71,  95,  81,  85,
        65,  68,  84,  79,  68,  118,  21,  -76,  67,  14,
        -81,  -105,  68,  117,  -100,  -84,  67,  14,  -72,  72,
        0,  11,  32,  83,  69,  71,  95,  76,  73,  78,
        69,  84,  79,  68,  117,  -88,  84,  67,  14,  -72,
        72,  0,  11,  32,  83,  69,  71,  95,  81,  85,
        65,  68,  84,  79,  68,  117,  108,  85,  67,  14,
        -73,  106,  68,  117,  53,  117,  67,  14,  74,
        -61,  0,  11,  32,  83,  69,  71,  95,  76,  73,  78,
        69,  84,  79,  68,  117,  72,  27,  67,  14,  112,
        14,  0,  11,  32,  83,  69,  71,  95,  76,  73,
        78,  69,  84,  79,  68,  118,  87,  -51,  67,  5,
        -16,  100,  0,  11,  32,  83,  69,  71,  95,  76,
        73,  78,  69,  84,  79,  68,  118,  69,  39,  67,
        5,  -53,  25,  0,  11,  32,  83,  69,  71,  95,
        81,  85,  65,  68,  84,  79,  68,  117,  -5,  -93,
        67,  5,  57,  38,  68,  117,  -88,  84,  67,  5,
        56,  72,  0,  11,  32,  83,  69,  71,  95,  76,
        73,  78,  69,  84,  79,  68,  117,  -100,  -84,  67,
        5,  56,  72,  0,  11,  32,  83,  69,  71,  95,
        81,  85,  65,  68,  84,  79,  68,  117,  12,  86,
        67,  5,  64,  -7,  68,  116,  -89,  -76,  67,
        6,  -22,  -51,  0,  11,  32,  83,  69,  71,  95,  76,
        73,  78,  69,  84,  79,  68,  116,  -98,  97,  67,
        7,  19,  116,  0,  11,  32,  83,  69,  71,  95,
        81,  85,  65,  68,  84,  79,  68,  116,  -75,  -100,
        67,  6,  -83,  -7,  68,  116,  -44,  -9,  67,  6,
        95,  -59,  0,  11,  32,  83,  69,  71,  95,  81,
        85,  65,  68,  84,  79,  68,  117,  26,  -119,  67,
        5,  -76,  14,  68,  117,  110,  14,  67,  5,  -78,
        59,  0,  11,  32,  83,  69,  71,  95,  76,  73,
        78,  69,  84,  79,  68,  117,  103,  16,  67,  5,
        -78,  59,  0,  11,  32,  83,  69,  71,  95,  81,
        85,  65,  68,  84,  79,  68,  117,  122,  -71,  67,
        5,  -78,  22,  68,  117,  -119,  -60,  67,  5,  -68,
        -61,  0,  11,  32,  83,  69,  71,  95,  76,  73,
        78,  69,  84,  79,  68,  117,  114,  117,  67,  5,
        -84,  21,  0,  11,  32,  83,  69,  71,  95,  76,
        73,  78,  69,  84,  79,  68,  117,  7,  -63,  67,
        15,  6,  87,  0,  10,  32,  83,  69,  71,  95,
        67,  76,  79,  83,  69,
    };

    public static final byte[] negSpace = {
        0,  11,  32,  83,  69,  71,  95,  77,  79,  86,
        69,  84,  79,  68,  116,  -33,  -12,  67,  4,  30,
        -23,  0,  11,  32,  83,  69,  71,  95,  76,  73,
        78,  69,  84,  79,  68,  118,  59,  73,  67,  6,
        -43,  -109,  0,  11,  32,  83,  69,  71,  95,  76,
        73,  78,  69,  84,  79,  68,  117,  100,  -97,  67,
        13,  -118,  -32,  0,  11,  32,  83,  69,  71,  95,
        76,  73,  78,  69,  84,  79,  68,  116,  9,  75,
        67,  10,  -44,  54,  0,  10,  32,  83,  69,  71,
        95,  67,  76,  79,  83,  69,
    };

    public static final byte[] strokedLineToAdd = {
        0,  11,  32,  83,  69,  71,  95,  77,  79,  86,
        69,  84,  79,  68,  98,  -100,  -76,  67,  -85,  -36,
        6,  0,  11,  32,  83,  69,  71,  95,  76,  73,
        78,  69,  84,  79,  68,  98,  -76,  4,  67,  -85,
        -48,  21,  0,  11,  32,  83,  69,  71,  95,  81,
        85,  65,  68,  84,  79,  68,  98,  -102,  -29,  67,
        -85,  -36,  -5,  68,  98,  -112,  107,  67,  -85,
        -35,  5,  0,  11,  32,  83,  69,  71,  95,  76,  73,
        78,  69,  84,  79,  68,  98,  -84,  99,  67,  -85,
        -35,  5,  0,  11,  32,  83,  69,  71,  95,  81,
        85,  65,  68,  84,  79,  68,  98,  -87,  1,  67,
        -85,  -35,  1,  68,  98,  -107,  -10,  67,  -85,  -43,
        23,  0,  11,  32,  83,  69,  71,  95,  76,  73,
        78,  69,  84,  79,  68,  98,  -95,  -98,  67,  -85,
        -39,  -11,  0,  11,  32,  83,  69,  71,  95,  81,
        85,  65,  68,  84,  79,  68,  98,  -53,  -8,  67,
        -85,  -21,  -99,  68,  98,  -10,  -6,  67,  -85,
        -21,  -95,  0,  11,  32,  83,  69,  71,  95,  76,  73,
        78,  69,  84,  79,  68,  99,  2,  -94,  67,  -85,
        -21,  -95,  0,  11,  32,  83,  69,  71,  95,  81,
        85,  65,  68,  84,  79,  68,  98,  -56,  -104, 67,
        -85,  -21,  61,  68,  98,  -109,  28,  67,  -85,  -73,
        -35,  0,  11,  32,  83,  69,  71,  95,  76,  73,
        78,  69,  84,  79,  68,  98,  -91,  -62,  67,  -85,
        -55,  -22,  0,  11,  32,  83,  69,  71,  95,  76,
        73,  78,  69,  84,  79,  68,  99,  -82,  -62,  67,
        -89,  -125,  126,  0,  11,  32,  83,  69,  71,  95,
        76,  73,  78,  69,  84,  79,  68,  99,  -100,  28,
        67,  -89,  113,  113,  0,  11,  32,  83,  69,  71,
        95,  81,  85,  65,  68,  84,  79,  68,  99,  83,
        -6,  67,  -89,  44,  5,  68,  99,  2,  -94,  67,
        -89,  43,  -95,  0,  11,  32,  83,  69,  71,  95,
        76,  73,  78,  69,  84,  79,  68,  98,  -10,  -6,
        67,  -89,  43,  -95,  0,  11,  32,  83,  69,  71,
        95,  81,  85,  65,  68,  84,  79,  68,  99,  10,
        -82,  67,  -89,  43,  -91,  68,  99,  29,  -72,  67,
        -89,  51,  -113,  0,  11,  32,  83,  69,  71,  95,
        76,  73,  78,  69,  84,  79,  68,  99,  18,  16,
        67,  -89,  46,  -79,  0,  11,  32,  83,  69,  71,
        95,  81,  85,  65,  68,  84,  79,  68,  98,  -25,
        -73,  67,  -89,  29,  9,  68,  98,  -84,  99,  67,
        -89,  29,  5,  0,  11,  32,  83,  69,  71,  95,
        76,  73,  78,  69,  84,  79,  68,  98,  -112,  107,
        67,  -89,  29,  5,  0,  11,  32,  83,  69,  71,
        95,  81,  85,  65,  68,  84,  79,  68,  98,  78,
        121,  67,  -89,  28,  -15,  68,  98,  29,  -110,  67,
        -89,  53,  -27,  0,  11,  32,  83,  69,  71,  95,
        76,  73,  78,  69,  84,  79,  68,  98,  6,  66,
        67,  -89,  65,  -42,  0,  11,  32,  83,  69,  71,
        95,  76,  73,  78,  69,  84,  79,  68,  98,  -100,
        -76,  67,  -85,  -36,  6,  0,  10,  32,  83,  69,
        71,  95,  67,  76,  79,  83,  69,
    };

    public static final byte[] shapeAdded = {
        0,  11,  32,  83,  69,  71,  95,  77,  79,  86,
        69,  84,  79,  68,  102,  54,  -63,  67,  -84,  -102,
        29,  0,  11,  32,  83,  69,  71,  95,  76,  73,  78,
        69,  84,  79,  68,  96,  -120,  114,  67,  -94,
        2,  48,  0,  11,  32,  83,  69,  71,  95,  76,  73,
        78,  69,  84,  79,  68,  94,  -119,  0,  67,
        -86,  67,  -25,  0,  10,  32,  83,  69,  71,  95,
        67,  76,  79,  83,  69,
    };

    public static void main(String[] args) {
        // Reversing the order of these try blocks has no effect.
        try {
            testAdd();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        try {
            testSubtract();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void testSubtract() throws IOException
    {
        Shape lineShape = loadShape(strokedLineToSubtract, "Line");
        Shape negShape = loadShape(negSpace, "Space");
        Area lineArea = new Area(lineShape);
        Area negArea = new Area(negShape);
        System.err.println("Attempting to subtract ... ");
        lineArea.subtract(negArea); // This is what throws the OutOfMemoryError
        System.err.println("Subtraction succeeded.");
    }

    private static void testAdd() throws IOException
    {
        Shape lineShape = loadShape(strokedLineToAdd, "Line");
        Shape negShape = loadShape(shapeAdded, "Space");
        Area lineArea = new Area(lineShape);
        Area negArea = new Area(negShape);
        System.err.println("Attempting to add ... ");
        lineArea.add(negArea); // This is what throws the OutOfMemoryError
        System.err.println("Addition succeeded.");
    }

    /**
     * Although this method isn't used by this test case, this is the method
     * used to create the two data sets that the test case uses.
     * @param name The name to give to the variable
     * @param shape
     */
    public static void saveShapeData(Shape shape, String name)
    {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(byteStream);
        try {
            saveShapeToStream(shape, os);
            System.out.println("\npublic static final byte[] " +
                               name + " = {\n");
            byte[] data = byteStream.toByteArray();
            int ii=0;
            for (byte bt : data)
                System.out.print("  " + Byte.toString(bt) + "," +
                                 ((++ii)%20==0? "\n":""));
            System.out.println("};");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveShapeToStream(Shape shape, DataOutputStream pOs)
        throws IOException
    {
        PathIterator iter = shape.getPathIterator(null);
        float[] coords = new float[6];
        while(!iter.isDone()) {
            int type = iter.currentSegment(coords);
            switch(type) {
            case PathIterator.SEG_CLOSE:
                pOs.writeUTF(SEG_CLOSE);
                break;
            case PathIterator.SEG_CUBICTO:
                pOs.writeUTF(SEG_CUBICTO);
                for (float coord : coords)
                    pOs.writeFloat(coord);
                break;
            case PathIterator.SEG_LINETO:
                pOs.writeUTF(SEG_LINETO);
                pOs.writeFloat(coords[0]);
                pOs.writeFloat(coords[1]);
                break;
            case PathIterator.SEG_MOVETO:
                pOs.writeUTF(SEG_MOVETO);
                pOs.writeFloat(coords[0]);
                pOs.writeFloat(coords[1]);
                break;
            case PathIterator.SEG_QUADTO:
                pOs.writeUTF(SEG_QUADTO);
                for (int ii=0; ii<4; ++ii)
                    pOs.writeFloat(coords[ii]);
                break;
            default:
                System.err.print(" UNKNOWN:" + type);
                break;
            }
            iter.next();
        }
    }


    public static Shape loadShape(byte[] fileData, String name)
        throws IOException
    {
        System.out.println("\n\n" + name + ":");
        GeneralPath path = new GeneralPath();
        path.setWindingRule(GeneralPath.WIND_NON_ZERO);
        DataInputStream is=null;
        is = new DataInputStream(new ByteArrayInputStream(fileData));
        float[] coords = new float[6];
        while (is.available()>0)
        {
            String type = is.readUTF();
            System.out.print("\n" + type + "\n   ");
            if (type.equals(SEG_CLOSE)) {
                path.closePath();
            } else if (type.equals(SEG_CUBICTO)) {
                for (int ii=0; ii<6; ++ii)
                    coords[ii] = readFloat(is);
                path.curveTo(coords[0], coords[1],
                             coords[2], coords[3],
                             coords[4], coords[5]);
            } else if (type.equals(SEG_LINETO)) {
                for (int ii=0; ii<2; ++ii)
                    coords[ii] = readFloat(is);
                path.lineTo(coords[0], coords[1]);
            } else if (type.equals(SEG_MOVETO)) {
                for (int ii=0; ii<2; ++ii)
                    coords[ii] = readFloat(is);
                path.moveTo(coords[0], coords[1]);
            } else if (type.equals(SEG_QUADTO)) {
                for (int ii=0; ii<4; ++ii)
                    coords[ii] = readFloat(is);
                path.quadTo(coords[0], coords[1], coords[2], coords[3]);
            }
        }
        return path;
    }

    /**
     * This call reads all the float values and prints them out.
     * @param is
     * @return
     * @throws IOException
     */
    private static float readFloat(DataInputStream is) throws IOException
    {
        float ft = is.readFloat();
        System.out.print("" + ft + ", ");
        return ft;
    }
}
