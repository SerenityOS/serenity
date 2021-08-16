/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.source.tree.LambdaExpressionTree.BodyKind;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Assert;

/**This analyzer guards against complex messages (i.e. those that use string concatenation) passed
 * to various Assert.check methods.
 */
public class AssertCheckAnalyzer extends AbstractCodingRulesAnalyzer {

    enum AssertOverloadKind {
        EAGER("crules.should.not.use.eager.string.evaluation"),
        LAZY("crules.should.not.use.lazy.string.evaluation"),
        NONE(null);

        String errKey;

        AssertOverloadKind(String errKey) {
            this.errKey = errKey;
        }

        boolean simpleArgExpected() {
            return this == AssertOverloadKind.EAGER;
        }
    }

    public AssertCheckAnalyzer(JavacTask task) {
        super(task);
        treeVisitor = new AssertCheckVisitor();
        eventKind = Kind.ANALYZE;
    }

    class AssertCheckVisitor extends TreeScanner {

        @Override
        public void visitApply(JCMethodInvocation tree) {
            Symbol m = TreeInfo.symbolFor(tree);
            AssertOverloadKind ak = assertOverloadKind(m);
            if (ak != AssertOverloadKind.NONE &&
                !m.name.contentEquals("error")) {
                JCExpression lastParam = tree.args.last();
                if (isSimpleStringArg(lastParam) != ak.simpleArgExpected()) {
                    messages.error(lastParam, ak.errKey);
                }
            }

            super.visitApply(tree);
        }

        AssertOverloadKind assertOverloadKind(Symbol method) {
            if (method == null ||
                !method.owner.getQualifiedName().contentEquals(Assert.class.getName()) ||
                method.type.getParameterTypes().tail == null) {
                return AssertOverloadKind.NONE;
            }
            Type formal = method.type.getParameterTypes().last();
            if (types.isSameType(formal, syms.stringType)) {
                return AssertOverloadKind.EAGER;
            } else if (types.isSameType(types.erasure(formal), types.erasure(syms.supplierType))) {
                return AssertOverloadKind.LAZY;
            } else {
                return AssertOverloadKind.NONE;
            }
        }

        boolean isSimpleStringArg(JCExpression e) {
            switch (e.getTag()) {
                case LAMBDA:
                    JCLambda lambda = (JCLambda)e;
                    return (lambda.getBodyKind() == BodyKind.EXPRESSION) &&
                            isSimpleStringArg((JCExpression)lambda.body);
                default:
                    Symbol argSym = TreeInfo.symbolFor(e);
                    return (e.type.constValue() != null ||
                            (argSym != null && argSym.kind == Kinds.Kind.VAR));
            }
        }
    }
}
