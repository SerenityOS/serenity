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
    Vector<Point> queue;
    queue.append(start_position);
    int queue_pos = 0;
    while (queue_pos != queue.size()) {
        auto position = queue[queue_pos++];

        if (queue_pos > 4096) {
            queue.shift_left(4096);
            queue_pos = 0;
        }

        if (!bitmap.rect().contains(position))
            continue;
        if (bitmap.get_pixel(position) != target_color)
            continue;
        bitmap.set_pixel(position, fill_color);

        if (position.x() != 0)
            queue.append(position.translated(0, -1));

        if (position.x() != bitmap.width() - 1)
            queue.append(position.translated(0, 1));

        if (position.y() != 0)
            queue.append(position.translated(-1, 0));

        if (position.y() != bitmap.height() - 1)
            queue.append(position.translated(1, 0));
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
