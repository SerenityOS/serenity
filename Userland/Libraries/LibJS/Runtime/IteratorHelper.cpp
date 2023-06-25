/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorHelper.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/Realm.h>

namespace JS {

ThrowCompletionOr<NonnullGCPtr<IteratorHelper>> IteratorHelper::create(Realm& realm, IteratorRecord underlying_iterator, Closure closure)
{
    return TRY(realm.heap().allocate<IteratorHelper>(realm, realm.intrinsics().iterator_helper_prototype(), move(underlying_iterator), move(closure)));
}

IteratorHelper::IteratorHelper(Object& prototype, IteratorRecord underlying_iterator, Closure closure)
    : Object(ConstructWithPrototypeTag::Tag, prototype)
    , m_underlying_iterator(move(underlying_iterator))
    , m_closure(move(closure))
{
}

void IteratorHelper::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_underlying_iterator.iterator);
}

Value IteratorHelper::result(Value value)
{
    if (value.is_undefined())
        m_done = true;
    return value;
}

ThrowCompletionOr<Value> IteratorHelper::close_result(Completion completion)
{
    m_done = true;
    return *TRY(iterator_close(vm(), underlying_iterator(), move(completion)));
}

}
