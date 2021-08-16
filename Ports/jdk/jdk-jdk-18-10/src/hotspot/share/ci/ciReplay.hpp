/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CI_CIREPLAY_HPP
#define SHARE_CI_CIREPLAY_HPP

#include "ci/ciMethod.hpp"

// ciReplay

//
// Replay compilation of a java method by using an information in replay file.
// Replay inlining decisions during compilation by using an information in inline file.
//
// NOTE: these replay functions only exist in debug version of VM.
//
// Replay compilation.
// -------------------
//
// Replay data file replay.txt can be created by Serviceability Agent
// from a core file, see agent/doc/cireplay.html
//
// $ java -cp <jdk>/lib/sa-jdi.jar sun.jvm.hotspot.CLHSDB
// hsdb> attach <jdk>/bin/java ./core
// hsdb> threads
// t@10 Service Thread
// t@9 C2 CompilerThread0
// t@8 Signal Dispatcher
// t@7 Finalizer
// t@6 Reference Handler
// t@2 main
// hsdb> dumpreplaydata t@9 > replay.txt
// hsdb> quit
//
// (Note: SA could be also used to extract app.jar and boot.jar files
//  from core file to replay compilation if only core file is available)
//
// Replay data file replay_pid%p.log is also created when VM crashes
// in Compiler thread during compilation. It is controlled by
// DumpReplayDataOnError flag which is ON by default.
//
// Replay file replay_pid%p_compid%d.log can be created
// for the specified java method during normal execution using
// CompileCommand option DumpReplay:
//
// -XX:CompileCommand=option,Benchmark::test,DumpReplay
//
// In this case the file name has additional compilation id "_compid%d"
// because the method could be compiled several times.
//
// To replay compilation the replay file should be specified:
//
// -XX:+ReplayCompiles -XX:ReplayDataFile=replay_pid2133.log
//
// VM thread reads data from the file immediately after VM initialization
// and puts the compilation task on compile queue. After that it goes into
// wait state (BackgroundCompilation flag is set to false) since there is no
// a program to execute. VM exits when the compilation is finished.
//
//
// Replay inlining.
// ----------------
//
// Replay inlining file inline_pid%p_compid%d.log is created for
// a specific java method during normal execution of a java program
// using CompileCommand option DumpInline:
//
// -XX:CompileCommand=option,Benchmark::test,DumpInline
//
// To replay inlining the replay file and the method should be specified:
//
// -XX:CompileCommand=option,Benchmark::test,ReplayInline -XX:InlineDataFile=inline_pid3244_compid6.log
//
// The difference from replay compilation is that replay inlining
// is performed during normal java program execution.
//

class ciReplay {
  CI_PACKAGE_ACCESS

#ifndef PRODUCT
 private:
  static int replay_impl(TRAPS);

 public:
  // Replay specified compilation and exit VM.
  static void replay(TRAPS);
  // Load inlining decisions from file and use them
  // during compilation of specified method.
  static void* load_inline_data(ciMethod* method, int entry_bci, int comp_level);

  // These are used by the CI to fill in the cached data from the
  // replay file when replaying compiles.
  static void initialize(ciMethodData* method);
  static void initialize(ciMethod* method);

  static bool is_loaded(Method* method);

  static bool should_not_inline(ciMethod* method);
  static bool should_inline(void* data, ciMethod* method, int bci, int inline_depth);
  static bool should_not_inline(void* data, ciMethod* method, int bci, int inline_depth);

#endif
};

#endif // SHARE_CI_CIREPLAY_HPP
