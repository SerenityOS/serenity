/*
 * Copyright (c) 2020-2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
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
    ASSERT(s_the);
    return *s_the;
}

void AppletManager::event(Core::Event& event)
{
    auto& mouse_event = static_cast<MouseEvent&>(event);

    for (auto& applet : m_applets) {
        if (!applet)
            continue;
        if (!applet->rect_in_menubar().contains(mouse_event.position()))
            continue;
        auto local_event = mouse_event.translated(-applet->rect_in_menubar().location());
        applet->event(local_event);
    }
}

void AppletManager::add_applet(Window& applet)
{
    m_applets.append(applet.make_weak_ptr());

    // Prune any dead weak pointers from the applet list.
    m_applets.remove_all_matching([](auto& entry) {
        return entry.is_null();
    });

    quick_sort(m_applets, [](auto& a, auto& b) {
        auto index_a = order_vector.find_first_index(a->title());
        auto index_b = order_vector.find_first_index(b->title());
        return index_a.value_or(0) > index_b.value_or(0);
    });

    calculate_applet_rects(MenuManager::the().window());

    MenuManager::the().refresh();
}

void AppletManager::calculate_applet_rects(Window& window)
{
    auto menubar_rect = window.rect();
    int right_edge_x = menubar_rect.width() - 4;
    for (auto& existing_applet : m_applets) {

        Gfx::IntRect new_applet_rect(right_edge_x - existing_applet->size().width(), 0, existing_applet->size().width(), existing_applet->size().height());
        Gfx::IntRect dummy_menubar_rect(0, 0, 0, 18);
        new_applet_rect.center_vertically_within(dummy_menubar_rect);

        existing_applet->set_rect_in_menubar(new_applet_rect);
        right_edge_x = existing_applet->rect_in_menubar().x() - 4;
    }
}

void AppletManager::remove_applet(Window& applet)
{
    m_applets.remove_first_matching([&](auto& entry) {
        return &applet == entry.ptr();
    });

    MenuManager::the().refresh();
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

    Gfx::Painter painter(*MenuManager::the().window().backing_store());
    Gfx::PainterStateSaver saver(painter);
    painter.add_clip_rect(applet.rect_in_menubar());
    painter.fill_rect(applet.rect_in_menubar(), WindowManager::the().palette().window());
    painter.blit(applet.rect_in_menubar().location(), *applet.backing_store(), applet.backing_store()->rect());
}

void AppletManager::invalidate_applet(const Window& applet, const Gfx::IntRect& rect)
{
    draw_applet(applet);
    MenuManager::the().window().invalidate(rect.translated(applet.rect_in_menubar().location()));
}

}
