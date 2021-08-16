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

/*
 * @test
 * @summary Test SignatureParser
 * @author Jean-Francois Denise
 * @modules java.base/jdk.internal.jimage.decompressor
 * @run main SignatureParserTest
 */

import java.util.Arrays;
import java.util.Objects;

import jdk.internal.jimage.decompressor.SignatureParser;

public class SignatureParserTest {

    private int passed = 0;
    private int failed = 0;

    public static void main(String[] args) {
        new SignatureParserTest().test();
    }

    private void test() {
        test("[Ljava/lang/String;", "[L;", "java/lang/String");
        test("[[[[[[[[[[Ljava/lang/String;", "[[[[[[[[[[L;", "java/lang/String");
        test("<T:Ljava/lang/Object;:Ljava/lang/Comparable<-TT;>;>" +
                        "(Ljava/lang/String;Ljava/lang/Class<TT;>;TT;Ljava/lang/Comparable<-TT;>;" +
                        "Ljava/lang/Comparable<-TT;>;ZZ)V",
                "<T:L;:L<-TT;>;>(L;L<TT;>;TT;L<-TT;>;L<-TT;>;ZZ)V",
                "java/lang/Object", "java/lang/Comparable", "java/lang/String",
                "java/lang/Class", "java/lang/Comparable", "java/lang/Comparable");
        test("(Ljava/lang/String;ZLjava/util/EventListener;TTK;)V",
                "(L;ZL;TTK;)V",
                "java/lang/String", "java/util/EventListener");
        test("<Y:Ljava/lang/String;>", "<Y:L;>", "java/lang/String");
        test("<Y:Ljava/lang/String;Z::Ljava/util/EventListener;>",
                "<Y:L;Z::L;>", "java/lang/String",
                "java/util/EventListener");
        test("<Y:Ljava/lang/String;Z::Ljava/util/EventListener;O::Ljava/lang/Comparable<Ljava/lang/String;>;>",
                "<Y:L;Z::L;O::L<L;>;>",
                "java/lang/String", "java/util/EventListener", "java/lang/Comparable", "java/lang/String");
        test("<Y:Ljava/lang/String;O::Ljava/lang/Comparable<Ljava/lang/String;Ljava/lang/Float;>;>",
                "<Y:L;O::L<L;L;>;>",
                "java/lang/String", "java/lang/Comparable", "java/lang/String", "java/lang/Float");
        test("<Y:Ljava/lang/String;O::Ljava/lang/Comparable<Ljava/lang/String;Ljava/lang/Float<Ljava/lang/Object;>;>;>",
                "<Y:L;O::L<L;L<L;>;>;>",
                "java/lang/String", "java/lang/Comparable", "java/lang/String", "java/lang/Float", "java/lang/Object");
        test("Ljava/util/Set;", "L;", "java/util/Set");
        test("Ljavaapplication20/Titi<[Ljava/lang/String;Ljava/lang/Integer;>;", "L<[L;L;>;",
                "javaapplication20/Titi",
                "java/lang/String", "java/lang/Integer");
        test("Ljava/lang/Comparable<TK;>;", "L<TK;>;", "java/lang/Comparable");
        test("Ljava/io/Serializable;Ljava/lang/Comparable<TK;>;", "L;L<TK;>;",
                "java/io/Serializable", "java/lang/Comparable");
        test("<Y:Ljava/lang/String;Z::Ljava/util/EventListener;K::Ljava/util/EventListener;O::"
                + "Ljava/lang/Comparable<Ljava/lang/String;>;>"
                + "Ljavaapplication20/Titi<[Ljava/lang/String;Ljava/lang/Integer;TZ;>;"
                + "Ljava/io/Serializable;Ljava/lang/Comparable<TK;>;",
                "<Y:L;Z::L;K::L;O::L<L;>;>L<[L;L;TZ;>;L;L<TK;>;",
                "java/lang/String", "java/util/EventListener", "java/util/EventListener", "java/lang/Comparable",
                "java/lang/String", "javaapplication20/Titi", "java/lang/String", "java/lang/Integer",
                "java/io/Serializable", "java/lang/Comparable");
        test("<PO:Ljava/lang/Object;>(Ljava/lang/Integer;TPO;)Ljava/lang/Integer;",
                "<PO:L;>(L;TPO;)L;",
                "java/lang/Object", "java/lang/Integer", "java/lang/Integer");
        test("<PO:Ljava/lang/Object;>(Ljava/lang/Integer;TPO;)TPO;", "<PO:L;>(L;TPO;)TPO;",
                "java/lang/Object", "java/lang/Integer");
        test("<T::Ljava/util/EventListener;>(Ljava/lang/Class<TT;>;)[TT;",
                "<T::L;>(L<TT;>;)[TT;",
                "java/util/EventListener", "java/lang/Class");
        test("<PO:LTiti;>(Ljava/lang/Integer;ITPO;)Z", "<PO:L;>(L;ITPO;)Z",
                "Titi", "java/lang/Integer");
        test("<K:Ljava/lang/Object;V:Ljava/lang/Object;>Ljava/lang/Object;",
                "<K:L;V:L;>L;",
                "java/lang/Object", "java/lang/Object", "java/lang/Object");
        test("Ljava/util/LinkedHashMap<TK;TV;>.LinkedHashIterator;Ljava/util/Iterator<TV;>;",
                "L<TK;TV;>.L;L<TV;>;",
                "java/util/LinkedHashMap",
                "inkedHashIterator",
                "java/util/Iterator");
        test("LToto<Ljava/lang/String;>;", "L<L;>;", "Toto",
                "java/lang/String");
        test("Ljavaapplication20/Titi<[Ljava/lang/String;Ljava/lang/Integer<LToto;>;TZ;>;",
                "L<[L;L<L;>;TZ;>;",
                "javaapplication20/Titi", "java/lang/String", "java/lang/Integer", "Toto");
        test("LX<[LQ;LW<LToto;>;TZ;>;", "L<[L;L<L;>;TZ;>;",
                "X", "Q", "W", "Toto");
        test("Ljava/lang/String<*>;", "L<*>;", "java/lang/String");
        test("Ljava/util/List<[B>;", "L<[B>;", "java/util/List");
        test("<T:Ljava/lang/Object;T_NODE::Ljava/util/stream/Node<TT;>;>Ljava/lang/Object;Ljava/util/stream/Node<TT;>;",
                "<T:L;T_NODE::L<TT;>;>L;L<TT;>;",
                "java/lang/Object", "java/util/stream/Node", "java/lang/Object", "java/util/stream/Node");
        test("Ljavaapplication20/Titi<[Ljava/lang/String;>;", "L<[L;>;",
                "javaapplication20/Titi", "java/lang/String");
        test("<A::Ljava/lang/annotation/Annotation;"
                        + "W::Lcom/sun/codemodel/internal/JAnnotationWriter<TA;>;>"
                        + "Ljava/lang/Object;Ljava/lang/reflect/InvocationHandler;"
                        + "Lcom/sun/codemodel/internal/JAnnotationWriter<TA;>;",
                "<A::L;W::L<TA;>;>L;L;L<TA;>;",
                "java/lang/annotation/Annotation", "com/sun/codemodel/internal/JAnnotationWriter",
                "java/lang/Object", "java/lang/reflect/InvocationHandler", "com/sun/codemodel/internal/JAnnotationWriter");
        test("<W::Lcom/sun/codemodel/internal/JAnnotationWriter<*>;>(Ljava/lang/Class<TW;>;" +
                "Lcom/sun/codemodel/internal/JAnnotatable;)TW;",
                "<W::L<*>;>(L<TW;>;L;)TW;",
                "com/sun/codemodel/internal/JAnnotationWriter", "java/lang/Class", "com/sun/codemodel/internal/JAnnotatable");
        test("Ljava/util/Set<Lcom/sun/tools/jdeps/JdepsTask$DotGraph<TT;>.Edge;>;",
                "L<L<TT;>.Edge;>;",
                "java/util/Set",
                "com/sun/tools/jdeps/JdepsTask$DotGraph");
        test("<E::Lcom/sun/xml/internal/rngom/ast/om/ParsedElementAnnotation;" +
                "L::Lcom/sun/xml/internal/rngom/ast/om/Location;" +
                "CL::Lcom/sun/xml/internal/rngom/ast/builder/CommentList<TL;>;>Ljava/lang/Object;",
                "<E::L;L::L;CL::L<TL;>;>L;",
                "com/sun/xml/internal/rngom/ast/om/ParsedElementAnnotation",
                "",
                "com/sun/xml/internal/rngom/ast/om/Location",
                "",
                "com/sun/xml/internal/rngom/ast/builder/CommentList",
                "",
                "java/lang/Object");
        test("(Ljava/util/List<Lcom/sun/xml/internal/rngom/nc/NameClass;>;TL;TA;)" +
                "Lcom/sun/xml/internal/rngom/nc/NameClass;",
                "(L<L;>;TL;TA;)L;",
                "java/util/List",
                "com/sun/xml/internal/rngom/nc/NameClass",
                "",
                "com/sun/xml/internal/rngom/nc/NameClass");
        test("[Ljava/util/List;", "[L;", "java/util/List");
        test("[Ljava/util/List<+Lcom/sun/jdi/request/EventRequest;>;",
                "[L<+L;>;",
                "java/util/List", "com/sun/jdi/request/EventRequest");
        test("Lcom/sun/xml/internal/bind/v2/util/QNameMap<TV;>.HashIterator" +
                "<Lcom/sun/xml/internal/bind/v2/util/QNameMap$Entry<TV;>;>;",
                "L<TV;>.HashIterator<L<TV;>;>;",
                "com/sun/xml/internal/bind/v2/util/QNameMap", "com/sun/xml/internal/bind/v2/util/QNameMap$Entry");
        test("[Ljava/lang/String;", "[L;", "java/lang/String");
        test("[Ljava/lang/String<Ljava/lang/Toto<Ljava/lang/Titi;>;>;",
                "[L<L<L;>;>;",
                "java/lang/String", "java/lang/Toto", "java/lang/Titi");
        test("<T::Ljava/util/EventListener;K:Ljava/util/BOO;>(ZCLjava/lang/Class<TT;>;IJS)[TT;",
                "<T::L;K:L;>(ZCL<TT;>;IJS)[TT;",
                "java/util/EventListener", "java/util/BOO", "java/lang/Class");
        test("<T:Ljava/lang/Object;>(TT;ILjava/lang/Long;)TT;",
                "<T:L;>(TT;IL;)TT;", "java/lang/Object", "java/lang/Long");
        test("<T:Ljava/lang/Object;>(TT;ILjava/lang/Long;)TT;^TT;",
                "<T:L;>(TT;IL;)TT;^TT;", "java/lang/Object", "java/lang/Long");
        test("<T:Ljava/lang/Object;>(TT;ILjava/lang/Long;)TT;^TT;^Ljava/lang/Exception;",
                "<T:L;>(TT;IL;)TT;^TT;^L;",
                "java/lang/Object", "java/lang/Long", "java/lang/Exception");
        if (passed + failed == 0) {
            throw new AssertionError("No tests were run");
        }
        String message = String.format("Passed: %d, failed: %d, total: %d", passed, failed, passed + failed);
        if (failed > 0) {
            throw new AssertionError("Test failed: " + message);
        } else {
            System.err.println(message);
        }
    }

    private void test(String type, String formatted, String...classNames) {
        try {
            SignatureParser.ParseResult result = SignatureParser.parseSignatureDescriptor(type);
            String[] parsedNames = parse(classNames);
            assertEquals(result.formatted, formatted, "Input: '" + type + "', checking 'formatted'");
            assertEquals(result.types.size(), 2 * classNames.length,
                    "Input: '" + type + "', checking the length of 'types':" +
                            "\nexpected: " + Arrays.toString(parsedNames) +
                            "\n     got: " + result.types);
            for (int i = 0; i < result.types.size(); ++i) {
                assertEquals(result.types.get(i), parsedNames[i],
                        "Input: '" + type + "', checking 'packageName' at index " + i / 2);
                ++i;
                assertEquals(result.types.get(i), parsedNames[i],
                        "Input: '" + type + "', checking 'simpleName' at index " + i / 2);
            }
            String reconstructed = SignatureParser.reconstruct(result.formatted, result.types);
            assertEquals(reconstructed, type, "Input: '" + type + "', checking reconstruction from: "
                    + result.formatted + " " + result.types);
            ++passed;
        } catch (Exception | AssertionError e) {
            e.printStackTrace();
            ++failed;
        }
    }

    private void assertEquals(Object actual, Object expected, String message) {
        if (!Objects.equals(actual, expected)) {
            throw new AssertionError(message + ": expected: " + expected + ", actual: " + actual);
        }
    }

    private String[] parse(String[] classNames) {
        String[] result = new String[2 * classNames.length];
        for (int i = 0; i < classNames.length; ++i) {
            int index = classNames[i].lastIndexOf("/");
            result[2 * i] = index == -1 ? "" : classNames[i].substring(0, index);
            result[2 *i + 1] = classNames[i].substring(index + 1);
        }
        return result;
    }
}
