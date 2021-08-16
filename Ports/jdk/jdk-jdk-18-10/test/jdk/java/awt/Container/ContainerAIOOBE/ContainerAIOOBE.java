/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Button;
import java.awt.Component;
import java.awt.Container;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @test
 * @key headful
 * @bug 8059590
 * @summary ArrayIndexOutOfBoundsException occurs when Container with overridden getComponents() is deserialized.
 * @author Alexey Ivanov
 * @run main ContainerAIOOBE
 */
public class ContainerAIOOBE {

    public static void main(final String[] args) throws Exception {
        ZContainer z = new ZContainer();
        z.add(new Button());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(z);
        oos.flush();
        oos.close();

        byte[] array = baos.toByteArray();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(array));

        // Reading the object must not throw ArrayIndexOutOfBoundsException
        ZContainer zz = (ZContainer) ois.readObject();

        if (zz.getComponentCount() != 1) {
            throw new Exception("deserialized object must have 1 component");
        }
        if (!(zz.getComponent(0) instanceof Button)) {
            throw new Exception("deserialized object must contain Button component");
        }
        if (zz.getComponents().length != 0) {
            throw new Exception("deserialized object returns non-empty array");
        }
        System.out.println("Test passed");
    }

    static class ZContainer extends Container {
        public Component[] getComponents() {
            return new Component[0];
        }
    }
}
