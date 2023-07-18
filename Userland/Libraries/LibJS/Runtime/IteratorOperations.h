/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2022-2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Optional.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Iterator.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

// 7.4 Operations on Iterator Objects, https://tc39.es/ecma262/#sec-operations-on-iterator-objects

enum class IteratorHint {
    Sync,
    Async,
};

ThrowCompletionOr<IteratorRecord> get_iterator_from_method(VM&, Value, NonnullGCPtr<FunctionObject>);
ThrowCompletionOr<IteratorRecord> get_iterator(VM&, Value, IteratorHint);
ThrowCompletionOr<NonnullGCPtr<Object>> iterator_next(VM&, IteratorRecord const&, Optional<Value> = {});
ThrowCompletionOr<GCPtr<Object>> iterator_step(VM&, IteratorRecord const&);
ThrowCompletionOr<bool> iterator_complete(VM&, Object& iterator_result);
ThrowCompletionOr<Value> iterator_value(VM&, Object& iterator_result);
Completion iterator_close(VM&, IteratorRecord const&, Completion);
Completion async_iterator_close(VM&, IteratorRecord const&, Completion);
NonnullGCPtr<Object> create_iterator_result_object(VM&, Value, bool done);
ThrowCompletionOr<MarkedVector<Value>> iterator_to_list(VM&, IteratorRecord const&);

using IteratorValueCallback = Function<Optional<Completion>(Value)>;
Completion get_iterator_values(VM&, Value iterable, IteratorValueCallback callback);

}
