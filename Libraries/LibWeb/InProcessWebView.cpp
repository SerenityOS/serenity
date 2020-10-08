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
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/Window.h>
#include <LibGfx/ImageDecoder.h>
#include <LibGfx/ShareableBitmap.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/HTML/Parser/HTMLDocumentParser.h>
#include <LibWeb/InProcessWebView.h>
#include <LibWeb/Layout/LayoutBreak.h>
#include <LibWeb/Layout/LayoutDocument.h>
#include <LibWeb/Layout/LayoutNode.h>
#include <LibWeb/Layout/LayoutText.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Page/EventHandler.h>
#include <LibWeb/Page/Frame.h>
#include <LibWeb/Painting/PaintContext.h>
#include <LibWeb/UIEvents/MouseEvent.h>
#include <stdio.h>

//#define SELECTION_DEBUG

REGISTER_WIDGET(Web, InProcessWebView)

namespace Web {

InProcessWebView::InProcessWebView()
    : m_page(make<Page>(*this))
{
    set_should_hide_unnecessary_scrollbars(true);
    set_background_role(ColorRole::Base);

    m_copy_action = GUI::CommonActions::make_copy_action([this](auto&) {
        GUI::Clipboard::the().set_plain_text(selected_text());
    });

    m_select_all_action = GUI::CommonActions::make_select_all_action([this](auto&) {
        select_all();
    });
}

InProcessWebView::~InProcessWebView()
{
}

void InProcessWebView::select_all()
{
    auto* layout_root = this->layout_root();
    if (!layout_root)
        return;

    const LayoutNode* first_layout_node = layout_root;

    for (;;) {
        auto* next = first_layout_node->next_in_pre_order();
        if (!next)
            break;
        first_layout_node = next;
        if (is<LayoutText>(*first_layout_node))
            break;
    }

    const LayoutNode* last_layout_node = first_layout_node;

    for (const LayoutNode* layout_node = first_layout_node; layout_node; layout_node = layout_node->next_in_pre_order()) {
        if (is<LayoutText>(*layout_node))
            last_layout_node = layout_node;
    }

    ASSERT(first_layout_node);
    ASSERT(last_layout_node);

    int last_layout_node_index_in_node = 0;
    if (is<LayoutText>(*last_layout_node))
        last_layout_node_index_in_node = downcast<LayoutText>(*last_layout_node).text_for_rendering().length() - 1;

    layout_root->set_selection({ { first_layout_node, 0 }, { last_layout_node, last_layout_node_index_in_node } });
    update();
}

String InProcessWebView::selected_text() const
{
    return page().focused_frame().selected_text();
}

void InProcessWebView::page_did_layout()
{
    ASSERT(layout_root());
    set_content_size(layout_root()->size().to_type<int>());
}

void InProcessWebView::page_did_change_title(const String& title)
{
    if (on_title_change)
        on_title_change(title);
}

void InProcessWebView::page_did_set_document_in_main_frame(DOM::Document* document)
{
    if (on_set_document)
        on_set_document(document);
    layout_and_sync_size();
    scroll_to_top();
    update();
}

void InProcessWebView::page_did_start_loading(const URL& url)
{
    if (on_load_start)
        on_load_start(url);
}

void InProcessWebView::page_did_change_selection()
{
    update();
}

void InProcessWebView::page_did_request_cursor_change(Gfx::StandardCursor cursor)
{
    if (window())
        window()->set_cursor(cursor);
}

void InProcessWebView::page_did_request_context_menu(const Gfx::IntPoint& content_position)
{
    if (on_context_menu_request)
        on_context_menu_request(screen_relative_rect().location().translated(to_widget_position(content_position)));
}

void InProcessWebView::page_did_request_link_context_menu(const Gfx::IntPoint& content_position, const URL& url, [[maybe_unused]] const String& target, [[maybe_unused]] unsigned modifiers)
{
    if (on_link_context_menu_request)
        on_link_context_menu_request(url, screen_relative_rect().location().translated(to_widget_position(content_position)));
}

void InProcessWebView::page_did_request_image_context_menu(const Gfx::IntPoint& content_position, const URL& url, [[maybe_unused]] const String& target, [[maybe_unused]] unsigned modifiers, const Gfx::Bitmap* bitmap)
{
    if (!on_image_context_menu_request)
        return;
    Gfx::ShareableBitmap shareable_bitmap;
    if (bitmap)
        shareable_bitmap = Gfx::ShareableBitmap(*bitmap);
    on_image_context_menu_request(url, screen_relative_rect().location().translated(to_widget_position(content_position)), shareable_bitmap);
}

void InProcessWebView::page_did_click_link(const URL& url, const String& target, unsigned modifiers)
{
    if (on_link_click)
        on_link_click(url, target, modifiers);
}

void InProcessWebView::page_did_middle_click_link(const URL& url, const String& target, unsigned modifiers)
{
    if (on_link_middle_click)
        on_link_middle_click(url, target, modifiers);
}

void InProcessWebView::page_did_enter_tooltip_area(const Gfx::IntPoint& content_position, const String& title)
{
    GUI::Application::the()->show_tooltip(title, screen_relative_rect().location().translated(to_widget_position(content_position)), nullptr);
}

void InProcessWebView::page_did_leave_tooltip_area()
{
    GUI::Application::the()->hide_tooltip();
}

void InProcessWebView::page_did_hover_link(const URL& url)
{
    if (on_link_hover)
        on_link_hover(url);
}

void InProcessWebView::page_did_unhover_link()
{
    if (on_link_hover)
        on_link_hover({});
}

void InProcessWebView::page_did_invalidate(const Gfx::IntRect&)
{
    update();
}

void InProcessWebView::page_did_change_favicon(const Gfx::Bitmap& bitmap)
{
    if (on_favicon_change)
        on_favicon_change(bitmap);
}

void InProcessWebView::layout_and_sync_size()
{
    if (!document())
        return;

    bool had_vertical_scrollbar = vertical_scrollbar().is_visible();
    bool had_horizontal_scrollbar = horizontal_scrollbar().is_visible();

    page().main_frame().set_size(available_size());
    document()->layout();
    set_content_size(layout_root()->size().to_type<int>());

    // NOTE: If layout caused us to gain or lose scrollbars, we have to lay out again
    //       since the scrollbars now take up some of the available space.
    if (had_vertical_scrollbar != vertical_scrollbar().is_visible() || had_horizontal_scrollbar != horizontal_scrollbar().is_visible()) {
        page().main_frame().set_size(available_size());
        document()->layout();
        set_content_size(layout_root()->size().to_type<int>());
    }

    page().main_frame().set_viewport_rect(viewport_rect_in_content_coordinates());

#ifdef HTML_DEBUG
    dbgprintf("\033[33;1mLayout tree after layout:\033[0m\n");
    ::dump_tree(*layout_root());
#endif
}

void InProcessWebView::resize_event(GUI::ResizeEvent& event)
{
    GUI::ScrollableWidget::resize_event(event);
    layout_and_sync_size();
}

void InProcessWebView::paint_event(GUI::PaintEvent& event)
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

    PaintContext context(painter, palette(), { horizontal_scrollbar().value(), vertical_scrollbar().value() });
    context.set_should_show_line_box_borders(m_should_show_line_box_borders);
    context.set_viewport_rect(viewport_rect_in_content_coordinates());
    context.set_has_focus(is_focused());
    layout_root()->paint_all_phases(context);
}

void InProcessWebView::mousemove_event(GUI::MouseEvent& event)
{
    page().handle_mousemove(to_content_position(event.position()), event.buttons(), event.modifiers());
    GUI::ScrollableWidget::mousemove_event(event);
}

void InProcessWebView::mousedown_event(GUI::MouseEvent& event)
{
    page().handle_mousedown(to_content_position(event.position()), event.button(), event.modifiers());
    GUI::ScrollableWidget::mousedown_event(event);
}

void InProcessWebView::mouseup_event(GUI::MouseEvent& event)
{
    page().handle_mouseup(to_content_position(event.position()), event.button(), event.modifiers());
    GUI::ScrollableWidget::mouseup_event(event);
}

void InProcessWebView::keydown_event(GUI::KeyEvent& event)
{
    bool page_accepted_event = page().handle_keydown(event.key(), event.modifiers(), event.code_point());

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
            if (!page_accepted_event) {
                ScrollableWidget::keydown_event(event);
                return;
            }
            break;
        }
    }

    event.accept();
}

URL InProcessWebView::url() const
{
    if (!page().main_frame().document())
        return {};
    return page().main_frame().document()->url();
}

void InProcessWebView::reload()
{
    load(url());
}

void InProcessWebView::load_html(const StringView& html, const URL& url)
{
    page().main_frame().loader().load_html(html, url);
}

bool InProcessWebView::load(const URL& url)
{
    set_override_cursor(Gfx::StandardCursor::None);
    return page().main_frame().loader().load(url, FrameLoader::Type::Navigation);
}

const LayoutDocument* InProcessWebView::layout_root() const
{
    return document() ? document()->layout_node() : nullptr;
}

LayoutDocument* InProcessWebView::layout_root()
{
    if (!document())
        return nullptr;
    return const_cast<LayoutDocument*>(document()->layout_node());
}

void InProcessWebView::page_did_request_scroll_into_view(const Gfx::IntRect& rect)
{
    scroll_into_view(rect, true, true);
    set_override_cursor(Gfx::StandardCursor::None);
}

void InProcessWebView::load_empty_document()
{
    page().main_frame().set_document(nullptr);
}

DOM::Document* InProcessWebView::document()
{
    return page().main_frame().document();
}

const DOM::Document* InProcessWebView::document() const
{
    return page().main_frame().document();
}

void InProcessWebView::set_document(DOM::Document* document)
{
    page().main_frame().set_document(document);
}

void InProcessWebView::did_scroll()
{
    page().main_frame().set_viewport_rect(viewport_rect_in_content_coordinates());
    page().main_frame().did_scroll({});
}

void InProcessWebView::drop_event(GUI::DropEvent& event)
{
    if (event.mime_data().has_urls()) {
        if (on_url_drop) {
            on_url_drop(event.mime_data().urls().first());
            return;
        }
    }
    ScrollableWidget::drop_event(event);
}

void InProcessWebView::page_did_request_alert(const String& message)
{
    GUI::MessageBox::show(window(), message, "Alert", GUI::MessageBox::Type::Information);
}

}
