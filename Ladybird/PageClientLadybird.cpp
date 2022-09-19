/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#define AK_DONT_REPLACE_STD

#include "PageClientLadybird.h"
#include "Utilities.h"
#include "WebView.h"
#include <LibCore/System.h>
#include <LibGfx/Painter.h>
#include <LibJS/Runtime/ConsoleObject.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <QIcon>
#include <QMessageBox>
#include <QScrollBar>
#include <QToolTip>

namespace Ladybird {

NonnullOwnPtr<PageClientLadybird> PageClientLadybird::create(WebView& view)
{
    return adopt_own(*new PageClientLadybird(view));
}

PageClientLadybird::PageClientLadybird(WebView& view)
    : m_view(view)
    , m_page(make<Web::Page>(*this))
{
}

Web::Layout::InitialContainingBlock* PageClientLadybird::layout_root()
{
    auto* document = page().top_level_browsing_context().active_document();
    if (!document)
        return nullptr;
    return document->layout_node();
}

void PageClientLadybird::load(AK::URL const& url)
{
    if (!url.is_valid())
        return;

    page().load(url);
}

void PageClientLadybird::paint(Gfx::IntRect const& content_rect, Gfx::Bitmap& target)
{
    Gfx::Painter painter(target);

    if (auto* document = page().top_level_browsing_context().active_document())
        document->update_layout();

    painter.fill_rect({ {}, content_rect.size() }, palette().base());

    auto* layout_root = this->layout_root();
    if (!layout_root) {
        return;
    }

    Web::PaintContext context(painter, palette(), content_rect.top_left());
    context.set_should_show_line_box_borders(m_should_show_line_box_borders);
    context.set_viewport_rect(content_rect);
    context.set_has_focus(true);
    layout_root->paint_all_phases(context);
}

void PageClientLadybird::setup_palette(Core::AnonymousBuffer theme_buffer)
{
    m_palette_impl = Gfx::PaletteImpl::create_with_anonymous_buffer(theme_buffer);
}

void PageClientLadybird::set_viewport_rect(Gfx::IntRect rect)
{
    m_viewport_rect = rect;
    page().top_level_browsing_context().set_viewport_rect(rect);
}

Gfx::Palette PageClientLadybird::palette() const
{
    return Gfx::Palette(*m_palette_impl);
}

Gfx::IntRect PageClientLadybird::screen_rect() const
{
    // FIXME: Return the actual screen rect.
    return m_viewport_rect;
}

Gfx::IntRect PageClientLadybird::viewport_rect() const
{
    return m_viewport_rect;
}

Web::CSS::PreferredColorScheme PageClientLadybird::preferred_color_scheme() const
{
    return m_preferred_color_scheme;
}

void PageClientLadybird::page_did_change_title(String const& title)
{
    emit m_view.title_changed(title.characters());
}

void PageClientLadybird::page_did_start_loading(AK::URL const& url)
{
    emit m_view.load_started(url);
}

void PageClientLadybird::page_did_finish_loading(AK::URL const&)
{
    initialize_js_console();
    m_console_client->send_messages(0);
}

void PageClientLadybird::initialize_js_console()
{
    auto* document = page().top_level_browsing_context().active_document();
    auto realm = document->realm().make_weak_ptr();
    if (m_realm && m_realm.ptr() == realm.ptr())
        return;

    m_realm = realm;

    auto& console_object = *document->realm().intrinsics().console_object();
    m_console_client = make<Ladybird::ConsoleClient>(console_object.console(), *realm, m_view);
    console_object.console().set_client(*m_console_client.ptr());
}

void PageClientLadybird::page_did_change_selection()
{
}

void PageClientLadybird::page_did_request_cursor_change(Gfx::StandardCursor cursor)
{
    switch (cursor) {
    case Gfx::StandardCursor::Hand:
        m_view.setCursor(Qt::PointingHandCursor);
        break;
    case Gfx::StandardCursor::IBeam:
        m_view.setCursor(Qt::IBeamCursor);
        break;
    case Gfx::StandardCursor::Arrow:
    default:
        m_view.setCursor(Qt::ArrowCursor);
        break;
    }
}

void PageClientLadybird::page_did_request_context_menu(Gfx::IntPoint const&)
{
}

void PageClientLadybird::page_did_request_link_context_menu(Gfx::IntPoint const&, AK::URL const&, String const&, unsigned)
{
}

void PageClientLadybird::page_did_request_image_context_menu(Gfx::IntPoint const&, AK::URL const&, String const&, unsigned, Gfx::Bitmap const*)
{
}

void PageClientLadybird::page_did_click_link(AK::URL const&, String const&, unsigned)
{
}

void PageClientLadybird::page_did_middle_click_link(AK::URL const&, String const&, unsigned)
{
}

void PageClientLadybird::page_did_enter_tooltip_area(Gfx::IntPoint const& content_position, String const& tooltip)
{
    auto widget_position = m_view.to_widget(content_position);
    QToolTip::showText(
        m_view.mapToGlobal(QPoint(widget_position.x(), widget_position.y())),
        qstring_from_akstring(tooltip),
        &m_view);
}

void PageClientLadybird::page_did_leave_tooltip_area()
{
    QToolTip::hideText();
}

void PageClientLadybird::page_did_hover_link(AK::URL const& url)
{
    emit m_view.link_hovered(url.to_string().characters());
}

void PageClientLadybird::page_did_unhover_link()
{
    emit m_view.link_unhovered();
}

void PageClientLadybird::page_did_invalidate(Gfx::IntRect const&)
{
    m_view.viewport()->update();
}

void PageClientLadybird::page_did_change_favicon(Gfx::Bitmap const& bitmap)
{
    auto qimage = QImage(bitmap.scanline_u8(0), bitmap.width(), bitmap.height(), QImage::Format_ARGB32);
    if (qimage.isNull())
        return;
    auto qpixmap = QPixmap::fromImage(qimage);
    if (qpixmap.isNull())
        return;
    emit m_view.favicon_changed(QIcon(qpixmap));
}

void PageClientLadybird::page_did_layout()
{
    auto* layout_root = this->layout_root();
    VERIFY(layout_root);
    Gfx::IntSize content_size;
    if (layout_root->paint_box()->has_overflow())
        content_size = enclosing_int_rect(layout_root->paint_box()->scrollable_overflow_rect().value()).size();
    else
        content_size = enclosing_int_rect(layout_root->paint_box()->absolute_rect()).size();

    m_view.verticalScrollBar()->setMaximum(content_size.height() - m_viewport_rect.height());
    m_view.verticalScrollBar()->setPageStep(m_viewport_rect.height());
    m_view.horizontalScrollBar()->setMaximum(content_size.width() - m_viewport_rect.width());
    m_view.horizontalScrollBar()->setPageStep(m_viewport_rect.width());
}

void PageClientLadybird::page_did_request_scroll_into_view(Gfx::IntRect const& rect)
{
    if (m_viewport_rect.contains(rect))
        return;

    if (rect.top() < m_viewport_rect.top()) {
        m_view.verticalScrollBar()->setValue(rect.top());
    } else if (rect.top() > m_viewport_rect.top() && rect.bottom() > m_viewport_rect.bottom()) {
        m_view.verticalScrollBar()->setValue(rect.bottom() - m_viewport_rect.height() + 1);
    }
}

void PageClientLadybird::page_did_request_alert(String const& message)
{
    QMessageBox::warning(&m_view, "Ladybird", qstring_from_akstring(message));
}

bool PageClientLadybird::page_did_request_confirm(String const& message)
{
    auto result = QMessageBox::question(&m_view, "Ladybird", qstring_from_akstring(message),
        QMessageBox::StandardButton::Ok | QMessageBox::StandardButton::Cancel);

    return result == QMessageBox::StandardButton::Ok;
}

String PageClientLadybird::page_did_request_prompt(String const&, String const&)
{
    return String::empty();
}

String PageClientLadybird::page_did_request_cookie(AK::URL const& url, Web::Cookie::Source source)
{
    return m_cookie_jar.get_cookie(url, source);
}

void PageClientLadybird::page_did_set_cookie(AK::URL const& url, Web::Cookie::ParsedCookie const& cookie, Web::Cookie::Source source)
{
    m_cookie_jar.set_cookie(url, cookie, source);
}

void PageClientLadybird::dump_cookies() const
{
    m_cookie_jar.dump_cookies();
}

void PageClientLadybird::request_file(NonnullRefPtr<Web::FileRequest>& request)
{
    auto const file = Core::System::open(request->path(), O_RDONLY);
    request->on_file_request_finish(file);
}

void PageClientLadybird::set_should_show_line_box_borders(bool state)
{
    m_should_show_line_box_borders = state;
}

}
