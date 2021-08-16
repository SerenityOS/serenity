/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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


package nsk.share.jvmti;

import nsk.share.Log;
import static nsk.share.Consts.*;

/**
 *
 * The abstract class, <b>RedefineAgent</b> is used in development of many
 * of hotswap testcases. This class tryied to encapsulate a work flow model
 * of a typical testcases. So, it reduces normal steps of creating
 * <b>Logger</b> ,<b>ArgumentHandler</b> objects.
 *
 * Need to extend and implement method <b>agentMethod</b>. That actually
 * carries out functionality of the testcase.
 * <code>
 *  public class MyTestCase extends RedefineAgent {
 *       public MyTestCase(String[] arg) {
 *          super(arg);
 *       }
 *
 *       public boolean agentMethod() {
 *           boolean state;
 *          // do some thing.
 *           return state;
 *       }
 *
 *       public static void main(String[] arg) {
 *           MyTestCase testcase = MyTestCase(arg);
 *           System.exit(testcase.runAgent());
 *        }
 *
 *  }
 * </code>
 */

public abstract class RedefineAgent {

    public static Log log = null;

    ArgumentHandler argHandler = null;

    /**
     * Constructor which will intiates the Logger
     * and ArgumentHandler.
     *
     */
    public RedefineAgent(String[] arg) {
        argHandler = new ArgumentHandler(arg);
        log = new Log(System.out,argHandler);

    }

    /**
     * Constructor which will intiates the Logger
     * and keeps ArgumentHandler as null.
     *
     */
    public RedefineAgent() {
        log = new Log(System.out);

    }
    /**
     * Native agent would use  <b>nsk_jvmti_agentFailed()</b> Method to
     * notify its status.
     */
    public native boolean agentStatus();

    /**
     * Native method which will do redefine This is the state when tha agent
     * is already in <b>nsk_jvmti_redefineClass</b>.
     *
     *
     */
    public native boolean redefineAttempted();

    /**
     *
     * Native method which gets the status from the redefineStatus.
     *
     */
    public native boolean isRedefined();
    /**
     * This method <i>agentMethod</i> needs to be implemented by the
     */


    abstract public boolean agentMethod();

   /**
    * This method will be executed from tha
    * mainClass to run the agent.
    */

    public int runAgent() {
        boolean agentStatus = agentMethod();
        int returnStatus =  ( agentStatus ? JCK_STATUS_BASE+TEST_PASSED
                : JCK_STATUS_BASE+TEST_FAILED)  ;
        return returnStatus;
    }

}
