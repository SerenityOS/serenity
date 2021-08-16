/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6204469
 * @summary Test ImmutableDescriptor serialization.
 * @author Eamonn McManus
 *
 * @run clean ImmutableDescriptorSerialTest
 * @run build ImmutableDescriptorSerialTest
 * @run main ImmutableDescriptorSerialTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;

public class ImmutableDescriptorSerialTest {
    public static void main(String[] args) throws Exception {
        System.out.println("Test that ImmutableDescriptor.EMPTY_DESCRIPTOR " +
                "deserializes identically");
        if (serialize(ImmutableDescriptor.EMPTY_DESCRIPTOR) !=
                ImmutableDescriptor.EMPTY_DESCRIPTOR) {
            throw new Exception("ImmutableDescriptor.EMPTY_DESCRIPTOR did not " +
                    "deserialize identically");
        }
        System.out.println("...OK");

        System.out.println("Test that serialization preserves case and " +
                "that deserialized object is case-insensitive");
        Descriptor d = new ImmutableDescriptor("a=aval", "B=Bval", "cC=cCval");
        Descriptor d1 = serialize(d);
        Set<String> keys = new HashSet(Arrays.asList(d1.getFieldNames()));
        if (keys.size() != 3 ||
                !keys.containsAll(Arrays.asList("a", "B", "cC"))) {
            throw new Exception("Keys don't match: " + keys);
        }
        for (String key : keys) {
            String value = (String) d.getFieldValue(key);
            for (String t :
                    Arrays.asList(key, key.toLowerCase(), key.toUpperCase())) {
                String tvalue = (String) d1.getFieldValue(t);
                if (!tvalue.equals(value)) {
                    throw new Exception("Value of " + key + " for " +
                            "deserialized object does not match: " +
                            tvalue + " should be " + value);
                }
            }
        }
        System.out.println("...OK");
    }

    private static <T> T serialize(T x) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(x);
        oout.close();
        byte[] bytes = bout.toByteArray();
        ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
        ObjectInputStream oin = new ObjectInputStream(bin);
        return (T) oin.readObject();
    }
}
