/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/HTMLCollection.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/BrowsingContextContainer.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Layout/BreakNode.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/Page/Page.h>

namespace Web::HTML {

BrowsingContext::BrowsingContext(Page& page, HTML::BrowsingContextContainer* container)
    : m_page(page)
    , m_loader(*this)
    , m_event_handler({}, *this)
    , m_container(container)
{
    m_cursor_blink_timer = Core::Timer::construct(500, [this] {
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
    if (m_active_document == document)
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

    m_active_document = document;

    if (m_active_document) {
        m_active_document->attach_to_browsing_context({}, *this);
        if (m_page && is_top_level())
            m_page->client().page_did_change_title(m_active_document->title());
    }

    if (m_page)
        m_page->client().page_did_set_document_in_top_level_browsing_context(m_active_document);
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
                element = verify_cast<HTML::HTMLAnchorElement>(*candidate);
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
RefPtr<DOM::Node> BrowsingContext::currently_focused_area()
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

}
