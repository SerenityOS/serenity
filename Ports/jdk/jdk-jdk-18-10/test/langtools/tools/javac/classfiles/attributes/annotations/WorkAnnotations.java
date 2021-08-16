/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
@Repeatable(RuntimeInvisibleRepeatableContainer.class)
@interface RuntimeInvisibleRepeatable {
    boolean booleanValue() default false;
    byte byteValue() default 0;
    char charValue() default 0;
    short shortValue() default 0;
    int intValue() default 0;
    long longValue() default 0;
    float floatValue() default 0;
    double doubleValue() default 0;
    String stringValue() default "";
    int[] arrayValue1() default {};
    String[] arrayValue2() default {};
    Class<?> classValue1() default void.class;
    Class<?> classValue2() default void.class;
    EnumValue enumValue() default EnumValue.VALUE1;
    AnnotationValue annoValue() default @AnnotationValue(stringValue = "StringValue");
    AnnotationValue[] annoArrayValue() default
            {@AnnotationValue(stringValue = "StringValue1"),
                    @AnnotationValue(stringValue = "StringValue2"),
                    @AnnotationValue(stringValue = "StringValue3")};
}

@Retention(RetentionPolicy.CLASS)
@interface RuntimeInvisibleRepeatableContainer {
    RuntimeInvisibleRepeatable[] value();
}

@interface RuntimeInvisibleNotRepeatable {
    boolean booleanValue() default false;
    byte byteValue() default 0;
    char charValue() default 0;
    short shortValue() default 0;
    int intValue() default 0;
    long longValue() default 0;
    float floatValue() default 0;
    double doubleValue() default 0;
    String stringValue() default "";
    int[] arrayValue1() default {};
    String[] arrayValue2() default {};
    Class<?> classValue1() default void.class;
    Class<?> classValue2() default void.class;
    EnumValue enumValue() default EnumValue.VALUE1;
    AnnotationValue annoValue() default @AnnotationValue(stringValue = "StringValue");
    AnnotationValue[] annoArrayValue() default
            {@AnnotationValue(stringValue = "StringValue1"),
                    @AnnotationValue(stringValue = "StringValue2"),
                    @AnnotationValue(stringValue = "StringValue3")};
}

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(RuntimeVisibleRepeatableContainer.class)
@interface RuntimeVisibleRepeatable {
    boolean booleanValue() default false;
    byte byteValue() default 0;
    char charValue() default 0;
    short shortValue() default 0;
    int intValue() default 0;
    long longValue() default 0;
    float floatValue() default 0;
    double doubleValue() default 0;
    String stringValue() default "";
    int[] arrayValue1() default {};
    String[] arrayValue2() default {};
    Class<?> classValue1() default void.class;
    Class<?> classValue2() default void.class;
    EnumValue enumValue() default EnumValue.VALUE1;
    AnnotationValue annoValue() default @AnnotationValue(stringValue = "StringValue");
    AnnotationValue[] annoArrayValue() default
            {@AnnotationValue(stringValue = "StringValue1"),
                    @AnnotationValue(stringValue = "StringValue2"),
                    @AnnotationValue(stringValue = "StringValue3")};
}

@Retention(RetentionPolicy.RUNTIME)
@interface RuntimeVisibleRepeatableContainer {
    RuntimeVisibleRepeatable[] value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface RuntimeVisibleNotRepeatable {
    boolean booleanValue() default false;
    byte byteValue() default 0;
    char charValue() default 0;
    short shortValue() default 0;
    int intValue() default 0;
    long longValue() default 0;
    float floatValue() default 0;
    double doubleValue() default 0;
    String stringValue() default "";
    int[] arrayValue1() default {};
    String[] arrayValue2() default {};
    Class<?> classValue1() default void.class;
    Class<?> classValue2() default void.class;
    EnumValue enumValue() default EnumValue.VALUE1;
    AnnotationValue annoValue() default @AnnotationValue(stringValue = "StringValue");
    AnnotationValue[] annoArrayValue() default
            {@AnnotationValue(stringValue = "StringValue1"),
                    @AnnotationValue(stringValue = "StringValue2"),
                    @AnnotationValue(stringValue = "StringValue3")};
}

enum EnumValue {
    VALUE1, VALUE2, VALUE3
}

@interface AnnotationValue {
    String stringValue() default "";
}