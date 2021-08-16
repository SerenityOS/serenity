/*
 * Copyright (c) 2020 SAP SE. All rights reserved.
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
#include "runtime/arguments.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/os.hpp"
#include "runtime/thread.inline.hpp"
#include "services/heapDumperCompression.hpp"


char const* FileWriter::open_writer() {
  assert(_fd < 0, "Must not already be open");

  _fd = os::create_binary_file(_path, _overwrite);

  if (_fd < 0) {
    return os::strerror(errno);
  }

  return NULL;
}

FileWriter::~FileWriter() {
  if (_fd >= 0) {
    os::close(_fd);
    _fd = -1;
  }
}

char const* FileWriter::write_buf(char* buf, ssize_t size) {
  assert(_fd >= 0, "Must be open");
  assert(size > 0, "Must write at least one byte");

  ssize_t n = (ssize_t) os::write(_fd, buf, (uint) size);

  if (n <= 0) {
    return os::strerror(errno);
  }

  return NULL;
}


typedef char const* (*GzipInitFunc)(size_t, size_t*, size_t*, int);
typedef size_t(*GzipCompressFunc)(char*, size_t, char*, size_t, char*, size_t,
                                  int, char*, char const**);

static GzipInitFunc gzip_init_func;
static GzipCompressFunc gzip_compress_func;

void* GZipCompressor::load_gzip_func(char const* name) {
  char path[JVM_MAXPATHLEN];
  char ebuf[1024];
  void* handle;
  MutexLocker locker(Zip_lock, Monitor::_no_safepoint_check_flag);

  if (os::dll_locate_lib(path, sizeof(path), Arguments::get_dll_dir(), "zip")) {
    handle = os::dll_load(path, ebuf, sizeof ebuf);

    if (handle != NULL) {
      return os::dll_lookup(handle, name);
    }
  }

  return NULL;
}

char const* GZipCompressor::init(size_t block_size, size_t* needed_out_size,
                                 size_t* needed_tmp_size) {
  _block_size = block_size;
  _is_first = true;

  if (gzip_compress_func == NULL) {
    gzip_compress_func = (GzipCompressFunc) load_gzip_func("ZIP_GZip_Fully");

    if (gzip_compress_func == NULL) {
      return "Cannot get ZIP_GZip_Fully function";
    }
  }

  if (gzip_init_func == NULL) {
    gzip_init_func = (GzipInitFunc) load_gzip_func("ZIP_GZip_InitParams");

    if (gzip_init_func == NULL) {
      return "Cannot get ZIP_GZip_InitParams function";
    }
  }

  char const* result = gzip_init_func(block_size, needed_out_size,
                                      needed_tmp_size, _level);
  *needed_out_size += 1024; // Add extra space for the comment in the first chunk.

  return result;
}

char const* GZipCompressor::compress(char* in, size_t in_size, char* out, size_t out_size,
                                     char* tmp, size_t tmp_size, size_t* compressed_size) {
  char const* msg = NULL;

  if (_is_first) {
    char buf[128];
    // Write the block size used as a comment in the first gzip chunk, so the
    // code used to read it later can make a good choice of the buffer sizes it uses.
    jio_snprintf(buf, sizeof(buf), "HPROF BLOCKSIZE=" SIZE_FORMAT, _block_size);
    *compressed_size = gzip_compress_func(in, in_size, out, out_size, tmp, tmp_size, _level,
                                          buf, &msg);
    _is_first = false;
  } else {
    *compressed_size = gzip_compress_func(in, in_size, out, out_size, tmp, tmp_size, _level,
                                          NULL, &msg);
  }

  return msg;
}

WorkList::WorkList() {
  _head._next = &_head;
  _head._prev = &_head;
}

void WorkList::insert(WriteWork* before, WriteWork* work) {
  work->_prev = before;
  work->_next = before->_next;
  before->_next = work;
  work->_next->_prev = work;
}

WriteWork* WorkList::remove(WriteWork* work) {
  if (work != NULL) {
    assert(work->_next != work, "Invalid next");
    assert(work->_prev != work, "Invalid prev");
    work->_prev->_next = work->_next;;
    work->_next->_prev = work->_prev;
    work->_next = NULL;
    work->_prev = NULL;
  }

  return work;
}

void WorkList::add_by_id(WriteWork* work) {
  if (is_empty()) {
    add_first(work);
  } else {
    WriteWork* last_curr = &_head;
    WriteWork* curr = _head._next;

    while (curr->_id < work->_id) {
      last_curr = curr;
      curr = curr->_next;

      if (curr == &_head) {
        add_last(work);
        return;
      }
    }

    insert(last_curr, work);
  }
}



CompressionBackend::CompressionBackend(AbstractWriter* writer,
     AbstractCompressor* compressor, size_t block_size, size_t max_waste) :
  _active(false),
  _err(NULL),
  _nr_of_threads(0),
  _works_created(0),
  _work_creation_failed(false),
  _id_to_write(0),
  _next_id(0),
  _in_size(block_size),
  _max_waste(max_waste),
  _out_size(0),
  _tmp_size(0),
  _written(0),
  _writer(writer),
  _compressor(compressor),
  _lock(new (std::nothrow) PaddedMonitor(Mutex::leaf, "HProf Compression Backend",
    true, Mutex::_safepoint_check_never)) {
  if (_writer == NULL) {
    set_error("Could not allocate writer");
  } else if (_lock == NULL) {
    set_error("Could not allocate lock");
  } else {
    set_error(_writer->open_writer());
  }

  if (_compressor != NULL) {
    set_error(_compressor->init(_in_size, &_out_size, &_tmp_size));
  }

  _current = allocate_work(_in_size, _out_size, _tmp_size);

  if (_current == NULL) {
    set_error("Could not allocate memory for buffer");
  }

  _active = (_err == NULL);
}

CompressionBackend::~CompressionBackend() {
  assert(!_active, "Must not be active by now");
  assert(_nr_of_threads == 0, "Must have no active threads");
  assert(_to_compress.is_empty() && _finished.is_empty(), "Still work to do");

  free_work_list(&_unused);
  free_work(_current);
  assert(_works_created == 0, "All work must have been freed");

  delete _compressor;
  delete _writer;
  delete _lock;
}

void CompressionBackend::deactivate() {
  assert(_active, "Must be active");

  MonitorLocker ml(_lock, Mutex::_no_safepoint_check_flag);

  // Make sure we write the last partially filled buffer.
  if ((_current != NULL) && (_current->_in_used > 0)) {
    _current->_id = _next_id++;
    _to_compress.add_last(_current);
    _current = NULL;
    ml.notify_all();
  }

  // Wait for the threads to drain the compression work list and do some work yourself.
  while (!_to_compress.is_empty()) {
    do_foreground_work();
  }

  _active = false;
  ml.notify_all();
}

void CompressionBackend::thread_loop() {
  {
    MonitorLocker ml(_lock, Mutex::_no_safepoint_check_flag);
    _nr_of_threads++;
  }

  WriteWork* work;
  while ((work = get_work()) != NULL) {
    do_compress(work);
    finish_work(work);
  }

  MonitorLocker ml(_lock, Mutex::_no_safepoint_check_flag);
  _nr_of_threads--;
  assert(_nr_of_threads >= 0, "Too many threads finished");
}

void CompressionBackend::set_error(char const* new_error) {
  if ((new_error != NULL) && (_err == NULL)) {
    _err = new_error;
  }
}

WriteWork* CompressionBackend::allocate_work(size_t in_size, size_t out_size,
                                             size_t tmp_size) {
  WriteWork* result = (WriteWork*) os::malloc(sizeof(WriteWork), mtInternal);

  if (result == NULL) {
    _work_creation_failed = true;
    return NULL;
  }

  _works_created++;
  result->_in = (char*) os::malloc(in_size, mtInternal);
  result->_in_max = in_size;
  result->_in_used = 0;
  result->_out = NULL;
  result->_tmp = NULL;

  if (result->_in == NULL) {
    goto fail;
  }

  if (out_size > 0) {
    result->_out = (char*) os::malloc(out_size, mtInternal);
    result->_out_used = 0;
    result->_out_max = out_size;

    if (result->_out == NULL) {
      goto fail;
    }
  }

  if (tmp_size > 0) {
    result->_tmp = (char*) os::malloc(tmp_size, mtInternal);
    result->_tmp_max = tmp_size;

    if (result->_tmp == NULL) {
      goto fail;
    }
  }

  return result;

fail:
  free_work(result);
  _work_creation_failed = true;
  return NULL;
}

void CompressionBackend::free_work(WriteWork* work) {
  if (work != NULL) {
    os::free(work->_in);
    os::free(work->_out);
    os::free(work->_tmp);
    os::free(work);
    --_works_created;
  }
}

void CompressionBackend::free_work_list(WorkList* list) {
  while (!list->is_empty()) {
    free_work(list->remove_first());
  }
}

void CompressionBackend::do_foreground_work() {
  assert(!_to_compress.is_empty(), "Must have work to do");
  assert(_lock->owned_by_self(), "Must have the lock");

  WriteWork* work = _to_compress.remove_first();
  MutexUnlocker mu(_lock, Mutex::_no_safepoint_check_flag);
  do_compress(work);
  finish_work(work);
}

WriteWork* CompressionBackend::get_work() {
  MonitorLocker ml(_lock, Mutex::_no_safepoint_check_flag);

  while (_active && _to_compress.is_empty()) {
    ml.wait();
  }

  return _to_compress.remove_first();
}

void CompressionBackend::get_new_buffer(char** buffer, size_t* used, size_t* max) {
  if (_active) {
    MonitorLocker ml(_lock, Mutex::_no_safepoint_check_flag);

    if (*used > 0) {
      _current->_in_used += *used;

      // Check if we do not waste more than _max_waste. If yes, write the buffer.
      // Otherwise return the rest of the buffer as the new buffer.
      if (_current->_in_max - _current->_in_used <= _max_waste) {
        _current->_id = _next_id++;
        _to_compress.add_last(_current);
        _current = NULL;
        ml.notify_all();
      } else {
        *buffer = _current->_in + _current->_in_used;
        *used = 0;
        *max = _current->_in_max - _current->_in_used;

        return;
      }
    }

    while ((_current == NULL) && _unused.is_empty() && _active) {
      // Add more work objects if needed.
      if (!_work_creation_failed && (_works_created <= _nr_of_threads)) {
        WriteWork* work = allocate_work(_in_size, _out_size, _tmp_size);

        if (work != NULL) {
          _unused.add_first(work);
        }
      } else if (!_to_compress.is_empty() && (_nr_of_threads == 0)) {
        do_foreground_work();
      } else {
        ml.wait();
      }
    }

    if (_current == NULL) {
      _current = _unused.remove_first();
    }

    if (_current != NULL) {
      _current->_in_used = 0;
      _current->_out_used = 0;
      *buffer = _current->_in;
      *used = 0;
      *max = _current->_in_max;

      return;
    }
  }

  *buffer = NULL;
  *used = 0;
  *max = 0;

  return;
}

void CompressionBackend::do_compress(WriteWork* work) {
  if (_compressor != NULL) {
    char const* msg = _compressor->compress(work->_in, work->_in_used, work->_out,
                                            work->_out_max,
    work->_tmp, _tmp_size, &work->_out_used);

    if (msg != NULL) {
      MutexLocker ml(_lock, Mutex::_no_safepoint_check_flag);
      set_error(msg);
    }
  }
}

void CompressionBackend::finish_work(WriteWork* work) {
  MonitorLocker ml(_lock, Mutex::_no_safepoint_check_flag);

  _finished.add_by_id(work);

  // Write all finished works as far as we can.
  while (!_finished.is_empty() && (_finished.first()->_id == _id_to_write)) {
    WriteWork* to_write = _finished.remove_first();
    size_t size = _compressor == NULL ? to_write->_in_used : to_write->_out_used;
    char* p = _compressor == NULL ? to_write->_in : to_write->_out;
    char const* msg = NULL;

    if (_err == NULL) {
      _written += size;
      MutexUnlocker mu(_lock, Mutex::_no_safepoint_check_flag);
      msg = _writer->write_buf(p, (ssize_t) size);
    }

    set_error(msg);
    _unused.add_first(to_write);
    _id_to_write++;
  }

  ml.notify_all();
}
