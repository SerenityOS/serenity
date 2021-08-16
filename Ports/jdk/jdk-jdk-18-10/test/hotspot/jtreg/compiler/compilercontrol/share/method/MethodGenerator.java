/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package compiler.compilercontrol.share.method;

import compiler.compilercontrol.share.method.MethodDescriptor.PatternType;
import compiler.compilercontrol.share.method.MethodDescriptor.Separator;
import compiler.compilercontrol.share.pool.PoolHelper;
import jdk.test.lib.util.Pair;
import jdk.test.lib.util.Triple;
import jdk.test.lib.Utils;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Generates combinations of method descriptors from the pool of methods
 */
public class MethodGenerator {
    private static final List<Pair<Executable, Callable<?>>> METHODS =
            new PoolHelper().getAllMethods(PoolHelper.METHOD_FILTER);
    // Different combinations of patterns
    private static final List<Combination<PatternType>> PATTERNS_LIST;
    // Different combinations of separators
    private static final List<Combination<Separator>> SEPARATORS_LIST;
    // List of functions that modify elements
    private static final List<Function<String, String>> ELEMENT_MUTATORS;

    static {
        PATTERNS_LIST =
                generate(EnumSet.allOf(PatternType.class),
                        EnumSet.allOf(PatternType.class),
                        EnumSet.of(PatternType.ANY, PatternType.EXACT));
        SEPARATORS_LIST =
                generate(EnumSet.of(Separator.SLASH, Separator.DOT),
                        EnumSet.complementOf(EnumSet.of(Separator.NONE)),
                        EnumSet.of(Separator.COMMA, Separator.SPACE,
                                Separator.NONE));
        ELEMENT_MUTATORS = generateMutators();
    }

    // Test method
    public static void main(String[] args) {
        MethodGenerator methodGenerator = new MethodGenerator();
        List<MethodDescriptor> tests = methodGenerator.getTests();
        tests.forEach(System.out::println);
    }

    /**
     * Generates random method descriptor
     *
     * @param executable executable used to generate descriptor
     * @return MethodDescriptor instance
     */
    public MethodDescriptor generateRandomDescriptor(Executable executable) {
        Combination<PatternType> patterns =
                Utils.getRandomElement(PATTERNS_LIST);
        Combination<Separator> separators =
                Utils.getRandomElement(SEPARATORS_LIST);
        // Create simple mutators for signature generation
        List<Function<String, String>> signMutators = new ArrayList<>();
        signMutators.add(input -> input);
        signMutators.add(input -> "");
        Combination<Function<String, String>> mutators = new Combination<>(
                Utils.getRandomElement(ELEMENT_MUTATORS),
                Utils.getRandomElement(ELEMENT_MUTATORS),
                // use only this type of mutators
                Utils.getRandomElement(signMutators));
        return makeMethodDescriptor(executable, patterns,
                separators, mutators);
    }

    /**
     * Compile command signature that looks like java/lang/String.indexOf
     * http://docs.oracle.com/javase/8/docs/technotes/tools/unix/java.html#BABDDFII
     *
     * @param executable executable used to generate descriptor
     * @return MethodDescriptor instance
     */
    public static MethodDescriptor commandDescriptor(Executable executable) {
        MethodDescriptor md = new MethodDescriptor(executable);
        md.aClass.setSeparator(Separator.SLASH);
        md.aMethod.setSeparator(Separator.DOT);
        md.aSignature.setSeparator(Separator.NONE);
        return md;
    }

    /**
     * Compile command signature that looks like java.lang.String::indexOf
     *
     * @param executable executable used to generate descriptor
     * @return MethodDescriptor instance
     */
    public static MethodDescriptor logDescriptor(Executable executable) {
        MethodDescriptor md = new MethodDescriptor(executable);
        md.aClass.setSeparator(Separator.DOT);
        md.aMethod.setSeparator(Separator.DOUBLECOLON);
        md.aSignature.setSeparator(Separator.NONE);
        return md;
    }

    /**
     * Method descriptor that matches any method. Its full signature is *.*
     *
     * @param executable executable used to generate descriptor
     * @return MethodDescriptor instance
     */
    public static MethodDescriptor anyMatchDescriptor(Executable executable) {
        MethodDescriptor md = new MethodDescriptor(executable);
        Combination<PatternType> patterns = new Combination<>(PatternType.ANY,
                PatternType.ANY, PatternType.ANY);
        md.aClass.setSeparator(Separator.SLASH);
        md.aMethod.setSeparator(Separator.DOT);
        md.aSignature.setSeparator(Separator.NONE);
        md.setPatterns(patterns);
        return md;
    }

    /**
     * Generates a list of method patterns from the pool of methods
     *
     * @return a list of test cases
     */
    public List<MethodDescriptor> getTests() {
        List<MethodDescriptor> list = new ArrayList<>();
        METHODS.forEach(pair -> list.addAll(getTests(pair.first)));
        return list;
    }

    /**
     * Generates all combinations of method descriptors for a given executable
     *
     * @param executable executable for which the different combination is built
     * @return list of method descriptors
     */
    public List<MethodDescriptor> getTests(Executable executable) {
        List<MethodDescriptor> list = new ArrayList<>();
        for (Combination<PatternType> patterns : PATTERNS_LIST) {
            for (Combination<Separator> separators : SEPARATORS_LIST) {
                for (Function<String, String> classGen : ELEMENT_MUTATORS) {
                    for (Function<String, String> methodGen :
                            ELEMENT_MUTATORS) {
                        for (Function<String, String> signatureGen :
                                ELEMENT_MUTATORS) {
                            list.add(makeMethodDescriptor(executable,
                                    patterns, separators,
                                    new Combination<>(classGen, methodGen,
                                        signatureGen)));
                        }
                    }
                }
            }
        }
        return list;
    }

    /**
     * Creates method descriptor from the given executable,
     * patterns and separators for its elements
     */
    private MethodDescriptor makeMethodDescriptor(
            Executable executable,
            Combination<PatternType> patterns,
            Combination<Separator> separators,
            Combination<Function<String, String>> mutators) {
        MethodDescriptor methodDescriptor = new MethodDescriptor(executable);
        methodDescriptor.setSeparators(separators);
        methodDescriptor.applyMutates(mutators);
        methodDescriptor.setPatterns(patterns);
        return methodDescriptor;
    }

    /**
     * Creates a list of functions that change given string
     */
    private static List<Function<String, String>> generateMutators() {
        List<Function<String, String>> elements = new ArrayList<>();
        // Use the input itself
        elements.add(input -> input);
        // Use half of the input string
        elements.add(input -> input.substring(input.length() / 2));
        // Add nonexistent element
        elements.add(input -> "nonexistent");
        // Use left and right angle brackets
        elements.add(input -> "<" + input + ">");
        // Embed * inside
        elements.add(input -> embed(input, "*"));
        // ** as a whole element
        elements.add(input -> "**");
        // Embed JLS-invalid letters
        elements.add(input -> embed(input, "@%"));
        elements.add(input -> embed(input, "]"));
        // Use JLS-invalid letters
        elements.add(input -> "-");
        elements.add(input -> "+");
        elements.add(input -> ")" + input);
        elements.add(input -> "{" + input + "}");
        // Add valid Java identifier start char
        elements.add(input -> "_" + input);
        elements.add(input -> "$" + input);
        elements.add(input -> "0" + input);

        /* TODO: uncomment this together with the fix for 8140631
        // Unicode characters
        elements.add(input -> embed(input, "\u0001"));
        elements.add(input -> embed(input, "\u007F"));
        // Combining character
        elements.add(input -> embed(input, "\u0300"));
        elements.add(input -> embed(input, "\u0306"));
        // Supplementary character
        elements.add(input -> new String(Character.toChars(0x1F64C)));
        */
        return elements;
    }

    /**
     * Embeds one string inside another one
     *
     * @param target  target source string
     * @param element string to be embedded into target string
     * @return result string
     */
    private static String embed(String target, String element) {
        int mid = target.length() / 2;
        String begin = target.substring(0, mid);
        String end = target.substring(mid);
        return begin + element + end;
    }

    /**
     * Generates triples from the given enum sets
     * for each of the method elements
     *
     * @param classSet  set of allowed elements for class
     * @param methodSet set of allowed elements for method
     * @param signSet   set of allowed elements for signature
     * @param <E>       type of generated triples
     * @return list of triples
     */
    private static <E extends Enum<E>> List<Combination<E>> generate(
            EnumSet<E> classSet, EnumSet<E> methodSet, EnumSet<E> signSet) {
        List<Combination<E>> list = new ArrayList<>();
        classSet.forEach(clsElement ->
            methodSet.forEach(methodElement ->
                signSet.forEach(signElement ->
                    list.add(new Combination<>(clsElement, methodElement,
                            signElement))
                )
            )
        );
        return list;
    }

    private static class Combination<T> extends Triple<T, T, T> {
        public Combination(T first, T second, T third) {
            super(first, second, third);
        }
    }
}
