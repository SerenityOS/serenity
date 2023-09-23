/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibWeb/HTML/Navigable.h>
#include <LibWeb/HTML/SessionHistoryTraversalQueue.h>
#include <LibWeb/HTML/VisibilityState.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/document-sequences.html#traversable-navigable
class TraversableNavigable final : public Navigable {
    JS_CELL(TraversableNavigable, Navigable);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<TraversableNavigable>> create_a_new_top_level_traversable(Page&, JS::GCPtr<BrowsingContext> opener, String target_name);
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<TraversableNavigable>> create_a_fresh_top_level_traversable(Page&, AK::URL const& initial_navigation_url, Variant<Empty, String, POSTResource> = Empty {});

    virtual ~TraversableNavigable() override;

    virtual bool is_top_level_traversable() const override;

    int current_session_history_step() const { return m_current_session_history_step; }
    Vector<JS::NonnullGCPtr<SessionHistoryEntry>>& session_history_entries() { return m_session_history_entries; }
    Vector<JS::NonnullGCPtr<SessionHistoryEntry>> const& session_history_entries() const { return m_session_history_entries; }
    bool running_nested_apply_history_step() const { return m_running_nested_apply_history_step; }

    VisibilityState system_visibility_state() const { return m_system_visibility_state; }
    void set_system_visibility_state(VisibilityState);

    struct HistoryObjectLengthAndIndex {
        u64 script_history_length;
        u64 script_history_index;
    };
    HistoryObjectLengthAndIndex get_the_history_object_length_and_index(int) const;

    void apply_the_reload_history_step();
    void apply_the_push_or_replace_history_step(int step);
    void update_for_navigable_creation_or_destruction();

    int get_the_used_step(int step) const;
    Vector<JS::Handle<Navigable>> get_all_navigables_whose_current_session_history_entry_will_change_or_reload(int) const;
    Vector<int> get_all_used_history_steps() const;
    void clear_the_forward_session_history();
    void traverse_the_history_by_delta(int delta);

    void close_top_level_traversable();
    void destroy_top_level_traversable();

    void append_session_history_traversal_steps(JS::SafeFunction<void()> steps)
    {
        m_session_history_traversal_queue.append(move(steps));
    }

    void process_session_history_traversal_queue()
    {
        m_session_history_traversal_queue.process();
    }

    Page* page() { return m_page; }
    Page const* page() const { return m_page; }

private:
    TraversableNavigable(Page&);

    virtual void visit_edges(Cell::Visitor&) override;

    void apply_the_history_step(int step, Optional<SourceSnapshotParams> = {});

    // https://html.spec.whatwg.org/multipage/document-sequences.html#tn-current-session-history-step
    int m_current_session_history_step { 0 };

    // https://html.spec.whatwg.org/multipage/document-sequences.html#tn-session-history-entries
    Vector<JS::NonnullGCPtr<SessionHistoryEntry>> m_session_history_entries;

    // FIXME: https://html.spec.whatwg.org/multipage/document-sequences.html#tn-session-history-traversal-queue

    // https://html.spec.whatwg.org/multipage/document-sequences.html#tn-running-nested-apply-history-step
    bool m_running_nested_apply_history_step { false };

    // https://html.spec.whatwg.org/multipage/document-sequences.html#system-visibility-state
    VisibilityState m_system_visibility_state { VisibilityState::Visible };

    SessionHistoryTraversalQueue m_session_history_traversal_queue;

    WeakPtr<Page> m_page;
};

struct BrowsingContextAndDocument {
    JS::NonnullGCPtr<HTML::BrowsingContext> browsing_context;
    JS::NonnullGCPtr<DOM::Document> document;
};

WebIDL::ExceptionOr<BrowsingContextAndDocument> create_a_new_top_level_browsing_context_and_document(Page& page);
void finalize_a_same_document_navigation(JS::NonnullGCPtr<TraversableNavigable> traversable, JS::NonnullGCPtr<Navigable> target_navigable, JS::NonnullGCPtr<SessionHistoryEntry> target_entry, JS::GCPtr<SessionHistoryEntry> entry_to_replace);

}
