/*
 * Copyright (c) 2021-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Bytecode/IdentifierTable.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Runtime/Completion.h>

namespace JS::Bytecode {

ThrowCompletionOr<NonnullGCPtr<Object>> base_object_for_get(Bytecode::Interpreter&, Value base_value);
ThrowCompletionOr<Value> get_by_id(Bytecode::Interpreter&, IdentifierTableIndex, Value base_value, Value this_value, u32 cache_index);
ThrowCompletionOr<Value> get_by_value(Bytecode::Interpreter&, Value base_value, Value property_key_value);
ThrowCompletionOr<Value> get_global(Bytecode::Interpreter&, IdentifierTableIndex, u32 cache_index);
ThrowCompletionOr<void> put_by_property_key(VM&, Value base, Value this_value, Value value, PropertyKey name, Op::PropertyKind kind);
template<typename InstructionType>
ThrowCompletionOr<Value> perform_call(Interpreter&, InstructionType const&, Value callee, MarkedVector<Value> argument_values);
template<typename InstructionType>
ThrowCompletionOr<void> throw_if_needed_for_call(Interpreter&, InstructionType const&, Value callee);

}
