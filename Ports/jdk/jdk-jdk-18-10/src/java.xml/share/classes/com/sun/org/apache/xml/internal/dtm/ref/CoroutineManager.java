/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xml.internal.dtm.ref;

import java.util.BitSet;

import com.sun.org.apache.xml.internal.res.XMLErrorResources;
import com.sun.org.apache.xml.internal.res.XMLMessages;


/**
 * <p>Support the coroutine design pattern.</p>
 *
 * <p>A coroutine set is a very simple cooperative non-preemptive
 * multitasking model, where the switch from one task to another is
 * performed via an explicit request. Coroutines interact according to
 * the following rules:</p>
 *
 * <ul>
 * <li>One coroutine in the set has control, which it retains until it
 * either exits or resumes another coroutine.</li>
 * <li>A coroutine is activated when it is resumed by some other coroutine
 * for the first time.</li>
 * <li>An active coroutine that gives up control by resuming another in
 * the set retains its context -- including call stack and local variables
 * -- so that if/when it is resumed, it will proceed from the point at which
 * it last gave up control.</li>
 * </ul>
 *
 * <p>Coroutines can be thought of as falling somewhere between pipes and
 * subroutines. Like call/return, there is an explicit flow of control
 * from one coroutine to another. Like pipes, neither coroutine is
 * actually "in charge", and neither must exit in order to transfer
 * control to the other. </p>
 *
 * <p>One classic application of coroutines is in compilers, where both
 * the parser and the lexer are maintaining complex state
 * information. The parser resumes the lexer to process incoming
 * characters into lexical tokens, and the lexer resumes the parser
 * when it has reached a point at which it has a reliably interpreted
 * set of tokens available for semantic processing. Structuring this
 * as call-and-return would require saving and restoring a
 * considerable amount of state each time. Structuring it as two tasks
 * connected by a queue may involve higher overhead (in systems which
 * can optimize the coroutine metaphor), isn't necessarily as clear in
 * intent, may have trouble handling cases where data flows in both
 * directions, and may not handle some of the more complex cases where
 * more than two coroutines are involved.</p>
 *
 * <p>Most coroutine systems also provide a way to pass data between the
 * source and target of a resume operation; this is sometimes referred
 * to as "yielding" a value.  Others rely on the fact that, since only
 * one member of a coroutine set is running at a time and does not
 * lose control until it chooses to do so, data structures may be
 * directly shared between them with only minimal precautions.</p>
 *
 * <p>"Note: This should not be taken to mean that producer/consumer
 * problems should be always be done with coroutines." Queueing is
 * often a better solution when only two threads of execution are
 * involved and full two-way handshaking is not required. It's a bit
 * difficult to find short pedagogical examples that require
 * coroutines for a clear solution.</p>
 *
 * <p>The fact that only one of a group of coroutines is running at a
 * time, and the control transfer between them is explicit, simplifies
 * their possible interactions, and in some implementations permits
 * them to be implemented more efficiently than general multitasking.
 * In some situations, coroutines can be compiled out entirely;
 * in others, they may only require a few instructions more than a
 * simple function call.</p>
 *
 * <p>This version is built on top of standard Java threading, since
 * that's all we have available right now. It's been encapsulated for
 * code clarity and possible future optimization.</p>
 *
 * <p>(Two possible approaches: wait-notify based and queue-based. Some
 * folks think that a one-item queue is a cleaner solution because it's
 * more abstract -- but since coroutine _is_ an abstraction I'm not really
 * worried about that; folks should be able to switch this code without
 * concern.)</p>
 *
 * <p>%TBD% THIS SHOULD BE AN INTERFACE, to facilitate building other
 * implementations... perhaps including a true coroutine system
 * someday, rather than controlled threading. Arguably Coroutine
 * itself should be an interface much like Runnable, but I think that
 * can be built on top of this.</p>
 * */
public class CoroutineManager
{
  /** "Is this coroutine ID number already in use" lookup table.
   * Currently implemented as a bitset as a compromise between
   * compactness and speed of access, but obviously other solutions
   * could be applied.
   * */
  BitSet m_activeIDs=new BitSet();

  /** Limit on the coroutine ID numbers accepted. I didn't want the
   * in-use table to grow without bound. If we switch to a more efficient
   * sparse-array mechanism, it may be possible to raise or eliminate
   * this boundary.
   * @xsl.usage internal
   */
  static final int m_unreasonableId=1024;

  /** Internal field used to hold the data being explicitly passed
   * from one coroutine to another during a co_resume() operation.
   * (Of course implicit data sharing may also occur; one of the reasons
   * for using coroutines is that you're guaranteed that none of the
   * other coroutines in your set are using shared structures at the time
   * you access them.)
   *
   * %REVIEW% It's been proposed that we be able to pass types of data
   * other than Object -- more specific object types, or
   * lighter-weight primitives.  This would seem to create a potential
   * explosion of "pass x recieve y back" methods (or require
   * fracturing resume into two calls, resume-other and
   * wait-to-be-resumed), and the weight issue could be managed by
   * reusing a mutable buffer object to contain the primitive
   * (remember that only one coroutine runs at a time, so once the
   * buffer's set it won't be walked on). Typechecking objects is
   * interesting from a code-robustness point of view, but it's
   * unclear whether it makes sense to encapsulate that in the
   * coroutine code or let the callers do it, since it depends on RTTI
   * either way. Restricting the parameters to objects implementing a
   * specific CoroutineParameter interface does _not_ seem to be a net
   * win; applications can do so if they want via front-end code, but
   * there seem to be too many use cases involving passing an existing
   * object type that you may not have the freedom to alter and may
   * not want to spend time wrapping another object around.
   * */
  Object m_yield=null;

  // Expose???
  final static int NOBODY=-1;
  final static int ANYBODY=-1;

  /** Internal field used to confirm that the coroutine now waking up is
   * in fact the one we intended to resume. Some such selection mechanism
   * is needed when more that two coroutines are operating within the same
   * group.
   */
  int m_nextCoroutine=NOBODY;

  /** <p>Each coroutine in the set managed by a single
   * CoroutineManager is identified by a small positive integer. This
   * brings up the question of how to manage those integers to avoid
   * reuse... since if two coroutines use the same ID number, resuming
   * that ID could resume either. I can see arguments for either
   * allowing applications to select their own numbers (they may want
   * to declare mnemonics via manefest constants) or generating
   * numbers on demand.  This routine's intended to support both
   * approaches.</p>
   *
   * <p>%REVIEW% We could use an object as the identifier. Not sure
   * it's a net gain, though it would allow the thread to be its own
   * ID. Ponder.</p>
   *
   * @param coroutineID  If >=0, requests that we reserve this number.
   * If <0, requests that we find, reserve, and return an available ID
   * number.
   *
   * @return If >=0, the ID number to be used by this coroutine. If <0,
   * an error occurred -- the ID requested was already in use, or we
   * couldn't assign one without going over the "unreasonable value" mark
   * */
  public synchronized int co_joinCoroutineSet(int coroutineID)
  {
    if(coroutineID>=0)
      {
        if(coroutineID>=m_unreasonableId || m_activeIDs.get(coroutineID))
          return -1;
      }
    else
      {
        // What I want is "Find first clear bit". That doesn't exist.
        // JDK1.2 added "find last set bit", but that doesn't help now.
        coroutineID=0;
        while(coroutineID<m_unreasonableId)
          {
            if(m_activeIDs.get(coroutineID))
              ++coroutineID;
            else
              break;
          }
        if(coroutineID>=m_unreasonableId)
          return -1;
      }

    m_activeIDs.set(coroutineID);
    return coroutineID;
  }

  /** In the standard coroutine architecture, coroutines are
   * identified by their method names and are launched and run up to
   * their first yield by simply resuming them; its's presumed that
   * this recognizes the not-already-running case and does the right
   * thing. We seem to need a way to achieve that same threadsafe
   * run-up...  eg, start the coroutine with a wait.
   *
   * %TBD% whether this makes any sense...
   *
   * @param thisCoroutine the identifier of this coroutine, so we can
   * recognize when we are being resumed.
   * @exception java.lang.NoSuchMethodException if thisCoroutine isn't
   * a registered member of this group. %REVIEW% whether this is the
   * best choice.
   * */
  public synchronized Object co_entry_pause(int thisCoroutine) throws java.lang.NoSuchMethodException
  {
    if(!m_activeIDs.get(thisCoroutine))
      throw new java.lang.NoSuchMethodException();

    while(m_nextCoroutine != thisCoroutine)
      {
        try
          {
            wait();
          }
        catch(java.lang.InterruptedException e)
          {
            // %TBD% -- Declare? Encapsulate? Ignore? Or
            // dance widdershins about the instruction cache?
          }
      }

    return m_yield;
  }

  /** Transfer control to another coroutine which has already been started and
   * is waiting on this CoroutineManager. We won't return from this call
   * until that routine has relinquished control.
   *
   * %TBD% What should we do if toCoroutine isn't registered? Exception?
   *
   * @param arg_object A value to be passed to the other coroutine.
   * @param thisCoroutine Integer identifier for this coroutine. This is the
   * ID we watch for to see if we're the ones being resumed.
   * @param toCoroutine  Integer identifier for the coroutine we wish to
   * invoke.
   * @exception java.lang.NoSuchMethodException if toCoroutine isn't a
   * registered member of this group. %REVIEW% whether this is the best choice.
   * */
  public synchronized Object co_resume(Object arg_object,int thisCoroutine,int toCoroutine) throws java.lang.NoSuchMethodException
  {
    if(!m_activeIDs.get(toCoroutine))
      throw new java.lang.NoSuchMethodException(XMLMessages.createXMLMessage(XMLErrorResources.ER_COROUTINE_NOT_AVAIL, new Object[]{Integer.toString(toCoroutine)})); //"Coroutine not available, id="+toCoroutine);

    // We expect these values to be overwritten during the notify()/wait()
    // periods, as other coroutines in this set get their opportunity to run.
    m_yield=arg_object;
    m_nextCoroutine=toCoroutine;

    notify();
    while(m_nextCoroutine != thisCoroutine || m_nextCoroutine==ANYBODY || m_nextCoroutine==NOBODY)
      {
        try
          {
            // System.out.println("waiting...");
            wait();
          }
        catch(java.lang.InterruptedException e)
          {
            // %TBD% -- Declare? Encapsulate? Ignore? Or
            // dance deasil about the program counter?
          }
      }

    if(m_nextCoroutine==NOBODY)
      {
        // Pass it along
        co_exit(thisCoroutine);
        // And inform this coroutine that its partners are Going Away
        // %REVIEW% Should this throw/return something more useful?
        throw new java.lang.NoSuchMethodException(XMLMessages.createXMLMessage(XMLErrorResources.ER_COROUTINE_CO_EXIT, null)); //"CoroutineManager recieved co_exit() request");
      }

    return m_yield;
  }

  /** Terminate this entire set of coroutines. The others will be
   * deregistered and have exceptions thrown at them. Note that this
   * is intended as a panic-shutdown operation; under normal
   * circumstances a coroutine should always end with co_exit_to() in
   * order to politely inform at least one of its partners that it is
   * going away.
   *
   * %TBD% This may need significantly more work.
   *
   * %TBD% Should this just be co_exit_to(,,CoroutineManager.PANIC)?
   *
   * @param thisCoroutine Integer identifier for the coroutine requesting exit.
   * */
  public synchronized void co_exit(int thisCoroutine)
  {
    m_activeIDs.clear(thisCoroutine);
    m_nextCoroutine=NOBODY; // %REVIEW%
    notify();
  }

  /** Make the ID available for reuse and terminate this coroutine,
   * transferring control to the specified coroutine. Note that this
   * returns immediately rather than waiting for any further coroutine
   * traffic, so the thread can proceed with other shutdown activities.
   *
   * @param arg_object    A value to be passed to the other coroutine.
   * @param thisCoroutine Integer identifier for the coroutine leaving the set.
   * @param toCoroutine   Integer identifier for the coroutine we wish to
   * invoke.
   * @exception java.lang.NoSuchMethodException if toCoroutine isn't a
   * registered member of this group. %REVIEW% whether this is the best choice.
   * */
  public synchronized void co_exit_to(Object arg_object,int thisCoroutine,int toCoroutine) throws java.lang.NoSuchMethodException
  {
    if(!m_activeIDs.get(toCoroutine))
      throw new java.lang.NoSuchMethodException(XMLMessages.createXMLMessage(XMLErrorResources.ER_COROUTINE_NOT_AVAIL, new Object[]{Integer.toString(toCoroutine)})); //"Coroutine not available, id="+toCoroutine);

    // We expect these values to be overwritten during the notify()/wait()
    // periods, as other coroutines in this set get their opportunity to run.
    m_yield=arg_object;
    m_nextCoroutine=toCoroutine;

    m_activeIDs.clear(thisCoroutine);

    notify();
  }
}
