/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm.h"
#include "classfile/classLoaderData.inline.hpp"
#include "classfile/classLoaderDataGraph.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "gc/shared/gcLocker.hpp"
#include "gc/shared/gcVMOperations.hpp"
#include "gc/shared/workgroup.hpp"
#include "jfr/jfrEvents.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/klass.inline.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/jniHandles.hpp"
#include "runtime/os.hpp"
#include "runtime/reflectionUtils.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/threadSMR.hpp"
#include "runtime/vframe.hpp"
#include "runtime/vmThread.hpp"
#include "runtime/vmOperations.hpp"
#include "services/heapDumper.hpp"
#include "services/heapDumperCompression.hpp"
#include "services/threadService.hpp"
#include "utilities/macros.hpp"
#include "utilities/ostream.hpp"

/*
 * HPROF binary format - description copied from:
 *   src/share/demo/jvmti/hprof/hprof_io.c
 *
 *
 *  header    "JAVA PROFILE 1.0.2" (0-terminated)
 *
 *  u4        size of identifiers. Identifiers are used to represent
 *            UTF8 strings, objects, stack traces, etc. They usually
 *            have the same size as host pointers. For example, on
 *            Solaris and Win32, the size is 4.
 * u4         high word
 * u4         low word    number of milliseconds since 0:00 GMT, 1/1/70
 * [record]*  a sequence of records.
 *
 *
 * Record format:
 *
 * u1         a TAG denoting the type of the record
 * u4         number of *microseconds* since the time stamp in the
 *            header. (wraps around in a little more than an hour)
 * u4         number of bytes *remaining* in the record. Note that
 *            this number excludes the tag and the length field itself.
 * [u1]*      BODY of the record (a sequence of bytes)
 *
 *
 * The following TAGs are supported:
 *
 * TAG           BODY       notes
 *----------------------------------------------------------
 * HPROF_UTF8               a UTF8-encoded name
 *
 *               id         name ID
 *               [u1]*      UTF8 characters (no trailing zero)
 *
 * HPROF_LOAD_CLASS         a newly loaded class
 *
 *                u4        class serial number (> 0)
 *                id        class object ID
 *                u4        stack trace serial number
 *                id        class name ID
 *
 * HPROF_UNLOAD_CLASS       an unloading class
 *
 *                u4        class serial_number
 *
 * HPROF_FRAME              a Java stack frame
 *
 *                id        stack frame ID
 *                id        method name ID
 *                id        method signature ID
 *                id        source file name ID
 *                u4        class serial number
 *                i4        line number. >0: normal
 *                                       -1: unknown
 *                                       -2: compiled method
 *                                       -3: native method
 *
 * HPROF_TRACE              a Java stack trace
 *
 *               u4         stack trace serial number
 *               u4         thread serial number
 *               u4         number of frames
 *               [id]*      stack frame IDs
 *
 *
 * HPROF_ALLOC_SITES        a set of heap allocation sites, obtained after GC
 *
 *               u2         flags 0x0001: incremental vs. complete
 *                                0x0002: sorted by allocation vs. live
 *                                0x0004: whether to force a GC
 *               u4         cutoff ratio
 *               u4         total live bytes
 *               u4         total live instances
 *               u8         total bytes allocated
 *               u8         total instances allocated
 *               u4         number of sites that follow
 *               [u1        is_array: 0:  normal object
 *                                    2:  object array
 *                                    4:  boolean array
 *                                    5:  char array
 *                                    6:  float array
 *                                    7:  double array
 *                                    8:  byte array
 *                                    9:  short array
 *                                    10: int array
 *                                    11: long array
 *                u4        class serial number (may be zero during startup)
 *                u4        stack trace serial number
 *                u4        number of bytes alive
 *                u4        number of instances alive
 *                u4        number of bytes allocated
 *                u4]*      number of instance allocated
 *
 * HPROF_START_THREAD       a newly started thread.
 *
 *               u4         thread serial number (> 0)
 *               id         thread object ID
 *               u4         stack trace serial number
 *               id         thread name ID
 *               id         thread group name ID
 *               id         thread group parent name ID
 *
 * HPROF_END_THREAD         a terminating thread.
 *
 *               u4         thread serial number
 *
 * HPROF_HEAP_SUMMARY       heap summary
 *
 *               u4         total live bytes
 *               u4         total live instances
 *               u8         total bytes allocated
 *               u8         total instances allocated
 *
 * HPROF_HEAP_DUMP          denote a heap dump
 *
 *               [heap dump sub-records]*
 *
 *                          There are four kinds of heap dump sub-records:
 *
 *               u1         sub-record type
 *
 *               HPROF_GC_ROOT_UNKNOWN         unknown root
 *
 *                          id         object ID
 *
 *               HPROF_GC_ROOT_THREAD_OBJ      thread object
 *
 *                          id         thread object ID  (may be 0 for a
 *                                     thread newly attached through JNI)
 *                          u4         thread sequence number
 *                          u4         stack trace sequence number
 *
 *               HPROF_GC_ROOT_JNI_GLOBAL      JNI global ref root
 *
 *                          id         object ID
 *                          id         JNI global ref ID
 *
 *               HPROF_GC_ROOT_JNI_LOCAL       JNI local ref
 *
 *                          id         object ID
 *                          u4         thread serial number
 *                          u4         frame # in stack trace (-1 for empty)
 *
 *               HPROF_GC_ROOT_JAVA_FRAME      Java stack frame
 *
 *                          id         object ID
 *                          u4         thread serial number
 *                          u4         frame # in stack trace (-1 for empty)
 *
 *               HPROF_GC_ROOT_NATIVE_STACK    Native stack
 *
 *                          id         object ID
 *                          u4         thread serial number
 *
 *               HPROF_GC_ROOT_STICKY_CLASS    System class
 *
 *                          id         object ID
 *
 *               HPROF_GC_ROOT_THREAD_BLOCK    Reference from thread block
 *
 *                          id         object ID
 *                          u4         thread serial number
 *
 *               HPROF_GC_ROOT_MONITOR_USED    Busy monitor
 *
 *                          id         object ID
 *
 *               HPROF_GC_CLASS_DUMP           dump of a class object
 *
 *                          id         class object ID
 *                          u4         stack trace serial number
 *                          id         super class object ID
 *                          id         class loader object ID
 *                          id         signers object ID
 *                          id         protection domain object ID
 *                          id         reserved
 *                          id         reserved
 *
 *                          u4         instance size (in bytes)
 *
 *                          u2         size of constant pool
 *                          [u2,       constant pool index,
 *                           ty,       type
 *                                     2:  object
 *                                     4:  boolean
 *                                     5:  char
 *                                     6:  float
 *                                     7:  double
 *                                     8:  byte
 *                                     9:  short
 *                                     10: int
 *                                     11: long
 *                           vl]*      and value
 *
 *                          u2         number of static fields
 *                          [id,       static field name,
 *                           ty,       type,
 *                           vl]*      and value
 *
 *                          u2         number of inst. fields (not inc. super)
 *                          [id,       instance field name,
 *                           ty]*      type
 *
 *               HPROF_GC_INSTANCE_DUMP        dump of a normal object
 *
 *                          id         object ID
 *                          u4         stack trace serial number
 *                          id         class object ID
 *                          u4         number of bytes that follow
 *                          [vl]*      instance field values (class, followed
 *                                     by super, super's super ...)
 *
 *               HPROF_GC_OBJ_ARRAY_DUMP       dump of an object array
 *
 *                          id         array object ID
 *                          u4         stack trace serial number
 *                          u4         number of elements
 *                          id         array class ID
 *                          [id]*      elements
 *
 *               HPROF_GC_PRIM_ARRAY_DUMP      dump of a primitive array
 *
 *                          id         array object ID
 *                          u4         stack trace serial number
 *                          u4         number of elements
 *                          u1         element type
 *                                     4:  boolean array
 *                                     5:  char array
 *                                     6:  float array
 *                                     7:  double array
 *                                     8:  byte array
 *                                     9:  short array
 *                                     10: int array
 *                                     11: long array
 *                          [u1]*      elements
 *
 * HPROF_CPU_SAMPLES        a set of sample traces of running threads
 *
 *                u4        total number of samples
 *                u4        # of traces
 *               [u4        # of samples
 *                u4]*      stack trace serial number
 *
 * HPROF_CONTROL_SETTINGS   the settings of on/off switches
 *
 *                u4        0x00000001: alloc traces on/off
 *                          0x00000002: cpu sampling on/off
 *                u2        stack trace depth
 *
 *
 * When the header is "JAVA PROFILE 1.0.2" a heap dump can optionally
 * be generated as a sequence of heap dump segments. This sequence is
 * terminated by an end record. The additional tags allowed by format
 * "JAVA PROFILE 1.0.2" are:
 *
 * HPROF_HEAP_DUMP_SEGMENT  denote a heap dump segment
 *
 *               [heap dump sub-records]*
 *               The same sub-record types allowed by HPROF_HEAP_DUMP
 *
 * HPROF_HEAP_DUMP_END      denotes the end of a heap dump
 *
 */


// HPROF tags

typedef enum {
  // top-level records
  HPROF_UTF8                    = 0x01,
  HPROF_LOAD_CLASS              = 0x02,
  HPROF_UNLOAD_CLASS            = 0x03,
  HPROF_FRAME                   = 0x04,
  HPROF_TRACE                   = 0x05,
  HPROF_ALLOC_SITES             = 0x06,
  HPROF_HEAP_SUMMARY            = 0x07,
  HPROF_START_THREAD            = 0x0A,
  HPROF_END_THREAD              = 0x0B,
  HPROF_HEAP_DUMP               = 0x0C,
  HPROF_CPU_SAMPLES             = 0x0D,
  HPROF_CONTROL_SETTINGS        = 0x0E,

  // 1.0.2 record types
  HPROF_HEAP_DUMP_SEGMENT       = 0x1C,
  HPROF_HEAP_DUMP_END           = 0x2C,

  // field types
  HPROF_ARRAY_OBJECT            = 0x01,
  HPROF_NORMAL_OBJECT           = 0x02,
  HPROF_BOOLEAN                 = 0x04,
  HPROF_CHAR                    = 0x05,
  HPROF_FLOAT                   = 0x06,
  HPROF_DOUBLE                  = 0x07,
  HPROF_BYTE                    = 0x08,
  HPROF_SHORT                   = 0x09,
  HPROF_INT                     = 0x0A,
  HPROF_LONG                    = 0x0B,

  // data-dump sub-records
  HPROF_GC_ROOT_UNKNOWN         = 0xFF,
  HPROF_GC_ROOT_JNI_GLOBAL      = 0x01,
  HPROF_GC_ROOT_JNI_LOCAL       = 0x02,
  HPROF_GC_ROOT_JAVA_FRAME      = 0x03,
  HPROF_GC_ROOT_NATIVE_STACK    = 0x04,
  HPROF_GC_ROOT_STICKY_CLASS    = 0x05,
  HPROF_GC_ROOT_THREAD_BLOCK    = 0x06,
  HPROF_GC_ROOT_MONITOR_USED    = 0x07,
  HPROF_GC_ROOT_THREAD_OBJ      = 0x08,
  HPROF_GC_CLASS_DUMP           = 0x20,
  HPROF_GC_INSTANCE_DUMP        = 0x21,
  HPROF_GC_OBJ_ARRAY_DUMP       = 0x22,
  HPROF_GC_PRIM_ARRAY_DUMP      = 0x23
} hprofTag;

// Default stack trace ID (used for dummy HPROF_TRACE record)
enum {
  STACK_TRACE_ID = 1,
  INITIAL_CLASS_COUNT = 200
};

// Supports I/O operations for a dump

class DumpWriter : public StackObj {
 private:
  enum {
    io_buffer_max_size = 1*M,
    io_buffer_max_waste = 10*K,
    dump_segment_header_size = 9
  };

  char* _buffer;    // internal buffer
  size_t _size;
  size_t _pos;

  bool _in_dump_segment; // Are we currently in a dump segment?
  bool _is_huge_sub_record; // Are we writing a sub-record larger than the buffer size?
  DEBUG_ONLY(size_t _sub_record_left;) // The bytes not written for the current sub-record.
  DEBUG_ONLY(bool _sub_record_ended;) // True if we have called the end_sub_record().

  CompressionBackend _backend; // Does the actual writing.

  void flush();

  char* buffer() const                          { return _buffer; }
  size_t buffer_size() const                    { return _size; }
  size_t position() const                       { return _pos; }
  void set_position(size_t pos)                 { _pos = pos; }

  // Can be called if we have enough room in the buffer.
  void write_fast(void* s, size_t len);

  // Returns true if we have enough room in the buffer for 'len' bytes.
  bool can_write_fast(size_t len);

 public:
  // Takes ownership of the writer and compressor.
  DumpWriter(AbstractWriter* writer, AbstractCompressor* compressor);

  ~DumpWriter();

  // total number of bytes written to the disk
  julong bytes_written() const          { return (julong) _backend.get_written(); }

  char const* error() const             { return _backend.error(); }

  // writer functions
  void write_raw(void* s, size_t len);
  void write_u1(u1 x);
  void write_u2(u2 x);
  void write_u4(u4 x);
  void write_u8(u8 x);
  void write_objectID(oop o);
  void write_symbolID(Symbol* o);
  void write_classID(Klass* k);
  void write_id(u4 x);

  // Start a new sub-record. Starts a new heap dump segment if needed.
  void start_sub_record(u1 tag, u4 len);
  // Ends the current sub-record.
  void end_sub_record();
  // Finishes the current dump segment if not already finished.
  void finish_dump_segment();

  // Called by threads used for parallel writing.
  void writer_loop()                    { _backend.thread_loop(); }
  // Called when finished to release the threads.
  void deactivate()                     { flush(); _backend.deactivate(); }
};

// Check for error after constructing the object and destroy it in case of an error.
DumpWriter::DumpWriter(AbstractWriter* writer, AbstractCompressor* compressor) :
  _buffer(NULL),
  _size(0),
  _pos(0),
  _in_dump_segment(false),
  _backend(writer, compressor, io_buffer_max_size, io_buffer_max_waste) {
  flush();
}

DumpWriter::~DumpWriter() {
  flush();
}

void DumpWriter::write_fast(void* s, size_t len) {
  assert(!_in_dump_segment || (_sub_record_left >= len), "sub-record too large");
  assert(buffer_size() - position() >= len, "Must fit");
  debug_only(_sub_record_left -= len);

  memcpy(buffer() + position(), s, len);
  set_position(position() + len);
}

bool DumpWriter::can_write_fast(size_t len) {
  return buffer_size() - position() >= len;
}

// write raw bytes
void DumpWriter::write_raw(void* s, size_t len) {
  assert(!_in_dump_segment || (_sub_record_left >= len), "sub-record too large");
  debug_only(_sub_record_left -= len);

  // flush buffer to make room.
  while (len > buffer_size() - position()) {
    assert(!_in_dump_segment || _is_huge_sub_record,
           "Cannot overflow in non-huge sub-record.");

    size_t to_write = buffer_size() - position();
    memcpy(buffer() + position(), s, to_write);
    s = (void*) ((char*) s + to_write);
    len -= to_write;
    set_position(position() + to_write);
    flush();
  }

  memcpy(buffer() + position(), s, len);
  set_position(position() + len);
}

// flush any buffered bytes to the file
void DumpWriter::flush() {
  _backend.get_new_buffer(&_buffer, &_pos, &_size);
}

// Makes sure we inline the fast write into the write_u* functions. This is a big speedup.
#define WRITE_KNOWN_TYPE(p, len) do { if (can_write_fast((len))) write_fast((p), (len)); \
                                      else write_raw((p), (len)); } while (0)

void DumpWriter::write_u1(u1 x) {
  WRITE_KNOWN_TYPE((void*) &x, 1);
}

void DumpWriter::write_u2(u2 x) {
  u2 v;
  Bytes::put_Java_u2((address)&v, x);
  WRITE_KNOWN_TYPE((void*)&v, 2);
}

void DumpWriter::write_u4(u4 x) {
  u4 v;
  Bytes::put_Java_u4((address)&v, x);
  WRITE_KNOWN_TYPE((void*)&v, 4);
}

void DumpWriter::write_u8(u8 x) {
  u8 v;
  Bytes::put_Java_u8((address)&v, x);
  WRITE_KNOWN_TYPE((void*)&v, 8);
}

void DumpWriter::write_objectID(oop o) {
  address a = cast_from_oop<address>(o);
#ifdef _LP64
  write_u8((u8)a);
#else
  write_u4((u4)a);
#endif
}

void DumpWriter::write_symbolID(Symbol* s) {
  address a = (address)((uintptr_t)s);
#ifdef _LP64
  write_u8((u8)a);
#else
  write_u4((u4)a);
#endif
}

void DumpWriter::write_id(u4 x) {
#ifdef _LP64
  write_u8((u8) x);
#else
  write_u4(x);
#endif
}

// We use java mirror as the class ID
void DumpWriter::write_classID(Klass* k) {
  write_objectID(k->java_mirror());
}

void DumpWriter::finish_dump_segment() {
  if (_in_dump_segment) {
    assert(_sub_record_left == 0, "Last sub-record not written completely");
    assert(_sub_record_ended, "sub-record must have ended");

    // Fix up the dump segment length if we haven't written a huge sub-record last
    // (in which case the segment length was already set to the correct value initially).
    if (!_is_huge_sub_record) {
      assert(position() > dump_segment_header_size, "Dump segment should have some content");
      Bytes::put_Java_u4((address) (buffer() + 5),
                         (u4) (position() - dump_segment_header_size));
    }

    flush();
    _in_dump_segment = false;
  }
}

void DumpWriter::start_sub_record(u1 tag, u4 len) {
  if (!_in_dump_segment) {
    if (position() > 0) {
      flush();
    }

    assert(position() == 0, "Must be at the start");

    write_u1(HPROF_HEAP_DUMP_SEGMENT);
    write_u4(0); // timestamp
    // Will be fixed up later if we add more sub-records.  If this is a huge sub-record,
    // this is already the correct length, since we don't add more sub-records.
    write_u4(len);
    _in_dump_segment = true;
    _is_huge_sub_record = len > buffer_size() - dump_segment_header_size;
  } else if (_is_huge_sub_record || (len > buffer_size() - position())) {
    // This object will not fit in completely or the last sub-record was huge.
    // Finish the current segement and try again.
    finish_dump_segment();
    start_sub_record(tag, len);

    return;
  }

  debug_only(_sub_record_left = len);
  debug_only(_sub_record_ended = false);

  write_u1(tag);
}

void DumpWriter::end_sub_record() {
  assert(_in_dump_segment, "must be in dump segment");
  assert(_sub_record_left == 0, "sub-record not written completely");
  assert(!_sub_record_ended, "Must not have ended yet");
  debug_only(_sub_record_ended = true);
}

// Support class with a collection of functions used when dumping the heap

class DumperSupport : AllStatic {
 public:

  // write a header of the given type
  static void write_header(DumpWriter* writer, hprofTag tag, u4 len);

  // returns hprof tag for the given type signature
  static hprofTag sig2tag(Symbol* sig);
  // returns hprof tag for the given basic type
  static hprofTag type2tag(BasicType type);
  // Returns the size of the data to write.
  static u4 sig2size(Symbol* sig);

  // returns the size of the instance of the given class
  static u4 instance_size(Klass* k);

  // dump a jfloat
  static void dump_float(DumpWriter* writer, jfloat f);
  // dump a jdouble
  static void dump_double(DumpWriter* writer, jdouble d);
  // dumps the raw value of the given field
  static void dump_field_value(DumpWriter* writer, char type, oop obj, int offset);
  // returns the size of the static fields; also counts the static fields
  static u4 get_static_fields_size(InstanceKlass* ik, u2& field_count);
  // dumps static fields of the given class
  static void dump_static_fields(DumpWriter* writer, Klass* k);
  // dump the raw values of the instance fields of the given object
  static void dump_instance_fields(DumpWriter* writer, oop o);
  // get the count of the instance fields for a given class
  static u2 get_instance_fields_count(InstanceKlass* ik);
  // dumps the definition of the instance fields for a given class
  static void dump_instance_field_descriptors(DumpWriter* writer, Klass* k);
  // creates HPROF_GC_INSTANCE_DUMP record for the given object
  static void dump_instance(DumpWriter* writer, oop o);
  // creates HPROF_GC_CLASS_DUMP record for the given class and each of its
  // array classes
  static void dump_class_and_array_classes(DumpWriter* writer, Klass* k);
  // creates HPROF_GC_CLASS_DUMP record for a given primitive array
  // class (and each multi-dimensional array class too)
  static void dump_basic_type_array_class(DumpWriter* writer, Klass* k);

  // creates HPROF_GC_OBJ_ARRAY_DUMP record for the given object array
  static void dump_object_array(DumpWriter* writer, objArrayOop array);
  // creates HPROF_GC_PRIM_ARRAY_DUMP record for the given type array
  static void dump_prim_array(DumpWriter* writer, typeArrayOop array);
  // create HPROF_FRAME record for the given method and bci
  static void dump_stack_frame(DumpWriter* writer, int frame_serial_num, int class_serial_num, Method* m, int bci);

  // check if we need to truncate an array
  static int calculate_array_max_length(DumpWriter* writer, arrayOop array, short header_size);

  // fixes up the current dump record and writes HPROF_HEAP_DUMP_END record
  static void end_of_dump(DumpWriter* writer);

  static oop mask_dormant_archived_object(oop o) {
    if (o != NULL && o->klass()->java_mirror() == NULL) {
      // Ignore this object since the corresponding java mirror is not loaded.
      // Might be a dormant archive object.
      return NULL;
    } else {
      return o;
    }
  }
};

// write a header of the given type
void DumperSupport:: write_header(DumpWriter* writer, hprofTag tag, u4 len) {
  writer->write_u1((u1)tag);
  writer->write_u4(0);                  // current ticks
  writer->write_u4(len);
}

// returns hprof tag for the given type signature
hprofTag DumperSupport::sig2tag(Symbol* sig) {
  switch (sig->char_at(0)) {
    case JVM_SIGNATURE_CLASS    : return HPROF_NORMAL_OBJECT;
    case JVM_SIGNATURE_ARRAY    : return HPROF_NORMAL_OBJECT;
    case JVM_SIGNATURE_BYTE     : return HPROF_BYTE;
    case JVM_SIGNATURE_CHAR     : return HPROF_CHAR;
    case JVM_SIGNATURE_FLOAT    : return HPROF_FLOAT;
    case JVM_SIGNATURE_DOUBLE   : return HPROF_DOUBLE;
    case JVM_SIGNATURE_INT      : return HPROF_INT;
    case JVM_SIGNATURE_LONG     : return HPROF_LONG;
    case JVM_SIGNATURE_SHORT    : return HPROF_SHORT;
    case JVM_SIGNATURE_BOOLEAN  : return HPROF_BOOLEAN;
    default : ShouldNotReachHere(); /* to shut up compiler */ return HPROF_BYTE;
  }
}

hprofTag DumperSupport::type2tag(BasicType type) {
  switch (type) {
    case T_BYTE     : return HPROF_BYTE;
    case T_CHAR     : return HPROF_CHAR;
    case T_FLOAT    : return HPROF_FLOAT;
    case T_DOUBLE   : return HPROF_DOUBLE;
    case T_INT      : return HPROF_INT;
    case T_LONG     : return HPROF_LONG;
    case T_SHORT    : return HPROF_SHORT;
    case T_BOOLEAN  : return HPROF_BOOLEAN;
    default : ShouldNotReachHere(); /* to shut up compiler */ return HPROF_BYTE;
  }
}

u4 DumperSupport::sig2size(Symbol* sig) {
  switch (sig->char_at(0)) {
    case JVM_SIGNATURE_CLASS:
    case JVM_SIGNATURE_ARRAY: return sizeof(address);
    case JVM_SIGNATURE_BOOLEAN:
    case JVM_SIGNATURE_BYTE: return 1;
    case JVM_SIGNATURE_SHORT:
    case JVM_SIGNATURE_CHAR: return 2;
    case JVM_SIGNATURE_INT:
    case JVM_SIGNATURE_FLOAT: return 4;
    case JVM_SIGNATURE_LONG:
    case JVM_SIGNATURE_DOUBLE: return 8;
    default: ShouldNotReachHere(); /* to shut up compiler */ return 0;
  }
}

// dump a jfloat
void DumperSupport::dump_float(DumpWriter* writer, jfloat f) {
  if (g_isnan(f)) {
    writer->write_u4(0x7fc00000);    // collapsing NaNs
  } else {
    union {
      int i;
      float f;
    } u;
    u.f = (float)f;
    writer->write_u4((u4)u.i);
  }
}

// dump a jdouble
void DumperSupport::dump_double(DumpWriter* writer, jdouble d) {
  union {
    jlong l;
    double d;
  } u;
  if (g_isnan(d)) {                 // collapsing NaNs
    u.l = (jlong)(0x7ff80000);
    u.l = (u.l << 32);
  } else {
    u.d = (double)d;
  }
  writer->write_u8((u8)u.l);
}

// dumps the raw value of the given field
void DumperSupport::dump_field_value(DumpWriter* writer, char type, oop obj, int offset) {
  switch (type) {
    case JVM_SIGNATURE_CLASS :
    case JVM_SIGNATURE_ARRAY : {
      oop o = obj->obj_field_access<ON_UNKNOWN_OOP_REF | AS_NO_KEEPALIVE>(offset);
      if (o != NULL && log_is_enabled(Debug, cds, heap) && mask_dormant_archived_object(o) == NULL) {
        ResourceMark rm;
        log_debug(cds, heap)("skipped dormant archived object " INTPTR_FORMAT " (%s) referenced by " INTPTR_FORMAT " (%s)",
                             p2i(o), o->klass()->external_name(),
                             p2i(obj), obj->klass()->external_name());
      }
      o = mask_dormant_archived_object(o);
      assert(oopDesc::is_oop_or_null(o), "Expected an oop or NULL at " PTR_FORMAT, p2i(o));
      writer->write_objectID(o);
      break;
    }
    case JVM_SIGNATURE_BYTE : {
      jbyte b = obj->byte_field(offset);
      writer->write_u1((u1)b);
      break;
    }
    case JVM_SIGNATURE_CHAR : {
      jchar c = obj->char_field(offset);
      writer->write_u2((u2)c);
      break;
    }
    case JVM_SIGNATURE_SHORT : {
      jshort s = obj->short_field(offset);
      writer->write_u2((u2)s);
      break;
    }
    case JVM_SIGNATURE_FLOAT : {
      jfloat f = obj->float_field(offset);
      dump_float(writer, f);
      break;
    }
    case JVM_SIGNATURE_DOUBLE : {
      jdouble d = obj->double_field(offset);
      dump_double(writer, d);
      break;
    }
    case JVM_SIGNATURE_INT : {
      jint i = obj->int_field(offset);
      writer->write_u4((u4)i);
      break;
    }
    case JVM_SIGNATURE_LONG : {
      jlong l = obj->long_field(offset);
      writer->write_u8((u8)l);
      break;
    }
    case JVM_SIGNATURE_BOOLEAN : {
      jboolean b = obj->bool_field(offset);
      writer->write_u1((u1)b);
      break;
    }
    default : {
      ShouldNotReachHere();
      break;
    }
  }
}

// returns the size of the instance of the given class
u4 DumperSupport::instance_size(Klass* k) {
  InstanceKlass* ik = InstanceKlass::cast(k);
  u4 size = 0;

  for (FieldStream fld(ik, false, false); !fld.eos(); fld.next()) {
    if (!fld.access_flags().is_static()) {
      size += sig2size(fld.signature());
    }
  }
  return size;
}

u4 DumperSupport::get_static_fields_size(InstanceKlass* ik, u2& field_count) {
  field_count = 0;
  u4 size = 0;

  for (FieldStream fldc(ik, true, true); !fldc.eos(); fldc.next()) {
    if (fldc.access_flags().is_static()) {
      field_count++;
      size += sig2size(fldc.signature());
    }
  }

  // Add in resolved_references which is referenced by the cpCache
  // The resolved_references is an array per InstanceKlass holding the
  // strings and other oops resolved from the constant pool.
  oop resolved_references = ik->constants()->resolved_references_or_null();
  if (resolved_references != NULL) {
    field_count++;
    size += sizeof(address);

    // Add in the resolved_references of the used previous versions of the class
    // in the case of RedefineClasses
    InstanceKlass* prev = ik->previous_versions();
    while (prev != NULL && prev->constants()->resolved_references_or_null() != NULL) {
      field_count++;
      size += sizeof(address);
      prev = prev->previous_versions();
    }
  }

  // Also provide a pointer to the init_lock if present, so there aren't unreferenced int[0]
  // arrays.
  oop init_lock = ik->init_lock();
  if (init_lock != NULL) {
    field_count++;
    size += sizeof(address);
  }

  // We write the value itself plus a name and a one byte type tag per field.
  return size + field_count * (sizeof(address) + 1);
}

// dumps static fields of the given class
void DumperSupport::dump_static_fields(DumpWriter* writer, Klass* k) {
  InstanceKlass* ik = InstanceKlass::cast(k);

  // dump the field descriptors and raw values
  for (FieldStream fld(ik, true, true); !fld.eos(); fld.next()) {
    if (fld.access_flags().is_static()) {
      Symbol* sig = fld.signature();

      writer->write_symbolID(fld.name());   // name
      writer->write_u1(sig2tag(sig));       // type

      // value
      dump_field_value(writer, sig->char_at(0), ik->java_mirror(), fld.offset());
    }
  }

  // Add resolved_references for each class that has them
  oop resolved_references = ik->constants()->resolved_references_or_null();
  if (resolved_references != NULL) {
    writer->write_symbolID(vmSymbols::resolved_references_name());  // name
    writer->write_u1(sig2tag(vmSymbols::object_array_signature())); // type
    writer->write_objectID(resolved_references);

    // Also write any previous versions
    InstanceKlass* prev = ik->previous_versions();
    while (prev != NULL && prev->constants()->resolved_references_or_null() != NULL) {
      writer->write_symbolID(vmSymbols::resolved_references_name());  // name
      writer->write_u1(sig2tag(vmSymbols::object_array_signature())); // type
      writer->write_objectID(prev->constants()->resolved_references());
      prev = prev->previous_versions();
    }
  }

  // Add init lock to the end if the class is not yet initialized
  oop init_lock = ik->init_lock();
  if (init_lock != NULL) {
    writer->write_symbolID(vmSymbols::init_lock_name());         // name
    writer->write_u1(sig2tag(vmSymbols::int_array_signature())); // type
    writer->write_objectID(init_lock);
  }
}

// dump the raw values of the instance fields of the given object
void DumperSupport::dump_instance_fields(DumpWriter* writer, oop o) {
  InstanceKlass* ik = InstanceKlass::cast(o->klass());

  for (FieldStream fld(ik, false, false); !fld.eos(); fld.next()) {
    if (!fld.access_flags().is_static()) {
      Symbol* sig = fld.signature();
      dump_field_value(writer, sig->char_at(0), o, fld.offset());
    }
  }
}

// dumps the definition of the instance fields for a given class
u2 DumperSupport::get_instance_fields_count(InstanceKlass* ik) {
  u2 field_count = 0;

  for (FieldStream fldc(ik, true, true); !fldc.eos(); fldc.next()) {
    if (!fldc.access_flags().is_static()) field_count++;
  }

  return field_count;
}

// dumps the definition of the instance fields for a given class
void DumperSupport::dump_instance_field_descriptors(DumpWriter* writer, Klass* k) {
  InstanceKlass* ik = InstanceKlass::cast(k);

  // dump the field descriptors
  for (FieldStream fld(ik, true, true); !fld.eos(); fld.next()) {
    if (!fld.access_flags().is_static()) {
      Symbol* sig = fld.signature();

      writer->write_symbolID(fld.name());   // name
      writer->write_u1(sig2tag(sig));       // type
    }
  }
}

// creates HPROF_GC_INSTANCE_DUMP record for the given object
void DumperSupport::dump_instance(DumpWriter* writer, oop o) {
  InstanceKlass* ik = InstanceKlass::cast(o->klass());
  u4 is = instance_size(ik);
  u4 size = 1 + sizeof(address) + 4 + sizeof(address) + 4 + is;

  writer->start_sub_record(HPROF_GC_INSTANCE_DUMP, size);
  writer->write_objectID(o);
  writer->write_u4(STACK_TRACE_ID);

  // class ID
  writer->write_classID(ik);

  // number of bytes that follow
  writer->write_u4(is);

  // field values
  dump_instance_fields(writer, o);

  writer->end_sub_record();
}

// creates HPROF_GC_CLASS_DUMP record for the given class and each of
// its array classes
void DumperSupport::dump_class_and_array_classes(DumpWriter* writer, Klass* k) {
  InstanceKlass* ik = InstanceKlass::cast(k);

  // We can safepoint and do a heap dump at a point where we have a Klass,
  // but no java mirror class has been setup for it. So we need to check
  // that the class is at least loaded, to avoid crash from a null mirror.
  if (!ik->is_loaded()) {
    return;
  }

  u2 static_fields_count = 0;
  u4 static_size = get_static_fields_size(ik, static_fields_count);
  u2 instance_fields_count = get_instance_fields_count(ik);
  u4 instance_fields_size = instance_fields_count * (sizeof(address) + 1);
  u4 size = 1 + sizeof(address) + 4 + 6 * sizeof(address) + 4 + 2 + 2 + static_size + 2 + instance_fields_size;

  writer->start_sub_record(HPROF_GC_CLASS_DUMP, size);

  // class ID
  writer->write_classID(ik);
  writer->write_u4(STACK_TRACE_ID);

  // super class ID
  InstanceKlass* java_super = ik->java_super();
  if (java_super == NULL) {
    writer->write_objectID(oop(NULL));
  } else {
    writer->write_classID(java_super);
  }

  writer->write_objectID(ik->class_loader());
  writer->write_objectID(ik->signers());
  writer->write_objectID(ik->protection_domain());

  // reserved
  writer->write_objectID(oop(NULL));
  writer->write_objectID(oop(NULL));

  // instance size
  writer->write_u4(DumperSupport::instance_size(ik));

  // size of constant pool - ignored by HAT 1.1
  writer->write_u2(0);

  // static fields
  writer->write_u2(static_fields_count);
  dump_static_fields(writer, ik);

  // description of instance fields
  writer->write_u2(instance_fields_count);
  dump_instance_field_descriptors(writer, ik);

  writer->end_sub_record();

  // array classes
  k = ik->array_klass_or_null();
  while (k != NULL) {
    assert(k->is_objArray_klass(), "not an ObjArrayKlass");

    u4 size = 1 + sizeof(address) + 4 + 6 * sizeof(address) + 4 + 2 + 2 + 2;
    writer->start_sub_record(HPROF_GC_CLASS_DUMP, size);
    writer->write_classID(k);
    writer->write_u4(STACK_TRACE_ID);

    // super class of array classes is java.lang.Object
    java_super = k->java_super();
    assert(java_super != NULL, "checking");
    writer->write_classID(java_super);

    writer->write_objectID(ik->class_loader());
    writer->write_objectID(ik->signers());
    writer->write_objectID(ik->protection_domain());

    writer->write_objectID(oop(NULL));    // reserved
    writer->write_objectID(oop(NULL));
    writer->write_u4(0);             // instance size
    writer->write_u2(0);             // constant pool
    writer->write_u2(0);             // static fields
    writer->write_u2(0);             // instance fields

    writer->end_sub_record();

    // get the array class for the next rank
    k = k->array_klass_or_null();
  }
}

// creates HPROF_GC_CLASS_DUMP record for a given primitive array
// class (and each multi-dimensional array class too)
void DumperSupport::dump_basic_type_array_class(DumpWriter* writer, Klass* k) {
 // array classes
 while (k != NULL) {
    Klass* klass = k;

    u4 size = 1 + sizeof(address) + 4 + 6 * sizeof(address) + 4 + 2 + 2 + 2;
    writer->start_sub_record(HPROF_GC_CLASS_DUMP, size);
    writer->write_classID(klass);
    writer->write_u4(STACK_TRACE_ID);

    // super class of array classes is java.lang.Object
    InstanceKlass* java_super = klass->java_super();
    assert(java_super != NULL, "checking");
    writer->write_classID(java_super);

    writer->write_objectID(oop(NULL));    // loader
    writer->write_objectID(oop(NULL));    // signers
    writer->write_objectID(oop(NULL));    // protection domain

    writer->write_objectID(oop(NULL));    // reserved
    writer->write_objectID(oop(NULL));
    writer->write_u4(0);             // instance size
    writer->write_u2(0);             // constant pool
    writer->write_u2(0);             // static fields
    writer->write_u2(0);             // instance fields

    writer->end_sub_record();

    // get the array class for the next rank
    k = klass->array_klass_or_null();
  }
}

// Hprof uses an u4 as record length field,
// which means we need to truncate arrays that are too long.
int DumperSupport::calculate_array_max_length(DumpWriter* writer, arrayOop array, short header_size) {
  BasicType type = ArrayKlass::cast(array->klass())->element_type();
  assert(type >= T_BOOLEAN && type <= T_OBJECT, "invalid array element type");

  int length = array->length();

  int type_size;
  if (type == T_OBJECT) {
    type_size = sizeof(address);
  } else {
    type_size = type2aelembytes(type);
  }

  size_t length_in_bytes = (size_t)length * type_size;
  uint max_bytes = max_juint - header_size;

  if (length_in_bytes > max_bytes) {
    length = max_bytes / type_size;
    length_in_bytes = (size_t)length * type_size;

    warning("cannot dump array of type %s[] with length %d; truncating to length %d",
            type2name_tab[type], array->length(), length);
  }
  return length;
}

// creates HPROF_GC_OBJ_ARRAY_DUMP record for the given object array
void DumperSupport::dump_object_array(DumpWriter* writer, objArrayOop array) {
  // sizeof(u1) + 2 * sizeof(u4) + sizeof(objectID) + sizeof(classID)
  short header_size = 1 + 2 * 4 + 2 * sizeof(address);
  int length = calculate_array_max_length(writer, array, header_size);
  u4 size = header_size + length * sizeof(address);

  writer->start_sub_record(HPROF_GC_OBJ_ARRAY_DUMP, size);
  writer->write_objectID(array);
  writer->write_u4(STACK_TRACE_ID);
  writer->write_u4(length);

  // array class ID
  writer->write_classID(array->klass());

  // [id]* elements
  for (int index = 0; index < length; index++) {
    oop o = array->obj_at(index);
    if (o != NULL && log_is_enabled(Debug, cds, heap) && mask_dormant_archived_object(o) == NULL) {
      ResourceMark rm;
      log_debug(cds, heap)("skipped dormant archived object " INTPTR_FORMAT " (%s) referenced by " INTPTR_FORMAT " (%s)",
                           p2i(o), o->klass()->external_name(),
                           p2i(array), array->klass()->external_name());
    }
    o = mask_dormant_archived_object(o);
    writer->write_objectID(o);
  }

  writer->end_sub_record();
}

#define WRITE_ARRAY(Array, Type, Size, Length) \
  for (int i = 0; i < Length; i++) { writer->write_##Size((Size)Array->Type##_at(i)); }

// creates HPROF_GC_PRIM_ARRAY_DUMP record for the given type array
void DumperSupport::dump_prim_array(DumpWriter* writer, typeArrayOop array) {
  BasicType type = TypeArrayKlass::cast(array->klass())->element_type();

  // 2 * sizeof(u1) + 2 * sizeof(u4) + sizeof(objectID)
  short header_size = 2 * 1 + 2 * 4 + sizeof(address);

  int length = calculate_array_max_length(writer, array, header_size);
  int type_size = type2aelembytes(type);
  u4 length_in_bytes = (u4)length * type_size;
  u4 size = header_size + length_in_bytes;

  writer->start_sub_record(HPROF_GC_PRIM_ARRAY_DUMP, size);
  writer->write_objectID(array);
  writer->write_u4(STACK_TRACE_ID);
  writer->write_u4(length);
  writer->write_u1(type2tag(type));

  // nothing to copy
  if (length == 0) {
    writer->end_sub_record();
    return;
  }

  // If the byte ordering is big endian then we can copy most types directly

  switch (type) {
    case T_INT : {
      if (Endian::is_Java_byte_ordering_different()) {
        WRITE_ARRAY(array, int, u4, length);
      } else {
        writer->write_raw((void*)(array->int_at_addr(0)), length_in_bytes);
      }
      break;
    }
    case T_BYTE : {
      writer->write_raw((void*)(array->byte_at_addr(0)), length_in_bytes);
      break;
    }
    case T_CHAR : {
      if (Endian::is_Java_byte_ordering_different()) {
        WRITE_ARRAY(array, char, u2, length);
      } else {
        writer->write_raw((void*)(array->char_at_addr(0)), length_in_bytes);
      }
      break;
    }
    case T_SHORT : {
      if (Endian::is_Java_byte_ordering_different()) {
        WRITE_ARRAY(array, short, u2, length);
      } else {
        writer->write_raw((void*)(array->short_at_addr(0)), length_in_bytes);
      }
      break;
    }
    case T_BOOLEAN : {
      if (Endian::is_Java_byte_ordering_different()) {
        WRITE_ARRAY(array, bool, u1, length);
      } else {
        writer->write_raw((void*)(array->bool_at_addr(0)), length_in_bytes);
      }
      break;
    }
    case T_LONG : {
      if (Endian::is_Java_byte_ordering_different()) {
        WRITE_ARRAY(array, long, u8, length);
      } else {
        writer->write_raw((void*)(array->long_at_addr(0)), length_in_bytes);
      }
      break;
    }

    // handle float/doubles in a special value to ensure than NaNs are
    // written correctly. TO DO: Check if we can avoid this on processors that
    // use IEEE 754.

    case T_FLOAT : {
      for (int i = 0; i < length; i++) {
        dump_float(writer, array->float_at(i));
      }
      break;
    }
    case T_DOUBLE : {
      for (int i = 0; i < length; i++) {
        dump_double(writer, array->double_at(i));
      }
      break;
    }
    default : ShouldNotReachHere();
  }

  writer->end_sub_record();
}

// create a HPROF_FRAME record of the given Method* and bci
void DumperSupport::dump_stack_frame(DumpWriter* writer,
                                     int frame_serial_num,
                                     int class_serial_num,
                                     Method* m,
                                     int bci) {
  int line_number;
  if (m->is_native()) {
    line_number = -3;  // native frame
  } else {
    line_number = m->line_number_from_bci(bci);
  }

  write_header(writer, HPROF_FRAME, 4*oopSize + 2*sizeof(u4));
  writer->write_id(frame_serial_num);               // frame serial number
  writer->write_symbolID(m->name());                // method's name
  writer->write_symbolID(m->signature());           // method's signature

  assert(m->method_holder()->is_instance_klass(), "not InstanceKlass");
  writer->write_symbolID(m->method_holder()->source_file_name());  // source file name
  writer->write_u4(class_serial_num);               // class serial number
  writer->write_u4((u4) line_number);               // line number
}


// Support class used to generate HPROF_UTF8 records from the entries in the
// SymbolTable.

class SymbolTableDumper : public SymbolClosure {
 private:
  DumpWriter* _writer;
  DumpWriter* writer() const                { return _writer; }
 public:
  SymbolTableDumper(DumpWriter* writer)     { _writer = writer; }
  void do_symbol(Symbol** p);
};

void SymbolTableDumper::do_symbol(Symbol** p) {
  ResourceMark rm;
  Symbol* sym = load_symbol(p);
  int len = sym->utf8_length();
  if (len > 0) {
    char* s = sym->as_utf8();
    DumperSupport::write_header(writer(), HPROF_UTF8, oopSize + len);
    writer()->write_symbolID(sym);
    writer()->write_raw(s, len);
  }
}

// Support class used to generate HPROF_GC_ROOT_JNI_LOCAL records

class JNILocalsDumper : public OopClosure {
 private:
  DumpWriter* _writer;
  u4 _thread_serial_num;
  int _frame_num;
  DumpWriter* writer() const                { return _writer; }
 public:
  JNILocalsDumper(DumpWriter* writer, u4 thread_serial_num) {
    _writer = writer;
    _thread_serial_num = thread_serial_num;
    _frame_num = -1;  // default - empty stack
  }
  void set_frame_number(int n) { _frame_num = n; }
  void do_oop(oop* obj_p);
  void do_oop(narrowOop* obj_p) { ShouldNotReachHere(); }
};


void JNILocalsDumper::do_oop(oop* obj_p) {
  // ignore null handles
  oop o = *obj_p;
  if (o != NULL) {
    u4 size = 1 + sizeof(address) + 4 + 4;
    writer()->start_sub_record(HPROF_GC_ROOT_JNI_LOCAL, size);
    writer()->write_objectID(o);
    writer()->write_u4(_thread_serial_num);
    writer()->write_u4((u4)_frame_num);
    writer()->end_sub_record();
  }
}


// Support class used to generate HPROF_GC_ROOT_JNI_GLOBAL records

class JNIGlobalsDumper : public OopClosure {
 private:
  DumpWriter* _writer;
  DumpWriter* writer() const                { return _writer; }

 public:
  JNIGlobalsDumper(DumpWriter* writer) {
    _writer = writer;
  }
  void do_oop(oop* obj_p);
  void do_oop(narrowOop* obj_p) { ShouldNotReachHere(); }
};

void JNIGlobalsDumper::do_oop(oop* obj_p) {
  oop o = *obj_p;

  // ignore these
  if (o == NULL) return;

  // we ignore global ref to symbols and other internal objects
  if (o->is_instance() || o->is_objArray() || o->is_typeArray()) {
    u4 size = 1 + 2 * sizeof(address);
    writer()->start_sub_record(HPROF_GC_ROOT_JNI_GLOBAL, size);
    writer()->write_objectID(o);
    writer()->write_objectID((oopDesc*)obj_p);      // global ref ID
    writer()->end_sub_record();
  }
};

// Support class used to generate HPROF_GC_ROOT_STICKY_CLASS records

class StickyClassDumper : public KlassClosure {
 private:
  DumpWriter* _writer;
  DumpWriter* writer() const                { return _writer; }
 public:
  StickyClassDumper(DumpWriter* writer) {
    _writer = writer;
  }
  void do_klass(Klass* k) {
    if (k->is_instance_klass()) {
      InstanceKlass* ik = InstanceKlass::cast(k);
      u4 size = 1 + sizeof(address);
      writer()->start_sub_record(HPROF_GC_ROOT_STICKY_CLASS, size);
      writer()->write_classID(ik);
      writer()->end_sub_record();
    }
  }
};


class VM_HeapDumper;

// Support class using when iterating over the heap.

class HeapObjectDumper : public ObjectClosure {
 private:
  DumpWriter* _writer;

  DumpWriter* writer()                  { return _writer; }

 public:
  HeapObjectDumper(DumpWriter* writer) {
    _writer = writer;
  }

  // called for each object in the heap
  void do_object(oop o);
};

void HeapObjectDumper::do_object(oop o) {
  // skip classes as these emitted as HPROF_GC_CLASS_DUMP records
  if (o->klass() == vmClasses::Class_klass()) {
    if (!java_lang_Class::is_primitive(o)) {
      return;
    }
  }

  if (DumperSupport::mask_dormant_archived_object(o) == NULL) {
    log_debug(cds, heap)("skipped dormant archived object " INTPTR_FORMAT " (%s)", p2i(o), o->klass()->external_name());
    return;
  }

  if (o->is_instance()) {
    // create a HPROF_GC_INSTANCE record for each object
    DumperSupport::dump_instance(writer(), o);
  } else if (o->is_objArray()) {
    // create a HPROF_GC_OBJ_ARRAY_DUMP record for each object array
    DumperSupport::dump_object_array(writer(), objArrayOop(o));
  } else if (o->is_typeArray()) {
    // create a HPROF_GC_PRIM_ARRAY_DUMP record for each type array
    DumperSupport::dump_prim_array(writer(), typeArrayOop(o));
  }
}

// The VM operation that performs the heap dump
class VM_HeapDumper : public VM_GC_Operation, public AbstractGangTask {
 private:
  static VM_HeapDumper* _global_dumper;
  static DumpWriter*    _global_writer;
  DumpWriter*           _local_writer;
  JavaThread*           _oome_thread;
  Method*               _oome_constructor;
  bool _gc_before_heap_dump;
  GrowableArray<Klass*>* _klass_map;
  ThreadStackTrace** _stack_traces;
  int _num_threads;

  // accessors and setters
  static VM_HeapDumper* dumper()         {  assert(_global_dumper != NULL, "Error"); return _global_dumper; }
  static DumpWriter* writer()            {  assert(_global_writer != NULL, "Error"); return _global_writer; }
  void set_global_dumper() {
    assert(_global_dumper == NULL, "Error");
    _global_dumper = this;
  }
  void set_global_writer() {
    assert(_global_writer == NULL, "Error");
    _global_writer = _local_writer;
  }
  void clear_global_dumper() { _global_dumper = NULL; }
  void clear_global_writer() { _global_writer = NULL; }

  bool skip_operation() const;

  // writes a HPROF_LOAD_CLASS record
  class ClassesDo;
  static void do_load_class(Klass* k);

  // writes a HPROF_GC_CLASS_DUMP record for the given class
  // (and each array class too)
  static void do_class_dump(Klass* k);

  // writes a HPROF_GC_CLASS_DUMP records for a given basic type
  // array (and each multi-dimensional array too)
  static void do_basic_type_array_class_dump(Klass* k);

  // HPROF_GC_ROOT_THREAD_OBJ records
  int do_thread(JavaThread* thread, u4 thread_serial_num);
  void do_threads();

  void add_class_serial_number(Klass* k, int serial_num) {
    _klass_map->at_put_grow(serial_num, k);
  }

  // HPROF_TRACE and HPROF_FRAME records
  void dump_stack_traces();

 public:
  VM_HeapDumper(DumpWriter* writer, bool gc_before_heap_dump, bool oome) :
    VM_GC_Operation(0 /* total collections,      dummy, ignored */,
                    GCCause::_heap_dump /* GC Cause */,
                    0 /* total full collections, dummy, ignored */,
                    gc_before_heap_dump),
    AbstractGangTask("dump heap") {
    _local_writer = writer;
    _gc_before_heap_dump = gc_before_heap_dump;
    _klass_map = new (ResourceObj::C_HEAP, mtServiceability) GrowableArray<Klass*>(INITIAL_CLASS_COUNT, mtServiceability);
    _stack_traces = NULL;
    _num_threads = 0;
    if (oome) {
      assert(!Thread::current()->is_VM_thread(), "Dump from OutOfMemoryError cannot be called by the VMThread");
      // get OutOfMemoryError zero-parameter constructor
      InstanceKlass* oome_ik = vmClasses::OutOfMemoryError_klass();
      _oome_constructor = oome_ik->find_method(vmSymbols::object_initializer_name(),
                                                          vmSymbols::void_method_signature());
      // get thread throwing OOME when generating the heap dump at OOME
      _oome_thread = JavaThread::current();
    } else {
      _oome_thread = NULL;
      _oome_constructor = NULL;
    }
  }
  ~VM_HeapDumper() {
    if (_stack_traces != NULL) {
      for (int i=0; i < _num_threads; i++) {
        delete _stack_traces[i];
      }
      FREE_C_HEAP_ARRAY(ThreadStackTrace*, _stack_traces);
    }
    delete _klass_map;
  }

  VMOp_Type type() const { return VMOp_HeapDumper; }
  void doit();
  void work(uint worker_id);
};


VM_HeapDumper* VM_HeapDumper::_global_dumper = NULL;
DumpWriter*    VM_HeapDumper::_global_writer = NULL;

bool VM_HeapDumper::skip_operation() const {
  return false;
}

// fixes up the current dump record and writes HPROF_HEAP_DUMP_END record
void DumperSupport::end_of_dump(DumpWriter* writer) {
  writer->finish_dump_segment();

  writer->write_u1(HPROF_HEAP_DUMP_END);
  writer->write_u4(0);
  writer->write_u4(0);
}

// writes a HPROF_LOAD_CLASS record for the class (and each of its
// array classes)
void VM_HeapDumper::do_load_class(Klass* k) {
  static u4 class_serial_num = 0;

  // len of HPROF_LOAD_CLASS record
  u4 remaining = 2*oopSize + 2*sizeof(u4);

  // write a HPROF_LOAD_CLASS for the class and each array class
  do {
    DumperSupport::write_header(writer(), HPROF_LOAD_CLASS, remaining);

    // class serial number is just a number
    writer()->write_u4(++class_serial_num);

    // class ID
    Klass* klass = k;
    writer()->write_classID(klass);

    // add the Klass* and class serial number pair
    dumper()->add_class_serial_number(klass, class_serial_num);

    writer()->write_u4(STACK_TRACE_ID);

    // class name ID
    Symbol* name = klass->name();
    writer()->write_symbolID(name);

    // write a LOAD_CLASS record for the array type (if it exists)
    k = klass->array_klass_or_null();
  } while (k != NULL);
}

// writes a HPROF_GC_CLASS_DUMP record for the given class
void VM_HeapDumper::do_class_dump(Klass* k) {
  if (k->is_instance_klass()) {
    DumperSupport::dump_class_and_array_classes(writer(), k);
  }
}

// writes a HPROF_GC_CLASS_DUMP records for a given basic type
// array (and each multi-dimensional array too)
void VM_HeapDumper::do_basic_type_array_class_dump(Klass* k) {
  DumperSupport::dump_basic_type_array_class(writer(), k);
}

// Walk the stack of the given thread.
// Dumps a HPROF_GC_ROOT_JAVA_FRAME record for each local
// Dumps a HPROF_GC_ROOT_JNI_LOCAL record for each JNI local
//
// It returns the number of Java frames in this thread stack
int VM_HeapDumper::do_thread(JavaThread* java_thread, u4 thread_serial_num) {
  JNILocalsDumper blk(writer(), thread_serial_num);

  oop threadObj = java_thread->threadObj();
  assert(threadObj != NULL, "sanity check");

  int stack_depth = 0;
  if (java_thread->has_last_Java_frame()) {

    // vframes are resource allocated
    Thread* current_thread = Thread::current();
    ResourceMark rm(current_thread);
    HandleMark hm(current_thread);

    RegisterMap reg_map(java_thread);
    frame f = java_thread->last_frame();
    vframe* vf = vframe::new_vframe(&f, &reg_map, java_thread);
    frame* last_entry_frame = NULL;
    int extra_frames = 0;

    if (java_thread == _oome_thread && _oome_constructor != NULL) {
      extra_frames++;
    }
    while (vf != NULL) {
      blk.set_frame_number(stack_depth);
      if (vf->is_java_frame()) {

        // java frame (interpreted, compiled, ...)
        javaVFrame *jvf = javaVFrame::cast(vf);
        if (!(jvf->method()->is_native())) {
          StackValueCollection* locals = jvf->locals();
          for (int slot=0; slot<locals->size(); slot++) {
            if (locals->at(slot)->type() == T_OBJECT) {
              oop o = locals->obj_at(slot)();

              if (o != NULL) {
                u4 size = 1 + sizeof(address) + 4 + 4;
                writer()->start_sub_record(HPROF_GC_ROOT_JAVA_FRAME, size);
                writer()->write_objectID(o);
                writer()->write_u4(thread_serial_num);
                writer()->write_u4((u4) (stack_depth + extra_frames));
                writer()->end_sub_record();
              }
            }
          }
          StackValueCollection *exprs = jvf->expressions();
          for(int index = 0; index < exprs->size(); index++) {
            if (exprs->at(index)->type() == T_OBJECT) {
               oop o = exprs->obj_at(index)();
               if (o != NULL) {
                 u4 size = 1 + sizeof(address) + 4 + 4;
                 writer()->start_sub_record(HPROF_GC_ROOT_JAVA_FRAME, size);
                 writer()->write_objectID(o);
                 writer()->write_u4(thread_serial_num);
                 writer()->write_u4((u4) (stack_depth + extra_frames));
                 writer()->end_sub_record();
               }
             }
          }
        } else {
          // native frame
          if (stack_depth == 0) {
            // JNI locals for the top frame.
            java_thread->active_handles()->oops_do(&blk);
          } else {
            if (last_entry_frame != NULL) {
              // JNI locals for the entry frame
              assert(last_entry_frame->is_entry_frame(), "checking");
              last_entry_frame->entry_frame_call_wrapper()->handles()->oops_do(&blk);
            }
          }
        }
        // increment only for Java frames
        stack_depth++;
        last_entry_frame = NULL;

      } else {
        // externalVFrame - if it's an entry frame then report any JNI locals
        // as roots when we find the corresponding native javaVFrame
        frame* fr = vf->frame_pointer();
        assert(fr != NULL, "sanity check");
        if (fr->is_entry_frame()) {
          last_entry_frame = fr;
        }
      }
      vf = vf->sender();
    }
  } else {
    // no last java frame but there may be JNI locals
    java_thread->active_handles()->oops_do(&blk);
  }
  return stack_depth;
}


// write a HPROF_GC_ROOT_THREAD_OBJ record for each java thread. Then walk
// the stack so that locals and JNI locals are dumped.
void VM_HeapDumper::do_threads() {
  for (int i=0; i < _num_threads; i++) {
    JavaThread* thread = _stack_traces[i]->thread();
    oop threadObj = thread->threadObj();
    u4 thread_serial_num = i+1;
    u4 stack_serial_num = thread_serial_num + STACK_TRACE_ID;
    u4 size = 1 + sizeof(address) + 4 + 4;
    writer()->start_sub_record(HPROF_GC_ROOT_THREAD_OBJ, size);
    writer()->write_objectID(threadObj);
    writer()->write_u4(thread_serial_num);  // thread number
    writer()->write_u4(stack_serial_num);   // stack trace serial number
    writer()->end_sub_record();
    int num_frames = do_thread(thread, thread_serial_num);
    assert(num_frames == _stack_traces[i]->get_stack_depth(),
           "total number of Java frames not matched");
  }
}


// The VM operation that dumps the heap. The dump consists of the following
// records:
//
//  HPROF_HEADER
//  [HPROF_UTF8]*
//  [HPROF_LOAD_CLASS]*
//  [[HPROF_FRAME]*|HPROF_TRACE]*
//  [HPROF_GC_CLASS_DUMP]*
//  [HPROF_HEAP_DUMP_SEGMENT]*
//  HPROF_HEAP_DUMP_END
//
// The HPROF_TRACE records represent the stack traces where the heap dump
// is generated and a "dummy trace" record which does not include
// any frames. The dummy trace record is used to be referenced as the
// unknown object alloc site.
//
// Each HPROF_HEAP_DUMP_SEGMENT record has a length followed by sub-records.
// To allow the heap dump be generated in a single pass we remember the position
// of the dump length and fix it up after all sub-records have been written.
// To generate the sub-records we iterate over the heap, writing
// HPROF_GC_INSTANCE_DUMP, HPROF_GC_OBJ_ARRAY_DUMP, and HPROF_GC_PRIM_ARRAY_DUMP
// records as we go. Once that is done we write records for some of the GC
// roots.

void VM_HeapDumper::doit() {

  CollectedHeap* ch = Universe::heap();

  ch->ensure_parsability(false); // must happen, even if collection does
                                 // not happen (e.g. due to GCLocker)

  if (_gc_before_heap_dump) {
    if (GCLocker::is_active()) {
      warning("GC locker is held; pre-heapdump GC was skipped");
    } else {
      ch->collect_as_vm_thread(GCCause::_heap_dump);
    }
  }

  // At this point we should be the only dumper active, so
  // the following should be safe.
  set_global_dumper();
  set_global_writer();

  WorkGang* gang = ch->safepoint_workers();

  if (gang == NULL) {
    work(0);
  } else {
    gang->run_task(this, gang->active_workers(), true);
  }

  // Now we clear the global variables, so that a future dumper can run.
  clear_global_dumper();
  clear_global_writer();
}

void VM_HeapDumper::work(uint worker_id) {
  if (!Thread::current()->is_VM_thread()) {
    writer()->writer_loop();
    return;
  }

  // Write the file header - we always use 1.0.2
  const char* header = "JAVA PROFILE 1.0.2";

  // header is few bytes long - no chance to overflow int
  writer()->write_raw((void*)header, (int)strlen(header));
  writer()->write_u1(0); // terminator
  writer()->write_u4(oopSize);
  // timestamp is current time in ms
  writer()->write_u8(os::javaTimeMillis());

  // HPROF_UTF8 records
  SymbolTableDumper sym_dumper(writer());
  SymbolTable::symbols_do(&sym_dumper);

  // write HPROF_LOAD_CLASS records
  {
    LockedClassesDo locked_load_classes(&do_load_class);
    ClassLoaderDataGraph::classes_do(&locked_load_classes);
  }
  Universe::basic_type_classes_do(&do_load_class);

  // write HPROF_FRAME and HPROF_TRACE records
  // this must be called after _klass_map is built when iterating the classes above.
  dump_stack_traces();

  // Writes HPROF_GC_CLASS_DUMP records
  {
    LockedClassesDo locked_dump_class(&do_class_dump);
    ClassLoaderDataGraph::classes_do(&locked_dump_class);
  }
  Universe::basic_type_classes_do(&do_basic_type_array_class_dump);

  // writes HPROF_GC_INSTANCE_DUMP records.
  // After each sub-record is written check_segment_length will be invoked
  // to check if the current segment exceeds a threshold. If so, a new
  // segment is started.
  // The HPROF_GC_CLASS_DUMP and HPROF_GC_INSTANCE_DUMP are the vast bulk
  // of the heap dump.
  HeapObjectDumper obj_dumper(writer());
  Universe::heap()->object_iterate(&obj_dumper);

  // HPROF_GC_ROOT_THREAD_OBJ + frames + jni locals
  do_threads();

  // HPROF_GC_ROOT_JNI_GLOBAL
  JNIGlobalsDumper jni_dumper(writer());
  JNIHandles::oops_do(&jni_dumper);
  // technically not jni roots, but global roots
  // for things like preallocated throwable backtraces
  Universe::vm_global()->oops_do(&jni_dumper);

  // HPROF_GC_ROOT_STICKY_CLASS
  // These should be classes in the NULL class loader data, and not all classes
  // if !ClassUnloading
  StickyClassDumper class_dumper(writer());
  ClassLoaderData::the_null_class_loader_data()->classes_do(&class_dumper);

  // Writes the HPROF_HEAP_DUMP_END record.
  DumperSupport::end_of_dump(writer());

  // We are done with writing. Release the worker threads.
  writer()->deactivate();
}

void VM_HeapDumper::dump_stack_traces() {
  // write a HPROF_TRACE record without any frames to be referenced as object alloc sites
  DumperSupport::write_header(writer(), HPROF_TRACE, 3*sizeof(u4));
  writer()->write_u4((u4) STACK_TRACE_ID);
  writer()->write_u4(0);                    // thread number
  writer()->write_u4(0);                    // frame count

  _stack_traces = NEW_C_HEAP_ARRAY(ThreadStackTrace*, Threads::number_of_threads(), mtInternal);
  int frame_serial_num = 0;
  for (JavaThreadIteratorWithHandle jtiwh; JavaThread *thread = jtiwh.next(); ) {
    oop threadObj = thread->threadObj();
    if (threadObj != NULL && !thread->is_exiting() && !thread->is_hidden_from_external_view()) {
      // dump thread stack trace
      ResourceMark rm;
      ThreadStackTrace* stack_trace = new ThreadStackTrace(thread, false);
      stack_trace->dump_stack_at_safepoint(-1);
      _stack_traces[_num_threads++] = stack_trace;

      // write HPROF_FRAME records for this thread's stack trace
      int depth = stack_trace->get_stack_depth();
      int thread_frame_start = frame_serial_num;
      int extra_frames = 0;
      // write fake frame that makes it look like the thread, which caused OOME,
      // is in the OutOfMemoryError zero-parameter constructor
      if (thread == _oome_thread && _oome_constructor != NULL) {
        int oome_serial_num = _klass_map->find(_oome_constructor->method_holder());
        // the class serial number starts from 1
        assert(oome_serial_num > 0, "OutOfMemoryError class not found");
        DumperSupport::dump_stack_frame(writer(), ++frame_serial_num, oome_serial_num,
                                        _oome_constructor, 0);
        extra_frames++;
      }
      for (int j=0; j < depth; j++) {
        StackFrameInfo* frame = stack_trace->stack_frame_at(j);
        Method* m = frame->method();
        int class_serial_num = _klass_map->find(m->method_holder());
        // the class serial number starts from 1
        assert(class_serial_num > 0, "class not found");
        DumperSupport::dump_stack_frame(writer(), ++frame_serial_num, class_serial_num, m, frame->bci());
      }
      depth += extra_frames;

      // write HPROF_TRACE record for one thread
      DumperSupport::write_header(writer(), HPROF_TRACE, 3*sizeof(u4) + depth*oopSize);
      int stack_serial_num = _num_threads + STACK_TRACE_ID;
      writer()->write_u4(stack_serial_num);      // stack trace serial number
      writer()->write_u4((u4) _num_threads);     // thread serial number
      writer()->write_u4(depth);                 // frame count
      for (int j=1; j <= depth; j++) {
        writer()->write_id(thread_frame_start + j);
      }
    }
  }
}

// dump the heap to given path.
int HeapDumper::dump(const char* path, outputStream* out, int compression, bool overwrite) {
  assert(path != NULL && strlen(path) > 0, "path missing");

  // print message in interactive case
  if (out != NULL) {
    out->print_cr("Dumping heap to %s ...", path);
    timer()->start();
  }

  // create JFR event
  EventHeapDump event;

  AbstractCompressor* compressor = NULL;

  if (compression > 0) {
    compressor = new (std::nothrow) GZipCompressor(compression);

    if (compressor == NULL) {
      set_error("Could not allocate gzip compressor");
      return -1;
    }
  }

  DumpWriter writer(new (std::nothrow) FileWriter(path, overwrite), compressor);

  if (writer.error() != NULL) {
    set_error(writer.error());
    if (out != NULL) {
      out->print_cr("Unable to create %s: %s", path,
        (error() != NULL) ? error() : "reason unknown");
    }
    return -1;
  }

  // generate the dump
  VM_HeapDumper dumper(&writer, _gc_before_heap_dump, _oome);
  if (Thread::current()->is_VM_thread()) {
    assert(SafepointSynchronize::is_at_safepoint(), "Expected to be called at a safepoint");
    dumper.doit();
  } else {
    VMThread::execute(&dumper);
  }

  // record any error that the writer may have encountered
  set_error(writer.error());

  // emit JFR event
  if (error() == NULL) {
    event.set_destination(path);
    event.set_gcBeforeDump(_gc_before_heap_dump);
    event.set_size(writer.bytes_written());
    event.set_onOutOfMemoryError(_oome);
    event.commit();
  }

  // print message in interactive case
  if (out != NULL) {
    timer()->stop();
    if (error() == NULL) {
      out->print_cr("Heap dump file created [" JULONG_FORMAT " bytes in %3.3f secs]",
                    writer.bytes_written(), timer()->seconds());
    } else {
      out->print_cr("Dump file is incomplete: %s", writer.error());
    }
  }

  return (writer.error() == NULL) ? 0 : -1;
}

// stop timer (if still active), and free any error string we might be holding
HeapDumper::~HeapDumper() {
  if (timer()->is_active()) {
    timer()->stop();
  }
  set_error(NULL);
}


// returns the error string (resource allocated), or NULL
char* HeapDumper::error_as_C_string() const {
  if (error() != NULL) {
    char* str = NEW_RESOURCE_ARRAY(char, strlen(error())+1);
    strcpy(str, error());
    return str;
  } else {
    return NULL;
  }
}

// set the error string
void HeapDumper::set_error(char const* error) {
  if (_error != NULL) {
    os::free(_error);
  }
  if (error == NULL) {
    _error = NULL;
  } else {
    _error = os::strdup(error);
    assert(_error != NULL, "allocation failure");
  }
}

// Called by out-of-memory error reporting by a single Java thread
// outside of a JVM safepoint
void HeapDumper::dump_heap_from_oome() {
  HeapDumper::dump_heap(true);
}

// Called by error reporting by a single Java thread outside of a JVM safepoint,
// or by heap dumping by the VM thread during a (GC) safepoint. Thus, these various
// callers are strictly serialized and guaranteed not to interfere below. For more
// general use, however, this method will need modification to prevent
// inteference when updating the static variables base_path and dump_file_seq below.
void HeapDumper::dump_heap() {
  HeapDumper::dump_heap(false);
}

void HeapDumper::dump_heap(bool oome) {
  static char base_path[JVM_MAXPATHLEN] = {'\0'};
  static uint dump_file_seq = 0;
  char* my_path;
  const int max_digit_chars = 20;

  const char* dump_file_name = "java_pid";
  const char* dump_file_ext  = HeapDumpGzipLevel > 0 ? ".hprof.gz" : ".hprof";

  // The dump file defaults to java_pid<pid>.hprof in the current working
  // directory. HeapDumpPath=<file> can be used to specify an alternative
  // dump file name or a directory where dump file is created.
  if (dump_file_seq == 0) { // first time in, we initialize base_path
    // Calculate potentially longest base path and check if we have enough
    // allocated statically.
    const size_t total_length =
                      (HeapDumpPath == NULL ? 0 : strlen(HeapDumpPath)) +
                      strlen(os::file_separator()) + max_digit_chars +
                      strlen(dump_file_name) + strlen(dump_file_ext) + 1;
    if (total_length > sizeof(base_path)) {
      warning("Cannot create heap dump file.  HeapDumpPath is too long.");
      return;
    }

    bool use_default_filename = true;
    if (HeapDumpPath == NULL || HeapDumpPath[0] == '\0') {
      // HeapDumpPath=<file> not specified
    } else {
      strcpy(base_path, HeapDumpPath);
      // check if the path is a directory (must exist)
      DIR* dir = os::opendir(base_path);
      if (dir == NULL) {
        use_default_filename = false;
      } else {
        // HeapDumpPath specified a directory. We append a file separator
        // (if needed).
        os::closedir(dir);
        size_t fs_len = strlen(os::file_separator());
        if (strlen(base_path) >= fs_len) {
          char* end = base_path;
          end += (strlen(base_path) - fs_len);
          if (strcmp(end, os::file_separator()) != 0) {
            strcat(base_path, os::file_separator());
          }
        }
      }
    }
    // If HeapDumpPath wasn't a file name then we append the default name
    if (use_default_filename) {
      const size_t dlen = strlen(base_path);  // if heap dump dir specified
      jio_snprintf(&base_path[dlen], sizeof(base_path)-dlen, "%s%d%s",
                   dump_file_name, os::current_process_id(), dump_file_ext);
    }
    const size_t len = strlen(base_path) + 1;
    my_path = (char*)os::malloc(len, mtInternal);
    if (my_path == NULL) {
      warning("Cannot create heap dump file.  Out of system memory.");
      return;
    }
    strncpy(my_path, base_path, len);
  } else {
    // Append a sequence number id for dumps following the first
    const size_t len = strlen(base_path) + max_digit_chars + 2; // for '.' and \0
    my_path = (char*)os::malloc(len, mtInternal);
    if (my_path == NULL) {
      warning("Cannot create heap dump file.  Out of system memory.");
      return;
    }
    jio_snprintf(my_path, len, "%s.%d", base_path, dump_file_seq);
  }
  dump_file_seq++;   // increment seq number for next time we dump

  HeapDumper dumper(false /* no GC before heap dump */,
                    oome  /* pass along out-of-memory-error flag */);
  dumper.dump(my_path, tty, HeapDumpGzipLevel);
  os::free(my_path);
}
