/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLAudioElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLAudioElement::HTMLAudioElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLMediaElement(document, move(qualified_name))
{
}

HTMLAudioElement::~HTMLAudioElement() = default;

JS::ThrowCompletionOr<void> HTMLAudioElement::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLAudioElementPrototype>(realm, "HTMLAudioElement"));

    return {};
}

}
