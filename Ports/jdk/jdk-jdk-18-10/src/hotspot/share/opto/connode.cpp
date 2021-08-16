/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/allocation.inline.hpp"
#include "opto/addnode.hpp"
#include "opto/compile.hpp"
#include "opto/connode.hpp"
#include "opto/machnode.hpp"
#include "opto/matcher.hpp"
#include "opto/memnode.hpp"
#include "opto/phaseX.hpp"
#include "opto/subnode.hpp"
#include "runtime/sharedRuntime.hpp"

// Optimization - Graph Style

//=============================================================================
//------------------------------hash-------------------------------------------
uint ConNode::hash() const {
  return (uintptr_t)in(TypeFunc::Control) + _type->hash();
}

//------------------------------make-------------------------------------------
ConNode *ConNode::make(const Type *t) {
  switch( t->basic_type() ) {
  case T_INT:         return new ConINode( t->is_int() );
  case T_LONG:        return new ConLNode( t->is_long() );
  case T_FLOAT:       return new ConFNode( t->is_float_constant() );
  case T_DOUBLE:      return new ConDNode( t->is_double_constant() );
  case T_VOID:        return new ConNode ( Type::TOP );
  case T_OBJECT:      return new ConPNode( t->is_ptr() );
  case T_ARRAY:       return new ConPNode( t->is_aryptr() );
  case T_ADDRESS:     return new ConPNode( t->is_ptr() );
  case T_NARROWOOP:   return new ConNNode( t->is_narrowoop() );
  case T_NARROWKLASS: return new ConNKlassNode( t->is_narrowklass() );
  case T_METADATA:    return new ConPNode( t->is_ptr() );
    // Expected cases:  TypePtr::NULL_PTR, any is_rawptr()
    // Also seen: AnyPtr(TopPTR *+top);
    // %%%% Stop using TypePtr::NULL_PTR to represent nulls:  use either TypeRawPtr::NULL_PTR
    // or else TypeOopPtr::NULL_PTR.  Then set Type::_basic_type[AnyPtr] = T_ILLEGAL
  default:
    ShouldNotReachHere();
    return NULL;
  }
}
