/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4403192
 * @summary Verify that serialization includes the offending class name in the
 *          message string of a NotSerializableException.
 */

import java.io.*;

public class ExceptionDetail {
    public static void main(String[] args) throws Exception {
        String className = ExceptionDetail.class.getName();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        try {
            oout.writeObject(new ExceptionDetail());
            throw new Error();
        } catch (NotSerializableException ex) {
            if (ex.getMessage().indexOf(className) == -1) {
                throw new Error();
            }
        }
        oout.close();

        ObjectInputStream oin = new ObjectInputStream(
            new ByteArrayInputStream(bout.toByteArray()));
        try {
            oin.readObject();
            throw new Error();
        } catch (WriteAbortedException ex) {
            if (ex.detail.getMessage().indexOf(className) == -1) {
                throw new Error();
            }
        }
        oin.close();
    }
}
