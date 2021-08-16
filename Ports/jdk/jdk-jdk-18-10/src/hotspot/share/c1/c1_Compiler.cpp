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

#include "precompiled.hpp"
#include "c1/c1_Compilation.hpp"
#include "c1/c1_Compiler.hpp"
#include "c1/c1_FrameMap.hpp"
#include "c1/c1_GraphBuilder.hpp"
#include "c1/c1_LinearScan.hpp"
#include "c1/c1_MacroAssembler.hpp"
#include "c1/c1_Runtime1.hpp"
#include "c1/c1_ValueType.hpp"
#include "compiler/compileBroker.hpp"
#include "interpreter/linkResolver.hpp"
#include "jfr/support/jfrIntrinsics.hpp"
#include "memory/allocation.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/vm_version.hpp"
#include "utilities/bitMap.inline.hpp"
#include "utilities/macros.hpp"


Compiler::Compiler() : AbstractCompiler(compiler_c1) {
}

void Compiler::init_c1_runtime() {
  BufferBlob* buffer_blob = CompilerThread::current()->get_buffer_blob();
  Arena* arena = new (mtCompiler) Arena(mtCompiler);
  Runtime1::initialize(buffer_blob);
  FrameMap::initialize();
  // initialize data structures
  ValueType::initialize(arena);
  GraphBuilder::initialize();
  // note: to use more than one instance of LinearScan at a time this function call has to
  //       be moved somewhere outside of this constructor:
  Interval::initialize(arena);
}


void Compiler::initialize() {
  // Buffer blob must be allocated per C1 compiler thread at startup
  BufferBlob* buffer_blob = init_buffer_blob();

  if (should_perform_init()) {
    if (buffer_blob == NULL) {
      // When we come here we are in state 'initializing'; entire C1 compilation
      // can be shut down.
      set_state(failed);
    } else {
      init_c1_runtime();
      set_state(initialized);
    }
  }
}

int Compiler::code_buffer_size() {
  return Compilation::desired_max_code_buffer_size() + Compilation::desired_max_constant_size();
}

BufferBlob* Compiler::init_buffer_blob() {
  // Allocate buffer blob once at startup since allocation for each
  // compilation seems to be too expensive (at least on Intel win32).
  assert (CompilerThread::current()->get_buffer_blob() == NULL, "Should initialize only once");

  // setup CodeBuffer.  Preallocate a BufferBlob of size
  // NMethodSizeLimit plus some extra space for constants.
  BufferBlob* buffer_blob = BufferBlob::create("C1 temporary CodeBuffer", code_buffer_size());
  if (buffer_blob != NULL) {
    CompilerThread::current()->set_buffer_blob(buffer_blob);
  }

  return buffer_blob;
}

bool Compiler::is_intrinsic_supported(const methodHandle& method) {
  vmIntrinsics::ID id = method->intrinsic_id();
  assert(id != vmIntrinsics::_none, "must be a VM intrinsic");

  if (method->is_synchronized()) {
    // C1 does not support intrinsification of synchronized methods.
    return false;
  }

  switch (id) {
  case vmIntrinsics::_compareAndSetLong:
    if (!VM_Version::supports_cx8()) return false;
    break;
  case vmIntrinsics::_getAndAddInt:
    if (!VM_Version::supports_atomic_getadd4()) return false;
    break;
  case vmIntrinsics::_getAndAddLong:
    if (!VM_Version::supports_atomic_getadd8()) return false;
    break;
  case vmIntrinsics::_getAndSetInt:
    if (!VM_Version::supports_atomic_getset4()) return false;
    break;
  case vmIntrinsics::_getAndSetLong:
    if (!VM_Version::supports_atomic_getset8()) return false;
    break;
  case vmIntrinsics::_getAndSetReference:
#ifdef _LP64
    if (!UseCompressedOops && !VM_Version::supports_atomic_getset8()) return false;
    if (UseCompressedOops && !VM_Version::supports_atomic_getset4()) return false;
#else
    if (!VM_Version::supports_atomic_getset4()) return false;
#endif
    break;
  case vmIntrinsics::_onSpinWait:
    if (!VM_Version::supports_on_spin_wait()) return false;
    break;
  case vmIntrinsics::_arraycopy:
  case vmIntrinsics::_currentTimeMillis:
  case vmIntrinsics::_nanoTime:
  case vmIntrinsics::_Reference_get:
    // Use the intrinsic version of Reference.get() so that the value in
    // the referent field can be registered by the G1 pre-barrier code.
    // Also to prevent commoning reads from this field across safepoint
    // since GC can change its value.
  case vmIntrinsics::_loadFence:
  case vmIntrinsics::_storeFence:
  case vmIntrinsics::_fullFence:
  case vmIntrinsics::_floatToRawIntBits:
  case vmIntrinsics::_intBitsToFloat:
  case vmIntrinsics::_doubleToRawLongBits:
  case vmIntrinsics::_longBitsToDouble:
  case vmIntrinsics::_getClass:
  case vmIntrinsics::_isInstance:
  case vmIntrinsics::_isPrimitive:
  case vmIntrinsics::_getModifiers:
  case vmIntrinsics::_currentThread:
  case vmIntrinsics::_dabs:
  case vmIntrinsics::_dsqrt:
  case vmIntrinsics::_dsin:
  case vmIntrinsics::_dcos:
  case vmIntrinsics::_dtan:
  case vmIntrinsics::_dlog:
  case vmIntrinsics::_dlog10:
  case vmIntrinsics::_dexp:
  case vmIntrinsics::_dpow:
  case vmIntrinsics::_fmaD:
  case vmIntrinsics::_fmaF:
  case vmIntrinsics::_getReference:
  case vmIntrinsics::_getBoolean:
  case vmIntrinsics::_getByte:
  case vmIntrinsics::_getShort:
  case vmIntrinsics::_getChar:
  case vmIntrinsics::_getInt:
  case vmIntrinsics::_getLong:
  case vmIntrinsics::_getFloat:
  case vmIntrinsics::_getDouble:
  case vmIntrinsics::_putReference:
  case vmIntrinsics::_putBoolean:
  case vmIntrinsics::_putByte:
  case vmIntrinsics::_putShort:
  case vmIntrinsics::_putChar:
  case vmIntrinsics::_putInt:
  case vmIntrinsics::_putLong:
  case vmIntrinsics::_putFloat:
  case vmIntrinsics::_putDouble:
  case vmIntrinsics::_getReferenceVolatile:
  case vmIntrinsics::_getBooleanVolatile:
  case vmIntrinsics::_getByteVolatile:
  case vmIntrinsics::_getShortVolatile:
  case vmIntrinsics::_getCharVolatile:
  case vmIntrinsics::_getIntVolatile:
  case vmIntrinsics::_getLongVolatile:
  case vmIntrinsics::_getFloatVolatile:
  case vmIntrinsics::_getDoubleVolatile:
  case vmIntrinsics::_putReferenceVolatile:
  case vmIntrinsics::_putBooleanVolatile:
  case vmIntrinsics::_putByteVolatile:
  case vmIntrinsics::_putShortVolatile:
  case vmIntrinsics::_putCharVolatile:
  case vmIntrinsics::_putIntVolatile:
  case vmIntrinsics::_putLongVolatile:
  case vmIntrinsics::_putFloatVolatile:
  case vmIntrinsics::_putDoubleVolatile:
  case vmIntrinsics::_getShortUnaligned:
  case vmIntrinsics::_getCharUnaligned:
  case vmIntrinsics::_getIntUnaligned:
  case vmIntrinsics::_getLongUnaligned:
  case vmIntrinsics::_putShortUnaligned:
  case vmIntrinsics::_putCharUnaligned:
  case vmIntrinsics::_putIntUnaligned:
  case vmIntrinsics::_putLongUnaligned:
  case vmIntrinsics::_Preconditions_checkIndex:
  case vmIntrinsics::_Preconditions_checkLongIndex:
  case vmIntrinsics::_updateCRC32:
  case vmIntrinsics::_updateBytesCRC32:
  case vmIntrinsics::_updateByteBufferCRC32:
#if defined(S390) || defined(PPC64) || defined(AARCH64)
  case vmIntrinsics::_updateBytesCRC32C:
  case vmIntrinsics::_updateDirectByteBufferCRC32C:
#endif
  case vmIntrinsics::_vectorizedMismatch:
  case vmIntrinsics::_compareAndSetInt:
  case vmIntrinsics::_compareAndSetReference:
  case vmIntrinsics::_getCharStringU:
  case vmIntrinsics::_putCharStringU:
#ifdef JFR_HAVE_INTRINSICS
  case vmIntrinsics::_counterTime:
  case vmIntrinsics::_getEventWriter:
#endif
  case vmIntrinsics::_getObjectSize:
    break;
  case vmIntrinsics::_blackhole:
    break;
  default:
    return false; // Intrinsics not on the previous list are not available.
  }

  return true;
}

void Compiler::compile_method(ciEnv* env, ciMethod* method, int entry_bci, bool install_code, DirectiveSet* directive) {
  BufferBlob* buffer_blob = CompilerThread::current()->get_buffer_blob();
  assert(buffer_blob != NULL, "Must exist");
  // invoke compilation
  {
    // We are nested here because we need for the destructor
    // of Compilation to occur before we release the any
    // competing compiler thread
    ResourceMark rm;
    Compilation c(this, env, method, entry_bci, buffer_blob, install_code, directive);
  }
}


void Compiler::print_timers() {
  Compilation::print_timers();
}
