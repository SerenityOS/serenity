/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8135043
 * @summary ObjectStreamClass.getField(String) too restrictive
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;

public class TestObjectStreamClass {

    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(byteOutput);
        output.writeObject(new TestClass());

        ByteArrayInputStream bais = new ByteArrayInputStream(byteOutput.toByteArray());
        TestObjectInputStream input = new TestObjectInputStream(bais);
        input.readObject();

        ObjectStreamClass osc = input.getDescriptor();

        // All OSC public API methods should complete without throwing.
        osc.getName();
        osc.forClass();
        osc.getField("str");
        osc.getFields();
        osc.getSerialVersionUID();
        osc.toString();
    }

    static class TestClass implements Serializable {
        private static final long serialVersionUID = 1L;

        String str = "hello world";
    }

    static class TestObjectInputStream extends ObjectInputStream {
        private ObjectStreamClass objectStreamClass;

        public TestObjectInputStream(InputStream in) throws IOException {
            super(in);
        }

        public ObjectStreamClass getDescriptor()
            throws IOException, ClassNotFoundException
        {
            return objectStreamClass;
        }

        public ObjectStreamClass readClassDescriptor()
            throws IOException, ClassNotFoundException
        {
            objectStreamClass = super.readClassDescriptor();
            return objectStreamClass;
        }
    }
}
