/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContextGroup.h>
#include <LibWeb/HTML/DocumentState.h>
#include <LibWeb/HTML/SessionHistoryEntry.h>
#include <LibWeb/HTML/TraversableNavigable.h>

namespace Web::HTML {

TraversableNavigable::TraversableNavigable() = default;

TraversableNavigable::~TraversableNavigable() = default;

void TraversableNavigable::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto& entry : m_session_history_entries)
        visitor.visit(entry);
}

static OrderedHashTable<TraversableNavigable*>& user_agent_top_level_traversable_set()
{
    static OrderedHashTable<TraversableNavigable*> set;
    return set;
}

struct BrowsingContextAndDocument {
    JS::NonnullGCPtr<HTML::BrowsingContext> browsing_context;
    JS::NonnullGCPtr<DOM::Document> document;
};

// https://html.spec.whatwg.org/multipage/document-sequences.html#creating-a-new-top-level-browsing-context
static WebIDL::ExceptionOr<BrowsingContextAndDocument> create_a_new_top_level_browsing_context_and_document(Page& page)
{
    // 1. Let group and document be the result of creating a new browsing context group and document.
    auto [group, document] = TRY(BrowsingContextGroup::create_a_new_browsing_context_group_and_document(page));

    // 2. Return group's browsing context set[0] and document.
    return BrowsingContextAndDocument { **group->browsing_context_set().begin(), document };
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#creating-a-new-top-level-traversable
WebIDL::ExceptionOr<JS::NonnullGCPtr<TraversableNavigable>> TraversableNavigable::create_a_new_top_level_traversable(Page& page, JS::GCPtr<HTML::BrowsingContext> opener, String target_name)
{
    auto& vm = Bindings::main_thread_vm();

    // 1. Let document be null.
    JS::GCPtr<DOM::Document> document = nullptr;

    // 2. If opener is null, then set document to the second return value of creating a new top-level browsing context and document.
    if (!opener) {
        document = TRY(create_a_new_top_level_browsing_context_and_document(page)).document;
    }

    // 3. Otherwise, set document to the second return value of creating a new auxiliary browsing context and document given opener.
    else {
        document = TRY(BrowsingContext::create_a_new_auxiliary_browsing_context_and_document(page, *opener)).document;
    }

    // 4. Let documentState be a new document state, with
    auto document_state = vm.heap().allocate_without_realm<DocumentState>();

    // document: document
    document_state->set_document(document);

    // navigable target name: targetName
    document_state->set_navigable_target_name(target_name);

    // 5. Let traversable be a new traversable navigable.
    auto traversable = vm.heap().allocate_without_realm<TraversableNavigable>();

    // 6. Initialize the navigable traversable given documentState.
    TRY_OR_THROW_OOM(vm, traversable->initialize_navigable(document_state, nullptr));

    // 7. Let initialHistoryEntry be traversable's active session history entry.
    auto initial_history_entry = traversable->active_session_history_entry();
    VERIFY(initial_history_entry);

    // 8. Set initialHistoryEntry's step to 0.
    initial_history_entry->step = 0;

    // 9. Append initialHistoryEntry to traversable's session history entries.
    traversable->m_session_history_entries.append(*initial_history_entry);

    // FIXME: 10. If opener is non-null, then legacy-clone a traversable storage shed given opener's top-level traversable and traversable. [STORAGE]

    // 11. Append traversable to the user agent's top-level traversable set.
    user_agent_top_level_traversable_set().set(traversable);

    // 12. Return traversable.
    return traversable;
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#create-a-fresh-top-level-traversable
WebIDL::ExceptionOr<JS::NonnullGCPtr<TraversableNavigable>> TraversableNavigable::create_a_fresh_top_level_traversable(Page& page, AK::URL const& initial_navigation_url, Variant<Empty, String, POSTResource> initial_navigation_post_resource)
{
    // 1. Let traversable be the result of creating a new top-level traversable given null and the empty string.
    auto traversable = TRY(create_a_new_top_level_traversable(page, nullptr, {}));

    // 2. Navigate traversable to initialNavigationURL using traversable's active document, with documentResource set to initialNavigationPostResource.
    TRY(traversable->navigate(initial_navigation_url, *traversable->active_document(), initial_navigation_post_resource));

    // 3. Return traversable.
    return traversable;
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#top-level-traversable
bool TraversableNavigable::is_top_level_traversable() const
{
    // A top-level traversable is a traversable navigable with a null parent.
    return parent() == nullptr;
}

}
