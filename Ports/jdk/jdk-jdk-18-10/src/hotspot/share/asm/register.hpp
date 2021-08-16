/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_ASM_REGISTER_HPP
#define SHARE_ASM_REGISTER_HPP

#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

// Use AbstractRegister as shortcut
class AbstractRegisterImpl;
typedef AbstractRegisterImpl* AbstractRegister;


// The super class for platform specific registers. Instead of using value objects,
// registers are implemented as pointers. Subclassing is used so all registers can
// use the debugging suport below. No virtual functions are used for efficiency.
// They are canonicalized; i.e., registers are equal if their pointers are equal,
// and vice versa. A concrete implementation may just map the register onto 'this'.

class AbstractRegisterImpl {
 protected:
  int value() const                              { return (int)(intx)this; }
};

#define AS_REGISTER(type,name)         ((type)name##_##type##EnumValue)

#define CONSTANT_REGISTER_DECLARATION(type, name, value)        \
const type name = ((type)value);                                \
enum { name##_##type##EnumValue = (value) }

#define REGISTER_DECLARATION(type, name, value)                 \
const type name = ((type)value)

#define REGISTER_DEFINITION(type, name)

#include CPU_HEADER(register)

// Debugging support

inline void assert_different_registers(
  AbstractRegister a,
  AbstractRegister b
) {
  assert(
    a != b,
    "registers must be different: a=" INTPTR_FORMAT ", b=" INTPTR_FORMAT "", p2i(a), p2i(b)
  );
}


inline void assert_different_registers(
  AbstractRegister a,
  AbstractRegister b,
  AbstractRegister c
) {
  assert(
    a != b && a != c
           && b != c,
    "registers must be different: a=" INTPTR_FORMAT ", b=" INTPTR_FORMAT
    ", c=" INTPTR_FORMAT "",
    p2i(a), p2i(b), p2i(c)
  );
}


inline void assert_different_registers(
  AbstractRegister a,
  AbstractRegister b,
  AbstractRegister c,
  AbstractRegister d
) {
  assert(
    a != b && a != c && a != d
           && b != c && b != d
                     && c != d,
    "registers must be different: a=" INTPTR_FORMAT ", b=" INTPTR_FORMAT
    ", c=" INTPTR_FORMAT ", d=" INTPTR_FORMAT "",
    p2i(a), p2i(b), p2i(c), p2i(d)
  );
}


inline void assert_different_registers(
  AbstractRegister a,
  AbstractRegister b,
  AbstractRegister c,
  AbstractRegister d,
  AbstractRegister e
) {
  assert(
    a != b && a != c && a != d && a != e
           && b != c && b != d && b != e
                     && c != d && c != e
                               && d != e,
    "registers must be different: a=" INTPTR_FORMAT ", b=" INTPTR_FORMAT
    ", c=" INTPTR_FORMAT ", d=" INTPTR_FORMAT ", e=" INTPTR_FORMAT "",
    p2i(a), p2i(b), p2i(c), p2i(d), p2i(e)
  );
}


inline void assert_different_registers(
  AbstractRegister a,
  AbstractRegister b,
  AbstractRegister c,
  AbstractRegister d,
  AbstractRegister e,
  AbstractRegister f
) {
  assert(
    a != b && a != c && a != d && a != e && a != f
           && b != c && b != d && b != e && b != f
                     && c != d && c != e && c != f
                               && d != e && d != f
                                         && e != f,
    "registers must be different: a=" INTPTR_FORMAT ", b=" INTPTR_FORMAT
    ", c=" INTPTR_FORMAT ", d=" INTPTR_FORMAT ", e=" INTPTR_FORMAT
    ", f=" INTPTR_FORMAT "",
    p2i(a), p2i(b), p2i(c), p2i(d), p2i(e), p2i(f)
  );
}


inline void assert_different_registers(
  AbstractRegister a,
  AbstractRegister b,
  AbstractRegister c,
  AbstractRegister d,
  AbstractRegister e,
  AbstractRegister f,
  AbstractRegister g
) {
  assert(
    a != b && a != c && a != d && a != e && a != f && a != g
           && b != c && b != d && b != e && b != f && b != g
                     && c != d && c != e && c != f && c != g
                               && d != e && d != f && d != g
                                         && e != f && e != g
                                                   && f != g,
    "registers must be different: a=" INTPTR_FORMAT ", b=" INTPTR_FORMAT
    ", c=" INTPTR_FORMAT ", d=" INTPTR_FORMAT ", e=" INTPTR_FORMAT
    ", f=" INTPTR_FORMAT ", g=" INTPTR_FORMAT "",
    p2i(a), p2i(b), p2i(c), p2i(d), p2i(e), p2i(f), p2i(g)
  );
}


inline void assert_different_registers(
  AbstractRegister a,
  AbstractRegister b,
  AbstractRegister c,
  AbstractRegister d,
  AbstractRegister e,
  AbstractRegister f,
  AbstractRegister g,
  AbstractRegister h
) {
  assert(
    a != b && a != c && a != d && a != e && a != f && a != g && a != h
           && b != c && b != d && b != e && b != f && b != g && b != h
                     && c != d && c != e && c != f && c != g && c != h
                               && d != e && d != f && d != g && d != h
                                         && e != f && e != g && e != h
                                                   && f != g && f != h
                                                             && g != h,
    "registers must be different: a=" INTPTR_FORMAT ", b=" INTPTR_FORMAT
    ", c=" INTPTR_FORMAT ", d=" INTPTR_FORMAT ", e=" INTPTR_FORMAT
    ", f=" INTPTR_FORMAT ", g=" INTPTR_FORMAT ", h=" INTPTR_FORMAT "",
    p2i(a), p2i(b), p2i(c), p2i(d), p2i(e), p2i(f), p2i(g), p2i(h)
  );
}


inline void assert_different_registers(
  AbstractRegister a,
  AbstractRegister b,
  AbstractRegister c,
  AbstractRegister d,
  AbstractRegister e,
  AbstractRegister f,
  AbstractRegister g,
  AbstractRegister h,
  AbstractRegister i
) {
  assert(
    a != b && a != c && a != d && a != e && a != f && a != g && a != h && a != i
           && b != c && b != d && b != e && b != f && b != g && b != h && b != i
                     && c != d && c != e && c != f && c != g && c != h && c != i
                               && d != e && d != f && d != g && d != h && d != i
                                         && e != f && e != g && e != h && e != i
                                                   && f != g && f != h && f != i
                                                             && g != h && g != i
                                                                       && h != i,
    "registers must be different: a=" INTPTR_FORMAT ", b=" INTPTR_FORMAT
    ", c=" INTPTR_FORMAT ", d=" INTPTR_FORMAT ", e=" INTPTR_FORMAT
    ", f=" INTPTR_FORMAT ", g=" INTPTR_FORMAT ", h=" INTPTR_FORMAT
    ", i=" INTPTR_FORMAT "",
    p2i(a), p2i(b), p2i(c), p2i(d), p2i(e), p2i(f), p2i(g), p2i(h), p2i(i)
  );
}

inline void assert_different_registers(
  AbstractRegister a,
  AbstractRegister b,
  AbstractRegister c,
  AbstractRegister d,
  AbstractRegister e,
  AbstractRegister f,
  AbstractRegister g,
  AbstractRegister h,
  AbstractRegister i,
  AbstractRegister j
) {
  assert(
    a != b && a != c && a != d && a != e && a != f && a != g && a != h && a != i && a != j
           && b != c && b != d && b != e && b != f && b != g && b != h && b != i && b != j
                     && c != d && c != e && c != f && c != g && c != h && c != i && c != j
                               && d != e && d != f && d != g && d != h && d != i && d != j
                                         && e != f && e != g && e != h && e != i && e != j
                                                   && f != g && f != h && f != i && f != j
                                                             && g != h && g != i && g != j
                                                                       && h != i && h != j
                                                                                 && i != j,
    "registers must be different: a=" INTPTR_FORMAT ", b=" INTPTR_FORMAT
    ", c=" INTPTR_FORMAT ", d=" INTPTR_FORMAT ", e=" INTPTR_FORMAT
    ", f=" INTPTR_FORMAT ", g=" INTPTR_FORMAT ", h=" INTPTR_FORMAT
    ", i=" INTPTR_FORMAT ", j=" INTPTR_FORMAT "",
    p2i(a), p2i(b), p2i(c), p2i(d), p2i(e), p2i(f), p2i(g), p2i(h), p2i(i), p2i(j)
  );
}

inline void assert_different_registers(
  AbstractRegister a,
  AbstractRegister b,
  AbstractRegister c,
  AbstractRegister d,
  AbstractRegister e,
  AbstractRegister f,
  AbstractRegister g,
  AbstractRegister h,
  AbstractRegister i,
  AbstractRegister j,
  AbstractRegister k
) {
  assert(
    a != b && a != c && a != d && a != e && a != f && a != g && a != h && a != i && a != j && a !=k
           && b != c && b != d && b != e && b != f && b != g && b != h && b != i && b != j && b !=k
                     && c != d && c != e && c != f && c != g && c != h && c != i && c != j && c !=k
                               && d != e && d != f && d != g && d != h && d != i && d != j && d !=k
                                         && e != f && e != g && e != h && e != i && e != j && e !=k
                                                   && f != g && f != h && f != i && f != j && f !=k
                                                             && g != h && g != i && g != j && g !=k
                                                                       && h != i && h != j && h !=k
                                                                                 && i != j && i !=k
                                                                                           && j !=k,
    "registers must be different: a=" INTPTR_FORMAT ", b=" INTPTR_FORMAT
    ", c=" INTPTR_FORMAT ", d=" INTPTR_FORMAT ", e=" INTPTR_FORMAT
    ", f=" INTPTR_FORMAT ", g=" INTPTR_FORMAT ", h=" INTPTR_FORMAT
    ", i=" INTPTR_FORMAT ", j=" INTPTR_FORMAT ", k=" INTPTR_FORMAT "",
    p2i(a), p2i(b), p2i(c), p2i(d), p2i(e), p2i(f), p2i(g), p2i(h), p2i(i), p2i(j), p2i(k)
  );
}

inline void assert_different_registers(
  AbstractRegister a,
  AbstractRegister b,
  AbstractRegister c,
  AbstractRegister d,
  AbstractRegister e,
  AbstractRegister f,
  AbstractRegister g,
  AbstractRegister h,
  AbstractRegister i,
  AbstractRegister j,
  AbstractRegister k,
  AbstractRegister l
) {
  assert(
    a != b && a != c && a != d && a != e && a != f && a != g && a != h && a != i && a != j && a !=k && a !=l
           && b != c && b != d && b != e && b != f && b != g && b != h && b != i && b != j && b !=k && b !=l
                     && c != d && c != e && c != f && c != g && c != h && c != i && c != j && c !=k && c !=l
                               && d != e && d != f && d != g && d != h && d != i && d != j && d !=k && d !=l
                                         && e != f && e != g && e != h && e != i && e != j && e !=k && e !=l
                                                   && f != g && f != h && f != i && f != j && f !=k && f !=l
                                                             && g != h && g != i && g != j && g !=k && g !=l
                                                                       && h != i && h != j && h !=k && h !=l
                                                                                 && i != j && i !=k && i !=l
                                                                                           && j !=k && j !=l
                                                                                                    && k !=l,
    "registers must be different: a=" INTPTR_FORMAT ", b=" INTPTR_FORMAT
    ", c=" INTPTR_FORMAT ", d=" INTPTR_FORMAT ", e=" INTPTR_FORMAT
    ", f=" INTPTR_FORMAT ", g=" INTPTR_FORMAT ", h=" INTPTR_FORMAT
    ", i=" INTPTR_FORMAT ", j=" INTPTR_FORMAT ", k=" INTPTR_FORMAT
    ", l=" INTPTR_FORMAT "",
    p2i(a), p2i(b), p2i(c), p2i(d), p2i(e), p2i(f), p2i(g), p2i(h), p2i(i), p2i(j), p2i(k), p2i(l)
  );
}

#endif // SHARE_ASM_REGISTER_HPP
