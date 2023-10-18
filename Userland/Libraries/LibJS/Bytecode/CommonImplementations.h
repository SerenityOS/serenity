/*
 * Copyright (c) 2021-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Bytecode/IdentifierTable.h>
#include <LibJS/Runtime/Completion.h>

namespace JS::Bytecode {

ThrowCompletionOr<NonnullGCPtr<Object>> base_object_for_get(Bytecode::Interpreter&, Value base_value);
ThrowCompletionOr<Value> get_by_id(Bytecode::Interpreter&, IdentifierTableIndex, Value base_value, Value this_value, u32 cache_index);

}
