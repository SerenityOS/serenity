/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <errno.h>
#include <elf.h>
#include <dirent.h>
#include <ctype.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/ptrace.h>
#include <sys/uio.h>
#include "libproc_impl.h"

#if defined(x86_64) && !defined(amd64)
#define amd64 1
#endif

#ifndef __WALL
#define __WALL          0x40000000  // Copied from /usr/include/linux/wait.h
#endif

// This file has the libproc implementation specific to live process
// For core files, refer to ps_core.c

typedef enum {
  ATTACH_SUCCESS,
  ATTACH_FAIL,
  ATTACH_THREAD_DEAD
} attach_state_t;

static inline uintptr_t align(uintptr_t ptr, size_t size) {
  return (ptr & ~(size - 1));
}

// ---------------------------------------------
// ptrace functions
// ---------------------------------------------

// read "size" bytes of data from "addr" within the target process.
// unlike the standard ptrace() function, process_read_data() can handle
// unaligned address - alignment check, if required, should be done
// before calling process_read_data.

static bool process_read_data(struct ps_prochandle* ph, uintptr_t addr, char *buf, size_t size) {
  long rslt;
  size_t i, words;
  uintptr_t end_addr = addr + size;
  uintptr_t aligned_addr = align(addr, sizeof(long));

  if (aligned_addr != addr) {
    char *ptr = (char *)&rslt;
    errno = 0;
    rslt = ptrace(PTRACE_PEEKDATA, ph->pid, aligned_addr, 0);
    if (errno) {
      print_debug("ptrace(PTRACE_PEEKDATA, ..) failed for %d bytes @ %lx\n", size, addr);
      return false;
    }
    for (; aligned_addr != addr; aligned_addr++, ptr++);
    for (; ((intptr_t)aligned_addr % sizeof(long)) && aligned_addr < end_addr;
        aligned_addr++)
       *(buf++) = *(ptr++);
  }

  words = (end_addr - aligned_addr) / sizeof(long);

  // assert((intptr_t)aligned_addr % sizeof(long) == 0);
  for (i = 0; i < words; i++) {
    errno = 0;
    rslt = ptrace(PTRACE_PEEKDATA, ph->pid, aligned_addr, 0);
    if (errno) {
      print_debug("ptrace(PTRACE_PEEKDATA, ..) failed for %d bytes @ %lx\n", size, addr);
      return false;
    }
    *(long *)buf = rslt;
    buf += sizeof(long);
    aligned_addr += sizeof(long);
  }

  if (aligned_addr != end_addr) {
    char *ptr = (char *)&rslt;
    errno = 0;
    rslt = ptrace(PTRACE_PEEKDATA, ph->pid, aligned_addr, 0);
    if (errno) {
      print_debug("ptrace(PTRACE_PEEKDATA, ..) failed for %d bytes @ %lx\n", size, addr);
      return false;
    }
    for (; aligned_addr != end_addr; aligned_addr++)
       *(buf++) = *(ptr++);
  }
  return true;
}

// null implementation for write
static bool process_write_data(struct ps_prochandle* ph,
                             uintptr_t addr, const char *buf , size_t size) {
  return false;
}

// "user" should be a pointer to a user_regs_struct
static bool process_get_lwp_regs(struct ps_prochandle* ph, pid_t pid, struct user_regs_struct *user) {
  // we have already attached to all thread 'pid's, just use ptrace call
  // to get regset now. Note that we don't cache regset upfront for processes.
// Linux on x86 and sparc are different.  On x86 ptrace(PTRACE_GETREGS, ...)
// uses pointer from 4th argument and ignores 3rd argument.  On sparc it uses
// pointer from 3rd argument and ignores 4th argument
#define ptrace_getregs(request, pid, addr, data) ptrace(request, pid, data, addr)

#if defined(_LP64) && defined(PTRACE_GETREGS64)
#define PTRACE_GETREGS_REQ PTRACE_GETREGS64
#elif defined(PTRACE_GETREGS)
#define PTRACE_GETREGS_REQ PTRACE_GETREGS
#elif defined(PT_GETREGS)
#define PTRACE_GETREGS_REQ PT_GETREGS
#endif

#ifdef PTRACE_GETREGS_REQ
 if (ptrace_getregs(PTRACE_GETREGS_REQ, pid, user, NULL) < 0) {
   print_debug("ptrace(PTRACE_GETREGS, ...) failed for lwp(%d) errno(%d) \"%s\"\n", pid,
               errno, strerror(errno));
   return false;
 }
 return true;
#elif defined(PTRACE_GETREGSET)
 struct iovec iov;
 iov.iov_base = user;
 iov.iov_len = sizeof(*user);
 if (ptrace(PTRACE_GETREGSET, pid, NT_PRSTATUS, (void*) &iov) < 0) {
   print_debug("ptrace(PTRACE_GETREGSET, ...) failed for lwp %d\n", pid);
   return false;
 }
 return true;
#else
 print_debug("ptrace(PTRACE_GETREGS, ...) not supported\n");
 return false;
#endif

}

static bool ptrace_continue(pid_t pid, int signal) {
  // pass the signal to the process so we don't swallow it
  if (ptrace(PTRACE_CONT, pid, NULL, signal) < 0) {
    print_debug("ptrace(PTRACE_CONT, ..) failed for %d\n", pid);
    return false;
  }
  return true;
}

// waits until the ATTACH has stopped the process
// by signal SIGSTOP
static attach_state_t ptrace_waitpid(pid_t pid) {
  int ret;
  int status;
  errno = 0;
  while (true) {
    // Wait for debuggee to stop.
    ret = waitpid(pid, &status, 0);
    if (ret == -1 && errno == ECHILD) {
      // try cloned process.
      ret = waitpid(pid, &status, __WALL);
    }
    if (ret >= 0) {
      if (WIFSTOPPED(status)) {
        // Any signal will stop the thread, make sure it is SIGSTOP. Otherwise SIGSTOP
        // will still be pending and delivered when the process is DETACHED and the process
        // will go to sleep.
        if (WSTOPSIG(status) == SIGSTOP) {
          // Debuggee stopped by SIGSTOP.
          return ATTACH_SUCCESS;
        }
        if (!ptrace_continue(pid, WSTOPSIG(status))) {
          print_error("Failed to correctly attach to VM. VM might HANG! [PTRACE_CONT failed, stopped by %d]\n", WSTOPSIG(status));
          return ATTACH_FAIL;
        }
      } else {
        print_debug("waitpid(): Child process %d exited/terminated (status = 0x%x)\n", pid, status);
        return ATTACH_THREAD_DEAD;
      }
    } else {
      switch (errno) {
        case EINTR:
          continue;
          break;
        case ECHILD:
          print_debug("waitpid() failed. Child process pid (%d) does not exist \n", pid);
          return ATTACH_THREAD_DEAD;
        case EINVAL:
          print_error("waitpid() failed. Invalid options argument.\n");
          return ATTACH_FAIL;
        default:
          print_error("waitpid() failed. Unexpected error %d\n", errno);
          return ATTACH_FAIL;
      }
    } // else
  } // while
}

// checks the state of the thread/process specified by "pid", by reading
// in the 'State:' value from the /proc/<pid>/status file. From the proc
// man page, "Current state of the process. One of "R (running)",
// "S (sleeping)", "D (disk sleep)", "T (stopped)", "T (tracing stop)",
// "Z (zombie)", or "X (dead)"." Assumes that the thread is dead if we
// don't find the status file or if the status is 'X' or 'Z'.
static bool process_doesnt_exist(pid_t pid) {
  char fname[32];
  char buf[30];
  FILE *fp = NULL;
  const char state_string[] = "State:";

  sprintf(fname, "/proc/%d/status", pid);
  fp = fopen(fname, "r");
  if (fp == NULL) {
    print_debug("can't open /proc/%d/status file\n", pid);
    // Assume the thread does not exist anymore.
    return true;
  }
  bool found_state = false;
  size_t state_len = strlen(state_string);
  while (fgets(buf, sizeof(buf), fp) != NULL) {
    char *state = NULL;
    if (strncmp (buf, state_string, state_len) == 0) {
      found_state = true;
      state = buf + state_len;
      // Skip the spaces
      while (isspace(*state)) {
        state++;
      }
      // A state value of 'X' indicates that the thread is dead. 'Z'
      // indicates that the thread is a zombie.
      if (*state == 'X' || *state == 'Z') {
        fclose (fp);
        return true;
      }
      break;
    }
  }
  // If the state value is not 'X' or 'Z', the thread exists.
  if (!found_state) {
    // We haven't found the line beginning with 'State:'.
    // Assuming the thread exists.
    print_error("Could not find the 'State:' string in the /proc/%d/status file\n", pid);
  }
  fclose (fp);
  return false;
}

// attach to a process/thread specified by "pid"
static attach_state_t ptrace_attach(pid_t pid, char* err_buf, size_t err_buf_len) {
  errno = 0;
  if (ptrace(PTRACE_ATTACH, pid, NULL, NULL) < 0) {
    if (errno == EPERM || errno == ESRCH) {
      // Check if the process/thread is exiting or is a zombie
      if (process_doesnt_exist(pid)) {
        print_debug("Thread with pid %d does not exist\n", pid);
        return ATTACH_THREAD_DEAD;
      }
    }

    // strerror_r() API function is not compatible in different implementations:
    // GNU-specific:  char *strerror_r(int errnum, char *buf, size_t buflen);
    // XSI-compliant: int   strerror_r(int errnum, char *buf, size_t buflen);
    char buf[200];
#if defined(__GLIBC__) && defined(_GNU_SOURCE)
    char* msg = strerror_r(errno, buf, sizeof(buf));
#else
    int rc = strerror_r(errno, buf, sizeof(buf));
    char* msg = (rc == 0) ? (char*)buf : "Unknown";
#endif
    snprintf(err_buf, err_buf_len, "ptrace(PTRACE_ATTACH, ..) failed for %d: %s", pid, msg);
    print_error("%s\n", err_buf);
    return ATTACH_FAIL;
  } else {
    attach_state_t wait_ret = ptrace_waitpid(pid);
    if (wait_ret == ATTACH_THREAD_DEAD) {
      print_debug("Thread with pid %d does not exist\n", pid);
    }
    return wait_ret;
  }
}

// -------------------------------------------------------
// functions for obtaining library information
// -------------------------------------------------------

/*
 * splits a string _str_ into substrings with delimiter _delim_ by replacing old * delimiters with _new_delim_ (ideally, '\0'). the address of each substring
 * is stored in array _ptrs_ as the return value. the maximum capacity of _ptrs_ * array is specified by parameter _n_.
 * RETURN VALUE: total number of substrings (always <= _n_)
 * NOTE: string _str_ is modified if _delim_!=_new_delim_
 */
static int split_n_str(char * str, int n, char ** ptrs, char delim, char new_delim)
{
   int i;
   for(i = 0; i < n; i++) ptrs[i] = NULL;
   if (str == NULL || n < 1 ) return 0;

   i = 0;

   // skipping leading blanks
   while(*str&&*str==delim) str++;

   while(*str&&i<n){
     ptrs[i++] = str;
     while(*str&&*str!=delim) str++;
     while(*str&&*str==delim) *(str++) = new_delim;
   }

   return i;
}

/*
 * fgets without storing '\n' at the end of the string
 */
static char * fgets_no_cr(char * buf, int n, FILE *fp)
{
   char * rslt = fgets(buf, n, fp);
   if (rslt && buf && *buf){
       char *p = strchr(buf, '\0');
       if (*--p=='\n') *p='\0';
   }
   return rslt;
}

static bool read_lib_info(struct ps_prochandle* ph) {
  char fname[32];
  char buf[PATH_MAX];
  FILE *fp = NULL;

  sprintf(fname, "/proc/%d/maps", ph->pid);
  fp = fopen(fname, "r");
  if (fp == NULL) {
    print_debug("can't open /proc/%d/maps file\n", ph->pid);
    return false;
  }

  while(fgets_no_cr(buf, PATH_MAX, fp)){
    char * word[7];
    int nwords = split_n_str(buf, 7, word, ' ', '\0');

    if (nwords < 6) {
      // not a shared library entry. ignore.
      continue;
    }

    // SA does not handle the lines with patterns:
    //   "[stack]", "[heap]", "[vdso]", "[vsyscall]", etc.
    if (word[5][0] == '[') {
        // not a shared library entry. ignore.
        continue;
    }

    if (nwords > 6) {
      // prelink altered mapfile when the program is running.
      // Entries like one below have to be skipped
      //  /lib64/libc-2.15.so (deleted)
      // SO name in entries like one below have to be stripped.
      //  /lib64/libpthread-2.15.so.#prelink#.EECVts
      char *s = strstr(word[5],".#prelink#");
      if (s == NULL) {
        // No prelink keyword. skip deleted library
        print_debug("skip shared object %s deleted by prelink\n", word[5]);
        continue;
      }

      // Fall through
      print_debug("rectifying shared object name %s changed by prelink\n", word[5]);
      *s = 0;
    }

    if (find_lib(ph, word[5]) == false) {
       intptr_t base;
       lib_info* lib;
#ifdef _LP64
       sscanf(word[0], "%lx", &base);
#else
       sscanf(word[0], "%x", &base);
#endif
       if ((lib = add_lib_info(ph, word[5], (uintptr_t)base)) == NULL)
          continue; // ignore, add_lib_info prints error

       // we don't need to keep the library open, symtab is already
       // built. Only for core dump we need to keep the fd open.
       close(lib->fd);
       lib->fd = -1;
    }
  }
  fclose(fp);
  return true;
}

// detach a given pid
static bool ptrace_detach(pid_t pid) {
  if (pid && ptrace(PTRACE_DETACH, pid, NULL, NULL) < 0) {
    print_debug("ptrace(PTRACE_DETACH, ..) failed for %d\n", pid);
    return false;
  } else {
    return true;
  }
}

// detach all pids of a ps_prochandle
static void detach_all_pids(struct ps_prochandle* ph) {
  thread_info* thr = ph->threads;
  while (thr) {
     ptrace_detach(thr->lwp_id);
     thr = thr->next;
  }
}

static void process_cleanup(struct ps_prochandle* ph) {
  detach_all_pids(ph);
}

static ps_prochandle_ops process_ops = {
  .release=  process_cleanup,
  .p_pread=  process_read_data,
  .p_pwrite= process_write_data,
  .get_lwp_regs= process_get_lwp_regs
};

// attach to the process. One and only one exposed stuff
JNIEXPORT struct ps_prochandle* JNICALL
Pgrab(pid_t pid, char* err_buf, size_t err_buf_len) {
  struct ps_prochandle* ph = NULL;
  thread_info* thr = NULL;
  attach_state_t attach_status = ATTACH_SUCCESS;

  if ( (ph = (struct ps_prochandle*) calloc(1, sizeof(struct ps_prochandle))) == NULL) {
    snprintf(err_buf, err_buf_len, "can't allocate memory for ps_prochandle");
    print_debug("%s\n", err_buf);
    return NULL;
  }

  if ((attach_status = ptrace_attach(pid, err_buf, err_buf_len)) != ATTACH_SUCCESS) {
    if (attach_status == ATTACH_THREAD_DEAD) {
       print_error("The process with pid %d does not exist.\n", pid);
    }
    free(ph);
    return NULL;
  }

  // initialize ps_prochandle
  ph->pid = pid;
  add_thread_info(ph, ph->pid);

  // initialize vtable
  ph->ops = &process_ops;

  // read library info and symbol tables, must do this before attaching threads,
  // as the symbols in the pthread library will be used to figure out
  // the list of threads within the same process.
  read_lib_info(ph);

  /*
   * Read thread info.
   * SA scans all tasks in /proc/<PID>/task to read all threads info.
   */
  char taskpath[PATH_MAX];
  DIR *dirp;
  struct dirent *entry;

  snprintf(taskpath, PATH_MAX, "/proc/%d/task", ph->pid);
  dirp = opendir(taskpath);
  int lwp_id;
  while ((entry = readdir(dirp)) != NULL) {
    if (*entry->d_name == '.') {
      continue;
    }
    lwp_id = atoi(entry->d_name);
    if (lwp_id == ph->pid) {
      continue;
    }
    if (!process_doesnt_exist(lwp_id)) {
      add_thread_info(ph, lwp_id);
    }
  }
  closedir(dirp);

  // attach to the threads
  thr = ph->threads;

  while (thr) {
    thread_info* current_thr = thr;
    thr = thr->next;
    // don't attach to the main thread again
    if (ph->pid != current_thr->lwp_id) {
      if ((attach_status = ptrace_attach(current_thr->lwp_id, err_buf, err_buf_len)) != ATTACH_SUCCESS) {
        if (attach_status == ATTACH_THREAD_DEAD) {
          // Remove this thread from the threads list
          delete_thread_info(ph, current_thr);
        }
        else {
          Prelease(ph);
          return NULL;
        } // ATTACH_THREAD_DEAD
      } // !ATTACH_SUCCESS
    }
  }
  return ph;
}
