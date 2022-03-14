/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLMediaElementWrapper.h>
#include <LibWeb/HTML/HTMLMediaElement.h>

namespace Web::HTML {

HTMLMediaElement::HTMLMediaElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLMediaElement::~HTMLMediaElement() = default;

// https://html.spec.whatwg.org/multipage/media.html#dom-navigator-canplaytype
Bindings::CanPlayTypeResult HTMLMediaElement::can_play_type(String const& type) const
{
    // The canPlayType(type) method must:
    // - return the empty string if type is a type that the user agent knows it cannot render or is the type "application/octet-stream"
    // - return "probably" if the user agent is confident that the type represents a media resource that it can render if used in with this audio or video element
    // - return "maybe" otherwise. Implementers are encouraged to return "maybe" unless the type can be confidently established as being supported or not
    // Generally, a user agent should never return "probably" for a type that allows the codecs parameter if that parameter is not present.
    if (type == "application/octet-stream"sv)
        return Bindings::CanPlayTypeResult::Empty;
    // FIXME: Eventually we should return `Maybe` here, but for now `Empty` is our best bet :^)
    //        Being honest here leads to some apps and frameworks skipping things like audio loading,
    //        which for the time being would create more issues than it solves - e.g. endless waiting
    //        for audio that will never load.
    return Bindings::CanPlayTypeResult::Empty;
}

}
