/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/HTMLCollection.h>
#include <LibWeb/DOM/Range.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Requests.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/BrowsingContextGroup.h>
#include <LibWeb/HTML/CrossOrigin/CrossOriginOpenerPolicy.h>
#include <LibWeb/HTML/DocumentState.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/HTMLDocument.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/HTML/NavigableContainer.h>
#include <LibWeb/HTML/RemoteBrowsingContext.h>
#include <LibWeb/HTML/SandboxingFlagSet.h>
#include <LibWeb/HTML/Scripting/WindowEnvironmentSettingsObject.h>
#include <LibWeb/HTML/TraversableNavigable.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/HTML/WindowProxy.h>
#include <LibWeb/HighResolutionTime/TimeOrigin.h>
#include <LibWeb/Infra/Strings.h>
#include <LibWeb/Layout/BreakNode.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Namespace.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/URL/URL.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/urls-and-fetching.html#matches-about:blank
bool url_matches_about_blank(AK::URL const& url)
{
    // A URL matches about:blank if its scheme is "about", its path contains a single string "blank", its username and password are the empty string, and its host is null.
    return url.scheme() == "about"sv
        && url.serialize_path() == "blank"sv
        && url.raw_username().is_empty()
        && url.raw_password().is_empty()
        && url.host().has<Empty>();
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#determining-the-origin
HTML::Origin determine_the_origin(AK::URL const& url, SandboxingFlagSet sandbox_flags, Optional<HTML::Origin> source_origin)
{
    // 1. If sandboxFlags has its sandboxed origin browsing context flag set, then return a new opaque origin.
    if (has_flag(sandbox_flags, SandboxingFlagSet::SandboxedOrigin)) {
        return HTML::Origin {};
    }

    // FIXME: 2. If url is null, then return a new opaque origin.
    // FIXME: There appears to be no way to get a null URL here, so it might be a spec bug.

    // 3. If url is about:srcdoc, then:
    if (url == "about:srcdoc"sv) {
        // 1. Assert: sourceOrigin is non-null.
        VERIFY(source_origin.has_value());

        // 2. Return sourceOrigin.
        return source_origin.release_value();
    }

    // 4. If url matches about:blank and sourceOrigin is non-null, then return sourceOrigin.
    if (url_matches_about_blank(url) && source_origin.has_value())
        return source_origin.release_value();

    // 5. Return url's origin.
    return URL::url_origin(url);
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#creating-a-new-auxiliary-browsing-context
WebIDL::ExceptionOr<BrowsingContext::BrowsingContextAndDocument> BrowsingContext::create_a_new_auxiliary_browsing_context_and_document(Page& page, JS::NonnullGCPtr<HTML::BrowsingContext> opener)
{
    // 1. Let openerTopLevelBrowsingContext be opener's top-level traversable's active browsing context.
    auto opener_top_level_browsing_context = opener->top_level_traversable()->active_browsing_context();

    // 2. Let group be openerTopLevelBrowsingContext's group.
    auto group = opener_top_level_browsing_context->group();

    // 3. Assert: group is non-null, as navigating invokes this directly.
    VERIFY(group);

    // 4. Set browsingContext and document be the result of creating a new browsing context and document with opener's active document, null, and group.
    auto [browsing_context, document] = TRY(create_a_new_browsing_context_and_document(page, opener->active_document(), nullptr, *group));

    // FIXME: 5. Set browsingContext's is auxiliary to true.

    // 6. Append browsingContext to group.
    group->append(browsing_context);

    // 7. Set browsingContext's opener browsing context to opener.
    browsing_context->set_opener_browsing_context(opener);

    // FIXME: 8. Set browsingContext's virtual browsing context group ID to openerTopLevelBrowsingContext's virtual browsing context group ID.

    // FIXME: 9. Set browsingContext's opener origin at creation to opener's active document's origin.

    // 10. Return browsingContext and document.
    return BrowsingContext::BrowsingContextAndDocument { browsing_context, document };
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#creating-a-new-browsing-context
WebIDL::ExceptionOr<BrowsingContext::BrowsingContextAndDocument> BrowsingContext::create_a_new_browsing_context_and_document(Page& page, JS::GCPtr<DOM::Document> creator, JS::GCPtr<DOM::Element> embedder, JS::NonnullGCPtr<BrowsingContextGroup> group)
{
    auto& vm = group->vm();

    // 1. Let browsingContext be a new browsing context.
    JS::NonnullGCPtr<BrowsingContext> browsing_context = *vm.heap().allocate_without_realm<BrowsingContext>(page, nullptr);

    // 2. Let unsafeContextCreationTime be the unsafe shared current time.
    [[maybe_unused]] auto unsafe_context_creation_time = HighResolutionTime::unsafe_shared_current_time();

    // 3. Let creatorOrigin be null.
    Optional<Origin> creator_origin = {};

    // 4. If creator is non-null, then:
    if (creator) {
        // 1. Set creatorOrigin to creator's origin.
        creator_origin = creator->origin();

        // FIXME: 2. Set browsingContext's creator base URL to an algorithm which returns creator's base URL.

        // FIXME: 3. Set browsingContext's virtual browsing context group ID to creator's browsing context's top-level browsing context's virtual browsing context group ID.
    }

    // FIXME: 5. Let sandboxFlags be the result of determining the creation sandboxing flags given browsingContext and embedder.
    SandboxingFlagSet sandbox_flags = {};

    // 6. Let origin be the result of determining the origin given about:blank, sandboxFlags, and creatorOrigin.
    auto origin = determine_the_origin(AK::URL("about:blank"sv), sandbox_flags, creator_origin);

    // FIXME: 7. Let permissionsPolicy be the result of creating a permissions policy given browsingContext and origin. [PERMISSIONSPOLICY]

    // FIXME: 8. Let agent be the result of obtaining a similar-origin window agent given origin, group, and false.

    JS::GCPtr<Window> window;

    // 9. Let realm execution context be the result of creating a new JavaScript realm given agent and the following customizations:
    auto realm_execution_context = Bindings::create_a_new_javascript_realm(
        Bindings::main_thread_vm(),
        [&](JS::Realm& realm) -> JS::Object* {
            auto window_proxy = realm.heap().allocate<WindowProxy>(realm, realm);
            browsing_context->set_window_proxy(window_proxy);

            // - For the global object, create a new Window object.
            window = Window::create(realm);
            return window.ptr();
        },
        [&](JS::Realm&) -> JS::Object* {
            // - For the global this binding, use browsingContext's WindowProxy object.
            return browsing_context->window_proxy();
        });

    // 10. Let topLevelCreationURL be about:blank if embedder is null; otherwise embedder's relevant settings object's top-level creation URL.
    auto top_level_creation_url = !embedder ? AK::URL("about:blank") : relevant_settings_object(*embedder).top_level_creation_url;

    // 11. Let topLevelOrigin be origin if embedder is null; otherwise embedder's relevant settings object's top-level origin.
    auto top_level_origin = !embedder ? origin : relevant_settings_object(*embedder).origin();

    // 12. Set up a window environment settings object with about:blank, realm execution context, null, topLevelCreationURL, and topLevelOrigin.
    WindowEnvironmentSettingsObject::setup(
        AK::URL("about:blank"),
        move(realm_execution_context),
        {},
        top_level_creation_url,
        top_level_origin);

    // 13. Let loadTimingInfo be a new document load timing info with its navigation start time set to the result of calling
    //     coarsen time with unsafeContextCreationTime and the new environment settings object's cross-origin isolated capability.
    auto load_timing_info = DOM::DocumentLoadTimingInfo();
    load_timing_info.navigation_start_time = HighResolutionTime::coarsen_time(
        unsafe_context_creation_time,
        verify_cast<WindowEnvironmentSettingsObject>(Bindings::host_defined_environment_settings_object(window->realm())).cross_origin_isolated_capability() == CanUseCrossOriginIsolatedAPIs::Yes);

    // 14. Let document be a new Document, with:
    auto document = HTML::HTMLDocument::create(window->realm());

    // Non-standard
    window->set_associated_document(*document);

    // type: "html"
    document->set_document_type(DOM::Document::Type::HTML);

    // content type: "text/html"
    document->set_content_type("text/html"_string);

    // mode: "quirks"
    document->set_quirks_mode(DOM::QuirksMode::Yes);

    // origin: origin
    document->set_origin(origin);

    // browsing context: browsingContext
    document->set_browsing_context(browsing_context);

    // FIXME: permissions policy: permissionsPolicy

    // FIXME: active sandboxing flag set: sandboxFlags

    // load timing info: loadTimingInfo
    document->set_load_timing_info(load_timing_info);

    // is initial about:blank: true
    document->set_is_initial_about_blank(true);

    // 15. If creator is non-null, then:
    if (creator) {
        // 1. Set document's referrer to the serialization of creator's URL.
        document->set_referrer(creator->url().serialize());

        // FIXME: 2. Set document's policy container to a clone of creator's policy container.

        // 3. If creator's origin is same origin with creator's relevant settings object's top-level origin,
        if (creator->origin().is_same_origin(creator->relevant_settings_object().top_level_origin)) {
            // then set document's cross-origin opener policy to creator's browsing context's top-level browsing context's active document's cross-origin opener policy.
            VERIFY(creator->browsing_context());
            VERIFY(creator->browsing_context()->top_level_browsing_context()->active_document());
            document->set_cross_origin_opener_policy(creator->browsing_context()->top_level_browsing_context()->active_document()->cross_origin_opener_policy());
        }
    }

    // 16. Assert: document's URL and document's relevant settings object's creation URL are about:blank.
    VERIFY(document->url() == "about:blank"sv);
    VERIFY(document->relevant_settings_object().creation_url == "about:blank"sv);

    // 17. Mark document as ready for post-load tasks.
    document->set_ready_for_post_load_tasks(true);

    // 18. Ensure that document has a single child html node, which itself has two empty child nodes: a head element, and a body element.
    auto html_node = TRY(DOM::create_element(document, HTML::TagNames::html, Namespace::HTML));
    auto head_element = TRY(DOM::create_element(document, HTML::TagNames::head, Namespace::HTML));
    TRY(html_node->append_child(head_element));
    auto body_element = TRY(DOM::create_element(document, HTML::TagNames::body, Namespace::HTML));
    TRY(html_node->append_child(body_element));
    TRY(document->append_child(html_node));

    // 19. Make active document.
    document->make_active();

    // 20. Completely finish loading document.
    document->completely_finish_loading();

    // 21. Return browsingContext and document.
    return BrowsingContext::BrowsingContextAndDocument { browsing_context, document };
}

BrowsingContext::BrowsingContext(Page& page, HTML::NavigableContainer* container)
    : m_page(page)
    , m_loader(*this)
    , m_event_handler({}, *this)
    , m_container(container)
{
    m_cursor_blink_timer = Core::Timer::create_repeating(500, [this] {
        if (!is_focused_context())
            return;
        if (m_cursor_position.node() && m_cursor_position.node()->layout_node()) {
            m_cursor_blink_state = !m_cursor_blink_state;
            m_cursor_position.node()->layout_node()->set_needs_display();
        }
    }).release_value_but_fixme_should_propagate_errors();
}

BrowsingContext::~BrowsingContext() = default;

void BrowsingContext::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);

    for (auto& entry : m_session_history)
        visitor.visit(entry);
    visitor.visit(m_container);
    visitor.visit(m_window_proxy);
    visitor.visit(m_opener_browsing_context);
    visitor.visit(m_group);
    visitor.visit(m_parent);
    visitor.visit(m_first_child);
    visitor.visit(m_last_child);
    visitor.visit(m_next_sibling);
    visitor.visit(m_previous_sibling);
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#bc-traversable
JS::NonnullGCPtr<HTML::TraversableNavigable> BrowsingContext::top_level_traversable() const
{
    // A browsing context's top-level traversable is its active document's node navigable's top-level traversable.
    auto traversable = active_document()->navigable()->top_level_traversable();
    VERIFY(traversable);
    VERIFY(traversable->is_top_level_traversable());
    return *traversable;
}

void BrowsingContext::did_edit(Badge<EditEventHandler>)
{
    reset_cursor_blink_cycle();

    if (m_cursor_position.node() && is<DOM::Text>(*m_cursor_position.node())) {
        auto& text_node = static_cast<DOM::Text&>(*m_cursor_position.node());
        if (auto* text_node_owner = text_node.editable_text_node_owner())
            text_node_owner->did_edit_text_node({});
    }
}

void BrowsingContext::reset_cursor_blink_cycle()
{
    m_cursor_blink_state = true;
    m_cursor_blink_timer->restart();
    if (m_cursor_position.is_valid() && m_cursor_position.node()->layout_node())
        m_cursor_position.node()->layout_node()->set_needs_display();
}

// https://html.spec.whatwg.org/multipage/browsers.html#top-level-browsing-context
bool BrowsingContext::is_top_level() const
{
    // A browsing context that has no parent browsing context is the top-level browsing context for itself and all of the browsing contexts for which it is an ancestor browsing context.
    return !parent();
}

bool BrowsingContext::is_focused_context() const
{
    return m_page && &m_page->focused_context() == this;
}

void BrowsingContext::scroll_to(CSSPixelPoint position)
{
    // NOTE: Scrolling to a position requires up-to-date layout *unless* we're scrolling to (0, 0)
    //       as (0, 0) is always guaranteed to be a valid scroll position.
    if (!position.is_zero()) {
        if (active_document())
            active_document()->update_layout();
    }

    if (m_page && this == &m_page->top_level_browsing_context())
        m_page->client().page_did_request_scroll_to(position);
}

void BrowsingContext::scroll_to_anchor(DeprecatedString const& fragment)
{
    JS::GCPtr<DOM::Document> document = active_document();
    if (!document)
        return;

    auto element = document->get_element_by_id(fragment);
    if (!element) {
        auto candidates = document->get_elements_by_name(MUST(String::from_deprecated_string(fragment)));
        for (auto& candidate : candidates->collect_matching_elements()) {
            if (is<HTML::HTMLAnchorElement>(*candidate)) {
                element = &verify_cast<HTML::HTMLAnchorElement>(*candidate);
                break;
            }
        }
    }

    if (!element)
        return;

    document->update_layout();

    if (!element->layout_node())
        return;

    auto& layout_node = *element->layout_node();

    auto const viewport_rect = document->viewport_rect();
    CSSPixelRect target_rect { layout_node.box_type_agnostic_position(), { viewport_rect.width(), viewport_rect.height() } };
    if (is<Layout::Box>(layout_node)) {
        auto& layout_box = verify_cast<Layout::Box>(layout_node);
        auto padding_box = layout_box.box_model().padding_box();
        target_rect.translate_by(-padding_box.left, -padding_box.top);
    }

    if (m_page && this == &m_page->top_level_browsing_context())
        m_page->client().page_did_request_scroll_into_view(target_rect);
}

JS::GCPtr<BrowsingContext> BrowsingContext::top_level_browsing_context() const
{
    auto const* start = this;

    // 1. If start's active document is not fully active, then return null.
    if (!start->active_document()->is_fully_active()) {
        return nullptr;
    }

    // 2. Let navigable be start's active document's node navigable.
    auto navigable = start->active_document()->navigable();

    // 3. While navigable's parent is not null, set navigable to navigable's parent.
    while (navigable->parent()) {
        navigable = navigable->parent();
    }

    // 4. Return navigable's active browsing context.
    return navigable->active_browsing_context();
}

CSSPixelRect BrowsingContext::to_top_level_rect(CSSPixelRect const& a_rect)
{
    auto rect = a_rect;
    rect.set_location(to_top_level_position(a_rect.location()));
    return rect;
}

CSSPixelPoint BrowsingContext::to_top_level_position(CSSPixelPoint a_position)
{
    auto position = a_position;
    for (auto ancestor = parent(); ancestor; ancestor = ancestor->parent()) {
        if (ancestor->is_top_level())
            break;
        if (!ancestor->container())
            return {};
        if (!ancestor->container()->layout_node())
            return {};
        position.translate_by(ancestor->container()->layout_node()->box_type_agnostic_position());
    }
    return position;
}

void BrowsingContext::set_cursor_position(DOM::Position position)
{
    if (m_cursor_position == position)
        return;

    if (m_cursor_position.node() && m_cursor_position.node()->layout_node())
        m_cursor_position.node()->layout_node()->set_needs_display();

    m_cursor_position = move(position);

    if (m_cursor_position.node() && m_cursor_position.node()->layout_node())
        m_cursor_position.node()->layout_node()->set_needs_display();

    reset_cursor_blink_cycle();
}

static DeprecatedString visible_text_in_range(DOM::Range const& range)
{
    // NOTE: This is an adaption of Range stringification, but we skip over DOM nodes that don't have a corresponding layout node.
    StringBuilder builder;

    if (range.start_container() == range.end_container() && is<DOM::Text>(*range.start_container())) {
        if (!range.start_container()->layout_node())
            return ""sv;
        return static_cast<DOM::Text const&>(*range.start_container()).data().substring(range.start_offset(), range.end_offset() - range.start_offset());
    }

    if (is<DOM::Text>(*range.start_container()) && range.start_container()->layout_node())
        builder.append(static_cast<DOM::Text const&>(*range.start_container()).data().substring_view(range.start_offset()));

    for (DOM::Node const* node = range.start_container(); node != range.end_container()->next_sibling(); node = node->next_in_pre_order()) {
        if (is<DOM::Text>(*node) && range.contains_node(*node) && node->layout_node())
            builder.append(static_cast<DOM::Text const&>(*node).data());
    }

    if (is<DOM::Text>(*range.end_container()) && range.end_container()->layout_node())
        builder.append(static_cast<DOM::Text const&>(*range.end_container()).data().substring_view(0, range.end_offset()));

    return builder.to_deprecated_string();
}

DeprecatedString BrowsingContext::selected_text() const
{
    auto* document = active_document();
    if (!document)
        return ""sv;
    auto selection = const_cast<DOM::Document&>(*document).get_selection();
    auto range = selection->range();
    if (!range)
        return ""sv;
    return visible_text_in_range(*range);
}

void BrowsingContext::select_all()
{
    auto* document = active_document();
    if (!document)
        return;
    auto* body = document->body();
    if (!body)
        return;
    auto selection = document->get_selection();
    if (!selection)
        return;
    (void)selection->select_all_children(*document->body());
}

void BrowsingContext::register_frame_nesting(AK::URL const& url)
{
    m_frame_nesting_levels.ensure(url)++;
}

bool BrowsingContext::is_frame_nesting_allowed(AK::URL const& url) const
{
    return m_frame_nesting_levels.get(url).value_or(0) < 3;
}

bool BrowsingContext::increment_cursor_position_offset()
{
    if (!m_cursor_position.increment_offset())
        return false;
    reset_cursor_blink_cycle();
    return true;
}

bool BrowsingContext::decrement_cursor_position_offset()
{
    if (!m_cursor_position.decrement_offset())
        return false;
    reset_cursor_blink_cycle();
    return true;
}

// https://html.spec.whatwg.org/#rendering-opportunity
bool BrowsingContext::has_a_rendering_opportunity() const
{
    // A browsing context has a rendering opportunity if the user agent is currently able to present the contents of the browsing context to the user,
    // accounting for hardware refresh rate constraints and user agent throttling for performance reasons, but considering content presentable even if it's outside the viewport.

    // FIXME: We should at the very least say `false` here if we're an inactive browser tab.
    return true;
}

// https://html.spec.whatwg.org/multipage/interaction.html#currently-focused-area-of-a-top-level-browsing-context
JS::GCPtr<DOM::Node> BrowsingContext::currently_focused_area()
{
    // 1. If topLevelBC does not have system focus, then return null.
    if (!is_focused_context())
        return nullptr;

    // 2. Let candidate be topLevelBC's active document.
    auto* candidate = active_document();

    // 3. While candidate's focused area is a browsing context container with a non-null nested browsing context:
    //    set candidate to the active document of that browsing context container's nested browsing context.
    while (candidate->focused_element()
        && is<HTML::NavigableContainer>(candidate->focused_element())
        && static_cast<HTML::NavigableContainer&>(*candidate->focused_element()).nested_browsing_context()) {
        candidate = static_cast<HTML::NavigableContainer&>(*candidate->focused_element()).nested_browsing_context()->active_document();
    }

    // 4. If candidate's focused area is non-null, set candidate to candidate's focused area.
    if (candidate->focused_element()) {
        // NOTE: We return right away here instead of assigning to candidate,
        //       since that would require compromising type safety.
        return candidate->focused_element();
    }

    // 5. Return candidate.
    return candidate;
}

// https://html.spec.whatwg.org/#the-rules-for-choosing-a-browsing-context-given-a-browsing-context-name
BrowsingContext::ChosenBrowsingContext BrowsingContext::choose_a_browsing_context(StringView name, TokenizedFeature::NoOpener no_opener, ActivateTab activate_tab)
{
    // The rules for choosing a browsing context, given a browsing context name name, a browsing context current, and
    // a boolean noopener are as follows:
    JS::GCPtr<AbstractBrowsingContext> matching_name_in_tree = nullptr;
    top_level_browsing_context()->for_each_in_subtree([&](auto& context) {
        if (context.name() == name) {
            matching_name_in_tree = &context;
            return IterationDecision::Break;
        }

        return IterationDecision::Continue;
    });

    // 1. Let chosen be null.
    JS::GCPtr<AbstractBrowsingContext> chosen = nullptr;

    // 2. Let windowType be "existing or none".
    auto window_type = WindowType::ExistingOrNone;

    // 3. Let sandboxingFlagSet be current's active document's active sandboxing flag set.
    auto sandboxing_flag_set = active_document()->active_sandboxing_flag_set();

    // 4. If name is the empty string or an ASCII case-insensitive match for "_self", then set chosen to current.
    if (name.is_empty() || Infra::is_ascii_case_insensitive_match(name, "_self"sv)) {
        chosen = this;
    }

    // 5. Otherwise, if name is an ASCII case-insensitive match for "_parent", set chosen to current's parent browsing
    //    context, if any, and current otherwise.
    else if (Infra::is_ascii_case_insensitive_match(name, "_parent"sv)) {
        if (auto parent = this->parent())
            chosen = parent;
        else
            chosen = this;
    }

    // 6. Otherwise, if name is an ASCII case-insensitive match for "_top", set chosen to current's top-level browsing
    //    context, if any, and current otherwise.
    else if (Infra::is_ascii_case_insensitive_match(name, "_top"sv)) {
        chosen = top_level_browsing_context();
    }

    // 7. Otherwise, if name is not an ASCII case-insensitive match for "_blank", there exists a browsing context
    //    whose name is the same as name, current is familiar with that browsing context, and the user agent
    //    determines that the two browsing contexts are related enough that it is ok if they reach each other,
    //    set chosen to that browsing context. If there are multiple matching browsing contexts, the user agent
    //    should set chosen to one in some arbitrary consistent manner, such as the most recently opened, most
    //    recently focused, or more closely related.
    else if (!Infra::is_ascii_case_insensitive_match(name, "_blank"sv) && matching_name_in_tree) {
        chosen = matching_name_in_tree;
    } else {
        // 8. Otherwise, a new browsing context is being requested, and what happens depends on the user agent's
        //    configuration and abilities — it is determined by the rules given for the first applicable option from
        //    the following list:

        // --> If current's active window does not have transient activation and the user agent has been configured to
        //     not show popups (i.e., the user agent has a "popup blocker" enabled)
        VERIFY(m_page);
        if (!active_window()->has_transient_activation() && m_page->should_block_pop_ups()) {
            // FIXME: The user agent may inform the user that a popup has been blocked.
            dbgln("Pop-up blocked!");
        }

        // --> If sandboxingFlagSet has the sandboxed auxiliary navigation browsing context flag set
        else if (has_flag(sandboxing_flag_set, SandboxingFlagSet::SandboxedAuxiliaryNavigation)) {
            // FIXME: The user agent may report to a developer console that a popup has been blocked.
            dbgln("Pop-up blocked!");
        }

        // --> If the user agent has been configured such that in this instance it will create a new browsing context
        else if (true) { // FIXME: When is this the case?
            // 1. Set windowType to "new and unrestricted".
            window_type = WindowType::NewAndUnrestricted;

            // 2. If current's top-level browsing context's active document's cross-origin opener policy's value is
            //    "same-origin" or "same-origin-plus-COEP", then:
            if (top_level_browsing_context()->active_document()->cross_origin_opener_policy().value == CrossOriginOpenerPolicyValue::SameOrigin || top_level_browsing_context()->active_document()->cross_origin_opener_policy().value == CrossOriginOpenerPolicyValue::SameOriginPlusCOEP) {
                // 1. Let currentDocument be current's active document.
                auto* current_document = top_level_browsing_context()->active_document();

                // 2. If currentDocument's origin is not same origin with currentDocument's relevant settings object's
                //    top-level origin, then set noopener to true, name to "_blank", and windowType to "new with no opener".
                if (!current_document->origin().is_same_origin(current_document->relevant_settings_object().top_level_origin)) {
                    no_opener = TokenizedFeature::NoOpener::Yes;
                    name = "_blank"sv;
                    window_type = WindowType::NewWithNoOpener;
                }
            }

            // 3. If noopener is true, then set chosen to the result of creating a new top-level browsing context.
            if (no_opener == TokenizedFeature::NoOpener::Yes) {
                auto handle = m_page->client().page_did_request_new_tab(activate_tab);
                chosen = RemoteBrowsingContext::create_a_new_remote_browsing_context(handle);
            }

            // 4. Otherwise:
            else {
                // 1. Set chosen to the result of creating a new auxiliary browsing context with current.
                // FIXME: We have no concept of auxiliary browsing context
                chosen = HTML::create_a_new_top_level_browsing_context_and_document(*m_page).release_value_but_fixme_should_propagate_errors().browsing_context;

                // 2. If sandboxingFlagSet's sandboxed navigation browsing context flag is set, then current must be
                //    set as chosen's one permitted sandboxed navigator.
                // FIXME: We have no concept of one permitted sandboxed navigator
            }

            // 5. If sandboxingFlagSet's sandbox propagates to auxiliary browsing contexts flag is set, then all the
            //    flags that are set in sandboxingFlagSet must be set in chosen's popup sandboxing flag set.
            // FIXME: Our BrowsingContexts do not have SandboxingFlagSets yet, only documents do

            // 6. If name is not an ASCII case-insensitive match for "_blank", then set chosen's name to name.
            if (!Infra::is_ascii_case_insensitive_match(name, "_blank"sv))
                chosen->set_name(String::from_utf8(name).release_value_but_fixme_should_propagate_errors());
        }

        // --> If the user agent has been configured such that in this instance t will reuse current
        else if (false) { // FIXME: When is this the case?
            // Set chosen to current.
            chosen = *this;
        }

        // --> If the user agent has been configured such that in this instance it will not find a browsing context
        else if (false) { // FIXME: When is this the case?
            // Do nothing.
        }
    }

    // 9. Return chosen and windowType.
    return { chosen.ptr(), window_type };
}

// https://html.spec.whatwg.org/multipage/dom.html#still-on-its-initial-about:blank-document
bool BrowsingContext::still_on_its_initial_about_blank_document() const
{
    // A browsing context browsingContext is still on its initial about:blank Document
    // if browsingContext's session history's size is 1
    // and browsingContext's session history[0]'s document's is initial about:blank is true.
    return m_session_history.size() == 1
        && m_session_history[0]->document_state->document()
        && m_session_history[0]->document_state->document()->is_initial_about_blank();
}

DOM::Document const* BrowsingContext::active_document() const
{
    auto* window = active_window();
    if (!window)
        return nullptr;
    return &window->associated_document();
}

DOM::Document* BrowsingContext::active_document()
{
    auto* window = active_window();
    if (!window)
        return nullptr;
    return &window->associated_document();
}

// https://html.spec.whatwg.org/multipage/browsers.html#active-window
HTML::Window* BrowsingContext::active_window()
{
    return m_window_proxy->window();
}

// https://html.spec.whatwg.org/multipage/browsers.html#active-window
HTML::Window const* BrowsingContext::active_window() const
{
    return m_window_proxy->window();
}

HTML::WindowProxy* BrowsingContext::window_proxy()
{
    return m_window_proxy.ptr();
}

HTML::WindowProxy const* BrowsingContext::window_proxy() const
{
    return m_window_proxy.ptr();
}

void BrowsingContext::set_window_proxy(JS::GCPtr<WindowProxy> window_proxy)
{
    m_window_proxy = move(window_proxy);
}

void BrowsingContext::scroll_offset_did_change()
{
    // https://w3c.github.io/csswg-drafts/cssom-view-1/#scrolling-events
    // Whenever a viewport gets scrolled (whether in response to user interaction or by an API), the user agent must run these steps:

    // 1. Let doc be the viewport’s associated Document.
    auto* doc = active_document();
    VERIFY(doc);

    // 2. If doc is already in doc’s pending scroll event targets, abort these steps.
    for (auto& target : doc->pending_scroll_event_targets()) {
        if (target.ptr() == doc)
            return;
    }

    // 3. Append doc to doc’s pending scroll event targets.
    doc->pending_scroll_event_targets().append(*doc);
}

BrowsingContextGroup* BrowsingContext::group()
{
    return m_group;
}

void BrowsingContext::set_group(BrowsingContextGroup* group)
{
    m_group = group;
}

// https://html.spec.whatwg.org/multipage/browsers.html#bcg-remove
void BrowsingContext::remove()
{
    // 1. Assert: browsingContext's group is non-null, because a browsing context only gets discarded once.
    VERIFY(group());

    // 2. Let group be browsingContext's group.
    JS::NonnullGCPtr<BrowsingContextGroup> group = *this->group();

    // 3. Set browsingContext's group to null.
    set_group(nullptr);

    // 4. Remove browsingContext from group's browsing context set.
    group->browsing_context_set().remove(*this);

    // 5. If group's browsing context set is empty, then remove group from the user agent's browsing context group set.
    // NOTE: This is done by ~BrowsingContextGroup() when the refcount reaches 0.
}

// https://html.spec.whatwg.org/multipage/origin.html#one-permitted-sandboxed-navigator
BrowsingContext const* BrowsingContext::the_one_permitted_sandboxed_navigator() const
{
    // FIXME: Implement this.
    return nullptr;
}

// https://html.spec.whatwg.org/multipage/browsers.html#document-family
Vector<JS::Handle<DOM::Document>> BrowsingContext::document_family() const
{
    HashTable<DOM::Document*> documents;
    for (auto& entry : m_session_history) {
        if (!entry->document_state->document())
            continue;
        if (documents.set(const_cast<DOM::Document*>(entry->document_state->document().ptr())) == AK::HashSetResult::ReplacedExistingEntry)
            continue;
        for (auto& context : entry->document_state->document()->list_of_descendant_browsing_contexts()) {
            for (auto& document : context->document_family()) {
                documents.set(document.ptr());
            }
        }
    }

    Vector<JS::Handle<DOM::Document>> family;
    for (auto* document : documents) {
        family.append(*document);
    }
    return family;
}

// https://html.spec.whatwg.org/multipage/browsers.html#document-family
bool BrowsingContext::document_family_contains(DOM::Document const& document) const
{
    return document_family().first_matching([&](auto& entry) { return entry.ptr() == &document; }).has_value();
}

VisibilityState BrowsingContext::system_visibility_state() const
{
    return m_system_visibility_state;
}

// https://html.spec.whatwg.org/multipage/interaction.html#system-visibility-state
void BrowsingContext::set_system_visibility_state(VisibilityState visibility_state)
{
    if (m_system_visibility_state == visibility_state)
        return;
    m_system_visibility_state = visibility_state;

    // When a user-agent determines that the system visibility state for top-level browsing context context
    // has changed to newState, it must queue a task on the user interaction task source to update
    // the visibility state of all the Document objects in the top-level browsing context's document family with newState.
    auto document_family = top_level_browsing_context()->document_family();

    // From the new navigable version, where it tells us what global object to use here:
    // 1. Let document be navigable's active document.
    // 2. Queue a global task on the user interaction task source given document's relevant global object to update the visibility state of document with newState.
    // FIXME: Update this function to fully match the navigable version.
    VERIFY(active_document());
    queue_global_task(Task::Source::UserInteraction, relevant_global_object(*active_document()), [visibility_state, document_family = move(document_family)] {
        for (auto& document : document_family) {
            document->update_the_visibility_state(visibility_state);
        }
    });
}

void BrowsingContext::append_child(JS::NonnullGCPtr<BrowsingContext> child)
{
    VERIFY(!child->m_parent);

    if (m_last_child)
        m_last_child->m_next_sibling = child;
    child->m_previous_sibling = m_last_child;
    child->m_parent = this;
    m_last_child = child;
    if (!m_first_child)
        m_first_child = m_last_child;
}

void BrowsingContext::remove_child(JS::NonnullGCPtr<BrowsingContext> child)
{
    VERIFY(child->m_parent.ptr() == this);

    if (m_first_child == child)
        m_first_child = child->m_next_sibling;

    if (m_last_child == child)
        m_last_child = child->m_previous_sibling;

    if (child->m_next_sibling)
        child->m_next_sibling->m_previous_sibling = child->m_previous_sibling;

    if (child->m_previous_sibling)
        child->m_previous_sibling->m_next_sibling = child->m_next_sibling;

    child->m_next_sibling = nullptr;
    child->m_previous_sibling = nullptr;
    child->m_parent = nullptr;
}

JS::GCPtr<BrowsingContext> BrowsingContext::first_child() const
{
    return m_first_child;
}
JS::GCPtr<BrowsingContext> BrowsingContext::next_sibling() const
{
    return m_next_sibling;
}

bool BrowsingContext::is_ancestor_of(BrowsingContext const& other) const
{
    for (auto ancestor = other.parent(); ancestor; ancestor = ancestor->parent()) {
        if (ancestor == this)
            return true;
    }
    return false;
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#snapshotting-target-snapshot-params
SandboxingFlagSet determine_the_creation_sandboxing_flags(BrowsingContext const&, JS::GCPtr<DOM::Element>)
{
    // FIXME: Populate this once we have the proper flag sets on BrowsingContext
    return {};
}

}
