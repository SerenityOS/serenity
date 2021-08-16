/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.runtime.posix;

public class POSIXSignals {
  private static String[] signalNames = {
    "",           /* No signal 0 */
    "SIGHUP",     /* hangup */
    "SIGINT",     /* interrupt (rubout) */
    "SIGQUIT",    /* quit (ASCII FS) */
    "SIGILL",     /* illegal instruction (not reset when caught) */
    "SIGTRAP",    /* trace trap (not reset when caught) */
    "SIGABRT",    /* used by abort, replace SIGIOT in the future */
    "SIGEMT",     /* EMT instruction */
    "SIGFPE",     /* floating point exception */
    "SIGKILL",    /* kill (cannot be caught or ignored) */
    "SIGBUS",     /* bus error */
    "SIGSEGV",    /* segmentation violation */
    "SIGSYS",     /* bad argument to system call */
    "SIGPIPE",    /* write on a pipe with no one to read it */
    "SIGALRM",    /* alarm clock */
    "SIGTERM",    /* software termination signal from kill */
    "SIGUSR1",    /* user defined signal 1 */
    "SIGUSR2",    /* user defined signal 2 */
    "SIGCHLD",    /* child status change alias (POSIX) */
    "SIGPWR",     /* power-fail restart */
    "SIGWINCH",   /* window size change */
    "SIGURG",     /* urgent socket condition */
    "SIGPOLL",    /* pollable event occured */
    "SIGSTOP",    /* stop (cannot be caught or ignored) */
    "SIGTSTP",    /* user stop requested from tty */
    "SIGCONT",    /* stopped process has been continued */
    "SIGTTIN",    /* background tty read attempted */
    "SIGTTOU",    /* background tty write attempted */
    "SIGVTALRM",  /* virtual timer expired */
    "SIGPROF",    /* profiling timer expired */
    "SIGXCPU",    /* exceeded cpu limit */
    "SIGXFSZ",    /* exceeded file size limit */
    "SIGWAITING", /* process's lwps are blocked */
    "SIGLWP",     /* special signal used by thread library */
    "SIGFREEZE",  /* special signal used by CPR */
    "SIGTHAW",    /* special signal used by CPR */
    "SIGCANCEL",  /* thread cancellation signal used by libthread */
    "SIGLOST",    /* resource lost (eg, record-lock lost) */
  };

  public static String getSignalName(int sigNum) {
    if ((sigNum <= 0) || (sigNum >= signalNames.length)) {
      // Probably best to fail in a non-destructive way
      return "<Error: Illegal signal number " + sigNum + ">";
    }
    return signalNames[sigNum];
  }
}
