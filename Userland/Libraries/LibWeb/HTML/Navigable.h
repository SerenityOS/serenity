/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Heap/Cell.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/HistoryHandlingBehavior.h>
#include <LibWeb/HTML/POSTResource.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/document-sequences.html#navigable
class Navigable : public JS::Cell {
    JS_CELL(Navigable, JS::Cell);

public:
    virtual ~Navigable() override;

    String const& id() const { return m_id; };
    JS::GCPtr<Navigable> parent() const { return m_parent; };

    bool is_closing() const { return m_closing; };
    void set_closing(bool value) { m_closing = value; };

    bool is_delaying_load_events() const { return m_delaying_load_events; };
    void set_delaying_load_events(bool value) { m_delaying_load_events = value; };

    JS::GCPtr<SessionHistoryEntry> active_session_history_entry() const { return m_active_session_history_entry; };
    JS::GCPtr<SessionHistoryEntry> current_session_history_entry() const { return m_current_session_history_entry; };

    JS::GCPtr<DOM::Document> active_document();
    JS::GCPtr<BrowsingContext> active_browsing_context();
    JS::GCPtr<WindowProxy> active_window_proxy();
    JS::GCPtr<Window> active_window();

    String target_name() const;

    JS::GCPtr<NavigableContainer> container() const;
    void set_container(JS::GCPtr<NavigableContainer>);

    JS::GCPtr<TraversableNavigable> traversable_navigable();
    JS::GCPtr<TraversableNavigable> top_level_traversable();

    static JS::GCPtr<Navigable> navigable_with_active_document(JS::NonnullGCPtr<DOM::Document>);

    enum class Traversal {
        Tag
    };

    Variant<Empty, Traversal, String> ongoing_navigation() const { return m_ongoing_navigation; }

    WebIDL::ExceptionOr<void> navigate(
        AK::URL const&,
        JS::NonnullGCPtr<DOM::Document> source_document,
        Variant<Empty, String, POSTResource> document_resource = Empty {},
        JS::GCPtr<Fetch::Infrastructure::Response> = nullptr,
        bool exceptions_enabled = false,
        HistoryHandlingBehavior = HistoryHandlingBehavior::Push,
        String csp_navigation_type = String::from_utf8_short_string("other"sv),
        ReferrerPolicy::ReferrerPolicy = ReferrerPolicy::ReferrerPolicy::EmptyString);

    WebIDL::ExceptionOr<void> navigate_to_a_fragment(AK::URL const&, HistoryHandlingBehavior, String navigation_id);

    WebIDL::ExceptionOr<void> navigate_to_a_javascript_url(AK::URL const&, HistoryHandlingBehavior, Origin const& initiator_origin, String csp_navigation_type);

protected:
    Navigable();

    virtual void visit_edges(Cell::Visitor&) override;

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#ongoing-navigation
    Variant<Empty, Traversal, String> m_ongoing_navigation;

private:
    // https://html.spec.whatwg.org/multipage/document-sequences.html#nav-id
    String m_id;

    // https://html.spec.whatwg.org/multipage/document-sequences.html#nav-parent
    JS::GCPtr<Navigable> m_parent;

    // https://html.spec.whatwg.org/multipage/document-sequences.html#nav-current-history-entry
    JS::GCPtr<SessionHistoryEntry> m_current_session_history_entry;

    // https://html.spec.whatwg.org/multipage/document-sequences.html#nav-active-history-entry
    JS::GCPtr<SessionHistoryEntry> m_active_session_history_entry;

    // https://html.spec.whatwg.org/multipage/document-sequences.html#is-closing
    bool m_closing { false };

    // https://html.spec.whatwg.org/multipage/document-sequences.html#delaying-load-events-mode
    bool m_delaying_load_events { false };

    // Implied link between navigable and its container.
    JS::GCPtr<NavigableContainer> m_container;
};

}
