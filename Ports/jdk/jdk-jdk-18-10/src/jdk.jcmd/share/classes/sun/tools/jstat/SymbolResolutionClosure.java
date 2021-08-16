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

import java.text.*;
import sun.jvmstat.monitor.MonitorException;

/**
 * A class implementing the Closure interface which is used to resolve
 * all the symbols in the expressions contained in ColumnFormat objects.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class SymbolResolutionClosure implements Closure {
    private static final boolean debug =
            Boolean.getBoolean("SymbolResolutionClosure.debug");

    private ExpressionEvaluator ee;

    public SymbolResolutionClosure(ExpressionEvaluator ee) {
        this.ee = ee;
    }

    public void visit(Object o, boolean hasNext) throws MonitorException {
        if (! (o instanceof ColumnFormat)) {
            return;
        }

        ColumnFormat c = (ColumnFormat)o;
        Expression e = c.getExpression();
        String previous = e.toString();
        e = (Expression)ee.evaluate(e);
        if (debug) {
            System.out.print("Expression: " + previous + " resolved to "
                             + e.toString());
        }
        c.setExpression(e);
    }
}
