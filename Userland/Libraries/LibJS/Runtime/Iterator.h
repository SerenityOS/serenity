/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

// 7.4.1 Iterator Records, https://tc39.es/ecma262/#sec-iterator-records
struct IteratorRecord {
    GCPtr<Object> iterator; // [[Iterator]]
    Value next_method;      // [[NextMethod]]
    bool done { false };    // [[Done]]
};

class Iterator : public Object {
    JS_OBJECT(Iterator, Object);

public:
    static ThrowCompletionOr<NonnullGCPtr<Iterator>> create(Realm&, Object& prototype, IteratorRecord iterated);

    IteratorRecord const& iterated() const { return m_iterated; }

private:
    Iterator(Object& prototype, IteratorRecord iterated);
    explicit Iterator(Object& prototype);

    IteratorRecord m_iterated; // [[Iterated]]
};

enum class StringHandling {
    IterateStrings,
    RejectStrings,
};

ThrowCompletionOr<IteratorRecord> get_iterator_direct(VM&, Object&);
ThrowCompletionOr<IteratorRecord> get_iterator_flattenable(VM&, Value, StringHandling);

}
