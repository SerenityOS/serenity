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

// FIXME: This is an outdated older version of "determining the origin" and should be removed.
// https://html.spec.whatwg.org/multipage/browsers.html#determining-the-origin
HTML::Origin determine_the_origin(BrowsingContext const& browsing_context, Optional<AK::URL> url, SandboxingFlagSet sandbox_flags, Optional<HTML::Origin> invocation_origin)
{
    // 1. If sandboxFlags has its sandboxed origin browsing context flag set, then return a new opaque origin.
    if (has_flag(sandbox_flags, SandboxingFlagSet::SandboxedOrigin)) {
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
    return URL::url_origin(*url);
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#determining-the-origin
HTML::Origin determine_the_origin(AK::URL const& url, SandboxingFlagSet sandbox_flags, Optional<HTML::Origin> source_origin, Optional<HTML::Origin> container_origin)
{
    // 1. If sandboxFlags has its sandboxed origin browsing context flag set, then return a new opaque origin.
    if (has_flag(sandbox_flags, SandboxingFlagSet::SandboxedOrigin)) {
        return HTML::Origin {};
    }

    // FIXME: 2. If url is null, then return a new opaque origin.
    // FIXME: There appears to be no way to get a null URL here, so it might be a spec bug.

    // 3. If url is about:srcdoc, then:
    if (url == "about:srcdoc"sv) {
        // 1. Assert: containerOrigin is non-null.
        VERIFY(container_origin.has_value());

        // 2. Return containerOrigin.
        return container_origin.release_value();
    }

    // 4. If url matches about:blank and sourceOrigin is non-null, then return sourceOrigin.
    if (url_matches_about_blank(url) && source_origin.has_value())
        return source_origin.release_value();

    // 5. Return url's origin.
    return URL::url_origin(url);
}

// https://html.spec.whatwg.org/multipage/browsers.html#creating-a-new-top-level-browsing-context
JS::NonnullGCPtr<BrowsingContext> BrowsingContext::create_a_new_top_level_browsing_context(Web::Page& page)
{
    // 1. Let group be the result of creating a new browsing context group.
    auto group = BrowsingContextGroup::create_a_new_browsing_context_group(page);

    // 2. Return group's browsing context set[0].
    return *group->browsing_context_set().begin();
}

// https://html.spec.whatwg.org/multipage/browsers.html#creating-a-new-browsing-context
JS::NonnullGCPtr<BrowsingContext> BrowsingContext::create_a_new_browsing_context(Page& page, JS::GCPtr<DOM::Document> creator, JS::GCPtr<DOM::Element> embedder, BrowsingContextGroup&)
{
    // 1. Let browsingContext be a new browsing context.
    NavigableContainer* container = (embedder && is<NavigableContainer>(*embedder)) ? static_cast<NavigableContainer*>(embedder.ptr()) : nullptr;
    auto browsing_context = Bindings::main_thread_vm().heap().allocate_without_realm<BrowsingContext>(page, container);

    // 2. Let unsafeContextCreationTime be the unsafe shared current time.
    [[maybe_unused]] auto unsafe_context_creation_time = HighResolutionTime::unsafe_shared_current_time();

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
    SandboxingFlagSet sandbox_flags = {};

    // 5. Let origin be the result of determining the origin given browsingContext, about:blank, sandboxFlags, and browsingContext's creator origin.
    auto origin = determine_the_origin(*browsing_context, AK::URL("about:blank"), sandbox_flags, browsing_context->m_creator_origin);

    // FIXME: 6. Let permissionsPolicy be the result of creating a permissions policy given browsingContext and origin. [PERMISSIONSPOLICY]

    // FIXME: 7. Let agent be the result of obtaining a similar-origin window agent given origin, group, and false.

    JS::GCPtr<Window> window;

    // 8. Let realm execution context be the result of creating a new JavaScript realm given agent and the following customizations:
    auto realm_execution_context = Bindings::create_a_new_javascript_realm(
        Bindings::main_thread_vm(),
        [&](JS::Realm& realm) -> JS::Object* {
            browsing_context->m_window_proxy = realm.heap().allocate<WindowProxy>(realm, realm);

            // - For the global object, create a new Window object.
            window = HTML::Window::create(realm);
            return window.ptr();
        },
        [&](JS::Realm&) -> JS::Object* {
            // - For the global this binding, use browsingContext's WindowProxy object.
            return browsing_context->m_window_proxy;
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

    // 12. Let loadTimingInfo be a new document load timing info with its navigation start time set to the result of calling
    //     coarsen time with unsafeContextCreationTime and the new environment settings object's cross-origin isolated capability.
    auto load_timing_info = DOM::DocumentLoadTimingInfo();
    load_timing_info.navigation_start_time = HighResolutionTime::coarsen_time(
        unsafe_context_creation_time,
        verify_cast<WindowEnvironmentSettingsObject>(Bindings::host_defined_environment_settings_object(window->realm())).cross_origin_isolated_capability() == CanUseCrossOriginIsolatedAPIs::Yes);

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
    //     load timing info is loadTimingInfo,
    //     FIXME: navigation id is null,
    //     and which is ready for post-load tasks.
    auto document = HTML::HTMLDocument::create(window->realm());

    // Non-standard
    document->set_window(*window);
    window->set_associated_document(*document);

    document->set_quirks_mode(DOM::QuirksMode::Yes);
    document->set_content_type("text/html"_string);
    document->set_origin(origin);
    document->set_url(AK::URL("about:blank"));
    document->set_cross_origin_opener_policy(coop);
    document->set_load_timing_info(load_timing_info);
    document->set_ready_for_post_load_tasks(true);

    // FIXME: 16. Assert: document's URL and document's relevant settings object's creation URL are about:blank.

    // 17. Set document's is initial about:blank to true.
    document->set_is_initial_about_blank(true);

    // 18. Ensure that document has a single child html node, which itself has two empty child nodes: a head element, and a body element.
    auto html_node = DOM::create_element(document, HTML::TagNames::html, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(html_node->append_child(DOM::create_element(document, HTML::TagNames::head, Namespace::HTML).release_value_but_fixme_should_propagate_errors()));
    MUST(html_node->append_child(DOM::create_element(document, HTML::TagNames::body, Namespace::HTML).release_value_but_fixme_should_propagate_errors()));
    MUST(document->append_child(html_node));

    // 19. Set the active document of browsingContext to document.
    browsing_context->set_active_document(*document);

    // 20. If browsingContext's creator URL is non-null, then set document's referrer to the serialization of it.
    if (browsing_context->m_creator_url.has_value()) {
        document->set_referrer(browsing_context->m_creator_url->serialize());
    }

    // FIXME: 21. If creator is non-null, then set document's policy container to a clone of creator's policy container.

    // 22. Append a new session history entry to browsingContext's session history whose URL is about:blank and document is document.
    auto new_entry = browsing_context->heap().allocate_without_realm<SessionHistoryEntry>();
    new_entry->url = AK::URL("about:blank");
    new_entry->document = document.ptr();
    new_entry->policy_container = {};
    new_entry->scroll_restoration_mode = {};
    new_entry->browsing_context_name = {};
    new_entry->original_source_browsing_context = {};
    browsing_context->m_session_history.append(*new_entry);

    // 23. Completely finish loading document.
    document->completely_finish_loading();

    // 24. Return browsingContext.
    return *browsing_context;
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

    // 6. Let origin be the result of determining the origin given about:blank, sandboxFlags, creatorOrigin, and null.
    auto origin = determine_the_origin(AK::URL("about:blank"sv), sandbox_flags, creator_origin, {});

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
    document->set_window(*window);
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
            VERIFY(creator->browsing_context()->top_level_browsing_context().active_document());
            document->set_cross_origin_opener_policy(creator->browsing_context()->top_level_browsing_context().active_document()->cross_origin_opener_policy());
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

// https://html.spec.whatwg.org/multipage/browsers.html#set-the-active-document
void BrowsingContext::set_active_document(JS::NonnullGCPtr<DOM::Document> document)
{
    auto previously_active_document = active_document();

    // 1. Let window be document's relevant global object.
    auto& window = verify_cast<HTML::Window>(relevant_global_object(document));

    // 2. Set document's visibility state to browsingContext's top-level browsing context's system visibility state.
    document->set_visibility_state({}, top_level_browsing_context().system_visibility_state());

    // 3. Set browsingContext's active window to window.
    m_window_proxy->set_window(window);

    // 4. Set window's associated Document to document.
    window.set_associated_document(document);

    // 5. Set window's relevant settings object's execution ready flag.
    relevant_settings_object(window).execution_ready = true;

    // AD-HOC:
    document->set_browsing_context(this);

    if (m_page && m_page->top_level_traversable_is_initialized() && this == &m_page->top_level_browsing_context())
        m_page->client().page_did_change_title(document->title());

    if (previously_active_document && previously_active_document != document.ptr())
        previously_active_document->did_stop_being_active_document_in_browsing_context({});
}

void BrowsingContext::set_viewport_rect(CSSPixelRect const& rect)
{
    bool did_change = false;

    if (m_size != rect.size()) {
        m_size = rect.size();
        if (auto* document = active_document()) {
            // NOTE: Resizing the viewport changes the reference value for viewport-relative CSS lengths.
            document->invalidate_style();
            document->set_needs_layout();
        }
        did_change = true;
    }

    if (m_viewport_scroll_offset != rect.location()) {
        m_viewport_scroll_offset = rect.location();
        scroll_offset_did_change();
        did_change = true;
    }

    if (did_change && active_document()) {
        active_document()->inform_all_viewport_clients_about_the_current_viewport_rect();
    }

    // Schedule the HTML event loop to ensure that a `resize` event gets fired.
    HTML::main_thread_event_loop().schedule();
}

void BrowsingContext::set_size(CSSPixelSize size)
{
    if (m_size == size)
        return;
    m_size = size;

    if (auto* document = active_document()) {
        document->invalidate_style();
        document->set_needs_layout();
    }

    if (auto* document = active_document()) {
        document->inform_all_viewport_clients_about_the_current_viewport_rect();
    }

    // Schedule the HTML event loop to ensure that a `resize` event gets fired.
    HTML::main_thread_event_loop().schedule();
}

void BrowsingContext::set_needs_display()
{
    set_needs_display(viewport_rect());
}

void BrowsingContext::set_needs_display(CSSPixelRect const& rect)
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

    CSSPixelRect target_rect { layout_node.box_type_agnostic_position(), { viewport_rect().width(), viewport_rect().height() } };
    if (is<Layout::Box>(layout_node)) {
        auto& layout_box = verify_cast<Layout::Box>(layout_node);
        auto padding_box = layout_box.box_model().padding_box();
        target_rect.translate_by(-padding_box.left, -padding_box.top);
    }

    if (m_page && this == &m_page->top_level_browsing_context())
        m_page->client().page_did_request_scroll_into_view(target_rect);
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
    top_level_browsing_context().for_each_in_subtree([&](auto& context) {
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
        chosen = &top_level_browsing_context();
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
            if (top_level_browsing_context().active_document()->cross_origin_opener_policy().value == CrossOriginOpenerPolicyValue::SameOrigin || top_level_browsing_context().active_document()->cross_origin_opener_policy().value == CrossOriginOpenerPolicyValue::SameOriginPlusCOEP) {
                // 1. Let currentDocument be current's active document.
                auto* current_document = top_level_browsing_context().active_document();

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
                chosen = HTML::BrowsingContext::create_a_new_top_level_browsing_context(*m_page);

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
        && m_session_history[0]->document
        && m_session_history[0]->document->is_initial_about_blank();
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

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#navigate
WebIDL::ExceptionOr<void> BrowsingContext::navigate(
    JS::NonnullGCPtr<Fetch::Infrastructure::Request> resource,
    BrowsingContext& source_browsing_context,
    bool exceptions_enabled,
    HistoryHandlingBehavior history_handling,
    Optional<PolicyContainer> history_policy_container,
    DeprecatedString navigation_type,
    Optional<String> navigation_id,
    Function<void(JS::NonnullGCPtr<Fetch::Infrastructure::Response>)> process_response_end_of_body)
{
    // 1. If resource is a URL, then set resource to a new request whose URL is resource.
    // NOTE: This function only accepts resources that are already a request, so this is irrelevant.

    // 2. If resource is a request and historyHandling is "reload", then set resource's reload-navigation flag.
    if (history_handling == HistoryHandlingBehavior::Reload)
        resource->set_reload_navigation(true);

    // 3. If the source browsing context is not allowed to navigate browsingContext, then:
    if (!source_browsing_context.is_allowed_to_navigate(*this)) {
        // 1. If exceptionsEnabled is given and is true, then throw a "SecurityError" DOMException.
        if (exceptions_enabled) {
            VERIFY(source_browsing_context.active_document());
            return WebIDL::SecurityError::create(source_browsing_context.active_document()->realm(), "Source browsing context not allowed to navigate"_fly_string);
        }

        // FIXME: 2. Otherwise, the user agent may instead offer to open resource in a new top-level browsing context
        //           or in the top-level browsing context of the source browsing context, at the user's option,
        //           in which case the user agent must navigate that designated top-level browsing context
        //           to resource as if the user had requested it independently.
    }

    // 4. If navigationId is null:
    if (!navigation_id.has_value()) {
        // 1. If historyHandling is "reload", and browsingContext's active document's navigation id is not null,
        if (history_handling == HistoryHandlingBehavior::Reload && active_document()->navigation_id().has_value()) {
            // let navigationId be browsingContext's active document's navigation id.
            navigation_id = active_document()->navigation_id();
        } else {
            // Otherwise let navigation id be the result of generating a random UUID. [UUID]
            // FIXME: Generate a UUID.
            navigation_id = "FIXME"_string;
        }
    }

    // FIXME: 5. If browsingContext's active document's unload counter is greater than 0,
    //           then invoke WebDriver BiDi navigation failed
    //           with a WebDriver BiDi navigation status whose id is navigationId, status is "canceled", and url is resource's url
    //           and return.

    // 6. If historyHandling is "default", and any of the following are true:
    //    - browsingContext is still on its initial about:blank Document
    //    - resource is a request whose URL equals browsingContext's active document's URL
    //    - resource is a request whose URL's scheme is "javascript"
    if (history_handling == HistoryHandlingBehavior::Default
        && (still_on_its_initial_about_blank_document()
            || resource->url().equals(active_document()->url())
            || resource->url().scheme() == "javascript"sv)) {
        // then set historyHandling to "replace".
        history_handling = HistoryHandlingBehavior::Replace;
    }

    // 7. If historyHandling is not "reload", resource is a request,
    //    resource's URL equals browsingContext's active document's URL with exclude fragments set to true,
    //    and resource's URL's fragment is non-null, then:
    if (history_handling != HistoryHandlingBehavior::Reload
        && resource->url().equals(active_document()->url(), AK::URL::ExcludeFragment::Yes)
        && resource->url().fragment().has_value()) {
        // 1. Navigate to a fragment given browsingContext, resource's URL, historyHandling, and navigationId.
        TRY(navigate_to_a_fragment(resource->url(), history_handling, *navigation_id));

        // 2. Return.
        return {};
    }

    // FIXME: 8. Let incumbentNavigationOrigin be the origin of the incumbent settings object,
    //           or if no script was involved, the origin of the node document of the element that initiated the navigation.

    // FIXME: 9. Let initiatorPolicyContainer be a clone of the source browsing context's active document's policy container.

    // FIXME: 10. If resource is a request, then set resource's policy container to initiatorPolicyContainer.

    // FIXME: 11. Cancel any preexisting but not yet mature attempt to navigate browsingContext,
    //            including canceling any instances of the fetch algorithm started by those attempts.
    //            If one of those attempts has already created and initialized a new Document object,
    //            abort that Document also.
    //            (Navigation attempts that have matured already have session history entries,
    //            and are therefore handled during the update the session history with the new page algorithm, later.)

    // FIXME: 12. Let unloadPromptResult be the result of calling prompt to unload with the active document of browsingContext.
    //            If this instance of the navigation algorithm gets canceled while this step is running,
    //            the prompt to unload algorithm must nonetheless be run to completion.

    // FIXME: 13. If unloadPromptResult is "refuse", then return a new WebDriver BiDi navigation status whose id is navigationId and status is "canceled".

    // 14. Abort the active document of browsingContext.
    active_document()->abort();

    // FIXME: 15. If browsingContext is a child browsing context, then put it in the delaying load events mode.
    //            The user agent must take this child browsing context out of the delaying load events mode when this navigation algorithm later matures,
    //            or when it terminates (whether due to having run all the steps, or being canceled, or being aborted),
    //            whichever happens first.

    // FIXME: 16. Let sandboxFlags be the result of determining the creation sandboxing flags given browsingContext and browsingContext's container.

    // FIXME: 17. Let allowedToDownload be the result of running the allowed to download algorithm given the source browsing context and browsingContext.

    // 18. Let hasTransientActivation be true if the source browsing context's active window has transient activation; otherwise false.
    [[maybe_unused]] bool has_transient_activation = source_browsing_context.active_window()->has_transient_activation();

    // FIXME: 19. Invoke WebDriver BiDi navigation started with browsingContext, and a new WebDriver BiDi navigation status whose id is navigationId, url is resource's url, and status is "pending".

    // 20. Return, and continue running these steps in parallel.

    // FIXME: Implement the rest of this algorithm
    (void)history_policy_container;
    (void)navigation_type;
    (void)process_response_end_of_body;

    // AD-HOC:
    auto request = LoadRequest::create_for_url_on_page(resource->url(), page());
    request.set_method(DeprecatedString { resource->method() });
    for (auto& header : *resource->header_list()) {
        request.set_header(DeprecatedString { header.name.bytes() }, DeprecatedString { header.value.bytes() });
    }
    if (request.method() == "POST"sv) {
        if (resource->body().has<ByteBuffer>()) {
            auto const& byte_buffer = resource->body().get<ByteBuffer>();
            request.set_body(byte_buffer);
            request.set_header("Content-Length", DeprecatedString::number(byte_buffer.size()));
        } else {
            request.set_header("Content-Length", DeprecatedString::number(0));
        }
    }
    loader().load(request, FrameLoader::Type::Navigation);
    return {};
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#navigate-fragid
WebIDL::ExceptionOr<void> BrowsingContext::navigate_to_a_fragment(AK::URL const& url, HistoryHandlingBehavior history_handling, String navigation_id)
{
    // 1. If historyHandling is not "replace",
    if (history_handling != HistoryHandlingBehavior::Replace) {
        // FIXME: then remove all the entries in browsingContext's session history after the current entry.
        //        (If the current entry is the last entry in the session history, then no entries are removed.)
    }

    // 2. Remove any tasks queued by the history traversal task source that are associated with any Document objects
    //    in browsingContext's top-level browsing context's document family.
    HTML::main_thread_event_loop().task_queue().remove_tasks_matching([&](HTML::Task const& task) {
        return task.source() == Task::Source::HistoryTraversal
            && task.document()
            && top_level_browsing_context().document_family_contains(*task.document());
    });

    // 3. Append a new session history entry to the session history whose URL is url,
    //    document is the current entry's document,
    //    policy container is the current entry's policy-container
    //    and scroll restoration mode is the current entry's scroll restoration mode.
    auto new_entry = heap().allocate_without_realm<SessionHistoryEntry>();
    new_entry->url = url;
    new_entry->document = current_entry().document;
    new_entry->policy_container = current_entry().policy_container;
    new_entry->scroll_restoration_mode = current_entry().scroll_restoration_mode;
    new_entry->browsing_context_name = {};
    new_entry->original_source_browsing_context = {};
    m_session_history.append(*new_entry);

    // 4. Traverse the history to the new entry, with historyHandling set to historyHandling.
    //    This will scroll to the fragment given in what is now the document's URL.
    TRY(traverse_the_history(m_session_history.size() - 1, history_handling));

    // FIXME: 5. Invoke WebDriver BiDi fragment navigated with browsingContext,
    //           and a new WebDriver BiDi navigation status whose id is navigationId, url is resource's url, and status is "complete".
    (void)navigation_id;

    return {};
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#traverse-the-history
WebIDL::ExceptionOr<void> BrowsingContext::traverse_the_history(size_t entry_index, HistoryHandlingBehavior history_handling, bool explicit_history_navigation)
{
    auto entry = m_session_history[entry_index];

    // 1. If entry's document is null, then:
    if (!entry->document) {
        // 1. Assert: historyHandling is "default".
        VERIFY(history_handling == HistoryHandlingBehavior::Default);

        // 2. Let request be a new request whose URL is entry's URL.
        auto& vm = Bindings::main_thread_vm();
        auto request = Fetch::Infrastructure::Request::create(vm);
        request->set_url(entry->url);

        // 3. If explicitHistoryNavigation is true, then set request's history-navigation flag.
        if (explicit_history_navigation)
            request->set_history_navigation(true);

        // 4. Navigate the browsing context to request with historyHandling set to "entry update"
        //    and with historyPolicyContainer set to entry's policy container.
        //    The navigation must be done using the same source browsing context as was used the first time entry was created.
        VERIFY(entry->original_source_browsing_context);
        TRY(navigate(request, *entry->original_source_browsing_context, false, HistoryHandlingBehavior::EntryUpdate, entry->policy_container));

        // 5. Return.
        return {};
    }

    // FIXME: 2. Save persisted state to the current entry.

    // 3. Let newDocument be entry's document.
    JS::GCPtr<DOM::Document> new_document = entry->document.ptr();

    // 4. Assert: newDocument's is initial about:blank is false,
    //   i.e., we never traverse back to the initial about:blank Document because it always gets replaced when we navigate away from it.
    VERIFY(!new_document->is_initial_about_blank());

    // 5. If newDocument is different than the current entry's document, or historyHandling is "entry update" or "reload", then:
    if (new_document.ptr() != current_entry().document.ptr()
        || history_handling == HistoryHandlingBehavior::EntryUpdate) {
        // FIXME: 1. If newDocument's suspended timer handles is not empty:
        // FIXME:    1. Assert: newDocument's suspension time is not zero.
        // FIXME:    2. Let suspendDuration be the current high resolution time minus newDocument's suspension time.
        // FIXME:    3. Let activeTimers be newDocument's relevant global object's map of active timers.
        // FIXME:    4. For each handle in newDocument's suspended timer handles, if activeTimers[handle] exists, then increase activeTimers[handle] by suspendDuration.
    }

    // 2. Remove any tasks queued by the history traversal task source
    //    that are associated with any Document objects in the top-level browsing context's document family.
    HTML::main_thread_event_loop().task_queue().remove_tasks_matching([&](HTML::Task const& task) {
        return task.source() == Task::Source::HistoryTraversal
            && task.document()
            && top_level_browsing_context().document_family_contains(*task.document());
    });

    // 3. If newDocument's origin is not same origin with the current entry's document's origin, then:
    if (!new_document->origin().is_same_origin(current_entry().document->origin())) {
        // FIXME: 1. Let entriesToUpdate be all entries in the session history whose document's origin is same origin as the active document
        //           and that are contiguous with the current entry.
        // FIXME: 2. For each entryToUpdate of entriesToUpdate, set entryToUpdate's browsing context name to the current browsing context name.
        // FIXME: 3. If the browsing context is a top-level browsing context, but not an auxiliary browsing context whose disowned is false, then set the browsing context's name to the empty string.
    }

    // 4. Set the active document of the browsing context to newDocument.
    set_active_document(*new_document);

    // 5. If entry's browsing context name is not null, then:
    if (entry->browsing_context_name.has_value()) {
        // 1. Set the browsing context's name to entry's browsing context name.
        m_name = *entry->browsing_context_name;

        // FIXME: 2. Let entriesToUpdate be all entries in the session history whose document's origin is same origin as the new active document's origin and that are contiguous with entry.
        // FIXME: 3. For each entryToUpdate of entriesToUpdate, set entryToUpdate's browsing context name to null.
    }

    // FIXME: 6. If newDocument has any form controls whose autofill field name is "off", invoke the reset algorithm of each of those elements.

    // 7. If newDocument's current document readiness "complete",
    if (new_document->readiness() == HTML::DocumentReadyState::Complete) {
        // then queue a global task on the DOM manipulation task source given newDocument's relevant global object to run the following steps:

        queue_global_task(Task::Source::DOMManipulation, relevant_global_object(*new_document), [new_document] {
            // 1. If newDocument's page showing flag is true, then abort these steps.
            if (new_document->page_showing())
                return;

            // 2. Set newDocument's page showing flag to true.
            new_document->set_page_showing(true);

            // 3. Update the visibility state of newDocument to "hidden".
            new_document->update_the_visibility_state(VisibilityState::Hidden);

            // 4. Fire a page transition event named pageshow at newDocument's relevant global object with true.
            auto& window = verify_cast<HTML::Window>(relevant_global_object(*new_document));
            window.fire_a_page_transition_event(HTML::EventNames::pageshow, true);
        });
    }

    // 6. Set newDocument's URL to entry's URL.
    new_document->set_url(entry->url);

    // 7. Let hashChanged be false, and let oldURL and newURL be null.
    bool hash_changed = false;
    Optional<AK::URL> old_url;
    Optional<AK::URL> new_url;

    // 8. If entry's URL's fragment is not identical to the current entry's URL's fragment,
    //    and entry's document equals the current entry's document,
    if (entry->url.fragment() != current_entry().url.fragment()
        && entry->document.ptr() == current_entry().document.ptr()) {
        // then set hashChanged to true, set oldURL to the current entry's URL, and set newURL to entry's URL.
        hash_changed = true;
        old_url = current_entry().url;
        new_url = entry->url;
    }

    // 9. If historyHandling is "replace", then remove the entry immediately before entry in the session history.
    if (history_handling == HistoryHandlingBehavior::Replace) {
        // FIXME: This is gnarly.
        m_session_history.remove(entry_index - 1);
        entry_index--;
        entry = m_session_history[entry_index];
    }

    // 10. If entry's persisted user state is null, and its URL's fragment is non-null, then scroll to the fragment.
    if (entry->url.fragment().has_value())
        active_document()->scroll_to_the_fragment();

    // 11. Set the current entry to entry.
    m_session_history_index = entry_index;

    // 12. Let targetRealm be the current Realm Record.
    auto* target_realm = Bindings::main_thread_vm().current_realm();
    VERIFY(target_realm);

    // FIXME: 13. Let state be null.
    // FIXME: 14. If entry's serialized state is not null, then set state to StructuredDeserialize(entry's serialized state, targetRealm).
    //            If this throws an exception, catch it and ignore the exception.
    // FIXME: 15. Set newDocument's History object's state to state.
    // FIXME: 16. Let stateChanged be true if newDocument has a latest entry, and that entry is not entry; otherwise let it be false.
    // FIXME: 17. Set newDocument's latest entry to entry.
    // FIXME: 18. If stateChanged is true, then fire an event named popstate at newDocument's relevant global object, using PopStateEvent, with the state attribute initialized to state.
    // FIXME: 19. Restore persisted state from entry.

    // 20. If hashChanged is true,
    if (hash_changed) {
        // then queue a global task on the DOM manipulation task source given newDocument's relevant global object
        queue_global_task(Task::Source::DOMManipulation, relevant_global_object(*new_document), [new_document] {
            // to fire an event named hashchange at newDocument's relevant global object,
            // using HashChangeEvent, with the oldURL attribute initialized to oldURL
            // and the newURL attribute initialized to newURL.

            // FIXME: Implement a proper HashChangeEvent class.
            auto event = DOM::Event::create(verify_cast<HTML::Window>(relevant_global_object(*new_document)).realm(), HTML::EventNames::hashchange);
            new_document->dispatch_event(event);
        });
    }

    return {};
}

// https://html.spec.whatwg.org/multipage/browsers.html#allowed-to-navigate
bool BrowsingContext::is_allowed_to_navigate(BrowsingContext const& other) const
{
    VERIFY(active_window());
    VERIFY(active_document());

    // 1. If A is not the same browsing context as B,
    //    and A is not one of the ancestor browsing contexts of B,
    //    and B is not a top-level browsing context,
    //    FIXME: and A's active document's active sandboxing flag set has its sandboxed navigation browsing context flag set,
    //    then return false.
    if (this != &other
        && !this->is_ancestor_of(other)
        && !other.is_top_level()) {
        return false;
    }

    // 2. Otherwise, if B is a top-level browsing context, and is one of the ancestor browsing contexts of A, then:
    if (other.is_top_level() && other.is_ancestor_of(*this)) {
        // 1. If A's active window has transient activation
        //    and A's active document's active sandboxing flag set has its sandboxed top-level navigation with user activation browsing context flag set,
        //    then return false.
        if (active_window()->has_transient_activation()
            && has_flag(active_document()->active_sandboxing_flag_set(), SandboxingFlagSet::SandboxedTopLevelNavigationWithUserActivation)) {
            return false;
        }

        // 2. Otherwise, if A's active window does not have transient activation
        //    and A's active document's active sandboxing flag set has its sandboxed top-level navigation without user activation browsing context flag set,
        //    then return false.
        if (!active_window()->has_transient_activation()
            && has_flag(active_document()->active_sandboxing_flag_set(), SandboxingFlagSet::SandboxedTopLevelNavigationWithoutUserActivation)) {
            return false;
        }
    }

    // 3. Otherwise, if B is a top-level browsing context,
    //    and is neither A nor one of the ancestor browsing contexts of A,
    //    and A's Document's active sandboxing flag set has its sandboxed navigation browsing context flag set,
    //    and A is not the one permitted sandboxed navigator of B,
    //    then return false.
    if (other.is_top_level()
        && &other != this
        && !other.is_ancestor_of(*this)
        && has_flag(active_document()->active_sandboxing_flag_set(), SandboxingFlagSet::SandboxedNavigation)
        && this != other.the_one_permitted_sandboxed_navigator()) {
        return false;
    }

    // 4. Return true.
    return true;
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
        if (!entry->document)
            continue;
        if (documents.set(const_cast<DOM::Document*>(entry->document.ptr())) == AK::HashSetResult::ReplacedExistingEntry)
            continue;
        for (auto& context : entry->document->list_of_descendant_browsing_contexts()) {
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
    auto document_family = top_level_browsing_context().document_family();

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

// https://html.spec.whatwg.org/multipage/window-object.html#a-browsing-context-is-discarded
void BrowsingContext::discard()
{
    m_has_been_discarded = true;

    // 1. Discard all Document objects for all the entries in browsingContext's session history.
    for (auto& entry : m_session_history) {
        if (entry->document)
            entry->document->discard();
    }

    // AD-HOC:
    // FIXME: This should be in the session history!
    if (auto* document = active_document())
        document->discard();

    // 2. If browsingContext is a top-level browsing context, then remove browsingContext.
    if (is_top_level())
        remove();

    // AD-HOC:
    if (parent())
        parent()->remove_child(*this);
}

// https://html.spec.whatwg.org/multipage/window-object.html#close-a-browsing-context
void BrowsingContext::close()
{
    VERIFY(active_document());

    // FIXME: 1. If the result of calling prompt to unload with browsingContext's active document is "refuse", then return.

    // 2. Unload browsingContext's active document.
    active_document()->unload();

    // 3. Remove browsingContext from the user interface (e.g., close or hide its tab in a tabbed browser).
    if (m_page)
        m_page->client().page_did_close_browsing_context(*this);

    // 4. Discard browsingContext.
    discard();
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
