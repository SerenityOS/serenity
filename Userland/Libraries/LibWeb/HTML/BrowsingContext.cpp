/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/HTMLCollection.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/BrowsingContextContainer.h>
#include <LibWeb/HTML/BrowsingContextGroup.h>
#include <LibWeb/HTML/CrossOrigin/CrossOriginOpenerPolicy.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/HTML/SandboxingFlagSet.h>
#include <LibWeb/HTML/Scripting/WindowEnvironmentSettingsObject.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Layout/BreakNode.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/Page/Page.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/urls-and-fetching.html#matches-about:blank
static bool url_matches_about_blank(AK::URL const& url)
{
    // A URL matches about:blank if its scheme is "about", its path contains a single string "blank", its username and password are the empty string, and its host is null.
    return url.scheme() == "about"sv
        && url.path() == "blank"sv
        && url.username().is_empty()
        && url.password().is_empty()
        && url.host().is_null();
}

// https://url.spec.whatwg.org/#concept-url-origin
static HTML::Origin url_origin(AK::URL const& url)
{
    // FIXME: Move this whole function somewhere better.

    if (url.scheme() == "blob"sv) {
        // FIXME: Implement
        return HTML::Origin {};
    }

    if (url.scheme().is_one_of("ftp"sv, "http"sv, "https"sv, "ws"sv, "wss"sv)) {
        // Return the tuple origin (url’s scheme, url’s host, url’s port, null).
        return HTML::Origin(url.scheme(), url.host(), url.port().value_or(0));
    }

    if (url.scheme() == "file"sv) {
        // Unfortunate as it is, this is left as an exercise to the reader. When in doubt, return a new opaque origin.
        // Note: We must return an origin with the `file://' protocol for `file://' iframes to work from `file://' pages.
        return HTML::Origin(url.protocol(), String(), 0);
    }

    return HTML::Origin {};
}

// https://html.spec.whatwg.org/multipage/browsers.html#determining-the-origin
HTML::Origin determine_the_origin(BrowsingContext const& browsing_context, Optional<AK::URL> url, SandboxingFlagSet sandbox_flags, Optional<HTML::Origin> invocation_origin)
{
    // 1. If sandboxFlags has its sandboxed origin browsing context flag set, then return a new opaque origin.
    if (sandbox_flags.flags & SandboxingFlagSet::SandboxedOrigin) {
        return HTML::Origin {};
    }

    // 2. If url is null, then return a new opaque origin.
    if (!url.has_value()) {
        return HTML::Origin {};
    }

    // 3. If invocationOrigin is non-null and url matches about:blank, then return invocationOrigin.
    if (invocation_origin.has_value() && url_matches_about_blank(*url)) {
        return invocation_origin.value();
    }

    // 4. If url is about:srcdoc, then return the origin of browsingContext's container document.
    if (url == AK::URL("about:srcdoc")) {
        VERIFY(browsing_context.container_document());
        return browsing_context.container_document()->origin();
    }

    // 5. Return url's origin.
    return url_origin(*url);
}

// https://html.spec.whatwg.org/multipage/browsers.html#creating-a-new-top-level-browsing-context
NonnullRefPtr<BrowsingContext> BrowsingContext::create_a_new_top_level_browsing_context(Web::Page& page)
{
    // 1. Let group be the result of creating a new browsing context group.
    auto group = BrowsingContextGroup::create_a_new_browsing_context_group(page);

    // 2. Return group's browsing context set[0].
    return *group->browsing_context_set().begin();
}

// https://html.spec.whatwg.org/multipage/browsers.html#creating-a-new-browsing-context
NonnullRefPtr<BrowsingContext> BrowsingContext::create_a_new_browsing_context(Page& page, JS::GCPtr<DOM::Document> creator, JS::GCPtr<DOM::Element> embedder, BrowsingContextGroup&)
{
    // 1. Let browsingContext be a new browsing context.
    BrowsingContextContainer* container = (embedder && is<BrowsingContextContainer>(*embedder)) ? static_cast<BrowsingContextContainer*>(embedder.ptr()) : nullptr;
    auto browsing_context = adopt_ref(*new BrowsingContext(page, container));

    // 2. Let unsafeContextCreationTime be the unsafe shared current time.
    [[maybe_unused]] auto unsafe_context_creation_time = HTML::main_thread_event_loop().unsafe_shared_current_time();

    // 3. If creator is non-null, then set browsingContext's creator origin to return creator's origin,
    //    browsingContext's creator URL to return creator's URL,
    //    browsingContext's creator base URL to return creator's base URL,
    //    FIXME: and browsingContext's virtual browsing context group ID to creator's top-level browsing context's virtual browsing context group ID.
    if (creator) {
        browsing_context->m_creator_origin = creator->origin();
        browsing_context->m_creator_url = creator->url();
        browsing_context->m_creator_base_url = creator->base_url();
    }

    // FIXME: 4. Let sandboxFlags be the result of determining the creation sandboxing flags given browsingContext and embedded.
    SandboxingFlagSet sandbox_flags;

    // 5. Let origin be the result of determining the origin given browsingContext, about:blank, sandboxFlags, and browsingContext's creator origin.
    auto origin = determine_the_origin(browsing_context, AK::URL("about:blank"), sandbox_flags, browsing_context->m_creator_origin);

    // FIXME: 6. Let permissionsPolicy be the result of creating a permissions policy given browsingContext and origin. [PERMISSIONSPOLICY]

    // FIXME: 7. Let agent be the result of obtaining a similar-origin window agent given origin, group, and false.

    JS::GCPtr<Window> window;

    // 8. Let realm execution context be the result of creating a new JavaScript realm given agent and the following customizations:
    auto realm_execution_context = Bindings::create_a_new_javascript_realm(
        Bindings::main_thread_vm(),
        [&](JS::Realm& realm) -> JS::Object* {
            // - For the global object, create a new Window object.
            window = HTML::Window::create(realm);
            return window.ptr();
        },
        [](JS::Realm&) -> JS::Object* {
            // FIXME: - For the global this binding, use browsingContext's WindowProxy object.
            return nullptr;
        });

    // 9. Let topLevelCreationURL be about:blank if embedder is null; otherwise embedder's relevant settings object's top-level creation URL.
    auto top_level_creation_url = !embedder ? AK::URL("about:blank") : relevant_settings_object(*embedder).top_level_creation_url;

    // 10. Let topLevelOrigin be origin if embedder is null; otherwise embedder's relevant settings object's top-level origin.
    auto top_level_origin = !embedder ? origin : relevant_settings_object(*embedder).origin();

    // 11. Set up a window environment settings object with about:blank, realm execution context, null, topLevelCreationURL, and topLevelOrigin.
    HTML::WindowEnvironmentSettingsObject::setup(
        AK::URL("about:blank"),
        move(realm_execution_context),
        {},
        top_level_creation_url,
        top_level_origin);

    // FIXME: 12. Let loadTimingInfo be a new document load timing info with its navigation start time set to the result of calling
    //            coarsen time with unsafeContextCreationTime and the new environment settings object's cross-origin isolated capability.

    // 13. Let coop be a new cross-origin opener policy.
    auto coop = CrossOriginOpenerPolicy {};

    // 14. If creator is non-null and creator's origin is same origin with creator's relevant settings object's top-level origin,
    //     then set coop to creator's browsing context's top-level browsing context's active document's cross-origin opener policy.
    if (creator && creator->origin().is_same_origin(relevant_settings_object(*creator).top_level_origin)) {
        VERIFY(creator->browsing_context());
        auto* top_level_document = creator->browsing_context()->top_level_browsing_context().active_document();
        VERIFY(top_level_document);
        coop = top_level_document->cross_origin_opener_policy();
    }

    // 15. Let document be a new Document, marked as an HTML document in quirks mode,
    //     whose content type is "text/html",
    //     origin is origin,
    //     FIXME: active sandboxing flag set is sandboxFlags,
    //     FIXME: permissions policy is permissionsPolicy,
    //     cross-origin opener policy is coop,
    //     FIXME: load timing info is loadTimingInfo,
    //     FIXME: navigation id is null,
    //     and which is ready for post-load tasks.
    auto document = DOM::Document::create(*window);

    // Non-standard
    document->set_window({}, *window);
    window->set_associated_document(*document);

    document->set_quirks_mode(DOM::QuirksMode::Yes);
    document->set_content_type("text/html");
    document->set_origin(origin);
    document->set_url(AK::URL("about:blank"));
    document->set_cross_origin_opener_policy(coop);
    document->set_ready_for_post_load_tasks(true);

    // FIXME: 16. Assert: document's URL and document's relevant settings object's creation URL are about:blank.

    // 17. Set document's is initial about:blank to true.
    document->set_is_initial_about_blank(true);

    // 18. Ensure that document has a single child html node, which itself has two empty child nodes: a head element, and a body element.
    auto html_node = document->create_element(HTML::TagNames::html).release_value();
    html_node->append_child(document->create_element(HTML::TagNames::head).release_value());
    html_node->append_child(document->create_element(HTML::TagNames::body).release_value());
    document->append_child(html_node);

    // 19. Set the active document of browsingContext to document.
    browsing_context->m_active_document = JS::make_handle(*document);

    // 20. If browsingContext's creator URL is non-null, then set document's referrer to the serialization of it.
    if (browsing_context->m_creator_url.has_value()) {
        document->set_referrer(browsing_context->m_creator_url->serialize());
    }

    // FIXME: 21. If creator is non-null, then set document's policy container to a clone of creator's policy container.

    // 22. Append a new session history entry to browsingContext's session history whose URL is about:blank and document is document.
    browsing_context->m_session_history.append(HTML::SessionHistoryEntry {
        .url = AK::URL("about:blank"),
        .document = document.ptr(),
        .serialized_state = {},
        .policy_container = {},
        .scroll_restoration_mode = {},
        .browsing_context_name = {},
    });

    // Non-standard:
    document->attach_to_browsing_context({}, browsing_context);

    // 23. Completely finish loading document.
    document->completely_finish_loading();

    // 24. Return browsingContext.
    return browsing_context;
}

BrowsingContext::BrowsingContext(Page& page, HTML::BrowsingContextContainer* container)
    : m_page(page)
    , m_loader(*this)
    , m_event_handler({}, *this)
    , m_container(container)
{
    m_cursor_blink_timer = Platform::Timer::create_repeating(500, [this] {
        if (!is_focused_context())
            return;
        if (m_cursor_position.node() && m_cursor_position.node()->layout_node()) {
            m_cursor_blink_state = !m_cursor_blink_state;
            m_cursor_position.node()->layout_node()->set_needs_display();
        }
    });
}

BrowsingContext::~BrowsingContext() = default;

void BrowsingContext::did_edit(Badge<EditEventHandler>)
{
    reset_cursor_blink_cycle();

    if (m_cursor_position.node() && is<DOM::Text>(*m_cursor_position.node())) {
        auto& text_node = static_cast<DOM::Text&>(*m_cursor_position.node());
        if (auto* input_element = text_node.owner_input_element())
            input_element->did_edit_text_node({});
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

void BrowsingContext::set_active_document(DOM::Document* document)
{
    if (m_active_document.ptr() == document)
        return;

    m_cursor_position = {};

    if (m_active_document)
        m_active_document->detach_from_browsing_context({}, *this);

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#resetBCName
    // FIXME: The rest of set_active_document does not follow the spec very closely, this just implements the
    //   relevant steps for resetting the browsing context name and should be updated closer to the spec once
    //   the other parts of history handling/navigating are implemented
    // 3. If newDocument's origin is not same origin with the current entry's document's origin, then:
    if (!document || !m_active_document || !document->origin().is_same_origin(m_active_document->origin())) {
        // 3. If the browsing context is a top-level browsing context, but not an auxiliary browsing context
        //    whose disowned is false, then set the browsing context's name to the empty string.
        // FIXME: this is not checking the second part of the condition yet
        if (is_top_level())
            m_name = String::empty();
    }

    m_active_document = JS::make_handle(document);

    if (m_active_document) {
        m_active_document->attach_to_browsing_context({}, *this);
        if (m_page && is_top_level())
            m_page->client().page_did_change_title(m_active_document->title());
    }
}

void BrowsingContext::set_viewport_rect(Gfx::IntRect const& rect)
{
    bool did_change = false;

    if (m_size != rect.size()) {
        m_size = rect.size();
        if (auto* document = active_document()) {
            // NOTE: Resizing the viewport changes the reference value for viewport-relative CSS lengths.
            document->invalidate_style();
            document->invalidate_layout();
        }
        did_change = true;
    }

    if (m_viewport_scroll_offset != rect.location()) {
        m_viewport_scroll_offset = rect.location();
        scroll_offset_did_change();
        did_change = true;
    }

    if (did_change) {
        for (auto* client : m_viewport_clients)
            client->browsing_context_did_set_viewport_rect(rect);
    }

    // Schedule the HTML event loop to ensure that a `resize` event gets fired.
    HTML::main_thread_event_loop().schedule();
}

void BrowsingContext::set_size(Gfx::IntSize const& size)
{
    if (m_size == size)
        return;
    m_size = size;

    if (auto* document = active_document()) {
        document->invalidate_style();
        document->invalidate_layout();
    }

    for (auto* client : m_viewport_clients)
        client->browsing_context_did_set_viewport_rect(viewport_rect());

    // Schedule the HTML event loop to ensure that a `resize` event gets fired.
    HTML::main_thread_event_loop().schedule();
}

void BrowsingContext::set_needs_display()
{
    set_needs_display(viewport_rect());
}

void BrowsingContext::set_needs_display(Gfx::IntRect const& rect)
{
    if (!viewport_rect().intersects(rect))
        return;

    if (is_top_level()) {
        if (m_page)
            m_page->client().page_did_invalidate(to_top_level_rect(rect));
        return;
    }

    if (container() && container()->layout_node())
        container()->layout_node()->set_needs_display();
}

void BrowsingContext::scroll_to(Gfx::IntPoint const& position)
{
    if (active_document())
        active_document()->force_layout();

    if (m_page)
        m_page->client().page_did_request_scroll_to(position);
}

void BrowsingContext::scroll_to_anchor(String const& fragment)
{
    if (!active_document())
        return;

    auto element = active_document()->get_element_by_id(fragment);
    if (!element) {
        auto candidates = active_document()->get_elements_by_name(fragment);
        for (auto& candidate : candidates->collect_matching_elements()) {
            if (is<HTML::HTMLAnchorElement>(*candidate)) {
                element = &verify_cast<HTML::HTMLAnchorElement>(*candidate);
                break;
            }
        }
    }

    active_document()->force_layout();

    if (!element || !element->layout_node())
        return;

    auto& layout_node = *element->layout_node();

    Gfx::FloatRect float_rect { layout_node.box_type_agnostic_position(), { (float)viewport_rect().width(), (float)viewport_rect().height() } };
    if (is<Layout::Box>(layout_node)) {
        auto& layout_box = verify_cast<Layout::Box>(layout_node);
        auto padding_box = layout_box.box_model().padding_box();
        float_rect.translate_by(-padding_box.left, -padding_box.top);
    }

    if (m_page)
        m_page->client().page_did_request_scroll_into_view(enclosing_int_rect(float_rect));
}

Gfx::IntRect BrowsingContext::to_top_level_rect(Gfx::IntRect const& a_rect)
{
    auto rect = a_rect;
    rect.set_location(to_top_level_position(a_rect.location()));
    return rect;
}

Gfx::IntPoint BrowsingContext::to_top_level_position(Gfx::IntPoint const& a_position)
{
    auto position = a_position;
    for (auto* ancestor = parent(); ancestor; ancestor = ancestor->parent()) {
        if (ancestor->is_top_level())
            break;
        if (!ancestor->container())
            return {};
        if (!ancestor->container()->layout_node())
            return {};
        position.translate_by(ancestor->container()->layout_node()->box_type_agnostic_position().to_type<int>());
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

String BrowsingContext::selected_text() const
{
    StringBuilder builder;
    if (!active_document())
        return {};
    auto* layout_root = active_document()->layout_node();
    if (!layout_root)
        return {};
    if (!layout_root->selection().is_valid())
        return {};

    auto selection = layout_root->selection().normalized();

    if (selection.start().layout_node == selection.end().layout_node) {
        if (!is<Layout::TextNode>(*selection.start().layout_node))
            return "";
        return verify_cast<Layout::TextNode>(*selection.start().layout_node).text_for_rendering().substring(selection.start().index_in_node, selection.end().index_in_node - selection.start().index_in_node);
    }

    // Start node
    auto layout_node = selection.start().layout_node;
    if (is<Layout::TextNode>(*layout_node)) {
        auto& text = verify_cast<Layout::TextNode>(*layout_node).text_for_rendering();
        builder.append(text.substring(selection.start().index_in_node, text.length() - selection.start().index_in_node));
    }

    // Middle nodes
    layout_node = layout_node->next_in_pre_order();
    while (layout_node && layout_node != selection.end().layout_node) {
        if (is<Layout::TextNode>(*layout_node))
            builder.append(verify_cast<Layout::TextNode>(*layout_node).text_for_rendering());
        else if (is<Layout::BreakNode>(*layout_node) || is<Layout::BlockContainer>(*layout_node))
            builder.append('\n');

        layout_node = layout_node->next_in_pre_order();
    }

    // End node
    VERIFY(layout_node == selection.end().layout_node);
    if (is<Layout::TextNode>(*layout_node)) {
        auto& text = verify_cast<Layout::TextNode>(*layout_node).text_for_rendering();
        builder.append(text.substring(0, selection.end().index_in_node));
    }

    return builder.to_string();
}

void BrowsingContext::select_all()
{
    if (!active_document())
        return;
    auto* layout_root = active_document()->layout_node();
    if (!layout_root)
        return;

    Layout::Node const* first_layout_node = layout_root;

    for (;;) {
        auto* next = first_layout_node->next_in_pre_order();
        if (!next)
            break;
        first_layout_node = next;
        if (is<Layout::TextNode>(*first_layout_node))
            break;
    }

    Layout::Node const* last_layout_node = first_layout_node;

    for (Layout::Node const* layout_node = first_layout_node; layout_node; layout_node = layout_node->next_in_pre_order()) {
        if (is<Layout::TextNode>(*layout_node))
            last_layout_node = layout_node;
    }

    VERIFY(first_layout_node);
    VERIFY(last_layout_node);

    int last_layout_node_index_in_node = 0;
    if (is<Layout::TextNode>(*last_layout_node)) {
        auto const& text_for_rendering = verify_cast<Layout::TextNode>(*last_layout_node).text_for_rendering();
        if (!text_for_rendering.is_empty())
            last_layout_node_index_in_node = text_for_rendering.length() - 1;
    }

    layout_root->set_selection({ { first_layout_node, 0 }, { last_layout_node, last_layout_node_index_in_node } });
}

void BrowsingContext::register_viewport_client(ViewportClient& client)
{
    auto result = m_viewport_clients.set(&client);
    VERIFY(result == AK::HashSetResult::InsertedNewEntry);
}

void BrowsingContext::unregister_viewport_client(ViewportClient& client)
{
    bool was_removed = m_viewport_clients.remove(&client);
    VERIFY(was_removed);
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

DOM::Document* BrowsingContext::container_document()
{
    if (auto* container = this->container())
        return &container->document();
    return nullptr;
}

DOM::Document const* BrowsingContext::container_document() const
{
    if (auto* container = this->container())
        return &container->document();
    return nullptr;
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
        && is<HTML::BrowsingContextContainer>(candidate->focused_element())
        && static_cast<HTML::BrowsingContextContainer&>(*candidate->focused_element()).nested_browsing_context()) {
        candidate = static_cast<HTML::BrowsingContextContainer&>(*candidate->focused_element()).nested_browsing_context()->active_document();
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

BrowsingContext* BrowsingContext::choose_a_browsing_context(StringView name, bool)
{
    // The rules for choosing a browsing context, given a browsing context name
    // name, a browsing context current, and a boolean noopener are as follows:

    // 1. Let chosen be null.
    BrowsingContext* chosen = nullptr;

    // FIXME: 2. Let windowType be "existing or none".

    // FIXME: 3. Let sandboxingFlagSet be current's active document's active
    // sandboxing flag set.

    // 4. If name is the empty string or an ASCII case-insensitive match for "_self", then set chosen to current.
    if (name.is_empty() || name.equals_ignoring_case("_self"sv))
        chosen = this;

    // 5. Otherwise, if name is an ASCII case-insensitive match for "_parent",
    // set chosen to current's parent browsing context, if any, and current
    // otherwise.
    if (name.equals_ignoring_case("_parent"sv)) {
        if (auto* parent = this->parent())
            chosen = parent;
        else
            chosen = this;
    }

    // 6. Otherwise, if name is an ASCII case-insensitive match for "_top", set
    // chosen to current's top-level browsing context, if any, and current
    // otherwise.
    if (name.equals_ignoring_case("_top"sv)) {
        chosen = &top_level_browsing_context();
    }

    // FIXME: 7. Otherwise, if name is not an ASCII case-insensitive match for
    // "_blank", there exists a browsing context whose name is the same as name,
    // current is familiar with that browsing context, and the user agent
    // determines that the two browsing contexts are related enough that it is
    // ok if they reach each other, set chosen to that browsing context. If
    // there are multiple matching browsing contexts, the user agent should set
    // chosen to one in some arbitrary consistent manner, such as the most
    // recently opened, most recently focused, or more closely related.
    if (!name.equals_ignoring_case("_blank"sv)) {
        chosen = this;
    } else {
        // 8. Otherwise, a new browsing context is being requested, and what
        // happens depends on the user agent's configuration and abilities — it
        // is determined by the rules given for the first applicable option from
        // the following list:
        dbgln("FIXME: Create a new browsing context!");

        // --> If current's active window does not have transient activation and
        //     the user agent has been configured to not show popups (i.e., the
        //     user agent has a "popup blocker" enabled)
        //
        //     The user agent may inform the user that a popup has been blocked.

        // --> If sandboxingFlagSet has the sandboxed auxiliary navigation
        //     browsing context flag set
        //
        //     The user agent may report to a developer console that a popup has
        //     been blocked.

        // --> If the user agent has been configured such that in this instance
        //     it will create a new browsing context
        //
        //     1. Set windowType to "new and unrestricted".

        //     2. If current's top-level browsing context's active document's
        //     cross-origin opener policy's value is "same-origin" or
        //     "same-origin-plus-COEP", then:

        //         2.1. Let currentDocument be current's active document.

        //         2.2. If currentDocument's origin is not same origin with
        //         currentDocument's relevant settings object's top-level
        //         origin, then set noopener to true, name to "_blank", and
        //         windowType to "new with no opener".

        //     3. If noopener is true, then set chosen to the result of creating
        //     a new top-level browsing context.

        //     4. Otherwise:

        //         4.1. Set chosen to the result of creating a new auxiliary
        //         browsing context with current.

        //         4.2. If sandboxingFlagSet's sandboxed navigation browsing
        //         context flag is set, then current must be set as chosen's one
        //         permitted sandboxed navigator.

        //     5. If sandboxingFlagSet's sandbox propagates to auxiliary
        //     browsing contexts flag is set, then all the flags that are set in
        //     sandboxingFlagSet must be set in chosen's popup sandboxing flag
        //     set.

        //     6. If name is not an ASCII case-insensitive match for "_blank",
        //     then set chosen's name to name.

        // --> If the user agent has been configured such that in this instance
        //     it will reuse current
        //
        //     Set chosen to current.

        // --> If the user agent has been configured such that in this instance
        //     it will not find a browsing context
        //
        //     Do nothing.
    }

    // 9. Return chosen and windowType.
    return chosen;
}

// https://html.spec.whatwg.org/multipage/browsers.html#document-tree-child-browsing-context
size_t BrowsingContext::document_tree_child_browsing_context_count() const
{
    size_t count = 0;

    // A browsing context child is a document-tree child browsing context of parent if child is a child browsing context and child's container is in a document tree.
    for_each_child([this, &count](BrowsingContext const& child) {
        if (child.is_child_of(*this) && child.container()->in_a_document_tree())
            ++count;
    });

    return count;
}

// https://html.spec.whatwg.org/multipage/browsers.html#child-browsing-context
bool BrowsingContext::is_child_of(BrowsingContext const& parent) const
{
    // A browsing context child is said to be a child browsing context of another browsing context parent,
    // if child's container document is non-null and child's container document's browsing context is parent.
    return container_document() && container_document()->browsing_context() == &parent;
}

// https://html.spec.whatwg.org/multipage/dom.html#still-on-its-initial-about:blank-document
bool BrowsingContext::still_on_its_initial_about_blank_document() const
{
    // A browsing context browsingContext is still on its initial about:blank Document
    // if browsingContext's session history's size is 1
    // and browsingContext's session history[0]'s document's is initial about:blank is true.
    return m_session_history.size() == 1
        && m_session_history[0].document
        && m_session_history[0].document->is_initial_about_blank();
}

DOM::Document const* BrowsingContext::active_document() const
{
    return m_active_document.cell();
}

DOM::Document* BrowsingContext::active_document()
{
    return m_active_document.cell();
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
    NonnullRefPtr<BrowsingContextGroup> group = *this->group();

    // 3. Set browsingContext's group to null.
    set_group(nullptr);

    // 4. Remove browsingContext from group's browsing context set.
    group->browsing_context_set().remove(*this);

    // 5. If group's browsing context set is empty, then remove group from the user agent's browsing context group set.
    // NOTE: This is done by ~BrowsingContextGroup() when the refcount reaches 0.
}

}
