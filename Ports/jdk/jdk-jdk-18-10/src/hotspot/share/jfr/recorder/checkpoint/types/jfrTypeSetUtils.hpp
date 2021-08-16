/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_RECORDER_CHECKPOINT_TYPES_JFRTYPESETUTILS_HPP
#define SHARE_JFR_RECORDER_CHECKPOINT_TYPES_JFRTYPESETUTILS_HPP

#include "jfr/recorder/checkpoint/types/traceid/jfrTraceId.inline.hpp"
#include "jfr/utilities/jfrAllocation.hpp"
#include "jfr/utilities/jfrHashtable.hpp"
#include "oops/klass.hpp"
#include "oops/method.hpp"

template <typename T>
class GrowableArray;

// Composite callback/functor building block
template <typename T, typename Func1, typename Func2>
class CompositeFunctor {
 private:
  Func1* _f;
  Func2* _g;
 public:
  CompositeFunctor(Func1* f, Func2* g) : _f(f), _g(g) {
    assert(f != NULL, "invariant");
    assert(g != NULL, "invariant");
  }
  bool operator()(T const& value) {
    return (*_f)(value) && (*_g)(value);
  }
};

class JfrArtifactClosure {
 public:
  virtual void do_artifact(const void* artifact) = 0;
};

template <typename T, typename Callback>
class JfrArtifactCallbackHost : public JfrArtifactClosure {
 private:
  JfrArtifactClosure** _subsystem_callback_loc;
  Callback* _callback;
 public:
  JfrArtifactCallbackHost(JfrArtifactClosure** subsystem_callback_loc, Callback* callback) :
          _subsystem_callback_loc(subsystem_callback_loc), _callback(callback) {
    assert(*_subsystem_callback_loc == NULL, "Subsystem callback should not be set yet");
    *_subsystem_callback_loc = this;
  }
  ~JfrArtifactCallbackHost() {
    *_subsystem_callback_loc = NULL;
  }
  void do_artifact(const void* artifact) {
    (*_callback)(reinterpret_cast<T const&>(artifact));
  }
};

template <typename FieldSelector, typename Letter>
class KlassToFieldEnvelope {
  Letter* _letter;
 public:
  KlassToFieldEnvelope(Letter* letter) : _letter(letter) {}
  bool operator()(const Klass* klass) {
    typename FieldSelector::TypePtr t = FieldSelector::select(klass);
    return t != NULL ? (*_letter)(t) : true;
  }
};

template <typename T>
class ClearArtifact {
 public:
  bool operator()(T const& value) {
    CLEAR_SERIALIZED(value);
    assert(IS_NOT_SERIALIZED(value), "invariant");
    SET_PREVIOUS_EPOCH_CLEARED_BIT(value);
    CLEAR_PREVIOUS_EPOCH_METHOD_AND_CLASS(value);
    return true;
  }
};

template <>
class ClearArtifact<const Method*> {
 public:
  bool operator()(const Method* method) {
    assert(METHOD_FLAG_USED_PREVIOUS_EPOCH(method), "invariant");
    CLEAR_SERIALIZED_METHOD(method);
    assert(METHOD_NOT_SERIALIZED(method), "invariant");
    SET_PREVIOUS_EPOCH_METHOD_CLEARED_BIT(method);
    CLEAR_PREVIOUS_EPOCH_METHOD_FLAG(method);
    return true;
  }
};

template <typename T>
class SerializePredicate {
  bool _class_unload;
 public:
  SerializePredicate(bool class_unload) : _class_unload(class_unload) {}
  bool operator()(T const& value) {
    assert(value != NULL, "invariant");
    return _class_unload ? true : IS_NOT_SERIALIZED(value);
  }
};

template <>
class SerializePredicate<const Method*> {
  bool _class_unload;
 public:
  SerializePredicate(bool class_unload) : _class_unload(class_unload) {}
  bool operator()(const Method* method) {
    assert(method != NULL, "invariant");
    return _class_unload ? true : METHOD_NOT_SERIALIZED(method);
  }
};

template <typename T, bool leakp>
class SymbolPredicate {
  bool _class_unload;
 public:
  SymbolPredicate(bool class_unload) : _class_unload(class_unload) {}
  bool operator()(T const& value) {
    assert(value != NULL, "invariant");
    if (_class_unload) {
      return leakp ? value->is_leakp() : value->is_unloading();
    }
    return leakp ? value->is_leakp() : !value->is_serialized();
  }
};

template <bool leakp>
class MethodUsedPredicate {
  bool _current_epoch;
public:
  MethodUsedPredicate(bool current_epoch) : _current_epoch(current_epoch) {}
  bool operator()(const Klass* klass) {
    if (_current_epoch) {
      return leakp ? IS_LEAKP(klass) : METHOD_USED_THIS_EPOCH(klass);
    }
    return  leakp ? IS_LEAKP(klass) : METHOD_USED_PREVIOUS_EPOCH(klass);
  }
};

template <bool leakp>
class MethodFlagPredicate {
  bool _current_epoch;
 public:
  MethodFlagPredicate(bool current_epoch) : _current_epoch(current_epoch) {}
  bool operator()(const Method* method) {
    if (_current_epoch) {
      return leakp ? IS_METHOD_LEAKP_USED(method) : METHOD_FLAG_USED_THIS_EPOCH(method);
    }
    return leakp ? IS_METHOD_LEAKP_USED(method) : METHOD_FLAG_USED_PREVIOUS_EPOCH(method);
  }
};

template <typename T>
class LeakPredicate {
 public:
  LeakPredicate(bool class_unload) {}
  bool operator()(T const& value) {
    return IS_LEAKP(value);
  }
};

template <>
class LeakPredicate<const Method*> {
 public:
  LeakPredicate(bool class_unload) {}
  bool operator()(const Method* method) {
    assert(method != NULL, "invariant");
    return IS_METHOD_LEAKP_USED(method);
  }
};

template <typename T, typename IdType>
class ListEntry : public JfrHashtableEntry<T, IdType> {
 public:
  ListEntry(uintptr_t hash, const T& data) : JfrHashtableEntry<T, IdType>(hash, data),
    _list_next(NULL), _serialized(false), _unloading(false), _leakp(false) {}
  const ListEntry<T, IdType>* list_next() const { return _list_next; }
  void reset() const {
    _list_next = NULL; _serialized = false; _unloading = false; _leakp = false;
  }
  void set_list_next(const ListEntry<T, IdType>* next) const { _list_next = next; }
  bool is_serialized() const { return _serialized; }
  void set_serialized() const { _serialized = true; }
  bool is_unloading() const { return _unloading; }
  void set_unloading() const { _unloading = true; }
  bool is_leakp() const { return _leakp; }
  void set_leakp() const { _leakp = true; }
 private:
  mutable const ListEntry<T, IdType>* _list_next;
  mutable bool _serialized;
  mutable bool _unloading;
  mutable bool _leakp;
};

class JfrSymbolId : public JfrCHeapObj {
  template <typename, typename, template<typename, typename> class, typename, size_t>
  friend class HashTableHost;
  typedef HashTableHost<const Symbol*, traceid, ListEntry, JfrSymbolId> SymbolTable;
  typedef HashTableHost<const char*, traceid, ListEntry, JfrSymbolId> CStringTable;
  friend class JfrArtifactSet;
 public:
  typedef SymbolTable::HashEntry SymbolEntry;
  typedef CStringTable::HashEntry CStringEntry;
 private:
  SymbolTable* _sym_table;
  CStringTable* _cstring_table;
  const SymbolEntry* _sym_list;
  const CStringEntry* _cstring_list;
  const Symbol* _sym_query;
  const char* _cstring_query;
  traceid _symbol_id_counter;
  bool _class_unload;

  // hashtable(s) callbacks
  void on_link(const SymbolEntry* entry);
  bool on_equals(uintptr_t hash, const SymbolEntry* entry);
  void on_unlink(const SymbolEntry* entry);
  void on_link(const CStringEntry* entry);
  bool on_equals(uintptr_t hash, const CStringEntry* entry);
  void on_unlink(const CStringEntry* entry);

  template <typename Functor, typename T>
  void iterate(Functor& functor, const T* list) {
    const T* symbol = list;
    while (symbol != NULL) {
      const T* next = symbol->list_next();
      functor(symbol);
      symbol = next;
    }
  }

  traceid mark_hidden_klass_name(const InstanceKlass* k, bool leakp);
  bool is_hidden_klass(const Klass* k);
  uintptr_t hidden_klass_name_hash(const InstanceKlass* ik);

 public:
  JfrSymbolId();
  ~JfrSymbolId();

  void clear();
  void set_class_unload(bool class_unload);

  traceid mark(uintptr_t hash, const Symbol* sym, bool leakp);
  traceid mark(const Klass* k, bool leakp);
  traceid mark(const Symbol* symbol, bool leakp);
  traceid mark(uintptr_t hash, const char* str, bool leakp);
  traceid bootstrap_name(bool leakp);

  template <typename Functor>
  void iterate_symbols(Functor& functor) {
    iterate(functor, _sym_list);
  }

  template <typename Functor>
  void iterate_cstrings(Functor& functor) {
    iterate(functor, _cstring_list);
  }

  bool has_entries() const { return has_symbol_entries() || has_cstring_entries(); }
  bool has_symbol_entries() const { return _sym_list != NULL; }
  bool has_cstring_entries() const { return _cstring_list != NULL; }
};

/**
 * When processing a set of artifacts, there will be a need
 * to track transitive dependencies originating with each artifact.
 * These might or might not be explicitly "tagged" at that point.
 * With the introduction of "epochs" to allow for concurrent tagging,
 * we attempt to avoid "tagging" an artifact to indicate its use in a
 * previous epoch. This is mainly to reduce the risk for data races.
 * Instead, JfrArtifactSet is used to track transitive dependencies
 * during the write process itself.
 *
 * It can also provide opportunities for caching, as the ideal should
 * be to reduce the amount of iterations neccessary for locating artifacts
 * in the respective VM subsystems.
 */
class JfrArtifactSet : public JfrCHeapObj {
 private:
  JfrSymbolId* _symbol_id;
  GrowableArray<const Klass*>* _klass_list;
  GrowableArray<const Klass*>* _klass_loader_set;
  size_t _total_count;

 public:
  JfrArtifactSet(bool class_unload);
  ~JfrArtifactSet();

  // caller needs ResourceMark
  void initialize(bool class_unload, bool clear = false);

  traceid mark(uintptr_t hash, const Symbol* sym, bool leakp);
  traceid mark(const Klass* klass, bool leakp);
  traceid mark(const Symbol* symbol, bool leakp);
  traceid mark(uintptr_t hash, const char* const str, bool leakp);
  traceid mark_hidden_klass_name(const Klass* klass, bool leakp);
  traceid bootstrap_name(bool leakp);

  const JfrSymbolId::SymbolEntry* map_symbol(const Symbol* symbol) const;
  const JfrSymbolId::SymbolEntry* map_symbol(uintptr_t hash) const;
  const JfrSymbolId::CStringEntry* map_cstring(uintptr_t hash) const;

  bool has_klass_entries() const;
  int entries() const;
  size_t total_count() const;
  void register_klass(const Klass* k);
  bool should_do_loader_klass(const Klass* k);

  template <typename Functor>
  void iterate_klasses(Functor& functor) const {
    for (int i = 0; i < _klass_list->length(); ++i) {
      if (!functor(_klass_list->at(i))) {
        break;
      }
    }
  }

  template <typename T>
  void iterate_symbols(T& functor) {
    _symbol_id->iterate_symbols(functor);
  }

  template <typename T>
  void iterate_cstrings(T& functor) {
    _symbol_id->iterate_cstrings(functor);
  }

  template <typename Writer>
  void tally(Writer& writer) {
    _total_count += writer.count();
  }

};

class KlassArtifactRegistrator {
 private:
  JfrArtifactSet* _artifacts;
 public:
  KlassArtifactRegistrator(JfrArtifactSet* artifacts) :
    _artifacts(artifacts) {
    assert(_artifacts != NULL, "invariant");
  }

  bool operator()(const Klass* klass) {
    assert(klass != NULL, "invariant");
    _artifacts->register_klass(klass);
    return true;
  }
};

#endif // SHARE_JFR_RECORDER_CHECKPOINT_TYPES_JFRTYPESETUTILS_HPP
