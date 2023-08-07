/*
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/WebAudio/BaseAudioContext.h>

namespace Web::WebAudio {

BaseAudioContext::BaseAudioContext(JS::Realm& realm)
    : DOM::EventTarget(realm)
{
}

BaseAudioContext::~BaseAudioContext() = default;

void BaseAudioContext::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::BaseAudioContextPrototype>(realm, "BaseAudioContext"));
}

void BaseAudioContext::set_onstatechange(WebIDL::CallbackType* event_handler)
{
    set_event_handler_attribute(HTML::EventNames::statechange, event_handler);
}

WebIDL::CallbackType* BaseAudioContext::onstatechange()
{
    return event_handler_attribute(HTML::EventNames::statechange);
}

}
