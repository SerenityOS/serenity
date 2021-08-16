/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.Kinds.KindSelector;
import com.sun.tools.javac.code.Scope.WriteableScope;
import com.sun.tools.javac.code.Source.Feature;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.TypeMetadata.Entry.Kind;
import com.sun.tools.javac.comp.Check.CheckContext;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.resources.CompilerProperties.Fragments;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;

import javax.tools.JavaFileObject;

import java.util.*;

import static com.sun.tools.javac.code.Flags.SYNTHETIC;
import static com.sun.tools.javac.code.Kinds.Kind.MDL;
import static com.sun.tools.javac.code.Kinds.Kind.MTH;
import static com.sun.tools.javac.code.Kinds.Kind.PCK;
import static com.sun.tools.javac.code.Kinds.Kind.TYP;
import static com.sun.tools.javac.code.Kinds.Kind.VAR;
import static com.sun.tools.javac.code.Scope.LookupKind.NON_RECURSIVE;
import static com.sun.tools.javac.code.TypeTag.ARRAY;
import static com.sun.tools.javac.code.TypeTag.CLASS;
import static com.sun.tools.javac.tree.JCTree.Tag.ANNOTATION;
import static com.sun.tools.javac.tree.JCTree.Tag.ASSIGN;
import static com.sun.tools.javac.tree.JCTree.Tag.IDENT;
import static com.sun.tools.javac.tree.JCTree.Tag.NEWARRAY;

import com.sun.tools.javac.util.JCDiagnostic.DiagnosticFlag;


/** Enter annotations onto symbols and types (and trees).
 *
 *  This is also a pseudo stage in the compiler taking care of scheduling when annotations are
 *  entered.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Annotate {
    protected static final Context.Key<Annotate> annotateKey = new Context.Key<>();

    public static Annotate instance(Context context) {
        Annotate instance = context.get(annotateKey);
        if (instance == null)
            instance = new Annotate(context);
        return instance;
    }

    private final Attr attr;
    private final Check chk;
    private final ConstFold cfolder;
    private final DeferredLintHandler deferredLintHandler;
    private final Enter enter;
    private final Lint lint;
    private final Log log;
    private final Names names;
    private final Resolve resolve;
    private final TreeMaker make;
    private final Symtab syms;
    private final TypeEnvs typeEnvs;
    private final Types types;

    private final Attribute theUnfinishedDefaultValue;
    private final boolean allowRepeatedAnnos;
    private final String sourceName;

    protected Annotate(Context context) {
        context.put(annotateKey, this);

        attr = Attr.instance(context);
        chk = Check.instance(context);
        cfolder = ConstFold.instance(context);
        deferredLintHandler = DeferredLintHandler.instance(context);
        enter = Enter.instance(context);
        log = Log.instance(context);
        lint = Lint.instance(context);
        make = TreeMaker.instance(context);
        names = Names.instance(context);
        resolve = Resolve.instance(context);
        syms = Symtab.instance(context);
        typeEnvs = TypeEnvs.instance(context);
        types = Types.instance(context);

        theUnfinishedDefaultValue =  new Attribute.Error(syms.errType);

        Source source = Source.instance(context);
        allowRepeatedAnnos = Feature.REPEATED_ANNOTATIONS.allowedInSource(source);
        sourceName = source.name;

        blockCount = 1;
    }

    /** Semaphore to delay annotation processing */
    private int blockCount = 0;

    /** Called when annotations processing needs to be postponed. */
    public void blockAnnotations() {
        blockCount++;
    }

    /** Called when annotation processing can be resumed. */
    public void unblockAnnotations() {
        blockCount--;
        if (blockCount == 0)
            flush();
    }

    /** Variant which allows for a delayed flush of annotations.
     * Needed by ClassReader */
    public void unblockAnnotationsNoFlush() {
        blockCount--;
    }

    /** are we blocking annotation processing? */
    public boolean annotationsBlocked() {return blockCount > 0; }

    public void enterDone() {
        unblockAnnotations();
    }

    public List<TypeCompound> fromAnnotations(List<JCAnnotation> annotations) {
        if (annotations.isEmpty()) {
            return List.nil();
        }

        ListBuffer<TypeCompound> buf = new ListBuffer<>();
        for (JCAnnotation anno : annotations) {
            Assert.checkNonNull(anno.attribute);
            buf.append((TypeCompound) anno.attribute);
        }
        return buf.toList();
    }

    /** Annotate (used for everything else) */
    public void normal(Runnable r) {
        q.append(r);
    }

    /** Validate, triggers after 'normal' */
    public void validate(Runnable a) {
        validateQ.append(a);
    }

    /** Flush all annotation queues */
    public void flush() {
        if (annotationsBlocked()) return;
        if (isFlushing()) return;

        startFlushing();
        try {
            while (q.nonEmpty()) {
                q.next().run();
            }
            while (typesQ.nonEmpty()) {
                typesQ.next().run();
            }
            while (afterTypesQ.nonEmpty()) {
                afterTypesQ.next().run();
            }
            while (validateQ.nonEmpty()) {
                validateQ.next().run();
            }
        } finally {
            doneFlushing();
        }
    }

    private ListBuffer<Runnable> q = new ListBuffer<>();
    private ListBuffer<Runnable> validateQ = new ListBuffer<>();

    private int flushCount = 0;
    private boolean isFlushing() { return flushCount > 0; }
    private void startFlushing() { flushCount++; }
    private void doneFlushing() { flushCount--; }

    ListBuffer<Runnable> typesQ = new ListBuffer<>();
    ListBuffer<Runnable> afterTypesQ = new ListBuffer<>();


    public void typeAnnotation(Runnable a) {
        typesQ.append(a);
    }

    public void afterTypes(Runnable a) {
        afterTypesQ.append(a);
    }

    /**
     * Queue annotations for later attribution and entering. This is probably the method you are looking for.
     *
     * @param annotations the list of JCAnnotations to attribute and enter
     * @param localEnv    the enclosing env
     * @param s           ths Symbol on which to enter the annotations
     * @param deferPos    report errors here
     */
    public void annotateLater(List<JCAnnotation> annotations, Env<AttrContext> localEnv,
            Symbol s, DiagnosticPosition deferPos)
    {
        if (annotations.isEmpty()) {
            return;
        }

        s.resetAnnotations(); // mark Annotations as incomplete for now

        normal(() -> {
            // Packages are unusual, in that they are the only type of declaration that can legally appear
            // more than once in a compilation, and in all cases refer to the same underlying symbol.
            // This means they are the only kind of declaration that syntactically may have multiple sets
            // of annotations, each on a different package declaration, even though that is ultimately
            // forbidden by JLS 8 section 7.4.
            // The corollary here is that all of the annotations on a package symbol may have already
            // been handled, meaning that the set of annotations pending completion is now empty.
            Assert.check(s.kind == PCK || s.annotationsPendingCompletion());
            JavaFileObject prev = log.useSource(localEnv.toplevel.sourcefile);
            DiagnosticPosition prevLintPos =
                    deferPos != null
                            ? deferredLintHandler.setPos(deferPos)
                            : deferredLintHandler.immediate();
            Lint prevLint = deferPos != null ? null : chk.setLint(lint);
            try {
                if (s.hasAnnotations() && annotations.nonEmpty())
                    log.error(annotations.head.pos, Errors.AlreadyAnnotated(Kinds.kindName(s), s));

                Assert.checkNonNull(s, "Symbol argument to actualEnterAnnotations is null");

                // false is passed as fifth parameter since annotateLater is
                // never called for a type parameter
                annotateNow(s, annotations, localEnv, false, false);
            } finally {
                if (prevLint != null)
                    chk.setLint(prevLint);
                deferredLintHandler.setPos(prevLintPos);
                log.useSource(prev);
            }
        });

        validate(() -> { //validate annotations
            JavaFileObject prev = log.useSource(localEnv.toplevel.sourcefile);
            try {
                chk.validateAnnotations(annotations, TreeInfo.declarationFor(s, localEnv.tree), s);
            } finally {
                log.useSource(prev);
            }
        });
    }


    /** Queue processing of an attribute default value. */
    public void annotateDefaultValueLater(JCExpression defaultValue, Env<AttrContext> localEnv,
            MethodSymbol m, DiagnosticPosition deferPos)
    {
        normal(() -> {
            JavaFileObject prev = log.useSource(localEnv.toplevel.sourcefile);
            DiagnosticPosition prevLintPos = deferredLintHandler.setPos(deferPos);
            try {
                enterDefaultValue(defaultValue, localEnv, m);
            } finally {
                deferredLintHandler.setPos(prevLintPos);
                log.useSource(prev);
            }
        });

        validate(() -> { //validate annotations
            JavaFileObject prev = log.useSource(localEnv.toplevel.sourcefile);
            try {
                // if default value is an annotation, check it is a well-formed
                // annotation value (e.g. no duplicate values, no missing values, etc.)
                chk.validateAnnotationTree(defaultValue);
            } finally {
                log.useSource(prev);
            }
        });
    }

    /** Enter a default value for an annotation element. */
    private void enterDefaultValue(JCExpression defaultValue,
            Env<AttrContext> localEnv, MethodSymbol m) {
        m.defaultValue = attributeAnnotationValue(m.type.getReturnType(), defaultValue, localEnv);
    }

    /**
     * Gather up annotations into a map from type symbols to lists of Compound attributes,
     * then continue on with repeating annotations processing.
     */
    private <T extends Attribute.Compound> void annotateNow(Symbol toAnnotate,
            List<JCAnnotation> withAnnotations, Env<AttrContext> env, boolean typeAnnotations,
            boolean isTypeParam)
    {
        Map<TypeSymbol, ListBuffer<T>> annotated = new LinkedHashMap<>();
        Map<T, DiagnosticPosition> pos = new HashMap<>();

        for (List<JCAnnotation> al = withAnnotations; !al.isEmpty(); al = al.tail) {
            JCAnnotation a = al.head;

            T c;
            if (typeAnnotations) {
                @SuppressWarnings("unchecked")
                T tmp = (T)attributeTypeAnnotation(a, syms.annotationType, env);
                c = tmp;
            } else {
                @SuppressWarnings("unchecked")
                T tmp = (T)attributeAnnotation(a, syms.annotationType, env);
                c = tmp;
            }

            Assert.checkNonNull(c, "Failed to create annotation");

            if (a.type.tsym.isAnnotationType()) {
                if (annotated.containsKey(a.type.tsym)) {
                    if (!allowRepeatedAnnos) {
                        log.error(DiagnosticFlag.SOURCE_LEVEL, a.pos(), Feature.REPEATED_ANNOTATIONS.error(sourceName));
                    }
                    ListBuffer<T> l = annotated.get(a.type.tsym);
                    l = l.append(c);
                    annotated.put(a.type.tsym, l);
                    pos.put(c, a.pos());
                } else {
                    annotated.put(a.type.tsym, ListBuffer.of(c));
                    pos.put(c, a.pos());
                }
            }

            // Note: @Deprecated has no effect on local variables and parameters
            if (!c.type.isErroneous()
                    && (toAnnotate.kind == MDL || toAnnotate.owner.kind != MTH)
                    && types.isSameType(c.type, syms.deprecatedType)) {
                toAnnotate.flags_field |= (Flags.DEPRECATED | Flags.DEPRECATED_ANNOTATION);
                if (isAttributeTrue(c.member(names.forRemoval))) {
                    toAnnotate.flags_field |= Flags.DEPRECATED_REMOVAL;
                }
            }

            if (!c.type.isErroneous()
                    && types.isSameType(c.type, syms.previewFeatureType)) {
                toAnnotate.flags_field |= Flags.PREVIEW_API;
                if (isAttributeTrue(c.member(names.reflective))) {
                    toAnnotate.flags_field |= Flags.PREVIEW_REFLECTIVE;
                }
            }

            if (!c.type.isErroneous()
                    && toAnnotate.kind == TYP
                    && types.isSameType(c.type, syms.valueBasedType)) {
                toAnnotate.flags_field |= Flags.VALUE_BASED;
            }
        }

        List<T> buf = List.nil();
        for (ListBuffer<T> lb : annotated.values()) {
            if (lb.size() == 1) {
                buf = buf.prepend(lb.first());
            } else {
                AnnotationContext<T> ctx = new AnnotationContext<>(env, annotated, pos, typeAnnotations);
                T res = makeContainerAnnotation(lb.toList(), ctx, toAnnotate, isTypeParam);
                if (res != null)
                    buf = buf.prepend(res);
            }
        }

        if (typeAnnotations) {
            @SuppressWarnings("unchecked")
            List<TypeCompound> attrs = (List<TypeCompound>)buf.reverse();
            toAnnotate.appendUniqueTypeAttributes(attrs);
        } else {
            @SuppressWarnings("unchecked")
            List<Attribute.Compound> attrs =  (List<Attribute.Compound>)buf.reverse();
            toAnnotate.resetAnnotations();
            toAnnotate.setDeclarationAttributes(attrs);
        }
    }
    //where:
        private boolean isAttributeTrue(Attribute attr) {
            return (attr instanceof Attribute.Constant constant)
                    && constant.type == syms.booleanType
                    && ((Integer) constant.value) != 0;
        }

    /**
     * Attribute and store a semantic representation of the annotation tree {@code tree} into the
     * tree.attribute field.
     *
     * @param tree the tree representing an annotation
     * @param expectedAnnotationType the expected (super)type of the annotation
     * @param env the current env in where the annotation instance is found
     */
    public Attribute.Compound attributeAnnotation(JCAnnotation tree, Type expectedAnnotationType,
                                                  Env<AttrContext> env)
    {
        // The attribute might have been entered if it is Target or Repeatable
        // Because TreeCopier does not copy type, redo this if type is null
        if (tree.attribute != null && tree.type != null)
            return tree.attribute;

        List<Pair<MethodSymbol, Attribute>> elems = attributeAnnotationValues(tree, expectedAnnotationType, env);
        Attribute.Compound ac = new Attribute.Compound(tree.type, elems);

        return tree.attribute = ac;
    }

    /** Attribute and store a semantic representation of the type annotation tree {@code tree} into
     * the tree.attribute field.
     *
     * @param a the tree representing an annotation
     * @param expectedAnnotationType the expected (super)type of the annotation
     * @param env the the current env in where the annotation instance is found
     */
    public Attribute.TypeCompound attributeTypeAnnotation(JCAnnotation a, Type expectedAnnotationType,
                                                          Env<AttrContext> env)
    {
        // The attribute might have been entered if it is Target or Repeatable
        // Because TreeCopier does not copy type, redo this if type is null
        if (a.attribute == null || a.type == null || !(a.attribute instanceof Attribute.TypeCompound typeCompound)) {
            // Create a new TypeCompound
            List<Pair<MethodSymbol,Attribute>> elems =
                    attributeAnnotationValues(a, expectedAnnotationType, env);

            Attribute.TypeCompound tc =
                    new Attribute.TypeCompound(a.type, elems, TypeAnnotationPosition.unknown);
            a.attribute = tc;
            return tc;
        } else {
            // Use an existing TypeCompound
            return typeCompound;
        }
    }

    /**
     *  Attribute annotation elements creating a list of pairs of the Symbol representing that
     *  element and the value of that element as an Attribute. */
    private List<Pair<MethodSymbol, Attribute>> attributeAnnotationValues(JCAnnotation a,
            Type expected, Env<AttrContext> env)
    {
        // The annotation might have had its type attributed (but not
        // checked) by attr.attribAnnotationTypes during MemberEnter,
        // in which case we do not need to do it again.
        Type at = (a.annotationType.type != null ?
                a.annotationType.type : attr.attribType(a.annotationType, env));
        a.type = chk.checkType(a.annotationType.pos(), at, expected);

        boolean isError = a.type.isErroneous();
        if (!a.type.tsym.isAnnotationType() && !isError) {
            log.error(a.annotationType.pos(), Errors.NotAnnotationType(a.type));
            isError = true;
        }

        // List of name=value pairs (or implicit "value=" if size 1)
        List<JCExpression> args = a.args;

        boolean elidedValue = false;
        // special case: elided "value=" assumed
        if (args.length() == 1 && !args.head.hasTag(ASSIGN)) {
            args.head = make.at(args.head.pos).
                    Assign(make.Ident(names.value), args.head);
            elidedValue = true;
        }

        ListBuffer<Pair<MethodSymbol,Attribute>> buf = new ListBuffer<>();
        for (List<JCExpression> tl = args; tl.nonEmpty(); tl = tl.tail) {
            Pair<MethodSymbol, Attribute> p = attributeAnnotationNameValuePair(tl.head, a.type, isError, env, elidedValue);
            if (p != null && !p.fst.type.isErroneous())
                buf.append(p);
        }
        return buf.toList();
    }

    // where
    private Pair<MethodSymbol, Attribute> attributeAnnotationNameValuePair(JCExpression nameValuePair,
            Type thisAnnotationType, boolean badAnnotation, Env<AttrContext> env, boolean elidedValue)
    {
        if (!nameValuePair.hasTag(ASSIGN)) {
            log.error(nameValuePair.pos(), Errors.AnnotationValueMustBeNameValue);
            attributeAnnotationValue(nameValuePair.type = syms.errType, nameValuePair, env);
            return null;
        }
        JCAssign assign = (JCAssign)nameValuePair;
        if (!assign.lhs.hasTag(IDENT)) {
            log.error(nameValuePair.pos(), Errors.AnnotationValueMustBeNameValue);
            attributeAnnotationValue(nameValuePair.type = syms.errType, nameValuePair, env);
            return null;
        }

        // Resolve element to MethodSym
        JCIdent left = (JCIdent)assign.lhs;
        Symbol method = resolve.resolveQualifiedMethod(elidedValue ? assign.rhs.pos() : left.pos(),
                env, thisAnnotationType,
                left.name, List.nil(), null);
        left.sym = method;
        left.type = method.type;
        if (method.owner != thisAnnotationType.tsym && !badAnnotation)
            log.error(left.pos(), Errors.NoAnnotationMember(left.name, thisAnnotationType));
        Type resultType = method.type.getReturnType();

        // Compute value part
        Attribute value = attributeAnnotationValue(resultType, assign.rhs, env);
        nameValuePair.type = resultType;

        return method.type.isErroneous() ? null : new Pair<>((MethodSymbol)method, value);

    }

    /** Attribute an annotation element value */
    private Attribute attributeAnnotationValue(Type expectedElementType, JCExpression tree,
            Env<AttrContext> env)
    {
        //first, try completing the symbol for the annotation value - if a completion
        //error is thrown, we should recover gracefully, and display an
        //ordinary resolution diagnostic.
        try {
            expectedElementType.tsym.complete();
        } catch(CompletionFailure e) {
            log.error(tree.pos(), Errors.CantResolve(Kinds.kindName(e.sym), e.sym.getQualifiedName(), null, null));
            expectedElementType = syms.errType;
        }

        if (expectedElementType.hasTag(ARRAY)) {
            return getAnnotationArrayValue(expectedElementType, tree, env);

        }

        //error recovery
        if (tree.hasTag(NEWARRAY)) {
            if (!expectedElementType.isErroneous())
                log.error(tree.pos(), Errors.AnnotationValueNotAllowableType);
            JCNewArray na = (JCNewArray)tree;
            if (na.elemtype != null) {
                log.error(na.elemtype.pos(), Errors.NewNotAllowedInAnnotation);
            }
            for (List<JCExpression> l = na.elems; l.nonEmpty(); l=l.tail) {
                attributeAnnotationValue(syms.errType,
                        l.head,
                        env);
            }
            return new Attribute.Error(syms.errType);
        }

        if (expectedElementType.tsym.isAnnotationType()) {
            if (tree.hasTag(ANNOTATION)) {
                return attributeAnnotation((JCAnnotation)tree, expectedElementType, env);
            } else {
                log.error(tree.pos(), Errors.AnnotationValueMustBeAnnotation);
                expectedElementType = syms.errType;
            }
        }

        //error recovery
        if (tree.hasTag(ANNOTATION)) {
            if (!expectedElementType.isErroneous())
                log.error(tree.pos(), Errors.AnnotationNotValidForType(expectedElementType));
            attributeAnnotation((JCAnnotation)tree, syms.errType, env);
            return new Attribute.Error(((JCAnnotation)tree).annotationType.type);
        }

        MemberEnter.InitTreeVisitor initTreeVisitor = new MemberEnter.InitTreeVisitor() {
            // the methods below are added to allow class literals on top of constant expressions
            @Override
            public void visitTypeIdent(JCPrimitiveTypeTree that) {}

            @Override
            public void visitTypeArray(JCArrayTypeTree that) {}
        };
        tree.accept(initTreeVisitor);
        if (!initTreeVisitor.result) {
            log.error(tree.pos(), Errors.ExpressionNotAllowableAsAnnotationValue);
            return new Attribute.Error(syms.errType);
        }

        if (expectedElementType.isPrimitive() ||
                (types.isSameType(expectedElementType, syms.stringType) && !expectedElementType.hasTag(TypeTag.ERROR))) {
            return getAnnotationPrimitiveValue(expectedElementType, tree, env);
        }

        if (expectedElementType.tsym == syms.classType.tsym) {
            return getAnnotationClassValue(expectedElementType, tree, env);
        }

        if (expectedElementType.hasTag(CLASS) &&
                (expectedElementType.tsym.flags() & Flags.ENUM) != 0) {
            return getAnnotationEnumValue(expectedElementType, tree, env);
        }

        //error recovery:
        if (!expectedElementType.isErroneous())
            log.error(tree.pos(), Errors.AnnotationValueNotAllowableType);
        return new Attribute.Error(attr.attribExpr(tree, env, expectedElementType));
    }

    private Attribute getAnnotationEnumValue(Type expectedElementType, JCExpression tree, Env<AttrContext> env) {
        Type result = attr.attribTree(tree, env, annotationValueInfo(expectedElementType));
        Symbol sym = TreeInfo.symbol(tree);
        if (sym == null ||
                TreeInfo.nonstaticSelect(tree) ||
                sym.kind != VAR ||
                (sym.flags() & Flags.ENUM) == 0) {
            log.error(tree.pos(), Errors.EnumAnnotationMustBeEnumConstant);
            return new Attribute.Error(result.getOriginalType());
        }
        VarSymbol enumerator = (VarSymbol) sym;
        return new Attribute.Enum(expectedElementType, enumerator);
    }

    private Attribute getAnnotationClassValue(Type expectedElementType, JCExpression tree, Env<AttrContext> env) {
        Type result = attr.attribTree(tree, env, annotationValueInfo(expectedElementType));
        if (result.isErroneous()) {
            // Does it look like an unresolved class literal?
            if (TreeInfo.name(tree) == names._class &&
                    ((JCFieldAccess) tree).selected.type.isErroneous()) {
                Name n = (((JCFieldAccess) tree).selected).type.tsym.flatName();
                return new Attribute.UnresolvedClass(expectedElementType,
                        types.createErrorType(n,
                                syms.unknownSymbol, syms.classType));
            } else {
                return new Attribute.Error(result.getOriginalType());
            }
        }

        // Class literals look like field accesses of a field named class
        // at the tree level
        if (TreeInfo.name(tree) != names._class) {
            log.error(tree.pos(), Errors.AnnotationValueMustBeClassLiteral);
            return new Attribute.Error(syms.errType);
        }

        return new Attribute.Class(types,
                (((JCFieldAccess) tree).selected).type);
    }

    private Attribute getAnnotationPrimitiveValue(Type expectedElementType, JCExpression tree, Env<AttrContext> env) {
        Type result = attr.attribTree(tree, env, annotationValueInfo(expectedElementType));
        if (result.isErroneous())
            return new Attribute.Error(result.getOriginalType());
        if (result.constValue() == null) {
            log.error(tree.pos(), Errors.AttributeValueMustBeConstant);
            return new Attribute.Error(expectedElementType);
        }
        result = cfolder.coerce(result, expectedElementType);
        return new Attribute.Constant(expectedElementType, result.constValue());
    }

    private Attr.ResultInfo annotationValueInfo(Type pt) {
        return attr.unknownExprInfo.dup(pt, new AnnotationValueContext(attr.unknownExprInfo.checkContext));
    }

    class AnnotationValueContext extends Check.NestedCheckContext {
        AnnotationValueContext(CheckContext enclosingContext) {
            super(enclosingContext);
        }

        @Override
        public boolean compatible(Type found, Type req, Warner warn) {
            //handle non-final implicitly-typed vars (will be rejected later on)
            return found.hasTag(TypeTag.NONE) || super.compatible(found, req, warn);
        }
    }

    private Attribute getAnnotationArrayValue(Type expectedElementType, JCExpression tree, Env<AttrContext> env) {
        // Special case, implicit array
        if (!tree.hasTag(NEWARRAY)) {
            tree = make.at(tree.pos).
                    NewArray(null, List.nil(), List.of(tree));
        }

        JCNewArray na = (JCNewArray)tree;
        if (na.elemtype != null) {
            log.error(na.elemtype.pos(), Errors.NewNotAllowedInAnnotation);
        }
        ListBuffer<Attribute> buf = new ListBuffer<>();
        for (List<JCExpression> l = na.elems; l.nonEmpty(); l=l.tail) {
            buf.append(attributeAnnotationValue(types.elemtype(expectedElementType),
                    l.head,
                    env));
        }
        na.type = expectedElementType;
        return new Attribute.
                Array(expectedElementType, buf.toArray(new Attribute[buf.length()]));
    }

    /* *********************************
     * Support for repeating annotations
     ***********************************/

    /**
     * This context contains all the information needed to synthesize new
     * annotations trees for repeating annotations.
     */
    private class AnnotationContext<T extends Attribute.Compound> {
        public final Env<AttrContext> env;
        public final Map<Symbol.TypeSymbol, ListBuffer<T>> annotated;
        public final Map<T, JCDiagnostic.DiagnosticPosition> pos;
        public final boolean isTypeCompound;

        public AnnotationContext(Env<AttrContext> env,
                                 Map<Symbol.TypeSymbol, ListBuffer<T>> annotated,
                                 Map<T, JCDiagnostic.DiagnosticPosition> pos,
                                 boolean isTypeCompound) {
            Assert.checkNonNull(env);
            Assert.checkNonNull(annotated);
            Assert.checkNonNull(pos);

            this.env = env;
            this.annotated = annotated;
            this.pos = pos;
            this.isTypeCompound = isTypeCompound;
        }
    }

    /* Process repeated annotations. This method returns the
     * synthesized container annotation or null IFF all repeating
     * annotation are invalid.  This method reports errors/warnings.
     */
    private <T extends Attribute.Compound> T processRepeatedAnnotations(List<T> annotations,
            AnnotationContext<T> ctx, Symbol on, boolean isTypeParam)
    {
        T firstOccurrence = annotations.head;
        List<Attribute> repeated = List.nil();
        Type origAnnoType = null;
        Type arrayOfOrigAnnoType = null;
        Type targetContainerType = null;
        MethodSymbol containerValueSymbol = null;

        Assert.check(!annotations.isEmpty() && !annotations.tail.isEmpty()); // i.e. size() > 1

        int count = 0;
        for (List<T> al = annotations; !al.isEmpty(); al = al.tail) {
            count++;

            // There must be more than a single anno in the annotation list
            Assert.check(count > 1 || !al.tail.isEmpty());

            T currentAnno = al.head;

            origAnnoType = currentAnno.type;
            if (arrayOfOrigAnnoType == null) {
                arrayOfOrigAnnoType = types.makeArrayType(origAnnoType);
            }

            // Only report errors if this isn't the first occurrence I.E. count > 1
            boolean reportError = count > 1;
            Type currentContainerType = getContainingType(currentAnno, ctx.pos.get(currentAnno), reportError);
            if (currentContainerType == null) {
                continue;
            }
            // Assert that the target Container is == for all repeated
            // annos of the same annotation type, the types should
            // come from the same Symbol, i.e. be '=='
            Assert.check(targetContainerType == null || currentContainerType == targetContainerType);
            targetContainerType = currentContainerType;

            containerValueSymbol = validateContainer(targetContainerType, origAnnoType, ctx.pos.get(currentAnno));

            if (containerValueSymbol == null) { // Check of CA type failed
                // errors are already reported
                continue;
            }

            repeated = repeated.prepend(currentAnno);
        }

        if (!repeated.isEmpty() && targetContainerType == null) {
            log.error(ctx.pos.get(annotations.head), Errors.DuplicateAnnotationInvalidRepeated(origAnnoType));
            return null;
        }

        if (!repeated.isEmpty()) {
            repeated = repeated.reverse();
            DiagnosticPosition pos = ctx.pos.get(firstOccurrence);
            TreeMaker m = make.at(pos);
            Pair<MethodSymbol, Attribute> p =
                    new Pair<MethodSymbol, Attribute>(containerValueSymbol,
                            new Attribute.Array(arrayOfOrigAnnoType, repeated));
            if (ctx.isTypeCompound) {
                /* TODO: the following code would be cleaner:
                Attribute.TypeCompound at = new Attribute.TypeCompound(targetContainerType, List.of(p),
                        ((Attribute.TypeCompound)annotations.head).position);
                JCTypeAnnotation annoTree = m.TypeAnnotation(at);
                at = attributeTypeAnnotation(annoTree, targetContainerType, ctx.env);
                */
                // However, we directly construct the TypeCompound to keep the
                // direct relation to the contained TypeCompounds.
                Attribute.TypeCompound at = new Attribute.TypeCompound(targetContainerType, List.of(p),
                        ((Attribute.TypeCompound)annotations.head).position);

                JCAnnotation annoTree = m.TypeAnnotation(at);
                if (!chk.validateAnnotationDeferErrors(annoTree))
                    log.error(annoTree.pos(), Errors.DuplicateAnnotationInvalidRepeated(origAnnoType));

                if (!chk.isTypeAnnotation(annoTree, isTypeParam)) {
                    log.error(pos, isTypeParam ? Errors.InvalidRepeatableAnnotationNotApplicable(targetContainerType, on)
                                               : Errors.InvalidRepeatableAnnotationNotApplicableInContext(targetContainerType));
                }

                at.setSynthesized(true);

                @SuppressWarnings("unchecked")
                T x = (T) at;
                return x;
            } else {
                Attribute.Compound c = new Attribute.Compound(targetContainerType, List.of(p));
                JCAnnotation annoTree = m.Annotation(c);

                boolean isRecordMember = (on.flags_field & Flags.RECORD) != 0 || on.enclClass() != null && on.enclClass().isRecord();
                /* if it is a record member we will not issue the error now and wait until annotations on records are
                 * checked at Check::validateAnnotation, which will issue it
                 */
                if (!chk.annotationApplicable(annoTree, on) && (!isRecordMember || isRecordMember && (on.flags_field & Flags.GENERATED_MEMBER) == 0)) {
                    log.error(annoTree.pos(),
                              Errors.InvalidRepeatableAnnotationNotApplicable(targetContainerType, on));
                }

                if (!chk.validateAnnotationDeferErrors(annoTree))
                    log.error(annoTree.pos(), Errors.DuplicateAnnotationInvalidRepeated(origAnnoType));

                c = attributeAnnotation(annoTree, targetContainerType, ctx.env);
                c.setSynthesized(true);

                @SuppressWarnings("unchecked")
                T x = (T) c;
                return x;
            }
        } else {
            return null; // errors should have been reported elsewhere
        }
    }

    /**
     * Fetches the actual Type that should be the containing annotation.
     */
    private Type getContainingType(Attribute.Compound currentAnno,
                                   DiagnosticPosition pos,
                                   boolean reportError)
    {
        Type origAnnoType = currentAnno.type;
        TypeSymbol origAnnoDecl = origAnnoType.tsym;

        // Fetch the Repeatable annotation from the current
        // annotation's declaration, or null if it has none
        Attribute.Compound ca = origAnnoDecl.getAnnotationTypeMetadata().getRepeatable();
        if (ca == null) { // has no Repeatable annotation
            if (reportError)
                log.error(pos, Errors.DuplicateAnnotationMissingContainer(origAnnoType));
            return null;
        }

        return filterSame(extractContainingType(ca, pos, origAnnoDecl),
                origAnnoType);
    }

    // returns null if t is same as 's', returns 't' otherwise
    private Type filterSame(Type t, Type s) {
        if (t == null || s == null) {
            return t;
        }

        return types.isSameType(t, s) ? null : t;
    }

    /** Extract the actual Type to be used for a containing annotation. */
    private Type extractContainingType(Attribute.Compound ca,
                                       DiagnosticPosition pos,
                                       TypeSymbol annoDecl)
    {
        // The next three checks check that the Repeatable annotation
        // on the declaration of the annotation type that is repeating is
        // valid.

        // Repeatable must have at least one element
        if (ca.values.isEmpty()) {
            log.error(pos, Errors.InvalidRepeatableAnnotation(annoDecl));
            return null;
        }
        Pair<MethodSymbol,Attribute> p = ca.values.head;
        Name name = p.fst.name;
        if (name != names.value) { // should contain only one element, named "value"
            log.error(pos, Errors.InvalidRepeatableAnnotation(annoDecl));
            return null;
        }
        if (!(p.snd instanceof Attribute.Class attributeClass)) { // check that the value of "value" is an Attribute.Class
            log.error(pos, Errors.InvalidRepeatableAnnotation(annoDecl));
            return null;
        }

        return attributeClass.getValue();
    }

    /* Validate that the suggested targetContainerType Type is a valid
     * container type for repeated instances of originalAnnoType
     * annotations. Return null and report errors if this is not the
     * case, return the MethodSymbol of the value element in
     * targetContainerType if it is suitable (this is needed to
     * synthesize the container). */
    private MethodSymbol validateContainer(Type targetContainerType,
                                           Type originalAnnoType,
                                           DiagnosticPosition pos) {
        MethodSymbol containerValueSymbol = null;
        boolean fatalError = false;

        // Validate that there is a (and only 1) value method
        Scope scope = targetContainerType.tsym.members();
        int nr_value_elems = 0;
        boolean error = false;
        for(Symbol elm : scope.getSymbolsByName(names.value)) {
            nr_value_elems++;

            if (nr_value_elems == 1 &&
                    elm.kind == MTH) {
                containerValueSymbol = (MethodSymbol)elm;
            } else {
                error = true;
            }
        }
        if (error) {
            log.error(pos,
                      Errors.InvalidRepeatableAnnotationMultipleValues(targetContainerType,
                                                                       nr_value_elems));
            return null;
        } else if (nr_value_elems == 0) {
            log.error(pos,
                      Errors.InvalidRepeatableAnnotationNoValue(targetContainerType));
            return null;
        }

        // validate that the 'value' element is a method
        // probably "impossible" to fail this
        if (containerValueSymbol.kind != MTH) {
            log.error(pos,
                    Errors.InvalidRepeatableAnnotationInvalidValue(targetContainerType));
            fatalError = true;
        }

        // validate that the 'value' element has the correct return type
        // i.e. array of original anno
        Type valueRetType = containerValueSymbol.type.getReturnType();
        Type expectedType = types.makeArrayType(originalAnnoType);
        if (!(types.isArray(valueRetType) &&
                types.isSameType(expectedType, valueRetType))) {
            log.error(pos,
                      Errors.InvalidRepeatableAnnotationValueReturn(targetContainerType,
                                                                    valueRetType,
                                                                    expectedType));
            fatalError = true;
        }

        return fatalError ? null : containerValueSymbol;
    }

    private <T extends Attribute.Compound> T makeContainerAnnotation(List<T> toBeReplaced,
            AnnotationContext<T> ctx, Symbol sym, boolean isTypeParam)
    {
        // Process repeated annotations
        T validRepeated =
                processRepeatedAnnotations(toBeReplaced, ctx, sym, isTypeParam);

        if (validRepeated != null) {
            // Check that the container isn't manually
            // present along with repeated instances of
            // its contained annotation.
            ListBuffer<T> manualContainer = ctx.annotated.get(validRepeated.type.tsym);
            if (manualContainer != null) {
                log.error(ctx.pos.get(manualContainer.first()),
                          Errors.InvalidRepeatableAnnotationRepeatedAndContainerPresent(manualContainer.first().type.tsym));
            }
        }

        // A null return will delete the Placeholder
        return validRepeated;
    }

    /********************
     * Type annotations *
     ********************/

    /**
     * Attribute the list of annotations and enter them onto s.
     */
    public void enterTypeAnnotations(List<JCAnnotation> annotations, Env<AttrContext> env,
            Symbol s, DiagnosticPosition deferPos, boolean isTypeParam)
    {
        Assert.checkNonNull(s, "Symbol argument to actualEnterTypeAnnotations is nul/");
        JavaFileObject prev = log.useSource(env.toplevel.sourcefile);
        DiagnosticPosition prevLintPos = null;

        if (deferPos != null) {
            prevLintPos = deferredLintHandler.setPos(deferPos);
        }
        try {
            annotateNow(s, annotations, env, true, isTypeParam);
        } finally {
            if (prevLintPos != null)
                deferredLintHandler.setPos(prevLintPos);
            log.useSource(prev);
        }
    }

    /**
     * Enqueue tree for scanning of type annotations, attaching to the Symbol sym.
     */
    public void queueScanTreeAndTypeAnnotate(JCTree tree, Env<AttrContext> env, Symbol sym,
            DiagnosticPosition deferPos)
    {
        Assert.checkNonNull(sym);
        normal(() -> tree.accept(new TypeAnnotate(env, sym, deferPos)));
    }

    /**
     * Apply the annotations to the particular type.
     */
    public void annotateTypeSecondStage(JCTree tree, List<JCAnnotation> annotations, Type storeAt) {
        typeAnnotation(() -> {
            List<Attribute.TypeCompound> compounds = fromAnnotations(annotations);
            Assert.check(annotations.size() == compounds.size());
            storeAt.getMetadataOfKind(Kind.ANNOTATIONS).combine(new TypeMetadata.Annotations(compounds));
        });
    }

    /**
     * Apply the annotations to the particular type.
     */
    public void annotateTypeParameterSecondStage(JCTree tree, List<JCAnnotation> annotations) {
        typeAnnotation(() -> {
            List<Attribute.TypeCompound> compounds = fromAnnotations(annotations);
            Assert.check(annotations.size() == compounds.size());
        });
    }

    /**
     * We need to use a TreeScanner, because it is not enough to visit the top-level
     * annotations. We also need to visit type arguments, etc.
     */
    private class TypeAnnotate extends TreeScanner {
        private final Env<AttrContext> env;
        private final Symbol sym;
        private DiagnosticPosition deferPos;

        public TypeAnnotate(Env<AttrContext> env, Symbol sym, DiagnosticPosition deferPos) {

            this.env = env;
            this.sym = sym;
            this.deferPos = deferPos;
        }

        @Override
        public void visitAnnotatedType(JCAnnotatedType tree) {
            enterTypeAnnotations(tree.annotations, env, sym, deferPos, false);
            scan(tree.underlyingType);
        }

        @Override
        public void visitTypeParameter(JCTypeParameter tree) {
            enterTypeAnnotations(tree.annotations, env, sym, deferPos, true);
            scan(tree.bounds);
        }

        @Override
        public void visitNewArray(JCNewArray tree) {
            enterTypeAnnotations(tree.annotations, env, sym, deferPos, false);
            for (List<JCAnnotation> dimAnnos : tree.dimAnnotations)
                enterTypeAnnotations(dimAnnos, env, sym, deferPos, false);
            scan(tree.elemtype);
            scan(tree.elems);
        }

        @Override
        public void visitMethodDef(JCMethodDecl tree) {
            scan(tree.mods);
            scan(tree.restype);
            scan(tree.typarams);
            scan(tree.recvparam);
            scan(tree.params);
            scan(tree.thrown);
            scan(tree.defaultValue);
            // Do not annotate the body, just the signature.
        }

        @Override
        public void visitVarDef(JCVariableDecl tree) {
            DiagnosticPosition prevPos = deferPos;
            deferPos = tree.pos();
            try {
                if (sym != null && sym.kind == VAR) {
                    // Don't visit a parameter once when the sym is the method
                    // and once when the sym is the parameter.
                    scan(tree.mods);
                    scan(tree.vartype);
                }
                scan(tree.init);
            } finally {
                deferPos = prevPos;
            }
        }

        @Override
        public void visitBindingPattern(JCTree.JCBindingPattern tree) {
            //type binding pattern's type will be annotated separately, avoid
            //adding its annotations into the owning method here (would clash
            //with repeatable annotations).
        }

        @Override
        public void visitClassDef(JCClassDecl tree) {
            // We can only hit a classdef if it is declared within
            // a method. Ignore it - the class will be visited
            // separately later.
        }

        @Override
        public void visitNewClass(JCNewClass tree) {
            scan(tree.encl);
            scan(tree.typeargs);
            if (tree.def == null) {
                scan(tree.clazz);
            }
            scan(tree.args);
            // the anonymous class instantiation if any will be visited separately.
        }
    }

    /*********************
     * Completer support *
     *********************/

    private AnnotationTypeCompleter theSourceCompleter = new AnnotationTypeCompleter() {
        @Override
        public void complete(ClassSymbol sym) throws CompletionFailure {
            Env<AttrContext> context = typeEnvs.get(sym);
            Annotate.this.attributeAnnotationType(context);
        }
    };

    /* Last stage completer to enter just enough annotations to have a prototype annotation type.
     * This currently means entering @Target and @Repeatable.
     */
    public AnnotationTypeCompleter annotationTypeSourceCompleter() {
        return theSourceCompleter;
    }

    private void attributeAnnotationType(Env<AttrContext> env) {
        Assert.check(((JCClassDecl)env.tree).sym.isAnnotationType(),
                "Trying to annotation type complete a non-annotation type");

        JavaFileObject prev = log.useSource(env.toplevel.sourcefile);
        try {
            JCClassDecl tree = (JCClassDecl)env.tree;
            AnnotationTypeVisitor v = new AnnotationTypeVisitor(attr, chk, syms, typeEnvs);
            v.scanAnnotationType(tree);
            tree.sym.getAnnotationTypeMetadata().setRepeatable(v.repeatable);
            tree.sym.getAnnotationTypeMetadata().setTarget(v.target);
        } finally {
            log.useSource(prev);
        }
    }

    public Attribute unfinishedDefaultValue() {
        return theUnfinishedDefaultValue;
    }

    public static interface AnnotationTypeCompleter {
        void complete(ClassSymbol sym) throws CompletionFailure;
    }

    /** Visitor to determine a prototype annotation type for a class declaring an annotation type.
     *
     *  <p><b>This is NOT part of any supported API.
     *  If you write code that depends on this, you do so at your own risk.
     *  This code and its internal interfaces are subject to change or
     *  deletion without notice.</b>
     */
    public class AnnotationTypeVisitor extends TreeScanner {
        private Env<AttrContext> env;

        private final Attr attr;
        private final Check check;
        private final Symtab tab;
        private final TypeEnvs typeEnvs;

        private Compound target;
        private Compound repeatable;

        public AnnotationTypeVisitor(Attr attr, Check check, Symtab tab, TypeEnvs typeEnvs) {
            this.attr = attr;
            this.check = check;
            this.tab = tab;
            this.typeEnvs = typeEnvs;
        }

        public Compound getRepeatable() {
            return repeatable;
        }

        public Compound getTarget() {
            return target;
        }

        public void scanAnnotationType(JCClassDecl decl) {
            visitClassDef(decl);
        }

        @Override
        public void visitClassDef(JCClassDecl tree) {
            Env<AttrContext> prevEnv = env;
            env = typeEnvs.get(tree.sym);
            try {
                scan(tree.mods); // look for repeatable and target
                // don't descend into body
            } finally {
                env = prevEnv;
            }
        }

        @Override
        public void visitAnnotation(JCAnnotation tree) {
            Type t = tree.annotationType.type;
            if (t == null) {
                t = attr.attribType(tree.annotationType, env);
                tree.annotationType.type = t = check.checkType(tree.annotationType.pos(), t, tab.annotationType);
            }

            if (t == tab.annotationTargetType) {
                target = Annotate.this.attributeAnnotation(tree, tab.annotationTargetType, env);
            } else if (t == tab.repeatableType) {
                repeatable = Annotate.this.attributeAnnotation(tree, tab.repeatableType, env);
            }
        }
    }

    /** Represents the semantics of an Annotation Type.
     *
     *  <p><b>This is NOT part of any supported API.
     *  If you write code that depends on this, you do so at your own risk.
     *  This code and its internal interfaces are subject to change or
     *  deletion without notice.</b>
     */
    public static class AnnotationTypeMetadata {
        final ClassSymbol metaDataFor;
        private Compound target;
        private Compound repeatable;
        private AnnotationTypeCompleter annotationTypeCompleter;

        public AnnotationTypeMetadata(ClassSymbol metaDataFor, AnnotationTypeCompleter annotationTypeCompleter) {
            this.metaDataFor = metaDataFor;
            this.annotationTypeCompleter = annotationTypeCompleter;
        }

        private void init() {
            // Make sure metaDataFor is member entered
            while (!metaDataFor.isCompleted())
                metaDataFor.complete();

            if (annotationTypeCompleter != null) {
                AnnotationTypeCompleter c = annotationTypeCompleter;
                annotationTypeCompleter = null;
                c.complete(metaDataFor);
            }
        }

        public void complete() {
            init();
        }

        public Compound getRepeatable() {
            init();
            return repeatable;
        }

        public void setRepeatable(Compound repeatable) {
            Assert.checkNull(this.repeatable);
            this.repeatable = repeatable;
        }

        public Compound getTarget() {
            init();
            return target;
        }

        public void setTarget(Compound target) {
            Assert.checkNull(this.target);
                this.target = target;
        }

        public Set<MethodSymbol> getAnnotationElements() {
            init();
            Set<MethodSymbol> members = new LinkedHashSet<>();
            WriteableScope s = metaDataFor.members();
            Iterable<Symbol> ss = s.getSymbols(NON_RECURSIVE);
            for (Symbol sym : ss)
                if (sym.kind == MTH &&
                        sym.name != sym.name.table.names.clinit &&
                        (sym.flags() & SYNTHETIC) == 0)
                    members.add((MethodSymbol)sym);
            return members;
        }

        public Set<MethodSymbol> getAnnotationElementsWithDefault() {
            init();
            Set<MethodSymbol> members = getAnnotationElements();
            Set<MethodSymbol> res = new LinkedHashSet<>();
            for (MethodSymbol m : members)
                if (m.defaultValue != null)
                    res.add(m);
            return res;
        }

        @Override
        public String toString() {
            return "Annotation type for: " + metaDataFor;
        }

        public boolean isMetadataForAnnotationType() { return true; }

        public static AnnotationTypeMetadata notAnAnnotationType() {
            return NOT_AN_ANNOTATION_TYPE;
        }

        private static final AnnotationTypeMetadata NOT_AN_ANNOTATION_TYPE =
                new AnnotationTypeMetadata(null, null) {
                    @Override
                    public void complete() {
                    } // do nothing

                    @Override
                    public String toString() {
                        return "Not an annotation type";
                    }

                    @Override
                    public Set<MethodSymbol> getAnnotationElements() {
                        return new LinkedHashSet<>(0);
                    }

                    @Override
                    public Set<MethodSymbol> getAnnotationElementsWithDefault() {
                        return new LinkedHashSet<>(0);
                    }

                    @Override
                    public boolean isMetadataForAnnotationType() {
                        return false;
                    }

                    @Override
                    public Compound getTarget() {
                        return null;
                    }

                    @Override
                    public Compound getRepeatable() {
                        return null;
                    }
                };
    }

    public void newRound() {
        blockCount = 1;
    }

    public Queues setQueues(Queues nue) {
        Queues stored = new Queues(q, validateQ, typesQ, afterTypesQ);
        this.q = nue.q;
        this.typesQ = nue.typesQ;
        this.afterTypesQ = nue.afterTypesQ;
        this.validateQ = nue.validateQ;
        return stored;
    }

    static class Queues {
        private final ListBuffer<Runnable> q;
        private final ListBuffer<Runnable> validateQ;
        private final ListBuffer<Runnable> typesQ;
        private final ListBuffer<Runnable> afterTypesQ;

        public Queues() {
            this(new ListBuffer<Runnable>(), new ListBuffer<Runnable>(), new ListBuffer<Runnable>(), new ListBuffer<Runnable>());
        }

        public Queues(ListBuffer<Runnable> q, ListBuffer<Runnable> validateQ, ListBuffer<Runnable> typesQ, ListBuffer<Runnable> afterTypesQ) {
            this.q = q;
            this.validateQ = validateQ;
            this.typesQ = typesQ;
            this.afterTypesQ = afterTypesQ;
        }
    }
}
