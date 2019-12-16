#include <AK/CircularQueue.h>
#include <LibCore/CProcessStatisticsReader.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>

class GraphWidget final : public GWidget {
    C_OBJECT(GraphWidget)
public:
    GraphWidget()
        : GWidget(nullptr)
    {
        start_timer(1000);
    }

    virtual ~GraphWidget() override {}

private:
    virtual void timer_event(CTimerEvent&) override
    {
        unsigned busy;
        unsigned idle;
        get_cpu_usage(busy, idle);
        unsigned busy_diff = busy - m_last_busy;
        unsigned idle_diff = idle - m_last_idle;
        m_last_busy = busy;
        m_last_idle = idle;
        float cpu = (float)busy_diff / (float)(busy_diff + idle_diff);
        m_cpu_history.enqueue(cpu);
        update();
    }

    virtual void paint_event(GPaintEvent& event) override
    {
        GPainter painter(*this);
        painter.add_clip_rect(event.rect());
        painter.fill_rect(event.rect(), Color::Black);
        int i = m_cpu_history.capacity() - m_cpu_history.size();
        for (auto cpu_usage : m_cpu_history) {
            painter.draw_line(
                { i, rect().bottom() },
                { i, (int)(height() - (cpu_usage * (float)height())) },
                Color::from_rgb(0xaa6d4b));
            ++i;
        }
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

    CircularQueue<float, 30> m_cpu_history;
    unsigned m_last_busy { 0 };
    unsigned m_last_idle { 0 };
};

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto window = GWindow::construct();
    window->set_window_type(GWindowType::MenuApplet);
    window->resize(30, 16);

    auto widget = GraphWidget::construct();
    window->set_main_widget(widget);
    window->show();
    return app.exec();
}
