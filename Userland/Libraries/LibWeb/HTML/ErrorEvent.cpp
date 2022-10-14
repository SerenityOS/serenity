/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/ErrorEvent.h>

namespace Web::HTML {

ErrorEvent* ErrorEvent::create(JS::Realm& realm, FlyString const& event_name, ErrorEventInit const& event_init)
{
    return realm.heap().allocate<ErrorEvent>(realm, realm, event_name, event_init);
}

ErrorEvent* ErrorEvent::construct_impl(JS::Realm& realm, FlyString const& event_name, ErrorEventInit const& event_init)
{
    return create(realm, event_name, event_init);
}

ErrorEvent::ErrorEvent(JS::Realm& realm, FlyString const& event_name, ErrorEventInit const& event_init)
    : DOM::Event(realm, event_name)
    , m_message(event_init.message)
    , m_filename(event_init.filename)
    , m_lineno(event_init.lineno)
    , m_colno(event_init.colno)
    , m_error(event_init.error)
{
    set_prototype(&Bindings::cached_web_prototype(realm, "ErrorEvent"));
}

ErrorEvent::~ErrorEvent() = default;

void ErrorEvent::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_error);
}

}
