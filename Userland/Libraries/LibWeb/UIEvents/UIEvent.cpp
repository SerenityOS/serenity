/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/UIEvents/UIEvent.h>

namespace Web::UIEvents {

UIEvent* UIEvent::create(JS::Realm& realm, FlyString const& event_name)
{
    return realm.heap().allocate<UIEvent>(realm, realm, event_name);
}

UIEvent* UIEvent::construct_impl(JS::Realm& realm, FlyString const& event_name, UIEventInit const& event_init)
{
    return realm.heap().allocate<UIEvent>(realm, realm, event_name, event_init);
}

UIEvent::UIEvent(JS::Realm& realm, FlyString const& event_name)
    : Event(realm, event_name)
{
    set_prototype(&Bindings::cached_web_prototype(realm, "UIEvent"));
}

UIEvent::UIEvent(JS::Realm& realm, FlyString const& event_name, UIEventInit const& event_init)
    : Event(realm, event_name, event_init)
    , m_view(event_init.view)
    , m_detail(event_init.detail)
{
    set_prototype(&Bindings::cached_web_prototype(realm, "UIEvent"));
}

UIEvent::~UIEvent() = default;

void UIEvent::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_view.ptr());
}

}
