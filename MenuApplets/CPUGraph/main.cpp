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

#include <AK/CircularQueue.h>
#include <LibCore/CProcessStatisticsReader.h>
#include <LibDraw/Palette.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>

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
    virtual void timer_event(Core::TimerEvent&) override
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
                palette().menu_selection());
            ++i;
        }
    }

    virtual void mousedown_event(GMouseEvent& event) override
    {
        if (event.button() != GMouseButton::Left)
            return;
        pid_t pid = fork();
        if (pid < 0) {
            perror("fork");
        } else if (pid == 0) {
            execl("/bin/SystemMonitor", "SystemMonitor", nullptr);
            perror("execl");
            ASSERT_NOT_REACHED();
        }
    }

    static void get_cpu_usage(unsigned& busy, unsigned& idle)
    {
        busy = 0;
        idle = 0;

        auto all_processes = Core::ProcessStatisticsReader::get_all();

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
    if (pledge("stdio shared_buffer accept proc exec rpath unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GApplication app(argc, argv);

    if (pledge("stdio shared_buffer accept proc exec rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto window = GWindow::construct();
    window->set_window_type(GWindowType::MenuApplet);
    window->resize(30, 16);

    auto widget = GraphWidget::construct();
    window->set_main_widget(widget);
    window->show();

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    // FIXME: This is required by Core::ProcessStatisticsReader.
    //        It would be good if we didn't depend on that.
    if (unveil("/etc/passwd", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/proc/all", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/bin/SystemMonitor", "x") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    return app.exec();
}
