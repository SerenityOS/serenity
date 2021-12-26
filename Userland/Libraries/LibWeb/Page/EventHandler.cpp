/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Event.h>
#include <LibWeb/DOM/Range.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/HTMLIFrameElement.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Page/BrowsingContext.h>
#include <LibWeb/Page/EventHandler.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/UIEvents/EventNames.h>
#include <LibWeb/UIEvents/KeyboardEvent.h>
#include <LibWeb/UIEvents/MouseEvent.h>

namespace Web {

static Gfx::StandardCursor cursor_css_to_gfx(Optional<CSS::Cursor> cursor)
{
    if (!cursor.has_value()) {
        return Gfx::StandardCursor::None;
    }
    switch (cursor.value()) {
    case CSS::Cursor::Crosshair:
    case CSS::Cursor::Cell:
        return Gfx::StandardCursor::Crosshair;
    case CSS::Cursor::Grab:
    case CSS::Cursor::Grabbing:
        return Gfx::StandardCursor::Drag;
    case CSS::Cursor::Pointer:
        return Gfx::StandardCursor::Hand;
    case CSS::Cursor::Help:
        return Gfx::StandardCursor::Help;
    case CSS::Cursor::None:
        return Gfx::StandardCursor::Hidden;
    case CSS::Cursor::Text:
    case CSS::Cursor::VerticalText:
        return Gfx::StandardCursor::IBeam;
    case CSS::Cursor::Move:
    case CSS::Cursor::AllScroll:
        return Gfx::StandardCursor::Move;
    case CSS::Cursor::Progress:
    case CSS::Cursor::Wait:
        return Gfx::StandardCursor::Wait;

    case CSS::Cursor::ColResize:
        return Gfx::StandardCursor::ResizeColumn;
    case CSS::Cursor::EResize:
    case CSS::Cursor::WResize:
    case CSS::Cursor::EwResize:
        return Gfx::StandardCursor::ResizeHorizontal;

    case CSS::Cursor::RowResize:
        return Gfx::StandardCursor::ResizeRow;
    case CSS::Cursor::NResize:
    case CSS::Cursor::SResize:
    case CSS::Cursor::NsResize:
        return Gfx::StandardCursor::ResizeVertical;

    case CSS::Cursor::NeResize:
    case CSS::Cursor::SwResize:
    case CSS::Cursor::NeswResize:
        return Gfx::StandardCursor::ResizeDiagonalBLTR;

    case CSS::Cursor::NwResize:
    case CSS::Cursor::SeResize:
    case CSS::Cursor::NwseResize:
        return Gfx::StandardCursor::ResizeDiagonalTLBR;

    default:
        return Gfx::StandardCursor::None;
    }
}

static Gfx::IntPoint compute_mouse_event_offset(const Gfx::IntPoint& position, const Layout::Node& layout_node)
{
    auto top_left_of_layout_node = layout_node.box_type_agnostic_position();
    return {
        position.x() - static_cast<int>(top_left_of_layout_node.x()),
        position.y() - static_cast<int>(top_left_of_layout_node.y())
    };
}

EventHandler::EventHandler(Badge<BrowsingContext>, BrowsingContext& frame)
    : m_frame(frame)
    , m_edit_event_handler(make<EditEventHandler>(frame))
{
}

EventHandler::~EventHandler()
{
}

const Layout::InitialContainingBlock* EventHandler::layout_root() const
{
    if (!m_frame.active_document())
        return nullptr;
    return m_frame.active_document()->layout_node();
}

Layout::InitialContainingBlock* EventHandler::layout_root()
{
    if (!m_frame.active_document())
        return nullptr;
    return m_frame.active_document()->layout_node();
}

bool EventHandler::handle_mousewheel(const Gfx::IntPoint& position, unsigned int buttons, unsigned int modifiers, int wheel_delta)
{
    if (!layout_root())
        return false;

    // FIXME: Support wheel events in subframes.

    auto result = layout_root()->hit_test(position, Layout::HitTestType::Exact);
    if (result.layout_node) {
        if (result.layout_node->handle_mousewheel({}, position, buttons, modifiers, wheel_delta))
            return true;
    }

    if (auto* page = m_frame.page()) {
        page->client().page_did_request_scroll(0, wheel_delta * 20);
        return true;
    }

    return false;
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
            if (auto* subframe = verify_cast<HTML::HTMLIFrameElement>(*node).nested_browsing_context())
                return subframe->event_handler().handle_mouseup(position.translated(compute_mouse_event_offset({}, *result.layout_node)), button, modifiers);
            return false;
        }
        auto offset = compute_mouse_event_offset(position, *result.layout_node);
        node->dispatch_event(UIEvents::MouseEvent::create(UIEvents::EventNames::mouseup, offset.x(), offset.y(), position.x(), position.y()));
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

    NonnullRefPtr document = *m_frame.active_document();

    // TODO: Allow selecting element behind if one on top has pointer-events set to none.
    auto result = layout_root()->hit_test(position, Layout::HitTestType::Exact);
    auto pointer_events = result.layout_node->computed_values().pointer_events();
    if (pointer_events == CSS::PointerEvents::None)
        return false;

    RefPtr<DOM::Node> node;

    {
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
            if (auto* subframe = verify_cast<HTML::HTMLIFrameElement>(*node).nested_browsing_context())
                return subframe->event_handler().handle_mousedown(position.translated(compute_mouse_event_offset({}, *result.layout_node)), button, modifiers);
            return false;
        }

        if (auto* page = m_frame.page())
            page->set_focused_browsing_context({}, m_frame);

        auto offset = compute_mouse_event_offset(position, *result.layout_node);
        node->dispatch_event(UIEvents::MouseEvent::create(UIEvents::EventNames::mousedown, offset.x(), offset.y(), position.x(), position.y()));
    }

    // NOTE: Dispatching an event may have disturbed the world.
    if (!layout_root() || layout_root() != node->document().layout_node())
        return true;

    if (button == GUI::MouseButton::Right && is<HTML::HTMLImageElement>(*node)) {
        auto& image_element = verify_cast<HTML::HTMLImageElement>(*node);
        auto image_url = image_element.document().parse_url(image_element.src());
        if (auto* page = m_frame.page())
            page->client().page_did_request_image_context_menu(m_frame.to_top_level_position(position), image_url, "", modifiers, image_element.bitmap());
        return true;
    }

    if (RefPtr<HTML::HTMLAnchorElement> link = node->enclosing_link_element()) {
        auto href = link->href();
        auto url = document->parse_url(href);
        dbgln("Web::EventHandler: Clicking on a link to {}", url);
        if (button == GUI::MouseButton::Left) {
            if (href.starts_with("javascript:")) {
                document->run_javascript(href.substring_view(11, href.length() - 11));
            } else if (href.starts_with('#')) {
                auto anchor = href.substring_view(1, href.length() - 1);
                m_frame.scroll_to_anchor(anchor);
            } else {
                document->set_active_element(link);
                if (m_frame.is_top_level()) {
                    if (auto* page = m_frame.page())
                        page->client().page_did_click_link(url, link->target(), modifiers);
                } else {
                    // FIXME: Handle different targets!
                    m_frame.loader().load(url, FrameLoader::Type::Navigation);
                }
            }
        } else if (button == GUI::MouseButton::Right) {
            if (auto* page = m_frame.page())
                page->client().page_did_request_link_context_menu(m_frame.to_top_level_position(position), url, link->target(), modifiers);
        } else if (button == GUI::MouseButton::Middle) {
            if (auto* page = m_frame.page())
                page->client().page_did_middle_click_link(url, link->target(), modifiers);
        }
    } else {
        if (button == GUI::MouseButton::Left) {
            auto result = layout_root()->hit_test(position, Layout::HitTestType::TextCursor);
            if (result.layout_node && result.layout_node->dom_node()) {
                m_frame.set_cursor_position(DOM::Position(*result.layout_node->dom_node(), result.index_in_node));
                layout_root()->set_selection({ { result.layout_node, result.index_in_node }, {} });
                m_in_mouse_selection = true;
            }
        } else if (button == GUI::MouseButton::Right) {
            if (auto* page = m_frame.page())
                page->client().page_did_request_context_menu(m_frame.to_top_level_position(position));
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

    auto& document = *m_frame.active_document();

    bool hovered_node_changed = false;
    bool is_hovering_link = false;
    Gfx::StandardCursor hovered_node_cursor = Gfx::StandardCursor::None;
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
            if (auto* subframe = verify_cast<HTML::HTMLIFrameElement>(*node).nested_browsing_context())
                return subframe->event_handler().handle_mousemove(position.translated(compute_mouse_event_offset({}, *result.layout_node)), buttons, modifiers);
            return false;
        }

        auto pointer_events = result.layout_node->computed_values().pointer_events();
        if (pointer_events == CSS::PointerEvents::None)
            return false;

        hovered_node_changed = node != document.hovered_node();
        document.set_hovered_node(node);
        if (node) {
            hovered_link_element = node->enclosing_link_element();
            if (hovered_link_element)
                is_hovering_link = true;

            if (node->is_text()) {
                auto cursor = result.layout_node->computed_values().cursor();
                if (cursor == CSS::Cursor::Auto)
                    hovered_node_cursor = Gfx::StandardCursor::IBeam;
                else
                    hovered_node_cursor = cursor_css_to_gfx(cursor);
            }

            auto offset = compute_mouse_event_offset(position, *result.layout_node);
            node->dispatch_event(UIEvents::MouseEvent::create(UIEvents::EventNames::mousemove, offset.x(), offset.y(), position.x(), position.y()));
            // NOTE: Dispatching an event may have disturbed the world.
            if (!layout_root() || layout_root() != node->document().layout_node())
                return true;
        }
        if (m_in_mouse_selection) {
            auto hit = layout_root()->hit_test(position, Layout::HitTestType::TextCursor);
            if (hit.layout_node && hit.layout_node->dom_node()) {
                m_frame.set_cursor_position(DOM::Position(*hit.layout_node->dom_node(), result.index_in_node));
                layout_root()->set_selection_end({ hit.layout_node, hit.index_in_node });
            }
            if (auto* page = m_frame.page())
                page->client().page_did_change_selection();
        }
    }

    if (auto* page = m_frame.page()) {
        page->client().page_did_request_cursor_change(hovered_node_cursor);

        if (hovered_node_changed) {
            RefPtr<HTML::HTMLElement> hovered_html_element = document.hovered_node() ? document.hovered_node()->enclosing_html_element_with_attribute(HTML::AttributeNames::title) : nullptr;
            if (hovered_html_element && !hovered_html_element->title().is_null()) {
                page->client().page_did_enter_tooltip_area(m_frame.to_top_level_position(position), hovered_html_element->title());
            } else {
                page->client().page_did_leave_tooltip_area();
            }
            if (is_hovering_link)
                page->client().page_did_hover_link(document.parse_url(hovered_link_element->href()));
            else
                page->client().page_did_unhover_link();
        }
    }
    return true;
}

bool EventHandler::focus_next_element()
{
    if (!m_frame.active_document())
        return false;
    auto* element = m_frame.active_document()->focused_element();
    if (!element) {
        element = m_frame.active_document()->first_child_of_type<DOM::Element>();
        if (element && element->is_focusable()) {
            m_frame.active_document()->set_focused_element(element);
            return true;
        }
    }

    for (element = element->next_element_in_pre_order(); element && !element->is_focusable(); element = element->next_element_in_pre_order())
        ;

    m_frame.active_document()->set_focused_element(element);
    return element;
}

bool EventHandler::focus_previous_element()
{
    // FIXME: Implement Shift-Tab cycling backwards through focusable elements!
    return false;
}

constexpr bool should_ignore_keydown_event(u32 code_point)
{
    // FIXME: There are probably also keys with non-zero code points that should be filtered out.
    return code_point == 0;
}

bool EventHandler::handle_keydown(KeyCode key, unsigned modifiers, u32 code_point)
{
    if (key == KeyCode::Key_Tab) {
        if (modifiers & KeyModifier::Mod_Shift)
            return focus_previous_element();
        else
            return focus_next_element();
    }

    if (!layout_root())
        return false;

    if (layout_root()->selection().is_valid()) {
        auto range = layout_root()->selection().to_dom_range()->normalized();
        if (range->start_container()->is_editable()) {
            m_frame.active_document()->layout_node()->set_selection({});

            // FIXME: This doesn't work for some reason?
            m_frame.set_cursor_position({ *range->start_container(), range->start_offset() });

            if (key == KeyCode::Key_Backspace || key == KeyCode::Key_Delete) {
                m_edit_event_handler->handle_delete(range);
                return true;
            } else if (!should_ignore_keydown_event(code_point)) {
                m_edit_event_handler->handle_delete(range);
                m_edit_event_handler->handle_insert(m_frame.cursor_position(), code_point);
                m_frame.increment_cursor_position_offset();
                return true;
            }
        }
    }

    if (m_frame.cursor_position().is_valid() && m_frame.cursor_position().node()->is_editable()) {
        if (key == KeyCode::Key_Backspace) {
            if (!m_frame.decrement_cursor_position_offset()) {
                // FIXME: Move to the previous node and delete the last character there.
                return true;
            }

            m_edit_event_handler->handle_delete_character_after(m_frame.cursor_position());
            return true;
        } else if (key == KeyCode::Key_Delete) {
            if (m_frame.cursor_position().offset_is_at_end_of_node()) {
                // FIXME: Move to the next node and delete the first character there.
                return true;
            }
            m_edit_event_handler->handle_delete_character_after(m_frame.cursor_position());
            return true;
        } else if (key == KeyCode::Key_Right) {
            if (!m_frame.increment_cursor_position_offset()) {
                // FIXME: Move to the next node.
            }
            return true;
        } else if (key == KeyCode::Key_Left) {
            if (!m_frame.decrement_cursor_position_offset()) {
                // FIXME: Move to the previous node.
            }
            return true;
        } else if (!should_ignore_keydown_event(code_point)) {
            m_edit_event_handler->handle_insert(m_frame.cursor_position(), code_point);
            m_frame.increment_cursor_position_offset();
            return true;
        } else {
            // NOTE: Because modifier keys should be ignored, we need to return true.
            return true;
        }
    }

    auto event = UIEvents::KeyboardEvent::create_from_platform_event(UIEvents::EventNames::keydown, key, modifiers, code_point);
    if (m_frame.active_document()->focused_element())
        return m_frame.active_document()->focused_element()->dispatch_event(move(event));
    else if (m_frame.active_document()->body())
        return m_frame.active_document()->body()->dispatch_event(move(event));
    else
        return m_frame.active_document()->root().dispatch_event(move(event));
}

bool EventHandler::handle_keyup(KeyCode key, unsigned modifiers, u32 code_point)
{
    RefPtr<DOM::Document> document = m_frame.active_document();
    if (!document)
        return false;

    auto event = UIEvents::KeyboardEvent::create_from_platform_event(UIEvents::EventNames::keyup, key, modifiers, code_point);

    if (RefPtr<DOM::Element> focused_element = document->focused_element())
        return document->focused_element()->dispatch_event(move(event));

    if (RefPtr<HTML::HTMLElement> body = document->body())
        return body->dispatch_event(move(event));

    return document->root().dispatch_event(move(event));
}

void EventHandler::set_mouse_event_tracking_layout_node(Layout::Node* layout_node)
{
    if (layout_node)
        m_mouse_event_tracking_layout_node = layout_node->make_weak_ptr();
    else
        m_mouse_event_tracking_layout_node = nullptr;
}

}
