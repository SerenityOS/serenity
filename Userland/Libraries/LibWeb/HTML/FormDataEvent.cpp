/*
 * Copyright (c) 2023, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/FormDataEventPrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/FormDataEvent.h>

namespace Web::HTML {

WebIDL::ExceptionOr<JS::NonnullGCPtr<FormDataEvent>> FormDataEvent::construct_impl(JS::Realm& realm, DeprecatedString const& event_name, FormDataEventInit const& event_init)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<FormDataEvent>(realm, realm, event_name, event_init));
}

FormDataEvent::FormDataEvent(JS::Realm& realm, DeprecatedString const& event_name, FormDataEventInit const& event_init)
    : DOM::Event(realm, event_name, event_init)
    , m_form_data(event_init.form_data)
{
}

FormDataEvent::~FormDataEvent() = default;

JS::ThrowCompletionOr<void> FormDataEvent::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::FormDataEventPrototype>(realm, "FormDataEvent"));

    return {};
}

void FormDataEvent::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_form_data);
}

}
