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

#include "BucketTool.h"
#include "PaintableWidget.h"
#include <AK/Queue.h>
#include <AK/SinglyLinkedList.h>
#include <LibGUI/GPainter.h>
#include <LibDraw/GraphicsBitmap.h>
#include <stdio.h>

BucketTool::BucketTool()
{
}

BucketTool::~BucketTool()
{
}

static void flood_fill(GraphicsBitmap& bitmap, const Point& start_position, Color target_color, Color fill_color)
{
    ASSERT(bitmap.bpp() == 32);

    if (target_color == fill_color)
        return;

    Queue<Point> queue;
    queue.enqueue(Point(start_position));
    while (!queue.is_empty()) {
        auto position = queue.dequeue();

        if (bitmap.get_pixel<GraphicsBitmap::Format::RGB32>(position.x(), position.y()) != target_color)
            continue;
        bitmap.set_pixel<GraphicsBitmap::Format::RGB32>(position.x(), position.y(), fill_color);

        if (position.x() != 0)
            queue.enqueue(position.translated(-1, 0));

        if (position.x() != bitmap.width() - 1)
            queue.enqueue(position.translated(1, 0));

        if (position.y() != 0)
            queue.enqueue(position.translated(0, -1));

        if (position.y() != bitmap.height() - 1)
            queue.enqueue(position.translated(0, 1));
    }
}

void BucketTool::on_mousedown(GUI::MouseEvent& event)
{
    if (!m_widget->rect().contains(event.position()))
        return;

    GUI::Painter painter(m_widget->bitmap());
    auto target_color = m_widget->bitmap().get_pixel(event.x(), event.y());

    flood_fill(m_widget->bitmap(), event.position(), target_color, m_widget->color_for(event));

    m_widget->update();
}
