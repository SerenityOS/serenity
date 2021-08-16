/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2017, 2020 SAP SE. All rights reserved.
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
#include "memory/allocation.inline.hpp"
#include "utilities/decoder.hpp"
#include "utilities/vmError.hpp"

#ifndef _WINDOWS
#if defined(__APPLE__)
  #include "decoder_machO.hpp"
#elif defined(AIX)
  #include "decoder_aix.hpp"
#else
  #include "decoder_elf.hpp"
#endif

AbstractDecoder*  Decoder::_shared_decoder = NULL;
AbstractDecoder*  Decoder::_error_handler_decoder = NULL;
NullDecoder       Decoder::_do_nothing_decoder;

AbstractDecoder* Decoder::get_shared_instance() {
  assert(shared_decoder_lock()->owned_by_self(), "Require DecoderLock to enter");

  if (_shared_decoder == NULL) {
    _shared_decoder = create_decoder();
  }
  return _shared_decoder;
}

AbstractDecoder* Decoder::get_error_handler_instance() {
  if (_error_handler_decoder == NULL) {
    _error_handler_decoder = create_decoder();
  }
  return _error_handler_decoder;
}


AbstractDecoder* Decoder::create_decoder() {
  AbstractDecoder* decoder;
#if defined (__APPLE__)
  decoder = new (std::nothrow)MachODecoder();
#elif defined(AIX)
  decoder = new (std::nothrow)AIXDecoder();
#else
  decoder = new (std::nothrow)ElfDecoder();
#endif

  if (decoder == NULL || decoder->has_error()) {
    if (decoder != NULL) {
      delete decoder;
    }
    decoder = &_do_nothing_decoder;
  }
  return decoder;
}

Mutex* Decoder::shared_decoder_lock() {
  assert(SharedDecoder_lock != NULL, "Just check");
  return SharedDecoder_lock;
}

bool Decoder::decode(address addr, char* buf, int buflen, int* offset, const char* modulepath, bool demangle) {
  if (VMError::is_error_reported_in_current_thread()) {
    return get_error_handler_instance()->decode(addr, buf, buflen, offset, modulepath, demangle);
  } else {
    MutexLocker locker(shared_decoder_lock(), Mutex::_no_safepoint_check_flag);
    return get_shared_instance()->decode(addr, buf, buflen, offset, modulepath, demangle);
  }
}

bool Decoder::decode(address addr, char* buf, int buflen, int* offset, const void* base) {
  if (VMError::is_error_reported_in_current_thread()) {
    return get_error_handler_instance()->decode(addr, buf, buflen, offset, base);
  } else {
    MutexLocker locker(shared_decoder_lock(), Mutex::_no_safepoint_check_flag);
    return get_shared_instance()->decode(addr, buf, buflen, offset, base);
  }
}

bool Decoder::demangle(const char* symbol, char* buf, int buflen) {
  if (VMError::is_error_reported_in_current_thread()) {
    return get_error_handler_instance()->demangle(symbol, buf, buflen);
  } else {
    MutexLocker locker(shared_decoder_lock(), Mutex::_no_safepoint_check_flag);
    return get_shared_instance()->demangle(symbol, buf, buflen);
  }
}

void Decoder::print_state_on(outputStream* st) {
}

bool Decoder::get_source_info(address pc, char* buf, size_t buflen, int* line) {
  return false;
}

#endif // !_WINDOWS

