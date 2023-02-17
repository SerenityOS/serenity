/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/ErrorEvent.h>

namespace Web::HTML {

WebIDL::ExceptionOr<JS::NonnullGCPtr<ErrorEvent>> ErrorEvent::create(JS::Realm& realm, DeprecatedFlyString const& event_name, ErrorEventInit const& event_init)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<ErrorEvent>(realm, realm, event_name, event_init));
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<ErrorEvent>> ErrorEvent::construct_impl(JS::Realm& realm, DeprecatedFlyString const& event_name, ErrorEventInit const& event_init)
{
    return create(realm, event_name, event_init);
}

ErrorEvent::ErrorEvent(JS::Realm& realm, DeprecatedFlyString const& event_name, ErrorEventInit const& event_init)
    : DOM::Event(realm, event_name)
    , m_message(event_init.message)
    , m_filename(event_init.filename)
    , m_lineno(event_init.lineno)
    , m_colno(event_init.colno)
    , m_error(event_init.error)
{
}

ErrorEvent::~ErrorEvent() = default;

JS::ThrowCompletionOr<void> ErrorEvent::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::ErrorEventPrototype>(realm, "ErrorEvent"));

    return {};
}

void ErrorEvent::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_error);
}

}
