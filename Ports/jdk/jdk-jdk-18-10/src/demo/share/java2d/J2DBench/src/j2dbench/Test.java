/*
 * Copyright (c) 2002, 2011, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */


package j2dbench;

public abstract class Test extends Option.Enable {
    private DependentLink dependencies;

    public Test(Group parent, String nodeName, String description) {
        super(parent, nodeName, description, false);
    }

    public void addDependency(Modifier mod) {
        addDependency(mod, null);
    }

    public void addDependency(Modifier mod, Modifier.Filter filter) {
        dependencies = DependentLink.add(dependencies, mod, filter);
    }

    public void addDependencies(Group g, boolean recursive) {
        addDependencies(g, recursive, null);
    }

    public void addDependencies(Group g, boolean recursive,
                                Modifier.Filter filter)
    {
        if (g instanceof Modifier) {
            addDependency((Modifier) g, filter);
        }
        for (Node n = g.getFirstChild(); n != null; n = n.getNext()) {
            if (n instanceof Modifier) {
                addDependency((Modifier) n, filter);
            } else if (recursive && n instanceof Group) {
                addDependencies((Group) n, recursive, filter);
            }
        }
    }

    public void runTest(TestEnvironment env) {
        if (!env.isStopped() && isEnabled()) {
            dependencies.recurseAndRun(env, this);
        }
    }

    public void runOneTest(TestEnvironment env) {
        if (!env.isStopped()) {
            Result result = new Result(this);
            env.erase();
            Object ctx = initTest(env, result);
            result.setModifiers(env.getModifiers());
            try {
                runTestLoop(env, result, ctx);
            } catch (Throwable t) {
                result.setError(t);
            }
            cleanupTest(env, ctx);
            // Skip recording results if we were interrupted before
            // anything interesting happened...
            if (result.getError() != null || result.getNumRuns() != 0) {
                if (J2DBench.printresults.isEnabled()) {
                    result.summarize();
                }
                env.record(result);
            }
            ctx = null;
            result = null;
            env.idle();  // Also done after this method returns...
        }
    }

    public abstract Object initTest(TestEnvironment env, Result result);
    public abstract void runTest(Object context, int numReps);
    public abstract void cleanupTest(TestEnvironment env, Object context);

    public void runTestLoop(TestEnvironment env, Result result, Object ctx) {
        // Prime the pump
        runTest(ctx, 1);

        // Determine the number of reps
        int numReps = env.getRepCount();
        if (numReps == 0) {
            numReps = calibrate(env, ctx);
        }
        result.setReps(numReps);

        int numRuns = env.getRunCount();
        for (int i = 0; i < numRuns; i++) {
            if (env.idle()) {
                break;
            }

            env.sync();
            env.startTiming();
            runTest(ctx, numReps);
            env.sync();
            env.stopTiming();
            result.addTime(env.getTimeMillis());

            env.flushToScreen();
        }
    }

    public int calibrate(TestEnvironment env, Object ctx) {
        long testTime = env.getTestTime();
        int numReps = 0;
        int totalReps = 0;

        // First do one at a time until we get to 1 second elapsed
        // But, if we get to 1000 reps we'll start ramping up our
        // reps per cycle and throwing sync() calls in to make sure
        // we aren't spinning our gears queueing up graphics calls
        env.idle();
        long now = System.currentTimeMillis();
        long startTime = now;
        while (numReps < 1000 && now < startTime + 1000) {
            runTest(ctx, 1);
            numReps++;
            now = System.currentTimeMillis();
        }

        // Time to shift gears into an exponential number of tests
        // sync() each time in case batching at a lower level is
        // causing us to spin our gears
        env.sync();
        now = System.currentTimeMillis();
        int reps = 250;
        while (now < startTime + 1000) {
            runTest(ctx, reps);
            env.sync();
            numReps += reps;
            reps *= 2;
            now = System.currentTimeMillis();
        }

        // Now keep estimating how many reps it takes to hit our target
        // time exactly, trying it out, and guessing again.
        while (now < startTime + testTime) {
            int estimate = (int) (numReps * testTime / (now - startTime));
            if (estimate <= numReps) {
                estimate = numReps+1;
            }
            runTest(ctx, estimate - numReps);
            numReps = estimate;
            env.sync();
            now = System.currentTimeMillis();
        }

        // Now make one last estimate of how many reps it takes to
        // hit the target exactly in case we overshot.
        int estimate = (int) (numReps * testTime / (now - startTime));
        if (estimate < 1) {
            estimate = 1;
        }
        return estimate;
    }

    /*
     * Finds a new width (w2) such that
     *     (w-2) <= w2 <= w
     *     and w2 is not a multiple of 3 (the X step size)
     *     and GCD(w2, h) is as small as possible
     */
    static int prevw;
    public static int adjustWidth(int w, int h) {
        int bestv = w;
        int bestw = w;
        boolean verbose = (prevw != w && J2DBench.verbose.isEnabled());
        for (int i = 0; i < 3; i++) {
            int w2 = w-i;
            int u = w2;
            int v = h;
            while (u > 0) {
                if (u < v) { int t = u; u = v; v = t; }
                u -= v;
            }
            if (verbose) {
                System.out.println("w = "+w2+", h = "+h+
                                   ", w % 3 == "+(w2 % 3)+
                                   ", gcd(w, h) = "+v);
            }
            if (v < bestv && (w2 % 3) != 0) {
                bestv = v;
                bestw = w2;
            }
        }
        if (verbose) {
            System.out.println("using "+bestw+" (gcd = "+bestv+")");
            prevw = w;
        }
        return bestw;
    }

    public String toString() {
        return "Test("+getTreeName()+")";
    }

    public static class DependentLink {
        public static DependentLink add(DependentLink d, Modifier mod,
                                        Modifier.Filter filter)
        {
            DependentLink dl = new DependentLink(mod, filter);
            if (d == null) {
                d = dl;
            } else {
                DependentLink last = d;
                while (last.next != null) {
                    last = last.next;
                }
                last.next = dl;
            }
            return d;
        }

        private DependentLink next;
        private Modifier mod;
        private Modifier.Filter filter;

        private DependentLink(Modifier mod, Modifier.Filter filter) {
            this.mod = mod;
            this.filter = filter;
        }

        public Modifier getModifier() {
            return mod;
        }

        public Modifier.Filter getFilter() {
            return filter;
        }

        public DependentLink getNext() {
            return next;
        }

        public void recurseAndRun(TestEnvironment env, Test test) {
            Modifier.Iterator iter = mod.getIterator(env);
            while (iter.hasNext()) {
                Object val = iter.next();
                if (filter == null || filter.isCompatible(val)) {
                    mod.modifyTest(env, val);
                    if (next == null) {
                        test.runOneTest(env);
                        env.idle();  // One more time outside of runOneTest()
                    } else {
                        next.recurseAndRun(env, test);
                    }
                    mod.restoreTest(env, val);
                }
            }
        }
    }
}
