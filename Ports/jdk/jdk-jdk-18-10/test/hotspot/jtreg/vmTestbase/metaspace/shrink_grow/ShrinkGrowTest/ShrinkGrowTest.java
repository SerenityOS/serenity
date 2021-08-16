/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @bug 8217432
 * @summary converted from VM Testbase metaspace/shrink_grow/ShrinkGrowTest.
 *
 * @requires vm.opt.final.ClassUnloading
 * @library /vmTestbase /test/lib
 * @run main/othervm
 *      -XX:MetaspaceSize=10m
 *      -XX:MaxMetaspaceSize=20m
 *      -Xlog:gc*:gc.log
 *      metaspace.shrink_grow.ShrinkGrowTest.ShrinkGrowTest
 */

package metaspace.shrink_grow.ShrinkGrowTest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the main test in the metaspace shrink/grow series.
 *
 * It tries to allocate all available metespace (loads new classes and keeps
 * them in map), then checks that loading new classes causes OOM.
 * After that it does cleanup loaded classes and then expect the new classes
 * could be loaded again.
 *
 * <b>Note</b>: Don't forget to limit the metaspace size by giving
 * -XX:MaxMetaspaceSize=100k vm option.
 */
public class ShrinkGrowTest {

    /**
     * Dead classes storage.
     */
    private final Map<String, ShrinkGrowTest.Foo> loadedClasses = new HashMap<>();

    private static int counter = 0;

    private String errorMessage = "not completed";

     // thread id to distinguish threads in output
    private final String whoAmI;

    // the limit of classes to load expecting OOM
    private final int maxClassesToLoad;

    public static void main(String[] args) {
        String name = args.length > 0 ? args[0] : "singleTest" ;
        new ShrinkGrowTest(name, 20000).run();
    }

    /**
     * @param name - thread id used in logging
     * @param classesToLoad - the limit of classes to load expecting OOM
     */
    public ShrinkGrowTest(String name, int classesToLoad) {
        whoAmI = name;
        maxClassesToLoad = classesToLoad;

    }

    /**
     * Just outputs given message preceeded with the thread identifier
     *
     * @param message text to print out
     */
    void log(String message) {
        System.out.println("%" + whoAmI + "% " + message);
    }

    void throwFault(String message) {
        throw new TestFault("%" + whoAmI + "% " + message);
    }

    void throwFault(String message, Throwable t) {
        throw new TestFault("%" + whoAmI + "% " + message, t);
    }

    /**
     * Entry to the test.
     * Just exits if passes or throws an Error if failed.
     */
    public void run() {
        if (System.getProperty("requiresCompressedClassSpace") != null &&
                   !isCompressedClassSpaceAvailable()) {
                System.out.println("Not applicalbe, Compressed Class Space is required");
            return;
        }

        try {
            log("Bootstrapping string concatenation for " + whoAmI );
            go();
            // The quest completed! Yahoo!
            setErrorMessage(null);
            log("passed");
        } catch (TestFault failure) {
            failure.printStackTrace(System.err);
            setErrorMessage(failure.getMessage());
            log("failed :" + errorMessage);
            throw failure;
        } catch (Throwable badThing) {
            setErrorMessage(badThing.toString());
            throw new TestFault(badThing);
        }
    }

    private void go() {
        // step 1: eat all metaspace
        log("eating metaspace");
        runOutOfMetaspace(maxClassesToLoad);

        // step 2: try to load one more class
        // it should be impossible
        try {
            eatALittleMemory();
            throwFault("We haven't cleaned metaspace yet!");
        } catch (OutOfMemoryError error) {
            if (!isMetaspaceError(error)) {
                throwFault("Hmm, we ran out metaspace. Metaspace error is still excpected here " + error, error);
            }
        }

        // step 3: clean up metaspace and try loading a class again.
        log("washing hands before meal");
        loadedClasses.clear();
        System.gc();
        try {
            log("one more try to eat");
            eatALittleMemory();
        } catch (OutOfMemoryError error) {
            throwFault("we already should be able to consume metaspace " + error, error);
        }
    }

    /**
     * @return true if the test has successfully passed.
     */
    public boolean isPassed() {
        return errorMessage == null;
    }

    /**
     * @return message describing the reason of failure, or null if passes
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the message describing why test failed, or null if test passed
     */
    void setErrorMessage(String msg) {
        errorMessage = msg;
    }

    /**
     * Loads new classes until OOM.
     * Checks that OOM is caused by metaspace and throws an Error if not.
     *
     * @param times - maximum limit of classes to load.
     */
    private void runOutOfMetaspace(int times) {
        try {
            for (int i = 0; i < times; i++) {
                eatALittleMemory();
            }
        } catch (OutOfMemoryError error) {
            if (isMetaspaceError(error)) {
                return;
            }
            throwFault("We ran out of another space, not metaspace: " + error, error);
        }
        throwFault("OOM hasn't happened after " + times + " iterations. Might be too much space?..");
    }

    /**
     * Imitates class loading.
     * Each invocation of this method causes a new class loader object is created
     * and a new class is loaded by this class loader.
     * Method throws OOM when run out of memory.
     */
    private void eatALittleMemory() {
        try {
            String jarUrl = "file:" + counter + ".jar";
            counter++;
            URL[] urls = new URL[]{new URL(jarUrl)};
            URLClassLoader cl = new URLClassLoader(urls);
            ShrinkGrowTest.Foo foo = (ShrinkGrowTest.Foo) Proxy.newProxyInstance(cl,
                    new Class[]{ShrinkGrowTest.Foo.class},
                    new ShrinkGrowTest.FooInvocationHandler(new ShrinkGrowTest.FooBar()));
            loadedClasses.put(jarUrl, foo);
        } catch (java.net.MalformedURLException badThing) {
            // should never occur
            throwFault("Unexpeted error: " + badThing, badThing);
        }

    }

    /**
     * Checks if given OOM is about metaspace
     * @param error OOM
     * @return true if message contains 'metaspace' word, false otherwise.
     */
    boolean isMetaspaceError(OutOfMemoryError error) {
            String message = error.getMessage();
        return message != null && (message.contains("Metaspace") ||
                        message.contains("Compressed class space"));
    }

    boolean isCompressedClassSpaceAvailable() {
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getName().equalsIgnoreCase("Compressed class space")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Runtime exception signaling test failure.
     */
    public static class TestFault extends RuntimeException {
        public TestFault(String message) {
            super(message);
        }
        public TestFault(Throwable t) {
            super(t);
        }
        public TestFault(String message, Throwable t) {
            super(message, t);
        }
    }

    public static interface Foo {
    }

    public static class FooBar implements ShrinkGrowTest.Foo {
    }

    class FooInvocationHandler implements InvocationHandler {
        private final ShrinkGrowTest.Foo foo;

        FooInvocationHandler(ShrinkGrowTest.Foo foo) {
            this.foo = foo;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return method.invoke(foo, args);
        }
    }
}
