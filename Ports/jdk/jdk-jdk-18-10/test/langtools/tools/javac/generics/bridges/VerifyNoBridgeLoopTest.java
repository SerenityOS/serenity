/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8189659
 * @summary Verify that the infinite loop of bridges from JDK-6996415 is not reintroduced
 * @run main VerifyNoBridgeLoopTest
 */

public class VerifyNoBridgeLoopTest {
    static class Expression {}
    abstract static class ExpVisitor<R,D> {
      static int f = 1;
      protected R visitExpression (Expression exp, D d) { f *= 100; System.out.println(exp); return null; }
    }

    abstract static class ExpExpVisitor<D> extends ExpVisitor<Expression,D> { }

    static class FindTail extends ExpExpVisitor<Expression> {
      protected Expression visitExpression (Expression exp, Expression returnContinuation) {
          return super.visitExpression(exp, exp);
      }
    }
    public static void main(String [] args) {
        new FindTail().visitExpression(new Expression(), new Expression());
        ExpVisitor<Expression, Expression> e = new FindTail();
        e.visitExpression(new Expression(), new Expression());
        if (e.f != 10000)
            throw new AssertionError("Incorrect call sequence");
    }
}
