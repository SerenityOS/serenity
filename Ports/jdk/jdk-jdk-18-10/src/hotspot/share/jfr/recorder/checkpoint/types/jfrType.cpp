/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/javaClasses.inline.hpp"
#include "code/codeBlob.hpp"
#include "code/codeCache.hpp"
#include "compiler/compilerDefinitions.hpp"
#include "gc/shared/gcCause.hpp"
#include "gc/shared/gcName.hpp"
#include "gc/shared/gcTrace.hpp"
#include "gc/shared/gcWhen.hpp"
#include "jfr/leakprofiler/leakProfiler.hpp"
#include "jfr/recorder/checkpoint/jfrCheckpointWriter.hpp"
#include "jfr/recorder/checkpoint/types/jfrType.hpp"
#include "jfr/recorder/jfrRecorder.hpp"
#include "jfr/recorder/checkpoint/types/jfrThreadGroup.hpp"
#include "jfr/recorder/checkpoint/types/jfrThreadState.hpp"
#include "jfr/support/jfrThreadLocal.hpp"
#include "jfr/writers/jfrJavaEventWriter.hpp"
#include "jfr/utilities/jfrThreadIterator.hpp"
#include "memory/iterator.hpp"
#include "memory/metaspace.hpp"
#include "memory/metaspaceUtils.hpp"
#include "memory/referenceType.hpp"
#include "memory/universe.hpp"
#include "oops/compressedOops.hpp"
#include "runtime/flags/jvmFlag.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/osThread.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/synchronizer.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/vmOperations.hpp"

#ifdef COMPILER2
#include "opto/compile.hpp"
#include "opto/node.hpp"
#endif

// Requires a ResourceMark for get_thread_name/as_utf8
class JfrCheckpointThreadClosure : public ThreadClosure {
 private:
  JfrCheckpointWriter& _writer;
  JfrCheckpointContext _ctx;
  const int64_t _count_position;
  Thread* const _curthread;
  u4 _count;

 public:
  JfrCheckpointThreadClosure(JfrCheckpointWriter& writer) : _writer(writer),
                                                            _ctx(writer.context()),
                                                            _count_position(writer.reserve(sizeof(u4))),
                                                            _curthread(Thread::current()),
                                                            _count(0) {
  }

  ~JfrCheckpointThreadClosure() {
    if (_count == 0) {
      // restore
      _writer.set_context(_ctx);
      return;
    }
    _writer.write_count(_count, _count_position);
  }

  void do_thread(Thread* t);
};

void JfrCheckpointThreadClosure::do_thread(Thread* t) {
  assert(t != NULL, "invariant");
  ++_count;
  _writer.write_key(JfrThreadId::jfr_id(t));
  const char* const name = JfrThreadName::name(t);
  assert(name != NULL, "invariant");
  _writer.write(name);
  _writer.write<traceid>(JfrThreadId::os_id(t));
  if (t->is_Java_thread()) {
    _writer.write(name);
    _writer.write(JfrThreadId::id(t));
    _writer.write(JfrThreadGroup::thread_group_id(JavaThread::cast(t), _curthread));
    return;
  }
  _writer.write((const char*)NULL); // java name
  _writer.write((traceid)0); // java thread id
  _writer.write((traceid)0); // java thread group
}

void JfrThreadConstantSet::serialize(JfrCheckpointWriter& writer) {
  JfrCheckpointThreadClosure tc(writer);
  JfrJavaThreadIterator javathreads(false); // include not yet live threads (_thread_new)
  while (javathreads.has_next()) {
    tc.do_thread(javathreads.next());
  }
  JfrNonJavaThreadIterator nonjavathreads;
  while (nonjavathreads.has_next()) {
    tc.do_thread(nonjavathreads.next());
  }
}

void JfrThreadGroupConstant::serialize(JfrCheckpointWriter& writer) {
  JfrThreadGroup::serialize(writer);
}

static const char* flag_value_origin_to_string(JVMFlagOrigin origin) {
  switch (origin) {
    case JVMFlagOrigin::DEFAULT: return "Default";
    case JVMFlagOrigin::COMMAND_LINE: return "Command line";
    case JVMFlagOrigin::ENVIRON_VAR: return "Environment variable";
    case JVMFlagOrigin::CONFIG_FILE: return "Config file";
    case JVMFlagOrigin::MANAGEMENT: return "Management";
    case JVMFlagOrigin::ERGONOMIC: return "Ergonomic";
    case JVMFlagOrigin::ATTACH_ON_DEMAND: return "Attach on demand";
    case JVMFlagOrigin::INTERNAL: return "Internal";
    case JVMFlagOrigin::JIMAGE_RESOURCE: return "JImage resource";
    default: ShouldNotReachHere(); return "";
  }
}

void FlagValueOriginConstant::serialize(JfrCheckpointWriter& writer) {
  constexpr EnumRange<JVMFlagOrigin> range{};
  writer.write_count(static_cast<u4>(range.size()));

  for (JVMFlagOrigin origin : range) {
    writer.write_key(static_cast<u4>(origin));
    writer.write(flag_value_origin_to_string(origin));
  }
}

void MonitorInflateCauseConstant::serialize(JfrCheckpointWriter& writer) {
  static const u4 nof_entries = ObjectSynchronizer::inflate_cause_nof;
  writer.write_count(nof_entries);
  for (u4 i = 0; i < nof_entries; ++i) {
    writer.write_key(i);
    writer.write(ObjectSynchronizer::inflate_cause_name((ObjectSynchronizer::InflateCause)i));
  }
}

void GCCauseConstant::serialize(JfrCheckpointWriter& writer) {
  static const u4 nof_entries = GCCause::_last_gc_cause;
  writer.write_count(nof_entries);
  for (u4 i = 0; i < nof_entries; ++i) {
    writer.write_key(i);
    writer.write(GCCause::to_string((GCCause::Cause)i));
  }
}

void GCNameConstant::serialize(JfrCheckpointWriter& writer) {
  static const u4 nof_entries = GCNameEndSentinel;
  writer.write_count(nof_entries);
  for (u4 i = 0; i < nof_entries; ++i) {
    writer.write_key(i);
    writer.write(GCNameHelper::to_string((GCName)i));
  }
}

void GCWhenConstant::serialize(JfrCheckpointWriter& writer) {
  static const u4 nof_entries = GCWhen::GCWhenEndSentinel;
  writer.write_count(nof_entries);
  for (u4 i = 0; i < nof_entries; ++i) {
    writer.write_key(i);
    writer.write(GCWhen::to_string((GCWhen::Type)i));
  }
}

void GCThresholdUpdaterConstant::serialize(JfrCheckpointWriter& writer) {
  static const u4 nof_entries = MetaspaceGCThresholdUpdater::Last;
  writer.write_count(nof_entries);
  for (u4 i = 0; i < nof_entries; ++i) {
    writer.write_key(i);
    writer.write(MetaspaceGCThresholdUpdater::to_string((MetaspaceGCThresholdUpdater::Type)i));
  }
}

void MetadataTypeConstant::serialize(JfrCheckpointWriter& writer) {
  static const u4 nof_entries = Metaspace::MetadataTypeCount;
  writer.write_count(nof_entries);
  for (u4 i = 0; i < nof_entries; ++i) {
    writer.write_key(i);
    writer.write(Metaspace::metadata_type_name((Metaspace::MetadataType)i));
  }
}

void MetaspaceObjectTypeConstant::serialize(JfrCheckpointWriter& writer) {
  static const u4 nof_entries = MetaspaceObj::_number_of_types;
  writer.write_count(nof_entries);
  for (u4 i = 0; i < nof_entries; ++i) {
    writer.write_key(i);
    writer.write(MetaspaceObj::type_name((MetaspaceObj::Type)i));
  }
}

static const char* reference_type_to_string(ReferenceType rt) {
  switch (rt) {
    case REF_NONE: return "None reference";
    case REF_OTHER: return "Other reference";
    case REF_SOFT: return "Soft reference";
    case REF_WEAK: return "Weak reference";
    case REF_FINAL: return "Final reference";
    case REF_PHANTOM: return "Phantom reference";
    default:
      ShouldNotReachHere();
    return NULL;
  }
}

void ReferenceTypeConstant::serialize(JfrCheckpointWriter& writer) {
  static const u4 nof_entries = REF_PHANTOM + 1;
  writer.write_count(nof_entries);
  for (u4 i = 0; i < nof_entries; ++i) {
    writer.write_key(i);
    writer.write(reference_type_to_string((ReferenceType)i));
  }
}

void NarrowOopModeConstant::serialize(JfrCheckpointWriter& writer) {
  static const u4 nof_entries = CompressedOops::HeapBasedNarrowOop + 1;
  writer.write_count(nof_entries);
  for (u4 i = 0; i < nof_entries; ++i) {
    writer.write_key(i);
    writer.write(CompressedOops::mode_to_string((CompressedOops::Mode)i));
  }
}

void CodeBlobTypeConstant::serialize(JfrCheckpointWriter& writer) {
  static const u4 nof_entries = CodeBlobType::NumTypes;
  writer.write_count(nof_entries);
  for (u4 i = 0; i < nof_entries; ++i) {
    writer.write_key(i);
    writer.write(CodeCache::get_code_heap_name(i));
  }
};

void VMOperationTypeConstant::serialize(JfrCheckpointWriter& writer) {
  static const u4 nof_entries = VM_Operation::VMOp_Terminating;
  writer.write_count(nof_entries);
  for (u4 i = 0; i < nof_entries; ++i) {
    writer.write_key(i);
    writer.write(VM_Operation::name(VM_Operation::VMOp_Type(i)));
  }
}

void ThreadStateConstant::serialize(JfrCheckpointWriter& writer) {
  JfrThreadState::serialize(writer);
}

void JfrThreadConstant::serialize(JfrCheckpointWriter& writer) {
  assert(_thread != NULL, "invariant");
  assert(_thread == Thread::current(), "invariant");
  writer.write_count(1);
  writer.write_key(JfrThreadId::jfr_id(_thread));
  const char* const name = JfrThreadName::name(_thread);
  writer.write(name);
  writer.write(JfrThreadId::os_id(_thread));
  if (_thread->is_Java_thread()) {
    writer.write(name);
    writer.write(JfrThreadId::id(_thread));
    JavaThread* const jt = JavaThread::cast(_thread);
    const traceid thread_group_id = JfrThreadGroup::thread_group_id(jt, jt);
    writer.write(thread_group_id);
    JfrThreadGroup::serialize(&writer, thread_group_id);
    return;
  }
  writer.write((const char*)NULL); // java name
  writer.write((traceid)0); // java thread id
  writer.write((traceid)0); // java thread group
}

void BytecodeConstant::serialize(JfrCheckpointWriter& writer) {
  static const u4 nof_entries = Bytecodes::number_of_codes;
  writer.write_count(nof_entries);
  for (u4 i = 0; i < nof_entries; ++i) {
    writer.write_key(i);
    writer.write(Bytecodes::name((Bytecodes::Code)i));
  }
}

void CompilerTypeConstant::serialize(JfrCheckpointWriter& writer) {
  static const u4 nof_entries = compiler_number_of_types;
  writer.write_count(nof_entries);
  for (u4 i = 0; i < nof_entries; ++i) {
    writer.write_key(i);
    writer.write(compilertype2name((CompilerType)i));
  }
}
