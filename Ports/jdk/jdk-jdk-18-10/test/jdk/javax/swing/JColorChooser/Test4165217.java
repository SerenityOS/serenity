/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4165217
 * @summary Tests JColorChooser serialization
 * @author Ilya Boyandin
 */

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;
import javax.swing.JColorChooser;

public class Test4165217 {
    public static void main(String[] args) {
        JColorChooser chooser = new JColorChooser();
        chooser.setColor(new Color(new Random().nextInt()));

        Color before = chooser.getColor();
        Color after = copy(chooser).getColor();

        if (!after.equals(before)) {
            throw new Error("color is changed after serialization");
        }
    }

    private static JColorChooser copy(JColorChooser chooser) {
        try {
            return (JColorChooser) deserialize(serialize(chooser));
        }
        catch (ClassNotFoundException exception) {
            throw new Error("unexpected exception during class creation", exception);
        }
        catch (IOException exception) {
            throw new Error("unexpected exception during serialization", exception);
        }
    }

    private static byte[] serialize(Object object) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(object);
        oos.flush();
        return baos.toByteArray();
    }

    private static Object deserialize(byte[] array) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(array);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return ois.readObject();
    }
}
