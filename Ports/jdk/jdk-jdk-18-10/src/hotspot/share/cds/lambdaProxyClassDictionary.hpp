/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARED_CDS_LAMBDAPROXYCLASSINFO_HPP
#define SHARED_CDS_LAMBDAPROXYCLASSINFO_HPP
#include "cds/metaspaceShared.hpp"
#include "classfile/javaClasses.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/resourceHash.hpp"

class InstanceKlass;
class Method;
class Symbol;

class LambdaProxyClassKey {
  InstanceKlass* _caller_ik;
  Symbol*        _invoked_name;
  Symbol*        _invoked_type;
  Symbol*        _method_type;
  Method*        _member_method;
  Symbol*        _instantiated_method_type;

public:
  LambdaProxyClassKey(InstanceKlass* caller_ik,
                      Symbol*        invoked_name,
                      Symbol*        invoked_type,
                      Symbol*        method_type,
                      Method*        member_method,
                      Symbol*        instantiated_method_type) :
    _caller_ik(caller_ik),
    _invoked_name(invoked_name),
    _invoked_type(invoked_type),
    _method_type(method_type),
    _member_method(member_method),
    _instantiated_method_type(instantiated_method_type) {}

  void metaspace_pointers_do(MetaspaceClosure* it) {
    it->push(&_caller_ik);
    it->push(&_invoked_name);
    it->push(&_invoked_type);
    it->push(&_method_type);
    it->push(&_member_method);
    it->push(&_instantiated_method_type);
  }

  bool equals(LambdaProxyClassKey const& other) const {
    return _caller_ik == other._caller_ik &&
           _invoked_name == other._invoked_name &&
           _invoked_type == other._invoked_type &&
           _method_type == other._method_type &&
           _member_method == other._member_method &&
           _instantiated_method_type == other._instantiated_method_type;
  }

  void mark_pointers();
  unsigned int hash() const;

  static unsigned int dumptime_hash(Symbol* sym)  {
    if (sym == NULL) {
      // _invoked_name maybe NULL
      return 0;
    }
    return java_lang_String::hash_code((const jbyte*)sym->bytes(), sym->utf8_length());
  }

  unsigned int dumptime_hash() const {
    return dumptime_hash(_caller_ik->name()) +
           dumptime_hash(_invoked_name) +
           dumptime_hash(_invoked_type) +
           dumptime_hash(_method_type) +
           dumptime_hash(_instantiated_method_type);
  }

  static inline unsigned int DUMPTIME_HASH(LambdaProxyClassKey const& key) {
    return (key.dumptime_hash());
  }

  static inline bool DUMPTIME_EQUALS(
      LambdaProxyClassKey const& k1, LambdaProxyClassKey const& k2) {
    return (k1.equals(k2));
  }

  InstanceKlass* caller_ik() const { return _caller_ik; }
};

class DumpTimeLambdaProxyClassInfo {
public:
  GrowableArray<InstanceKlass*>* _proxy_klasses;
  DumpTimeLambdaProxyClassInfo() : _proxy_klasses(NULL) {}
  void add_proxy_klass(InstanceKlass* proxy_klass) {
    if (_proxy_klasses == NULL) {
      _proxy_klasses = new (ResourceObj::C_HEAP, mtClassShared) GrowableArray<InstanceKlass*>(5, mtClassShared);
    }
    assert(_proxy_klasses != NULL, "sanity");
    _proxy_klasses->append(proxy_klass);
  }

  void metaspace_pointers_do(MetaspaceClosure* it) {
    for (int i=0; i<_proxy_klasses->length(); i++) {
      it->push(_proxy_klasses->adr_at(i));
    }
  }
  DumpTimeLambdaProxyClassInfo clone(); // copy ctor will cause implicitly-declared
};

class RunTimeLambdaProxyClassInfo {
  LambdaProxyClassKey _key;
  InstanceKlass* _proxy_klass_head;
public:
  RunTimeLambdaProxyClassInfo(LambdaProxyClassKey key, InstanceKlass* proxy_klass_head) :
    _key(key), _proxy_klass_head(proxy_klass_head) {}

  InstanceKlass* proxy_klass_head() const { return _proxy_klass_head; }

  // Used by LambdaProxyClassDictionary to implement OffsetCompactHashtable::EQUALS
  static inline bool EQUALS(
       const RunTimeLambdaProxyClassInfo* value, LambdaProxyClassKey* key, int len_unused) {
    return (value->_key.equals(*key));
  }
  void init(LambdaProxyClassKey& key, DumpTimeLambdaProxyClassInfo& info) {
    _key = key;
    _key.mark_pointers();
    _proxy_klass_head = info._proxy_klasses->at(0);
    ArchivePtrMarker::mark_pointer(&_proxy_klass_head);
  }

  unsigned int hash() const {
    return _key.hash();
  }
  LambdaProxyClassKey key() const {
    return _key;
  }
};

class DumpTimeLambdaProxyClassDictionary
  : public ResourceHashtable<LambdaProxyClassKey,
                             DumpTimeLambdaProxyClassInfo,
                             137, // prime number
                             ResourceObj::C_HEAP,
                             mtClassShared,
                             LambdaProxyClassKey::DUMPTIME_HASH,
                             LambdaProxyClassKey::DUMPTIME_EQUALS> {
public:
  DumpTimeLambdaProxyClassDictionary() : _count(0) {}
  int _count;
};

class LambdaProxyClassDictionary : public OffsetCompactHashtable<
  LambdaProxyClassKey*,
  const RunTimeLambdaProxyClassInfo*,
  RunTimeLambdaProxyClassInfo::EQUALS> {};

#endif // SHARED_CDS_LAMBDAPROXYCLASSINFO_HPP
