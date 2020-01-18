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

#include "PaintableWidget.h"
#include "Tool.h"
#include <LibDraw/GraphicsBitmap.h>
#include <LibDraw/Palette.h>
#include <LibGUI/GPainter.h>

static PaintableWidget* s_the;

PaintableWidget& PaintableWidget::the()
{
    return *s_the;
}

PaintableWidget::PaintableWidget(GWidget* parent)
    : GWidget(parent)
{
    ASSERT(!s_the);
    s_the = this;
    set_fill_with_background_color(true);
    auto pal = palette();
    pal.set_color(ColorRole::Window, Color::MidGray);
    set_palette(pal);
    set_background_color(Color::MidGray);
    m_bitmap = GraphicsBitmap::create(GraphicsBitmap::Format::RGB32, { 600, 400 });
    m_bitmap->fill(Color::White);
}

PaintableWidget::~PaintableWidget()
{
}

void PaintableWidget::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.blit({ 0, 0 }, *m_bitmap, m_bitmap->rect());
}

void PaintableWidget::set_tool(Tool* tool)
{
    if (m_tool)
        m_tool->clear();
    m_tool = tool;
    if (m_tool)
        m_tool->setup(*this);
}

Tool* PaintableWidget::tool()
{
    return m_tool;
}

Color PaintableWidget::color_for(GMouseButton button) const
{
    if (button == GMouseButton::Left)
        return m_primary_color;
    if (button == GMouseButton::Right)
        return m_secondary_color;
    ASSERT_NOT_REACHED();
}

Color PaintableWidget::color_for(const GMouseEvent& event) const
{
    if (event.buttons() & GMouseButton::Left)
        return m_primary_color;
    if (event.buttons() & GMouseButton::Right)
        return m_secondary_color;
    ASSERT_NOT_REACHED();
}

void PaintableWidget::mousedown_event(GMouseEvent& event)
{
    if (event.button() == GMouseButton::Left || event.button() == GMouseButton::Right) {
        if (m_tool)
            m_tool->on_mousedown(event);
    }
    GWidget::mousedown_event(event);
}

void PaintableWidget::mouseup_event(GMouseEvent& event)
{
    if (event.button() == GMouseButton::Left || event.button() == GMouseButton::Right) {
        if (m_tool)
            m_tool->on_mouseup(event);
    }
    GWidget::mouseup_event(event);
}

void PaintableWidget::mousemove_event(GMouseEvent& event)
{
    if (m_tool)
        m_tool->on_mousemove(event);
    GWidget::mousemove_event(event);
}

void PaintableWidget::second_paint_event(GPaintEvent& event)
{
    if (m_tool)
        m_tool->on_second_paint(event);
    GWidget::second_paint_event(event);
}

void PaintableWidget::keydown_event(GKeyEvent& event)
{
    if (m_tool)
        m_tool->on_keydown(event);
    GWidget::keydown_event(event);
}

void PaintableWidget::keyup_event(GKeyEvent& event)
{
    if (m_tool)
        m_tool->on_keyup(event);
    GWidget::keyup_event(event);
}

void PaintableWidget::set_primary_color(Color color)
{
    if (m_primary_color == color)
        return;
    m_primary_color = color;
    if (on_primary_color_change)
        on_primary_color_change(color);
}

void PaintableWidget::set_secondary_color(Color color)
{
    if (m_secondary_color == color)
        return;
    m_secondary_color = color;
    if (on_secondary_color_change)
        on_secondary_color_change(color);
}

void PaintableWidget::set_bitmap(const GraphicsBitmap& bitmap)
{
    m_bitmap = bitmap;
    update();
}
