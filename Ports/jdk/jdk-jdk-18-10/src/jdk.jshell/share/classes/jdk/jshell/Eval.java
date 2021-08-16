/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package jdk.jshell;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;

import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.Pretty;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import jdk.jshell.ExpressionToTypeInfo.ExpressionInfo;
import jdk.jshell.ExpressionToTypeInfo.ExpressionInfo.AnonymousDescription;
import jdk.jshell.ExpressionToTypeInfo.ExpressionInfo.AnonymousDescription.VariableDesc;
import jdk.jshell.Key.ErroneousKey;
import jdk.jshell.Key.MethodKey;
import jdk.jshell.Key.TypeDeclKey;
import jdk.jshell.Snippet.Kind;
import jdk.jshell.Snippet.SubKind;
import jdk.jshell.TaskFactory.AnalyzeTask;
import jdk.jshell.TaskFactory.BaseTask;
import jdk.jshell.TaskFactory.ParseTask;
import jdk.jshell.Util.Pair;
import jdk.jshell.Wrap.CompoundWrap;
import jdk.jshell.Wrap.Range;
import jdk.jshell.Snippet.Status;
import jdk.jshell.spi.ExecutionControl.ClassBytecodes;
import jdk.jshell.spi.ExecutionControl.ClassInstallException;
import jdk.jshell.spi.ExecutionControl.EngineTerminationException;
import jdk.jshell.spi.ExecutionControl.InternalException;
import jdk.jshell.spi.ExecutionControl.NotImplementedException;
import jdk.jshell.spi.ExecutionControl.ResolutionException;
import jdk.jshell.spi.ExecutionControl.RunException;
import jdk.jshell.spi.ExecutionControl.UserException;
import static java.util.stream.Collectors.toSet;
import static java.util.Collections.singletonList;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import static jdk.internal.jshell.debug.InternalDebugControl.DBG_GEN;
import static jdk.jshell.Snippet.Status.RECOVERABLE_DEFINED;
import static jdk.jshell.Snippet.Status.VALID;
import static jdk.jshell.Util.DOIT_METHOD_NAME;
import static jdk.jshell.Util.PREFIX_PATTERN;
import static jdk.jshell.Util.expunge;
import static jdk.jshell.Snippet.SubKind.SINGLE_TYPE_IMPORT_SUBKIND;
import static jdk.jshell.Snippet.SubKind.SINGLE_STATIC_IMPORT_SUBKIND;
import static jdk.jshell.Snippet.SubKind.TYPE_IMPORT_ON_DEMAND_SUBKIND;
import static jdk.jshell.Snippet.SubKind.STATIC_IMPORT_ON_DEMAND_SUBKIND;

/**
 * The Evaluation Engine. Source internal analysis, wrapping control,
 * compilation, declaration. redefinition, replacement, and execution.
 *
 * @author Robert Field
 */
class Eval {

    private static final Pattern IMPORT_PATTERN = Pattern.compile("import\\p{javaWhitespace}+(?<static>static\\p{javaWhitespace}+)?(?<fullname>[\\p{L}\\p{N}_\\$\\.]+\\.(?<name>[\\p{L}\\p{N}_\\$]+|\\*))");
    private static final Pattern DEFAULT_PREFIX = Pattern.compile("\\p{javaWhitespace}*(default)\\p{javaWhitespace}+");

    // for uses that should not change state -- non-evaluations
    private boolean preserveState = false;

    private int varNumber = 0;

    /* The number of anonymous innerclasses seen so far. Used to generate unique
     * names of these classes.
     */
    private int anonCount = 0;

    private final JShell state;

    // The set of names of methods on Object
    private final Set<String> objectMethods = Arrays
            .stream(Object.class.getMethods())
            .map(m -> m.getName())
            .collect(toSet());

    Eval(JShell state) {
        this.state = state;
    }

    /**
     * Evaluates a snippet of source.
     *
     * @param userSource the source of the snippet
     * @return the list of primary and update events
     * @throws IllegalStateException
     */
    List<SnippetEvent> eval(String userSource) throws IllegalStateException {
        List<SnippetEvent> allEvents = new ArrayList<>();
        for (Snippet snip : sourceToSnippets(userSource)) {
            if (snip.kind() == Kind.ERRONEOUS) {
                state.maps.installSnippet(snip);
                allEvents.add(new SnippetEvent(
                        snip, Status.NONEXISTENT, Status.REJECTED,
                        false, null, null, null));
            } else {
                allEvents.addAll(declare(snip, snip.syntheticDiags()));
            }
        }
        return allEvents;
    }

    /**
     * Converts the user source of a snippet into a Snippet list -- Snippet will
     * have wrappers.
     *
     * @param userSource the source of the snippet
     * @return usually a singleton list of Snippet, but may be empty or multiple
     */
    List<Snippet> sourceToSnippetsWithWrappers(String userSource) {
        List<Snippet> snippets = sourceToSnippets(userSource);
        for (Snippet snip : snippets) {
            if (snip.outerWrap() == null) {
                snip.setOuterWrap(
                        (snip.kind() == Kind.IMPORT)
                                ? state.outerMap.wrapImport(snip.guts(), snip)
                                : state.outerMap.wrapInTrialClass(snip.guts())
                );
            }
        }
        return snippets;
    }

    /**
     * Converts the user source of a snippet into a Snippet object (or list of
     * objects in the case of: int x, y, z;).  Does not install the Snippets
     * or execute them.  Does not change any state.
     *
     * @param userSource the source of the snippet
     * @return usually a singleton list of Snippet, but may be empty or multiple
     */
    List<Snippet> toScratchSnippets(String userSource) {
        try {
            preserveState = true;
            return sourceToSnippets(userSource);
        } finally {
            preserveState = false;
        }
    }

    /**
     * Converts the user source of a snippet into a Snippet object (or list of
     * objects in the case of: int x, y, z;).  Does not install the Snippets
     * or execute them.
     *
     * @param userSource the source of the snippet
     * @return usually a singleton list of Snippet, but may be empty or multiple
     */
    private List<Snippet> sourceToSnippets(String userSource) {
        String compileSource = Util.trimEnd(new MaskCommentsAndModifiers(userSource, false).cleared());
        if (compileSource.length() == 0) {
            return Collections.emptyList();
        }
        return state.taskFactory.parse(compileSource, pt -> {
            List<? extends Tree> units = pt.units();
            if (units.isEmpty()) {
                return compileFailResult(pt, userSource, Kind.ERRONEOUS);
            }
            Tree unitTree = units.get(0);
            if (pt.getDiagnostics().hasOtherThanNotStatementErrors()) {
                Matcher matcher = DEFAULT_PREFIX.matcher(compileSource);
                DiagList dlist = matcher.lookingAt()
                        ? new DiagList(new ModifierDiagnostic(true,
                            state.messageFormat("jshell.diag.modifier.single.fatal", "'default'"),
                            matcher.start(1), matcher.end(1)))
                        : pt.getDiagnostics();
                return compileFailResult(dlist, userSource, kindOfTree(unitTree));
            }

            // Erase illegal/ignored modifiers
            String compileSourceInt = new MaskCommentsAndModifiers(compileSource, true).cleared();

            state.debug(DBG_GEN, "Kind: %s -- %s\n", unitTree.getKind(), unitTree);
            return switch (unitTree.getKind()) {
                case IMPORT
                    -> processImport(userSource, compileSourceInt);
                case VARIABLE
                    -> processVariables(userSource, units, compileSourceInt, pt);
                case EXPRESSION_STATEMENT
                    -> processExpression(userSource, unitTree, compileSourceInt, pt);
                case CLASS
                    -> processClass(userSource, unitTree, compileSourceInt, SubKind.CLASS_SUBKIND, pt);
                case ENUM
                    -> processClass(userSource, unitTree, compileSourceInt, SubKind.ENUM_SUBKIND, pt);
                case ANNOTATION_TYPE
                    -> processClass(userSource, unitTree, compileSourceInt, SubKind.ANNOTATION_TYPE_SUBKIND, pt);
                case INTERFACE
                    -> processClass(userSource, unitTree, compileSourceInt, SubKind.INTERFACE_SUBKIND, pt);
                case RECORD
                    -> processClass(userSource, unitTree, compileSourceInt, SubKind.RECORD_SUBKIND, pt);
                case METHOD
                    -> processMethod(userSource, unitTree, compileSourceInt, pt);
                default
                    -> processStatement(userSource, compileSourceInt);
            };
        });
    }

    private List<Snippet> processImport(String userSource, String compileSource) {
        Wrap guts = Wrap.simpleWrap(compileSource);
        Matcher mat = IMPORT_PATTERN.matcher(compileSource);
        String fullname;
        String name;
        boolean isStatic;
        if (mat.find()) {
            isStatic = mat.group("static") != null;
            name = mat.group("name");
            fullname = mat.group("fullname");
        } else {
            // bad import -- fake it
            isStatic = compileSource.contains("static");
            name = fullname = compileSource;
        }
        String fullkey = (isStatic ? "static-" : "") + fullname;
        boolean isStar = name.equals("*");
        String keyName = isStar
                ? fullname
                : name;
        SubKind snippetKind = isStar
                ? (isStatic ? STATIC_IMPORT_ON_DEMAND_SUBKIND : TYPE_IMPORT_ON_DEMAND_SUBKIND)
                : (isStatic ? SINGLE_STATIC_IMPORT_SUBKIND : SINGLE_TYPE_IMPORT_SUBKIND);
        Snippet snip = new ImportSnippet(state.keyMap.keyForImport(keyName, snippetKind),
                userSource, guts, fullname, name, snippetKind, fullkey, isStatic, isStar);
        return singletonList(snip);
    }

    private static class EvalPretty extends Pretty {

        private final Writer out;

        public EvalPretty(Writer writer, boolean bln) {
            super(writer, bln);
            this.out = writer;
        }

        /**
         * Print string, DO NOT replacing all non-ascii character with unicode
         * escapes.
         */
        @Override
        public void print(Object o) throws IOException {
            out.write(o.toString());
        }

        static String prettyExpr(JCTree tree, boolean bln) {
            StringWriter out = new StringWriter();
            try {
                new EvalPretty(out, bln).printExpr(tree);
            } catch (IOException e) {
                throw new AssertionError(e);
            }
            return out.toString();
        }
    }

    private List<Snippet> processVariables(String userSource, List<? extends Tree> units, String compileSource, ParseTask pt) {
        List<Snippet> snippets = new ArrayList<>();
        TreeDissector dis = TreeDissector.createByFirstClass(pt);
        for (Tree unitTree : units) {
            VariableTree vt = (VariableTree) unitTree;
            String name = vt.getName().toString();
//            String name = userReadableName(vt.getName(), compileSource);
            String typeName;
            String fullTypeName;
            String displayType;
            boolean hasEnhancedType = false;
            TreeDependencyScanner tds = new TreeDependencyScanner();
            Wrap typeWrap;
            Wrap anonDeclareWrap = null;
            Wrap winit = null;
            boolean enhancedDesugaring = false;
            Set<String> anonymousClasses = Collections.emptySet();
            StringBuilder sbBrackets = new StringBuilder();
            Tree baseType = vt.getType();
            if (baseType != null) {
                tds.scan(baseType); // Not dependent on initializer
                fullTypeName = displayType = typeName = EvalPretty.prettyExpr((JCTree) vt.getType(), false);
                while (baseType instanceof ArrayTypeTree) {
                    //TODO handle annotations too
                    baseType = ((ArrayTypeTree) baseType).getType();
                    sbBrackets.append("[]");
                }
                Range rtype = dis.treeToRange(baseType);
                typeWrap = Wrap.rangeWrap(compileSource, rtype);
            } else {
                DiagList dl = trialCompile(Wrap.methodWrap(compileSource));
                if (dl.hasErrors()) {
                    return compileFailResult(dl, userSource, kindOfTree(unitTree));
                }
                Tree init = vt.getInitializer();
                if (init != null) {
                    Range rinit = dis.treeToRange(init);
                    String initCode = rinit.part(compileSource);
                    ExpressionInfo ei =
                            ExpressionToTypeInfo.localVariableTypeForInitializer(initCode, state, false);
                    if (ei != null && ei.declareTypeName != null) {
                        typeName = ei.declareTypeName;
                        fullTypeName = ei.fullTypeName;
                        displayType = ei.displayTypeName;

                        hasEnhancedType = !typeName.equals(fullTypeName);

                        enhancedDesugaring = !ei.isPrimitiveType;

                        Pair<Wrap, Wrap> anonymous2Member =
                                anonymous2Member(ei, compileSource, rinit, dis, init);
                        anonDeclareWrap = anonymous2Member.first;
                        winit = anonymous2Member.second;
                        anonymousClasses = ei.anonymousClasses.stream().map(ad -> ad.declareTypeName).collect(Collectors.toSet());
                    } else {
                        displayType = fullTypeName = typeName = "java.lang.Object";
                    }
                    tds.scan(init);
                } else {
                    displayType = fullTypeName = typeName = "java.lang.Object";
                }
                typeWrap = Wrap.identityWrap(typeName);
            }
            Range runit = dis.treeToRange(vt);
            runit = new Range(runit.begin, runit.end - 1);
            ExpressionTree it = vt.getInitializer();
            int nameMax = runit.end - 1;
            SubKind subkind;
            if (it != null) {
                subkind = SubKind.VAR_DECLARATION_WITH_INITIALIZER_SUBKIND;
                Range rinit = dis.treeToRange(it);
                winit = winit == null ? Wrap.rangeWrap(compileSource, rinit) : winit;
                nameMax = rinit.begin - 1;
            } else {
                String sinit = switch (typeName) {
                    case "byte",
                         "short",
                         "int"     -> "0";
                    case "long"    -> "0L";
                    case "float"   -> "0.0f";
                    case "double"  -> "0.0d";
                    case "boolean" -> "false";
                    case "char"    -> "'\\u0000'";
                    default        -> "null";
                };
                winit = Wrap.simpleWrap(sinit);
                subkind = SubKind.VAR_DECLARATION_SUBKIND;
            }
            Wrap wname;
            int nameStart = compileSource.lastIndexOf(name, nameMax);
            if (nameStart < 0) {
                // the name has been transformed (e.g. unicode).
                // Use it directly
                wname = Wrap.identityWrap(name);
            } else {
                int nameEnd = nameStart + name.length();
                Range rname = new Range(nameStart, nameEnd);
                wname = new Wrap.RangeWrap(compileSource, rname);
            }
            Wrap guts = Wrap.varWrap(compileSource, typeWrap, sbBrackets.toString(), wname,
                                     winit, enhancedDesugaring, anonDeclareWrap);
            DiagList modDiag = modifierDiagnostics(vt.getModifiers(), dis, true);
            Snippet snip = new VarSnippet(state.keyMap.keyForVariable(name), userSource, guts,
                    name, subkind, displayType, hasEnhancedType ? fullTypeName : null, anonymousClasses,
                    tds.declareReferences(), modDiag);
            snippets.add(snip);
        }
        return snippets;
    }

    private String userReadableName(Name nn, String compileSource) {
        String s = nn.toString();
        if (s.length() > 0 && Character.isJavaIdentifierStart(s.charAt(0)) && compileSource.contains(s)) {
            return s;
        }
        String l = nameInUnicode(nn, false);
        if (compileSource.contains(l)) {
            return l;
        }
        return nameInUnicode(nn, true);
    }

    private String nameInUnicode(Name nn, boolean upper) {
        return nn.codePoints()
                .mapToObj(cp -> (cp > 0x7F)
                        ? String.format(upper ? "\\u%04X" : "\\u%04x", cp)
                        : "" + (char) cp)
                .collect(Collectors.joining());
    }

    /**Convert anonymous classes in "init" to member classes, based
     * on the additional information from ExpressionInfo.anonymousClasses.
     *
     * This means:
     * -if the code in the anonymous class captures any variables from the
     *  enclosing context, create fields for them
     * -creating an explicit constructor that:
     * --if the new class expression has a base/enclosing expression, make it an
     *   explicit constructor parameter "encl" and use "encl.super" when invoking
     *   the supertype constructor
     * --if the (used) supertype constructor has any parameters, declare them
     *   as explicit parameters of the constructor, and pass them to the super
     *   constructor
     * --if the code in the anonymous class captures any variables from the
     *   enclosing context, make them an explicit paramters of the constructor
     *   and assign to respective fields.
     * --if there are any explicit fields with initializers in the anonymous class,
     *   move the initializers at the end of the constructor (after the captured fields
     *   are assigned, so that the initializers of these fields can use them).
     * -from the captured variables fields, constructor, and existing members
     *  (with cleared field initializers), create an explicit class that extends or
     *  implements the supertype of the anonymous class.
     *
     * This method returns two wraps: the first contains the class declarations for the
     * converted classes, the first one should be used instead of "init" in the variable
     * declaration.
     */
    private Pair<Wrap, Wrap> anonymous2Member(ExpressionInfo ei,
                                              String compileSource,
                                              Range rinit,
                                              TreeDissector dis,
                                              Tree init) {
        List<Wrap> anonymousDeclarations = new ArrayList<>();
        List<Wrap> partitionedInit = new ArrayList<>();
        int lastPos = rinit.begin;
        com.sun.tools.javac.util.List<NewClassTree> toConvert =
                ExpressionToTypeInfo.listAnonymousClassesToConvert(init);
        com.sun.tools.javac.util.List<AnonymousDescription> descriptions =
                ei.anonymousClasses;
        while (toConvert.nonEmpty() && descriptions.nonEmpty()) {
            NewClassTree node = toConvert.head;
            AnonymousDescription ad = descriptions.head;

            toConvert = toConvert.tail;
            descriptions = descriptions.tail;

            List<Object> classBodyParts = new ArrayList<>();
            //declarations of the captured variables:
            for (VariableDesc vd : ad.capturedVariables) {
                classBodyParts.add(vd.type + " " + vd.name + ";\n");
            }

            List<Object> constructorParts = new ArrayList<>();
            constructorParts.add(ad.declareTypeName + "(");
            String sep = "";
            //add the parameter for the base/enclosing expression, if any:
            if (ad.enclosingInstanceType != null) {
                constructorParts.add(ad.enclosingInstanceType + " encl");
                sep = ", ";
            }
            int idx = 0;
            //add parameters of the super constructor, if any:
            for (String type : ad.parameterTypes) {
                constructorParts.add(sep);
                constructorParts.add(type + " " + "arg" + idx++);
                sep = ", ";
            }
            //add parameters for the captured variables:
            for (VariableDesc vd : ad.capturedVariables) {
                constructorParts.add(sep);
                constructorParts.add(vd.type + " " + "cap$" + vd.name);
                sep = ", ";
            }
            //construct super constructor call:
            if (ad.enclosingInstanceType != null) {
                //if there's an enclosing instance, call super on it:
                constructorParts.add(") { encl.super (");
            } else {
                constructorParts.add(") { super (");
            }
            sep = "";
            for (int i = 0; i < idx; i++) {
                constructorParts.add(sep);
                constructorParts.add("arg" + i);
                sep = ", ";
            }
            constructorParts.add(");");
            //initialize the captured variables:
            for (VariableDesc vd : ad.capturedVariables) {
                constructorParts.add("this." + vd.name + " = " + "cap$" + vd.name + ";\n");
            }
            List<? extends Tree> members =
                    node.getClassBody().getMembers();
            for (Tree member : members) {
                if (member.getKind() == Tree.Kind.VARIABLE) {
                    VariableTree vt = (VariableTree) member;

                    if (vt.getInitializer() != null) {
                        //for variables with initializer, explicitly move the initializer
                        //to the constructor after the captured variables as assigned
                        //(the initializers would otherwise run too early):
                        Range wholeVar = dis.treeToRange(vt);
                        int name = ((JCTree) vt).pos;
                        classBodyParts.add(new CompoundWrap(Wrap.rangeWrap(compileSource,
                                                                      new Range(wholeVar.begin, name)),
                                                       vt.getName().toString(),
                                                       ";\n"));
                        constructorParts.add(Wrap.rangeWrap(compileSource,
                                                            new Range(name, wholeVar.end)));
                        continue;
                    }
                }
                classBodyParts.add(Wrap.rangeWrap(compileSource,
                                             dis.treeToRange(member)));
            }

            constructorParts.add("}");

            //construct the member class:
            classBodyParts.add(new CompoundWrap(constructorParts.toArray()));

            Wrap classBodyWrap = new CompoundWrap(classBodyParts.toArray());

            anonymousDeclarations.add(new CompoundWrap("public static class ", ad.declareTypeName,
                                         (ad.isClass ? " extends " : " implements "),
                                         ad.superTypeName, " { ", classBodyWrap, "}"));

            //change the new class expression to use the newly created member type:
            Range argRange = dis.treeListToRange(node.getArguments());
            Wrap argWrap;

            if (argRange != null) {
                argWrap = Wrap.rangeWrap(compileSource, argRange);
            } else {
                argWrap = Wrap.simpleWrap(" ");
            }

            if (ad.enclosingInstanceType != null) {
                //if there's an enclosing expression, set it as the first parameter:
                Range enclosingRanges =
                        dis.treeToRange(node.getEnclosingExpression());
                Wrap enclosingWrap = Wrap.rangeWrap(compileSource, enclosingRanges);
                argWrap = argRange != null ? new CompoundWrap(enclosingWrap,
                                                              Wrap.simpleWrap(","),
                                                              argWrap)
                                           : enclosingWrap;
            }

            Range current = dis.treeToRange(node);
            String capturedArgs;
            if (!ad.capturedVariables.isEmpty()) {
                capturedArgs = (ad.parameterTypes.isEmpty() ? "" : ", ") +
                               ad.capturedVariables.stream()
                                                   .map(vd -> vd.name)
                                                   .collect(Collectors.joining(","));
            } else {
                capturedArgs = "";
            }
            if (lastPos < current.begin)
                partitionedInit.add(Wrap.rangeWrap(compileSource,
                                                   new Range(lastPos, current.begin)));
            partitionedInit.add(new CompoundWrap("new " + ad.declareTypeName + "(",
                                                 argWrap,
                                                 capturedArgs,
                                                 ")"));
            lastPos = current.end;
        }

        if (lastPos < rinit.end)
            partitionedInit.add(Wrap.rangeWrap(compileSource, new Range(lastPos, rinit.end)));

        return new Pair<>(new CompoundWrap(anonymousDeclarations.toArray()),
                          new CompoundWrap(partitionedInit.toArray()));
    }

    private List<Snippet> processExpression(String userSource, Tree tree, String compileSource, ParseTask pt) {
        ExpressionStatementTree expr = (ExpressionStatementTree) tree;
        String name = null;
        ExpressionInfo ei = ExpressionToTypeInfo.expressionInfo(compileSource, state);
        ExpressionTree assignVar;
        Wrap guts;
        Snippet snip;
        if (ei != null && ei.isNonVoid) {
            String typeName = ei.typeName;
            SubKind subkind;
            if (ei.tree instanceof IdentifierTree) {
                IdentifierTree id = (IdentifierTree) ei.tree;
                name = id.getName().toString();
                subkind = SubKind.VAR_VALUE_SUBKIND;

            } else if (ei.tree instanceof AssignmentTree
                    && (assignVar = ((AssignmentTree) ei.tree).getVariable()) instanceof IdentifierTree) {
                name = assignVar.toString();
                subkind = SubKind.ASSIGNMENT_SUBKIND;
            } else {
                subkind = SubKind.OTHER_EXPRESSION_SUBKIND;
            }
            if (shouldGenTempVar(subkind)) {
                if (preserveState) {
                    name = "$$";
                } else {
                    if (state.tempVariableNameGenerator != null) {
                        name = state.tempVariableNameGenerator.get();
                    }
                    while (name == null || state.keyMap.doesVariableNameExist(name)) {
                        name = "$" + ++varNumber;
                    }
                }
                ExpressionInfo varEI =
                        ExpressionToTypeInfo.localVariableTypeForInitializer(compileSource, state, true);
                String declareTypeName;
                String fullTypeName;
                String displayTypeName;
                Set<String> anonymousClasses;
                if (varEI != null) {
                    declareTypeName = varEI.declareTypeName;
                    fullTypeName = varEI.fullTypeName;
                    displayTypeName = varEI.displayTypeName;

                    TreeDissector dis = TreeDissector.createByFirstClass(pt);
                    Pair<Wrap, Wrap> anonymous2Member =
                            anonymous2Member(varEI, compileSource, new Range(0, compileSource.length()), dis, expr.getExpression());
                    guts = Wrap.tempVarWrap(anonymous2Member.second.wrapped(), declareTypeName, name, anonymous2Member.first);
                    anonymousClasses = varEI.anonymousClasses.stream().map(ad -> ad.declareTypeName).collect(Collectors.toSet());
                } else {
                    declareTypeName = ei.accessibleTypeName;
                    displayTypeName = fullTypeName = typeName;
                    guts = Wrap.tempVarWrap(compileSource, declareTypeName, name, null);
                    anonymousClasses = Collections.emptySet();
                }
                Collection<String> declareReferences = null; //TODO
                snip = new VarSnippet(state.keyMap.keyForVariable(name), userSource, guts,
                        name, SubKind.TEMP_VAR_EXPRESSION_SUBKIND, displayTypeName, fullTypeName, anonymousClasses, declareReferences, null);
            } else {
                guts = Wrap.methodReturnWrap(compileSource);
                snip = new ExpressionSnippet(state.keyMap.keyForExpression(name, typeName), userSource, guts,
                        name, subkind);
            }
        } else {
            guts = Wrap.methodWrap(compileSource);
            if (ei == null) {
                // We got no type info, check for not a statement by trying
                DiagList dl = trialCompile(guts);
                if (dl.hasUnreachableError()) {
                    guts = Wrap.methodUnreachableWrap(compileSource);
                    dl = trialCompile(guts);
                }
                if (dl.hasNotStatement()) {
                    guts = Wrap.methodReturnWrap(compileSource);
                    dl = trialCompile(guts);
                }
                if (dl.hasErrors()) {
                    return compileFailResult(dl, userSource, Kind.EXPRESSION);
                }
            }
            snip = new StatementSnippet(state.keyMap.keyForStatement(), userSource, guts);
        }
        return singletonList(snip);
    }

    private List<Snippet> processClass(String userSource, Tree unitTree, String compileSource, SubKind snippetKind, ParseTask pt) {
        TreeDependencyScanner tds = new TreeDependencyScanner();
        tds.scan(unitTree);

        TreeDissector dis = TreeDissector.createByFirstClass(pt);

        ClassTree klassTree = (ClassTree) unitTree;
//        String name = userReadableName(klassTree.getSimpleName(), compileSource);
        String name = klassTree.getSimpleName().toString();
        DiagList modDiag = modifierDiagnostics(klassTree.getModifiers(), dis, false);
        TypeDeclKey key = state.keyMap.keyForClass(name);
        // Corralling
        Wrap corralled = new Corraller(dis, key.index(), compileSource).corralType(klassTree);

        Wrap guts = Wrap.classMemberWrap(compileSource);
        Snippet snip = new TypeDeclSnippet(key, userSource, guts,
                name, snippetKind,
                corralled, tds.declareReferences(), tds.bodyReferences(), modDiag);
        return singletonList(snip);
    }

    private List<Snippet> processStatement(String userSource, String compileSource) {
        Wrap guts = Wrap.methodWrap(compileSource);
        // Check for unreachable by trying
        DiagList dl = trialCompile(guts);
        if (dl.hasErrors()) {
            if (dl.hasUnreachableError()) {
                guts = Wrap.methodUnreachableSemiWrap(compileSource);
                dl = trialCompile(guts);
                if (dl.hasErrors()) {
                    if (dl.hasUnreachableError()) {
                        // Without ending semicolon
                        guts = Wrap.methodUnreachableWrap(compileSource);
                        dl = trialCompile(guts);
                    }
                    if (dl.hasErrors()) {
                        return compileFailResult(dl, userSource, Kind.STATEMENT);
                    }
                }
            } else {
                return compileFailResult(dl, userSource, Kind.STATEMENT);
            }
        }
        Snippet snip = new StatementSnippet(state.keyMap.keyForStatement(), userSource, guts);
        return singletonList(snip);
    }

    private DiagList trialCompile(Wrap guts) {
        OuterWrap outer = state.outerMap.wrapInTrialClass(guts);
        return state.taskFactory.analyze(outer, AnalyzeTask::getDiagnostics);
    }

    private List<Snippet> processMethod(String userSource, Tree unitTree, String compileSource, ParseTask pt) {
        TreeDependencyScanner tds = new TreeDependencyScanner();
        tds.scan(unitTree);
        final TreeDissector dis = TreeDissector.createByFirstClass(pt);

        final MethodTree mt = (MethodTree) unitTree;
        //String name = userReadableName(mt.getName(), compileSource);
        final String name = mt.getName().toString();
        if (objectMethods.contains(name)) {
            // The name matches a method on Object, short of an overhaul, this
            // fails, see 8187137.  Generate a descriptive error message

            // The error position will be the position of the name, find it
            long possibleStart = dis.getEndPosition(mt.getReturnType());
            Range possibleRange = new Range((int) possibleStart,
                    dis.getStartPosition(mt.getBody()));
            String possibleNameSection = possibleRange.part(compileSource);
            int offset = possibleNameSection.indexOf(name);
            long start = offset < 0
                    ? possibleStart // something wrong, punt
                    : possibleStart + offset;

            return compileFailResult(new DiagList(objectMethodNameDiag(name, start)), userSource, Kind.METHOD);
        }
        String parameterTypes
                = mt.getParameters()
                .stream()
                .map(param -> dis.treeToRange(param.getType()).part(compileSource))
                .collect(Collectors.joining(","));
        Tree returnType = mt.getReturnType();
        DiagList modDiag = modifierDiagnostics(mt.getModifiers(), dis, false);
        if (modDiag.hasErrors()) {
            return compileFailResult(modDiag, userSource, Kind.METHOD);
        }
        MethodKey key = state.keyMap.keyForMethod(name, parameterTypes);

        Wrap corralled;
        Wrap guts;
        String unresolvedSelf;
        if (mt.getModifiers().getFlags().contains(Modifier.ABSTRACT)) {
            if (mt.getBody() == null) {
                // abstract method -- pre-corral
                corralled = null; // no fall-back
                guts = new Corraller(dis, key.index(), compileSource).corralMethod(mt);
                unresolvedSelf = "method " + name + "(" + parameterTypes + ")";
            } else {
                // abstract with body, don't pollute the error message
                corralled = null;
                guts = Wrap.simpleWrap(compileSource);
                unresolvedSelf = null;
            }
        } else {
            // normal method
            corralled = new Corraller(dis, key.index(), compileSource).corralMethod(mt);
            guts = Wrap.classMemberWrap(compileSource);
            unresolvedSelf = null;
        }
        Range typeRange = dis.treeToRange(returnType);
        String signature = "(" + parameterTypes + ")" + typeRange.part(compileSource);

        Snippet snip = new MethodSnippet(key, userSource, guts,
                name, signature,
                corralled, tds.declareReferences(), tds.bodyReferences(),
                unresolvedSelf, modDiag);
        return singletonList(snip);
    }

    private Kind kindOfTree(Tree tree) {
        switch (tree.getKind()) {
            case IMPORT:
                return Kind.IMPORT;
            case VARIABLE:
                return Kind.VAR;
            case EXPRESSION_STATEMENT:
                return Kind.EXPRESSION;
            case CLASS:
            case ENUM:
            case ANNOTATION_TYPE:
            case INTERFACE:
                return Kind.TYPE_DECL;
            case METHOD:
                return Kind.METHOD;
            default:
                return Kind.STATEMENT;
        }
    }

    /**
     * The snippet has failed, return with the rejected snippet
     *
     * @param xt the task from which to extract the failure diagnostics
     * @param userSource the incoming bad user source
     * @return a rejected snippet
     */
    private List<Snippet> compileFailResult(BaseTask xt, String userSource, Kind probableKind) {
        return compileFailResult(xt.getDiagnostics(), userSource, probableKind);
    }

    /**
     * The snippet has failed, return with the rejected snippet
     *
     * @param diags the failure diagnostics
     * @param userSource the incoming bad user source
     * @return a rejected snippet
     */
    private List<Snippet> compileFailResult(DiagList diags, String userSource, Kind probableKind) {
        ErroneousKey key = state.keyMap.keyForErroneous();
        Snippet snip = new ErroneousSnippet(key, userSource, null,
                probableKind, SubKind.UNKNOWN_SUBKIND);
        snip.setFailed(diags);

        // Install  wrapper for query by SourceCodeAnalysis.wrapper
        String compileSource = Util.trimEnd(new MaskCommentsAndModifiers(userSource, true).cleared());
        OuterWrap outer = switch (probableKind) {
            case IMPORT     -> state.outerMap.wrapImport(Wrap.simpleWrap(compileSource), snip);
            case EXPRESSION -> state.outerMap.wrapInTrialClass(Wrap.methodReturnWrap(compileSource));
            case VAR,
                 TYPE_DECL,
                 METHOD     -> state.outerMap.wrapInTrialClass(Wrap.classMemberWrap(compileSource));
            default         -> state.outerMap.wrapInTrialClass(Wrap.methodWrap(compileSource));
        };
        snip.setOuterWrap(outer);

        return singletonList(snip);
    }

    /**
     * Should a temp var wrap the expression. TODO make this user configurable.
     *
     * @param snippetKind
     * @return
     */
    private boolean shouldGenTempVar(SubKind snippetKind) {
        return snippetKind == SubKind.OTHER_EXPRESSION_SUBKIND;
    }

    List<SnippetEvent> drop(Snippet si) {
        Unit c = new Unit(state, si);
        Set<Unit> outs;
        if (si instanceof PersistentSnippet) {
            Set<Unit> ins = c.dependents().collect(toSet());
            outs = compileAndLoad(ins);
        } else {
            outs = Collections.emptySet();
        }
        return events(c, outs, null, null);
    }

    private List<SnippetEvent> declare(Snippet si, DiagList generatedDiagnostics) {
        Unit c = new Unit(state, si, null, generatedDiagnostics);
        Set<Unit> ins = new LinkedHashSet<>();
        ins.add(c);
        Set<Unit> outs = compileAndLoad(ins);

        if (si.status().isActive() && si instanceof MethodSnippet) {
            // special processing for abstract methods
            MethodSnippet msi = (MethodSnippet) si;
            String unresolvedSelf = msi.unresolvedSelf;
            if (unresolvedSelf != null) {
                List<String> unresolved = new ArrayList<>(si.unresolved());
                unresolved.add(unresolvedSelf);
                si.setCompilationStatus(si.status() == VALID ? RECOVERABLE_DEFINED : si.status(),
                        unresolved, si.diagnostics());
            }
        }

        if (!si.status().isDefined()
                && si.diagnostics().isEmpty()
                && si.unresolved().isEmpty()) {
            // did not succeed, but no record of it, extract from others
            si.setDiagnostics(outs.stream()
                    .flatMap(u -> u.snippet().diagnostics().stream())
                    .collect(Collectors.toCollection(DiagList::new)));
        }

        // If appropriate, execute the snippet
        String value = null;
        JShellException exception = null;
        if (si.status().isDefined()) {
            if (si.isExecutable()) {
                try {
                    value = state.executionControl().invoke(si.classFullName(), DOIT_METHOD_NAME);
                    value = si.subKind().hasValue()
                            ? expunge(value)
                            : "";
                } catch (ResolutionException ex) {
                    exception = asUnresolvedReferenceException(ex);
                } catch (UserException ex) {
                    exception = asEvalException(ex);
                } catch (RunException ex) {
                    // StopException - no-op
                } catch (InternalException ex) {
                    state.debug(ex, "invoke");
                } catch (EngineTerminationException ex) {
                    state.debug(ex, "termination");
                    state.closeDown();
                }
            }
        }
        return events(c, outs, value, exception);
    }

    // Convert an internal UserException to an API EvalException, translating
    // the stack to snippet form.  Convert any chained exceptions
    private EvalException asEvalException(UserException ue) {
        return new EvalException(ue.getMessage(),
                ue.causeExceptionClass(),
                translateExceptionStack(ue),
                asJShellException(ue.getCause()));
    }

    // Convert an internal ResolutionException to an API UnresolvedReferenceException,
    // translating the snippet id to snipper and the stack to snippet form
    private UnresolvedReferenceException asUnresolvedReferenceException(ResolutionException re) {
        DeclarationSnippet sn = (DeclarationSnippet) state.maps.getSnippetDeadOrAlive(re.id());
        return new UnresolvedReferenceException(sn, translateExceptionStack(re));
    }

    // Convert an internal UserException/ResolutionException to an API
    // EvalException/UnresolvedReferenceException
    private JShellException asJShellException(Throwable e) {
        if (e == null) {
            return null;
        } else if (e instanceof UserException) {
            return asEvalException((UserException) e);
        } else if (e instanceof ResolutionException) {
            return asUnresolvedReferenceException((ResolutionException) e);
        } else {
            throw new AssertionError(e);
        }
    }

    private boolean interestingEvent(SnippetEvent e) {
        return e.isSignatureChange()
                    || e.causeSnippet() == null
                    || e.status() != e.previousStatus()
                    || e.exception() != null;
    }

    private List<SnippetEvent> events(Unit c, Collection<Unit> outs, String value, JShellException exception) {
        List<SnippetEvent> events = new ArrayList<>();
        events.add(c.event(value, exception));
        events.addAll(outs.stream()
                .filter(u -> u != c)
                .map(u -> u.event(null, null))
                .filter(this::interestingEvent)
                .toList());
        events.addAll(outs.stream()
                .flatMap(u -> u.secondaryEvents().stream())
                .filter(this::interestingEvent)
                .toList());
        //System.err.printf("Events: %s\n", events);
        return events;
    }

    private Set<OuterWrap> outerWrapSet(Collection<Unit> units) {
        return units.stream()
                .map(u -> u.snippet().outerWrap())
                .collect(toSet());
    }

    private Set<Unit> compileAndLoad(Set<Unit> ins) {
        if (ins.isEmpty()) {
            return ins;
        }
        Set<Unit> replaced = new LinkedHashSet<>();
        // Loop until dependencies and errors are stable
        while (true) {
            state.debug(DBG_GEN, "compileAndLoad  %s\n", ins);

            ins.stream().forEach(Unit::initialize);
            ins.stream().forEach(u -> u.setWrap(ins, ins));
            state.taskFactory.analyze(outerWrapSet(ins), at -> {
                ins.stream().forEach(u -> u.setDiagnostics(at));

                // corral any Snippets that need it
                if (ins.stream().filter(u -> u.corralIfNeeded(ins)).count() > 0) {
                    // if any were corralled, re-analyze everything
                    state.taskFactory.analyze(outerWrapSet(ins), cat -> {
                        ins.stream().forEach(u -> u.setCorralledDiagnostics(cat));
                        ins.stream().forEach(u -> u.setStatus(cat));
                        return null;
                    });
                } else {
                    ins.stream().forEach(u -> u.setStatus(at));
                }
                return null;
            });
            // compile and load the legit snippets
            boolean success;
            while (true) {
                List<Unit> legit = ins.stream()
                        .filter(Unit::isDefined)
                        .toList();
                state.debug(DBG_GEN, "compileAndLoad ins = %s -- legit = %s\n",
                        ins, legit);
                if (legit.isEmpty()) {
                    // no class files can be generated
                    success = true;
                } else {
                    // re-wrap with legit imports
                    legit.stream().forEach(u -> u.setWrap(ins, legit));

                    // generate class files for those capable
                    Result res = state.taskFactory.compile(outerWrapSet(legit), ct -> {
                        if (!ct.compile()) {
                            // oy! compile failed because of recursive new unresolved
                            if (legit.stream()
                                    .filter(u -> u.smashingErrorDiagnostics(ct))
                                    .count() > 0) {
                                // try again, with the erroreous removed
                                return Result.CONTINUE;
                            } else {
                                state.debug(DBG_GEN, "Should never happen error-less failure - %s\n",
                                        legit);
                            }
                        }

                        // load all new classes
                        load(legit.stream()
                                .flatMap(u -> u.classesToLoad(ct.classList(u.snippet().outerWrap())))
                                .collect(toSet()));
                        // attempt to redefine the remaining classes
                        List<Unit> toReplace = legit.stream()
                                .filter(u -> !u.doRedefines())
                                .toList();

                        // prevent alternating redefine/replace cyclic dependency
                        // loop by replacing all that have been replaced
                        if (!toReplace.isEmpty()) {
                            replaced.addAll(toReplace);
                            replaced.stream().forEach(Unit::markForReplacement);
                            //ensure correct classnames are set in the snippets:
                            replaced.stream().forEach(u -> u.setWrap(ins, legit));
                        }

                        return toReplace.isEmpty() ? Result.SUCESS : Result.FAILURE;
                    });

                    switch (res) {
                        case CONTINUE: continue;
                        case SUCESS: success = true; break;
                        default:
                        case FAILURE: success = false; break;
                    }
                }
                break;
            }

            // add any new dependencies to the working set
            List<Unit> newDependencies = ins.stream()
                    .flatMap(Unit::effectedDependents)
                    .toList();
            state.debug(DBG_GEN, "compileAndLoad %s -- deps: %s  success: %s\n",
                    ins, newDependencies, success);
            if (!ins.addAll(newDependencies) && success) {
                // all classes that could not be directly loaded (because they
                // are new) have been redefined, and no new dependnencies were
                // identified
                ins.stream().forEach(Unit::finish);
                return ins;
            }
        }
    }
    //where:
        enum Result {SUCESS, FAILURE, CONTINUE}

    /**
     * If there are classes to load, loads by calling the execution engine.
     * @param classbytecodes names of the classes to load.
     */
    private void load(Collection<ClassBytecodes> classbytecodes) {
        if (!classbytecodes.isEmpty()) {
            ClassBytecodes[] cbcs = classbytecodes.toArray(new ClassBytecodes[classbytecodes.size()]);
            try {
                state.executionControl().load(cbcs);
                state.classTracker.markLoaded(cbcs);
            } catch (ClassInstallException ex) {
                state.classTracker.markLoaded(cbcs, ex.installed());
            } catch (NotImplementedException ex) {
                state.debug(ex, "Seriously?!? load not implemented");
                state.closeDown();
            } catch (EngineTerminationException ex) {
                state.closeDown();
            }
        }
    }

    private StackTraceElement[] translateExceptionStack(Exception ex) {
        StackTraceElement[] raw = ex.getStackTrace();
        int last = raw.length;
        do {
            if (last == 0) {
                last = raw.length - 1;
                break;
            }
        } while (!isWrap(raw[--last]));
        StackTraceElement[] elems = new StackTraceElement[last + 1];
        for (int i = 0; i <= last; ++i) {
            StackTraceElement r = raw[i];
            OuterSnippetsClassWrap outer = state.outerMap.getOuter(r.getClassName());
            if (outer != null) {
                String klass = expunge(r.getClassName());
                String method = r.getMethodName().equals(DOIT_METHOD_NAME) ? "" : r.getMethodName();
                int wln = r.getLineNumber() - 1;
                int line = outer.wrapLineToSnippetLine(wln) + 1;
                Snippet sn = outer.wrapLineToSnippet(wln);
                String file = "#" + sn.id();
                elems[i] = new StackTraceElement(klass, method, file, line);
            } else if ("<none>".equals(r.getFileName())) {
                elems[i] = new StackTraceElement(r.getClassName(), r.getMethodName(), null, r.getLineNumber());
            } else {
                elems[i] = r;
            }
        }
        return elems;
    }

    private boolean isWrap(StackTraceElement ste) {
        return PREFIX_PATTERN.matcher(ste.getClassName()).find();
    }

    /**
     * Construct a diagnostic for a method name matching an Object method name
     * @param name the method name
     * @param nameStart the position within the source of the method name
     * @return the generated diagnostic
     */
    private Diag objectMethodNameDiag(String name, long nameStart) {
        return new Diag() {
            @Override
            public boolean isError() {
                return true;
            }

            @Override
            public long getPosition() {
                return nameStart;
            }

            @Override
            public long getStartPosition() {
                return nameStart;
            }

            @Override
            public long getEndPosition() {
                return nameStart + name.length();
            }

            @Override
            public String getCode() {
                return "jdk.eval.error.object.method";
            }

            @Override
            public String getMessage(Locale locale) {
                return state.messageFormat("jshell.diag.object.method.fatal",
                        String.join(" ", objectMethods));
            }
        };
    }

    private class ModifierDiagnostic extends Diag {

            final boolean fatal;
            final String message;
            final long start;
            final long end;

            ModifierDiagnostic(boolean fatal,
                    final String message,
                    long start,
                    long end) {
                this.fatal = fatal;
                this.message = message;
                this.start = start;
                this.end = end;
            }

            @Override
            public boolean isError() {
                return fatal;
            }

            @Override
            public long getPosition() {
                return start;
            }

            @Override
            public long getStartPosition() {
                return start;
            }

            @Override
            public long getEndPosition() {
                return end;
            }

            @Override
            public String getCode() {
                return fatal
                        ? "jdk.eval.error.illegal.modifiers"
                        : "jdk.eval.warn.illegal.modifiers";
            }

            @Override
            public String getMessage(Locale locale) {
                return message;
            }
    }

    private DiagList modifierDiagnostics(ModifiersTree modtree,
                                         final TreeDissector dis, boolean isAbstractProhibited) {

        List<Modifier> list = new ArrayList<>();
        boolean fatal = false;
        for (Modifier mod : modtree.getFlags()) {
            switch (mod) {
                case SYNCHRONIZED:
                case NATIVE:
                    list.add(mod);
                    fatal = true;
                    break;
                case ABSTRACT:
                    // for classes, abstract is valid
                    // for variables, generate an error message
                    // for methods, we generate a placeholder method
                    if (isAbstractProhibited) {
                        list.add(mod);
                        fatal = true;
                    }
                    break;
                case PUBLIC:
                case PROTECTED:
                case PRIVATE:
                    // quietly ignore, user cannot see effects one way or the other
                    break;
                case FINAL:
                    //OK to declare an element final
                    //final classes needed for sealed classes
                    break;
                case STATIC:
                    // everything is static -- warning just adds noise when pasting
                    break;
            }
        }
        if (list.isEmpty()) {
            return new DiagList();
        } else {
            StringBuilder sb = new StringBuilder();
            for (Modifier mod : list) {
                sb.append("'");
                sb.append(mod.toString());
                sb.append("' ");
            }
            String key = (list.size() > 1)
                    ? fatal
                    ? "jshell.diag.modifier.plural.fatal"
                    : "jshell.diag.modifier.plural.ignore"
                    : fatal
                    ? "jshell.diag.modifier.single.fatal"
                    : "jshell.diag.modifier.single.ignore";
            String message = state.messageFormat(key, sb.toString().trim());
            return new DiagList(new ModifierDiagnostic(fatal, message,
                    dis.getStartPosition(modtree), dis.getEndPosition(modtree)));
        }
    }

    String computeDeclareName(TypeSymbol ts) {
        return Util.JSHELL_ANONYMOUS + "$" + Long.toUnsignedString(anonCount++);
    }
}
