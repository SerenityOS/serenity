#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <WindowServer/WSCPUMonitor.h>
#include <WindowServer/WSEventLoop.h>
#include <WindowServer/WSWindowManager.h>
#include <stdio.h>
#include <unistd.h>

WSCPUMonitor::WSCPUMonitor()
    : m_proc_all("/proc/all")
{
    if (!m_proc_all.open(CIODevice::OpenMode::ReadOnly))
        ASSERT_NOT_REACHED();

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

    m_proc_all.seek(0);
    auto file_contents = m_proc_all.read_all();
    auto json = JsonValue::from_string({ file_contents.data(), file_contents.size() });
    json.as_array().for_each([&](auto& value) {
        const JsonObject& process_object = value.as_object();
        pid_t pid = process_object.get("pid").to_dword();
        unsigned nsched = process_object.get("times_scheduled").to_dword();
        if (pid == 0)
            idle += nsched;
        else
            busy += nsched;
    });
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
