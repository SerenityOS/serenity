/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.MethodType;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.testng.annotations.Test;

import static java.lang.constant.ConstantDescs.CD_int;
import static java.lang.constant.ConstantDescs.CD_void;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * @test
 * @compile MethodTypeDescTest.java
 * @run testng MethodTypeDescTest
 * @summary unit tests for java.lang.constant.MethodTypeDesc
 */
@Test
public class MethodTypeDescTest extends SymbolicDescTest {

    private void testMethodTypeDesc(MethodTypeDesc r) throws ReflectiveOperationException {
        testSymbolicDesc(r);

        // Tests accessors (rType, pType, pCount, pList, pArray, descriptorString),
        // factories (ofDescriptor, of), equals
        assertEquals(r, MethodTypeDesc.ofDescriptor(r.descriptorString()));
        assertEquals(r, MethodTypeDesc.of(r.returnType(), r.parameterArray()));
        assertEquals(r, MethodTypeDesc.of(r.returnType(), r.parameterList().toArray(new ClassDesc[0])));
        assertEquals(r, MethodTypeDesc.of(r.returnType(), r.parameterList().stream().toArray(ClassDesc[]::new)));
        assertEquals(r, MethodTypeDesc.of(r.returnType(), IntStream.range(0, r.parameterCount())
                                                                   .mapToObj(r::parameterType)
                                                                   .toArray(ClassDesc[]::new)));
    }

    private void testMethodTypeDesc(MethodTypeDesc r, MethodType mt) throws ReflectiveOperationException {
        testMethodTypeDesc(r);

        assertEquals(r.resolveConstantDesc(LOOKUP), mt);
        assertEquals(mt.describeConstable().get(), r);

        assertEquals(r.descriptorString(), mt.toMethodDescriptorString());
        assertEquals(r.parameterCount(), mt.parameterCount());
        assertEquals(r.parameterList(), mt.parameterList().stream().map(SymbolicDescTest::classToDesc).collect(toList()));
        assertEquals(r.parameterArray(), Stream.of(mt.parameterArray()).map(SymbolicDescTest::classToDesc).toArray(ClassDesc[]::new));
        for (int i=0; i<r.parameterCount(); i++)
            assertEquals(r.parameterType(i), classToDesc(mt.parameterType(i)));
        assertEquals(r.returnType(), classToDesc(mt.returnType()));
    }

    private void assertMethodType(ClassDesc returnType,
                                  ClassDesc... paramTypes) throws ReflectiveOperationException {
        String descriptor = Stream.of(paramTypes).map(ClassDesc::descriptorString).collect(joining("", "(", ")"))
                            + returnType.descriptorString();
        MethodTypeDesc mtDesc = MethodTypeDesc.of(returnType, paramTypes);

        // MTDesc accessors
        assertEquals(descriptor, mtDesc.descriptorString());
        assertEquals(returnType, mtDesc.returnType());
        assertEquals(paramTypes, mtDesc.parameterArray());
        assertEquals(Arrays.asList(paramTypes), mtDesc.parameterList());
        assertEquals(paramTypes.length, mtDesc.parameterCount());
        for (int i=0; i<paramTypes.length; i++)
            assertEquals(paramTypes[i], mtDesc.parameterType(i));

        // Consistency between MT and MTDesc
        MethodType mt = MethodType.fromMethodDescriptorString(descriptor, null);
        testMethodTypeDesc(mtDesc, mt);

        // changeReturnType
        for (String r : returnDescs) {
            ClassDesc rc = ClassDesc.ofDescriptor(r);
            MethodTypeDesc newDesc = mtDesc.changeReturnType(rc);
            assertEquals(newDesc, MethodTypeDesc.of(rc, paramTypes));
            testMethodTypeDesc(newDesc, mt.changeReturnType((Class<?>)rc.resolveConstantDesc(LOOKUP)));
        }

        // try with null parameter
        try {
            MethodTypeDesc newDesc = mtDesc.changeReturnType(null);
            fail("should fail with NPE");
        } catch (NullPointerException ex) {
            // good
        }

        // changeParamType
        for (int i=0; i<paramTypes.length; i++) {
            for (String p : paramDescs) {
                ClassDesc pc = ClassDesc.ofDescriptor(p);
                ClassDesc[] ps = paramTypes.clone();
                ps[i] = pc;
                MethodTypeDesc newDesc = mtDesc.changeParameterType(i, pc);
                assertEquals(newDesc, MethodTypeDesc.of(returnType, ps));
                testMethodTypeDesc(newDesc, mt.changeParameterType(i, (Class<?>)pc.resolveConstantDesc(LOOKUP)));
            }
        }

        // dropParamType
        for (int i=0; i<paramTypes.length; i++) {
            int k = i;
            ClassDesc[] ps = IntStream.range(0, paramTypes.length)
                                      .filter(j -> j != k)
                                      .mapToObj(j -> paramTypes[j])
                                      .toArray(ClassDesc[]::new);
            MethodTypeDesc newDesc = mtDesc.dropParameterTypes(i, i + 1);
            assertEquals(newDesc, MethodTypeDesc.of(returnType, ps));
            testMethodTypeDesc(newDesc, mt.dropParameterTypes(i, i+1));
        }

        badDropParametersTypes(CD_void, paramDescs);

        // addParam
        for (int i=0; i <= paramTypes.length; i++) {
            for (ClassDesc p : paramTypes) {
                int k = i;
                ClassDesc[] ps = IntStream.range(0, paramTypes.length + 1)
                                          .mapToObj(j -> (j < k) ? paramTypes[j] : (j == k) ? p : paramTypes[j-1])
                                          .toArray(ClassDesc[]::new);
                MethodTypeDesc newDesc = mtDesc.insertParameterTypes(i, p);
                assertEquals(newDesc, MethodTypeDesc.of(returnType, ps));
                testMethodTypeDesc(newDesc, mt.insertParameterTypes(i, (Class<?>)p.resolveConstantDesc(LOOKUP)));
            }
        }

        badInsertParametersTypes(CD_void, paramDescs);
    }

    private void badInsertParametersTypes(ClassDesc returnType, String... paramDescTypes) {
        ClassDesc[] paramTypes =
                IntStream.rangeClosed(0, paramDescTypes.length - 1)
                        .mapToObj(i -> ClassDesc.ofDescriptor(paramDescTypes[i])).toArray(ClassDesc[]::new);
        MethodTypeDesc mtDesc = MethodTypeDesc.of(returnType, paramTypes);
        try {
            MethodTypeDesc newDesc = mtDesc.insertParameterTypes(-1, paramTypes);
            fail("pos < 0 should have failed");
        } catch (IndexOutOfBoundsException ex) {
            // good
        }

        try {
            MethodTypeDesc newDesc = mtDesc.insertParameterTypes(paramTypes.length + 1, paramTypes);
            fail("pos > current arguments length should have failed");
        } catch (IndexOutOfBoundsException ex) {
            // good
        }

        try {
            ClassDesc[] newParamTypes = new ClassDesc[1];
            newParamTypes[0] = CD_void;
            MethodTypeDesc newDesc = MethodTypeDesc.of(returnType, CD_int);
            newDesc = newDesc.insertParameterTypes(0, newParamTypes);
            fail("shouldn't allow parameters with class descriptor CD_void");
        } catch (IllegalArgumentException ex) {
            // good
        }

        try {
            MethodTypeDesc newDesc = MethodTypeDesc.of(returnType, CD_int);
            newDesc = newDesc.insertParameterTypes(0, null);
            fail("should fail with NPE");
        } catch (NullPointerException ex) {
            // good
        }

        try {
            ClassDesc[] newParamTypes = new ClassDesc[1];
            newParamTypes[0] = null;
            MethodTypeDesc newDesc = MethodTypeDesc.of(returnType, CD_int);
            newDesc = newDesc.insertParameterTypes(0, newParamTypes);
            fail("should fail with NPE");
        } catch (NullPointerException ex) {
            // good
        }
    }

    private void badDropParametersTypes(ClassDesc returnType, String... paramDescTypes) {
        ClassDesc[] paramTypes =
                IntStream.rangeClosed(0, paramDescTypes.length - 1)
                        .mapToObj(i -> ClassDesc.ofDescriptor(paramDescTypes[i])).toArray(ClassDesc[]::new);
        MethodTypeDesc mtDesc = MethodTypeDesc.of(returnType, paramTypes);
        try {
            MethodTypeDesc newDesc = mtDesc.dropParameterTypes(-1, 0);
            fail("start index < 0 should have failed");
        } catch (IndexOutOfBoundsException ex) {
            // good
        }

        try {
            MethodTypeDesc newDesc = mtDesc.dropParameterTypes(paramTypes.length, 0);
            fail("start index = arguments.length should have failed");
        } catch (IndexOutOfBoundsException ex) {
            // good
        }

        try {
            MethodTypeDesc newDesc = mtDesc.dropParameterTypes(paramTypes.length + 1, 0);
            fail("start index > arguments.length should have failed");
        } catch (IndexOutOfBoundsException ex) {
            // good
        }

        try {
            MethodTypeDesc newDesc = mtDesc.dropParameterTypes(0, paramTypes.length + 1);
            fail("end index > arguments.length should have failed");
        } catch (IndexOutOfBoundsException ex) {
            // good
        }

        try {
            MethodTypeDesc newDesc = mtDesc.dropParameterTypes(1, 0);
            fail("start index > end index should have failed");
        } catch (IndexOutOfBoundsException ex) {
            // good
        }
    }

    public void testMethodTypeDesc() throws ReflectiveOperationException {
        for (String r : returnDescs) {
            assertMethodType(ClassDesc.ofDescriptor(r));
            for (String p1 : paramDescs) {
                assertMethodType(ClassDesc.ofDescriptor(r), ClassDesc.ofDescriptor(p1));
                for (String p2 : paramDescs) {
                    assertMethodType(ClassDesc.ofDescriptor(r), ClassDesc.ofDescriptor(p1), ClassDesc.ofDescriptor(p2));
                }
            }
        }
    }

    public void testBadMethodTypeRefs() {
        List<String> badDescriptors = List.of("()II", "()I;", "(I;)", "(I)", "()L", "(V)V",
                                              "(java.lang.String)V", "()[]", "(Ljava/lang/String)V",
                                              "(Ljava.lang.String;)V", "(java/lang/String)V");

        for (String d : badDescriptors) {
            try {
                MethodTypeDesc r = MethodTypeDesc.ofDescriptor(d);
                fail(d);
            }
            catch (IllegalArgumentException e) {
                // good
            }
        }

        // try with null argument
        try {
            MethodTypeDesc r = MethodTypeDesc.ofDescriptor(null);
            fail("should fail with NPE");
        } catch (NullPointerException ex) {
            // good
        }

        // try with void arguments, this will stress another code path in particular
        // ConstantMethodTypeDesc::init
        try {
            MethodTypeDesc r = MethodTypeDesc.of(CD_int, CD_void);
            fail("can't reach here");
        }
        catch (IllegalArgumentException e) {
            // good
        }

        try {
            MethodTypeDesc r = MethodTypeDesc.of(CD_int, null);
            fail("ClassDesc array should not be null");
        }
        catch (NullPointerException e) {
            // good
        }

        try {
            ClassDesc[] paramDescs = new ClassDesc[1];
            paramDescs[0] = null;
            MethodTypeDesc r = MethodTypeDesc.of(CD_int, paramDescs);
            fail("ClassDesc should not be null");
        }
        catch (NullPointerException e) {
            // good
        }
    }
}
