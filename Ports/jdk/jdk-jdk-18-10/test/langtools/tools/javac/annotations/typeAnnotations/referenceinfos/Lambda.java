/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8008077 8029721 8042451 8043974
 * @summary Test population of reference info for lambda expressions
 *          javac crash for annotated parameter type of lambda in a field
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @ignore 8057687 emit correct byte code an attributes for type annotations
 * @compile -g Driver.java ReferenceInfoUtil.java Lambda.java
 * @run main Driver Lambda
 * @author Werner Dietl
 */

import static com.sun.tools.classfile.TypeAnnotation.TargetType.*;

public class Lambda {

    @TADescription(annotation = "TA", type = METHOD_REFERENCE,
                offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TB", type = METHOD_REFERENCE,
                offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String returnMethodRef1() {
        return
                "class Lambda {" +
                "  public String getName() { return \"Lambda!\"; }" +
                "}" +

                "class %TEST_CLASS_NAME% {" +
                "  java.util.function.Function<Lambda, String> lambda() {" +
                "    return @TA @TB Lambda::getName;" +
                "  }" +
                "}";
    }

    @TADescription(annotation = "TA", type = METHOD_REFERENCE,
                offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TB", type = METHOD_REFERENCE,
                offset = ReferenceInfoUtil.IGNORE_VALUE,
                genericLocation = { 3, 0 })
    @TADescription(annotation = "TC", type = METHOD_REFERENCE,
                offset = ReferenceInfoUtil.IGNORE_VALUE,
                genericLocation = { 3, 0 })
    @TADescription(annotation = "TD", type = METHOD_REFERENCE,
                offset = ReferenceInfoUtil.IGNORE_VALUE,
                genericLocation = { 3, 1 })
    @TADescription(annotation = "TE", type = METHOD_REFERENCE,
                offset = ReferenceInfoUtil.IGNORE_VALUE,
                genericLocation = { 3, 1})
    public String returnMethodRef2() {
        return
                "class Lambda<S, T> {" +
                "  public String getName() { return \"Lambda!\"; }" +
                "}" +

                "class %TEST_CLASS_NAME% {" +
                "  java.util.function.Function<Lambda<Integer, Float>, String> lambda() {" +
                "    return @TA Lambda<@TB @TC Integer, @TD @TE Float>::getName;" +
                "  }" +
                "}";
    }

    @TADescription(annotation = "CTA", type = METHOD_REFERENCE,
                offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "CTB", type = METHOD_REFERENCE,
                offset = ReferenceInfoUtil.IGNORE_VALUE,
                genericLocation = { 3, 0 })
    @TADescription(annotation = "CTC", type = METHOD_REFERENCE,
                offset = ReferenceInfoUtil.IGNORE_VALUE,
                genericLocation = { 3, 1    })
    public String returnMethodRef3() {
        return
                "class Lambda<S, T> {" +
                "  public String getName() { return \"Lambda!\"; }" +
                "}" +

                "@Target(ElementType.TYPE_USE)" +
                "@interface CTA {" +
                "  String value();" +
                "}" +

                "@Target(ElementType.TYPE_USE)" +
                "@interface CTB {" +
                "  int age();" +
                "}" +

                "@Target(ElementType.TYPE_USE)" +
                "@interface CTC {" +
                "  String name();" +
                "}" +

                "class %TEST_CLASS_NAME% {" +
                "  java.util.function.Function<Lambda<Integer, Float>, String> lambda() {" +
                "    return @CTA(\"x\") Lambda<@CTB(age = 5) Integer, @CTC(name = \"y\") Float>::getName;" +
                "  }" +
                "}";
    }


    @TADescription(annotation = "TA", type = CONSTRUCTOR_REFERENCE,
                offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TB", type = CONSTRUCTOR_REFERENCE,
                offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String returnConstructorRef1() {
        return
                "class Lambda {" +
                "  Lambda() { }" +
                "}" +

                "class %TEST_CLASS_NAME% {" +
                "  Runnable lambda() {" +
                "    return @TA @TB Lambda::new;" +
                "  }" +
                "}";
    }

    @TADescription(annotation = "TA", type = CONSTRUCTOR_REFERENCE,
                offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TB", type = CONSTRUCTOR_REFERENCE,
                offset = ReferenceInfoUtil.IGNORE_VALUE,
                genericLocation = { 3, 0 })
    @TADescription(annotation = "TC", type = CONSTRUCTOR_REFERENCE,
                offset = ReferenceInfoUtil.IGNORE_VALUE,
                genericLocation = { 3, 0 })
    @TADescription(annotation = "TD", type = CONSTRUCTOR_REFERENCE,
                offset = ReferenceInfoUtil.IGNORE_VALUE,
                genericLocation = { 3, 1 })
    @TADescription(annotation = "TE", type = CONSTRUCTOR_REFERENCE,
                offset = ReferenceInfoUtil.IGNORE_VALUE,
                genericLocation = { 3, 1    })
    public String returnConstructorRef2() {
        return
                "class Lambda<S, T> {" +
                "  Lambda() { }" +
                "}" +

                "class %TEST_CLASS_NAME% {" +
                "  Runnable lambda() {" +
                "    return @TA Lambda<@TB @TC Integer, @TD @TE Float>::new;" +
                "  }" +
                "}";
    }

    @TADescription(annotation = "CTA", type = CONSTRUCTOR_REFERENCE,
                offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "CTB", type = CONSTRUCTOR_REFERENCE,
                offset = ReferenceInfoUtil.IGNORE_VALUE,
                genericLocation = { 3, 0 })
    @TADescription(annotation = "CTC", type = CONSTRUCTOR_REFERENCE,
                offset = ReferenceInfoUtil.IGNORE_VALUE,
                genericLocation = { 3, 1    })
    public String returnConstructorRef3() {
        return
                "class Lambda<S, T> {" +
                "  Lambda() { }" +
                "}" +

                "@Target(ElementType.TYPE_USE)" +
                "@interface CTA {" +
                "  String value();" +
                "}" +

                "@Target(ElementType.TYPE_USE)" +
                "@interface CTB {" +
                "  int age();" +
                "}" +

                "@Target(ElementType.TYPE_USE)" +
                "@interface CTC {" +
                "  String name();" +
                "}" +

                "class %TEST_CLASS_NAME% {" +
                "  Runnable lambda() {" +
                "    return @CTA(\"x\") Lambda<@CTB(age = 5) Integer, @CTC(name = \"y\") Float>::new;" +
                "  }" +
                "}";
    }


    @TADescription(annotation = "TA", type = METHOD_REFERENCE_TYPE_ARGUMENT,
                 offset = ReferenceInfoUtil.IGNORE_VALUE,
                 typeIndex = 0)
    @TADescription(annotation = "TB", type = METHOD_REFERENCE_TYPE_ARGUMENT,
                 offset = ReferenceInfoUtil.IGNORE_VALUE,
                 typeIndex = 1)
    public String returnMethodRefTA1() {
        return
                "interface Lambda {" +
                "  <S, T> void generic(S p1, T p2);" +
                "}" +

                "class LambdaImpl implements Lambda {" +
                "  public <S, T> void generic(S p1, T p2) {}" +
                "}" +

                "class %TEST_CLASS_NAME% {" +
                "  Lambda lambda(LambdaImpl r) {" +
                "    return r::<@TA Object, @TB Object>generic;" +
                "  }" +
                "}";
    }

    @TADescription(annotation = "TA", type = CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT,
                 offset = ReferenceInfoUtil.IGNORE_VALUE,
                 typeIndex = 0)
    @TADescription(annotation = "TB", type = CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT,
                 offset = ReferenceInfoUtil.IGNORE_VALUE,
                 typeIndex = 1)
    public String returnConstructorRefTA2() {
        return
                "interface Lambda {" +
                "  <S, T> void generic(S p1, T p2);" +
                "}" +

                "class LambdaImpl implements Lambda {" +
                "  <S, T> LambdaImpl(S p1, T p2) {}" +
                "  public <S, T> void generic(S p1, T p2) {}" +
                "}" +

                "class %TEST_CLASS_NAME% {" +
                "  Lambda lambda() {" +
                "    return LambdaImpl::<@TA Object, @TB Object>new;" +
                "  }" +
                "}";
    }

    @TADescription(annotation = "TA", type = METHOD_FORMAL_PARAMETER,
                paramIndex = 0)
    @TADescription(annotation = "TB", type = METHOD_FORMAL_PARAMETER,
                paramIndex = 1)
    @TADescription(annotation = "TC", type = METHOD_FORMAL_PARAMETER,
                paramIndex = 1, genericLocation = { 3, 0 })
    @TADescription(annotation = "TD", type = LOCAL_VARIABLE,
                lvarOffset = ReferenceInfoUtil.IGNORE_VALUE,
                lvarLength = ReferenceInfoUtil.IGNORE_VALUE,
                lvarIndex = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TE", type = CAST,
                offset = ReferenceInfoUtil.IGNORE_VALUE,
                typeIndex = 0)
    public String returnLambdaExpr1() {
        return
                "interface LambdaInt {" +
                "  void lambda(Object p1, List<Object> p2);" +
                "}" +
                "class %TEST_CLASS_NAME% {" +
                "  LambdaInt getLambda() {" +
                "    return (@TA Object x, @TB List<@TC Object> y) -> { @TD Object l = null; System.out.println((@TE Object) l); };" +
                "  }" +
                "}";
    }

    @TADescription(annotation = "TA", type = METHOD_FORMAL_PARAMETER,
            paramIndex = 0)
    public String lambdaField1() {
        return
            "class %TEST_CLASS_NAME% {" +
                " java.util.function.IntUnaryOperator field = (@TA int y) -> 1;" +
            "}";
    }

    @TADescription(annotation = "TA", type = METHOD_FORMAL_PARAMETER,
            paramIndex = 0)
    public String lambdaField2() {
        return
            "class %TEST_CLASS_NAME% {" +
                " static java.util.function.IntUnaryOperator field = (@TA int y) -> 1;" +
            "}";
    }

    @TADescription(annotation = "RTAs", type = METHOD_REFERENCE,
            offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String returnMethodRefRepeatableAnnotation1() {
        return
                "class Lambda {" +
                        "  public String getName() { return \"Lambda!\"; }" +
                        "}" +

                        "class %TEST_CLASS_NAME% {" +
                        "  java.util.function.Function<Lambda, String> lambda() {" +
                        "    return @RTA @RTA Lambda::getName;" +
                        "  }" +
                        "}";
    }

    @TADescription(annotation = "RTAs", type = METHOD_REFERENCE,
            offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTBs", type = METHOD_REFERENCE,
            offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = { 3, 0 })
    @TADescription(annotation = "RTCs", type = METHOD_REFERENCE,
            offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = { 3, 0 })
    @TADescription(annotation = "RTDs", type = METHOD_REFERENCE,
            offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = { 3, 1 })
    @TADescription(annotation = "RTEs", type = METHOD_REFERENCE,
            offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = { 3, 1})
    public String returnMethodRefRepeatableAnnotation2() {
        return
                "class Lambda<S, T> {" +
                        "  public String getName() { return \"Lambda!\"; }" +
                        "}" +

                        "class %TEST_CLASS_NAME% {" +
                        "  java.util.function.Function<Lambda<Integer, Float>, String> lambda() {" +
                        "    return @RTA @RTA Lambda<@RTB @RTB @RTC @RTC Integer, @RTD @RTD @RTE @RTE Float>::getName;" +
                        "  }" +
                        "}";
    }

    @TADescription(annotation = "RTAs", type = CONSTRUCTOR_REFERENCE,
            offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String returnConstructorRefRepeatable1() {
        return
                "class Lambda {" +
                        "  Lambda() { }" +
                        "}" +

                        "class %TEST_CLASS_NAME% {" +
                        "  Runnable lambda() {" +
                        "    return @RTA @RTA Lambda::new;" +
                        "  }" +
                        "}";
    }

    @TADescription(annotation = "RTAs", type = CONSTRUCTOR_REFERENCE,
            offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTBs", type = CONSTRUCTOR_REFERENCE,
            offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = { 3, 0 })
    @TADescription(annotation = "RTCs", type = CONSTRUCTOR_REFERENCE,
            offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = { 3, 1    })
    public String returnConstructorRefRepeatable2() {
        return
                "class Lambda<S, T> {" +
                        "  Lambda() { }" +
                        "}" +

                        "class %TEST_CLASS_NAME% {" +
                        "  Runnable lambda() {" +
                        "    return @RTA @RTA Lambda<@RTB @RTB Integer, @RTC @RTC Float>::new;" +
                        "  }" +
                        "}";
    }

    @TADescription(annotation = "RTAs", type = METHOD_REFERENCE_TYPE_ARGUMENT,
            offset = ReferenceInfoUtil.IGNORE_VALUE,
            typeIndex = 0)
    @TADescription(annotation = "RTBs", type = METHOD_REFERENCE_TYPE_ARGUMENT,
            offset = ReferenceInfoUtil.IGNORE_VALUE,
            typeIndex = 1)
    public String returnMethodRefTARepeatableAnnotation1() {
        return
                "interface Lambda {" +
                        "  <S, T> void generic(S p1, T p2);" +
                        "}" +

                        "class LambdaImpl implements Lambda {" +
                        "  public <S, T> void generic(S p1, T p2) {}" +
                        "}" +

                        "class %TEST_CLASS_NAME% {" +
                        "  Lambda lambda(LambdaImpl r) {" +
                        "    return r::<@RTA @RTA Object, @RTB @RTB Object>generic;" +
                        "  }" +
                        "}";
    }

    @TADescription(annotation = "RTAs", type = CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT,
            offset = ReferenceInfoUtil.IGNORE_VALUE,
            typeIndex = 0)
    @TADescription(annotation = "RTBs", type = CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT,
            offset = ReferenceInfoUtil.IGNORE_VALUE,
            typeIndex = 1)
    public String returnConstructorRefTARepeatableAnnotation2() {
        return
                "interface Lambda {" +
                        "  <S, T> void generic(S p1, T p2);" +
                        "}" +

                        "class LambdaImpl implements Lambda {" +
                        "  <S, T> LambdaImpl(S p1, T p2) {}" +
                        "  public <S, T> void generic(S p1, T p2) {}" +
                        "}" +

                        "class %TEST_CLASS_NAME% {" +
                        "  Lambda lambda() {" +
                        "    return LambdaImpl::<@RTA @RTA Object, @RTB @RTB Object>new;" +
                        "  }" +
                        "}";
    }

    @TADescription(annotation = "RTAs", type = METHOD_FORMAL_PARAMETER,
            paramIndex = 0)
    @TADescription(annotation = "RTBs", type = METHOD_FORMAL_PARAMETER,
            paramIndex = 1)
    @TADescription(annotation = "RTCs", type = METHOD_FORMAL_PARAMETER,
            paramIndex = 1, genericLocation = { 3, 0 })
    @TADescription(annotation = "RTDs", type = LOCAL_VARIABLE,
            lvarOffset = ReferenceInfoUtil.IGNORE_VALUE,
            lvarLength = ReferenceInfoUtil.IGNORE_VALUE,
            lvarIndex = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTEs", type = CAST,
            offset = ReferenceInfoUtil.IGNORE_VALUE,
            typeIndex = 0)
    public String returnLambdaExprRepeatableAnnotation1() {
        return
                "interface LambdaInt {" +
                        "  void lambda(Object p1, List<Object> p2);" +
                        "}" +
                        "class %TEST_CLASS_NAME% {" +
                        "  LambdaInt getLambda() {" +
                        "    return (@RTA @RTA Object x, @RTB @RTB List<@RTC @RTC Object> y) ->" +
                        " { @RTD @RTD Object l = null; System.out.println((@RTE @RTE Object) l); };" +
                        "  }" +
                        "}";
    }

    @TADescription(annotation = "RTAs", type = METHOD_FORMAL_PARAMETER,
            paramIndex = 0)
    public String lambdaFieldRepeatableAnnotation1() {
        return
                "class %TEST_CLASS_NAME% {" +
                        " java.util.function.IntUnaryOperator field = (@RTA @RTA int y) -> 1;" +
                        "}";
    }

    @TADescription(annotation = "RTAs", type = METHOD_FORMAL_PARAMETER,
            paramIndex = 0)
    public String lambdaFieldRepeatableAnnotation2() {
        return
                "class %TEST_CLASS_NAME% {" +
                        " static java.util.function.IntUnaryOperator field = (@RTA @RTA int y) -> 1;" +
                        "}";
    }
}
