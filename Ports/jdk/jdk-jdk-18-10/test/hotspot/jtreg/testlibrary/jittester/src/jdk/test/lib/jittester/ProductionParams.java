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

package jdk.test.lib.jittester;

import jdk.test.lib.jittester.utils.OptionResolver;
import jdk.test.lib.jittester.utils.OptionResolver.Option;

public class ProductionParams {

    public static Option<Integer> productionLimit = null;
    public static Option<Integer> dataMemberLimit = null;
    public static Option<Integer> statementLimit = null;
    public static Option<Integer> testStatementLimit = null;
    public static Option<Integer> operatorLimit = null;
    public static Option<Long> complexityLimit = null;
    public static Option<Integer> memberFunctionsLimit = null;
    public static Option<Integer> memberFunctionsArgLimit = null;
    public static Option<Integer> stringLiteralSizeLimit = null;
    public static Option<Integer> classesLimit = null;
    public static Option<Integer> implementationLimit = null;
    public static Option<Integer> dimensionsLimit = null;
    public static Option<Integer> floatingPointPrecision = null;
    public static Option<Integer> minCfgDepth = null;
    public static Option<Integer> maxCfgDepth = null;
    public static Option<Boolean> enableStrictFP = null;
    public static Option<Boolean> printComplexity = null;
    public static Option<Boolean> printHierarchy = null;
    //public static BooleanOption disableFinals = optionResolver.addBooleanOption("disable-finals", "Don\'t use finals");
    public static Option<Boolean> disableFinalClasses = null;
    public static Option<Boolean> disableFinalMethods = null;
    public static Option<Boolean> disableFinalVariables = null;
    public static Option<Boolean> disableIf = null;
    public static Option<Boolean> disableSwitch = null;
    public static Option<Boolean> disableWhile = null;
    public static Option<Boolean> disableDoWhile = null;
    public static Option<Boolean> disableFor = null;
    public static Option<Boolean> disableFunctions = null;
    public static Option<Boolean> disableVarsInBlock = null;
    public static Option<Boolean> disableExprInInit = null;
    public static Option<Boolean> disableExternalSymbols = null;
    public static Option<String> addExternalSymbols = null;
    public static Option<Boolean> disableInheritance = null;
    public static Option<Boolean> disableDowncasts = null;
    public static Option<Boolean> disableStatic = null;
    public static Option<Boolean> disableInterfaces = null;
    public static Option<Boolean> disableClasses = null;
    public static Option<Boolean> disableNestedBlocks = null;
    public static Option<Boolean> disableArrays = null;
    public static Option<Boolean> enableFinalizers = null;
    // workaraound: to reduce chance throwing ArrayIndexOutOfBoundsException
    public static Option<Integer> chanceExpressionIndex = null;
    public static Option<String> testbaseDir = null;
    public static Option<Integer> numberOfTests = null;
    public static Option<String> seed = null;
    public static Option<Long> specificSeed = null;
    public static Option<String> classesFile = null;
    public static Option<String> excludeMethodsFile = null;
    public static Option<String> generators = null;
    public static Option<String> generatorsFactories = null;

    public static void register(OptionResolver optionResolver) {
        productionLimit = optionResolver.addIntegerOption('l', "production-limit", 100, "Limit on steps in the production of an expression");
        dataMemberLimit = optionResolver.addIntegerOption('v', "data-member-limit", 10, "Upper limit on data members");
        statementLimit = optionResolver.addIntegerOption('s', "statement-limit", 30, "Upper limit on statements in function");
        testStatementLimit = optionResolver.addIntegerOption('e', "test-statement-limit", 300, "Upper limit on statements in test() function");
        operatorLimit = optionResolver.addIntegerOption('o', "operator-limit", 50, "Upper limit on operators in a statement");
        complexityLimit = optionResolver.addLongOption('x', "complexity-limit", 10000000, "Upper limit on complexity");
        memberFunctionsLimit = optionResolver.addIntegerOption('m', "member-functions-limit", 15, "Upper limit on member functions");
        memberFunctionsArgLimit = optionResolver.addIntegerOption('a', "member-functions-arg-limit", 5, "Upper limit on the number of member function args");
        stringLiteralSizeLimit = optionResolver.addIntegerOption("string-literal-size-limit", 10, "Upper limit on the number of chars in string literal");
        classesLimit = optionResolver.addIntegerOption('c', "classes-limit", 12, "Upper limit on the number of classes");
        implementationLimit = optionResolver.addIntegerOption('i', "implementation-limit", 3, "Upper limit on a number of interfaces a class can implement");
        dimensionsLimit = optionResolver.addIntegerOption('d', "dimensions-limit", 3, "Upper limit on array dimensions");
        floatingPointPrecision = optionResolver.addIntegerOption("fp-precision", 8, "A non-negative decimal integer used to restrict the number of digits after the decimal separator");
        minCfgDepth = optionResolver.addIntegerOption("min-cfg-depth", 2, "A non-negative decimal integer used to restrict the lower bound of depth of control flow graph");
        maxCfgDepth = optionResolver.addIntegerOption("max-cfg-depth", 3, "A non-negative decimal integer used to restrict the upper bound of depth of control flow graph");
        enableStrictFP = optionResolver.addBooleanOption("enable-strict-fp", "Add strictfp attribute to test class");
        printComplexity = optionResolver.addBooleanOption("print-complexity", "Print complexity of each statement");
        printHierarchy = optionResolver.addBooleanOption("print-hierarchy", "Print resulting class hierarchy");
        //disableFinals = optionResolver.addBooleanOption("disable-finals", "Don\'t use finals");
        disableFinalClasses = optionResolver.addBooleanOption("disable-final-classes", "Don\'t use final classes");
        disableFinalMethods = optionResolver.addBooleanOption("disable-final-methods", "Don\'t use final methods");
        disableFinalVariables = optionResolver.addBooleanOption("disable-final-variabless", "Don\'t use final variables");
        disableIf = optionResolver.addBooleanOption("disable-if", "Don\'t use conditionals");
        disableSwitch = optionResolver.addBooleanOption("disable-switch", "Don\'t use switch");
        disableWhile = optionResolver.addBooleanOption("disable-while", "Don\'t use while");
        disableDoWhile = optionResolver.addBooleanOption("disable-do-while", "Don\'t use do-while");
        disableFor = optionResolver.addBooleanOption("disable-for", "Don\'t use for");
        disableFunctions = optionResolver.addBooleanOption("disable-functions", "Don\'t use functions");
        disableVarsInBlock = optionResolver.addBooleanOption("disable-vars-in-block", "Don\'t generate variables in blocks");
        disableExprInInit = optionResolver.addBooleanOption("disable-expr-in-init", "Don\'t use complex expressions in variable initialization");
        disableExternalSymbols = optionResolver.addBooleanOption("disable-external-symbols", "Don\'t use external symbols");
        addExternalSymbols = optionResolver.addStringOption("add-external-symbols", "all", "Add symbols for listed classes (comma-separated list)");
        disableInheritance = optionResolver.addBooleanOption("disable-inheritance", "Disable inheritance");
        disableDowncasts = optionResolver.addBooleanOption("disable-downcasts", "Disable downcasting of objects");
        disableStatic = optionResolver.addBooleanOption("disable-static", "Disable generation of static objects and functions");
        disableInterfaces = optionResolver.addBooleanOption("disable-interfaces", "Disable generation of interfaces");
        disableClasses = optionResolver.addBooleanOption("disable-classes", "Disable generation of classes");
        disableNestedBlocks = optionResolver.addBooleanOption("disable-nested-blocks", "Disable generation of nested blocks");
        disableArrays = optionResolver.addBooleanOption("disable-arrays", "Disable generation of arrays");
        enableFinalizers = optionResolver.addBooleanOption("enable-finalizers", "Enable finalizers (for stress testing)");
        chanceExpressionIndex = optionResolver.addIntegerOption("chance-expression-index", 0, "A non negative decimal integer used to restrict chane of generating expression in array index while creating or accessing by index");
        testbaseDir = optionResolver.addStringOption("testbase-dir", ".", "Testbase dir");
        numberOfTests = optionResolver.addIntegerOption('n', "number-of-tests", 0, "Number of test classes to generate");
        seed = optionResolver.addStringOption("seed", "", "Random seed");
        specificSeed = optionResolver.addLongOption('z', "specificSeed", 0L, "A seed to be set for specific test generation(regular seed still needed for initialization)");
        classesFile = optionResolver.addStringOption('f', "classes-file", "conf/classes.lst", "File to read classes from");
        excludeMethodsFile = optionResolver.addStringOption('r', "exclude-methods-file", "conf/exclude.methods.lst", "File to read excluded methods from");
        generators = optionResolver.addStringOption("generators", "", "Comma-separated list of generator names");
        generatorsFactories = optionResolver.addStringOption("generatorsFactories", "", "Comma-separated list of generators factories class names");
    }
}
