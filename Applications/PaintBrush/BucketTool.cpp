#include "BucketTool.h"
#include "PaintableWidget.h"
#include <AK/Queue.h>
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
    Queue<Point> queue;
    queue.enqueue(Point(start_position));
    while (!queue.is_empty()) {
        auto position = queue.dequeue();

        if (bitmap.get_pixel(position) != target_color)
            continue;
        bitmap.set_pixel(position, fill_color);

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

void BucketTool::on_mousedown(PaintableWidget& paintable_widget, GMouseEvent& event)
{
    if (!paintable_widget.rect().contains(event.position()))
        return;

    GPainter painter(paintable_widget.bitmap());
    auto target_color = paintable_widget.bitmap().get_pixel(event.x(), event.y());

    flood_fill(paintable_widget.bitmap(), event.position(), target_color, paintable_widget.color_for(event));

    paintable_widget.update();
}
