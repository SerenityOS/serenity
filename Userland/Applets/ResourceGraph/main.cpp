/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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

#include <AK/ByteBuffer.h>
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
    static constexpr size_t history_size = 24;

    GraphWidget(GraphType graph_type, Optional<Gfx::Color> graph_color, Optional<Gfx::Color> graph_error_color)
        : m_graph_type(graph_type)
    {
        set_frame_thickness(1);
        m_graph_color = graph_color.value_or(palette().menu_selection());
        m_graph_error_color = graph_error_color.value_or(Color::Red);
        start_timer(1000);
    }

private:
    virtual void timer_event(Core::TimerEvent&) override
    {
        switch (m_graph_type) {
        case GraphType::CPU: {
            unsigned busy;
            unsigned idle;
            if (get_cpu_usage(busy, idle)) {
                unsigned busy_diff = busy - m_last_cpu_busy;
                unsigned idle_diff = idle - m_last_cpu_idle;
                m_last_cpu_busy = busy;
                m_last_cpu_idle = idle;
                float cpu = (float)busy_diff / (float)(busy_diff + idle_diff);
                m_history.enqueue(cpu);
                m_tooltip = String::format("CPU usage: %.1f%%", 100 * cpu);
            } else {
                m_history.enqueue(-1);
                m_tooltip = StringView("Unable to determine CPU usage");
            }
            break;
        }
        case GraphType::Memory: {
            u64 allocated, available;
            if (get_memory_usage(allocated, available)) {
                double total_memory = allocated + available;
                double memory = (double)allocated / total_memory;
                m_history.enqueue(memory);
                m_tooltip = String::format("Memory: %.1f MiB of %.1f MiB in use", (float)(allocated / MiB), (float)(total_memory / MiB));
            } else {
                m_history.enqueue(-1);
                m_tooltip = StringView("Unable to determine memory usage");
            }
            break;
        }
        default:
            VERIFY_NOT_REACHED();
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
            if (value >= 0) {
                painter.draw_line(
                    { rect.x() + i, rect.bottom() },
                    { rect.x() + i, rect.top() + (int)(round(rect.height() - (value * rect.height()))) },
                    m_graph_color);
            } else {
                painter.draw_line(
                    { rect.x() + i, rect.top() },
                    { rect.x() + i, rect.bottom() },
                    m_graph_error_color);
            }
            ++i;
        }
    }

    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        if (event.button() != GUI::MouseButton::Left)
            return;
        pid_t child_pid;
        const char* argv[] = { "SystemMonitor", "-t", "graphs", nullptr };
        if ((errno = posix_spawn(&child_pid, "/bin/SystemMonitor", nullptr, nullptr, const_cast<char**>(argv), environ))) {
            perror("posix_spawn");
        } else {
            if (disown(child_pid) < 0)
                perror("disown");
        }
    }

    bool get_cpu_usage(unsigned& busy, unsigned& idle)
    {
        busy = 0;
        idle = 0;

        auto all_processes = Core::ProcessStatisticsReader::get_all(m_proc_all);
        if (!all_processes.has_value() || all_processes.value().is_empty())
            return false;

        for (auto& it : all_processes.value()) {
            for (auto& jt : it.value.threads) {
                if (it.value.pid == 0)
                    idle += jt.ticks_user + jt.ticks_kernel;
                else
                    busy += jt.ticks_user + jt.ticks_kernel;
            }
        }
        return true;
    }

    bool get_memory_usage(u64& allocated, u64& available)
    {
        if (m_proc_mem) {
            // Seeking to the beginning causes a data refresh!
            if (!m_proc_mem->seek(0, Core::File::SeekMode::SetPosition))
                return false;
        } else {
            auto proc_memstat = Core::File::construct("/proc/memstat");
            if (!proc_memstat->open(Core::IODevice::OpenMode::ReadOnly))
                return false;
            m_proc_mem = move(proc_memstat);
        }

        auto file_contents = m_proc_mem->read_all();
        auto json = JsonValue::from_string(file_contents);
        VERIFY(json.has_value());
        auto& obj = json.value().as_object();
        unsigned kmalloc_allocated = obj.get("kmalloc_allocated").to_u32();
        unsigned kmalloc_available = obj.get("kmalloc_available").to_u32();
        unsigned user_physical_allocated = obj.get("user_physical_allocated").to_u32();
        unsigned user_physical_committed = obj.get("user_physical_committed").to_u32();
        unsigned user_physical_uncommitted = obj.get("user_physical_uncommitted").to_u32();
        unsigned kmalloc_bytes_total = kmalloc_allocated + kmalloc_available;
        unsigned kmalloc_pages_total = (kmalloc_bytes_total + PAGE_SIZE - 1) / PAGE_SIZE;
        unsigned total_userphysical_and_swappable_pages = kmalloc_pages_total + user_physical_allocated + user_physical_committed + user_physical_uncommitted;
        allocated = kmalloc_allocated + ((u64)(user_physical_allocated + user_physical_committed) * PAGE_SIZE);
        available = (u64)(total_userphysical_and_swappable_pages * PAGE_SIZE) - allocated;
        return true;
    }

    GraphType m_graph_type;
    Gfx::Color m_graph_color;
    Gfx::Color m_graph_error_color;
    CircularQueue<float, history_size> m_history;
    unsigned m_last_cpu_busy { 0 };
    unsigned m_last_cpu_idle { 0 };
    String m_tooltip;
    RefPtr<Core::File> m_proc_all;
    RefPtr<Core::File> m_proc_mem;
};

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd accept proc exec rpath unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio recvfd sendfd accept proc exec rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* cpu = nullptr;
    const char* memory = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_option(cpu, "Create CPU graph", "cpu", 'C', "cpu");
    args_parser.add_option(memory, "Create memory graph", "memory", 'M', "memory");
    args_parser.parse(argc, argv);

    if (!cpu && !memory) {
        printf("At least one of --cpu or --memory must be used");
        return 1;
    }

    NonnullRefPtrVector<GUI::Window> applet_windows;

    auto create_applet = [&](GraphType graph_type, StringView spec) {
        auto parts = spec.split_view(',');

        dbgln("Create applet: {} with spec '{}'", (int)graph_type, spec);

        if (parts.size() != 2)
            return;

        auto name = parts[0];
        auto graph_color = Gfx::Color::from_string(parts[1]);

        auto window = GUI::Window::construct();
        window->set_title(name);
        window->set_window_type(GUI::WindowType::Applet);
        window->resize(GraphWidget::history_size + 2, 15);

        window->set_main_widget<GraphWidget>(graph_type, graph_color, Optional<Gfx::Color> {});
        window->show();
        applet_windows.append(move(window));
    };

    if (cpu)
        create_applet(GraphType::CPU, cpu);
    if (memory)
        create_applet(GraphType::Memory, memory);

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
