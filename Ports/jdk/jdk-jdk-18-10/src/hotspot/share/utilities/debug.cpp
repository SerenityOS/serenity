/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/systemDictionary.hpp"
#include "code/codeCache.hpp"
#include "code/icBuffer.hpp"
#include "code/nmethod.hpp"
#include "code/vtableStubs.hpp"
#include "compiler/compileBroker.hpp"
#include "compiler/disassembler.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "interpreter/interpreter.hpp"
#include "memory/allocation.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/klass.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/flags/flagSetting.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/java.hpp"
#include "runtime/os.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubCodeGenerator.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/vframe.hpp"
#include "runtime/vm_version.hpp"
#include "services/heapDumper.hpp"
#include "services/memTracker.hpp"
#include "utilities/defaultStream.hpp"
#include "utilities/events.hpp"
#include "utilities/formatBuffer.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"
#include "utilities/vmError.hpp"

#include <stdio.h>
#include <stdarg.h>

// Support for showing register content on asserts/guarantees.
#ifdef CAN_SHOW_REGISTERS_ON_ASSERT
static char g_dummy;
char* g_assert_poison = &g_dummy;
static intx g_asserting_thread = 0;
static void* g_assertion_context = NULL;
#endif // CAN_SHOW_REGISTERS_ON_ASSERT

// Set to suppress secondary error reporting.
bool Debugging = false;

#ifndef ASSERT
#  ifdef _DEBUG
   // NOTE: don't turn the lines below into a comment -- if you're getting
   // a compile error here, change the settings to define ASSERT
   ASSERT should be defined when _DEBUG is defined.  It is not intended to be used for debugging
   functions that do not slow down the system too much and thus can be left in optimized code.
   On the other hand, the code should not be included in a production version.
#  endif // _DEBUG
#endif // ASSERT


#ifdef _DEBUG
#  ifndef ASSERT
     configuration error: ASSERT must be defined in debug version
#  endif // ASSERT
#endif // _DEBUG


#ifdef PRODUCT
#  if -defined _DEBUG || -defined ASSERT
     configuration error: ASSERT et al. must not be defined in PRODUCT version
#  endif
#endif // PRODUCT

#ifdef ASSERT
// This is to test that error reporting works if we assert during dynamic
// initialization of the hotspot. See JDK-8214975.
struct Crasher {
  Crasher() {
    // Using getenv - no other mechanism would work yet.
    const char* s = ::getenv("HOTSPOT_FATAL_ERROR_DURING_DYNAMIC_INITIALIZATION");
    if (s != NULL && ::strcmp(s, "1") == 0) {
      fatal("HOTSPOT_FATAL_ERROR_DURING_DYNAMIC_INITIALIZATION");
    }
  }
};
static Crasher g_crasher;
#endif // ASSERT

ATTRIBUTE_PRINTF(1, 2)
void warning(const char* format, ...) {
  if (PrintWarnings) {
    FILE* const err = defaultStream::error_stream();
    jio_fprintf(err, "%s warning: ", VM_Version::vm_name());
    va_list ap;
    va_start(ap, format);
    vfprintf(err, format, ap);
    va_end(ap);
    fputc('\n', err);
  }
}

#ifndef PRODUCT

#define is_token_break(ch) (isspace(ch) || (ch) == ',')

static const char* last_file_name = NULL;
static int         last_line_no   = -1;

// assert/guarantee/... may happen very early during VM initialization.
// Don't rely on anything that is initialized by Threads::create_vm(). For
// example, don't use tty.
bool error_is_suppressed(const char* file_name, int line_no) {
  // The following 1-element cache requires that passed-in
  // file names are always only constant literals.
  if (file_name == last_file_name && line_no == last_line_no)  return true;

  int file_name_len = (int)strlen(file_name);
  char separator = os::file_separator()[0];
  const char* base_name = strrchr(file_name, separator);
  if (base_name == NULL)
    base_name = file_name;

  // scan the SuppressErrorAt option
  const char* cp = SuppressErrorAt;
  for (;;) {
    const char* sfile;
    int sfile_len;
    int sline;
    bool noisy;
    while ((*cp) != '\0' && is_token_break(*cp))  cp++;
    if ((*cp) == '\0')  break;
    sfile = cp;
    while ((*cp) != '\0' && !is_token_break(*cp) && (*cp) != ':')  cp++;
    sfile_len = cp - sfile;
    if ((*cp) == ':')  cp++;
    sline = 0;
    while ((*cp) != '\0' && isdigit(*cp)) {
      sline *= 10;
      sline += (*cp) - '0';
      cp++;
    }
    // "file:line!" means the assert suppression is not silent
    noisy = ((*cp) == '!');
    while ((*cp) != '\0' && !is_token_break(*cp))  cp++;
    // match the line
    if (sline != 0) {
      if (sline != line_no)  continue;
    }
    // match the file
    if (sfile_len > 0) {
      const char* look = file_name;
      const char* look_max = file_name + file_name_len - sfile_len;
      const char* foundp;
      bool match = false;
      while (!match
             && (foundp = strchr(look, sfile[0])) != NULL
             && foundp <= look_max) {
        match = true;
        for (int i = 1; i < sfile_len; i++) {
          if (sfile[i] != foundp[i]) {
            match = false;
            break;
          }
        }
        look = foundp + 1;
      }
      if (!match)  continue;
    }
    // got a match!
    if (noisy) {
      fdStream out(defaultStream::output_fd());
      out.print_raw("[error suppressed at ");
      out.print_raw(base_name);
      char buf[16];
      jio_snprintf(buf, sizeof(buf), ":%d]", line_no);
      out.print_raw_cr(buf);
    } else {
      // update 1-element cache for fast silent matches
      last_file_name = file_name;
      last_line_no   = line_no;
    }
    return true;
  }

  if (!VMError::is_error_reported() && !SuppressFatalErrorMessage) {
    // print a friendly hint:
    fdStream out(defaultStream::output_fd());
    out.print_raw_cr("# To suppress the following error report, specify this argument");
    out.print_raw   ("# after -XX: or in .hotspotrc:  SuppressErrorAt=");
    out.print_raw   (base_name);
    char buf[16];
    jio_snprintf(buf, sizeof(buf), ":%d", line_no);
    out.print_raw_cr(buf);
  }
  return false;
}

#undef is_token_break

#else

// Place-holder for non-existent suppression check:
#define error_is_suppressed(file_name, line_no) (false)

#endif // !PRODUCT

void report_vm_error(const char* file, int line, const char* error_msg)
{
  report_vm_error(file, line, error_msg, "%s", "");
}


static void print_error_for_unit_test(const char* message, const char* detail_fmt, va_list detail_args) {
#ifdef ASSERT
  if (ExecutingUnitTests) {
    char detail_msg[256];
    if (detail_fmt != NULL) {
      // Special handling for the sake of gtest death tests which expect the assert
      // message to be printed in one short line to stderr (see TEST_VM_ASSERT_MSG) and
      // cannot be tweaked to accept our normal assert message.
      va_list detail_args_copy;
      va_copy(detail_args_copy, detail_args);
      jio_vsnprintf(detail_msg, sizeof(detail_msg), detail_fmt, detail_args_copy);

      // the VM assert tests look for "assert failed: "
      if (message == NULL) {
        fprintf(stderr, "assert failed: %s", detail_msg);
      } else {
        if (strlen(detail_msg) > 0) {
          fprintf(stderr, "assert failed: %s: %s", message, detail_msg);
        } else {
          fprintf(stderr, "assert failed: Error: %s", message);
        }
      }
      ::fflush(stderr);
      va_end(detail_args_copy);
    }
  }
#endif // ASSERT
}

void report_vm_error(const char* file, int line, const char* error_msg, const char* detail_fmt, ...)
{
  if (Debugging || error_is_suppressed(file, line)) return;
  va_list detail_args;
  va_start(detail_args, detail_fmt);
  void* context = NULL;
#ifdef CAN_SHOW_REGISTERS_ON_ASSERT
  if (g_assertion_context != NULL && os::current_thread_id() == g_asserting_thread) {
    context = g_assertion_context;
  }
#endif // CAN_SHOW_REGISTERS_ON_ASSERT

  print_error_for_unit_test(error_msg, detail_fmt, detail_args);

  VMError::report_and_die(Thread::current_or_null(), context, file, line, error_msg, detail_fmt, detail_args);
  va_end(detail_args);
}

void report_vm_status_error(const char* file, int line, const char* error_msg,
                            int status, const char* detail) {
  report_vm_error(file, line, error_msg, "error %s(%d), %s", os::errno_name(status), status, detail);
}

void report_fatal(VMErrorType error_type, const char* file, int line, const char* detail_fmt, ...) {
  if (Debugging || error_is_suppressed(file, line)) return;
  va_list detail_args;
  va_start(detail_args, detail_fmt);
  void* context = NULL;
#ifdef CAN_SHOW_REGISTERS_ON_ASSERT
  if (g_assertion_context != NULL && os::current_thread_id() == g_asserting_thread) {
    context = g_assertion_context;
  }
#endif // CAN_SHOW_REGISTERS_ON_ASSERT

  print_error_for_unit_test("fatal error", detail_fmt, detail_args);

  VMError::report_and_die(error_type, "fatal error", detail_fmt, detail_args,
                          Thread::current_or_null(), NULL, NULL, context,
                          file, line, 0);
  va_end(detail_args);
}

void report_vm_out_of_memory(const char* file, int line, size_t size,
                             VMErrorType vm_err_type, const char* detail_fmt, ...) {
  if (Debugging) return;
  va_list detail_args;
  va_start(detail_args, detail_fmt);

  print_error_for_unit_test(NULL, detail_fmt, detail_args);

  VMError::report_and_die(Thread::current_or_null(), file, line, size, vm_err_type, detail_fmt, detail_args);
  va_end(detail_args);

  // The UseOSErrorReporting option in report_and_die() may allow a return
  // to here. If so then we'll have to figure out how to handle it.
  guarantee(false, "report_and_die() should not return here");
}

void report_should_not_call(const char* file, int line) {
  report_vm_error(file, line, "ShouldNotCall()");
}

void report_should_not_reach_here(const char* file, int line) {
  report_vm_error(file, line, "ShouldNotReachHere()");
}

void report_unimplemented(const char* file, int line) {
  report_vm_error(file, line, "Unimplemented()");
}

void report_untested(const char* file, int line, const char* message) {
#ifndef PRODUCT
  warning("Untested: %s in %s: %d\n", message, file, line);
#endif // !PRODUCT
}

void report_java_out_of_memory(const char* message) {
  static int out_of_memory_reported = 0;

  // A number of threads may attempt to report OutOfMemoryError at around the
  // same time. To avoid dumping the heap or executing the data collection
  // commands multiple times we just do it once when the first threads reports
  // the error.
  if (Atomic::cmpxchg(&out_of_memory_reported, 0, 1) == 0) {
    // create heap dump before OnOutOfMemoryError commands are executed
    if (HeapDumpOnOutOfMemoryError) {
      tty->print_cr("java.lang.OutOfMemoryError: %s", message);
      HeapDumper::dump_heap_from_oome();
    }

    if (OnOutOfMemoryError && OnOutOfMemoryError[0]) {
      VMError::report_java_out_of_memory(message);
    }

    if (CrashOnOutOfMemoryError) {
      tty->print_cr("Aborting due to java.lang.OutOfMemoryError: %s", message);
      report_fatal(OOM_JAVA_HEAP_FATAL, __FILE__, __LINE__, "OutOfMemory encountered: %s", message);
    }

    if (ExitOnOutOfMemoryError) {
      tty->print_cr("Terminating due to java.lang.OutOfMemoryError: %s", message);
      os::exit(3);
    }
  }
}

// ------ helper functions for debugging go here ------------

// All debug entries should be wrapped with a stack allocated
// Command object. It makes sure a resource mark is set and
// flushes the logfile to prevent file sharing problems.

class Command : public StackObj {
 private:
  ResourceMark rm;
  bool debug_save;
 public:
  static int level;
  Command(const char* str) {
    debug_save = Debugging;
    Debugging = true;
    if (level++ > 0)  return;
    tty->cr();
    tty->print_cr("\"Executing %s\"", str);
  }

  ~Command() {
    tty->flush();
    Debugging = debug_save;
    level--;
  }
};

int Command::level = 0;

extern "C" JNIEXPORT void blob(CodeBlob* cb) {
  Command c("blob");
  cb->print();
}


extern "C" JNIEXPORT void dump_vtable(address p) {
  Command c("dump_vtable");
  Klass* k = (Klass*)p;
  k->vtable().print();
}


extern "C" JNIEXPORT void nm(intptr_t p) {
  // Actually we look through all CodeBlobs (the nm name has been kept for backwards compatability)
  Command c("nm");
  CodeBlob* cb = CodeCache::find_blob((address)p);
  if (cb == NULL) {
    tty->print_cr("NULL");
  } else {
    cb->print();
  }
}


extern "C" JNIEXPORT void disnm(intptr_t p) {
  Command c("disnm");
  CodeBlob* cb = CodeCache::find_blob((address) p);
  if (cb != NULL) {
    nmethod* nm = cb->as_nmethod_or_null();
    if (nm != NULL) {
      nm->print();
    } else {
      cb->print();
    }
    Disassembler::decode(cb);
  }
}


extern "C" JNIEXPORT void printnm(intptr_t p) {
  char buffer[256];
  sprintf(buffer, "printnm: " INTPTR_FORMAT, p);
  Command c(buffer);
  CodeBlob* cb = CodeCache::find_blob((address) p);
  if (cb->is_nmethod()) {
    nmethod* nm = (nmethod*)cb;
    nm->print_nmethod(true);
  }
}


extern "C" JNIEXPORT void universe() {
  Command c("universe");
  Universe::print_on(tty);
}


extern "C" JNIEXPORT void verify() {
  // try to run a verify on the entire system
  // note: this may not be safe if we're not at a safepoint; for debugging,
  // this manipulates the safepoint settings to avoid assertion failures
  Command c("universe verify");
  bool safe = SafepointSynchronize::is_at_safepoint();
  if (!safe) {
    tty->print_cr("warning: not at safepoint -- verify may fail");
    SafepointSynchronize::set_is_at_safepoint();
  }
  // Ensure Eden top is correct before verification
  Universe::heap()->prepare_for_verify();
  Universe::verify();
  if (!safe) SafepointSynchronize::set_is_not_at_safepoint();
}


extern "C" JNIEXPORT void pp(void* p) {
  Command c("pp");
  FlagSetting fl(DisplayVMOutput, true);
  if (Universe::heap()->is_in(p)) {
    oop obj = cast_to_oop(p);
    obj->print();
  } else {
    tty->print(PTR_FORMAT, p2i(p));
  }
}


extern "C" JNIEXPORT void findpc(intptr_t x);

extern "C" JNIEXPORT void ps() { // print stack
  if (Thread::current_or_null() == NULL) return;
  Command c("ps");

  // Prints the stack of the current Java thread
  JavaThread* p = JavaThread::active();
  tty->print(" for thread: ");
  p->print();
  tty->cr();

  if (p->has_last_Java_frame()) {
    // If the last_Java_fp is set we are in C land and
    // can call the standard stack_trace function.
    p->print_stack();
#ifndef PRODUCT
    if (Verbose) p->trace_stack();
  } else {
    frame f = os::current_frame();
    RegisterMap reg_map(p);
    f = f.sender(&reg_map);
    tty->print("(guessing starting frame id=" PTR_FORMAT " based on current fp)\n", p2i(f.id()));
    p->trace_stack_from(vframe::new_vframe(&f, &reg_map, p));
    f.pd_ps();
#endif
  }
}

extern "C" JNIEXPORT void pfl() {
  // print frame layout
  Command c("pfl");
  JavaThread* p = JavaThread::active();
  tty->print(" for thread: ");
  p->print();
  tty->cr();
  if (p->has_last_Java_frame()) {
    p->print_frame_layout();
  }
}

extern "C" JNIEXPORT void psf() { // print stack frames
  {
    Command c("psf");
    JavaThread* p = JavaThread::active();
    tty->print(" for thread: ");
    p->print();
    tty->cr();
    if (p->has_last_Java_frame()) {
      p->trace_frames();
    }
  }
}


extern "C" JNIEXPORT void threads() {
  Command c("threads");
  Threads::print(false, true);
}


extern "C" JNIEXPORT void psd() {
  Command c("psd");
  SystemDictionary::print();
}


extern "C" JNIEXPORT void pss() { // print all stacks
  if (Thread::current_or_null() == NULL) return;
  Command c("pss");
  Threads::print(true, PRODUCT_ONLY(false) NOT_PRODUCT(true));
}

// #ifndef PRODUCT

extern "C" JNIEXPORT void debug() {               // to set things up for compiler debugging
  Command c("debug");
  NOT_PRODUCT(WizardMode = true;)
  PrintCompilation = true;
  PrintInlining = PrintAssembly = true;
  tty->flush();
}


extern "C" JNIEXPORT void ndebug() {              // undo debug()
  Command c("ndebug");
  PrintCompilation = false;
  PrintInlining = PrintAssembly = false;
  tty->flush();
}


extern "C" JNIEXPORT void flush()  {
  Command c("flush");
  tty->flush();
}

extern "C" JNIEXPORT void events() {
  Command c("events");
  Events::print();
}

extern "C" JNIEXPORT Method* findm(intptr_t pc) {
  Command c("findm");
  nmethod* nm = CodeCache::find_nmethod((address)pc);
  return (nm == NULL) ? (Method*)NULL : nm->method();
}


extern "C" JNIEXPORT nmethod* findnm(intptr_t addr) {
  Command c("findnm");
  return  CodeCache::find_nmethod((address)addr);
}

extern "C" JNIEXPORT void find(intptr_t x) {
  Command c("find");
  os::print_location(tty, x, false);
}


extern "C" JNIEXPORT void findpc(intptr_t x) {
  Command c("findpc");
  os::print_location(tty, x, true);
}


// Need method pointer to find bcp, when not in permgen.
extern "C" JNIEXPORT void findbcp(intptr_t method, intptr_t bcp) {
  Command c("findbcp");
  Method* mh = (Method*)method;
  if (!mh->is_native()) {
    tty->print_cr("bci_from(%p) = %d; print_codes():",
                        mh, mh->bci_from(address(bcp)));
    mh->print_codes_on(tty);
  }
}

// int versions of all methods to avoid having to type type casts in the debugger

void pp(intptr_t p)          { pp((void*)p); }
void pp(oop p)               { pp((void*)p); }

void help() {
  Command c("help");
  tty->print_cr("basic");
  tty->print_cr("  pp(void* p)   - try to make sense of p");
  tty->print_cr("  ps()          - print current thread stack");
  tty->print_cr("  pss()         - print all thread stacks");
  tty->print_cr("  pm(int pc)    - print Method* given compiled PC");
  tty->print_cr("  findm(intptr_t pc) - finds Method*");
  tty->print_cr("  find(intptr_t x)   - finds & prints nmethod/stub/bytecode/oop based on pointer into it");
  tty->print_cr("  pns(void* sp, void* fp, void* pc)  - print native (i.e. mixed) stack trace. E.g.");
  tty->print_cr("                   pns($sp, $rbp, $pc) on Linux/amd64 or");
  tty->print_cr("                   pns($sp, $ebp, $pc) on Linux/x86 or");
  tty->print_cr("                   pns($sp, $fp, $pc)  on Linux/AArch64 or");
  tty->print_cr("                   pns($sp, 0, $pc)    on Linux/ppc64 or");
  tty->print_cr("                   pns($sp, $s8, $pc)  on Linux/mips or");
  tty->print_cr("                 - in gdb do 'set overload-resolution off' before calling pns()");
  tty->print_cr("                 - in dbx do 'frame 1' before calling pns()");

  tty->print_cr("misc.");
  tty->print_cr("  flush()       - flushes the log file");
  tty->print_cr("  events()      - dump events from ring buffers");


  tty->print_cr("compiler debugging");
  tty->print_cr("  debug()       - to set things up for compiler debugging");
  tty->print_cr("  ndebug()      - undo debug");
}

#ifndef PRODUCT
extern "C" JNIEXPORT void pns(void* sp, void* fp, void* pc) { // print native stack
  Command c("pns");
  static char buf[O_BUFLEN];
  Thread* t = Thread::current_or_null();
  // Call generic frame constructor (certain arguments may be ignored)
  frame fr(sp, fp, pc);
  VMError::print_native_stack(tty, fr, t, buf, sizeof(buf));
}

//
// This version of pns() will not work when called from the debugger, but is
// useful when called from within hotspot code. The advantages over pns()
// are not having to pass in any arguments, and it will work on Windows/x64.
//
// WARNING: Only intended for use when debugging. Do not leave calls to
// pns2() in committed source (product or debug).
//
extern "C" JNIEXPORT void pns2() { // print native stack
  Command c("pns2");
  static char buf[O_BUFLEN];
  if (os::platform_print_native_stack(tty, NULL, buf, sizeof(buf))) {
    // We have printed the native stack in platform-specific code,
    // so nothing else to do in this case.
  } else {
    Thread* t = Thread::current_or_null();
    frame fr = os::current_frame();
    VMError::print_native_stack(tty, fr, t, buf, sizeof(buf));
  }
}
#endif


//////////////////////////////////////////////////////////////////////////////
// Test multiple STATIC_ASSERT forms in various scopes.

#ifndef PRODUCT

// namespace scope
STATIC_ASSERT(true);
STATIC_ASSERT(true);
STATIC_ASSERT(1 == 1);
STATIC_ASSERT(0 == 0);

void test_multiple_static_assert_forms_in_function_scope() {
  STATIC_ASSERT(true);
  STATIC_ASSERT(true);
  STATIC_ASSERT(0 == 0);
  STATIC_ASSERT(1 == 1);
}

// class scope
struct TestMultipleStaticAssertFormsInClassScope {
  STATIC_ASSERT(true);
  STATIC_ASSERT(true);
  STATIC_ASSERT(0 == 0);
  STATIC_ASSERT(1 == 1);
};

#endif // !PRODUCT

// Support for showing register content on asserts/guarantees.
#ifdef CAN_SHOW_REGISTERS_ON_ASSERT

static ucontext_t g_stored_assertion_context;

void initialize_assert_poison() {
  char* page = os::reserve_memory(os::vm_page_size());
  if (page) {
    MemTracker::record_virtual_memory_type(page, mtInternal);
    if (os::commit_memory(page, os::vm_page_size(), false) &&
        os::protect_memory(page, os::vm_page_size(), os::MEM_PROT_NONE)) {
      g_assert_poison = page;
    }
  }
}

void disarm_assert_poison() {
  g_assert_poison = &g_dummy;
}

static void store_context(const void* context) {
  memcpy(&g_stored_assertion_context, context, sizeof(ucontext_t));
#if defined(LINUX) && defined(PPC64)
  // on Linux ppc64, ucontext_t contains pointers into itself which have to be patched up
  //  after copying the context (see comment in sys/ucontext.h):
  *((void**) &g_stored_assertion_context.uc_mcontext.regs) = &(g_stored_assertion_context.uc_mcontext.gp_regs);
#endif
}

bool handle_assert_poison_fault(const void* ucVoid, const void* faulting_address) {
  if (faulting_address == g_assert_poison) {
    // Disarm poison page.
    if (os::protect_memory((char*)g_assert_poison, os::vm_page_size(), os::MEM_PROT_RWX) == false) {
#ifdef ASSERT
      fprintf(stderr, "Assertion poison page cannot be unprotected - mprotect failed with %d (%s)",
              errno, os::strerror(errno));
      fflush(stderr);
#endif
      return false; // unprotecting memory may fail in OOM situations, as surprising as this sounds.
    }
    // Store Context away.
    if (ucVoid) {
      const intx my_tid = os::current_thread_id();
      if (Atomic::cmpxchg(&g_asserting_thread, (intx)0, my_tid) == 0) {
        store_context(ucVoid);
        g_assertion_context = &g_stored_assertion_context;
      }
    }
    return true;
  }
  return false;
}
#endif // CAN_SHOW_REGISTERS_ON_ASSERT
