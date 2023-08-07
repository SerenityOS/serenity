/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Set.h>

namespace JS {

NonnullGCPtr<Set> Set::create(Realm& realm)
{
    return realm.heap().allocate<Set>(realm, realm.intrinsics().set_prototype()).release_allocated_value_but_fixme_should_propagate_errors();
}

Set::Set(Object& prototype)
    : Object(ConstructWithPrototypeTag::Tag, prototype)
{
}

void Set::initialize(Realm& realm)
{
    m_values = Map::create(realm);
}

NonnullGCPtr<Set> Set::copy() const
{
    auto& vm = this->vm();
    auto& realm = *vm.current_realm();
    // FIXME: This is very inefficient, but there's no better way to do this at the moment, as the underlying Map
    //  implementation of m_values uses a non-copyable RedBlackTree.
    auto result = Set::create(realm);
    for (auto const& entry : *this)
        result->set_add(entry.key);
    return *result;
}

void Set::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_values);
}

}
