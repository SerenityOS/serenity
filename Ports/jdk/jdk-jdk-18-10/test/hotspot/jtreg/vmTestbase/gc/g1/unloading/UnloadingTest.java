/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1.unloading;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.*;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import gc.g1.unloading.check.Assertion;
import gc.g1.unloading.check.AssertionContainer;
import gc.g1.unloading.check.ClassAssertion;
import gc.g1.unloading.configuration.*;
import gc.g1.unloading.loading.*;
import nsk.share.gc.GCTestBase;
import nsk.share.test.ExecutionController;
import nsk.share.test.Stresser;
import nsk.share.test.Tests;

import jtreg.SkippedException;

/**
 * This class contains main method. It's entry point for all configurations.
 *
 */
public class UnloadingTest extends GCTestBase {

    private static String[] args;

    private TestConfiguration configuration;

    private AssertionContainer assertionContainer = new AssertionContainer();

    private Random random;

    private static final String classNamePrefix = "ClassAbc_";

    private static final long DELAY = 300;

    private static AtomicLong systemGcCallsCounter = new AtomicLong(0);

    public static void main(String[] args) {
        UnloadingTest.args = args;
        Tests.runTest(new UnloadingTest(), args);
    }

    @Override
    public void run() {
        configuration = TestConfiguration.createTestConfiguration(args);

        checkIfG1Used();
        checkFlags();

        ExecutionController stresser = new Stresser(args);
        stresser.start(1);

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                System.out.println("Throwable \"" + e + "\" in thread " + t.getName() + ", id=" + t.getId());
                e.printStackTrace();
                try {
                    checkGCCounters();
                } catch (Throwable thr) {
                    thr.printStackTrace();
                }
                System.exit(2);
            }
        });

        random = new Random(runParams.getSeed());
        ClassLoadingHelper classLoadingHelper = new ClassLoadingHelper(stresser, random.nextLong(), configuration);

        int classesCounter = 0;
        while (stresser.continueExecution()) {
            Collection<Assertion> assertions = null;
            String className = classNamePrefix + (classesCounter++);

            try {
                Thread.sleep(DELAY);
            } catch (InterruptedException | IllegalArgumentException e) {
                throw new RuntimeException("Something went wrong in ClassLoadingHelper", e);
            }

            if (random.nextBoolean()) {
                assertions = classLoadingHelper.loadClassThatGonnaDie(className);
            } else {
                assertions = classLoadingHelper.loadClassThatGonnaLive(className);
            }

            System.gc();
            long systemGCCalls = systemGcCallsCounter.incrementAndGet();

            assertionContainer.enqueue(assertions, systemGCCalls);

            check(assertionContainer.getElder(systemGCCalls - configuration.getNumberOfGCsBeforeCheck()));

            if (configuration.getNumberOfChecksLimit() >= 0 &&
                    ClassAssertion.getCounterOfCheckedAlive() >= configuration.getNumberOfChecksLimit() &&
                    ClassAssertion.getCounterOfCheckedUnloaded() >= configuration.getNumberOfChecksLimit()) {
                System.out.println("Exiting because numberOfChecksLimit exceeded.");
                stresser.finish();
                break;
            }
        }

        System.out.println("ClassAssertion.getCounterOfCheckedAlive() = " + ClassAssertion.getCounterOfCheckedAlive());
        System.out.println("ClassAssertion.getCounterOfCheckedUnloaded() = " + ClassAssertion.getCounterOfCheckedUnloaded());
        checkGCCounters();
        if (System.getProperty("FailTestIfNothingChecked") != null) {
            if (ClassAssertion.getCounterOfCheckedAlive() == 0 || ClassAssertion.getCounterOfCheckedUnloaded() == 0) {
                throw new RuntimeException("Test was useless. Smthng not checked: " + ClassAssertion.getCounterOfCheckedAlive() + " " +
                        ClassAssertion.getCounterOfCheckedUnloaded());
            }
        }
    }

    private void check(Collection<Assertion> assertions) {
        if (assertions.isEmpty()) {
            return;
        }
        for (Assertion assertion : assertions) {
            assertion.check();
            assertion.cleanup();
        }
    }

    private static void checkGCCounters() {
//        System.out.println("WhiteBox.getWhiteBox().g1GetTotalCollections() = \t" + WhiteBox.getWhiteBox().g1GetTotalCollections());
//        System.out.println("WhiteBox.getWhiteBox().g1GetTotalFullCollections() = \t" + WhiteBox.getWhiteBox().g1GetTotalFullCollections());
        GarbageCollectorMXBean oldGenBean = null;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            System.out.println("bean.getName() = \t\"" + bean.getName() + "\", bean.getCollectionCount() = \t" + bean.getCollectionCount());
            if (bean.getName().contains("Old")) {
                oldGenBean = bean;
            }
        }
//        if (WhiteBox.getWhiteBox().g1GetTotalFullCollections() != 0 || (oldGenBean != null && oldGenBean.getCollectionCount() != 0)) {
        if (oldGenBean != null && oldGenBean.getCollectionCount() != 0) {
            throw new RuntimeException("Full gc happened. Test was useless.");
        }
    }

    private void checkIfG1Used() {
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (!bean.getName().contains("G1")) {
                throw new SkippedException("This test was created to cover G1 class unloading feature. It should be ran with -XX:+UseG1GC");
            }
        }
    }

    private void checkFlags() {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();
        for (String argument : arguments) {
            if (argument.contains("ExplicitGCInvokesConcurrent")) {
                return;
            }
        }
        throw new RuntimeException("This test supposed to be ran with -XX:+ExplicitGCInvokesConcurrent flag");
    }

}
