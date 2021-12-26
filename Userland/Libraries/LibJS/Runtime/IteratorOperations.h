/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

// 7.4 Operations on Iterator Objects, https://tc39.es/ecma262/#sec-operations-on-iterator-objects

enum class IteratorHint {
    Sync,
    Async,
};

Object* get_iterator(GlobalObject&, Value value, IteratorHint hint = IteratorHint::Sync, Value method = {});
Object* iterator_next(Object& iterator, Value value = {});
bool iterator_complete(GlobalObject&, Object& iterator_result);
Value iterator_value(GlobalObject&, Object& iterator_result);
void iterator_close(Object& iterator);
Value create_iterator_result_object(GlobalObject&, Value value, bool done);
MarkedValueList iterable_to_list(GlobalObject&, Value iterable, Value method = {});

enum class CloseOnAbrupt {
    No,
    Yes
};
void get_iterator_values(GlobalObject&, Value value, AK::Function<IterationDecision(Value)> callback, Value method = {}, CloseOnAbrupt close_on_abrupt = CloseOnAbrupt::Yes);

}
