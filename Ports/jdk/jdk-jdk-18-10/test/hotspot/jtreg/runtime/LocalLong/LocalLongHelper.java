/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.lang.StackWalker.StackFrame;

public class LocalLongHelper {
    static StackWalker sw;
    static Method longValue;
    static Method getLocals;
    static Class<?> primitiveValueClass;
    static Method primitiveSize;
    static Method getMethodType;
    static Field memberName;
    static Field offset;

    public static void main(String[] args) throws Throwable {
        setupReflectionStatics();
        new LocalLongHelper().longArg(0xC0FFEE, 0x1234567890ABCDEFL);
    }

    // locals[2] contains the unused slot of the long argument.
    public long longArg(int i, long l) throws Throwable {
        List<StackFrame> frames = sw.walk(s -> s.collect(Collectors.toList()));
        Object[] locals = (Object[]) getLocals.invoke(frames.get(0));

        if (8 == (int) primitiveSize.invoke(locals[2])) { // Only test 64-bit
            long locals_2 = (long) longValue.invoke(locals[2]);
            if (locals_2 != 0){
                throw new RuntimeException("Expected locals_2 == 0");
            }
        }
        return l; // Don't want l to become a dead var
    }

    private static void setupReflectionStatics() throws Throwable {
        Class<?> liveStackFrameClass = Class.forName("java.lang.LiveStackFrame");
        primitiveValueClass = Class.forName("java.lang.LiveStackFrame$PrimitiveSlot");

        getLocals = liveStackFrameClass.getDeclaredMethod("getLocals");
        getLocals.setAccessible(true);

        longValue = primitiveValueClass.getDeclaredMethod("longValue");
        longValue.setAccessible(true);

        Class<?> stackFrameInfoClass = Class.forName("java.lang.StackFrameInfo");
        memberName = stackFrameInfoClass.getDeclaredField("memberName");
        memberName.setAccessible(true);
        offset = stackFrameInfoClass.getDeclaredField("bci");
        offset.setAccessible(true);
        getMethodType = Class.forName("java.lang.invoke.MemberName").getDeclaredMethod("getMethodType");
        getMethodType.setAccessible(true);

        Class<?> extendedOptionClass = Class.forName("java.lang.StackWalker$ExtendedOption");
        Method ewsNI = StackWalker.class.getDeclaredMethod("newInstance", Set.class, extendedOptionClass);
        ewsNI.setAccessible(true);
        Field f = extendedOptionClass.getDeclaredField("LOCALS_AND_OPERANDS");
        f.setAccessible(true);
        Object localsAndOperandsOption = f.get(null);

        primitiveSize = primitiveValueClass.getDeclaredMethod("size");
        primitiveSize.setAccessible(true);
        sw = (StackWalker) ewsNI.invoke(null, java.util.Collections.emptySet(), localsAndOperandsOption);
    }
}
