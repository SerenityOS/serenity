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

#include "DevicesModel.h"
#include "GraphWidget.h"
#include "MemoryStatsWidget.h"
#include "NetworkStatisticsWidget.h"
#include "ProcessFileDescriptorMapWidget.h"
#include "ProcessMemoryMapWidget.h"
#include "ProcessModel.h"
#include "ProcessUnveiledPathsWidget.h"
#include "ThreadStackWidget.h"
#include <AK/NumberFormat.h>
#include <LibCore/Timer.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/LazyWidget.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/Painter.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TableView.h>
#include <LibGUI/ToolBar.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>
#include <LibPCIDB/Database.h>
#include <serenity.h>
#include <signal.h>
#include <spawn.h>
#include <stdio.h>
#include <unistd.h>

static NonnullRefPtr<GUI::Widget> build_file_systems_tab();
static NonnullRefPtr<GUI::Widget> build_pci_devices_tab();
static NonnullRefPtr<GUI::Widget> build_devices_tab();
static NonnullRefPtr<GUI::Widget> build_graphs_tab();
static NonnullRefPtr<GUI::Widget> build_processors_tab();

class UnavailableProcessWidget final : public GUI::Frame {
    C_OBJECT(UnavailableProcessWidget)
public:
    virtual ~UnavailableProcessWidget() override { }

    const String& text() const { return m_text; }
    void set_text(String text) { m_text = move(text); }

private:
    UnavailableProcessWidget(String text)
        : m_text(move(text))
    {
    }

    virtual void paint_event(GUI::PaintEvent& event) override
    {
        Frame::paint_event(event);
        if (text().is_empty())
            return;
        GUI::Painter painter(*this);
        painter.add_clip_rect(event.rect());
        painter.draw_text(frame_inner_rect(), text(), Gfx::TextAlignment::Center, palette().window_text(), Gfx::TextElision::Right);
    }

    String m_text;
};

static bool can_access_pid(pid_t pid)
{
    auto path = String::formatted("/proc/{}", pid);
    return access(path.characters(), X_OK) == 0;
}

int main(int argc, char** argv)
{
    if (pledge("stdio proc shared_buffer accept rpath exec unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio proc shared_buffer accept rpath exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/etc/passwd", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/proc", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/dev", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/bin/Profiler", "x") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/bin/Inspector", "x") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    auto window = GUI::Window::construct();
    window->set_title("System Monitor");
    window->resize(680, 400);

    auto& keeper = window->set_main_widget<GUI::Widget>();
    keeper.set_layout<GUI::VerticalBoxLayout>();
    keeper.set_fill_with_background_color(true);
    keeper.layout()->set_margins({ 2, 2, 2, 2 });

    auto& tabwidget = keeper.add<GUI::TabWidget>();

    auto& process_container_splitter = tabwidget.add_tab<GUI::VerticalSplitter>("Processes");
    process_container_splitter.layout()->set_margins({ 4, 4, 4, 4 });

    auto& process_table_container = process_container_splitter.add<GUI::Widget>();

    tabwidget.add_widget("Graphs", build_graphs_tab());

    tabwidget.add_widget("File systems", build_file_systems_tab());

    tabwidget.add_widget("PCI devices", build_pci_devices_tab());

    tabwidget.add_widget("Devices", build_devices_tab());

    auto network_stats_widget = NetworkStatisticsWidget::construct();
    tabwidget.add_widget("Network", network_stats_widget);

    tabwidget.add_widget("Processors", build_processors_tab());

    process_table_container.set_layout<GUI::VerticalBoxLayout>();
    process_table_container.layout()->set_spacing(0);

    auto& process_table_view = process_table_container.add<GUI::TableView>();
    process_table_view.set_column_headers_visible(true);
    process_table_view.set_model(GUI::SortingProxyModel::create(ProcessModel::create()));
    process_table_view.set_key_column_and_sort_order(ProcessModel::Column::CPU, GUI::SortOrder::Descending);
    process_table_view.model()->update();

    auto& refresh_timer = window->add<Core::Timer>(
        3000, [&] {
            process_table_view.model()->update();
            if (auto* memory_stats_widget = MemoryStatsWidget::the())
                memory_stats_widget->refresh();
        });

    auto selected_id = [&](ProcessModel::Column column) -> pid_t {
        if (process_table_view.selection().is_empty())
            return -1;
        auto pid_index = process_table_view.model()->index(process_table_view.selection().first().row(), column);
        return pid_index.data().to_i32();
    };

    auto kill_action = GUI::Action::create("Kill process", { Mod_Ctrl, Key_K }, Gfx::Bitmap::load_from_file("/res/icons/16x16/kill.png"), [&](const GUI::Action&) {
        pid_t pid = selected_id(ProcessModel::Column::PID);
        if (pid != -1)
            kill(pid, SIGKILL);
    });

    auto stop_action = GUI::Action::create("Stop process", { Mod_Ctrl, Key_S }, Gfx::Bitmap::load_from_file("/res/icons/16x16/stop-hand.png"), [&](const GUI::Action&) {
        pid_t pid = selected_id(ProcessModel::Column::PID);
        if (pid != -1)
            kill(pid, SIGSTOP);
    });

    auto continue_action = GUI::Action::create("Continue process", { Mod_Ctrl, Key_C }, Gfx::Bitmap::load_from_file("/res/icons/16x16/continue.png"), [&](const GUI::Action&) {
        pid_t pid = selected_id(ProcessModel::Column::PID);
        if (pid != -1)
            kill(pid, SIGCONT);
    });

    auto profile_action = GUI::Action::create("Profile process", { Mod_Ctrl, Key_P },
        Gfx::Bitmap::load_from_file("/res/icons/16x16/app-profiler.png"), [&](auto&) {
            pid_t pid = selected_id(ProcessModel::Column::PID);
            if (pid != -1) {
                auto pid_string = String::format("%d", pid);
                pid_t child;
                const char* argv[] = { "/bin/Profiler", "--pid", pid_string.characters(), nullptr };
                if ((errno = posix_spawn(&child, "/bin/Profiler", nullptr, nullptr, const_cast<char**>(argv), environ))) {
                    perror("posix_spawn");
                } else {
                    if (disown(child) < 0)
                        perror("disown");
                }
            }
        });

    auto inspect_action = GUI::Action::create("Inspect process", { Mod_Ctrl, Key_I },
        Gfx::Bitmap::load_from_file("/res/icons/16x16/app-inspector.png"), [&](auto&) {
            pid_t pid = selected_id(ProcessModel::Column::PID);
            if (pid != -1) {
                auto pid_string = String::format("%d", pid);
                pid_t child;
                const char* argv[] = { "/bin/Inspector", pid_string.characters(), nullptr };
                if ((errno = posix_spawn(&child, "/bin/Inspector", nullptr, nullptr, const_cast<char**>(argv), environ))) {
                    perror("posix_spawn");
                } else {
                    if (disown(child) < 0)
                        perror("disown");
                }
            }
        });

    auto menubar = GUI::MenuBar::construct();
    auto& app_menu = menubar->add_menu("System Monitor");
    app_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
        return;
    }));

    auto& process_menu = menubar->add_menu("Process");
    process_menu.add_action(kill_action);
    process_menu.add_action(stop_action);
    process_menu.add_action(continue_action);
    process_menu.add_separator();
    process_menu.add_action(profile_action);
    process_menu.add_action(inspect_action);

    auto process_context_menu = GUI::Menu::construct();
    process_context_menu->add_action(kill_action);
    process_context_menu->add_action(stop_action);
    process_context_menu->add_action(continue_action);
    process_context_menu->add_separator();
    process_context_menu->add_action(profile_action);
    process_context_menu->add_action(inspect_action);
    process_table_view.on_context_menu_request = [&](const GUI::ModelIndex& index, const GUI::ContextMenuEvent& event) {
        (void)index;
        process_context_menu->popup(event.screen_position());
    };

    auto& frequency_menu = menubar->add_menu("Frequency");
    GUI::ActionGroup frequency_action_group;
    frequency_action_group.set_exclusive(true);

    auto make_frequency_action = [&](auto& title, int interval, bool checked = false) {
        auto action = GUI::Action::create_checkable(title, [&refresh_timer, interval](auto&) {
            refresh_timer.restart(interval);
        });
        action->set_checked(checked);
        frequency_action_group.add_action(*action);
        frequency_menu.add_action(*action);
    };

    make_frequency_action("1 sec", 1000);
    make_frequency_action("3 sec", 3000, true);
    make_frequency_action("5 sec", 5000);

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::Action::create("About", [&](const GUI::Action&) {
        GUI::AboutDialog::show("System Monitor", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-system-monitor.png"), window);
    }));

    app->set_menubar(move(menubar));

    auto& process_tab_unused_widget = process_container_splitter.add<UnavailableProcessWidget>("No process selected");
    process_tab_unused_widget.set_visible(true);

    auto& process_tab_widget = process_container_splitter.add<GUI::TabWidget>();
    process_tab_widget.set_tab_position(GUI::TabWidget::TabPosition::Bottom);
    process_tab_widget.set_visible(false);

    auto& memory_map_widget = process_tab_widget.add_tab<ProcessMemoryMapWidget>("Memory map");
    auto& open_files_widget = process_tab_widget.add_tab<ProcessFileDescriptorMapWidget>("Open files");
    auto& unveiled_paths_widget = process_tab_widget.add_tab<ProcessUnveiledPathsWidget>("Unveiled paths");
    auto& stack_widget = process_tab_widget.add_tab<ThreadStackWidget>("Stack");

    process_table_view.on_selection = [&](auto&) {
        auto pid = selected_id(ProcessModel::Column::PID);
        auto tid = selected_id(ProcessModel::Column::TID);
        if (!can_access_pid(pid)) {
            process_tab_widget.set_visible(false);
            process_tab_unused_widget.set_text("Process cannot be accessed");
            process_tab_unused_widget.set_visible(true);
            return;
        }

        process_tab_widget.set_visible(true);
        process_tab_unused_widget.set_visible(false);
        open_files_widget.set_pid(pid);
        stack_widget.set_ids(pid, tid);
        memory_map_widget.set_pid(pid);
        unveiled_paths_widget.set_pid(pid);
    };

    window->show();

    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-system-monitor.png"));

    return app->exec();
}

class ProgressBarPaintingDelegate final : public GUI::TableCellPaintingDelegate {
public:
    virtual ~ProgressBarPaintingDelegate() override { }

    virtual void paint(GUI::Painter& painter, const Gfx::IntRect& a_rect, const Palette& palette, const GUI::ModelIndex& index) override
    {
        auto rect = a_rect.shrunken(2, 2);
        auto percentage = index.data(GUI::ModelRole::Custom).to_i32();

        auto data = index.data();
        String text;
        if (data.is_string())
            text = data.as_string();
        Gfx::StylePainter::paint_progress_bar(painter, rect, palette, 0, 100, percentage, text);
        painter.draw_rect(rect, Color::Black);
    }
};

NonnullRefPtr<GUI::Widget> build_file_systems_tab()
{
    auto fs_widget = GUI::LazyWidget::construct();

    fs_widget->on_first_show = [](GUI::LazyWidget& self) {
        self.set_layout<GUI::VerticalBoxLayout>();
        self.layout()->set_margins({ 4, 4, 4, 4 });
        auto& fs_table_view = self.add<GUI::TableView>();

        Vector<GUI::JsonArrayModel::FieldSpec> df_fields;
        df_fields.empend("mount_point", "Mount point", Gfx::TextAlignment::CenterLeft);
        df_fields.empend("class_name", "Class", Gfx::TextAlignment::CenterLeft);
        df_fields.empend("source", "Source", Gfx::TextAlignment::CenterLeft);
        df_fields.empend(
            "Size", Gfx::TextAlignment::CenterRight,
            [](const JsonObject& object) {
                StringBuilder size_builder;
                size_builder.append(" ");
                size_builder.append(human_readable_size(object.get("total_block_count").to_u32() * object.get("block_size").to_u32()));
                size_builder.append(" ");
                return size_builder.to_string();
            },
            [](const JsonObject& object) {
                return object.get("total_block_count").to_u32() * object.get("block_size").to_u32();
            },
            [](const JsonObject& object) {
                auto total_blocks = object.get("total_block_count").to_u32();
                if (total_blocks == 0)
                    return 0;
                auto free_blocks = object.get("free_block_count").to_u32();
                auto used_blocks = total_blocks - free_blocks;
                int percentage = (int)((float)used_blocks / (float)total_blocks * 100.0f);
                return percentage;
            });
        df_fields.empend(
            "Used", Gfx::TextAlignment::CenterRight,
            [](const JsonObject& object) {
            auto total_blocks = object.get("total_block_count").to_u32();
            auto free_blocks = object.get("free_block_count").to_u32();
            auto used_blocks = total_blocks - free_blocks;
            return human_readable_size(used_blocks * object.get("block_size").to_u32()); },
            [](const JsonObject& object) {
                auto total_blocks = object.get("total_block_count").to_u32();
                auto free_blocks = object.get("free_block_count").to_u32();
                auto used_blocks = total_blocks - free_blocks;
                return used_blocks * object.get("block_size").to_u32();
            });
        df_fields.empend(
            "Available", Gfx::TextAlignment::CenterRight,
            [](const JsonObject& object) {
                return human_readable_size(object.get("free_block_count").to_u32() * object.get("block_size").to_u32());
            },
            [](const JsonObject& object) {
                return object.get("free_block_count").to_u32() * object.get("block_size").to_u32();
            });
        df_fields.empend("Access", Gfx::TextAlignment::CenterLeft, [](const JsonObject& object) {
            bool readonly = object.get("readonly").to_bool();
            int mount_flags = object.get("mount_flags").to_int();
            return readonly || (mount_flags & MS_RDONLY) ? "Read-only" : "Read/Write";
        });
        df_fields.empend("Mount flags", Gfx::TextAlignment::CenterLeft, [](const JsonObject& object) {
            int mount_flags = object.get("mount_flags").to_int();
            StringBuilder builder;
            bool first = true;
            auto check = [&](int flag, const char* name) {
                if (!(mount_flags & flag))
                    return;
                if (!first)
                    builder.append(',');
                builder.append(name);
                first = false;
            };
            check(MS_NODEV, "nodev");
            check(MS_NOEXEC, "noexec");
            check(MS_NOSUID, "nosuid");
            check(MS_BIND, "bind");
            check(MS_RDONLY, "ro");
            if (builder.string_view().is_empty())
                return String("defaults");
            return builder.to_string();
        });
        df_fields.empend("free_block_count", "Free blocks", Gfx::TextAlignment::CenterRight);
        df_fields.empend("total_block_count", "Total blocks", Gfx::TextAlignment::CenterRight);
        df_fields.empend("free_inode_count", "Free inodes", Gfx::TextAlignment::CenterRight);
        df_fields.empend("total_inode_count", "Total inodes", Gfx::TextAlignment::CenterRight);
        df_fields.empend("block_size", "Block size", Gfx::TextAlignment::CenterRight);
        fs_table_view.set_model(GUI::SortingProxyModel::create(GUI::JsonArrayModel::create("/proc/df", move(df_fields))));

        fs_table_view.set_column_painting_delegate(3, make<ProgressBarPaintingDelegate>());

        fs_table_view.model()->update();
    };
    return fs_widget;
}

NonnullRefPtr<GUI::Widget> build_pci_devices_tab()
{
    auto pci_widget = GUI::LazyWidget::construct();

    pci_widget->on_first_show = [](GUI::LazyWidget& self) {
        self.set_layout<GUI::VerticalBoxLayout>();
        self.layout()->set_margins({ 4, 4, 4, 4 });
        auto& pci_table_view = self.add<GUI::TableView>();

        auto db = PCIDB::Database::open();

        Vector<GUI::JsonArrayModel::FieldSpec> pci_fields;
        pci_fields.empend(
            "Address", Gfx::TextAlignment::CenterLeft,
            [](const JsonObject& object) {
                auto seg = object.get("seg").to_u32();
                auto bus = object.get("bus").to_u32();
                auto slot = object.get("slot").to_u32();
                auto function = object.get("function").to_u32();
                return String::formatted("{:04x}:{:02x}:{:02x}.{}", seg, bus, slot, function);
            });
        pci_fields.empend(
            "Class", Gfx::TextAlignment::CenterLeft,
            [db](const JsonObject& object) {
                auto class_id = object.get("class").to_u32();
                String class_name = db->get_class(class_id);
                return class_name == "" ? String::formatted("{:04x}", class_id) : class_name;
            });
        pci_fields.empend(
            "Vendor", Gfx::TextAlignment::CenterLeft,
            [db](const JsonObject& object) {
                auto vendor_id = object.get("vendor_id").to_u32();
                String vendor_name = db->get_vendor(vendor_id);
                return vendor_name == "" ? String::formatted("{:02x}", vendor_id) : vendor_name;
            });
        pci_fields.empend(
            "Device", Gfx::TextAlignment::CenterLeft,
            [db](const JsonObject& object) {
                auto vendor_id = object.get("vendor_id").to_u32();
                auto device_id = object.get("device_id").to_u32();
                String device_name = db->get_device(vendor_id, device_id);
                return device_name == "" ? String::formatted("{:02x}", device_id) : device_name;
            });
        pci_fields.empend(
            "Revision", Gfx::TextAlignment::CenterRight,
            [](const JsonObject& object) {
                auto revision_id = object.get("revision_id").to_u32();
                return String::formatted("{:02x}", revision_id);
            });

        pci_table_view.set_model(GUI::SortingProxyModel::create(GUI::JsonArrayModel::create("/proc/pci", move(pci_fields))));
        pci_table_view.model()->update();
    };

    return pci_widget;
}

NonnullRefPtr<GUI::Widget> build_devices_tab()
{
    auto devices_widget = GUI::LazyWidget::construct();

    devices_widget->on_first_show = [](GUI::LazyWidget& self) {
        self.set_layout<GUI::VerticalBoxLayout>();
        self.layout()->set_margins({ 4, 4, 4, 4 });

        auto& devices_table_view = self.add<GUI::TableView>();
        devices_table_view.set_model(GUI::SortingProxyModel::create(DevicesModel::create()));
        devices_table_view.model()->update();
    };

    return devices_widget;
}

NonnullRefPtr<GUI::Widget> build_graphs_tab()
{
    auto graphs_container = GUI::LazyWidget::construct();

    graphs_container->on_first_show = [](GUI::LazyWidget& self) {
        self.set_fill_with_background_color(true);
        self.set_background_role(ColorRole::Button);
        self.set_layout<GUI::VerticalBoxLayout>();
        self.layout()->set_margins({ 4, 4, 4, 4 });

        auto& cpu_graph_group_box = self.add<GUI::GroupBox>("CPU usage");
        cpu_graph_group_box.set_layout<GUI::HorizontalBoxLayout>();
        cpu_graph_group_box.layout()->set_margins({ 6, 16, 6, 6 });
        cpu_graph_group_box.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
        cpu_graph_group_box.set_preferred_size(0, 120);
        Vector<GraphWidget*> cpu_graphs;
        for (size_t i = 0; i < ProcessModel::the().cpus().size(); i++) {
            auto& cpu_graph = cpu_graph_group_box.add<GraphWidget>();
            cpu_graph.set_max(100);
            cpu_graph.set_text_color(Color::Green);
            cpu_graph.set_graph_color(Color::from_rgb(0x00bb00));
            cpu_graph.text_formatter = [](int value, int) {
                return String::formatted("{}%", value);
            };
            cpu_graphs.append(&cpu_graph);
        }
        ProcessModel::the().on_cpu_info_change = [cpu_graphs](const NonnullOwnPtrVector<ProcessModel::CpuInfo>& cpus) {
            for (size_t i = 0; i < cpus.size(); i++)
                cpu_graphs[i]->add_value(cpus[i].total_cpu_percent);
        };

        auto& memory_graph_group_box = self.add<GUI::GroupBox>("Memory usage");
        memory_graph_group_box.set_layout<GUI::VerticalBoxLayout>();
        memory_graph_group_box.layout()->set_margins({ 6, 16, 6, 6 });
        memory_graph_group_box.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
        memory_graph_group_box.set_preferred_size(0, 120);
        auto& memory_graph = memory_graph_group_box.add<GraphWidget>();
        memory_graph.set_text_color(Color::Cyan);
        memory_graph.set_graph_color(Color::from_rgb(0x00bbbb));
        memory_graph.text_formatter = [](int value, int max) {
            return String::formatted("{} / {} KiB", value, max);
        };

        self.add<MemoryStatsWidget>(memory_graph);
    };
    return graphs_container;
}

NonnullRefPtr<GUI::Widget> build_processors_tab()
{
    auto processors_widget = GUI::LazyWidget::construct();

    processors_widget->on_first_show = [](GUI::LazyWidget& self) {
        self.set_layout<GUI::VerticalBoxLayout>();
        self.layout()->set_margins({ 4, 4, 4, 4 });

        Vector<GUI::JsonArrayModel::FieldSpec> processors_field;
        processors_field.empend("processor", "Processor", Gfx::TextAlignment::CenterRight);
        processors_field.empend("cpuid", "CPUID", Gfx::TextAlignment::CenterLeft);
        processors_field.empend("brandstr", "Brand", Gfx::TextAlignment::CenterLeft);
        processors_field.empend("Features", Gfx::TextAlignment::CenterLeft, [](auto& object) {
            StringBuilder builder;
            auto features = object.get("features").as_array();
            for (auto& feature : features.values()) {
                builder.append(feature.to_string());
                builder.append(' ');
            }
            return GUI::Variant(builder.to_string());
        });
        processors_field.empend("family", "Family", Gfx::TextAlignment::CenterRight);
        processors_field.empend("model", "Model", Gfx::TextAlignment::CenterRight);
        processors_field.empend("stepping", "Stepping", Gfx::TextAlignment::CenterRight);
        processors_field.empend("type", "Type", Gfx::TextAlignment::CenterRight);

        auto& processors_table_view = self.add<GUI::TableView>();
        processors_table_view.set_model(GUI::JsonArrayModel::create("/proc/cpuinfo", move(processors_field)));
        processors_table_view.model()->update();
    };

    return processors_widget;
}
