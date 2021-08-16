/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.Visitor;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import static com.sun.tools.javac.code.Flags.FINAL;
import static com.sun.tools.javac.code.Flags.PUBLIC;
import static com.sun.tools.javac.code.Flags.STATIC;
import static com.sun.tools.javac.code.Flags.INTERFACE;
import static com.sun.tools.javac.code.Flags.ENUM;
import static com.sun.tools.javac.code.Flags.RECORD;
import static com.sun.tools.javac.code.Flags.SYNTHETIC;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.tree.TreeInfo;
import jdk.jshell.Wrap.CompoundWrap;
import jdk.jshell.Wrap.Range;
import jdk.jshell.Wrap.RangeWrap;

import java.util.Set;

/**
 * Produce a corralled version of the Wrap for a snippet.
 */
class Corraller extends Visitor {

    /** Visitor result field: a Wrap
     */
    protected Wrap result;

    private final TreeDissector dis;
    private final String resolutionExceptionBlock;
    private final String source;

    public Corraller(TreeDissector dis, int keyIndex, String source) {
        this.dis = dis;
        this.resolutionExceptionBlock = "\n      { throw new jdk.jshell.spi.SPIResolutionException(" + keyIndex + "); }";
        this.source = source;
    }

    public Wrap corralType(ClassTree tree) {
        return corralToWrap(tree);
    }

    public Wrap corralMethod(MethodTree tree) {
        return corralToWrap(tree);
    }

    private Wrap corralToWrap(Tree tree) {
        try {
            JCTree jct = (JCTree) tree;
            Wrap w = new CompoundWrap(
                    "    public static\n    ",
                    corral(jct));
            debugWrap("corralToWrap SUCCESS source: %s -- wrap:\n %s\n", tree, w.wrapped());
            return w;
        } catch (Exception ex) {
            debugWrap("corralToWrap FAIL: %s - %s\n", tree, ex);
            //ex.printStackTrace(System.err);
            return null;
        }
    }

    // Corral a single node.
//    @SuppressWarnings("unchecked")
    private <T extends JCTree> Wrap corral(T tree) {
        if (tree == null) {
            return null;
        } else {
            tree.accept(this);
            Wrap tmpResult = this.result;
            this.result = null;
            return tmpResult;
        }
    }

    private String defaultConstructor(JCClassDecl tree) {
        return "  public " + tree.name.toString() + "() " +
                resolutionExceptionBlock;
    }

    /* ***************************************************************************
     * Visitor methods
     ****************************************************************************/

    @Override
    public void visitClassDef(JCClassDecl tree) {
        boolean isEnum = (tree.mods.flags & ENUM) != 0;
        boolean isInterface = (tree.mods.flags & INTERFACE ) != 0;
        boolean isRecord = (tree.mods.flags & RECORD ) != 0;
        int classBegin = dis.getStartPosition(tree);
        int classEnd = dis.getEndPosition(tree);
        //debugWrap("visitClassDef: %d-%d = %s\n", classBegin, classEnd, source.substring(classBegin, classEnd));
        ListBuffer<Object> wrappedDefs = new ListBuffer<>();
        int bodyBegin = -1;
        if (tree.defs != null && !tree.defs.isEmpty()) {
            if (isEnum) {
                // copy the enum constants verbatim
                int enumBegin = dis.getStartPosition(tree.defs.head);
                JCTree t = null; // null to shut-up compiler, always set because non-empty
                List<? extends JCTree> l = tree.defs;
                for (; l.nonEmpty(); l = l.tail) {
                    t = l.head;
                    if (t.getKind() == Kind.VARIABLE) {
                        if ((((JCVariableDecl)t).mods.flags & (PUBLIC | STATIC | FINAL)) != (PUBLIC | STATIC | FINAL)) {
                            // non-enum constant, process normally
                            break;
                        }
                    } else {
                        // non-variable, process normally
                        break;
                    }
                }
                int constEnd = l.nonEmpty()                  // end of constants
                        ? dis.getStartPosition(l.head) - 1   // is one before next defs, if there is one
                        : dis.getEndPosition(t);             // and otherwise end of the last constant
                wrappedDefs.append(new RangeWrap(source, new Range(enumBegin, constEnd)));
                // handle any other defs
                for (; l.nonEmpty(); l = l.tail) {
                    wrappedDefs.append("\n");
                    t = l.head;
                    wrappedDefs.append(corral(t));
                }
            } else {
                // non-enum
                boolean constructorSeen = false;
                for (List<? extends JCTree> l = tree.defs; l.nonEmpty(); l = l.tail) {
                    JCTree t = l.head;
                    if (isRecord && t.hasTag(Tag.VARDEF) && (TreeInfo.flags(t) & RECORD) != 0) {
                        //record parameters are part of the record's header
                        continue;
                    }
                    wrappedDefs.append("\n   ");
                    switch (t.getKind()) {
                        case METHOD:
                            constructorSeen = constructorSeen || ((MethodTree)t).getName() == tree.name.table.names.init;
                            break;
                        case BLOCK:
                            // throw exception in instance initializer too -- inline because String not Wrap
                            wrappedDefs.append((((JCBlock)t).flags & STATIC) != 0
                                    ? new RangeWrap(source, dis.treeToRange(t))
                                    : resolutionExceptionBlock);
                            continue; // already appended, skip append below
                    }
                    wrappedDefs.append(corral(t));
                }
                if (!constructorSeen && isRecord) {
                    // Generate a default constructor, since
                    // this is a regular record and there are no constructors
                    if (wrappedDefs.length() > 0) {
                        wrappedDefs.append("\n ");
                    }
                    wrappedDefs.append("  public " + tree.name.toString() + " " + resolutionExceptionBlock);
                } else if (!constructorSeen && !isInterface && !isEnum) {
                    // Generate a default constructor, since
                    // this is a regular class and there are no constructors
                    if (wrappedDefs.length() > 0) {
                        wrappedDefs.append("\n ");
                    }
                    wrappedDefs.append(defaultConstructor(tree));
                }
            }
            if (!isRecord) {
                bodyBegin = dis.getStartPosition(tree.defs.head);
            }
        }
        Object defs = wrappedDefs.length() == 1
            ? wrappedDefs.first()
            : new CompoundWrap(wrappedDefs.toArray());
        if (bodyBegin < 0) {
            int brace = source.indexOf('{', classBegin);
            if (brace < 0 || brace >= classEnd) {
                throw new IllegalArgumentException("No brace found: " + source.substring(classBegin, classEnd));
            }
            bodyBegin = brace + 1;
        }
        // body includes openning brace
        result = new CompoundWrap(
                new RangeWrap(source, new Range(classBegin, bodyBegin)),
                defs,
                "\n}"
        );
    }

    // Corral the body
    @Override
    public void visitMethodDef(JCMethodDecl tree) {
        int methodBegin = dis.getStartPosition(tree);
        int methodEnd = dis.getEndPosition(tree);
        //debugWrap("+visitMethodDef: %d-%d = %s\n", methodBegin, methodEnd,
        //        source.substring(methodBegin, methodEnd));
        int bodyBegin = dis.getStartPosition(tree.getBody());
        if (bodyBegin < 0) {
            bodyBegin = source.indexOf('{', methodBegin);
            if (bodyBegin > methodEnd) {
                bodyBegin = -1;
            }
        }
        String adjustedSource;
        if (bodyBegin < 0) {
            adjustedSource = new MaskCommentsAndModifiers(source, Set.of("abstract")).cleared();
            bodyBegin = adjustedSource.charAt(methodEnd - 1) == ';'
                ? methodEnd - 1
                : methodEnd;
        } else {
            adjustedSource = source;
        }
        debugWrap("-visitMethodDef BEGIN: %d = '%s'\n", bodyBegin,
                adjustedSource.substring(methodBegin, bodyBegin));
        Range noBodyRange = new Range(methodBegin, bodyBegin);
        result = new CompoundWrap(
                new RangeWrap(adjustedSource, noBodyRange),
                resolutionExceptionBlock);
    }

    // Remove initializer, if present
    @Override
    public void visitVarDef(JCVariableDecl tree) {
        int begin = dis.getStartPosition(tree);
        int end = dis.getEndPosition(tree);
        if (tree.init == null) {
            result = new RangeWrap(source, new Range(begin, end));
        } else {
            int sinit = dis.getStartPosition(tree.init);
            int eq = source.lastIndexOf('=', sinit);
            if (eq < begin) {
                throw new IllegalArgumentException("Equals not found before init: " + source + " @" + sinit);
            }
            result = new CompoundWrap(new RangeWrap(source, new Range(begin, eq - 1)), ";");
        }
    }

    @Override
    public void visitTree(JCTree tree) {
        throw new IllegalArgumentException("Unexpected tree: " + tree);
    }

    void debugWrap(String format, Object... args) {
        //state.debug(this, InternalDebugControl.DBG_WRAP, format, args);
    }
}
