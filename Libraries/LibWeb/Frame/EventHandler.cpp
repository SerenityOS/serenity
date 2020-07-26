/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <LibGUI/Event.h>
#include <LibGUI/Window.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/HTMLIFrameElement.h>
#include <LibWeb/DOM/MouseEvent.h>
#include <LibWeb/Frame/EventHandler.h>
#include <LibWeb/Frame/Frame.h>
#include <LibWeb/Layout/LayoutDocument.h>
#include <LibWeb/PageView.h>

namespace Web {

static Gfx::IntPoint compute_mouse_event_offset(const Gfx::IntPoint& position, const LayoutNode& layout_node)
{
    auto top_left_of_layout_node = layout_node.box_type_agnostic_position();
    return {
        position.x() - static_cast<int>(top_left_of_layout_node.x()),
        position.y() - static_cast<int>(top_left_of_layout_node.y())
    };
}

EventHandler::EventHandler(Badge<Frame>, Frame& frame)
    : m_frame(frame)
{
}

EventHandler::~EventHandler()
{
}

const LayoutDocument* EventHandler::layout_root() const
{
    if (!m_frame.document())
        return nullptr;
    return m_frame.document()->layout_node();
}

LayoutDocument* EventHandler::layout_root()
{
    if (!m_frame.document())
        return nullptr;
    return m_frame.document()->layout_node();
}

bool EventHandler::handle_mouseup(const Gfx::IntPoint& position, unsigned button, unsigned modifiers)
{
    if (!layout_root())
        return false;
    bool handled_event = false;

    auto result = layout_root()->hit_test(position);
    if (result.layout_node && result.layout_node->node()) {
        RefPtr<Node> node = result.layout_node->node();
        if (is<HTMLIFrameElement>(*node)) {
            if (auto* subframe = downcast<HTMLIFrameElement>(*node).hosted_frame())
                return subframe->event_handler().handle_mouseup(position.translated(compute_mouse_event_offset({}, *result.layout_node)), button, modifiers);
            return false;
        }
        auto offset = compute_mouse_event_offset(position, *result.layout_node);
        node->dispatch_event(MouseEvent::create("mouseup", offset.x(), offset.y()));
        handled_event = true;
    }

    if (button == GUI::MouseButton::Left) {
        dump_selection("MouseUp");
        m_in_mouse_selection = false;
    }
    return handled_event;
}

bool EventHandler::handle_mousedown(const Gfx::IntPoint& position, unsigned button, unsigned modifiers)
{
    if (!layout_root())
        return false;
    NonnullRefPtr document = *m_frame.document();
    auto& page_client = m_frame.page().client();

    auto result = layout_root()->hit_test(position);
    if (!result.layout_node)
        return false;

    RefPtr<Node> node = result.layout_node->node();
    document->set_hovered_node(node);
    if (!node)
        return false;

    if (is<HTMLIFrameElement>(*node)) {
        if (auto* subframe = downcast<HTMLIFrameElement>(*node).hosted_frame())
            return subframe->event_handler().handle_mousedown(position.translated(compute_mouse_event_offset({}, *result.layout_node)), button, modifiers);
        return false;
    }

    auto offset = compute_mouse_event_offset(position, *result.layout_node);
    node->dispatch_event(MouseEvent::create("mousedown", offset.x(), offset.y()));
    if (!layout_root())
        return true;

    if (RefPtr<HTMLAnchorElement> link = node->enclosing_link_element()) {
        auto href = link->href();
        auto url = document->complete_url(href);
        dbg() << "Web::EventHandler: Clicking on a link to " << url;
        if (button == GUI::MouseButton::Left) {
            auto href = link->href();
            auto url = document->complete_url(href);
            if (href.starts_with("javascript:")) {
                document->run_javascript(href.substring_view(11, href.length() - 11));
            } else if (href.starts_with('#')) {
                auto anchor = href.substring_view(1, href.length() - 1);
                m_frame.scroll_to_anchor(anchor);
            } else {
                if (m_frame.is_main_frame()) {
                    page_client.page_did_click_link(url, link->target(), modifiers);
                } else {
                    // FIXME: Handle different targets!
                    m_frame.loader().load(url, FrameLoader::Type::Navigation);
                }
            }
        } else if (button == GUI::MouseButton::Right) {
            page_client.page_did_request_link_context_menu(m_frame.to_main_frame_position(position), url, link->target(), modifiers);
        } else if (button == GUI::MouseButton::Middle) {
            page_client.page_did_middle_click_link(url, link->target(), modifiers);
        }
    } else {
        if (button == GUI::MouseButton::Left) {
            layout_root()->selection().set({ result.layout_node, result.index_in_node }, {});
            dump_selection("MouseDown");
            m_in_mouse_selection = true;
        }
        else if (button == GUI::MouseButton::Right) {
            page_client.page_did_request_context_menu(m_frame.to_main_frame_position(position));
        }
    }
    return true;
}

bool EventHandler::handle_mousemove(const Gfx::IntPoint& position, unsigned buttons, unsigned modifiers)
{
    if (!layout_root())
        return false;
    auto& document = *m_frame.document();
    auto& page_client = m_frame.page().client();

    bool hovered_node_changed = false;
    bool is_hovering_link = false;
    auto result = layout_root()->hit_test(position);
    const HTMLAnchorElement* hovered_link_element = nullptr;
    if (result.layout_node) {
        RefPtr<Node> node = result.layout_node->node();

        if (node && is<HTMLIFrameElement>(*node)) {
            if (auto* subframe = downcast<HTMLIFrameElement>(*node).hosted_frame())
                return subframe->event_handler().handle_mousemove(position.translated(compute_mouse_event_offset({}, *result.layout_node)), buttons, modifiers);
            return false;
        }

        hovered_node_changed = node != document.hovered_node();
        document.set_hovered_node(node);
        if (node) {
            hovered_link_element = node->enclosing_link_element();
            if (hovered_link_element) {
#ifdef HTML_DEBUG
                dbg() << "PageView: hovering over a link to " << hovered_link_element->href();
#endif
                is_hovering_link = true;
            }
            auto offset = compute_mouse_event_offset(position, *result.layout_node);
            node->dispatch_event(MouseEvent::create("mousemove", offset.x(), offset.y()));
            if (!layout_root())
                return true;
        }
        if (m_in_mouse_selection) {
            layout_root()->selection().set_end({ result.layout_node, result.index_in_node });
            dump_selection("MouseMove");
            page_client.page_did_change_selection();
        }
    }
    page_client.page_did_request_cursor_change(is_hovering_link ? GUI::StandardCursor::Hand : GUI::StandardCursor::None);
    if (hovered_node_changed) {
        RefPtr<HTMLElement> hovered_html_element = document.hovered_node() ? document.hovered_node()->enclosing_html_element() : nullptr;
        if (hovered_html_element && !hovered_html_element->title().is_null()) {
            page_client.page_did_enter_tooltip_area(m_frame.to_main_frame_position(position), hovered_html_element->title());
        } else {
            page_client.page_did_leave_tooltip_area();
        }
        if (is_hovering_link)
            page_client.page_did_hover_link(document.complete_url(hovered_link_element->href()));
        else
            page_client.page_did_unhover_link();
    }
    return true;
}

void EventHandler::dump_selection(const char* event_name) const
{
    UNUSED_PARAM(event_name);
#ifdef SELECTION_DEBUG
    dbg() << event_name << " selection start: "
          << layout_root()->selection().start().layout_node << ":" << layout_root()->selection().start().index_in_node << ", end: "
          << layout_root()->selection().end().layout_node << ":" << layout_root()->selection().end().index_in_node;
#endif
}

}
