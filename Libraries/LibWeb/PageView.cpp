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

#include <AK/LexicalPath.h>
#include <AK/URL.h>
#include <LibCore/File.h>
#include <LibCore/MimeData.h>
#include <LibGUI/Application.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/Window.h>
#include <LibGfx/ImageDecoder.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/HTMLAnchorElement.h>
#include <LibWeb/DOM/HTMLImageElement.h>
#include <LibWeb/DOM/MouseEvent.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/Dump.h>
#include <LibWeb/Frame.h>
#include <LibWeb/Layout/LayoutDocument.h>
#include <LibWeb/Layout/LayoutNode.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/PageView.h>
#include <LibWeb/RenderingContext.h>
#include <stdio.h>

//#define SELECTION_DEBUG

namespace Web {

PageView::PageView()
    : m_main_frame(Web::Frame::create(*this))
{
    main_frame().on_set_document = [this](auto* document) {
        if (on_set_document)
            on_set_document(document);
        layout_and_sync_size();
        scroll_to_top();
        update();
    };
    main_frame().on_title_change = [this](auto& title) {
        if (on_title_change)
            on_title_change(title);
    };
    main_frame().on_load_start = [this](auto& url) {
        if (on_load_start)
            on_load_start(url);
    };
    main_frame().on_set_needs_display = [this](auto& content_rect) {
        if (content_rect.is_empty()) {
            update();
            return;
        }
        Gfx::Rect adjusted_rect = content_rect;
        adjusted_rect.set_location(to_widget_position(content_rect.location()));
        update(adjusted_rect);
    };

    set_should_hide_unnecessary_scrollbars(true);
    set_background_role(ColorRole::Base);
}

PageView::~PageView()
{
}

void PageView::layout_and_sync_size()
{
    if (!document())
        return;

    bool had_vertical_scrollbar = vertical_scrollbar().is_visible();
    bool had_horizontal_scrollbar = horizontal_scrollbar().is_visible();

    main_frame().set_size(available_size());
    document()->layout();
    set_content_size(enclosing_int_rect(layout_root()->rect()).size());

    // NOTE: If layout caused us to gain or lose scrollbars, we have to lay out again
    //       since the scrollbars now take up some of the available space.
    if (had_vertical_scrollbar != vertical_scrollbar().is_visible() || had_horizontal_scrollbar != horizontal_scrollbar().is_visible()) {
        main_frame().set_size(available_size());
        document()->layout();
        set_content_size(enclosing_int_rect(layout_root()->rect()).size());
    }

    main_frame().set_viewport_rect(viewport_rect_in_content_coordinates());

#ifdef HTML_DEBUG
    dbgprintf("\033[33;1mLayout tree after layout:\033[0m\n");
    ::dump_tree(*layout_root());
#endif
}

void PageView::resize_event(GUI::ResizeEvent& event)
{
    GUI::ScrollableWidget::resize_event(event);
    layout_and_sync_size();
}

void PageView::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(widget_inner_rect());
    painter.add_clip_rect(event.rect());

    if (!layout_root()) {
        painter.fill_rect(event.rect(), palette().color(background_role()));
        return;
    }

    painter.fill_rect(event.rect(), document()->background_color(palette()));

    if (auto background_bitmap = document()->background_image()) {
        painter.draw_tiled_bitmap(event.rect(), *background_bitmap);
    }

    painter.translate(frame_thickness(), frame_thickness());
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    RenderingContext context(painter, palette());
    context.set_should_show_line_box_borders(m_should_show_line_box_borders);
    context.set_viewport_rect(viewport_rect_in_content_coordinates());
    layout_root()->render(context);
}

void PageView::mousemove_event(GUI::MouseEvent& event)
{
    if (!layout_root())
        return GUI::ScrollableWidget::mousemove_event(event);

    bool hovered_node_changed = false;
    bool is_hovering_link = false;
    bool was_hovering_link = document()->hovered_node() && document()->hovered_node()->is_link();
    auto result = layout_root()->hit_test(to_content_position(event.position()));
    const HTMLAnchorElement* hovered_link_element = nullptr;
    if (result.layout_node) {
        RefPtr<Node> node = result.layout_node->node();
        hovered_node_changed = node != document()->hovered_node();
        document()->set_hovered_node(node);
        if (node) {
            hovered_link_element = node->enclosing_link_element();
            if (hovered_link_element) {
#ifdef HTML_DEBUG
                dbg() << "PageView: hovering over a link to " << hovered_link_element->href();
#endif
                is_hovering_link = true;
            }
            auto offset = compute_mouse_event_offset(event.position(), *result.layout_node);
            node->dispatch_event(MouseEvent::create("mousemove", offset.x(), offset.y()));
        }
        if (m_in_mouse_selection) {
            layout_root()->selection().set_end({ result.layout_node, result.index_in_node });
            dump_selection("MouseMove");
            update();
        }
    }
    if (window())
        window()->set_override_cursor(is_hovering_link ? GUI::StandardCursor::Hand : GUI::StandardCursor::None);
    if (hovered_node_changed) {
        update();
        RefPtr<HTMLElement> hovered_html_element = document()->hovered_node() ? document()->hovered_node()->enclosing_html_element() : nullptr;
        if (hovered_html_element && !hovered_html_element->title().is_null()) {
            auto screen_position = screen_relative_rect().location().translated(event.position());
            GUI::Application::the().show_tooltip(hovered_html_element->title(), screen_position.translated(4, 4));
        } else {
            GUI::Application::the().hide_tooltip();
        }
    }
    if (is_hovering_link != was_hovering_link) {
        if (on_link_hover) {
            on_link_hover(hovered_link_element ? document()->complete_url(hovered_link_element->href()).to_string() : String());
        }
    }
    event.accept();
}

void PageView::mousedown_event(GUI::MouseEvent& event)
{
    if (!layout_root())
        return GUI::ScrollableWidget::mousemove_event(event);

    bool hovered_node_changed = false;
    auto result = layout_root()->hit_test(to_content_position(event.position()));
    if (result.layout_node) {
        RefPtr<Node> node = result.layout_node->node();
        hovered_node_changed = node != document()->hovered_node();
        document()->set_hovered_node(node);
        if (node) {
            auto offset = compute_mouse_event_offset(event.position(), *result.layout_node);
            node->dispatch_event(MouseEvent::create("mousedown", offset.x(), offset.y()));
            if (RefPtr<HTMLAnchorElement> link = node->enclosing_link_element()) {
                dbg() << "PageView: clicking on a link to " << link->href();

                if (event.button() == GUI::MouseButton::Left) {
                    if (link->href().starts_with("javascript:")) {
                        run_javascript_url(link->href());
                    } else {
                        if (on_link_click)
                            on_link_click(link->href(), link->target(), event.modifiers());
                    }
                } else if (event.button() == GUI::MouseButton::Right) {
                    if (on_link_context_menu_request)
                        on_link_context_menu_request(link->href(), event.position().translated(screen_relative_rect().location()));
                } else if (event.button() == GUI::MouseButton::Middle) {
                    if (on_link_middle_click)
                        on_link_middle_click(link->href());
                }
            } else {
                if (event.button() == GUI::MouseButton::Left) {
                    if (layout_root())
                        layout_root()->selection().set({ result.layout_node, result.index_in_node }, {});
                    dump_selection("MouseDown");
                    m_in_mouse_selection = true;
                }
            }
        }
    }
    if (hovered_node_changed)
        update();
    event.accept();
}

void PageView::mouseup_event(GUI::MouseEvent& event)
{
    if (!layout_root())
        return GUI::ScrollableWidget::mouseup_event(event);

    auto result = layout_root()->hit_test(to_content_position(event.position()));
    if (result.layout_node) {
        if (RefPtr<Node> node = result.layout_node->node()) {
            auto offset = compute_mouse_event_offset(event.position(), *result.layout_node);
            node->dispatch_event(MouseEvent::create("mouseup", offset.x(), offset.y()));
        }
    }

    if (event.button() == GUI::MouseButton::Left) {
        dump_selection("MouseUp");
        m_in_mouse_selection = false;
    }
}

void PageView::keydown_event(GUI::KeyEvent& event)
{
    if (event.modifiers() == 0) {
        switch (event.key()) {
        case Key_Home:
            vertical_scrollbar().set_value(0);
            break;
        case Key_End:
            vertical_scrollbar().set_value(vertical_scrollbar().max());
            break;
        case Key_Down:
            vertical_scrollbar().set_value(vertical_scrollbar().value() + vertical_scrollbar().step());
            break;
        case Key_Up:
            vertical_scrollbar().set_value(vertical_scrollbar().value() - vertical_scrollbar().step());
            break;
        case Key_Left:
            horizontal_scrollbar().set_value(horizontal_scrollbar().value() + horizontal_scrollbar().step());
            break;
        case Key_Right:
            horizontal_scrollbar().set_value(horizontal_scrollbar().value() - horizontal_scrollbar().step());
            break;
        case Key_PageDown:
            vertical_scrollbar().set_value(vertical_scrollbar().value() + frame_inner_rect().height());
            break;
        case Key_PageUp:
            vertical_scrollbar().set_value(vertical_scrollbar().value() - frame_inner_rect().height());
            break;
        default:
            break;
        }
    }

    event.accept();
}

void PageView::reload()
{
    load(main_frame().document()->url());
}

bool PageView::load(const URL& url)
{
    if (window())
        window()->set_override_cursor(GUI::StandardCursor::None);

    return main_frame().loader().load(url);
}

const LayoutDocument* PageView::layout_root() const
{
    return document() ? document()->layout_node() : nullptr;
}

LayoutDocument* PageView::layout_root()
{
    if (!document())
        return nullptr;
    return const_cast<LayoutDocument*>(document()->layout_node());
}

void PageView::scroll_to_anchor(const StringView& name)
{
    if (!document())
        return;

    const auto* element = document()->get_element_by_id(name);
    if (!element) {
        auto candidates = document()->get_elements_by_name(name);
        for (auto* candidate : candidates) {
            if (is<HTMLAnchorElement>(*candidate)) {
                element = to<HTMLAnchorElement>(candidate);
                break;
            }
        }
    }

    if (!element) {
        dbg() << "PageView::scroll_to_anchor(): Anchor not found: '" << name << "'";
        return;
    }
    if (!element->layout_node()) {
        dbg() << "PageView::scroll_to_anchor(): Anchor found but without layout node: '" << name << "'";
        return;
    }
    auto& layout_node = *element->layout_node();
    Gfx::FloatRect float_rect { layout_node.box_type_agnostic_position(), { (float)visible_content_rect().width(), (float)visible_content_rect().height() } };
    scroll_into_view(enclosing_int_rect(float_rect), true, true);
    window()->set_override_cursor(GUI::StandardCursor::None);
}

void PageView::set_use_old_parser(bool use_old_parser)
{
    main_frame().loader().set_use_old_parser(use_old_parser);
}

void PageView::load_empty_document()
{
    main_frame().set_document(nullptr);
}

Document* PageView::document()
{
    return main_frame().document();
}

const Document* PageView::document() const
{
    return main_frame().document();
}

void PageView::set_document(Document* document)
{
    main_frame().set_document(document);
}

void PageView::dump_selection(const char* event_name)
{
    UNUSED_PARAM(event_name);
#ifdef SELECTION_DEBUG
    dbg() << event_name << " selection start: "
          << layout_root()->selection().start().layout_node << ":" << layout_root()->selection().start().index_in_node << ", end: "
          << layout_root()->selection().end().layout_node << ":" << layout_root()->selection().end().index_in_node;
#endif
}

void PageView::did_scroll()
{
    main_frame().set_viewport_rect(viewport_rect_in_content_coordinates());
    main_frame().did_scroll({});
}

Gfx::Point PageView::compute_mouse_event_offset(const Gfx::Point& event_position, const LayoutNode& layout_node) const
{
    auto content_event_position = to_content_position(event_position);
    auto top_left_of_layout_node = layout_node.box_type_agnostic_position();

    return {
        content_event_position.x() - static_cast<int>(top_left_of_layout_node.x()),
        content_event_position.y() - static_cast<int>(top_left_of_layout_node.y())
    };
}

void PageView::run_javascript_url(const String& url)
{
    ASSERT(url.starts_with("javascript:"));
    if (!document())
        return;

    auto source = url.substring_view(11, url.length() - 11);
    dbg() << "running js from url: _" << source << "_";
    document()->run_javascript(source);
}

void PageView::drop_event(GUI::DropEvent& event)
{
    if (event.mime_data().has_urls()) {
        if (on_url_drop) {
            on_url_drop(event.mime_data().urls().first());
            return;
        }
    }
    ScrollableWidget::drop_event(event);
}

}
