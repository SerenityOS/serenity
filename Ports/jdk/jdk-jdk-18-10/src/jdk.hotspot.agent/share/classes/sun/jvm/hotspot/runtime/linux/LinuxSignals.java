/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.runtime.linux;

public class LinuxSignals {
  private static String[] signalNames = {
    "",           /* No signal 0 */
    "SIGHUP",     /* hangup */
    "SIGINT",     /* interrupt (rubout) */
    "SIGQUIT",    /* quit (ASCII FS) */
    "SIGILL",     /* illegal instruction (not reset when caught) */
    "SIGTRAP",    /* trace trap (not reset when caught) */
    "SIGABRT",    /* used by abort, replace SIGIOT in the future */
    "SIGIOT",
    "SIGBUS",
    "SIGFPE",     /* floating point exception */
    "SIGKILL",    /* kill (cannot be caught or ignored) */
    "SIGUSR1",    /* user defined signal 1 */
    "SIGSEGV",    /* segmentation violation */
    "SIGUSR2",    /* user defined signal 2 */
    "SIGPIPE",    /* write on a pipe with no one to read it */
    "SIGALRM",    /* alarm clock */
    "SIGTERM",    /* software termination signal from kill */
    "SIGSTKFLT",
    "SIGCHLD",    /* child status change alias */
    "SIGCONT",    /* stopped process has been continued */
    "SIGSTOP",    /* stop (cannot be caught or ignored) */
    "SIGTSTP",    /* user stop requested from tty */
    "SIGTTIN",    /* background tty read attempted */
    "SIGTTOU",    /* background tty write attempted */
    "SIGURG",     /* urgent socket condition */
    "SIGXCPU",    /* exceeded cpu limit */
    "SIGXFSZ",    /* exceeded file size limit */
    "SIGVTALRM",  /* virtual timer expired */
    "SIGPROF",    /* profiling timer expired */
    "SIGWINCH",   /* window size change */
    "SIGPOLL",    /* pollable event occured */
    "SIGPWR",     /* power-fail restart */
    "SIGSYS"
  };

  public static String getSignalName(int sigNum) {
    if ((sigNum <= 0) || (sigNum >= signalNames.length)) {
      // Probably best to fail in a non-destructive way
      return "<Error: Illegal signal number " + sigNum + ">";
    }
    return signalNames[sigNum];
  }
}
