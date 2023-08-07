/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/AbstractRange.h>
#include <LibWeb/DOM/Document.h>

namespace Web::DOM {

AbstractRange::AbstractRange(Node& start_container, u32 start_offset, Node& end_container, u32 end_offset)
    : Bindings::PlatformObject(start_container.realm())
    , m_start_container(start_container)
    , m_start_offset(start_offset)
    , m_end_container(end_container)
    , m_end_offset(end_offset)
{
}

AbstractRange::~AbstractRange() = default;

void AbstractRange::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::AbstractRangePrototype>(realm, "AbstractRange"));
}

void AbstractRange::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_start_container.ptr());
    visitor.visit(m_end_container.ptr());
}

}
