/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

// no precompiled headers
#include "jvm_io.h"
#include "classfile/javaClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/threadLocalAllocBuffer.inline.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "interpreter/bytecodeHistogram.hpp"
#include "interpreter/zero/bytecodeInterpreter.inline.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "logging/log.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/constantPool.inline.hpp"
#include "oops/cpCache.inline.hpp"
#include "oops/instanceKlass.inline.hpp"
#include "oops/klass.inline.hpp"
#include "oops/method.inline.hpp"
#include "oops/methodCounters.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/jvmtiThreadState.hpp"
#include "runtime/atomic.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/threadCritical.hpp"
#include "utilities/debug.hpp"
#include "utilities/exceptions.hpp"
#include "utilities/macros.hpp"

// no precompiled headers

/*
 * USELABELS - If using GCC, then use labels for the opcode dispatching
 * rather -then a switch statement. This improves performance because it
 * gives us the opportunity to have the instructions that calculate the
 * next opcode to jump to be intermixed with the rest of the instructions
 * that implement the opcode (see UPDATE_PC_AND_TOS_AND_CONTINUE macro).
 */
#undef USELABELS
#ifdef __GNUC__
/*
   ASSERT signifies debugging. It is much easier to step thru bytecodes if we
   don't use the computed goto approach.
*/
#ifndef ASSERT
#define USELABELS
#endif
#endif

#undef CASE
#ifdef USELABELS
#define CASE(opcode) opc ## opcode
#define DEFAULT opc_default
#else
#define CASE(opcode) case Bytecodes:: opcode
#define DEFAULT default
#endif

/*
 * PREFETCH_OPCCODE - Some compilers do better if you prefetch the next
 * opcode before going back to the top of the while loop, rather then having
 * the top of the while loop handle it. This provides a better opportunity
 * for instruction scheduling. Some compilers just do this prefetch
 * automatically. Some actually end up with worse performance if you
 * force the prefetch. Solaris gcc seems to do better, but cc does worse.
 */
#undef PREFETCH_OPCCODE
#define PREFETCH_OPCCODE

/*
  Interpreter safepoint: it is expected that the interpreter will have no live
  handles of its own creation live at an interpreter safepoint. Therefore we
  run a HandleMarkCleaner and trash all handles allocated in the call chain
  since the JavaCalls::call_helper invocation that initiated the chain.
  There really shouldn't be any handles remaining to trash but this is cheap
  in relation to a safepoint.
*/
#define SAFEPOINT                                                                                         \
    if (SafepointMechanism::should_process(THREAD)) {                                                     \
      HandleMarkCleaner __hmc(THREAD);                                                                    \
      CALL_VM(SafepointMechanism::process_if_requested_with_exit_check(THREAD, true /* check asyncs */),  \
              handle_exception);                                                                          \
    }                                                                                                     \

/*
 * VM_JAVA_ERROR - Macro for throwing a java exception from
 * the interpreter loop. Should really be a CALL_VM but there
 * is no entry point to do the transition to vm so we just
 * do it by hand here.
 */
#define VM_JAVA_ERROR_NO_JUMP(name, msg)                                          \
    DECACHE_STATE();                                                              \
    SET_LAST_JAVA_FRAME();                                                        \
    {                                                                             \
       ThreadInVMfromJava trans(THREAD);                                          \
       Exceptions::_throw_msg(THREAD, __FILE__, __LINE__, name, msg);             \
    }                                                                             \
    RESET_LAST_JAVA_FRAME();                                                      \
    CACHE_STATE();

// Normal throw of a java error.
#define VM_JAVA_ERROR(name, msg)                                     \
    VM_JAVA_ERROR_NO_JUMP(name, msg)                                 \
    goto handle_exception;

#ifdef PRODUCT
#define DO_UPDATE_INSTRUCTION_COUNT(opcode)
#else
#define DO_UPDATE_INSTRUCTION_COUNT(opcode)                                            \
{                                                                                      \
    if (PrintBytecodeHistogram) {                                                      \
      BytecodeHistogram::_counters[(Bytecodes::Code)opcode]++;                         \
    }                                                                                  \
    if (CountBytecodes || TraceBytecodes || StopInterpreterAt > 0) {                   \
      BytecodeCounter::_counter_value++;                                               \
      if (StopInterpreterAt == BytecodeCounter::_counter_value) {                      \
        os::breakpoint();                                                              \
      }                                                                                \
      if (TraceBytecodes) {                                                            \
        CALL_VM((void)InterpreterRuntime::trace_bytecode(THREAD, 0,                    \
                                          topOfStack[Interpreter::expr_index_at(1)],   \
                                          topOfStack[Interpreter::expr_index_at(2)]),  \
                                          handle_exception);                           \
      }                                                                                \
    }                                                                                  \
}
#endif

#undef DEBUGGER_SINGLE_STEP_NOTIFY
#if INCLUDE_JVMTI
/* NOTE: (kbr) This macro must be called AFTER the PC has been
   incremented. JvmtiExport::at_single_stepping_point() may cause a
   breakpoint opcode to get inserted at the current PC to allow the
   debugger to coalesce single-step events.

   As a result if we call at_single_stepping_point() we refetch opcode
   to get the current opcode. This will override any other prefetching
   that might have occurred.
*/
#define DEBUGGER_SINGLE_STEP_NOTIFY()                                        \
{                                                                            \
    if (JVMTI_ENABLED && JvmtiExport::should_post_single_step()) {           \
      DECACHE_STATE();                                                       \
      SET_LAST_JAVA_FRAME();                                                 \
      ThreadInVMfromJava trans(THREAD);                                      \
      JvmtiExport::at_single_stepping_point(THREAD,                          \
                                           istate->method(),                 \
                                           pc);                              \
      RESET_LAST_JAVA_FRAME();                                               \
      CACHE_STATE();                                                         \
      if (THREAD->has_pending_popframe() &&                                  \
        !THREAD->pop_frame_in_process()) {                                   \
        goto handle_Pop_Frame;                                               \
      }                                                                      \
      if (THREAD->jvmti_thread_state() &&                                    \
          THREAD->jvmti_thread_state()->is_earlyret_pending()) {             \
        goto handle_Early_Return;                                            \
      }                                                                      \
      opcode = *pc;                                                          \
   }                                                                         \
}
#else
#define DEBUGGER_SINGLE_STEP_NOTIFY()
#endif // INCLUDE_JVMTI

/*
 * CONTINUE - Macro for executing the next opcode.
 */
#undef CONTINUE
#ifdef USELABELS
// Have to do this dispatch this way in C++ because otherwise gcc complains about crossing an
// initialization (which is is the initialization of the table pointer...)
#define DISPATCH(opcode) goto *(void*)dispatch_table[opcode]
#define CONTINUE {                              \
        opcode = *pc;                           \
        DO_UPDATE_INSTRUCTION_COUNT(opcode);    \
        DEBUGGER_SINGLE_STEP_NOTIFY();          \
        DISPATCH(opcode);                       \
    }
#else
#ifdef PREFETCH_OPCCODE
#define CONTINUE {                              \
        opcode = *pc;                           \
        DO_UPDATE_INSTRUCTION_COUNT(opcode);    \
        DEBUGGER_SINGLE_STEP_NOTIFY();          \
        continue;                               \
    }
#else
#define CONTINUE {                              \
        DO_UPDATE_INSTRUCTION_COUNT(opcode);    \
        DEBUGGER_SINGLE_STEP_NOTIFY();          \
        continue;                               \
    }
#endif
#endif


#define UPDATE_PC(opsize) {pc += opsize; }
/*
 * UPDATE_PC_AND_TOS - Macro for updating the pc and topOfStack.
 */
#undef UPDATE_PC_AND_TOS
#define UPDATE_PC_AND_TOS(opsize, stack) \
    {pc += opsize; MORE_STACK(stack); }

/*
 * UPDATE_PC_AND_TOS_AND_CONTINUE - Macro for updating the pc and topOfStack,
 * and executing the next opcode. It's somewhat similar to the combination
 * of UPDATE_PC_AND_TOS and CONTINUE, but with some minor optimizations.
 */
#undef UPDATE_PC_AND_TOS_AND_CONTINUE
#ifdef USELABELS
#define UPDATE_PC_AND_TOS_AND_CONTINUE(opsize, stack) {         \
        pc += opsize; opcode = *pc; MORE_STACK(stack);          \
        DO_UPDATE_INSTRUCTION_COUNT(opcode);                    \
        DEBUGGER_SINGLE_STEP_NOTIFY();                          \
        DISPATCH(opcode);                                       \
    }

#define UPDATE_PC_AND_CONTINUE(opsize) {                        \
        pc += opsize; opcode = *pc;                             \
        DO_UPDATE_INSTRUCTION_COUNT(opcode);                    \
        DEBUGGER_SINGLE_STEP_NOTIFY();                          \
        DISPATCH(opcode);                                       \
    }
#else
#ifdef PREFETCH_OPCCODE
#define UPDATE_PC_AND_TOS_AND_CONTINUE(opsize, stack) {         \
        pc += opsize; opcode = *pc; MORE_STACK(stack);          \
        DO_UPDATE_INSTRUCTION_COUNT(opcode);                    \
        DEBUGGER_SINGLE_STEP_NOTIFY();                          \
        goto do_continue;                                       \
    }

#define UPDATE_PC_AND_CONTINUE(opsize) {                        \
        pc += opsize; opcode = *pc;                             \
        DO_UPDATE_INSTRUCTION_COUNT(opcode);                    \
        DEBUGGER_SINGLE_STEP_NOTIFY();                          \
        goto do_continue;                                       \
    }
#else
#define UPDATE_PC_AND_TOS_AND_CONTINUE(opsize, stack) { \
        pc += opsize; MORE_STACK(stack);                \
        DO_UPDATE_INSTRUCTION_COUNT(opcode);            \
        DEBUGGER_SINGLE_STEP_NOTIFY();                  \
        goto do_continue;                               \
    }

#define UPDATE_PC_AND_CONTINUE(opsize) {                \
        pc += opsize;                                   \
        DO_UPDATE_INSTRUCTION_COUNT(opcode);            \
        DEBUGGER_SINGLE_STEP_NOTIFY();                  \
        goto do_continue;                               \
    }
#endif /* PREFETCH_OPCCODE */
#endif /* USELABELS */

// About to call a new method, update the save the adjusted pc and return to frame manager
#define UPDATE_PC_AND_RETURN(opsize)  \
   DECACHE_TOS();                     \
   istate->set_bcp(pc+opsize);        \
   return;


#define METHOD istate->method()
#define GET_METHOD_COUNTERS(res)
#define DO_BACKEDGE_CHECKS(skip, branch_pc)

/*
 * For those opcodes that need to have a GC point on a backwards branch
 */

/*
 * Macros for caching and flushing the interpreter state. Some local
 * variables need to be flushed out to the frame before we do certain
 * things (like pushing frames or becomming gc safe) and some need to
 * be recached later (like after popping a frame). We could use one
 * macro to cache or decache everything, but this would be less then
 * optimal because we don't always need to cache or decache everything
 * because some things we know are already cached or decached.
 */
#undef DECACHE_TOS
#undef CACHE_TOS
#undef CACHE_PREV_TOS
#define DECACHE_TOS()    istate->set_stack(topOfStack);

#define CACHE_TOS()      topOfStack = (intptr_t *)istate->stack();

#undef DECACHE_PC
#undef CACHE_PC
#define DECACHE_PC()    istate->set_bcp(pc);
#define CACHE_PC()      pc = istate->bcp();
#define CACHE_CP()      cp = istate->constants();
#define CACHE_LOCALS()  locals = istate->locals();
#undef CACHE_FRAME
#define CACHE_FRAME()

// BCI() returns the current bytecode-index.
#undef  BCI
#define BCI()           ((int)(intptr_t)(pc - (intptr_t)istate->method()->code_base()))

/*
 * CHECK_NULL - Macro for throwing a NullPointerException if the object
 * passed is a null ref.
 * On some architectures/platforms it should be possible to do this implicitly
 */
#undef CHECK_NULL
#define CHECK_NULL(obj_)                                                                         \
        if ((obj_) == NULL) {                                                                    \
          VM_JAVA_ERROR(vmSymbols::java_lang_NullPointerException(), NULL);                      \
        }                                                                                        \
        VERIFY_OOP(obj_)

#define VMdoubleConstZero() 0.0
#define VMdoubleConstOne() 1.0
#define VMlongConstZero() (max_jlong-max_jlong)
#define VMlongConstOne() ((max_jlong-max_jlong)+1)

/*
 * Alignment
 */
#define VMalignWordUp(val)          (((uintptr_t)(val) + 3) & ~3)

// Decache the interpreter state that interpreter modifies directly (i.e. GC is indirect mod)
#define DECACHE_STATE() DECACHE_PC(); DECACHE_TOS();

// Reload interpreter state after calling the VM or a possible GC
#define CACHE_STATE()   \
        CACHE_TOS();    \
        CACHE_PC();     \
        CACHE_CP();     \
        CACHE_LOCALS();

// Call the VM with last java frame only.
#define CALL_VM_NAKED_LJF(func)                                    \
        DECACHE_STATE();                                           \
        SET_LAST_JAVA_FRAME();                                     \
        func;                                                      \
        RESET_LAST_JAVA_FRAME();                                   \
        CACHE_STATE();

// Call the VM. Don't check for pending exceptions.
#define CALL_VM_NOCHECK(func)                                      \
        CALL_VM_NAKED_LJF(func)                                    \
        if (THREAD->has_pending_popframe() &&                      \
            !THREAD->pop_frame_in_process()) {                     \
          goto handle_Pop_Frame;                                   \
        }                                                          \
        if (THREAD->jvmti_thread_state() &&                        \
            THREAD->jvmti_thread_state()->is_earlyret_pending()) { \
          goto handle_Early_Return;                                \
        }

// Call the VM and check for pending exceptions
#define CALL_VM(func, label) {                                     \
          CALL_VM_NOCHECK(func);                                   \
          if (THREAD->has_pending_exception()) goto label;         \
        }

/*
 * BytecodeInterpreter::run(interpreterState istate)
 *
 * The real deal. This is where byte codes actually get interpreted.
 * Basically it's a big while loop that iterates until we return from
 * the method passed in.
 */

// Instantiate two variants of the method for future linking.
template void BytecodeInterpreter::run<true>(interpreterState istate);
template void BytecodeInterpreter::run<false>(interpreterState istate);

template<bool JVMTI_ENABLED>
void BytecodeInterpreter::run(interpreterState istate) {

  // In order to simplify some tests based on switches set at runtime
  // we invoke the interpreter a single time after switches are enabled
  // and set simpler to to test variables rather than method calls or complex
  // boolean expressions.

  static int initialized = 0;
  static int checkit = 0;
  static intptr_t* c_addr = NULL;
  static intptr_t  c_value;

  if (checkit && *c_addr != c_value) {
    os::breakpoint();
  }

#ifdef ASSERT
  if (istate->_msg != initialize) {
    assert(labs(istate->_stack_base - istate->_stack_limit) == (istate->_method->max_stack() + 1), "bad stack limit");
  }
  // Verify linkages.
  interpreterState l = istate;
  do {
    assert(l == l->_self_link, "bad link");
    l = l->_prev_link;
  } while (l != NULL);
  // Screwups with stack management usually cause us to overwrite istate
  // save a copy so we can verify it.
  interpreterState orig = istate;
#endif

  intptr_t*        topOfStack = (intptr_t *)istate->stack(); /* access with STACK macros */
  address          pc = istate->bcp();
  jubyte opcode;
  intptr_t*        locals = istate->locals();
  ConstantPoolCache*    cp = istate->constants(); // method()->constants()->cache()
#ifdef LOTS_OF_REGS
  JavaThread*      THREAD = istate->thread();
#else
#undef THREAD
#define THREAD istate->thread()
#endif

#ifdef USELABELS
  const static void* const opclabels_data[256] = {
/* 0x00 */ &&opc_nop,     &&opc_aconst_null,&&opc_iconst_m1,&&opc_iconst_0,
/* 0x04 */ &&opc_iconst_1,&&opc_iconst_2,   &&opc_iconst_3, &&opc_iconst_4,
/* 0x08 */ &&opc_iconst_5,&&opc_lconst_0,   &&opc_lconst_1, &&opc_fconst_0,
/* 0x0C */ &&opc_fconst_1,&&opc_fconst_2,   &&opc_dconst_0, &&opc_dconst_1,

/* 0x10 */ &&opc_bipush, &&opc_sipush, &&opc_ldc,    &&opc_ldc_w,
/* 0x14 */ &&opc_ldc2_w, &&opc_iload,  &&opc_lload,  &&opc_fload,
/* 0x18 */ &&opc_dload,  &&opc_aload,  &&opc_iload_0,&&opc_iload_1,
/* 0x1C */ &&opc_iload_2,&&opc_iload_3,&&opc_lload_0,&&opc_lload_1,

/* 0x20 */ &&opc_lload_2,&&opc_lload_3,&&opc_fload_0,&&opc_fload_1,
/* 0x24 */ &&opc_fload_2,&&opc_fload_3,&&opc_dload_0,&&opc_dload_1,
/* 0x28 */ &&opc_dload_2,&&opc_dload_3,&&opc_aload_0,&&opc_aload_1,
/* 0x2C */ &&opc_aload_2,&&opc_aload_3,&&opc_iaload, &&opc_laload,

/* 0x30 */ &&opc_faload,  &&opc_daload,  &&opc_aaload,  &&opc_baload,
/* 0x34 */ &&opc_caload,  &&opc_saload,  &&opc_istore,  &&opc_lstore,
/* 0x38 */ &&opc_fstore,  &&opc_dstore,  &&opc_astore,  &&opc_istore_0,
/* 0x3C */ &&opc_istore_1,&&opc_istore_2,&&opc_istore_3,&&opc_lstore_0,

/* 0x40 */ &&opc_lstore_1,&&opc_lstore_2,&&opc_lstore_3,&&opc_fstore_0,
/* 0x44 */ &&opc_fstore_1,&&opc_fstore_2,&&opc_fstore_3,&&opc_dstore_0,
/* 0x48 */ &&opc_dstore_1,&&opc_dstore_2,&&opc_dstore_3,&&opc_astore_0,
/* 0x4C */ &&opc_astore_1,&&opc_astore_2,&&opc_astore_3,&&opc_iastore,

/* 0x50 */ &&opc_lastore,&&opc_fastore,&&opc_dastore,&&opc_aastore,
/* 0x54 */ &&opc_bastore,&&opc_castore,&&opc_sastore,&&opc_pop,
/* 0x58 */ &&opc_pop2,   &&opc_dup,    &&opc_dup_x1, &&opc_dup_x2,
/* 0x5C */ &&opc_dup2,   &&opc_dup2_x1,&&opc_dup2_x2,&&opc_swap,

/* 0x60 */ &&opc_iadd,&&opc_ladd,&&opc_fadd,&&opc_dadd,
/* 0x64 */ &&opc_isub,&&opc_lsub,&&opc_fsub,&&opc_dsub,
/* 0x68 */ &&opc_imul,&&opc_lmul,&&opc_fmul,&&opc_dmul,
/* 0x6C */ &&opc_idiv,&&opc_ldiv,&&opc_fdiv,&&opc_ddiv,

/* 0x70 */ &&opc_irem, &&opc_lrem, &&opc_frem,&&opc_drem,
/* 0x74 */ &&opc_ineg, &&opc_lneg, &&opc_fneg,&&opc_dneg,
/* 0x78 */ &&opc_ishl, &&opc_lshl, &&opc_ishr,&&opc_lshr,
/* 0x7C */ &&opc_iushr,&&opc_lushr,&&opc_iand,&&opc_land,

/* 0x80 */ &&opc_ior, &&opc_lor,&&opc_ixor,&&opc_lxor,
/* 0x84 */ &&opc_iinc,&&opc_i2l,&&opc_i2f, &&opc_i2d,
/* 0x88 */ &&opc_l2i, &&opc_l2f,&&opc_l2d, &&opc_f2i,
/* 0x8C */ &&opc_f2l, &&opc_f2d,&&opc_d2i, &&opc_d2l,

/* 0x90 */ &&opc_d2f,  &&opc_i2b,  &&opc_i2c,  &&opc_i2s,
/* 0x94 */ &&opc_lcmp, &&opc_fcmpl,&&opc_fcmpg,&&opc_dcmpl,
/* 0x98 */ &&opc_dcmpg,&&opc_ifeq, &&opc_ifne, &&opc_iflt,
/* 0x9C */ &&opc_ifge, &&opc_ifgt, &&opc_ifle, &&opc_if_icmpeq,

/* 0xA0 */ &&opc_if_icmpne,&&opc_if_icmplt,&&opc_if_icmpge,  &&opc_if_icmpgt,
/* 0xA4 */ &&opc_if_icmple,&&opc_if_acmpeq,&&opc_if_acmpne,  &&opc_goto,
/* 0xA8 */ &&opc_jsr,      &&opc_ret,      &&opc_tableswitch,&&opc_lookupswitch,
/* 0xAC */ &&opc_ireturn,  &&opc_lreturn,  &&opc_freturn,    &&opc_dreturn,

/* 0xB0 */ &&opc_areturn,     &&opc_return,         &&opc_getstatic,    &&opc_putstatic,
/* 0xB4 */ &&opc_getfield,    &&opc_putfield,       &&opc_invokevirtual,&&opc_invokespecial,
/* 0xB8 */ &&opc_invokestatic,&&opc_invokeinterface,&&opc_invokedynamic,&&opc_new,
/* 0xBC */ &&opc_newarray,    &&opc_anewarray,      &&opc_arraylength,  &&opc_athrow,

/* 0xC0 */ &&opc_checkcast,   &&opc_instanceof,     &&opc_monitorenter, &&opc_monitorexit,
/* 0xC4 */ &&opc_wide,        &&opc_multianewarray, &&opc_ifnull,       &&opc_ifnonnull,
/* 0xC8 */ &&opc_goto_w,      &&opc_jsr_w,          &&opc_breakpoint,   &&opc_default,
/* 0xCC */ &&opc_default,     &&opc_default,        &&opc_default,      &&opc_default,

/* 0xD0 */ &&opc_default,     &&opc_default,        &&opc_default,      &&opc_default,
/* 0xD4 */ &&opc_default,     &&opc_default,        &&opc_default,      &&opc_default,
/* 0xD8 */ &&opc_default,     &&opc_default,        &&opc_default,      &&opc_default,
/* 0xDC */ &&opc_default,     &&opc_default,        &&opc_default,      &&opc_default,

/* 0xE0 */ &&opc_default,     &&opc_default,        &&opc_default,         &&opc_default,
/* 0xE4 */ &&opc_default,     &&opc_default,        &&opc_fast_aldc,    &&opc_fast_aldc_w,
/* 0xE8 */ &&opc_return_register_finalizer,
                              &&opc_invokehandle,   &&opc_default,      &&opc_default,
/* 0xEC */ &&opc_default,     &&opc_default,        &&opc_default,         &&opc_default,

/* 0xF0 */ &&opc_default,     &&opc_default,        &&opc_default,      &&opc_default,
/* 0xF4 */ &&opc_default,     &&opc_default,        &&opc_default,      &&opc_default,
/* 0xF8 */ &&opc_default,     &&opc_default,        &&opc_default,      &&opc_default,
/* 0xFC */ &&opc_default,     &&opc_default,        &&opc_default,      &&opc_default
  };
  uintptr_t *dispatch_table = (uintptr_t*)&opclabels_data[0];
#endif /* USELABELS */

#ifdef ASSERT
  // this will trigger a VERIFY_OOP on entry
  if (istate->msg() != initialize && ! METHOD->is_static()) {
    oop rcvr = LOCALS_OBJECT(0);
    VERIFY_OOP(rcvr);
  }
#endif

  /* QQQ this should be a stack method so we don't know actual direction */
  guarantee(istate->msg() == initialize ||
         topOfStack >= istate->stack_limit() &&
         topOfStack < istate->stack_base(),
         "Stack top out of range");

  assert(!UseCompiler, "Zero does not support compilers");
  assert(!CountCompiledCalls, "Zero does not support counting compiled calls");

  switch (istate->msg()) {
    case initialize: {
      if (initialized++) ShouldNotReachHere(); // Only one initialize call.
      return;
    }
    break;
    case method_entry: {
      THREAD->set_do_not_unlock();
      // count invocations
      assert(initialized, "Interpreter not initialized");

      if ((istate->_stack_base - istate->_stack_limit) != istate->method()->max_stack() + 1) {
        // initialize
        os::breakpoint();
      }

      // Lock method if synchronized.
      if (METHOD->is_synchronized()) {
        // oop rcvr = locals[0].j.r;
        oop rcvr;
        if (METHOD->is_static()) {
          rcvr = METHOD->constants()->pool_holder()->java_mirror();
        } else {
          rcvr = LOCALS_OBJECT(0);
          VERIFY_OOP(rcvr);
        }

        // The initial monitor is ours for the taking.
        BasicObjectLock* mon = &istate->monitor_base()[-1];
        mon->set_obj(rcvr);

        // Traditional lightweight locking.
        markWord displaced = rcvr->mark().set_unlocked();
        mon->lock()->set_displaced_header(displaced);
        bool call_vm = UseHeavyMonitors;
        if (call_vm || rcvr->cas_set_mark(markWord::from_pointer(mon), displaced) != displaced) {
          // Is it simple recursive case?
          if (!call_vm && THREAD->is_lock_owned((address) displaced.clear_lock_bits().to_pointer())) {
            mon->lock()->set_displaced_header(markWord::from_pointer(NULL));
          } else {
            CALL_VM(InterpreterRuntime::monitorenter(THREAD, mon), handle_exception);
          }
        }
      }
      THREAD->clr_do_not_unlock();

      // Notify jvmti.
      // Whenever JVMTI puts a thread in interp_only_mode, method
      // entry/exit events are sent for that thread to track stack depth.
      if (JVMTI_ENABLED && THREAD->is_interp_only_mode()) {
        CALL_VM(InterpreterRuntime::post_method_entry(THREAD),
                handle_exception);
      }

      goto run;
    }

    case popping_frame: {
      // returned from a java call to pop the frame, restart the call
      // clear the message so we don't confuse ourselves later
      assert(THREAD->pop_frame_in_process(), "wrong frame pop state");
      istate->set_msg(no_request);
      THREAD->clr_pop_frame_in_process();
      goto run;
    }

    case method_resume: {
      if ((istate->_stack_base - istate->_stack_limit) != istate->method()->max_stack() + 1) {
        // resume
        os::breakpoint();
      }
      // returned from a java call, continue executing.
      if (THREAD->has_pending_popframe() && !THREAD->pop_frame_in_process()) {
        goto handle_Pop_Frame;
      }
      if (THREAD->jvmti_thread_state() &&
          THREAD->jvmti_thread_state()->is_earlyret_pending()) {
        goto handle_Early_Return;
      }

      if (THREAD->has_pending_exception()) goto handle_exception;
      // Update the pc by the saved amount of the invoke bytecode size
      UPDATE_PC(istate->bcp_advance());
      goto run;
    }

    case deopt_resume2: {
      // Returned from an opcode that will reexecute. Deopt was
      // a result of a PopFrame request.
      //
      goto run;
    }

    case deopt_resume: {
      // Returned from an opcode that has completed. The stack has
      // the result all we need to do is skip across the bytecode
      // and continue (assuming there is no exception pending)
      //
      // compute continuation length
      //
      // Note: it is possible to deopt at a return_register_finalizer opcode
      // because this requires entering the vm to do the registering. While the
      // opcode is complete we can't advance because there are no more opcodes
      // much like trying to deopt at a poll return. In that has we simply
      // get out of here
      //
      if ( Bytecodes::code_at(METHOD, pc) == Bytecodes::_return_register_finalizer) {
        // this will do the right thing even if an exception is pending.
        goto handle_return;
      }
      UPDATE_PC(Bytecodes::length_at(METHOD, pc));
      if (THREAD->has_pending_exception()) goto handle_exception;
      goto run;
    }
    case got_monitors: {
      // continue locking now that we have a monitor to use
      // we expect to find newly allocated monitor at the "top" of the monitor stack.
      oop lockee = STACK_OBJECT(-1);
      VERIFY_OOP(lockee);
      // derefing's lockee ought to provoke implicit null check
      // find a free monitor
      BasicObjectLock* entry = (BasicObjectLock*) istate->stack_base();
      assert(entry->obj() == NULL, "Frame manager didn't allocate the monitor");
      entry->set_obj(lockee);

      // traditional lightweight locking
      markWord displaced = lockee->mark().set_unlocked();
      entry->lock()->set_displaced_header(displaced);
      bool call_vm = UseHeavyMonitors;
      if (call_vm || lockee->cas_set_mark(markWord::from_pointer(entry), displaced) != displaced) {
        // Is it simple recursive case?
        if (!call_vm && THREAD->is_lock_owned((address) displaced.clear_lock_bits().to_pointer())) {
          entry->lock()->set_displaced_header(markWord::from_pointer(NULL));
        } else {
          CALL_VM(InterpreterRuntime::monitorenter(THREAD, entry), handle_exception);
        }
      }
      UPDATE_PC_AND_TOS(1, -1);
      goto run;
    }
    default: {
      fatal("Unexpected message from frame manager");
    }
  }

run:

  DO_UPDATE_INSTRUCTION_COUNT(*pc)
  DEBUGGER_SINGLE_STEP_NOTIFY();
#ifdef PREFETCH_OPCCODE
  opcode = *pc;  /* prefetch first opcode */
#endif

#ifndef USELABELS
  while (1)
#endif
  {
#ifndef PREFETCH_OPCCODE
      opcode = *pc;
#endif
      // Seems like this happens twice per opcode. At worst this is only
      // need at entry to the loop.
      // DEBUGGER_SINGLE_STEP_NOTIFY();
      /* Using this labels avoids double breakpoints when quickening and
       * when returing from transition frames.
       */
  opcode_switch:
      assert(istate == orig, "Corrupted istate");
      /* QQQ Hmm this has knowledge of direction, ought to be a stack method */
      assert(topOfStack >= istate->stack_limit(), "Stack overrun");
      assert(topOfStack < istate->stack_base(), "Stack underrun");

#ifdef USELABELS
      DISPATCH(opcode);
#else
      switch (opcode)
#endif
      {
      CASE(_nop):
          UPDATE_PC_AND_CONTINUE(1);

          /* Push miscellaneous constants onto the stack. */

      CASE(_aconst_null):
          SET_STACK_OBJECT(NULL, 0);
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 1);

#undef  OPC_CONST_n
#define OPC_CONST_n(opcode, const_type, value)                          \
      CASE(opcode):                                                     \
          SET_STACK_ ## const_type(value, 0);                           \
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 1);

          OPC_CONST_n(_iconst_m1,   INT,       -1);
          OPC_CONST_n(_iconst_0,    INT,        0);
          OPC_CONST_n(_iconst_1,    INT,        1);
          OPC_CONST_n(_iconst_2,    INT,        2);
          OPC_CONST_n(_iconst_3,    INT,        3);
          OPC_CONST_n(_iconst_4,    INT,        4);
          OPC_CONST_n(_iconst_5,    INT,        5);
          OPC_CONST_n(_fconst_0,    FLOAT,      0.0);
          OPC_CONST_n(_fconst_1,    FLOAT,      1.0);
          OPC_CONST_n(_fconst_2,    FLOAT,      2.0);

#undef  OPC_CONST2_n
#define OPC_CONST2_n(opcname, value, key, kind)                         \
      CASE(_##opcname):                                                 \
      {                                                                 \
          SET_STACK_ ## kind(VM##key##Const##value(), 1);               \
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 2);                         \
      }
         OPC_CONST2_n(dconst_0, Zero, double, DOUBLE);
         OPC_CONST2_n(dconst_1, One,  double, DOUBLE);
         OPC_CONST2_n(lconst_0, Zero, long, LONG);
         OPC_CONST2_n(lconst_1, One,  long, LONG);

         /* Load constant from constant pool: */

          /* Push a 1-byte signed integer value onto the stack. */
      CASE(_bipush):
          SET_STACK_INT((jbyte)(pc[1]), 0);
          UPDATE_PC_AND_TOS_AND_CONTINUE(2, 1);

          /* Push a 2-byte signed integer constant onto the stack. */
      CASE(_sipush):
          SET_STACK_INT((int16_t)Bytes::get_Java_u2(pc + 1), 0);
          UPDATE_PC_AND_TOS_AND_CONTINUE(3, 1);

          /* load from local variable */

      CASE(_aload):
          VERIFY_OOP(LOCALS_OBJECT(pc[1]));
          SET_STACK_OBJECT(LOCALS_OBJECT(pc[1]), 0);
          UPDATE_PC_AND_TOS_AND_CONTINUE(2, 1);

      CASE(_iload):
      CASE(_fload):
          SET_STACK_SLOT(LOCALS_SLOT(pc[1]), 0);
          UPDATE_PC_AND_TOS_AND_CONTINUE(2, 1);

      CASE(_lload):
          SET_STACK_LONG_FROM_ADDR(LOCALS_LONG_AT(pc[1]), 1);
          UPDATE_PC_AND_TOS_AND_CONTINUE(2, 2);

      CASE(_dload):
          SET_STACK_DOUBLE_FROM_ADDR(LOCALS_DOUBLE_AT(pc[1]), 1);
          UPDATE_PC_AND_TOS_AND_CONTINUE(2, 2);

#undef  OPC_LOAD_n
#define OPC_LOAD_n(num)                                                 \
      CASE(_aload_##num):                                               \
          VERIFY_OOP(LOCALS_OBJECT(num));                               \
          SET_STACK_OBJECT(LOCALS_OBJECT(num), 0);                      \
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 1);                         \
                                                                        \
      CASE(_iload_##num):                                               \
      CASE(_fload_##num):                                               \
          SET_STACK_SLOT(LOCALS_SLOT(num), 0);                          \
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 1);                         \
                                                                        \
      CASE(_lload_##num):                                               \
          SET_STACK_LONG_FROM_ADDR(LOCALS_LONG_AT(num), 1);             \
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 2);                         \
      CASE(_dload_##num):                                               \
          SET_STACK_DOUBLE_FROM_ADDR(LOCALS_DOUBLE_AT(num), 1);         \
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 2);

          OPC_LOAD_n(0);
          OPC_LOAD_n(1);
          OPC_LOAD_n(2);
          OPC_LOAD_n(3);

          /* store to a local variable */

      CASE(_astore):
          astore(topOfStack, -1, locals, pc[1]);
          UPDATE_PC_AND_TOS_AND_CONTINUE(2, -1);

      CASE(_istore):
      CASE(_fstore):
          SET_LOCALS_SLOT(STACK_SLOT(-1), pc[1]);
          UPDATE_PC_AND_TOS_AND_CONTINUE(2, -1);

      CASE(_lstore):
          SET_LOCALS_LONG(STACK_LONG(-1), pc[1]);
          UPDATE_PC_AND_TOS_AND_CONTINUE(2, -2);

      CASE(_dstore):
          SET_LOCALS_DOUBLE(STACK_DOUBLE(-1), pc[1]);
          UPDATE_PC_AND_TOS_AND_CONTINUE(2, -2);

      CASE(_wide): {
          uint16_t reg = Bytes::get_Java_u2(pc + 2);

          opcode = pc[1];

          // Wide and it's sub-bytecode are counted as separate instructions. If we
          // don't account for this here, the bytecode trace skips the next bytecode.
          DO_UPDATE_INSTRUCTION_COUNT(opcode);

          switch(opcode) {
              case Bytecodes::_aload:
                  VERIFY_OOP(LOCALS_OBJECT(reg));
                  SET_STACK_OBJECT(LOCALS_OBJECT(reg), 0);
                  UPDATE_PC_AND_TOS_AND_CONTINUE(4, 1);

              case Bytecodes::_iload:
              case Bytecodes::_fload:
                  SET_STACK_SLOT(LOCALS_SLOT(reg), 0);
                  UPDATE_PC_AND_TOS_AND_CONTINUE(4, 1);

              case Bytecodes::_lload:
                  SET_STACK_LONG_FROM_ADDR(LOCALS_LONG_AT(reg), 1);
                  UPDATE_PC_AND_TOS_AND_CONTINUE(4, 2);

              case Bytecodes::_dload:
                  SET_STACK_DOUBLE_FROM_ADDR(LOCALS_LONG_AT(reg), 1);
                  UPDATE_PC_AND_TOS_AND_CONTINUE(4, 2);

              case Bytecodes::_astore:
                  astore(topOfStack, -1, locals, reg);
                  UPDATE_PC_AND_TOS_AND_CONTINUE(4, -1);

              case Bytecodes::_istore:
              case Bytecodes::_fstore:
                  SET_LOCALS_SLOT(STACK_SLOT(-1), reg);
                  UPDATE_PC_AND_TOS_AND_CONTINUE(4, -1);

              case Bytecodes::_lstore:
                  SET_LOCALS_LONG(STACK_LONG(-1), reg);
                  UPDATE_PC_AND_TOS_AND_CONTINUE(4, -2);

              case Bytecodes::_dstore:
                  SET_LOCALS_DOUBLE(STACK_DOUBLE(-1), reg);
                  UPDATE_PC_AND_TOS_AND_CONTINUE(4, -2);

              case Bytecodes::_iinc: {
                  int16_t offset = (int16_t)Bytes::get_Java_u2(pc+4);
                  // Be nice to see what this generates.... QQQ
                  SET_LOCALS_INT(LOCALS_INT(reg) + offset, reg);
                  UPDATE_PC_AND_CONTINUE(6);
              }
              case Bytecodes::_ret:
                  pc = istate->method()->code_base() + (intptr_t)(LOCALS_ADDR(reg));
                  UPDATE_PC_AND_CONTINUE(0);
              default:
                  VM_JAVA_ERROR(vmSymbols::java_lang_InternalError(), "undefined opcode");
          }
      }


#undef  OPC_STORE_n
#define OPC_STORE_n(num)                                                \
      CASE(_astore_##num):                                              \
          astore(topOfStack, -1, locals, num);                          \
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, -1);                        \
      CASE(_istore_##num):                                              \
      CASE(_fstore_##num):                                              \
          SET_LOCALS_SLOT(STACK_SLOT(-1), num);                         \
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, -1);

          OPC_STORE_n(0);
          OPC_STORE_n(1);
          OPC_STORE_n(2);
          OPC_STORE_n(3);

#undef  OPC_DSTORE_n
#define OPC_DSTORE_n(num)                                               \
      CASE(_dstore_##num):                                              \
          SET_LOCALS_DOUBLE(STACK_DOUBLE(-1), num);                     \
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, -2);                        \
      CASE(_lstore_##num):                                              \
          SET_LOCALS_LONG(STACK_LONG(-1), num);                         \
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, -2);

          OPC_DSTORE_n(0);
          OPC_DSTORE_n(1);
          OPC_DSTORE_n(2);
          OPC_DSTORE_n(3);

          /* stack pop, dup, and insert opcodes */


      CASE(_pop):                /* Discard the top item on the stack */
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, -1);


      CASE(_pop2):               /* Discard the top 2 items on the stack */
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, -2);


      CASE(_dup):               /* Duplicate the top item on the stack */
          dup(topOfStack);
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 1);

      CASE(_dup2):              /* Duplicate the top 2 items on the stack */
          dup2(topOfStack);
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 2);

      CASE(_dup_x1):    /* insert top word two down */
          dup_x1(topOfStack);
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 1);

      CASE(_dup_x2):    /* insert top word three down  */
          dup_x2(topOfStack);
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 1);

      CASE(_dup2_x1):   /* insert top 2 slots three down */
          dup2_x1(topOfStack);
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 2);

      CASE(_dup2_x2):   /* insert top 2 slots four down */
          dup2_x2(topOfStack);
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 2);

      CASE(_swap): {        /* swap top two elements on the stack */
          swap(topOfStack);
          UPDATE_PC_AND_CONTINUE(1);
      }

          /* Perform various binary integer operations */

#undef  OPC_INT_BINARY
#define OPC_INT_BINARY(opcname, opname, test)                           \
      CASE(_i##opcname):                                                \
          if (test && (STACK_INT(-1) == 0)) {                           \
              VM_JAVA_ERROR(vmSymbols::java_lang_ArithmeticException(), \
                            "/ by zero");                               \
          }                                                             \
          SET_STACK_INT(VMint##opname(STACK_INT(-2),                    \
                                      STACK_INT(-1)),                   \
                                      -2);                              \
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, -1);                        \
      CASE(_l##opcname):                                                \
      {                                                                 \
          if (test) {                                                   \
            jlong l1 = STACK_LONG(-1);                                  \
            if (VMlongEqz(l1)) {                                        \
              VM_JAVA_ERROR(vmSymbols::java_lang_ArithmeticException(), \
                            "/ by long zero");                          \
            }                                                           \
          }                                                             \
          /* First long at (-1,-2) next long at (-3,-4) */              \
          SET_STACK_LONG(VMlong##opname(STACK_LONG(-3),                 \
                                        STACK_LONG(-1)),                \
                                        -3);                            \
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, -2);                        \
      }

      OPC_INT_BINARY(add, Add, 0);
      OPC_INT_BINARY(sub, Sub, 0);
      OPC_INT_BINARY(mul, Mul, 0);
      OPC_INT_BINARY(and, And, 0);
      OPC_INT_BINARY(or,  Or,  0);
      OPC_INT_BINARY(xor, Xor, 0);
      OPC_INT_BINARY(div, Div, 1);
      OPC_INT_BINARY(rem, Rem, 1);


      /* Perform various binary floating number operations */
      /* On some machine/platforms/compilers div zero check can be implicit */

#undef  OPC_FLOAT_BINARY
#define OPC_FLOAT_BINARY(opcname, opname)                                  \
      CASE(_d##opcname): {                                                 \
          SET_STACK_DOUBLE(VMdouble##opname(STACK_DOUBLE(-3),              \
                                            STACK_DOUBLE(-1)),             \
                                            -3);                           \
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, -2);                           \
      }                                                                    \
      CASE(_f##opcname):                                                   \
          SET_STACK_FLOAT(VMfloat##opname(STACK_FLOAT(-2),                 \
                                          STACK_FLOAT(-1)),                \
                                          -2);                             \
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, -1);


     OPC_FLOAT_BINARY(add, Add);
     OPC_FLOAT_BINARY(sub, Sub);
     OPC_FLOAT_BINARY(mul, Mul);
     OPC_FLOAT_BINARY(div, Div);
     OPC_FLOAT_BINARY(rem, Rem);

      /* Shift operations
       * Shift left int and long: ishl, lshl
       * Logical shift right int and long w/zero extension: iushr, lushr
       * Arithmetic shift right int and long w/sign extension: ishr, lshr
       */

#undef  OPC_SHIFT_BINARY
#define OPC_SHIFT_BINARY(opcname, opname)                               \
      CASE(_i##opcname):                                                \
         SET_STACK_INT(VMint##opname(STACK_INT(-2),                     \
                                     STACK_INT(-1)),                    \
                                     -2);                               \
         UPDATE_PC_AND_TOS_AND_CONTINUE(1, -1);                         \
      CASE(_l##opcname):                                                \
      {                                                                 \
         SET_STACK_LONG(VMlong##opname(STACK_LONG(-2),                  \
                                       STACK_INT(-1)),                  \
                                       -2);                             \
         UPDATE_PC_AND_TOS_AND_CONTINUE(1, -1);                         \
      }

      OPC_SHIFT_BINARY(shl, Shl);
      OPC_SHIFT_BINARY(shr, Shr);
      OPC_SHIFT_BINARY(ushr, Ushr);

     /* Increment local variable by constant */
      CASE(_iinc):
      {
          // locals[pc[1]].j.i += (jbyte)(pc[2]);
          SET_LOCALS_INT(LOCALS_INT(pc[1]) + (jbyte)(pc[2]), pc[1]);
          UPDATE_PC_AND_CONTINUE(3);
      }

     /* negate the value on the top of the stack */

      CASE(_ineg):
         SET_STACK_INT(VMintNeg(STACK_INT(-1)), -1);
         UPDATE_PC_AND_CONTINUE(1);

      CASE(_fneg):
         SET_STACK_FLOAT(VMfloatNeg(STACK_FLOAT(-1)), -1);
         UPDATE_PC_AND_CONTINUE(1);

      CASE(_lneg):
      {
         SET_STACK_LONG(VMlongNeg(STACK_LONG(-1)), -1);
         UPDATE_PC_AND_CONTINUE(1);
      }

      CASE(_dneg):
      {
         SET_STACK_DOUBLE(VMdoubleNeg(STACK_DOUBLE(-1)), -1);
         UPDATE_PC_AND_CONTINUE(1);
      }

      /* Conversion operations */

      CASE(_i2f):       /* convert top of stack int to float */
         SET_STACK_FLOAT(VMint2Float(STACK_INT(-1)), -1);
         UPDATE_PC_AND_CONTINUE(1);

      CASE(_i2l):       /* convert top of stack int to long */
      {
          // this is ugly QQQ
          jlong r = VMint2Long(STACK_INT(-1));
          MORE_STACK(-1); // Pop
          SET_STACK_LONG(r, 1);

          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 2);
      }

      CASE(_i2d):       /* convert top of stack int to double */
      {
          // this is ugly QQQ (why cast to jlong?? )
          jdouble r = (jlong)STACK_INT(-1);
          MORE_STACK(-1); // Pop
          SET_STACK_DOUBLE(r, 1);

          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 2);
      }

      CASE(_l2i):       /* convert top of stack long to int */
      {
          jint r = VMlong2Int(STACK_LONG(-1));
          MORE_STACK(-2); // Pop
          SET_STACK_INT(r, 0);
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 1);
      }

      CASE(_l2f):   /* convert top of stack long to float */
      {
          jlong r = STACK_LONG(-1);
          MORE_STACK(-2); // Pop
          SET_STACK_FLOAT(VMlong2Float(r), 0);
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 1);
      }

      CASE(_l2d):       /* convert top of stack long to double */
      {
          jlong r = STACK_LONG(-1);
          MORE_STACK(-2); // Pop
          SET_STACK_DOUBLE(VMlong2Double(r), 1);
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 2);
      }

      CASE(_f2i):  /* Convert top of stack float to int */
          SET_STACK_INT(SharedRuntime::f2i(STACK_FLOAT(-1)), -1);
          UPDATE_PC_AND_CONTINUE(1);

      CASE(_f2l):  /* convert top of stack float to long */
      {
          jlong r = SharedRuntime::f2l(STACK_FLOAT(-1));
          MORE_STACK(-1); // POP
          SET_STACK_LONG(r, 1);
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 2);
      }

      CASE(_f2d):  /* convert top of stack float to double */
      {
          jfloat f;
          jdouble r;
          f = STACK_FLOAT(-1);
          r = (jdouble) f;
          MORE_STACK(-1); // POP
          SET_STACK_DOUBLE(r, 1);
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 2);
      }

      CASE(_d2i): /* convert top of stack double to int */
      {
          jint r1 = SharedRuntime::d2i(STACK_DOUBLE(-1));
          MORE_STACK(-2);
          SET_STACK_INT(r1, 0);
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 1);
      }

      CASE(_d2f): /* convert top of stack double to float */
      {
          jfloat r1 = VMdouble2Float(STACK_DOUBLE(-1));
          MORE_STACK(-2);
          SET_STACK_FLOAT(r1, 0);
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 1);
      }

      CASE(_d2l): /* convert top of stack double to long */
      {
          jlong r1 = SharedRuntime::d2l(STACK_DOUBLE(-1));
          MORE_STACK(-2);
          SET_STACK_LONG(r1, 1);
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 2);
      }

      CASE(_i2b):
          SET_STACK_INT(VMint2Byte(STACK_INT(-1)), -1);
          UPDATE_PC_AND_CONTINUE(1);

      CASE(_i2c):
          SET_STACK_INT(VMint2Char(STACK_INT(-1)), -1);
          UPDATE_PC_AND_CONTINUE(1);

      CASE(_i2s):
          SET_STACK_INT(VMint2Short(STACK_INT(-1)), -1);
          UPDATE_PC_AND_CONTINUE(1);

      /* comparison operators */


#define COMPARISON_OP(name, comparison)                                      \
      CASE(_if_icmp##name): {                                                \
          int skip = (STACK_INT(-2) comparison STACK_INT(-1))                \
                      ? (int16_t)Bytes::get_Java_u2(pc + 1) : 3;             \
          address branch_pc = pc;                                            \
          UPDATE_PC_AND_TOS(skip, -2);                                       \
          DO_BACKEDGE_CHECKS(skip, branch_pc);                               \
          CONTINUE;                                                          \
      }                                                                      \
      CASE(_if##name): {                                                     \
          int skip = (STACK_INT(-1) comparison 0)                            \
                      ? (int16_t)Bytes::get_Java_u2(pc + 1) : 3;             \
          address branch_pc = pc;                                            \
          UPDATE_PC_AND_TOS(skip, -1);                                       \
          DO_BACKEDGE_CHECKS(skip, branch_pc);                               \
          CONTINUE;                                                          \
      }

#define COMPARISON_OP2(name, comparison)                                     \
      COMPARISON_OP(name, comparison)                                        \
      CASE(_if_acmp##name): {                                                \
          int skip = (STACK_OBJECT(-2) comparison STACK_OBJECT(-1))          \
                       ? (int16_t)Bytes::get_Java_u2(pc + 1) : 3;            \
          address branch_pc = pc;                                            \
          UPDATE_PC_AND_TOS(skip, -2);                                       \
          DO_BACKEDGE_CHECKS(skip, branch_pc);                               \
          CONTINUE;                                                          \
      }

#define NULL_COMPARISON_NOT_OP(name)                                         \
      CASE(_if##name): {                                                     \
          int skip = (!(STACK_OBJECT(-1) == NULL))                           \
                      ? (int16_t)Bytes::get_Java_u2(pc + 1) : 3;             \
          address branch_pc = pc;                                            \
          UPDATE_PC_AND_TOS(skip, -1);                                       \
          DO_BACKEDGE_CHECKS(skip, branch_pc);                               \
          CONTINUE;                                                          \
      }

#define NULL_COMPARISON_OP(name)                                             \
      CASE(_if##name): {                                                     \
          int skip = ((STACK_OBJECT(-1) == NULL))                            \
                      ? (int16_t)Bytes::get_Java_u2(pc + 1) : 3;             \
          address branch_pc = pc;                                            \
          UPDATE_PC_AND_TOS(skip, -1);                                       \
          DO_BACKEDGE_CHECKS(skip, branch_pc);                               \
          CONTINUE;                                                          \
      }
      COMPARISON_OP(lt, <);
      COMPARISON_OP(gt, >);
      COMPARISON_OP(le, <=);
      COMPARISON_OP(ge, >=);
      COMPARISON_OP2(eq, ==);  /* include ref comparison */
      COMPARISON_OP2(ne, !=);  /* include ref comparison */
      NULL_COMPARISON_OP(null);
      NULL_COMPARISON_NOT_OP(nonnull);

      /* Goto pc at specified offset in switch table. */

      CASE(_tableswitch): {
          jint* lpc  = (jint*)VMalignWordUp(pc+1);
          int32_t  key  = STACK_INT(-1);
          int32_t  low  = Bytes::get_Java_u4((address)&lpc[1]);
          int32_t  high = Bytes::get_Java_u4((address)&lpc[2]);
          int32_t  skip;
          key -= low;
          if (((uint32_t) key > (uint32_t)(high - low))) {
            skip = Bytes::get_Java_u4((address)&lpc[0]);
          } else {
            skip = Bytes::get_Java_u4((address)&lpc[key + 3]);
          }
          // Does this really need a full backedge check (osr)?
          address branch_pc = pc;
          UPDATE_PC_AND_TOS(skip, -1);
          DO_BACKEDGE_CHECKS(skip, branch_pc);
          CONTINUE;
      }

      /* Goto pc whose table entry matches specified key. */

      CASE(_lookupswitch): {
          jint* lpc  = (jint*)VMalignWordUp(pc+1);
          int32_t  key  = STACK_INT(-1);
          int32_t  skip = Bytes::get_Java_u4((address) lpc); /* default amount */
          int32_t  npairs = Bytes::get_Java_u4((address) &lpc[1]);
          while (--npairs >= 0) {
            lpc += 2;
            if (key == (int32_t)Bytes::get_Java_u4((address)lpc)) {
              skip = Bytes::get_Java_u4((address)&lpc[1]);
              break;
            }
          }
          address branch_pc = pc;
          UPDATE_PC_AND_TOS(skip, -1);
          DO_BACKEDGE_CHECKS(skip, branch_pc);
          CONTINUE;
      }

      CASE(_fcmpl):
      CASE(_fcmpg):
      {
          SET_STACK_INT(VMfloatCompare(STACK_FLOAT(-2),
                                        STACK_FLOAT(-1),
                                        (opcode == Bytecodes::_fcmpl ? -1 : 1)),
                        -2);
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, -1);
      }

      CASE(_dcmpl):
      CASE(_dcmpg):
      {
          int r = VMdoubleCompare(STACK_DOUBLE(-3),
                                  STACK_DOUBLE(-1),
                                  (opcode == Bytecodes::_dcmpl ? -1 : 1));
          MORE_STACK(-4); // Pop
          SET_STACK_INT(r, 0);
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 1);
      }

      CASE(_lcmp):
      {
          int r = VMlongCompare(STACK_LONG(-3), STACK_LONG(-1));
          MORE_STACK(-4);
          SET_STACK_INT(r, 0);
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, 1);
      }


      /* Return from a method */

      CASE(_areturn):
      CASE(_ireturn):
      CASE(_freturn):
      {
          // Allow a safepoint before returning to frame manager.
          SAFEPOINT;

          goto handle_return;
      }

      CASE(_lreturn):
      CASE(_dreturn):
      {
          // Allow a safepoint before returning to frame manager.
          SAFEPOINT;
          goto handle_return;
      }

      CASE(_return_register_finalizer): {

          oop rcvr = LOCALS_OBJECT(0);
          VERIFY_OOP(rcvr);
          if (rcvr->klass()->has_finalizer()) {
            CALL_VM(InterpreterRuntime::register_finalizer(THREAD, rcvr), handle_exception);
          }
          goto handle_return;
      }
      CASE(_return): {

          // Allow a safepoint before returning to frame manager.
          SAFEPOINT;
          goto handle_return;
      }

      /* Array access byte-codes */

      /* Every array access byte-code starts out like this */
//        arrayOopDesc* arrObj = (arrayOopDesc*)STACK_OBJECT(arrayOff);
#define ARRAY_INTRO(arrayOff)                                                  \
      arrayOop arrObj = (arrayOop)STACK_OBJECT(arrayOff);                      \
      jint     index  = STACK_INT(arrayOff + 1);                               \
      /* Two integers, the additional message, and the null-terminator */      \
      char message[2 * jintAsStringSize + 33];                                 \
      CHECK_NULL(arrObj);                                                      \
      if ((uint32_t)index >= (uint32_t)arrObj->length()) {                     \
          jio_snprintf(message, sizeof(message),                               \
                  "Index %d out of bounds for length %d",                      \
                  index, arrObj->length());                                    \
          VM_JAVA_ERROR(vmSymbols::java_lang_ArrayIndexOutOfBoundsException(), \
                        message);                                              \
      }

      /* 32-bit loads. These handle conversion from < 32-bit types */
#define ARRAY_LOADTO32(T, T2, format, stackRes, extra)                                \
      {                                                                               \
          ARRAY_INTRO(-2);                                                            \
          (void)extra;                                                                \
          SET_ ## stackRes(*(T2 *)(((address) arrObj->base(T)) + index * sizeof(T2)), \
                           -2);                                                       \
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, -1);                                      \
      }

      /* 64-bit loads */
#define ARRAY_LOADTO64(T,T2, stackRes, extra)                                              \
      {                                                                                    \
          ARRAY_INTRO(-2);                                                                 \
          SET_ ## stackRes(*(T2 *)(((address) arrObj->base(T)) + index * sizeof(T2)), -1); \
          (void)extra;                                                                     \
          UPDATE_PC_AND_CONTINUE(1);                                                       \
      }

      CASE(_iaload):
          ARRAY_LOADTO32(T_INT, jint,   "%d",   STACK_INT, 0);
      CASE(_faload):
          ARRAY_LOADTO32(T_FLOAT, jfloat, "%f",   STACK_FLOAT, 0);
      CASE(_aaload): {
          ARRAY_INTRO(-2);
          SET_STACK_OBJECT(((objArrayOop) arrObj)->obj_at(index), -2);
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, -1);
      }
      CASE(_baload):
          ARRAY_LOADTO32(T_BYTE, jbyte,  "%d",   STACK_INT, 0);
      CASE(_caload):
          ARRAY_LOADTO32(T_CHAR,  jchar, "%d",   STACK_INT, 0);
      CASE(_saload):
          ARRAY_LOADTO32(T_SHORT, jshort, "%d",   STACK_INT, 0);
      CASE(_laload):
          ARRAY_LOADTO64(T_LONG, jlong, STACK_LONG, 0);
      CASE(_daload):
          ARRAY_LOADTO64(T_DOUBLE, jdouble, STACK_DOUBLE, 0);

      /* 32-bit stores. These handle conversion to < 32-bit types */
#define ARRAY_STOREFROM32(T, T2, format, stackSrc, extra)                            \
      {                                                                              \
          ARRAY_INTRO(-3);                                                           \
          (void)extra;                                                               \
          *(T2 *)(((address) arrObj->base(T)) + index * sizeof(T2)) = stackSrc( -1); \
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, -3);                                     \
      }

      /* 64-bit stores */
#define ARRAY_STOREFROM64(T, T2, stackSrc, extra)                                    \
      {                                                                              \
          ARRAY_INTRO(-4);                                                           \
          (void)extra;                                                               \
          *(T2 *)(((address) arrObj->base(T)) + index * sizeof(T2)) = stackSrc( -1); \
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, -4);                                     \
      }

      CASE(_iastore):
          ARRAY_STOREFROM32(T_INT, jint,   "%d",   STACK_INT, 0);
      CASE(_fastore):
          ARRAY_STOREFROM32(T_FLOAT, jfloat, "%f",   STACK_FLOAT, 0);
      /*
       * This one looks different because of the assignability check
       */
      CASE(_aastore): {
          oop rhsObject = STACK_OBJECT(-1);
          VERIFY_OOP(rhsObject);
          ARRAY_INTRO( -3);
          // arrObj, index are set
          if (rhsObject != NULL) {
            /* Check assignability of rhsObject into arrObj */
            Klass* rhsKlass = rhsObject->klass(); // EBX (subclass)
            Klass* elemKlass = ObjArrayKlass::cast(arrObj->klass())->element_klass(); // superklass EAX
            //
            // Check for compatibilty. This check must not GC!!
            // Seems way more expensive now that we must dispatch
            //
            if (rhsKlass != elemKlass && !rhsKlass->is_subtype_of(elemKlass)) { // ebx->is...
              VM_JAVA_ERROR(vmSymbols::java_lang_ArrayStoreException(), "");
            }
          }
          ((objArrayOop) arrObj)->obj_at_put(index, rhsObject);
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, -3);
      }
      CASE(_bastore): {
          ARRAY_INTRO(-3);
          int item = STACK_INT(-1);
          // if it is a T_BOOLEAN array, mask the stored value to 0/1
          if (arrObj->klass() == Universe::boolArrayKlassObj()) {
            item &= 1;
          } else {
            assert(arrObj->klass() == Universe::byteArrayKlassObj(),
                   "should be byte array otherwise");
          }
          ((typeArrayOop)arrObj)->byte_at_put(index, item);
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, -3);
      }
      CASE(_castore):
          ARRAY_STOREFROM32(T_CHAR, jchar,  "%d",   STACK_INT, 0);
      CASE(_sastore):
          ARRAY_STOREFROM32(T_SHORT, jshort, "%d",   STACK_INT, 0);
      CASE(_lastore):
          ARRAY_STOREFROM64(T_LONG, jlong, STACK_LONG, 0);
      CASE(_dastore):
          ARRAY_STOREFROM64(T_DOUBLE, jdouble, STACK_DOUBLE, 0);

      CASE(_arraylength):
      {
          arrayOop ary = (arrayOop) STACK_OBJECT(-1);
          CHECK_NULL(ary);
          SET_STACK_INT(ary->length(), -1);
          UPDATE_PC_AND_CONTINUE(1);
      }

      /* monitorenter and monitorexit for locking/unlocking an object */

      CASE(_monitorenter): {
        oop lockee = STACK_OBJECT(-1);
        // derefing's lockee ought to provoke implicit null check
        CHECK_NULL(lockee);
        // find a free monitor or one already allocated for this object
        // if we find a matching object then we need a new monitor
        // since this is recursive enter
        BasicObjectLock* limit = istate->monitor_base();
        BasicObjectLock* most_recent = (BasicObjectLock*) istate->stack_base();
        BasicObjectLock* entry = NULL;
        while (most_recent != limit ) {
          if (most_recent->obj() == NULL) entry = most_recent;
          else if (most_recent->obj() == lockee) break;
          most_recent++;
        }
        if (entry != NULL) {
          entry->set_obj(lockee);

          // traditional lightweight locking
          markWord displaced = lockee->mark().set_unlocked();
          entry->lock()->set_displaced_header(displaced);
          bool call_vm = UseHeavyMonitors;
          if (call_vm || lockee->cas_set_mark(markWord::from_pointer(entry), displaced) != displaced) {
            // Is it simple recursive case?
            if (!call_vm && THREAD->is_lock_owned((address) displaced.clear_lock_bits().to_pointer())) {
              entry->lock()->set_displaced_header(markWord::from_pointer(NULL));
            } else {
              CALL_VM(InterpreterRuntime::monitorenter(THREAD, entry), handle_exception);
            }
          }
          UPDATE_PC_AND_TOS_AND_CONTINUE(1, -1);
        } else {
          istate->set_msg(more_monitors);
          UPDATE_PC_AND_RETURN(0); // Re-execute
        }
      }

      CASE(_monitorexit): {
        oop lockee = STACK_OBJECT(-1);
        CHECK_NULL(lockee);
        // derefing's lockee ought to provoke implicit null check
        // find our monitor slot
        BasicObjectLock* limit = istate->monitor_base();
        BasicObjectLock* most_recent = (BasicObjectLock*) istate->stack_base();
        while (most_recent != limit ) {
          if ((most_recent)->obj() == lockee) {
            BasicLock* lock = most_recent->lock();
            markWord header = lock->displaced_header();
            most_recent->set_obj(NULL);

            // If it isn't recursive we either must swap old header or call the runtime
            bool call_vm = UseHeavyMonitors;
            if (header.to_pointer() != NULL || call_vm) {
              markWord old_header = markWord::encode(lock);
              if (call_vm || lockee->cas_set_mark(header, old_header) != old_header) {
                // restore object for the slow case
                most_recent->set_obj(lockee);
                InterpreterRuntime::monitorexit(most_recent);
              }
            }
            UPDATE_PC_AND_TOS_AND_CONTINUE(1, -1);
          }
          most_recent++;
        }
        // Need to throw illegal monitor state exception
        CALL_VM(InterpreterRuntime::throw_illegal_monitor_state_exception(THREAD), handle_exception);
        ShouldNotReachHere();
      }

      /* All of the non-quick opcodes. */

      /* -Set clobbersCpIndex true if the quickened opcode clobbers the
       *  constant pool index in the instruction.
       */
      CASE(_getfield):
      CASE(_getstatic):
        {
          u2 index;
          ConstantPoolCacheEntry* cache;
          index = Bytes::get_native_u2(pc+1);

          // QQQ Need to make this as inlined as possible. Probably need to
          // split all the bytecode cases out so c++ compiler has a chance
          // for constant prop to fold everything possible away.

          cache = cp->entry_at(index);
          if (!cache->is_resolved((Bytecodes::Code)opcode)) {
            CALL_VM(InterpreterRuntime::resolve_from_cache(THREAD, (Bytecodes::Code)opcode),
                    handle_exception);
            cache = cp->entry_at(index);
          }

          if (JVMTI_ENABLED) {
            int *count_addr;
            oop obj;
            // Check to see if a field modification watch has been set
            // before we take the time to call into the VM.
            count_addr = (int *)JvmtiExport::get_field_access_count_addr();
            if ( *count_addr > 0 ) {
              if ((Bytecodes::Code)opcode == Bytecodes::_getstatic) {
                obj = NULL;
              } else {
                obj = STACK_OBJECT(-1);
                VERIFY_OOP(obj);
              }
              CALL_VM(InterpreterRuntime::post_field_access(THREAD,
                                          obj,
                                          cache),
                                          handle_exception);
            }
          }

          oop obj;
          if ((Bytecodes::Code)opcode == Bytecodes::_getstatic) {
            Klass* k = cache->f1_as_klass();
            obj = k->java_mirror();
            MORE_STACK(1);  // Assume single slot push
          } else {
            obj = STACK_OBJECT(-1);
            CHECK_NULL(obj);
          }

          //
          // Now store the result on the stack
          //
          TosState tos_type = cache->flag_state();
          int field_offset = cache->f2_as_index();
          if (cache->is_volatile()) {
            if (support_IRIW_for_not_multiple_copy_atomic_cpu) {
              OrderAccess::fence();
            }
            switch (tos_type) {
              case btos:
              case ztos:
                SET_STACK_INT(obj->byte_field_acquire(field_offset), -1);
                break;
              case ctos:
                SET_STACK_INT(obj->char_field_acquire(field_offset), -1);
                break;
              case stos:
                SET_STACK_INT(obj->short_field_acquire(field_offset), -1);
                break;
              case itos:
                SET_STACK_INT(obj->int_field_acquire(field_offset), -1);
                break;
              case ftos:
                SET_STACK_FLOAT(obj->float_field_acquire(field_offset), -1);
                break;
              case ltos:
                SET_STACK_LONG(obj->long_field_acquire(field_offset), 0);
                MORE_STACK(1);
                break;
              case dtos:
                SET_STACK_DOUBLE(obj->double_field_acquire(field_offset), 0);
                MORE_STACK(1);
                break;
              case atos: {
                oop val = obj->obj_field_acquire(field_offset);
                VERIFY_OOP(val);
                SET_STACK_OBJECT(val, -1);
                break;
              }
              default:
                ShouldNotReachHere();
            }
          } else {
            switch (tos_type) {
              case btos:
              case ztos:
                SET_STACK_INT(obj->byte_field(field_offset), -1);
                break;
              case ctos:
                SET_STACK_INT(obj->char_field(field_offset), -1);
                break;
              case stos:
                SET_STACK_INT(obj->short_field(field_offset), -1);
                break;
              case itos:
                SET_STACK_INT(obj->int_field(field_offset), -1);
                break;
              case ftos:
                SET_STACK_FLOAT(obj->float_field(field_offset), -1);
                break;
              case ltos:
                SET_STACK_LONG(obj->long_field(field_offset), 0);
                MORE_STACK(1);
                break;
              case dtos:
                SET_STACK_DOUBLE(obj->double_field(field_offset), 0);
                MORE_STACK(1);
                break;
              case atos: {
                oop val = obj->obj_field(field_offset);
                VERIFY_OOP(val);
                SET_STACK_OBJECT(val, -1);
                break;
              }
              default:
                ShouldNotReachHere();
            }
          }

          UPDATE_PC_AND_CONTINUE(3);
         }

      CASE(_putfield):
      CASE(_putstatic):
        {
          u2 index = Bytes::get_native_u2(pc+1);
          ConstantPoolCacheEntry* cache = cp->entry_at(index);
          if (!cache->is_resolved((Bytecodes::Code)opcode)) {
            CALL_VM(InterpreterRuntime::resolve_from_cache(THREAD, (Bytecodes::Code)opcode),
                    handle_exception);
            cache = cp->entry_at(index);
          }

          if (JVMTI_ENABLED) {
            int *count_addr;
            oop obj;
            // Check to see if a field modification watch has been set
            // before we take the time to call into the VM.
            count_addr = (int *)JvmtiExport::get_field_modification_count_addr();
            if ( *count_addr > 0 ) {
              if ((Bytecodes::Code)opcode == Bytecodes::_putstatic) {
                obj = NULL;
              }
              else {
                if (cache->is_long() || cache->is_double()) {
                  obj = STACK_OBJECT(-3);
                } else {
                  obj = STACK_OBJECT(-2);
                }
                VERIFY_OOP(obj);
              }

              CALL_VM(InterpreterRuntime::post_field_modification(THREAD,
                                          obj,
                                          cache,
                                          (jvalue *)STACK_SLOT(-1)),
                                          handle_exception);
            }
          }

          // QQQ Need to make this as inlined as possible. Probably need to split all the bytecode cases
          // out so c++ compiler has a chance for constant prop to fold everything possible away.

          oop obj;
          int count;
          TosState tos_type = cache->flag_state();

          count = -1;
          if (tos_type == ltos || tos_type == dtos) {
            --count;
          }
          if ((Bytecodes::Code)opcode == Bytecodes::_putstatic) {
            Klass* k = cache->f1_as_klass();
            obj = k->java_mirror();
          } else {
            --count;
            obj = STACK_OBJECT(count);
            CHECK_NULL(obj);
          }

          //
          // Now store the result
          //
          int field_offset = cache->f2_as_index();
          if (cache->is_volatile()) {
            switch (tos_type) {
              case ztos:
                obj->release_byte_field_put(field_offset, (STACK_INT(-1) & 1)); // only store LSB
                break;
              case btos:
                obj->release_byte_field_put(field_offset, STACK_INT(-1));
                break;
              case ctos:
                obj->release_char_field_put(field_offset, STACK_INT(-1));
                break;
              case stos:
                obj->release_short_field_put(field_offset, STACK_INT(-1));
                break;
              case itos:
                obj->release_int_field_put(field_offset, STACK_INT(-1));
                break;
              case ftos:
                obj->release_float_field_put(field_offset, STACK_FLOAT(-1));
                break;
              case ltos:
                obj->release_long_field_put(field_offset, STACK_LONG(-1));
                break;
              case dtos:
                obj->release_double_field_put(field_offset, STACK_DOUBLE(-1));
                break;
              case atos: {
                oop val = STACK_OBJECT(-1);
                VERIFY_OOP(val);
                obj->release_obj_field_put(field_offset, val);
                break;
              }
              default:
                ShouldNotReachHere();
            }
            OrderAccess::storeload();
          } else {
            switch (tos_type) {
              case ztos:
                obj->byte_field_put(field_offset, (STACK_INT(-1) & 1)); // only store LSB
                break;
              case btos:
                obj->byte_field_put(field_offset, STACK_INT(-1));
                break;
              case ctos:
                obj->char_field_put(field_offset, STACK_INT(-1));
                break;
              case stos:
                obj->short_field_put(field_offset, STACK_INT(-1));
                break;
              case itos:
                obj->int_field_put(field_offset, STACK_INT(-1));
                break;
              case ftos:
                obj->float_field_put(field_offset, STACK_FLOAT(-1));
                break;
              case ltos:
                obj->long_field_put(field_offset, STACK_LONG(-1));
                break;
              case dtos:
                obj->double_field_put(field_offset, STACK_DOUBLE(-1));
                break;
              case atos: {
                oop val = STACK_OBJECT(-1);
                VERIFY_OOP(val);
                obj->obj_field_put(field_offset, val);
                break;
              }
              default:
                ShouldNotReachHere();
            }
          }

          UPDATE_PC_AND_TOS_AND_CONTINUE(3, count);
        }

      CASE(_new): {
        u2 index = Bytes::get_Java_u2(pc+1);

        // Attempt TLAB allocation first.
        //
        // To do this, we need to make sure:
        //   - klass is initialized
        //   - klass can be fastpath allocated (e.g. does not have finalizer)
        //   - TLAB accepts the allocation
        ConstantPool* constants = istate->method()->constants();
        if (UseTLAB && !constants->tag_at(index).is_unresolved_klass()) {
          Klass* entry = constants->resolved_klass_at(index);
          InstanceKlass* ik = InstanceKlass::cast(entry);
          if (ik->is_initialized() && ik->can_be_fastpath_allocated()) {
            size_t obj_size = ik->size_helper();
            HeapWord* result = THREAD->tlab().allocate(obj_size);
            if (result != NULL) {
              // Initialize object field block:
              //   - if TLAB is pre-zeroed, we can skip this path
              //   - in debug mode, ThreadLocalAllocBuffer::allocate mangles
              //     this area, and we still need to initialize it
              if (DEBUG_ONLY(true ||) !ZeroTLAB) {
                size_t hdr_size = oopDesc::header_size();
                Copy::fill_to_words(result + hdr_size, obj_size - hdr_size, 0);
              }

              oop obj = cast_to_oop(result);

              // Initialize header
              obj->set_mark(markWord::prototype());
              obj->set_klass_gap(0);
              obj->set_klass(ik);

              // Must prevent reordering of stores for object initialization
              // with stores that publish the new object.
              OrderAccess::storestore();
              SET_STACK_OBJECT(obj, 0);
              UPDATE_PC_AND_TOS_AND_CONTINUE(3, 1);
            }
          }
        }
        // Slow case allocation
        CALL_VM(InterpreterRuntime::_new(THREAD, METHOD->constants(), index),
                handle_exception);
        // Must prevent reordering of stores for object initialization
        // with stores that publish the new object.
        OrderAccess::storestore();
        SET_STACK_OBJECT(THREAD->vm_result(), 0);
        THREAD->set_vm_result(NULL);
        UPDATE_PC_AND_TOS_AND_CONTINUE(3, 1);
      }
      CASE(_anewarray): {
        u2 index = Bytes::get_Java_u2(pc+1);
        jint size = STACK_INT(-1);
        CALL_VM(InterpreterRuntime::anewarray(THREAD, METHOD->constants(), index, size),
                handle_exception);
        // Must prevent reordering of stores for object initialization
        // with stores that publish the new object.
        OrderAccess::storestore();
        SET_STACK_OBJECT(THREAD->vm_result(), -1);
        THREAD->set_vm_result(NULL);
        UPDATE_PC_AND_CONTINUE(3);
      }
      CASE(_multianewarray): {
        jint dims = *(pc+3);
        jint size = STACK_INT(-1);
        // stack grows down, dimensions are up!
        jint *dimarray =
                   (jint*)&topOfStack[dims * Interpreter::stackElementWords+
                                      Interpreter::stackElementWords-1];
        //adjust pointer to start of stack element
        CALL_VM(InterpreterRuntime::multianewarray(THREAD, dimarray),
                handle_exception);
        // Must prevent reordering of stores for object initialization
        // with stores that publish the new object.
        OrderAccess::storestore();
        SET_STACK_OBJECT(THREAD->vm_result(), -dims);
        THREAD->set_vm_result(NULL);
        UPDATE_PC_AND_TOS_AND_CONTINUE(4, -(dims-1));
      }
      CASE(_checkcast):
          if (STACK_OBJECT(-1) != NULL) {
            VERIFY_OOP(STACK_OBJECT(-1));
            u2 index = Bytes::get_Java_u2(pc+1);
            // Constant pool may have actual klass or unresolved klass. If it is
            // unresolved we must resolve it.
            if (METHOD->constants()->tag_at(index).is_unresolved_klass()) {
              CALL_VM(InterpreterRuntime::quicken_io_cc(THREAD), handle_exception);
            }
            Klass* klassOf = (Klass*) METHOD->constants()->resolved_klass_at(index);
            Klass* objKlass = STACK_OBJECT(-1)->klass(); // ebx
            //
            // Check for compatibilty. This check must not GC!!
            // Seems way more expensive now that we must dispatch.
            //
            if (objKlass != klassOf && !objKlass->is_subtype_of(klassOf)) {
              ResourceMark rm(THREAD);
              char* message = SharedRuntime::generate_class_cast_message(
                objKlass, klassOf);
              VM_JAVA_ERROR(vmSymbols::java_lang_ClassCastException(), message);
            }
          }
          UPDATE_PC_AND_CONTINUE(3);

      CASE(_instanceof):
          if (STACK_OBJECT(-1) == NULL) {
            SET_STACK_INT(0, -1);
          } else {
            VERIFY_OOP(STACK_OBJECT(-1));
            u2 index = Bytes::get_Java_u2(pc+1);
            // Constant pool may have actual klass or unresolved klass. If it is
            // unresolved we must resolve it.
            if (METHOD->constants()->tag_at(index).is_unresolved_klass()) {
              CALL_VM(InterpreterRuntime::quicken_io_cc(THREAD), handle_exception);
            }
            Klass* klassOf = (Klass*) METHOD->constants()->resolved_klass_at(index);
            Klass* objKlass = STACK_OBJECT(-1)->klass();
            //
            // Check for compatibilty. This check must not GC!!
            // Seems way more expensive now that we must dispatch.
            //
            if ( objKlass == klassOf || objKlass->is_subtype_of(klassOf)) {
              SET_STACK_INT(1, -1);
            } else {
              SET_STACK_INT(0, -1);
            }
          }
          UPDATE_PC_AND_CONTINUE(3);

      CASE(_ldc_w):
      CASE(_ldc):
        {
          u2 index;
          bool wide = false;
          int incr = 2; // frequent case
          if (opcode == Bytecodes::_ldc) {
            index = pc[1];
          } else {
            index = Bytes::get_Java_u2(pc+1);
            incr = 3;
            wide = true;
          }

          ConstantPool* constants = METHOD->constants();
          switch (constants->tag_at(index).value()) {
          case JVM_CONSTANT_Integer:
            SET_STACK_INT(constants->int_at(index), 0);
            break;

          case JVM_CONSTANT_Float:
            SET_STACK_FLOAT(constants->float_at(index), 0);
            break;

          case JVM_CONSTANT_String:
            {
              oop result = constants->resolved_references()->obj_at(index);
              if (result == NULL) {
                CALL_VM(InterpreterRuntime::resolve_ldc(THREAD, (Bytecodes::Code) opcode), handle_exception);
                SET_STACK_OBJECT(THREAD->vm_result(), 0);
                THREAD->set_vm_result(NULL);
              } else {
                VERIFY_OOP(result);
                SET_STACK_OBJECT(result, 0);
              }
            break;
            }

          case JVM_CONSTANT_Class:
            VERIFY_OOP(constants->resolved_klass_at(index)->java_mirror());
            SET_STACK_OBJECT(constants->resolved_klass_at(index)->java_mirror(), 0);
            break;

          case JVM_CONSTANT_UnresolvedClass:
          case JVM_CONSTANT_UnresolvedClassInError:
            CALL_VM(InterpreterRuntime::ldc(THREAD, wide), handle_exception);
            SET_STACK_OBJECT(THREAD->vm_result(), 0);
            THREAD->set_vm_result(NULL);
            break;

          case JVM_CONSTANT_Dynamic:
          case JVM_CONSTANT_DynamicInError:
            {
              CALL_VM(InterpreterRuntime::resolve_ldc(THREAD, (Bytecodes::Code) opcode), handle_exception);
              oop result = THREAD->vm_result();
              VERIFY_OOP(result);

              jvalue value;
              BasicType type = java_lang_boxing_object::get_value(result, &value);
              switch (type) {
              case T_FLOAT:   SET_STACK_FLOAT(value.f, 0); break;
              case T_INT:     SET_STACK_INT(value.i, 0); break;
              case T_SHORT:   SET_STACK_INT(value.s, 0); break;
              case T_BYTE:    SET_STACK_INT(value.b, 0); break;
              case T_CHAR:    SET_STACK_INT(value.c, 0); break;
              case T_BOOLEAN: SET_STACK_INT(value.z, 0); break;
              default:  ShouldNotReachHere();
              }

              break;
            }

          default:  ShouldNotReachHere();
          }
          UPDATE_PC_AND_TOS_AND_CONTINUE(incr, 1);
        }

      CASE(_ldc2_w):
        {
          u2 index = Bytes::get_Java_u2(pc+1);

          ConstantPool* constants = METHOD->constants();
          switch (constants->tag_at(index).value()) {

          case JVM_CONSTANT_Long:
             SET_STACK_LONG(constants->long_at(index), 1);
            break;

          case JVM_CONSTANT_Double:
             SET_STACK_DOUBLE(constants->double_at(index), 1);
            break;

          case JVM_CONSTANT_Dynamic:
          case JVM_CONSTANT_DynamicInError:
            {
              CALL_VM(InterpreterRuntime::resolve_ldc(THREAD, (Bytecodes::Code) opcode), handle_exception);
              oop result = THREAD->vm_result();
              VERIFY_OOP(result);

              jvalue value;
              BasicType type = java_lang_boxing_object::get_value(result, &value);
              switch (type) {
              case T_DOUBLE: SET_STACK_DOUBLE(value.d, 1); break;
              case T_LONG:   SET_STACK_LONG(value.j, 1); break;
              default:  ShouldNotReachHere();
              }

              break;
            }

          default:  ShouldNotReachHere();
          }
          UPDATE_PC_AND_TOS_AND_CONTINUE(3, 2);
        }

      CASE(_fast_aldc_w):
      CASE(_fast_aldc): {
        u2 index;
        int incr;
        if (opcode == Bytecodes::_fast_aldc) {
          index = pc[1];
          incr = 2;
        } else {
          index = Bytes::get_native_u2(pc+1);
          incr = 3;
        }

        // We are resolved if the resolved_references array contains a non-null object (CallSite, etc.)
        // This kind of CP cache entry does not need to match the flags byte, because
        // there is a 1-1 relation between bytecode type and CP entry type.
        ConstantPool* constants = METHOD->constants();
        oop result = constants->resolved_references()->obj_at(index);
        if (result == NULL) {
          CALL_VM(InterpreterRuntime::resolve_ldc(THREAD, (Bytecodes::Code) opcode),
                  handle_exception);
          result = THREAD->vm_result();
        }
        if (result == Universe::the_null_sentinel())
          result = NULL;

        VERIFY_OOP(result);
        SET_STACK_OBJECT(result, 0);
        UPDATE_PC_AND_TOS_AND_CONTINUE(incr, 1);
      }

      CASE(_invokedynamic): {

        u4 index = Bytes::get_native_u4(pc+1);
        ConstantPoolCacheEntry* cache = cp->constant_pool()->invokedynamic_cp_cache_entry_at(index);

        // We are resolved if the resolved_references array contains a non-null object (CallSite, etc.)
        // This kind of CP cache entry does not need to match the flags byte, because
        // there is a 1-1 relation between bytecode type and CP entry type.
        if (! cache->is_resolved((Bytecodes::Code) opcode)) {
          CALL_VM(InterpreterRuntime::resolve_from_cache(THREAD, (Bytecodes::Code)opcode),
                  handle_exception);
          cache = cp->constant_pool()->invokedynamic_cp_cache_entry_at(index);
        }

        Method* method = cache->f1_as_method();
        if (VerifyOops) method->verify();

        if (cache->has_appendix()) {
          constantPoolHandle cp(THREAD, METHOD->constants());
          SET_STACK_OBJECT(cache->appendix_if_resolved(cp), 0);
          MORE_STACK(1);
        }

        istate->set_msg(call_method);
        istate->set_callee(method);
        istate->set_callee_entry_point(method->from_interpreted_entry());
        istate->set_bcp_advance(5);

        UPDATE_PC_AND_RETURN(0); // I'll be back...
      }

      CASE(_invokehandle): {

        u2 index = Bytes::get_native_u2(pc+1);
        ConstantPoolCacheEntry* cache = cp->entry_at(index);

        if (! cache->is_resolved((Bytecodes::Code) opcode)) {
          CALL_VM(InterpreterRuntime::resolve_from_cache(THREAD, (Bytecodes::Code)opcode),
                  handle_exception);
          cache = cp->entry_at(index);
        }

        Method* method = cache->f1_as_method();
        if (VerifyOops) method->verify();

        if (cache->has_appendix()) {
          constantPoolHandle cp(THREAD, METHOD->constants());
          SET_STACK_OBJECT(cache->appendix_if_resolved(cp), 0);
          MORE_STACK(1);
        }

        istate->set_msg(call_method);
        istate->set_callee(method);
        istate->set_callee_entry_point(method->from_interpreted_entry());
        istate->set_bcp_advance(3);

        UPDATE_PC_AND_RETURN(0); // I'll be back...
      }

      CASE(_invokeinterface): {
        u2 index = Bytes::get_native_u2(pc+1);

        // QQQ Need to make this as inlined as possible. Probably need to split all the bytecode cases
        // out so c++ compiler has a chance for constant prop to fold everything possible away.

        ConstantPoolCacheEntry* cache = cp->entry_at(index);
        if (!cache->is_resolved((Bytecodes::Code)opcode)) {
          CALL_VM(InterpreterRuntime::resolve_from_cache(THREAD, (Bytecodes::Code)opcode),
                  handle_exception);
          cache = cp->entry_at(index);
        }

        istate->set_msg(call_method);

        // Special case of invokeinterface called for virtual method of
        // java.lang.Object.  See cpCache.cpp for details.
        Method* callee = NULL;
        if (cache->is_forced_virtual()) {
          CHECK_NULL(STACK_OBJECT(-(cache->parameter_size())));
          if (cache->is_vfinal()) {
            callee = cache->f2_as_vfinal_method();
          } else {
            // Get receiver.
            int parms = cache->parameter_size();
            // Same comments as invokevirtual apply here.
            oop rcvr = STACK_OBJECT(-parms);
            VERIFY_OOP(rcvr);
            Klass* rcvrKlass = rcvr->klass();
            callee = (Method*) rcvrKlass->method_at_vtable(cache->f2_as_index());
          }
        } else if (cache->is_vfinal()) {
          // private interface method invocations
          //
          // Ensure receiver class actually implements
          // the resolved interface class. The link resolver
          // does this, but only for the first time this
          // interface is being called.
          int parms = cache->parameter_size();
          oop rcvr = STACK_OBJECT(-parms);
          CHECK_NULL(rcvr);
          Klass* recv_klass = rcvr->klass();
          Klass* resolved_klass = cache->f1_as_klass();
          if (!recv_klass->is_subtype_of(resolved_klass)) {
            ResourceMark rm(THREAD);
            char buf[200];
            jio_snprintf(buf, sizeof(buf), "Class %s does not implement the requested interface %s",
              recv_klass->external_name(),
              resolved_klass->external_name());
            VM_JAVA_ERROR(vmSymbols::java_lang_IncompatibleClassChangeError(), buf);
          }
          callee = cache->f2_as_vfinal_method();
        }
        if (callee != NULL) {
          istate->set_callee(callee);
          istate->set_callee_entry_point(callee->from_interpreted_entry());
          if (JVMTI_ENABLED && THREAD->is_interp_only_mode()) {
            istate->set_callee_entry_point(callee->interpreter_entry());
          }
          istate->set_bcp_advance(5);
          UPDATE_PC_AND_RETURN(0); // I'll be back...
        }

        // this could definitely be cleaned up QQQ
        Method *interface_method = cache->f2_as_interface_method();
        InstanceKlass* iclass = interface_method->method_holder();

        // get receiver
        int parms = cache->parameter_size();
        oop rcvr = STACK_OBJECT(-parms);
        CHECK_NULL(rcvr);
        InstanceKlass* int2 = (InstanceKlass*) rcvr->klass();

        // Receiver subtype check against resolved interface klass (REFC).
        {
          Klass* refc = cache->f1_as_klass();
          itableOffsetEntry* scan;
          for (scan = (itableOffsetEntry*) int2->start_of_itable();
               scan->interface_klass() != NULL;
               scan++) {
            if (scan->interface_klass() == refc) {
              break;
            }
          }
          // Check that the entry is non-null.  A null entry means
          // that the receiver class doesn't implement the
          // interface, and wasn't the same as when the caller was
          // compiled.
          if (scan->interface_klass() == NULL) {
            VM_JAVA_ERROR(vmSymbols::java_lang_IncompatibleClassChangeError(), "");
          }
        }

        itableOffsetEntry* ki = (itableOffsetEntry*) int2->start_of_itable();
        int i;
        for ( i = 0 ; i < int2->itable_length() ; i++, ki++ ) {
          if (ki->interface_klass() == iclass) break;
        }
        // If the interface isn't found, this class doesn't implement this
        // interface. The link resolver checks this but only for the first
        // time this interface is called.
        if (i == int2->itable_length()) {
          CALL_VM(InterpreterRuntime::throw_IncompatibleClassChangeErrorVerbose(THREAD, rcvr->klass(), iclass),
                  handle_exception);
        }
        int mindex = interface_method->itable_index();

        itableMethodEntry* im = ki->first_method_entry(rcvr->klass());
        callee = im[mindex].method();
        if (callee == NULL) {
          CALL_VM(InterpreterRuntime::throw_AbstractMethodErrorVerbose(THREAD, rcvr->klass(), interface_method),
                  handle_exception);
        }

        istate->set_callee(callee);
        istate->set_callee_entry_point(callee->from_interpreted_entry());
        if (JVMTI_ENABLED && THREAD->is_interp_only_mode()) {
          istate->set_callee_entry_point(callee->interpreter_entry());
        }
        istate->set_bcp_advance(5);
        UPDATE_PC_AND_RETURN(0); // I'll be back...
      }

      CASE(_invokevirtual):
      CASE(_invokespecial):
      CASE(_invokestatic): {
        u2 index = Bytes::get_native_u2(pc+1);

        ConstantPoolCacheEntry* cache = cp->entry_at(index);
        // QQQ Need to make this as inlined as possible. Probably need to split all the bytecode cases
        // out so c++ compiler has a chance for constant prop to fold everything possible away.

        if (!cache->is_resolved((Bytecodes::Code)opcode)) {
          CALL_VM(InterpreterRuntime::resolve_from_cache(THREAD, (Bytecodes::Code)opcode),
                  handle_exception);
          cache = cp->entry_at(index);
        }

        istate->set_msg(call_method);
        {
          Method* callee;
          if ((Bytecodes::Code)opcode == Bytecodes::_invokevirtual) {
            CHECK_NULL(STACK_OBJECT(-(cache->parameter_size())));
            if (cache->is_vfinal()) {
              callee = cache->f2_as_vfinal_method();
            } else {
              // get receiver
              int parms = cache->parameter_size();
              // this works but needs a resourcemark and seems to create a vtable on every call:
              // Method* callee = rcvr->klass()->vtable()->method_at(cache->f2_as_index());
              //
              // this fails with an assert
              // InstanceKlass* rcvrKlass = InstanceKlass::cast(STACK_OBJECT(-parms)->klass());
              // but this works
              oop rcvr = STACK_OBJECT(-parms);
              VERIFY_OOP(rcvr);
              Klass* rcvrKlass = rcvr->klass();
              /*
                Executing this code in java.lang.String:
                    public String(char value[]) {
                          this.count = value.length;
                          this.value = (char[])value.clone();
                     }

                 a find on rcvr->klass() reports:
                 {type array char}{type array class}
                  - klass: {other class}

                  but using InstanceKlass::cast(STACK_OBJECT(-parms)->klass()) causes in assertion failure
                  because rcvr->klass()->is_instance_klass() == 0
                  However it seems to have a vtable in the right location. Huh?
                  Because vtables have the same offset for ArrayKlass and InstanceKlass.
              */
              callee = (Method*) rcvrKlass->method_at_vtable(cache->f2_as_index());
            }
          } else {
            if ((Bytecodes::Code)opcode == Bytecodes::_invokespecial) {
              CHECK_NULL(STACK_OBJECT(-(cache->parameter_size())));
            }
            callee = cache->f1_as_method();
          }

          istate->set_callee(callee);
          istate->set_callee_entry_point(callee->from_interpreted_entry());
          if (JVMTI_ENABLED && THREAD->is_interp_only_mode()) {
            istate->set_callee_entry_point(callee->interpreter_entry());
          }
          istate->set_bcp_advance(3);
          UPDATE_PC_AND_RETURN(0); // I'll be back...
        }
      }

      /* Allocate memory for a new java object. */

      CASE(_newarray): {
        BasicType atype = (BasicType) *(pc+1);
        jint size = STACK_INT(-1);
        CALL_VM(InterpreterRuntime::newarray(THREAD, atype, size),
                handle_exception);
        // Must prevent reordering of stores for object initialization
        // with stores that publish the new object.
        OrderAccess::storestore();
        SET_STACK_OBJECT(THREAD->vm_result(), -1);
        THREAD->set_vm_result(NULL);

        UPDATE_PC_AND_CONTINUE(2);
      }

      /* Throw an exception. */

      CASE(_athrow): {
          oop except_oop = STACK_OBJECT(-1);
          CHECK_NULL(except_oop);
          // set pending_exception so we use common code
          THREAD->set_pending_exception(except_oop, NULL, 0);
          goto handle_exception;
      }

      /* goto and jsr. They are exactly the same except jsr pushes
       * the address of the next instruction first.
       */

      CASE(_jsr): {
          /* push bytecode index on stack */
          SET_STACK_ADDR(((address)pc - (intptr_t)(istate->method()->code_base()) + 3), 0);
          MORE_STACK(1);
          /* FALL THROUGH */
      }

      CASE(_goto):
      {
          int16_t offset = (int16_t)Bytes::get_Java_u2(pc + 1);
          address branch_pc = pc;
          UPDATE_PC(offset);
          DO_BACKEDGE_CHECKS(offset, branch_pc);
          CONTINUE;
      }

      CASE(_jsr_w): {
          /* push return address on the stack */
          SET_STACK_ADDR(((address)pc - (intptr_t)(istate->method()->code_base()) + 5), 0);
          MORE_STACK(1);
          /* FALL THROUGH */
      }

      CASE(_goto_w):
      {
          int32_t offset = Bytes::get_Java_u4(pc + 1);
          address branch_pc = pc;
          UPDATE_PC(offset);
          DO_BACKEDGE_CHECKS(offset, branch_pc);
          CONTINUE;
      }

      /* return from a jsr or jsr_w */

      CASE(_ret): {
          pc = istate->method()->code_base() + (intptr_t)(LOCALS_ADDR(pc[1]));
          UPDATE_PC_AND_CONTINUE(0);
      }

      /* debugger breakpoint */

      CASE(_breakpoint): {
          Bytecodes::Code original_bytecode;
          DECACHE_STATE();
          SET_LAST_JAVA_FRAME();
          original_bytecode = InterpreterRuntime::get_original_bytecode_at(THREAD,
                              METHOD, pc);
          RESET_LAST_JAVA_FRAME();
          CACHE_STATE();
          if (THREAD->has_pending_exception()) goto handle_exception;
            CALL_VM(InterpreterRuntime::_breakpoint(THREAD, METHOD, pc),
                                                    handle_exception);

          opcode = (jubyte)original_bytecode;
          goto opcode_switch;
      }

      DEFAULT:
          fatal("Unimplemented opcode %d = %s", opcode,
                Bytecodes::name((Bytecodes::Code)opcode));
          goto finish;

      } /* switch(opc) */


#ifdef USELABELS
    check_for_exception:
#endif
    {
      if (!THREAD->has_pending_exception()) {
        CONTINUE;
      }
      /* We will be gcsafe soon, so flush our state. */
      DECACHE_PC();
      goto handle_exception;
    }
  do_continue: ;

  } /* while (1) interpreter loop */


  // An exception exists in the thread state see whether this activation can handle it
  handle_exception: {

    HandleMarkCleaner __hmc(THREAD);
    Handle except_oop(THREAD, THREAD->pending_exception());
    // Prevent any subsequent HandleMarkCleaner in the VM
    // from freeing the except_oop handle.
    HandleMark __hm(THREAD);

    THREAD->clear_pending_exception();
    assert(except_oop() != NULL, "No exception to process");
    intptr_t continuation_bci;
    // expression stack is emptied
    topOfStack = istate->stack_base() - Interpreter::stackElementWords;
    CALL_VM(continuation_bci = (intptr_t)InterpreterRuntime::exception_handler_for_exception(THREAD, except_oop()),
            handle_exception);

    except_oop = Handle(THREAD, THREAD->vm_result());
    THREAD->set_vm_result(NULL);
    if (continuation_bci >= 0) {
      // Place exception on top of stack
      SET_STACK_OBJECT(except_oop(), 0);
      MORE_STACK(1);
      pc = METHOD->code_base() + continuation_bci;
      if (log_is_enabled(Info, exceptions)) {
        ResourceMark rm(THREAD);
        stringStream tempst;
        tempst.print("interpreter method <%s>\n"
                     " at bci %d, continuing at %d for thread " INTPTR_FORMAT,
                     METHOD->print_value_string(),
                     (int)(istate->bcp() - METHOD->code_base()),
                     (int)continuation_bci, p2i(THREAD));
        Exceptions::log_exception(except_oop, tempst.as_string());
      }
      // for AbortVMOnException flag
      Exceptions::debug_check_abort(except_oop);
      goto run;
    }
    if (log_is_enabled(Info, exceptions)) {
      ResourceMark rm;
      stringStream tempst;
      tempst.print("interpreter method <%s>\n"
             " at bci %d, unwinding for thread " INTPTR_FORMAT,
             METHOD->print_value_string(),
             (int)(istate->bcp() - METHOD->code_base()),
             p2i(THREAD));
      Exceptions::log_exception(except_oop, tempst.as_string());
    }
    // for AbortVMOnException flag
    Exceptions::debug_check_abort(except_oop);

    // No handler in this activation, unwind and try again
    THREAD->set_pending_exception(except_oop(), NULL, 0);
    goto handle_return;
  }  // handle_exception:

  // Return from an interpreter invocation with the result of the interpretation
  // on the top of the Java Stack (or a pending exception)

  handle_Pop_Frame: {

    // We don't really do anything special here except we must be aware
    // that we can get here without ever locking the method (if sync).
    // Also we skip the notification of the exit.

    istate->set_msg(popping_frame);
    // Clear pending so while the pop is in process
    // we don't start another one if a call_vm is done.
    THREAD->clear_popframe_condition();
    // Let interpreter (only) see the we're in the process of popping a frame
    THREAD->set_pop_frame_in_process();

    goto handle_return;

  } // handle_Pop_Frame

  // ForceEarlyReturn ends a method, and returns to the caller with a return value
  // given by the invoker of the early return.
  handle_Early_Return: {

    istate->set_msg(early_return);

    // Clear expression stack.
    topOfStack = istate->stack_base() - Interpreter::stackElementWords;

    JvmtiThreadState *ts = THREAD->jvmti_thread_state();

    // Push the value to be returned.
    switch (istate->method()->result_type()) {
      case T_BOOLEAN:
      case T_SHORT:
      case T_BYTE:
      case T_CHAR:
      case T_INT:
        SET_STACK_INT(ts->earlyret_value().i, 0);
        MORE_STACK(1);
        break;
      case T_LONG:
        SET_STACK_LONG(ts->earlyret_value().j, 1);
        MORE_STACK(2);
        break;
      case T_FLOAT:
        SET_STACK_FLOAT(ts->earlyret_value().f, 0);
        MORE_STACK(1);
        break;
      case T_DOUBLE:
        SET_STACK_DOUBLE(ts->earlyret_value().d, 1);
        MORE_STACK(2);
        break;
      case T_ARRAY:
      case T_OBJECT:
        SET_STACK_OBJECT(ts->earlyret_oop(), 0);
        MORE_STACK(1);
        break;
    }

    ts->clr_earlyret_value();
    ts->set_earlyret_oop(NULL);
    ts->clr_earlyret_pending();

    // Fall through to handle_return.

  } // handle_Early_Return

  handle_return: {
    // A storestore barrier is required to order initialization of
    // final fields with publishing the reference to the object that
    // holds the field. Without the barrier the value of final fields
    // can be observed to change.
    OrderAccess::storestore();

    DECACHE_STATE();

    bool suppress_error = istate->msg() == popping_frame || istate->msg() == early_return;
    bool suppress_exit_event = THREAD->has_pending_exception() || istate->msg() == popping_frame;
    Handle original_exception(THREAD, THREAD->pending_exception());
    Handle illegal_state_oop(THREAD, NULL);

    // We'd like a HandleMark here to prevent any subsequent HandleMarkCleaner
    // in any following VM entries from freeing our live handles, but illegal_state_oop
    // isn't really allocated yet and so doesn't become live until later and
    // in unpredicatable places. Instead we must protect the places where we enter the
    // VM. It would be much simpler (and safer) if we could allocate a real handle with
    // a NULL oop in it and then overwrite the oop later as needed. This isn't
    // unfortunately isn't possible.

    if (THREAD->has_pending_exception()) {
      THREAD->clear_pending_exception();
    }

    //
    // As far as we are concerned we have returned. If we have a pending exception
    // that will be returned as this invocation's result. However if we get any
    // exception(s) while checking monitor state one of those IllegalMonitorStateExceptions
    // will be our final result (i.e. monitor exception trumps a pending exception).
    //

    // If we never locked the method (or really passed the point where we would have),
    // there is no need to unlock it (or look for other monitors), since that
    // could not have happened.

    if (THREAD->do_not_unlock()) {

      // Never locked, reset the flag now because obviously any caller must
      // have passed their point of locking for us to have gotten here.

      THREAD->clr_do_not_unlock();
    } else {
      // At this point we consider that we have returned. We now check that the
      // locks were properly block structured. If we find that they were not
      // used properly we will return with an illegal monitor exception.
      // The exception is checked by the caller not the callee since this
      // checking is considered to be part of the invocation and therefore
      // in the callers scope (JVM spec 8.13).
      //
      // Another weird thing to watch for is if the method was locked
      // recursively and then not exited properly. This means we must
      // examine all the entries in reverse time(and stack) order and
      // unlock as we find them. If we find the method monitor before
      // we are at the initial entry then we should throw an exception.
      // It is not clear the template based interpreter does this
      // correctly

      BasicObjectLock* base = istate->monitor_base();
      BasicObjectLock* end = (BasicObjectLock*) istate->stack_base();
      bool method_unlock_needed = METHOD->is_synchronized();
      // We know the initial monitor was used for the method don't check that
      // slot in the loop
      if (method_unlock_needed) base--;

      // Check all the monitors to see they are unlocked. Install exception if found to be locked.
      while (end < base) {
        oop lockee = end->obj();
        if (lockee != NULL) {
          BasicLock* lock = end->lock();
          markWord header = lock->displaced_header();
          end->set_obj(NULL);

          // If it isn't recursive we either must swap old header or call the runtime
          if (header.to_pointer() != NULL) {
            markWord old_header = markWord::encode(lock);
            if (lockee->cas_set_mark(header, old_header) != old_header) {
              // restore object for the slow case
              end->set_obj(lockee);
              InterpreterRuntime::monitorexit(end);
            }
          }

          // One error is plenty
          if (illegal_state_oop() == NULL && !suppress_error) {
            {
              // Prevent any HandleMarkCleaner from freeing our live handles
              HandleMark __hm(THREAD);
              CALL_VM_NOCHECK(InterpreterRuntime::throw_illegal_monitor_state_exception(THREAD));
            }
            assert(THREAD->has_pending_exception(), "Lost our exception!");
            illegal_state_oop = Handle(THREAD, THREAD->pending_exception());
            THREAD->clear_pending_exception();
          }
        }
        end++;
      }
      // Unlock the method if needed
      if (method_unlock_needed) {
        if (base->obj() == NULL) {
          // The method is already unlocked this is not good.
          if (illegal_state_oop() == NULL && !suppress_error) {
            {
              // Prevent any HandleMarkCleaner from freeing our live handles
              HandleMark __hm(THREAD);
              CALL_VM_NOCHECK(InterpreterRuntime::throw_illegal_monitor_state_exception(THREAD));
            }
            assert(THREAD->has_pending_exception(), "Lost our exception!");
            illegal_state_oop = Handle(THREAD, THREAD->pending_exception());
            THREAD->clear_pending_exception();
          }
        } else {
          //
          // The initial monitor is always used for the method
          // However if that slot is no longer the oop for the method it was unlocked
          // and reused by something that wasn't unlocked!
          //
          // deopt can come in with rcvr dead because c2 knows
          // its value is preserved in the monitor. So we can't use locals[0] at all
          // and must use first monitor slot.
          //
          oop rcvr = base->obj();
          if (rcvr == NULL) {
            if (!suppress_error) {
              VM_JAVA_ERROR_NO_JUMP(vmSymbols::java_lang_NullPointerException(), "");
              illegal_state_oop = Handle(THREAD, THREAD->pending_exception());
              THREAD->clear_pending_exception();
            }
          } else if (UseHeavyMonitors) {
            InterpreterRuntime::monitorexit(base);
            if (THREAD->has_pending_exception()) {
              if (!suppress_error) illegal_state_oop = Handle(THREAD, THREAD->pending_exception());
              THREAD->clear_pending_exception();
            }
          } else {
            BasicLock* lock = base->lock();
            markWord header = lock->displaced_header();
            base->set_obj(NULL);

            // If it isn't recursive we either must swap old header or call the runtime
            if (header.to_pointer() != NULL) {
              markWord old_header = markWord::encode(lock);
              if (rcvr->cas_set_mark(header, old_header) != old_header) {
                // restore object for the slow case
                base->set_obj(rcvr);
                InterpreterRuntime::monitorexit(base);
                if (THREAD->has_pending_exception()) {
                  if (!suppress_error) illegal_state_oop = Handle(THREAD, THREAD->pending_exception());
                  THREAD->clear_pending_exception();
                }
              }
            }
          }
        }
      }
    }
    // Clear the do_not_unlock flag now.
    THREAD->clr_do_not_unlock();

    //
    // Notify jvmti/jvmdi
    //
    // NOTE: we do not notify a method_exit if we have a pending exception,
    // including an exception we generate for unlocking checks.  In the former
    // case, JVMDI has already been notified by our call for the exception handler
    // and in both cases as far as JVMDI is concerned we have already returned.
    // If we notify it again JVMDI will be all confused about how many frames
    // are still on the stack (4340444).
    //
    // NOTE Further! It turns out the the JVMTI spec in fact expects to see
    // method_exit events whenever we leave an activation unless it was done
    // for popframe. This is nothing like jvmdi. However we are passing the
    // tests at the moment (apparently because they are jvmdi based) so rather
    // than change this code and possibly fail tests we will leave it alone
    // (with this note) in anticipation of changing the vm and the tests
    // simultaneously.

    suppress_exit_event = suppress_exit_event || illegal_state_oop() != NULL;

    // Whenever JVMTI puts a thread in interp_only_mode, method
    // entry/exit events are sent for that thread to track stack depth.

    if (JVMTI_ENABLED && !suppress_exit_event && THREAD->is_interp_only_mode()) {
      // Prevent any HandleMarkCleaner from freeing our live handles
      HandleMark __hm(THREAD);
      CALL_VM_NOCHECK(InterpreterRuntime::post_method_exit(THREAD));
    }

    //
    // See if we are returning any exception
    // A pending exception that was pending prior to a possible popping frame
    // overrides the popping frame.
    //
    assert(!suppress_error || (suppress_error && illegal_state_oop() == NULL), "Error was not suppressed");
    if (illegal_state_oop() != NULL || original_exception() != NULL) {
      // Inform the frame manager we have no result.
      istate->set_msg(throwing_exception);
      if (illegal_state_oop() != NULL)
        THREAD->set_pending_exception(illegal_state_oop(), NULL, 0);
      else
        THREAD->set_pending_exception(original_exception(), NULL, 0);
      UPDATE_PC_AND_RETURN(0);
    }

    if (istate->msg() == popping_frame) {
      // Make it simpler on the assembly code and set the message for the frame pop.
      // returns
      if (istate->prev() == NULL) {
        // We must be returning to a deoptimized frame (because popframe only happens between
        // two interpreted frames). We need to save the current arguments in C heap so that
        // the deoptimized frame when it restarts can copy the arguments to its expression
        // stack and re-execute the call. We also have to notify deoptimization that this
        // has occurred and to pick the preserved args copy them to the deoptimized frame's
        // java expression stack. Yuck.
        //
        THREAD->popframe_preserve_args(in_ByteSize(METHOD->size_of_parameters() * wordSize),
                                LOCALS_SLOT(METHOD->size_of_parameters() - 1));
        THREAD->set_popframe_condition_bit(JavaThread::popframe_force_deopt_reexecution_bit);
      }
    } else {
      istate->set_msg(return_from_method);
    }

    // Normal return
    // Advance the pc and return to frame manager
    UPDATE_PC_AND_RETURN(1);
  } /* handle_return: */

// This is really a fatal error return

finish:
  DECACHE_TOS();
  DECACHE_PC();

  return;
}

// This constructor should only be used to contruct the object to signal
// interpreter initialization. All other instances should be created by
// the frame manager.
BytecodeInterpreter::BytecodeInterpreter(messages msg) {
  if (msg != initialize) ShouldNotReachHere();
  _msg = msg;
  _self_link = this;
  _prev_link = NULL;
}

void BytecodeInterpreter::astore(intptr_t* tos,    int stack_offset,
                          intptr_t* locals, int locals_offset) {
  intptr_t value = tos[Interpreter::expr_index_at(-stack_offset)];
  locals[Interpreter::local_index_at(-locals_offset)] = value;
}

void BytecodeInterpreter::copy_stack_slot(intptr_t *tos, int from_offset,
                                   int to_offset) {
  tos[Interpreter::expr_index_at(-to_offset)] =
                      (intptr_t)tos[Interpreter::expr_index_at(-from_offset)];
}

void BytecodeInterpreter::dup(intptr_t *tos) {
  copy_stack_slot(tos, -1, 0);
}

void BytecodeInterpreter::dup2(intptr_t *tos) {
  copy_stack_slot(tos, -2, 0);
  copy_stack_slot(tos, -1, 1);
}

void BytecodeInterpreter::dup_x1(intptr_t *tos) {
  /* insert top word two down */
  copy_stack_slot(tos, -1, 0);
  copy_stack_slot(tos, -2, -1);
  copy_stack_slot(tos, 0, -2);
}

void BytecodeInterpreter::dup_x2(intptr_t *tos) {
  /* insert top word three down  */
  copy_stack_slot(tos, -1, 0);
  copy_stack_slot(tos, -2, -1);
  copy_stack_slot(tos, -3, -2);
  copy_stack_slot(tos, 0, -3);
}
void BytecodeInterpreter::dup2_x1(intptr_t *tos) {
  /* insert top 2 slots three down */
  copy_stack_slot(tos, -1, 1);
  copy_stack_slot(tos, -2, 0);
  copy_stack_slot(tos, -3, -1);
  copy_stack_slot(tos, 1, -2);
  copy_stack_slot(tos, 0, -3);
}
void BytecodeInterpreter::dup2_x2(intptr_t *tos) {
  /* insert top 2 slots four down */
  copy_stack_slot(tos, -1, 1);
  copy_stack_slot(tos, -2, 0);
  copy_stack_slot(tos, -3, -1);
  copy_stack_slot(tos, -4, -2);
  copy_stack_slot(tos, 1, -3);
  copy_stack_slot(tos, 0, -4);
}


void BytecodeInterpreter::swap(intptr_t *tos) {
  // swap top two elements
  intptr_t val = tos[Interpreter::expr_index_at(1)];
  // Copy -2 entry to -1
  copy_stack_slot(tos, -2, -1);
  // Store saved -1 entry into -2
  tos[Interpreter::expr_index_at(2)] = val;
}
// --------------------------------------------------------------------------------
// Non-product code
#ifndef PRODUCT

const char* BytecodeInterpreter::C_msg(BytecodeInterpreter::messages msg) {
  switch (msg) {
     case BytecodeInterpreter::no_request:  return("no_request");
     case BytecodeInterpreter::initialize:  return("initialize");
     // status message to C++ interpreter
     case BytecodeInterpreter::method_entry:  return("method_entry");
     case BytecodeInterpreter::method_resume:  return("method_resume");
     case BytecodeInterpreter::got_monitors:  return("got_monitors");
     case BytecodeInterpreter::rethrow_exception:  return("rethrow_exception");
     // requests to frame manager from C++ interpreter
     case BytecodeInterpreter::call_method:  return("call_method");
     case BytecodeInterpreter::return_from_method:  return("return_from_method");
     case BytecodeInterpreter::more_monitors:  return("more_monitors");
     case BytecodeInterpreter::throwing_exception:  return("throwing_exception");
     case BytecodeInterpreter::popping_frame:  return("popping_frame");
     case BytecodeInterpreter::do_osr:  return("do_osr");
     // deopt
     case BytecodeInterpreter::deopt_resume:  return("deopt_resume");
     case BytecodeInterpreter::deopt_resume2:  return("deopt_resume2");
     default: return("BAD MSG");
  }
}
void
BytecodeInterpreter::print() {
  tty->print_cr("thread: " INTPTR_FORMAT, (uintptr_t) this->_thread);
  tty->print_cr("bcp: " INTPTR_FORMAT, (uintptr_t) this->_bcp);
  tty->print_cr("locals: " INTPTR_FORMAT, (uintptr_t) this->_locals);
  tty->print_cr("constants: " INTPTR_FORMAT, (uintptr_t) this->_constants);
  {
    ResourceMark rm;
    char *method_name = _method->name_and_sig_as_C_string();
    tty->print_cr("method: " INTPTR_FORMAT "[ %s ]",  (uintptr_t) this->_method, method_name);
  }
  tty->print_cr("stack: " INTPTR_FORMAT, (uintptr_t) this->_stack);
  tty->print_cr("msg: %s", C_msg(this->_msg));
  tty->print_cr("result_to_call._callee: " INTPTR_FORMAT, (uintptr_t) this->_result._to_call._callee);
  tty->print_cr("result_to_call._callee_entry_point: " INTPTR_FORMAT, (uintptr_t) this->_result._to_call._callee_entry_point);
  tty->print_cr("result_to_call._bcp_advance: %d ", this->_result._to_call._bcp_advance);
  tty->print_cr("osr._osr_buf: " INTPTR_FORMAT, (uintptr_t) this->_result._osr._osr_buf);
  tty->print_cr("osr._osr_entry: " INTPTR_FORMAT, (uintptr_t) this->_result._osr._osr_entry);
  tty->print_cr("prev_link: " INTPTR_FORMAT, (uintptr_t) this->_prev_link);
  tty->print_cr("native_mirror: " INTPTR_FORMAT, (uintptr_t) p2i(this->_oop_temp));
  tty->print_cr("stack_base: " INTPTR_FORMAT, (uintptr_t) this->_stack_base);
  tty->print_cr("stack_limit: " INTPTR_FORMAT, (uintptr_t) this->_stack_limit);
  tty->print_cr("monitor_base: " INTPTR_FORMAT, (uintptr_t) this->_monitor_base);
  tty->print_cr("self_link: " INTPTR_FORMAT, (uintptr_t) this->_self_link);
}

extern "C" {
  void PI(uintptr_t arg) {
    ((BytecodeInterpreter*)arg)->print();
  }
}
#endif // PRODUCT
