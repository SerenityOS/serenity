/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/URL.h>
#include <LibCore/MimeData.h>
#include <LibGUI/Application.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Scrollbar.h>
#include <LibGUI/Window.h>
#include <LibGfx/ShareableBitmap.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/InProcessWebView.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Page/EventHandler.h>
#include <LibWeb/Painting/PaintContext.h>
#include <LibWeb/UIEvents/MouseEvent.h>

REGISTER_WIDGET(Web, InProcessWebView)

namespace Web {

InProcessWebView::InProcessWebView()
    : m_page(make<Page>(*this))
{
    set_should_hide_unnecessary_scrollbars(true);
    set_background_role(ColorRole::Base);
    set_focus_policy(GUI::FocusPolicy::StrongFocus);
}

InProcessWebView::~InProcessWebView()
{
}

void InProcessWebView::select_all()
{
    page().focused_context().select_all();
    update();
}

String InProcessWebView::selected_text() const
{
    return page().focused_context().selected_text();
}

void InProcessWebView::set_preferred_color_scheme(CSS::PreferredColorScheme color_scheme)
{
    m_preferred_color_scheme = color_scheme;
    if (auto* document = page().top_level_browsing_context().active_document())
        document->invalidate_style();
}

void InProcessWebView::page_did_layout()
{
    VERIFY(layout_root());
    set_content_size(layout_root()->size().to_type<int>());
}

void InProcessWebView::page_did_change_title(const String& title)
{
    if (on_title_change)
        on_title_change(title);
}

void InProcessWebView::page_did_set_document_in_top_level_browsing_context(DOM::Document* document)
{
    if (on_set_document)
        on_set_document(document);
    layout_and_sync_size();
    scroll_to_top();
    update();
}

void InProcessWebView::page_did_start_loading(const AK::URL& url)
{
    if (on_load_start)
        on_load_start(url);
}

void InProcessWebView::page_did_finish_loading(const AK::URL& url)
{
    if (on_load_finish)
        on_load_finish(url);
}

void InProcessWebView::page_did_change_selection()
{
    update();
}

void InProcessWebView::page_did_request_cursor_change(Gfx::StandardCursor cursor)
{
    set_override_cursor(cursor);
}

void InProcessWebView::page_did_request_context_menu(const Gfx::IntPoint& content_position)
{
    if (on_context_menu_request)
        on_context_menu_request(screen_relative_rect().location().translated(to_widget_position(content_position)));
}

void InProcessWebView::page_did_request_link_context_menu(const Gfx::IntPoint& content_position, const AK::URL& url, [[maybe_unused]] const String& target, [[maybe_unused]] unsigned modifiers)
{
    if (on_link_context_menu_request)
        on_link_context_menu_request(url, screen_relative_rect().location().translated(to_widget_position(content_position)));
}

void InProcessWebView::page_did_request_image_context_menu(const Gfx::IntPoint& content_position, const AK::URL& url, [[maybe_unused]] const String& target, [[maybe_unused]] unsigned modifiers, const Gfx::Bitmap* bitmap)
{
    if (!on_image_context_menu_request)
        return;
    Gfx::ShareableBitmap shareable_bitmap;
    if (bitmap)
        shareable_bitmap = bitmap->to_shareable_bitmap();
    on_image_context_menu_request(url, screen_relative_rect().location().translated(to_widget_position(content_position)), move(shareable_bitmap));
}

void InProcessWebView::page_did_click_link(const AK::URL& url, const String& target, unsigned modifiers)
{
    if (on_link_click)
        on_link_click(url, target, modifiers);
}

void InProcessWebView::page_did_middle_click_link(const AK::URL& url, const String& target, unsigned modifiers)
{
    if (on_link_middle_click)
        on_link_middle_click(url, target, modifiers);
}

void InProcessWebView::page_did_enter_tooltip_area([[maybe_unused]] const Gfx::IntPoint& content_position, const String& title)
{
    GUI::Application::the()->show_tooltip(title, nullptr);
}

void InProcessWebView::page_did_leave_tooltip_area()
{
    GUI::Application::the()->hide_tooltip();
}

void InProcessWebView::page_did_hover_link(const AK::URL& url)
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

    page().top_level_browsing_context().set_size(available_size());
    set_content_size(layout_root()->size().to_type<int>());

    // NOTE: If layout caused us to gain or lose scrollbars, we have to lay out again
    //       since the scrollbars now take up some of the available space.
    if (had_vertical_scrollbar != vertical_scrollbar().is_visible() || had_horizontal_scrollbar != horizontal_scrollbar().is_visible()) {
        page().top_level_browsing_context().set_size(available_size());
        set_content_size(layout_root()->size().to_type<int>());
    }

    page().top_level_browsing_context().set_viewport_scroll_offset({ horizontal_scrollbar().value(), vertical_scrollbar().value() });
}

void InProcessWebView::resize_event(GUI::ResizeEvent& event)
{
    GUI::AbstractScrollableWidget::resize_event(event);
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

    painter.translate(frame_thickness(), frame_thickness());

    PaintContext context(painter, palette(), { horizontal_scrollbar().value(), vertical_scrollbar().value() });
    context.set_should_show_line_box_borders(m_should_show_line_box_borders);
    context.set_viewport_rect(viewport_rect_in_content_coordinates());
    context.set_has_focus(is_focused());
    layout_root()->paint_all_phases(context);
}

void InProcessWebView::mousemove_event(GUI::MouseEvent& event)
{
    page().handle_mousemove(to_content_position(event.position()), event.buttons(), event.modifiers());
    GUI::AbstractScrollableWidget::mousemove_event(event);
}

void InProcessWebView::mousedown_event(GUI::MouseEvent& event)
{
    page().handle_mousedown(to_content_position(event.position()), event.button(), event.modifiers());
    GUI::AbstractScrollableWidget::mousedown_event(event);
}

void InProcessWebView::mouseup_event(GUI::MouseEvent& event)
{
    page().handle_mouseup(to_content_position(event.position()), event.button(), event.modifiers());
    GUI::AbstractScrollableWidget::mouseup_event(event);
}

void InProcessWebView::mousewheel_event(GUI::MouseEvent& event)
{
    page().handle_mousewheel(to_content_position(event.position()), event.button(), event.modifiers(), event.wheel_delta());
    GUI::AbstractScrollableWidget::mousewheel_event(event);
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
                AbstractScrollableWidget::keydown_event(event);
                return;
            }
            break;
        }
    }

    event.accept();
}

AK::URL InProcessWebView::url() const
{
    if (!page().top_level_browsing_context().active_document())
        return {};
    return page().top_level_browsing_context().active_document()->url();
}

void InProcessWebView::reload()
{
    load(url());
}

void InProcessWebView::load_html(StringView html, const AK::URL& url)
{
    page().top_level_browsing_context().loader().load_html(html, url);
}

bool InProcessWebView::load(const AK::URL& url)
{
    set_override_cursor(Gfx::StandardCursor::None);
    return page().top_level_browsing_context().loader().load(url, FrameLoader::Type::Navigation);
}

const Layout::InitialContainingBlock* InProcessWebView::layout_root() const
{
    return document() ? document()->layout_node() : nullptr;
}

Layout::InitialContainingBlock* InProcessWebView::layout_root()
{
    if (!document())
        return nullptr;
    return const_cast<Layout::InitialContainingBlock*>(document()->layout_node());
}

void InProcessWebView::page_did_request_scroll_into_view(const Gfx::IntRect& rect)
{
    scroll_into_view(rect, true, true);
    set_override_cursor(Gfx::StandardCursor::None);
}

void InProcessWebView::load_empty_document()
{
    page().top_level_browsing_context().set_active_document(nullptr);
}

DOM::Document* InProcessWebView::document()
{
    return page().top_level_browsing_context().active_document();
}

const DOM::Document* InProcessWebView::document() const
{
    return page().top_level_browsing_context().active_document();
}

void InProcessWebView::set_document(DOM::Document* document)
{
    page().top_level_browsing_context().set_active_document(document);
}

void InProcessWebView::did_scroll()
{
    page().top_level_browsing_context().set_viewport_scroll_offset({ horizontal_scrollbar().value(), vertical_scrollbar().value() });
}

void InProcessWebView::drop_event(GUI::DropEvent& event)
{
    if (event.mime_data().has_urls()) {
        if (on_url_drop) {
            on_url_drop(event.mime_data().urls().first());
            return;
        }
    }
    AbstractScrollableWidget::drop_event(event);
}

void InProcessWebView::page_did_request_alert(const String& message)
{
    GUI::MessageBox::show(window(), message, "Alert", GUI::MessageBox::Type::Information);
}

bool InProcessWebView::page_did_request_confirm(const String& message)
{
    auto confirm_result = GUI::MessageBox::show(window(), message, "Confirm", GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::OKCancel);
    return confirm_result == GUI::Dialog::ExecResult::ExecOK;
}

String InProcessWebView::page_did_request_prompt(const String& message, const String& default_)
{
    String value { default_ };
    if (GUI::InputBox::show(window(), value, message, "Prompt") == GUI::InputBox::ExecOK)
        return value;
    return {};
}

String InProcessWebView::page_did_request_cookie(const AK::URL& url, Cookie::Source source)
{
    if (on_get_cookie)
        return on_get_cookie(url, source);
    return {};
}

void InProcessWebView::page_did_set_cookie(const AK::URL& url, const Cookie::ParsedCookie& cookie, Cookie::Source source)
{
    if (on_set_cookie)
        on_set_cookie(url, cookie, source);
}

}
