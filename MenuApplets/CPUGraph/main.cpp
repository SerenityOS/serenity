#include <AK/CircularQueue.h>
#include <LibCore/CProcessStatisticsReader.h>
#include <LibCore/CTimer.h>
#include <LibDraw/GraphicsBitmap.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GWindowServerConnection.h>

NonnullRefPtr<GraphicsBitmap> create_shared_bitmap(const Size& size)
{
    ASSERT(GWindowServerConnection::the().server_pid());
    ASSERT(!size.is_empty());
    size_t pitch = round_up_to_power_of_two(size.width() * sizeof(RGBA32), 16);
    size_t size_in_bytes = size.height() * pitch;
    auto shared_buffer = SharedBuffer::create_with_size(size_in_bytes);
    ASSERT(shared_buffer);
    shared_buffer->share_with(GWindowServerConnection::the().server_pid());
    return GraphicsBitmap::create_with_shared_buffer(GraphicsBitmap::Format::RGBA32, *shared_buffer, size);
}

static void get_cpu_usage(unsigned& busy, unsigned& idle)
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

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    Size applet_size(30, 16);
    Rect applet_rect({}, applet_size);

    CircularQueue<float, 30> cpu_history;

    i32 applet_id = GWindowServerConnection::the().send_sync<WindowServer::CreateMenuApplet>(applet_size)->applet_id();
    auto bitmap = create_shared_bitmap(applet_size);

    GWindowServerConnection::the().send_sync<WindowServer::SetMenuAppletBackingStore>(applet_id, bitmap->shared_buffer_id());

    unsigned last_busy = 0;
    unsigned last_idle = 0;

    auto repaint = [&] {
        unsigned busy;
        unsigned idle;
        get_cpu_usage(busy, idle);
        unsigned busy_diff = busy - last_busy;
        unsigned idle_diff = idle - last_idle;
        last_busy = busy;
        last_idle = idle;
        float cpu = (float)busy_diff / (float)(busy_diff + idle_diff);
        cpu_history.enqueue(cpu);

        GPainter painter(*bitmap);
        painter.fill_rect(applet_rect, Color::Black);
        int i = cpu_history.capacity() - cpu_history.size();
        for (auto cpu_usage : cpu_history) {
            painter.draw_line(
                { applet_rect.x() + i, applet_rect.bottom() },
                { applet_rect.x() + i, (int)(applet_rect.y() + (applet_rect.height() - (cpu_usage * (float)applet_rect.height()))) },
                Color::from_rgb(0xaa6d4b));
            ++i;
        }

        GWindowServerConnection::the().send_sync<WindowServer::InvalidateMenuAppletRect>(applet_id, applet_rect);
    };

    repaint();

    auto timer = CTimer::construct(1000, [&] {
        repaint();
    });

    return app.exec();
}
