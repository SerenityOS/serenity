/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Requests.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/BrowsingContextGroup.h>
#include <LibWeb/HTML/DocumentState.h>
#include <LibWeb/HTML/HTMLIFrameElement.h>
#include <LibWeb/HTML/Navigable.h>
#include <LibWeb/HTML/NavigableContainer.h>
#include <LibWeb/HTML/NavigationParams.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/Scripting/WindowEnvironmentSettingsObject.h>
#include <LibWeb/HTML/TraversableNavigable.h>
#include <LibWeb/HighResolutionTime/TimeOrigin.h>
#include <LibWeb/Page/Page.h>

namespace Web::HTML {

HashTable<NavigableContainer*>& NavigableContainer::all_instances()
{
    static HashTable<NavigableContainer*> set;
    return set;
}

NavigableContainer::NavigableContainer(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    all_instances().set(this);
}

NavigableContainer::~NavigableContainer()
{
    all_instances().remove(this);
}

void NavigableContainer::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_content_navigable);
}

JS::GCPtr<NavigableContainer> NavigableContainer::navigable_container_with_content_navigable(JS::NonnullGCPtr<Navigable> navigable)
{
    for (auto* navigable_container : all_instances()) {
        if (navigable_container->content_navigable() == navigable)
            return navigable_container;
    }
    return nullptr;
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#create-a-new-child-navigable
WebIDL::ExceptionOr<void> NavigableContainer::create_new_child_navigable()
{
    // 1. Let parentNavigable be element's node navigable.
    auto parent_navigable = navigable();

    // 2. Let group be element's node document's browsing context's top-level browsing context's group.
    VERIFY(document().browsing_context());
    auto group = document().browsing_context()->top_level_browsing_context()->group();
    VERIFY(group);

    // 3. Let browsingContext and document be the result of creating a new browsing context and document given element's node document, element, and group.
    auto* page = document().page();
    VERIFY(page);
    auto [browsing_context, document] = TRY(BrowsingContext::create_a_new_browsing_context_and_document(*page, this->document(), *this, *group));

    // 4. Let targetName be null.
    Optional<String> target_name;

    // 5. If element has a name content attribute, then set targetName to the value of that attribute.
    if (auto value = deprecated_attribute(HTML::AttributeNames::name); !value.is_null())
        target_name = String::from_deprecated_string(value).release_value_but_fixme_should_propagate_errors();

    // 6. Let documentState be a new document state, with
    //    document: document
    //    navigable target name: targetName
    JS::NonnullGCPtr<DocumentState> document_state = *heap().allocate_without_realm<HTML::DocumentState>();
    document_state->set_document(document);
    if (target_name.has_value())
        document_state->set_navigable_target_name(*target_name);

    // 7. Let navigable be a new navigable.
    JS::NonnullGCPtr<Navigable> navigable = *heap().allocate_without_realm<Navigable>();

    // 8. Initialize the navigable navigable given documentState and parentNavigable.
    TRY_OR_THROW_OOM(vm(), navigable->initialize_navigable(document_state, parent_navigable));

    // 9. Set element's content navigable to navigable.
    m_content_navigable = navigable;

    // 10. Let historyEntry be navigable's active session history entry.
    auto history_entry = navigable->active_session_history_entry();

    // 11. Let traversable be parentNavigable's traversable navigable.
    auto traversable = parent_navigable->traversable_navigable();

    // 12. Append the following session history traversal steps to traversable:
    traversable->append_session_history_traversal_steps([traversable, navigable, parent_navigable, history_entry] {
        // 1. Let parentDocState be parentNavigable's active session history entry's document state.

        auto parent_doc_state = parent_navigable->active_session_history_entry()->document_state;

        // 2. Let targetStepSHE be the first session history entry in traversable's session history entries whose document state equals parentDocState.
        // NOTE: We need to look for parent document state in parent navigable instead of traversable as specification says. https://github.com/whatwg/html/issues/9686
        auto target_step_she = *(parent_navigable->get_session_history_entries().find_if([parent_doc_state](auto& entry) {
            return entry->document_state == parent_doc_state;
        }));

        // 3. Set historyEntry's step to targetStepSHE's step.
        history_entry->step = target_step_she->step;

        // 4. Let nestedHistory be a new nested history whose id is navigable's id and entries list is « historyEntry ».
        DocumentState::NestedHistory nested_history {
            .id = navigable->id(),
            .entries { *history_entry },
        };

        // 5. Append nestedHistory to parentDocState's nested histories.
        parent_doc_state->nested_histories().append(move(nested_history));

        // 6. Update for navigable creation/destruction given traversable
        traversable->update_for_navigable_creation_or_destruction();
    });

    return {};
}

// https://html.spec.whatwg.org/multipage/browsers.html#concept-bcc-content-document
const DOM::Document* NavigableContainer::content_document() const
{
    // 1. If container's content navigable is null, then return null.
    if (m_content_navigable == nullptr)
        return nullptr;

    // 2. Let document be container's content navigable's active document.
    auto document = m_content_navigable->active_document();

    // 4. If document's origin and container's node document's origin are not same origin-domain, then return null.
    if (!document->origin().is_same_origin_domain(m_document->origin()))
        return nullptr;

    // 5. Return document.
    return document;
}

DOM::Document const* NavigableContainer::content_document_without_origin_check() const
{
    if (!m_content_navigable)
        return nullptr;

    return m_content_navigable->active_document();
}

// https://html.spec.whatwg.org/multipage/embedded-content-other.html#dom-media-getsvgdocument
const DOM::Document* NavigableContainer::get_svg_document() const
{
    // 1. Let document be this element's content document.
    auto const* document = content_document();

    // 2. If document is non-null and was created by the page load processing model for XML files section because the computed type of the resource in the navigate algorithm was image/svg+xml, then return document.
    if (document && document->content_type() == "image/svg+xml"sv)
        return document;
    // 3. Return null.
    return nullptr;
}

HTML::WindowProxy* NavigableContainer::content_window()
{
    if (!m_content_navigable)
        return nullptr;
    return m_content_navigable->active_window_proxy();
}

// https://html.spec.whatwg.org/multipage/iframe-embed-object.html#shared-attribute-processing-steps-for-iframe-and-frame-elements
Optional<AK::URL> NavigableContainer::shared_attribute_processing_steps_for_iframe_and_frame(bool initial_insertion)
{
    // 1. Let url be the URL record about:blank.
    auto url = AK::URL("about:blank");

    // 2. If element has a src attribute specified, and its value is not the empty string,
    //    then parse the value of that attribute relative to element's node document.
    //    If this is successful, then set url to the resulting URL record.
    auto src_attribute_value = deprecated_attribute(HTML::AttributeNames::src);
    if (!src_attribute_value.is_null() && !src_attribute_value.is_empty()) {
        auto parsed_src = document().parse_url(src_attribute_value);
        if (parsed_src.is_valid())
            url = parsed_src;
    }

    // 3. If the inclusive ancestor navigables of element's node navigable contains a navigable
    //    whose active document's URL equals url with exclude fragments set to true, then return null.
    if (m_content_navigable) {
        for (auto const& navigable : document().inclusive_ancestor_navigables()) {
            VERIFY(navigable->active_document());
            if (navigable->active_document()->url().equals(url, AK::URL::ExcludeFragment::Yes))
                return {};
        }
    }

    // 4. If url matches about:blank and initialInsertion is true, then perform the URL and history update steps given element's content navigable's active document and url.
    if (url_matches_about_blank(url) && initial_insertion) {
        perform_url_and_history_update_steps(*m_content_navigable->active_document(), url);
        // NOTE: Not in the spec but we need to make sure that "apply the history step" for initial navigation to about:blank
        //       is applied before subsequent navigation.
        navigable()->traversable_navigable()->process_session_history_traversal_queue();
    }

    // 5. Return url.
    return url;
}

// https://html.spec.whatwg.org/multipage/iframe-embed-object.html#navigate-an-iframe-or-frame
void NavigableContainer::navigate_an_iframe_or_frame(AK::URL url, ReferrerPolicy::ReferrerPolicy referrer_policy, Optional<String> srcdoc_string)
{
    // 1. Let historyHandling be "auto".
    auto history_handling = Bindings::NavigationHistoryBehavior::Auto;

    // 2. If element's content navigable's active document is not completely loaded, then set historyHandling to "replace".
    if (m_content_navigable->active_document() && !m_content_navigable->active_document()->is_completely_loaded()) {
        history_handling = Bindings::NavigationHistoryBehavior::Replace;
    }

    // FIXME: 3. If element is an iframe, then set element's pending resource-timing start time to the current high resolution
    //           time given element's node document's relevant global object.

    // 4. Navigate element's content navigable to url using element's node document, with historyHandling set to historyHandling,
    //    referrerPolicy set to referrerPolicy, and documentResource set to scrdocString.
    Variant<Empty, String, POSTResource> document_resource = Empty {};
    if (srcdoc_string.has_value())
        document_resource = srcdoc_string.value();
    MUST(m_content_navigable->navigate(url, document(), document_resource, nullptr, false, history_handling, {}, {}, referrer_policy));
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#destroy-a-child-navigable
void NavigableContainer::destroy_the_child_navigable()
{
    // 1. Let navigable be container's content navigable.
    auto navigable = content_navigable();

    // 2. If navigable is null, then return.
    if (!navigable)
        return;

    // 3. Set container's content navigable to null.
    m_content_navigable = nullptr;

    // 4. Destroy navigable's active document.
    navigable->active_document()->destroy();

    // 5. Let parentDocState be container's node navigable's active session history entry's document state.
    auto parent_doc_state = navigable->active_session_history_entry()->document_state;

    // 6. Remove the nested history from parentDocState's nested histories whose id equals navigable's id.
    parent_doc_state->nested_histories().remove_all_matching([&](auto& nested_history) {
        return navigable->id() == nested_history.id;
    });

    // 7. Let traversable be container's node navigable's traversable navigable.
    auto traversable = navigable->traversable_navigable();

    // Not in the spec
    navigable->set_has_been_destroyed();
    HTML::all_navigables().remove(navigable);

    // 8. Append the following session history traversal steps to traversable:
    traversable->append_session_history_traversal_steps([traversable] {
        // 1. Apply pending history changes to traversable.
        traversable->update_for_navigable_creation_or_destruction();
    });
}

}
