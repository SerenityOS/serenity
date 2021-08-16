/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm_io.h"
#include "jfr/jfrEvents.hpp"
#include "jfr/jni/jfrJavaSupport.hpp"
#include "jfr/leakprofiler/leakProfiler.hpp"
#include "jfr/recorder/repository/jfrEmergencyDump.hpp"
#include "jfr/recorder/service/jfrPostBox.hpp"
#include "jfr/recorder/service/jfrRecorderService.hpp"
#include "jfr/utilities/jfrTypes.hpp"
#include "logging/log.hpp"
#include "runtime/arguments.hpp"
#include "runtime/atomic.hpp"
#include "runtime/globals.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/os.hpp"
#include "runtime/thread.inline.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/ostream.hpp"

static const char vm_error_filename_fmt[] = "hs_err_pid%p.jfr";
static const char vm_oom_filename_fmt[] = "hs_oom_pid%p.jfr";
static const char vm_soe_filename_fmt[] = "hs_soe_pid%p.jfr";
static const char chunk_file_jfr_ext[] = ".jfr";
static const size_t iso8601_len = 19; // "YYYY-MM-DDTHH:MM:SS" (note: we just use a subset of the full timestamp)
static fio_fd emergency_fd = invalid_fd;
static const int64_t chunk_file_header_size = 68;
static const size_t chunk_file_extension_length = sizeof chunk_file_jfr_ext - 1;

/*
 * The emergency dump logic is restrictive when it comes to
 * using internal VM constructs such as ResourceArea / Handle / Arena.
 * The reason being that the thread context is unknown.
 *
 * A single static buffer of size JVM_MAXPATHLEN is used for building paths.
 * os::malloc / os::free are used in a few places.
 */

static char _path_buffer[JVM_MAXPATHLEN] = { 0 };

static bool is_path_empty() {
  return *_path_buffer == '\0';
}

// returns with an appended file separator (if successful)
static size_t get_current_directory() {
  if (os::get_current_directory(_path_buffer, sizeof(_path_buffer)) == NULL) {
    return 0;
  }
  const size_t cwd_len = strlen(_path_buffer);
  const int result = jio_snprintf(_path_buffer + cwd_len,
                                  sizeof(_path_buffer),
                                  "%s",
                                  os::file_separator());
  return (result == -1) ? 0 : strlen(_path_buffer);
}

static fio_fd open_exclusivly(const char* path) {
  assert((path != NULL) && (*path != '\0'), "invariant");
  return os::open(path, O_CREAT | O_RDWR, S_IREAD | S_IWRITE);
}

static bool is_emergency_dump_file_open() {
  return emergency_fd != invalid_fd;
}

static bool open_emergency_dump_fd(const char* path) {
  if (path == NULL) {
    return false;
  }
  assert(emergency_fd == invalid_fd, "invariant");
  emergency_fd = open_exclusivly(path);
  return emergency_fd != invalid_fd;
}

static void close_emergency_dump_file() {
  if (is_emergency_dump_file_open()) {
    os::close(emergency_fd);
  }
}

static const char* create_emergency_dump_path() {
  assert(is_path_empty(), "invariant");

  const size_t path_len = get_current_directory();
  if (path_len == 0) {
    return NULL;
  }
  const char* filename_fmt = NULL;
  // fetch specific error cause
  switch (JfrJavaSupport::cause()) {
    case JfrJavaSupport::OUT_OF_MEMORY:
      filename_fmt = vm_oom_filename_fmt;
      break;
    case JfrJavaSupport::STACK_OVERFLOW:
      filename_fmt = vm_soe_filename_fmt;
      break;
    default:
      filename_fmt = vm_error_filename_fmt;
  }
  const bool result = Arguments::copy_expand_pid(filename_fmt, strlen(filename_fmt), _path_buffer + path_len, JVM_MAXPATHLEN - path_len);
  return result ? _path_buffer : NULL;
}

static bool open_emergency_dump_file() {
  if (is_emergency_dump_file_open()) {
    // opened already
    return true;
  }
  return open_emergency_dump_fd(create_emergency_dump_path());
}

static void report(outputStream* st, bool emergency_file_opened, const char* repository_path) {
  assert(st != NULL, "invariant");
  if (emergency_file_opened) {
    st->print_raw("# JFR recording file will be written. Location: ");
    st->print_raw_cr(_path_buffer);
    st->print_raw_cr("#");
  } else if (repository_path != NULL) {
    st->print_raw("# The JFR repository may contain useful JFR files. Location: ");
    st->print_raw_cr(repository_path);
    st->print_raw_cr("#");
  } else if (!is_path_empty()) {
    st->print_raw("# Unable to create a JFR recording file at location: ");
    st->print_raw_cr(_path_buffer);
    st->print_raw_cr("#");
  }
}

void JfrEmergencyDump::on_vm_error_report(outputStream* st, const char* repository_path) {
  assert(st != NULL, "invariant");
  Thread* thread = Thread::current_or_null_safe();
  if (thread != NULL) {
    report(st, open_emergency_dump_file(), repository_path);
  } else if (repository_path != NULL) {
    // a non-attached thread will not be able to write anything later
    report(st, false, repository_path);
  }
}

static int file_sort(const char** const file1, const char** file2) {
  assert(NULL != *file1 && NULL != *file2, "invariant");
  int cmp = strncmp(*file1, *file2, iso8601_len);
  if (0 == cmp) {
    const char* const dot1 = strchr(*file1, '.');
    assert(NULL != dot1, "invariant");
    const char* const dot2 = strchr(*file2, '.');
    assert(NULL != dot2, "invariant");
    ptrdiff_t file1_len = dot1 - *file1;
    ptrdiff_t file2_len = dot2 - *file2;
    if (file1_len < file2_len) {
      return -1;
    }
    if (file1_len > file2_len) {
      return 1;
    }
    assert(file1_len == file2_len, "invariant");
    cmp = strncmp(*file1, *file2, file1_len);
  }
  assert(cmp != 0, "invariant");
  return cmp;
}

static void iso8601_to_date_time(char* iso8601_str) {
  assert(iso8601_str != NULL, "invariant");
  assert(strlen(iso8601_str) == iso8601_len, "invariant");
  // "YYYY-MM-DDTHH:MM:SS"
  for (size_t i = 0; i < iso8601_len; ++i) {
    switch (iso8601_str[i]) {
    case 'T':
    case '-':
    case ':':
      iso8601_str[i] = '_';
      break;
    }
  }
  // "YYYY_MM_DD_HH_MM_SS"
}

static void date_time(char* buffer, size_t buffer_len) {
  assert(buffer != NULL, "invariant");
  assert(buffer_len >= iso8601_len, "buffer too small");
  os::iso8601_time(buffer, buffer_len);
  assert(strlen(buffer) >= iso8601_len + 1, "invariant");
  // "YYYY-MM-DDTHH:MM:SS"
  buffer[iso8601_len] = '\0';
  iso8601_to_date_time(buffer);
}

static int64_t file_size(fio_fd fd) {
  assert(fd != invalid_fd, "invariant");
  const int64_t current_offset = os::current_file_offset(fd);
  const int64_t size = os::lseek(fd, 0, SEEK_END);
  os::seek_to_file_offset(fd, current_offset);
  return size;
}

class RepositoryIterator : public StackObj {
 private:
  GrowableArray<const char*>* _file_names;
  int _path_buffer_file_name_offset;
  mutable int _iterator;
  const char* fully_qualified(const char* file_name) const;
  const char* filter(const char* file_name) const;
 public:
  RepositoryIterator(const char* repository_path);
  ~RepositoryIterator();
  bool has_next() const;
  const char* next() const;
};

// append the file_name at the _path_buffer_file_name_offset position
const char* RepositoryIterator::fully_qualified(const char* file_name) const {
  assert(NULL != file_name, "invariant");
  assert(!is_path_empty(), "invariant");
  assert(_path_buffer_file_name_offset != 0, "invariant");

  const int result = jio_snprintf(_path_buffer + _path_buffer_file_name_offset,
                                  sizeof(_path_buffer) - _path_buffer_file_name_offset,
                                  "%s",
                                  file_name);
  return result != -1 ? _path_buffer : NULL;
}

// caller responsible for deallocation
const char* RepositoryIterator::filter(const char* file_name) const {
  if (file_name == NULL) {
    return NULL;
  }
  const size_t len = strlen(file_name);
  if ((len < chunk_file_extension_length) ||
      (strncmp(&file_name[len - chunk_file_extension_length],
               chunk_file_jfr_ext,
               chunk_file_extension_length) != 0)) {
    // not a .jfr file
    return NULL;
  }
  const char* fqn = fully_qualified(file_name);
  if (fqn == NULL) {
    return NULL;
  }
  const fio_fd fd = open_exclusivly(fqn);
  if (invalid_fd == fd) {
    return NULL;
  }
  const int64_t size = file_size(fd);
  os::close(fd);
  if (size <= chunk_file_header_size) {
    return NULL;
  }
  char* const file_name_copy = (char*)os::malloc(len + 1, mtTracing);
  if (file_name_copy == NULL) {
    log_error(jfr, system)("Unable to malloc memory during jfr emergency dump");
    return NULL;
  }
  strncpy(file_name_copy, file_name, len + 1);
  return file_name_copy;
}

RepositoryIterator::RepositoryIterator(const char* repository_path) :
  _file_names(NULL),
  _path_buffer_file_name_offset(0),
  _iterator(0) {
    DIR* dirp = os::opendir(repository_path);
    if (dirp == NULL) {
      log_error(jfr, system)("Unable to open repository %s", repository_path);
      return;
    }
    // store repository path in the path buffer and save that position
    _path_buffer_file_name_offset = jio_snprintf(_path_buffer,
                                                 sizeof(_path_buffer),
                                                 "%s%s",
                                                 repository_path,
                                                 os::file_separator());
    if (_path_buffer_file_name_offset == -1) {
      return;
    }
    _file_names = new (ResourceObj::C_HEAP, mtTracing) GrowableArray<const char*>(10, mtTracing);
    if (_file_names == NULL) {
      log_error(jfr, system)("Unable to malloc memory during jfr emergency dump");
      return;
    }
    // iterate files in the repository and append filtered file names to the files array
    struct dirent* dentry;
    while ((dentry = os::readdir(dirp)) != NULL) {
      const char* file_name = filter(dentry->d_name);
      if (file_name != NULL) {
        _file_names->append(file_name);
      }
    }
    os::closedir(dirp);
    if (_file_names->length() > 1) {
      _file_names->sort(file_sort);
    }
}

RepositoryIterator::~RepositoryIterator() {
  if (_file_names != NULL) {
    for (int i = 0; i < _file_names->length(); ++i) {
      os::free(const_cast<char*>(_file_names->at(i)));
    }
    delete _file_names;
  }
}

bool RepositoryIterator::has_next() const {
  return _file_names != NULL && _iterator < _file_names->length();
}

const char* RepositoryIterator::next() const {
  return _iterator >= _file_names->length() ? NULL : fully_qualified(_file_names->at(_iterator++));
}

static void write_repository_files(const RepositoryIterator& iterator, char* const copy_block, size_t block_size) {
  assert(is_emergency_dump_file_open(), "invariant");
  while (iterator.has_next()) {
    fio_fd current_fd = invalid_fd;
    const char* const fqn = iterator.next();
    assert(fqn != NULL, "invariant");
    current_fd = open_exclusivly(fqn);
    if (current_fd != invalid_fd) {
      const int64_t size = file_size(current_fd);
      assert(size > 0, "invariant");
      int64_t bytes_read = 0;
      int64_t bytes_written = 0;
      while (bytes_read < size) {
        const ssize_t read_result = os::read_at(current_fd, copy_block, (int)block_size, bytes_read);
        if (-1 == read_result) {
          log_info(jfr)( // For user, should not be "jfr, system"
              "Unable to recover JFR data");
          break;
        }
        bytes_read += (int64_t)read_result;
        assert(bytes_read - bytes_written <= (int64_t)block_size, "invariant");
        bytes_written += (int64_t)os::write(emergency_fd, copy_block, bytes_read - bytes_written);
        assert(bytes_read == bytes_written, "invariant");
      }
      os::close(current_fd);
    }
  }
}

static void write_emergency_dump_file(const RepositoryIterator& iterator) {
  static const size_t block_size = 1 * M; // 1 mb
  char* const copy_block = (char*)os::malloc(block_size, mtTracing);
  if (copy_block == NULL) {
    log_error(jfr, system)("Unable to malloc memory during jfr emergency dump");
    log_error(jfr, system)("Unable to write jfr emergency dump file");
  } else {
    write_repository_files(iterator, copy_block, block_size);
    os::free(copy_block);
  }
}

void JfrEmergencyDump::on_vm_error(const char* repository_path) {
  assert(repository_path != NULL, "invariant");
  if (open_emergency_dump_file()) {
    RepositoryIterator iterator(repository_path);
    write_emergency_dump_file(iterator);
    close_emergency_dump_file();
  }
}

static const char* create_emergency_chunk_path(const char* repository_path) {
  const size_t repository_path_len = strlen(repository_path);
  char date_time_buffer[32] = { 0 };
  date_time(date_time_buffer, sizeof(date_time_buffer));
  // append the individual substrings
  const int result = jio_snprintf(_path_buffer,
                                  JVM_MAXPATHLEN,
                                  "%s%s%s%s",
                                  repository_path,
                                  os::file_separator(),
                                  date_time_buffer,
                                  chunk_file_jfr_ext);
  return result == -1 ? NULL : _path_buffer;
}

const char* JfrEmergencyDump::chunk_path(const char* repository_path) {
  if (repository_path == NULL) {
    if (!open_emergency_dump_file()) {
      return NULL;
    }
    // We can directly use the emergency dump file name as the chunk.
    // The chunk writer will open its own fd so we close this descriptor.
    close_emergency_dump_file();
    assert(!is_path_empty(), "invariant");
    return _path_buffer;
  }
  return create_emergency_chunk_path(repository_path);
}

/*
* We are just about to exit the VM, so we will be very aggressive
* at this point in order to increase overall success of dumping jfr data.
*
* If we end up deadlocking in the attempt of dumping out jfr data,
* we rely on the WatcherThread task "is_error_reported()",
* to exit the VM after a hard-coded timeout (disallow WatcherThread to emergency dump).
* This "safety net" somewhat explains the aggressiveness in this attempt.
*
*/
static bool prepare_for_emergency_dump(Thread* thread) {
  assert(thread != NULL, "invariant");
  if (thread->is_Watcher_thread()) {
    // need WatcherThread as a safeguard against potential deadlocks
    return false;
  }

#ifdef ASSERT
  Mutex* owned_lock = thread->owned_locks();
  while (owned_lock != NULL) {
    Mutex* next = owned_lock->next();
    owned_lock->unlock();
    owned_lock = next;
  }
#endif // ASSERT

  if (Threads_lock->owned_by_self()) {
    Threads_lock->unlock();
  }

  if (Module_lock->owned_by_self()) {
    Module_lock->unlock();
  }

  if (ClassLoaderDataGraph_lock->owned_by_self()) {
    ClassLoaderDataGraph_lock->unlock();
  }

  if (Heap_lock->owned_by_self()) {
    Heap_lock->unlock();
  }

  if (VMOperation_lock->owned_by_self()) {
    VMOperation_lock->unlock();
  }

  if (Service_lock->owned_by_self()) {
    Service_lock->unlock();
  }

  if (UseNotificationThread && Notification_lock->owned_by_self()) {
    Notification_lock->unlock();
  }

  if (CodeCache_lock->owned_by_self()) {
    CodeCache_lock->unlock();
  }

  if (PeriodicTask_lock->owned_by_self()) {
    PeriodicTask_lock->unlock();
  }

  if (JfrMsg_lock->owned_by_self()) {
    JfrMsg_lock->unlock();
  }

  if (JfrBuffer_lock->owned_by_self()) {
    JfrBuffer_lock->unlock();
  }

  if (JfrStacktrace_lock->owned_by_self()) {
    JfrStacktrace_lock->unlock();
  }
  return true;
}

static volatile int jfr_shutdown_lock = 0;

static bool guard_reentrancy() {
  return Atomic::cmpxchg(&jfr_shutdown_lock, 0, 1) == 0;
}

class JavaThreadInVMAndNative : public StackObj {
 private:
  JavaThread* const _jt;
  JavaThreadState _original_state;
 public:

  JavaThreadInVMAndNative(Thread* t) : _jt(t->is_Java_thread() ? JavaThread::cast(t) : NULL),
                                       _original_state(_thread_max_state) {
    if (_jt != NULL) {
      _original_state = _jt->thread_state();
      if (_original_state != _thread_in_vm) {
        _jt->set_thread_state(_thread_in_vm);
      }
    }
  }

  ~JavaThreadInVMAndNative() {
    if (_original_state != _thread_max_state) {
      _jt->set_thread_state(_original_state);
    }
  }

  void transition_to_native() {
    if (_jt != NULL) {
      assert(_jt->thread_state() == _thread_in_vm, "invariant");
      _jt->set_thread_state(_thread_in_native);
    }
  }
};

static void post_events(bool exception_handler, Thread* thread) {
  if (exception_handler) {
    EventShutdown e;
    e.set_reason("VM Error");
    e.commit();
  } else {
    // OOM
    LeakProfiler::emit_events(max_jlong, false, false);
  }
  EventDumpReason event;
  event.set_reason(exception_handler ? "Crash" : "Out of Memory");
  event.set_recordingId(-1);
  event.commit();
}

void JfrEmergencyDump::on_vm_shutdown(bool exception_handler) {
  if (!guard_reentrancy()) {
    return;
  }
  Thread* thread = Thread::current_or_null_safe();
  if (thread == NULL) {
    return;
  }
  // Ensure a JavaThread is _thread_in_vm when we make this call
  JavaThreadInVMAndNative jtivm(thread);
  if (!prepare_for_emergency_dump(thread)) {
    return;
  }
  post_events(exception_handler, thread);
  // if JavaThread, transition to _thread_in_native to issue a final flushpoint
  NoHandleMark nhm;
  jtivm.transition_to_native();
  const int messages = MSGBIT(MSG_VM_ERROR);
  JfrRecorderService service;
  service.rotate(messages);
}
