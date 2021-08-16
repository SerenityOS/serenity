/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4227189
 * @summary Ensure that class descriptor read, write hooks exist, are backwards
 *          compatible, and work as advertised.
 */

import java.io.*;
import java.util.*;

class Foo implements Serializable {
    private static final long serialVersionUID = 1L;
    Short s = (short) 1;
    Integer i = 2;
    Long l = 3L;

    public boolean equals(Object obj) {
        if (obj instanceof Foo) {
            Foo ofoo = (Foo) obj;
            return s.equals(ofoo.s) && i.equals(ofoo.i) && l.equals(ofoo.l);
        }
        return false;
    }

    public int hashCode() {
        return i;
    }
}

class CustomOutputStream extends ObjectOutputStream {

    boolean hookCalled = false;

    CustomOutputStream(OutputStream out) throws IOException {
        super(out);
        useProtocolVersion(PROTOCOL_VERSION_2);
    }

    protected void writeClassDescriptor(ObjectStreamClass desc)
        throws IOException
    {
        writeUTF(desc.getName());
        hookCalled = true;
    }
}

class CustomInputStream extends ObjectInputStream {

    boolean hookCalled = false;

    CustomInputStream(InputStream in) throws IOException {
        super(in);
    }

    protected ObjectStreamClass readClassDescriptor()
        throws IOException, ClassNotFoundException
    {
        hookCalled = true;
        return ObjectStreamClass.lookup(Class.forName(readUTF()));
    }
}

public class ClassDescHooks implements ObjectStreamConstants {
    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream bout;
        ByteArrayInputStream bin;
        ObjectOutputStream oout;
        ObjectInputStream oin;
        FileInputStream fin;
        File foof;
        CustomOutputStream cout;
        CustomInputStream cin;

        // test for backwards compatibility
        bout = new ByteArrayOutputStream();
        foof = new File(System.getProperty("test.src", "."), "Foo.ser");
        fin = new FileInputStream(foof);
        try {
            while (fin.available() > 0)
                bout.write(fin.read());
        } finally {
            fin.close();
        }
        byte[] buf1 = bout.toByteArray();

        bout = new ByteArrayOutputStream();
        oout = new ObjectOutputStream(bout);
        Foo foo = new Foo();
        oout.writeObject(foo);
        oout.flush();
        byte[] buf2 = bout.toByteArray();

        if (! Arrays.equals(buf1, buf2))
            throw new Error("Incompatible stream format (write)");

        Foo foocopy;
        fin = new FileInputStream(foof);
        try {
            oin = new ObjectInputStream(fin);
            foocopy = (Foo) oin.readObject();
            if (! foo.equals(foocopy))
                throw new Error("Incompatible stream format (read)");
        } finally {
            fin.close();
        }

        // make sure write hook not called when old protocol in use
        bout = new ByteArrayOutputStream();
        cout = new CustomOutputStream(bout);
        cout.useProtocolVersion(PROTOCOL_VERSION_1);
        cout.writeObject(foo);
        if (cout.hookCalled)
            throw new Error("write descriptor hook should not be called");

        // write custom class descriptor representations
        bout = new ByteArrayOutputStream();
        cout = new CustomOutputStream(bout);
        cout.writeObject(foo);
        cout.flush();
        bin = new ByteArrayInputStream(bout.toByteArray());
        cin = new CustomInputStream(bin);
        foocopy = (Foo) cin.readObject();
        if (! cout.hookCalled)
            throw new Error("write descriptor hook never called");
        if (! cin.hookCalled)
            throw new Error("read descriptor hook never called");
        if (! foo.equals(foocopy))
            throw new Error("serialization failed when hooks active");
    }
}
