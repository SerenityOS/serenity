/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

// Common iterator operations defined in ECMA262 7.4
// https://tc39.es/ecma262/#sec-operations-on-iterator-objects

enum class IteratorHint {
    Sync,
    Async,
};

Object* get_iterator(GlobalObject&, Value value, IteratorHint hint = IteratorHint::Sync, Value method = {});
bool is_iterator_complete(Object& iterator_result);
Value create_iterator_result_object(GlobalObject&, Value value, bool done);

Object* iterator_next(Object& iterator, Value value = {});
void iterator_close(Object& iterator);

MarkedValueList iterable_to_list(GlobalObject&, Value iterable, Value method = {});

void get_iterator_values(GlobalObject&, Value value, AK::Function<IterationDecision(Value)> callback, Value method = {});

}
