/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/XHR/ProgressEvent.h>

namespace Web::XHR {

ProgressEvent* ProgressEvent::create(JS::Realm& realm, DeprecatedFlyString const& event_name, ProgressEventInit const& event_init)
{
    return realm.heap().allocate<ProgressEvent>(realm, realm, event_name, event_init);
}

ProgressEvent* ProgressEvent::construct_impl(JS::Realm& realm, DeprecatedFlyString const& event_name, ProgressEventInit const& event_init)
{
    return create(realm, event_name, event_init);
}

ProgressEvent::ProgressEvent(JS::Realm& realm, DeprecatedFlyString const& event_name, ProgressEventInit const& event_init)
    : Event(realm, event_name, event_init)
    , m_length_computable(event_init.length_computable)
    , m_loaded(event_init.loaded)
    , m_total(event_init.total)
{
    set_prototype(&Bindings::cached_web_prototype(realm, "ProgressEvent"));
}

ProgressEvent::~ProgressEvent() = default;

}
