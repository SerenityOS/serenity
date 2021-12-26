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
#include "ProcessStacksWidget.h"
#include "ProcessTableView.h"
#include "ProcessUnveiledPathsWidget.h"
#include <LibCore/CTimer.h>
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GActionGroup.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GGroupBox.h>
#include <LibGUI/GJsonArrayModel.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GLazyWidget.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GSortingProxyModel.h>
#include <LibGUI/GSplitter.h>
#include <LibGUI/GTabWidget.h>
#include <LibGUI/GToolBar.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <LibPCIDB/Database.h>
#include <signal.h>
#include <stdio.h>
#include <unistd.h>

static String human_readable_size(u32 size)
{
    if (size < (64 * KB))
        return String::format("%u", size);
    if (size < MB)
        return String::format("%u KB", size / KB);
    if (size < GB)
        return String::format("%u MB", size / MB);
    return String::format("%u GB", size / GB);
}

static RefPtr<GWidget> build_file_systems_tab();
static RefPtr<GWidget> build_pci_devices_tab();
static RefPtr<GWidget> build_devices_tab();
static NonnullRefPtr<GWidget> build_graphs_tab();

int main(int argc, char** argv)
{
    if (pledge("stdio proc shared_buffer accept rpath unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GApplication app(argc, argv);

    if (pledge("stdio proc shared_buffer accept rpath", nullptr) < 0) {
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

    unveil(nullptr, nullptr);

    auto keeper = GWidget::construct();
    keeper->set_layout(make<GBoxLayout>(Orientation::Vertical));
    keeper->set_fill_with_background_color(true);
    keeper->layout()->set_margins({ 4, 4, 4, 4 });

    auto tabwidget = GTabWidget::construct(keeper);

    auto process_container_splitter = GSplitter::construct(Orientation::Vertical, nullptr);
    tabwidget->add_widget("Processes", process_container_splitter);

    auto process_table_container = GWidget::construct(process_container_splitter.ptr());

    tabwidget->add_widget("Graphs", build_graphs_tab());

    tabwidget->add_widget("File systems", build_file_systems_tab());

    tabwidget->add_widget("PCI devices", build_pci_devices_tab());

    tabwidget->add_widget("Devices", build_devices_tab());

    auto network_stats_widget = NetworkStatisticsWidget::construct(nullptr);
    tabwidget->add_widget("Network", network_stats_widget);

    process_table_container->set_layout(make<GBoxLayout>(Orientation::Vertical));
    process_table_container->layout()->set_margins({ 4, 0, 4, 4 });
    process_table_container->layout()->set_spacing(0);

    auto toolbar = GToolBar::construct(process_table_container);
    toolbar->set_has_frame(false);
    auto process_table_view = ProcessTableView::construct(process_table_container);

    auto refresh_timer = CTimer::construct(1000, [&] {
        process_table_view->refresh();
        if (auto* memory_stats_widget = MemoryStatsWidget::the())
            memory_stats_widget->refresh();
    });

    auto kill_action = GAction::create("Kill process", { Mod_Ctrl, Key_K }, GraphicsBitmap::load_from_file("/res/icons/kill16.png"), [process_table_view](const GAction&) {
        pid_t pid = process_table_view->selected_pid();
        if (pid != -1)
            kill(pid, SIGKILL);
    });

    auto stop_action = GAction::create("Stop process", { Mod_Ctrl, Key_S }, GraphicsBitmap::load_from_file("/res/icons/stop16.png"), [process_table_view](const GAction&) {
        pid_t pid = process_table_view->selected_pid();
        if (pid != -1)
            kill(pid, SIGSTOP);
    });

    auto continue_action = GAction::create("Continue process", { Mod_Ctrl, Key_C }, GraphicsBitmap::load_from_file("/res/icons/continue16.png"), [process_table_view](const GAction&) {
        pid_t pid = process_table_view->selected_pid();
        if (pid != -1)
            kill(pid, SIGCONT);
    });

    toolbar->add_action(kill_action);
    toolbar->add_action(stop_action);
    toolbar->add_action(continue_action);

    auto window = GWindow::construct();
    window->set_title("System Monitor");
    window->set_rect(20, 200, 680, 400);
    window->set_main_widget(keeper);

    auto menubar = make<GMenuBar>();
    auto app_menu = GMenu::construct("System Monitor");
    app_menu->add_action(GCommonActions::make_quit_action([](auto&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto process_menu = GMenu::construct("Process");
    process_menu->add_action(kill_action);
    process_menu->add_action(stop_action);
    process_menu->add_action(continue_action);
    menubar->add_menu(move(process_menu));

    auto process_context_menu = GMenu::construct();
    process_context_menu->add_action(kill_action);
    process_context_menu->add_action(stop_action);
    process_context_menu->add_action(continue_action);
    process_table_view->on_context_menu_request = [&](const GModelIndex& index, const GContextMenuEvent& event) {
        (void)index;
        process_context_menu->popup(event.screen_position());
    };

    auto frequency_menu = GMenu::construct("Frequency");
    GActionGroup frequency_action_group;
    frequency_action_group.set_exclusive(true);

    auto make_frequency_action = [&](auto& title, int interval, bool checked = false) {
        auto action = GAction::create(title, [&refresh_timer, interval](auto& action) {
            refresh_timer->restart(interval);
            action.set_checked(true);
        });
        action->set_checkable(true);
        action->set_checked(checked);
        frequency_action_group.add_action(*action);
        frequency_menu->add_action(*action);
    };

    make_frequency_action("0.25 sec", 250);
    make_frequency_action("0.5 sec", 500);
    make_frequency_action("1 sec", 1000, true);
    make_frequency_action("3 sec", 3000);
    make_frequency_action("5 sec", 5000);

    menubar->add_menu(move(frequency_menu));

    auto help_menu = GMenu::construct("Help");
    help_menu->add_action(GAction::create("About", [&](const GAction&) {
        GAboutDialog::show("System Monitor", load_png("/res/icons/32x32/app-system-monitor.png"), window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    auto process_tab_widget = GTabWidget::construct(process_container_splitter);

    auto memory_map_widget = ProcessMemoryMapWidget::construct(nullptr);
    process_tab_widget->add_widget("Memory map", memory_map_widget);

    auto open_files_widget = ProcessFileDescriptorMapWidget::construct(nullptr);
    process_tab_widget->add_widget("Open files", open_files_widget);

    auto unveiled_paths_widget = ProcessUnveiledPathsWidget::construct(nullptr);
    process_tab_widget->add_widget("Unveiled paths", unveiled_paths_widget);

    auto stacks_widget = ProcessStacksWidget::construct(nullptr);
    process_tab_widget->add_widget("Stacks", stacks_widget);

    process_table_view->on_process_selected = [&](pid_t pid) {
        open_files_widget->set_pid(pid);
        stacks_widget->set_pid(pid);
        memory_map_widget->set_pid(pid);
        unveiled_paths_widget->set_pid(pid);
    };

    window->show();

    window->set_icon(load_png("/res/icons/16x16/app-system-monitor.png"));

    return app.exec();
}

class ProgressBarPaintingDelegate final : public GTableCellPaintingDelegate {
public:
    virtual ~ProgressBarPaintingDelegate() override {}

    virtual void paint(GPainter& painter, const Rect& a_rect, const Palette& palette, const GModel& model, const GModelIndex& index) override
    {
        auto rect = a_rect.shrunken(2, 2);
        auto percentage = model.data(index, GModel::Role::Custom).to_int();

        auto data = model.data(index, GModel::Role::Display);
        String text;
        if (data.is_string())
            text = data.as_string();
        StylePainter::paint_progress_bar(painter, rect, palette, 0, 100, percentage, text);
        painter.draw_rect(rect, Color::Black);
    }
};

RefPtr<GWidget> build_file_systems_tab()
{
    auto fs_widget = GLazyWidget::construct();

    fs_widget->on_first_show = [](auto& self) {
        self.set_layout(make<GBoxLayout>(Orientation::Vertical));
        self.layout()->set_margins({ 4, 4, 4, 4 });
        auto fs_table_view = GTableView::construct(&self);
        fs_table_view->set_size_columns_to_fit_content(true);

        Vector<GJsonArrayModel::FieldSpec> df_fields;
        df_fields.empend("mount_point", "Mount point", TextAlignment::CenterLeft);
        df_fields.empend("class_name", "Class", TextAlignment::CenterLeft);
        df_fields.empend("device", "Device", TextAlignment::CenterLeft);
        df_fields.empend(
            "Size", TextAlignment::CenterRight,
            [](const JsonObject& object) {
                return human_readable_size(object.get("total_block_count").to_u32() * object.get("block_size").to_u32());
            },
            [](const JsonObject& object) {
                return object.get("total_block_count").to_u32() * object.get("block_size").to_u32();
            });
        df_fields.empend(
            "Used", TextAlignment::CenterRight,
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
            "Available", TextAlignment::CenterRight,
            [](const JsonObject& object) {
                return human_readable_size(object.get("free_block_count").to_u32() * object.get("block_size").to_u32());
            },
            [](const JsonObject& object) {
                return object.get("free_block_count").to_u32() * object.get("block_size").to_u32();
            });
        df_fields.empend("Access", TextAlignment::CenterLeft, [](const JsonObject& object) {
            return object.get("readonly").to_bool() ? "Read-only" : "Read/Write";
        });
        df_fields.empend("Mount flags", TextAlignment::CenterLeft, [](const JsonObject& object) {
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
            if (builder.string_view().is_empty())
                return String("defaults");
            return builder.to_string();
        });
        df_fields.empend("free_block_count", "Free blocks", TextAlignment::CenterRight);
        df_fields.empend("total_block_count", "Total blocks", TextAlignment::CenterRight);
        df_fields.empend("free_inode_count", "Free inodes", TextAlignment::CenterRight);
        df_fields.empend("total_inode_count", "Total inodes", TextAlignment::CenterRight);
        df_fields.empend("block_size", "Block size", TextAlignment::CenterRight);
        fs_table_view->set_model(GSortingProxyModel::create(GJsonArrayModel::create("/proc/df", move(df_fields))));

        fs_table_view->set_cell_painting_delegate(3, make<ProgressBarPaintingDelegate>());

        fs_table_view->model()->update();
    };
    return fs_widget;
}

RefPtr<GWidget> build_pci_devices_tab()
{
    auto pci_widget = GLazyWidget::construct();

    pci_widget->on_first_show = [](auto& self) {
        self.set_layout(make<GBoxLayout>(Orientation::Vertical));
        self.layout()->set_margins({ 4, 4, 4, 4 });
        auto pci_table_view = GTableView::construct(&self);
        pci_table_view->set_size_columns_to_fit_content(true);

        auto db = PCIDB::Database::open();

        Vector<GJsonArrayModel::FieldSpec> pci_fields;
        pci_fields.empend(
            "Address", TextAlignment::CenterLeft,
            [](const JsonObject& object) {
                auto seg = object.get("seg").to_u32();
                auto bus = object.get("bus").to_u32();
                auto slot = object.get("slot").to_u32();
                auto function = object.get("function").to_u32();
                return String::format("%04x:%02x:%02x.%d", seg, bus, slot, function);
            });
        pci_fields.empend(
            "Class", TextAlignment::CenterLeft,
            [db](const JsonObject& object) {
                auto class_id = object.get("class").to_u32();
                String class_name = db->get_class(class_id);
                return class_name == "" ? String::format("%04x", class_id) : class_name;
            });
        pci_fields.empend(
            "Vendor", TextAlignment::CenterLeft,
            [db](const JsonObject& object) {
                auto vendor_id = object.get("vendor_id").to_u32();
                String vendor_name = db->get_vendor(vendor_id);
                return vendor_name == "" ? String::format("%02x", vendor_id) : vendor_name;
            });
        pci_fields.empend(
            "Device", TextAlignment::CenterLeft,
            [db](const JsonObject& object) {
                auto vendor_id = object.get("vendor_id").to_u32();
                auto device_id = object.get("device_id").to_u32();
                String device_name = db->get_device(vendor_id, device_id);
                return device_name == "" ? String::format("%02x", device_id) : device_name;
            });
        pci_fields.empend(
            "Revision", TextAlignment::CenterRight,
            [](const JsonObject& object) {
                auto revision_id = object.get("revision_id").to_u32();
                return String::format("%02x", revision_id);
            });

        pci_table_view->set_model(GSortingProxyModel::create(GJsonArrayModel::create("/proc/pci", move(pci_fields))));
        pci_table_view->model()->update();
    };

    return pci_widget;
}

RefPtr<GWidget> build_devices_tab()
{
    auto devices_widget = GLazyWidget::construct();

    devices_widget->on_first_show = [](auto& self) {
        self.set_layout(make<GBoxLayout>(Orientation::Vertical));
        self.layout()->set_margins({ 4, 4, 4, 4 });

        auto devices_table_view = GTableView::construct(&self);
        devices_table_view->set_size_columns_to_fit_content(true);
        devices_table_view->set_model(GSortingProxyModel::create(DevicesModel::create()));
        devices_table_view->model()->update();
    };

    return devices_widget;
}

NonnullRefPtr<GWidget> build_graphs_tab()
{
    auto graphs_container = GLazyWidget::construct();

    graphs_container->on_first_show = [](auto& self) {
        self.set_fill_with_background_color(true);
        self.set_background_role(ColorRole::Button);
        self.set_layout(make<GBoxLayout>(Orientation::Vertical));
        self.layout()->set_margins({ 4, 4, 4, 4 });

        auto cpu_graph_group_box = GGroupBox::construct("CPU usage", &self);
        cpu_graph_group_box->set_layout(make<GBoxLayout>(Orientation::Vertical));
        cpu_graph_group_box->layout()->set_margins({ 6, 16, 6, 6 });
        cpu_graph_group_box->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
        cpu_graph_group_box->set_preferred_size(0, 120);
        auto cpu_graph = GraphWidget::construct(cpu_graph_group_box);
        cpu_graph->set_max(100);
        cpu_graph->set_text_color(Color::Green);
        cpu_graph->set_graph_color(Color::from_rgb(0x00bb00));
        cpu_graph->text_formatter = [](int value, int) {
            return String::format("%d%%", value);
        };

        ProcessModel::the().on_new_cpu_data_point = [graph = cpu_graph.ptr()](float cpu_percent) {
            graph->add_value(cpu_percent);
        };

        auto memory_graph_group_box = GGroupBox::construct("Memory usage", &self);
        memory_graph_group_box->set_layout(make<GBoxLayout>(Orientation::Vertical));
        memory_graph_group_box->layout()->set_margins({ 6, 16, 6, 6 });
        memory_graph_group_box->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
        memory_graph_group_box->set_preferred_size(0, 120);
        auto memory_graph = GraphWidget::construct(memory_graph_group_box);
        memory_graph->set_text_color(Color::Cyan);
        memory_graph->set_graph_color(Color::from_rgb(0x00bbbb));
        memory_graph->text_formatter = [](int value, int max) {
            return String::format("%d / %d KB", value, max);
        };

        auto memory_stats_widget = MemoryStatsWidget::construct(*memory_graph, &self);
    };
    return graphs_container;
}
