/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8141615
 * @summary Tests new public methods at ConstantPool
 * @modules java.base/jdk.internal.access
 *          java.base/jdk.internal.reflect
 * @library /test/lib
 * @compile ConstantPoolTestDummy.jasm
 * @run main jdk.internal.reflect.constantPool.ConstantPoolTest
 */

package jdk.internal.reflect.constantPool;

import java.util.HashMap;
import java.util.Map;
import jdk.internal.access.SharedSecrets;
import jdk.internal.reflect.ConstantPool;
import jdk.test.lib.Asserts;

public class ConstantPoolTest {

    private static final Class<?> TEST_CLASS = ConstantPoolTestDummy.class;
    private static final ConstantPool CP = SharedSecrets.getJavaLangAccess()
            .getConstantPool(TEST_CLASS);

    public static void main(String[] s) {
        for (TestCase testCase : TestCase.values()) {
            testCase.test();
        }
    }

    public static enum TestCase {
        GET_TAG_AT {
            {
                referenceMap.put(1, ConstantPool.Tag.METHODREF);
                referenceMap.put(2, ConstantPool.Tag.CLASS);
                referenceMap.put(4, ConstantPool.Tag.UTF8);
                referenceMap.put(10, ConstantPool.Tag.NAMEANDTYPE);
                referenceMap.put(13, ConstantPool.Tag.LONG);
                referenceMap.put(15, ConstantPool.Tag.INTEGER);
                referenceMap.put(16, ConstantPool.Tag.INTERFACEMETHODREF);
                referenceMap.put(21, ConstantPool.Tag.DOUBLE);
                referenceMap.put(23, ConstantPool.Tag.STRING);
                referenceMap.put(25, ConstantPool.Tag.INVOKEDYNAMIC);
                referenceMap.put(29, ConstantPool.Tag.METHODHANDLE);
                referenceMap.put(30, ConstantPool.Tag.METHODTYPE);
                referenceMap.put(48, ConstantPool.Tag.FIELDREF);
                referenceMap.put(52, ConstantPool.Tag.FLOAT);
            }
            @Override
            void testIndex(int cpi, Object reference) {
                ConstantPool.Tag tagToVerify = CP.getTagAt(cpi);
                ConstantPool.Tag tagToRefer = (ConstantPool.Tag) reference;
                String msg = String.format("Method getTagAt works not as expected"
                        + "at CP entry #%d: got CP tag %s, but should be %s",
                        cpi, tagToVerify.name(), tagToRefer.name());
                Asserts.assertEquals(tagToVerify, tagToRefer, msg);
            }
        },
        GET_CLASS_REF_INDEX_AT {
            {
                referenceMap.put(1, 3);
                referenceMap.put(16, 17);
                referenceMap.put(32, 35);
                referenceMap.put(34, 3);
                referenceMap.put(48, 2);
            }
            @Override
            void testIndex(int cpi, Object reference) {
                int indexToVerify = CP.getClassRefIndexAt(cpi);
                int indexToRefer = (int) reference;
                String msg = String.format("Method getClassRefIndexAt works not"
                        + " as expected at CP entry #%d:"
                        + " got index %d, but should be %d",
                        cpi, indexToVerify, indexToRefer);
                Asserts.assertEquals(indexToVerify, indexToRefer, msg);
            }
        },
        GET_NAME_AND_TYPE_REF_INDEX_AT {
            {
                referenceMap.put(1, 10);
                referenceMap.put(16, 18);
                referenceMap.put(25, 26);
                referenceMap.put(32, 36);
                referenceMap.put(34, 37);
                referenceMap.put(48, 49);
            }
            @Override
            void testIndex(int cpi, Object reference) {
                int indexToRefer = (int) reference;
                int indexToVerify = CP.getNameAndTypeRefIndexAt(cpi);
                String msg = String.format("Method getNameAndTypeRefIndexAt works"
                        + " not as expected at CP entry #%d:"
                        + " got index %d, but should be %d",
                        cpi, indexToVerify, indexToRefer);
                Asserts.assertEquals(indexToVerify, indexToRefer, msg);
            }
        },
        GET_NAME_AND_TYPE_REF_INFO_AT {
            {
                referenceMap.put(10, new String[]{"<init>", "()V"});
                referenceMap.put(18, new String[]{"run", "()V"});
                referenceMap.put(26, new String[]{"accept", "()Ljava/util/function/Consumer;"});
                referenceMap.put(36, new String[]{"metafactory",
                        "(Ljava/lang/invoke/MethodHandles$Lookup;"
                        + "Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                        + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;"
                        + "Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"});
                referenceMap.put(37, new String[]{"toString", "()Ljava/lang/String;"});
                referenceMap.put(49, new String[]{"myField", "I"});
            }
            @Override
            void testIndex(int cpi, Object reference) {
                String[] natInfo = CP.getNameAndTypeRefInfoAt(cpi);
                String msg = String.format("Method getNameAndTypeRefInfoAt"
                        + " works not as expected at CP entry #%d:"
                        + " returned value should not be null", cpi);
                Asserts.assertNotNull(natInfo, msg);
                String[] castedReference = (String[]) reference;
                int natInfoLength = natInfo.length;
                msg = String.format("Method getNameAndTypeRefInfoAt"
                        + " works not as expected at CP entry #%d:"
                        + " length of the returned string array is %d, but should be 2",
                        cpi, natInfoLength);
                Asserts.assertEquals(natInfoLength, 2, msg);
                String[] nameOrType = new String[]{"name", "type"};
                for (int i = 0; i < 2; i++) {
                    String infoToVerify = natInfo[i];
                    String infoToRefer = castedReference[i];
                    msg = String.format("Method getNameAndTypeRefInfoAt"
                            + " works not as expected at CP entry #%d:"
                            + " got %s info %s, but should be %s",
                            cpi, nameOrType[i], infoToVerify, infoToRefer);
                    Asserts.assertEquals(infoToVerify, infoToRefer, msg);
                }
            }
        };

        protected final Map<Integer, Object> referenceMap;
        TestCase() {
            this.referenceMap = new HashMap<>();
        }
        abstract void testIndex(int cpi, Object reference);
        public void test() {
            referenceMap.forEach(this::testIndex);
        }
    }
}
