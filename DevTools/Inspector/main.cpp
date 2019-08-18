#include "RemoteObjectGraphModel.h"
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GTreeView.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>

[[noreturn]] static void print_usage_and_exit()
{
    printf("usage: Inspector <pid>\n");
    exit(0);
}

int main(int argc, char** argv)
{
    if (argc != 2)
        print_usage_and_exit();

    bool ok;
    pid_t pid = String(argv[1]).to_int(ok);
    if (!ok)
        print_usage_and_exit();

    GApplication app(argc, argv);

    auto* window = new GWindow;
    window->set_title("Inspector");
    window->set_rect(150, 150, 300, 500);

    auto* widget = new GWidget;
    window->set_main_widget(widget);
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));

    auto* tree_view = new GTreeView(widget);
    tree_view->set_model(RemoteObjectGraphModel::create_with_pid(pid));
    tree_view->model()->update();

    window->show();
    return app.exec();
}
