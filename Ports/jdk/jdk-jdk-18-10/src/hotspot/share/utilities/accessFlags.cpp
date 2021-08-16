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
#include "oops/oop.inline.hpp"
#include "runtime/atomic.hpp"
#include "utilities/accessFlags.hpp"

void AccessFlags::atomic_set_bits(jint bits) {
  // Atomically update the flags with the bits given
  jint old_flags, new_flags, f;
  do {
    old_flags = _flags;
    new_flags = old_flags | bits;
    f = Atomic::cmpxchg(&_flags, old_flags, new_flags);
  } while(f != old_flags);
}

void AccessFlags::atomic_clear_bits(jint bits) {
  // Atomically update the flags with the bits given
  jint old_flags, new_flags, f;
  do {
    old_flags = _flags;
    new_flags = old_flags & ~bits;
    f = Atomic::cmpxchg(&_flags, old_flags, new_flags);
  } while(f != old_flags);
}


#if !defined(PRODUCT) || INCLUDE_JVMTI

void AccessFlags::print_on(outputStream* st) const {
  if (is_public      ()) st->print("public "      );
  if (is_private     ()) st->print("private "     );
  if (is_protected   ()) st->print("protected "   );
  if (is_static      ()) st->print("static "      );
  if (is_final       ()) st->print("final "       );
  if (is_synchronized()) st->print("synchronized ");
  if (is_volatile    ()) st->print("volatile "    );
  if (is_transient   ()) st->print("transient "   );
  if (is_native      ()) st->print("native "      );
  if (is_interface   ()) st->print("interface "   );
  if (is_abstract    ()) st->print("abstract "    );
  if (is_synthetic   ()) st->print("synthetic "   );
  if (is_old         ()) st->print("{old} "       );
  if (is_obsolete    ()) st->print("{obsolete} "  );
  if (on_stack       ()) st->print("{on_stack} "  );
}

#endif // !PRODUCT || INCLUDE_JVMTI

void accessFlags_init() {
  assert(sizeof(AccessFlags) == sizeof(jint), "just checking size of flags");
}
