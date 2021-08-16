/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8167108 8266130
 * @summary converted from VM Testbase nsk/jvmti/SuspendThread/suspendthrd003.
 * VM Testbase keywords: [jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     Same test as suspendthrd001 with additional calls to
 *     SuspendThread() and ResumeThread() while threads are exiting.
 *     Failing criteria for the test are:
 *       - failures of used JVMTI functions.
 * COMMENTS
 *     Derived from nsk/jvmti/SuspendThread/suspendthrd001.
 * Transaction Diagram for this test:
 * main thread                                      agent thread                                       TestedThread-N
 * ===============================================  =================================================  =============================================
 * data->thread_state = NEW
 * for N = 0; i < N_THREADS; i++ {
 *   thr = new TestedThread-N
 *   thr.start()                                                                                       run()
 *   # SP1-w - wait for TestedThread-N to be ready                                                     :
 *   thr.checkReady()                                                                                  // SP1-n - tell main we are ready
 *   :                                                                                                 threadReady = true
 *   // SP2.1-w - wait for agent thread                                                                while (!shouldFinish) {
 *   // SP3.1-n - notify to start test                                                                   // do work
 *   // SP5.1-w - wait while testing                                                                     :
 *   checkStatus(() {                                                                                    :
 *     enter(data.monitor)                                                                               :
 *     if data.thread_state == NEW {                                                                     :
 *       nsk_jvmti_runAgentThread                                                                        :
 *       :                                          // this set is in the agent wrapper:                 :
 *       :                                          data.thread_state = RUNNABLE                         :
 *       :                                          agentProc() {                                        :
 *       :                                            // SP2.1-n - notify agent is waiting               :
 *       :                                            // SP3.1-w - wait to start test                    :
 *       while (data.thread_state == NEW) {           waitForSync() {                                    :
 *         // SP2.2-w - wait for agent thread           enter(data.monitor)                              :
 *         wait(data.monitor)                           : <blocked>                                      :
 *         :                                            : <enter>                                        :
 *         :                                            data.thread_state = WAITING                      :
 *         :                                            // SP2.2-n - notify agent is waiting and wait    :
 *         :                                            notify(data.monitor)                             :
 *         : <notified>                                 while (data.thread_state == WAITING) {           :
 *         :                                              // SP3.2-w - wait to start test                :
 *         :                                              wait(data.monitor)                             :
 *         : <reentered>                                  :                                              :
 *       }                                                :                                              :
 *       // saw data.thread_state == WAITING              :                                              :
 *     }                                                  :                                              :
 *     // we don't enter loop in first call               :                                              :
 *     while (data.thread_state != WAITING                :                                              :
 *            data.thread_state != TERMINATED) {          :                                              :
 *       // SP4.2-w - second wait for agent thread        :                                              :
 *       wait(data.monitor)                               :                                              :
 *     }                                                  :                                              :
 *     if (data.thread_state != TERMINATED) {             :                                              :
 *       data.thread_state = SUSPENDED                    :                                              :
 *       // SP3.2-n - notify to start test                :                                              :
 *       notify(data.monitor)                             :                                              :
 *     }                                                  : <notified>                                   :
 *     while (data.thread_state == SUSPENDED) {           :                                              :
 *       // SP5.2-w - wait while testing                  :                                              :
 *       wait(data.monitor)                               :                                              :
 *       :                                                : <reentered>                                  :
 *       :                                              }                                                :
 *       :                                              // saw data.thread_state == SUSPENDED            :
 *       :                                              exit(data.monitor)                               :
 *       :                                            } // end waitForSync()                             :
 *       :                                            SuspendThread(TestedThread-N)                      :
 *       :                                            // SP5.1-n - notify suspend done                   : <thread suspended>
 *       :                                            resumeSync() {                                     :
 *       :                                              enter(data.monitor)                              :
 *       :                                              if (data.thread_state == SUSPENDED) {            :
 *       :                                                data.thread_state = RUNNABLE                   :
 *       :                                                // SP5.2-n - notify suspend done               :
 *       :                                                notify(data.monitor)                           :
 *       : <notified>                                   }                                                :
 *       :                                              exit(data.monitor)                               :
 *       : <re-entered>                               } // end resumeSync()                              :
 *     }                                              GetThreadState(TestedThread-N)                     :
 *     // saw data.thread_state -= RUNNABLE           ResumeThread(TestedThread-N)                       : <thread resumed>
 *     :                                              for (1..N_LATE_CALLS) {                            :
 *   } // end checkStatus()                             SuspendThread(TestedThread-N)                    :
 *   :                                                  :                                                : <thread suspended>
 *   thr.letFinish()                                    ResumeThread(TestedThread-N)                     :
 *     shouldFinish = true;                             :                                                : <thread resumed>
 *   thr.join()                                         :                                                : <sees shouldFinish == true>
 *   :                                                  :                                              }
 *   :                                                  :                                              : <thread exits>
 *   : <join() sees thread exit>                        : <SuspendThread sees thread exit & breaks>
 *   // SP4.1-w - second wait for agent thread        }
 *   // SP6.1-n - notify to end test                  :
 *   // SP7.1 - wait for agent end                    :
 *   checkStatus()                                    // SP4.1-n - notify agent is waiting and wait
 *     enter(data.monitor)                            // SP6.1-w - wait to end test
 *     :                                              waitForSync()
 *     // we don't enter if-stmt in second call         enter(data.monitor)
 *     if data.thread_state == NEW {                    : <blocked>
 *     }                                                :
 *     while (data.thread_state != WAITING              :
 *            data.thread_state != TERMINATED) {        :
 *       // SP4.2-w - second wait for agent thread      :
 *       wait(data.monitor)                             :
 *       :                                              : <enter>
 *       :                                              data.thread_state = WAITING
 *       :                                              // SP4.2-n - notify agent is waiting and wait
 *       :                                              notify(data.monitor)
 *       : <notified>                                   while (data.thread_state == WAITING) {
 *       :                                                // SP6.2-w - wait to end test
 *       :                                                wait(data.monitor)
 *       : <reentered>                                    :
 *     }                                                  :
 *     // saw thread_state == WAITING                     :
 *     if (data.thread_state != TERMINATED) {             :
 *       data.thread_state = SUSPENDED                    :
 *       // SP6.2-n - notify to end test                  :
 *       notify(data.monitor)                             :
 *     }                                                  : <notified>
 *     while (data.thread_state == SUSPENDED) {           :
 *       // SP7.2 - wait for agent end                    :
 *       wait(data.monitor)                               :
 *       :                                                : <reentered>
 *       :                                              }
 *       :                                              // saw data.thread_state == SUSPENDED
 *       :                                              exit(data.monitor)
 *       :                                            } // end waitForSync()
 *       :                                            // SP7.1-n - notify agent end
 *       :                                            resumeSync() {
 *       :                                              enter(data.monitor)
 *       :                                              if (data.thread_state == SUSPENDED) {
 *       :                                                data.thread_state = RUNNABLE
 *       :                                                // SP7.2-n - notify agent end
 *       :                                                notify(data.monitor)
 *       : <notified>                                   }
 *       :                                              exit(data.monitor)
 *       : <re-entered>                               } // end resumeSync()
 *     }                                            } // end agentProc
 *     // saw data.thread_state -= RUNNABLE         // this set is in the wrapper:
 *   } // end checkStatus()                         data.thread_state = TERMINATED
 *   resetAgentData() {
 *     enter(data.monitor)
 *     while (data.thread_state != TERMINATED) {
 *       wait(data.monitor, 10);
 *     }
 *     data.thread_state = NEW
 *     exit(data.monitor)
 *   } // end resetAgentData()
 *
 * @requires vm.jvmti
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:suspendthrd003=-waittime=5
 *      nsk.jvmti.SuspendThread.suspendthrd003
 */

