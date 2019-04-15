#include <WindowServer/WSCPUMonitor.h>
#include <WindowServer/WSEventLoop.h>
#include <WindowServer/WSWindowManager.h>
#include <unistd.h>
#include <stdio.h>

WSCPUMonitor::WSCPUMonitor()
{
    create_thread([] (void* context) -> int {
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
    }, this);
}

void WSCPUMonitor::get_cpu_usage(unsigned& busy, unsigned& idle)
{
    busy = 0;
    idle = 0;

    if (!m_fp)
        m_fp = fopen("/proc/all", "r");
    if (!m_fp) {
        perror("failed to open /proc/all");
        ASSERT_NOT_REACHED();
    }
    for (;;) {
        char buf[BUFSIZ];
        char* ptr = fgets(buf, sizeof(buf), m_fp);
        if (!ptr)
            break;
        auto parts = String(buf, Chomp).split(',');
        if (parts.size() < 17)
            break;
        bool ok;
        pid_t pid = parts[0].to_uint(ok);
        ASSERT(ok);
        unsigned nsched = parts[1].to_uint(ok);
        ASSERT(ok);

        if (pid == 0)
            idle += nsched;
        else
            busy += nsched;
    }
    int rc = fseek(m_fp, 0, SEEK_SET);
    ASSERT(rc == 0);
}

void WSCPUMonitor::paint(Painter& painter, const Rect& rect)
{
    painter.fill_rect(rect, Color::Black);
    int i = m_cpu_history.capacity() - m_cpu_history.size();
    for (auto cpu_usage : m_cpu_history) {
        painter.draw_line(
            { rect.x() + i, rect.bottom() },
            { rect.x() + i, (int)(rect.y() + (rect.height() - (cpu_usage * (float)rect.height()))) },
            Color::from_rgb(0xaa6d4b)
        );
        ++i;
    }
}
