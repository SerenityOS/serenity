/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import java.util.function.BinaryOperator;
import jdk.jshell.ExpressionToTypeInfo.ExpressionInfo.AnonymousDescription;
import jdk.jshell.ExpressionToTypeInfo.ExpressionInfo.AnonymousDescription.VariableDesc;
import jdk.jshell.TaskFactory.AnalyzeTask;
import jdk.jshell.TypePrinter.AnonymousTypeKind;

/**
 * Compute information about an expression string, particularly its type name.
 */
class ExpressionToTypeInfo {

    private static final String OBJECT_TYPE_NAME = "Object";

    final AnalyzeTask at;
    final CompilationUnitTree cu;
    final JShell state;
    final boolean computeEnhancedInfo;
    final boolean enhancedTypesAccessible;
    final Symtab syms;
    final Types types;
    final Map<TypeSymbol, String> anon2Name = new HashMap<>();

    private ExpressionToTypeInfo(AnalyzeTask at, CompilationUnitTree cu, JShell state,
                                 boolean computeEnhancedInfo, boolean enhancedTypesAccessible) {
        this.at = at;
        this.cu = cu;
        this.state = state;
        this.computeEnhancedInfo = computeEnhancedInfo;
        this.enhancedTypesAccessible = enhancedTypesAccessible;
        this.syms = Symtab.instance(at.context);
        this.types = Types.instance(at.context);
    }

    public static class ExpressionInfo {
        ExpressionTree tree;
        boolean isPrimitiveType;
        String typeName;
        String accessibleTypeName;
        /* In result of localVariableTypeForInitializer, the type that should be used
         * as a declaration type of the field. This does not include intersection types,
         * but does contain references to anonymous types converted to member types.
         */
        String declareTypeName;
        /* In result of localVariableTypeForInitializer, the apparent/infered type of
         * the variable. This includes intersection types, and references to anonymous
         * types converted to member types.
         */
        String fullTypeName;
        /* In result of localVariableTypeForInitializer, the human readable type of
         * the variable. This includes intersection types, and human readable descriptions
         * of anonymous types.
         */
        String displayTypeName;
        boolean isNonVoid;
        /* In result of localVariableTypeForInitializer, description of important anonymous
         * classes.
         */
        List<AnonymousDescription> anonymousClasses = List.nil();

        /* A description of an anonymous class. */
        static class AnonymousDescription {
            /* Parameter types of the invoked super constructor.*/
            List<String> parameterTypes;
            /* Type of the base/enclosing expression, if any.*/
            String enclosingInstanceType;
            /* The denotable name of the supertype.*/
            String superTypeName;
            /* The human-readable name of this class.*/
            String declareTypeName;
            /* If the supertype of this anonymous is a class. */
            boolean isClass;
            /* Variables captured by this anonymous class*/
            List<VariableDesc> capturedVariables;

            static class VariableDesc {
                String type;
                String name;

                public VariableDesc(String type, String name) {
                    this.type = type;
                    this.name = name;
                }

            }
        }
    }

    // return mechanism and other general structure from TreePath.getPath()
    private static class Result extends Error {

        static final long serialVersionUID = -5942088234594905629L;
        @SuppressWarnings("serial") // Not statically typed as Serializable
        final TreePath expressionPath;

        Result(TreePath path) {
            this.expressionPath = path;
        }
    }

    private static class PathFinder extends TreePathScanner<TreePath, Boolean> {

        // Optimize out imports etc
        @Override
        public TreePath visitCompilationUnit(CompilationUnitTree node, Boolean isTargetContext) {
            return scan(node.getTypeDecls(), isTargetContext);
        }

        // Only care about members
        @Override
        public TreePath visitClass(ClassTree node, Boolean isTargetContext) {
            return scan(node.getMembers(), isTargetContext);
        }

        // Only want the doit method where the code is
        @Override
        public TreePath visitMethod(MethodTree node, Boolean isTargetContext) {
            if (Util.isDoIt(node.getName())) {
                return scan(node.getBody(), true);
            } else {
                return null;
            }
        }

        @Override
        public TreePath visitReturn(ReturnTree node, Boolean isTargetContext) {
            ExpressionTree tree = node.getExpression();
            TreePath tp = new TreePath(getCurrentPath(), tree);
            if (isTargetContext) {
                throw new Result(tp);
            } else {
                return null;
            }
        }

        @Override
        public TreePath visitVariable(VariableTree node, Boolean isTargetContext) {
            if (isTargetContext) {
                throw new Result(getCurrentPath());
            } else {
                return null;
            }
        }

    }

    private Type pathToType(TreePath tp) {
        return (Type) at.trees().getTypeMirror(tp);
    }

    private Type pathToType(TreePath tp, Tree tree) {
        if (tree instanceof ConditionalExpressionTree) {
            // Conditionals always wind up as Object -- this corrects
            ConditionalExpressionTree cet = (ConditionalExpressionTree) tree;
            Type tmt = pathToType(new TreePath(tp, cet.getTrueExpression()));
            Type tmf = pathToType(new TreePath(tp, cet.getFalseExpression()));
            if (!tmt.isPrimitive() && !tmf.isPrimitive()) {
                Type lub = types.lub(tmt, tmf);
                // System.err.printf("cond ? %s : %s  --  lub = %s\n",
                //             varTypeName(tmt), varTypeName(tmf), varTypeName(lub));
                return lub;
            }
        }
        return pathToType(tp);
    }

    /**
     * Entry method: get expression info
     * @param code the expression as a string
     * @param state a JShell instance
     * @return type information
     */
    public static ExpressionInfo expressionInfo(String code, JShell state) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        OuterWrap codeWrap = state.outerMap.wrapInTrialClass(Wrap.methodReturnWrap(code));
        try {
            return state.taskFactory.analyze(codeWrap, at -> {
                CompilationUnitTree cu = at.firstCuTree();
                if (at.hasErrors() || cu == null) {
                    return null;
                }
                return new ExpressionToTypeInfo(at, cu, state, false, false).typeOfExpression();
            });
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Entry method: get expression info corresponding to a local variable declaration if its type
     * has been inferred automatically from the given initializer.
     * @param code the initializer as a string
     * @param state a JShell instance
     * @return type information
     */
    public static ExpressionInfo localVariableTypeForInitializer(String code, JShell state, boolean onlyAccessible) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        try {
            OuterWrap codeWrap = state.outerMap.wrapInTrialClass(Wrap.methodWrap("var $$$ = " + code));
            return state.taskFactory.analyze(codeWrap, at -> {
                CompilationUnitTree cu = at.firstCuTree();
                if (at.hasErrors() || cu == null) {
                    return null;
                }
                return new ExpressionToTypeInfo(at, cu, state, true, onlyAccessible)
                        .typeOfExpression();
            });
        } catch (Exception ex) {
            return null;
        }
    }

    /**List (in a stable order) all NewClassTree instances under {@code from} that should be
     * converted to member classes
     *
     * @param from tree to inspect
     * @return NewClassTree instances that should be converted to member classes
     */
    public static List<NewClassTree> listAnonymousClassesToConvert(Tree from) {
        ListBuffer<NewClassTree> classes = new ListBuffer<>();

        new TreeScanner<Void, Void>() {
            @Override
            public Void visitNewClass(NewClassTree node, Void p) {
                if (node.getClassBody() != null) {
                    classes.append(node);
                    return null;
                }
                return super.visitNewClass(node, p);
            }
        }.scan(from, null);

        return classes.toList();
    }

    private ExpressionInfo typeOfExpression() {
        return treeToInfo(findExpressionPath());
    }

    private TreePath findExpressionPath() {
        try {
            new PathFinder().scan(new TreePath(cu), false);
        } catch (Result result) {
            return result.expressionPath;
        }
        return null;
    }

    /**
     * A type is accessible if it is public or if it is package-private and is a
     * type defined in JShell.  Additionally, all its type arguments must be
     * accessible
     *
     * @param type the type to check for accessibility
     * @return true if the type name can be referenced
     */
    private boolean isAccessible(Type type) {
        Symbol.TypeSymbol tsym = type.asElement();
        return ((tsym.flags() & Flags.PUBLIC) != 0 ||
                ((tsym.flags() & Flags.PRIVATE) == 0 &&
                Util.isInJShellClass(tsym.flatName().toString()))) &&
                 type.getTypeArguments().stream()
                        .allMatch(this::isAccessible);
    }

    /**
     * Return the superclass.
     *
     * @param type the type
     * @return the superclass, or Object on error
     */
    private Type supertype(Type type) {
        Type sup = types.supertype(type);
        if (sup == Type.noType || sup == null) {
            return syms.objectType;
        }
        return sup;
    }

    /**
     * Find an accessible supertype.
     *
     * @param type the type
     * @return the type, if it is accessible, otherwise a superclass or
     * interface which is
     */
    private List<Type> findAccessibleSupertypes(Type type) {
        List<Type> accessible = List.nil();
        Type accessibleSuper = syms.objectType;
        // Iterate up the superclasses, see if any are accessible
        for (Type sup = type; !types.isSameType(sup, syms.objectType); sup = supertype(sup)) {
            if (isAccessible(sup)) {
                accessible = accessible.prepend(sup);
                accessibleSuper = sup;
                break;
            }
        }
        // then look through superclasses for accessible interfaces
        for (Type sup = type; !types.isSameType(sup, accessibleSuper); sup = supertype(sup)) {
            for (Type itf : types.interfaces(sup)) {
                if (isAccessible(itf)) {
                    accessible = accessible.prepend(itf);
                }
            }
        }
        if (accessible.isEmpty()) {
            // Punt, use Object which is the supertype of everything
            accessible = accessible.prepend(syms.objectType);
        }

        return accessible.reverse();
    }

    private ExpressionInfo treeToInfo(TreePath tp) {
        if (tp != null) {
            Tree tree = tp.getLeaf();
            boolean isExpression = tree instanceof ExpressionTree;
            if (isExpression || tree.getKind() == Kind.VARIABLE) {
                ExpressionInfo ei = new ExpressionInfo();
                if (isExpression)
                    ei.tree = (ExpressionTree) tree;
                Type type = pathToType(tp, tree);
                if (type != null) {
                    switch (type.getKind()) {
                        case VOID:
                        case NONE:
                        case ERROR:
                        case OTHER:
                            break;
                        case NULL:
                            ei.isNonVoid = true;
                            ei.typeName = OBJECT_TYPE_NAME;
                            ei.accessibleTypeName = OBJECT_TYPE_NAME;
                            break;
                        default: {
                            ei.isNonVoid = true;
                            ei.isPrimitiveType = type.isPrimitive();
                            ei.typeName = varTypeName(type, false, AnonymousTypeKind.SUPER);
                            List<Type> accessibleTypes = findAccessibleSupertypes(type);
                            ei.accessibleTypeName =
                                    varTypeName(accessibleTypes.head, false, AnonymousTypeKind.SUPER);
                            if (computeEnhancedInfo) {
                                Type accessibleType = accessibleTypes.size() == 1 ? accessibleTypes.head
                                            : types.makeIntersectionType(accessibleTypes);
                                ei.declareTypeName =
                                        varTypeName(accessibleType, (full, pkg) -> full, false, AnonymousTypeKind.DECLARE);
                                ei.fullTypeName =
                                        varTypeName(enhancedTypesAccessible ? accessibleType : type, (full, pkg) -> full,
                                                    true, AnonymousTypeKind.DECLARE);
                                ei.displayTypeName =
                                        varTypeName(type, true, AnonymousTypeKind.DISPLAY);
                            }
                            break;
                        }
                    }
                }
                if (tree.getKind() == Tree.Kind.VARIABLE && computeEnhancedInfo) {
                    Tree init = ((VariableTree) tree).getInitializer();
                    for (NewClassTree node : listAnonymousClassesToConvert(init)) {
                        Set<VariableElement> captured = capturedVariables(at,
                                                                          tp.getCompilationUnit(),
                                                                          node);
                        JCClassDecl clazz = (JCClassDecl) node.getClassBody();
                        MethodInvocationTree superCall =
                                clazz.getMembers()
                                     .stream()
                                     .map(TreeInfo::firstConstructorCall)
                                     .findAny()
                                     .get();
                        TreePath superCallPath
                                = at.trees().
                                        getPath(tp.getCompilationUnit(), superCall.
                                                getMethodSelect());
                        Type constrType = pathToType(superCallPath);
                        AnonymousDescription desc = new AnonymousDescription();
                        desc.parameterTypes = constrType.getParameterTypes().
                                stream().
                                map(t -> varTypeName(t, false, AnonymousTypeKind.DECLARE)).
                                collect(List.collector());
                        if (node.getEnclosingExpression() != null) {
                            TreePath enclPath = new TreePath(tp,
                                                             node.getEnclosingExpression());
                            desc.enclosingInstanceType = varTypeName(pathToType(enclPath),
                                                                     false,
                                                                     AnonymousTypeKind.DECLARE);
                        }
                        TreePath currentPath = at.trees()
                                                 .getPath(tp.getCompilationUnit(),
                                                          node);
                        Type nodeType = pathToType(currentPath, node);
                        desc.superTypeName = varTypeName(nodeType,
                                                         false,
                                                         AnonymousTypeKind.SUPER);
                        desc.declareTypeName = varTypeName(nodeType,
                                                           true, AnonymousTypeKind.DECLARE);
                        desc.capturedVariables =
                                captured.stream()
                                        .map(ve -> new VariableDesc(varTypeName((Type) ve.asType(),
                                                                                false,
                                                                                AnonymousTypeKind.DECLARE),
                                                                    ve.getSimpleName().toString()))
                                        .collect(List.collector());

                        desc.isClass = at.task.getTypes().directSupertypes(nodeType).size() == 1;
                        ei.anonymousClasses = ei.anonymousClasses.prepend(desc);
                    }
                    ei.anonymousClasses = ei.anonymousClasses.reverse();
                }
                return ei;
            }
        }
        return null;
    }
    //where:
        private static Set<VariableElement> capturedVariables(AnalyzeTask at,
                                                              CompilationUnitTree topLevel,
                                                              Tree tree) {
            Set<VariableElement> capturedVars = new HashSet<>();
            new TreeScanner<Void, Void>() {
                Set<VariableElement> declaredLocalVars = new HashSet<>();
                @Override
                public Void visitVariable(VariableTree node, Void p) {
                    TreePath currentPath = at.trees()
                                             .getPath(topLevel, node);
                    declaredLocalVars.add((VariableElement) at.trees().getElement(currentPath));
                    return super.visitVariable(node, p);
                }

                @Override
                public Void visitIdentifier(IdentifierTree node, Void p) {
                    TreePath currentPath = at.trees()
                                             .getPath(topLevel, node);
                    Element el = at.trees().getElement(currentPath);
                    if (el != null &&
                        LOCAL_VARIABLES.contains(el.getKind()) &&
                        !declaredLocalVars.contains(el)) {
                        capturedVars.add((VariableElement) el);
                    }
                    return super.visitIdentifier(node, p);
                }
            }.scan(tree, null);

            return capturedVars;
        }
        private static final Set<ElementKind> LOCAL_VARIABLES =
                EnumSet.of(ElementKind.EXCEPTION_PARAMETER, ElementKind.LOCAL_VARIABLE,
                           ElementKind.PARAMETER, ElementKind.RESOURCE_VARIABLE);

    private String varTypeName(Type type, boolean printIntersectionTypes, AnonymousTypeKind anonymousTypesKind) {
        return varTypeName(type, state.maps::fullClassNameAndPackageToClass, printIntersectionTypes, anonymousTypesKind);
    }

    private String varTypeName(Type type, BinaryOperator<String> fullClassNameAndPackageToClass, boolean printIntersectionTypes, AnonymousTypeKind anonymousTypesKind) {
        try {
            Function<TypeSymbol, String> anonymousClass2DeclareName =
                    cs -> anon2Name.computeIfAbsent(cs, state.eval::computeDeclareName);
            TypePrinter tp = new TypePrinter(at.messages(), at.types(),
                    fullClassNameAndPackageToClass, anonymousClass2DeclareName,
                    printIntersectionTypes, anonymousTypesKind);
            List<Type> captures = types.captures(type);
            String res = tp.toString(types.upward(type, captures));

            if (res == null)
                res = OBJECT_TYPE_NAME;

            return res;
        } catch (Exception ex) {
            return OBJECT_TYPE_NAME;
        }
    }

}
