/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2020 SAP SE. All rights reserved.
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

// According to the AIX OS doc #pragma alloca must be used
// with C++ compiler before referencing the function alloca()
#pragma alloca

// no precompiled headers
#include "jvm.h"
#include "classfile/vmSymbols.hpp"
#include "code/icBuffer.hpp"
#include "code/vtableStubs.hpp"
#include "compiler/compileBroker.hpp"
#include "interpreter/interpreter.hpp"
#include "jvmtifiles/jvmti.h"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "libo4.hpp"
#include "libperfstat_aix.hpp"
#include "libodm_aix.hpp"
#include "loadlib_aix.hpp"
#include "memory/allocation.inline.hpp"
#include "misc_aix.hpp"
#include "oops/oop.inline.hpp"
#include "os_aix.inline.hpp"
#include "os_share_aix.hpp"
#include "porting_aix.hpp"
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
#include "runtime/os.hpp"
#include "runtime/osThread.hpp"
#include "runtime/perfMemory.hpp"
#include "runtime/safefetch.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/statSampler.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/threadCritical.hpp"
#include "runtime/timer.hpp"
#include "runtime/vm_version.hpp"
#include "services/attachListener.hpp"
#include "services/runtimeService.hpp"
#include "signals_posix.hpp"
#include "utilities/align.hpp"
#include "utilities/decoder.hpp"
#include "utilities/defaultStream.hpp"
#include "utilities/events.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/vmError.hpp"

// put OS-includes here (sorted alphabetically)
#include <errno.h>
#include <fcntl.h>
#include <inttypes.h>
#include <poll.h>
#include <procinfo.h>
#include <pthread.h>
#include <pwd.h>
#include <semaphore.h>
#include <signal.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <sys/ipc.h>
#include <sys/mman.h>
#include <sys/resource.h>
#include <sys/select.h>
#include <sys/shm.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/sysinfo.h>
#include <sys/systemcfg.h>
#include <sys/time.h>
#include <sys/times.h>
#include <sys/types.h>
#include <sys/utsname.h>
#include <sys/vminfo.h>

// Missing prototypes for various system APIs.
extern "C"
int mread_real_time(timebasestruct_t *t, size_t size_of_timebasestruct_t);

#if !defined(_AIXVERSION_610)
extern "C" int getthrds64(pid_t, struct thrdentry64*, int, tid64_t*, int);
extern "C" int getprocs64(procentry64*, int, fdsinfo*, int, pid_t*, int);
extern "C" int getargs(procsinfo*, int, char*, int);
#endif

#define MAX_PATH (2 * K)

// for timer info max values which include all bits
#define ALL_64_BITS CONST64(0xFFFFFFFFFFFFFFFF)
// for multipage initialization error analysis (in 'g_multipage_error')
#define ERROR_MP_OS_TOO_OLD                          100
#define ERROR_MP_EXTSHM_ACTIVE                       101
#define ERROR_MP_VMGETINFO_FAILED                    102
#define ERROR_MP_VMGETINFO_CLAIMS_NO_SUPPORT_FOR_64K 103

// excerpts from systemcfg.h that might be missing on older os levels
#ifndef PV_7
  #define PV_7 0x200000          /* Power PC 7 */
#endif
#ifndef PV_7_Compat
  #define PV_7_Compat 0x208000   /* Power PC 7 */
#endif
#ifndef PV_8
  #define PV_8 0x300000          /* Power PC 8 */
#endif
#ifndef PV_8_Compat
  #define PV_8_Compat 0x308000   /* Power PC 8 */
#endif
#ifndef PV_9
  #define PV_9 0x400000          /* Power PC 9 */
#endif
#ifndef PV_9_Compat
  #define PV_9_Compat  0x408000  /* Power PC 9 */
#endif


static address resolve_function_descriptor_to_code_pointer(address p);

static void vmembk_print_on(outputStream* os);

////////////////////////////////////////////////////////////////////////////////
// global variables (for a description see os_aix.hpp)

julong    os::Aix::_physical_memory = 0;

pthread_t os::Aix::_main_thread = ((pthread_t)0);
int       os::Aix::_page_size = -1;

// -1 = uninitialized, 0 if AIX, 1 if OS/400 pase
int       os::Aix::_on_pase = -1;

// 0 = uninitialized, otherwise 32 bit number:
//  0xVVRRTTSS
//  VV - major version
//  RR - minor version
//  TT - tech level, if known, 0 otherwise
//  SS - service pack, if known, 0 otherwise
uint32_t  os::Aix::_os_version = 0;

// -1 = uninitialized, 0 - no, 1 - yes
int       os::Aix::_xpg_sus_mode = -1;

// -1 = uninitialized, 0 - no, 1 - yes
int       os::Aix::_extshm = -1;

////////////////////////////////////////////////////////////////////////////////
// local variables

static volatile jlong max_real_time = 0;
static jlong    initial_time_count = 0;
static int      clock_tics_per_sec = 100;

// Process break recorded at startup.
static address g_brk_at_startup = NULL;

// This describes the state of multipage support of the underlying
// OS. Note that this is of no interest to the outsize world and
// therefore should not be defined in AIX class.
//
// AIX supports four different page sizes - 4K, 64K, 16MB, 16GB. The
// latter two (16M "large" resp. 16G "huge" pages) require special
// setup and are normally not available.
//
// AIX supports multiple page sizes per process, for:
//  - Stack (of the primordial thread, so not relevant for us)
//  - Data - data, bss, heap, for us also pthread stacks
//  - Text - text code
//  - shared memory
//
// Default page sizes can be set via linker options (-bdatapsize, -bstacksize, ...)
// and via environment variable LDR_CNTRL (DATAPSIZE, STACKPSIZE, ...).
//
// For shared memory, page size can be set dynamically via
// shmctl(). Different shared memory regions can have different page
// sizes.
//
// More information can be found at AIBM info center:
//   http://publib.boulder.ibm.com/infocenter/aix/v6r1/index.jsp?topic=/com.ibm.aix.prftungd/doc/prftungd/multiple_page_size_app_support.htm
//
static struct {
  size_t pagesize;            // sysconf _SC_PAGESIZE (4K)
  size_t datapsize;           // default data page size (LDR_CNTRL DATAPSIZE)
  size_t shmpsize;            // default shared memory page size (LDR_CNTRL SHMPSIZE)
  size_t pthr_stack_pagesize; // stack page size of pthread threads
  size_t textpsize;           // default text page size (LDR_CNTRL STACKPSIZE)
  bool can_use_64K_pages;     // True if we can alloc 64K pages dynamically with Sys V shm.
  bool can_use_16M_pages;     // True if we can alloc 16M pages dynamically with Sys V shm.
  int error;                  // Error describing if something went wrong at multipage init.
} g_multipage_support = {
  (size_t) -1,
  (size_t) -1,
  (size_t) -1,
  (size_t) -1,
  (size_t) -1,
  false, false,
  0
};

// We must not accidentally allocate memory close to the BRK - even if
// that would work - because then we prevent the BRK segment from
// growing which may result in a malloc OOM even though there is
// enough memory. The problem only arises if we shmat() or mmap() at
// a specific wish address, e.g. to place the heap in a
// compressed-oops-friendly way.
static bool is_close_to_brk(address a) {
  assert0(g_brk_at_startup != NULL);
  if (a >= g_brk_at_startup &&
      a < (g_brk_at_startup + MaxExpectedDataSegmentSize)) {
    return true;
  }
  return false;
}

julong os::available_memory() {
  return Aix::available_memory();
}

julong os::Aix::available_memory() {
  // Avoid expensive API call here, as returned value will always be null.
  if (os::Aix::on_pase()) {
    return 0x0LL;
  }
  os::Aix::meminfo_t mi;
  if (os::Aix::get_meminfo(&mi)) {
    return mi.real_free;
  } else {
    return ULONG_MAX;
  }
}

julong os::physical_memory() {
  return Aix::physical_memory();
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

// Helper function, emulates disclaim64 using multiple 32bit disclaims
// because we cannot use disclaim64() on AS/400 and old AIX releases.
static bool my_disclaim64(char* addr, size_t size) {

  if (size == 0) {
    return true;
  }

  // Maximum size 32bit disclaim() accepts. (Theoretically 4GB, but I just do not trust that.)
  const unsigned int maxDisclaimSize = 0x40000000;

  const unsigned int numFullDisclaimsNeeded = (size / maxDisclaimSize);
  const unsigned int lastDisclaimSize = (size % maxDisclaimSize);

  char* p = addr;

  for (int i = 0; i < numFullDisclaimsNeeded; i ++) {
    if (::disclaim(p, maxDisclaimSize, DISCLAIM_ZEROMEM) != 0) {
      trcVerbose("Cannot disclaim %p - %p (errno %d)\n", p, p + maxDisclaimSize, errno);
      return false;
    }
    p += maxDisclaimSize;
  }

  if (lastDisclaimSize > 0) {
    if (::disclaim(p, lastDisclaimSize, DISCLAIM_ZEROMEM) != 0) {
      trcVerbose("Cannot disclaim %p - %p (errno %d)\n", p, p + lastDisclaimSize, errno);
      return false;
    }
  }

  return true;
}

// Cpu architecture string
#if defined(PPC32)
static char cpu_arch[] = "ppc";
#elif defined(PPC64)
static char cpu_arch[] = "ppc64";
#else
#error Add appropriate cpu_arch setting
#endif

// Wrap the function "vmgetinfo" which is not available on older OS releases.
static int checked_vmgetinfo(void *out, int command, int arg) {
  if (os::Aix::on_pase() && os::Aix::os_version_short() < 0x0601) {
    guarantee(false, "cannot call vmgetinfo on AS/400 older than V6R1");
  }
  return ::vmgetinfo(out, command, arg);
}

// Given an address, returns the size of the page backing that address.
size_t os::Aix::query_pagesize(void* addr) {

  if (os::Aix::on_pase() && os::Aix::os_version_short() < 0x0601) {
    // AS/400 older than V6R1: no vmgetinfo here, default to 4K
    return 4*K;
  }

  vm_page_info pi;
  pi.addr = (uint64_t)addr;
  if (checked_vmgetinfo(&pi, VM_PAGE_INFO, sizeof(pi)) == 0) {
    return pi.pagesize;
  } else {
    assert(false, "vmgetinfo failed to retrieve page size");
    return 4*K;
  }
}

void os::Aix::initialize_system_info() {

  // Get the number of online(logical) cpus instead of configured.
  os::_processor_count = sysconf(_SC_NPROCESSORS_ONLN);
  assert(_processor_count > 0, "_processor_count must be > 0");

  // Retrieve total physical storage.
  os::Aix::meminfo_t mi;
  if (!os::Aix::get_meminfo(&mi)) {
    assert(false, "os::Aix::get_meminfo failed.");
  }
  _physical_memory = (julong) mi.real_total;
}

// Helper function for tracing page sizes.
static const char* describe_pagesize(size_t pagesize) {
  switch (pagesize) {
    case 4*K : return "4K";
    case 64*K: return "64K";
    case 16*M: return "16M";
    case 16*G: return "16G";
    default:
      assert(false, "surprise");
      return "??";
  }
}

// Probe OS for multipage support.
// Will fill the global g_multipage_support structure.
// Must be called before calling os::large_page_init().
static void query_multipage_support() {

  guarantee(g_multipage_support.pagesize == -1,
            "do not call twice");

  g_multipage_support.pagesize = ::sysconf(_SC_PAGESIZE);

  // This really would surprise me.
  assert(g_multipage_support.pagesize == 4*K, "surprise!");

  // Query default data page size (default page size for C-Heap, pthread stacks and .bss).
  // Default data page size is defined either by linker options (-bdatapsize)
  // or by environment variable LDR_CNTRL (suboption DATAPSIZE). If none is given,
  // default should be 4K.
  {
    void* p = ::malloc(16*M);
    g_multipage_support.datapsize = os::Aix::query_pagesize(p);
    ::free(p);
  }

  // Query default shm page size (LDR_CNTRL SHMPSIZE).
  // Note that this is pure curiosity. We do not rely on default page size but set
  // our own page size after allocated.
  {
    const int shmid = ::shmget(IPC_PRIVATE, 1, IPC_CREAT | S_IRUSR | S_IWUSR);
    guarantee(shmid != -1, "shmget failed");
    void* p = ::shmat(shmid, NULL, 0);
    ::shmctl(shmid, IPC_RMID, NULL);
    guarantee(p != (void*) -1, "shmat failed");
    g_multipage_support.shmpsize = os::Aix::query_pagesize(p);
    ::shmdt(p);
  }

  // Before querying the stack page size, make sure we are not running as primordial
  // thread (because primordial thread's stack may have different page size than
  // pthread thread stacks). Running a VM on the primordial thread won't work for a
  // number of reasons so we may just as well guarantee it here.
  guarantee0(!os::is_primordial_thread());

  // Query pthread stack page size. Should be the same as data page size because
  // pthread stacks are allocated from C-Heap.
  {
    int dummy = 0;
    g_multipage_support.pthr_stack_pagesize = os::Aix::query_pagesize(&dummy);
  }

  // Query default text page size (LDR_CNTRL TEXTPSIZE).
  {
    address any_function =
      resolve_function_descriptor_to_code_pointer((address)describe_pagesize);
    g_multipage_support.textpsize = os::Aix::query_pagesize(any_function);
  }

  // Now probe for support of 64K pages and 16M pages.

  // Before OS/400 V6R1, there is no support for pages other than 4K.
  if (os::Aix::on_pase_V5R4_or_older()) {
    trcVerbose("OS/400 < V6R1 - no large page support.");
    g_multipage_support.error = ERROR_MP_OS_TOO_OLD;
    goto query_multipage_support_end;
  }

  // Now check which page sizes the OS claims it supports, and of those, which actually can be used.
  {
    const int MAX_PAGE_SIZES = 4;
    psize_t sizes[MAX_PAGE_SIZES];
    const int num_psizes = checked_vmgetinfo(sizes, VMINFO_GETPSIZES, MAX_PAGE_SIZES);
    if (num_psizes == -1) {
      trcVerbose("vmgetinfo(VMINFO_GETPSIZES) failed (errno: %d)", errno);
      trcVerbose("disabling multipage support.");
      g_multipage_support.error = ERROR_MP_VMGETINFO_FAILED;
      goto query_multipage_support_end;
    }
    guarantee(num_psizes > 0, "vmgetinfo(.., VMINFO_GETPSIZES, ...) failed.");
    assert(num_psizes <= MAX_PAGE_SIZES, "Surprise! more than 4 page sizes?");
    trcVerbose("vmgetinfo(.., VMINFO_GETPSIZES, ...) returns %d supported page sizes: ", num_psizes);
    for (int i = 0; i < num_psizes; i ++) {
      trcVerbose(" %s ", describe_pagesize(sizes[i]));
    }

    // Can we use 64K, 16M pages?
    for (int i = 0; i < num_psizes; i ++) {
      const size_t pagesize = sizes[i];
      if (pagesize != 64*K && pagesize != 16*M) {
        continue;
      }
      bool can_use = false;
      trcVerbose("Probing support for %s pages...", describe_pagesize(pagesize));
      const int shmid = ::shmget(IPC_PRIVATE, pagesize,
        IPC_CREAT | S_IRUSR | S_IWUSR);
      guarantee0(shmid != -1); // Should always work.
      // Try to set pagesize.
      struct shmid_ds shm_buf = { 0 };
      shm_buf.shm_pagesize = pagesize;
      if (::shmctl(shmid, SHM_PAGESIZE, &shm_buf) != 0) {
        const int en = errno;
        ::shmctl(shmid, IPC_RMID, NULL); // As early as possible!
        trcVerbose("shmctl(SHM_PAGESIZE) failed with errno=%d", errno);
      } else {
        // Attach and double check pageisze.
        void* p = ::shmat(shmid, NULL, 0);
        ::shmctl(shmid, IPC_RMID, NULL); // As early as possible!
        guarantee0(p != (void*) -1); // Should always work.
        const size_t real_pagesize = os::Aix::query_pagesize(p);
        if (real_pagesize != pagesize) {
          trcVerbose("real page size (" SIZE_FORMAT_HEX ") differs.", real_pagesize);
        } else {
          can_use = true;
        }
        ::shmdt(p);
      }
      trcVerbose("Can use: %s", (can_use ? "yes" : "no"));
      if (pagesize == 64*K) {
        g_multipage_support.can_use_64K_pages = can_use;
      } else if (pagesize == 16*M) {
        g_multipage_support.can_use_16M_pages = can_use;
      }
    }

  } // end: check which pages can be used for shared memory

query_multipage_support_end:

  trcVerbose("base page size (sysconf _SC_PAGESIZE): %s",
      describe_pagesize(g_multipage_support.pagesize));
  trcVerbose("Data page size (C-Heap, bss, etc): %s",
      describe_pagesize(g_multipage_support.datapsize));
  trcVerbose("Text page size: %s",
      describe_pagesize(g_multipage_support.textpsize));
  trcVerbose("Thread stack page size (pthread): %s",
      describe_pagesize(g_multipage_support.pthr_stack_pagesize));
  trcVerbose("Default shared memory page size: %s",
      describe_pagesize(g_multipage_support.shmpsize));
  trcVerbose("Can use 64K pages dynamically with shared memory: %s",
      (g_multipage_support.can_use_64K_pages ? "yes" :"no"));
  trcVerbose("Can use 16M pages dynamically with shared memory: %s",
      (g_multipage_support.can_use_16M_pages ? "yes" :"no"));
  trcVerbose("Multipage error details: %d",
      g_multipage_support.error);

  // sanity checks
  assert0(g_multipage_support.pagesize == 4*K);
  assert0(g_multipage_support.datapsize == 4*K || g_multipage_support.datapsize == 64*K);
  assert0(g_multipage_support.textpsize == 4*K || g_multipage_support.textpsize == 64*K);
  assert0(g_multipage_support.pthr_stack_pagesize == g_multipage_support.datapsize);
  assert0(g_multipage_support.shmpsize == 4*K || g_multipage_support.shmpsize == 64*K);

}

void os::init_system_properties_values() {

#ifndef OVERRIDE_LIBPATH
  #define DEFAULT_LIBPATH "/lib:/usr/lib"
#else
  #define DEFAULT_LIBPATH OVERRIDE_LIBPATH
#endif
#define EXTENSIONS_DIR  "/lib/ext"

  // Buffer that fits several sprintfs.
  // Note that the space for the trailing null is provided
  // by the nulls included by the sizeof operator.
  const size_t bufsize =
    MAX2((size_t)MAXPATHLEN,  // For dll_dir & friends.
         (size_t)MAXPATHLEN + sizeof(EXTENSIONS_DIR)); // extensions dir
  char *buf = NEW_C_HEAP_ARRAY(char, bufsize, mtInternal);

  // sysclasspath, java_home, dll_dir
  {
    char *pslash;
    os::jvm_path(buf, bufsize);

    // Found the full path to libjvm.so.
    // Now cut the path to <java_home>/jre if we can.
    pslash = strrchr(buf, '/');
    if (pslash != NULL) {
      *pslash = '\0';            // Get rid of /libjvm.so.
    }
    pslash = strrchr(buf, '/');
    if (pslash != NULL) {
      *pslash = '\0';            // Get rid of /{client|server|hotspot}.
    }
    Arguments::set_dll_dir(buf);

    if (pslash != NULL) {
      pslash = strrchr(buf, '/');
      if (pslash != NULL) {
        *pslash = '\0';        // Get rid of /lib.
      }
    }
    Arguments::set_java_home(buf);
    if (!set_boot_path('/', ':')) {
      vm_exit_during_initialization("Failed setting boot class path.", NULL);
    }
  }

  // Where to look for native libraries.

  // On Aix we get the user setting of LIBPATH.
  // Eventually, all the library path setting will be done here.
  // Get the user setting of LIBPATH.
  const char *v = ::getenv("LIBPATH");
  const char *v_colon = ":";
  if (v == NULL) { v = ""; v_colon = ""; }

  // Concatenate user and invariant part of ld_library_path.
  // That's +1 for the colon and +1 for the trailing '\0'.
  char *ld_library_path = NEW_C_HEAP_ARRAY(char, strlen(v) + 1 + sizeof(DEFAULT_LIBPATH) + 1, mtInternal);
  sprintf(ld_library_path, "%s%s" DEFAULT_LIBPATH, v, v_colon);
  Arguments::set_library_path(ld_library_path);
  FREE_C_HEAP_ARRAY(char, ld_library_path);

  // Extensions directories.
  sprintf(buf, "%s" EXTENSIONS_DIR, Arguments::get_java_home());
  Arguments::set_ext_dirs(buf);

  FREE_C_HEAP_ARRAY(char, buf);

#undef DEFAULT_LIBPATH
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

// retrieve memory information.
// Returns false if something went wrong;
// content of pmi undefined in this case.
bool os::Aix::get_meminfo(meminfo_t* pmi) {

  assert(pmi, "get_meminfo: invalid parameter");

  memset(pmi, 0, sizeof(meminfo_t));

  if (os::Aix::on_pase()) {
    // On PASE, use the libo4 porting library.

    unsigned long long virt_total = 0;
    unsigned long long real_total = 0;
    unsigned long long real_free = 0;
    unsigned long long pgsp_total = 0;
    unsigned long long pgsp_free = 0;
    if (libo4::get_memory_info(&virt_total, &real_total, &real_free, &pgsp_total, &pgsp_free)) {
      pmi->virt_total = virt_total;
      pmi->real_total = real_total;
      pmi->real_free = real_free;
      pmi->pgsp_total = pgsp_total;
      pmi->pgsp_free = pgsp_free;
      return true;
    }
    return false;

  } else {

    // On AIX, I use the (dynamically loaded) perfstat library to retrieve memory statistics
    // See:
    // http://publib.boulder.ibm.com/infocenter/systems/index.jsp
    //        ?topic=/com.ibm.aix.basetechref/doc/basetrf1/perfstat_memtot.htm
    // http://publib.boulder.ibm.com/infocenter/systems/index.jsp
    //        ?topic=/com.ibm.aix.files/doc/aixfiles/libperfstat.h.htm

    perfstat_memory_total_t psmt;
    memset (&psmt, '\0', sizeof(psmt));
    const int rc = libperfstat::perfstat_memory_total(NULL, &psmt, sizeof(psmt), 1);
    if (rc == -1) {
      trcVerbose("perfstat_memory_total() failed (errno=%d)", errno);
      assert(0, "perfstat_memory_total() failed");
      return false;
    }

    assert(rc == 1, "perfstat_memory_total() - weird return code");

    // excerpt from
    // http://publib.boulder.ibm.com/infocenter/systems/index.jsp
    //        ?topic=/com.ibm.aix.files/doc/aixfiles/libperfstat.h.htm
    // The fields of perfstat_memory_total_t:
    // u_longlong_t virt_total         Total virtual memory (in 4 KB pages).
    // u_longlong_t real_total         Total real memory (in 4 KB pages).
    // u_longlong_t real_free          Free real memory (in 4 KB pages).
    // u_longlong_t pgsp_total         Total paging space (in 4 KB pages).
    // u_longlong_t pgsp_free          Free paging space (in 4 KB pages).

    pmi->virt_total = psmt.virt_total * 4096;
    pmi->real_total = psmt.real_total * 4096;
    pmi->real_free = psmt.real_free * 4096;
    pmi->pgsp_total = psmt.pgsp_total * 4096;
    pmi->pgsp_free = psmt.pgsp_free * 4096;

    return true;

  }
} // end os::Aix::get_meminfo

//////////////////////////////////////////////////////////////////////////////
// create new thread

// Thread start routine for all newly created threads
static void *thread_native_entry(Thread *thread) {

  thread->record_stack_base_and_size();

  const pthread_t pthread_id = ::pthread_self();
  const tid_t kernel_thread_id = ::thread_self();

  LogTarget(Info, os, thread) lt;
  if (lt.is_enabled()) {
    address low_address = thread->stack_end();
    address high_address = thread->stack_base();
    lt.print("Thread is alive (tid: " UINTX_FORMAT ", kernel thread id: " UINTX_FORMAT
             ", stack [" PTR_FORMAT " - " PTR_FORMAT " (" SIZE_FORMAT "k using %uk pages)).",
             os::current_thread_id(), (uintx) kernel_thread_id, low_address, high_address,
             (high_address - low_address) / K, os::Aix::query_pagesize(low_address) / K);
  }

  // Normally, pthread stacks on AIX live in the data segment (are allocated with malloc()
  // by the pthread library). In rare cases, this may not be the case, e.g. when third-party
  // tools hook pthread_create(). In this case, we may run into problems establishing
  // guard pages on those stacks, because the stacks may reside in memory which is not
  // protectable (shmated).
  if (thread->stack_base() > ::sbrk(0)) {
    log_warning(os, thread)("Thread stack not in data segment.");
  }

  // Try to randomize the cache line index of hot stack frames.
  // This helps when threads of the same stack traces evict each other's
  // cache lines. The threads can be either from the same JVM instance, or
  // from different JVM instances. The benefit is especially true for
  // processors with hyperthreading technology.

  static int counter = 0;
  int pid = os::current_process_id();
  alloca(((pid ^ counter++) & 7) * 128);

  thread->initialize_thread_current();

  OSThread* osthread = thread->osthread();

  // Thread_id is pthread id.
  osthread->set_thread_id(pthread_id);

  // .. but keep kernel thread id too for diagnostics
  osthread->set_kernel_thread_id(kernel_thread_id);

  // Initialize signal mask for this thread.
  PosixSignals::hotspot_sigmask(thread);

  // Initialize floating point control register.
  os::Aix::init_thread_fpu_state();

  assert(osthread->get_state() == RUNNABLE, "invalid os thread state");

  // Call one more level start routine.
  thread->call_run();

  // Note: at this point the thread object may already have deleted itself.
  // Prevent dereferencing it from here on out.
  thread = NULL;

  log_info(os, thread)("Thread finished (tid: " UINTX_FORMAT ", kernel thread id: " UINTX_FORMAT ").",
    os::current_thread_id(), (uintx) kernel_thread_id);

  return 0;
}

bool os::create_thread(Thread* thread, ThreadType thr_type,
                       size_t req_stack_size) {

  assert(thread->osthread() == NULL, "caller responsible");

  // Allocate the OSThread object.
  OSThread* osthread = new OSThread(NULL, NULL);
  if (osthread == NULL) {
    return false;
  }

  // Set the correct thread state.
  osthread->set_thread_type(thr_type);

  // Initial state is ALLOCATED but not INITIALIZED
  osthread->set_state(ALLOCATED);

  thread->set_osthread(osthread);

  // Init thread attributes.
  pthread_attr_t attr;
  pthread_attr_init(&attr);
  guarantee(pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED) == 0, "???");

  // Make sure we run in 1:1 kernel-user-thread mode.
  if (os::Aix::on_aix()) {
    guarantee(pthread_attr_setscope(&attr, PTHREAD_SCOPE_SYSTEM) == 0, "???");
    guarantee(pthread_attr_setinheritsched(&attr, PTHREAD_EXPLICIT_SCHED) == 0, "???");
  }

  // Start in suspended state, and in os::thread_start, wake the thread up.
  guarantee(pthread_attr_setsuspendstate_np(&attr, PTHREAD_CREATE_SUSPENDED_NP) == 0, "???");

  // Calculate stack size if it's not specified by caller.
  size_t stack_size = os::Posix::get_initial_stack_size(thr_type, req_stack_size);

  // JDK-8187028: It was observed that on some configurations (4K backed thread stacks)
  // the real thread stack size may be smaller than the requested stack size, by as much as 64K.
  // This very much looks like a pthread lib error. As a workaround, increase the stack size
  // by 64K for small thread stacks (arbitrarily chosen to be < 4MB)
  if (stack_size < 4096 * K) {
    stack_size += 64 * K;
  }

  // On Aix, pthread_attr_setstacksize fails with huge values and leaves the
  // thread size in attr unchanged. If this is the minimal stack size as set
  // by pthread_attr_init this leads to crashes after thread creation. E.g. the
  // guard pages might not fit on the tiny stack created.
  int ret = pthread_attr_setstacksize(&attr, stack_size);
  if (ret != 0) {
    log_warning(os, thread)("The %sthread stack size specified is invalid: " SIZE_FORMAT "k",
                            (thr_type == compiler_thread) ? "compiler " : ((thr_type == java_thread) ? "" : "VM "),
                            stack_size / K);
    thread->set_osthread(NULL);
    delete osthread;
    return false;
  }

  // Save some cycles and a page by disabling OS guard pages where we have our own
  // VM guard pages (in java threads). For other threads, keep system default guard
  // pages in place.
  if (thr_type == java_thread || thr_type == compiler_thread) {
    ret = pthread_attr_setguardsize(&attr, 0);
  }

  ResourceMark rm;
  pthread_t tid = 0;

  if (ret == 0) {
    int limit = 3;
    do {
      ret = pthread_create(&tid, &attr, (void* (*)(void*)) thread_native_entry, thread);
    } while (ret == EAGAIN && limit-- > 0);
  }

  if (ret == 0) {
    char buf[64];
    log_info(os, thread)("Thread \"%s\" started (pthread id: " UINTX_FORMAT ", attributes: %s). ",
                         thread->name(), (uintx) tid, os::Posix::describe_pthread_attr(buf, sizeof(buf), &attr));
  } else {
    char buf[64];
    log_warning(os, thread)("Failed to start thread \"%s\" - pthread_create failed (%d=%s) for attributes: %s.",
                            thread->name(), ret, os::errno_name(ret), os::Posix::describe_pthread_attr(buf, sizeof(buf), &attr));
    // Log some OS information which might explain why creating the thread failed.
    log_info(os, thread)("Number of threads approx. running in the VM: %d", Threads::number_of_threads());
    LogStream st(Log(os, thread)::info());
    os::Posix::print_rlimit_info(&st);
    os::print_memory_info(&st);
  }

  pthread_attr_destroy(&attr);

  if (ret != 0) {
    // Need to clean up stuff we've allocated so far.
    thread->set_osthread(NULL);
    delete osthread;
    return false;
  }

  // OSThread::thread_id is the pthread id.
  osthread->set_thread_id(tid);

  return true;
}

/////////////////////////////////////////////////////////////////////////////
// attach existing thread

// bootstrap the main thread
bool os::create_main_thread(JavaThread* thread) {
  assert(os::Aix::_main_thread == pthread_self(), "should be called inside main thread");
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

  const pthread_t pthread_id = ::pthread_self();
  const tid_t kernel_thread_id = ::thread_self();

  // OSThread::thread_id is the pthread id.
  osthread->set_thread_id(pthread_id);

  // .. but keep kernel thread id too for diagnostics
  osthread->set_kernel_thread_id(kernel_thread_id);

  // initialize floating point control register
  os::Aix::init_thread_fpu_state();

  // Initial thread state is RUNNABLE
  osthread->set_state(RUNNABLE);

  thread->set_osthread(osthread);

  if (UseNUMA) {
    int lgrp_id = os::numa_get_group_id();
    if (lgrp_id != -1) {
      thread->set_lgrp_id(lgrp_id);
    }
  }

  // initialize signal mask for this thread
  // and save the caller's signal mask
  PosixSignals::hotspot_sigmask(thread);

  log_info(os, thread)("Thread attached (tid: " UINTX_FORMAT ", kernel thread id: " UINTX_FORMAT ").",
    os::current_thread_id(), (uintx) kernel_thread_id);

  return true;
}

void os::pd_start_thread(Thread* thread) {
  int status = pthread_continue_np(thread->osthread()->pthread_id());
  assert(status == 0, "thr_continue failed");
}

// Free OS resources related to the OSThread
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
  return ((double)os::elapsed_counter()) / os::elapsed_frequency(); // nanosecond resolution
}

jlong os::elapsed_counter() {
  return javaTimeNanos() - initial_time_count;
}

jlong os::elapsed_frequency() {
  return NANOSECS_PER_SEC; // nanosecond resolution
}

bool os::supports_vtime() { return true; }

double os::elapsedVTime() {
  struct rusage usage;
  int retval = getrusage(RUSAGE_THREAD, &usage);
  if (retval == 0) {
    return usage.ru_utime.tv_sec + usage.ru_stime.tv_sec + (usage.ru_utime.tv_usec + usage.ru_stime.tv_usec) / (1000.0 * 1000);
  } else {
    // better than nothing, but not much
    return elapsedTime();
  }
}

// We use mread_real_time here.
// On AIX: If the CPU has a time register, the result will be RTC_POWER and
// it has to be converted to real time. AIX documentations suggests to do
// this unconditionally, so we do it.
//
// See: https://www.ibm.com/support/knowledgecenter/ssw_aix_61/com.ibm.aix.basetrf2/read_real_time.htm
//
// On PASE: mread_real_time will always return RTC_POWER_PC data, so no
// conversion is necessary. However, mread_real_time will not return
// monotonic results but merely matches read_real_time. So we need a tweak
// to ensure monotonic results.
//
// For PASE no public documentation exists, just word by IBM
jlong os::javaTimeNanos() {
  timebasestruct_t time;
  int rc = mread_real_time(&time, TIMEBASE_SZ);
  if (os::Aix::on_pase()) {
    assert(rc == RTC_POWER, "expected time format RTC_POWER from mread_real_time in PASE");
    jlong now = jlong(time.tb_high) * NANOSECS_PER_SEC + jlong(time.tb_low);
    jlong prev = max_real_time;
    if (now <= prev) {
      return prev;   // same or retrograde time;
    }
    jlong obsv = Atomic::cmpxchg(&max_real_time, prev, now);
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
  } else {
    if (rc != RTC_POWER) {
      rc = time_base_to_time(&time, TIMEBASE_SZ);
      assert(rc != -1, "error calling time_base_to_time()");
    }
    return jlong(time.tb_high) * NANOSECS_PER_SEC + jlong(time.tb_low);
  }
}

void os::javaTimeNanos_info(jvmtiTimerInfo *info_ptr) {
  info_ptr->max_value = ALL_64_BITS;
  // mread_real_time() is monotonic (see 'os::javaTimeNanos()')
  info_ptr->may_skip_backward = false;
  info_ptr->may_skip_forward = false;
  info_ptr->kind = JVMTI_TIMER_ELAPSED;    // elapsed not CPU time
}

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

struct tm* os::localtime_pd(const time_t* clock, struct tm* res) {
  return localtime_r(clock, res);
}

intx os::current_thread_id() {
  return (intx)pthread_self();
}

int os::current_process_id() {
  return getpid();
}

// DLL functions

const char* os::dll_file_extension() { return ".so"; }

// This must be hard coded because it's the system's temporary
// directory not the java application's temp directory, ala java.io.tmpdir.
const char* os::get_temp_directory() { return "/tmp"; }

// Check if addr is inside libjvm.so.
bool os::address_is_in_vm(address addr) {

  // Input could be a real pc or a function pointer literal. The latter
  // would be a function descriptor residing in the data segment of a module.
  loaded_module_t lm;
  if (LoadedLibraries::find_for_text_address(addr, &lm) != NULL) {
    return lm.is_in_vm;
  } else if (LoadedLibraries::find_for_data_address(addr, &lm) != NULL) {
    return lm.is_in_vm;
  } else {
    return false;
  }

}

// Resolve an AIX function descriptor literal to a code pointer.
// If the input is a valid code pointer to a text segment of a loaded module,
//   it is returned unchanged.
// If the input is a valid AIX function descriptor, it is resolved to the
//   code entry point.
// If the input is neither a valid function descriptor nor a valid code pointer,
//   NULL is returned.
static address resolve_function_descriptor_to_code_pointer(address p) {

  if (LoadedLibraries::find_for_text_address(p, NULL) != NULL) {
    // It is a real code pointer.
    return p;
  } else if (LoadedLibraries::find_for_data_address(p, NULL) != NULL) {
    // Pointer to data segment, potential function descriptor.
    address code_entry = (address)(((FunctionDescriptor*)p)->entry());
    if (LoadedLibraries::find_for_text_address(code_entry, NULL) != NULL) {
      // It is a function descriptor.
      return code_entry;
    }
  }

  return NULL;
}

bool os::dll_address_to_function_name(address addr, char *buf,
                                      int buflen, int *offset,
                                      bool demangle) {
  if (offset) {
    *offset = -1;
  }
  // Buf is not optional, but offset is optional.
  assert(buf != NULL, "sanity check");
  buf[0] = '\0';

  // Resolve function ptr literals first.
  addr = resolve_function_descriptor_to_code_pointer(addr);
  if (!addr) {
    return false;
  }

  return AixSymbols::get_function_name(addr, buf, buflen, offset, NULL, demangle);
}

bool os::dll_address_to_library_name(address addr, char* buf,
                                     int buflen, int* offset) {
  if (offset) {
    *offset = -1;
  }
  // Buf is not optional, but offset is optional.
  assert(buf != NULL, "sanity check");
  buf[0] = '\0';

  // Resolve function ptr literals first.
  addr = resolve_function_descriptor_to_code_pointer(addr);
  if (!addr) {
    return false;
  }

  return AixSymbols::get_module_name(addr, buf, buflen);
}

// Loads .dll/.so and in case of error it checks if .dll/.so was built
// for the same architecture as Hotspot is running on.
void *os::dll_load(const char *filename, char *ebuf, int ebuflen) {

  log_info(os)("attempting shared library load of %s", filename);

  if (ebuf && ebuflen > 0) {
    ebuf[0] = '\0';
    ebuf[ebuflen - 1] = '\0';
  }

  if (!filename || strlen(filename) == 0) {
    ::strncpy(ebuf, "dll_load: empty filename specified", ebuflen - 1);
    return NULL;
  }

  // RTLD_LAZY is currently not implemented. The dl is loaded immediately with all its dependants.
  void * result= ::dlopen(filename, RTLD_LAZY);
  if (result != NULL) {
    Events::log(NULL, "Loaded shared library %s", filename);
    // Reload dll cache. Don't do this in signal handling.
    LoadedLibraries::reload();
    log_info(os)("shared library load of %s was successful", filename);
    return result;
  } else {
    // error analysis when dlopen fails
    const char* error_report = ::dlerror();
    if (error_report == NULL) {
      error_report = "dlerror returned no error description";
    }
    if (ebuf != NULL && ebuflen > 0) {
      snprintf(ebuf, ebuflen - 1, "%s, LIBPATH=%s, LD_LIBRARY_PATH=%s : %s",
               filename, ::getenv("LIBPATH"), ::getenv("LD_LIBRARY_PATH"), error_report);
    }
    Events::log(NULL, "Loading shared library %s failed, %s", filename, error_report);
    log_info(os)("shared library load of %s failed, %s", filename, error_report);
  }
  return NULL;
}

void* os::dll_lookup(void* handle, const char* name) {
  void* res = dlsym(handle, name);
  return res;
}

void* os::get_default_process_handle() {
  return (void*)::dlopen(NULL, RTLD_LAZY);
}

void os::print_dll_info(outputStream *st) {
  st->print_cr("Dynamic libraries:");
  LoadedLibraries::print(st);
}

void os::get_summary_os_info(char* buf, size_t buflen) {
  // There might be something more readable than uname results for AIX.
  struct utsname name;
  uname(&name);
  snprintf(buf, buflen, "%s %s", name.release, name.version);
}

int os::get_loaded_modules_info(os::LoadedModulesCallbackFunc callback, void *param) {
  // Not yet implemented.
  return 0;
}

void os::print_os_info_brief(outputStream* st) {
  uint32_t ver = os::Aix::os_version();
  st->print_cr("AIX kernel version %u.%u.%u.%u",
               (ver >> 24) & 0xFF, (ver >> 16) & 0xFF, (ver >> 8) & 0xFF, ver & 0xFF);

  os::Posix::print_uname_info(st);

  // Linux uses print_libversion_info(st); here.
}

void os::print_os_info(outputStream* st) {
  st->print_cr("OS:");

  os::Posix::print_uname_info(st);

  uint32_t ver = os::Aix::os_version();
  st->print_cr("AIX kernel version %u.%u.%u.%u",
               (ver >> 24) & 0xFF, (ver >> 16) & 0xFF, (ver >> 8) & 0xFF, ver & 0xFF);

  os::Posix::print_uptime_info(st);

  os::Posix::print_rlimit_info(st);

  os::Posix::print_load_average(st);

  // _SC_THREAD_THREADS_MAX is the maximum number of threads within a process.
  long tmax = sysconf(_SC_THREAD_THREADS_MAX);
  st->print_cr("maximum #threads within a process:%ld", tmax);

  // print wpar info
  libperfstat::wparinfo_t wi;
  if (libperfstat::get_wparinfo(&wi)) {
    st->print_cr("wpar info");
    st->print_cr("name: %s", wi.name);
    st->print_cr("id:   %d", wi.wpar_id);
    st->print_cr("type: %s", (wi.app_wpar ? "application" : "system"));
  }

  VM_Version::print_platform_virtualization_info(st);
}

void os::print_memory_info(outputStream* st) {

  st->print_cr("Memory:");

  st->print_cr("  Base page size (sysconf _SC_PAGESIZE):  %s",
    describe_pagesize(g_multipage_support.pagesize));
  st->print_cr("  Data page size (C-Heap, bss, etc):      %s",
    describe_pagesize(g_multipage_support.datapsize));
  st->print_cr("  Text page size:                         %s",
    describe_pagesize(g_multipage_support.textpsize));
  st->print_cr("  Thread stack page size (pthread):       %s",
    describe_pagesize(g_multipage_support.pthr_stack_pagesize));
  st->print_cr("  Default shared memory page size:        %s",
    describe_pagesize(g_multipage_support.shmpsize));
  st->print_cr("  Can use 64K pages dynamically with shared memory:  %s",
    (g_multipage_support.can_use_64K_pages ? "yes" :"no"));
  st->print_cr("  Can use 16M pages dynamically with shared memory: %s",
    (g_multipage_support.can_use_16M_pages ? "yes" :"no"));
  st->print_cr("  Multipage error: %d",
    g_multipage_support.error);
  st->cr();
  st->print_cr("  os::vm_page_size:       %s", describe_pagesize(os::vm_page_size()));

  // print out LDR_CNTRL because it affects the default page sizes
  const char* const ldr_cntrl = ::getenv("LDR_CNTRL");
  st->print_cr("  LDR_CNTRL=%s.", ldr_cntrl ? ldr_cntrl : "<unset>");

  // Print out EXTSHM because it is an unsupported setting.
  const char* const extshm = ::getenv("EXTSHM");
  st->print_cr("  EXTSHM=%s.", extshm ? extshm : "<unset>");
  if ( (strcmp(extshm, "on") == 0) || (strcmp(extshm, "ON") == 0) ) {
    st->print_cr("  *** Unsupported! Please remove EXTSHM from your environment! ***");
  }

  // Print out AIXTHREAD_GUARDPAGES because it affects the size of pthread stacks.
  const char* const aixthread_guardpages = ::getenv("AIXTHREAD_GUARDPAGES");
  st->print_cr("  AIXTHREAD_GUARDPAGES=%s.",
      aixthread_guardpages ? aixthread_guardpages : "<unset>");
  st->cr();

  os::Aix::meminfo_t mi;
  if (os::Aix::get_meminfo(&mi)) {
    if (os::Aix::on_aix()) {
      st->print_cr("physical total : " SIZE_FORMAT, mi.real_total);
      st->print_cr("physical free  : " SIZE_FORMAT, mi.real_free);
      st->print_cr("swap total     : " SIZE_FORMAT, mi.pgsp_total);
      st->print_cr("swap free      : " SIZE_FORMAT, mi.pgsp_free);
    } else {
      // PASE - Numbers are result of QWCRSSTS; they mean:
      // real_total: Sum of all system pools
      // real_free: always 0
      // pgsp_total: we take the size of the system ASP
      // pgsp_free: size of system ASP times percentage of system ASP unused
      st->print_cr("physical total     : " SIZE_FORMAT, mi.real_total);
      st->print_cr("system asp total   : " SIZE_FORMAT, mi.pgsp_total);
      st->print_cr("%% system asp used : %.2f",
        mi.pgsp_total ? (100.0f * (mi.pgsp_total - mi.pgsp_free) / mi.pgsp_total) : -1.0f);
    }
  }
  st->cr();

  // Print program break.
  st->print_cr("Program break at VM startup: " PTR_FORMAT ".", p2i(g_brk_at_startup));
  address brk_now = (address)::sbrk(0);
  if (brk_now != (address)-1) {
    st->print_cr("Program break now          : " PTR_FORMAT " (distance: " SIZE_FORMAT "k).",
                 p2i(brk_now), (size_t)((brk_now - g_brk_at_startup) / K));
  }
  st->print_cr("MaxExpectedDataSegmentSize    : " SIZE_FORMAT "k.", MaxExpectedDataSegmentSize / K);
  st->cr();

  // Print segments allocated with os::reserve_memory.
  st->print_cr("internal virtual memory regions used by vm:");
  vmembk_print_on(st);
}

// Get a string for the cpuinfo that is a summary of the cpu type
void os::get_summary_cpu_info(char* buf, size_t buflen) {
  // read _system_configuration.version
  switch (_system_configuration.version) {
  case PV_9:
    strncpy(buf, "Power PC 9", buflen);
    break;
  case PV_8:
    strncpy(buf, "Power PC 8", buflen);
    break;
  case PV_7:
    strncpy(buf, "Power PC 7", buflen);
    break;
  case PV_6_1:
    strncpy(buf, "Power PC 6 DD1.x", buflen);
    break;
  case PV_6:
    strncpy(buf, "Power PC 6", buflen);
    break;
  case PV_5:
    strncpy(buf, "Power PC 5", buflen);
    break;
  case PV_5_2:
    strncpy(buf, "Power PC 5_2", buflen);
    break;
  case PV_5_3:
    strncpy(buf, "Power PC 5_3", buflen);
    break;
  case PV_5_Compat:
    strncpy(buf, "PV_5_Compat", buflen);
    break;
  case PV_6_Compat:
    strncpy(buf, "PV_6_Compat", buflen);
    break;
  case PV_7_Compat:
    strncpy(buf, "PV_7_Compat", buflen);
    break;
  case PV_8_Compat:
    strncpy(buf, "PV_8_Compat", buflen);
    break;
  case PV_9_Compat:
    strncpy(buf, "PV_9_Compat", buflen);
    break;
  default:
    strncpy(buf, "unknown", buflen);
  }
}

void os::pd_print_cpu_info(outputStream* st, char* buf, size_t buflen) {
  // Nothing to do beyond of what os::print_cpu_info() does.
}

static char saved_jvm_path[MAXPATHLEN] = {0};

// Find the full path to the current module, libjvm.so.
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

  Dl_info dlinfo;
  int ret = dladdr(CAST_FROM_FN_PTR(void *, os::jvm_path), &dlinfo);
  assert(ret != 0, "cannot locate libjvm");
  char* rp = os::Posix::realpath((char *)dlinfo.dli_fname, buf, buflen);
  assert(rp != NULL, "error in realpath(): maybe the 'path' argument is too long?");

  if (Arguments::sun_java_launcher_is_altjvm()) {
    // Support for the java launcher's '-XXaltjvm=<path>' option. Typical
    // value for buf is "<JAVA_HOME>/jre/lib/<vmtype>/libjvm.so".
    // If "/jre/lib/" appears at the right place in the string, then
    // assume we are installed in a JDK and we're done. Otherwise, check
    // for a JAVA_HOME environment variable and fix up the path so it
    // looks like libjvm.so is installed there (append a fake suffix
    // hotspot/libjvm.so).
    const char *p = buf + strlen(buf) - 1;
    for (int count = 0; p > buf && count < 4; ++count) {
      for (--p; p > buf && *p != '/'; --p)
        /* empty */ ;
    }

    if (strncmp(p, "/jre/lib/", 9) != 0) {
      // Look for JAVA_HOME in the environment.
      char* java_home_var = ::getenv("JAVA_HOME");
      if (java_home_var != NULL && java_home_var[0] != 0) {
        char* jrelib_p;
        int len;

        // Check the current module name "libjvm.so".
        p = strrchr(buf, '/');
        if (p == NULL) {
          return;
        }
        assert(strstr(p, "/libjvm") == p, "invalid library name");

        rp = os::Posix::realpath(java_home_var, buf, buflen);
        if (rp == NULL) {
          return;
        }

        // determine if this is a legacy image or modules image
        // modules image doesn't have "jre" subdirectory
        len = strlen(buf);
        assert(len < buflen, "Ran out of buffer room");
        jrelib_p = buf + len;
        snprintf(jrelib_p, buflen-len, "/jre/lib");
        if (0 != access(buf, F_OK)) {
          snprintf(jrelib_p, buflen-len, "/lib");
        }

        if (0 == access(buf, F_OK)) {
          // Use current module name "libjvm.so"
          len = strlen(buf);
          snprintf(buf + len, buflen-len, "/hotspot/libjvm.so");
        } else {
          // Go back to path of .so
          rp = os::Posix::realpath((char *)dlinfo.dli_fname, buf, buflen);
          if (rp == NULL) {
            return;
          }
        }
      }
    }
  }

  strncpy(saved_jvm_path, buf, sizeof(saved_jvm_path));
  saved_jvm_path[sizeof(saved_jvm_path) - 1] = '\0';
}

void os::print_jni_name_prefix_on(outputStream* st, int args_size) {
  // no prefix required, not even "_"
}

void os::print_jni_name_suffix_on(outputStream* st, int args_size) {
  // no suffix required
}

////////////////////////////////////////////////////////////////////////////////
// Virtual Memory

// We need to keep small simple bookkeeping for os::reserve_memory and friends.

#define VMEM_MAPPED  1
#define VMEM_SHMATED 2

struct vmembk_t {
  int type;         // 1 - mmap, 2 - shmat
  char* addr;
  size_t size;      // Real size, may be larger than usersize.
  size_t pagesize;  // page size of area
  vmembk_t* next;

  bool contains_addr(char* p) const {
    return p >= addr && p < (addr + size);
  }

  bool contains_range(char* p, size_t s) const {
    return contains_addr(p) && contains_addr(p + s - 1);
  }

  void print_on(outputStream* os) const {
    os->print("[" PTR_FORMAT " - " PTR_FORMAT "] (" UINTX_FORMAT
      " bytes, %d %s pages), %s",
      addr, addr + size - 1, size, size / pagesize, describe_pagesize(pagesize),
      (type == VMEM_SHMATED ? "shmat" : "mmap")
    );
  }

  // Check that range is a sub range of memory block (or equal to memory block);
  // also check that range is fully page aligned to the page size if the block.
  void assert_is_valid_subrange(char* p, size_t s) const {
    if (!contains_range(p, s)) {
      trcVerbose("[" PTR_FORMAT " - " PTR_FORMAT "] is not a sub "
              "range of [" PTR_FORMAT " - " PTR_FORMAT "].",
              p2i(p), p2i(p + s), p2i(addr), p2i(addr + size));
      guarantee0(false);
    }
    if (!is_aligned_to(p, pagesize) || !is_aligned_to(p + s, pagesize)) {
      trcVerbose("range [" PTR_FORMAT " - " PTR_FORMAT "] is not"
              " aligned to pagesize (%lu)", p2i(p), p2i(p + s), (unsigned long) pagesize);
      guarantee0(false);
    }
  }
};

static struct {
  vmembk_t* first;
  MiscUtils::CritSect cs;
} vmem;

static void vmembk_add(char* addr, size_t size, size_t pagesize, int type) {
  vmembk_t* p = (vmembk_t*) ::malloc(sizeof(vmembk_t));
  assert0(p);
  if (p) {
    MiscUtils::AutoCritSect lck(&vmem.cs);
    p->addr = addr; p->size = size;
    p->pagesize = pagesize;
    p->type = type;
    p->next = vmem.first;
    vmem.first = p;
  }
}

static vmembk_t* vmembk_find(char* addr) {
  MiscUtils::AutoCritSect lck(&vmem.cs);
  for (vmembk_t* p = vmem.first; p; p = p->next) {
    if (p->addr <= addr && (p->addr + p->size) > addr) {
      return p;
    }
  }
  return NULL;
}

static void vmembk_remove(vmembk_t* p0) {
  MiscUtils::AutoCritSect lck(&vmem.cs);
  assert0(p0);
  assert0(vmem.first); // List should not be empty.
  for (vmembk_t** pp = &(vmem.first); *pp; pp = &((*pp)->next)) {
    if (*pp == p0) {
      *pp = p0->next;
      ::free(p0);
      return;
    }
  }
  assert0(false); // Not found?
}

static void vmembk_print_on(outputStream* os) {
  MiscUtils::AutoCritSect lck(&vmem.cs);
  for (vmembk_t* vmi = vmem.first; vmi; vmi = vmi->next) {
    vmi->print_on(os);
    os->cr();
  }
}

// Reserve and attach a section of System V memory.
// If <requested_addr> is not NULL, function will attempt to attach the memory at the given
// address. Failing that, it will attach the memory anywhere.
// If <requested_addr> is NULL, function will attach the memory anywhere.
static char* reserve_shmated_memory (size_t bytes, char* requested_addr) {

  trcVerbose("reserve_shmated_memory " UINTX_FORMAT " bytes, wishaddress "
    PTR_FORMAT "...", bytes, p2i(requested_addr));

  // We must prevent anyone from attaching too close to the
  // BRK because that may cause malloc OOM.
  if (requested_addr != NULL && is_close_to_brk((address)requested_addr)) {
    trcVerbose("Wish address " PTR_FORMAT " is too close to the BRK segment.", p2i(requested_addr));
    // Since we treat an attach to the wrong address as an error later anyway,
    // we return NULL here
    return NULL;
  }

  // For old AS/400's (V5R4 and older) we should not even be here - System V shared memory is not
  // really supported (max size 4GB), so reserve_mmapped_memory should have been used instead.
  if (os::Aix::on_pase_V5R4_or_older()) {
    ShouldNotReachHere();
  }

  // Align size of shm up to 64K to avoid errors if we later try to change the page size.
  const size_t size = align_up(bytes, 64*K);

  // Reserve the shared segment.
  int shmid = shmget(IPC_PRIVATE, size, IPC_CREAT | S_IRUSR | S_IWUSR);
  if (shmid == -1) {
    trcVerbose("shmget(.., " UINTX_FORMAT ", ..) failed (errno: %d).", size, errno);
    return NULL;
  }

  // Important note:
  // It is very important that we, upon leaving this function, do not leave a shm segment alive.
  // We must right after attaching it remove it from the system. System V shm segments are global and
  // survive the process.
  // So, from here on: Do not assert, do not return, until we have called shmctl(IPC_RMID) (A).

  struct shmid_ds shmbuf;
  memset(&shmbuf, 0, sizeof(shmbuf));
  shmbuf.shm_pagesize = 64*K;
  if (shmctl(shmid, SHM_PAGESIZE, &shmbuf) != 0) {
    trcVerbose("Failed to set page size (need " UINTX_FORMAT " 64K pages) - shmctl failed with %d.",
               size / (64*K), errno);
    // I want to know if this ever happens.
    assert(false, "failed to set page size for shmat");
  }

  // Now attach the shared segment.
  // Note that I attach with SHM_RND - which means that the requested address is rounded down, if
  // needed, to the next lowest segment boundary. Otherwise the attach would fail if the address
  // were not a segment boundary.
  char* const addr = (char*) shmat(shmid, requested_addr, SHM_RND);
  const int errno_shmat = errno;

  // (A) Right after shmat and before handing shmat errors delete the shm segment.
  if (::shmctl(shmid, IPC_RMID, NULL) == -1) {
    trcVerbose("shmctl(%u, IPC_RMID) failed (%d)\n", shmid, errno);
    assert(false, "failed to remove shared memory segment!");
  }

  // Handle shmat error. If we failed to attach, just return.
  if (addr == (char*)-1) {
    trcVerbose("Failed to attach segment at " PTR_FORMAT " (%d).", p2i(requested_addr), errno_shmat);
    return NULL;
  }

  // Just for info: query the real page size. In case setting the page size did not
  // work (see above), the system may have given us something other then 4K (LDR_CNTRL).
  const size_t real_pagesize = os::Aix::query_pagesize(addr);
  if (real_pagesize != shmbuf.shm_pagesize) {
    trcVerbose("pagesize is, surprisingly, " SIZE_FORMAT, real_pagesize);
  }

  if (addr) {
    trcVerbose("shm-allocated " PTR_FORMAT " .. " PTR_FORMAT " (" UINTX_FORMAT " bytes, " UINTX_FORMAT " %s pages)",
      p2i(addr), p2i(addr + size - 1), size, size/real_pagesize, describe_pagesize(real_pagesize));
  } else {
    if (requested_addr != NULL) {
      trcVerbose("failed to shm-allocate " UINTX_FORMAT " bytes at with address " PTR_FORMAT ".", size, p2i(requested_addr));
    } else {
      trcVerbose("failed to shm-allocate " UINTX_FORMAT " bytes at any address.", size);
    }
  }

  // book-keeping
  vmembk_add(addr, size, real_pagesize, VMEM_SHMATED);
  assert0(is_aligned_to(addr, os::vm_page_size()));

  return addr;
}

static bool release_shmated_memory(char* addr, size_t size) {

  trcVerbose("release_shmated_memory [" PTR_FORMAT " - " PTR_FORMAT "].",
    p2i(addr), p2i(addr + size - 1));

  bool rc = false;

  // TODO: is there a way to verify shm size without doing bookkeeping?
  if (::shmdt(addr) != 0) {
    trcVerbose("error (%d).", errno);
  } else {
    trcVerbose("ok.");
    rc = true;
  }
  return rc;
}

static bool uncommit_shmated_memory(char* addr, size_t size) {
  trcVerbose("uncommit_shmated_memory [" PTR_FORMAT " - " PTR_FORMAT "].",
    p2i(addr), p2i(addr + size - 1));

  const bool rc = my_disclaim64(addr, size);

  if (!rc) {
    trcVerbose("my_disclaim64(" PTR_FORMAT ", " UINTX_FORMAT ") failed.\n", p2i(addr), size);
    return false;
  }
  return true;
}

////////////////////////////////  mmap-based routines /////////////////////////////////

// Reserve memory via mmap.
// If <requested_addr> is given, an attempt is made to attach at the given address.
// Failing that, memory is allocated at any address.
static char* reserve_mmaped_memory(size_t bytes, char* requested_addr) {
  trcVerbose("reserve_mmaped_memory " UINTX_FORMAT " bytes, wishaddress " PTR_FORMAT "...",
    bytes, p2i(requested_addr));

  if (requested_addr && !is_aligned_to(requested_addr, os::vm_page_size()) != 0) {
    trcVerbose("Wish address " PTR_FORMAT " not aligned to page boundary.", p2i(requested_addr));
    return NULL;
  }

  // We must prevent anyone from attaching too close to the
  // BRK because that may cause malloc OOM.
  if (requested_addr != NULL && is_close_to_brk((address)requested_addr)) {
    trcVerbose("Wish address " PTR_FORMAT " is too close to the BRK segment.", p2i(requested_addr));
    // Since we treat an attach to the wrong address as an error later anyway,
    // we return NULL here
    return NULL;
  }

  // In 64K mode, we lie and claim the global page size (os::vm_page_size()) is 64K
  //  (complicated story). This mostly works just fine since 64K is a multiple of the
  //  actual 4K lowest page size. Only at a few seams light shines thru, e.g. when
  //  calling mmap. mmap will return memory aligned to the lowest pages size - 4K -
  //  so we must make sure - transparently - that the caller only ever sees 64K
  //  aligned mapping start addresses.
  const size_t alignment = os::vm_page_size();

  // Size shall always be a multiple of os::vm_page_size (esp. in 64K mode).
  const size_t size = align_up(bytes, os::vm_page_size());

  // alignment: Allocate memory large enough to include an aligned range of the right size and
  // cut off the leading and trailing waste pages.
  assert0(alignment != 0 && is_aligned_to(alignment, os::vm_page_size())); // see above
  const size_t extra_size = size + alignment;

  // Note: MAP_SHARED (instead of MAP_PRIVATE) needed to be able to
  // later use msync(MS_INVALIDATE) (see os::uncommit_memory).
  int flags = MAP_ANONYMOUS | MAP_SHARED;

  // MAP_FIXED is needed to enforce requested_addr - manpage is vague about what
  // it means if wishaddress is given but MAP_FIXED is not set.
  //
  // Important! Behaviour differs depending on whether SPEC1170 mode is active or not.
  // SPEC1170 mode active: behaviour like POSIX, MAP_FIXED will clobber existing mappings.
  // SPEC1170 mode not active: behaviour, unlike POSIX, is that no existing mappings will
  // get clobbered.
  if (requested_addr != NULL) {
    if (!os::Aix::xpg_sus_mode()) {  // not SPEC1170 Behaviour
      flags |= MAP_FIXED;
    }
  }

  char* addr = (char*)::mmap(requested_addr, extra_size,
      PROT_READ|PROT_WRITE|PROT_EXEC, flags, -1, 0);

  if (addr == MAP_FAILED) {
    trcVerbose("mmap(" PTR_FORMAT ", " UINTX_FORMAT ", ..) failed (%d)", p2i(requested_addr), size, errno);
    return NULL;
  } else if (requested_addr != NULL && addr != requested_addr) {
    trcVerbose("mmap(" PTR_FORMAT ", " UINTX_FORMAT ", ..) succeeded, but at a different address than requested (" PTR_FORMAT "), will unmap",
               p2i(requested_addr), size, p2i(addr));
    ::munmap(addr, extra_size);
    return NULL;
  }

  // Handle alignment.
  char* const addr_aligned = align_up(addr, alignment);
  const size_t waste_pre = addr_aligned - addr;
  char* const addr_aligned_end = addr_aligned + size;
  const size_t waste_post = extra_size - waste_pre - size;
  if (waste_pre > 0) {
    ::munmap(addr, waste_pre);
  }
  if (waste_post > 0) {
    ::munmap(addr_aligned_end, waste_post);
  }
  addr = addr_aligned;

  trcVerbose("mmap-allocated " PTR_FORMAT " .. " PTR_FORMAT " (" UINTX_FORMAT " bytes)",
    p2i(addr), p2i(addr + bytes), bytes);

  // bookkeeping
  vmembk_add(addr, size, 4*K, VMEM_MAPPED);

  // Test alignment, see above.
  assert0(is_aligned_to(addr, os::vm_page_size()));

  return addr;
}

static bool release_mmaped_memory(char* addr, size_t size) {
  assert0(is_aligned_to(addr, os::vm_page_size()));
  assert0(is_aligned_to(size, os::vm_page_size()));

  trcVerbose("release_mmaped_memory [" PTR_FORMAT " - " PTR_FORMAT "].",
    p2i(addr), p2i(addr + size - 1));
  bool rc = false;

  if (::munmap(addr, size) != 0) {
    trcVerbose("failed (%d)\n", errno);
    rc = false;
  } else {
    trcVerbose("ok.");
    rc = true;
  }

  return rc;
}

static bool uncommit_mmaped_memory(char* addr, size_t size) {

  assert0(is_aligned_to(addr, os::vm_page_size()));
  assert0(is_aligned_to(size, os::vm_page_size()));

  trcVerbose("uncommit_mmaped_memory [" PTR_FORMAT " - " PTR_FORMAT "].",
    p2i(addr), p2i(addr + size - 1));
  bool rc = false;

  // Uncommit mmap memory with msync MS_INVALIDATE.
  if (::msync(addr, size, MS_INVALIDATE) != 0) {
    trcVerbose("failed (%d)\n", errno);
    rc = false;
  } else {
    trcVerbose("ok.");
    rc = true;
  }

  return rc;
}

int os::vm_page_size() {
  // Seems redundant as all get out.
  assert(os::Aix::page_size() != -1, "must call os::init");
  return os::Aix::page_size();
}

// Aix allocates memory by pages.
int os::vm_allocation_granularity() {
  assert(os::Aix::page_size() != -1, "must call os::init");
  return os::Aix::page_size();
}

#ifdef PRODUCT
static void warn_fail_commit_memory(char* addr, size_t size, bool exec,
                                    int err) {
  warning("INFO: os::commit_memory(" PTR_FORMAT ", " SIZE_FORMAT
          ", %d) failed; error='%s' (errno=%d)", p2i(addr), size, exec,
          os::errno_name(err), err);
}
#endif

void os::pd_commit_memory_or_exit(char* addr, size_t size, bool exec,
                                  const char* mesg) {
  assert(mesg != NULL, "mesg must be specified");
  if (!pd_commit_memory(addr, size, exec)) {
    // Add extra info in product mode for vm_exit_out_of_memory():
    PRODUCT_ONLY(warn_fail_commit_memory(addr, size, exec, errno);)
    vm_exit_out_of_memory(size, OOM_MMAP_ERROR, "%s", mesg);
  }
}

bool os::pd_commit_memory(char* addr, size_t size, bool exec) {

  assert(is_aligned_to(addr, os::vm_page_size()),
    "addr " PTR_FORMAT " not aligned to vm_page_size (" PTR_FORMAT ")",
    p2i(addr), os::vm_page_size());
  assert(is_aligned_to(size, os::vm_page_size()),
    "size " PTR_FORMAT " not aligned to vm_page_size (" PTR_FORMAT ")",
    size, os::vm_page_size());

  vmembk_t* const vmi = vmembk_find(addr);
  guarantee0(vmi);
  vmi->assert_is_valid_subrange(addr, size);

  trcVerbose("commit_memory [" PTR_FORMAT " - " PTR_FORMAT "].", p2i(addr), p2i(addr + size - 1));

  if (UseExplicitCommit) {
    // AIX commits memory on touch. So, touch all pages to be committed.
    for (char* p = addr; p < (addr + size); p += 4*K) {
      *p = '\0';
    }
  }

  return true;
}

bool os::pd_commit_memory(char* addr, size_t size, size_t alignment_hint, bool exec) {
  return pd_commit_memory(addr, size, exec);
}

void os::pd_commit_memory_or_exit(char* addr, size_t size,
                                  size_t alignment_hint, bool exec,
                                  const char* mesg) {
  // Alignment_hint is ignored on this OS.
  pd_commit_memory_or_exit(addr, size, exec, mesg);
}

bool os::pd_uncommit_memory(char* addr, size_t size, bool exec) {
  assert(is_aligned_to(addr, os::vm_page_size()),
    "addr " PTR_FORMAT " not aligned to vm_page_size (" PTR_FORMAT ")",
    p2i(addr), os::vm_page_size());
  assert(is_aligned_to(size, os::vm_page_size()),
    "size " PTR_FORMAT " not aligned to vm_page_size (" PTR_FORMAT ")",
    size, os::vm_page_size());

  // Dynamically do different things for mmap/shmat.
  const vmembk_t* const vmi = vmembk_find(addr);
  guarantee0(vmi);
  vmi->assert_is_valid_subrange(addr, size);

  if (vmi->type == VMEM_SHMATED) {
    return uncommit_shmated_memory(addr, size);
  } else {
    return uncommit_mmaped_memory(addr, size);
  }
}

bool os::pd_create_stack_guard_pages(char* addr, size_t size) {
  // Do not call this; no need to commit stack pages on AIX.
  ShouldNotReachHere();
  return true;
}

bool os::remove_stack_guard_pages(char* addr, size_t size) {
  // Do not call this; no need to commit stack pages on AIX.
  ShouldNotReachHere();
  return true;
}

void os::pd_realign_memory(char *addr, size_t bytes, size_t alignment_hint) {
}

void os::pd_free_memory(char *addr, size_t bytes, size_t alignment_hint) {
}

void os::numa_make_global(char *addr, size_t bytes) {
}

void os::numa_make_local(char *addr, size_t bytes, int lgrp_hint) {
}

bool os::numa_topology_changed() {
  return false;
}

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

// Reserves and attaches a shared memory segment.
char* os::pd_reserve_memory(size_t bytes, bool exec) {
  // Always round to os::vm_page_size(), which may be larger than 4K.
  bytes = align_up(bytes, os::vm_page_size());

  // In 4K mode always use mmap.
  // In 64K mode allocate small sizes with mmap, large ones with 64K shmatted.
  if (os::vm_page_size() == 4*K) {
    return reserve_mmaped_memory(bytes, NULL /* requested_addr */);
  } else {
    if (bytes >= Use64KPagesThreshold) {
      return reserve_shmated_memory(bytes, NULL /* requested_addr */);
    } else {
      return reserve_mmaped_memory(bytes, NULL /* requested_addr */);
    }
  }
}

bool os::pd_release_memory(char* addr, size_t size) {

  // Dynamically do different things for mmap/shmat.
  vmembk_t* const vmi = vmembk_find(addr);
  guarantee0(vmi);
  vmi->assert_is_valid_subrange(addr, size);

  // Always round to os::vm_page_size(), which may be larger than 4K.
  size = align_up(size, os::vm_page_size());
  addr = align_up(addr, os::vm_page_size());

  bool rc = false;
  bool remove_bookkeeping = false;
  if (vmi->type == VMEM_SHMATED) {
    // For shmatted memory, we do:
    // - If user wants to release the whole range, release the memory (shmdt).
    // - If user only wants to release a partial range, uncommit (disclaim) that
    //   range. That way, at least, we do not use memory anymore (bust still page
    //   table space).
    if (addr == vmi->addr && size == vmi->size) {
      rc = release_shmated_memory(addr, size);
      remove_bookkeeping = true;
    } else {
      rc = uncommit_shmated_memory(addr, size);
    }
  } else {
    // In mmap-mode:
    //  - If the user wants to release the full range, we do that and remove the mapping.
    //  - If the user wants to release part of the range, we release that part, but need
    //    to adjust bookkeeping.
    assert(is_aligned(size, 4 * K), "Sanity");
    rc = release_mmaped_memory(addr, size);
    if (addr == vmi->addr && size == vmi->size) {
      remove_bookkeeping = true;
    } else {
      if (addr == vmi->addr && size < vmi->size) {
        // Chopped from head
        vmi->addr += size;
        vmi->size -= size;
      } else if (addr + size == vmi->addr + vmi->size) {
        // Chopped from tail
        vmi->size -= size;
      } else {
        // releasing a mapping in the middle of the original mapping:
        // For now we forbid this, since this is an invalid scenario
        // (the bookkeeping is easy enough to fix if needed but there
        //  is no use case for it; any occurrence is likely an error.
        ShouldNotReachHere();
      }
    }
  }

  // update bookkeeping
  if (rc && remove_bookkeeping) {
    vmembk_remove(vmi);
  }

  return rc;
}

static bool checked_mprotect(char* addr, size_t size, int prot) {

  // Little problem here: if SPEC1170 behaviour is off, mprotect() on AIX will
  // not tell me if protection failed when trying to protect an un-protectable range.
  //
  // This means if the memory was allocated using shmget/shmat, protection wont work
  // but mprotect will still return 0:
  //
  // See http://publib.boulder.ibm.com/infocenter/pseries/v5r3/index.jsp?topic=/com.ibm.aix.basetechref/doc/basetrf1/mprotect.htm

  Events::log(NULL, "Protecting memory [" INTPTR_FORMAT "," INTPTR_FORMAT "] with protection modes %x", p2i(addr), p2i(addr+size), prot);
  bool rc = ::mprotect(addr, size, prot) == 0 ? true : false;

  if (!rc) {
    const char* const s_errno = os::errno_name(errno);
    warning("mprotect(" PTR_FORMAT "-" PTR_FORMAT ", 0x%X) failed (%s).", addr, addr + size, prot, s_errno);
    return false;
  }

  // mprotect success check
  //
  // Mprotect said it changed the protection but can I believe it?
  //
  // To be sure I need to check the protection afterwards. Try to
  // read from protected memory and check whether that causes a segfault.
  //
  if (!os::Aix::xpg_sus_mode()) {

    if (CanUseSafeFetch32()) {

      const bool read_protected =
        (SafeFetch32((int*)addr, 0x12345678) == 0x12345678 &&
         SafeFetch32((int*)addr, 0x76543210) == 0x76543210) ? true : false;

      if (prot & PROT_READ) {
        rc = !read_protected;
      } else {
        rc = read_protected;
      }

      if (!rc) {
        if (os::Aix::on_pase()) {
          // There is an issue on older PASE systems where mprotect() will return success but the
          // memory will not be protected.
          // This has nothing to do with the problem of using mproect() on SPEC1170 incompatible
          // machines; we only see it rarely, when using mprotect() to protect the guard page of
          // a stack. It is an OS error.
          //
          // A valid strategy is just to try again. This usually works. :-/

          ::usleep(1000);
          Events::log(NULL, "Protecting memory [" INTPTR_FORMAT "," INTPTR_FORMAT "] with protection modes %x", p2i(addr), p2i(addr+size), prot);
          if (::mprotect(addr, size, prot) == 0) {
            const bool read_protected_2 =
              (SafeFetch32((int*)addr, 0x12345678) == 0x12345678 &&
              SafeFetch32((int*)addr, 0x76543210) == 0x76543210) ? true : false;
            rc = true;
          }
        }
      }
    }
  }

  assert(rc == true, "mprotect failed.");

  return rc;
}

// Set protections specified
bool os::protect_memory(char* addr, size_t size, ProtType prot, bool is_committed) {
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
  return checked_mprotect(addr, size, p);
}

bool os::guard_memory(char* addr, size_t size) {
  return checked_mprotect(addr, size, PROT_NONE);
}

bool os::unguard_memory(char* addr, size_t size) {
  return checked_mprotect(addr, size, PROT_READ|PROT_WRITE|PROT_EXEC);
}

// Large page support

static size_t _large_page_size = 0;

// Enable large page support if OS allows that.
void os::large_page_init() {
  return; // Nothing to do. See query_multipage_support and friends.
}

char* os::pd_reserve_memory_special(size_t bytes, size_t alignment, size_t page_size, char* req_addr, bool exec) {
  fatal("os::reserve_memory_special should not be called on AIX.");
  return NULL;
}

bool os::pd_release_memory_special(char* base, size_t bytes) {
  fatal("os::release_memory_special should not be called on AIX.");
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
  char* result = NULL;

  // Always round to os::vm_page_size(), which may be larger than 4K.
  bytes = align_up(bytes, os::vm_page_size());
  result = reserve_mmaped_memory(bytes, requested_addr);

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
  char* addr = NULL;

  // Always round to os::vm_page_size(), which may be larger than 4K.
  bytes = align_up(bytes, os::vm_page_size());

  // In 4K mode always use mmap.
  // In 64K mode allocate small sizes with mmap, large ones with 64K shmatted.
  if (os::vm_page_size() == 4*K) {
    return reserve_mmaped_memory(bytes, requested_addr);
  } else {
    if (bytes >= Use64KPagesThreshold) {
      return reserve_shmated_memory(bytes, requested_addr);
    } else {
      return reserve_mmaped_memory(bytes, requested_addr);
    }
  }

  return addr;
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

// From AIX manpage to pthread_setschedparam
// (see: http://publib.boulder.ibm.com/infocenter/pseries/v5r3/index.jsp?
//    topic=/com.ibm.aix.basetechref/doc/basetrf1/pthread_setschedparam.htm):
//
// "If schedpolicy is SCHED_OTHER, then sched_priority must be in the
// range from 40 to 80, where 40 is the least favored priority and 80
// is the most favored."
//
// (Actually, I doubt this even has an impact on AIX, as we do kernel
// scheduling there; however, this still leaves iSeries.)
//
// We use the same values for AIX and PASE.
int os::java_to_os_priority[CriticalPriority + 1] = {
  54,             // 0 Entry should never be used

  55,             // 1 MinPriority
  55,             // 2
  56,             // 3

  56,             // 4
  57,             // 5 NormPriority
  57,             // 6

  58,             // 7
  58,             // 8
  59,             // 9 NearMaxPriority

  60,             // 10 MaxPriority

  60              // 11 CriticalPriority
};

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
  pthread_t thr = thread->osthread()->pthread_id();
  int policy = SCHED_OTHER;
  struct sched_param param;
  param.sched_priority = newpri;
  int ret = pthread_setschedparam(thr, policy, &param);

  if (ret != 0) {
    trcVerbose("Could not change priority for thread %d to %d (error %d, %s)",
        (int)thr, newpri, ret, os::errno_name(ret));
  }
  return (ret == 0) ? OS_OK : OS_ERR;
}

OSReturn os::get_native_priority(const Thread* const thread, int *priority_ptr) {
  if (!UseThreadPriorities || ThreadPriorityPolicy == 0) {
    *priority_ptr = java_to_os_priority[NormPriority];
    return OS_OK;
  }
  pthread_t thr = thread->osthread()->pthread_id();
  int policy = SCHED_OTHER;
  struct sched_param param;
  int ret = pthread_getschedparam(thr, &policy, &param);
  *priority_ptr = param.sched_priority;

  return (ret == 0) ? OS_OK : OS_ERR;
}

// To install functions for atexit system call
extern "C" {
  static void perfMemory_exit_helper() {
    perfMemory_exit();
  }
}

// This is called _before_ the most of global arguments have been parsed.
void os::init(void) {
  // This is basic, we want to know if that ever changes.
  // (Shared memory boundary is supposed to be a 256M aligned.)
  assert(SHMLBA == ((uint64_t)0x10000000ULL)/*256M*/, "unexpected");

  // Record process break at startup.
  g_brk_at_startup = (address) ::sbrk(0);
  assert(g_brk_at_startup != (address) -1, "sbrk failed");

  // First off, we need to know whether we run on AIX or PASE, and
  // the OS level we run on.
  os::Aix::initialize_os_info();

  // Scan environment (SPEC1170 behaviour, etc).
  os::Aix::scan_environment();

  // Probe multipage support.
  query_multipage_support();

  // Act like we only have one page size by eliminating corner cases which
  // we did not support very well anyway.
  // We have two input conditions:
  // 1) Data segment page size. This is controlled by linker setting (datapsize) on the
  //    launcher, and/or by LDR_CNTRL environment variable. The latter overrules the linker
  //    setting.
  //    Data segment page size is important for us because it defines the thread stack page
  //    size, which is needed for guard page handling, stack banging etc.
  // 2) The ability to allocate 64k pages dynamically. If this is a given, java heap can
  //    and should be allocated with 64k pages.
  //
  // So, we do the following:
  // LDR_CNTRL    can_use_64K_pages_dynamically       what we do                      remarks
  // 4K           no                                  4K                              old systems (aix 5.2, as/400 v5r4) or new systems with AME activated
  // 4k           yes                                 64k (treat 4k stacks as 64k)    different loader than java and standard settings
  // 64k          no              --- AIX 5.2 ? ---
  // 64k          yes                                 64k                             new systems and standard java loader (we set datapsize=64k when linking)

  // We explicitly leave no option to change page size, because only upgrading would work,
  // not downgrading (if stack page size is 64k you cannot pretend its 4k).

  if (g_multipage_support.datapsize == 4*K) {
    // datapsize = 4K. Data segment, thread stacks are 4K paged.
    if (g_multipage_support.can_use_64K_pages) {
      // .. but we are able to use 64K pages dynamically.
      // This would be typical for java launchers which are not linked
      // with datapsize=64K (like, any other launcher but our own).
      //
      // In this case it would be smart to allocate the java heap with 64K
      // to get the performance benefit, and to fake 64k pages for the
      // data segment (when dealing with thread stacks).
      //
      // However, leave a possibility to downgrade to 4K, using
      // -XX:-Use64KPages.
      if (Use64KPages) {
        trcVerbose("64K page mode (faked for data segment)");
        Aix::_page_size = 64*K;
      } else {
        trcVerbose("4K page mode (Use64KPages=off)");
        Aix::_page_size = 4*K;
      }
    } else {
      // .. and not able to allocate 64k pages dynamically. Here, just
      // fall back to 4K paged mode and use mmap for everything.
      trcVerbose("4K page mode");
      Aix::_page_size = 4*K;
      FLAG_SET_ERGO(Use64KPages, false);
    }
  } else {
    // datapsize = 64k. Data segment, thread stacks are 64k paged.
    // This normally means that we can allocate 64k pages dynamically.
    // (There is one special case where this may be false: EXTSHM=on.
    // but we decided to not support that mode).
    assert0(g_multipage_support.can_use_64K_pages);
    Aix::_page_size = 64*K;
    trcVerbose("64K page mode");
    FLAG_SET_ERGO(Use64KPages, true);
  }

  // For now UseLargePages is just ignored.
  FLAG_SET_ERGO(UseLargePages, false);
  _page_sizes.add(Aix::_page_size);

  // debug trace
  trcVerbose("os::vm_page_size %s", describe_pagesize(os::vm_page_size()));

  // Next, we need to initialize libo4 and libperfstat libraries.
  if (os::Aix::on_pase()) {
    os::Aix::initialize_libo4();
  } else {
    os::Aix::initialize_libperfstat();
  }

  // Reset the perfstat information provided by ODM.
  if (os::Aix::on_aix()) {
    libperfstat::perfstat_reset();
  }

  // Now initialze basic system properties. Note that for some of the values we
  // need libperfstat etc.
  os::Aix::initialize_system_info();

  clock_tics_per_sec = sysconf(_SC_CLK_TCK);

  // _main_thread points to the thread that created/loaded the JVM.
  Aix::_main_thread = pthread_self();

  initial_time_count = javaTimeNanos();

  os::Posix::init();
}

// This is called _after_ the global arguments have been parsed.
jint os::init_2(void) {

  // This could be set after os::Posix::init() but all platforms
  // have to set it the same so we have to mirror Solaris.
  DEBUG_ONLY(os::set_mutex_init_done();)

  os::Posix::init_2();

  if (os::Aix::on_pase()) {
    trcVerbose("Running on PASE.");
  } else {
    trcVerbose("Running on AIX (not PASE).");
  }

  trcVerbose("processor count: %d", os::_processor_count);
  trcVerbose("physical memory: %lu", Aix::_physical_memory);

  // Initially build up the loaded dll map.
  LoadedLibraries::reload();
  if (Verbose) {
    trcVerbose("Loaded Libraries: ");
    LoadedLibraries::print(tty);
  }

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
    // Set the number of file descriptors to max. print out error
    // if getrlimit/setrlimit fails but continue regardless.
    struct rlimit nbr_files;
    int status = getrlimit(RLIMIT_NOFILE, &nbr_files);
    if (status != 0) {
      log_info(os)("os::init_2 getrlimit failed: %s", os::strerror(errno));
    } else {
      nbr_files.rlim_cur = nbr_files.rlim_max;
      status = setrlimit(RLIMIT_NOFILE, &nbr_files);
      if (status != 0) {
        log_info(os)("os::init_2 setrlimit failed: %s", os::strerror(errno));
      }
    }
  }

  if (PerfAllowAtExitRegistration) {
    // Only register atexit functions if PerfAllowAtExitRegistration is set.
    // At exit functions can be delayed until process exit time, which
    // can be problematic for embedded VM situations. Embedded VMs should
    // call DestroyJavaVM() to assure that VM resources are released.

    // Note: perfMemory_exit_helper atexit function may be removed in
    // the future if the appropriate cleanup code can be added to the
    // VM_Exit VMOperation's doit method.
    if (atexit(perfMemory_exit_helper) != 0) {
      warning("os::init_2 atexit(perfMemory_exit_helper) failed");
    }
  }

  // initialize thread priority policy
  prio_init();

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

  int online_cpus = ::sysconf(_SC_NPROCESSORS_ONLN);
  assert(online_cpus > 0 && online_cpus <= processor_count(), "sanity check");
  return online_cpus;
}

void os::set_native_thread_name(const char *name) {
  // Not yet implemented.
  return;
}

////////////////////////////////////////////////////////////////////////////////
// debug support

bool os::find(address addr, outputStream* st) {

  st->print(PTR_FORMAT ": ", addr);

  loaded_module_t lm;
  if (LoadedLibraries::find_for_text_address(addr, &lm) != NULL ||
      LoadedLibraries::find_for_data_address(addr, &lm) != NULL) {
    st->print_cr("%s", lm.path);
    return true;
  }

  return false;
}

////////////////////////////////////////////////////////////////////////////////
// misc

// This does not do anything on Aix. This is basically a hook for being
// able to use structured exception handling (thread-local exception filters)
// on, e.g., Win32.
void
os::os_exception_wrapper(java_call_t f, JavaValue* value, const methodHandle& method,
                         JavaCallArguments* args, JavaThread* thread) {
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

// Is a (classpath) directory empty?
bool os::dir_is_empty(const char* path) {
  DIR *dir = NULL;
  struct dirent *ptr;

  dir = opendir(path);
  if (dir == NULL) return true;

  /* Scan the directory */
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
  // AIX 7.X now supports O_CLOEXEC too, like modern Linux; but we have to be careful, see
  // IV90804: OPENING A FILE IN AFS WITH O_CLOEXEC FAILS WITH AN EINVAL ERROR APPLIES TO AIX 7100-04 17/04/14 PTF PECHANGE
  int oflag_with_o_cloexec = oflag | O_CLOEXEC;

  int fd = ::open64(path, oflag_with_o_cloexec, mode);
  if (fd == -1) {
    // we might fail in the open call when O_CLOEXEC is set, so try again without (see IV90804)
    fd = ::open64(path, oflag, mode);
    if (fd == -1) {
      return -1;
    }
  }

  // If the open succeeded, the file might still be a directory.
  {
    struct stat64 buf64;
    int ret = ::fstat64(fd, &buf64);
    int st_mode = buf64.st_mode;

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
  // close-on-exec flag set. If we don't set it, then careless 3rd
  // party native code might fork and exec without closing all
  // appropriate file descriptors (e.g. as we do in closeDescriptors in
  // UNIXProcess.c), and this in turn might:
  //
  // - cause end-of-file to fail to be detected on some file
  //   descriptors, resulting in mysterious hangs, or
  //
  // - might cause an fopen in the subprocess to fail on a system
  //   suffering from bug 1085341.

  // Validate that the use of the O_CLOEXEC flag on open above worked.
  static sig_atomic_t O_CLOEXEC_is_known_to_work = 0;
  if (O_CLOEXEC_is_known_to_work == 0) {
    int flags = ::fcntl(fd, F_GETFD);
    if (flags != -1) {
      if ((flags & FD_CLOEXEC) != 0) {
        O_CLOEXEC_is_known_to_work = 1;
      } else { // it does not work
        ::fcntl(fd, F_SETFD, flags | FD_CLOEXEC);
        O_CLOEXEC_is_known_to_work = -1;
      }
    }
  } else if (O_CLOEXEC_is_known_to_work == -1) {
    int flags = ::fcntl(fd, F_GETFD);
    if (flags != -1) {
      ::fcntl(fd, F_SETFD, flags | FD_CLOEXEC);
    }
  }

  return fd;
}

// create binary file, rewriting existing file if required
int os::create_binary_file(const char* path, bool rewrite_existing) {
  int oflags = O_WRONLY | O_CREAT;
  oflags |= rewrite_existing ? O_TRUNC : O_EXCL;
  return ::open64(path, oflags, S_IREAD | S_IWRITE);
}

// return current position of file pointer
jlong os::current_file_offset(int fd) {
  return (jlong)::lseek64(fd, (off64_t)0, SEEK_CUR);
}

// move file pointer to the specified offset
jlong os::seek_to_file_offset(int fd, jlong offset) {
  return (jlong)::lseek64(fd, (off64_t)offset, SEEK_SET);
}

// This code originates from JDK's sysAvailable
// from src/solaris/hpi/src/native_threads/src/sys_api_td.c

int os::available(int fd, jlong *bytes) {
  jlong cur, end;
  int mode;
  struct stat64 buf64;

  if (::fstat64(fd, &buf64) >= 0) {
    mode = buf64.st_mode;
    if (S_ISCHR(mode) || S_ISFIFO(mode) || S_ISSOCK(mode)) {
      int n;
      if (::ioctl(fd, FIONREAD, &n) >= 0) {
        *bytes = n;
        return 1;
      }
    }
  }
  if ((cur = ::lseek64(fd, 0L, SEEK_CUR)) == -1) {
    return 0;
  } else if ((end = ::lseek64(fd, 0L, SEEK_END)) == -1) {
    return 0;
  } else if (::lseek64(fd, cur, SEEK_SET) == -1) {
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
  int flags = MAP_PRIVATE;

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

  // Allow anonymous mappings if 'fd' is -1.
  if (fd == -1) {
    flags |= MAP_ANONYMOUS;
  }

  char* mapped_address = (char*)::mmap(addr, (size_t)bytes, prot, flags,
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
  // return user + sys since the cost is the same
  const jlong n = os::thread_cpu_time(Thread::current(), true /* user + sys */);
  assert(n >= 0, "negative CPU time");
  return n;
}

jlong os::thread_cpu_time(Thread* thread) {
  // consistent with what current_thread_cpu_time() returns
  const jlong n = os::thread_cpu_time(thread, true /* user + sys */);
  assert(n >= 0, "negative CPU time");
  return n;
}

jlong os::current_thread_cpu_time(bool user_sys_cpu_time) {
  const jlong n = os::thread_cpu_time(Thread::current(), user_sys_cpu_time);
  assert(n >= 0, "negative CPU time");
  return n;
}

static bool thread_cpu_time_unchecked(Thread* thread, jlong* p_sys_time, jlong* p_user_time) {
  bool error = false;

  jlong sys_time = 0;
  jlong user_time = 0;

  // Reimplemented using getthrds64().
  //
  // Works like this:
  // For the thread in question, get the kernel thread id. Then get the
  // kernel thread statistics using that id.
  //
  // This only works of course when no pthread scheduling is used,
  // i.e. there is a 1:1 relationship to kernel threads.
  // On AIX, see AIXTHREAD_SCOPE variable.

  pthread_t pthtid = thread->osthread()->pthread_id();

  // retrieve kernel thread id for the pthread:
  tid64_t tid = 0;
  struct __pthrdsinfo pinfo;
  // I just love those otherworldly IBM APIs which force me to hand down
  // dummy buffers for stuff I dont care for...
  char dummy[1];
  int dummy_size = sizeof(dummy);
  if (pthread_getthrds_np(&pthtid, PTHRDSINFO_QUERY_TID, &pinfo, sizeof(pinfo),
                          dummy, &dummy_size) == 0) {
    tid = pinfo.__pi_tid;
  } else {
    tty->print_cr("pthread_getthrds_np failed.");
    error = true;
  }

  // retrieve kernel timing info for that kernel thread
  if (!error) {
    struct thrdentry64 thrdentry;
    if (getthrds64(getpid(), &thrdentry, sizeof(thrdentry), &tid, 1) == 1) {
      sys_time = thrdentry.ti_ru.ru_stime.tv_sec * 1000000000LL + thrdentry.ti_ru.ru_stime.tv_usec * 1000LL;
      user_time = thrdentry.ti_ru.ru_utime.tv_sec * 1000000000LL + thrdentry.ti_ru.ru_utime.tv_usec * 1000LL;
    } else {
      tty->print_cr("pthread_getthrds_np failed.");
      error = true;
    }
  }

  if (p_sys_time) {
    *p_sys_time = sys_time;
  }

  if (p_user_time) {
    *p_user_time = user_time;
  }

  if (error) {
    return false;
  }

  return true;
}

jlong os::thread_cpu_time(Thread *thread, bool user_sys_cpu_time) {
  jlong sys_time;
  jlong user_time;

  if (!thread_cpu_time_unchecked(thread, &sys_time, &user_time)) {
    return -1;
  }

  return user_sys_cpu_time ? sys_time + user_time : user_time;
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
  return true;
}

// System loadavg support. Returns -1 if load average cannot be obtained.
// For now just return the system wide load average (no processor sets).
int os::loadavg(double values[], int nelem) {

  guarantee(nelem >= 0 && nelem <= 3, "argument error");
  guarantee(values, "argument error");

  if (os::Aix::on_pase()) {

    // AS/400 PASE: use libo4 porting library
    double v[3] = { 0.0, 0.0, 0.0 };

    if (libo4::get_load_avg(v, v + 1, v + 2)) {
      for (int i = 0; i < nelem; i ++) {
        values[i] = v[i];
      }
      return nelem;
    } else {
      return -1;
    }

  } else {

    // AIX: use libperfstat
    libperfstat::cpuinfo_t ci;
    if (libperfstat::get_cpuinfo(&ci)) {
      for (int i = 0; i < nelem; i++) {
        values[i] = ci.loadavg[i];
      }
    } else {
      return -1;
    }
    return nelem;
  }
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
    trcVerbose("Could not open pause file '%s', continuing immediately.", filename);
  }
}

bool os::is_primordial_thread(void) {
  if (pthread_self() == (pthread_t)1) {
    return true;
  } else {
    return false;
  }
}

// OS recognitions (PASE/AIX, OS level) call this before calling any
// one of Aix::on_pase(), Aix::os_version() static
void os::Aix::initialize_os_info() {

  assert(_on_pase == -1 && _os_version == 0, "already called.");

  struct utsname uts;
  memset(&uts, 0, sizeof(uts));
  strcpy(uts.sysname, "?");
  if (::uname(&uts) == -1) {
    trcVerbose("uname failed (%d)", errno);
    guarantee(0, "Could not determine whether we run on AIX or PASE");
  } else {
    trcVerbose("uname says: sysname \"%s\" version \"%s\" release \"%s\" "
               "node \"%s\" machine \"%s\"\n",
               uts.sysname, uts.version, uts.release, uts.nodename, uts.machine);
    const int major = atoi(uts.version);
    assert(major > 0, "invalid OS version");
    const int minor = atoi(uts.release);
    assert(minor > 0, "invalid OS release");
    _os_version = (major << 24) | (minor << 16);
    char ver_str[20] = {0};
    const char* name_str = "unknown OS";
    if (strcmp(uts.sysname, "OS400") == 0) {
      // We run on AS/400 PASE. We do not support versions older than V5R4M0.
      _on_pase = 1;
      if (os_version_short() < 0x0504) {
        trcVerbose("OS/400 releases older than V5R4M0 not supported.");
        assert(false, "OS/400 release too old.");
      }
      name_str = "OS/400 (pase)";
      jio_snprintf(ver_str, sizeof(ver_str), "%u.%u", major, minor);
    } else if (strcmp(uts.sysname, "AIX") == 0) {
      // We run on AIX. We do not support versions older than AIX 7.1.
      _on_pase = 0;
      // Determine detailed AIX version: Version, Release, Modification, Fix Level.
      odmWrapper::determine_os_kernel_version(&_os_version);
      if (os_version_short() < 0x0701) {
        trcVerbose("AIX releases older than AIX 7.1 are not supported.");
        assert(false, "AIX release too old.");
      }
      name_str = "AIX";
      jio_snprintf(ver_str, sizeof(ver_str), "%u.%u.%u.%u",
                   major, minor, (_os_version >> 8) & 0xFF, _os_version & 0xFF);
    } else {
      assert(false, "%s", name_str);
    }
    trcVerbose("We run on %s %s", name_str, ver_str);
  }

  guarantee(_on_pase != -1 && _os_version, "Could not determine AIX/OS400 release");
} // end: os::Aix::initialize_os_info()

// Scan environment for important settings which might effect the VM.
// Trace out settings. Warn about invalid settings and/or correct them.
//
// Must run after os::Aix::initialue_os_info().
void os::Aix::scan_environment() {

  char* p;
  int rc;

  // Warn explicity if EXTSHM=ON is used. That switch changes how
  // System V shared memory behaves. One effect is that page size of
  // shared memory cannot be change dynamically, effectivly preventing
  // large pages from working.
  // This switch was needed on AIX 32bit, but on AIX 64bit the general
  // recommendation is (in OSS notes) to switch it off.
  p = ::getenv("EXTSHM");
  trcVerbose("EXTSHM=%s.", p ? p : "<unset>");
  if (p && strcasecmp(p, "ON") == 0) {
    _extshm = 1;
    trcVerbose("*** Unsupported mode! Please remove EXTSHM from your environment! ***");
    if (!AllowExtshm) {
      // We allow under certain conditions the user to continue. However, we want this
      // to be a fatal error by default. On certain AIX systems, leaving EXTSHM=ON means
      // that the VM is not able to allocate 64k pages for the heap.
      // We do not want to run with reduced performance.
      vm_exit_during_initialization("EXTSHM is ON. Please remove EXTSHM from your environment.");
    }
  } else {
    _extshm = 0;
  }

  // SPEC1170 behaviour: will change the behaviour of a number of POSIX APIs.
  // Not tested, not supported.
  //
  // Note that it might be worth the trouble to test and to require it, if only to
  // get useful return codes for mprotect.
  //
  // Note: Setting XPG_SUS_ENV in the process is too late. Must be set earlier (before
  // exec() ? before loading the libjvm ? ....)
  p = ::getenv("XPG_SUS_ENV");
  trcVerbose("XPG_SUS_ENV=%s.", p ? p : "<unset>");
  if (p && strcmp(p, "ON") == 0) {
    _xpg_sus_mode = 1;
    trcVerbose("Unsupported setting: XPG_SUS_ENV=ON");
    // This is not supported. Worst of all, it changes behaviour of mmap MAP_FIXED to
    // clobber address ranges. If we ever want to support that, we have to do some
    // testing first.
    guarantee(false, "XPG_SUS_ENV=ON not supported");
  } else {
    _xpg_sus_mode = 0;
  }

  if (os::Aix::on_pase()) {
    p = ::getenv("QIBM_MULTI_THREADED");
    trcVerbose("QIBM_MULTI_THREADED=%s.", p ? p : "<unset>");
  }

  p = ::getenv("LDR_CNTRL");
  trcVerbose("LDR_CNTRL=%s.", p ? p : "<unset>");
  if (os::Aix::on_pase() && os::Aix::os_version_short() == 0x0701) {
    if (p && ::strstr(p, "TEXTPSIZE")) {
      trcVerbose("*** WARNING - LDR_CNTRL contains TEXTPSIZE. "
        "you may experience hangs or crashes on OS/400 V7R1.");
    }
  }

  p = ::getenv("AIXTHREAD_GUARDPAGES");
  trcVerbose("AIXTHREAD_GUARDPAGES=%s.", p ? p : "<unset>");

} // end: os::Aix::scan_environment()

// PASE: initialize the libo4 library (PASE porting library).
void os::Aix::initialize_libo4() {
  guarantee(os::Aix::on_pase(), "OS/400 only.");
  if (!libo4::init()) {
    trcVerbose("libo4 initialization failed.");
    assert(false, "libo4 initialization failed");
  } else {
    trcVerbose("libo4 initialized.");
  }
}

// AIX: initialize the libperfstat library.
void os::Aix::initialize_libperfstat() {
  assert(os::Aix::on_aix(), "AIX only");
  if (!libperfstat::init()) {
    trcVerbose("libperfstat initialization failed.");
    assert(false, "libperfstat initialization failed");
  } else {
    trcVerbose("libperfstat initialized.");
  }
}

/////////////////////////////////////////////////////////////////////////////
// thread stack

// Get the current stack base from the OS (actually, the pthread library).
// Note: usually not page aligned.
address os::current_stack_base() {
  AixMisc::stackbounds_t bounds;
  bool rc = AixMisc::query_stack_bounds_for_current_thread(&bounds);
  guarantee(rc, "Unable to retrieve stack bounds.");
  return bounds.base;
}

// Get the current stack size from the OS (actually, the pthread library).
// Returned size is such that (base - size) is always aligned to page size.
size_t os::current_stack_size() {
  AixMisc::stackbounds_t bounds;
  bool rc = AixMisc::query_stack_bounds_for_current_thread(&bounds);
  guarantee(rc, "Unable to retrieve stack bounds.");
  // Align the returned stack size such that the stack low address
  // is aligned to page size (Note: base is usually not and we do not care).
  // We need to do this because caller code will assume stack low address is
  // page aligned and will place guard pages without checking.
  address low = bounds.base - bounds.size;
  address low_aligned = (address)align_up(low, os::vm_page_size());
  size_t s = bounds.base - low_aligned;
  return s;
}

// Get the default path to the core file
// Returns the length of the string
int os::get_core_path(char* buffer, size_t bufferSize) {
  const char* p = get_current_directory(buffer, bufferSize);

  if (p == NULL) {
    assert(p != NULL, "failed to get current directory");
    return 0;
  }

  jio_snprintf(buffer, bufferSize, "%s/core or core.%d",
                                               p, current_process_id());

  return strlen(buffer);
}

bool os::start_debugging(char *buf, int buflen) {
  int len = (int)strlen(buf);
  char *p = &buf[len];

  jio_snprintf(p, buflen -len,
                 "\n\n"
                 "Do you want to debug the problem?\n\n"
                 "To debug, run 'dbx -a %d'; then switch to thread tid " INTX_FORMAT ", k-tid " INTX_FORMAT "\n"
                 "Enter 'yes' to launch dbx automatically (PATH must include dbx)\n"
                 "Otherwise, press RETURN to abort...",
                 os::current_process_id(),
                 os::current_thread_id(), thread_self());

  bool yes = os::message_box("Unexpected Error", buf);

  if (yes) {
    // yes, user asked VM to launch debugger
    jio_snprintf(buf, buflen, "dbx -a %d", os::current_process_id());

    os::fork_and_exec(buf);
    yes = false;
  }
  return yes;
}

static inline time_t get_mtime(const char* filename) {
  struct stat st;
  int ret = os::stat(filename, &st);
  assert(ret == 0, "failed to stat() file '%s': %s", filename, os::strerror(errno));
  return st.st_mtime;
}

int os::compare_file_modified_times(const char* file1, const char* file2) {
  time_t t1 = get_mtime(file1);
  time_t t2 = get_mtime(file2);
  return t1 - t2;
}

bool os::supports_map_sync() {
  return false;
}

void os::print_memory_mappings(char* addr, size_t bytes, outputStream* st) {}
