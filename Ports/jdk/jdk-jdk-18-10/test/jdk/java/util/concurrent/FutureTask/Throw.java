/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6415572
 * @summary Check exceptional behavior in run and done methods
 */

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class Throw {

    static void THROW(final Throwable t) {
        if (t != null)
            Throw.<RuntimeException>uncheckedThrow(t);
    }

    Callable<Void> thrower(final Throwable t) {
        return new Callable<Void>() { public Void call() {
            THROW(t); return null; }};
    }

    @SuppressWarnings("serial")
    private static class DoneError extends Error {}

    @SuppressWarnings("serial")
    private static class DoneException extends RuntimeException {}

    static class MyFutureTask extends FutureTask<Void> {
        MyFutureTask(Callable<Void> task) { super(task); }
        public boolean runAndReset() { return super.runAndReset(); }
    }

    MyFutureTask checkTask(final MyFutureTask task) {
        check(! task.isCancelled());
        check(! task.isDone());
        return task;
    }

    MyFutureTask taskFor(final Throwable callableThrowable,
                         final Throwable doneThrowable) {
        return checkTask(
            new MyFutureTask(thrower(callableThrowable)) {
                protected void done() { THROW(doneThrowable); }});
    }

    void test(String[] args) throws Throwable {
        final Throwable[] callableThrowables = {
            null, new Exception(), new Error(), new RuntimeException() };
        final Throwable[] doneThrowables = {
            new DoneError(), new DoneException() };
        for (final Throwable c : callableThrowables) {
            for (final Throwable d : doneThrowables) {
                THROWS(d.getClass(),
                       new F(){void f(){
                           taskFor(c, d).cancel(false);}},
                       new F(){void f(){
                           taskFor(c, d).run();}});
                if (c != null)
                    THROWS(d.getClass(),
                           new F(){void f(){
                               taskFor(c, d).runAndReset();}});
            }

            try {
                final MyFutureTask task = taskFor(c, null);
                check(task.cancel(false));
                THROWS(CancellationException.class,
                       new F(){void f() throws Throwable { task.get(); }});
            } catch (Throwable t) { unexpected(t); }

            if (c != null) {
                final MyFutureTask task = taskFor(c, null);
                task.run();
                try {
                    task.get();
                    fail("Expected ExecutionException");
                } catch (ExecutionException ee) {
                    equal(c.getClass(), ee.getCause().getClass());
                } catch (Throwable t) { unexpected(t); }
            }

            if (c != null) {
                final MyFutureTask task = taskFor(c, null);
                task.runAndReset();
                try {
                    task.get();
                    fail("Expected ExecutionException");
                } catch (ExecutionException ee) {
                    check(c.getClass().isInstance(ee.getCause()));
                } catch (Throwable t) { unexpected(t); }
            }
        }
    }

    //--------------------- Infrastructure ---------------------------
    volatile int passed = 0, failed = 0;
    void pass() {passed++;}
    void fail() {failed++; Thread.dumpStack();}
    void fail(String msg) {System.err.println(msg); fail();}
    void unexpected(Throwable t) {failed++; t.printStackTrace();}
    void check(boolean cond) {if (cond) pass(); else fail();}
    void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        new Throw().instanceMain(args);}
    void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
    abstract class F {abstract void f() throws Throwable;}
    void THROWS(Class<? extends Throwable> k, F... fs) {
        for (F f : fs)
            try {f.f(); fail("Expected " + k.getName() + " not thrown");}
            catch (Throwable t) {
                if (k.isAssignableFrom(t.getClass())) pass();
                else unexpected(t);}}
    @SuppressWarnings("unchecked")
    static <T extends Throwable> void uncheckedThrow(Throwable t) throws T {
        throw (T)t; // rely on vacuous cast
    }
}
