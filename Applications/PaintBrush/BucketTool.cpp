#include "BucketTool.h"
#include "PaintableWidget.h"
#include <AK/SinglyLinkedList.h>
#include <LibGUI/GPainter.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <stdio.h>

BucketTool::BucketTool()
{
}

BucketTool::~BucketTool()
{
}

static void flood_fill(GraphicsBitmap& bitmap, const Point& start_position, Color target_color, Color fill_color)
{
    SinglyLinkedList<Point> queue;

    queue.append(Point(start_position));
    while (!queue.is_empty()) {
        auto position = queue.take_first();
        if (!bitmap.rect().contains(position))
            continue;
        if (bitmap.get_pixel(position) != target_color)
            continue;
        bitmap.set_pixel(position, fill_color);

        queue.append(position.translated(0, -1));
        queue.append(position.translated(0, 1));
        queue.append(position.translated(1, 0));
        queue.append(position.translated(-1, 0));
    }
}

void BucketTool::on_mousedown(PaintableWidget& paintable_widget, GMouseEvent& event)
{
    if (!paintable_widget.rect().contains(event.position()))
        return;

    GPainter painter(paintable_widget.bitmap());
    auto target_color = paintable_widget.bitmap().get_pixel(event.x(), event.y());

    flood_fill(paintable_widget.bitmap(), event.position(), target_color, paintable_widget.color_for(event));

    paintable_widget.update();
}
