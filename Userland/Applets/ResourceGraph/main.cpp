/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CircularQueue.h>
#include <AK/JsonObject.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Process.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>
#include <LibMain/Main.h>
#include <stdio.h>

enum class GraphType {
    CPU,
    Memory,
    Network,
};

class GraphWidget final : public GUI::Frame {
    C_OBJECT(GraphWidget);

public:
    static constexpr size_t history_size = 24;

private:
    GraphWidget(GraphType graph_type, Optional<Gfx::Color> graph_color, Optional<Gfx::Color> graph_error_color)
        : m_graph_type(graph_type)
    {
        set_frame_style(Gfx::FrameStyle::SunkenPanel);
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
                m_tooltip = MUST(String::formatted("CPU usage: {:.1}%", 100 * cpu));
            } else {
                m_history.enqueue(-1);
                m_tooltip = "Unable to determine CPU usage"_string;
            }
            break;
        }
        case GraphType::Memory: {
            u64 allocated, available;
            if (get_memory_usage(allocated, available)) {
                double total_memory = allocated + available;
                double memory = (double)allocated / total_memory;
                m_history.enqueue(memory);
                m_tooltip = MUST(String::formatted("Memory: {} MiB of {:.1} MiB in use", allocated / MiB, total_memory / MiB));
            } else {
                m_history.enqueue(-1);
                m_tooltip = "Unable to determine memory usage"_string;
            }
            break;
        }
        case GraphType::Network: {
            u64 tx, rx, link_speed;
            if (get_network_usage(tx, rx, link_speed)) {
                u64 recent_tx = tx - m_last_total;
                m_last_total = tx;
                if (recent_tx > m_current_scale) {
                    u64 m_old_scale = m_current_scale;
                    // Scale in multiples of 1000 kB/s
                    m_current_scale = (recent_tx / scale_unit) * scale_unit;
                    rescale_history(m_old_scale, m_current_scale);
                } else {
                    // Figure out if we can scale back down.
                    float max = static_cast<float>(recent_tx) / static_cast<float>(m_current_scale);
                    for (auto const value : m_history) {
                        if (value > max)
                            max = value;
                    }
                    if (max < 0.5f && m_current_scale > scale_unit) {
                        u64 m_old_scale = m_current_scale;
                        m_current_scale = ::max((static_cast<u64>(max * m_current_scale) / scale_unit) * scale_unit, scale_unit);
                        rescale_history(m_old_scale, m_current_scale);
                    }
                }
                m_history.enqueue(static_cast<float>(recent_tx) / static_cast<float>(m_current_scale));
                m_tooltip = MUST(String::formatted("Network: TX {} / RX {} ({:.1} kbit/s)", tx, rx, static_cast<double>(recent_tx) * 8.0 / 1000.0));
            } else {
                m_history.enqueue(-1);
                m_tooltip = "Unable to determine network usage"_string;
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
                    { rect.x() + i, rect.bottom() - 1 },
                    { rect.x() + i, rect.top() + (int)(roundf(rect.height() - (value * rect.height()))) },
                    m_graph_color);
            } else {
                painter.draw_line(
                    { rect.x() + i, rect.top() },
                    { rect.x() + i, rect.bottom() - 1 },
                    m_graph_error_color);
            }
            ++i;
        }
    }

    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        if (event.button() != GUI::MouseButton::Primary)
            return;
        GUI::Process::spawn_or_show_error(window(), "/bin/SystemMonitor"sv, Array { "-t", m_graph_type == GraphType::Network ? "network" : "graphs" });
    }

    ErrorOr<JsonValue> get_data_as_json(OwnPtr<Core::File>& file, StringView filename)
    {
        if (file) {
            // Seeking to the beginning causes a data refresh!
            TRY(file->seek(0, SeekMode::SetPosition));
        } else {
            file = TRY(Core::File::open(filename, Core::File::OpenMode::Read));
        }

        auto file_contents = TRY(file->read_until_eof());
        return TRY(JsonValue::from_string(file_contents));
    }

    bool get_cpu_usage(u64& total, u64& idle)
    {
        total = 0;
        idle = 0;

        auto json = get_data_as_json(m_proc_stat, "/sys/kernel/stats"sv);
        if (json.is_error())
            return false;

        auto const& obj = json.value().as_object();
        total = obj.get_u64("total_time"sv).value_or(0);
        idle = obj.get_u64("idle_time"sv).value_or(0);
        return true;
    }

    bool get_memory_usage(u64& allocated, u64& available)
    {
        auto json = get_data_as_json(m_proc_mem, "/sys/kernel/memstat"sv);
        if (json.is_error())
            return false;

        auto const& obj = json.value().as_object();
        unsigned kmalloc_allocated = obj.get_u32("kmalloc_allocated"sv).value_or(0);
        unsigned kmalloc_available = obj.get_u32("kmalloc_available"sv).value_or(0);
        auto physical_allocated = obj.get_u64("physical_allocated"sv).value_or(0);
        auto physical_committed = obj.get_u64("physical_committed"sv).value_or(0);
        auto physical_uncommitted = obj.get_u64("physical_uncommitted"sv).value_or(0);
        unsigned kmalloc_bytes_total = kmalloc_allocated + kmalloc_available;
        unsigned kmalloc_pages_total = (kmalloc_bytes_total + PAGE_SIZE - 1) / PAGE_SIZE;
        u64 total_userphysical_and_swappable_pages = kmalloc_pages_total + physical_allocated + physical_committed + physical_uncommitted;
        allocated = kmalloc_allocated + ((physical_allocated + physical_committed) * PAGE_SIZE);
        available = (total_userphysical_and_swappable_pages * PAGE_SIZE) - allocated;
        return true;
    }

    bool get_network_usage(u64& tx, u64& rx, u64& link_speed)
    {
        tx = rx = link_speed = 0;

        auto json = get_data_as_json(m_proc_net, "/sys/kernel/net/adapters"sv);
        if (json.is_error())
            return false;

        auto const& array = json.value().as_array();
        for (auto const& adapter_value : array.values()) {
            auto const& adapter_obj = adapter_value.as_object();
            if (!adapter_obj.has_string("ipv4_address"sv) || !adapter_obj.get_bool("link_up"sv).value())
                continue;

            tx += adapter_obj.get_u64("bytes_in"sv).value_or(0);
            rx += adapter_obj.get_u64("bytes_out"sv).value_or(0);
            // Link speed data is given in megabits, but we want all return values to be in bytes.
            link_speed += adapter_obj.get_u64("link_speed"sv).value_or(0) * 8'000'000;
        }
        link_speed /= 8;
        return tx != 0;
    }

    void rescale_history(u64 old_scale, u64 new_scale)
    {
        float factor = static_cast<float>(old_scale) / static_cast<float>(new_scale);
        for (auto& value : m_history)
            value *= factor;
    }

    GraphType m_graph_type;
    Gfx::Color m_graph_color;
    Gfx::Color m_graph_error_color;
    CircularQueue<float, history_size> m_history;
    u64 m_last_idle { 0 };
    u64 m_last_total { 0 };
    static constexpr u64 const scale_unit = 8000;
    u64 m_current_scale { scale_unit };
    String m_tooltip;
    OwnPtr<Core::File> m_proc_stat;
    OwnPtr<Core::File> m_proc_mem;
    OwnPtr<Core::File> m_proc_net;
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd proc exec rpath unix"));

    auto app = TRY(GUI::Application::create(arguments));

    TRY(Core::System::pledge("stdio recvfd sendfd proc exec rpath"));

    StringView cpu {};
    StringView memory {};
    StringView network {};
    Core::ArgsParser args_parser;
    args_parser.add_option(cpu, "Create CPU graph", "cpu", 'C', "cpu");
    args_parser.add_option(memory, "Create memory graph", "memory", 'M', "memory");
    args_parser.add_option(network, "Create network graph", "network", 'N', "network");
    args_parser.parse(arguments);

    if (cpu.is_empty() && memory.is_empty() && network.is_empty()) {
        printf("At least one of --cpu, --memory, or --network must be used");
        return 1;
    }

    Vector<NonnullRefPtr<GUI::Window>> applet_windows;

    auto create_applet = [&](GraphType graph_type, StringView spec) -> ErrorOr<void> {
        auto parts = spec.split_view(',');

        dbgln("Create applet: {} with spec '{}'", (int)graph_type, spec);

        if (parts.size() != 2)
            return Error::from_string_literal("ResourceGraph: Applet spec is not composed of exactly 2 comma-separated parts");

        auto name = parts[0];
        auto graph_color = Gfx::Color::from_string(parts[1]);

        auto window = GUI::Window::construct();
        window->set_title(name);
        window->set_window_type(GUI::WindowType::Applet);
        window->resize(GraphWidget::history_size + 2, 15);

        auto graph_widget = window->set_main_widget<GraphWidget>(graph_type, graph_color, Optional<Gfx::Color> {});
        window->show();
        applet_windows.append(move(window));

        return {};
    };

    if (!cpu.is_empty())
        TRY(create_applet(GraphType::CPU, cpu));
    if (!memory.is_empty())
        TRY(create_applet(GraphType::Memory, memory));
    if (!network.is_empty())
        TRY(create_applet(GraphType::Network, network));

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/sys/kernel/stats", "r"));
    TRY(Core::System::unveil("/sys/kernel/memstat", "r"));
    TRY(Core::System::unveil("/sys/kernel/net/adapters", "r"));
    TRY(Core::System::unveil("/bin/SystemMonitor", "x"));
    TRY(Core::System::unveil(nullptr, nullptr));

    return app->exec();
}
