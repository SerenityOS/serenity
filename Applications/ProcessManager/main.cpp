#include <LibCore/CTimer.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GToolBar.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GGroupBox.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GTabWidget.h>
#include <LibGUI/GLabel.h>
#include <unistd.h>
#include <stdio.h>
#include <signal.h>
#include "ProcessTableView.h"
#include "MemoryStatsWidget.h"
#include "GraphWidget.h"

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* keeper = new GWidget;
    keeper->set_layout(make<GBoxLayout>(Orientation::Vertical));
    keeper->set_fill_with_background_color(true);
    keeper->set_background_color(Color::LightGray);
    keeper->layout()->set_margins({ 4, 4, 4, 4 });

    auto* tabwidget = new GTabWidget(keeper);

    auto* widget = new GWidget(nullptr);
    tabwidget->add_widget("Processes", widget);

    auto* graphs_container = new GWidget;
    graphs_container->set_fill_with_background_color(true);
    graphs_container->set_background_color(Color::LightGray);
    graphs_container->set_layout(make<GBoxLayout>(Orientation::Vertical));
    graphs_container->layout()->set_margins({ 4, 4, 4, 4 });

    auto* cpu_graph_group_box = new GGroupBox("CPU usage", graphs_container);
    cpu_graph_group_box->set_layout(make<GBoxLayout>(Orientation::Vertical));
    cpu_graph_group_box->layout()->set_margins({ 6, 16, 6, 6 });
    cpu_graph_group_box->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    cpu_graph_group_box->set_preferred_size({ 0, 120 });
    auto* cpu_graph = new GraphWidget(cpu_graph_group_box);
    cpu_graph->set_max(100);
    cpu_graph->set_text_color(Color::Green);
    cpu_graph->set_graph_color(Color::from_rgb(0x00bb00));
    cpu_graph->text_formatter = [] (int value, int) {
        return String::format("%d%%", value);
    };

    auto* memory_graph_group_box = new GGroupBox("Memory usage", graphs_container);
    memory_graph_group_box->set_layout(make<GBoxLayout>(Orientation::Vertical));
    memory_graph_group_box->layout()->set_margins({ 6, 16, 6, 6 });
    memory_graph_group_box->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    memory_graph_group_box->set_preferred_size({ 0, 120 });
    auto* memory_graph = new GraphWidget(memory_graph_group_box);
    memory_graph->set_text_color(Color::Cyan);
    memory_graph->set_graph_color(Color::from_rgb(0x00bbbb));
    memory_graph->text_formatter = [] (int value, int max) {
        return String::format("%d / %d KB", value, max);
    };

    tabwidget->add_widget("Graphs", graphs_container);

    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->layout()->set_margins({ 4, 0, 4, 4 });
    widget->layout()->set_spacing(0);

    auto* toolbar = new GToolBar(widget);
    toolbar->set_has_frame(false);
    auto* process_table_view = new ProcessTableView(*cpu_graph, widget);
    auto* memory_stats_widget = new MemoryStatsWidget(*memory_graph, widget);

    auto* refresh_timer = new CTimer(1000, [&] {
        process_table_view->refresh();
        memory_stats_widget->refresh();
    });

    auto kill_action = GAction::create("Kill process", GraphicsBitmap::load_from_file("/res/icons/kill16.png"), [process_table_view] (const GAction&) {
        pid_t pid = process_table_view->selected_pid();
        if (pid != -1)
            kill(pid, SIGKILL);
    });

    auto stop_action = GAction::create("Stop process", GraphicsBitmap::load_from_file("/res/icons/stop16.png"), [process_table_view] (const GAction&) {
        pid_t pid = process_table_view->selected_pid();
        if (pid != -1)
            kill(pid, SIGSTOP);
    });

    auto continue_action = GAction::create("Continue process", GraphicsBitmap::load_from_file("/res/icons/continue16.png"), [process_table_view] (const GAction&) {
        pid_t pid = process_table_view->selected_pid();
        if (pid != -1)
            kill(pid, SIGCONT);
    });

    toolbar->add_action(kill_action.copy_ref());
    toolbar->add_action(stop_action.copy_ref());
    toolbar->add_action(continue_action.copy_ref());

    auto menubar = make<GMenuBar>();
    auto app_menu = make<GMenu>("ProcessManager");
    app_menu->add_action(GAction::create("Quit", { Mod_Alt, Key_F4 }, [] (const GAction&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto process_menu = make<GMenu>("Process");
    process_menu->add_action(kill_action.copy_ref());
    process_menu->add_action(stop_action.copy_ref());
    process_menu->add_action(continue_action.copy_ref());
    menubar->add_menu(move(process_menu));

    auto frequency_menu = make<GMenu>("Frequency");
    frequency_menu->add_action(GAction::create("0.25 sec", [refresh_timer] (auto&) {
        refresh_timer->restart(250);
    }));
    frequency_menu->add_action(GAction::create("0.5 sec", [refresh_timer] (auto&) {
        refresh_timer->restart(500);
    }));
    frequency_menu->add_action(GAction::create("1 sec", [refresh_timer] (auto&) {
        refresh_timer->restart(1000);
    }));
    frequency_menu->add_action(GAction::create("3 sec", [refresh_timer] (auto&) {
        refresh_timer->restart(3000);
    }));
    frequency_menu->add_action(GAction::create("5 sec", [refresh_timer] (auto&) {
        refresh_timer->restart(5000);
    }));
    menubar->add_menu(move(frequency_menu));

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(GAction::create("About", [] (const GAction&) {
        dbgprintf("FIXME: Implement Help/About\n");
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    auto* window = new GWindow;
    window->set_title("ProcessManager");
    window->set_rect(20, 200, 680, 400);
    window->set_main_widget(keeper);
    window->set_should_exit_event_loop_on_close(true);
    window->show();

    window->set_icon_path("/res/icons/16x16/app-process-manager.png");

    return app.exec();
}
