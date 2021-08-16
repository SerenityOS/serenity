/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jstat;

import java.util.*;
import sun.jvmstat.monitor.*;

/**
 * A class implementing the ExpressionEvaluator to evaluate an expression
 * in the context of the available monitoring data.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class ExpressionExecuter implements ExpressionEvaluator {
    private static final boolean debug =
            Boolean.getBoolean("ExpressionEvaluator.debug");
    private MonitoredVm vm;
    private HashMap<String, Object> map = new HashMap<String, Object>();

    ExpressionExecuter(MonitoredVm vm) {
        this.vm = vm;
    }

    /*
     * evaluate the given expression.
     */
    public Object evaluate(Expression e) {
        if (e == null) {
            return null;
        }

        if (debug) {
            System.out.println("Evaluating expression: " + e);
        }

        if (e instanceof Literal) {
            return ((Literal)e).getValue();
        }

        if (e instanceof Identifier) {
            Identifier id = (Identifier)e;
            if (map.containsKey(id.getName())) {
                return map.get(id.getName());
            } else {
                // cache the data values for coherency of the values over
                // the life of this expression executer.
                Monitor m = (Monitor)id.getValue();
                Object v = m.getValue();
                map.put(id.getName(), v);
                return v;
            }
        }

        Expression l = e.getLeft();
        Expression r = e.getRight();

        Operator op = e.getOperator();

        if (op == null) {
            return evaluate(l);
        } else {
            double lval = ((Number)evaluate(l)).doubleValue();
            double rval = ((Number)evaluate(r)).doubleValue();
            double result = op.eval(lval, rval);
            if (debug) {
                System.out.println("Performed Operation: " + lval + op + rval
                                   + " = " + result);
            }
            return Double.valueOf(result);
        }
    }
}
