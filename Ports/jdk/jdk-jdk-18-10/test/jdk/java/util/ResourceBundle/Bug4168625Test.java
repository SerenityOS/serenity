/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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
    @test
    @summary test Resource Bundle for bug 4168625
    @build Bug4168625Class Bug4168625Getter Bug4168625Resource Bug4168625Resource3 Bug4168625Resource3_en Bug4168625Resource3_en_CA Bug4168625Resource3_en_IE Bug4168625Resource3_en_US Bug4168625Resource2_en_US Bug4168625Resource2
    @run main/timeout=600 Bug4168625Test
    @bug 4168625 6993339
*/
/*
 *
 *
 * (C) Copyright IBM Corp. 1999 - All Rights Reserved
 *
 * The original version of this source code and documentation is
 * copyrighted and owned by IBM. These materials are provided
 * under terms of a License Agreement between IBM and Sun.
 * This technology is protected by multiple US and International
 * patents. This notice and attribution to IBM may not be removed.
 *
 */

import java.util.*;
import java.io.*;

/**
 *  This test tries to correct two efficiency problems with the caching
 *  mechanism of ResourceBundle.  It also allows concurrent loads
 *  of resource bundles to be performed if the bundles are unrelated (ex. a
 *  load of a local system resource by one thread while another thread is
 *  doing a slow load over a network).
 */
public class Bug4168625Test extends RBTestFmwk {
    public static void main(String[] args) throws Exception {
        new Bug4168625Test().run(args);
    }

    /**
     * Verify that getBundle will do something reasonable when part of the
     * resource hierarchy is missing.
     */
    public void testMissingParent() throws Exception {
        final Locale oldDefault = Locale.getDefault();
        Locale.setDefault(new Locale("en", "US"));
        try {
            final Locale loc = new Locale("jf", "jf");
            ResourceBundle bundle = ResourceBundle.getBundle("Bug4168625Resource2", loc);
            final String s1 = bundle.getString("name");
            if (!s1.equals("Bug4168625Resource2_en_US")) {
                errln("getBundle did not find leaf bundle: "+bundle.getClass().getName());
            }
            final String s2 = bundle.getString("baseName");
            if (!s2.equals("Bug4168625Resource2")) {
                errln("getBundle did not set up proper inheritance chain");
            }
        } finally {
            Locale.setDefault(oldDefault);
        }
    }

    /**
     *  Previous versions of ResourceBundle have had the following
     *  caching behavior.  Assume the classes
     *  Bug4168625Resource_fr_FR, Bug4168625Resource_fr,
     *  Bug4168625Resource_en_US, and Bug4168625Resource_en don't
     *  exist.  The class Bug4168625Resource does.  Assume the default
     *  locale is en_US.
     *  <P>
     *  <pre>
     *  getBundle("Bug4168625Resource", new Locale("fr", "FR"));
     *      -->try to load Bug4168625Resource_fr_FR
     *      -->try to load Bug4168625Resource_fr
     *      -->try to load Bug4168625Resource_en_US
     *      -->try to load Bug4168625Resource_en
     *      -->load Bug4168625Resource
     *      -->cache Bug4168625Resource as Bug4168625Resource
     *      -->cache Bug4168625Resource as Bug4168625Resource_en
     *      -->cache Bug4168625Resource as Bug4168625Resource_en_US
     *      -->return Bug4168625Resource
     *  getBundle("Bug4168625Resource", new Locale("fr", "FR"));
     *      -->try to load Bug4168625Resource_fr_FR
     *      -->try to load Bug4168625Resource_fr
     *      -->find cached Bug4168625Resource_en_US
     *      -->return Bug4168625Resource_en_US (which is realy Bug4168625Resource)
     *  </pre>
     *  <P>
     *  The second call causes two loads for Bug4168625Resource_fr_FR and
     *  Bug4168625Resource_en which have already been tried and failed.  These
     *  two loads should have been cached as Bug4168625Resource by the first
     *  call.
     *
     *  The following, more efficient behavior is desired:
     *  <P>
     *  <pre>
     *  getBundle("Bug4168625Resource", new Locale("fr", "FR"));
     *      -->try to load Bug4168625Resource_fr_FR
     *      -->try to load Bug4168625Resource_fr
     *      -->try to load Bug4168625Resource_en_US
     *      -->try to load Bug4168625Resource_en
     *      -->load Bug4168625Resource
     *      -->cache Bug4168625Resource as Bug4168625Resource
     *      -->cache Bug4168625Resource as Bug4168625Resource_en
     *      -->cache Bug4168625Resource as Bug4168625Resource_en_US
     *      -->cache Bug4168625Resource as Bug4168625Resource_fr
     *      -->cache Bug4168625Resource as Bug4168625Resource_fr_FR
     *      -->return Bug4168625Resource
     *  getBundle("Bug4168625Resource", new Locale("fr", "FR"));
     *      -->find cached Bug4168625Resource_fr_FR
     *      -->return Bug4168625Resource_en_US (which is realy Bug4168625Resource)
     *  </pre>
     *  <P>
     *
     */
    public void testCacheFailures() throws Exception {
        checkResourceLoading("Bug4168625Resource", new Locale("fr", "FR"));
    }

    /**
     *  Previous versions of ResourceBundle have had the following
     *  caching behavior.  Assume the current locale is locale is en_US.
     *  The classes Bug4168625Resource_en_US, and Bug4168625Resource_en don't
     *  exist.  The class Bug4168625Resource does.
     *  <P>
     *  <pre>
     *  getBundle("Bug4168625Resource", new Locale("en", "US"));
     *      -->try to load Bug4168625Resource_en_US
     *      -->try to load Bug4168625Resource_en
     *      -->try to load Bug4168625Resource_en_US
     *      -->try to load Bug4168625Resource_en
     *      -->load Bug4168625Resource
     *      -->cache Bug4168625Resource as Bug4168625Resource
     *      -->cache Bug4168625Resource as Bug4168625Resource_en
     *      -->cache Bug4168625Resource as Bug4168625Resource_en_US
     *      -->return Bug4168625Resource
     *  </pre>
     *  <P>
     *  The redundant loads of Bug4168625Resource_en_US and Bug4168625Resource_en
     *  should not occur.  The desired behavior is as follows:
     *  <P>
     *  <pre>
     *  getBundle("Bug4168625Resource", new Locale("en", "US"));
     *      -->try to load Bug4168625Resource_en_US
     *      -->try to load Bug4168625Resource_en
     *      -->load Bug4168625Resource
     *      -->cache Bug4168625Resource as Bug4168625Resource
     *      -->cache Bug4168625Resource as Bug4168625Resource_en
     *      -->cache Bug4168625Resource as Bug4168625Resource_en_US
     *      -->return Bug4168625Resource
     *  </pre>
     *  <P>
     */
    public void testRedundantLoads() throws Exception {
        checkResourceLoading("Bug4168625Resource", Locale.getDefault());
    }

    /**
     * Ensure that resources are only loaded once and are cached correctly
     */
    private void checkResourceLoading(String resName, Locale l) throws Exception {
        final Loader loader = new Loader( new String[] { "Bug4168625Class" }, new String[] { "Bug4168625Resource3_en_US", "Bug4168625Resource3_en_CA" });
        final Class c = loader.loadClass("Bug4168625Class");
        Bug4168625Getter test = (Bug4168625Getter)c.newInstance();
        final String resClassName;
        if (l.toString().length() > 0) {
            resClassName = resName+"_"+l;
        } else {
            resClassName = resName;
        }

        Object bundle = test.getResourceBundle(resName, l);
        loader.logClasses("Initial lookup of "+resClassName+" generated the following loads:");

        final Vector lastLoad = new Vector(loader.loadedClasses.size());
        boolean dups = false;
        for (int i = loader.loadedClasses.size() - 1; i >= 0 ; i--) {
            final Object item = loader.loadedClasses.elementAt(i);
            loader.loadedClasses.removeElementAt(i);
            if (loader.loadedClasses.contains(item)) {
                logln("Resource loaded more than once: "+item);
                dups = true;
            } else {
                lastLoad.addElement(item);
            }
        }
        if (dups) {
            errln("ResourceBundle loaded some classes multiple times");
        }

        loader.loadedClasses.removeAllElements();
        bundle = test.getResourceBundle(resName, l);
        loader.logClasses("Second lookup of "+resClassName+" generated the following loads:");

        dups = false;
        for (int i = 0; i < loader.loadedClasses.size(); i++) {
            Object item = loader.loadedClasses.elementAt(i);
            if (lastLoad.contains(item)) {
                logln("ResourceBundle did not cache "+item+" correctly");
                dups = true;
            }
        }
        if (dups) {
            errln("Resource bundle not caching some classes properly");
        }
    }

    private class ConcurrentLoadingThread extends Thread {
        private Loader loader;
        public Object bundle;
        private Bug4168625Getter test;
        private Locale locale;
        private String resourceName = "Bug4168625Resource3";
        public ConcurrentLoadingThread(Loader loader, Bug4168625Getter test, Locale l, String resourceName) {
            this.loader = loader;
            this.test = test;
            this.locale = l;
            this.resourceName = resourceName;
        }
        public ConcurrentLoadingThread(Loader loader, Bug4168625Getter test, Locale l) {
            this.loader = loader;
            this.test = test;
            this.locale = l;
        }
        public void run() {
            try {
                logln(">>"+threadName()+">run");
                bundle = test.getResourceBundle(resourceName, locale);
            } catch (Exception e) {
                errln("TEST CAUGHT UNEXPECTED EXCEPTION: "+e);
            } finally {
                logln("<<"+threadName()+"<run");
            }
        }
        public synchronized void waitUntilPinged() {
            logln(">>"+threadName()+">waitUntilPinged");
            loader.notifyEveryone();
            try {
                wait(30000);    //wait 30 seconds max.
            } catch (InterruptedException e) {
                logln("Test deadlocked.");
            }
            logln("<<"+threadName()+"<waitUntilPinged");
        }
        public synchronized void ping() {
            logln(">>"+threadName()+">ping "+threadName(this));
            notifyAll();
            logln("<<"+threadName()+"<ping "+threadName(this));
        }
    };

    /**
     * This test ensures that multiple resources can be loading at the same
     * time as long as they don't depend on each other in some way.
     */
    public void testConcurrentLoading() throws Exception {
        final Loader loader = new Loader( new String[] { "Bug4168625Class" }, new String[] { "Bug4168625Resource3_en_US", "Bug4168625Resource3_en_CA" });
        final Class c = loader.loadClass("Bug4168625Class");
        final Bug4168625Getter test = (Bug4168625Getter)c.newInstance();

        ConcurrentLoadingThread thread1 = new ConcurrentLoadingThread(loader, test, new Locale("en", "CA"));
        ConcurrentLoadingThread thread2 = new ConcurrentLoadingThread(loader, test, new Locale("en", "IE"));

        thread1.start();            //start thread 1
        loader.waitForNotify(1);    //wait for thread1 to do getBundle & block in loader
        thread2.start();            //start second thread
        thread2.join();             //wait until thread2 terminates.

            //Thread1 should be blocked inside getBundle at the class loader
            //Thread2 should have completed its getBundle call and terminated
        if (!thread1.isAlive() || thread2.isAlive()) {
            errln("ResourceBundle.getBundle not allowing legal concurrent loads");
        }

        thread1.ping();             //continue thread1
        thread1.join();
    }

    /**
     * This test ensures that a resource loads correctly (with all its parents)
     * when memory is very low (ex. the cache gets purged during a load).
     */
    public void testLowMemoryLoad() throws Exception {
        final String[] classToLoad = { "Bug4168625Class" };
        final String[] classToWait = { "Bug4168625Resource3_en_US","Bug4168625Resource3_en","Bug4168625Resource3" };
        final Loader loader = new Loader(classToLoad, classToWait);
        final Class c = loader.loadClass("Bug4168625Class");
        final Bug4168625Getter test = (Bug4168625Getter)c.newInstance();
        causeResourceBundleCacheFlush();

        ConcurrentLoadingThread thread1 = new ConcurrentLoadingThread(loader, test, new Locale("en", "US"));
        thread1.start();            //start thread 1
        loader.waitForNotify(1);    //wait for thread1 to do getBundle(en_US) & block in loader
        causeResourceBundleCacheFlush();    //cause a cache flush
        thread1.ping();             //kick thread 1
        loader.waitForNotify(2);    //wait for thread1 to do getBundle(en) & block in loader
        causeResourceBundleCacheFlush();    //cause a cache flush
        thread1.ping();             //kick thread 1
        loader.waitForNotify(3);    //wait for thread1 to do getBundle(en) & block in loader
        causeResourceBundleCacheFlush();    //cause a cache flush
        thread1.ping();             //kick thread 1
        thread1.join();             //wait until thread1 terminates

        ResourceBundle bundle = (ResourceBundle)thread1.bundle;
        String s1 = bundle.getString("Bug4168625Resource3_en_US");
        String s2 = bundle.getString("Bug4168625Resource3_en");
        String s3 = bundle.getString("Bug4168625Resource3");
        if ((s1 == null) || (s2 == null) || (s3 == null)) {
            errln("Bundle not constructed correctly.  The parent chain is incorrect.");
        }
    }

    /**
     * A simple class loader that loads classes from the current
     * working directory.  The loader will block the current thread
     * of execution before it returns when it tries to load
     * the class "Bug4168625Resource3_en_US".
     */
    private static final String CLASS_PREFIX = "";
    private static final String CLASS_SUFFIX = ".class";

    private static final class SimpleLoader extends ClassLoader {
        private boolean network = false;

        public SimpleLoader() {
            super(SimpleLoader.class.getClassLoader());
            this.network = false;
        }
        public SimpleLoader(boolean simulateNetworkLoad) {
            super(SimpleLoader.class.getClassLoader());
            this.network = simulateNetworkLoad;
        }
        public Class loadClass(final String className, final boolean resolveIt)
                throws ClassNotFoundException {
            Class result;
            synchronized (this) {
                result = findLoadedClass(className);
                if (result == null) {
                    if (network) {
                        try {
                             Thread.sleep(100);
                        } catch (java.lang.InterruptedException e) {
                        }
                    }
                    result = getParent().loadClass(className);
                    if ((result != null) && resolveIt) {
                        resolveClass(result);
                    }
                }
            }
            return result;
        }
    }

    private final class Loader extends ClassLoader {
        public final Vector loadedClasses = new Vector();
        private String[] classesToLoad;
        private String[] classesToWaitFor;

        public Loader() {
            super(Loader.class.getClassLoader());
            classesToLoad = new String[0];
            classesToWaitFor = new String[0];
        }

        public Loader(final String[] classesToLoadIn, final String[] classesToWaitForIn) {
            super(Loader.class.getClassLoader());
            classesToLoad = classesToLoadIn;
            classesToWaitFor = classesToWaitForIn;
        }

        /**
         * Load a class.  Files we can load take preference over ones the system
         * can load.
         */
        private byte[] getClassData(final String className) {
            boolean shouldLoad = false;
            for (int i = classesToLoad.length-1; i >= 0; --i) {
                if (className.equals(classesToLoad[i])) {
                    shouldLoad = true;
                    break;
                }
            }

            if (shouldLoad) {
                final String name = CLASS_PREFIX+className+CLASS_SUFFIX;
                try {
                    final InputStream fi = this.getClass().getClassLoader().getResourceAsStream(name);
                    final byte[] result = new byte[fi.available()];
                    fi.read(result);
                    return result;
                } catch (Exception e) {
                    logln("Error loading test class: "+name);
                    logln(e.toString());
                    return null;
                }
            } else {
                return null;
            }
        }

        /**
         * Load a class.  Files we can load take preference over ones the system
         * can load.
         */
        public Class loadClass(final String className, final boolean resolveIt)
                throws ClassNotFoundException {
            Class result;
            synchronized (this) {
                try {
                    logln(">>"+threadName()+">load "+className);
                    loadedClasses.addElement(className);

                    result = findLoadedClass(className);
                    if (result == null) {
                        final byte[] classData = getClassData(className);
                        if (classData == null) {
                            //we don't have a local copy of this one
                            logln("Loading system class: "+className);
                            result = loadFromSystem(className);
                        } else {
                            result = defineClass(classData, 0, classData.length);
                            if (result == null) {
                                //there was an error defining the class
                                result = loadFromSystem(className);
                            }
                        }
                        if ((result != null) && resolveIt) {
                            resolveClass(result);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    // Ignore loading of Bug4168625ResourceProvider
                    if (className.equals("Bug4168625ResourceProvider")) {
                        logln("Ignoring " + className);
                        loadedClasses.remove(className);
                        return null;
                    }
                    throw e;
                }
            }
            for (int i = classesToWaitFor.length-1; i >= 0; --i) {
                if (className.equals(classesToWaitFor[i])) {
                    rendezvous();
                    break;
                }
            }
            logln("<<"+threadName()+"<load "+className);
            return result;
        }

        /**
         * Delegate loading to its parent class loader that loads the test classes.
         * In othervm mode, the parent class loader is the system class loader;
         * in samevm mode, the parent class loader is the jtreg URLClassLoader.
         */
        private Class loadFromSystem(String className) throws ClassNotFoundException {
            return getParent().loadClass(className);
        }

        public void logClasses(String title) {
            logln(title);
            for (int i = 0; i < loadedClasses.size(); i++) {
                logln("    "+loadedClasses.elementAt(i));
            }
            logln("");
        }

        public int notifyCount = 0;
        public int waitForNotify(int count) {
            return waitForNotify(count, 0);
        }
        public synchronized int waitForNotify(int count, long time) {
            logln(">>"+threadName()+">waitForNotify");
            if (count > notifyCount) {
                try {
                    wait(time);
                } catch (InterruptedException e) {
                }
            } else {
                logln("  count("+count+") > notifyCount("+notifyCount+")");
            }
            logln("<<"+threadName()+"<waitForNotify");
            return notifyCount;
        }
        private synchronized void notifyEveryone() {
            logln(">>"+threadName()+">notifyEveryone");
            notifyCount++;
            notifyAll();
            logln("<<"+threadName()+"<notifyEveryone");
        }
        private void rendezvous() {
            final Thread current = Thread.currentThread();
            if (current instanceof ConcurrentLoadingThread) {
                ((ConcurrentLoadingThread)current).waitUntilPinged();
            }
        }
    }

    private static String threadName() {
        return threadName(Thread.currentThread());
    }

    private static String threadName(Thread t) {
        String temp = t.toString();
        int ndx = temp.indexOf("Thread[");
        temp = temp.substring(ndx + "Thread[".length());
        ndx = temp.indexOf(',');
        temp = temp.substring(0, ndx);
        return temp;
    }

    /** Fill memory to force all SoftReferences to be GCed */
    private void causeResourceBundleCacheFlush() {
        logln("Filling memory...");
        int allocationSize = 1024;
        Vector memoryHog = new Vector();
        try {
            while (true) {
                memoryHog.addElement(new byte[allocationSize]);
                allocationSize *= 2;
            }
        } catch (Throwable e) {
            logln("Caught "+e+" filling memory");
        } finally{
            memoryHog = null;
            System.gc();
        }
        logln("last allocation size: " + allocationSize);
    }

    /**
     *  NOTE: this problem is not externally testable and can only be
     *  verified through code inspection unless special code to force
     *  a task switch is inserted into ResourceBundle.
     *  The class Bug4168625Resource_sp exists.  It's parent bundle
     *  (Bug4168625Resource) contains a resource string with the tag
     *  "language" but Bug4168625Resource_sp does not.
     *  Assume two threads are executing, ThreadA and ThreadB and they both
     *  load a resource Bug4168625Resource with from sp locale.
     *  ResourceBundle.getBundle adds a bundle to the bundle cache (in
     *  findBundle) before it sets the bundle's parent (in getBundle after
     *  returning from findBundle).
     *  <P>
     *  <pre>
     *  ThreadA.getBundle("Bug4168625Resource", new Locale("sp"));
     *      A-->load Bug4168625Resource_sp
     *      A-->find cached Bug4168625Resource
     *      A-->cache Bug4168625Resource_sp as Bug4168625Resource_sp
     *  ThreadB.getBundle("Bug4168625Resource", new Locale("sp"));
     *      B-->find cached Bug4168625Resource_sp
     *      B-->return Bug4168625Resource_sp
     *  ThreadB.bundle.getString("language");
     *      B-->try to find "language" in Bug4168625Resource_sp
     *      B-->Bug4168625Resource_sp does not have a parent, so return null;
     *  ThreadB.System.out.println("Some unknown country");
     *      A-->set parent of Bug4168625Resource_sp to Bug4168625Resource
     *      A-->return Bug4168625Resource_sp (the same bundle ThreadB got)
     *  ThreadA.bundle.getString("language");
     *      A-->try to find "language" in Bug4168625Resource_sp
     *      A-->try to find "language" in Bug4168625Resource (parent of Bug4168625Resource_sp)
     *      A-->return the string
     *  ThreadA.System.out.println("Langauge = "+country);
     *  ThreadB.bundle.getString("language");
     *      B-->try to find "language" in Bug4168625Resource_sp
     *      B-->try to find "language" in Bug4168625Resource (parent of Bug4168625Resource_sp)
     *      B-->return the string
     *  ThreadB.System.out.println("Langauge = "+country);
     *  </pre>
     *  <P>
     *  Note that the first call to getString() by ThreadB returns null, but the second
     *  returns a value.  Thus to ThreadB, the bundle appears to change.  ThreadA gets
     *  the expected results right away.
     */
}
