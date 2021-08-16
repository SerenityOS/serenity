/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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


#ifndef SHARE_UTILITIES_DECODER_HPP
#define SHARE_UTILITIES_DECODER_HPP

#include "memory/allocation.hpp"
#include "runtime/mutex.hpp"
#include "runtime/mutexLocker.hpp"
#include "utilities/ostream.hpp"

class AbstractDecoder : public CHeapObj<mtInternal> {
public:
  // status code for decoding native C frame
  enum decoder_status {
         not_available = -10,  // real decoder is not available
         no_error = 0,         // no error encountered
         out_of_memory,        // out of memory
         file_invalid,         // invalid elf file
         file_not_found,       // could not found symbol file (on windows), such as jvm.pdb or jvm.map
         helper_func_error,    // decoding functions not found (Windows only)
         helper_init_error     // SymInitialize failed (Windows only)
  };

protected:
  decoder_status  _decoder_status;

public:
  virtual ~AbstractDecoder() {}

  // decode an pc address to corresponding function name and an offset from the beginning of
  // the function
  //
  // Note: the 'base' variant does not demangle names. The
  // demangling that was done systematically in the 'modulepath' variant
  // is now optional.
  virtual bool decode(address pc, char* buf, int buflen, int* offset,
                      const char* modulepath = NULL, bool demangle = true) = 0;
  virtual bool decode(address pc, char* buf, int buflen, int* offset, const void* base) = 0;

  // demangle a C++ symbol
  virtual bool demangle(const char* symbol, char* buf, int buflen) = 0;

  virtual decoder_status status() const {
    return _decoder_status;
  }

  virtual bool has_error() const {
    return is_error(_decoder_status);
  }

  static bool is_error(decoder_status status) {
    return (status > no_error);
  }
};

// Do nothing decoder
class NullDecoder : public AbstractDecoder {
public:
  NullDecoder() {
    _decoder_status = not_available;
  }

  virtual ~NullDecoder() {};

  virtual bool decode(address pc, char* buf, int buflen, int* offset,
                      const char* modulepath, bool demangle) {
    return false;
  }

  virtual bool decode(address pc, char* buf, int buflen, int* offset, const void* base) {
    return false;
  }

  virtual bool demangle(const char* symbol, char* buf, int buflen) {
    return false;
  }
};

class Decoder : AllStatic {
public:
  static bool decode(address pc, char* buf, int buflen, int* offset, const char* modulepath = NULL, bool demangle = true);
  static bool decode(address pc, char* buf, int buflen, int* offset, bool demangle) {
    return decode(pc, buf, buflen, offset, (const char*) NULL, demangle);
  }
  static bool decode(address pc, char* buf, int buflen, int* offset, const void* base);
  static bool demangle(const char* symbol, char* buf, int buflen);

  // Attempts to retrieve source file name and line number associated with a pc.
  // If buf != NULL, points to a buffer of size buflen which will receive the
  // file name. File name will be silently truncated if output buffer is too small.
  static bool get_source_info(address pc, char* buf, size_t buflen, int* line);

  static void print_state_on(outputStream* st);

protected:
  // shared decoder instance, _shared_instance_lock is needed
  static AbstractDecoder* get_shared_instance();
  // a private instance for error handler. Error handler can be
  // triggered almost everywhere, including signal handler, where
  // no lock can be taken. So the shared decoder can not be used
  // in this scenario.
  static AbstractDecoder* get_error_handler_instance();

  static AbstractDecoder* create_decoder();
private:
  static AbstractDecoder*     _shared_decoder;
  static AbstractDecoder*     _error_handler_decoder;
  static NullDecoder          _do_nothing_decoder;

protected:
  static Mutex* shared_decoder_lock();

  friend class DecoderLocker;
};

#endif // SHARE_UTILITIES_DECODER_HPP
