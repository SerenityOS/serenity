/*
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ExecutorCompletionService9Test extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(ExecutorCompletionService9Test.class);
    }

    void solveAll(Executor e,
                  Collection<Callable<Integer>> solvers)
        throws InterruptedException, ExecutionException {
        CompletionService<Integer> cs
            = new ExecutorCompletionService<>(e);
        solvers.forEach(cs::submit);
        for (int i = solvers.size(); i > 0; i--) {
            Integer r = cs.take().get();
            if (r != null)
                use(r);
        }
    }

    void solveAny(Executor e,
                  Collection<Callable<Integer>> solvers)
        throws InterruptedException {
        CompletionService<Integer> cs
            = new ExecutorCompletionService<>(e);
        int n = solvers.size();
        List<Future<Integer>> futures = new ArrayList<>(n);
        Integer result = null;
        try {
            solvers.forEach(solver -> futures.add(cs.submit(solver)));
            for (int i = n; i > 0; i--) {
                try {
                    Integer r = cs.take().get();
                    if (r != null) {
                        result = r;
                        break;
                    }
                } catch (ExecutionException ignore) {}
            }
        } finally {
            futures.forEach(future -> future.cancel(true));
        }

        if (result != null)
            use(result);
    }

    ArrayList<Integer> results;

    void use(Integer x) {
        if (results == null) results = new ArrayList<>();
        results.add(x);
    }

    /**
     * The first "solvers" sample code in the class javadoc works.
     */
    public void testSolveAll()
        throws InterruptedException, ExecutionException {
        results = null;
        Set<Callable<Integer>> solvers = Set.of(
            () -> null,
            () -> 1,
            () -> 2,
            () -> 3,
            () -> null);
        solveAll(cachedThreadPool, solvers);
        results.sort(Comparator.naturalOrder());
        assertEquals(List.of(1, 2, 3), results);
    }

    /**
     * The second "solvers" sample code in the class javadoc works.
     */
    public void testSolveAny()
        throws InterruptedException {
        results = null;
        Set<Callable<Integer>> solvers = Set.of(
            () -> { throw new ArithmeticException(); },
            () -> null,
            () -> 1,
            () -> 2);
        solveAny(cachedThreadPool, solvers);
        assertEquals(1, results.size());
        Integer elt = results.get(0);
        assertTrue(elt.equals(1) || elt.equals(2));
    }

}
