#include "DevicesModel.h"
#include "GraphWidget.h"
#include "MemoryStatsWidget.h"
#include "NetworkStatisticsWidget.h"
#include "ProcessFileDescriptorMapWidget.h"
#include "ProcessMemoryMapWidget.h"
#include "ProcessStacksWidget.h"
#include "ProcessTableView.h"
#include <LibCore/CTimer.h>
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GGroupBox.h>
#include <LibGUI/GJsonArrayModel.h>
#include <LibGUI/GLabel.h>
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

static GWidget* build_file_systems_tab();
static GWidget* build_pci_devices_tab();
static GWidget* build_devices_tab();

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* keeper = new GWidget;
    keeper->set_layout(make<GBoxLayout>(Orientation::Vertical));
    keeper->set_fill_with_background_color(true);
    keeper->set_background_color(Color::WarmGray);
    keeper->layout()->set_margins({ 4, 4, 4, 4 });

    auto* tabwidget = new GTabWidget(keeper);

    auto process_container_splitter = GSplitter::construct(Orientation::Vertical, nullptr);
    tabwidget->add_widget("Processes", process_container_splitter);

    auto* process_table_container = new GWidget(process_container_splitter);

    auto* graphs_container = new GWidget;
    graphs_container->set_fill_with_background_color(true);
    graphs_container->set_background_color(Color::WarmGray);
    graphs_container->set_layout(make<GBoxLayout>(Orientation::Vertical));
    graphs_container->layout()->set_margins({ 4, 4, 4, 4 });

    auto cpu_graph_group_box = GGroupBox::construct("CPU usage", graphs_container);
    cpu_graph_group_box->set_layout(make<GBoxLayout>(Orientation::Vertical));
    cpu_graph_group_box->layout()->set_margins({ 6, 16, 6, 6 });
    cpu_graph_group_box->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    cpu_graph_group_box->set_preferred_size(0, 120);
    auto* cpu_graph = new GraphWidget(cpu_graph_group_box);
    cpu_graph->set_max(100);
    cpu_graph->set_text_color(Color::Green);
    cpu_graph->set_graph_color(Color::from_rgb(0x00bb00));
    cpu_graph->text_formatter = [](int value, int) {
        return String::format("%d%%", value);
    };

    auto memory_graph_group_box = GGroupBox::construct("Memory usage", graphs_container);
    memory_graph_group_box->set_layout(make<GBoxLayout>(Orientation::Vertical));
    memory_graph_group_box->layout()->set_margins({ 6, 16, 6, 6 });
    memory_graph_group_box->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    memory_graph_group_box->set_preferred_size(0, 120);
    auto* memory_graph = new GraphWidget(memory_graph_group_box);
    memory_graph->set_text_color(Color::Cyan);
    memory_graph->set_graph_color(Color::from_rgb(0x00bbbb));
    memory_graph->text_formatter = [](int value, int max) {
        return String::format("%d / %d KB", value, max);
    };

    tabwidget->add_widget("Graphs", graphs_container);

    tabwidget->add_widget("File systems", build_file_systems_tab());

    tabwidget->add_widget("PCI devices", build_pci_devices_tab());

    tabwidget->add_widget("Devices", build_devices_tab());

    auto* network_stats_widget = new NetworkStatisticsWidget(nullptr);
    tabwidget->add_widget("Network", network_stats_widget);

    process_table_container->set_layout(make<GBoxLayout>(Orientation::Vertical));
    process_table_container->layout()->set_margins({ 4, 0, 4, 4 });
    process_table_container->layout()->set_spacing(0);

    auto toolbar = GToolBar::construct(process_table_container);
    toolbar->set_has_frame(false);
    auto* process_table_view = new ProcessTableView(*cpu_graph, process_table_container);
    auto* memory_stats_widget = new MemoryStatsWidget(*memory_graph, graphs_container);

    auto refresh_timer = CTimer::create(1000, [&] {
        process_table_view->refresh();
        memory_stats_widget->refresh();
    });

    auto kill_action = GAction::create("Kill process", GraphicsBitmap::load_from_file("/res/icons/kill16.png"), [process_table_view](const GAction&) {
        pid_t pid = process_table_view->selected_pid();
        if (pid != -1)
            kill(pid, SIGKILL);
    });

    auto stop_action = GAction::create("Stop process", GraphicsBitmap::load_from_file("/res/icons/stop16.png"), [process_table_view](const GAction&) {
        pid_t pid = process_table_view->selected_pid();
        if (pid != -1)
            kill(pid, SIGSTOP);
    });

    auto continue_action = GAction::create("Continue process", GraphicsBitmap::load_from_file("/res/icons/continue16.png"), [process_table_view](const GAction&) {
        pid_t pid = process_table_view->selected_pid();
        if (pid != -1)
            kill(pid, SIGCONT);
    });

    toolbar->add_action(kill_action);
    toolbar->add_action(stop_action);
    toolbar->add_action(continue_action);

    auto* window = new GWindow;
    window->set_title("System Monitor");
    window->set_rect(20, 200, 680, 400);
    window->set_main_widget(keeper);

    auto menubar = make<GMenuBar>();
    auto app_menu = make<GMenu>("System Monitor");
    app_menu->add_action(GCommonActions::make_quit_action([](auto&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto process_menu = make<GMenu>("Process");
    process_menu->add_action(kill_action);
    process_menu->add_action(stop_action);
    process_menu->add_action(continue_action);
    menubar->add_menu(move(process_menu));

    auto process_context_menu = make<GMenu>();
    process_context_menu->add_action(kill_action);
    process_context_menu->add_action(stop_action);
    process_context_menu->add_action(continue_action);
    process_table_view->on_context_menu_request = [&](const GModelIndex& index, const GContextMenuEvent& event) {
        (void)index;
        process_context_menu->popup(event.screen_position());
    };

    auto frequency_menu = make<GMenu>("Frequency");
    frequency_menu->add_action(GAction::create("0.25 sec", [&](auto&) {
        refresh_timer->restart(250);
    }));
    frequency_menu->add_action(GAction::create("0.5 sec", [&](auto&) {
        refresh_timer->restart(500);
    }));
    frequency_menu->add_action(GAction::create("1 sec", [&](auto&) {
        refresh_timer->restart(1000);
    }));
    frequency_menu->add_action(GAction::create("3 sec", [&](auto&) {
        refresh_timer->restart(3000);
    }));
    frequency_menu->add_action(GAction::create("5 sec", [&](auto&) {
        refresh_timer->restart(5000);
    }));
    menubar->add_menu(move(frequency_menu));

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(GAction::create("About", [&](const GAction&) {
        GAboutDialog::show("SystemMonitor", load_png("/res/icons/32x32/app-system-monitor.png"), window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    auto* process_tab_widget = new GTabWidget(process_container_splitter);

    auto* open_files_widget = new ProcessFileDescriptorMapWidget(nullptr);
    process_tab_widget->add_widget("Open files", open_files_widget);

    auto* memory_map_widget = new ProcessMemoryMapWidget(nullptr);
    process_tab_widget->add_widget("Memory map", memory_map_widget);

    auto* stacks_widget = new ProcessStacksWidget(nullptr);
    process_tab_widget->add_widget("Stacks", stacks_widget);

    process_table_view->on_process_selected = [&](pid_t pid) {
        open_files_widget->set_pid(pid);
        stacks_widget->set_pid(pid);
        memory_map_widget->set_pid(pid);
    };

    window->show();

    window->set_icon(load_png("/res/icons/16x16/app-system-monitor.png"));

    return app.exec();
}

class ProgressBarPaintingDelegate final : public GTableCellPaintingDelegate {
public:
    virtual ~ProgressBarPaintingDelegate() override {}

    virtual void paint(GPainter& painter, const Rect& a_rect, const GModel& model, const GModelIndex& index) override
    {
        auto rect = a_rect.shrunken(2, 2);
        auto percentage = model.data(index, GModel::Role::Custom).to_int();

        auto data = model.data(index, GModel::Role::Display);
        String text;
        if (data.is_string())
            text = data.as_string();
        StylePainter::paint_progress_bar(painter, rect, 0, 100, percentage, text);
        painter.draw_rect(rect, Color::Black);
    }
};

GWidget* build_file_systems_tab()
{
    auto* fs_widget = new GWidget(nullptr);
    fs_widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    fs_widget->layout()->set_margins({ 4, 4, 4, 4 });
    auto fs_table_view = GTableView::construct(fs_widget);
    fs_table_view->set_size_columns_to_fit_content(true);

    Vector<GJsonArrayModel::FieldSpec> df_fields;
    df_fields.empend("mount_point", "Mount point", TextAlignment::CenterLeft);
    df_fields.empend("class_name", "Class", TextAlignment::CenterLeft);
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
    df_fields.empend("free_block_count", "Free blocks", TextAlignment::CenterRight);
    df_fields.empend("total_block_count", "Total blocks", TextAlignment::CenterRight);
    df_fields.empend("free_inode_count", "Free inodes", TextAlignment::CenterRight);
    df_fields.empend("total_inode_count", "Total inodes", TextAlignment::CenterRight);
    df_fields.empend("block_size", "Block size", TextAlignment::CenterRight);
    fs_table_view->set_model(GSortingProxyModel::create(GJsonArrayModel::create("/proc/df", move(df_fields))));

    fs_table_view->set_cell_painting_delegate(3, make<ProgressBarPaintingDelegate>());

    fs_table_view->model()->update();
    return fs_widget;
}

GWidget* build_pci_devices_tab()
{
    auto* pci_widget = new GWidget(nullptr);
    pci_widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    pci_widget->layout()->set_margins({ 4, 4, 4, 4 });
    auto pci_table_view = GTableView::construct(pci_widget);
    pci_table_view->set_size_columns_to_fit_content(true);

    auto db = PCIDB::Database::open();

    Vector<GJsonArrayModel::FieldSpec> pci_fields;
    pci_fields.empend(
        "Address", TextAlignment::CenterLeft,
        [](const JsonObject& object) {
            auto bus = object.get("bus").to_u32();
            auto slot = object.get("slot").to_u32();
            auto function = object.get("function").to_u32();
            return String::format("%02x:%02x.%d", bus, slot, function);
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

    return pci_widget;
}

GWidget* build_devices_tab()
{
    auto* devices_widget = new GWidget(nullptr);
    devices_widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    devices_widget->layout()->set_margins({ 4, 4, 4, 4 });

    auto devices_table_view = GTableView::construct(devices_widget);
    devices_table_view->set_size_columns_to_fit_content(true);
    devices_table_view->set_model(GSortingProxyModel::create(DevicesModel::create()));
    devices_table_view->model()->update();

    return devices_widget;
}
