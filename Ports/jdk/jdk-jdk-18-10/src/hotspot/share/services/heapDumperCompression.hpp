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

#ifndef SHARE_SERVICES_HEAPDUMPERCOMPRESSION_HPP
#define SHARE_SERVICES_HEAPDUMPERCOMPRESSION_HPP

#include "memory/allocation.hpp"


// Interface for a compression  implementation.
class AbstractCompressor : public CHeapObj<mtInternal> {
public:
  virtual ~AbstractCompressor() { }

  // Initializes the compressor. Returns a static error message in case of an error.
  // Otherwise initializes the needed out and tmp size for the given block size.
  virtual char const* init(size_t block_size, size_t* needed_out_size,
                           size_t* needed_tmp_size) = 0;

  // Does the actual compression. Returns NULL on success and a static error
  // message otherwise. Sets the 'compressed_size'.
  virtual char const* compress(char* in, size_t in_size, char* out, size_t out_size,
                               char* tmp, size_t tmp_size, size_t* compressed_size) = 0;
};

// Interface for a writer implementation.
class AbstractWriter : public CHeapObj<mtInternal> {
public:
  virtual ~AbstractWriter() { }

  // Opens the writer. Returns NULL on success and a static error message otherwise.
  virtual char const* open_writer() = 0;

  // Does the write. Returns NULL on success and a static error message otherwise.
  virtual char const* write_buf(char* buf, ssize_t size) = 0;
};


// A writer for a file.
class FileWriter : public AbstractWriter {
private:
  char const* _path;
  bool _overwrite;
  int _fd;

public:
  FileWriter(char const* path, bool overwrite) : _path(path), _overwrite(overwrite), _fd(-1) { }

  ~FileWriter();

  // Opens the writer. Returns NULL on success and a static error message otherwise.
  virtual char const* open_writer();

  // Does the write. Returns NULL on success and a static error message otherwise.
  virtual char const* write_buf(char* buf, ssize_t size);
};


// A compressor using the gzip format.
class GZipCompressor : public AbstractCompressor {
private:
  int _level;
  size_t _block_size;
  bool _is_first;

  void* load_gzip_func(char const* name);

public:
  GZipCompressor(int level) : _level(level), _block_size(0), _is_first(false) {
  }

  virtual char const* init(size_t block_size, size_t* needed_out_size,
                           size_t* needed_tmp_size);

  virtual char const* compress(char* in, size_t in_size, char* out, size_t out_size,
                               char* tmp, size_t tmp_size, size_t* compressed_size);
};


// The data needed to write a single buffer (and compress it optionally).
struct WriteWork {
  // The id of the work.
  int64_t _id;

  // The input buffer where the raw data is
  char* _in;
  size_t _in_used;
  size_t _in_max;

  // The output buffer where the compressed data is. Is NULL when compression is disabled.
  char* _out;
  size_t _out_used;
  size_t _out_max;

  // The temporary space needed for compression. Is NULL when compression is disabled.
  char* _tmp;
  size_t _tmp_max;

  // Used to link WriteWorks into lists.
  WriteWork* _next;
  WriteWork* _prev;
};

// A list for works.
class WorkList {
private:
  WriteWork _head;

  void insert(WriteWork* before, WriteWork* work);
  WriteWork* remove(WriteWork* work);

public:
  WorkList();

  // Return true if the list is empty.
  bool is_empty() { return _head._next == &_head; }

  // Adds to the beginning of the list.
  void add_first(WriteWork* work) { insert(&_head, work); }

  // Adds to the end of the list.
  void add_last(WriteWork* work) { insert(_head._prev, work); }

  // Adds so the ids are ordered.
  void add_by_id(WriteWork* work);

  // Returns the first element.
  WriteWork* first() { return is_empty() ? NULL : _head._next; }

  // Returns the last element.
  WriteWork* last() { return is_empty() ? NULL : _head._prev; }

  // Removes the first element. Returns NULL if empty.
  WriteWork* remove_first() { return remove(first()); }

  // Removes the last element. Returns NULL if empty.
  WriteWork* remove_last() { return remove(first()); }
};


class Monitor;

// This class is used by the DumpWriter class. It supplies the DumpWriter with
// chunks of memory to write the heap dump data into. When the DumpWriter needs a
// new memory chunk, it calls get_new_buffer(), which commits the old chunk used
// and returns a new chunk. The old chunk is then added to a queue to be compressed
// and then written in the background.
class CompressionBackend : StackObj {
  bool _active;
  char const * _err;

  int _nr_of_threads;
  int _works_created;
  bool _work_creation_failed;

  int64_t _id_to_write;
  int64_t _next_id;

  size_t _in_size;
  size_t _max_waste;
  size_t _out_size;
  size_t _tmp_size;

  size_t _written;

  AbstractWriter* const _writer;
  AbstractCompressor* const _compressor;

  Monitor* const _lock;

  WriteWork* _current;
  WorkList _to_compress;
  WorkList _unused;
  WorkList _finished;

  void set_error(char const* new_error);

  WriteWork* allocate_work(size_t in_size, size_t out_size, size_t tmp_size);
  void free_work(WriteWork* work);
  void free_work_list(WorkList* list);

  void do_foreground_work();
  WriteWork* get_work();
  void do_compress(WriteWork* work);
  void finish_work(WriteWork* work);

public:
  // compressor can be NULL if no compression is used.
  // Takes ownership of the writer and compressor.
  // block_size is the buffer size of a WriteWork.
  // max_waste is the maximum number of bytes to leave
  // empty in the buffer when it is written.
  CompressionBackend(AbstractWriter* writer, AbstractCompressor* compressor,
    size_t block_size, size_t max_waste);

  ~CompressionBackend();

  size_t get_written() const { return _written; }

  char const* error() const { return _err; }

  // Commits the old buffer (using the value in *used) and sets up a new one.
  void get_new_buffer(char** buffer, size_t* used, size_t* max);

  // The entry point for a worker thread.
  void thread_loop();

  // Shuts down the backend, releasing all threads.
  void deactivate();
};


#endif // SHARE_SERVICES_HEAPDUMPERCOMPRESSION_HPP
