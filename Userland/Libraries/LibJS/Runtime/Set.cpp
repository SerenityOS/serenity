/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Set.h>

namespace JS {

Set* Set::create(Realm& realm)
{
    return realm.heap().allocate<Set>(realm, *realm.intrinsics().set_prototype());
}

Set::Set(Object& prototype)
    : Object(prototype)
    , m_values(*prototype.shape().realm().intrinsics().map_prototype())
{
}

void Set::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    static_cast<Object&>(m_values).visit_edges(visitor);
}

}
