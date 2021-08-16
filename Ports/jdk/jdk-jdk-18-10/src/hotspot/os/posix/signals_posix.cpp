/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"

#include "jvm.h"
#include "logging/log.hpp"
#include "runtime/atomic.hpp"
#include "runtime/globals.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/java.hpp"
#include "runtime/os.hpp"
#include "runtime/osThread.hpp"
#include "runtime/semaphore.inline.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/thread.hpp"
#include "signals_posix.hpp"
#include "utilities/events.hpp"
#include "utilities/ostream.hpp"
#include "utilities/vmError.hpp"

#ifdef ZERO
// See stubGenerator_zero.cpp
#include <setjmp.h>
extern sigjmp_buf* get_jmp_buf_for_continuation();
#endif

#include <signal.h>


static const char* get_signal_name(int sig, char* out, size_t outlen);

// Returns address of a handler associated with the given sigaction
static address get_signal_handler(const struct sigaction* action);

#define HANDLER_IS(handler, address)    ((handler) == CAST_FROM_FN_PTR(void*, (address)))
#define HANDLER_IS_IGN(handler)         (HANDLER_IS(handler, SIG_IGN))
#define HANDLER_IS_DFL(handler)         (HANDLER_IS(handler, SIG_DFL))
#define HANDLER_IS_IGN_OR_DFL(handler)  (HANDLER_IS_IGN(handler) || HANDLER_IS_DFL(handler))

// Various signal related mechanism are laid out in the following order:
//
// sun.misc.Signal
// signal chaining
// signal handling (except suspend/resume)
// suspend/resume

// Helper function to strip any flags from a sigaction sa_flag
// which are not needed for semantic comparison (see remarks below
// about SA_RESTORER on Linux).
// Also to work around the fact that not all platforms define sa_flags
// as signed int (looking at you, zlinux).
static int get_sanitized_sa_flags(const struct sigaction* sa) {
  int f = (int) sa->sa_flags;
#ifdef LINUX
  // Glibc on Linux uses the SA_RESTORER flag to indicate
  // the use of a "signal trampoline". We have no interest
  // in this flag and need to ignore it when checking our
  // own flag settings.
  // Note: SA_RESTORER is not exposed through signal.h so we
  // have to hardcode its 0x04000000 value here.
  const int sa_restorer_flag = 0x04000000;
  f &= ~sa_restorer_flag;
#endif // LINUX
  return f;
}

// Todo: provide a os::get_max_process_id() or similar. Number of processes
// may have been configured, can be read more accurately from proc fs etc.
#ifndef MAX_PID
  #define MAX_PID INT_MAX
#endif
#define IS_VALID_PID(p) (p > 0 && p < MAX_PID)

#define NUM_IMPORTANT_SIGS 32

extern "C" {
  typedef void (*sa_handler_t)(int);
  typedef void (*sa_sigaction_t)(int, siginfo_t *, void *);
}

// At various places we store handler information for each installed handler.
//  SavedSignalHandlers is a helper class for those cases, keeping an array of sigaction
//  structures.
class SavedSignalHandlers {
  // Note: NSIG can be largish, depending on platform, and this array is expected
  // to be sparsely populated. To save space the contained structures are
  // C-heap allocated. Since they only get added outside of signal handling
  // this is no problem.
  struct sigaction* _sa[NSIG];

  bool check_signal_number(int sig) const {
    assert(sig > 0 && sig < NSIG, "invalid signal number %d", sig);
    return sig > 0 && sig < NSIG;
  }

public:

  SavedSignalHandlers() {
    ::memset(_sa, 0, sizeof(_sa));
  }

  ~SavedSignalHandlers() {
    for (int i = 0; i < NSIG; i ++) {
      FREE_C_HEAP_OBJ(_sa[i]);
    }
  }

  void set(int sig, const struct sigaction* act) {
    if (check_signal_number(sig)) {
      assert(_sa[sig] == NULL, "Overwriting signal handler?");
      _sa[sig] = NEW_C_HEAP_OBJ(struct sigaction, mtInternal);
      *_sa[sig] = *act;
    }
  }

  const struct sigaction* get(int sig) const {
    if (check_signal_number(sig)) {
      return _sa[sig];
    }
    return NULL;
  }
};


debug_only(static bool signal_sets_initialized = false);
static sigset_t unblocked_sigs, vm_sigs, preinstalled_sigs;

// Our own signal handlers should never ever get replaced by a third party one.
//  To check that, and to aid with diagnostics, store a copy of the handler setup
//  and compare it periodically against reality (see os::run_periodic_checks()).
static bool check_signals = true;
static SavedSignalHandlers vm_handlers;
static bool do_check_signal_periodically[NSIG] = { 0 };

// For signal-chaining:
//  if chaining is active, chained_handlers contains all handlers which we
//  replaced with our own and to which we must delegate.
static SavedSignalHandlers chained_handlers;
static bool libjsig_is_loaded = false;
typedef struct sigaction *(*get_signal_t)(int);
static get_signal_t get_signal_action = NULL;

// suspend/resume support
#if defined(__APPLE__)
  static OSXSemaphore sr_semaphore;
#else
  static PosixSemaphore sr_semaphore;
#endif

// Signal number used to suspend/resume a thread
// do not use any signal number less than SIGSEGV, see 4355769
int PosixSignals::SR_signum = SIGUSR2;

// sun.misc.Signal support
static Semaphore* sig_semaphore = NULL;
// a counter for each possible signal value
static volatile jint pending_signals[NSIG+1] = { 0 };

static const struct {
  int sig; const char* name;
} g_signal_info[] = {
  {  SIGABRT,     "SIGABRT" },
#ifdef SIGAIO
  {  SIGAIO,      "SIGAIO" },
#endif
  {  SIGALRM,     "SIGALRM" },
#ifdef SIGALRM1
  {  SIGALRM1,    "SIGALRM1" },
#endif
  {  SIGBUS,      "SIGBUS" },
#ifdef SIGCANCEL
  {  SIGCANCEL,   "SIGCANCEL" },
#endif
  {  SIGCHLD,     "SIGCHLD" },
#ifdef SIGCLD
  {  SIGCLD,      "SIGCLD" },
#endif
  {  SIGCONT,     "SIGCONT" },
#ifdef SIGCPUFAIL
  {  SIGCPUFAIL,  "SIGCPUFAIL" },
#endif
#ifdef SIGDANGER
  {  SIGDANGER,   "SIGDANGER" },
#endif
#ifdef SIGDIL
  {  SIGDIL,      "SIGDIL" },
#endif
#ifdef SIGEMT
  {  SIGEMT,      "SIGEMT" },
#endif
  {  SIGFPE,      "SIGFPE" },
#ifdef SIGFREEZE
  {  SIGFREEZE,   "SIGFREEZE" },
#endif
#ifdef SIGGFAULT
  {  SIGGFAULT,   "SIGGFAULT" },
#endif
#ifdef SIGGRANT
  {  SIGGRANT,    "SIGGRANT" },
#endif
  {  SIGHUP,      "SIGHUP" },
  {  SIGILL,      "SIGILL" },
#ifdef SIGINFO
  {  SIGINFO,     "SIGINFO" },
#endif
  {  SIGINT,      "SIGINT" },
#ifdef SIGIO
  {  SIGIO,       "SIGIO" },
#endif
#ifdef SIGIOINT
  {  SIGIOINT,    "SIGIOINT" },
#endif
#ifdef SIGIOT
// SIGIOT is there for BSD compatibility, but on most Unices just a
// synonym for SIGABRT. The result should be "SIGABRT", not
// "SIGIOT".
#if (SIGIOT != SIGABRT )
  {  SIGIOT,      "SIGIOT" },
#endif
#endif
#ifdef SIGKAP
  {  SIGKAP,      "SIGKAP" },
#endif
  {  SIGKILL,     "SIGKILL" },
#ifdef SIGLOST
  {  SIGLOST,     "SIGLOST" },
#endif
#ifdef SIGLWP
  {  SIGLWP,      "SIGLWP" },
#endif
#ifdef SIGLWPTIMER
  {  SIGLWPTIMER, "SIGLWPTIMER" },
#endif
#ifdef SIGMIGRATE
  {  SIGMIGRATE,  "SIGMIGRATE" },
#endif
#ifdef SIGMSG
  {  SIGMSG,      "SIGMSG" },
#endif
  {  SIGPIPE,     "SIGPIPE" },
#ifdef SIGPOLL
  {  SIGPOLL,     "SIGPOLL" },
#endif
#ifdef SIGPRE
  {  SIGPRE,      "SIGPRE" },
#endif
  {  SIGPROF,     "SIGPROF" },
#ifdef SIGPTY
  {  SIGPTY,      "SIGPTY" },
#endif
#ifdef SIGPWR
  {  SIGPWR,      "SIGPWR" },
#endif
  {  SIGQUIT,     "SIGQUIT" },
#ifdef SIGRECONFIG
  {  SIGRECONFIG, "SIGRECONFIG" },
#endif
#ifdef SIGRECOVERY
  {  SIGRECOVERY, "SIGRECOVERY" },
#endif
#ifdef SIGRESERVE
  {  SIGRESERVE,  "SIGRESERVE" },
#endif
#ifdef SIGRETRACT
  {  SIGRETRACT,  "SIGRETRACT" },
#endif
#ifdef SIGSAK
  {  SIGSAK,      "SIGSAK" },
#endif
  {  SIGSEGV,     "SIGSEGV" },
#ifdef SIGSOUND
  {  SIGSOUND,    "SIGSOUND" },
#endif
#ifdef SIGSTKFLT
  {  SIGSTKFLT,    "SIGSTKFLT" },
#endif
  {  SIGSTOP,     "SIGSTOP" },
  {  SIGSYS,      "SIGSYS" },
#ifdef SIGSYSERROR
  {  SIGSYSERROR, "SIGSYSERROR" },
#endif
#ifdef SIGTALRM
  {  SIGTALRM,    "SIGTALRM" },
#endif
  {  SIGTERM,     "SIGTERM" },
#ifdef SIGTHAW
  {  SIGTHAW,     "SIGTHAW" },
#endif
  {  SIGTRAP,     "SIGTRAP" },
#ifdef SIGTSTP
  {  SIGTSTP,     "SIGTSTP" },
#endif
  {  SIGTTIN,     "SIGTTIN" },
  {  SIGTTOU,     "SIGTTOU" },
#ifdef SIGURG
  {  SIGURG,      "SIGURG" },
#endif
  {  SIGUSR1,     "SIGUSR1" },
  {  SIGUSR2,     "SIGUSR2" },
#ifdef SIGVIRT
  {  SIGVIRT,     "SIGVIRT" },
#endif
  {  SIGVTALRM,   "SIGVTALRM" },
#ifdef SIGWAITING
  {  SIGWAITING,  "SIGWAITING" },
#endif
#ifdef SIGWINCH
  {  SIGWINCH,    "SIGWINCH" },
#endif
#ifdef SIGWINDOW
  {  SIGWINDOW,   "SIGWINDOW" },
#endif
  {  SIGXCPU,     "SIGXCPU" },
  {  SIGXFSZ,     "SIGXFSZ" },
#ifdef SIGXRES
  {  SIGXRES,     "SIGXRES" },
#endif
  { -1, NULL }
};

////////////////////////////////////////////////////////////////////////////////
// sun.misc.Signal support

void jdk_misc_signal_init() {
  // Initialize signal structures
  ::memset((void*)pending_signals, 0, sizeof(pending_signals));

  // Initialize signal semaphore
  sig_semaphore = new Semaphore();
}

void os::signal_notify(int sig) {
  if (sig_semaphore != NULL) {
    Atomic::inc(&pending_signals[sig]);
    sig_semaphore->signal();
  } else {
    // Signal thread is not created with ReduceSignalUsage and jdk_misc_signal_init
    // initialization isn't called.
    assert(ReduceSignalUsage, "signal semaphore should be created");
  }
}

static int check_pending_signals() {
  for (;;) {
    for (int i = 0; i < NSIG + 1; i++) {
      jint n = pending_signals[i];
      if (n > 0 && n == Atomic::cmpxchg(&pending_signals[i], n, n - 1)) {
        return i;
      }
    }
    sig_semaphore->wait_with_safepoint_check(JavaThread::current());
  }
  ShouldNotReachHere();
  return 0; // Satisfy compiler
}

int os::signal_wait() {
  return check_pending_signals();
}

////////////////////////////////////////////////////////////////////////////////
// signal chaining support

struct sigaction* get_chained_signal_action(int sig) {
  struct sigaction *actp = NULL;

  if (libjsig_is_loaded) {
    // Retrieve the old signal handler from libjsig
    actp = (*get_signal_action)(sig);
  }
  if (actp == NULL) {
    // Retrieve the preinstalled signal handler from jvm
    actp = const_cast<struct sigaction*>(chained_handlers.get(sig));
  }

  return actp;
}

static bool call_chained_handler(struct sigaction *actp, int sig,
                                 siginfo_t *siginfo, void *context) {
  // Call the old signal handler
  if (actp->sa_handler == SIG_DFL) {
    // It's more reasonable to let jvm treat it as an unexpected exception
    // instead of taking the default action.
    return false;
  } else if (actp->sa_handler != SIG_IGN) {
    if ((actp->sa_flags & SA_NODEFER) == 0) {
      // automaticlly block the signal
      sigaddset(&(actp->sa_mask), sig);
    }

    sa_handler_t hand = NULL;
    sa_sigaction_t sa = NULL;
    bool siginfo_flag_set = (actp->sa_flags & SA_SIGINFO) != 0;
    // retrieve the chained handler
    if (siginfo_flag_set) {
      sa = actp->sa_sigaction;
    } else {
      hand = actp->sa_handler;
    }

    if ((actp->sa_flags & SA_RESETHAND) != 0) {
      actp->sa_handler = SIG_DFL;
    }

    // try to honor the signal mask
    sigset_t oset;
    sigemptyset(&oset);
    pthread_sigmask(SIG_SETMASK, &(actp->sa_mask), &oset);

    // call into the chained handler
    if (siginfo_flag_set) {
      (*sa)(sig, siginfo, context);
    } else {
      (*hand)(sig);
    }

    // restore the signal mask
    pthread_sigmask(SIG_SETMASK, &oset, NULL);
  }
  // Tell jvm's signal handler the signal is taken care of.
  return true;
}

bool PosixSignals::chained_handler(int sig, siginfo_t* siginfo, void* context) {
  bool chained = false;
  // signal-chaining
  if (UseSignalChaining) {
    struct sigaction *actp = get_chained_signal_action(sig);
    if (actp != NULL) {
      chained = call_chained_handler(actp, sig, siginfo, context);
    }
  }
  return chained;
}

///// Synchronous (non-deferrable) error signals (ILL, SEGV, FPE, BUS, TRAP):

// These signals are special because they cannot be deferred and, if they
// happen while delivery is blocked for the receiving thread, will cause UB
// (in practice typically resulting in sudden process deaths or hangs, see
// JDK-8252533). So we must take care never to block them when we cannot be
// absolutely sure they won't happen. In practice, this is always.
//
// Relevant Posix quote:
// "The behavior of a process is undefined after it ignores a SIGFPE, SIGILL,
//  SIGSEGV, or SIGBUS signal that was not generated by kill(), sigqueue(), or
//  raise()."
//
// We also include SIGTRAP in that list of never-to-block-signals. While not
// mentioned by the Posix documentation, in our (SAPs) experience blocking it
// causes similar problems. Beside, during normal operation - outside of error
// handling - SIGTRAP may be used for implicit NULL checking, so it makes sense
// to never block it.
//
// We deal with those signals in two ways:
// - we just never explicitly block them, which includes not accidentally blocking
//   them via sa_mask when establishing signal handlers.
// - as an additional safety measure, at the entrance of a signal handler, we
//   unblock them explicitly.

static void add_error_signals_to_set(sigset_t* set) {
  sigaddset(set, SIGILL);
  sigaddset(set, SIGBUS);
  sigaddset(set, SIGFPE);
  sigaddset(set, SIGSEGV);
  sigaddset(set, SIGTRAP);
}

static void remove_error_signals_from_set(sigset_t* set) {
  sigdelset(set, SIGILL);
  sigdelset(set, SIGBUS);
  sigdelset(set, SIGFPE);
  sigdelset(set, SIGSEGV);
  sigdelset(set, SIGTRAP);
}

// Unblock all signals whose delivery cannot be deferred and which, if they happen
//  while delivery is blocked, would cause crashes or hangs (JDK-8252533).
void PosixSignals::unblock_error_signals() {
  sigset_t set;
  sigemptyset(&set);
  add_error_signals_to_set(&set);
  ::pthread_sigmask(SIG_UNBLOCK, &set, NULL);
}

class ErrnoPreserver: public StackObj {
  const int _saved;
public:
  ErrnoPreserver() : _saved(errno) {}
  ~ErrnoPreserver() { errno = _saved; }
};

////////////////////////////////////////////////////////////////////////////////
// JVM_handle_(linux|aix|bsd)_signal()

// This routine is the shared part of the central hotspot signal handler. It can
// also be called by a user application, if a user application prefers to do
// signal handling itself - in that case it needs to pass signals the VM
// internally uses on to the VM first.
//
// The user-defined signal handler must pass unrecognized signals to this
// routine, and if it returns true (non-zero), then the signal handler must
// return immediately.  If the flag "abort_if_unrecognized" is true, then this
// routine will never return false (zero), but instead will execute a VM panic
// routine to kill the process.
//
// If this routine returns false, it is OK to call it again.  This allows
// the user-defined signal handler to perform checks either before or after
// the VM performs its own checks.  Naturally, the user code would be making
// a serious error if it tried to handle an exception (such as a null check
// or breakpoint) that the VM was generating for its own correct operation.
//
// This routine may recognize any of the following kinds of signals:
//    SIGBUS, SIGSEGV, SIGILL, SIGFPE, SIGQUIT, SIGPIPE, SIGXFSZ, SIGUSR1.
// It should be consulted by handlers for any of those signals.
//
// The caller of this routine must pass in the three arguments supplied
// to the function referred to in the "sa_sigaction" (not the "sa_handler")
// field of the structure passed to sigaction().  This routine assumes that
// the sa_flags field passed to sigaction() includes SA_SIGINFO and SA_RESTART.
//
// Note that the VM will print warnings if it detects conflicting signal
// handlers, unless invoked with the option "-XX:+AllowUserSignalHandlers".
//

#if defined(BSD)
#define JVM_HANDLE_XXX_SIGNAL JVM_handle_bsd_signal
#elif defined(AIX)
#define JVM_HANDLE_XXX_SIGNAL JVM_handle_aix_signal
#elif defined(LINUX)
#define JVM_HANDLE_XXX_SIGNAL JVM_handle_linux_signal
#else
#error who are you?
#endif

extern "C" JNIEXPORT
int JVM_HANDLE_XXX_SIGNAL(int sig, siginfo_t* info,
                          void* ucVoid, int abort_if_unrecognized)
{
  assert(info != NULL && ucVoid != NULL, "sanity");

  // Note: it's not uncommon that JNI code uses signal/sigset to install,
  // then restore certain signal handler (e.g. to temporarily block SIGPIPE,
  // or have a SIGILL handler when detecting CPU type). When that happens,
  // this handler might be invoked with junk info/ucVoid. To avoid unnecessary
  // crash when libjsig is not preloaded, try handle signals that do not require
  // siginfo/ucontext first.

  // Preserve errno value over signal handler.
  //  (note: RAII ok here, even with JFR thread crash protection, see below).
  ErrnoPreserver ep;

  // Unblock all synchronous error signals (see JDK-8252533)
  PosixSignals::unblock_error_signals();

  ucontext_t* const uc = (ucontext_t*) ucVoid;
  Thread* const t = Thread::current_or_null_safe();

  // Handle JFR thread crash protection.
  //  Note: this may cause us to longjmp away. Do not use any code before this
  //  point which really needs any form of epilogue code running, eg RAII objects.
  os::ThreadCrashProtection::check_crash_protection(sig, t);

  bool signal_was_handled = false;

  // Handle assertion poison page accesses.
#ifdef CAN_SHOW_REGISTERS_ON_ASSERT
  if (!signal_was_handled &&
      ((sig == SIGSEGV || sig == SIGBUS) && info != NULL && info->si_addr == g_assert_poison)) {
    signal_was_handled = handle_assert_poison_fault(ucVoid, info->si_addr);
  }
#endif

  if (!signal_was_handled) {
    // Handle SafeFetch access.
#ifndef ZERO
    if (uc != NULL) {
      address pc = os::Posix::ucontext_get_pc(uc);
      if (StubRoutines::is_safefetch_fault(pc)) {
        os::Posix::ucontext_set_pc(uc, StubRoutines::continuation_for_safefetch_fault(pc));
        signal_was_handled = true;
      }
    }
#else
    // See JDK-8076185
    if (sig == SIGSEGV || sig == SIGBUS) {
      sigjmp_buf* const pjb = get_jmp_buf_for_continuation();
      if (pjb) {
        siglongjmp(*pjb, 1);
      }
    }
#endif // ZERO
  }

  // Ignore SIGPIPE and SIGXFSZ (4229104, 6499219).
  if (!signal_was_handled &&
      (sig == SIGPIPE || sig == SIGXFSZ)) {
    PosixSignals::chained_handler(sig, info, ucVoid);
    signal_was_handled = true; // unconditionally.
  }

  // Call platform dependent signal handler.
  if (!signal_was_handled) {
    JavaThread* const jt = (t != NULL && t->is_Java_thread()) ? (JavaThread*) t : NULL;
    signal_was_handled = PosixSignals::pd_hotspot_signal_handler(sig, info, uc, jt);
  }

  // From here on, if the signal had not been handled, it is a fatal error.

  // Give the chained signal handler - should it exist - a shot.
  if (!signal_was_handled) {
    signal_was_handled = PosixSignals::chained_handler(sig, info, ucVoid);
  }

  // Invoke fatal error handling.
  if (!signal_was_handled && abort_if_unrecognized) {
    // Extract pc from context for the error handler to display.
    address pc = NULL;
    if (uc != NULL) {
      // prepare fault pc address for error reporting.
      if (S390_ONLY(sig == SIGILL || sig == SIGFPE) NOT_S390(false)) {
        pc = (address)info->si_addr;
      } else if (ZERO_ONLY(true) NOT_ZERO(false)) {
        // Non-arch-specific Zero code does not really know the pc.
        // This can be alleviated by making arch-specific os::Posix::ucontext_get_pc
        // available for Zero for known architectures. But for generic Zero
        // code, it would still remain unknown.
        pc = NULL;
      } else {
        pc = os::Posix::ucontext_get_pc(uc);
      }
    }
    // For Zero, we ignore the crash context, because:
    //  a) The crash would be in C++ interpreter code, so context is not really relevant;
    //  b) Generic Zero code would not be able to parse it, so when generic error
    //     reporting code asks e.g. about frames on stack, Zero would experience
    //     a secondary ShouldNotCallThis() crash.
    VMError::report_and_die(t, sig, pc, info, NOT_ZERO(ucVoid) ZERO_ONLY(NULL));
    // VMError should not return.
    ShouldNotReachHere();
  }
  return signal_was_handled;
}

// Entry point for the hotspot signal handler.
static void javaSignalHandler(int sig, siginfo_t* info, void* ucVoid) {
  // Do not add any code here!
  // Only add code to either JVM_HANDLE_XXX_SIGNAL or PosixSignals::pd_hotspot_signal_handler.
  (void)JVM_HANDLE_XXX_SIGNAL(sig, info, ucVoid, true);
}

static void UserHandler(int sig, void *siginfo, void *context) {

  PosixSignals::unblock_error_signals();

  // Ctrl-C is pressed during error reporting, likely because the error
  // handler fails to abort. Let VM die immediately.
  if (sig == SIGINT && VMError::is_error_reported()) {
    os::die();
  }

  os::signal_notify(sig);
}

static void print_signal_handler_name(outputStream* os, address handler, char* buf, size_t buflen) {
  // We demangle, but omit arguments - signal handlers should have always the same prototype.
  os::print_function_and_library_name(os, handler, buf, buflen,
                                       true, // shorten_path
                                       true, // demangle
                                       true  // omit arguments
                                       );
}

// Writes one-line description of a combination of sigaction.sa_flags into a user
// provided buffer. Returns that buffer.
static const char* describe_sa_flags(int flags, char* buffer, size_t size) {
  char* p = buffer;
  size_t remaining = size;
  bool first = true;
  int idx = 0;

  assert(buffer, "invalid argument");

  if (size == 0) {
    return buffer;
  }

  strncpy(buffer, "none", size);

  const unsigned int unknown_flag = ~(SA_NOCLDSTOP |
                                      SA_ONSTACK   |
                                      SA_NOCLDSTOP |
                                      SA_RESTART   |
                                      SA_SIGINFO   |
                                      SA_NOCLDWAIT |
                                      SA_NODEFER
                                      AIX_ONLY(| SA_OLDSTYLE)
                                      );

  const struct {
    // NB: i is an unsigned int here because SA_RESETHAND is on some
    // systems 0x80000000, which is implicitly unsigned.  Assigning
    // it to an int field would be an overflow in unsigned-to-signed
    // conversion.
    unsigned int i;
    const char* s;
  } flaginfo [] = {
    { SA_NOCLDSTOP, "SA_NOCLDSTOP" },
    { SA_ONSTACK,   "SA_ONSTACK"   },
    { SA_RESETHAND, "SA_RESETHAND" },
    { SA_RESTART,   "SA_RESTART"   },
    { SA_SIGINFO,   "SA_SIGINFO"   },
    { SA_NOCLDWAIT, "SA_NOCLDWAIT" },
    { SA_NODEFER,   "SA_NODEFER"   },
#if defined(AIX)
    { SA_OLDSTYLE,  "SA_OLDSTYLE"  },
#endif
    { unknown_flag, "NOT USED"     }
  };

  for (idx = 0; flaginfo[idx].i != unknown_flag && remaining > 1; idx++) {
    if (flags & flaginfo[idx].i) {
      if (first) {
        jio_snprintf(p, remaining, "%s", flaginfo[idx].s);
        first = false;
      } else {
        jio_snprintf(p, remaining, "|%s", flaginfo[idx].s);
      }
      const size_t len = strlen(p);
      p += len;
      remaining -= len;
    }
  }
  unsigned int unknowns = flags & unknown_flag;
  if (unknowns != 0) {
    jio_snprintf(p, remaining, "|Unknown_flags:%x", unknowns);
  }

  buffer[size - 1] = '\0';

  return buffer;
}

// Prints one-line description of a combination of sigaction.sa_flags.
static void print_sa_flags(outputStream* st, int flags) {
  char buffer[0x100];
  describe_sa_flags(flags, buffer, sizeof(buffer));
  st->print("%s", buffer);
}

// Implementation may use the same storage for both the sa_sigaction field and the sa_handler field,
// so check for "sigAct.sa_flags == SA_SIGINFO"
static address get_signal_handler(const struct sigaction* action) {
  bool siginfo_flag_set = (action->sa_flags & SA_SIGINFO) != 0;
  if (siginfo_flag_set) {
    return CAST_FROM_FN_PTR(address, action->sa_sigaction);
  } else {
    return CAST_FROM_FN_PTR(address, action->sa_handler);
  }
}

typedef int (*os_sigaction_t)(int, const struct sigaction *, struct sigaction *);

static void SR_handler(int sig, siginfo_t* siginfo, ucontext_t* context);

// Semantically compare two sigaction structures. Return true if they are referring to
// the same handler, using the same flags.
static bool are_handlers_equal(const struct sigaction* sa,
                               const struct sigaction* expected_sa) {
  address this_handler = get_signal_handler(sa);
  address expected_handler = get_signal_handler(expected_sa);
  const int this_flags = get_sanitized_sa_flags(sa);
  const int expected_flags = get_sanitized_sa_flags(expected_sa);
  return (this_handler == expected_handler) &&
         (this_flags == expected_flags);
}

// If we installed one of our signal handlers for sig, check that the current
//  setup matches what we originally installed.
static void check_signal_handler(int sig) {
  char buf[O_BUFLEN];
  bool mismatch = false;

  if (!do_check_signal_periodically[sig]) {
    return;
  }

  const struct sigaction* expected_act = vm_handlers.get(sig);
  assert(expected_act != NULL, "Sanity");

  // Retrieve current signal setup.
  struct sigaction act;
  static os_sigaction_t os_sigaction = NULL;
  if (os_sigaction == NULL) {
    // only trust the default sigaction, in case it has been interposed
    os_sigaction = (os_sigaction_t)dlsym(RTLD_DEFAULT, "sigaction");
    if (os_sigaction == NULL) return;
  }

  os_sigaction(sig, (struct sigaction*)NULL, &act);

  // Compare both sigaction structures (intelligently; only the members we care about).
  if (!are_handlers_equal(&act, expected_act)) {
    tty->print_cr("Warning: %s handler modified!", os::exception_name(sig, buf, sizeof(buf)));
    // If we had a mismatch:
    // - print all signal handlers. As part of that printout, details will be printed
    //   about any modified handlers.
    // - Disable any further checks for this signal - we do not want to flood stdout. Though
    //   depending on which signal had been overwritten, we may die very soon anyway.
    os::print_signal_handlers(tty, buf, O_BUFLEN);
    do_check_signal_periodically[sig] = false;
    tty->print_cr("Consider using jsig library.");
    // Running under non-interactive shell, SHUTDOWN2_SIGNAL will be reassigned SIG_IGN
    if (sig == SHUTDOWN2_SIGNAL && !isatty(fileno(stdin))) {
      tty->print_cr("Note: Running in non-interactive shell, %s handler is replaced by shell",
                    os::exception_name(sig, buf, O_BUFLEN));
    }
  }
}

void* os::user_handler() {
  return CAST_FROM_FN_PTR(void*, UserHandler);
}

void* os::signal(int signal_number, void* handler) {
  struct sigaction sigAct, oldSigAct;

  sigfillset(&(sigAct.sa_mask));
  remove_error_signals_from_set(&(sigAct.sa_mask));

  sigAct.sa_flags   = SA_RESTART|SA_SIGINFO;
  sigAct.sa_handler = CAST_TO_FN_PTR(sa_handler_t, handler);

  if (sigaction(signal_number, &sigAct, &oldSigAct)) {
    // -1 means registration failed
    return (void *)-1;
  }

  return get_signal_handler(&oldSigAct);
}

void os::signal_raise(int signal_number) {
  ::raise(signal_number);
}

// Will be modified when max signal is changed to be dynamic
int os::sigexitnum_pd() {
  return NSIG;
}

// This method is a periodic task to check for misbehaving JNI applications
// under CheckJNI, we can add any periodic checks here
void os::run_periodic_checks() {

  if (check_signals == false) return;

  // SEGV and BUS if overridden could potentially prevent
  // generation of hs*.log in the event of a crash, debugging
  // such a case can be very challenging, so we absolutely
  // check the following for a good measure:
  check_signal_handler(SIGSEGV);
  check_signal_handler(SIGILL);
  check_signal_handler(SIGFPE);
  check_signal_handler(SIGBUS);
  check_signal_handler(SIGPIPE);
  check_signal_handler(SIGXFSZ);
  PPC64_ONLY(check_signal_handler(SIGTRAP);)

  // ReduceSignalUsage allows the user to override these handlers
  // see comments at the very top and jvm_md.h
  if (!ReduceSignalUsage) {
    check_signal_handler(SHUTDOWN1_SIGNAL);
    check_signal_handler(SHUTDOWN2_SIGNAL);
    check_signal_handler(SHUTDOWN3_SIGNAL);
    check_signal_handler(BREAK_SIGNAL);
  }

  check_signal_handler(PosixSignals::SR_signum);
}

// Helper function for PosixSignals::print_siginfo_...():
// return a textual description for signal code.
struct enum_sigcode_desc_t {
  const char* s_name;
  const char* s_desc;
};

static bool get_signal_code_description(const siginfo_t* si, enum_sigcode_desc_t* out) {

  const struct {
    int sig; int code; const char* s_code; const char* s_desc;
  } t1 [] = {
    { SIGILL,  ILL_ILLOPC,   "ILL_ILLOPC",   "Illegal opcode." },
    { SIGILL,  ILL_ILLOPN,   "ILL_ILLOPN",   "Illegal operand." },
    { SIGILL,  ILL_ILLADR,   "ILL_ILLADR",   "Illegal addressing mode." },
    { SIGILL,  ILL_ILLTRP,   "ILL_ILLTRP",   "Illegal trap." },
    { SIGILL,  ILL_PRVOPC,   "ILL_PRVOPC",   "Privileged opcode." },
    { SIGILL,  ILL_PRVREG,   "ILL_PRVREG",   "Privileged register." },
    { SIGILL,  ILL_COPROC,   "ILL_COPROC",   "Coprocessor error." },
    { SIGILL,  ILL_BADSTK,   "ILL_BADSTK",   "Internal stack error." },
#if defined(IA64) && defined(LINUX)
    { SIGILL,  ILL_BADIADDR, "ILL_BADIADDR", "Unimplemented instruction address" },
    { SIGILL,  ILL_BREAK,    "ILL_BREAK",    "Application Break instruction" },
#endif
    { SIGFPE,  FPE_INTDIV,   "FPE_INTDIV",   "Integer divide by zero." },
    { SIGFPE,  FPE_INTOVF,   "FPE_INTOVF",   "Integer overflow." },
    { SIGFPE,  FPE_FLTDIV,   "FPE_FLTDIV",   "Floating-point divide by zero." },
    { SIGFPE,  FPE_FLTOVF,   "FPE_FLTOVF",   "Floating-point overflow." },
    { SIGFPE,  FPE_FLTUND,   "FPE_FLTUND",   "Floating-point underflow." },
    { SIGFPE,  FPE_FLTRES,   "FPE_FLTRES",   "Floating-point inexact result." },
    { SIGFPE,  FPE_FLTINV,   "FPE_FLTINV",   "Invalid floating-point operation." },
    { SIGFPE,  FPE_FLTSUB,   "FPE_FLTSUB",   "Subscript out of range." },
    { SIGSEGV, SEGV_MAPERR,  "SEGV_MAPERR",  "Address not mapped to object." },
    { SIGSEGV, SEGV_ACCERR,  "SEGV_ACCERR",  "Invalid permissions for mapped object." },
#if defined(AIX)
    // no explanation found what keyerr would be
    { SIGSEGV, SEGV_KEYERR,  "SEGV_KEYERR",  "key error" },
#endif
#if defined(IA64) && !defined(AIX)
    { SIGSEGV, SEGV_PSTKOVF, "SEGV_PSTKOVF", "Paragraph stack overflow" },
#endif
    { SIGBUS,  BUS_ADRALN,   "BUS_ADRALN",   "Invalid address alignment." },
    { SIGBUS,  BUS_ADRERR,   "BUS_ADRERR",   "Nonexistent physical address." },
    { SIGBUS,  BUS_OBJERR,   "BUS_OBJERR",   "Object-specific hardware error." },
    { SIGTRAP, TRAP_BRKPT,   "TRAP_BRKPT",   "Process breakpoint." },
    { SIGTRAP, TRAP_TRACE,   "TRAP_TRACE",   "Process trace trap." },
    { SIGCHLD, CLD_EXITED,   "CLD_EXITED",   "Child has exited." },
    { SIGCHLD, CLD_KILLED,   "CLD_KILLED",   "Child has terminated abnormally and did not create a core file." },
    { SIGCHLD, CLD_DUMPED,   "CLD_DUMPED",   "Child has terminated abnormally and created a core file." },
    { SIGCHLD, CLD_TRAPPED,  "CLD_TRAPPED",  "Traced child has trapped." },
    { SIGCHLD, CLD_STOPPED,  "CLD_STOPPED",  "Child has stopped." },
    { SIGCHLD, CLD_CONTINUED,"CLD_CONTINUED","Stopped child has continued." },
#ifdef SIGPOLL
    { SIGPOLL, POLL_OUT,     "POLL_OUT",     "Output buffers available." },
    { SIGPOLL, POLL_MSG,     "POLL_MSG",     "Input message available." },
    { SIGPOLL, POLL_ERR,     "POLL_ERR",     "I/O error." },
    { SIGPOLL, POLL_PRI,     "POLL_PRI",     "High priority input available." },
    { SIGPOLL, POLL_HUP,     "POLL_HUP",     "Device disconnected. [Option End]" },
#endif
    { -1, -1, NULL, NULL }
  };

  // Codes valid in any signal context.
  const struct {
    int code; const char* s_code; const char* s_desc;
  } t2 [] = {
    { SI_USER,      "SI_USER",     "Signal sent by kill()." },
    { SI_QUEUE,     "SI_QUEUE",    "Signal sent by the sigqueue()." },
    { SI_TIMER,     "SI_TIMER",    "Signal generated by expiration of a timer set by timer_settime()." },
    { SI_ASYNCIO,   "SI_ASYNCIO",  "Signal generated by completion of an asynchronous I/O request." },
    { SI_MESGQ,     "SI_MESGQ",    "Signal generated by arrival of a message on an empty message queue." },
    // Linux specific
#ifdef SI_TKILL
    { SI_TKILL,     "SI_TKILL",    "Signal sent by tkill (pthread_kill)" },
#endif
#ifdef SI_DETHREAD
    { SI_DETHREAD,  "SI_DETHREAD", "Signal sent by execve() killing subsidiary threads" },
#endif
#ifdef SI_KERNEL
    { SI_KERNEL,    "SI_KERNEL",   "Signal sent by kernel." },
#endif
#ifdef SI_SIGIO
    { SI_SIGIO,     "SI_SIGIO",    "Signal sent by queued SIGIO" },
#endif

#if defined(AIX)
    { SI_UNDEFINED, "SI_UNDEFINED","siginfo contains partial information" },
    { SI_EMPTY,     "SI_EMPTY",    "siginfo contains no useful information" },
#endif

    { -1, NULL, NULL }
  };

  const char* s_code = NULL;
  const char* s_desc = NULL;

  for (int i = 0; t1[i].sig != -1; i ++) {
    if (t1[i].sig == si->si_signo && t1[i].code == si->si_code) {
      s_code = t1[i].s_code;
      s_desc = t1[i].s_desc;
      break;
    }
  }

  if (s_code == NULL) {
    for (int i = 0; t2[i].s_code != NULL; i ++) {
      if (t2[i].code == si->si_code) {
        s_code = t2[i].s_code;
        s_desc = t2[i].s_desc;
      }
    }
  }

  if (s_code == NULL) {
    out->s_name = "unknown";
    out->s_desc = "unknown";
    return false;
  }

  out->s_name = s_code;
  out->s_desc = s_desc;

  return true;
}

bool os::signal_sent_by_kill(const void* siginfo) {
  const siginfo_t* const si = (const siginfo_t*)siginfo;
  return si->si_code == SI_USER || si->si_code == SI_QUEUE
#ifdef SI_TKILL
         || si->si_code == SI_TKILL
#endif
  ;
}

// Returns true if signal number is valid.
static bool is_valid_signal(int sig) {
  // MacOS not really POSIX compliant: sigaddset does not return
  // an error for invalid signal numbers. However, MacOS does not
  // support real time signals and simply seems to have just 33
  // signals with no holes in the signal range.
#if defined(__APPLE__)
  return sig >= 1 && sig < NSIG;
#else
  // Use sigaddset to check for signal validity.
  sigset_t set;
  sigemptyset(&set);
  if (sigaddset(&set, sig) == -1 && errno == EINVAL) {
    return false;
  }
  return true;
#endif
}

static const char* get_signal_name(int sig, char* out, size_t outlen) {

  const char* ret = NULL;

#ifdef SIGRTMIN
  if (sig >= SIGRTMIN && sig <= SIGRTMAX) {
    if (sig == SIGRTMIN) {
      ret = "SIGRTMIN";
    } else if (sig == SIGRTMAX) {
      ret = "SIGRTMAX";
    } else {
      jio_snprintf(out, outlen, "SIGRTMIN+%d", sig - SIGRTMIN);
      return out;
    }
  }
#endif

  if (sig > 0) {
    for (int idx = 0; g_signal_info[idx].sig != -1; idx ++) {
      if (g_signal_info[idx].sig == sig) {
        ret = g_signal_info[idx].name;
        break;
      }
    }
  }

  if (!ret) {
    if (!is_valid_signal(sig)) {
      ret = "INVALID";
    } else {
      ret = "UNKNOWN";
    }
  }

  if (out && outlen > 0) {
    strncpy(out, ret, outlen);
    out[outlen - 1] = '\0';
  }
  return out;
}

void os::print_siginfo(outputStream* os, const void* si0) {

  const siginfo_t* const si = (const siginfo_t*) si0;

  char buf[20];
  os->print("siginfo:");

  if (!si) {
    os->print(" <null>");
    return;
  }

  const int sig = si->si_signo;

  os->print(" si_signo: %d (%s)", sig, get_signal_name(sig, buf, sizeof(buf)));

  enum_sigcode_desc_t ed;
  get_signal_code_description(si, &ed);
  os->print(", si_code: %d (%s)", si->si_code, ed.s_name);

  if (si->si_errno) {
    os->print(", si_errno: %d", si->si_errno);
  }

  // Output additional information depending on the signal code.

  // Note: Many implementations lump si_addr, si_pid, si_uid etc. together as unions,
  // so it depends on the context which member to use. For synchronous error signals,
  // we print si_addr, unless the signal was sent by another process or thread, in
  // which case we print out pid or tid of the sender.
  if (os::signal_sent_by_kill(si)) {
    const pid_t pid = si->si_pid;
    os->print(", si_pid: %ld", (long) pid);
    if (IS_VALID_PID(pid)) {
      const pid_t me = getpid();
      if (me == pid) {
        os->print(" (current process)");
      }
    } else {
      os->print(" (invalid)");
    }
    os->print(", si_uid: %ld", (long) si->si_uid);
    if (sig == SIGCHLD) {
      os->print(", si_status: %d", si->si_status);
    }
  } else if (sig == SIGSEGV || sig == SIGBUS || sig == SIGILL ||
             sig == SIGTRAP || sig == SIGFPE) {
    os->print(", si_addr: " PTR_FORMAT, p2i(si->si_addr));
#ifdef SIGPOLL
  } else if (sig == SIGPOLL) {
    os->print(", si_band: %ld", si->si_band);
#endif
  }
}

bool os::signal_thread(Thread* thread, int sig, const char* reason) {
  OSThread* osthread = thread->osthread();
  if (osthread) {
    int status = pthread_kill(osthread->pthread_id(), sig);
    if (status == 0) {
      Events::log(Thread::current(), "sent signal %d to Thread " INTPTR_FORMAT " because %s.",
                  sig, p2i(thread), reason);
      return true;
    }
  }
  return false;
}

// Returns:
// NULL for an invalid signal number
// "SIG<num>" for a valid but unknown signal number
// signal name otherwise.
const char* os::exception_name(int sig, char* buf, size_t size) {
  if (!is_valid_signal(sig)) {
    return NULL;
  }
  const char* const name = get_signal_name(sig, buf, size);
  if (strcmp(name, "UNKNOWN") == 0) {
    jio_snprintf(buf, size, "SIG%d", sig);
  }
  return buf;
}

int os::get_signal_number(const char* signal_name) {
  char tmp[30];
  const char* s = signal_name;
  if (s[0] != 'S' || s[1] != 'I' || s[2] != 'G') {
    jio_snprintf(tmp, sizeof(tmp), "SIG%s", signal_name);
    s = tmp;
  }
  for (int idx = 0; g_signal_info[idx].sig != -1; idx ++) {
    if (strcmp(g_signal_info[idx].name, s) == 0) {
      return g_signal_info[idx].sig;
    }
  }
  return -1;
}

void set_signal_handler(int sig) {
  // Check for overwrite.
  struct sigaction oldAct;
  sigaction(sig, (struct sigaction*)NULL, &oldAct);

  // Query the current signal handler. Needs to be a separate operation
  // from installing a new handler since we need to honor AllowUserSignalHandlers.
  void* oldhand = get_signal_handler(&oldAct);
  if (!HANDLER_IS_IGN_OR_DFL(oldhand) &&
      !HANDLER_IS(oldhand, javaSignalHandler)) {
    if (AllowUserSignalHandlers) {
      // Do not overwrite; user takes responsibility to forward to us.
      return;
    } else if (UseSignalChaining) {
      // save the old handler in jvm
      chained_handlers.set(sig, &oldAct);
      // libjsig also interposes the sigaction() call below and saves the
      // old sigaction on it own.
    } else {
      fatal("Encountered unexpected pre-existing sigaction handler "
            "%#lx for signal %d.", (long)oldhand, sig);
    }
  }

  struct sigaction sigAct;
  sigfillset(&(sigAct.sa_mask));
  remove_error_signals_from_set(&(sigAct.sa_mask));
  sigAct.sa_sigaction = javaSignalHandler;
  sigAct.sa_flags = SA_SIGINFO|SA_RESTART;
#if defined(__APPLE__)
  // Needed for main thread as XNU (Mac OS X kernel) will only deliver SIGSEGV
  // (which starts as SIGBUS) on main thread with faulting address inside "stack+guard pages"
  // if the signal handler declares it will handle it on alternate stack.
  // Notice we only declare we will handle it on alt stack, but we are not
  // actually going to use real alt stack - this is just a workaround.
  // Please see ux_exception.c, method catch_mach_exception_raise for details
  // link http://www.opensource.apple.com/source/xnu/xnu-2050.18.24/bsd/uxkern/ux_exception.c
  if (sig == SIGSEGV) {
    sigAct.sa_flags |= SA_ONSTACK;
  }
#endif

  // Save handler setup for later checking
  vm_handlers.set(sig, &sigAct);
  do_check_signal_periodically[sig] = true;

  int ret = sigaction(sig, &sigAct, &oldAct);
  assert(ret == 0, "check");

  void* oldhand2  = get_signal_handler(&oldAct);
  assert(oldhand2 == oldhand, "no concurrent signal handler installation");
}

// install signal handlers for signals that HotSpot needs to
// handle in order to support Java-level exception handling.
void install_signal_handlers() {
  // signal-chaining
  typedef void (*signal_setting_t)();
  signal_setting_t begin_signal_setting = NULL;
  signal_setting_t end_signal_setting = NULL;
  begin_signal_setting = CAST_TO_FN_PTR(signal_setting_t,
                                        dlsym(RTLD_DEFAULT, "JVM_begin_signal_setting"));
  if (begin_signal_setting != NULL) {
    end_signal_setting = CAST_TO_FN_PTR(signal_setting_t,
                                        dlsym(RTLD_DEFAULT, "JVM_end_signal_setting"));
    get_signal_action = CAST_TO_FN_PTR(get_signal_t,
                                       dlsym(RTLD_DEFAULT, "JVM_get_signal_action"));
    libjsig_is_loaded = true;
    assert(UseSignalChaining, "should enable signal-chaining");
  }
  if (libjsig_is_loaded) {
    // Tell libjsig jvm is setting signal handlers
    (*begin_signal_setting)();
  }

  set_signal_handler(SIGSEGV);
  set_signal_handler(SIGPIPE);
  set_signal_handler(SIGBUS);
  set_signal_handler(SIGILL);
  set_signal_handler(SIGFPE);
  PPC64_ONLY(set_signal_handler(SIGTRAP);)
  set_signal_handler(SIGXFSZ);

#if defined(__APPLE__)
  // lldb (gdb) installs both standard BSD signal handlers, and mach exception
  // handlers. By replacing the existing task exception handler, we disable lldb's mach
  // exception handling, while leaving the standard BSD signal handlers functional.
  //
  // EXC_MASK_BAD_ACCESS needed by all architectures for NULL ptr checking
  // EXC_MASK_ARITHMETIC needed by all architectures for div by 0 checking
  // EXC_MASK_BAD_INSTRUCTION needed by aarch64 to initiate deoptimization
  kern_return_t kr;
  kr = task_set_exception_ports(mach_task_self(),
                                EXC_MASK_BAD_ACCESS | EXC_MASK_ARITHMETIC
                                  AARCH64_ONLY(| EXC_MASK_BAD_INSTRUCTION),
                                MACH_PORT_NULL,
                                EXCEPTION_STATE_IDENTITY,
                                MACHINE_THREAD_STATE);

  assert(kr == KERN_SUCCESS, "could not set mach task signal handler");
#endif

  if (libjsig_is_loaded) {
    // Tell libjsig jvm finishes setting signal handlers
    (*end_signal_setting)();
  }

  // We don't activate signal checker if libjsig is in place, we trust ourselves
  // and if UserSignalHandler is installed all bets are off.
  // Log that signal checking is off only if -verbose:jni is specified.
  if (CheckJNICalls) {
    if (libjsig_is_loaded) {
      log_debug(jni, resolve)("Info: libjsig is activated, all active signal checking is disabled");
      check_signals = false;
    }
    if (AllowUserSignalHandlers) {
      log_debug(jni, resolve)("Info: AllowUserSignalHandlers is activated, all active signal checking is disabled");
      check_signals = false;
    }
  }
}

// Returns one-line short description of a signal set in a user provided buffer.
static const char* describe_signal_set_short(const sigset_t* set, char* buffer, size_t buf_size) {
  assert(buf_size == (NUM_IMPORTANT_SIGS + 1), "wrong buffer size");
  // Note: for shortness, just print out the first 32. That should
  // cover most of the useful ones, apart from realtime signals.
  for (int sig = 1; sig <= NUM_IMPORTANT_SIGS; sig++) {
    const int rc = sigismember(set, sig);
    if (rc == -1 && errno == EINVAL) {
      buffer[sig-1] = '?';
    } else {
      buffer[sig-1] = rc == 0 ? '0' : '1';
    }
  }
  buffer[NUM_IMPORTANT_SIGS] = 0;
  return buffer;
}

// Prints one-line description of a signal set.
static void print_signal_set_short(outputStream* st, const sigset_t* set) {
  char buf[NUM_IMPORTANT_SIGS + 1];
  describe_signal_set_short(set, buf, sizeof(buf));
  st->print("%s", buf);
}

static void print_single_signal_handler(outputStream* st,
                                        const struct sigaction* act,
                                        char* buf, size_t buflen) {

  address handler = get_signal_handler(act);
  if (HANDLER_IS_DFL(handler)) {
    st->print("SIG_DFL");
  } else if (HANDLER_IS_IGN(handler)) {
    st->print("SIG_IGN");
  } else {
    print_signal_handler_name(st, handler, buf, buflen);
  }

  st->print(", mask=");
  print_signal_set_short(st, &(act->sa_mask));

  st->print(", flags=");
  int flags = get_sanitized_sa_flags(act);
  print_sa_flags(st, flags);

}

// Print established signal handler for this signal.
// - if this signal handler was installed by us and is chained to a pre-established user handler
//    it replaced, print that one too.
// - otherwise, if this signal handler was installed by us and replaced another handler to which we
//    are not chained (e.g. if chaining is off), print that one too.
void PosixSignals::print_signal_handler(outputStream* st, int sig,
                                        char* buf, size_t buflen) {

  st->print("%10s: ", os::exception_name(sig, buf, buflen));

  struct sigaction current_act;
  sigaction(sig, NULL, &current_act);

  print_single_signal_handler(st, &current_act, buf, buflen);
  st->cr();

  // If we expected to see our own hotspot signal handler but found a different one,
  //  print a warning (unless the handler replacing it is our own crash handler, which can
  //  happen if this function is called during error reporting).
  const struct sigaction* expected_act = vm_handlers.get(sig);
  if (expected_act != NULL) {
    const address current_handler = get_signal_handler(&current_act);
    if (!(HANDLER_IS(current_handler, VMError::crash_handler_address))) {
      if (!are_handlers_equal(&current_act, expected_act)) {
        st->print_cr("  *** Handler was modified!");
        st->print   ("  *** Expected: ");
        print_single_signal_handler(st, expected_act, buf, buflen);
        st->cr();
      }
    }
  }

  // If there is a chained handler waiting behind the current one, print it too.
  const struct sigaction* chained_act = get_chained_signal_action(sig);
  if (chained_act != NULL) {
    st->print("  chained to: ");
    print_single_signal_handler(st, &current_act, buf, buflen);
    st->cr();
  }
}

void os::print_signal_handlers(outputStream* st, char* buf, size_t buflen) {
  st->print_cr("Signal Handlers:");
  PosixSignals::print_signal_handler(st, SIGSEGV, buf, buflen);
  PosixSignals::print_signal_handler(st, SIGBUS , buf, buflen);
  PosixSignals::print_signal_handler(st, SIGFPE , buf, buflen);
  PosixSignals::print_signal_handler(st, SIGPIPE, buf, buflen);
  PosixSignals::print_signal_handler(st, SIGXFSZ, buf, buflen);
  PosixSignals::print_signal_handler(st, SIGILL , buf, buflen);
  PosixSignals::print_signal_handler(st, PosixSignals::SR_signum, buf, buflen);
  PosixSignals::print_signal_handler(st, SHUTDOWN1_SIGNAL, buf, buflen);
  PosixSignals::print_signal_handler(st, SHUTDOWN2_SIGNAL , buf, buflen);
  PosixSignals::print_signal_handler(st, SHUTDOWN3_SIGNAL , buf, buflen);
  PosixSignals::print_signal_handler(st, BREAK_SIGNAL, buf, buflen);
#if defined(SIGDANGER)
  // We also want to know if someone else adds a SIGDANGER handler because
  // that will interfere with OOM killling.
  PosixSignals::print_signal_handler(st, SIGDANGER, buf, buflen);
#endif
#if defined(SIGTRAP)
  PosixSignals::print_signal_handler(st, SIGTRAP, buf, buflen);
#endif
}

bool PosixSignals::is_sig_ignored(int sig) {
  struct sigaction oact;
  sigaction(sig, (struct sigaction*)NULL, &oact);
  if (HANDLER_IS_IGN(get_signal_handler(&oact))) {
    return true;
  } else {
    return false;
  }
}

static void signal_sets_init() {
  sigemptyset(&preinstalled_sigs);

  // Should also have an assertion stating we are still single-threaded.
  assert(!signal_sets_initialized, "Already initialized");
  // Fill in signals that are necessarily unblocked for all threads in
  // the VM. Currently, we unblock the following signals:
  // SHUTDOWN{1,2,3}_SIGNAL: for shutdown hooks support (unless over-ridden
  //                         by -Xrs (=ReduceSignalUsage));
  // BREAK_SIGNAL which is unblocked only by the VM thread and blocked by all
  // other threads. The "ReduceSignalUsage" boolean tells us not to alter
  // the dispositions or masks wrt these signals.
  // Programs embedding the VM that want to use the above signals for their
  // own purposes must, at this time, use the "-Xrs" option to prevent
  // interference with shutdown hooks and BREAK_SIGNAL thread dumping.
  // (See bug 4345157, and other related bugs).
  // In reality, though, unblocking these signals is really a nop, since
  // these signals are not blocked by default.
  sigemptyset(&unblocked_sigs);
  sigaddset(&unblocked_sigs, SIGILL);
  sigaddset(&unblocked_sigs, SIGSEGV);
  sigaddset(&unblocked_sigs, SIGBUS);
  sigaddset(&unblocked_sigs, SIGFPE);
  PPC64_ONLY(sigaddset(&unblocked_sigs, SIGTRAP);)
  sigaddset(&unblocked_sigs, PosixSignals::SR_signum);

  if (!ReduceSignalUsage) {
    if (!PosixSignals::is_sig_ignored(SHUTDOWN1_SIGNAL)) {
      sigaddset(&unblocked_sigs, SHUTDOWN1_SIGNAL);
    }
    if (!PosixSignals::is_sig_ignored(SHUTDOWN2_SIGNAL)) {
      sigaddset(&unblocked_sigs, SHUTDOWN2_SIGNAL);
    }
    if (!PosixSignals::is_sig_ignored(SHUTDOWN3_SIGNAL)) {
      sigaddset(&unblocked_sigs, SHUTDOWN3_SIGNAL);
    }
  }
  // Fill in signals that are blocked by all but the VM thread.
  sigemptyset(&vm_sigs);
  if (!ReduceSignalUsage) {
    sigaddset(&vm_sigs, BREAK_SIGNAL);
  }
  debug_only(signal_sets_initialized = true);
}

// These are signals that are unblocked while a thread is running Java.
// (For some reason, they get blocked by default.)
static sigset_t* unblocked_signals() {
  assert(signal_sets_initialized, "Not initialized");
  return &unblocked_sigs;
}

// These are the signals that are blocked while a (non-VM) thread is
// running Java. Only the VM thread handles these signals.
static sigset_t* vm_signals() {
  assert(signal_sets_initialized, "Not initialized");
  return &vm_sigs;
}

void PosixSignals::hotspot_sigmask(Thread* thread) {

  //Save caller's signal mask before setting VM signal mask
  sigset_t caller_sigmask;
  pthread_sigmask(SIG_BLOCK, NULL, &caller_sigmask);

  OSThread* osthread = thread->osthread();
  osthread->set_caller_sigmask(caller_sigmask);

  pthread_sigmask(SIG_UNBLOCK, unblocked_signals(), NULL);

  if (!ReduceSignalUsage) {
    if (thread->is_VM_thread()) {
      // Only the VM thread handles BREAK_SIGNAL ...
      pthread_sigmask(SIG_UNBLOCK, vm_signals(), NULL);
    } else {
      // ... all other threads block BREAK_SIGNAL
      pthread_sigmask(SIG_BLOCK, vm_signals(), NULL);
    }
  }
}

////////////////////////////////////////////////////////////////////////////////
// suspend/resume support

//  The low-level signal-based suspend/resume support is a remnant from the
//  old VM-suspension that used to be for java-suspension, safepoints etc,
//  within hotspot. Currently used by JFR's OSThreadSampler
//
//  The remaining code is greatly simplified from the more general suspension
//  code that used to be used.
//
//  The protocol is quite simple:
//  - suspend:
//      - sends a signal to the target thread
//      - polls the suspend state of the osthread using a yield loop
//      - target thread signal handler (SR_handler) sets suspend state
//        and blocks in sigsuspend until continued
//  - resume:
//      - sets target osthread state to continue
//      - sends signal to end the sigsuspend loop in the SR_handler
//
//  Note that resume_clear_context() and suspend_save_context() are needed
//  by SR_handler(), so that fetch_frame_from_context() works,
//  which in part is used by:
//    - Forte Analyzer: AsyncGetCallTrace()
//    - StackBanging: get_frame_at_stack_banging_point()

sigset_t SR_sigset;

static void resume_clear_context(OSThread *osthread) {
  osthread->set_ucontext(NULL);
  osthread->set_siginfo(NULL);
}

static void suspend_save_context(OSThread *osthread, siginfo_t* siginfo, ucontext_t* context) {
  osthread->set_ucontext(context);
  osthread->set_siginfo(siginfo);
}

// Handler function invoked when a thread's execution is suspended or
// resumed. We have to be careful that only async-safe functions are
// called here (Note: most pthread functions are not async safe and
// should be avoided.)
//
// Note: sigwait() is a more natural fit than sigsuspend() from an
// interface point of view, but sigwait() prevents the signal handler
// from being run. libpthread would get very confused by not having
// its signal handlers run and prevents sigwait()'s use with the
// mutex granting signal.
//
// Currently only ever called on the VMThread and JavaThreads (PC sampling)
//
static void SR_handler(int sig, siginfo_t* siginfo, ucontext_t* context) {

  // Save and restore errno to avoid confusing native code with EINTR
  // after sigsuspend.
  int old_errno = errno;

  PosixSignals::unblock_error_signals();

  Thread* thread = Thread::current_or_null_safe();
  assert(thread != NULL, "Missing current thread in SR_handler");

  // On some systems we have seen signal delivery get "stuck" until the signal
  // mask is changed as part of thread termination. Check that the current thread
  // has not already terminated - else the following assertion
  // will fail because the thread is no longer a JavaThread as the ~JavaThread
  // destructor has completed.

  if (thread->has_terminated()) {
    return;
  }

  assert(thread->is_VM_thread() || thread->is_Java_thread(), "Must be VMThread or JavaThread");

  OSThread* osthread = thread->osthread();

  os::SuspendResume::State current = osthread->sr.state();

  if (current == os::SuspendResume::SR_SUSPEND_REQUEST) {
    suspend_save_context(osthread, siginfo, context);

    // attempt to switch the state, we assume we had a SUSPEND_REQUEST
    os::SuspendResume::State state = osthread->sr.suspended();
    if (state == os::SuspendResume::SR_SUSPENDED) {
      sigset_t suspend_set;  // signals for sigsuspend()
      sigemptyset(&suspend_set);

      // get current set of blocked signals and unblock resume signal
      pthread_sigmask(SIG_BLOCK, NULL, &suspend_set);
      sigdelset(&suspend_set, PosixSignals::SR_signum);

      sr_semaphore.signal();

      // wait here until we are resumed
      while (1) {
        sigsuspend(&suspend_set);

        os::SuspendResume::State result = osthread->sr.running();
        if (result == os::SuspendResume::SR_RUNNING) {
          // double check AIX doesn't need this!
          sr_semaphore.signal();
          break;
        } else if (result != os::SuspendResume::SR_SUSPENDED) {
          ShouldNotReachHere();
        }
      }

    } else if (state == os::SuspendResume::SR_RUNNING) {
      // request was cancelled, continue
    } else {
      ShouldNotReachHere();
    }

    resume_clear_context(osthread);
  } else if (current == os::SuspendResume::SR_RUNNING) {
    // request was cancelled, continue
  } else if (current == os::SuspendResume::SR_WAKEUP_REQUEST) {
    // ignore
  } else {
    // ignore
  }

  errno = old_errno;
}

int SR_initialize() {
  struct sigaction act;
  char *s;
  // Get signal number to use for suspend/resume
  if ((s = ::getenv("_JAVA_SR_SIGNUM")) != 0) {
    int sig = ::strtol(s, 0, 10);
    if (sig > MAX2(SIGSEGV, SIGBUS) &&  // See 4355769.
        sig < NSIG) {                   // Must be legal signal and fit into sigflags[].
      PosixSignals::SR_signum = sig;
    } else {
      warning("You set _JAVA_SR_SIGNUM=%d. It must be in range [%d, %d]. Using %d instead.",
              sig, MAX2(SIGSEGV, SIGBUS)+1, NSIG-1, PosixSignals::SR_signum);
    }
  }

  assert(PosixSignals::SR_signum > SIGSEGV && PosixSignals::SR_signum > SIGBUS,
         "SR_signum must be greater than max(SIGSEGV, SIGBUS), see 4355769");

  sigemptyset(&SR_sigset);
  sigaddset(&SR_sigset, PosixSignals::SR_signum);

  // Set up signal handler for suspend/resume
  act.sa_flags = SA_RESTART|SA_SIGINFO;
  act.sa_handler = (void (*)(int)) SR_handler;

  // SR_signum is blocked by default.
  pthread_sigmask(SIG_BLOCK, NULL, &act.sa_mask);
  remove_error_signals_from_set(&(act.sa_mask));

  if (sigaction(PosixSignals::SR_signum, &act, 0) == -1) {
    return -1;
  }

  // Save signal setup information for later checking.
  vm_handlers.set(PosixSignals::SR_signum, &act);
  do_check_signal_periodically[PosixSignals::SR_signum] = true;

  return 0;
}

static int sr_notify(OSThread* osthread) {
  int status = pthread_kill(osthread->pthread_id(), PosixSignals::SR_signum);
  assert_status(status == 0, status, "pthread_kill");
  return status;
}

// returns true on success and false on error - really an error is fatal
// but this seems the normal response to library errors
bool PosixSignals::do_suspend(OSThread* osthread) {
  assert(osthread->sr.is_running(), "thread should be running");
  assert(!sr_semaphore.trywait(), "semaphore has invalid state");

  // mark as suspended and send signal
  if (osthread->sr.request_suspend() != os::SuspendResume::SR_SUSPEND_REQUEST) {
    // failed to switch, state wasn't running?
    ShouldNotReachHere();
    return false;
  }

  if (sr_notify(osthread) != 0) {
    ShouldNotReachHere();
  }

  // managed to send the signal and switch to SUSPEND_REQUEST, now wait for SUSPENDED
  while (true) {
    if (sr_semaphore.timedwait(2)) {
      break;
    } else {
      // timeout
      os::SuspendResume::State cancelled = osthread->sr.cancel_suspend();
      if (cancelled == os::SuspendResume::SR_RUNNING) {
        return false;
      } else if (cancelled == os::SuspendResume::SR_SUSPENDED) {
        // make sure that we consume the signal on the semaphore as well
        sr_semaphore.wait();
        break;
      } else {
        ShouldNotReachHere();
        return false;
      }
    }
  }

  guarantee(osthread->sr.is_suspended(), "Must be suspended");
  return true;
}

void PosixSignals::do_resume(OSThread* osthread) {
  assert(osthread->sr.is_suspended(), "thread should be suspended");
  assert(!sr_semaphore.trywait(), "invalid semaphore state");

  if (osthread->sr.request_wakeup() != os::SuspendResume::SR_WAKEUP_REQUEST) {
    // failed to switch to WAKEUP_REQUEST
    ShouldNotReachHere();
    return;
  }

  while (true) {
    if (sr_notify(osthread) == 0) {
      if (sr_semaphore.timedwait(2)) {
        if (osthread->sr.is_running()) {
          return;
        }
      }
    } else {
      ShouldNotReachHere();
    }
  }

  guarantee(osthread->sr.is_running(), "Must be running!");
}

void os::SuspendedThreadTask::internal_do_task() {
  if (PosixSignals::do_suspend(_thread->osthread())) {
    os::SuspendedThreadTaskContext context(_thread, _thread->osthread()->ucontext());
    do_task(context);
    PosixSignals::do_resume(_thread->osthread());
  }
}

int PosixSignals::init() {
  // initialize suspend/resume support - must do this before signal_sets_init()
  if (SR_initialize() != 0) {
    vm_exit_during_initialization("SR_initialize failed");
    return JNI_ERR;
  }

  signal_sets_init();

  install_signal_handlers();

  // Initialize data for jdk.internal.misc.Signal
  if (!ReduceSignalUsage) {
    jdk_misc_signal_init();
  }

  return JNI_OK;
}
