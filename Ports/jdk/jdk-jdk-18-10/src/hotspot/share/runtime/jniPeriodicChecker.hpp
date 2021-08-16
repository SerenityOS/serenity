/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_JNIPERIODICCHECKER_HPP
#define SHARE_RUNTIME_JNIPERIODICCHECKER_HPP

class JniPeriodicCheckerTask;

/*
 * This gets activated under Xcheck:jni (CheckJNICalls), and is typically
 * to detect any errors caused by JNI applications, such as signal handler,
 * hijacking, va 0x0 hijacking either by mmap or an OS error.
 */


class JniPeriodicChecker : AllStatic {

  friend class JniPeriodicCheckerTask;

  private:
    static JniPeriodicCheckerTask* _task;

  public:
    // Start/stop task
    static void engage();
    static void disengage();

    static bool is_active() { return _task != NULL; }

    static void initialize();
    static void destroy();
};

void jniPeriodicChecker_exit();

#endif // SHARE_RUNTIME_JNIPERIODICCHECKER_HPP
