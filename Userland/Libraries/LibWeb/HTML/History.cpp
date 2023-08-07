/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/History.h>

namespace Web::HTML {

WebIDL::ExceptionOr<JS::NonnullGCPtr<History>> History::create(JS::Realm& realm, DOM::Document& document)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<History>(realm, realm, document));
}

History::History(JS::Realm& realm, DOM::Document& document)
    : PlatformObject(realm)
    , m_associated_document(document)
{
}

History::~History() = default;

void History::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HistoryPrototype>(realm, "History"));
}

void History::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_associated_document.ptr());
}

// https://html.spec.whatwg.org/multipage/history.html#dom-history-pushstate
WebIDL::ExceptionOr<void> History::push_state(JS::Value data, DeprecatedString const&, DeprecatedString const& url)
{
    // NOTE: The second parameter of this function is intentionally unused.
    return shared_history_push_replace_state(data, url, IsPush::Yes);
}

// https://html.spec.whatwg.org/multipage/history.html#dom-history-replacestate
WebIDL::ExceptionOr<void> History::replace_state(JS::Value data, DeprecatedString const&, DeprecatedString const& url)
{
    // NOTE: The second parameter of this function is intentionally unused.
    return shared_history_push_replace_state(data, url, IsPush::No);
}

// https://html.spec.whatwg.org/multipage/history.html#dom-history-length
WebIDL::ExceptionOr<u64> History::length() const
{
    // 1. If this's associated Document is not fully active, then throw a "SecurityError" DOMException.
    if (!m_associated_document->is_fully_active())
        return WebIDL::SecurityError::create(realm(), "Cannot perform length on a document that isn't fully active."sv);

    // 2. Return the number of entries in the top-level browsing context's joint session history.
    auto const* browsing_context = m_associated_document->browsing_context();

    // FIXME: We don't have the concept of "joint session history", this is an ad-hoc implementation.
    //        See: https://html.spec.whatwg.org/multipage/history.html#joint-session-history
    return browsing_context->session_history().size();
}

// https://html.spec.whatwg.org/multipage/history.html#dom-history-go
WebIDL::ExceptionOr<void> History::go(long delta = 0)
{
    // 1. Let document be this's associated Document.

    // 2. If document is not fully active, then throw a "SecurityError" DOMException.
    if (!m_associated_document->is_fully_active())
        return WebIDL::SecurityError::create(realm(), "Cannot perform go on a document that isn't fully active."sv);

    // 3. If delta is 0, then act as if the location.reload() method was called, and return.
    auto* browsing_context = m_associated_document->browsing_context();
    auto current_entry_index = browsing_context->session_history_index();
    auto next_entry_index = current_entry_index + delta;
    auto const& sessions = browsing_context->session_history();
    if (next_entry_index < sessions.size()) {
        auto const& next_entry = sessions.at(next_entry_index);
        // FIXME: 4. Traverse the history by a delta with delta and document's browsing context.
        browsing_context->loader().load(next_entry->url, FrameLoader::Type::Reload);
    }

    return {};
}

// https://html.spec.whatwg.org/multipage/history.html#dom-history-back
WebIDL::ExceptionOr<void> History::back()
{
    // 1. Let document be this's associated Document.
    // 2. If document is not fully active, then throw a "SecurityError" DOMException.
    // NOTE: We already did this check in `go` method, so skip the fully active check here.

    // 3. Traverse the history by a delta with −1 and document's browsing context.
    return go(-1);
}

// https://html.spec.whatwg.org/multipage/history.html#dom-history-forward
WebIDL::ExceptionOr<void> History::forward()
{
    // 1. Let document be this's associated Document.
    // 2. If document is not fully active, then throw a "SecurityError" DOMException.
    // NOTE: We already did this check in `go` method, so skip the fully active check here.

    // 3. Traverse the history by a delta with +1 and document's browsing context.
    return go(1);
}

// https://html.spec.whatwg.org/multipage/history.html#shared-history-push/replace-state-steps
WebIDL::ExceptionOr<void> History::shared_history_push_replace_state(JS::Value, DeprecatedString const&, IsPush)
{
    // 1. Let document be history's associated Document. (NOTE: Not necessary)

    // 2. If document is not fully active, then throw a "SecurityError" DOMException.
    if (!m_associated_document->is_fully_active())
        return WebIDL::SecurityError::create(realm(), "Cannot perform pushState or replaceState on a document that isn't fully active."sv);

    // 3. Optionally, return. (For example, the user agent might disallow calls to these methods that are invoked on a timer,
    //    or from event listeners that are not triggered in response to a clear user action, or that are invoked in rapid succession.)
    dbgln("FIXME: Implement shared_history_push_replace_state.");
    return {};

    // FIXME: Add the rest of the spec steps once they're added.
}

}
