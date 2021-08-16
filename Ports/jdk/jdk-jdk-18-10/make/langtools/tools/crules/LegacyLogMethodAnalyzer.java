/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package crules;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.AbstractLog;
import com.sun.tools.javac.util.JCDiagnostic;

/**This analyzer guards against legacy Log.error/warning/note methods that don't use the typed keys.*/
public class LegacyLogMethodAnalyzer extends AbstractCodingRulesAnalyzer {

    public LegacyLogMethodAnalyzer(JavacTask task) {
        super(task);
        treeVisitor = new LegacyLogMethodVisitor();
        eventKind = Kind.ANALYZE;
    }

    private static final Set<String> LEGACY_METHOD_NAMES = new HashSet<>(
            Arrays.asList("error", "mandatoryWarning", "warning", "mandatoryNote", "note", "fragment"));

    class LegacyLogMethodVisitor extends TreeScanner {

        @Override
        public void visitClassDef(JCClassDecl tree) {
            if (!tree.sym.packge().fullname.toString().startsWith("com.sun.tools.javac."))
                return ;
            super.visitClassDef(tree);
        }

        @Override
        public void visitApply(JCMethodInvocation tree) {
            checkLegacyLogMethod(tree);
            super.visitApply(tree);
        }

        void checkLegacyLogMethod(JCMethodInvocation tree) {
            Symbol method = TreeInfo.symbolFor(tree);
            if (method == null ||
                method.kind != Kinds.Kind.MTH ||
                !typeToCheck(method.owner.type) ||
                !LEGACY_METHOD_NAMES.contains(method.name.toString()) ||
                !((MethodSymbol) method).isVarArgs() ||
                method.type.getParameterTypes().size() < 2) {
                return ;
            }
            JCExpression key = tree.args.get(method.type.getParameterTypes().size() - 2);
            if (key.hasTag(Tag.LITERAL)) {
                messages.error(tree, "crules.use.of.legacy.log.method", tree);
            }
        }

        boolean typeToCheck(Type type) {
            Symbol abstractLog = elements.getTypeElement(AbstractLog.class.getName());
            Symbol diagnosticFactory = elements.getTypeElement(JCDiagnostic.Factory.class.getName().replace('$', '.'));
            return types.isSubtype(type, abstractLog.type) ||
                   types.isSubtype(type, diagnosticFactory.type);
        }
    }
}
