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
 * @bug 6288100
 * @summary Test the new serialization/deserialization methods.
 * @author Shanliang JIANG
 *
 * @run clean SerializationTest
 * @run build SerializationTest
 * @run main SerializationTest
 */

import java.io.*;
import javax.management.*;

public class SerializationTest {
    public static void main(String[] args) throws Exception {
        MBeanFeatureInfo mfi1 = new MBeanFeatureInfo("", "", null);
        test(mfi1);

        MBeanFeatureInfo mfi2 = new MBeanFeatureInfo("",
                                                     "",
                                                     ImmutableDescriptor.EMPTY_DESCRIPTOR);
        test(mfi2);

        MBeanFeatureInfo mfi3 = new MBeanFeatureInfo("", "",
                                     new ImmutableDescriptor(new String[] {"hi"},
                                                             new Object[] {"ha"}));
        test(mfi3);

        MBeanInfo mi1 = new MBeanInfo("",
                                      "",
                                      new MBeanAttributeInfo[]{},
                                      new MBeanConstructorInfo[]{},
                                      new MBeanOperationInfo[]{},
                                      new MBeanNotificationInfo[]{},
                                      null);


        test(mi1);

        MBeanInfo mi2 = new MBeanInfo("",
                                      "",
                                      new MBeanAttributeInfo[]{},
                                      new MBeanConstructorInfo[]{},
                                      new MBeanOperationInfo[]{},
                                      new MBeanNotificationInfo[]{},
                                      ImmutableDescriptor.EMPTY_DESCRIPTOR);


        test(mi2);

        MBeanInfo mi3 = new MBeanInfo("",
                                      "",
                                      new MBeanAttributeInfo[]{},
                                      new MBeanConstructorInfo[]{},
                                      new MBeanOperationInfo[]{},
                                      new MBeanNotificationInfo[]{},
                                      new ImmutableDescriptor(new String[] {"hi"},
                                                             new Object[] {"ha"}));


        test(mi3);


    }

    public static void test(Object obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Object newObj = ois.readObject();

        if (!obj.equals(newObj)) {
            throw new RuntimeException("Serialization/deserialization failed.");
        }
    }
}
