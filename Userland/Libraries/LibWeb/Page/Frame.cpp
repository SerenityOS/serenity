/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/InProcessWebView.h>
#include <LibWeb/Layout/BreakNode.h>
#include <LibWeb/Layout/InitialContainingBlockBox.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/Layout/WidgetBox.h>
#include <LibWeb/Page/Frame.h>

namespace Web {

Frame::Frame(DOM::Element& host_element, Frame& main_frame)
    : m_page(*main_frame.page())
    , m_main_frame(main_frame)
    , m_loader(*this)
    , m_event_handler({}, *this)
    , m_host_element(host_element)
{
    setup();
}

Frame::Frame(Page& page)
    : m_page(page)
    , m_main_frame(*this)
    , m_loader(*this)
    , m_event_handler({}, *this)
{
    setup();
}

Frame::~Frame()
{
}

void Frame::setup()
{
    m_cursor_blink_timer = Core::Timer::construct(500, [this] {
        if (!is_focused_frame())
            return;
        if (m_cursor_position.node() && m_cursor_position.node()->layout_node()) {
            m_cursor_blink_state = !m_cursor_blink_state;
            m_cursor_position.node()->layout_node()->set_needs_display();
        }
    });
}

void Frame::did_edit(Badge<EditEventHandler>)
{
    reset_cursor_blink_cycle();
}

void Frame::reset_cursor_blink_cycle()
{
    m_cursor_blink_state = true;
    m_cursor_blink_timer->restart();
}

bool Frame::is_focused_frame() const
{
    return m_page && &m_page->focused_frame() == this;
}

void Frame::set_document(DOM::Document* document)
{
    if (m_document == document)
        return;

    m_cursor_position = {};

    if (m_document)
        m_document->detach_from_frame({}, *this);

    m_document = document;

    if (m_document) {
        m_document->attach_to_frame({}, *this);
        if (m_page)
            m_page->client().page_did_change_title(m_document->title());
    }

    if (m_page)
        m_page->client().page_did_set_document_in_main_frame(m_document);
}

void Frame::set_viewport_rect(const Gfx::IntRect& rect)
{
    bool did_change = false;

    if (m_size != rect.size()) {
        m_size = rect.size();
        if (m_document)
            m_document->update_layout();
        did_change = true;
    }

    if (m_viewport_scroll_offset != rect.location()) {
        m_viewport_scroll_offset = rect.location();
        did_change = true;
    }

    if (did_change) {
        for (auto* client : m_viewport_clients)
            client->frame_did_set_viewport_rect(rect);
    }
}

void Frame::set_size(const Gfx::IntSize& size)
{
    if (m_size == size)
        return;
    m_size = size;
    if (m_document)
        m_document->update_layout();

    for (auto* client : m_viewport_clients)
        client->frame_did_set_viewport_rect(viewport_rect());
}

void Frame::set_viewport_scroll_offset(const Gfx::IntPoint& offset)
{
    if (m_viewport_scroll_offset == offset)
        return;
    m_viewport_scroll_offset = offset;

    for (auto* client : m_viewport_clients)
        client->frame_did_set_viewport_rect(viewport_rect());
}

void Frame::set_needs_display(const Gfx::IntRect& rect)
{
    if (!viewport_rect().intersects(rect))
        return;

    if (is_main_frame()) {
        if (m_page)
            m_page->client().page_did_invalidate(to_main_frame_rect(rect));
        return;
    }

    if (host_element() && host_element()->layout_node())
        host_element()->layout_node()->set_needs_display();
}

void Frame::did_scroll(Badge<InProcessWebView>)
{
    if (!m_document)
        return;
    if (!m_document->layout_node())
        return;
    m_document->layout_node()->for_each_in_subtree_of_type<Layout::WidgetBox>([&](auto& layout_widget) {
        layout_widget.update_widget();
        return IterationDecision::Continue;
    });
}

void Frame::scroll_to_anchor(const String& fragment)
{
    if (!document())
        return;

    auto element = document()->get_element_by_id(fragment);
    if (!element) {
        auto candidates = document()->get_elements_by_name(fragment);
        for (auto& candidate : candidates) {
            if (is<HTML::HTMLAnchorElement>(candidate)) {
                element = downcast<HTML::HTMLAnchorElement>(candidate);
                break;
            }
        }
    }

    // FIXME: This is overly aggressive and should be something more like a "update_layout_if_needed()"
    document()->force_layout();

    if (!element || !element->layout_node())
        return;

    auto& layout_node = *element->layout_node();

    Gfx::FloatRect float_rect { layout_node.box_type_agnostic_position(), { (float)viewport_rect().width(), (float)viewport_rect().height() } };
    if (is<Layout::Box>(layout_node)) {
        auto& layout_box = downcast<Layout::Box>(layout_node);
        auto padding_box = layout_box.box_model().padding_box();
        float_rect.move_by(-padding_box.left, -padding_box.top);
    }

    if (m_page)
        m_page->client().page_did_request_scroll_into_view(enclosing_int_rect(float_rect));
}

Gfx::IntRect Frame::to_main_frame_rect(const Gfx::IntRect& a_rect)
{
    auto rect = a_rect;
    rect.set_location(to_main_frame_position(a_rect.location()));
    return rect;
}

Gfx::IntPoint Frame::to_main_frame_position(const Gfx::IntPoint& a_position)
{
    auto position = a_position;
    for (auto* ancestor = parent(); ancestor; ancestor = ancestor->parent()) {
        if (ancestor->is_main_frame())
            break;
        if (!ancestor->host_element())
            return {};
        if (!ancestor->host_element()->layout_node())
            return {};
        position.move_by(ancestor->host_element()->layout_node()->box_type_agnostic_position().to_type<int>());
    }
    return position;
}

void Frame::set_cursor_position(DOM::Position position)
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

String Frame::selected_text() const
{
    StringBuilder builder;
    if (!m_document)
        return {};
    auto* layout_root = m_document->layout_node();
    if (!layout_root)
        return {};
    if (!layout_root->selection().is_valid())
        return {};

    auto selection = layout_root->selection().normalized();

    if (selection.start().layout_node == selection.end().layout_node) {
        if (!is<Layout::TextNode>(*selection.start().layout_node))
            return "";
        return downcast<Layout::TextNode>(*selection.start().layout_node).text_for_rendering().substring(selection.start().index_in_node, selection.end().index_in_node - selection.start().index_in_node);
    }

    // Start node
    auto layout_node = selection.start().layout_node;
    if (is<Layout::TextNode>(*layout_node)) {
        auto& text = downcast<Layout::TextNode>(*layout_node).text_for_rendering();
        builder.append(text.substring(selection.start().index_in_node, text.length() - selection.start().index_in_node));
    }

    // Middle nodes
    layout_node = layout_node->next_in_pre_order();
    while (layout_node && layout_node != selection.end().layout_node) {
        if (is<Layout::TextNode>(*layout_node))
            builder.append(downcast<Layout::TextNode>(*layout_node).text_for_rendering());
        else if (is<Layout::BreakNode>(*layout_node) || is<Layout::BlockBox>(*layout_node))
            builder.append('\n');

        layout_node = layout_node->next_in_pre_order();
    }

    // End node
    ASSERT(layout_node == selection.end().layout_node);
    if (is<Layout::TextNode>(*layout_node)) {
        auto& text = downcast<Layout::TextNode>(*layout_node).text_for_rendering();
        builder.append(text.substring(0, selection.end().index_in_node));
    }

    return builder.to_string();
}

void Frame::register_viewport_client(ViewportClient& client)
{
    auto result = m_viewport_clients.set(&client);
    ASSERT(result == AK::HashSetResult::InsertedNewEntry);
}

void Frame::unregister_viewport_client(ViewportClient& client)
{
    bool was_removed = m_viewport_clients.remove(&client);
    ASSERT(was_removed);
}

}
