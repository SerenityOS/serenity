/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

// no precompiled headers
#include "jvm.h"
#include "classfile/vmSymbols.hpp"
#include "code/icBuffer.hpp"
#include "code/vtableStubs.hpp"
#include "compiler/compileBroker.hpp"
#include "compiler/disassembler.hpp"
#include "interpreter/interpreter.hpp"
#include "jvmtifiles/jvmti.h"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/allocation.inline.hpp"
#include "oops/oop.inline.hpp"
#include "os_bsd.inline.hpp"
#include "os_posix.inline.hpp"
#include "os_share_bsd.hpp"
#include "prims/jniFastGetField.hpp"
#include "prims/jvm_misc.hpp"
#include "runtime/arguments.hpp"
#include "runtime/atomic.hpp"
#include "runtime/globals.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/java.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/objectMonitor.hpp"
#include "runtime/osThread.hpp"
#include "runtime/perfMemory.hpp"
#include "runtime/semaphore.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/statSampler.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/threadCritical.hpp"
#include "runtime/timer.hpp"
#include "services/attachListener.hpp"
#include "services/memTracker.hpp"
#include "services/runtimeService.hpp"
#include "signals_posix.hpp"
#include "utilities/align.hpp"
#include "utilities/decoder.hpp"
#include "utilities/defaultStream.hpp"
#include "utilities/events.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/vmError.hpp"

// put OS-includes here
# include <dlfcn.h>
# include <errno.h>
# include <fcntl.h>
# include <inttypes.h>
# include <poll.h>
# include <pthread.h>
# include <pwd.h>
# include <signal.h>
# include <stdint.h>
# include <stdio.h>
# include <string.h>
# include <sys/ioctl.h>
# include <sys/mman.h>
# include <sys/param.h>
# include <sys/resource.h>
# include <sys/socket.h>
# include <sys/stat.h>
# include <sys/syscall.h>
# include <sys/sysctl.h>
# include <sys/time.h>
# include <sys/times.h>
# include <sys/types.h>
# include <time.h>
# include <unistd.h>

#if defined(__FreeBSD__) || defined(__NetBSD__)
  #include <elf.h>
#endif

#ifdef __APPLE__
  #include <mach-o/dyld.h>
#endif

#ifndef MAP_ANONYMOUS
  #define MAP_ANONYMOUS MAP_ANON
#endif

#define MAX_PATH    (2 * K)

// for timer info max values which include all bits
#define ALL_64_BITS CONST64(0xFFFFFFFFFFFFFFFF)

////////////////////////////////////////////////////////////////////////////////
// global variables
julong os::Bsd::_physical_memory = 0;

#ifdef __APPLE__
mach_timebase_info_data_t os::Bsd::_timebase_info = {0, 0};
volatile uint64_t         os::Bsd::_max_abstime   = 0;
#endif
pthread_t os::Bsd::_main_thread;
int os::Bsd::_page_size = -1;

static jlong initial_time_count=0;

static int clock_tics_per_sec = 100;

#if defined(__APPLE__) && defined(__x86_64__)
static const int processor_id_unassigned = -1;
static const int processor_id_assigning = -2;
static const int processor_id_map_size = 256;
static volatile int processor_id_map[processor_id_map_size];
static volatile int processor_id_next = 0;
#endif

////////////////////////////////////////////////////////////////////////////////
// utility functions

julong os::available_memory() {
  return Bsd::available_memory();
}

// available here means free
julong os::Bsd::available_memory() {
  uint64_t available = physical_memory() >> 2;
#ifdef __APPLE__
  mach_msg_type_number_t count = HOST_VM_INFO64_COUNT;
  vm_statistics64_data_t vmstat;
  kern_return_t kerr = host_statistics64(mach_host_self(), HOST_VM_INFO64,
                                         (host_info64_t)&vmstat, &count);
  assert(kerr == KERN_SUCCESS,
         "host_statistics64 failed - check mach_host_self() and count");
  if (kerr == KERN_SUCCESS) {
    available = vmstat.free_count * os::vm_page_size();
  }
#endif
  return available;
}

// for more info see :
// https://man.openbsd.org/sysctl.2
void os::Bsd::print_uptime_info(outputStream* st) {
  struct timeval boottime;
  size_t len = sizeof(boottime);
  int mib[2];
  mib[0] = CTL_KERN;
  mib[1] = KERN_BOOTTIME;

  if (sysctl(mib, 2, &boottime, &len, NULL, 0) >= 0) {
    time_t bootsec = boottime.tv_sec;
    time_t currsec = time(NULL);
    os::print_dhm(st, "OS uptime:", (long) difftime(currsec, bootsec));
  }
}

julong os::physical_memory() {
  return Bsd::physical_memory();
}

// Return true if user is running as root.

bool os::have_special_privileges() {
  static bool init = false;
  static bool privileges = false;
  if (!init) {
    privileges = (getuid() != geteuid()) || (getgid() != getegid());
    init = true;
  }
  return privileges;
}



// Cpu architecture string
#if   defined(ZERO)
static char cpu_arch[] = ZERO_LIBARCH;
#elif defined(IA64)
static char cpu_arch[] = "ia64";
#elif defined(IA32)
static char cpu_arch[] = "i386";
#elif defined(AMD64)
static char cpu_arch[] = "amd64";
#elif defined(ARM)
static char cpu_arch[] = "arm";
#elif defined(AARCH64)
static char cpu_arch[] = "aarch64";
#elif defined(PPC32)
static char cpu_arch[] = "ppc";
#else
  #error Add appropriate cpu_arch setting
#endif

// Compiler variant
#ifdef COMPILER2
  #define COMPILER_VARIANT "server"
#else
  #define COMPILER_VARIANT "client"
#endif


void os::Bsd::initialize_system_info() {
  int mib[2];
  size_t len;
  int cpu_val;
  julong mem_val;

  // get processors count via hw.ncpus sysctl
  mib[0] = CTL_HW;
  mib[1] = HW_NCPU;
  len = sizeof(cpu_val);
  if (sysctl(mib, 2, &cpu_val, &len, NULL, 0) != -1 && cpu_val >= 1) {
    assert(len == sizeof(cpu_val), "unexpected data size");
    set_processor_count(cpu_val);
  } else {
    set_processor_count(1);   // fallback
  }

#if defined(__APPLE__) && defined(__x86_64__)
  // initialize processor id map
  for (int i = 0; i < processor_id_map_size; i++) {
    processor_id_map[i] = processor_id_unassigned;
  }
#endif

  // get physical memory via hw.memsize sysctl (hw.memsize is used
  // since it returns a 64 bit value)
  mib[0] = CTL_HW;

#if defined (HW_MEMSIZE) // Apple
  mib[1] = HW_MEMSIZE;
#elif defined(HW_PHYSMEM) // Most of BSD
  mib[1] = HW_PHYSMEM;
#elif defined(HW_REALMEM) // Old FreeBSD
  mib[1] = HW_REALMEM;
#else
  #error No ways to get physmem
#endif

  len = sizeof(mem_val);
  if (sysctl(mib, 2, &mem_val, &len, NULL, 0) != -1) {
    assert(len == sizeof(mem_val), "unexpected data size");
    _physical_memory = mem_val;
  } else {
    _physical_memory = 256 * 1024 * 1024;       // fallback (XXXBSD?)
  }

#ifdef __OpenBSD__
  {
    // limit _physical_memory memory view on OpenBSD since
    // datasize rlimit restricts us anyway.
    struct rlimit limits;
    getrlimit(RLIMIT_DATA, &limits);
    _physical_memory = MIN2(_physical_memory, (julong)limits.rlim_cur);
  }
#endif
}

#ifdef __APPLE__
static const char *get_home() {
  const char *home_dir = ::getenv("HOME");
  if ((home_dir == NULL) || (*home_dir == '\0')) {
    struct passwd *passwd_info = getpwuid(geteuid());
    if (passwd_info != NULL) {
      home_dir = passwd_info->pw_dir;
    }
  }

  return home_dir;
}
#endif

void os::init_system_properties_values() {
  // The next steps are taken in the product version:
  //
  // Obtain the JAVA_HOME value from the location of libjvm.so.
  // This library should be located at:
  // <JAVA_HOME>/jre/lib/<arch>/{client|server}/libjvm.so.
  //
  // If "/jre/lib/" appears at the right place in the path, then we
  // assume libjvm.so is installed in a JDK and we use this path.
  //
  // Otherwise exit with message: "Could not create the Java virtual machine."
  //
  // The following extra steps are taken in the debugging version:
  //
  // If "/jre/lib/" does NOT appear at the right place in the path
  // instead of exit check for $JAVA_HOME environment variable.
  //
  // If it is defined and we are able to locate $JAVA_HOME/jre/lib/<arch>,
  // then we append a fake suffix "hotspot/libjvm.so" to this path so
  // it looks like libjvm.so is installed there
  // <JAVA_HOME>/jre/lib/<arch>/hotspot/libjvm.so.
  //
  // Otherwise exit.
  //
  // Important note: if the location of libjvm.so changes this
  // code needs to be changed accordingly.

  // See ld(1):
  //      The linker uses the following search paths to locate required
  //      shared libraries:
  //        1: ...
  //        ...
  //        7: The default directories, normally /lib and /usr/lib.
#ifndef DEFAULT_LIBPATH
  #ifndef OVERRIDE_LIBPATH
    #define DEFAULT_LIBPATH "/lib:/usr/lib"
  #else
    #define DEFAULT_LIBPATH OVERRIDE_LIBPATH
  #endif
#endif

// Base path of extensions installed on the system.
#define SYS_EXT_DIR     "/usr/java/packages"
#define EXTENSIONS_DIR  "/lib/ext"

#ifndef __APPLE__

  // Buffer that fits several sprintfs.
  // Note that the space for the colon and the trailing null are provided
  // by the nulls included by the sizeof operator.
  const size_t bufsize =
    MAX2((size_t)MAXPATHLEN,  // For dll_dir & friends.
         (size_t)MAXPATHLEN + sizeof(EXTENSIONS_DIR) + sizeof(SYS_EXT_DIR) + sizeof(EXTENSIONS_DIR)); // extensions dir
  char *buf = NEW_C_HEAP_ARRAY(char, bufsize, mtInternal);

  // sysclasspath, java_home, dll_dir
  {
    char *pslash;
    os::jvm_path(buf, bufsize);

    // Found the full path to libjvm.so.
    // Now cut the path to <java_home>/jre if we can.
    *(strrchr(buf, '/')) = '\0'; // Get rid of /libjvm.so.
    pslash = strrchr(buf, '/');
    if (pslash != NULL) {
      *pslash = '\0';            // Get rid of /{client|server|hotspot}.
    }
    Arguments::set_dll_dir(buf);

    if (pslash != NULL) {
      pslash = strrchr(buf, '/');
      if (pslash != NULL) {
        *pslash = '\0';          // Get rid of /<arch>.
        pslash = strrchr(buf, '/');
        if (pslash != NULL) {
          *pslash = '\0';        // Get rid of /lib.
        }
      }
    }
    Arguments::set_java_home(buf);
    if (!set_boot_path('/', ':')) {
      vm_exit_during_initialization("Failed setting boot class path.", NULL);
    }
  }

  // Where to look for native libraries.
  //
  // Note: Due to a legacy implementation, most of the library path
  // is set in the launcher. This was to accomodate linking restrictions
  // on legacy Bsd implementations (which are no longer supported).
  // Eventually, all the library path setting will be done here.
  //
  // However, to prevent the proliferation of improperly built native
  // libraries, the new path component /usr/java/packages is added here.
  // Eventually, all the library path setting will be done here.
  {
    // Get the user setting of LD_LIBRARY_PATH, and prepended it. It
    // should always exist (until the legacy problem cited above is
    // addressed).
    const char *v = ::getenv("LD_LIBRARY_PATH");
    const char *v_colon = ":";
    if (v == NULL) { v = ""; v_colon = ""; }
    // That's +1 for the colon and +1 for the trailing '\0'.
    char *ld_library_path = NEW_C_HEAP_ARRAY(char,
                                             strlen(v) + 1 +
                                             sizeof(SYS_EXT_DIR) + sizeof("/lib/") + strlen(cpu_arch) + sizeof(DEFAULT_LIBPATH) + 1,
                                             mtInternal);
    sprintf(ld_library_path, "%s%s" SYS_EXT_DIR "/lib/%s:" DEFAULT_LIBPATH, v, v_colon, cpu_arch);
    Arguments::set_library_path(ld_library_path);
    FREE_C_HEAP_ARRAY(char, ld_library_path);
  }

  // Extensions directories.
  sprintf(buf, "%s" EXTENSIONS_DIR ":" SYS_EXT_DIR EXTENSIONS_DIR, Arguments::get_java_home());
  Arguments::set_ext_dirs(buf);

  FREE_C_HEAP_ARRAY(char, buf);

#else // __APPLE__

  #define SYS_EXTENSIONS_DIR   "/Library/Java/Extensions"
  #define SYS_EXTENSIONS_DIRS  SYS_EXTENSIONS_DIR ":/Network" SYS_EXTENSIONS_DIR ":/System" SYS_EXTENSIONS_DIR ":/usr/lib/java"

  const char *user_home_dir = get_home();
  // The null in SYS_EXTENSIONS_DIRS counts for the size of the colon after user_home_dir.
  size_t system_ext_size = strlen(user_home_dir) + sizeof(SYS_EXTENSIONS_DIR) +
    sizeof(SYS_EXTENSIONS_DIRS);

  // Buffer that fits several sprintfs.
  // Note that the space for the colon and the trailing null are provided
  // by the nulls included by the sizeof operator.
  const size_t bufsize =
    MAX2((size_t)MAXPATHLEN,  // for dll_dir & friends.
         (size_t)MAXPATHLEN + sizeof(EXTENSIONS_DIR) + system_ext_size); // extensions dir
  char *buf = NEW_C_HEAP_ARRAY(char, bufsize, mtInternal);

  // sysclasspath, java_home, dll_dir
  {
    char *pslash;
    os::jvm_path(buf, bufsize);

    // Found the full path to libjvm.so.
    // Now cut the path to <java_home>/jre if we can.
    *(strrchr(buf, '/')) = '\0'; // Get rid of /libjvm.so.
    pslash = strrchr(buf, '/');
    if (pslash != NULL) {
      *pslash = '\0';            // Get rid of /{client|server|hotspot}.
    }
#ifdef STATIC_BUILD
    strcat(buf, "/lib");
#endif

    Arguments::set_dll_dir(buf);

    if (pslash != NULL) {
      pslash = strrchr(buf, '/');
      if (pslash != NULL) {
        *pslash = '\0';          // Get rid of /lib.
      }
    }
    Arguments::set_java_home(buf);
    set_boot_path('/', ':');
  }

  // Where to look for native libraries.
  //
  // Note: Due to a legacy implementation, most of the library path
  // is set in the launcher. This was to accomodate linking restrictions
  // on legacy Bsd implementations (which are no longer supported).
  // Eventually, all the library path setting will be done here.
  //
  // However, to prevent the proliferation of improperly built native
  // libraries, the new path component /usr/java/packages is added here.
  // Eventually, all the library path setting will be done here.
  {
    // Get the user setting of LD_LIBRARY_PATH, and prepended it. It
    // should always exist (until the legacy problem cited above is
    // addressed).
    // Prepend the default path with the JAVA_LIBRARY_PATH so that the app launcher code
    // can specify a directory inside an app wrapper
    const char *l = ::getenv("JAVA_LIBRARY_PATH");
    const char *l_colon = ":";
    if (l == NULL) { l = ""; l_colon = ""; }

    const char *v = ::getenv("DYLD_LIBRARY_PATH");
    const char *v_colon = ":";
    if (v == NULL) { v = ""; v_colon = ""; }

    // Apple's Java6 has "." at the beginning of java.library.path.
    // OpenJDK on Windows has "." at the end of java.library.path.
    // OpenJDK on Linux and Solaris don't have "." in java.library.path
    // at all. To ease the transition from Apple's Java6 to OpenJDK7,
    // "." is appended to the end of java.library.path. Yes, this
    // could cause a change in behavior, but Apple's Java6 behavior
    // can be achieved by putting "." at the beginning of the
    // JAVA_LIBRARY_PATH environment variable.
    char *ld_library_path = NEW_C_HEAP_ARRAY(char,
                                             strlen(v) + 1 + strlen(l) + 1 +
                                             system_ext_size + 3,
                                             mtInternal);
    sprintf(ld_library_path, "%s%s%s%s%s" SYS_EXTENSIONS_DIR ":" SYS_EXTENSIONS_DIRS ":.",
            v, v_colon, l, l_colon, user_home_dir);
    Arguments::set_library_path(ld_library_path);
    FREE_C_HEAP_ARRAY(char, ld_library_path);
  }

  // Extensions directories.
  //
  // Note that the space for the colon and the trailing null are provided
  // by the nulls included by the sizeof operator (so actually one byte more
  // than necessary is allocated).
  sprintf(buf, "%s" SYS_EXTENSIONS_DIR ":%s" EXTENSIONS_DIR ":" SYS_EXTENSIONS_DIRS,
          user_home_dir, Arguments::get_java_home());
  Arguments::set_ext_dirs(buf);

  FREE_C_HEAP_ARRAY(char, buf);

#undef SYS_EXTENSIONS_DIR
#undef SYS_EXTENSIONS_DIRS

#endif // __APPLE__

#undef SYS_EXT_DIR
#undef EXTENSIONS_DIR
}

////////////////////////////////////////////////////////////////////////////////
// breakpoint support

void os::breakpoint() {
  BREAKPOINT;
}

extern "C" void breakpoint() {
  // use debugger to set breakpoint here
}

//////////////////////////////////////////////////////////////////////////////
// create new thread

#ifdef __APPLE__
// library handle for calling objc_registerThreadWithCollector()
// without static linking to the libobjc library
  #define OBJC_LIB "/usr/lib/libobjc.dylib"
  #define OBJC_GCREGISTER "objc_registerThreadWithCollector"
typedef void (*objc_registerThreadWithCollector_t)();
extern "C" objc_registerThreadWithCollector_t objc_registerThreadWithCollectorFunction;
objc_registerThreadWithCollector_t objc_registerThreadWithCollectorFunction = NULL;
#endif

// Thread start routine for all newly created threads
static void *thread_native_entry(Thread *thread) {

  thread->record_stack_base_and_size();
  thread->initialize_thread_current();

  OSThread* osthread = thread->osthread();
  Monitor* sync = osthread->startThread_lock();

  osthread->set_thread_id(os::Bsd::gettid());

#ifdef __APPLE__
  // Store unique OS X thread id used by SA
  osthread->set_unique_thread_id();
#endif

  // initialize signal mask for this thread
  PosixSignals::hotspot_sigmask(thread);

  // initialize floating point control register
  os::Bsd::init_thread_fpu_state();

#ifdef __APPLE__
  // register thread with objc gc
  if (objc_registerThreadWithCollectorFunction != NULL) {
    objc_registerThreadWithCollectorFunction();
  }
#endif

  // handshaking with parent thread
  {
    MutexLocker ml(sync, Mutex::_no_safepoint_check_flag);

    // notify parent thread
    osthread->set_state(INITIALIZED);
    sync->notify_all();

    // wait until os::start_thread()
    while (osthread->get_state() == INITIALIZED) {
      sync->wait_without_safepoint_check();
    }
  }

  log_info(os, thread)("Thread is alive (tid: " UINTX_FORMAT ", pthread id: " UINTX_FORMAT ").",
    os::current_thread_id(), (uintx) pthread_self());

  // call one more level start routine
  thread->call_run();

  // Note: at this point the thread object may already have deleted itself.
  // Prevent dereferencing it from here on out.
  thread = NULL;

  log_info(os, thread)("Thread finished (tid: " UINTX_FORMAT ", pthread id: " UINTX_FORMAT ").",
    os::current_thread_id(), (uintx) pthread_self());

  return 0;
}

bool os::create_thread(Thread* thread, ThreadType thr_type,
                       size_t req_stack_size) {
  assert(thread->osthread() == NULL, "caller responsible");

  // Allocate the OSThread object
  OSThread* osthread = new OSThread(NULL, NULL);
  if (osthread == NULL) {
    return false;
  }

  // set the correct thread state
  osthread->set_thread_type(thr_type);

  // Initial state is ALLOCATED but not INITIALIZED
  osthread->set_state(ALLOCATED);

  thread->set_osthread(osthread);

  // init thread attributes
  pthread_attr_t attr;
  pthread_attr_init(&attr);
  pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);

  // calculate stack size if it's not specified by caller
  size_t stack_size = os::Posix::get_initial_stack_size(thr_type, req_stack_size);
  int status = pthread_attr_setstacksize(&attr, stack_size);
  assert_status(status == 0, status, "pthread_attr_setstacksize");

  ThreadState state;

  {

    ResourceMark rm;
    pthread_t tid;
    int ret = 0;
    int limit = 3;
    do {
      ret = pthread_create(&tid, &attr, (void* (*)(void*)) thread_native_entry, thread);
    } while (ret == EAGAIN && limit-- > 0);

    char buf[64];
    if (ret == 0) {
      log_info(os, thread)("Thread \"%s\" started (pthread id: " UINTX_FORMAT ", attributes: %s). ",
                           thread->name(), (uintx) tid, os::Posix::describe_pthread_attr(buf, sizeof(buf), &attr));
    } else {
      log_warning(os, thread)("Failed to start thread \"%s\" - pthread_create failed (%s) for attributes: %s.",
                              thread->name(), os::errno_name(ret), os::Posix::describe_pthread_attr(buf, sizeof(buf), &attr));
      // Log some OS information which might explain why creating the thread failed.
      log_info(os, thread)("Number of threads approx. running in the VM: %d", Threads::number_of_threads());
      LogStream st(Log(os, thread)::info());
      os::Posix::print_rlimit_info(&st);
      os::print_memory_info(&st);
    }

    pthread_attr_destroy(&attr);

    if (ret != 0) {
      // Need to clean up stuff we've allocated so far
      thread->set_osthread(NULL);
      delete osthread;
      return false;
    }

    // Store pthread info into the OSThread
    osthread->set_pthread_id(tid);

    // Wait until child thread is either initialized or aborted
    {
      Monitor* sync_with_child = osthread->startThread_lock();
      MutexLocker ml(sync_with_child, Mutex::_no_safepoint_check_flag);
      while ((state = osthread->get_state()) == ALLOCATED) {
        sync_with_child->wait_without_safepoint_check();
      }
    }

  }

  // The thread is returned suspended (in state INITIALIZED),
  // and is started higher up in the call chain
  assert(state == INITIALIZED, "race condition");
  return true;
}

/////////////////////////////////////////////////////////////////////////////
// attach existing thread

// bootstrap the main thread
bool os::create_main_thread(JavaThread* thread) {
  assert(os::Bsd::_main_thread == pthread_self(), "should be called inside main thread");
  return create_attached_thread(thread);
}

bool os::create_attached_thread(JavaThread* thread) {
#ifdef ASSERT
  thread->verify_not_published();
#endif

  // Allocate the OSThread object
  OSThread* osthread = new OSThread(NULL, NULL);

  if (osthread == NULL) {
    return false;
  }

  osthread->set_thread_id(os::Bsd::gettid());

#ifdef __APPLE__
  // Store unique OS X thread id used by SA
  osthread->set_unique_thread_id();
#endif

  // Store pthread info into the OSThread
  osthread->set_pthread_id(::pthread_self());

  // initialize floating point control register
  os::Bsd::init_thread_fpu_state();

  // Initial thread state is RUNNABLE
  osthread->set_state(RUNNABLE);

  thread->set_osthread(osthread);

  // initialize signal mask for this thread
  // and save the caller's signal mask
  PosixSignals::hotspot_sigmask(thread);

  log_info(os, thread)("Thread attached (tid: " UINTX_FORMAT ", pthread id: " UINTX_FORMAT ").",
    os::current_thread_id(), (uintx) pthread_self());

  return true;
}

void os::pd_start_thread(Thread* thread) {
  OSThread * osthread = thread->osthread();
  assert(osthread->get_state() != INITIALIZED, "just checking");
  Monitor* sync_with_child = osthread->startThread_lock();
  MutexLocker ml(sync_with_child, Mutex::_no_safepoint_check_flag);
  sync_with_child->notify();
}

// Free Bsd resources related to the OSThread
void os::free_thread(OSThread* osthread) {
  assert(osthread != NULL, "osthread not set");

  // We are told to free resources of the argument thread,
  // but we can only really operate on the current thread.
  assert(Thread::current()->osthread() == osthread,
         "os::free_thread but not current thread");

  // Restore caller's signal mask
  sigset_t sigmask = osthread->caller_sigmask();
  pthread_sigmask(SIG_SETMASK, &sigmask, NULL);

  delete osthread;
}

////////////////////////////////////////////////////////////////////////////////
// time support

// Time since start-up in seconds to a fine granularity.
double os::elapsedTime() {
  return ((double)os::elapsed_counter()) / os::elapsed_frequency();
}

jlong os::elapsed_counter() {
  return javaTimeNanos() - initial_time_count;
}

jlong os::elapsed_frequency() {
  return NANOSECS_PER_SEC; // nanosecond resolution
}

bool os::supports_vtime() { return true; }

double os::elapsedVTime() {
  // better than nothing, but not much
  return elapsedTime();
}

#ifdef __APPLE__
void os::Bsd::clock_init() {
  mach_timebase_info(&_timebase_info);
}
#else
void os::Bsd::clock_init() {
  // Nothing to do
}
#endif



#ifdef __APPLE__

jlong os::javaTimeNanos() {
  const uint64_t tm = mach_absolute_time();
  const uint64_t now = (tm * Bsd::_timebase_info.numer) / Bsd::_timebase_info.denom;
  const uint64_t prev = Bsd::_max_abstime;
  if (now <= prev) {
    return prev;   // same or retrograde time;
  }
  const uint64_t obsv = Atomic::cmpxchg(&Bsd::_max_abstime, prev, now);
  assert(obsv >= prev, "invariant");   // Monotonicity
  // If the CAS succeeded then we're done and return "now".
  // If the CAS failed and the observed value "obsv" is >= now then
  // we should return "obsv".  If the CAS failed and now > obsv > prv then
  // some other thread raced this thread and installed a new value, in which case
  // we could either (a) retry the entire operation, (b) retry trying to install now
  // or (c) just return obsv.  We use (c).   No loop is required although in some cases
  // we might discard a higher "now" value in deference to a slightly lower but freshly
  // installed obsv value.   That's entirely benign -- it admits no new orderings compared
  // to (a) or (b) -- and greatly reduces coherence traffic.
  // We might also condition (c) on the magnitude of the delta between obsv and now.
  // Avoiding excessive CAS operations to hot RW locations is critical.
  // See https://blogs.oracle.com/dave/entry/cas_and_cache_trivia_invalidate
  return (prev == obsv) ? now : obsv;
}

void os::javaTimeNanos_info(jvmtiTimerInfo *info_ptr) {
  info_ptr->max_value = ALL_64_BITS;
  info_ptr->may_skip_backward = false;      // not subject to resetting or drifting
  info_ptr->may_skip_forward = false;       // not subject to resetting or drifting
  info_ptr->kind = JVMTI_TIMER_ELAPSED;     // elapsed not CPU time
}

#endif // __APPLE__

// Return the real, user, and system times in seconds from an
// arbitrary fixed point in the past.
bool os::getTimesSecs(double* process_real_time,
                      double* process_user_time,
                      double* process_system_time) {
  struct tms ticks;
  clock_t real_ticks = times(&ticks);

  if (real_ticks == (clock_t) (-1)) {
    return false;
  } else {
    double ticks_per_second = (double) clock_tics_per_sec;
    *process_user_time = ((double) ticks.tms_utime) / ticks_per_second;
    *process_system_time = ((double) ticks.tms_stime) / ticks_per_second;
    *process_real_time = ((double) real_ticks) / ticks_per_second;

    return true;
  }
}


char * os::local_time_string(char *buf, size_t buflen) {
  struct tm t;
  time_t long_time;
  time(&long_time);
  localtime_r(&long_time, &t);
  jio_snprintf(buf, buflen, "%d-%02d-%02d %02d:%02d:%02d",
               t.tm_year + 1900, t.tm_mon + 1, t.tm_mday,
               t.tm_hour, t.tm_min, t.tm_sec);
  return buf;
}

struct tm* os::localtime_pd(const time_t* clock, struct tm*  res) {
  return localtime_r(clock, res);
}

// Information of current thread in variety of formats
pid_t os::Bsd::gettid() {
  int retval = -1;

#ifdef __APPLE__ // XNU kernel
  mach_port_t port = mach_thread_self();
  guarantee(MACH_PORT_VALID(port), "just checking");
  mach_port_deallocate(mach_task_self(), port);
  return (pid_t)port;

#else
  #ifdef __FreeBSD__
  retval = syscall(SYS_thr_self);
  #else
    #ifdef __OpenBSD__
  retval = syscall(SYS_getthrid);
    #else
      #ifdef __NetBSD__
  retval = (pid_t) syscall(SYS__lwp_self);
      #endif
    #endif
  #endif
#endif

  if (retval == -1) {
    return getpid();
  }
}

intx os::current_thread_id() {
#ifdef __APPLE__
  return (intx)os::Bsd::gettid();
#else
  return (intx)::pthread_self();
#endif
}

int os::current_process_id() {
  return (int)(getpid());
}

// DLL functions

const char* os::dll_file_extension() { return JNI_LIB_SUFFIX; }

// This must be hard coded because it's the system's temporary
// directory not the java application's temp directory, ala java.io.tmpdir.
#ifdef __APPLE__
// macosx has a secure per-user temporary directory
char temp_path_storage[PATH_MAX];
const char* os::get_temp_directory() {
  static char *temp_path = NULL;
  if (temp_path == NULL) {
    int pathSize = confstr(_CS_DARWIN_USER_TEMP_DIR, temp_path_storage, PATH_MAX);
    if (pathSize == 0 || pathSize > PATH_MAX) {
      strlcpy(temp_path_storage, "/tmp/", sizeof(temp_path_storage));
    }
    temp_path = temp_path_storage;
  }
  return temp_path;
}
#else // __APPLE__
const char* os::get_temp_directory() { return "/tmp"; }
#endif // __APPLE__

// check if addr is inside libjvm.so
bool os::address_is_in_vm(address addr) {
  static address libjvm_base_addr;
  Dl_info dlinfo;

  if (libjvm_base_addr == NULL) {
    if (dladdr(CAST_FROM_FN_PTR(void *, os::address_is_in_vm), &dlinfo) != 0) {
      libjvm_base_addr = (address)dlinfo.dli_fbase;
    }
    assert(libjvm_base_addr !=NULL, "Cannot obtain base address for libjvm");
  }

  if (dladdr((void *)addr, &dlinfo) != 0) {
    if (libjvm_base_addr == (address)dlinfo.dli_fbase) return true;
  }

  return false;
}


#define MACH_MAXSYMLEN 256

bool os::dll_address_to_function_name(address addr, char *buf,
                                      int buflen, int *offset,
                                      bool demangle) {
  // buf is not optional, but offset is optional
  assert(buf != NULL, "sanity check");

  Dl_info dlinfo;
  char localbuf[MACH_MAXSYMLEN];

  if (dladdr((void*)addr, &dlinfo) != 0) {
    // see if we have a matching symbol
    if (dlinfo.dli_saddr != NULL && dlinfo.dli_sname != NULL) {
      if (!(demangle && Decoder::demangle(dlinfo.dli_sname, buf, buflen))) {
        jio_snprintf(buf, buflen, "%s", dlinfo.dli_sname);
      }
      if (offset != NULL) *offset = addr - (address)dlinfo.dli_saddr;
      return true;
    }
    // no matching symbol so try for just file info
    if (dlinfo.dli_fname != NULL && dlinfo.dli_fbase != NULL) {
      if (Decoder::decode((address)(addr - (address)dlinfo.dli_fbase),
                          buf, buflen, offset, dlinfo.dli_fname, demangle)) {
        return true;
      }
    }

    // Handle non-dynamic manually:
    if (dlinfo.dli_fbase != NULL &&
        Decoder::decode(addr, localbuf, MACH_MAXSYMLEN, offset,
                        dlinfo.dli_fbase)) {
      if (!(demangle && Decoder::demangle(localbuf, buf, buflen))) {
        jio_snprintf(buf, buflen, "%s", localbuf);
      }
      return true;
    }
  }
  buf[0] = '\0';
  if (offset != NULL) *offset = -1;
  return false;
}

// ported from solaris version
bool os::dll_address_to_library_name(address addr, char* buf,
                                     int buflen, int* offset) {
  // buf is not optional, but offset is optional
  assert(buf != NULL, "sanity check");

  Dl_info dlinfo;

  if (dladdr((void*)addr, &dlinfo) != 0) {
    if (dlinfo.dli_fname != NULL) {
      jio_snprintf(buf, buflen, "%s", dlinfo.dli_fname);
    }
    if (dlinfo.dli_fbase != NULL && offset != NULL) {
      *offset = addr - (address)dlinfo.dli_fbase;
    }
    return true;
  }

  buf[0] = '\0';
  if (offset) *offset = -1;
  return false;
}

// Loads .dll/.so and
// in case of error it checks if .dll/.so was built for the
// same architecture as Hotspot is running on

#ifdef __APPLE__
void * os::dll_load(const char *filename, char *ebuf, int ebuflen) {
#ifdef STATIC_BUILD
  return os::get_default_process_handle();
#else
  log_info(os)("attempting shared library load of %s", filename);

  void * result= ::dlopen(filename, RTLD_LAZY);
  if (result != NULL) {
    Events::log(NULL, "Loaded shared library %s", filename);
    // Successful loading
    log_info(os)("shared library load of %s was successful", filename);
    return result;
  }

  const char* error_report = ::dlerror();
  if (error_report == NULL) {
    error_report = "dlerror returned no error description";
  }
  if (ebuf != NULL && ebuflen > 0) {
    // Read system error message into ebuf
    ::strncpy(ebuf, error_report, ebuflen-1);
    ebuf[ebuflen-1]='\0';
  }
  Events::log(NULL, "Loading shared library %s failed, %s", filename, error_report);
  log_info(os)("shared library load of %s failed, %s", filename, error_report);

  return NULL;
#endif // STATIC_BUILD
}
#else
void * os::dll_load(const char *filename, char *ebuf, int ebuflen) {
#ifdef STATIC_BUILD
  return os::get_default_process_handle();
#else
  log_info(os)("attempting shared library load of %s", filename);
  void * result= ::dlopen(filename, RTLD_LAZY);
  if (result != NULL) {
    Events::log(NULL, "Loaded shared library %s", filename);
    // Successful loading
    log_info(os)("shared library load of %s was successful", filename);
    return result;
  }

  Elf32_Ehdr elf_head;

  const char* const error_report = ::dlerror();
  if (error_report == NULL) {
    error_report = "dlerror returned no error description";
  }
  if (ebuf != NULL && ebuflen > 0) {
    // Read system error message into ebuf
    ::strncpy(ebuf, error_report, ebuflen-1);
    ebuf[ebuflen-1]='\0';
  }
  Events::log(NULL, "Loading shared library %s failed, %s", filename, error_report);
  log_info(os)("shared library load of %s failed, %s", filename, error_report);

  int diag_msg_max_length=ebuflen-strlen(ebuf);
  char* diag_msg_buf=ebuf+strlen(ebuf);

  if (diag_msg_max_length==0) {
    // No more space in ebuf for additional diagnostics message
    return NULL;
  }


  int file_descriptor= ::open(filename, O_RDONLY | O_NONBLOCK);

  if (file_descriptor < 0) {
    // Can't open library, report dlerror() message
    return NULL;
  }

  bool failed_to_read_elf_head=
    (sizeof(elf_head)!=
     (::read(file_descriptor, &elf_head,sizeof(elf_head))));

  ::close(file_descriptor);
  if (failed_to_read_elf_head) {
    // file i/o error - report dlerror() msg
    return NULL;
  }

  typedef struct {
    Elf32_Half  code;         // Actual value as defined in elf.h
    Elf32_Half  compat_class; // Compatibility of archs at VM's sense
    char        elf_class;    // 32 or 64 bit
    char        endianess;    // MSB or LSB
    char*       name;         // String representation
  } arch_t;

  #ifndef EM_486
    #define EM_486          6               /* Intel 80486 */
  #endif

  #ifndef EM_MIPS_RS3_LE
    #define EM_MIPS_RS3_LE  10              /* MIPS */
  #endif

  #ifndef EM_PPC64
    #define EM_PPC64        21              /* PowerPC64 */
  #endif

  #ifndef EM_S390
    #define EM_S390         22              /* IBM System/390 */
  #endif

  #ifndef EM_IA_64
    #define EM_IA_64        50              /* HP/Intel IA-64 */
  #endif

  #ifndef EM_X86_64
    #define EM_X86_64       62              /* AMD x86-64 */
  #endif

  static const arch_t arch_array[]={
    {EM_386,         EM_386,     ELFCLASS32, ELFDATA2LSB, (char*)"IA 32"},
    {EM_486,         EM_386,     ELFCLASS32, ELFDATA2LSB, (char*)"IA 32"},
    {EM_IA_64,       EM_IA_64,   ELFCLASS64, ELFDATA2LSB, (char*)"IA 64"},
    {EM_X86_64,      EM_X86_64,  ELFCLASS64, ELFDATA2LSB, (char*)"AMD 64"},
    {EM_PPC,         EM_PPC,     ELFCLASS32, ELFDATA2MSB, (char*)"Power PC 32"},
    {EM_PPC64,       EM_PPC64,   ELFCLASS64, ELFDATA2MSB, (char*)"Power PC 64"},
    {EM_ARM,         EM_ARM,     ELFCLASS32,   ELFDATA2LSB, (char*)"ARM"},
    {EM_S390,        EM_S390,    ELFCLASSNONE, ELFDATA2MSB, (char*)"IBM System/390"},
    {EM_ALPHA,       EM_ALPHA,   ELFCLASS64, ELFDATA2LSB, (char*)"Alpha"},
    {EM_MIPS_RS3_LE, EM_MIPS_RS3_LE, ELFCLASS32, ELFDATA2LSB, (char*)"MIPSel"},
    {EM_MIPS,        EM_MIPS,    ELFCLASS32, ELFDATA2MSB, (char*)"MIPS"},
    {EM_PARISC,      EM_PARISC,  ELFCLASS32, ELFDATA2MSB, (char*)"PARISC"},
    {EM_68K,         EM_68K,     ELFCLASS32, ELFDATA2MSB, (char*)"M68k"}
  };

  #if  (defined IA32)
  static  Elf32_Half running_arch_code=EM_386;
  #elif   (defined AMD64)
  static  Elf32_Half running_arch_code=EM_X86_64;
  #elif  (defined IA64)
  static  Elf32_Half running_arch_code=EM_IA_64;
  #elif  (defined __powerpc64__)
  static  Elf32_Half running_arch_code=EM_PPC64;
  #elif  (defined __powerpc__)
  static  Elf32_Half running_arch_code=EM_PPC;
  #elif  (defined ARM)
  static  Elf32_Half running_arch_code=EM_ARM;
  #elif  (defined S390)
  static  Elf32_Half running_arch_code=EM_S390;
  #elif  (defined ALPHA)
  static  Elf32_Half running_arch_code=EM_ALPHA;
  #elif  (defined MIPSEL)
  static  Elf32_Half running_arch_code=EM_MIPS_RS3_LE;
  #elif  (defined PARISC)
  static  Elf32_Half running_arch_code=EM_PARISC;
  #elif  (defined MIPS)
  static  Elf32_Half running_arch_code=EM_MIPS;
  #elif  (defined M68K)
  static  Elf32_Half running_arch_code=EM_68K;
  #else
    #error Method os::dll_load requires that one of following is defined:\
         IA32, AMD64, IA64, __powerpc__, ARM, S390, ALPHA, MIPS, MIPSEL, PARISC, M68K
  #endif

  // Identify compatability class for VM's architecture and library's architecture
  // Obtain string descriptions for architectures

  arch_t lib_arch={elf_head.e_machine,0,elf_head.e_ident[EI_CLASS], elf_head.e_ident[EI_DATA], NULL};
  int running_arch_index=-1;

  for (unsigned int i=0; i < ARRAY_SIZE(arch_array); i++) {
    if (running_arch_code == arch_array[i].code) {
      running_arch_index    = i;
    }
    if (lib_arch.code == arch_array[i].code) {
      lib_arch.compat_class = arch_array[i].compat_class;
      lib_arch.name         = arch_array[i].name;
    }
  }

  assert(running_arch_index != -1,
         "Didn't find running architecture code (running_arch_code) in arch_array");
  if (running_arch_index == -1) {
    // Even though running architecture detection failed
    // we may still continue with reporting dlerror() message
    return NULL;
  }

  if (lib_arch.endianess != arch_array[running_arch_index].endianess) {
    ::snprintf(diag_msg_buf, diag_msg_max_length-1," (Possible cause: endianness mismatch)");
    return NULL;
  }

#ifndef S390
  if (lib_arch.elf_class != arch_array[running_arch_index].elf_class) {
    ::snprintf(diag_msg_buf, diag_msg_max_length-1," (Possible cause: architecture word width mismatch)");
    return NULL;
  }
#endif // !S390

  if (lib_arch.compat_class != arch_array[running_arch_index].compat_class) {
    if (lib_arch.name!=NULL) {
      ::snprintf(diag_msg_buf, diag_msg_max_length-1,
                 " (Possible cause: can't load %s-bit .so on a %s-bit platform)",
                 lib_arch.name, arch_array[running_arch_index].name);
    } else {
      ::snprintf(diag_msg_buf, diag_msg_max_length-1,
                 " (Possible cause: can't load this .so (machine code=0x%x) on a %s-bit platform)",
                 lib_arch.code,
                 arch_array[running_arch_index].name);
    }
  }

  return NULL;
#endif // STATIC_BUILD
}
#endif // !__APPLE__

void* os::get_default_process_handle() {
#ifdef __APPLE__
  // MacOS X needs to use RTLD_FIRST instead of RTLD_LAZY
  // to avoid finding unexpected symbols on second (or later)
  // loads of a library.
  return (void*)::dlopen(NULL, RTLD_FIRST);
#else
  return (void*)::dlopen(NULL, RTLD_LAZY);
#endif
}

// XXX: Do we need a lock around this as per Linux?
void* os::dll_lookup(void* handle, const char* name) {
  return dlsym(handle, name);
}

int _print_dll_info_cb(const char * name, address base_address, address top_address, void * param) {
  outputStream * out = (outputStream *) param;
  out->print_cr(INTPTR_FORMAT " \t%s", (intptr_t)base_address, name);
  return 0;
}

void os::print_dll_info(outputStream *st) {
  st->print_cr("Dynamic libraries:");
  if (get_loaded_modules_info(_print_dll_info_cb, (void *)st)) {
    st->print_cr("Error: Cannot print dynamic libraries.");
  }
}

int os::get_loaded_modules_info(os::LoadedModulesCallbackFunc callback, void *param) {
#ifdef RTLD_DI_LINKMAP
  Dl_info dli;
  void *handle;
  Link_map *map;
  Link_map *p;

  if (dladdr(CAST_FROM_FN_PTR(void *, os::print_dll_info), &dli) == 0 ||
      dli.dli_fname == NULL) {
    return 1;
  }
  handle = dlopen(dli.dli_fname, RTLD_LAZY);
  if (handle == NULL) {
    return 1;
  }
  dlinfo(handle, RTLD_DI_LINKMAP, &map);
  if (map == NULL) {
    dlclose(handle);
    return 1;
  }

  while (map->l_prev != NULL)
    map = map->l_prev;

  while (map != NULL) {
    // Value for top_address is returned as 0 since we don't have any information about module size
    if (callback(map->l_name, (address)map->l_addr, (address)0, param)) {
      dlclose(handle);
      return 1;
    }
    map = map->l_next;
  }

  dlclose(handle);
#elif defined(__APPLE__)
  for (uint32_t i = 1; i < _dyld_image_count(); i++) {
    // Value for top_address is returned as 0 since we don't have any information about module size
    if (callback(_dyld_get_image_name(i), (address)_dyld_get_image_header(i), (address)0, param)) {
      return 1;
    }
  }
  return 0;
#else
  return 1;
#endif
}

void os::get_summary_os_info(char* buf, size_t buflen) {
  // These buffers are small because we want this to be brief
  // and not use a lot of stack while generating the hs_err file.
  char os[100];
  size_t size = sizeof(os);
  int mib_kern[] = { CTL_KERN, KERN_OSTYPE };
  if (sysctl(mib_kern, 2, os, &size, NULL, 0) < 0) {
#ifdef __APPLE__
    strncpy(os, "Darwin", sizeof(os));
#elif __OpenBSD__
    strncpy(os, "OpenBSD", sizeof(os));
#else
    strncpy(os, "BSD", sizeof(os));
#endif
  }

  char release[100];
  size = sizeof(release);
  int mib_release[] = { CTL_KERN, KERN_OSRELEASE };
  if (sysctl(mib_release, 2, release, &size, NULL, 0) < 0) {
    // if error, leave blank
    strncpy(release, "", sizeof(release));
  }

#ifdef __APPLE__
  char osproductversion[100];
  size_t sz = sizeof(osproductversion);
  int ret = sysctlbyname("kern.osproductversion", osproductversion, &sz, NULL, 0);
  if (ret == 0) {
    char build[100];
    size = sizeof(build);
    int mib_build[] = { CTL_KERN, KERN_OSVERSION };
    if (sysctl(mib_build, 2, build, &size, NULL, 0) < 0) {
      snprintf(buf, buflen, "%s %s, macOS %s", os, release, osproductversion);
    } else {
      snprintf(buf, buflen, "%s %s, macOS %s (%s)", os, release, osproductversion, build);
    }
  } else
#endif
  snprintf(buf, buflen, "%s %s", os, release);
}

void os::print_os_info_brief(outputStream* st) {
  os::Posix::print_uname_info(st);
}

void os::print_os_info(outputStream* st) {
  st->print_cr("OS:");

  os::Posix::print_uname_info(st);

  os::Bsd::print_uptime_info(st);

  os::Posix::print_rlimit_info(st);

  os::Posix::print_load_average(st);

  VM_Version::print_platform_virtualization_info(st);
}

void os::pd_print_cpu_info(outputStream* st, char* buf, size_t buflen) {
  // Nothing to do for now.
}

void os::get_summary_cpu_info(char* buf, size_t buflen) {
  unsigned int mhz;
  size_t size = sizeof(mhz);
  int mib[] = { CTL_HW, HW_CPU_FREQ };
  if (sysctl(mib, 2, &mhz, &size, NULL, 0) < 0) {
    mhz = 1;  // looks like an error but can be divided by
  } else {
    mhz /= 1000000;  // reported in millions
  }

  char model[100];
  size = sizeof(model);
  int mib_model[] = { CTL_HW, HW_MODEL };
  if (sysctl(mib_model, 2, model, &size, NULL, 0) < 0) {
    strncpy(model, cpu_arch, sizeof(model));
  }

  char machine[100];
  size = sizeof(machine);
  int mib_machine[] = { CTL_HW, HW_MACHINE };
  if (sysctl(mib_machine, 2, machine, &size, NULL, 0) < 0) {
      strncpy(machine, "", sizeof(machine));
  }

  const char* emulated = "";
#if defined(__APPLE__) && !defined(ZERO)
  if (VM_Version::is_cpu_emulated()) {
    emulated = " (EMULATED)";
  }
#endif
  snprintf(buf, buflen, "\"%s\" %s%s %d MHz", model, machine, emulated, mhz);
}

void os::print_memory_info(outputStream* st) {
  xsw_usage swap_usage;
  size_t size = sizeof(swap_usage);

  st->print("Memory:");
  st->print(" %dk page", os::vm_page_size()>>10);

  st->print(", physical " UINT64_FORMAT "k",
            os::physical_memory() >> 10);
  st->print("(" UINT64_FORMAT "k free)",
            os::available_memory() >> 10);

  if((sysctlbyname("vm.swapusage", &swap_usage, &size, NULL, 0) == 0) || (errno == ENOMEM)) {
    if (size >= offset_of(xsw_usage, xsu_used)) {
      st->print(", swap " UINT64_FORMAT "k",
                ((julong) swap_usage.xsu_total) >> 10);
      st->print("(" UINT64_FORMAT "k free)",
                ((julong) swap_usage.xsu_avail) >> 10);
    }
  }

  st->cr();
}

static char saved_jvm_path[MAXPATHLEN] = {0};

// Find the full path to the current module, libjvm
void os::jvm_path(char *buf, jint buflen) {
  // Error checking.
  if (buflen < MAXPATHLEN) {
    assert(false, "must use a large-enough buffer");
    buf[0] = '\0';
    return;
  }
  // Lazy resolve the path to current module.
  if (saved_jvm_path[0] != 0) {
    strcpy(buf, saved_jvm_path);
    return;
  }

  char dli_fname[MAXPATHLEN];
  dli_fname[0] = '\0';
  bool ret = dll_address_to_library_name(
                                         CAST_FROM_FN_PTR(address, os::jvm_path),
                                         dli_fname, sizeof(dli_fname), NULL);
  assert(ret, "cannot locate libjvm");
  char *rp = NULL;
  if (ret && dli_fname[0] != '\0') {
    rp = os::Posix::realpath(dli_fname, buf, buflen);
  }
  if (rp == NULL) {
    return;
  }

  if (Arguments::sun_java_launcher_is_altjvm()) {
    // Support for the java launcher's '-XXaltjvm=<path>' option. Typical
    // value for buf is "<JAVA_HOME>/jre/lib/<arch>/<vmtype>/libjvm.so"
    // or "<JAVA_HOME>/jre/lib/<vmtype>/libjvm.dylib". If "/jre/lib/"
    // appears at the right place in the string, then assume we are
    // installed in a JDK and we're done. Otherwise, check for a
    // JAVA_HOME environment variable and construct a path to the JVM
    // being overridden.

    const char *p = buf + strlen(buf) - 1;
    for (int count = 0; p > buf && count < 5; ++count) {
      for (--p; p > buf && *p != '/'; --p)
        /* empty */ ;
    }

    if (strncmp(p, "/jre/lib/", 9) != 0) {
      // Look for JAVA_HOME in the environment.
      char* java_home_var = ::getenv("JAVA_HOME");
      if (java_home_var != NULL && java_home_var[0] != 0) {
        char* jrelib_p;
        int len;

        // Check the current module name "libjvm"
        p = strrchr(buf, '/');
        assert(strstr(p, "/libjvm") == p, "invalid library name");

        rp = os::Posix::realpath(java_home_var, buf, buflen);
        if (rp == NULL) {
          return;
        }

        // determine if this is a legacy image or modules image
        // modules image doesn't have "jre" subdirectory
        len = strlen(buf);
        assert(len < buflen, "Ran out of buffer space");
        jrelib_p = buf + len;

        // Add the appropriate library subdir
        snprintf(jrelib_p, buflen-len, "/jre/lib");
        if (0 != access(buf, F_OK)) {
          snprintf(jrelib_p, buflen-len, "/lib");
        }

        // Add the appropriate client or server subdir
        len = strlen(buf);
        jrelib_p = buf + len;
        snprintf(jrelib_p, buflen-len, "/%s", COMPILER_VARIANT);
        if (0 != access(buf, F_OK)) {
          snprintf(jrelib_p, buflen-len, "%s", "");
        }

        // If the path exists within JAVA_HOME, add the JVM library name
        // to complete the path to JVM being overridden.  Otherwise fallback
        // to the path to the current library.
        if (0 == access(buf, F_OK)) {
          // Use current module name "libjvm"
          len = strlen(buf);
          snprintf(buf + len, buflen-len, "/libjvm%s", JNI_LIB_SUFFIX);
        } else {
          // Fall back to path of current library
          rp = os::Posix::realpath(dli_fname, buf, buflen);
          if (rp == NULL) {
            return;
          }
        }
      }
    }
  }

  strncpy(saved_jvm_path, buf, MAXPATHLEN);
  saved_jvm_path[MAXPATHLEN - 1] = '\0';
}

void os::print_jni_name_prefix_on(outputStream* st, int args_size) {
  // no prefix required, not even "_"
}

void os::print_jni_name_suffix_on(outputStream* st, int args_size) {
  // no suffix required
}

////////////////////////////////////////////////////////////////////////////////
// Virtual Memory

int os::vm_page_size() {
  // Seems redundant as all get out
  assert(os::Bsd::page_size() != -1, "must call os::init");
  return os::Bsd::page_size();
}

// Solaris allocates memory by pages.
int os::vm_allocation_granularity() {
  assert(os::Bsd::page_size() != -1, "must call os::init");
  return os::Bsd::page_size();
}

static void warn_fail_commit_memory(char* addr, size_t size, bool exec,
                                    int err) {
  warning("INFO: os::commit_memory(" INTPTR_FORMAT ", " SIZE_FORMAT
          ", %d) failed; error='%s' (errno=%d)", (intptr_t)addr, size, exec,
           os::errno_name(err), err);
}

// NOTE: Bsd kernel does not really reserve the pages for us.
//       All it does is to check if there are enough free pages
//       left at the time of mmap(). This could be a potential
//       problem.
bool os::pd_commit_memory(char* addr, size_t size, bool exec) {
  int prot = exec ? PROT_READ|PROT_WRITE|PROT_EXEC : PROT_READ|PROT_WRITE;
#if defined(__OpenBSD__)
  // XXX: Work-around mmap/MAP_FIXED bug temporarily on OpenBSD
  Events::log(NULL, "Protecting memory [" INTPTR_FORMAT "," INTPTR_FORMAT "] with protection modes %x", p2i(addr), p2i(addr+size), prot);
  if (::mprotect(addr, size, prot) == 0) {
    return true;
  }
#elif defined(__APPLE__)
  if (exec) {
    // Do not replace MAP_JIT mappings, see JDK-8234930
    if (::mprotect(addr, size, prot) == 0) {
      return true;
    }
  } else {
    uintptr_t res = (uintptr_t) ::mmap(addr, size, prot,
                                       MAP_PRIVATE|MAP_FIXED|MAP_ANONYMOUS, -1, 0);
    if (res != (uintptr_t) MAP_FAILED) {
      return true;
    }
  }
#else
  uintptr_t res = (uintptr_t) ::mmap(addr, size, prot,
                                     MAP_PRIVATE|MAP_FIXED|MAP_ANONYMOUS, -1, 0);
  if (res != (uintptr_t) MAP_FAILED) {
    return true;
  }
#endif

  // Warn about any commit errors we see in non-product builds just
  // in case mmap() doesn't work as described on the man page.
  NOT_PRODUCT(warn_fail_commit_memory(addr, size, exec, errno);)

  return false;
}

bool os::pd_commit_memory(char* addr, size_t size, size_t alignment_hint,
                          bool exec) {
  // alignment_hint is ignored on this OS
  return pd_commit_memory(addr, size, exec);
}

void os::pd_commit_memory_or_exit(char* addr, size_t size, bool exec,
                                  const char* mesg) {
  assert(mesg != NULL, "mesg must be specified");
  if (!pd_commit_memory(addr, size, exec)) {
    // add extra info in product mode for vm_exit_out_of_memory():
    PRODUCT_ONLY(warn_fail_commit_memory(addr, size, exec, errno);)
    vm_exit_out_of_memory(size, OOM_MMAP_ERROR, "%s", mesg);
  }
}

void os::pd_commit_memory_or_exit(char* addr, size_t size,
                                  size_t alignment_hint, bool exec,
                                  const char* mesg) {
  // alignment_hint is ignored on this OS
  pd_commit_memory_or_exit(addr, size, exec, mesg);
}

void os::pd_realign_memory(char *addr, size_t bytes, size_t alignment_hint) {
}

void os::pd_free_memory(char *addr, size_t bytes, size_t alignment_hint) {
  ::madvise(addr, bytes, MADV_DONTNEED);
}

void os::numa_make_global(char *addr, size_t bytes) {
}

void os::numa_make_local(char *addr, size_t bytes, int lgrp_hint) {
}

bool os::numa_topology_changed()   { return false; }

size_t os::numa_get_groups_num() {
  return 1;
}

int os::numa_get_group_id() {
  return 0;
}

size_t os::numa_get_leaf_groups(int *ids, size_t size) {
  if (size > 0) {
    ids[0] = 0;
    return 1;
  }
  return 0;
}

int os::numa_get_group_id_for_address(const void* address) {
  return 0;
}

bool os::get_page_info(char *start, page_info* info) {
  return false;
}

char *os::scan_pages(char *start, char* end, page_info* page_expected, page_info* page_found) {
  return end;
}


bool os::pd_uncommit_memory(char* addr, size_t size, bool exec) {
#if defined(__OpenBSD__)
  // XXX: Work-around mmap/MAP_FIXED bug temporarily on OpenBSD
  Events::log(NULL, "Protecting memory [" INTPTR_FORMAT "," INTPTR_FORMAT "] with PROT_NONE", p2i(addr), p2i(addr+size));
  return ::mprotect(addr, size, PROT_NONE) == 0;
#elif defined(__APPLE__)
  if (exec) {
    if (::madvise(addr, size, MADV_FREE) != 0) {
      return false;
    }
    return ::mprotect(addr, size, PROT_NONE) == 0;
  } else {
    uintptr_t res = (uintptr_t) ::mmap(addr, size, PROT_NONE,
        MAP_PRIVATE|MAP_FIXED|MAP_NORESERVE|MAP_ANONYMOUS, -1, 0);
    return res  != (uintptr_t) MAP_FAILED;
  }
#else
  uintptr_t res = (uintptr_t) ::mmap(addr, size, PROT_NONE,
                                     MAP_PRIVATE|MAP_FIXED|MAP_NORESERVE|MAP_ANONYMOUS, -1, 0);
  return res  != (uintptr_t) MAP_FAILED;
#endif
}

bool os::pd_create_stack_guard_pages(char* addr, size_t size) {
  return os::commit_memory(addr, size, !ExecMem);
}

// If this is a growable mapping, remove the guard pages entirely by
// munmap()ping them.  If not, just call uncommit_memory().
bool os::remove_stack_guard_pages(char* addr, size_t size) {
  return os::uncommit_memory(addr, size);
}

// 'requested_addr' is only treated as a hint, the return value may or
// may not start from the requested address. Unlike Bsd mmap(), this
// function returns NULL to indicate failure.
static char* anon_mmap(char* requested_addr, size_t bytes, bool exec) {
  // MAP_FIXED is intentionally left out, to leave existing mappings intact.
  const int flags = MAP_PRIVATE | MAP_NORESERVE | MAP_ANONYMOUS
      MACOS_ONLY(| (exec ? MAP_JIT : 0));

  // Map reserved/uncommitted pages PROT_NONE so we fail early if we
  // touch an uncommitted page. Otherwise, the read/write might
  // succeed if we have enough swap space to back the physical page.
  char* addr = (char*)::mmap(requested_addr, bytes, PROT_NONE, flags, -1, 0);

  return addr == MAP_FAILED ? NULL : addr;
}

static int anon_munmap(char * addr, size_t size) {
  return ::munmap(addr, size) == 0;
}

char* os::pd_reserve_memory(size_t bytes, bool exec) {
  return anon_mmap(NULL /* addr */, bytes, exec);
}

bool os::pd_release_memory(char* addr, size_t size) {
  return anon_munmap(addr, size);
}

static bool bsd_mprotect(char* addr, size_t size, int prot) {
  // Bsd wants the mprotect address argument to be page aligned.
  char* bottom = (char*)align_down((intptr_t)addr, os::Bsd::page_size());

  // According to SUSv3, mprotect() should only be used with mappings
  // established by mmap(), and mmap() always maps whole pages. Unaligned
  // 'addr' likely indicates problem in the VM (e.g. trying to change
  // protection of malloc'ed or statically allocated memory). Check the
  // caller if you hit this assert.
  assert(addr == bottom, "sanity check");

  size = align_up(pointer_delta(addr, bottom, 1) + size, os::Bsd::page_size());
  Events::log(NULL, "Protecting memory [" INTPTR_FORMAT "," INTPTR_FORMAT "] with protection modes %x", p2i(bottom), p2i(bottom+size), prot);
  return ::mprotect(bottom, size, prot) == 0;
}

// Set protections specified
bool os::protect_memory(char* addr, size_t bytes, ProtType prot,
                        bool is_committed) {
  unsigned int p = 0;
  switch (prot) {
  case MEM_PROT_NONE: p = PROT_NONE; break;
  case MEM_PROT_READ: p = PROT_READ; break;
  case MEM_PROT_RW:   p = PROT_READ|PROT_WRITE; break;
  case MEM_PROT_RWX:  p = PROT_READ|PROT_WRITE|PROT_EXEC; break;
  default:
    ShouldNotReachHere();
  }
  // is_committed is unused.
  return bsd_mprotect(addr, bytes, p);
}

bool os::guard_memory(char* addr, size_t size) {
  return bsd_mprotect(addr, size, PROT_NONE);
}

bool os::unguard_memory(char* addr, size_t size) {
  return bsd_mprotect(addr, size, PROT_READ|PROT_WRITE);
}

bool os::Bsd::hugetlbfs_sanity_check(bool warn, size_t page_size) {
  return false;
}

// Large page support

static size_t _large_page_size = 0;

void os::large_page_init() {
}


char* os::pd_reserve_memory_special(size_t bytes, size_t alignment, size_t page_size, char* req_addr, bool exec) {
  fatal("os::reserve_memory_special should not be called on BSD.");
  return NULL;
}

bool os::pd_release_memory_special(char* base, size_t bytes) {
  fatal("os::release_memory_special should not be called on BSD.");
  return false;
}

size_t os::large_page_size() {
  return _large_page_size;
}

bool os::can_commit_large_page_memory() {
  // Does not matter, we do not support huge pages.
  return false;
}

bool os::can_execute_large_page_memory() {
  // Does not matter, we do not support huge pages.
  return false;
}

char* os::pd_attempt_map_memory_to_file_at(char* requested_addr, size_t bytes, int file_desc) {
  assert(file_desc >= 0, "file_desc is not valid");
  char* result = pd_attempt_reserve_memory_at(requested_addr, bytes, !ExecMem);
  if (result != NULL) {
    if (replace_existing_mapping_with_file_mapping(result, bytes, file_desc) == NULL) {
      vm_exit_during_initialization(err_msg("Error in mapping Java heap at the given filesystem directory"));
    }
  }
  return result;
}

// Reserve memory at an arbitrary address, only if that area is
// available (and not reserved for something else).

char* os::pd_attempt_reserve_memory_at(char* requested_addr, size_t bytes, bool exec) {
  // Assert only that the size is a multiple of the page size, since
  // that's all that mmap requires, and since that's all we really know
  // about at this low abstraction level.  If we need higher alignment,
  // we can either pass an alignment to this method or verify alignment
  // in one of the methods further up the call chain.  See bug 5044738.
  assert(bytes % os::vm_page_size() == 0, "reserving unexpected size block");

  // Repeatedly allocate blocks until the block is allocated at the
  // right spot.

  // Bsd mmap allows caller to pass an address as hint; give it a try first,
  // if kernel honors the hint then we can return immediately.
  char * addr = anon_mmap(requested_addr, bytes, exec);
  if (addr == requested_addr) {
    return requested_addr;
  }

  if (addr != NULL) {
    // mmap() is successful but it fails to reserve at the requested address
    anon_munmap(addr, bytes);
  }

  return NULL;
}

// Sleep forever; naked call to OS-specific sleep; use with CAUTION
void os::infinite_sleep() {
  while (true) {    // sleep forever ...
    ::sleep(100);   // ... 100 seconds at a time
  }
}

// Used to convert frequent JVM_Yield() to nops
bool os::dont_yield() {
  return DontYieldALot;
}

void os::naked_yield() {
  sched_yield();
}

////////////////////////////////////////////////////////////////////////////////
// thread priority support

// Note: Normal Bsd applications are run with SCHED_OTHER policy. SCHED_OTHER
// only supports dynamic priority, static priority must be zero. For real-time
// applications, Bsd supports SCHED_RR which allows static priority (1-99).
// However, for large multi-threaded applications, SCHED_RR is not only slower
// than SCHED_OTHER, but also very unstable (my volano tests hang hard 4 out
// of 5 runs - Sep 2005).
//
// The following code actually changes the niceness of kernel-thread/LWP. It
// has an assumption that setpriority() only modifies one kernel-thread/LWP,
// not the entire user process, and user level threads are 1:1 mapped to kernel
// threads. It has always been the case, but could change in the future. For
// this reason, the code should not be used as default (ThreadPriorityPolicy=0).
// It is only used when ThreadPriorityPolicy=1 and may require system level permission
// (e.g., root privilege or CAP_SYS_NICE capability).

#if !defined(__APPLE__)
int os::java_to_os_priority[CriticalPriority + 1] = {
  19,              // 0 Entry should never be used

   0,              // 1 MinPriority
   3,              // 2
   6,              // 3

  10,              // 4
  15,              // 5 NormPriority
  18,              // 6

  21,              // 7
  25,              // 8
  28,              // 9 NearMaxPriority

  31,              // 10 MaxPriority

  31               // 11 CriticalPriority
};
#else
// Using Mach high-level priority assignments
int os::java_to_os_priority[CriticalPriority + 1] = {
   0,              // 0 Entry should never be used (MINPRI_USER)

  27,              // 1 MinPriority
  28,              // 2
  29,              // 3

  30,              // 4
  31,              // 5 NormPriority (BASEPRI_DEFAULT)
  32,              // 6

  33,              // 7
  34,              // 8
  35,              // 9 NearMaxPriority

  36,              // 10 MaxPriority

  36               // 11 CriticalPriority
};
#endif

static int prio_init() {
  if (ThreadPriorityPolicy == 1) {
    if (geteuid() != 0) {
      if (!FLAG_IS_DEFAULT(ThreadPriorityPolicy) && !FLAG_IS_JIMAGE_RESOURCE(ThreadPriorityPolicy)) {
        warning("-XX:ThreadPriorityPolicy=1 may require system level permission, " \
                "e.g., being the root user. If the necessary permission is not " \
                "possessed, changes to priority will be silently ignored.");
      }
    }
  }
  if (UseCriticalJavaThreadPriority) {
    os::java_to_os_priority[MaxPriority] = os::java_to_os_priority[CriticalPriority];
  }
  return 0;
}

OSReturn os::set_native_priority(Thread* thread, int newpri) {
  if (!UseThreadPriorities || ThreadPriorityPolicy == 0) return OS_OK;

#ifdef __OpenBSD__
  // OpenBSD pthread_setprio starves low priority threads
  return OS_OK;
#elif defined(__FreeBSD__)
  int ret = pthread_setprio(thread->osthread()->pthread_id(), newpri);
  return (ret == 0) ? OS_OK : OS_ERR;
#elif defined(__APPLE__) || defined(__NetBSD__)
  struct sched_param sp;
  int policy;

  if (pthread_getschedparam(thread->osthread()->pthread_id(), &policy, &sp) != 0) {
    return OS_ERR;
  }

  sp.sched_priority = newpri;
  if (pthread_setschedparam(thread->osthread()->pthread_id(), policy, &sp) != 0) {
    return OS_ERR;
  }

  return OS_OK;
#else
  int ret = setpriority(PRIO_PROCESS, thread->osthread()->thread_id(), newpri);
  return (ret == 0) ? OS_OK : OS_ERR;
#endif
}

OSReturn os::get_native_priority(const Thread* const thread, int *priority_ptr) {
  if (!UseThreadPriorities || ThreadPriorityPolicy == 0) {
    *priority_ptr = java_to_os_priority[NormPriority];
    return OS_OK;
  }

  errno = 0;
#if defined(__OpenBSD__) || defined(__FreeBSD__)
  *priority_ptr = pthread_getprio(thread->osthread()->pthread_id());
#elif defined(__APPLE__) || defined(__NetBSD__)
  int policy;
  struct sched_param sp;

  int res = pthread_getschedparam(thread->osthread()->pthread_id(), &policy, &sp);
  if (res != 0) {
    *priority_ptr = -1;
    return OS_ERR;
  } else {
    *priority_ptr = sp.sched_priority;
    return OS_OK;
  }
#else
  *priority_ptr = getpriority(PRIO_PROCESS, thread->osthread()->thread_id());
#endif
  return (*priority_ptr != -1 || errno == 0 ? OS_OK : OS_ERR);
}

extern void report_error(char* file_name, int line_no, char* title,
                         char* format, ...);

// this is called _before_ the most of global arguments have been parsed
void os::init(void) {
  char dummy;   // used to get a guess on initial stack address

  clock_tics_per_sec = CLK_TCK;

  Bsd::set_page_size(getpagesize());
  if (Bsd::page_size() == -1) {
    fatal("os_bsd.cpp: os::init: sysconf failed (%s)", os::strerror(errno));
  }
  _page_sizes.add(Bsd::page_size());

  Bsd::initialize_system_info();

  // _main_thread points to the thread that created/loaded the JVM.
  Bsd::_main_thread = pthread_self();

  Bsd::clock_init();
  initial_time_count = javaTimeNanos();

  os::Posix::init();
}

// To install functions for atexit system call
extern "C" {
  static void perfMemory_exit_helper() {
    perfMemory_exit();
  }
}

// this is called _after_ the global arguments have been parsed
jint os::init_2(void) {

  // This could be set after os::Posix::init() but all platforms
  // have to set it the same so we have to mirror Solaris.
  DEBUG_ONLY(os::set_mutex_init_done();)

  os::Posix::init_2();

  if (PosixSignals::init() == JNI_ERR) {
    return JNI_ERR;
  }

  // Check and sets minimum stack sizes against command line options
  if (Posix::set_minimum_stack_sizes() == JNI_ERR) {
    return JNI_ERR;
  }

  // Not supported.
  FLAG_SET_ERGO(UseNUMA, false);
  FLAG_SET_ERGO(UseNUMAInterleaving, false);

  if (MaxFDLimit) {
    // set the number of file descriptors to max. print out error
    // if getrlimit/setrlimit fails but continue regardless.
    struct rlimit nbr_files;
    int status = getrlimit(RLIMIT_NOFILE, &nbr_files);
    if (status != 0) {
      log_info(os)("os::init_2 getrlimit failed: %s", os::strerror(errno));
    } else {
      nbr_files.rlim_cur = nbr_files.rlim_max;

#ifdef __APPLE__
      // Darwin returns RLIM_INFINITY for rlim_max, but fails with EINVAL if
      // you attempt to use RLIM_INFINITY. As per setrlimit(2), OPEN_MAX must
      // be used instead
      nbr_files.rlim_cur = MIN(OPEN_MAX, nbr_files.rlim_cur);
#endif

      status = setrlimit(RLIMIT_NOFILE, &nbr_files);
      if (status != 0) {
        log_info(os)("os::init_2 setrlimit failed: %s", os::strerror(errno));
      }
    }
  }

  // at-exit methods are called in the reverse order of their registration.
  // atexit functions are called on return from main or as a result of a
  // call to exit(3C). There can be only 32 of these functions registered
  // and atexit() does not set errno.

  if (PerfAllowAtExitRegistration) {
    // only register atexit functions if PerfAllowAtExitRegistration is set.
    // atexit functions can be delayed until process exit time, which
    // can be problematic for embedded VM situations. Embedded VMs should
    // call DestroyJavaVM() to assure that VM resources are released.

    // note: perfMemory_exit_helper atexit function may be removed in
    // the future if the appropriate cleanup code can be added to the
    // VM_Exit VMOperation's doit method.
    if (atexit(perfMemory_exit_helper) != 0) {
      warning("os::init_2 atexit(perfMemory_exit_helper) failed");
    }
  }

  // initialize thread priority policy
  prio_init();

#ifdef __APPLE__
  // dynamically link to objective c gc registration
  void *handleLibObjc = dlopen(OBJC_LIB, RTLD_LAZY);
  if (handleLibObjc != NULL) {
    objc_registerThreadWithCollectorFunction = (objc_registerThreadWithCollector_t) dlsym(handleLibObjc, OBJC_GCREGISTER);
  }
#endif

  return JNI_OK;
}

int os::active_processor_count() {
  // User has overridden the number of active processors
  if (ActiveProcessorCount > 0) {
    log_trace(os)("active_processor_count: "
                  "active processor count set by user : %d",
                  ActiveProcessorCount);
    return ActiveProcessorCount;
  }

  return _processor_count;
}

uint os::processor_id() {
#if defined(__APPLE__) && defined(__x86_64__)
  // Get the initial APIC id and return the associated processor id. The initial APIC
  // id is limited to 8-bits, which means we can have at most 256 unique APIC ids. If
  // the system has more processors (or the initial APIC ids are discontiguous) the
  // APIC id will be truncated and more than one processor will potentially share the
  // same processor id. This is not optimal, but unlikely to happen in practice. Should
  // this become a real problem we could switch to using x2APIC ids, which are 32-bit
  // wide. However, note that x2APIC is Intel-specific, and the wider number space
  // would require a more complicated mapping approach.
  uint eax = 0x1;
  uint ebx;
  uint ecx = 0;
  uint edx;

  __asm__ ("cpuid\n\t" : "+a" (eax), "+b" (ebx), "+c" (ecx), "+d" (edx) : );

  uint apic_id = (ebx >> 24) & (processor_id_map_size - 1);
  int processor_id = Atomic::load(&processor_id_map[apic_id]);

  while (processor_id < 0) {
    // Assign processor id to APIC id
    processor_id = Atomic::cmpxchg(&processor_id_map[apic_id], processor_id_unassigned, processor_id_assigning);
    if (processor_id == processor_id_unassigned) {
      processor_id = Atomic::fetch_and_add(&processor_id_next, 1) % os::processor_count();
      Atomic::store(&processor_id_map[apic_id], processor_id);
    }
  }

  assert(processor_id >= 0 && processor_id < os::processor_count(), "invalid processor id");

  return (uint)processor_id;
#else // defined(__APPLE__) && defined(__x86_64__)
  // Return 0 until we find a good way to get the current processor id on
  // the platform. Returning 0 is safe, since there is always at least one
  // processor, but might not be optimal for performance in some cases.
  return 0;
#endif
}

void os::set_native_thread_name(const char *name) {
#if defined(__APPLE__) && MAC_OS_X_VERSION_MIN_REQUIRED > MAC_OS_X_VERSION_10_5
  // This is only supported in Snow Leopard and beyond
  if (name != NULL) {
    // Add a "Java: " prefix to the name
    char buf[MAXTHREADNAMESIZE];
    snprintf(buf, sizeof(buf), "Java: %s", name);
    pthread_setname_np(buf);
  }
#endif
}

////////////////////////////////////////////////////////////////////////////////
// debug support

bool os::find(address addr, outputStream* st) {
  Dl_info dlinfo;
  memset(&dlinfo, 0, sizeof(dlinfo));
  if (dladdr(addr, &dlinfo) != 0) {
    st->print(INTPTR_FORMAT ": ", (intptr_t)addr);
    if (dlinfo.dli_sname != NULL && dlinfo.dli_saddr != NULL) {
      st->print("%s+%#x", dlinfo.dli_sname,
                (uint)((uintptr_t)addr - (uintptr_t)dlinfo.dli_saddr));
    } else if (dlinfo.dli_fbase != NULL) {
      st->print("<offset %#x>", (uint)((uintptr_t)addr - (uintptr_t)dlinfo.dli_fbase));
    } else {
      st->print("<absolute address>");
    }
    if (dlinfo.dli_fname != NULL) {
      st->print(" in %s", dlinfo.dli_fname);
    }
    if (dlinfo.dli_fbase != NULL) {
      st->print(" at " INTPTR_FORMAT, (intptr_t)dlinfo.dli_fbase);
    }
    st->cr();

    if (Verbose) {
      // decode some bytes around the PC
      address begin = clamp_address_in_page(addr-40, addr, os::vm_page_size());
      address end   = clamp_address_in_page(addr+40, addr, os::vm_page_size());
      address       lowest = (address) dlinfo.dli_sname;
      if (!lowest)  lowest = (address) dlinfo.dli_fbase;
      if (begin < lowest)  begin = lowest;
      Dl_info dlinfo2;
      if (dladdr(end, &dlinfo2) != 0 && dlinfo2.dli_saddr != dlinfo.dli_saddr
          && end > dlinfo2.dli_saddr && dlinfo2.dli_saddr > begin) {
        end = (address) dlinfo2.dli_saddr;
      }
      Disassembler::decode(begin, end, st);
    }
    return true;
  }
  return false;
}

////////////////////////////////////////////////////////////////////////////////
// misc

// This does not do anything on Bsd. This is basically a hook for being
// able to use structured exception handling (thread-local exception filters)
// on, e.g., Win32.
void os::os_exception_wrapper(java_call_t f, JavaValue* value,
                              const methodHandle& method, JavaCallArguments* args,
                              JavaThread* thread) {
  f(value, method, args, thread);
}

void os::print_statistics() {
}

bool os::message_box(const char* title, const char* message) {
  int i;
  fdStream err(defaultStream::error_fd());
  for (i = 0; i < 78; i++) err.print_raw("=");
  err.cr();
  err.print_raw_cr(title);
  for (i = 0; i < 78; i++) err.print_raw("-");
  err.cr();
  err.print_raw_cr(message);
  for (i = 0; i < 78; i++) err.print_raw("=");
  err.cr();

  char buf[16];
  // Prevent process from exiting upon "read error" without consuming all CPU
  while (::read(0, buf, sizeof(buf)) <= 0) { ::sleep(100); }

  return buf[0] == 'y' || buf[0] == 'Y';
}

static inline struct timespec get_mtime(const char* filename) {
  struct stat st;
  int ret = os::stat(filename, &st);
  assert(ret == 0, "failed to stat() file '%s': %s", filename, os::strerror(errno));
#ifdef __APPLE__
  return st.st_mtimespec;
#else
  return st.st_mtim;
#endif
}

int os::compare_file_modified_times(const char* file1, const char* file2) {
  struct timespec filetime1 = get_mtime(file1);
  struct timespec filetime2 = get_mtime(file2);
  int diff = filetime1.tv_sec - filetime2.tv_sec;
  if (diff == 0) {
    return filetime1.tv_nsec - filetime2.tv_nsec;
  }
  return diff;
}

// Is a (classpath) directory empty?
bool os::dir_is_empty(const char* path) {
  DIR *dir = NULL;
  struct dirent *ptr;

  dir = opendir(path);
  if (dir == NULL) return true;

  // Scan the directory
  bool result = true;
  while (result && (ptr = readdir(dir)) != NULL) {
    if (strcmp(ptr->d_name, ".") != 0 && strcmp(ptr->d_name, "..") != 0) {
      result = false;
    }
  }
  closedir(dir);
  return result;
}

// This code originates from JDK's sysOpen and open64_w
// from src/solaris/hpi/src/system_md.c

int os::open(const char *path, int oflag, int mode) {
  if (strlen(path) > MAX_PATH - 1) {
    errno = ENAMETOOLONG;
    return -1;
  }
  int fd;

  fd = ::open(path, oflag, mode);
  if (fd == -1) return -1;

  // If the open succeeded, the file might still be a directory
  {
    struct stat buf;
    int ret = ::fstat(fd, &buf);
    int st_mode = buf.st_mode;

    if (ret != -1) {
      if ((st_mode & S_IFMT) == S_IFDIR) {
        errno = EISDIR;
        ::close(fd);
        return -1;
      }
    } else {
      ::close(fd);
      return -1;
    }
  }

  // All file descriptors that are opened in the JVM and not
  // specifically destined for a subprocess should have the
  // close-on-exec flag set.  If we don't set it, then careless 3rd
  // party native code might fork and exec without closing all
  // appropriate file descriptors (e.g. as we do in closeDescriptors in
  // UNIXProcess.c), and this in turn might:
  //
  // - cause end-of-file to fail to be detected on some file
  //   descriptors, resulting in mysterious hangs, or
  //
  // - might cause an fopen in the subprocess to fail on a system
  //   suffering from bug 1085341.
  //
  // (Yes, the default setting of the close-on-exec flag is a Unix
  // design flaw)
  //
  // See:
  // 1085341: 32-bit stdio routines should support file descriptors >255
  // 4843136: (process) pipe file descriptor from Runtime.exec not being closed
  // 6339493: (process) Runtime.exec does not close all file descriptors on Solaris 9
  //
#ifdef FD_CLOEXEC
  {
    int flags = ::fcntl(fd, F_GETFD);
    if (flags != -1) {
      ::fcntl(fd, F_SETFD, flags | FD_CLOEXEC);
    }
  }
#endif

  return fd;
}


// create binary file, rewriting existing file if required
int os::create_binary_file(const char* path, bool rewrite_existing) {
  int oflags = O_WRONLY | O_CREAT;
  oflags |= rewrite_existing ? O_TRUNC : O_EXCL;
  return ::open(path, oflags, S_IREAD | S_IWRITE);
}

// return current position of file pointer
jlong os::current_file_offset(int fd) {
  return (jlong)::lseek(fd, (off_t)0, SEEK_CUR);
}

// move file pointer to the specified offset
jlong os::seek_to_file_offset(int fd, jlong offset) {
  return (jlong)::lseek(fd, (off_t)offset, SEEK_SET);
}

// This code originates from JDK's sysAvailable
// from src/solaris/hpi/src/native_threads/src/sys_api_td.c

int os::available(int fd, jlong *bytes) {
  jlong cur, end;
  int mode;
  struct stat buf;

  if (::fstat(fd, &buf) >= 0) {
    mode = buf.st_mode;
    if (S_ISCHR(mode) || S_ISFIFO(mode) || S_ISSOCK(mode)) {
      int n;
      if (::ioctl(fd, FIONREAD, &n) >= 0) {
        *bytes = n;
        return 1;
      }
    }
  }
  if ((cur = ::lseek(fd, 0L, SEEK_CUR)) == -1) {
    return 0;
  } else if ((end = ::lseek(fd, 0L, SEEK_END)) == -1) {
    return 0;
  } else if (::lseek(fd, cur, SEEK_SET) == -1) {
    return 0;
  }
  *bytes = end - cur;
  return 1;
}

// Map a block of memory.
char* os::pd_map_memory(int fd, const char* file_name, size_t file_offset,
                        char *addr, size_t bytes, bool read_only,
                        bool allow_exec) {
  int prot;
  int flags;

  if (read_only) {
    prot = PROT_READ;
    flags = MAP_SHARED;
  } else {
    prot = PROT_READ | PROT_WRITE;
    flags = MAP_PRIVATE;
  }

  if (allow_exec) {
    prot |= PROT_EXEC;
  }

  if (addr != NULL) {
    flags |= MAP_FIXED;
  }

  char* mapped_address = (char*)mmap(addr, (size_t)bytes, prot, flags,
                                     fd, file_offset);
  if (mapped_address == MAP_FAILED) {
    return NULL;
  }
  return mapped_address;
}


// Remap a block of memory.
char* os::pd_remap_memory(int fd, const char* file_name, size_t file_offset,
                          char *addr, size_t bytes, bool read_only,
                          bool allow_exec) {
  // same as map_memory() on this OS
  return os::map_memory(fd, file_name, file_offset, addr, bytes, read_only,
                        allow_exec);
}


// Unmap a block of memory.
bool os::pd_unmap_memory(char* addr, size_t bytes) {
  return munmap(addr, bytes) == 0;
}

// current_thread_cpu_time(bool) and thread_cpu_time(Thread*, bool)
// are used by JVM M&M and JVMTI to get user+sys or user CPU time
// of a thread.
//
// current_thread_cpu_time() and thread_cpu_time(Thread*) returns
// the fast estimate available on the platform.

jlong os::current_thread_cpu_time() {
#ifdef __APPLE__
  return os::thread_cpu_time(Thread::current(), true /* user + sys */);
#else
  Unimplemented();
  return 0;
#endif
}

jlong os::thread_cpu_time(Thread* thread) {
#ifdef __APPLE__
  return os::thread_cpu_time(thread, true /* user + sys */);
#else
  Unimplemented();
  return 0;
#endif
}

jlong os::current_thread_cpu_time(bool user_sys_cpu_time) {
#ifdef __APPLE__
  return os::thread_cpu_time(Thread::current(), user_sys_cpu_time);
#else
  Unimplemented();
  return 0;
#endif
}

jlong os::thread_cpu_time(Thread *thread, bool user_sys_cpu_time) {
#ifdef __APPLE__
  struct thread_basic_info tinfo;
  mach_msg_type_number_t tcount = THREAD_INFO_MAX;
  kern_return_t kr;
  thread_t mach_thread;

  mach_thread = thread->osthread()->thread_id();
  kr = thread_info(mach_thread, THREAD_BASIC_INFO, (thread_info_t)&tinfo, &tcount);
  if (kr != KERN_SUCCESS) {
    return -1;
  }

  if (user_sys_cpu_time) {
    jlong nanos;
    nanos = ((jlong) tinfo.system_time.seconds + tinfo.user_time.seconds) * (jlong)1000000000;
    nanos += ((jlong) tinfo.system_time.microseconds + (jlong) tinfo.user_time.microseconds) * (jlong)1000;
    return nanos;
  } else {
    return ((jlong)tinfo.user_time.seconds * 1000000000) + ((jlong)tinfo.user_time.microseconds * (jlong)1000);
  }
#else
  Unimplemented();
  return 0;
#endif
}


void os::current_thread_cpu_time_info(jvmtiTimerInfo *info_ptr) {
  info_ptr->max_value = ALL_64_BITS;       // will not wrap in less than 64 bits
  info_ptr->may_skip_backward = false;     // elapsed time not wall time
  info_ptr->may_skip_forward = false;      // elapsed time not wall time
  info_ptr->kind = JVMTI_TIMER_TOTAL_CPU;  // user+system time is returned
}

void os::thread_cpu_time_info(jvmtiTimerInfo *info_ptr) {
  info_ptr->max_value = ALL_64_BITS;       // will not wrap in less than 64 bits
  info_ptr->may_skip_backward = false;     // elapsed time not wall time
  info_ptr->may_skip_forward = false;      // elapsed time not wall time
  info_ptr->kind = JVMTI_TIMER_TOTAL_CPU;  // user+system time is returned
}

bool os::is_thread_cpu_time_supported() {
#ifdef __APPLE__
  return true;
#else
  return false;
#endif
}

// System loadavg support.  Returns -1 if load average cannot be obtained.
// Bsd doesn't yet have a (official) notion of processor sets,
// so just return the system wide load average.
int os::loadavg(double loadavg[], int nelem) {
  return ::getloadavg(loadavg, nelem);
}

void os::pause() {
  char filename[MAX_PATH];
  if (PauseAtStartupFile && PauseAtStartupFile[0]) {
    jio_snprintf(filename, MAX_PATH, "%s", PauseAtStartupFile);
  } else {
    jio_snprintf(filename, MAX_PATH, "./vm.paused.%d", current_process_id());
  }

  int fd = ::open(filename, O_WRONLY | O_CREAT | O_TRUNC, 0666);
  if (fd != -1) {
    struct stat buf;
    ::close(fd);
    while (::stat(filename, &buf) == 0) {
      (void)::poll(NULL, 0, 100);
    }
  } else {
    jio_fprintf(stderr,
                "Could not open pause file '%s', continuing immediately.\n", filename);
  }
}

// Get the kern.corefile setting, or otherwise the default path to the core file
// Returns the length of the string
int os::get_core_path(char* buffer, size_t bufferSize) {
  int n = 0;
#ifdef __APPLE__
  char coreinfo[MAX_PATH];
  size_t sz = sizeof(coreinfo);
  int ret = sysctlbyname("kern.corefile", coreinfo, &sz, NULL, 0);
  if (ret == 0) {
    char *pid_pos = strstr(coreinfo, "%P");
    // skip over the "%P" to preserve any optional custom user pattern
    const char* tail = (pid_pos != NULL) ? (pid_pos + 2) : "";

    if (pid_pos != NULL) {
      *pid_pos = '\0';
      n = jio_snprintf(buffer, bufferSize, "%s%d%s", coreinfo, os::current_process_id(), tail);
    } else {
      n = jio_snprintf(buffer, bufferSize, "%s", coreinfo);
    }
  } else
#endif
  {
    n = jio_snprintf(buffer, bufferSize, "/cores/core.%d", os::current_process_id());
  }
  // Truncate if theoretical string was longer than bufferSize
  n = MIN2(n, (int)bufferSize);

  return n;
}

bool os::supports_map_sync() {
  return false;
}

bool os::start_debugging(char *buf, int buflen) {
  int len = (int)strlen(buf);
  char *p = &buf[len];

  jio_snprintf(p, buflen-len,
             "\n\n"
             "Do you want to debug the problem?\n\n"
             "To debug, run 'gdb /proc/%d/exe %d'; then switch to thread " INTX_FORMAT " (" INTPTR_FORMAT ")\n"
             "Enter 'yes' to launch gdb automatically (PATH must include gdb)\n"
             "Otherwise, press RETURN to abort...",
             os::current_process_id(), os::current_process_id(),
             os::current_thread_id(), os::current_thread_id());

  bool yes = os::message_box("Unexpected Error", buf);

  if (yes) {
    // yes, user asked VM to launch debugger
    jio_snprintf(buf, sizeof(buf), "gdb /proc/%d/exe %d",
                     os::current_process_id(), os::current_process_id());

    os::fork_and_exec(buf);
    yes = false;
  }
  return yes;
}

void os::print_memory_mappings(char* addr, size_t bytes, outputStream* st) {}
