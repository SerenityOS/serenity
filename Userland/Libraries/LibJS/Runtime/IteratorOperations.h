/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

// 7.4 Operations on Iterator Objects, https://tc39.es/ecma262/#sec-operations-on-iterator-objects

enum class IteratorHint {
    Sync,
    Async,
};

ThrowCompletionOr<Object*> get_iterator(GlobalObject&, Value value, IteratorHint hint = IteratorHint::Sync, Value method = {});
ThrowCompletionOr<Object*> iterator_next(Object& iterator, Value value = {});
ThrowCompletionOr<Object*> iterator_step(GlobalObject&, Object& iterator);
ThrowCompletionOr<bool> iterator_complete(GlobalObject&, Object& iterator_result);
ThrowCompletionOr<Value> iterator_value(GlobalObject&, Object& iterator_result);
void iterator_close(Object& iterator);
Object* create_iterator_result_object(GlobalObject&, Value value, bool done);
MarkedValueList iterable_to_list(GlobalObject&, Value iterable, Value method = {});

enum class CloseOnAbrupt {
    No,
    Yes
};
void get_iterator_values(GlobalObject&, Value value, Function<IterationDecision(Value)> callback, Value method = {}, CloseOnAbrupt close_on_abrupt = CloseOnAbrupt::Yes);

}
