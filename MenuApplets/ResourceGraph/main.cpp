/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
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
#include <AK/JsonObject.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibGUI/Application.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>
#include <serenity.h>
#include <spawn.h>
#include <stdio.h>

enum class GraphType {
    CPU,
    Memory,
};

class GraphWidget final : public GUI::Frame {
    C_OBJECT(GraphWidget);

public:
    static constexpr size_t history_size = 30;

    GraphWidget(GraphType graph_type, Optional<Gfx::Color> graph_color)
        : m_graph_type(graph_type)
    {
        set_frame_thickness(1);
        m_graph_color = graph_color.value_or(palette().menu_selection());
        start_timer(1000);
    }

private:
    virtual void timer_event(Core::TimerEvent&) override
    {
        switch (m_graph_type) {
        case GraphType::CPU: {
            unsigned busy;
            unsigned idle;
            get_cpu_usage(busy, idle);
            unsigned busy_diff = busy - m_last_cpu_busy;
            unsigned idle_diff = idle - m_last_cpu_idle;
            m_last_cpu_busy = busy;
            m_last_cpu_idle = idle;
            float cpu = (float)busy_diff / (float)(busy_diff + idle_diff);
            m_history.enqueue(cpu);
            m_tooltip = String::format("CPU usage: %.1f%%", 100 * cpu);
            break;
        }
        case GraphType::Memory: {
            unsigned allocated;
            unsigned available;
            get_memory_usage(allocated, available);
            float total_memory = allocated + available;
            float memory = (float)allocated / total_memory;
            m_history.enqueue(memory);
            m_tooltip = String::format("Memory: %.1f MiB of %.1f MiB in use", (float)allocated / MiB, total_memory / MiB);
            break;
        }
        default:
            ASSERT_NOT_REACHED();
        }
        set_tooltip(m_tooltip);
        update();
    }

    virtual void paint_event(GUI::PaintEvent& event) override
    {
        GUI::Frame::paint_event(event);
        GUI::Painter painter(*this);
        painter.add_clip_rect(event.rect());
        painter.add_clip_rect(frame_inner_rect());
        painter.fill_rect(event.rect(), Color::Black);
        int i = m_history.capacity() - m_history.size();
        auto rect = frame_inner_rect();
        for (auto value : m_history) {
            painter.draw_line(
                { rect.x() + i, rect.bottom() },
                { rect.x() + i, rect.top() + (int)(round(rect.height() - (value * rect.height()))) },
                m_graph_color);
            ++i;
        }
    }

    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        if (event.button() != GUI::MouseButton::Left)
            return;
        pid_t child_pid;
        const char* argv[] = { "SystemMonitor", nullptr };
        if ((errno = posix_spawn(&child_pid, "/bin/SystemMonitor", nullptr, nullptr, const_cast<char**>(argv), environ))) {
            perror("posix_spawn");
        } else {
            if (disown(child_pid) < 0)
                perror("disown");
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

    static void get_memory_usage(unsigned& allocated, unsigned& available)
    {
        auto proc_memstat = Core::File::construct("/proc/memstat");
        if (!proc_memstat->open(Core::IODevice::OpenMode::ReadOnly))
            ASSERT_NOT_REACHED();

        auto file_contents = proc_memstat->read_all();
        auto json = JsonValue::from_string(file_contents);
        ASSERT(json.has_value());
        unsigned user_physical_allocated = json.value().as_object().get("user_physical_allocated").to_u32();
        unsigned user_physical_available = json.value().as_object().get("user_physical_available").to_u32();
        allocated = (user_physical_allocated * PAGE_SIZE);
        available = (user_physical_available * PAGE_SIZE);
    }

    GraphType m_graph_type;
    Gfx::Color m_graph_color;
    CircularQueue<float, history_size> m_history;
    unsigned m_last_cpu_busy { 0 };
    unsigned m_last_cpu_idle { 0 };
    String m_tooltip;
};

int main(int argc, char** argv)
{
    if (pledge("stdio shared_buffer accept proc exec rpath unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio shared_buffer accept proc exec rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    bool cpu = false;
    bool memory = false;
    const char* name = nullptr;
    const char* color = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_option(cpu, "Show CPU usage", "cpu", 'C');
    args_parser.add_option(memory, "Show memory usage", "memory", 'M');
    args_parser.add_option(name, "Applet name used by WindowServer.ini to set the applet order", "name", 'n', "name");
    args_parser.add_option(color, "Graph color", "color", 'c', "color");
    args_parser.parse(argc, argv);

    if (!cpu && !memory) {
        printf("Either --cpu or --memory option must be used");
        return 1;
    }
    if (cpu && memory) {
        printf("--cpu and --memory options must not be used together");
        return 1;
    }
    GraphType graph_type;
    if (cpu)
        graph_type = GraphType::CPU;
    if (memory)
        graph_type = GraphType::Memory;

    if (name == nullptr)
        name = "ResourceGraph";

    Optional<Gfx::Color> graph_color;
    if (color != nullptr)
        graph_color = Gfx::Color::from_string(color);

    auto window = GUI::Window::construct();
    window->set_title(name);
    window->set_window_type(GUI::WindowType::MenuApplet);
    window->resize(GraphWidget::history_size + 2, 16);

    window->set_main_widget<GraphWidget>(graph_type, graph_color);
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

    if (unveil("/proc/memstat", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/bin/SystemMonitor", "x") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    return app->exec();
}
