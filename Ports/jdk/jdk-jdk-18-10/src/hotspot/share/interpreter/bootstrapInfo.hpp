/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_INTERPRETER_BOOTSTRAPINFO_HPP
#define SHARE_INTERPRETER_BOOTSTRAPINFO_HPP

#include "oops/constantPool.hpp"
#include "oops/instanceKlass.hpp"

// BootstrapInfo provides condensed information from the constant pool
// necessary to invoke a bootstrap method.
class BootstrapInfo : public StackObj {
  constantPoolHandle _pool;     // constant pool containing the bootstrap specifier
  const int   _bss_index;       // index of bootstrap specifier in CP (condy or indy)
  const int   _indy_index;      // internal index of indy call site, or -1 if a condy call
  const int   _argc;            // number of static arguments
  Symbol*     _name;            // extracted from JVM_CONSTANT_NameAndType
  Symbol*     _signature;

  // pre-bootstrap resolution state:
  Handle      _bsm;             // resolved bootstrap method
  Handle      _name_arg;        // resolved String
  Handle      _type_arg;        // resolved Class or MethodType
  Handle      _arg_values;      // array of static arguments; null implies either
                                // uresolved or zero static arguments are specified

  // post-bootstrap resolution state:
  bool        _is_resolved;       // set true when any of the next fields are set
  Handle      _resolved_value;    // bind this as condy constant
  methodHandle _resolved_method;  // bind this as indy behavior
  Handle      _resolved_appendix; // extra opaque static argument for _resolved_method

 public:
  BootstrapInfo(const constantPoolHandle& pool, int bss_index, int indy_index = -1);

  // accessors
  const constantPoolHandle& pool() const{ return _pool; }
  int bss_index() const                 { return _bss_index; }
  int indy_index() const                { return _indy_index; }
  int argc() const                      { return _argc; }
  bool is_method_call() const           { return (_indy_index != -1); }
  Symbol* name() const                  { return _name; }
  Symbol* signature() const             { return _signature; }

  // accessors to lazy state
  Handle bsm() const                    { return _bsm; }
  Handle name_arg() const               { return _name_arg; }
  Handle type_arg() const               { return _type_arg; }
  Handle arg_values() const             { return _arg_values; }
  bool is_resolved() const              { return _is_resolved; }
  Handle resolved_value() const         { assert(!is_method_call(), ""); return _resolved_value; }
  methodHandle resolved_method() const  { assert(is_method_call(), "");  return _resolved_method; }
  Handle resolved_appendix() const      { assert(is_method_call(), "");  return _resolved_appendix; }

  // derived accessors
  InstanceKlass* caller() const         { return _pool->pool_holder(); }
  oop caller_mirror() const             { return caller()->java_mirror(); }
  int decode_indy_index() const         { return ConstantPool::decode_invokedynamic_index(_indy_index); }
  int bsms_attr_index() const           { return _pool->bootstrap_methods_attribute_index(_bss_index); }
  int bsm_index() const                 { return _pool->bootstrap_method_ref_index_at(_bss_index); }
  //int argc() is eagerly cached in _argc
  int arg_index(int i) const            { return _pool->bootstrap_argument_index_at(_bss_index, i); }

  // CP cache entry for call site (indy only)
  ConstantPoolCacheEntry* invokedynamic_cp_cache_entry() const {
    assert(is_method_call(), "");
    return _pool->invokedynamic_cp_cache_entry_at(_indy_index);
  }

  // If there is evidence this call site was already linked, set the
  // existing linkage data into result, or throw previous exception.
  // Return true if either action is taken, else false.
  bool resolve_previously_linked_invokedynamic(CallInfo& result, TRAPS);
  bool save_and_throw_indy_exc(TRAPS);
  void resolve_newly_linked_invokedynamic(CallInfo& result, TRAPS);

  // pre-bootstrap resolution actions:
  Handle resolve_bsm(TRAPS); // lazily compute _bsm and return it
  void resolve_bss_name_and_type(TRAPS); // lazily compute _name/_type
  void resolve_args(TRAPS);  // compute arguments

  // setters for post-bootstrap results:
  void set_resolved_value(Handle value) {
    assert(!is_resolved() && !is_method_call(), "");
    _is_resolved = true;
    _resolved_value = value;
  }
  void set_resolved_method(methodHandle method, Handle appendix) {
    assert(!is_resolved() && is_method_call(), "");
    _is_resolved = true;
    _resolved_method = method;
    _resolved_appendix = appendix;
  }

  void print() { print_msg_on(tty); }
  void print_msg_on(outputStream* st, const char* msg = NULL);
};

#endif // SHARE_INTERPRETER_BOOTSTRAPINFO_HPP
