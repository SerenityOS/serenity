/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DevicesModel.h"
#include "GraphWidget.h"
#include "InterruptsWidget.h"
#include "MemoryStatsWidget.h"
#include "NetworkStatisticsWidget.h"
#include "ProcessFileDescriptorMapWidget.h"
#include "ProcessMemoryMapWidget.h"
#include "ProcessModel.h"
#include "ProcessStateWidget.h"
#include "ProcessUnveiledPathsWidget.h"
#include "ThreadStackWidget.h"
#include <AK/NumberFormat.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Timer.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/Icon.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/LazyWidget.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Painter.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/StackWidget.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TableView.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <LibPCIDB/Database.h>
#include <serenity.h>
#include <signal.h>
#include <spawn.h>
#include <stdio.h>
#include <unistd.h>

static NonnullRefPtr<GUI::Window> build_process_window(pid_t);
static NonnullRefPtr<GUI::Widget> build_file_systems_tab();
static NonnullRefPtr<GUI::Widget> build_pci_devices_tab();
static NonnullRefPtr<GUI::Widget> build_devices_tab();
static NonnullRefPtr<GUI::Widget> build_graphs_tab();
static NonnullRefPtr<GUI::Widget> build_processors_tab();

static RefPtr<GUI::Statusbar> statusbar;

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
    {
        // Before we do anything else, boost our process priority to the maximum allowed.
        // It's very frustrating when the system is bogged down under load and you just want
        // System Monitor to work.
        sched_param param {
            .sched_priority = THREAD_PRIORITY_MAX,
        };
        sched_setparam(0, &param);
    }

    if (pledge("stdio thread proc recvfd sendfd rpath exec unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

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

    if (unveil("/bin", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/usr/lib", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/bin/Profiler", "rx") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/bin/Inspector", "rx") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    const char* args_tab = "processes";
    Core::ArgsParser parser;
    parser.add_option(args_tab, "Tab, one of 'processes', 'graphs', 'fs', 'pci', 'devices', 'network', 'processors' or 'interrupts'", "open-tab", 't', "tab");
    parser.parse(argc, argv);
    StringView args_tab_view = args_tab;

    auto app_icon = GUI::Icon::default_icon("app-system-monitor");

    auto window = GUI::Window::construct();
    window->set_title("System Monitor");
    window->resize(560, 430);

    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.set_layout<GUI::VerticalBoxLayout>();
    main_widget.set_fill_with_background_color(true);

    // Add a tasteful separating line between the menu and the main UI.
    auto& top_line = main_widget.add<GUI::SeparatorWidget>(Gfx::Orientation::Horizontal);
    top_line.set_fixed_height(2);

    auto& tabwidget_container = main_widget.add<GUI::Widget>();
    tabwidget_container.set_layout<GUI::VerticalBoxLayout>();
    tabwidget_container.layout()->set_margins({ 4, 0, 4, 4 });
    auto& tabwidget = tabwidget_container.add<GUI::TabWidget>();

    statusbar = main_widget.add<GUI::Statusbar>(3);

    auto process_model = ProcessModel::create();
    process_model->on_state_update = [&](int process_count, int thread_count) {
        statusbar->set_text(0, String::formatted("Processes: {}", process_count));
        statusbar->set_text(1, String::formatted("Threads: {}", thread_count));
    };

    auto& process_table_container = tabwidget.add_tab<GUI::Widget>("Processes");

    auto graphs_widget = build_graphs_tab();
    tabwidget.add_widget("Graphs", graphs_widget);

    auto file_systems_widget = build_file_systems_tab();
    tabwidget.add_widget("File systems", file_systems_widget);

    auto pci_devices_widget = build_pci_devices_tab();
    tabwidget.add_widget("PCI devices", pci_devices_widget);

    auto devices_widget = build_devices_tab();
    tabwidget.add_widget("Devices", devices_widget);

    auto network_stats_widget = NetworkStatisticsWidget::construct();
    tabwidget.add_widget("Network", network_stats_widget);

    auto processors_widget = build_processors_tab();
    tabwidget.add_widget("Processors", processors_widget);

    auto interrupts_widget = InterruptsWidget::construct();
    tabwidget.add_widget("Interrupts", interrupts_widget);

    process_table_container.set_layout<GUI::VerticalBoxLayout>();
    process_table_container.layout()->set_margins({ 4, 4, 4, 4 });
    process_table_container.layout()->set_spacing(0);

    auto& process_table_view = process_table_container.add<GUI::TableView>();
    process_table_view.set_column_headers_visible(true);
    process_table_view.set_model(GUI::SortingProxyModel::create(process_model));
    for (auto column = 0; column < ProcessModel::Column::__Count; ++column)
        process_table_view.set_column_visible(column, false);
    process_table_view.set_column_visible(ProcessModel::Column::Icon, true);
    process_table_view.set_column_visible(ProcessModel::Column::PID, true);
    process_table_view.set_column_visible(ProcessModel::Column::Name, true);
    process_table_view.set_column_visible(ProcessModel::Column::CPU, true);
    process_table_view.set_column_visible(ProcessModel::Column::User, true);
    process_table_view.set_column_visible(ProcessModel::Column::Virtual, true);
    process_table_view.set_column_visible(ProcessModel::Column::DirtyPrivate, true);

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

    auto kill_action = GUI::Action::create(
        "&Kill Process", { Mod_Ctrl, Key_K }, Gfx::Bitmap::load_from_file("/res/icons/16x16/kill.png"), [&](const GUI::Action&) {
            pid_t pid = selected_id(ProcessModel::Column::PID);
            if (pid != -1)
                kill(pid, SIGKILL);
        },
        &process_table_view);

    auto stop_action = GUI::Action::create(
        "&Stop Process", { Mod_Ctrl, Key_S }, Gfx::Bitmap::load_from_file("/res/icons/16x16/stop-hand.png"), [&](const GUI::Action&) {
            pid_t pid = selected_id(ProcessModel::Column::PID);
            if (pid != -1)
                kill(pid, SIGSTOP);
        },
        &process_table_view);

    auto continue_action = GUI::Action::create(
        "&Continue Process", { Mod_Ctrl, Key_C }, Gfx::Bitmap::load_from_file("/res/icons/16x16/continue.png"), [&](const GUI::Action&) {
            pid_t pid = selected_id(ProcessModel::Column::PID);
            if (pid != -1)
                kill(pid, SIGCONT);
        },
        &process_table_view);

    auto profile_action = GUI::Action::create(
        "&Profile Process", { Mod_Ctrl, Key_P },
        Gfx::Bitmap::load_from_file("/res/icons/16x16/app-profiler.png"), [&](auto&) {
            pid_t pid = selected_id(ProcessModel::Column::PID);
            if (pid != -1) {
                auto pid_string = String::number(pid);
                pid_t child;
                const char* argv[] = { "/bin/Profiler", "--pid", pid_string.characters(), nullptr };
                if ((errno = posix_spawn(&child, "/bin/Profiler", nullptr, nullptr, const_cast<char**>(argv), environ))) {
                    perror("posix_spawn");
                } else {
                    if (disown(child) < 0)
                        perror("disown");
                }
            }
        },
        &process_table_view);

    HashMap<pid_t, NonnullRefPtr<GUI::Window>> process_windows;

    auto process_properties_action = GUI::CommonActions::make_properties_action(
        [&](auto&) {
            auto pid = selected_id(ProcessModel::Column::PID);
            if (pid == -1)
                return;

            RefPtr<GUI::Window> process_window;
            auto it = process_windows.find(pid);
            if (it == process_windows.end()) {
                process_window = build_process_window(pid);
                process_window->on_close_request = [pid, &process_windows] {
                    process_windows.remove(pid);
                    return GUI::Window::CloseRequestDecision::Close;
                };
                process_windows.set(pid, *process_window);
            } else {
                process_window = it->value;
            }
            process_window->show();
            process_window->move_to_front();
        },
        &process_table_view);

    auto menubar = GUI::Menubar::construct();
    auto& file_menu = menubar->add_menu("&File");
    file_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto process_context_menu = GUI::Menu::construct();
    process_context_menu->add_action(kill_action);
    process_context_menu->add_action(stop_action);
    process_context_menu->add_action(continue_action);
    process_context_menu->add_separator();
    process_context_menu->add_action(profile_action);
    process_context_menu->add_separator();
    process_context_menu->add_action(process_properties_action);
    process_table_view.on_context_menu_request = [&]([[maybe_unused]] const GUI::ModelIndex& index, const GUI::ContextMenuEvent& event) {
        if (index.is_valid())
            process_context_menu->popup(event.screen_position(), process_properties_action);
    };

    auto& frequency_menu = menubar->add_menu("F&requency");
    GUI::ActionGroup frequency_action_group;
    frequency_action_group.set_exclusive(true);

    auto make_frequency_action = [&](int seconds, bool checked = false) {
        auto action = GUI::Action::create_checkable(String::formatted("&{} Sec", seconds), [&refresh_timer, seconds](auto&) {
            refresh_timer.restart(seconds * 1000);
        });
        action->set_status_tip(String::formatted("Refresh every {} seconds", seconds));
        action->set_checked(checked);
        frequency_action_group.add_action(*action);
        frequency_menu.add_action(*action);
    };

    make_frequency_action(1);
    make_frequency_action(3, true);
    make_frequency_action(5);

    auto& help_menu = menubar->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("System Monitor", app_icon, window));

    window->set_menubar(move(menubar));

    process_table_view.on_activation = [&](auto&) {
        if (process_properties_action->is_enabled())
            process_properties_action->activate();
    };

    process_table_view.on_selection_change = [&] {
        pid_t pid = selected_id(ProcessModel::Column::PID);
        bool has_access = can_access_pid(pid);
        kill_action->set_enabled(has_access);
        stop_action->set_enabled(has_access);
        continue_action->set_enabled(has_access);
        profile_action->set_enabled(has_access);
        process_properties_action->set_enabled(has_access);
    };

    app->on_action_enter = [](GUI::Action const& action) {
        statusbar->set_override_text(action.status_tip());
    };
    app->on_action_leave = [](GUI::Action const&) {
        statusbar->set_override_text({});
    };

    window->show();
    window->set_icon(app_icon.bitmap_for_size(16));

    if (args_tab_view == "processes")
        tabwidget.set_active_widget(&process_table_container);
    else if (args_tab_view == "graphs")
        tabwidget.set_active_widget(graphs_widget);
    else if (args_tab_view == "fs")
        tabwidget.set_active_widget(file_systems_widget);
    else if (args_tab_view == "pci")
        tabwidget.set_active_widget(pci_devices_widget);
    else if (args_tab_view == "devices")
        tabwidget.set_active_widget(devices_widget);
    else if (args_tab_view == "network")
        tabwidget.set_active_widget(network_stats_widget);
    else if (args_tab_view == "processors")
        tabwidget.set_active_widget(processors_widget);
    else if (args_tab_view == "interrupts")
        tabwidget.set_active_widget(interrupts_widget);

    return app->exec();
}

class ProgressbarPaintingDelegate final : public GUI::TableCellPaintingDelegate {
public:
    virtual ~ProgressbarPaintingDelegate() override { }

    virtual void paint(GUI::Painter& painter, const Gfx::IntRect& a_rect, const Palette& palette, const GUI::ModelIndex& index) override
    {
        auto rect = a_rect.shrunken(2, 2);
        auto percentage = index.data(GUI::ModelRole::Custom).to_i32();

        auto data = index.data();
        String text;
        if (data.is_string())
            text = data.as_string();
        Gfx::StylePainter::paint_progressbar(painter, rect, palette, 0, 100, percentage, text);
        painter.draw_rect(rect, Color::Black);
    }
};

NonnullRefPtr<GUI::Window> build_process_window(pid_t pid)
{
    auto window = GUI::Window::construct();
    window->resize(480, 360);
    window->set_title(String::formatted("PID {} - System Monitor", pid));

    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();

    auto& hero_container = main_widget.add<GUI::Widget>();
    hero_container.set_shrink_to_fit(true);
    hero_container.set_layout<GUI::HorizontalBoxLayout>();
    hero_container.layout()->set_margins({ 4, 4, 4, 4 });
    hero_container.layout()->set_spacing(8);

    auto& icon_label = hero_container.add<GUI::Label>();
    icon_label.set_fixed_size(32, 32);

    GUI::ModelIndex process_index;
    for (int row = 0; row < ProcessModel::the().row_count({}); ++row) {
        auto index = ProcessModel::the().index(row, ProcessModel::Column::PID);
        if (index.data().to_i32() == pid) {
            process_index = index;
            break;
        }
    }

    VERIFY(process_index.is_valid());
    if (auto icon_data = process_index.sibling_at_column(ProcessModel::Column::Icon).data(); icon_data.is_icon()) {
        icon_label.set_icon(icon_data.as_icon().bitmap_for_size(32));
    }

    auto& process_name_label = hero_container.add<GUI::Label>();
    process_name_label.set_font(Gfx::FontDatabase::default_font().bold_variant());
    process_name_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    process_name_label.set_text(String::formatted("{} (PID {})",
        process_index.sibling_at_column(ProcessModel::Column::Name).data().to_string(),
        pid));

    auto& separator = main_widget.add<GUI::HorizontalSeparator>();
    separator.set_fixed_height(2);

    auto& widget_stack = main_widget.add<GUI::StackWidget>();
    auto& unavailable_process_widget = widget_stack.add<UnavailableProcessWidget>(String::formatted("Unable to access PID {}", pid));

    auto& process_tab_widget = widget_stack.add<GUI::TabWidget>();
    process_tab_widget.add_tab<ProcessStateWidget>("State", pid);
    auto& memory_map_widget = process_tab_widget.add_tab<ProcessMemoryMapWidget>("Memory map");
    auto& open_files_widget = process_tab_widget.add_tab<ProcessFileDescriptorMapWidget>("Open files");
    auto& unveiled_paths_widget = process_tab_widget.add_tab<ProcessUnveiledPathsWidget>("Unveiled paths");
    auto& thread_stack_widget = process_tab_widget.add_tab<ThreadStackWidget>("Stack");

    open_files_widget.set_pid(pid);
    thread_stack_widget.set_ids(pid, pid);
    memory_map_widget.set_pid(pid);
    unveiled_paths_widget.set_pid(pid);

    if (can_access_pid(pid))
        widget_stack.set_active_widget(&process_tab_widget);
    else
        widget_stack.set_active_widget(&unavailable_process_widget);

    return window;
}

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
                size_builder.append(human_readable_size(object.get("total_block_count").to_u64() * object.get("block_size").to_u64()));
                size_builder.append(" ");
                return size_builder.to_string();
            },
            [](const JsonObject& object) {
                return object.get("total_block_count").to_u64() * object.get("block_size").to_u64();
            },
            [](const JsonObject& object) {
                auto total_blocks = object.get("total_block_count").to_u64();
                if (total_blocks == 0)
                    return 0;
                auto free_blocks = object.get("free_block_count").to_u64();
                auto used_blocks = total_blocks - free_blocks;
                int percentage = (static_cast<double>(used_blocks) / static_cast<double>(total_blocks) * 100.0);
                return percentage;
            });
        df_fields.empend(
            "Used", Gfx::TextAlignment::CenterRight,
            [](const JsonObject& object) {
            auto total_blocks = object.get("total_block_count").to_u64();
            auto free_blocks = object.get("free_block_count").to_u64();
            auto used_blocks = total_blocks - free_blocks;
            return human_readable_size(used_blocks * object.get("block_size").to_u64()); },
            [](const JsonObject& object) {
                auto total_blocks = object.get("total_block_count").to_u64();
                auto free_blocks = object.get("free_block_count").to_u64();
                auto used_blocks = total_blocks - free_blocks;
                return used_blocks * object.get("block_size").to_u64();
            });
        df_fields.empend(
            "Available", Gfx::TextAlignment::CenterRight,
            [](const JsonObject& object) {
                return human_readable_size(object.get("free_block_count").to_u64() * object.get("block_size").to_u64());
            },
            [](const JsonObject& object) {
                return object.get("free_block_count").to_u64() * object.get("block_size").to_u64();
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

        fs_table_view.set_column_painting_delegate(3, make<ProgressbarPaintingDelegate>());

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
        if (!db)
            warnln("Couldn't open PCI ID database!");

        Vector<GUI::JsonArrayModel::FieldSpec> pci_fields;
        pci_fields.empend(
            "Address", Gfx::TextAlignment::CenterLeft,
            [](const JsonObject& object) {
                auto seg = object.get("seg").to_u32();
                auto bus = object.get("bus").to_u32();
                auto device = object.get("device").to_u32();
                auto function = object.get("function").to_u32();
                return String::formatted("{:04x}:{:02x}:{:02x}.{}", seg, bus, device, function);
            });
        pci_fields.empend(
            "Class", Gfx::TextAlignment::CenterLeft,
            [db](const JsonObject& object) {
                auto class_id = object.get("class").to_u32();
                String class_name = db ? db->get_class(class_id) : nullptr;
                return class_name.is_empty() ? String::formatted("{:04x}", class_id) : class_name;
            });
        pci_fields.empend(
            "Vendor", Gfx::TextAlignment::CenterLeft,
            [db](const JsonObject& object) {
                auto vendor_id = object.get("vendor_id").to_u32();
                String vendor_name = db ? db->get_vendor(vendor_id) : nullptr;
                return vendor_name.is_empty() ? String::formatted("{:02x}", vendor_id) : vendor_name;
            });
        pci_fields.empend(
            "Device", Gfx::TextAlignment::CenterLeft,
            [db](const JsonObject& object) {
                auto vendor_id = object.get("vendor_id").to_u32();
                auto device_id = object.get("device_id").to_u32();
                String device_name = db ? db->get_device(vendor_id, device_id) : nullptr;
                return device_name.is_empty() ? String::formatted("{:02x}", device_id) : device_name;
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
    auto graphs_container = GUI::Widget::construct();

    graphs_container->set_fill_with_background_color(true);
    graphs_container->set_background_role(ColorRole::Button);
    graphs_container->set_layout<GUI::VerticalBoxLayout>();
    graphs_container->layout()->set_margins({ 4, 4, 4, 4 });

    auto& cpu_graph_group_box = graphs_container->add<GUI::GroupBox>("CPU usage");
    cpu_graph_group_box.set_layout<GUI::HorizontalBoxLayout>();
    cpu_graph_group_box.layout()->set_margins({ 6, 16, 6, 6 });
    cpu_graph_group_box.set_fixed_height(120);
    Vector<GraphWidget&> cpu_graphs;
    for (size_t i = 0; i < ProcessModel::the().cpus().size(); i++) {
        auto& cpu_graph = cpu_graph_group_box.add<GraphWidget>();
        cpu_graph.set_max(100);
        cpu_graph.set_value_format(0, {
                                          .graph_color_role = ColorRole::SyntaxPreprocessorStatement,
                                          .text_formatter = [](int value) {
                                              return String::formatted("Total: {}%", value);
                                          },
                                      });
        cpu_graph.set_value_format(1, {
                                          .graph_color_role = ColorRole::SyntaxPreprocessorValue,
                                          .text_formatter = [](int value) {
                                              return String::formatted("Kernel: {}%", value);
                                          },
                                      });
        cpu_graphs.append(cpu_graph);
    }
    ProcessModel::the().on_cpu_info_change = [cpu_graphs](const NonnullOwnPtrVector<ProcessModel::CpuInfo>& cpus) {
        float sum_cpu = 0;
        for (size_t i = 0; i < cpus.size(); ++i) {
            cpu_graphs[i].add_value({ (int)cpus[i].total_cpu_percent, (int)cpus[i].total_cpu_percent_kernel });
            sum_cpu += cpus[i].total_cpu_percent;
        }
        float cpu_usage = sum_cpu / (float)cpus.size();
        statusbar->set_text(2, String::formatted("CPU usage: {}%", (int)roundf(cpu_usage)));
    };

    auto& memory_graph_group_box = graphs_container->add<GUI::GroupBox>("Memory usage");
    memory_graph_group_box.set_layout<GUI::VerticalBoxLayout>();
    memory_graph_group_box.layout()->set_margins({ 6, 16, 6, 6 });
    memory_graph_group_box.set_fixed_height(120);
    auto& memory_graph = memory_graph_group_box.add<GraphWidget>();
    memory_graph.set_stack_values(true);
    memory_graph.set_value_format(0, {
                                         .graph_color_role = ColorRole::SyntaxComment,
                                         .text_formatter = [&memory_graph](int value) {
                                             return String::formatted("Committed: {} KiB", value);
                                         },
                                     });
    memory_graph.set_value_format(1, {
                                         .graph_color_role = ColorRole::SyntaxPreprocessorStatement,
                                         .text_formatter = [&memory_graph](int value) {
                                             return String::formatted("Allocated: {} KiB", value);
                                         },
                                     });
    memory_graph.set_value_format(2, {
                                         .graph_color_role = ColorRole::SyntaxPreprocessorValue,
                                         .text_formatter = [&memory_graph](int value) {
                                             return String::formatted("Kernel heap: {} KiB", value);
                                         },
                                     });

    graphs_container->add<MemoryStatsWidget>(memory_graph);
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
