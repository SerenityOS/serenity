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
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @bug 6865571
 * @summary Numerical Integration using fork/join
 * @run main Integrate reps=1 forkPolicy=dynamic
 * @run main Integrate reps=1 forkPolicy=serial
 * @run main Integrate reps=1 forkPolicy=fork
 */

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Sample program using Gaussian Quadrature for numerical integration.
 * This version uses a simplified hardwired function.  Inspired by a
 * <A href="http://www.cs.uga.edu/~dkl/filaments/dist.html">
 * Filaments</A> demo program.
 */
public final class Integrate {

    static final double errorTolerance = 1.0e-11;
    /** for time conversion */
    static final long NPS = (1000L * 1000 * 1000);

    static final int SERIAL = -1;
    static final int DYNAMIC = 0;
    static final int FORK = 1;

    /** the function to integrate */
    static double computeFunction(double x) {
        return (x * x + 1.0) * x;
    }

    static final double start = 0.0;
    static final double end = 1536.0;

    /**
     * The number of recursive calls for
     * integrate from start to end.
     * (Empirically determined)
     */
    static final int calls = 263479047;

    static String keywordValue(String[] args, String keyword) {
        for (String arg : args)
            if (arg.startsWith(keyword))
                return arg.substring(keyword.length() + 1);
        return null;
    }

    static int intArg(String[] args, String keyword, int defaultValue) {
        String val = keywordValue(args, keyword);
        return (val == null) ? defaultValue : Integer.parseInt(val);
    }

    static int policyArg(String[] args, String keyword, int defaultPolicy) {
        String val = keywordValue(args, keyword);
        if (val == null) return defaultPolicy;
        if (val.equals("dynamic")) return DYNAMIC;
        if (val.equals("serial")) return SERIAL;
        if (val.equals("fork")) return FORK;
        throw new Error();
    }

    /**
     * Usage: Integrate [procs=N] [reps=N] forkPolicy=serial|dynamic|fork
     */
    public static void main(String[] args) throws Exception {
        final int procs = intArg(args, "procs",
                                 Runtime.getRuntime().availableProcessors());
        final int forkPolicy = policyArg(args, "forkPolicy", DYNAMIC);

        ForkJoinPool g = new ForkJoinPool(procs);
        System.out.println("Integrating from " + start + " to " + end +
                           " forkPolicy = " + forkPolicy);
        long lastTime = System.nanoTime();

        for (int reps = intArg(args, "reps", 10); reps > 0; reps--) {
            double a;
            if (forkPolicy == SERIAL)
                a = SQuad.computeArea(g, start, end);
            else if (forkPolicy == FORK)
                a = FQuad.computeArea(g, start, end);
            else
                a = DQuad.computeArea(g, start, end);
            long now = System.nanoTime();
            double s = (double) (now - lastTime) / NPS;
            lastTime = now;
            System.out.printf("Calls/sec: %12d", (long) (calls / s));
            System.out.printf(" Time: %7.3f", s);
            System.out.printf(" Area: %12.1f", a);
            System.out.println();
        }
        System.out.println(g);
        g.shutdown();
    }

    // Sequential version
    static final class SQuad extends RecursiveAction {
        static double computeArea(ForkJoinPool pool, double l, double r) {
            SQuad q = new SQuad(l, r, 0);
            pool.invoke(q);
            return q.area;
        }

        final double left;       // lower bound
        final double right;      // upper bound
        double area;

        SQuad(double l, double r, double a) {
            this.left = l; this.right = r; this.area = a;
        }

        public final void compute() {
            double l = left;
            double r = right;
            area = recEval(l, r, (l * l + 1.0) * l, (r * r + 1.0) * r, area);
        }

        static final double recEval(double l, double r, double fl,
                                    double fr, double a) {
            double h = (r - l) * 0.5;
            double c = l + h;
            double fc = (c * c + 1.0) * c;
            double hh = h * 0.5;
            double al = (fl + fc) * hh;
            double ar = (fr + fc) * hh;
            double alr = al + ar;
            if (Math.abs(alr - a) <= errorTolerance)
                return alr;
            else
                return recEval(c, r, fc, fr, ar) + recEval(l, c, fl, fc, al);
        }

    }

    //....................................

    // ForkJoin version
    static final class FQuad extends RecursiveAction {
        static double computeArea(ForkJoinPool pool, double l, double r) {
            FQuad q = new FQuad(l, r, 0);
            pool.invoke(q);
            return q.area;
        }

        final double left;       // lower bound
        final double right;      // upper bound
        double area;

        FQuad(double l, double r, double a) {
            this.left = l; this.right = r; this.area = a;
        }

        public final void compute() {
            double l = left;
            double r = right;
            area = recEval(l, r, (l * l + 1.0) * l, (r * r + 1.0) * r, area);
        }

        static final double recEval(double l, double r, double fl,
                                    double fr, double a) {
            double h = (r - l) * 0.5;
            double c = l + h;
            double fc = (c * c + 1.0) * c;
            double hh = h * 0.5;
            double al = (fl + fc) * hh;
            double ar = (fr + fc) * hh;
            double alr = al + ar;
            if (Math.abs(alr - a) <= errorTolerance)
                return alr;
            FQuad q = new FQuad(l, c, al);
            q.fork();
            ar = recEval(c, r, fc, fr, ar);
            if (!q.tryUnfork()) {
                q.quietlyJoin();
                return ar + q.area;
            }
            return ar + recEval(l, c, fl, fc, al);
        }

    }

    // ...........................

    // Version using on-demand Fork
    static final class DQuad extends RecursiveAction {
        static double computeArea(ForkJoinPool pool, double l, double r) {
            DQuad q = new DQuad(l, r, 0);
            pool.invoke(q);
            return q.area;
        }

        final double left;       // lower bound
        final double right;      // upper bound
        double area;

        DQuad(double l, double r, double a) {
            this.left = l; this.right = r; this.area = a;
        }

        public final void compute() {
            double l = left;
            double r = right;
            area = recEval(l, r, (l * l + 1.0) * l, (r * r + 1.0) * r, area);
        }

        static final double recEval(double l, double r, double fl,
                                    double fr, double a) {
            double h = (r - l) * 0.5;
            double c = l + h;
            double fc = (c * c + 1.0) * c;
            double hh = h * 0.5;
            double al = (fl + fc) * hh;
            double ar = (fr + fc) * hh;
            double alr = al + ar;
            if (Math.abs(alr - a) <= errorTolerance)
                return alr;
            DQuad q = null;
            if (getSurplusQueuedTaskCount() <= 3)
                (q = new DQuad(l, c, al)).fork();
            ar = recEval(c, r, fc, fr, ar);
            if (q != null && !q.tryUnfork()) {
                q.quietlyJoin();
                return ar + q.area;
            }
            return ar + recEval(l, c, fl, fc, al);
        }

    }

}
