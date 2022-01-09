/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Optional.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

// 7.4 Operations on Iterator Objects, https://tc39.es/ecma262/#sec-operations-on-iterator-objects

enum class IteratorHint {
    Sync,
    Async,
};

ThrowCompletionOr<Object*> get_iterator(GlobalObject&, Value value, IteratorHint hint = IteratorHint::Sync, Optional<Value> method = {});
ThrowCompletionOr<Object*> iterator_next(Object& iterator, Optional<Value> value = {});
ThrowCompletionOr<Object*> iterator_step(GlobalObject&, Object& iterator);
ThrowCompletionOr<bool> iterator_complete(GlobalObject&, Object& iterator_result);
ThrowCompletionOr<Value> iterator_value(GlobalObject&, Object& iterator_result);
Completion iterator_close(Object& iterator, Completion completion);
Completion async_iterator_close(Object& iterator, Completion completion);
Object* create_iterator_result_object(GlobalObject&, Value value, bool done);
ThrowCompletionOr<MarkedValueList> iterable_to_list(GlobalObject&, Value iterable, Optional<Value> method = {});

using IteratorValueCallback = Function<Optional<Completion>(Value)>;
Completion get_iterator_values(GlobalObject& global_object, Value iterable, IteratorValueCallback callback, Optional<Value> method = {});

}
