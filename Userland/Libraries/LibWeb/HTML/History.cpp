/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HistoryPrototype.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/History.h>

namespace Web::HTML {

JS::NonnullGCPtr<History> History::create(HTML::Window& window, DOM::Document& document)
{
    return *window.heap().allocate<History>(window.realm(), window, document);
}

History::History(HTML::Window& window, DOM::Document& document)
    : PlatformObject(window.realm())
    , m_associated_document(document)
{
    set_prototype(&window.ensure_web_prototype<Bindings::HistoryPrototype>("History"));
}

History::~History() = default;

void History::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_associated_document.ptr());
}

// https://html.spec.whatwg.org/multipage/history.html#dom-history-pushstate
DOM::ExceptionOr<void> History::push_state(JS::Value data, String const&, String const& url)
{
    // NOTE: The second parameter of this function is intentionally unused.
    return shared_history_push_replace_state(data, url, IsPush::Yes);
}

// https://html.spec.whatwg.org/multipage/history.html#dom-history-replacestate
DOM::ExceptionOr<void> History::replace_state(JS::Value data, String const&, String const& url)
{
    // NOTE: The second parameter of this function is intentionally unused.
    return shared_history_push_replace_state(data, url, IsPush::No);
}

// https://html.spec.whatwg.org/multipage/history.html#shared-history-push/replace-state-steps
DOM::ExceptionOr<void> History::shared_history_push_replace_state(JS::Value, String const&, IsPush)
{
    // 1. Let document be history's associated Document. (NOTE: Not necessary)

    // 2. If document is not fully active, then throw a "SecurityError" DOMException.
    if (!m_associated_document->is_fully_active())
        return DOM::SecurityError::create("Cannot perform pushState or replaceState on a document that isn't fully active.");

    // 3. Optionally, return. (For example, the user agent might disallow calls to these methods that are invoked on a timer,
    //    or from event listeners that are not triggered in response to a clear user action, or that are invoked in rapid succession.)
    dbgln("FIXME: Implement shared_history_push_replace_state.");
    return {};

    // FIXME: Add the rest of the spec steps once they're added.
}

}
