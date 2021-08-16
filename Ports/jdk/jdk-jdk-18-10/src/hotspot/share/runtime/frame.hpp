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

#ifndef SHARE_RUNTIME_FRAME_HPP
#define SHARE_RUNTIME_FRAME_HPP

#include "code/vmregTypes.hpp"
#include "runtime/basicLock.hpp"
#include "runtime/monitorChunk.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/macros.hpp"
#ifdef ZERO
# include "stack_zero.hpp"
#endif

typedef class BytecodeInterpreter* interpreterState;

class CodeBlob;
class CompiledMethod;
class FrameValues;
class vframeArray;
class JavaCallWrapper;
class Method;
class methodHandle;
class RegisterMap;

enum class DerivedPointerIterationMode {
  _with_table,
  _directly,
  _ignore
};

// A frame represents a physical stack frame (an activation).  Frames
// can be C or Java frames, and the Java frames can be interpreted or
// compiled.  In contrast, vframes represent source-level activations,
// so that one physical frame can correspond to multiple source level
// frames because of inlining.

class frame {
 private:
  // Instance variables:
  intptr_t* _sp; // stack pointer (from Thread::last_Java_sp)
  address   _pc; // program counter (the next instruction after the call)

  CodeBlob* _cb; // CodeBlob that "owns" pc
  enum deopt_state {
    not_deoptimized,
    is_deoptimized,
    unknown
  };

  deopt_state _deopt_state;

 public:
  // Constructors
  frame();

#ifndef PRODUCT
  // This is a generic constructor which is only used by pns() in debug.cpp.
  // pns (i.e. print native stack) uses this constructor to create a starting
  // frame for stack walking. The implementation of this constructor is platform
  // dependent (i.e. SPARC doesn't need an 'fp' argument an will ignore it) but
  // we want to keep the signature generic because pns() is shared code.
  frame(void* sp, void* fp, void* pc);
#endif

  // Accessors

  // pc: Returns the pc at which this frame will continue normally.
  // It must point at the beginning of the next instruction to execute.
  address pc() const             { return _pc; }

  // This returns the pc that if you were in the debugger you'd see. Not
  // the idealized value in the frame object. This undoes the magic conversion
  // that happens for deoptimized frames. In addition it makes the value the
  // hardware would want to see in the native frame. The only user (at this point)
  // is deoptimization. It likely no one else should ever use it.
  address raw_pc() const;

  void set_pc( address   newpc );

  intptr_t* sp() const           { return _sp; }
  void set_sp( intptr_t* newsp ) { _sp = newsp; }


  CodeBlob* cb() const           { return _cb; }

  // patching operations
  void   patch_pc(Thread* thread, address pc);

  // Every frame needs to return a unique id which distinguishes it from all other frames.
  // For sparc and ia32 use sp. ia64 can have memory frames that are empty so multiple frames
  // will have identical sp values. For ia64 the bsp (fp) value will serve. No real frame
  // should have an id() of NULL so it is a distinguishing value for an unmatchable frame.
  // We also have relationals which allow comparing a frame to anoth frame's id() allow
  // us to distinguish younger (more recent activation) from older (less recent activations)
  // A NULL id is only valid when comparing for equality.

  intptr_t* id(void) const;
  bool is_younger(intptr_t* id) const;
  bool is_older(intptr_t* id) const;

  // testers

  // Compares for strict equality. Rarely used or needed.
  // It can return a different result than f1.id() == f2.id()
  bool equal(frame other) const;

  // type testers
  bool is_interpreted_frame()    const;
  bool is_java_frame()           const;
  bool is_entry_frame()          const;             // Java frame called from C?
  bool is_stub_frame()           const;
  bool is_ignored_frame()        const;
  bool is_native_frame()         const;
  bool is_runtime_frame()        const;
  bool is_compiled_frame()       const;
  bool is_safepoint_blob_frame() const;
  bool is_deoptimized_frame()    const;
  bool is_optimized_entry_frame()         const;

  // testers
  bool is_first_frame() const; // oldest frame? (has no sender)
  bool is_first_java_frame() const;              // same for Java frame

  bool is_interpreted_frame_valid(JavaThread* thread) const;       // performs sanity checks on interpreted frames.

  // tells whether this frame is marked for deoptimization
  bool should_be_deoptimized() const;

  // tells whether this frame can be deoptimized
  bool can_be_deoptimized() const;

  // returns the frame size in stack slots
  int frame_size(RegisterMap* map) const;

  // returns the sending frame
  frame sender(RegisterMap* map) const;

  bool safe_for_sender(JavaThread *thread);

  // returns the sender, but skips conversion frames
  frame real_sender(RegisterMap* map) const;

  // returns the the sending Java frame, skipping any intermediate C frames
  // NB: receiver must not be first frame
  frame java_sender() const;

 private:
  // Helper methods for better factored code in frame::sender
  frame sender_for_compiled_frame(RegisterMap* map) const;
  frame sender_for_entry_frame(RegisterMap* map) const;
  frame sender_for_interpreter_frame(RegisterMap* map) const;
  frame sender_for_native_frame(RegisterMap* map) const;
  frame sender_for_optimized_entry_frame(RegisterMap* map) const;

  bool is_entry_frame_valid(JavaThread* thread) const;

  // All frames:

  // A low-level interface for vframes:

 public:

  intptr_t* addr_at(int index) const             { return &fp()[index];    }
  intptr_t  at(int index) const                  { return *addr_at(index); }

  // accessors for locals
  oop obj_at(int offset) const                   { return *obj_at_addr(offset);  }
  void obj_at_put(int offset, oop value)         { *obj_at_addr(offset) = value; }

  jint int_at(int offset) const                  { return *int_at_addr(offset);  }
  void int_at_put(int offset, jint value)        { *int_at_addr(offset) = value; }

  oop*      obj_at_addr(int offset) const        { return (oop*)     addr_at(offset); }

  oop*      adjusted_obj_at_addr(Method* method, int index) { return obj_at_addr(adjust_offset(method, index)); }

 private:
  jint*    int_at_addr(int offset) const         { return (jint*)    addr_at(offset); }

 public:
  // Link (i.e., the pointer to the previous frame)
  intptr_t* link() const;

  // Return address
  address  sender_pc() const;

  // Support for deoptimization
  void deoptimize(JavaThread* thread);

  // The frame's original SP, before any extension by an interpreted callee;
  // used for packing debug info into vframeArray objects and vframeArray lookup.
  intptr_t* unextended_sp() const;

  // returns the stack pointer of the calling frame
  intptr_t* sender_sp() const;

  // Returns the real 'frame pointer' for the current frame.
  // This is the value expected by the platform ABI when it defines a
  // frame pointer register. It may differ from the effective value of
  // the FP register when that register is used in the JVM for other
  // purposes (like compiled frames on some platforms).
  // On other platforms, it is defined so that the stack area used by
  // this frame goes from real_fp() to sp().
  intptr_t* real_fp() const;

  // Deoptimization info, if needed (platform dependent).
  // Stored in the initial_info field of the unroll info, to be used by
  // the platform dependent deoptimization blobs.
  intptr_t *initial_deoptimization_info();

  // Interpreter frames:

 private:
  intptr_t** interpreter_frame_locals_addr() const;
  intptr_t*  interpreter_frame_bcp_addr() const;
  intptr_t*  interpreter_frame_mdp_addr() const;

 public:
  // Locals

  // The _at version returns a pointer because the address is used for GC.
  intptr_t* interpreter_frame_local_at(int index) const;

  void interpreter_frame_set_locals(intptr_t* locs);

  // byte code index
  jint interpreter_frame_bci() const;

  // byte code pointer
  address interpreter_frame_bcp() const;
  void    interpreter_frame_set_bcp(address bcp);

  // method data pointer
  address interpreter_frame_mdp() const;
  void    interpreter_frame_set_mdp(address dp);

  // Find receiver out of caller's (compiled) argument list
  oop retrieve_receiver(RegisterMap *reg_map);

  // Return the monitor owner and BasicLock for compiled synchronized
  // native methods. Used by JVMTI's GetLocalInstance method
  // (via VM_GetReceiver) to retrieve the receiver from a native wrapper frame.
  BasicLock* get_native_monitor();
  oop        get_native_receiver();

  // Find receiver for an invoke when arguments are just pushed on stack (i.e., callee stack-frame is
  // not setup)
  oop interpreter_callee_receiver(Symbol* signature)     { return *interpreter_callee_receiver_addr(signature); }


  oop* interpreter_callee_receiver_addr(Symbol* signature);


  // expression stack (may go up or down, direction == 1 or -1)
 public:
  intptr_t* interpreter_frame_expression_stack() const;

  // The _at version returns a pointer because the address is used for GC.
  intptr_t* interpreter_frame_expression_stack_at(jint offset) const;

  // top of expression stack
  intptr_t* interpreter_frame_tos_at(jint offset) const;
  intptr_t* interpreter_frame_tos_address() const;


  jint  interpreter_frame_expression_stack_size() const;

  intptr_t* interpreter_frame_sender_sp() const;

  // template based interpreter deoptimization support
  void  set_interpreter_frame_sender_sp(intptr_t* sender_sp);
  void interpreter_frame_set_monitor_end(BasicObjectLock* value);

  // Address of the temp oop in the frame. Needed as GC root.
  oop* interpreter_frame_temp_oop_addr() const;

  // BasicObjectLocks:
  //
  // interpreter_frame_monitor_begin is higher in memory than interpreter_frame_monitor_end
  // Interpreter_frame_monitor_begin points to one element beyond the oldest one,
  // interpreter_frame_monitor_end   points to the youngest one, or if there are none,
  //                                 it points to one beyond where the first element will be.
  // interpreter_frame_monitor_size  reports the allocation size of a monitor in the interpreter stack.
  //                                 this value is >= BasicObjectLock::size(), and may be rounded up

  BasicObjectLock* interpreter_frame_monitor_begin() const;
  BasicObjectLock* interpreter_frame_monitor_end()   const;
  BasicObjectLock* next_monitor_in_interpreter_frame(BasicObjectLock* current) const;
  BasicObjectLock* previous_monitor_in_interpreter_frame(BasicObjectLock* current) const;
  static int interpreter_frame_monitor_size();

  void interpreter_frame_verify_monitor(BasicObjectLock* value) const;

  // Return/result value from this interpreter frame
  // If the method return type is T_OBJECT or T_ARRAY populates oop_result
  // For other (non-T_VOID) the appropriate field in the jvalue is populated
  // with the result value.
  // Should only be called when at method exit when the method is not
  // exiting due to an exception.
  BasicType interpreter_frame_result(oop* oop_result, jvalue* value_result);

 public:
  // Method & constant pool cache
  Method* interpreter_frame_method() const;
  void interpreter_frame_set_method(Method* method);
  Method** interpreter_frame_method_addr() const;
  ConstantPoolCache** interpreter_frame_cache_addr() const;
  oop* interpreter_frame_mirror_addr() const;

  void interpreter_frame_set_mirror(oop mirror);

 public:
  // Entry frames
  JavaCallWrapper* entry_frame_call_wrapper() const { return *entry_frame_call_wrapper_addr(); }
  JavaCallWrapper* entry_frame_call_wrapper_if_safe(JavaThread* thread) const;
  JavaCallWrapper** entry_frame_call_wrapper_addr() const;
  intptr_t* entry_frame_argument_at(int offset) const;

  // tells whether there is another chunk of Delta stack above
  bool entry_frame_is_first() const;
  bool optimized_entry_frame_is_first() const;

  // Safepoints

 public:
  oop saved_oop_result(RegisterMap* map) const;
  void set_saved_oop_result(RegisterMap* map, oop obj);

  // For debugging
 private:
  const char* print_name() const;

  void describe_pd(FrameValues& values, int frame_no);

 public:
  void print_value() const { print_value_on(tty,NULL); }
  void print_value_on(outputStream* st, JavaThread *thread) const;
  void print_on(outputStream* st) const;
  void interpreter_frame_print_on(outputStream* st) const;
  void print_on_error(outputStream* st, char* buf, int buflen, bool verbose = false) const;
  static void print_C_frame(outputStream* st, char* buf, int buflen, address pc);

  // Add annotated descriptions of memory locations belonging to this frame to values
  void describe(FrameValues& values, int frame_no);

  // Conversion from a VMReg to physical stack location
  address oopmapreg_to_location(VMReg reg, const RegisterMap* reg_map) const;
  oop* oopmapreg_to_oop_location(VMReg reg, const RegisterMap* reg_map) const;

  // Oops-do's
  void oops_compiled_arguments_do(Symbol* signature, bool has_receiver, bool has_appendix, const RegisterMap* reg_map, OopClosure* f) const;
  void oops_interpreted_do(OopClosure* f, const RegisterMap* map, bool query_oop_map_cache = true) const;

 private:
  void oops_interpreted_arguments_do(Symbol* signature, bool has_receiver, OopClosure* f) const;

  // Iteration of oops
  void oops_do_internal(OopClosure* f, CodeBlobClosure* cf, const RegisterMap* map,
                        bool use_interpreter_oop_map_cache, DerivedPointerIterationMode derived_mode) const;
  void oops_entry_do(OopClosure* f, const RegisterMap* map) const;
  void oops_code_blob_do(OopClosure* f, CodeBlobClosure* cf, const RegisterMap* map,
                         DerivedPointerIterationMode derived_mode) const;
  int adjust_offset(Method* method, int index); // helper for above fn
 public:
  // Memory management
  void oops_do(OopClosure* f, CodeBlobClosure* cf, const RegisterMap* map,
               DerivedPointerIterationMode derived_mode) const;
  void oops_do(OopClosure* f, CodeBlobClosure* cf, const RegisterMap* map) const;
  void nmethods_do(CodeBlobClosure* cf) const;

  // RedefineClasses support for finding live interpreted methods on the stack
  void metadata_do(MetadataClosure* f) const;

  // Verification
  void verify(const RegisterMap* map) const;
  static bool verify_return_pc(address x);
  // Usage:
  // assert(frame::verify_return_pc(return_address), "must be a return pc");

  NOT_PRODUCT(void pd_ps();)  // platform dependent frame printing

#include CPU_HEADER(frame)

};

#ifndef PRODUCT
// A simple class to describe a location on the stack
class FrameValue {
 public:
  intptr_t* location;
  char* description;
  int owner;
  int priority;

  FrameValue() {
    location = NULL;
    description = NULL;
    owner = -1;
    priority = 0;
  }

};


// A collection of described stack values that can print a symbolic
// description of the stack memory.  Interpreter frame values can be
// in the caller frames so all the values are collected first and then
// sorted before being printed.
class FrameValues {
 private:
  GrowableArray<FrameValue> _values;

  static int compare(FrameValue* a, FrameValue* b) {
    if (a->location == b->location) {
      return a->priority - b->priority;
    }
    return a->location - b->location;
  }

 public:
  // Used by frame functions to describe locations.
  void describe(int owner, intptr_t* location, const char* description, int priority = 0);

#ifdef ASSERT
  void validate();
#endif
  void print(JavaThread* thread) { print_on(thread, tty); }
  void print_on(JavaThread* thread, outputStream* out);
};

#endif


#endif // SHARE_RUNTIME_FRAME_HPP
