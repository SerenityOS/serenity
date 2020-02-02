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

#include "SprayTool.h"
#include "PaintableWidget.h"
#include <AK/Queue.h>
#include <AK/SinglyLinkedList.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GMenu.h>
#include <LibDraw/GraphicsBitmap.h>
#include <stdio.h>
#include <LibM/math.h>

SprayTool::SprayTool()
{
    m_timer = Core::Timer::construct();
    m_timer->on_timeout = [&]() {
        paint_it();
    };
    m_timer->set_interval(200);
}

SprayTool::~SprayTool()
{
}

static double nrand()
{
    return double(rand()) / double(RAND_MAX);
}

void SprayTool::paint_it()
{
    GUI::Painter painter(m_widget->bitmap());
    auto& bitmap = m_widget->bitmap();
    ASSERT(bitmap.bpp() == 32);
    m_widget->update();
    const double minimal_radius = 10;
    const double base_radius = minimal_radius * m_thickness;
    for (int i = 0; i < 100 + (nrand() * 800); i++) {
        double radius = base_radius * nrand();
        double angle = 2 * M_PI * nrand();
        const int xpos = m_last_pos.x() + radius * cos(angle);
        const int ypos = m_last_pos.y() - radius * sin(angle);
        if (xpos < 0 || xpos >= bitmap.width())
            continue;
        if (ypos < 0 || ypos >= bitmap.height())
            continue;
        bitmap.set_pixel<GraphicsBitmap::Format::RGB32>(xpos, ypos, m_color);
    }
}

void SprayTool::on_mousedown(GUI::MouseEvent& event)
{
    if (!m_widget->rect().contains(event.position()))
        return;

    m_color = m_widget->color_for(event);
    m_last_pos = event.position();
    m_timer->start();
    paint_it();
}

void SprayTool::on_mousemove(GUI::MouseEvent& event)
{
    m_last_pos = event.position();
    if (m_timer->is_active()) {
        paint_it();
        m_timer->restart(m_timer->interval());
    }
}

void SprayTool::on_mouseup(GUI::MouseEvent&)
{
    m_timer->stop();
}

void SprayTool::on_contextmenu(GUI::ContextMenuEvent& event)
{
    if (!m_context_menu) {
        m_context_menu = GUI::Menu::construct();
        m_thickness_actions.set_exclusive(true);
        auto insert_action = [&](int size, bool checked = false) {
            auto action = GUI::Action::create(String::number(size), [this, size](auto& action) {
                m_thickness = size;
                action.set_checked(true);
            });
            action->set_checkable(true);
            action->set_checked(checked);
            m_thickness_actions.add_action(*action);
            m_context_menu->add_action(move(action));
        };
        insert_action(1, true);
        insert_action(2);
        insert_action(3);
        insert_action(4);
    }
    m_context_menu->popup(event.screen_position());
}

