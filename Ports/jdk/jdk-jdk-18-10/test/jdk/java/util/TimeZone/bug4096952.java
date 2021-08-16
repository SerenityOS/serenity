/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4096952
 * @summary simple serialization/deserialization test
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.TimeZone;

public class bug4096952 {

    public static void main(String[] args) {
        int errors = 0;
        String[] ZONES = { "GMT", "MET", "IST" };
        for (String id : ZONES) {
            TimeZone zone = TimeZone.getTimeZone(id);
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (ObjectOutputStream ostream = new ObjectOutputStream(baos)) {
                    ostream.writeObject(zone);
                }
                try (ObjectInputStream istream
                        = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
                    if (!zone.equals(istream.readObject())) {
                        errors++;
                        System.out.println("Time zone " + id + " are not equal to serialized/deserialized one.");
                    } else {
                        System.out.println("Time zone " + id + " ok.");
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                errors++;
                System.out.println(e);
            }
        }
        if (errors > 0) {
            throw new RuntimeException("test failed");
        }
    }
}
