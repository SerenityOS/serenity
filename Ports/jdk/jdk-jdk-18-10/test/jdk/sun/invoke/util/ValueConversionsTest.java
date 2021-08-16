/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package test.sun.invoke.util;

import sun.invoke.util.ValueConversions;
import sun.invoke.util.Wrapper;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandle;
import java.io.Serializable;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/* @test
 * @summary unit tests for value-type conversion utilities
 * @modules java.base/sun.invoke.util
 * @compile -XDignore.symbol.file ValueConversionsTest.java
 * @run junit/othervm test.sun.invoke.util.ValueConversionsTest
 */

/**
 *
 * @author jrose
 */
public class ValueConversionsTest {
    @Test
    public void testUnbox() throws Throwable {
        testUnbox(false);
    }

    @Test
    public void testUnboxCast() throws Throwable {
        testUnbox(true);
    }

    private void testUnbox(boolean doCast) throws Throwable {
        for (Wrapper dst : Wrapper.values()) {
            for (Wrapper src : Wrapper.values()) {
                testUnbox(doCast, dst, src);
            }
        }
    }

    private void testUnbox(boolean doCast, Wrapper dst, Wrapper src) throws Throwable {
        boolean expectThrow = !doCast && !dst.isConvertibleFrom(src);
        if (dst == Wrapper.OBJECT || src == Wrapper.OBJECT)  return;  // must have prims
        if (dst == Wrapper.VOID   || src == Wrapper.VOID  )  return;  // must have values
        if (dst == Wrapper.OBJECT)
            expectThrow = false;  // everything (even VOID==null here) converts to OBJECT
        try {
            for (int n = -5; n < 10; n++) {
                Object box = src.wrap(n);
                switch (src) {
                    case VOID:   assertEquals(box, null); break;
                    case OBJECT: box = box.toString(); break;
                    case SHORT:  assertEquals(box.getClass(), Short.class); break;
                    default:     assertEquals(box.getClass(), src.wrapperType()); break;
                }
                MethodHandle unboxer;
                if (doCast)
                    unboxer = ValueConversions.unboxCast(dst);
                else
                    unboxer = ValueConversions.unboxWiden(dst);
                Object expResult = (box == null) ? dst.zero() : dst.wrap(box);
                Object result = null;
                switch (dst) {
                    case INT:     result = (int)     unboxer.invokeExact(box); break;
                    case LONG:    result = (long)    unboxer.invokeExact(box); break;
                    case FLOAT:   result = (float)   unboxer.invokeExact(box); break;
                    case DOUBLE:  result = (double)  unboxer.invokeExact(box); break;
                    case CHAR:    result = (char)    unboxer.invokeExact(box); break;
                    case BYTE:    result = (byte)    unboxer.invokeExact(box); break;
                    case SHORT:   result = (short)   unboxer.invokeExact(box); break;
                    case BOOLEAN: result = (boolean) unboxer.invokeExact(box); break;
                }
                if (expectThrow) {
                    expResult = "(need an exception)";
                }
                assertEquals("(doCast,expectThrow,dst,src,n,box)="+Arrays.asList(doCast,expectThrow,dst,src,n,box),
                             expResult, result);
            }
        } catch (RuntimeException ex) {
            if (expectThrow)  return;
            System.out.println("Unexpected throw for (doCast,expectThrow,dst,src)="+Arrays.asList(doCast,expectThrow,dst,src));
            throw ex;
        }
    }

    @Test
    public void testBox() throws Throwable {
        for (Wrapper w : Wrapper.values()) {
            if (w == Wrapper.VOID)    continue;  // skip this; no unboxed form
            if (w == Wrapper.OBJECT)  continue;  // skip this; already unboxed
            for (int n = -5; n < 10; n++) {
                Object box = w.wrap(n);
                MethodHandle boxer = ValueConversions.boxExact(w);
                Object expResult = box;
                Object result = null;
                switch (w) {
                    case INT:     result = (Integer) boxer.invokeExact(/*int*/n); break;
                    case LONG:    result = (Long)    boxer.invokeExact((long)n); break;
                    case FLOAT:   result = (Float)   boxer.invokeExact((float)n); break;
                    case DOUBLE:  result = (Double)  boxer.invokeExact((double)n); break;
                    case CHAR:    result = (Character) boxer.invokeExact((char)n); break;
                    case BYTE:    result = (Byte)    boxer.invokeExact((byte)n); break;
                    case SHORT:   result = (Short)   boxer.invokeExact((short)n); break;
                    case BOOLEAN: result = (Boolean) boxer.invokeExact((n & 1) != 0); break;
                }
                assertEquals("(dst,src,n,box)="+Arrays.asList(w,w,n,box),
                             expResult, result);
            }
        }
    }

    @Test
    public void testCast() throws Throwable {
        Class<?>[] types = { Object.class, Serializable.class, String.class, Number.class, Integer.class };
        Object[] objects = { new Object(), Boolean.FALSE,      "hello",      (Long)12L,    (Integer)6    };
        for (Class<?> dst : types) {
            MethodHandle caster = ValueConversions.cast().bindTo(dst);
            assertEquals(caster.type(), MethodHandles.identity(Object.class).type());
            for (Object obj : objects) {
                Class<?> src = obj.getClass();
                boolean canCast = dst.isAssignableFrom(src);
                try {
                    Object result = caster.invokeExact(obj);
                    if (canCast)
                        assertEquals(obj, result);
                    else
                        assertEquals("cast should not have succeeded", dst, obj);
                } catch (ClassCastException ex) {
                    if (canCast)
                        throw ex;
                }
            }
        }
    }

    @Test
    public void testConvert() throws Throwable {
        for (long tval = 0, ctr = 0;;) {
            if (++ctr > 99999)  throw new AssertionError("too many test values");
            // prints 3776 test patterns (3776 = 8*59*8)
            tval = nextTestValue(tval);
            if (tval == 0) {
                break;  // repeat
            }
        }
        for (Wrapper src : Wrapper.values()) {
            for (Wrapper dst : Wrapper.values()) {
                testConvert(src, dst, 0);
            }
        }
    }
    static void testConvert(Wrapper src, Wrapper dst, long tval) throws Throwable {
        if (dst == Wrapper.OBJECT || src == Wrapper.OBJECT)  return;  // must have prims
        if (dst == Wrapper.VOID   || src == Wrapper.VOID  )  return;  // must have values
        boolean testSingleCase = (tval != 0);
        final long tvalInit = tval;
        MethodHandle conv = ValueConversions.convertPrimitive(src, dst);
        MethodType convType = MethodType.methodType(dst.primitiveType(), src.primitiveType());
        assertEquals(convType, conv.type());
        MethodHandle converter = conv.asType(conv.type().changeReturnType(Object.class));
        for (;;) {
            long n = tval;
            Object testValue = src.wrap(n);
            Object expResult = dst.cast(testValue, dst.primitiveType());
            Object result;
            switch (src) {
                case INT:     result = converter.invokeExact((int)n); break;
                case LONG:    result = converter.invokeExact(/*long*/n); break;
                case FLOAT:   result = converter.invokeExact((float)n); break;
                case DOUBLE:  result = converter.invokeExact((double)n); break;
                case CHAR:    result = converter.invokeExact((char)n); break;
                case BYTE:    result = converter.invokeExact((byte)n); break;
                case SHORT:   result = converter.invokeExact((short)n); break;
                case BOOLEAN: result = converter.invokeExact((n & 1) != 0); break;
                default:  throw new AssertionError();
            }
            assertEquals("(src,dst,n,testValue)="+Arrays.asList(src,dst,"0x"+Long.toHexString(n),testValue),
                         expResult, result);
            if (testSingleCase)  break;
            // next test value:
            tval = nextTestValue(tval);
            if (tval == tvalInit)  break;  // repeat
        }
    }
    static long tweakSign(long x) {
        // Assuming that x is mostly zeroes, make those zeroes follow bit #62 (just below the sign).
        // This function is self-inverse.
        final long MID_SIGN_BIT = 62;
        long sign = -((x >>> MID_SIGN_BIT) & 1);  // all ones or all zeroes
        long flip = (sign >>> -MID_SIGN_BIT);  // apply the sign below the mid-bit
        return x ^ flip;
    }
    static long nextTestValue(long x) {
        // Produce 64 bits with three component bitfields:  [ high:3 | mid:58 | low:3 ].
        // The high and low fields vary through all possible bit patterns.
        // The middle field is either all zero or has a single bit set.
        // For better coverage of the neighborhood of zero, an internal sign bit is xored downward also.
        long ux = tweakSign(x);  // unsign the middle field
        final long LOW_BITS  = 3, LOW_BITS_MASK  = (1L << LOW_BITS)-1;
        final long HIGH_BITS = 3, HIGH_BITS_MASK = ~(-1L >>> HIGH_BITS);
        if ((ux & LOW_BITS_MASK) != LOW_BITS_MASK) {
            ++ux;
        } else {
            ux &= ~LOW_BITS_MASK;
            long midBit = (ux & ~HIGH_BITS_MASK);
            if (midBit == 0)
                midBit = (1L<<LOW_BITS);  // introduce a low bit
            ux += midBit;
        }
        return tweakSign(ux);
    }
}
