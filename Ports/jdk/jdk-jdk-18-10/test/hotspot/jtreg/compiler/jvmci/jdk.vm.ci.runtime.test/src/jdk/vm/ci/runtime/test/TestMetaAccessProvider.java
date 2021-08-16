/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @requires vm.jvmci
 * @library ../../../../../
 * @modules jdk.internal.vm.ci/jdk.vm.ci.meta
 *          jdk.internal.vm.ci/jdk.vm.ci.runtime
 *          java.base/jdk.internal.misc
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:-UseJVMCICompiler jdk.vm.ci.runtime.test.TestMetaAccessProvider
 */

package jdk.vm.ci.runtime.test;

import static jdk.vm.ci.meta.MetaUtil.toInternalName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.Test;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.Signature;

/**
 * Tests for {@link MetaAccessProvider}.
 */
public class TestMetaAccessProvider extends TypeUniverse {
    private static final DeoptimizationAction DEOPT_ACTION = DeoptimizationAction.InvalidateRecompile;
    private static final DeoptimizationReason DEOPT_REASON = DeoptimizationReason.Aliasing;
    private static final int INT_23BITS_SET = 0x7FFFFF;
    private static final int[] DEBUG_IDS = new int[]{0, 1, 42, INT_23BITS_SET};
    private static final int[] VALID_ENCODED_VALUES = new int[]{
                    metaAccess.encodeDeoptActionAndReason(DEOPT_ACTION, DEOPT_REASON, DEBUG_IDS[0]).asInt(),
                    metaAccess.encodeDeoptActionAndReason(DEOPT_ACTION, DEOPT_REASON, DEBUG_IDS[1]).asInt(),
                    metaAccess.encodeDeoptActionAndReason(DEOPT_ACTION, DEOPT_REASON, DEBUG_IDS[2]).asInt(),
                    metaAccess.encodeDeoptActionAndReason(DEOPT_ACTION, DEOPT_REASON, DEBUG_IDS[3]).asInt()
    };

    private static boolean isHiddenClass(Class<?> cls) {
        if (cls.isHidden()) {
            return true;
        }

        // Check array of hidden type.
        while (cls.getComponentType() != null) {
            cls = cls.getComponentType();
        }
        if (cls.isHidden()) {
            return true;
        }
        return false;
    }

    @Test
    public void lookupJavaTypeTest() {
        for (Class<?> c : classes) {
            ResolvedJavaType type = metaAccess.lookupJavaType(c);
            assertNotNull(c.toString(), type);
            if (!isHiddenClass(c)) {
                assertEquals(c.toString(), type.getName(), toInternalName(c.getName()));
                assertEquals(c.toString(), type.getName(), toInternalName(type.toJavaName()));
                assertEquals(c.toString(), c.getName(), type.toClassName());
                if (!type.isArray()) {
                    assertEquals(c.toString(), c.getName(), type.toJavaName());
                }
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void lookupJavaTypeNegativeTest() {
        metaAccess.lookupJavaType((Class<?>) null);
    }

    @Test
    public void lookupJavaTypesTest() {
        ResolvedJavaType[] result = metaAccess.lookupJavaTypes(classes.toArray(new Class<?>[classes.size()]));
        int counter = 0;
        for (Class<?> aClass : classes) {
            if (!isHiddenClass(aClass)) {
                assertEquals("Unexpected javaType: " + result[counter] + " while expecting of class: " + aClass, result[counter].toClassName(), aClass.getName());
            }
            counter++;
        }
    }

    @Test(expected = NullPointerException.class)
    public void lookupJavaTypesNegative1Test() {
        assertNull("Expected null", metaAccess.lookupJavaTypes(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void lookupJavaTypesNegative2Test() {
        ResolvedJavaType[] result = metaAccess.lookupJavaTypes(new Class<?>[]{null, null, null});
        for (ResolvedJavaType aType : result) {
            assertNull("Expected null javaType", aType);
        }
        result = metaAccess.lookupJavaTypes(new Class<?>[]{String.class, String.class});
        assertEquals("Results not equals", result[0].getClass(), result[1].getClass());
        assertEquals("Result is not String.class", result[0].getClass(), String.class);
    }

    @Test
    public void lookupJavaMethodTest() {
        for (Class<?> c : classes) {
            for (Method reflect : c.getDeclaredMethods()) {
                ResolvedJavaMethod method = metaAccess.lookupJavaMethod(reflect);
                assertNotNull(method);
                assertTrue(method.getDeclaringClass().equals(metaAccess.lookupJavaType(reflect.getDeclaringClass())));
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void lookupJavaMethodNegativeTest() {
        metaAccess.lookupJavaMethod(null);
    }

    @Test
    public void lookupJavaFieldTest() {
        for (Class<?> c : classes) {
            for (Field reflect : c.getDeclaredFields()) {
                ResolvedJavaField field = metaAccess.lookupJavaField(reflect);
                assertNotNull(field);
                assertTrue(field.getDeclaringClass().equals(metaAccess.lookupJavaType(reflect.getDeclaringClass())));
            }
        }
    }

    @Test
    public void lookupJavaTypeConstantTest() {
        for (ConstantValue cv : constants()) {
            JavaConstant c = cv.value;
            if (c.getJavaKind() == JavaKind.Object && !c.isNull()) {
                Object o = cv.boxed;
                ResolvedJavaType type = metaAccess.lookupJavaType(c);
                assertNotNull(type);
                assertTrue(type.equals(metaAccess.lookupJavaType(o.getClass())));
            } else {
                assertEquals(metaAccess.lookupJavaType(c), null);
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void lookupJavaTypeConstantNegativeTest() {
        metaAccess.lookupJavaType((JavaConstant) null);
    }

    @Test
    public void getMemorySizeTest() {
        for (ConstantValue cv : constants()) {
            JavaConstant c = cv.value;
            if (c.getJavaKind() == JavaKind.Illegal) {
                continue;
            }
            long memSize = metaAccess.getMemorySize(c);
            if (c.isNull()) {
                assertEquals("Expected size = 0 for null", memSize, 0L);
            } else {
                assertTrue("Expected size != 0 for " + cv, memSize != 0L);
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void getMemorySizeNegativeTest() {
        metaAccess.getMemorySize(null);
    }

    @Test
    public void parseMethodDescriptorTest() {
        for (String retType : new String[]{"V", "Z", "Ljava/lang/String;"}) {
            for (String paramTypes : new String[]{"", "B",
                            "Ljava/lang/String;", "JLjava/lang/String;",
                            "Ljava/lang/String;F",
                            "[Ljava/lang/String;ZBCDFIJLS[ILjava/lang/Object;"}) {
                String signature = "(" + paramTypes + ")" + retType;
                Signature result = metaAccess.parseMethodDescriptor(signature);
                assertEquals("Expected signatures to be equal", result.toMethodDescriptor(), signature);
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void parseMethodDescriptorNegativeNullTest() {
        metaAccess.parseMethodDescriptor(null);
    }

    @Test(expected = NullPointerException.class)
    public void encodeDeoptActionAndReasonNegative1Test() {
        metaAccess.encodeDeoptActionAndReason(null, DeoptimizationReason.Aliasing, 0);

    }

    @Test(expected = NullPointerException.class)
    public void encodeDeoptActionAndReasonNegative2Test() {
        metaAccess.encodeDeoptActionAndReason(DeoptimizationAction.InvalidateRecompile, null, 0);
    }

    @Test
    public void decodeDeoptReasonTest() {
        for (int encoded : VALID_ENCODED_VALUES) {
            JavaConstant value = JavaConstant.forInt(encoded);
            DeoptimizationReason reason = metaAccess.decodeDeoptReason(value);
            assertEquals("Expected equal reasons", reason, DEOPT_REASON);
        }
    }

    @Test
    public void decodeDeoptReasonNegative1Test() {
        int encoded = 42;
        JavaConstant value = JavaConstant.forInt(encoded);
        metaAccess.decodeDeoptReason(value);
    }

    @Test(expected = NullPointerException.class)
    public void decodeDeoptReasonNegative2Test() {
        metaAccess.decodeDeoptReason(null);
    }

    @Test
    public void decodeDeoptActionTest() {
        for (int encoded : VALID_ENCODED_VALUES) {
            JavaConstant value = JavaConstant.forInt(encoded);
            DeoptimizationAction action = metaAccess.decodeDeoptAction(value);
            assertEquals("Expected equal actions", action, DEOPT_ACTION);
        }
    }

    @Test
    public void decodeDeoptActionNegative1Test() {
        int encoded = 123456789;
        JavaConstant value = JavaConstant.forInt(encoded);
        metaAccess.decodeDeoptAction(value);
    }

    @Test(expected = NullPointerException.class)
    public void decodeDeoptActionNegative2Test() {
        metaAccess.decodeDeoptAction(null);
    }

    @Test
    public void decodeDebugIdTest() {
        for (int i = 0; i < VALID_ENCODED_VALUES.length; i++) {
            JavaConstant value = JavaConstant.forInt(VALID_ENCODED_VALUES[i]);
            assertEquals("Unexpected debugId", metaAccess.decodeDebugId(value), DEBUG_IDS[i]);
        }
    }

    @Test
    public void parseSignatureTest() {
        for (String badSig : new String[]{"", "()", "(", "()Vextra", "()E", "(E)", "(Ljava.lang.Object;)V"}) {
            try {
                metaAccess.parseMethodDescriptor(badSig);
                throw new AssertionError("Expected signature to be invalid: " + badSig);
            } catch (IllegalArgumentException e) {
            }
        }
    }
}
