/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/metadata/jfrSerializer.hpp"
#include "jfr/recorder/checkpoint/jfrCheckpointWriter.hpp"
#include "jfr/recorder/checkpoint/types/jfrType.hpp"
#include "jfr/recorder/checkpoint/types/jfrTypeManager.hpp"
#include "jfr/recorder/jfrRecorder.hpp"
#include "jfr/support/jfrThreadLocal.hpp"
#include "jfr/utilities/jfrIterator.hpp"
#include "jfr/utilities/jfrLinkedList.inline.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/semaphore.hpp"
#include "runtime/thread.inline.hpp"
#include "utilities/exceptions.hpp"

class JfrSerializerRegistration : public JfrCHeapObj {
 public:
  JfrSerializerRegistration* _next; // list support
 private:
  JfrSerializer* _serializer;
  mutable JfrBlobHandle _cache;
  JfrTypeId _id;
  bool _permit_cache;
 public:
  JfrSerializerRegistration(JfrTypeId id, bool permit_cache, JfrSerializer* serializer) :
    _next(NULL), _serializer(serializer), _cache(), _id(id), _permit_cache(permit_cache) {}
  ~JfrSerializerRegistration() {
    delete _serializer;
  }

  JfrTypeId id() const {
    return _id;
  }

  void on_rotation() const {
    _serializer->on_rotation();
  }

  void invoke(JfrCheckpointWriter& writer) const {
    if (_cache.valid()) {
      writer.increment();
      _cache->write(writer);
      return;
    }
    const JfrCheckpointContext ctx = writer.context();
    // serialize the type id before invoking callback
    writer.write_type(_id);
    const intptr_t start = writer.current_offset();
    // invoke the serializer routine
    _serializer->serialize(writer);
    if (start == writer.current_offset()) {
      // the serializer implementation did nothing, rewind to restore
      writer.set_context(ctx);
      return;
    }
    if (_permit_cache) {
      _cache = writer.copy(&ctx);
    }
  }
};

static void serialize_threads(JfrCheckpointWriter& writer) {
  JfrThreadConstantSet thread_set;
  writer.write_type(TYPE_THREAD);
  thread_set.serialize(writer);
}

static void serialize_thread_groups(JfrCheckpointWriter& writer) {
  JfrThreadGroupConstant thread_group_set;
  writer.write_type(TYPE_THREADGROUP);
  thread_group_set.serialize(writer);
}

void JfrTypeManager::write_threads(JfrCheckpointWriter& writer) {
  serialize_threads(writer);
  serialize_thread_groups(writer);
}

void JfrTypeManager::create_thread_blob(Thread* t) {
  assert(t != NULL, "invariant");
  ResourceMark rm(t);
  HandleMark hm(t);
  JfrThreadConstant type_thread(t);
  JfrCheckpointWriter writer(t, true, THREADS, false);
  writer.write_type(TYPE_THREAD);
  type_thread.serialize(writer);
  // create and install a checkpoint blob
  t->jfr_thread_local()->set_thread_blob(writer.move());
  assert(t->jfr_thread_local()->has_thread_blob(), "invariant");
}

void JfrTypeManager::write_thread_checkpoint(Thread* t) {
  assert(t != NULL, "invariant");
  if (!t->jfr_thread_local()->has_thread_blob()) {
    create_thread_blob(t);
  }
  JfrCheckpointWriter writer(t, false, THREADS, false);
  t->jfr_thread_local()->thread_blob()->write(writer);
}

class SerializerRegistrationGuard : public StackObj {
 private:
  static Semaphore _mutex_semaphore;
 public:
  SerializerRegistrationGuard() {
    _mutex_semaphore.wait();
  }
  ~SerializerRegistrationGuard() {
    _mutex_semaphore.signal();
  }
};

Semaphore SerializerRegistrationGuard::_mutex_semaphore(1);

typedef JfrLinkedList<JfrSerializerRegistration> List;
static List types;

void JfrTypeManager::destroy() {
  SerializerRegistrationGuard guard;
  JfrSerializerRegistration* registration;
  while (types.is_nonempty()) {
    registration = types.remove();
    assert(registration != NULL, "invariant");
    delete registration;
  }
}

class InvokeOnRotation {
 public:
  bool process(const JfrSerializerRegistration* r) {
    assert(r != NULL, "invariant");
    r->on_rotation();
    return true;
  }
};

void JfrTypeManager::on_rotation() {
  InvokeOnRotation ior;
  types.iterate(ior);
}

#ifdef ASSERT

class Diversity {
 private:
  const JfrTypeId _id;
 public:
  Diversity(JfrTypeId id) : _id(id) {}
  bool process(const JfrSerializerRegistration* r) {
    assert(r != NULL, "invariant");
    assert(r->id() != _id, "invariant");
    return true;
  }
};

static void assert_not_registered_twice(JfrTypeId id, List& list) {
  Diversity d(id);
  types.iterate(d);
}
#endif

static bool register_static_type(JfrTypeId id, bool permit_cache, JfrSerializer* serializer) {
  assert(serializer != NULL, "invariant");
  JfrSerializerRegistration* const registration = new JfrSerializerRegistration(id, permit_cache, serializer);
  if (registration == NULL) {
    delete serializer;
    return false;
  }
  assert(!types.in_list(registration), "invariant");
  DEBUG_ONLY(assert_not_registered_twice(id, types);)
  if (JfrRecorder::is_recording()) {
    JfrCheckpointWriter writer(STATICS);
    registration->invoke(writer);
  }
  types.add(registration);
  return true;
}

bool JfrTypeManager::initialize() {
  SerializerRegistrationGuard guard;
  register_static_type(TYPE_FLAGVALUEORIGIN, true, new FlagValueOriginConstant());
  register_static_type(TYPE_INFLATECAUSE, true, new MonitorInflateCauseConstant());
  register_static_type(TYPE_GCCAUSE, true, new GCCauseConstant());
  register_static_type(TYPE_GCNAME, true, new GCNameConstant());
  register_static_type(TYPE_GCWHEN, true, new GCWhenConstant());
  register_static_type(TYPE_GCTHRESHOLDUPDATER, true, new GCThresholdUpdaterConstant());
  register_static_type(TYPE_METADATATYPE, true, new MetadataTypeConstant());
  register_static_type(TYPE_METASPACEOBJECTTYPE, true, new MetaspaceObjectTypeConstant());
  register_static_type(TYPE_REFERENCETYPE, true, new ReferenceTypeConstant());
  register_static_type(TYPE_NARROWOOPMODE, true, new NarrowOopModeConstant());
  register_static_type(TYPE_CODEBLOBTYPE, true, new CodeBlobTypeConstant());
  register_static_type(TYPE_VMOPERATIONTYPE, true, new VMOperationTypeConstant());
  register_static_type(TYPE_THREADSTATE, true, new ThreadStateConstant());
  register_static_type(TYPE_BYTECODE, true, new BytecodeConstant());
  register_static_type(TYPE_COMPILERTYPE, true, new CompilerTypeConstant());
  return true;
}

// implementation for the static registration function exposed in the JfrSerializer api
bool JfrSerializer::register_serializer(JfrTypeId id, bool permit_cache, JfrSerializer* serializer) {
  SerializerRegistrationGuard guard;
  return register_static_type(id, permit_cache, serializer);
}

class InvokeSerializer {
 private:
   JfrCheckpointWriter& _writer;
 public:
  InvokeSerializer(JfrCheckpointWriter& writer) : _writer(writer) {}
  bool process(const JfrSerializerRegistration* r) {
    assert(r != NULL, "invariant");
    r->invoke(_writer);
    return true;
  }
};

void JfrTypeManager::write_static_types(JfrCheckpointWriter& writer) {
  InvokeSerializer is(writer);
  SerializerRegistrationGuard guard;
  types.iterate(is);
}
