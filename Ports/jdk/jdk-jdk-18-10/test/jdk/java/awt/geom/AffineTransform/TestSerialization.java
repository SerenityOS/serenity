/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6213608
 * @summary Test that AffineTransform can deserialize appropriate versions.
 * @author flar
 * @run main TestSerialization
 */

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.awt.geom.AffineTransform;

public class TestSerialization {
    public static void main(String argv[]) {
        if (argv.length > 0) {
            System.out.println("Saving from: "+
                               System.getProperty("java.version"));
            writeSer(argv[0]);
            return;
        }
        System.out.println("Testing on: "+System.getProperty("java.version"));
        testReadWrite();
        readSer("serial.1.2", true);
    }

    public static AffineTransform testATs[] = {
        new AffineTransform(),
        AffineTransform.getScaleInstance(2.5, -3.0),
        AffineTransform.getRotateInstance(Math.PI / 4.0),
        AffineTransform.getShearInstance(1.0, -3.0),
        AffineTransform.getTranslateInstance(25.0, 12.5),
        makeComplexAT(),
    };

    public static AffineTransform makeComplexAT() {
        AffineTransform at = new AffineTransform();
        at.scale(2.5, -3.0);
        at.rotate(Math.PI / 4.0);
        at.shear(1.0, -3.0);
        at.translate(25.0, 12.5);
        return at;
    };

    public static void testReadWrite() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            testWrite(oos);
            oos.flush();
            oos.close();
            byte buf[] = baos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            ObjectInputStream ois = new ObjectInputStream(bais);
            testRead(ois, true);
        } catch (InvalidClassException ice) {
            throw new RuntimeException("Object read failed from loopback");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("IOException testing loopback");
        }
    }

    public static String resolve(String relfilename) {
        String dir = System.getProperty("test.src");
        if (dir == null) {
            return relfilename;
        } else {
            return dir+"/"+relfilename;
        }
    }

    public static void readSer(String filename, boolean shouldsucceed) {
        try {
            FileInputStream fis = new FileInputStream(resolve(filename));
            ObjectInputStream ois = new ObjectInputStream(fis);
            testRead(ois, shouldsucceed);
        } catch (InvalidClassException ice) {
            throw new RuntimeException("Object read failed from: "+filename);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("IOException reading: "+filename);
        }
    }

    public static void testRead(ObjectInputStream ois, boolean shouldsucceed)
        throws IOException
    {
        for (int i = 0; i < testATs.length; i++) {
            AffineTransform at;
            try {
                at = (AffineTransform) ois.readObject();
                if (!shouldsucceed) {
                    throw new RuntimeException("readObj did not fail");
                }
            } catch (ClassNotFoundException e) {
                // Should never happen, but must catch declared exceptions...
                throw new RuntimeException("AffineTransform not found!");
            } catch (InvalidClassException e) {
                if (shouldsucceed) {
                    throw e;
                }
                continue;
            }
            if (!testATs[i].equals(at)) {
                throw new RuntimeException("wrong AT read from stream");
            }
        }
    }

    public static void writeSer(String filename) {
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            testWrite(oos);
            oos.flush();
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("IOException writing: "+filename);
        }
    }

    public static void testWrite(ObjectOutputStream oos)
        throws IOException
    {
        for (int i = 0; i < testATs.length; i++) {
            oos.writeObject(testATs[i]);
        }
    }
}
