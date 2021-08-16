/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm.mlvm.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

public class StratumAPTreeVisitor extends TreePathScanner<Object, Trees> {
    public static final String LABEL_PREFIX = "Stratum_";

    public static class StratumLineInfo implements Comparable<StratumLineInfo> {
        String stratumName;
        int stratumLineOrder;
        String stratumLine;
        int javaLineNum;

        public StratumLineInfo(String stratumName, int stratumLineOrder, String stratumLine, int javaLineNum) {
            this.stratumName = stratumName;
            this.stratumLineOrder = stratumLineOrder;
            this.stratumLine = stratumLine;
            this.javaLineNum = javaLineNum;
        }

        public String getStratumName() {
            return stratumName;
        }

        public int getStratumLineOrder() {
            return stratumLineOrder;
        }

        public String getStratumSourceLine() {
            return stratumLine;
        }

        public int getJavaLineNum() {
            return javaLineNum;
        }

        @Override
        public int compareTo(StratumLineInfo o) {
            int i;
            if ( (i = getStratumName().compareTo(o.getStratumName())) != 0 )
                return i;

            if ( (i = getStratumLineOrder() - o.getStratumLineOrder()) != 0 )
                return i;

            return 0;
        }

        @Override
        public String toString() {
            return getStratumName() + ":" + getStratumLineOrder()
                 + " =>  Java:" + getJavaLineNum()
                 + " [" + getStratumSourceLine() + "]";
        }
    }

    public Map<String, Set<StratumLineInfo>> strata = new HashMap<String, Set<StratumLineInfo>>();

    @Override
    public Object visitLabeledStatement(LabeledStatementTree node, Trees p) {
        processLabel(node, p);
        return super.visitLabeledStatement(node, p);
    }

    private void processLabel(LabeledStatementTree node, Trees p) {
        String label = node.getLabel().toString();

        if ( ! label.startsWith(LABEL_PREFIX) )
            return;

        int stratumNameEndPos = label.indexOf('_', LABEL_PREFIX.length());
        if ( stratumNameEndPos == -1 )
            return;

        String stratumName = label.substring(LABEL_PREFIX.length(), stratumNameEndPos);

        int stratumLineEndPos = label.indexOf('_', stratumNameEndPos + 1);
        if ( stratumLineEndPos == -1 )
            return;

        String stratumLineNumStr = label.substring(stratumNameEndPos + 1, stratumLineEndPos);
        int stratumLineNum = Integer.parseInt(stratumLineNumStr);

        String stratumLine = label.substring(stratumLineEndPos + 1);

        CompilationUnitTree unit = getCurrentPath().getCompilationUnit();
        int javaLineNum = (int) unit.getLineMap().getLineNumber(p.getSourcePositions().getStartPosition(unit, node));

        Set<StratumLineInfo> stratumLines = this.strata.get(stratumName);
        if ( stratumLines == null ) {
            stratumLines = new TreeSet<StratumLineInfo>();
            this.strata.put(stratumName, stratumLines);
        }

        stratumLines.add(new StratumLineInfo(stratumName, stratumLineNum, stratumLine, javaLineNum));
    }
}
