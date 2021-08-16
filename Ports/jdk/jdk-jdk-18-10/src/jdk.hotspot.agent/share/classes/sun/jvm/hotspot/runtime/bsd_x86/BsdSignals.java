/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.runtime.bsd_x86;

public class BsdSignals {
  private static String[] signalNames = {
    "",           /* No signal 0 */
    "SIGHUP",     /* hangup */
    "SIGINT",     /* interrupt */
    "SIGQUIT",    /* quit */
    "SIGILL",     /* illegal instr. (not reset when caught) */
    "SIGTRAP",    /* trace trap (not reset when caught) */
    "SIGABRT",    /* abort() */
    "SIGEMT",     /* EMT instruction */
    "SIGFPE",     /* floating point exception */
    "SIGKILL",    /* kill (cannot be caught or ignored) */
    "SIGBUS",     /* bus error */
    "SIGSEGV",    /* segmentation violation */
    "SIGSYS",     /* non-existent system call invoked */
    "SIGPIPE",    /* write on a pipe with no one to read it */
    "SIGALRM",    /* alarm clock */
    "SIGTERM",    /* software termination signal from kill */
    "SIGURG",     /* urgent condition on IO channel */
    "SIGSTOP",    /* sendable stop signal not from tty */
    "SIGTSTP",    /* stop signal from tty */
    "SIGCONT",    /* continue a stopped process */
    "SIGCHLD",    /* to parent on child stop or exit */
    "SIGTTIN",    /* to readers pgrp upon background tty read */
    "SIGTTOU",    /* like TTIN if (tp->t_local&LTOSTOP) */
    "SIGIO",      /* input/output possible signal */
    "SIGXCPU",    /* exceeded CPU time limit */
    "SIGXFSZ",    /* exceeded file size limit */
    "SIGVTALRM",  /* virtual time alarm */
    "SIGPROF",    /* profiling time alarm */
    "SIGWINCH",   /* window size changes */
    "SIGINFO",    /* information request */
    "SIGUSR1",    /* user defined signal 1 */
    "SIGUSR2"     /* user defined signal 2 */
  };

  public static String getSignalName(int sigNum) {
    if ((sigNum <= 0) || (sigNum >= signalNames.length)) {
      // Probably best to fail in a non-destructive way
      return "<Error: Illegal signal number " + sigNum + ">";
    }
    return signalNames[sigNum];
  }
}
