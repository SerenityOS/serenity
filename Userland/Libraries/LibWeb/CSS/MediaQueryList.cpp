/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/MediaQueryListWrapper.h>
#include <LibWeb/CSS/MediaQueryList.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/EventDispatcher.h>

namespace Web::CSS {

MediaQueryList::MediaQueryList(DOM::Document& document, String media)
    : DOM::EventTarget(static_cast<Bindings::ScriptExecutionContext&>(document))
    , m_document(document)
    , m_media(move(media))
{
}

MediaQueryList::~MediaQueryList()
{
}

// https://drafts.csswg.org/cssom-view/#dom-mediaquerylist-media
String MediaQueryList::media() const
{
    // TODO: Replace this with a "media query list" and serialize on demand
    return m_media;
}

// https://drafts.csswg.org/cssom-view/#dom-mediaquerylist-matches
bool MediaQueryList::matches() const
{
    // TODO: Implement me :^)
    return false;
}

bool MediaQueryList::dispatch_event(NonnullRefPtr<DOM::Event> event)
{
    return DOM::EventDispatcher::dispatch(*this, event);
}

JS::Object* MediaQueryList::create_wrapper(JS::GlobalObject& global_object)
{
    return wrap(global_object, *this);
}

}
