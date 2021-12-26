/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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

#include "AppletManager.h"
#include <AK/QuickSort.h>
#include <LibCore/EventLoop.h>
#include <LibGfx/Painter.h>
#include <WindowServer/MenuManager.h>

namespace WindowServer {

static AppletManager* s_the;
Vector<String> order_vector;

AppletManager::AppletManager()
{
    s_the = this;

    auto wm_config = Core::ConfigFile::open("/etc/WindowServer/WindowServer.ini");
    auto order = wm_config->read_entry("Applet", "Order");
    order_vector = order.split(',');
}

AppletManager::~AppletManager()
{
}

AppletManager& AppletManager::the()
{
    VERIFY(s_the);
    return *s_the;
}

void AppletManager::set_position(const Gfx::IntPoint& position)
{
    m_window->move_to(position);
    m_window->set_visible(true);
}

void AppletManager::set_hovered_applet(Window* applet)
{
    if (m_hovered_applet == applet)
        return;

    if (m_hovered_applet)
        Core::EventLoop::current().post_event(*m_hovered_applet, make<Event>(Event::WindowLeft));

    m_hovered_applet = applet;

    if (m_hovered_applet)
        Core::EventLoop::current().post_event(*m_hovered_applet, make<Event>(Event::WindowEntered));
}

void AppletManager::event(Core::Event& event)
{
    if (event.type() == Event::WindowLeft && m_hovered_applet) {
        set_hovered_applet(nullptr);
        return;
    }

    if (!is<MouseEvent>(event))
        return;
    auto& mouse_event = static_cast<MouseEvent&>(event);

    for (auto& applet : m_applets) {
        if (!applet)
            continue;
        if (!applet->rect_in_applet_area().contains(mouse_event.position()))
            continue;
        auto local_event = mouse_event.translated(-applet->rect_in_applet_area().location());
        set_hovered_applet(applet);
        Core::EventLoop::current().post_event(*applet, make<MouseEvent>(local_event));
        return;
    }
}

void AppletManager::add_applet(Window& applet)
{
    m_applets.append(applet);

    // Prune any dead weak pointers from the applet list.
    m_applets.remove_all_matching([](auto& entry) {
        return entry.is_null();
    });

    quick_sort(m_applets, [](auto& a, auto& b) {
        auto index_a = order_vector.find_first_index(a->title());
        auto index_b = order_vector.find_first_index(b->title());
        return index_a.value_or(0) > index_b.value_or(0);
    });

    relayout();

    MenuManager::the().refresh();
}

void AppletManager::relayout()
{
    constexpr int applet_spacing = 4;
    constexpr int applet_window_height = 20;
    int total_width = 0;
    for (auto& existing_applet : m_applets) {
        total_width += max(0, existing_applet->size().width()) + applet_spacing;
    }

    if (total_width > 0)
        total_width -= applet_spacing;

    auto right_edge_x = total_width;

    for (auto& existing_applet : m_applets) {
        auto applet_size = existing_applet->size();
        Gfx::IntRect new_applet_rect(right_edge_x - applet_size.width(), 0, applet_size.width(), applet_size.height());

        Gfx::IntRect dummy_container_rect(0, 0, 0, applet_window_height);
        new_applet_rect.center_vertically_within(dummy_container_rect);

        existing_applet->set_rect_in_applet_area(new_applet_rect);
        right_edge_x = existing_applet->rect_in_applet_area().x() - applet_spacing;
    }

    if (!m_window) {
        m_window = Window::construct(*this, WindowType::AppletArea);
        m_window->set_visible(false);
    }

    Gfx::IntRect rect { m_window->position(), Gfx::IntSize { total_width, applet_window_height } };
    if (m_window->rect() == rect)
        return;
    m_window->set_rect(rect);

    repaint();

    WindowManager::the().tell_wms_applet_area_size_changed(rect.size());
}

void AppletManager::repaint()
{
    if (!m_window) {
        return;
    }

    auto rect = Gfx::IntRect { { 0, 0 }, m_window->size() };

    if (!rect.is_empty()) {
        Gfx::Painter painter(*m_window->backing_store());
        painter.fill_rect(rect, WindowManager::the().palette().button());
    }
}

void AppletManager::did_change_theme()
{
    repaint();
}

void AppletManager::remove_applet(Window& applet)
{
    m_applets.remove_first_matching([&](auto& entry) {
        return &applet == entry.ptr();
    });

    relayout();
}

void AppletManager::draw()
{
    for (auto& applet : m_applets) {
        if (!applet)
            continue;
        draw_applet(*applet);
    }
}

void AppletManager::draw_applet(const Window& applet)
{
    if (!applet.backing_store())
        return;

    Gfx::Painter painter(*m_window->backing_store());
    Gfx::PainterStateSaver saver(painter);
    painter.add_clip_rect(applet.rect_in_applet_area());
    painter.fill_rect(applet.rect_in_applet_area(), WindowManager::the().palette().button());
    painter.blit(applet.rect_in_applet_area().location(), *applet.backing_store(), applet.backing_store()->rect());
}

void AppletManager::invalidate_applet(const Window& applet, const Gfx::IntRect&)
{
    draw_applet(applet);
    draw();
    // FIXME: Invalidate only the exact rect we've been given.
    if (m_window)
        m_window->invalidate();
}

}
