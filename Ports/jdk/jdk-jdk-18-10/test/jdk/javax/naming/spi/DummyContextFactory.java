/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.NamingManager;
import java.util.Hashtable;

public class DummyContextFactory implements InitialContextFactory {
    static final String DUMMY_FACTORY = "DummyContextFactory";
    static final String DUMMY_FACTORY2 = "DummyContextFactory2";
    static final String MISSING_FACTORY = "NonExistant";
    static int counter = 0;
    ClassLoader origContextLoader = Thread.currentThread().getContextClassLoader();

    public static void main(String[] s) throws Exception {
        DummyContextFactory dcf = new DummyContextFactory();
        dcf.runTest();
    }

    private void runTest() throws Exception {
        final String classes = System.getProperty("url.dir", ".");
        final URL curl = new File(classes).toURI().toURL();
        URLClassLoader testLoader = new URLClassLoader(new URL[] {curl}, null);
        WeakReference<URLClassLoader> weakRef = new WeakReference<>(testLoader);
        Thread.currentThread().setContextClassLoader(testLoader);
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, DUMMY_FACTORY);
        testContextCalls(env);

        // now test with another factory
        Thread.currentThread().setContextClassLoader(testLoader);
        env.put(Context.INITIAL_CONTEXT_FACTORY, DUMMY_FACTORY2);
        testContextCalls(env);

        // one count is derived from a default constructor call (ignored for test)
        // class associated with this ClassLoader should have 2 counts
        if (counter != 2) {
            throw new RuntimeException("wrong count: " + counter);
        }

        // a test for handling non-existent classes
        env.put(Context.INITIAL_CONTEXT_FACTORY, MISSING_FACTORY);
        testBadContextCall(env);

        // test that loader gets GC'ed
        testLoader = null;
        System.gc();
        while (weakRef.get() != null) {
            Thread.sleep(100);
            System.gc();
        }
    }

    private void testContextCalls(Hashtable<String, String> env) throws Exception {
        // the context is returned here but it's the ContextFactory that
        // we're mainly interested in. Hence the counter test.

        // 1st call populates the WeakHashMap
        // Uses URLClassLoader
        Context cxt = NamingManager.getInitialContext(env);

        // 2nd call uses cached factory
        cxt = NamingManager.getInitialContext(env);

        Thread.currentThread().setContextClassLoader(origContextLoader);

        // 3rd call uses new factory
        // AppClassLoader
        cxt = NamingManager.getInitialContext(env);

        // test with null TCCL
        // this shouldn't increase the count since a null TCCL
        // means we default to System ClassLoader in this case (AppClassLoader)
        Thread.currentThread().setContextClassLoader(null);
        cxt = NamingManager.getInitialContext(env);
    }

    private void testBadContextCall(Hashtable<String, String> env) throws Exception {
        try {
            Context cxt = NamingManager.getInitialContext(env);
            throw new RuntimeException("Expected NoInitialContextException");
        } catch (NoInitialContextException e) {
            if (!(e.getCause() instanceof ClassNotFoundException)) {
                throw new RuntimeException("unexpected cause", e.getCause());
            }
        }
    }

    public DummyContextFactory() {
        System.out.println("New DummyContextFactory " + (++counter));
        //new Throwable().printStackTrace(System.out);
    }

    @Override
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        return new DummyContext(environment);
    }

    public class DummyContext extends InitialContext {

        private Hashtable<?, ?> env;

        DummyContext(Hashtable<?, ?> env) throws NamingException {
            this.env = env;
        }

        public Hashtable<?, ?> getEnvironment() {
            return env;
        }
    }
}
