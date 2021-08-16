/*
 * Copyright (c) 2012, 2015 SAP SE. All rights reserved.
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


#ifndef OS_AIX_MISC_AIX_HPP
#define OS_AIX_MISC_AIX_HPP

// misc_aix.hpp, misc_aix.cpp: convenience functions needed for the OpenJDK AIX
// port.
#include "utilities/globalDefinitions.hpp"
#include "runtime/globals.hpp"
#include "utilities/debug.hpp"

#include <pthread.h>

// Trace if verbose to tty.
#define trcVerbose(fmt, ...) { \
  if (Verbose) { \
    fprintf(stderr, fmt, ##__VA_ARGS__); \
    fputc('\n', stderr); fflush(stderr); \
  } \
}

#define assert0(b) assert((b), "")
#define guarantee0(b) guarantee((b), "")
template <class T1, class T2> bool is_aligned_to(T1 what, T2 alignment) {
  return (((uintx)(what)) & (((uintx)(alignment)) - 1)) == 0 ? true : false;
}

// CritSect: simple critical section implementation using pthread mutexes.
namespace MiscUtils {
  typedef pthread_mutex_t critsect_t;

  void init_critsect(MiscUtils::critsect_t* cs);
  void free_critsect(MiscUtils::critsect_t* cs);
  void enter_critsect(MiscUtils::critsect_t* cs);
  void leave_critsect(MiscUtils::critsect_t* cs);

  // Need to wrap this in an object because we need to dynamically initialize
  // critical section (because of windows, where there is no way to initialize
  // a CRITICAL_SECTION statically. On Unix, we could use
  // PTHREAD_MUTEX_INITIALIZER).

  // Note: The critical section does NOT get cleaned up in the destructor. That is
  // by design: the CritSect class is only ever used as global objects whose
  // lifetime spans the whole VM life; in that context we don't want the lock to
  // be cleaned up when global C++ objects are destroyed, but to continue to work
  // correctly right to the very end of the process life.
  class CritSect {
    critsect_t _cs;
   public:
    CritSect()        { init_critsect(&_cs); }
    //~CritSect()       { free_critsect(&_cs); }
    void enter()      { enter_critsect(&_cs); }
    void leave()      { leave_critsect(&_cs); }
  };

  class AutoCritSect {
    CritSect* const _pcsobj;
   public:
    AutoCritSect(CritSect* pcsobj)
      : _pcsobj(pcsobj)
    {
      _pcsobj->enter();
    }
    ~AutoCritSect() {
      _pcsobj->leave();
    }
  };
}

#endif // OS_AIX_MISC_AIX_HPP
