/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 */

#include "precompiled.hpp"
#include "classfile/systemDictionary.hpp"
#include "compiler/compileTask.hpp"
#include "compiler/compilerThread.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "jvmci/jvmci.hpp"
#include "jvmci/jvmciJavaClasses.hpp"
#include "jvmci/jvmciEnv.hpp"
#include "jvmci/jvmciRuntime.hpp"
#include "jvmci/metadataHandles.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "runtime/arguments.hpp"
#include "runtime/atomic.hpp"
#include "utilities/events.hpp"

JVMCIRuntime* JVMCI::_compiler_runtime = NULL;
JVMCIRuntime* JVMCI::_java_runtime = NULL;
volatile bool JVMCI::_is_initialized = false;
bool JVMCI::_box_caches_initialized = false;
void* JVMCI::_shared_library_handle = NULL;
char* JVMCI::_shared_library_path = NULL;
volatile bool JVMCI::_in_shutdown = false;
StringEventLog* JVMCI::_events = NULL;
StringEventLog* JVMCI::_verbose_events = NULL;
volatile intx JVMCI::_fatal_log_init_thread = -1;
volatile int JVMCI::_fatal_log_fd = -1;
const char* JVMCI::_fatal_log_filename = NULL;

void jvmci_vmStructs_init() NOT_DEBUG_RETURN;

bool JVMCI::can_initialize_JVMCI() {
  // Initializing JVMCI requires the module system to be initialized past phase 3.
  // The JVMCI API itself isn't available until phase 2 and ServiceLoader (which
  // JVMCI initialization requires) isn't usable until after phase 3. Testing
  // whether the system loader is initialized satisfies all these invariants.
  if (SystemDictionary::java_system_loader() == NULL) {
    return false;
  }
  assert(Universe::is_module_initialized(), "must be");
  return true;
}

void* JVMCI::get_shared_library(char*& path, bool load) {
  void* sl_handle = _shared_library_handle;
  if (sl_handle != NULL || !load) {
    path = _shared_library_path;
    return sl_handle;
  }
  assert(JVMCI_lock->owner() == Thread::current(), "must be");
  path = NULL;
  if (_shared_library_handle == NULL) {
    char path[JVM_MAXPATHLEN];
    char ebuf[1024];
    if (JVMCILibPath != NULL) {
      if (!os::dll_locate_lib(path, sizeof(path), JVMCILibPath, JVMCI_SHARED_LIBRARY_NAME)) {
        fatal("Unable to create path to JVMCI shared library based on value of JVMCILibPath (%s)", JVMCILibPath);
      }
    } else {
      if (!os::dll_locate_lib(path, sizeof(path), Arguments::get_dll_dir(), JVMCI_SHARED_LIBRARY_NAME)) {
        fatal("Unable to create path to JVMCI shared library");
      }
    }

    void* handle = os::dll_load(path, ebuf, sizeof ebuf);
    if (handle == NULL) {
      fatal("Unable to load JVMCI shared library from %s: %s", path, ebuf);
    }
    _shared_library_handle = handle;
    _shared_library_path = strdup(path);

    JVMCI_event_1("loaded JVMCI shared library from %s", path);
  }
  path = _shared_library_path;
  return _shared_library_handle;
}

void JVMCI::initialize_compiler(TRAPS) {
  if (JVMCILibDumpJNIConfig) {
    JNIJVMCI::initialize_ids(NULL);
    ShouldNotReachHere();
  }

  JVMCI::compiler_runtime()->call_getCompiler(CHECK);
}

void JVMCI::initialize_globals() {
  jvmci_vmStructs_init();
  if (LogEvents) {
    if (JVMCIEventLogLevel > 0) {
      _events = new StringEventLog("JVMCI Events", "jvmci");
      if (JVMCIEventLogLevel > 1) {
        int count = LogEventsBufferEntries;
        for (int i = 1; i < JVMCIEventLogLevel && i < max_EventLog_level; i++) {
          // Expand event buffer by 10x for each level above 1
          count = count * 10;
        }
        _verbose_events = new StringEventLog("Verbose JVMCI Events", "verbose-jvmci", count);
      }
    }
  }
  if (UseJVMCINativeLibrary) {
    // There are two runtimes.
    _compiler_runtime = new JVMCIRuntime(0);
    _java_runtime = new JVMCIRuntime(-1);
  } else {
    // There is only a single runtime
    _java_runtime = _compiler_runtime = new JVMCIRuntime(0);
  }
}

void JVMCI::ensure_box_caches_initialized(TRAPS) {
  if (_box_caches_initialized) {
    return;
  }

  // While multiple threads may reach here, that's fine
  // since class initialization is synchronized.
  Symbol* box_classes[] = {
    java_lang_Boolean::symbol(),
    java_lang_Byte_ByteCache::symbol(),
    java_lang_Short_ShortCache::symbol(),
    java_lang_Character_CharacterCache::symbol(),
    java_lang_Integer_IntegerCache::symbol(),
    java_lang_Long_LongCache::symbol()
  };

  for (unsigned i = 0; i < sizeof(box_classes) / sizeof(Symbol*); i++) {
    Klass* k = SystemDictionary::resolve_or_fail(box_classes[i], true, CHECK);
    InstanceKlass* ik = InstanceKlass::cast(k);
    if (ik->is_not_initialized()) {
      ik->initialize(CHECK);
    }
  }
  _box_caches_initialized = true;
}

JavaThread* JVMCI::compilation_tick(JavaThread* thread) {
  if (thread->is_Compiler_thread()) {
    CompileTask *task = CompilerThread::cast(thread)->task();
    if (task != NULL) {
      JVMCICompileState *state = task->blocking_jvmci_compile_state();
      if (state != NULL) {
        state->inc_compilation_ticks();
      }
    }
  }
  return thread;
}

void JVMCI::metadata_do(void f(Metadata*)) {
  if (_java_runtime != NULL) {
    _java_runtime->_metadata_handles->metadata_do(f);
  }
  if (_compiler_runtime != NULL && _compiler_runtime != _java_runtime) {
    _compiler_runtime->_metadata_handles->metadata_do(f);
  }
}

void JVMCI::do_unloading(bool unloading_occurred) {
  if (unloading_occurred) {
    if (_java_runtime != NULL) {
      _java_runtime->_metadata_handles->do_unloading();
    }
    if (_compiler_runtime != NULL && _compiler_runtime != _java_runtime) {
      _compiler_runtime->_metadata_handles->do_unloading();
    }
  }
}

bool JVMCI::is_compiler_initialized() {
  return _is_initialized;
}

void JVMCI::shutdown() {
  ResourceMark rm;
  {
    MutexLocker locker(JVMCI_lock);
    _in_shutdown = true;
    JVMCI_event_1("shutting down JVMCI");
  }
  JVMCIRuntime* java_runtime = _java_runtime;
  if (java_runtime != compiler_runtime()) {
    java_runtime->shutdown();
  }
  if (compiler_runtime() != NULL) {
    compiler_runtime()->shutdown();
  }
}

bool JVMCI::in_shutdown() {
  return _in_shutdown;
}

void JVMCI::fatal_log(const char* buf, size_t count) {
  intx current_thread_id = os::current_thread_id();
  intx invalid_id = -1;
  int log_fd;
  if (_fatal_log_init_thread == invalid_id && Atomic::cmpxchg(&_fatal_log_init_thread, invalid_id, current_thread_id) == invalid_id) {
    if (ErrorFileToStdout) {
      log_fd = 1;
    } else if (ErrorFileToStderr) {
      log_fd = 2;
    } else {
      static char name_buffer[O_BUFLEN];
      log_fd = VMError::prepare_log_file(JVMCINativeLibraryErrorFile, LIBJVMCI_ERR_FILE, true, name_buffer, sizeof(name_buffer));
      if (log_fd != -1) {
        _fatal_log_filename = name_buffer;
      } else {
        int e = errno;
        tty->print("Can't open JVMCI shared library error report file. Error: ");
        tty->print_raw_cr(os::strerror(e));
        tty->print_cr("JVMCI shared library error report will be written to console.");

        // See notes in VMError::report_and_die about hard coding tty to 1
        log_fd = 1;
      }
    }
    _fatal_log_fd = log_fd;
  } else {
    // Another thread won the race to initialize the stream. Give it time
    // to complete initialization. VM locks cannot be used as the current
    // thread might not be attached to the VM (e.g. a native thread started
    // within libjvmci).
    while (_fatal_log_fd == -1) {
      os::naked_short_sleep(50);
    }
  }
  fdStream log(_fatal_log_fd);
  log.write(buf, count);
  log.flush();
}

void JVMCI::vlog(int level, const char* format, va_list ap) {
  if (LogEvents && JVMCIEventLogLevel >= level) {
    StringEventLog* events = level == 1 ? _events : _verbose_events;
    guarantee(events != NULL, "JVMCI event log not yet initialized");
    Thread* thread = Thread::current_or_null_safe();
    if (thread != NULL) {
      events->logv(thread, format, ap);
    }
  }
}

void JVMCI::vtrace(int level, const char* format, va_list ap) {
  if (JVMCITraceLevel >= level) {
    Thread* thread = Thread::current_or_null_safe();
    if (thread != NULL) {
      ResourceMark rm;
      tty->print("JVMCITrace-%d[%s]:%*c", level, thread->name(), level, ' ');
    } else {
      tty->print("JVMCITrace-%d[?]:%*c", level, level, ' ');
    }
    tty->vprint_cr(format, ap);
  }
}

#define LOG_TRACE(level) { va_list ap; \
  va_start(ap, format); vlog(level, format, ap); va_end(ap); \
  va_start(ap, format); vtrace(level, format, ap); va_end(ap); \
}

void JVMCI::event(int level, const char* format, ...) LOG_TRACE(level)
void JVMCI::event1(const char* format, ...) LOG_TRACE(1)
void JVMCI::event2(const char* format, ...) LOG_TRACE(2)
void JVMCI::event3(const char* format, ...) LOG_TRACE(3)
void JVMCI::event4(const char* format, ...) LOG_TRACE(4)

#undef LOG_TRACE
