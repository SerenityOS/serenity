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
 * @bug 6783290
 * @summary Test correct reading of an empty Descriptor.
 * @author Jaroslav Bachorik
 *
 * @run clean SerializationTest1
 * @run build SerializationTest1
 * @run main SerializationTest1
 */

import java.io.*;
import javax.management.*;

public class SerializationTest1 {
    public static void main(String[] args) throws Exception {
        MBeanInfo mi1 = new MBeanInfo("",
                                      "",
                                      new MBeanAttributeInfo[]{},
                                      new MBeanConstructorInfo[]{},
                                      new MBeanOperationInfo[]{},
                                      new MBeanNotificationInfo[]{},
                                      ImmutableDescriptor.EMPTY_DESCRIPTOR);

        test(mi1);

        MBeanFeatureInfo mfi2 = new MBeanFeatureInfo("",
                                                     "",
                                                     ImmutableDescriptor.EMPTY_DESCRIPTOR);

        test(mfi2);
    }

    public static void test(Object obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);

        final boolean[] failed = new boolean[]{false};
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(bais) {
            protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                System.out.println("*** " + desc.getName());
                if (desc.getName().equals("[Ljava.lang.Object;")) { // javax.management.Descriptor fields
                    Thread.dumpStack();
                    for(StackTraceElement e : Thread.currentThread().getStackTrace()) { // checking for the deserialization location
                        if (e.getMethodName().equals("skipCustomData")) { // indicates the presence of unread values from the custom object serialization
                            failed[0] = true;
                        }
                    }
                }
                return super.resolveClass(desc); //To change body of generated methods, choose Tools | Templates.
            }
        };
        Object newObj = ois.readObject();

        if (failed[0]) {
            throw new RuntimeException("Zero-length descriptor not read back");
        }
    }
}
