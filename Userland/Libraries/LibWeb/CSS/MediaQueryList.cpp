/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/MediaQueryListWrapper.h>
#include <LibWeb/CSS/MediaQueryList.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/DOM/EventListener.h>
#include <LibWeb/HTML/EventHandler.h>

namespace Web::CSS {

MediaQueryList::MediaQueryList(DOM::Document& document, NonnullRefPtrVector<MediaQuery>&& media)
    : DOM::EventTarget(static_cast<Bindings::ScriptExecutionContext&>(document))
    , m_document(document)
    , m_media(move(media))
{
    evaluate();
}

MediaQueryList::~MediaQueryList()
{
}

// https://drafts.csswg.org/cssom-view/#dom-mediaquerylist-media
String MediaQueryList::media() const
{
    return serialize_a_media_query_list(m_media);
}

// https://drafts.csswg.org/cssom-view/#dom-mediaquerylist-matches
bool MediaQueryList::matches() const
{
    for (auto& media : m_media) {
        if (media.matches())
            return true;
    }
    return false;
}

bool MediaQueryList::evaluate()
{
    if (!m_document)
        return false;

    bool now_matches = false;
    for (auto& media : m_media) {
        now_matches = now_matches || media.evaluate(m_document->window());
    }

    return now_matches;
}

JS::Object* MediaQueryList::create_wrapper(JS::GlobalObject& global_object)
{
    return wrap(global_object, *this);
}

// https://www.w3.org/TR/cssom-view/#dom-mediaquerylist-addlistener
void MediaQueryList::add_listener(RefPtr<DOM::EventListener> listener)
{
    // 1. If listener is null, terminate these steps.
    if (!listener)
        return;

    // 2. Append an event listener to the associated list of event listeners with type set to change,
    //    callback set to listener, and capture set to false, unless there already is an event listener
    //    in that list with the same type, callback, and capture.
    //    (NOTE: capture is set to false by default)
    add_event_listener(HTML::EventNames::change, listener);
}

// https://www.w3.org/TR/cssom-view/#dom-mediaquerylist-removelistener
void MediaQueryList::remove_listener(RefPtr<DOM::EventListener> listener)
{
    // 1. Remove an event listener from the associated list of event listeners, whose type is change, callback is listener, and capture is false.
    // NOTE: While the spec doesn't technically use remove_event_listener and instead manipulates the list directly, every major engine uses remove_event_listener.
    //       This means if an event listener removes another event listener that comes after it, the removed event listener will not be invoked.
    remove_event_listener(HTML::EventNames::change, listener);
}

void MediaQueryList::set_onchange(HTML::EventHandler event_handler)
{
    set_event_handler_attribute(HTML::EventNames::change, event_handler);
}

HTML::EventHandler MediaQueryList::onchange()
{
    return event_handler_attribute(HTML::EventNames::change);
}

}
