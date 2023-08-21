/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/History.h>
#include <LibWeb/HTML/StructuredSerialize.h>

namespace Web::HTML {

JS::NonnullGCPtr<History> History::create(JS::Realm& realm, DOM::Document& document)
{
    return realm.heap().allocate<History>(realm, realm, document);
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

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#can-have-its-url-rewritten
static bool can_have_its_url_rewritten(DOM::Document const& document, AK::URL const& target_url)
{
    // 1. Let documentURL be document's URL.
    auto document_url = document.url();

    // 2. If targetURL and documentURL differ in their scheme, username, password, host, or port components,
    //    then return false.
    if (target_url.scheme() != document_url.scheme()
        || target_url.raw_username() != document_url.raw_username()
        || target_url.raw_password() != document_url.raw_password()
        || target_url.host() != document_url.host()
        || target_url.port() != document_url.port())
        return false;

    // 3. If targetURL's scheme is an HTTP(S) scheme, then return true.
    //    (Differences in path, query, and fragment are allowed for http: and https: URLs.)
    if (target_url.scheme() == "http"sv || target_url.scheme() == "https"sv)
        return true;

    // 4. If targetURL's scheme is "file", and targetURL and documentURL differ in their path component,
    //    then return false. (Differences in query and fragment are allowed for file: URLs.)
    // FIXME: Don't create temporary strings to compare paths
    auto target_url_path = target_url.serialize_path();
    auto document_url_path = document_url.serialize_path();
    if (target_url.scheme() == "file"sv
        && target_url_path != document_url_path)
        return false;

    // 5. If targetURL and documentURL differ in their path component or query components, then return false.
    //    (Only differences in fragment are allowed for other types of URLs.)
    if (target_url_path != document_url_path
        || target_url.query() != document_url.query())
        return false;

    // 6. Return true.
    return true;
}

// https://html.spec.whatwg.org/multipage/history.html#shared-history-push/replace-state-steps
WebIDL::ExceptionOr<void> History::shared_history_push_replace_state(JS::Value value, DeprecatedString const& url, IsPush)
{
    // 1. Let document be history's associated Document.
    auto& document = m_associated_document;

    // 2. If document is not fully active, then throw a "SecurityError" DOMException.
    if (!document->is_fully_active())
        return WebIDL::SecurityError::create(realm(), "Cannot perform pushState or replaceState on a document that isn't fully active."sv);

    // 3. Optionally, return. (For example, the user agent might disallow calls to these methods that are invoked on a timer,
    //    or from event listeners that are not triggered in response to a clear user action, or that are invoked in rapid succession.)

    // 4. Let serializedData be StructuredSerializeForStorage(data). Rethrow any exceptions.
    //    FIXME: Actually rethrow exceptions here once we start using the serialized data.
    //           Throwing here on data types we don't yet serialize will regress sites that use push/replaceState.
    [[maybe_unused]] auto serialized_data_or_error = structured_serialize_for_storage(vm(), value);

    // 5. Let newURL be document's URL.
    auto new_url = document->url();

    // 6. If url is not null or the empty string, then:
    if (!url.is_empty() && !url.is_null()) {

        // 1. Parse url, relative to the relevant settings object of history.
        auto parsed_url = relevant_settings_object(*this).parse_url(url);

        // 2. If that fails, then throw a "SecurityError" DOMException.
        if (!parsed_url.is_valid())
            return WebIDL::SecurityError::create(realm(), "Cannot pushState or replaceState to incompatible URL"sv);

        // 3. Set newURL to the resulting URL record.
        new_url = parsed_url;

        // 4. If document cannot have its URL rewritten to newURL, then throw a "SecurityError" DOMException.
        if (!can_have_its_url_rewritten(document, new_url))
            return WebIDL::SecurityError::create(realm(), "Cannot pushState or replaceState to incompatible URL"sv);
    }

    // FIXME: 7. Let navigation be history's relevant global object's navigation API.
    // FIXME: 8. Let continue be the result of firing a push/replace/reload navigate event at navigation
    ///          with navigationType set to historyHandling, isSameDocument set to true, destinationURL set to newURL,
    //           and classicHistoryAPIState set to serializedData.
    // FIXME: 9. If continue is false, then return.
    // FIXME: 10. Run the URL and history update steps given document and newURL, with serializedData set to
    //            serializedData and historyHandling set to historyHandling.

    dbgln("FIXME: Implement shared_history_push_replace_state.");
    return {};
}

}
