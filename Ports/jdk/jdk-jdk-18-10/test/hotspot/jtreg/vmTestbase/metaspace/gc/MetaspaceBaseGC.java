/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package metaspace.gc;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jdk.internal.misc.Unsafe;

/**
 * Test that checks how GC works with Metaspace and "Compared Class Space".
 *
 * It comprises 3 test cases:
 * <ul>
 * <li>testcase1 - checks that used/committed memory doesn't grow
 * when gc is invoked</li>
 * <li>testcase2 - checks that gc is invoked when the class metadata  u
 * sage reaches MetaspaceSize</li>
 * <li>testcase3 - checks used/committed grow, inspite of gc is invoked</li>
 * </ul>
 *
 * It's supposed that this class will be executed with various setting of VM
 * flags. Via execute args it's possible to say which test cases to run and
 * what space to test: Metaspace or Compared Class Space.
 */
public abstract class MetaspaceBaseGC {

    // storage of loaded classes
    private final Map<String, MetaspaceBaseGC.Foo> loadedClasses = new HashMap<>();
    private static int counter = 0;

    // pool to test
    protected MemoryPoolMXBean pool = null;

    // memory page size
    protected static final long PAGE_SIZE = detectPageSize();

    // true when PAGE_SIZE is large and
    protected boolean useLargepages = false;

    // where the log will be saved
    protected String gclogFileName = null;

    protected final Set<String> vmArgs = new HashSet<>();

    protected abstract void parseArgs(String args[]);
    protected abstract String getPoolName();
    protected abstract void doCheck();

    public final void run(String args[]) {
        configure(args);
        if (pool == null) {
            System.out.println("%%% Cannot pull the pool, most likely 32-bits only");
            return;
        }
        System.out.println("%%% Working with " + getPoolName());
        for (String vmA: vmArgs) {
            if (vmA.contains("Metaspace") || vmA.contains("Compressed")) {
                System.out.println("%  " + vmA);
            }
        }
        doCheck();
        System.out.println("% Test passed.");
    }


    protected void configure(String args[]) {
        vmArgs.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());

        System.out.println(vmArgs);

        pool = getMemoryPool(getPoolName());
        if (pool == null) {
            return; // nothing to check
        }
        for (String arg: vmArgs) {
            if (arg.startsWith("-Xlog:gc") && arg.length() > 8) {
               gclogFileName = arg.substring(arg.lastIndexOf(':') + 1);
            }
        }
        parseArgs(args);
    }


    /**
     * Imitates class loading.
     * Each invocation of this method causes a new class loader object is created
     * and a new class is loaded by this class loader.
     * Method throws OOM when run out of memory.
     *
     * @param times how many classes to load
     * @param keepRefs true, if references to created classes should be stored
     */
    protected void loadNewClasses(int times, boolean keepRefs) {
        for (int i = 0; i < times; i++) {
            try {
                String jarUrl = "file:" + counter + ".jar";
                counter++;
                URL[] urls = new URL[]{new URL(jarUrl)};
                URLClassLoader cl = new URLClassLoader(urls);
                MetaspaceBaseGC.Foo foo = (MetaspaceBaseGC.Foo) Proxy.newProxyInstance(cl,
                        new Class[]{MetaspaceBaseGC.Foo.class},
                        new MetaspaceBaseGC.FooInvocationHandler(new MetaspaceBaseGC.FooBar()));
                if (keepRefs) {
                    loadedClasses.put(jarUrl, foo);
                }
            } catch (java.net.MalformedURLException badThing) {
                // should never occur
                System.err.println("Unexpeted error: " + badThing);
                throw new RuntimeException(badThing);
            }
        }

    }

    /**
     * Cleans references to loaded classes.
     */
    protected void cleanLoadedClasses() {
        loadedClasses.clear();
    }

    /**
     * Invokes System.gc() and sleeps a little.
     */
    protected void gc() {
        System.gc();
        try {
            Thread.currentThread().sleep(500);
        } catch (Exception whatever) {
        }
    }

    /**
     * Reads gc.log file and returns it as a list of lines.
     * It's supposed that the test is executed with -Xlog:gc:gc.log option.
     *
     * @return List of strings the gc.log file is comprised.
     * @throws IOException if problem occurred while reading.
     */
    protected List<String> readGCLog() throws IOException {
        return Files.readAllLines(Paths.get(".", gclogFileName));
    }

    /**
     * Reads gc.log file and counts GC induced by metaspace.
     * @return how many times GC induced by metaspace has occurred.
     */
    protected int getMetaspaceGCCount() {
        int count = 0;
        try {
            for (String line: readGCLog()) {
                if (line.indexOf("Metadata GC ") > 0) {
                    count++;
                }
            }
            return count;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            return -1;
        }
    }

    protected String lastGCLogLine() {
        if (gclogFileName == null) {
            return "";
        }
        try {
            List<String> list = Files.readAllLines(Paths.get(".", gclogFileName));
            return list.get(list.size() - 1);
        } catch (IOException e) {
            return "File not found";
        }
    }

    /**
     * Does it best to checks if the last GC was caused by metaspace.
     *
     * This method looks into gc.log file (if -Xloggc:file is given) and returns
     * true if the last line in the log contains the "Metadata" word.
     * It's not very reliable way to check, log might not be flushed yet.
     *
     * @return
     */
    protected boolean isMetaspaceGC() {
        return lastGCLogLine().contains("Metadata");
    }

    /**
     * Prints amounts of used and committed metaspace preceeded by the message
     * @param mesg a message to printed prior usages
     */
    protected void printMemoryUsage(String mesg) {
        MemoryUsage mu = pool.getUsage();
        printMemoryUsage(mesg, mu.getUsed(), mu.getCommitted());
    }
    protected void printMemoryUsage(String mesg, long v1, long v2) {
        System.out.println(mesg + ": " + bytes2k(v1) + "   :   " + bytes2k(v2));
    }
    protected String bytes2k(long v) {
        return (v / 1024) + "k";
    }



    /**
     * @return amount of used memory
     */
    public long getUsed() {
        return pool.getUsage().getUsed();
    }

    /**
     * @return amount of committed memory
     */
    public long getCommitted() {
        return pool.getUsage().getCommitted();
    }

    private static MemoryPoolMXBean getMemoryPool(String name) {
        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean pool : pools) {
            if (pool.getName().equals(name)) {
                return pool;
            }
        }
        return null;
    }

    private static long detectPageSize() {
        try {
            Unsafe unsafe = Unsafe.getUnsafe();

            int pageSize = unsafe.pageSize();
            System.out.println("Page size: " + pageSize);
            return pageSize;
        } catch (Exception e) {
            throw new Fault("Cannot detect page size");
        }
    }


    long parseValue(String s) {
        s = s.toLowerCase();
        int multiplier = 1;
        switch (s.charAt(s.length() - 1)) {
            case 'g': multiplier = 1024*1024*1024; break;
            case 'm': multiplier = 1024*1024; break;
            case 'k': multiplier = 1024; break;
        }
        if (multiplier == 1) {
            return Long.parseLong(s);
        } else {
            return Long.parseLong(s.substring(0, s.length() - 1)) * multiplier;
        }
    }

    public static interface Foo {
    }

    public static class FooBar implements Foo {
    }

    class FooInvocationHandler implements InvocationHandler {
        private final Foo foo;

        FooInvocationHandler(MetaspaceBaseGC.Foo foo) {
            this.foo = foo;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return method.invoke(foo, args);
        }
    }

}
