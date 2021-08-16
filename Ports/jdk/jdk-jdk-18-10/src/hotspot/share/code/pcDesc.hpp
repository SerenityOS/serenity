/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CODE_PCDESC_HPP
#define SHARE_CODE_PCDESC_HPP


// PcDescs map a physical PC (given as offset from start of nmethod) to
// the corresponding source scope and byte code index.

class CompiledMethod;

class PcDesc {
  friend class VMStructs;
 private:
  int _pc_offset;           // offset from start of nmethod
  int _scope_decode_offset; // offset for scope in nmethod
  int _obj_decode_offset;

  enum {
    PCDESC_reexecute                 = 1 << 0,
    PCDESC_is_method_handle_invoke   = 1 << 1,
    PCDESC_return_oop                = 1 << 2,
    PCDESC_rethrow_exception         = 1 << 3,
    PCDESC_has_ea_local_in_scope     = 1 << 4,
    PCDESC_arg_escape                = 1 << 5,
    PCDESC_is_optimized_linkToNative = 1 << 6
  };

  int _flags;

  void set_flag(int mask, bool z) {
    _flags = z ? (_flags | mask) : (_flags & ~mask);
  }

 public:
  int pc_offset() const           { return _pc_offset;   }
  int scope_decode_offset() const { return _scope_decode_offset; }
  int obj_decode_offset() const   { return _obj_decode_offset; }

  void set_pc_offset(int x)           { _pc_offset           = x; }
  void set_scope_decode_offset(int x) { _scope_decode_offset = x; }
  void set_obj_decode_offset(int x)   { _obj_decode_offset   = x; }

  // Constructor (only used for static in nmethod.cpp)
  // Also used by ScopeDesc::sender()]
  PcDesc(int pc_offset, int scope_decode_offset, int obj_decode_offset);

  enum {
    // upper and lower exclusive limits real offsets:
    lower_offset_limit = -1,
    upper_offset_limit = (unsigned int)-1 >> 1
  };

  // Flags
  bool     rethrow_exception()              const { return (_flags & PCDESC_rethrow_exception) != 0; }
  void set_rethrow_exception(bool z)              { set_flag(PCDESC_rethrow_exception, z); }
  bool     should_reexecute()              const { return (_flags & PCDESC_reexecute) != 0; }
  void set_should_reexecute(bool z)              { set_flag(PCDESC_reexecute, z); }

  // Does pd refer to the same information as pd?
  bool is_same_info(const PcDesc* pd) {
    return _scope_decode_offset == pd->_scope_decode_offset &&
      _obj_decode_offset == pd->_obj_decode_offset &&
      _flags == pd->_flags;
  }

  bool     is_method_handle_invoke()       const { return (_flags & PCDESC_is_method_handle_invoke) != 0;     }
  void set_is_method_handle_invoke(bool z)       { set_flag(PCDESC_is_method_handle_invoke, z); }

  bool     is_optimized_linkToNative()     const { return (_flags & PCDESC_is_optimized_linkToNative) != 0;     }
  void set_is_optimized_linkToNative(bool z)     { set_flag(PCDESC_is_optimized_linkToNative, z); }

  bool     return_oop()                    const { return (_flags & PCDESC_return_oop) != 0;     }
  void set_return_oop(bool z)                    { set_flag(PCDESC_return_oop, z); }

  // Indicates if there are objects in scope that, based on escape analysis, are local to the
  // compiled method or local to the current thread, i.e. NoEscape or ArgEscape
  bool     has_ea_local_in_scope()         const { return (_flags & PCDESC_has_ea_local_in_scope) != 0; }
  void set_has_ea_local_in_scope(bool z)         { set_flag(PCDESC_has_ea_local_in_scope, z); }

  // Indicates if this pc descriptor is at a call site where objects that do not escape the
  // current thread are passed as arguments.
  bool     arg_escape()                    const { return (_flags & PCDESC_arg_escape) != 0; }
  void set_arg_escape(bool z)                    { set_flag(PCDESC_arg_escape, z); }

  // Returns the real pc
  address real_pc(const CompiledMethod* code) const;

  void print(CompiledMethod* code) { print_on(tty, code); }
  void print_on(outputStream* st, CompiledMethod* code);
  bool verify(CompiledMethod* code);
};

#endif // SHARE_CODE_PCDESC_HPP
