/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6876135 7024172 7067691
 * @summary Test PlatformLoggingMXBean
 *          This test performs similar testing as
 *          java/util/logging/LoggingMXBeanTest.
 *
 * @build PlatformLoggingMXBeanTest
 * @run main PlatformLoggingMXBeanTest
 */

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.lang.management.PlatformLoggingMXBean;
import java.util.logging.*;
import java.util.List;

public class PlatformLoggingMXBeanTest
{
    ObjectName objectName = null;
    static String LOGGER_NAME_1 = "com.sun.management.Logger1";
    static String LOGGER_NAME_2 = "com.sun.management.Logger2";

    // Use Logger instance variables to prevent premature garbage collection
    // of weak references.
    Logger logger1;
    Logger logger2;

    public PlatformLoggingMXBeanTest() throws Exception {
    }

    private void runTest(PlatformLoggingMXBean mBean) throws Exception {

        /*
         * Create the MBeanServeri, register the PlatformLoggingMXBean
         */
        System.out.println( "***************************************************" );
        System.out.println( "********** PlatformLoggingMXBean Unit Test **********" );
        System.out.println( "***************************************************" );
        System.out.println( "" );
        System.out.println( "*******************************" );
        System.out.println( "*********** Phase 1 ***********" );
        System.out.println( "*******************************" );
        System.out.println( "    Creating MBeanServer " );
        System.out.print( "    Register PlatformLoggingMXBean: " );
        MBeanServer mbs = MBeanServerFactory.createMBeanServer();
        String[] list = new String[0];

        try {
            objectName = new ObjectName(LogManager.LOGGING_MXBEAN_NAME);
            mbs.registerMBean( mBean, objectName );
        }
        catch ( Exception e ) {
            System.out.println( "FAILED" );
            throw e;
        }
        System.out.println( "PASSED" );
        System.out.println("");

        /*
         * Access our MBean to get the current list of Loggers
         */
        System.out.println( "*******************************" );
        System.out.println( "*********** Phase 2 ***********" );
        System.out.println( "*******************************" );
        System.out.println( "   Test Logger Name retrieval (getLoggerNames) " );
        // check that Level object are returned properly
        try {
            list = (String[]) mbs.getAttribute( objectName,  "LoggerNames" );
        }
        catch ( Exception e ) {
            System.out.println("    : FAILED" );
            throw e;
        }

        /*
         * Dump the list of Loggers already present, if any
         */
        Object[] params =  new Object[1];
        String[] signature =  new String[1];
        Level l;

        if ( list == null ) {
            System.out.println("    : PASSED.  No Standard Loggers Present" );
            System.out.println("");
        }
        else {
            System.out.println("    : PASSED. There are " + list.length + " Loggers Present" );
            System.out.println("");
            System.out.println( "*******************************" );
            System.out.println( "*********** Phase 2B **********" );
            System.out.println( "*******************************" );
            System.out.println( " Examine Existing Loggers" );
            for ( int i = 0; i < list.length; i++ ) {
                try {
                    params[0] = list[i];
                    signature[0] = "java.lang.String";
                    String levelName = (String) mbs.invoke(  objectName, "getLoggerLevel", params, signature );
                    System.out.println("    : Logger #" + i + " = " + list[i] );
                    System.out.println("    : Level = " + levelName );
                }
                catch ( Exception e ) {
                    System.out.println("    : FAILED" );
                    throw e;
                }
            }
            System.out.println("    : PASSED" );
        }

        /*
         * Create two new loggers to the list of Loggers already present
         */
        System.out.println("");
        System.out.println( "*******************************" );
        System.out.println( "*********** Phase 3 ***********" );
        System.out.println( "*******************************" );
        System.out.println( " Create and test new Loggers" );
        logger1 = Logger.getLogger( LOGGER_NAME_1 );
        logger2 = Logger.getLogger( LOGGER_NAME_2 );

        // check that Level object are returned properly
        try {
            list = (String[]) mbs.getAttribute( objectName,  "LoggerNames" );
        }
        catch ( Exception e ) {
            System.out.println("    : FAILED" );
            throw e;
        }

        /*
         *  Check for the existence of our new Loggers
         */
        boolean log1 = false, log2 = false;

        if ( list == null || list.length < 2 ) {
            System.out.println("    : FAILED.  Could not Detect the presense of the new Loggers" );
            throw new RuntimeException(
                "Could not Detect the presense of the new Loggers");
        }
        else {
            for ( int i = 0; i < list.length; i++ ) {
                if ( list[i].equals( LOGGER_NAME_1 ) ) {
                    log1 = true;
                    System.out.println( "    : Found new Logger : " + list[i] );
                }
                if ( list[i].equals( LOGGER_NAME_2 ) ) {
                    log2 = true;
                    System.out.println( "    : Found new Logger : " + list[i] );
                }
            }
            if ( log1 && log2 )
                System.out.println( "    : PASSED." );
            else {
                System.out.println( "    : FAILED.  Could not Detect the new Loggers." );
                throw new RuntimeException(
                    "Could not Detect the presense of the new Loggers");
            }
        }

        /*
         *  Set a new Logging levels and check that it succeeded
         */
        System.out.println("");
        System.out.println( "*******************************" );
        System.out.println( "*********** Phase 4 ***********" );
        System.out.println( "*******************************" );
        System.out.println( " Set and Check the Logger Level" );
        log1 = false;
        log2 = false;

        try {
            // Set the level of logger1 to ALL
            params = new Object[2];
            signature =  new String[2];
            params[0] = LOGGER_NAME_1;
            params[1] = Level.ALL.getName();
            signature[0] = "java.lang.String";
            signature[1] = "java.lang.String";
            mbs.invoke(  objectName, "setLoggerLevel", params, signature );

            // Set the level of logger2 to FINER
            params[0] = LOGGER_NAME_2;
            params[1] = Level.FINER.getName();
            mbs.invoke(  objectName, "setLoggerLevel", params, signature );

            // Okay read back the Level from Logger1. Should be ALL
            params =  new Object[1];
            signature =  new String[1];
            params[0] = LOGGER_NAME_1;
            signature[0] = "java.lang.String";
            String levelName = (String) mbs.invoke(  objectName, "getLoggerLevel", params, signature );
            l = Level.parse(levelName);
            System.out.print("    Logger1: " );
            if ( l.equals( l.ALL ) ) {
                System.out.println("Level Set to ALL: PASSED" );
                log1 = true;
            }
            else {
                System.out.println("Level Set to ALL: FAILED" );
                throw new RuntimeException(
                    "Level Set to ALL but returned " + l.toString());
            }

            // Okay read back the Level from Logger2. Should be FINER
            params =  new Object[1];
            signature =  new String[1];
            params[0] = LOGGER_NAME_2;
            signature[0] = "java.lang.String";
            levelName = (String) mbs.invoke(  objectName, "getLoggerLevel", params, signature );
            l = Level.parse(levelName);
            System.out.print("    Logger2: " );
            if ( l.equals( l.FINER ) ) {
                System.out.println("Level Set to FINER: PASSED" );
                log2 = true;
            }
            else {
                System.out.println("Level Set to FINER: FAILED" );
                throw new RuntimeException(
                    "Level Set to FINER but returned " + l.toString());
            }
        }
        catch ( Exception e ) {
            throw e;
        }

        System.out.println( "" );
        System.out.println( "***************************************************" );
        System.out.println( "***************** All Tests Passed ****************" );
        System.out.println( "***************************************************" );
    }

    public static void main(String[] argv) throws Exception {
        PlatformLoggingMXBean mbean =
            ManagementFactory.getPlatformMXBean(PlatformLoggingMXBean.class);
        ObjectName objname = mbean.getObjectName();
        if (!objname.equals(new ObjectName(LogManager.LOGGING_MXBEAN_NAME))) {
            throw new RuntimeException("Invalid ObjectName " + objname);
        }

        // check if the PlatformLoggingMXBean is registered in the platform MBeanServer
        MBeanServer platformMBS = ManagementFactory.getPlatformMBeanServer();
        ObjectName objName = new ObjectName(LogManager.LOGGING_MXBEAN_NAME);

        // We could call mbs.isRegistered(objName) here.
        // Calling getMBeanInfo will throw exception if not found.
        platformMBS.getMBeanInfo(objName);

        if (!platformMBS.isInstanceOf(objName, "java.lang.management.PlatformLoggingMXBean")) {
            throw new RuntimeException(objName + " is of unexpected type");
        }

        // test if PlatformLoggingMXBean works properly in a MBeanServer
        PlatformLoggingMXBeanTest test = new PlatformLoggingMXBeanTest();
        test.runTest(mbean);
    }
}
