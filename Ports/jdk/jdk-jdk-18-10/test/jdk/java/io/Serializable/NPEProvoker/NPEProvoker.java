/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6541870
 * @summary this test checks that ObjectInputStream throws an IOException
 *          instead of a NullPointerException when deserializing an ArrayList
 *          of Externalizables if there is an IOException while deserializing
 *          one of these Externalizables.
 *
 * @author Andrey Ozerov
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class NPEProvoker implements java.io.Externalizable {
    private static final long serialVersionUID = 1L;

    private String test = "test";

    public void readExternal(ObjectInput in) throws IOException,
        ClassNotFoundException
    {
        throw new IOException(); //io exception for whatever reason
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(test);
    }

    public static void main(String[] args) {
        System.err.println("\n Regression test for bug 6541870\n");
        try {
            ArrayList<NPEProvoker> list = new ArrayList<>();
            list.add(new NPEProvoker());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(list);

            ObjectInputStream ois =
                new ObjectInputStream(new ByteArrayInputStream(
                baos.toByteArray()));
            ois.readObject();
            throw new Error();
        } catch (IOException e) {
            System.err.println("\nTEST PASSED");
        } catch (ClassNotFoundException e) {
            throw new Error();
        } catch (NullPointerException e) {
            throw new Error();
        }
    }
}
