/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Iterator.h>

namespace JS {

ThrowCompletionOr<NonnullGCPtr<Iterator>> Iterator::create(Realm& realm, Object& prototype, IteratorRecord iterated)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<Iterator>(realm, prototype, move(iterated)));
}

Iterator::Iterator(Object& prototype, IteratorRecord iterated)
    : Object(ConstructWithPrototypeTag::Tag, prototype)
    , m_iterated(move(iterated))
{
}

Iterator::Iterator(Object& prototype)
    : Iterator(prototype, {})
{
}

}
