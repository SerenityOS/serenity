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
#include <LibWeb/Frame/EventHandler.h>
#include <LibWeb/Frame/Frame.h>
#include <LibWeb/Layout/LayoutDocument.h>
#include <LibWeb/Layout/LayoutNode.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/PageView.h>
#include <LibWeb/RenderingContext.h>
#include <stdio.h>

//#define SELECTION_DEBUG

namespace Web {

PageView::PageView()
    : m_page(make<Page>(*this))
{
    set_should_hide_unnecessary_scrollbars(true);
    set_background_role(ColorRole::Base);
}

PageView::~PageView()
{
}

void PageView::page_did_change_title(const String& title)
{
    if (on_title_change)
        on_title_change(title);
}

void PageView::page_did_set_document_in_main_frame(Document* document)
{
    if (on_set_document)
        on_set_document(document);
    layout_and_sync_size();
    scroll_to_top();
    update();
}

void PageView::page_did_start_loading(const URL& url)
{
    if (on_load_start)
        on_load_start(url);
}

void PageView::page_did_change_selection()
{
    update();
}

void PageView::page_did_request_cursor_change(GUI::StandardCursor cursor)
{
    if (window())
        window()->set_override_cursor(cursor);
}

void PageView::page_did_request_link_context_menu(const Gfx::Point& content_position, const String& href, [[maybe_unused]] const String& target, [[maybe_unused]] unsigned modifiers)
{
    if (on_link_context_menu_request)
        on_link_context_menu_request(href, screen_relative_rect().location().translated(to_widget_position(content_position)));
}

void PageView::page_did_click_link(const String& href, const String& target, unsigned modifiers)
{
    if (on_link_click)
        on_link_click(href, target, modifiers);
}

void PageView::page_did_middle_click_link(const String& href, [[maybe_unused]] const String& target, [[maybe_unused]] unsigned modifiers)
{
    if (on_link_middle_click)
        on_link_middle_click(href);
}

void PageView::page_did_enter_tooltip_area(const Gfx::Point& content_position, const String& title)
{
    GUI::Application::the().show_tooltip(title, screen_relative_rect().location().translated(to_widget_position(content_position)));
}

void PageView::page_did_leave_tooltip_area()
{
    GUI::Application::the().hide_tooltip();
}

void PageView::page_did_hover_link(const URL& url)
{
    if (on_link_hover)
        on_link_hover(url.to_string());
}

void PageView::page_did_unhover_link()
{
}

void PageView::page_did_request_scroll_to_anchor(const String& fragment)
{
    scroll_to_anchor(fragment);
}

void PageView::page_did_invalidate(const Gfx::Rect&)
{
    update();
}

void PageView::page_did_change_favicon(const Gfx::Bitmap& bitmap)
{
    if (on_favicon_change)
        on_favicon_change(bitmap);
}

void PageView::layout_and_sync_size()
{
    if (!document())
        return;

    bool had_vertical_scrollbar = vertical_scrollbar().is_visible();
    bool had_horizontal_scrollbar = horizontal_scrollbar().is_visible();

    page().main_frame().set_size(available_size());
    document()->layout();
    set_content_size(layout_root()->size().to_int_size());

    // NOTE: If layout caused us to gain or lose scrollbars, we have to lay out again
    //       since the scrollbars now take up some of the available space.
    if (had_vertical_scrollbar != vertical_scrollbar().is_visible() || had_horizontal_scrollbar != horizontal_scrollbar().is_visible()) {
        page().main_frame().set_size(available_size());
        document()->layout();
        set_content_size(layout_root()->size().to_int_size());
    }

    page().main_frame().set_viewport_rect(viewport_rect_in_content_coordinates());

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
    page().handle_mousemove(to_content_position(event.position()), event.buttons(), event.modifiers());
    GUI::ScrollableWidget::mousemove_event(event);
}

void PageView::mousedown_event(GUI::MouseEvent& event)
{
    page().handle_mousedown(to_content_position(event.position()), event.button(), event.modifiers());
    GUI::ScrollableWidget::mousedown_event(event);
}

void PageView::mouseup_event(GUI::MouseEvent& event)
{
    page().handle_mouseup(to_content_position(event.position()), event.button(), event.modifiers());
    GUI::ScrollableWidget::mouseup_event(event);
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
    load(page().main_frame().document()->url());
}

bool PageView::load(const URL& url)
{
    if (window())
        window()->set_override_cursor(GUI::StandardCursor::None);

    return page().main_frame().loader().load(url);
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
    page().main_frame().loader().set_use_old_parser(use_old_parser);
}

void PageView::load_empty_document()
{
    page().main_frame().set_document(nullptr);
}

Document* PageView::document()
{
    return page().main_frame().document();
}

const Document* PageView::document() const
{
    return page().main_frame().document();
}

void PageView::set_document(Document* document)
{
    page().main_frame().set_document(document);
}

void PageView::did_scroll()
{
    page().main_frame().set_viewport_rect(viewport_rect_in_content_coordinates());
    page().main_frame().did_scroll({});
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
