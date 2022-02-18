/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/HTMLCollection.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/BrowsingContextContainer.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/HTMLInputElement.h>
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

BrowsingContext::~BrowsingContext()
{
}

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
    m_cursor_position.node()->layout_node()->set_needs_display();
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
        if (auto* document = active_document())
            document->set_needs_layout();
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

    if (auto* document = active_document())
        document->set_needs_layout();

    for (auto* client : m_viewport_clients)
        client->browsing_context_did_set_viewport_rect(viewport_rect());

    // Schedule the HTML event loop to ensure that a `resize` event gets fired.
    HTML::main_thread_event_loop().schedule();
}

void BrowsingContext::set_viewport_scroll_offset(Gfx::IntPoint const& offset)
{
    if (m_viewport_scroll_offset == offset)
        return;
    m_viewport_scroll_offset = offset;

    for (auto* client : m_viewport_clients)
        client->browsing_context_did_set_viewport_rect(viewport_rect());
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

}
