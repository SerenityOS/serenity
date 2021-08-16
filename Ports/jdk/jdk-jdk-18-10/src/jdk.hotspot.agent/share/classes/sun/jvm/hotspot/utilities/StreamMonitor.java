/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot.utilities;

import java.io.*;
import java.util.*;

/** Reads all of the data from the given InputStream, and allows the
    caller to wait for a given string to come in or watch for many
    possible strings. */

public class StreamMonitor implements Runnable {
  private BufferedReader input;
  private boolean printStreamContents;

  private String  waitString;
  private boolean waitStringSeen;
  private List<Trigger> triggers = new LinkedList<>();
  private List<Integer> triggersSeen = new LinkedList<>();

  private String  prefixString;
  private boolean printContents;

  private StringBuffer captureBuffer;

  class Trigger {
    private String[] triggerStrings;
    private int      triggerVal;

    Trigger(String str, int val) {
      triggerStrings = new String[] { str };
      triggerVal     = val;
    }

    // Hack because we don't have a regexp library yet.
    // This requires all strings to be matched.
    Trigger(String[] strs, int val) {
      triggerStrings = strs;
      triggerVal     = val;
    }

    boolean matches(String str) {
      for (int i = 0; i < triggerStrings.length; i++) {
        if (str.indexOf(triggerStrings[i]) == -1) {
          return false;
        }
      }
      return true;
    }

    boolean equals(String[] strs) {
      if (strs.length != triggerStrings.length) {
        return false;
      }

      for (int i = 0; i < strs.length; i++) {
        if (!strs[i].equals(triggerStrings[i])) {
          return false;
        }
      }

      return true;
    }
  }

  /** Equivalent to StreamMonitor(istr, null, false) */
  public StreamMonitor(InputStream istr) {
    this(istr, null, false);
  }

  public StreamMonitor(InputStream istr, String prefixString, boolean printContents) {
    input = new BufferedReader(new InputStreamReader(istr));
    this.prefixString = prefixString;
    this.printContents = printContents;
    Thread thr = new Thread(this);
    thr.setDaemon(true);
    thr.start();
  }

  /** Adds a "trigger", which the stream watches for and, if seen,
      reports the trigger value of via the getTriggers() method.
      Returns true if the addition was successful, false if the string
      was already present as a trigger. */
  public boolean addTrigger(String str, int value) {
    return addTrigger(new String[] { str }, value);
  }

  /** Adds a "trigger", which the stream watches for and, if seen,
      reports the trigger value of via the getTriggers() method.
      Returns true if the addition was successful, false if the string
      was already present as a trigger. */
  public boolean addTrigger(String[] strs, int value) {
    for (Iterator iter = triggers.iterator(); iter.hasNext(); ) {
      Trigger trigger = (Trigger) iter.next();
      if (trigger.equals(strs)) {
        return false;
      }
    }
    Trigger trigger = new Trigger(strs, value);
    return triggers.add(trigger);
  }

  /** Removes a previously added trigger. Returns true if it was
      present, false if not. */
  public boolean removeTrigger(String str) {
    return removeTrigger(new String[] { str });
  }

  /** Removes a previously added trigger. Returns true if it was
      present, false if not. */
  public boolean removeTrigger(String[] strs) {
    for (ListIterator iter = triggers.listIterator(); iter.hasNext(); ) {
      Trigger trigger = (Trigger) iter.next();
      if (trigger.equals(strs)) {
        iter.remove();
        return true;
      }
    }
    return false;
  }

  /** Returns an List of java.lang.Integer objects indicating the
      values of the triggers seen since the last call to
      getTriggersSeen. If there were no triggers seen, returns an
      empty list; does not return null. */
  public synchronized List<Integer> getTriggersSeen() {
    List<Integer> tmpList = triggersSeen;
    triggersSeen = new LinkedList<>();
    return tmpList;
  }

  /** Waits for the specified string to come in for the given period
      of time (measured in milliseconds). */
  public synchronized boolean waitFor(String str, long millis) {
    waitString = str;
    waitStringSeen = false;
    try {
      wait(millis);
    }
    catch (InterruptedException e) {
    }

    waitString = null;
    return waitStringSeen;
  }

  public synchronized void startCapture() {
    captureBuffer = new StringBuffer();
  }

  public synchronized String stopCapture() {
    String ret = captureBuffer.toString();
    captureBuffer = null;
    return ret;
  }

  public void run() {
    byte[] buf = new byte[10240];
    boolean shouldContinue = true;

    try {
      do {
        String str = input.readLine();
        if (str == null) {
          shouldContinue = false;
        } else {
          if (printContents) {
            System.err.println(prefixString + ": " + str);
          }
          synchronized (this) {

            if (captureBuffer != null) {
              captureBuffer.append(str);
              captureBuffer.append("\n");
            }

            // Check wait string
            if ((waitString != null) &&
                (str.indexOf(waitString) != -1)) {
              waitStringSeen = true;
              notifyAll();
            }

            // Check all triggers
            for (Iterator iter = triggers.iterator(); iter.hasNext(); ) {
              Trigger trigger = (Trigger) iter.next();
              if (trigger.matches(str)) {
                triggersSeen.add(trigger.triggerVal);
              }
            }
          }
        }
      } while (shouldContinue);
    }
    catch (IOException e) {
    }

    System.err.print("StreamMonitor ");
    if (prefixString != null) {
      System.err.print("\"" + prefixString + "\" ");
    }
    System.err.println("exiting");
  }
}
