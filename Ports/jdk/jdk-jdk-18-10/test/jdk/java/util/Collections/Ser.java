/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4190323
 * @summary EMPTY_SET, EMPTY_LIST, and the collections returned by
 *          nCopies and singleton were spec'd to be serializable, but weren't.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Ser {
    public static void main(String[] args) throws Exception {

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(Collections.EMPTY_SET);
            out.flush();
            ObjectInputStream in = new ObjectInputStream(
                    new ByteArrayInputStream(bos.toByteArray()));

            if (!Collections.EMPTY_SET.equals(in.readObject()))
                throw new RuntimeException("empty set Ser/Deser failure.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize empty set:" + e);
        }

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(Collections.EMPTY_LIST);
            out.flush();
            ObjectInputStream in = new ObjectInputStream(
                    new ByteArrayInputStream(bos.toByteArray()));

            if (!Collections.EMPTY_LIST.equals(in.readObject()))
                throw new RuntimeException("empty list Ser/Deser failure.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize empty list:" + e);
        }

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            Set gumby = Collections.singleton("gumby");
            out.writeObject(gumby);
            out.flush();
            ObjectInputStream in = new ObjectInputStream(
                    new ByteArrayInputStream(bos.toByteArray()));

            if (!gumby.equals(in.readObject()))
                throw new RuntimeException("Singleton Ser/Deser failure.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize singleton:" + e);
        }

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            List gumbies = Collections.nCopies(50, "gumby");
            out.writeObject(gumbies);
            out.flush();
            ObjectInputStream in = new ObjectInputStream(
                    new ByteArrayInputStream(bos.toByteArray()));

            if (!gumbies.equals(in.readObject()))
                throw new RuntimeException("nCopies Ser/Deser failure.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize nCopies:" + e);
        }
    }
}
