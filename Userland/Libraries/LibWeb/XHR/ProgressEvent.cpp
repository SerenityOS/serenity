/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/XHR/ProgressEvent.h>

namespace Web::XHR {

WebIDL::ExceptionOr<JS::NonnullGCPtr<ProgressEvent>> ProgressEvent::create(JS::Realm& realm, FlyString const& event_name, ProgressEventInit const& event_init)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<ProgressEvent>(realm, realm, event_name, event_init));
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<ProgressEvent>> ProgressEvent::construct_impl(JS::Realm& realm, FlyString const& event_name, ProgressEventInit const& event_init)
{
    return create(realm, event_name, event_init);
}

ProgressEvent::ProgressEvent(JS::Realm& realm, FlyString const& event_name, ProgressEventInit const& event_init)
    : Event(realm, event_name, event_init)
    , m_length_computable(event_init.length_computable)
    , m_loaded(event_init.loaded)
    , m_total(event_init.total)
{
}

ProgressEvent::~ProgressEvent() = default;

void ProgressEvent::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::ProgressEventPrototype>(realm, "ProgressEvent"));
}

}
