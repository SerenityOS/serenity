/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RemoteDesktopWidget.h"
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibRemoteDesktop/RemoteDesktopRenderer.h>
#include <LibRemoteDesktop/RemoteDesktopServerConnection.h>

REGISTER_WIDGET(RemoteDesktopClient, RemoteDesktopWidget)

namespace RemoteDesktopClient {

RemoteDesktopWidget::RemoteDesktopWidget()
{
}

RemoteDesktopWidget::~RemoteDesktopWidget()
{
}

bool RemoteDesktopWidget::connect(IPv4Address const& address, u16 port)
{
    if (m_connection) {
        m_connection->shutdown();
        m_connection = nullptr;

        if (on_disconnect)
            on_disconnect();
    }

    auto connection = RemoteDesktop::RemoteDesktopServerConnection::construct();
    if (!connection->connect(address, port)) {
        dbgln("RemoteDesktopWidget: failed to connect");
        return false;
    }

    auto renderer = adopt_own(*new RemoteDesktop::Renderer(*this, *connection));

    Vector<ByteBuffer, 10> available_fonts;
    renderer->font_database().for_each([&](auto& font) {
        available_fonts.append(ByteBuffer::copy(font.digest().data, font.digest().data_length()).release_value());
    });

    auto result = connection->start_session(available_fonts);
    if (result.error()) {
        GUI::MessageBox::show(window(), String::formatted("Failed to start session: {}", result.error_msg()), "Session failed");
        connection->shutdown();
        return false;
    }

    // TODO: hook up disconnect handler

    m_connection = move(connection);
    m_renderer = move(renderer);
    dbgln("RemoteDesktopWidget: connected");
    return true;
}

void RemoteDesktopWidget::did_scroll()
{
    update();
}

void RemoteDesktopWidget::paint_event(GUI::PaintEvent& event)
{
    AbstractScrollableWidget::paint_event(event);

    auto widget_content_rect = widget_inner_rect();
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.add_clip_rect(widget_content_rect);
    painter.translate(widget_content_rect.location());
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());
    if (m_renderer)
        m_renderer->paint(painter, to_content_rect(event.rect()));
    else
        painter.clear_rect(event.rect(), Color::Black);
}

bool RemoteDesktopWidget::is_connected() const
{
    return m_connection;
}

u64 RemoteDesktopWidget::bytes_sent() const
{
    return m_connection ? m_connection->bytes_sent() : 0;
}

u64 RemoteDesktopWidget::bytes_received() const
{
    return m_connection ? m_connection->bytes_received() : 0;
}

void RemoteDesktopWidget::set_surface_size(Gfx::IntSize const& size)
{
    set_content_size(size);
}

void RemoteDesktopWidget::invalidate_rects(Gfx::DisjointRectSet const& rects)
{
    auto widget_content_rect = widget_inner_rect();
    for (auto& rect : rects.rects())
        update(Gfx::IntRect { to_widget_position(rect.location()), rect.size() }.intersected(widget_content_rect));
}

void RemoteDesktopWidget::mousemove_event(GUI::MouseEvent& event)
{
    m_renderer->set_cursor_position(to_content_position(event.position()));
}

void RemoteDesktopWidget::mousedown_event(GUI::MouseEvent& event)
{
    m_renderer->set_mouse_buttons(to_content_position(event.position()), event.buttons());
}

void RemoteDesktopWidget::mouseup_event(GUI::MouseEvent& event)
{
    m_renderer->set_mouse_buttons(to_content_position(event.position()), event.buttons());
}

void RemoteDesktopWidget::mousewheel_event(GUI::MouseEvent& event)
{
    m_renderer->mouse_wheel_turned(to_content_position(event.position()), event.wheel_delta());
}

}
