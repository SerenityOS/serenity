/*
 * Copyright (c) 1997, 2010, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/macroAssembler.hpp"
#include "runtime/icache.hpp"

#define __ _masm->

void ICacheStubGenerator::generate_icache_flush(ICache::flush_icache_stub_t* flush_icache_stub) {
  StubCodeMark mark(this, "ICache", "flush_icache_stub");

  address start = __ pc();
#ifdef AMD64

  const Register addr  = c_rarg0;
  const Register lines = c_rarg1;
  const Register magic = c_rarg2;

  Label flush_line, done;

  __ testl(lines, lines);
  __ jcc(Assembler::zero, done);

  // Force ordering wrt cflush.
  // Other fence and sync instructions won't do the job.
  __ mfence();

  __ bind(flush_line);
  __ clflush(Address(addr, 0));
  __ addptr(addr, ICache::line_size);
  __ decrementl(lines);
  __ jcc(Assembler::notZero, flush_line);

  __ mfence();

  __ bind(done);

#else
  const Address magic(rsp, 3*wordSize);
  __ lock(); __ addl(Address(rsp, 0), 0);
#endif // AMD64
  __ movptr(rax, magic); // Handshake with caller to make sure it happened!
  __ ret(0);

  // Must be set here so StubCodeMark destructor can call the flush stub.
  *flush_icache_stub = (ICache::flush_icache_stub_t)start;
}

#undef __
