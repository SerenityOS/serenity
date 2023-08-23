/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <AK/WeakPtr.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/PolicyContainers.h>
#include <LibWeb/HTML/StructuredSerialize.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/history.html#scroll-restoration-mode
enum class ScrollRestorationMode {
    // https://html.spec.whatwg.org/multipage/history.html#dom-scrollrestoration-auto
    // The user agent is responsible for restoring the scroll position upon navigation.
    Auto,

    // https://html.spec.whatwg.org/multipage/history.html#dom-scrollrestoration-manual
    // The page is responsible for restoring the scroll position and the user agent does not attempt to do so automatically.
    Manual,
};

// https://html.spec.whatwg.org/multipage/history.html#session-history-entry
struct SessionHistoryEntry final : public JS::Cell {
    JS_CELL(SessionHistoryEntry, JS::Cell);

    SessionHistoryEntry();

    void visit_edges(Cell::Visitor&) override;

    enum class Pending {
        Tag,
    };

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#she-step
    // step, a non-negative integer or "pending", initially "pending".
    Variant<int, Pending> step { Pending::Tag };

    // URL, a URL
    AK::URL url;

    // document, a Document or null
    // FIXME: this property is not present in the spec anymore and should be gone after introducing navigables
    JS::GCPtr<DOM::Document> document;

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#she-document-state
    JS::GCPtr<HTML::DocumentState> document_state;

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#she-classic-history-api-state
    // classic history API state, which is serialized state, initially StructuredSerializeForStorage(null).
    SerializationRecord classic_history_api_state;

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#she-navigation-api-state
    // navigation API state, which is a serialized state, initially StructuredSerializeForStorage(undefined).
    SerializationRecord navigation_api_state;

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#she-navigation-api-key
    // navigation API key, which is a string, initially set to the result of generating a random UUID.
    String navigation_api_key;

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#she-navigation-api-id
    // navigation API ID, which is a string, initially set to the result of generating a random UUID.
    String navigation_api_id;

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#she-scroll-restoration-mode
    // scroll restoration mode, a scroll restoration mode, initially "auto"
    ScrollRestorationMode scroll_restoration_mode { ScrollRestorationMode::Auto };

    // policy container, a policy container or null
    Optional<PolicyContainer> policy_container;

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#she-scroll-position
    // FIXME: scroll position data, which is scroll position data for the document's restorable scrollable regions

    // browsing context name, a browsing context name or null, initially null
    Optional<DeprecatedString> browsing_context_name;

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#she-other
    // FIXME: persisted user state, which is implementation-defined, initially null
    // NOTE: This is where we could remember the state of form controls, for example.

    JS::GCPtr<BrowsingContext> original_source_browsing_context;
};

}
