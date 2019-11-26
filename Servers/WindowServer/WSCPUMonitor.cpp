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
    : m_thread([this] {
        monitor();
        return 0;
    })
{
    m_thread.start();
}

void WSCPUMonitor::monitor()
{
    for (;;) {
        static unsigned last_busy;
        static unsigned last_idle;
        unsigned busy;
        unsigned idle;
        get_cpu_usage(busy, idle);
        unsigned busy_diff = busy - last_busy;
        unsigned idle_diff = idle - last_idle;
        last_busy = busy;
        last_idle = idle;
        float cpu = (float)busy_diff / (float)(busy_diff + idle_diff);
        m_cpu_history.enqueue(cpu);
        m_dirty = true;
        sleep(1);
    }
}

void WSCPUMonitor::get_cpu_usage(unsigned& busy, unsigned& idle)
{
    busy = 0;
    idle = 0;

    auto all_processes = CProcessStatisticsReader::get_all();

    for (auto& it : all_processes) {
        for (auto& jt : it.value.threads) {
            if (it.value.pid == 0)
                idle += jt.times_scheduled;
            else
                busy += jt.times_scheduled;
        }
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
