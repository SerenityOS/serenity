/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
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
#include <LibWeb/DOM/Range.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/HTMLIFrameElement.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/InProcessWebView.h>
#include <LibWeb/Layout/InitialContainingBlockBox.h>
#include <LibWeb/Page/EventHandler.h>
#include <LibWeb/Page/Frame.h>
#include <LibWeb/UIEvents/EventNames.h>
#include <LibWeb/UIEvents/MouseEvent.h>

namespace Web {

static Gfx::IntPoint compute_mouse_event_offset(const Gfx::IntPoint& position, const Layout::Node& layout_node)
{
    auto top_left_of_layout_node = layout_node.box_type_agnostic_position();
    return {
        position.x() - static_cast<int>(top_left_of_layout_node.x()),
        position.y() - static_cast<int>(top_left_of_layout_node.y())
    };
}

EventHandler::EventHandler(Badge<Frame>, Frame& frame)
    : m_frame(frame)
    , m_edit_event_handler(make<EditEventHandler>(frame))
{
}

EventHandler::~EventHandler()
{
}

const Layout::InitialContainingBlockBox* EventHandler::layout_root() const
{
    if (!m_frame.document())
        return nullptr;
    return m_frame.document()->layout_node();
}

Layout::InitialContainingBlockBox* EventHandler::layout_root()
{
    if (!m_frame.document())
        return nullptr;
    return m_frame.document()->layout_node();
}

bool EventHandler::handle_mouseup(const Gfx::IntPoint& position, unsigned button, unsigned modifiers)
{
    if (!layout_root())
        return false;

    if (m_mouse_event_tracking_layout_node) {
        m_mouse_event_tracking_layout_node->handle_mouseup({}, position, button, modifiers);
        return true;
    }

    bool handled_event = false;

    auto result = layout_root()->hit_test(position, Layout::HitTestType::Exact);

    if (result.layout_node && result.layout_node->wants_mouse_events()) {
        result.layout_node->handle_mouseup({}, position, button, modifiers);

        // Things may have changed as a consequence of Layout::Node::handle_mouseup(). Hit test again.
        if (!layout_root())
            return true;
        result = layout_root()->hit_test(position, Layout::HitTestType::Exact);
    }

    if (result.layout_node && result.layout_node->dom_node()) {
        RefPtr<DOM::Node> node = result.layout_node->dom_node();
        if (is<HTML::HTMLIFrameElement>(*node)) {
            if (auto* subframe = downcast<HTML::HTMLIFrameElement>(*node).content_frame())
                return subframe->event_handler().handle_mouseup(position.translated(compute_mouse_event_offset({}, *result.layout_node)), button, modifiers);
            return false;
        }
        auto offset = compute_mouse_event_offset(position, *result.layout_node);
        node->dispatch_event(UIEvents::MouseEvent::create(UIEvents::EventNames::mouseup, offset.x(), offset.y()));
        handled_event = true;
    }

    if (button == GUI::MouseButton::Left)
        m_in_mouse_selection = false;
    return handled_event;
}

bool EventHandler::handle_mousedown(const Gfx::IntPoint& position, unsigned button, unsigned modifiers)
{
    if (!layout_root())
        return false;

    if (m_mouse_event_tracking_layout_node) {
        m_mouse_event_tracking_layout_node->handle_mousedown({}, position, button, modifiers);
        return true;
    }

    NonnullRefPtr document = *m_frame.document();
    RefPtr<DOM::Node> node;

    {
        auto result = layout_root()->hit_test(position, Layout::HitTestType::Exact);
        if (!result.layout_node)
            return false;

        node = result.layout_node->dom_node();
        document->set_hovered_node(node);

        if (result.layout_node->wants_mouse_events()) {
            result.layout_node->handle_mousedown({}, position, button, modifiers);
            return true;
        }

        if (!node)
            return false;

        if (is<HTML::HTMLIFrameElement>(*node)) {
            if (auto* subframe = downcast<HTML::HTMLIFrameElement>(*node).content_frame())
                return subframe->event_handler().handle_mousedown(position.translated(compute_mouse_event_offset({}, *result.layout_node)), button, modifiers);
            return false;
        }

        if (auto* page = m_frame.page())
            page->set_focused_frame({}, m_frame);

        auto offset = compute_mouse_event_offset(position, *result.layout_node);
        node->dispatch_event(UIEvents::MouseEvent::create(UIEvents::EventNames::mousedown, offset.x(), offset.y()));
    }

    // NOTE: Dispatching an event may have disturbed the world.
    if (!layout_root() || layout_root() != node->document().layout_node())
        return true;

    if (button == GUI::MouseButton::Right && is<HTML::HTMLImageElement>(*node)) {
        auto& image_element = downcast<HTML::HTMLImageElement>(*node);
        auto image_url = image_element.document().complete_url(image_element.src());
        if (auto* page = m_frame.page())
            page->client().page_did_request_image_context_menu(m_frame.to_main_frame_position(position), image_url, "", modifiers, image_element.bitmap());
        return true;
    }

    if (RefPtr<HTML::HTMLAnchorElement> link = node->enclosing_link_element()) {
        auto href = link->href();
        auto url = document->complete_url(href);
        dbgln("Web::EventHandler: Clicking on a link to {}", url);
        if (button == GUI::MouseButton::Left) {
            if (href.starts_with("javascript:")) {
                document->run_javascript(href.substring_view(11, href.length() - 11));
            } else if (href.starts_with('#')) {
                auto anchor = href.substring_view(1, href.length() - 1);
                m_frame.scroll_to_anchor(anchor);
            } else {
                if (m_frame.is_main_frame()) {
                    if (auto* page = m_frame.page())
                        page->client().page_did_click_link(url, link->target(), modifiers);
                } else {
                    // FIXME: Handle different targets!
                    m_frame.loader().load(url, FrameLoader::Type::Navigation);
                }
            }
        } else if (button == GUI::MouseButton::Right) {
            if (auto* page = m_frame.page())
                page->client().page_did_request_link_context_menu(m_frame.to_main_frame_position(position), url, link->target(), modifiers);
        } else if (button == GUI::MouseButton::Middle) {
            if (auto* page = m_frame.page())
                page->client().page_did_middle_click_link(url, link->target(), modifiers);
        }
    } else {
        if (button == GUI::MouseButton::Left) {
            auto result = layout_root()->hit_test(position, Layout::HitTestType::TextCursor);
            if (result.layout_node && result.layout_node->dom_node()) {
                m_frame.set_cursor_position(DOM::Position(*node, result.index_in_node));
                layout_root()->set_selection({ { result.layout_node, result.index_in_node }, {} });
                m_in_mouse_selection = true;
            }
        } else if (button == GUI::MouseButton::Right) {
            if (auto* page = m_frame.page())
                page->client().page_did_request_context_menu(m_frame.to_main_frame_position(position));
        }
    }
    return true;
}

bool EventHandler::handle_mousemove(const Gfx::IntPoint& position, unsigned buttons, unsigned modifiers)
{
    if (!layout_root())
        return false;

    if (m_mouse_event_tracking_layout_node) {
        m_mouse_event_tracking_layout_node->handle_mousemove({}, position, buttons, modifiers);
        return true;
    }

    auto& document = *m_frame.document();

    bool hovered_node_changed = false;
    bool is_hovering_link = false;
    bool is_hovering_text = false;
    auto result = layout_root()->hit_test(position, Layout::HitTestType::Exact);
    const HTML::HTMLAnchorElement* hovered_link_element = nullptr;
    if (result.layout_node) {

        if (result.layout_node->wants_mouse_events()) {
            document.set_hovered_node(result.layout_node->dom_node());
            result.layout_node->handle_mousemove({}, position, buttons, modifiers);
            // FIXME: It feels a bit aggressive to always update the cursor like this.
            if (auto* page = m_frame.page())
                page->client().page_did_request_cursor_change(Gfx::StandardCursor::None);
            return true;
        }

        RefPtr<DOM::Node> node = result.layout_node->dom_node();

        if (node && is<HTML::HTMLIFrameElement>(*node)) {
            if (auto* subframe = downcast<HTML::HTMLIFrameElement>(*node).content_frame())
                return subframe->event_handler().handle_mousemove(position.translated(compute_mouse_event_offset({}, *result.layout_node)), buttons, modifiers);
            return false;
        }

        hovered_node_changed = node != document.hovered_node();
        document.set_hovered_node(node);
        if (node) {
            if (node->is_text())
                is_hovering_text = true;
            hovered_link_element = node->enclosing_link_element();
            if (hovered_link_element)
                is_hovering_link = true;
            auto offset = compute_mouse_event_offset(position, *result.layout_node);
            node->dispatch_event(UIEvents::MouseEvent::create(UIEvents::EventNames::mousemove, offset.x(), offset.y()));
            // NOTE: Dispatching an event may have disturbed the world.
            if (!layout_root() || layout_root() != node->document().layout_node())
                return true;
        }
        if (m_in_mouse_selection) {
            auto hit = layout_root()->hit_test(position, Layout::HitTestType::TextCursor);
            if (hit.layout_node && hit.layout_node->dom_node()) {
                layout_root()->set_selection_end({ hit.layout_node, hit.index_in_node });
            }
            if (auto* page = m_frame.page())
                page->client().page_did_change_selection();
        }
    }

    if (auto* page = m_frame.page()) {
        if (is_hovering_link)
            page->client().page_did_request_cursor_change(Gfx::StandardCursor::Hand);
        else if (is_hovering_text)
            page->client().page_did_request_cursor_change(Gfx::StandardCursor::IBeam);
        else
            page->client().page_did_request_cursor_change(Gfx::StandardCursor::None);

        if (hovered_node_changed) {
            RefPtr<HTML::HTMLElement> hovered_html_element = document.hovered_node() ? document.hovered_node()->enclosing_html_element() : nullptr;
            if (hovered_html_element && !hovered_html_element->title().is_null()) {
                page->client().page_did_enter_tooltip_area(m_frame.to_main_frame_position(position), hovered_html_element->title());
            } else {
                page->client().page_did_leave_tooltip_area();
            }
            if (is_hovering_link)
                page->client().page_did_hover_link(document.complete_url(hovered_link_element->href()));
            else
                page->client().page_did_unhover_link();
        }
    }
    return true;
}

bool EventHandler::focus_next_element()
{
    if (!m_frame.document())
        return false;
    auto* element = m_frame.document()->focused_element();
    if (!element) {
        element = m_frame.document()->first_child_of_type<DOM::Element>();
        if (element && element->is_focusable()) {
            m_frame.document()->set_focused_element(element);
            return true;
        }
    }

    for (element = element->next_element_in_pre_order(); element && !element->is_focusable(); element = element->next_element_in_pre_order())
        ;

    m_frame.document()->set_focused_element(element);
    return element;
}

bool EventHandler::focus_previous_element()
{
    // FIXME: Implement Shift-Tab cycling backwards through focusable elements!
    return false;
}

bool EventHandler::handle_keydown(KeyCode key, unsigned modifiers, u32 code_point)
{
    if (key == KeyCode::Key_Tab) {
        if (modifiers & KeyModifier::Mod_Shift)
            return focus_previous_element();
        else
            return focus_next_element();
    }

    if (layout_root()->selection().is_valid()) {
        auto range = layout_root()->selection().to_dom_range()->normalized();
        if (range->start_container()->is_editable()) {
            m_frame.document()->layout_node()->set_selection({});

            // FIXME: This doesn't work for some reason?
            m_frame.set_cursor_position({ *range->start_container(), range->start_offset() });

            if (key == KeyCode::Key_Backspace || key == KeyCode::Key_Delete) {

                m_edit_event_handler->handle_delete(range);
                return true;
            } else {
                m_edit_event_handler->handle_delete(range);
                m_edit_event_handler->handle_insert(m_frame.cursor_position(), code_point);

                auto new_position = m_frame.cursor_position();
                new_position.set_offset(new_position.offset() + 1);
                m_frame.set_cursor_position(move(new_position));

                return true;
            }
        }
    }

    if (m_frame.cursor_position().is_valid() && m_frame.cursor_position().node()->is_editable()) {
        if (key == KeyCode::Key_Backspace) {
            auto position = m_frame.cursor_position();

            if (position.offset() == 0)
                TODO();

            auto new_position = m_frame.cursor_position();
            new_position.set_offset(position.offset() - 1);
            m_frame.set_cursor_position(move(new_position));

            m_edit_event_handler->handle_delete(DOM::Range::create(*position.node(), position.offset() - 1, *position.node(), position.offset()));

            return true;
        } else if (key == KeyCode::Key_Delete) {
            auto position = m_frame.cursor_position();

            if (position.offset() >= downcast<DOM::Text>(position.node())->data().length())
                TODO();

            m_edit_event_handler->handle_delete(DOM::Range::create(*position.node(), position.offset(), *position.node(), position.offset() + 1));

            return true;
        } else if (key == KeyCode::Key_Right) {
            auto position = m_frame.cursor_position();

            if (position.offset() >= downcast<DOM::Text>(position.node())->data().length())
                TODO();

            auto new_position = m_frame.cursor_position();
            new_position.set_offset(position.offset() + 1);
            m_frame.set_cursor_position(move(new_position));

            return true;
        } else if (key == KeyCode::Key_Left) {
            auto position = m_frame.cursor_position();

            if (position.offset() == 0)
                TODO();

            auto new_position = m_frame.cursor_position();
            new_position.set_offset(new_position.offset() - 1);
            m_frame.set_cursor_position(move(new_position));

            return true;
        } else {
            m_edit_event_handler->handle_insert(m_frame.cursor_position(), code_point);

            auto new_position = m_frame.cursor_position();
            new_position.set_offset(new_position.offset() + 1);
            m_frame.set_cursor_position(move(new_position));

            return true;
        }
    }

    return false;
}

void EventHandler::set_mouse_event_tracking_layout_node(Layout::Node* layout_node)
{
    if (layout_node)
        m_mouse_event_tracking_layout_node = layout_node->make_weak_ptr();
    else
        m_mouse_event_tracking_layout_node = nullptr;
}

}
