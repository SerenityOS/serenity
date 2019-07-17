#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibCore/CProcessStatisticsReader.h>
#include <WindowServer/WSCPUMonitor.h>
#include <WindowServer/WSEventLoop.h>
#include <WindowServer/WSWindowManager.h>
#include <stdio.h>
#include <unistd.h>

WSCPUMonitor::WSCPUMonitor()
{
    create_thread([](void* context) -> int {
        auto& monitor = *(WSCPUMonitor*)context;
        for (;;) {
            static unsigned last_busy;
            static unsigned last_idle;
            unsigned busy;
            unsigned idle;
            monitor.get_cpu_usage(busy, idle);
            unsigned busy_diff = busy - last_busy;
            unsigned idle_diff = idle - last_idle;
            last_busy = busy;
            last_idle = idle;
            float cpu = (float)busy_diff / (float)(busy_diff + idle_diff);
            monitor.m_cpu_history.enqueue(cpu);
            monitor.m_dirty = true;
            sleep(1);
        }
    },
        this);
}

void WSCPUMonitor::get_cpu_usage(unsigned& busy, unsigned& idle)
{
    busy = 0;
    idle = 0;

    auto all_processes = CProcessStatisticsReader::get_all();

    for (auto& it : all_processes) {
        if (it.value.pid == 0)
            idle += it.value.times_scheduled;
        else
            busy += it.value.times_scheduled;
    }
}

void WSCPUMonitor::paint(Painter& painter, const Rect& rect)
{
    painter.fill_rect(rect, Color::Black);
    int i = m_cpu_history.capacity() - m_cpu_history.size();
    for (auto cpu_usage : m_cpu_history) {
        painter.draw_line(
            { rect.x() + i, rect.bottom() },
            { rect.x() + i, (int)(rect.y() + (rect.height() - (cpu_usage * (float)rect.height()))) },
            Color::from_rgb(0xaa6d4b));
        ++i;
    }
}
