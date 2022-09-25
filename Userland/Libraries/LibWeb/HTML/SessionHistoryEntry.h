/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <AK/WeakPtr.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/PolicyContainers.h>

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
struct SessionHistoryEntry {
    // URL, a URL
    AK::URL url;

    // document, a Document or null
    WeakPtr<DOM::Document> document;

    // serialized state, which is serialized state or null, initially null
    Optional<String> serialized_state;

    // policy container, a policy container or null
    Optional<PolicyContainer> policy_container;

    // scroll restoration mode, a scroll restoration mode, initially "auto"
    ScrollRestorationMode scroll_restoration_mode { ScrollRestorationMode::Auto };

    // FIXME: scroll position data, which is scroll position data for the document's restorable scrollable regions

    // browsing context name, a browsing context name or null, initially null
    Optional<String> browsing_context_name;

    // FIXME: persisted user state, which is implementation-defined, initially null
    // NOTE: This is where we could remember the state of form controls, for example.

    WeakPtr<BrowsingContext> original_source_browsing_context;
};

}
