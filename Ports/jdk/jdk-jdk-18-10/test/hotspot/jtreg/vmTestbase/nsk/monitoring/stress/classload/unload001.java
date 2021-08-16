/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.stress.classload;

import java.io.*;
import java.util.*;
import java.lang.management.*;

import nsk.share.test.*;
import nsk.share.*;
import nsk.monitoring.share.*;

/**
 * The test checks up <code>getAllClasses()</code>,
 * <code>getLoadedClassCount()</code>, <code>getTotalLoadedClassCount()</code>,
 * <code>getUnloadedClassCount()</code> methods after unloading classes for
 * specified parameters such as:
 * <ul>
 *      <li>
 *          <code>${COMMON_CLASSES_LOCATION}/newclass</code> - path that
 *          contains precompiled loadable classes for which class
 *          loading/unloading is performed. The newclass path should not be
 *          included into <code>CLASSPATH</code> to avoid spontaneous loading
 *          of these classes.
 *      <li>
 *          <code>loadedClassCount</code> - number of loadable classes.
 *      <li>
 *          <code>loaderCount</code> - number of class loaders.
 * </ul>
 *
 * <p>The other parameters which may have an influence on running test are:
 * <ul>
 *      <li>
 *          <code>testMode</code> defines an execution mode either for
 *          the <code>ClassLoadingMBean</code> interface or for the
 *          <code>ClassLoadingMetrics</code> interface.
 *      <li>
 *          <code>MBeanServer</code> defines a MBean server implemetation
 *          under which test is executed.
 *      <li>
 *          <code>singleClassloaderClass</code> specifies whether class loaders
 *          are instances of the same class.
 * </ul>
 * For details about arguments, see the {@link
 * nsk.monitoring.share.ArgumentHamdler ArgumentHamdler} description .
 *
 * <p>The test loads classes according to specified parameters. After classes
 * have been loaded, the test makes an initial snapshot of class loading metrics
 * and tries to unload these classes. Then it makes snapshot of class loading
 * metrics again and compare them with initial values.
 *
 * <p>It is expected that <code>getLoadedClassCount()</code> and
 * <code>getTotalLoadedClassCount()</code> must be decreased by
 * <code>loadedClassCount * loaderCount</code> after unloading.
 *
 * <p>The test also fails if a list returned by <code>getAllClasses()</code>
 * contains any pair of names (loadable class name, class loader name) or size
 * of this list is not equal to <code>getLoadedClassCount()</code>.
 *
 */
public class unload001 extends MonitoringTestBase implements Initializable {
        private ClassLoadingMXBean classLoading;
        private ClassLoadingController controller;
        private int loadedClassCount;
        private int loaderCount;
        private final String ERR = "Unexpected value:: ";
        private long currentlyUnLoadedClassCount = 0;
        private long initialClassCount;
        private long initialTotalClassCount;
        private long initialUnloadedClassCount;

        private Stresser stresser;

        public unload001(Stresser stresser) {
            this.stresser = stresser;
        }

        public void initialize() {
                argHandler.dump(log);
                classLoading = monitoringFactory.getClassLoadingMXBean();
        }

        public void run() {
                boolean result = true;
                loadedClassCount = argHandler.getLoadableClassesCount();
                loaderCount = argHandler.getLoadersCount();

                stresser.start(loaderCount);
                controller =  new ClassLoadingController(log, argHandler, stresser);

                log.info("\nclass loading...");
                controller.loadClasses();

                //extra request for metrics to exclude undesirable class loading
                //during test execution.
                initialClassCount = classLoading.getLoadedClassCount();
                initialTotalClassCount = classLoading.getTotalLoadedClassCount();
                initialUnloadedClassCount = classLoading.getUnloadedClassCount();

                log.info("\nTEST STARTED");
                initialClassCount = classLoading.getLoadedClassCount();
                initialTotalClassCount = classLoading.getTotalLoadedClassCount();
                initialUnloadedClassCount = classLoading.getUnloadedClassCount();

                log.info("\nInitial values:");
                log.info("---------------");
                showValues(initialClassCount, initialTotalClassCount,
                                initialUnloadedClassCount);

                log.info("\nclass unloading...");
                currentlyUnLoadedClassCount = controller.unloadClasses();

                long classCount = classLoading.getLoadedClassCount();
                long totalClassCount = classLoading.getTotalLoadedClassCount();
                long unloadedClassCount = classLoading.getUnloadedClassCount();

                log.info("\nAmount of currently loaded classes:");
                log.info("-----------------------------------");
                showValues(/*classNames, */classCount, totalClassCount,
                                unloadedClassCount);

                log.info("\nchecking loaded classes...");
                result = result & checkValues(classCount, totalClassCount, unloadedClassCount);

                if (result) {
                        log.info("Test PASSED");
                } else {
                        log.info("Test FAILED");
                        setFailed(true);
                }

        }

        public static void main(String[] args) {
            Monitoring.runTest(new unload001(new Stresser(args)), args);
        }

        private void showValues(long classCount,  long totalClassCount, long unloadedClassCount) {
                log.info("\ttotal loaded class count = " + totalClassCount);
                log.info("\tcurrently loaded class count = " + classCount);
                log.info("\tunloaded class count = " + unloadedClassCount);
        }

        private boolean checkValues(long classCount, long totalClassCount, long unloadedClassCount) {
                boolean res = true;

                // We don't check for inequality here because totalClassCount can be greater than
                // initialTotalClassCount due to Lambda classes generation in controller.unloadClasses()
                long expectedValue = initialTotalClassCount;
                if (totalClassCount < expectedValue) {
                        log.error(ERR + "total loaded class count=" + totalClassCount
                                        + " Expected value: "
                                        + expectedValue);
                        res = false;
                }

                expectedValue = classCount + unloadedClassCount;
                if (totalClassCount != expectedValue) {
                        log.error(ERR + "total loaded class count=" + totalClassCount
                                        + " Expected value(classCount + "
                                        + "unloadedClassCount): " + expectedValue);
                        res = false;
                }

                if (currentlyUnLoadedClassCount > unloadedClassCount) {
                        log.error(ERR + "unloaded class count=" + unloadedClassCount
                                        + " Expected value at least "
                                        + currentlyUnLoadedClassCount);
                        res = false;
                }

                return res;
        }
}
