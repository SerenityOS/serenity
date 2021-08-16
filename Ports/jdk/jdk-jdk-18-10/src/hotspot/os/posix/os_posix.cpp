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


#include "jvm.h"
#ifdef LINUX
#include "classfile/classLoader.hpp"
#endif
#include "jvmtifiles/jvmti.h"
#include "logging/log.hpp"
#include "memory/allocation.inline.hpp"
#include "os_posix.inline.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/osThread.hpp"
#include "utilities/globalDefinitions.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "services/attachListener.hpp"
#include "services/memTracker.hpp"
#include "runtime/arguments.hpp"
#include "runtime/atomic.hpp"
#include "runtime/java.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/perfMemory.hpp"
#include "utilities/align.hpp"
#include "utilities/events.hpp"
#include "utilities/formatBuffer.hpp"
#include "utilities/macros.hpp"
#include "utilities/vmError.hpp"

#include <dirent.h>
#include <dlfcn.h>
#include <grp.h>
#include <netdb.h>
#include <pwd.h>
#include <pthread.h>
#include <signal.h>
#include <sys/mman.h>
#include <sys/resource.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/utsname.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>
#include <utmpx.h>

#ifdef __APPLE__
  #include <crt_externs.h>
#endif

#define ROOT_UID 0

#ifndef MAP_ANONYMOUS
  #define MAP_ANONYMOUS MAP_ANON
#endif

#define check_with_errno(check_type, cond, msg)                             \
  do {                                                                      \
    int err = errno;                                                        \
    check_type(cond, "%s; error='%s' (errno=%s)", msg, os::strerror(err),   \
               os::errno_name(err));                                        \
} while (false)

#define assert_with_errno(cond, msg)    check_with_errno(assert, cond, msg)
#define guarantee_with_errno(cond, msg) check_with_errno(guarantee, cond, msg)

// Check core dump limit and report possible place where core can be found
void os::check_dump_limit(char* buffer, size_t bufferSize) {
  if (!FLAG_IS_DEFAULT(CreateCoredumpOnCrash) && !CreateCoredumpOnCrash) {
    jio_snprintf(buffer, bufferSize, "CreateCoredumpOnCrash is disabled from command line");
    VMError::record_coredump_status(buffer, false);
    return;
  }

  int n;
  struct rlimit rlim;
  bool success;

  char core_path[PATH_MAX];
  n = get_core_path(core_path, PATH_MAX);

  if (n <= 0) {
    jio_snprintf(buffer, bufferSize, "core.%d (may not exist)", current_process_id());
    success = true;
#ifdef LINUX
  } else if (core_path[0] == '"') { // redirect to user process
    jio_snprintf(buffer, bufferSize, "Core dumps may be processed with %s", core_path);
    success = true;
#endif
  } else if (getrlimit(RLIMIT_CORE, &rlim) != 0) {
    jio_snprintf(buffer, bufferSize, "%s (may not exist)", core_path);
    success = true;
  } else {
    switch(rlim.rlim_cur) {
      case RLIM_INFINITY:
        jio_snprintf(buffer, bufferSize, "%s", core_path);
        success = true;
        break;
      case 0:
        jio_snprintf(buffer, bufferSize, "Core dumps have been disabled. To enable core dumping, try \"ulimit -c unlimited\" before starting Java again");
        success = false;
        break;
      default:
        jio_snprintf(buffer, bufferSize, "%s (max size " UINT64_FORMAT " kB). To ensure a full core dump, try \"ulimit -c unlimited\" before starting Java again", core_path, uint64_t(rlim.rlim_cur) / 1024);
        success = true;
        break;
    }
  }

  VMError::record_coredump_status(buffer, success);
}

int os::get_native_stack(address* stack, int frames, int toSkip) {
  int frame_idx = 0;
  int num_of_frames;  // number of frames captured
  frame fr = os::current_frame();
  while (fr.pc() && frame_idx < frames) {
    if (toSkip > 0) {
      toSkip --;
    } else {
      stack[frame_idx ++] = fr.pc();
    }
    if (fr.fp() == NULL || fr.cb() != NULL ||
        fr.sender_pc() == NULL || os::is_first_C_frame(&fr)) break;

    if (fr.sender_pc() && !os::is_first_C_frame(&fr)) {
      fr = os::get_sender_for_C_frame(&fr);
    } else {
      break;
    }
  }
  num_of_frames = frame_idx;
  for (; frame_idx < frames; frame_idx ++) {
    stack[frame_idx] = NULL;
  }

  return num_of_frames;
}


bool os::unsetenv(const char* name) {
  assert(name != NULL, "Null pointer");
  return (::unsetenv(name) == 0);
}

int os::get_last_error() {
  return errno;
}

size_t os::lasterror(char *buf, size_t len) {
  if (errno == 0)  return 0;

  const char *s = os::strerror(errno);
  size_t n = ::strlen(s);
  if (n >= len) {
    n = len - 1;
  }
  ::strncpy(buf, s, n);
  buf[n] = '\0';
  return n;
}

void os::wait_for_keypress_at_exit(void) {
  // don't do anything on posix platforms
  return;
}

int os::create_file_for_heap(const char* dir) {
  int fd;

#if defined(LINUX) && defined(O_TMPFILE)
  char* native_dir = os::strdup(dir);
  if (native_dir == NULL) {
    vm_exit_during_initialization(err_msg("strdup failed during creation of backing file for heap (%s)", os::strerror(errno)));
    return -1;
  }
  os::native_path(native_dir);
  fd = os::open(dir, O_TMPFILE | O_RDWR, S_IRUSR | S_IWUSR);
  os::free(native_dir);

  if (fd == -1)
#endif
  {
    const char name_template[] = "/jvmheap.XXXXXX";

    size_t fullname_len = strlen(dir) + strlen(name_template);
    char *fullname = (char*)os::malloc(fullname_len + 1, mtInternal);
    if (fullname == NULL) {
      vm_exit_during_initialization(err_msg("Malloc failed during creation of backing file for heap (%s)", os::strerror(errno)));
      return -1;
    }
    int n = snprintf(fullname, fullname_len + 1, "%s%s", dir, name_template);
    assert((size_t)n == fullname_len, "Unexpected number of characters in string");

    os::native_path(fullname);

    // create a new file.
    fd = mkstemp(fullname);

    if (fd < 0) {
      warning("Could not create file for heap with template %s", fullname);
      os::free(fullname);
      return -1;
    } else {
      // delete the name from the filesystem. When 'fd' is closed, the file (and space) will be deleted.
      int ret = unlink(fullname);
      assert_with_errno(ret == 0, "unlink returned error");
    }

    os::free(fullname);
  }

  return fd;
}

static char* reserve_mmapped_memory(size_t bytes, char* requested_addr) {
  char * addr;
  int flags = MAP_PRIVATE NOT_AIX( | MAP_NORESERVE ) | MAP_ANONYMOUS;
  if (requested_addr != NULL) {
    assert((uintptr_t)requested_addr % os::vm_page_size() == 0, "Requested address should be aligned to OS page size");
    flags |= MAP_FIXED;
  }

  // Map reserved/uncommitted pages PROT_NONE so we fail early if we
  // touch an uncommitted page. Otherwise, the read/write might
  // succeed if we have enough swap space to back the physical page.
  addr = (char*)::mmap(requested_addr, bytes, PROT_NONE,
                       flags, -1, 0);

  if (addr != MAP_FAILED) {
    MemTracker::record_virtual_memory_reserve((address)addr, bytes, CALLER_PC);
    return addr;
  }
  return NULL;
}

static int util_posix_fallocate(int fd, off_t offset, off_t len) {
#ifdef __APPLE__
  fstore_t store = { F_ALLOCATECONTIG, F_PEOFPOSMODE, 0, len };
  // First we try to get a continuous chunk of disk space
  int ret = fcntl(fd, F_PREALLOCATE, &store);
  if (ret == -1) {
    // Maybe we are too fragmented, try to allocate non-continuous range
    store.fst_flags = F_ALLOCATEALL;
    ret = fcntl(fd, F_PREALLOCATE, &store);
  }
  if(ret != -1) {
    return ftruncate(fd, len);
  }
  return -1;
#else
  return posix_fallocate(fd, offset, len);
#endif
}

// Map the given address range to the provided file descriptor.
char* os::map_memory_to_file(char* base, size_t size, int fd) {
  assert(fd != -1, "File descriptor is not valid");

  // allocate space for the file
  int ret = util_posix_fallocate(fd, 0, (off_t)size);
  if (ret != 0) {
    vm_exit_during_initialization(err_msg("Error in mapping Java heap at the given filesystem directory. error(%d)", ret));
    return NULL;
  }

  int prot = PROT_READ | PROT_WRITE;
  int flags = MAP_SHARED;
  if (base != NULL) {
    flags |= MAP_FIXED;
  }
  char* addr = (char*)mmap(base, size, prot, flags, fd, 0);

  if (addr == MAP_FAILED) {
    warning("Failed mmap to file. (%s)", os::strerror(errno));
    return NULL;
  }
  if (base != NULL && addr != base) {
    if (!os::release_memory(addr, size)) {
      warning("Could not release memory on unsuccessful file mapping");
    }
    return NULL;
  }
  return addr;
}

char* os::replace_existing_mapping_with_file_mapping(char* base, size_t size, int fd) {
  assert(fd != -1, "File descriptor is not valid");
  assert(base != NULL, "Base cannot be NULL");

  return map_memory_to_file(base, size, fd);
}

static size_t calculate_aligned_extra_size(size_t size, size_t alignment) {
  assert((alignment & (os::vm_allocation_granularity() - 1)) == 0,
      "Alignment must be a multiple of allocation granularity (page size)");
  assert((size & (alignment -1)) == 0, "size must be 'alignment' aligned");

  size_t extra_size = size + alignment;
  assert(extra_size >= size, "overflow, size is too large to allow alignment");
  return extra_size;
}

// After a bigger chunk was mapped, unmaps start and end parts to get the requested alignment.
static char* chop_extra_memory(size_t size, size_t alignment, char* extra_base, size_t extra_size) {
  // Do manual alignment
  char* aligned_base = align_up(extra_base, alignment);

  // [  |                                       |  ]
  // ^ extra_base
  //    ^ extra_base + begin_offset == aligned_base
  //     extra_base + begin_offset + size       ^
  //                       extra_base + extra_size ^
  // |<>| == begin_offset
  //                              end_offset == |<>|
  size_t begin_offset = aligned_base - extra_base;
  size_t end_offset = (extra_base + extra_size) - (aligned_base + size);

  if (begin_offset > 0) {
      os::release_memory(extra_base, begin_offset);
  }

  if (end_offset > 0) {
      os::release_memory(extra_base + begin_offset + size, end_offset);
  }

  return aligned_base;
}

// Multiple threads can race in this code, and can remap over each other with MAP_FIXED,
// so on posix, unmap the section at the start and at the end of the chunk that we mapped
// rather than unmapping and remapping the whole chunk to get requested alignment.
char* os::reserve_memory_aligned(size_t size, size_t alignment, bool exec) {
  size_t extra_size = calculate_aligned_extra_size(size, alignment);
  char* extra_base = os::reserve_memory(extra_size, exec);
  if (extra_base == NULL) {
    return NULL;
  }
  return chop_extra_memory(size, alignment, extra_base, extra_size);
}

char* os::map_memory_to_file_aligned(size_t size, size_t alignment, int file_desc) {
  size_t extra_size = calculate_aligned_extra_size(size, alignment);
  // For file mapping, we do not call os:map_memory_to_file(size,fd) since:
  // - we later chop away parts of the mapping using os::release_memory and that could fail if the
  //   original mmap call had been tied to an fd.
  // - The memory API os::reserve_memory uses is an implementation detail. It may (and usually is)
  //   mmap but it also may System V shared memory which cannot be uncommitted as a whole, so
  //   chopping off and unmapping excess bits back and front (see below) would not work.
  char* extra_base = reserve_mmapped_memory(extra_size, NULL);
  if (extra_base == NULL) {
    return NULL;
  }
  char* aligned_base = chop_extra_memory(size, alignment, extra_base, extra_size);
  // After we have an aligned address, we can replace anonymous mapping with file mapping
  if (replace_existing_mapping_with_file_mapping(aligned_base, size, file_desc) == NULL) {
    vm_exit_during_initialization(err_msg("Error in mapping Java heap at the given filesystem directory"));
  }
  MemTracker::record_virtual_memory_commit((address)aligned_base, size, CALLER_PC);
  return aligned_base;
}

int os::vsnprintf(char* buf, size_t len, const char* fmt, va_list args) {
  // All supported POSIX platforms provide C99 semantics.
  int result = ::vsnprintf(buf, len, fmt, args);
  // If an encoding error occurred (result < 0) then it's not clear
  // whether the buffer is NUL terminated, so ensure it is.
  if ((result < 0) && (len > 0)) {
    buf[len - 1] = '\0';
  }
  return result;
}

int os::get_fileno(FILE* fp) {
  return NOT_AIX(::)fileno(fp);
}

struct tm* os::gmtime_pd(const time_t* clock, struct tm*  res) {
  return gmtime_r(clock, res);
}

void os::Posix::print_load_average(outputStream* st) {
  st->print("load average: ");
  double loadavg[3];
  int res = os::loadavg(loadavg, 3);
  if (res != -1) {
    st->print("%0.02f %0.02f %0.02f", loadavg[0], loadavg[1], loadavg[2]);
  } else {
    st->print(" Unavailable");
  }
  st->cr();
}

// boot/uptime information;
// unfortunately it does not work on macOS and Linux because the utx chain has no entry
// for reboot at least on my test machines
void os::Posix::print_uptime_info(outputStream* st) {
  int bootsec = -1;
  int currsec = time(NULL);
  struct utmpx* ent;
  setutxent();
  while ((ent = getutxent())) {
    if (!strcmp("system boot", ent->ut_line)) {
      bootsec = ent->ut_tv.tv_sec;
      break;
    }
  }

  if (bootsec != -1) {
    os::print_dhm(st, "OS uptime:", (long) (currsec-bootsec));
  }
}

static void print_rlimit(outputStream* st, const char* msg,
                         int resource, bool output_k = false) {
  struct rlimit rlim;

  st->print(" %s ", msg);
  int res = getrlimit(resource, &rlim);
  if (res == -1) {
    st->print("could not obtain value");
  } else {
    // soft limit
    if (rlim.rlim_cur == RLIM_INFINITY) { st->print("infinity"); }
    else {
      if (output_k) { st->print(UINT64_FORMAT "k", uint64_t(rlim.rlim_cur) / 1024); }
      else { st->print(UINT64_FORMAT, uint64_t(rlim.rlim_cur)); }
    }
    // hard limit
    st->print("/");
    if (rlim.rlim_max == RLIM_INFINITY) { st->print("infinity"); }
    else {
      if (output_k) { st->print(UINT64_FORMAT "k", uint64_t(rlim.rlim_max) / 1024); }
      else { st->print(UINT64_FORMAT, uint64_t(rlim.rlim_max)); }
    }
  }
}

void os::Posix::print_rlimit_info(outputStream* st) {
  st->print("rlimit (soft/hard):");
  print_rlimit(st, "STACK", RLIMIT_STACK, true);
  print_rlimit(st, ", CORE", RLIMIT_CORE, true);

#if defined(AIX)
  st->print(", NPROC ");
  st->print("%d", sysconf(_SC_CHILD_MAX));

  print_rlimit(st, ", THREADS", RLIMIT_THREADS);
#else
  print_rlimit(st, ", NPROC", RLIMIT_NPROC);
#endif

  print_rlimit(st, ", NOFILE", RLIMIT_NOFILE);
  print_rlimit(st, ", AS", RLIMIT_AS, true);
  print_rlimit(st, ", CPU", RLIMIT_CPU);
  print_rlimit(st, ", DATA", RLIMIT_DATA, true);

  // maximum size of files that the process may create
  print_rlimit(st, ", FSIZE", RLIMIT_FSIZE, true);

#if defined(LINUX) || defined(__APPLE__)
  // maximum number of bytes of memory that may be locked into RAM
  // (rounded down to the nearest  multiple of system pagesize)
  print_rlimit(st, ", MEMLOCK", RLIMIT_MEMLOCK, true);
#endif

  // MacOS; The maximum size (in bytes) to which a process's resident set size may grow.
#if defined(__APPLE__)
  print_rlimit(st, ", RSS", RLIMIT_RSS, true);
#endif

  st->cr();
}

void os::Posix::print_uname_info(outputStream* st) {
  // kernel
  st->print("uname: ");
  struct utsname name;
  uname(&name);
  st->print("%s ", name.sysname);
#ifdef ASSERT
  st->print("%s ", name.nodename);
#endif
  st->print("%s ", name.release);
  st->print("%s ", name.version);
  st->print("%s", name.machine);
  st->cr();
}

void os::Posix::print_umask(outputStream* st, mode_t umsk) {
  st->print((umsk & S_IRUSR) ? "r" : "-");
  st->print((umsk & S_IWUSR) ? "w" : "-");
  st->print((umsk & S_IXUSR) ? "x" : "-");
  st->print((umsk & S_IRGRP) ? "r" : "-");
  st->print((umsk & S_IWGRP) ? "w" : "-");
  st->print((umsk & S_IXGRP) ? "x" : "-");
  st->print((umsk & S_IROTH) ? "r" : "-");
  st->print((umsk & S_IWOTH) ? "w" : "-");
  st->print((umsk & S_IXOTH) ? "x" : "-");
}

void os::Posix::print_user_info(outputStream* st) {
  unsigned id = (unsigned) ::getuid();
  st->print("uid  : %u ", id);
  id = (unsigned) ::geteuid();
  st->print("euid : %u ", id);
  id = (unsigned) ::getgid();
  st->print("gid  : %u ", id);
  id = (unsigned) ::getegid();
  st->print_cr("egid : %u", id);
  st->cr();

  mode_t umsk = ::umask(0);
  ::umask(umsk);
  st->print("umask: %04o (", (unsigned) umsk);
  print_umask(st, umsk);
  st->print_cr(")");
  st->cr();
}


bool os::get_host_name(char* buf, size_t buflen) {
  struct utsname name;
  uname(&name);
  jio_snprintf(buf, buflen, "%s", name.nodename);
  return true;
}

#ifndef _LP64
// Helper, on 32bit, for os::has_allocatable_memory_limit
static bool is_allocatable(size_t s) {
  if (s < 2 * G) {
    return true;
  }
  // Use raw anonymous mmap here; no need to go through any
  // of our reservation layers. We will unmap right away.
  void* p = ::mmap(NULL, s, PROT_NONE,
                   MAP_PRIVATE | MAP_NORESERVE | MAP_ANONYMOUS, -1, 0);
  if (p == MAP_FAILED) {
    return false;
  } else {
    ::munmap(p, s);
    return true;
  }
}
#endif // !_LP64


bool os::has_allocatable_memory_limit(size_t* limit) {
  struct rlimit rlim;
  int getrlimit_res = getrlimit(RLIMIT_AS, &rlim);
  // if there was an error when calling getrlimit, assume that there is no limitation
  // on virtual memory.
  bool result;
  if ((getrlimit_res != 0) || (rlim.rlim_cur == RLIM_INFINITY)) {
    result = false;
  } else {
    *limit = (size_t)rlim.rlim_cur;
    result = true;
  }
#ifdef _LP64
  return result;
#else
  // arbitrary virtual space limit for 32 bit Unices found by testing. If
  // getrlimit above returned a limit, bound it with this limit. Otherwise
  // directly use it.
  const size_t max_virtual_limit = 3800*M;
  if (result) {
    *limit = MIN2(*limit, max_virtual_limit);
  } else {
    *limit = max_virtual_limit;
  }

  // bound by actually allocatable memory. The algorithm uses two bounds, an
  // upper and a lower limit. The upper limit is the current highest amount of
  // memory that could not be allocated, the lower limit is the current highest
  // amount of memory that could be allocated.
  // The algorithm iteratively refines the result by halving the difference
  // between these limits, updating either the upper limit (if that value could
  // not be allocated) or the lower limit (if the that value could be allocated)
  // until the difference between these limits is "small".

  // the minimum amount of memory we care about allocating.
  const size_t min_allocation_size = M;

  size_t upper_limit = *limit;

  // first check a few trivial cases
  if (is_allocatable(upper_limit) || (upper_limit <= min_allocation_size)) {
    *limit = upper_limit;
  } else if (!is_allocatable(min_allocation_size)) {
    // we found that not even min_allocation_size is allocatable. Return it
    // anyway. There is no point to search for a better value any more.
    *limit = min_allocation_size;
  } else {
    // perform the binary search.
    size_t lower_limit = min_allocation_size;
    while ((upper_limit - lower_limit) > min_allocation_size) {
      size_t temp_limit = ((upper_limit - lower_limit) / 2) + lower_limit;
      temp_limit = align_down(temp_limit, min_allocation_size);
      if (is_allocatable(temp_limit)) {
        lower_limit = temp_limit;
      } else {
        upper_limit = temp_limit;
      }
    }
    *limit = lower_limit;
  }
  return true;
#endif
}

void os::dll_unload(void *lib) {
  ::dlclose(lib);
}

jlong os::lseek(int fd, jlong offset, int whence) {
  return (jlong) BSD_ONLY(::lseek) NOT_BSD(::lseek64)(fd, offset, whence);
}

int os::fsync(int fd) {
  return ::fsync(fd);
}

int os::ftruncate(int fd, jlong length) {
   return BSD_ONLY(::ftruncate) NOT_BSD(::ftruncate64)(fd, length);
}

const char* os::get_current_directory(char *buf, size_t buflen) {
  return getcwd(buf, buflen);
}

FILE* os::open(int fd, const char* mode) {
  return ::fdopen(fd, mode);
}

size_t os::write(int fd, const void *buf, unsigned int nBytes) {
  size_t res;
  RESTARTABLE((size_t) ::write(fd, buf, (size_t) nBytes), res);
  return res;
}

ssize_t os::read_at(int fd, void *buf, unsigned int nBytes, jlong offset) {
  return ::pread(fd, buf, nBytes, offset);
}

int os::close(int fd) {
  return ::close(fd);
}

void os::flockfile(FILE* fp) {
  ::flockfile(fp);
}

void os::funlockfile(FILE* fp) {
  ::funlockfile(fp);
}

DIR* os::opendir(const char* dirname) {
  assert(dirname != NULL, "just checking");
  return ::opendir(dirname);
}

struct dirent* os::readdir(DIR* dirp) {
  assert(dirp != NULL, "just checking");
  return ::readdir(dirp);
}

int os::closedir(DIR *dirp) {
  assert(dirp != NULL, "just checking");
  return ::closedir(dirp);
}

int os::socket_close(int fd) {
  return ::close(fd);
}

int os::socket(int domain, int type, int protocol) {
  return ::socket(domain, type, protocol);
}

int os::recv(int fd, char* buf, size_t nBytes, uint flags) {
  RESTARTABLE_RETURN_INT(::recv(fd, buf, nBytes, flags));
}

int os::send(int fd, char* buf, size_t nBytes, uint flags) {
  RESTARTABLE_RETURN_INT(::send(fd, buf, nBytes, flags));
}

int os::raw_send(int fd, char* buf, size_t nBytes, uint flags) {
  return os::send(fd, buf, nBytes, flags);
}

int os::connect(int fd, struct sockaddr* him, socklen_t len) {
  RESTARTABLE_RETURN_INT(::connect(fd, him, len));
}

struct hostent* os::get_host_by_name(char* name) {
  return ::gethostbyname(name);
}

void os::exit(int num) {
  ::exit(num);
}

// Builds a platform dependent Agent_OnLoad_<lib_name> function name
// which is used to find statically linked in agents.
// Parameters:
//            sym_name: Symbol in library we are looking for
//            lib_name: Name of library to look in, NULL for shared libs.
//            is_absolute_path == true if lib_name is absolute path to agent
//                                     such as "/a/b/libL.so"
//            == false if only the base name of the library is passed in
//               such as "L"
char* os::build_agent_function_name(const char *sym_name, const char *lib_name,
                                    bool is_absolute_path) {
  char *agent_entry_name;
  size_t len;
  size_t name_len;
  size_t prefix_len = strlen(JNI_LIB_PREFIX);
  size_t suffix_len = strlen(JNI_LIB_SUFFIX);
  const char *start;

  if (lib_name != NULL) {
    name_len = strlen(lib_name);
    if (is_absolute_path) {
      // Need to strip path, prefix and suffix
      if ((start = strrchr(lib_name, *os::file_separator())) != NULL) {
        lib_name = ++start;
      }
      if (strlen(lib_name) <= (prefix_len + suffix_len)) {
        return NULL;
      }
      lib_name += prefix_len;
      name_len = strlen(lib_name) - suffix_len;
    }
  }
  len = (lib_name != NULL ? name_len : 0) + strlen(sym_name) + 2;
  agent_entry_name = NEW_C_HEAP_ARRAY_RETURN_NULL(char, len, mtThread);
  if (agent_entry_name == NULL) {
    return NULL;
  }
  strcpy(agent_entry_name, sym_name);
  if (lib_name != NULL) {
    strcat(agent_entry_name, "_");
    strncat(agent_entry_name, lib_name, name_len);
  }
  return agent_entry_name;
}


void os::naked_short_nanosleep(jlong ns) {
  struct timespec req;
  assert(ns > -1 && ns < NANOUNITS, "Un-interruptable sleep, short time use only");
  req.tv_sec = 0;
  req.tv_nsec = ns;
  ::nanosleep(&req, NULL);
  return;
}

void os::naked_short_sleep(jlong ms) {
  assert(ms < MILLIUNITS, "Un-interruptable sleep, short time use only");
  os::naked_short_nanosleep(millis_to_nanos(ms));
  return;
}

char* os::Posix::describe_pthread_attr(char* buf, size_t buflen, const pthread_attr_t* attr) {
  size_t stack_size = 0;
  size_t guard_size = 0;
  int detachstate = 0;
  pthread_attr_getstacksize(attr, &stack_size);
  pthread_attr_getguardsize(attr, &guard_size);
  // Work around linux NPTL implementation error, see also os::create_thread() in os_linux.cpp.
  LINUX_ONLY(stack_size -= guard_size);
  pthread_attr_getdetachstate(attr, &detachstate);
  jio_snprintf(buf, buflen, "stacksize: " SIZE_FORMAT "k, guardsize: " SIZE_FORMAT "k, %s",
    stack_size / 1024, guard_size / 1024,
    (detachstate == PTHREAD_CREATE_DETACHED ? "detached" : "joinable"));
  return buf;
}

char* os::Posix::realpath(const char* filename, char* outbuf, size_t outbuflen) {

  if (filename == NULL || outbuf == NULL || outbuflen < 1) {
    assert(false, "os::Posix::realpath: invalid arguments.");
    errno = EINVAL;
    return NULL;
  }

  char* result = NULL;

  // This assumes platform realpath() is implemented according to POSIX.1-2008.
  // POSIX.1-2008 allows to specify NULL for the output buffer, in which case
  // output buffer is dynamically allocated and must be ::free()'d by the caller.
  char* p = ::realpath(filename, NULL);
  if (p != NULL) {
    if (strlen(p) < outbuflen) {
      strcpy(outbuf, p);
      result = outbuf;
    } else {
      errno = ENAMETOOLONG;
    }
    ::free(p); // *not* os::free
  } else {
    // Fallback for platforms struggling with modern Posix standards (AIX 5.3, 6.1). If realpath
    // returns EINVAL, this may indicate that realpath is not POSIX.1-2008 compatible and
    // that it complains about the NULL we handed down as user buffer.
    // In this case, use the user provided buffer but at least check whether realpath caused
    // a memory overwrite.
    if (errno == EINVAL) {
      outbuf[outbuflen - 1] = '\0';
      p = ::realpath(filename, outbuf);
      if (p != NULL) {
        guarantee(outbuf[outbuflen - 1] == '\0', "realpath buffer overwrite detected.");
        result = p;
      }
    }
  }
  return result;

}

int os::stat(const char *path, struct stat *sbuf) {
  return ::stat(path, sbuf);
}

char * os::native_path(char *path) {
  return path;
}

bool os::same_files(const char* file1, const char* file2) {
  if (file1 == nullptr && file2 == nullptr) {
    return true;
  }

  if (file1 == nullptr || file2 == nullptr) {
    return false;
  }

  if (strcmp(file1, file2) == 0) {
    return true;
  }

  bool is_same = false;
  struct stat st1;
  struct stat st2;

  if (os::stat(file1, &st1) < 0) {
    return false;
  }

  if (os::stat(file2, &st2) < 0) {
    return false;
  }

  if (st1.st_dev == st2.st_dev && st1.st_ino == st2.st_ino) {
    // same files
    is_same = true;
  }
  return is_same;
}

// Check minimum allowable stack sizes for thread creation and to initialize
// the java system classes, including StackOverflowError - depends on page
// size.
// The space needed for frames during startup is platform dependent. It
// depends on word size, platform calling conventions, C frame layout and
// interpreter/C1/C2 design decisions. Therefore this is given in a
// platform (os/cpu) dependent constant.
// To this, space for guard mechanisms is added, which depends on the
// page size which again depends on the concrete system the VM is running
// on. Space for libc guard pages is not included in this size.
jint os::Posix::set_minimum_stack_sizes() {
  size_t os_min_stack_allowed = PTHREAD_STACK_MIN;

  _java_thread_min_stack_allowed = _java_thread_min_stack_allowed +
                                   StackOverflow::stack_guard_zone_size() +
                                   StackOverflow::stack_shadow_zone_size();

  _java_thread_min_stack_allowed = align_up(_java_thread_min_stack_allowed, vm_page_size());
  _java_thread_min_stack_allowed = MAX2(_java_thread_min_stack_allowed, os_min_stack_allowed);

  size_t stack_size_in_bytes = ThreadStackSize * K;
  if (stack_size_in_bytes != 0 &&
      stack_size_in_bytes < _java_thread_min_stack_allowed) {
    // The '-Xss' and '-XX:ThreadStackSize=N' options both set
    // ThreadStackSize so we go with "Java thread stack size" instead
    // of "ThreadStackSize" to be more friendly.
    tty->print_cr("\nThe Java thread stack size specified is too small. "
                  "Specify at least " SIZE_FORMAT "k",
                  _java_thread_min_stack_allowed / K);
    return JNI_ERR;
  }

  // Make the stack size a multiple of the page size so that
  // the yellow/red zones can be guarded.
  JavaThread::set_stack_size_at_create(align_up(stack_size_in_bytes, vm_page_size()));

  // Reminder: a compiler thread is a Java thread.
  _compiler_thread_min_stack_allowed = _compiler_thread_min_stack_allowed +
                                       StackOverflow::stack_guard_zone_size() +
                                       StackOverflow::stack_shadow_zone_size();

  _compiler_thread_min_stack_allowed = align_up(_compiler_thread_min_stack_allowed, vm_page_size());
  _compiler_thread_min_stack_allowed = MAX2(_compiler_thread_min_stack_allowed, os_min_stack_allowed);

  stack_size_in_bytes = CompilerThreadStackSize * K;
  if (stack_size_in_bytes != 0 &&
      stack_size_in_bytes < _compiler_thread_min_stack_allowed) {
    tty->print_cr("\nThe CompilerThreadStackSize specified is too small. "
                  "Specify at least " SIZE_FORMAT "k",
                  _compiler_thread_min_stack_allowed / K);
    return JNI_ERR;
  }

  _vm_internal_thread_min_stack_allowed = align_up(_vm_internal_thread_min_stack_allowed, vm_page_size());
  _vm_internal_thread_min_stack_allowed = MAX2(_vm_internal_thread_min_stack_allowed, os_min_stack_allowed);

  stack_size_in_bytes = VMThreadStackSize * K;
  if (stack_size_in_bytes != 0 &&
      stack_size_in_bytes < _vm_internal_thread_min_stack_allowed) {
    tty->print_cr("\nThe VMThreadStackSize specified is too small. "
                  "Specify at least " SIZE_FORMAT "k",
                  _vm_internal_thread_min_stack_allowed / K);
    return JNI_ERR;
  }
  return JNI_OK;
}

// Called when creating the thread.  The minimum stack sizes have already been calculated
size_t os::Posix::get_initial_stack_size(ThreadType thr_type, size_t req_stack_size) {
  size_t stack_size;
  if (req_stack_size == 0) {
    stack_size = default_stack_size(thr_type);
  } else {
    stack_size = req_stack_size;
  }

  switch (thr_type) {
  case os::java_thread:
    // Java threads use ThreadStackSize which default value can be
    // changed with the flag -Xss
    if (req_stack_size == 0 && JavaThread::stack_size_at_create() > 0) {
      // no requested size and we have a more specific default value
      stack_size = JavaThread::stack_size_at_create();
    }
    stack_size = MAX2(stack_size,
                      _java_thread_min_stack_allowed);
    break;
  case os::compiler_thread:
    if (req_stack_size == 0 && CompilerThreadStackSize > 0) {
      // no requested size and we have a more specific default value
      stack_size = (size_t)(CompilerThreadStackSize * K);
    }
    stack_size = MAX2(stack_size,
                      _compiler_thread_min_stack_allowed);
    break;
  case os::vm_thread:
  case os::pgc_thread:
  case os::cgc_thread:
  case os::watcher_thread:
  default:  // presume the unknown thr_type is a VM internal
    if (req_stack_size == 0 && VMThreadStackSize > 0) {
      // no requested size and we have a more specific default value
      stack_size = (size_t)(VMThreadStackSize * K);
    }

    stack_size = MAX2(stack_size,
                      _vm_internal_thread_min_stack_allowed);
    break;
  }

  // pthread_attr_setstacksize() may require that the size be rounded up to the OS page size.
  // Be careful not to round up to 0. Align down in that case.
  if (stack_size <= SIZE_MAX - vm_page_size()) {
    stack_size = align_up(stack_size, vm_page_size());
  } else {
    stack_size = align_down(stack_size, vm_page_size());
  }

  return stack_size;
}

#ifndef ZERO
#ifndef ARM
static bool get_frame_at_stack_banging_point(JavaThread* thread, address pc, const void* ucVoid, frame* fr) {
  if (Interpreter::contains(pc)) {
    // interpreter performs stack banging after the fixed frame header has
    // been generated while the compilers perform it before. To maintain
    // semantic consistency between interpreted and compiled frames, the
    // method returns the Java sender of the current frame.
    *fr = os::fetch_frame_from_context(ucVoid);
    if (!fr->is_first_java_frame()) {
      // get_frame_at_stack_banging_point() is only called when we
      // have well defined stacks so java_sender() calls do not need
      // to assert safe_for_sender() first.
      *fr = fr->java_sender();
    }
  } else {
    // more complex code with compiled code
    assert(!Interpreter::contains(pc), "Interpreted methods should have been handled above");
    CodeBlob* cb = CodeCache::find_blob(pc);
    if (cb == NULL || !cb->is_nmethod() || cb->is_frame_complete_at(pc)) {
      // Not sure where the pc points to, fallback to default
      // stack overflow handling
      return false;
    } else {
      // in compiled code, the stack banging is performed just after the return pc
      // has been pushed on the stack
      *fr = os::fetch_compiled_frame_from_context(ucVoid);
      if (!fr->is_java_frame()) {
        assert(!fr->is_first_frame(), "Safety check");
        // See java_sender() comment above.
        *fr = fr->java_sender();
      }
    }
  }
  assert(fr->is_java_frame(), "Safety check");
  return true;
}
#endif // ARM

// This return true if the signal handler should just continue, ie. return after calling this
bool os::Posix::handle_stack_overflow(JavaThread* thread, address addr, address pc,
                                      const void* ucVoid, address* stub) {
  // stack overflow
  StackOverflow* overflow_state = thread->stack_overflow_state();
  if (overflow_state->in_stack_yellow_reserved_zone(addr)) {
    if (thread->thread_state() == _thread_in_Java) {
#ifndef ARM
      // arm32 doesn't have this
      if (overflow_state->in_stack_reserved_zone(addr)) {
        frame fr;
        if (get_frame_at_stack_banging_point(thread, pc, ucVoid, &fr)) {
          assert(fr.is_java_frame(), "Must be a Java frame");
          frame activation =
            SharedRuntime::look_for_reserved_stack_annotated_method(thread, fr);
          if (activation.sp() != NULL) {
            overflow_state->disable_stack_reserved_zone();
            if (activation.is_interpreted_frame()) {
              overflow_state->set_reserved_stack_activation((address)(activation.fp()
                // Some platforms use frame pointers for interpreter frames, others use initial sp.
#if !defined(PPC64) && !defined(S390)
                + frame::interpreter_frame_initial_sp_offset
#endif
                ));
            } else {
              overflow_state->set_reserved_stack_activation((address)activation.unextended_sp());
            }
            return true; // just continue
          }
        }
      }
#endif // ARM
      // Throw a stack overflow exception.  Guard pages will be reenabled
      // while unwinding the stack.
      overflow_state->disable_stack_yellow_reserved_zone();
      *stub = SharedRuntime::continuation_for_implicit_exception(thread, pc, SharedRuntime::STACK_OVERFLOW);
    } else {
      // Thread was in the vm or native code.  Return and try to finish.
      overflow_state->disable_stack_yellow_reserved_zone();
      return true; // just continue
    }
  } else if (overflow_state->in_stack_red_zone(addr)) {
    // Fatal red zone violation.  Disable the guard pages and fall through
    // to handle_unexpected_exception way down below.
    overflow_state->disable_stack_red_zone();
    tty->print_raw_cr("An irrecoverable stack overflow has occurred.");

    // This is a likely cause, but hard to verify. Let's just print
    // it as a hint.
    tty->print_raw_cr("Please check if any of your loaded .so files has "
                      "enabled executable stack (see man page execstack(8))");

  } else {
#if !defined(AIX) && !defined(__APPLE__)
    // bsd and aix don't have this

    // Accessing stack address below sp may cause SEGV if current
    // thread has MAP_GROWSDOWN stack. This should only happen when
    // current thread was created by user code with MAP_GROWSDOWN flag
    // and then attached to VM. See notes in os_linux.cpp.
    if (thread->osthread()->expanding_stack() == 0) {
       thread->osthread()->set_expanding_stack();
       if (os::Linux::manually_expand_stack(thread, addr)) {
         thread->osthread()->clear_expanding_stack();
         return true; // just continue
       }
       thread->osthread()->clear_expanding_stack();
    } else {
       fatal("recursive segv. expanding stack.");
    }
#else
    tty->print_raw_cr("SIGSEGV happened inside stack but outside yellow and red zone.");
#endif // AIX or BSD
  }
  return false;
}
#endif // ZERO

bool os::Posix::is_root(uid_t uid){
    return ROOT_UID == uid;
}

bool os::Posix::matches_effective_uid_or_root(uid_t uid) {
    return is_root(uid) || geteuid() == uid;
}

bool os::Posix::matches_effective_uid_and_gid_or_root(uid_t uid, gid_t gid) {
    return is_root(uid) || (geteuid() == uid && getegid() == gid);
}

Thread* os::ThreadCrashProtection::_protected_thread = NULL;
os::ThreadCrashProtection* os::ThreadCrashProtection::_crash_protection = NULL;

os::ThreadCrashProtection::ThreadCrashProtection() {
  _protected_thread = Thread::current();
  assert(_protected_thread->is_JfrSampler_thread(), "should be JFRSampler");
}

/*
 * See the caveats for this class in os_posix.hpp
 * Protects the callback call so that SIGSEGV / SIGBUS jumps back into this
 * method and returns false. If none of the signals are raised, returns true.
 * The callback is supposed to provide the method that should be protected.
 */
bool os::ThreadCrashProtection::call(os::CrashProtectionCallback& cb) {
  sigset_t saved_sig_mask;

  // we cannot rely on sigsetjmp/siglongjmp to save/restore the signal mask
  // since on at least some systems (OS X) siglongjmp will restore the mask
  // for the process, not the thread
  pthread_sigmask(0, NULL, &saved_sig_mask);
  if (sigsetjmp(_jmpbuf, 0) == 0) {
    // make sure we can see in the signal handler that we have crash protection
    // installed
    _crash_protection = this;
    cb.call();
    // and clear the crash protection
    _crash_protection = NULL;
    _protected_thread = NULL;
    return true;
  }
  // this happens when we siglongjmp() back
  pthread_sigmask(SIG_SETMASK, &saved_sig_mask, NULL);
  _crash_protection = NULL;
  _protected_thread = NULL;
  return false;
}

void os::ThreadCrashProtection::restore() {
  assert(_crash_protection != NULL, "must have crash protection");
  siglongjmp(_jmpbuf, 1);
}

void os::ThreadCrashProtection::check_crash_protection(int sig,
    Thread* thread) {

  if (thread != NULL &&
      thread == _protected_thread &&
      _crash_protection != NULL) {

    if (sig == SIGSEGV || sig == SIGBUS) {
      _crash_protection->restore();
    }
  }
}

// Shared clock/time and other supporting routines for pthread_mutex/cond
// initialization. This is enabled on Solaris but only some of the clock/time
// functionality is actually used there.

// Shared condattr object for use with relative timed-waits. Will be associated
// with CLOCK_MONOTONIC if available to avoid issues with time-of-day changes,
// but otherwise whatever default is used by the platform - generally the
// time-of-day clock.
static pthread_condattr_t _condAttr[1];

// Shared mutexattr to explicitly set the type to PTHREAD_MUTEX_NORMAL as not
// all systems (e.g. FreeBSD) map the default to "normal".
static pthread_mutexattr_t _mutexAttr[1];

// common basic initialization that is always supported
static void pthread_init_common(void) {
  int status;
  if ((status = pthread_condattr_init(_condAttr)) != 0) {
    fatal("pthread_condattr_init: %s", os::strerror(status));
  }
  if ((status = pthread_mutexattr_init(_mutexAttr)) != 0) {
    fatal("pthread_mutexattr_init: %s", os::strerror(status));
  }
  if ((status = pthread_mutexattr_settype(_mutexAttr, PTHREAD_MUTEX_NORMAL)) != 0) {
    fatal("pthread_mutexattr_settype: %s", os::strerror(status));
  }
  os::PlatformMutex::init();
}

static int (*_pthread_condattr_setclock)(pthread_condattr_t *, clockid_t) = NULL;

static bool _use_clock_monotonic_condattr = false;

// Determine what POSIX API's are present and do appropriate
// configuration.
void os::Posix::init(void) {

  // NOTE: no logging available when this is called. Put logging
  // statements in init_2().

  // Check for pthread_condattr_setclock support.

  // libpthread is already loaded.
  int (*condattr_setclock_func)(pthread_condattr_t*, clockid_t) =
    (int (*)(pthread_condattr_t*, clockid_t))dlsym(RTLD_DEFAULT,
                                                   "pthread_condattr_setclock");
  if (condattr_setclock_func != NULL) {
    _pthread_condattr_setclock = condattr_setclock_func;
  }

  // Now do general initialization.

  pthread_init_common();

  int status;
  if (_pthread_condattr_setclock != NULL) {
    if ((status = _pthread_condattr_setclock(_condAttr, CLOCK_MONOTONIC)) != 0) {
      if (status == EINVAL) {
        _use_clock_monotonic_condattr = false;
        warning("Unable to use monotonic clock with relative timed-waits" \
                " - changes to the time-of-day clock may have adverse affects");
      } else {
        fatal("pthread_condattr_setclock: %s", os::strerror(status));
      }
    } else {
      _use_clock_monotonic_condattr = true;
    }
  }
}

void os::Posix::init_2(void) {
  log_info(os)("Use of CLOCK_MONOTONIC is supported");
  log_info(os)("Use of pthread_condattr_setclock is%s supported",
               (_pthread_condattr_setclock != NULL ? "" : " not"));
  log_info(os)("Relative timed-wait using pthread_cond_timedwait is associated with %s",
               _use_clock_monotonic_condattr ? "CLOCK_MONOTONIC" : "the default clock");
}

// Utility to convert the given timeout to an absolute timespec
// (based on the appropriate clock) to use with pthread_cond_timewait,
// and sem_timedwait().
// The clock queried here must be the clock used to manage the
// timeout of the condition variable or semaphore.
//
// The passed in timeout value is either a relative time in nanoseconds
// or an absolute time in milliseconds. A relative timeout will be
// associated with CLOCK_MONOTONIC if available, unless the real-time clock
// is explicitly requested; otherwise, or if absolute,
// the default time-of-day clock will be used.

// Given time is a 64-bit value and the time_t used in the timespec is
// sometimes a signed-32-bit value we have to watch for overflow if times
// way in the future are given. Further on Solaris versions
// prior to 10 there is a restriction (see cond_timedwait) that the specified
// number of seconds, in abstime, is less than current_time + 100000000.
// As it will be over 20 years before "now + 100000000" will overflow we can
// ignore overflow and just impose a hard-limit on seconds using the value
// of "now + 100000000". This places a limit on the timeout of about 3.17
// years from "now".
//
#define MAX_SECS 100000000

// Calculate a new absolute time that is "timeout" nanoseconds from "now".
// "unit" indicates the unit of "now_part_sec" (may be nanos or micros depending
// on which clock API is being used).
static void calc_rel_time(timespec* abstime, jlong timeout, jlong now_sec,
                          jlong now_part_sec, jlong unit) {
  time_t max_secs = now_sec + MAX_SECS;

  jlong seconds = timeout / NANOUNITS;
  timeout %= NANOUNITS; // remaining nanos

  if (seconds >= MAX_SECS) {
    // More seconds than we can add, so pin to max_secs.
    abstime->tv_sec = max_secs;
    abstime->tv_nsec = 0;
  } else {
    abstime->tv_sec = now_sec  + seconds;
    long nanos = (now_part_sec * (NANOUNITS / unit)) + timeout;
    if (nanos >= NANOUNITS) { // overflow
      abstime->tv_sec += 1;
      nanos -= NANOUNITS;
    }
    abstime->tv_nsec = nanos;
  }
}

// Unpack the given deadline in milliseconds since the epoch, into the given timespec.
// The current time in seconds is also passed in to enforce an upper bound as discussed above.
static void unpack_abs_time(timespec* abstime, jlong deadline, jlong now_sec) {
  time_t max_secs = now_sec + MAX_SECS;

  jlong seconds = deadline / MILLIUNITS;
  jlong millis = deadline % MILLIUNITS;

  if (seconds >= max_secs) {
    // Absolute seconds exceeds allowed max, so pin to max_secs.
    abstime->tv_sec = max_secs;
    abstime->tv_nsec = 0;
  } else {
    abstime->tv_sec = seconds;
    abstime->tv_nsec = millis_to_nanos(millis);
  }
}

static jlong millis_to_nanos_bounded(jlong millis) {
  // We have to watch for overflow when converting millis to nanos,
  // but if millis is that large then we will end up limiting to
  // MAX_SECS anyway, so just do that here.
  if (millis / MILLIUNITS > MAX_SECS) {
    millis = jlong(MAX_SECS) * MILLIUNITS;
  }
  return millis_to_nanos(millis);
}

static void to_abstime(timespec* abstime, jlong timeout,
                       bool isAbsolute, bool isRealtime) {
  DEBUG_ONLY(int max_secs = MAX_SECS;)

  if (timeout < 0) {
    timeout = 0;
  }

  clockid_t clock = CLOCK_MONOTONIC;
  if (isAbsolute || (!_use_clock_monotonic_condattr || isRealtime)) {
    clock = CLOCK_REALTIME;
  }

  struct timespec now;
  int status = clock_gettime(clock, &now);
  assert(status == 0, "clock_gettime error: %s", os::strerror(errno));

  if (!isAbsolute) {
    calc_rel_time(abstime, timeout, now.tv_sec, now.tv_nsec, NANOUNITS);
  } else {
    unpack_abs_time(abstime, timeout, now.tv_sec);
  }
  DEBUG_ONLY(max_secs += now.tv_sec;)

  assert(abstime->tv_sec >= 0, "tv_sec < 0");
  assert(abstime->tv_sec <= max_secs, "tv_sec > max_secs");
  assert(abstime->tv_nsec >= 0, "tv_nsec < 0");
  assert(abstime->tv_nsec < NANOUNITS, "tv_nsec >= NANOUNITS");
}

// Create an absolute time 'millis' milliseconds in the future, using the
// real-time (time-of-day) clock. Used by PosixSemaphore.
void os::Posix::to_RTC_abstime(timespec* abstime, int64_t millis) {
  to_abstime(abstime, millis_to_nanos_bounded(millis),
             false /* not absolute */,
             true  /* use real-time clock */);
}

// Common (partly) shared time functions

jlong os::javaTimeMillis() {
  struct timespec ts;
  int status = clock_gettime(CLOCK_REALTIME, &ts);
  assert(status == 0, "clock_gettime error: %s", os::strerror(errno));
  return jlong(ts.tv_sec) * MILLIUNITS +
    jlong(ts.tv_nsec) / NANOUNITS_PER_MILLIUNIT;
}

void os::javaTimeSystemUTC(jlong &seconds, jlong &nanos) {
  struct timespec ts;
  int status = clock_gettime(CLOCK_REALTIME, &ts);
  assert(status == 0, "clock_gettime error: %s", os::strerror(errno));
  seconds = jlong(ts.tv_sec);
  nanos = jlong(ts.tv_nsec);
}

// macOS and AIX have platform specific implementations for javaTimeNanos()
// using native clock/timer access APIs. These have historically worked well
// for those platforms, but it may be possible for them to switch to the
// generic clock_gettime mechanism in the future.
#if !defined(__APPLE__) && !defined(AIX)

jlong os::javaTimeNanos() {
  struct timespec tp;
  int status = clock_gettime(CLOCK_MONOTONIC, &tp);
  assert(status == 0, "clock_gettime error: %s", os::strerror(errno));
  jlong result = jlong(tp.tv_sec) * NANOSECS_PER_SEC + jlong(tp.tv_nsec);
  return result;
}

// for timer info max values which include all bits
#define ALL_64_BITS CONST64(0xFFFFFFFFFFFFFFFF)

void os::javaTimeNanos_info(jvmtiTimerInfo *info_ptr) {
  // CLOCK_MONOTONIC - amount of time since some arbitrary point in the past
  info_ptr->max_value = ALL_64_BITS;
  info_ptr->may_skip_backward = false;      // not subject to resetting or drifting
  info_ptr->may_skip_forward = false;       // not subject to resetting or drifting
  info_ptr->kind = JVMTI_TIMER_ELAPSED;     // elapsed not CPU time
}

#endif // ! APPLE && !AIX

// Shared pthread_mutex/cond based PlatformEvent implementation.
// Not currently usable by Solaris.


// PlatformEvent
//
// Assumption:
//    Only one parker can exist on an event, which is why we allocate
//    them per-thread. Multiple unparkers can coexist.
//
// _event serves as a restricted-range semaphore.
//   -1 : thread is blocked, i.e. there is a waiter
//    0 : neutral: thread is running or ready,
//        could have been signaled after a wait started
//    1 : signaled - thread is running or ready
//
//    Having three states allows for some detection of bad usage - see
//    comments on unpark().

os::PlatformEvent::PlatformEvent() {
  int status = pthread_cond_init(_cond, _condAttr);
  assert_status(status == 0, status, "cond_init");
  status = pthread_mutex_init(_mutex, _mutexAttr);
  assert_status(status == 0, status, "mutex_init");
  _event   = 0;
  _nParked = 0;
}

void os::PlatformEvent::park() {       // AKA "down()"
  // Transitions for _event:
  //   -1 => -1 : illegal
  //    1 =>  0 : pass - return immediately
  //    0 => -1 : block; then set _event to 0 before returning

  // Invariant: Only the thread associated with the PlatformEvent
  // may call park().
  assert(_nParked == 0, "invariant");

  int v;

  // atomically decrement _event
  for (;;) {
    v = _event;
    if (Atomic::cmpxchg(&_event, v, v - 1) == v) break;
  }
  guarantee(v >= 0, "invariant");

  if (v == 0) { // Do this the hard way by blocking ...
    int status = pthread_mutex_lock(_mutex);
    assert_status(status == 0, status, "mutex_lock");
    guarantee(_nParked == 0, "invariant");
    ++_nParked;
    while (_event < 0) {
      // OS-level "spurious wakeups" are ignored
      status = pthread_cond_wait(_cond, _mutex);
      assert_status(status == 0 MACOS_ONLY(|| status == ETIMEDOUT),
                    status, "cond_wait");
    }
    --_nParked;

    _event = 0;
    status = pthread_mutex_unlock(_mutex);
    assert_status(status == 0, status, "mutex_unlock");
    // Paranoia to ensure our locked and lock-free paths interact
    // correctly with each other.
    OrderAccess::fence();
  }
  guarantee(_event >= 0, "invariant");
}

int os::PlatformEvent::park(jlong millis) {
  // Transitions for _event:
  //   -1 => -1 : illegal
  //    1 =>  0 : pass - return immediately
  //    0 => -1 : block; then set _event to 0 before returning

  // Invariant: Only the thread associated with the Event/PlatformEvent
  // may call park().
  assert(_nParked == 0, "invariant");

  int v;
  // atomically decrement _event
  for (;;) {
    v = _event;
    if (Atomic::cmpxchg(&_event, v, v - 1) == v) break;
  }
  guarantee(v >= 0, "invariant");

  if (v == 0) { // Do this the hard way by blocking ...
    struct timespec abst;
    to_abstime(&abst, millis_to_nanos_bounded(millis), false, false);

    int ret = OS_TIMEOUT;
    int status = pthread_mutex_lock(_mutex);
    assert_status(status == 0, status, "mutex_lock");
    guarantee(_nParked == 0, "invariant");
    ++_nParked;

    while (_event < 0) {
      status = pthread_cond_timedwait(_cond, _mutex, &abst);
      assert_status(status == 0 || status == ETIMEDOUT,
                    status, "cond_timedwait");
      // OS-level "spurious wakeups" are ignored unless the archaic
      // FilterSpuriousWakeups is set false. That flag should be obsoleted.
      if (!FilterSpuriousWakeups) break;
      if (status == ETIMEDOUT) break;
    }
    --_nParked;

    if (_event >= 0) {
      ret = OS_OK;
    }

    _event = 0;
    status = pthread_mutex_unlock(_mutex);
    assert_status(status == 0, status, "mutex_unlock");
    // Paranoia to ensure our locked and lock-free paths interact
    // correctly with each other.
    OrderAccess::fence();
    return ret;
  }
  return OS_OK;
}

void os::PlatformEvent::unpark() {
  // Transitions for _event:
  //    0 => 1 : just return
  //    1 => 1 : just return
  //   -1 => either 0 or 1; must signal target thread
  //         That is, we can safely transition _event from -1 to either
  //         0 or 1.
  // See also: "Semaphores in Plan 9" by Mullender & Cox
  //
  // Note: Forcing a transition from "-1" to "1" on an unpark() means
  // that it will take two back-to-back park() calls for the owning
  // thread to block. This has the benefit of forcing a spurious return
  // from the first park() call after an unpark() call which will help
  // shake out uses of park() and unpark() without checking state conditions
  // properly. This spurious return doesn't manifest itself in any user code
  // but only in the correctly written condition checking loops of ObjectMonitor,
  // Mutex/Monitor, and JavaThread::sleep

  if (Atomic::xchg(&_event, 1) >= 0) return;

  int status = pthread_mutex_lock(_mutex);
  assert_status(status == 0, status, "mutex_lock");
  int anyWaiters = _nParked;
  assert(anyWaiters == 0 || anyWaiters == 1, "invariant");
  status = pthread_mutex_unlock(_mutex);
  assert_status(status == 0, status, "mutex_unlock");

  // Note that we signal() *after* dropping the lock for "immortal" Events.
  // This is safe and avoids a common class of futile wakeups.  In rare
  // circumstances this can cause a thread to return prematurely from
  // cond_{timed}wait() but the spurious wakeup is benign and the victim
  // will simply re-test the condition and re-park itself.
  // This provides particular benefit if the underlying platform does not
  // provide wait morphing.

  if (anyWaiters != 0) {
    status = pthread_cond_signal(_cond);
    assert_status(status == 0, status, "cond_signal");
  }
}

// JSR166 support

 os::PlatformParker::PlatformParker() : _counter(0), _cur_index(-1) {
  int status = pthread_cond_init(&_cond[REL_INDEX], _condAttr);
  assert_status(status == 0, status, "cond_init rel");
  status = pthread_cond_init(&_cond[ABS_INDEX], NULL);
  assert_status(status == 0, status, "cond_init abs");
  status = pthread_mutex_init(_mutex, _mutexAttr);
  assert_status(status == 0, status, "mutex_init");
}

os::PlatformParker::~PlatformParker() {
  int status = pthread_cond_destroy(&_cond[REL_INDEX]);
  assert_status(status == 0, status, "cond_destroy rel");
  status = pthread_cond_destroy(&_cond[ABS_INDEX]);
  assert_status(status == 0, status, "cond_destroy abs");
  status = pthread_mutex_destroy(_mutex);
  assert_status(status == 0, status, "mutex_destroy");
}

// Parker::park decrements count if > 0, else does a condvar wait.  Unpark
// sets count to 1 and signals condvar.  Only one thread ever waits
// on the condvar. Contention seen when trying to park implies that someone
// is unparking you, so don't wait. And spurious returns are fine, so there
// is no need to track notifications.

void Parker::park(bool isAbsolute, jlong time) {

  // Optional fast-path check:
  // Return immediately if a permit is available.
  // We depend on Atomic::xchg() having full barrier semantics
  // since we are doing a lock-free update to _counter.
  if (Atomic::xchg(&_counter, 0) > 0) return;

  JavaThread *jt = JavaThread::current();

  // Optional optimization -- avoid state transitions if there's
  // an interrupt pending.
  if (jt->is_interrupted(false)) {
    return;
  }

  // Next, demultiplex/decode time arguments
  struct timespec absTime;
  if (time < 0 || (isAbsolute && time == 0)) { // don't wait at all
    return;
  }
  if (time > 0) {
    to_abstime(&absTime, time, isAbsolute, false);
  }

  // Enter safepoint region
  // Beware of deadlocks such as 6317397.
  // The per-thread Parker:: mutex is a classic leaf-lock.
  // In particular a thread must never block on the Threads_lock while
  // holding the Parker:: mutex.  If safepoints are pending both the
  // the ThreadBlockInVM() CTOR and DTOR may grab Threads_lock.
  ThreadBlockInVM tbivm(jt);

  // Can't access interrupt state now that we are _thread_blocked. If we've
  // been interrupted since we checked above then _counter will be > 0.

  // Don't wait if cannot get lock since interference arises from
  // unparking.
  if (pthread_mutex_trylock(_mutex) != 0) {
    return;
  }

  int status;
  if (_counter > 0)  { // no wait needed
    _counter = 0;
    status = pthread_mutex_unlock(_mutex);
    assert_status(status == 0, status, "invariant");
    // Paranoia to ensure our locked and lock-free paths interact
    // correctly with each other and Java-level accesses.
    OrderAccess::fence();
    return;
  }

  OSThreadWaitState osts(jt->osthread(), false /* not Object.wait() */);

  assert(_cur_index == -1, "invariant");
  if (time == 0) {
    _cur_index = REL_INDEX; // arbitrary choice when not timed
    status = pthread_cond_wait(&_cond[_cur_index], _mutex);
    assert_status(status == 0 MACOS_ONLY(|| status == ETIMEDOUT),
                  status, "cond_wait");
  }
  else {
    _cur_index = isAbsolute ? ABS_INDEX : REL_INDEX;
    status = pthread_cond_timedwait(&_cond[_cur_index], _mutex, &absTime);
    assert_status(status == 0 || status == ETIMEDOUT,
                  status, "cond_timedwait");
  }
  _cur_index = -1;

  _counter = 0;
  status = pthread_mutex_unlock(_mutex);
  assert_status(status == 0, status, "invariant");
  // Paranoia to ensure our locked and lock-free paths interact
  // correctly with each other and Java-level accesses.
  OrderAccess::fence();
}

void Parker::unpark() {
  int status = pthread_mutex_lock(_mutex);
  assert_status(status == 0, status, "invariant");
  const int s = _counter;
  _counter = 1;
  // must capture correct index before unlocking
  int index = _cur_index;
  status = pthread_mutex_unlock(_mutex);
  assert_status(status == 0, status, "invariant");

  // Note that we signal() *after* dropping the lock for "immortal" Events.
  // This is safe and avoids a common class of futile wakeups.  In rare
  // circumstances this can cause a thread to return prematurely from
  // cond_{timed}wait() but the spurious wakeup is benign and the victim
  // will simply re-test the condition and re-park itself.
  // This provides particular benefit if the underlying platform does not
  // provide wait morphing.

  if (s < 1 && index != -1) {
    // thread is definitely parked
    status = pthread_cond_signal(&_cond[index]);
    assert_status(status == 0, status, "invariant");
  }
}

// Platform Mutex/Monitor implementation

#if PLATFORM_MONITOR_IMPL_INDIRECT

os::PlatformMutex::Mutex::Mutex() : _next(NULL) {
  int status = pthread_mutex_init(&_mutex, _mutexAttr);
  assert_status(status == 0, status, "mutex_init");
}

os::PlatformMutex::Mutex::~Mutex() {
  int status = pthread_mutex_destroy(&_mutex);
  assert_status(status == 0, status, "mutex_destroy");
}

pthread_mutex_t os::PlatformMutex::_freelist_lock;
os::PlatformMutex::Mutex* os::PlatformMutex::_mutex_freelist = NULL;

void os::PlatformMutex::init() {
  int status = pthread_mutex_init(&_freelist_lock, _mutexAttr);
  assert_status(status == 0, status, "freelist lock init");
}

struct os::PlatformMutex::WithFreeListLocked : public StackObj {
  WithFreeListLocked() {
    int status = pthread_mutex_lock(&_freelist_lock);
    assert_status(status == 0, status, "freelist lock");
  }

  ~WithFreeListLocked() {
    int status = pthread_mutex_unlock(&_freelist_lock);
    assert_status(status == 0, status, "freelist unlock");
  }
};

os::PlatformMutex::PlatformMutex() {
  {
    WithFreeListLocked wfl;
    _impl = _mutex_freelist;
    if (_impl != NULL) {
      _mutex_freelist = _impl->_next;
      _impl->_next = NULL;
      return;
    }
  }
  _impl = new Mutex();
}

os::PlatformMutex::~PlatformMutex() {
  WithFreeListLocked wfl;
  assert(_impl->_next == NULL, "invariant");
  _impl->_next = _mutex_freelist;
  _mutex_freelist = _impl;
}

os::PlatformMonitor::Cond::Cond() : _next(NULL) {
  int status = pthread_cond_init(&_cond, _condAttr);
  assert_status(status == 0, status, "cond_init");
}

os::PlatformMonitor::Cond::~Cond() {
  int status = pthread_cond_destroy(&_cond);
  assert_status(status == 0, status, "cond_destroy");
}

os::PlatformMonitor::Cond* os::PlatformMonitor::_cond_freelist = NULL;

os::PlatformMonitor::PlatformMonitor() {
  {
    WithFreeListLocked wfl;
    _impl = _cond_freelist;
    if (_impl != NULL) {
      _cond_freelist = _impl->_next;
      _impl->_next = NULL;
      return;
    }
  }
  _impl = new Cond();
}

os::PlatformMonitor::~PlatformMonitor() {
  WithFreeListLocked wfl;
  assert(_impl->_next == NULL, "invariant");
  _impl->_next = _cond_freelist;
  _cond_freelist = _impl;
}

#else

os::PlatformMutex::PlatformMutex() {
  int status = pthread_mutex_init(&_mutex, _mutexAttr);
  assert_status(status == 0, status, "mutex_init");
}

os::PlatformMutex::~PlatformMutex() {
  int status = pthread_mutex_destroy(&_mutex);
  assert_status(status == 0, status, "mutex_destroy");
}

os::PlatformMonitor::PlatformMonitor() {
  int status = pthread_cond_init(&_cond, _condAttr);
  assert_status(status == 0, status, "cond_init");
}

os::PlatformMonitor::~PlatformMonitor() {
  int status = pthread_cond_destroy(&_cond);
  assert_status(status == 0, status, "cond_destroy");
}

#endif // PLATFORM_MONITOR_IMPL_INDIRECT

// Must already be locked
int os::PlatformMonitor::wait(jlong millis) {
  assert(millis >= 0, "negative timeout");
  if (millis > 0) {
    struct timespec abst;
    // We have to watch for overflow when converting millis to nanos,
    // but if millis is that large then we will end up limiting to
    // MAX_SECS anyway, so just do that here.
    if (millis / MILLIUNITS > MAX_SECS) {
      millis = jlong(MAX_SECS) * MILLIUNITS;
    }
    to_abstime(&abst, millis_to_nanos(millis), false, false);

    int ret = OS_TIMEOUT;
    int status = pthread_cond_timedwait(cond(), mutex(), &abst);
    assert_status(status == 0 || status == ETIMEDOUT,
                  status, "cond_timedwait");
    if (status == 0) {
      ret = OS_OK;
    }
    return ret;
  } else {
    int status = pthread_cond_wait(cond(), mutex());
    assert_status(status == 0 MACOS_ONLY(|| status == ETIMEDOUT),
                  status, "cond_wait");
    return OS_OK;
  }
}

// Darwin has no "environ" in a dynamic library.
#ifdef __APPLE__
  #define environ (*_NSGetEnviron())
#else
  extern char** environ;
#endif

char** os::get_environ() { return environ; }

// Run the specified command in a separate process. Return its exit value,
// or -1 on failure (e.g. can't fork a new process).
// Notes: -Unlike system(), this function can be called from signal handler. It
//         doesn't block SIGINT et al.
//        -this function is unsafe to use in non-error situations, mainly
//         because the child process will inherit all parent descriptors.
int os::fork_and_exec(const char* cmd, bool prefer_vfork) {
  const char * argv[4] = {"sh", "-c", cmd, NULL};

  pid_t pid ;

  char** env = os::get_environ();

  // Use always vfork on AIX, since its safe and helps with analyzing OOM situations.
  // Otherwise leave it up to the caller.
  AIX_ONLY(prefer_vfork = true;)
  pid = prefer_vfork ? ::vfork() : ::fork();

  if (pid < 0) {
    // fork failed
    return -1;

  } else if (pid == 0) {
    // child process

    ::execve("/bin/sh", (char* const*)argv, env);

    // execve failed
    ::_exit(-1);

  } else  {
    // copied from J2SE ..._waitForProcessExit() in UNIXProcess_md.c; we don't
    // care about the actual exit code, for now.

    int status;

    // Wait for the child process to exit.  This returns immediately if
    // the child has already exited. */
    while (::waitpid(pid, &status, 0) < 0) {
      switch (errno) {
      case ECHILD: return 0;
      case EINTR: break;
      default: return -1;
      }
    }

    if (WIFEXITED(status)) {
      // The child exited normally; get its exit code.
      return WEXITSTATUS(status);
    } else if (WIFSIGNALED(status)) {
      // The child exited because of a signal
      // The best value to return is 0x80 + signal number,
      // because that is what all Unix shells do, and because
      // it allows callers to distinguish between process exit and
      // process death by signal.
      return 0x80 + WTERMSIG(status);
    } else {
      // Unknown exit code; pass it through
      return status;
    }
  }
}

////////////////////////////////////////////////////////////////////////////////
// runtime exit support

// Note: os::shutdown() might be called very early during initialization, or
// called from signal handler. Before adding something to os::shutdown(), make
// sure it is async-safe and can handle partially initialized VM.
void os::shutdown() {

  // allow PerfMemory to attempt cleanup of any persistent resources
  perfMemory_exit();

  // needs to remove object in file system
  AttachListener::abort();

  // flush buffered output, finish log files
  ostream_abort();

  // Check for abort hook
  abort_hook_t abort_hook = Arguments::abort_hook();
  if (abort_hook != NULL) {
    abort_hook();
  }

}

// Note: os::abort() might be called very early during initialization, or
// called from signal handler. Before adding something to os::abort(), make
// sure it is async-safe and can handle partially initialized VM.
// Also note we can abort while other threads continue to run, so we can
// easily trigger secondary faults in those threads. To reduce the likelihood
// of that we use _exit rather than exit, so that no atexit hooks get run.
// But note that os::shutdown() could also trigger secondary faults.
void os::abort(bool dump_core, void* siginfo, const void* context) {
  os::shutdown();
  if (dump_core) {
    LINUX_ONLY(if (DumpPrivateMappingsInCore) ClassLoader::close_jrt_image();)
    ::abort(); // dump core
  }
  ::_exit(1);
}

// Die immediately, no exit hook, no abort hook, no cleanup.
// Dump a core file, if possible, for debugging.
void os::die() {
  if (TestUnresponsiveErrorHandler && !CreateCoredumpOnCrash) {
    // For TimeoutInErrorHandlingTest.java, we just kill the VM
    // and don't take the time to generate a core file.
    os::signal_raise(SIGKILL);
  } else {
    ::abort();
  }
}
