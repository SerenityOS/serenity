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
ThrowCompletionOr<Value> perform_call(Interpreter&, Value this_value, Op::CallType, Value callee, MarkedVector<Value> argument_values);
ThrowCompletionOr<void> throw_if_needed_for_call(Interpreter&, Value callee, Op::CallType, Optional<StringTableIndex> const& expression_string);
ThrowCompletionOr<Value> typeof_variable(VM&, DeprecatedFlyString const&);
ThrowCompletionOr<void> set_variable(VM&, DeprecatedFlyString const&, Value, Op::EnvironmentMode, Op::SetVariable::InitializationMode);
Value new_function(VM&, FunctionExpression const&, Optional<IdentifierTableIndex> const& lhs_name, Optional<Register> const& home_object);
ThrowCompletionOr<void> put_by_value(VM&, Value base, Value property_key_value, Value value, Op::PropertyKind);
ThrowCompletionOr<Value> get_variable(Bytecode::Interpreter&, DeprecatedFlyString const& name, u32 cache_index);

struct CalleeAndThis {
    Value callee;
    Value this_value;
};
ThrowCompletionOr<CalleeAndThis> get_callee_and_this_from_environment(Bytecode::Interpreter&, DeprecatedFlyString const& name, u32 cache_index);
Value new_regexp(VM&, ParsedRegex const&, DeprecatedString const& pattern, DeprecatedString const& flags);

}
