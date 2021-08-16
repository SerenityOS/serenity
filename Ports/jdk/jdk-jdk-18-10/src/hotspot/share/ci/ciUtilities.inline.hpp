/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CI_CIUTILITIES_INLINE_HPP
#define SHARE_CI_CIUTILITIES_INLINE_HPP

#include "ci/ciUtilities.hpp"

#include "runtime/interfaceSupport.inline.hpp"

// Add a ci native entry wrapper?

// Bring the compilation thread into the VM state.
#define VM_ENTRY_MARK                       \
  CompilerThread* thread=CompilerThread::current(); \
  ThreadInVMfromNative __tiv(thread);       \
  HandleMarkCleaner __hm(thread);           \
  JavaThread* THREAD = thread; /* For exception macros. */ \
  debug_only(VMNativeEntryWrapper __vew;)



// Bring the compilation thread into the VM state.  No handle mark.
#define VM_QUICK_ENTRY_MARK                 \
  CompilerThread* thread=CompilerThread::current(); \
  ThreadInVMfromNative __tiv(thread);       \
/*                                          \
 * [TODO] The NoHandleMark line does nothing but declare a function prototype \
 * The NoHandkeMark constructor is NOT executed. If the ()'s are   \
 * removed, causes the NoHandleMark assert to trigger. \
 * debug_only(NoHandleMark __hm();)         \
 */                                         \
  JavaThread* THREAD = thread; /* For exception macros. */ \
  debug_only(VMNativeEntryWrapper __vew;)


#define EXCEPTION_CONTEXT \
  CompilerThread* thread = CompilerThread::current(); \
  JavaThread* THREAD = thread; // For exception macros.


#define GUARDED_VM_ENTRY(action)            \
  {if (IS_IN_VM) { action } else { VM_ENTRY_MARK; { action }}}

#define GUARDED_VM_QUICK_ENTRY(action)      \
  {if (IS_IN_VM) { action } else { VM_QUICK_ENTRY_MARK; { action }}}

#endif // SHARE_CI_CIUTILITIES_INLINE_HPP
