/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLMediaElementPrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLMediaElement.h>

namespace Web::HTML {

HTMLMediaElement::HTMLMediaElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLMediaElement::~HTMLMediaElement() = default;

JS::ThrowCompletionOr<void> HTMLMediaElement::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLMediaElementPrototype>(realm, "HTMLMediaElement"));

    return {};
}

// https://html.spec.whatwg.org/multipage/media.html#queue-a-media-element-task
void HTMLMediaElement::queue_a_media_element_task(JS::SafeFunction<void()> steps)
{
    // To queue a media element task with a media element element and a series of steps steps, queue an element task on the media element's
    // media element event task source given element and steps.
    queue_an_element_task(media_element_event_task_source(), move(steps));
}

// https://html.spec.whatwg.org/multipage/media.html#dom-navigator-canplaytype
Bindings::CanPlayTypeResult HTMLMediaElement::can_play_type(DeprecatedString const& type) const
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

// https://html.spec.whatwg.org/multipage/media.html#dom-media-load
void HTMLMediaElement::load() const
{
    dbgln("(STUBBED) HTMLMediaElement::load()");
}

// https://html.spec.whatwg.org/multipage/media.html#dom-media-pause
void HTMLMediaElement::pause() const
{
    dbgln("(STUBBED) HTMLMediaElement::pause()");
}

}
