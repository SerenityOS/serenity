/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CircularQueue.h>
#include <AK/JsonObject.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>
#include <LibMain/Main.h>
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

private:
    GraphWidget(GraphType graph_type, Optional<Gfx::Color> graph_color, Optional<Gfx::Color> graph_error_color)
        : m_graph_type(graph_type)
    {
        set_frame_thickness(1);
        m_graph_color = graph_color.value_or(palette().menu_selection());
        m_graph_error_color = graph_error_color.value_or(Color::Red);
        start_timer(1000);
    }

    virtual void timer_event(Core::TimerEvent&) override
    {
        switch (m_graph_type) {
        case GraphType::CPU: {
            u64 total, idle;
            if (get_cpu_usage(total, idle)) {
                auto total_diff = total - m_last_total;
                m_last_total = total;
                auto idle_diff = idle - m_last_idle;
                m_last_idle = idle;
                float cpu = total_diff > 0 ? (float)(total_diff - idle_diff) / (float)total_diff : 0;
                m_history.enqueue(cpu);
                m_tooltip = String::formatted("CPU usage: {:.1}%", 100 * cpu);
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
                m_tooltip = String::formatted("Memory: {} MiB of {:.1} MiB in use", allocated / MiB, total_memory / MiB);
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
                    { rect.x() + i, rect.top() + (int)(roundf(rect.height() - (value * rect.height()))) },
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
        if (event.button() != GUI::MouseButton::Primary)
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

    bool get_cpu_usage(u64& total, u64& idle)
    {
        total = 0;
        idle = 0;

        if (m_proc_stat) {
            // Seeking to the beginning causes a data refresh!
            if (!m_proc_stat->seek(0, Core::SeekMode::SetPosition))
                return false;
        } else {
            auto proc_stat = Core::File::construct("/proc/stat");
            if (!proc_stat->open(Core::OpenMode::ReadOnly))
                return false;
            m_proc_stat = move(proc_stat);
        }

        auto file_contents = m_proc_stat->read_all();
        auto json_or_error = JsonValue::from_string(file_contents);
        if (json_or_error.is_error())
            return false;
        auto json = json_or_error.release_value();
        auto const& obj = json.as_object();
        total = obj.get("total_time").to_u64();
        idle = obj.get("idle_time").to_u64();
        return true;
    }

    bool get_memory_usage(u64& allocated, u64& available)
    {
        if (m_proc_mem) {
            // Seeking to the beginning causes a data refresh!
            if (!m_proc_mem->seek(0, Core::SeekMode::SetPosition))
                return false;
        } else {
            auto proc_memstat = Core::File::construct("/proc/memstat");
            if (!proc_memstat->open(Core::OpenMode::ReadOnly))
                return false;
            m_proc_mem = move(proc_memstat);
        }

        auto file_contents = m_proc_mem->read_all();
        auto json_or_error = JsonValue::from_string(file_contents);
        if (json_or_error.is_error())
            return false;
        auto json = json_or_error.release_value();
        auto const& obj = json.as_object();
        unsigned kmalloc_allocated = obj.get("kmalloc_allocated").to_u32();
        unsigned kmalloc_available = obj.get("kmalloc_available").to_u32();
        auto user_physical_allocated = obj.get("user_physical_allocated").to_u64();
        auto user_physical_committed = obj.get("user_physical_committed").to_u64();
        auto user_physical_uncommitted = obj.get("user_physical_uncommitted").to_u64();
        unsigned kmalloc_bytes_total = kmalloc_allocated + kmalloc_available;
        unsigned kmalloc_pages_total = (kmalloc_bytes_total + PAGE_SIZE - 1) / PAGE_SIZE;
        u64 total_userphysical_and_swappable_pages = kmalloc_pages_total + user_physical_allocated + user_physical_committed + user_physical_uncommitted;
        allocated = kmalloc_allocated + ((user_physical_allocated + user_physical_committed) * PAGE_SIZE);
        available = (total_userphysical_and_swappable_pages * PAGE_SIZE) - allocated;
        return true;
    }

    GraphType m_graph_type;
    Gfx::Color m_graph_color;
    Gfx::Color m_graph_error_color;
    CircularQueue<float, history_size> m_history;
    u64 m_last_idle { 0 };
    u64 m_last_total { 0 };
    String m_tooltip;
    RefPtr<Core::File> m_proc_stat;
    RefPtr<Core::File> m_proc_mem;
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd proc exec rpath unix"));

    auto app = GUI::Application::construct(arguments);

    TRY(Core::System::pledge("stdio recvfd sendfd proc exec rpath"));

    const char* cpu = nullptr;
    const char* memory = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_option(cpu, "Create CPU graph", "cpu", 'C', "cpu");
    args_parser.add_option(memory, "Create memory graph", "memory", 'M', "memory");
    args_parser.parse(arguments);

    if (!cpu && !memory) {
        printf("At least one of --cpu or --memory must be used");
        return 1;
    }

    NonnullRefPtrVector<GUI::Window> applet_windows;

    auto create_applet = [&](GraphType graph_type, StringView spec) -> ErrorOr<void> {
        auto parts = spec.split_view(',');

        dbgln("Create applet: {} with spec '{}'", (int)graph_type, spec);

        if (parts.size() != 2)
            return Error::from_string_literal("ResourceGraph: Applet spec is not composed of exactly 2 comma-separated parts"sv);

        auto name = parts[0];
        auto graph_color = Gfx::Color::from_string(parts[1]);

        auto window = GUI::Window::construct();
        window->set_title(name);
        window->set_window_type(GUI::WindowType::Applet);
        window->resize(GraphWidget::history_size + 2, 15);

        auto graph_widget = TRY(window->try_set_main_widget<GraphWidget>(graph_type, graph_color, Optional<Gfx::Color> {}));
        window->show();
        applet_windows.append(move(window));

        return {};
    };

    if (cpu)
        TRY(create_applet(GraphType::CPU, cpu));
    if (memory)
        TRY(create_applet(GraphType::Memory, memory));

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/proc/stat", "r"));
    TRY(Core::System::unveil("/proc/memstat", "r"));
    TRY(Core::System::unveil("/bin/SystemMonitor", "x"));
    TRY(Core::System::unveil(nullptr, nullptr));

    return app->exec();
}
