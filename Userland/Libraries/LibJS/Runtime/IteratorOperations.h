/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
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

ThrowCompletionOr<Iterator> get_iterator(VM&, Value, IteratorHint = IteratorHint::Sync, Optional<Value> method = {});
ThrowCompletionOr<Object*> iterator_next(VM&, Iterator const&, Optional<Value> = {});
ThrowCompletionOr<Object*> iterator_step(VM&, Iterator const&);
ThrowCompletionOr<bool> iterator_complete(VM&, Object& iterator_result);
ThrowCompletionOr<Value> iterator_value(VM&, Object& iterator_result);
Completion iterator_close(VM&, Iterator const&, Completion);
Completion async_iterator_close(VM&, Iterator const&, Completion);
Object* create_iterator_result_object(VM&, Value, bool done);
ThrowCompletionOr<MarkedVector<Value>> iterable_to_list(VM&, Value iterable, Optional<Value> method = {});

using IteratorValueCallback = Function<Optional<Completion>(Value)>;
Completion get_iterator_values(VM&, Value iterable, IteratorValueCallback callback, Optional<Value> method = {});

}
