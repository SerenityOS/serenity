/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.jvm;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.jvm.PoolConstant.LoadableConstant;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.*;

import static com.sun.tools.javac.code.Kinds.Kind.MTH;
import static com.sun.tools.javac.code.TypeTag.*;
import static com.sun.tools.javac.jvm.ByteCodes.*;
import static com.sun.tools.javac.tree.JCTree.Tag.PLUS;
import com.sun.tools.javac.jvm.Items.*;

import java.util.HashMap;
import java.util.Map;

/** This lowers the String concatenation to something that JVM can understand.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public abstract class StringConcat {

    /**
     * Maximum number of slots for String Concat call.
     * JDK's StringConcatFactory does not support more than that.
     */
    private static final int MAX_INDY_CONCAT_ARG_SLOTS = 200;
    private static final char TAG_ARG   = '\u0001';
    private static final char TAG_CONST = '\u0002';

    protected final Gen gen;
    protected final Symtab syms;
    protected final Names names;
    protected final TreeMaker make;
    protected final Types types;
    protected final Map<Type, Symbol> sbAppends;
    protected final Resolve rs;

    protected static final Context.Key<StringConcat> concatKey = new Context.Key<>();

    public static StringConcat instance(Context context) {
        StringConcat instance = context.get(concatKey);
        if (instance == null) {
            instance = makeConcat(context);
        }
        return instance;
    }

    private static StringConcat makeConcat(Context context) {
        Target target = Target.instance(context);
        String opt = Options.instance(context).get("stringConcat");
        if (target.hasStringConcatFactory()) {
            if (opt == null) {
                opt = "indyWithConstants";
            }
        } else {
            if (opt != null && !"inline".equals(opt)) {
                Assert.error("StringConcatFactory-based string concat is requested on a platform that does not support it.");
            }
            opt = "inline";
        }

        switch (opt) {
            case "inline":
                return new Inline(context);
            case "indy":
                return new IndyPlain(context);
            case "indyWithConstants":
                return new IndyConstants(context);
            default:
                Assert.error("Unknown stringConcat: " + opt);
                throw new IllegalStateException("Unknown stringConcat: " + opt);
        }
    }

    protected StringConcat(Context context) {
        context.put(concatKey, this);
        gen = Gen.instance(context);
        syms = Symtab.instance(context);
        types = Types.instance(context);
        names = Names.instance(context);
        make = TreeMaker.instance(context);
        rs = Resolve.instance(context);
        sbAppends = new HashMap<>();
    }

    public abstract Item makeConcat(JCTree.JCAssignOp tree);
    public abstract Item makeConcat(JCTree.JCBinary tree);

    protected List<JCTree> collectAll(JCTree tree) {
        return collect(tree, List.nil());
    }

    protected List<JCTree> collectAll(JCTree.JCExpression lhs, JCTree.JCExpression rhs) {
        return List.<JCTree>nil()
                .appendList(collectAll(lhs))
                .appendList(collectAll(rhs));
    }

    private List<JCTree> collect(JCTree tree, List<JCTree> res) {
        tree = TreeInfo.skipParens(tree);
        if (tree.hasTag(PLUS) && tree.type.constValue() == null) {
            JCTree.JCBinary op = (JCTree.JCBinary) tree;
            if (op.operator.kind == MTH && op.operator.opcode == string_add) {
                return res
                        .appendList(collect(op.lhs, res))
                        .appendList(collect(op.rhs, res));
            }
        }
        return res.append(tree);
    }

    /**
     * If the type is not accessible from current context, try to figure out the
     * sharpest accessible supertype.
     *
     * @param originalType type to sharpen
     * @return sharped type
     */
    Type sharpestAccessible(Type originalType) {
        if (originalType.hasTag(ARRAY)) {
            return types.makeArrayType(sharpestAccessible(types.elemtype(originalType)));
        }

        Type type = originalType;
        while (!rs.isAccessible(gen.getAttrEnv(), type.asElement())) {
            type = types.supertype(type);
        }
        return type;
    }

    /**
     * "Legacy" bytecode flavor: emit the StringBuilder.append chains for string
     * concatenation.
     */
    private static class Inline extends StringConcat {
        public Inline(Context context) {
            super(context);
        }

        @Override
        public Item makeConcat(JCTree.JCAssignOp tree) {
            // Generate code to make a string builder
            JCDiagnostic.DiagnosticPosition pos = tree.pos();

            // Create a string builder.
            newStringBuilder(tree);

            // Generate code for first string, possibly save one
            // copy under builder
            Item l = gen.genExpr(tree.lhs, tree.lhs.type);
            if (l.width() > 0) {
                gen.getCode().emitop0(dup_x1 + 3 * (l.width() - 1));
            }

            // Load first string and append to builder.
            l.load();
            appendString(tree.lhs);

            // Append all other strings to builder.
            List<JCTree> args = collectAll(tree.rhs);
            for (JCTree t : args) {
                gen.genExpr(t, t.type).load();
                appendString(t);
            }

            // Convert builder to string.
            builderToString(pos);

            return l;
        }

        @Override
        public Item makeConcat(JCTree.JCBinary tree) {
            JCDiagnostic.DiagnosticPosition pos = tree.pos();

            // Create a string builder.
            newStringBuilder(tree);

            // Append all strings to builder.
            List<JCTree> args = collectAll(tree);
            for (JCTree t : args) {
                gen.genExpr(t, t.type).load();
                appendString(t);
            }

            // Convert builder to string.
            builderToString(pos);

            return gen.getItems().makeStackItem(syms.stringType);
        }

        private JCDiagnostic.DiagnosticPosition newStringBuilder(JCTree tree) {
            JCDiagnostic.DiagnosticPosition pos = tree.pos();
            gen.getCode().emitop2(new_, gen.makeRef(pos, syms.stringBuilderType), syms.stringBuilderType);
            gen.getCode().emitop0(dup);
            gen.callMethod(pos, syms.stringBuilderType, names.init, List.nil(), false);
            return pos;
        }

        private void appendString(JCTree tree) {
            Type t = tree.type.baseType();
            if (!t.isPrimitive() && t.tsym != syms.stringType.tsym) {
                t = syms.objectType;
            }

            Assert.checkNull(t.constValue());
            Symbol method = sbAppends.get(t);
            if (method == null) {
                method = rs.resolveInternalMethod(tree.pos(), gen.getAttrEnv(), syms.stringBuilderType, names.append, List.of(t), null);
                sbAppends.put(t, method);
            }

            gen.getItems().makeMemberItem(method, false).invoke();
        }

        private void builderToString(JCDiagnostic.DiagnosticPosition pos) {
            gen.callMethod(pos, syms.stringBuilderType, names.toString, List.nil(), false);
        }
    }

    /**
     * Base class for indified concatenation bytecode flavors.
     */
    private static abstract class Indy extends StringConcat {
        public Indy(Context context) {
            super(context);
        }

        @Override
        public Item makeConcat(JCTree.JCAssignOp tree) {
            List<JCTree> args = collectAll(tree.lhs, tree.rhs);
            Item l = gen.genExpr(tree.lhs, tree.lhs.type);
            l.duplicate();
            l.load();
            emit(tree.pos(), args, false, tree.type);
            return l;
        }

        @Override
        public Item makeConcat(JCTree.JCBinary tree) {
            List<JCTree> args = collectAll(tree.lhs, tree.rhs);
            emit(tree.pos(), args, true, tree.type);
            return gen.getItems().makeStackItem(syms.stringType);
        }

        protected abstract void emit(JCDiagnostic.DiagnosticPosition pos, List<JCTree> args, boolean generateFirstArg, Type type);

        /** Peel the argument list into smaller chunks. */
        protected List<List<JCTree>> split(List<JCTree> args) {
            ListBuffer<List<JCTree>> splits = new ListBuffer<>();

            int slots = 0;

            // Need to peel, so that neither call has more than acceptable number
            // of slots for the arguments.
            ListBuffer<JCTree> cArgs = new ListBuffer<>();
            for (JCTree t : args) {
                int needSlots = (t.type.getTag() == LONG || t.type.getTag() == DOUBLE) ? 2 : 1;
                if (slots + needSlots >= MAX_INDY_CONCAT_ARG_SLOTS) {
                    splits.add(cArgs.toList());
                    cArgs.clear();
                    slots = 0;
                }
                cArgs.add(t);
                slots += needSlots;
            }

            // Flush the tail slice
            if (!cArgs.isEmpty()) {
                splits.add(cArgs.toList());
            }

            return splits.toList();
        }
    }

    /**
     * Emits the invokedynamic call to JDK java.lang.invoke.StringConcatFactory,
     * without handling constants specially.
     *
     * We bypass empty strings, because they have no meaning at this level. This
     * captures the Java language trick to force String concat with e.g. ("" + int)-like
     * expression. Down here, we already know we are in String concat business, and do
     * not require these markers.
     */
    private static class IndyPlain extends Indy {
        public IndyPlain(Context context) {
            super(context);
        }

        /** Emit the indy concat for all these arguments, possibly peeling along the way */
        protected void emit(JCDiagnostic.DiagnosticPosition pos, List<JCTree> args, boolean generateFirstArg, Type type) {
            List<List<JCTree>> split = split(args);

            boolean first = true;
            for (List<JCTree> t : split) {
                Assert.check(!t.isEmpty(), "Arguments list is empty");

                ListBuffer<Type> dynamicArgs = new ListBuffer<>();
                for (JCTree arg : t) {
                    Object constVal = arg.type.constValue();
                    if ("".equals(constVal)) continue;
                    if (arg.type == syms.botType) {
                        dynamicArgs.add(types.boxedClass(syms.voidType).type);
                    } else {
                        dynamicArgs.add(sharpestAccessible(arg.type));
                    }
                    if (!first || generateFirstArg) {
                        gen.genExpr(arg, arg.type).load();
                    }
                    first = false;
                }
                doCall(type, pos, dynamicArgs.toList());
            }

            // More that one peel slice produced: concatenate the results
            if (split.size() > 1) {
                ListBuffer<Type> argTypes = new ListBuffer<>();
                for (int c = 0; c < split.size(); c++) {
                    argTypes.append(syms.stringType);
                }
                doCall(type, pos, argTypes.toList());
            }
        }

        /** Produce the actual invokedynamic call to StringConcatFactory */
        private void doCall(Type type, JCDiagnostic.DiagnosticPosition pos, List<Type> dynamicArgTypes) {
            Type.MethodType indyType = new Type.MethodType(dynamicArgTypes,
                    type,
                    List.nil(),
                    syms.methodClass);

            int prevPos = make.pos;
            try {
                make.at(pos);

                List<Type> bsm_staticArgs = List.of(syms.methodHandleLookupType,
                        syms.stringType,
                        syms.methodTypeType);

                Symbol bsm = rs.resolveInternalMethod(pos,
                        gen.getAttrEnv(),
                        syms.stringConcatFactory,
                        names.makeConcat,
                        bsm_staticArgs,
                        null);

                Symbol.DynamicMethodSymbol dynSym = new Symbol.DynamicMethodSymbol(names.makeConcat,
                        syms.noSymbol,
                        ((MethodSymbol)bsm).asHandle(),
                        indyType,
                        List.nil().toArray(new LoadableConstant[0]));

                Items.Item item = gen.getItems().makeDynamicItem(dynSym);
                item.invoke();
            } finally {
                make.at(prevPos);
            }
        }
    }

    /**
     * Emits the invokedynamic call to JDK java.lang.invoke.StringConcatFactory.
     * This code concatenates all known constants into the recipe, possibly escaping
     * some constants separately.
     *
     * We also bypass empty strings, because they have no meaning at this level. This
     * captures the Java language trick to force String concat with e.g. ("" + int)-like
     * expression. Down here, we already know we are in String concat business, and do
     * not require these markers.
     */
    private static final class IndyConstants extends Indy {
        public IndyConstants(Context context) {
            super(context);
        }

        @Override
        protected void emit(JCDiagnostic.DiagnosticPosition pos, List<JCTree> args, boolean generateFirstArg, Type type) {
            List<List<JCTree>> split = split(args);

            boolean first = true;
            for (List<JCTree> t : split) {
                Assert.check(!t.isEmpty(), "Arguments list is empty");

                StringBuilder recipe = new StringBuilder(t.size());
                ListBuffer<Type> dynamicArgs = new ListBuffer<>();
                ListBuffer<LoadableConstant> staticArgs = new ListBuffer<>();

                for (JCTree arg : t) {
                    Object constVal = arg.type.constValue();
                    if ("".equals(constVal)) continue;
                    if (arg.type == syms.botType) {
                        // Concat the null into the recipe right away
                        recipe.append((String) null);
                    } else if (constVal != null) {
                        // Concat the String representation of the constant, except
                        // for the case it contains special tags, which requires us
                        // to expose it as detached constant.
                        String a = arg.type.stringValue();
                        if (a.indexOf(TAG_CONST) != -1 || a.indexOf(TAG_ARG) != -1) {
                            recipe.append(TAG_CONST);
                            staticArgs.add(LoadableConstant.String(a));
                        } else {
                            recipe.append(a);
                        }
                    } else {
                        // Ordinary arguments come through the dynamic arguments.
                        recipe.append(TAG_ARG);
                        dynamicArgs.add(sharpestAccessible(arg.type));
                        if (!first || generateFirstArg) {
                            gen.genExpr(arg, arg.type).load();
                        }
                        first = false;
                    }
                }

                doCall(type, pos, recipe.toString(), staticArgs.toList(), dynamicArgs.toList());
            }

            // More that one peel slice produced: concatenate the results
            // All arguments are assumed to be non-constant Strings.
            if (split.size() > 1) {
                ListBuffer<Type> argTypes = new ListBuffer<>();
                StringBuilder recipe = new StringBuilder();
                for (int c = 0; c < split.size(); c++) {
                    argTypes.append(syms.stringType);
                    recipe.append(TAG_ARG);
                }
                doCall(type, pos, recipe.toString(), List.nil(), argTypes.toList());
            }
        }

        /** Produce the actual invokedynamic call to StringConcatFactory */
        private void doCall(Type type, JCDiagnostic.DiagnosticPosition pos, String recipe, List<LoadableConstant> staticArgs, List<Type> dynamicArgTypes) {
            Type.MethodType indyType = new Type.MethodType(dynamicArgTypes,
                    type,
                    List.nil(),
                    syms.methodClass);

            int prevPos = make.pos;
            try {
                make.at(pos);

                ListBuffer<Type> constTypes = new ListBuffer<>();
                ListBuffer<LoadableConstant> constants = new ListBuffer<>();
                for (LoadableConstant t : staticArgs) {
                    constants.add(t);
                    constTypes.add(syms.stringType);
                }

                List<Type> bsm_staticArgs = List.of(syms.methodHandleLookupType,
                        syms.stringType,
                        syms.methodTypeType)
                        .append(syms.stringType)
                        .appendList(constTypes);

                Symbol bsm = rs.resolveInternalMethod(pos,
                        gen.getAttrEnv(),
                        syms.stringConcatFactory,
                        names.makeConcatWithConstants,
                        bsm_staticArgs,
                        null);

                Symbol.DynamicMethodSymbol dynSym = new Symbol.DynamicMethodSymbol(names.makeConcatWithConstants,
                        syms.noSymbol,
                        ((MethodSymbol)bsm).asHandle(),
                        indyType,
                        List.of(LoadableConstant.String(recipe))
                                .appendList(constants).toArray(new LoadableConstant[constants.size()]));

                Items.Item item = gen.getItems().makeDynamicItem(dynSym);
                item.invoke();
            } finally {
                make.at(prevPos);
            }
        }
    }

}
