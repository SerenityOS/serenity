/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "jfrfiles/jfrTypes.hpp"
#include "jfr/leakprofiler/chains/edge.hpp"
#include "jfr/leakprofiler/chains/edgeStore.hpp"
#include "jfr/leakprofiler/chains/edgeUtils.hpp"
#include "jfr/leakprofiler/checkpoint/objectSampleDescription.hpp"
#include "jfr/leakprofiler/checkpoint/objectSampleWriter.hpp"
#include "jfr/leakprofiler/checkpoint/rootResolver.hpp"
#include "jfr/leakprofiler/sampling/objectSampler.hpp"
#include "jfr/leakprofiler/utilities/rootType.hpp"
#include "jfr/leakprofiler/utilities/unifiedOopRef.inline.hpp"
#include "jfr/metadata/jfrSerializer.hpp"
#include "jfr/writers/jfrTypeWriterHost.hpp"
#include "oops/oop.inline.hpp"
#include "oops/symbol.hpp"
#include "utilities/growableArray.hpp"

template <typename Data>
class ObjectSampleAuxInfo : public ResourceObj {
 public:
  Data _data;
  traceid _id;
  ObjectSampleAuxInfo() : _data(), _id(0) {}
};

class ObjectSampleArrayData {
 public:
  int _array_size;
  int _array_index;
  ObjectSampleArrayData() : _array_size(0), _array_index(0) {}
};

class ObjectSampleFieldInfo : public ResourceObj {
 public:
  const Symbol* _field_name_symbol;
  jshort _field_modifiers;
  ObjectSampleFieldInfo() : _field_name_symbol(NULL), _field_modifiers(0) {}
};

class ObjectSampleRootDescriptionData {
 public:
  const Edge* _root_edge;
  const char* _description;
  OldObjectRoot::System _system;
  OldObjectRoot::Type _type;
  ObjectSampleRootDescriptionData() : _root_edge(NULL),
                                      _description(NULL),
                                      _system(OldObjectRoot::_system_undetermined),
                                      _type(OldObjectRoot::_type_undetermined) {}
};

class OldObjectSampleData {
 public:
  oop _object;
  traceid _reference_id;
};

class ReferenceData {
 public:
  traceid _field_info_id;
  traceid _array_info_id;
  traceid _old_object_sample_id;
  size_t  _skip;
};

static int initial_storage_size = 16;

template <typename Data>
class SampleSet : public ResourceObj {
 private:
  GrowableArray<Data>* _storage;
 public:
  SampleSet() : _storage(NULL) {}

  traceid store(Data data) {
    assert(data != NULL, "invariant");
    if (_storage == NULL) {
      _storage = new GrowableArray<Data>(initial_storage_size);
    }
    assert(_storage != NULL, "invariant");
    assert(_storage->find(data) == -1, "invariant");
    _storage->append(data);
    return data->_id;
  }

  size_t size() const {
    return _storage != NULL ? (size_t)_storage->length() : 0;
  }

  template <typename Functor>
  void iterate(Functor& functor) {
    if (_storage != NULL) {
      for (int i = 0; i < _storage->length(); ++i) {
        functor(_storage->at(i));
      }
    }
  }

  const GrowableArray<Data>& storage() const {
    return *_storage;
  }
};

typedef ObjectSampleAuxInfo<ObjectSampleArrayData> ObjectSampleArrayInfo;
typedef ObjectSampleAuxInfo<ObjectSampleRootDescriptionData> ObjectSampleRootDescriptionInfo;
typedef ObjectSampleAuxInfo<OldObjectSampleData> OldObjectSampleInfo;
typedef ObjectSampleAuxInfo<ReferenceData> ReferenceInfo;

class FieldTable : public ResourceObj {
  template <typename,
            typename,
            template<typename, typename> class,
            typename,
            size_t>
  friend class HashTableHost;
  typedef HashTableHost<const ObjectSampleFieldInfo*, traceid, JfrHashtableEntry, FieldTable, 109> FieldInfoTable;
 public:
  typedef FieldInfoTable::HashEntry FieldInfoEntry;

 private:
  static traceid _field_id_counter;
  FieldInfoTable* _table;
  const ObjectSampleFieldInfo* _lookup;

  void on_link(FieldInfoEntry* entry) {
    assert(entry != NULL, "invariant");
    entry->set_id(++_field_id_counter);
  }

  bool on_equals(uintptr_t hash, const FieldInfoEntry* entry) {
    assert(hash == entry->hash(), "invariant");
    assert(_lookup != NULL, "invariant");
    return entry->literal()->_field_modifiers == _lookup->_field_modifiers;
  }

  void on_unlink(FieldInfoEntry* entry) {
    assert(entry != NULL, "invariant");
    // nothing
  }

 public:
  FieldTable() : _table(new FieldInfoTable(this)), _lookup(NULL) {}
  ~FieldTable() {
    assert(_table != NULL, "invariant");
    delete _table;
  }

  traceid store(const ObjectSampleFieldInfo* field_info) {
    assert(field_info != NULL, "invariant");
    _lookup = field_info;
    const FieldInfoEntry& entry = _table->lookup_put(field_info->_field_name_symbol->identity_hash(), field_info);
    return entry.id();
  }

  size_t size() const {
    return _table->cardinality();
  }

  template <typename T>
  void iterate(T& functor) const {
    _table->iterate_entry<T>(functor);
  }
};

traceid FieldTable::_field_id_counter = 0;

typedef SampleSet<const OldObjectSampleInfo*> SampleInfo;
typedef SampleSet<const ReferenceInfo*> RefInfo;
typedef SampleSet<const ObjectSampleArrayInfo*> ArrayInfo;
typedef SampleSet<const ObjectSampleRootDescriptionInfo*> RootDescriptionInfo;

static SampleInfo* sample_infos = NULL;
static RefInfo* ref_infos = NULL;
static ArrayInfo* array_infos = NULL;
static FieldTable* field_infos = NULL;
static RootDescriptionInfo* root_infos = NULL;

int __write_sample_info__(JfrCheckpointWriter* writer, const void* si) {
  assert(writer != NULL, "invariant");
  assert(si != NULL, "invariant");
  const OldObjectSampleInfo* const oosi = (const OldObjectSampleInfo*)si;
  oop object = oosi->_data._object;
  assert(object != NULL, "invariant");
  writer->write(oosi->_id);
  writer->write(cast_from_oop<u8>(object));
  writer->write(const_cast<const Klass*>(object->klass()));
  ObjectSampleDescription od(object);
  writer->write(od.description());
  writer->write(oosi->_data._reference_id);
  return 1;
}

typedef JfrTypeWriterImplHost<const OldObjectSampleInfo*, __write_sample_info__> SampleWriterImpl;
typedef JfrTypeWriterHost<SampleWriterImpl, TYPE_OLDOBJECT> SampleWriter;

static void write_sample_infos(JfrCheckpointWriter& writer) {
  if (sample_infos != NULL) {
    SampleWriter sw(&writer);
    sample_infos->iterate(sw);
  }
}

int __write_reference_info__(JfrCheckpointWriter* writer, const void* ri) {
  assert(writer != NULL, "invariant");
  assert(ri != NULL, "invariant");
  const ReferenceInfo* const ref_info = (const ReferenceInfo*)ri;
  writer->write(ref_info->_id);
  writer->write(ref_info->_data._array_info_id);
  writer->write(ref_info->_data._field_info_id);
  writer->write(ref_info->_data._old_object_sample_id);
  writer->write<s4>((s4)ref_info->_data._skip);
  return 1;
}

typedef JfrTypeWriterImplHost<const ReferenceInfo*, __write_reference_info__> ReferenceWriterImpl;
typedef JfrTypeWriterHost<ReferenceWriterImpl, TYPE_REFERENCE> ReferenceWriter;

static void write_reference_infos(JfrCheckpointWriter& writer) {
  if (ref_infos != NULL) {
    ReferenceWriter rw(&writer);
    ref_infos->iterate(rw);
  }
}

int __write_array_info__(JfrCheckpointWriter* writer, const void* ai) {
  assert(writer != NULL, "invariant");
  assert(ai != NULL, "invariant");
  const ObjectSampleArrayInfo* const osai = (const ObjectSampleArrayInfo*)ai;
  writer->write(osai->_id);
  writer->write(osai->_data._array_size);
  writer->write(osai->_data._array_index);
  return 1;
}

static traceid get_array_info_id(const Edge& edge, traceid id) {
  if (edge.is_root() || !EdgeUtils::is_array_element(edge)) {
    return 0;
  }
  if (array_infos == NULL) {
    array_infos = new ArrayInfo();
  }
  assert(array_infos != NULL, "invariant");

  ObjectSampleArrayInfo* const osai = new ObjectSampleArrayInfo();
  assert(osai != NULL, "invariant");
  osai->_id = id;
  osai->_data._array_size = EdgeUtils::array_size(edge);
  osai->_data._array_index = EdgeUtils::array_index(edge);
  return array_infos->store(osai);
}

typedef JfrTypeWriterImplHost<const ObjectSampleArrayInfo*, __write_array_info__> ArrayWriterImpl;
typedef JfrTypeWriterHost<ArrayWriterImpl, TYPE_OLDOBJECTARRAY> ArrayWriter;

static void write_array_infos(JfrCheckpointWriter& writer) {
  if (array_infos != NULL) {
    ArrayWriter aw(&writer);
    array_infos->iterate(aw);
  }
}

int __write_field_info__(JfrCheckpointWriter* writer, const void* fi) {
  assert(writer != NULL, "invariant");
  assert(fi != NULL, "invariant");
  const FieldTable::FieldInfoEntry* field_info_entry = (const FieldTable::FieldInfoEntry*)fi;
  writer->write(field_info_entry->id());
  const ObjectSampleFieldInfo* const osfi = field_info_entry->literal();
  writer->write(osfi->_field_name_symbol->as_C_string());
  writer->write(osfi->_field_modifiers);
  return 1;
}

static traceid get_field_info_id(const Edge& edge) {
  if (edge.is_root()) {
    return 0;
  }
  assert(!EdgeUtils::is_array_element(edge), "invariant");
  jshort field_modifiers;
  const Symbol* const field_name_symbol = EdgeUtils::field_name(edge, &field_modifiers);
  if (field_name_symbol == NULL) {
    return 0;
  }
  if (field_infos == NULL) {
    field_infos = new FieldTable();
  }
  assert(field_infos != NULL, "invariant");
  ObjectSampleFieldInfo* const osfi = new ObjectSampleFieldInfo();
  assert(osfi != NULL, "invariant");
  osfi->_field_name_symbol = field_name_symbol;
  osfi->_field_modifiers = field_modifiers;
  return field_infos->store(osfi);
}

typedef JfrTypeWriterImplHost<const FieldTable::FieldInfoEntry*, __write_field_info__> FieldWriterImpl;
typedef JfrTypeWriterHost<FieldWriterImpl, TYPE_OLDOBJECTFIELD> FieldWriter;

static void write_field_infos(JfrCheckpointWriter& writer) {
  if (field_infos != NULL) {
    FieldWriter fw(&writer);
    field_infos->iterate(fw);
  }
}

static const char* description(const ObjectSampleRootDescriptionInfo* osdi) {
  assert(osdi != NULL, "invariant");

  if (osdi->_data._description == NULL) {
    return NULL;
  }

  ObjectDescriptionBuilder description;
  if (osdi->_data._system == OldObjectRoot::_threads) {
    description.write_text("Thread Name: ");
  }
  description.write_text(osdi->_data._description);
  return description.description();
}

int __write_root_description_info__(JfrCheckpointWriter* writer, const void* di) {
  assert(writer != NULL, "invariant");
  assert(di != NULL, "invariant");
  const ObjectSampleRootDescriptionInfo* const osdi = (const ObjectSampleRootDescriptionInfo*)di;
  writer->write(osdi->_id);
  writer->write(description(osdi));
  writer->write<u8>(osdi->_data._system);
  writer->write<u8>(osdi->_data._type);
  return 1;
}

static traceid get_gc_root_description_info_id(const Edge& edge, traceid id) {
  assert(edge.is_root(), "invariant");
  if (root_infos == NULL) {
    root_infos = new RootDescriptionInfo();
  }
  assert(root_infos != NULL, "invariant");
  ObjectSampleRootDescriptionInfo* const oodi = new ObjectSampleRootDescriptionInfo();
  oodi->_id = id;
  oodi->_data._root_edge = &edge;
  return root_infos->store(oodi);
}

typedef JfrTypeWriterImplHost<const ObjectSampleRootDescriptionInfo*, __write_root_description_info__> RootDescriptionWriterImpl;
typedef JfrTypeWriterHost<RootDescriptionWriterImpl, TYPE_OLDOBJECTGCROOT> RootDescriptionWriter;


int _edge_reference_compare_(uintptr_t lhs, uintptr_t rhs) {
  return lhs > rhs ? 1 : (lhs < rhs) ? -1 : 0;
}

int _root_desc_compare_(const ObjectSampleRootDescriptionInfo*const & lhs, const ObjectSampleRootDescriptionInfo* const& rhs) {
  const uintptr_t lhs_ref = lhs->_data._root_edge->reference().addr<uintptr_t>();
  const uintptr_t rhs_ref = rhs->_data._root_edge->reference().addr<uintptr_t>();
  return _edge_reference_compare_(lhs_ref, rhs_ref);
}

static int find_sorted(const RootCallbackInfo& callback_info,
                       const GrowableArray<const ObjectSampleRootDescriptionInfo*>* arr,
                       int length,
                       bool& found) {
  assert(arr != NULL, "invariant");
  assert(length >= 0, "invariant");
  assert(length <= arr->length(), "invariant");

  found = false;
  int min = 0;
  int max = length;
  while (max >= min) {
    const int mid = (int)(((uint)max + min) / 2);
    int diff = _edge_reference_compare_((uintptr_t)callback_info._high,
                                        arr->at(mid)->_data._root_edge->reference().addr<uintptr_t>());
    if (diff > 0) {
      min = mid + 1;
    } else if (diff < 0) {
      max = mid - 1;
    } else {
      found = true;
      return mid;
    }
  }
  return min;
}

class RootResolutionSet : public ResourceObj, public RootCallback {
 private:
  GrowableArray<const ObjectSampleRootDescriptionInfo*>* _unresolved_roots;

  const uintptr_t high() const {
    return _unresolved_roots->last()->_data._root_edge->reference().addr<uintptr_t>();
  }

  const uintptr_t low() const {
    return _unresolved_roots->first()->_data._root_edge->reference().addr<uintptr_t>();
  }

  bool in_set_address_range(const RootCallbackInfo& callback_info) const {
    assert(callback_info._low == NULL, "invariant");
    const uintptr_t addr = (uintptr_t)callback_info._high;
    return low() <= addr && high() >= addr;
  }

  int compare_to_range(const RootCallbackInfo& callback_info) const {
    assert(callback_info._high != NULL, "invariant");
    assert(callback_info._low != NULL, "invariant");

    for (int i = 0; i < _unresolved_roots->length(); ++i) {
      const uintptr_t ref_addr = _unresolved_roots->at(i)->_data._root_edge->reference().addr<uintptr_t>();
      if ((uintptr_t)callback_info._low <= ref_addr && (uintptr_t)callback_info._high >= ref_addr) {
        return i;
      }
    }
    return -1;
  }

  int exact(const RootCallbackInfo& callback_info) const {
    assert(callback_info._high != NULL, "invariant");
    assert(in_set_address_range(callback_info), "invariant");

    bool found;
    const int idx = find_sorted(callback_info, _unresolved_roots, _unresolved_roots->length(), found);
    return found ? idx : -1;
  }

  bool resolve_root(const RootCallbackInfo& callback_info, int idx) const {
    assert(idx >= 0, "invariant");
    assert(idx < _unresolved_roots->length(), "invariant");

    ObjectSampleRootDescriptionInfo* const desc =
      const_cast<ObjectSampleRootDescriptionInfo*>(_unresolved_roots->at(idx));
    assert(desc != NULL, "invariant");
    assert((uintptr_t)callback_info._high == desc->_data._root_edge->reference().addr<uintptr_t>(), "invariant");

    desc->_data._system = callback_info._system;
    desc->_data._type = callback_info._type;

    if (callback_info._system == OldObjectRoot::_threads) {
      const JavaThread* jt = (const JavaThread*)callback_info._context;
      assert(jt != NULL, "invariant");
      desc->_data._description = jt->name();
    }

    _unresolved_roots->remove_at(idx);
    return _unresolved_roots->is_empty();
  }

 public:
  RootResolutionSet(RootDescriptionInfo* info) : _unresolved_roots(NULL) {
    assert(info != NULL, "invariant");
    // construct a sorted copy
    const GrowableArray<const ObjectSampleRootDescriptionInfo*>& info_storage = info->storage();
    const int length = info_storage.length();
    _unresolved_roots = new GrowableArray<const ObjectSampleRootDescriptionInfo*>(length);
    assert(_unresolved_roots != NULL, "invariant");

    for (int i = 0; i < length; ++i) {
      _unresolved_roots->insert_sorted<_root_desc_compare_>(info_storage.at(i));
    }
  }

  bool process(const RootCallbackInfo& callback_info) {
    if (NULL == callback_info._low) {
      if (in_set_address_range(callback_info)) {
        const int idx = exact(callback_info);
        return idx == -1 ? false : resolve_root(callback_info, idx);
      }
      return false;
    }
    assert(callback_info._low != NULL, "invariant");
    const int idx = compare_to_range(callback_info);
    return idx == -1 ? false : resolve_root(callback_info, idx);
  }

  int entries() const {
    return _unresolved_roots->length();
  }

  UnifiedOopRef at(int idx) const {
    assert(idx >= 0, "invariant");
    assert(idx < _unresolved_roots->length(), "invariant");
    return _unresolved_roots->at(idx)->_data._root_edge->reference();
  }
};

static void write_root_descriptors(JfrCheckpointWriter& writer) {
  if (root_infos != NULL) {
    // resolve roots
    RootResolutionSet rrs(root_infos);
    RootResolver::resolve(rrs);
    // write roots
    RootDescriptionWriter rw(&writer);
    root_infos->iterate(rw);
  }
}

static void add_old_object_sample_info(const StoredEdge* current, traceid id) {
  assert(current != NULL, "invariant");
  if (sample_infos == NULL) {
    sample_infos = new SampleInfo();
  }
  assert(sample_infos != NULL, "invariant");
  OldObjectSampleInfo* const oosi = new OldObjectSampleInfo();
  assert(oosi != NULL, "invariant");
  oosi->_id = id;
  oosi->_data._object = current->pointee();
  oosi->_data._reference_id = current->parent() == NULL ? 0 : id;
  sample_infos->store(oosi);
}

static void add_reference_info(const StoredEdge* current, traceid id, traceid parent_id) {
  assert(current != NULL, "invariant");
  if (ref_infos == NULL) {
    ref_infos = new RefInfo();
  }

  assert(ref_infos != NULL, "invariant");
  ReferenceInfo* const ri = new ReferenceInfo();
  assert(ri != NULL, "invariant");

  ri->_id = id;
  ri->_data._array_info_id =  current->is_skip_edge() ? 0 : get_array_info_id(*current, id);
  ri->_data._field_info_id = ri->_data._array_info_id != 0 || current->is_skip_edge() ? 0 : get_field_info_id(*current);
  ri->_data._old_object_sample_id = parent_id;
  ri->_data._skip = current->skip_length();
  ref_infos->store(ri);
}

static bool is_gc_root(const StoredEdge* current) {
  assert(current != NULL, "invariant");
  return current->parent() == NULL && current->gc_root_id() != 0;
}

static traceid add_gc_root_info(const StoredEdge* root, traceid id) {
  assert(root != NULL, "invariant");
  assert(is_gc_root(root), "invariant");
  return get_gc_root_description_info_id(*root, id);
}

void ObjectSampleWriter::write(const StoredEdge* edge) {
  assert(edge != NULL, "invariant");
  const traceid id = _store->get_id(edge);
  add_old_object_sample_info(edge, id);
  const StoredEdge* const parent = edge->parent();
  if (parent != NULL) {
    add_reference_info(edge, id, _store->get_id(parent));
    return;
  }
  if (is_gc_root(edge)) {
    assert(edge->gc_root_id() == id, "invariant");
    add_gc_root_info(edge, id);
  }
}

class RootSystemType : public JfrSerializer {
 public:
  void serialize(JfrCheckpointWriter& writer) {
    const u4 nof_root_systems = OldObjectRoot::_number_of_systems;
    writer.write_count(nof_root_systems);
    for (u4 i = 0; i < nof_root_systems; ++i) {
      writer.write_key(i);
      writer.write(OldObjectRoot::system_description((OldObjectRoot::System)i));
    }
  }
};

class RootType : public JfrSerializer {
 public:
  void serialize(JfrCheckpointWriter& writer) {
    const u4 nof_root_types = OldObjectRoot::_number_of_types;
    writer.write_count(nof_root_types);
    for (u4 i = 0; i < nof_root_types; ++i) {
      writer.write_key(i);
      writer.write(OldObjectRoot::type_description((OldObjectRoot::Type)i));
    }
  }
};

static void register_serializers() {
  static bool is_registered = false;
  if (!is_registered) {
    JfrSerializer::register_serializer(TYPE_OLDOBJECTROOTSYSTEM, true, new RootSystemType());
    JfrSerializer::register_serializer(TYPE_OLDOBJECTROOTTYPE, true, new RootType());
    is_registered = true;
  }
}

ObjectSampleWriter::ObjectSampleWriter(JfrCheckpointWriter& writer, EdgeStore* store) :
  _writer(writer),
  _store(store) {
  assert(store != NULL, "invariant");
  assert(!store->is_empty(), "invariant");
  register_serializers();
  sample_infos = NULL;
  ref_infos = NULL;
  array_infos = NULL;
  field_infos = NULL;
  root_infos = NULL;
}

ObjectSampleWriter::~ObjectSampleWriter() {
  write_sample_infos(_writer);
  write_reference_infos(_writer);
  write_array_infos(_writer);
  write_field_infos(_writer);
  write_root_descriptors(_writer);
}

bool ObjectSampleWriter::operator()(StoredEdge& e) {
  write(&e);
  return true;
}
