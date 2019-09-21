#include "RemoteObject.h"
#include "RemoteObjectGraphModel.h"
#include "RemoteObjectPropertyModel.h"
#include "RemoteProcess.h"
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GSplitter.h>
#include <LibGUI/GTableView.h>
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

    auto widget = GWidget::construct();
    window->set_main_widget(widget);
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));

    auto splitter = GSplitter::construct(Orientation::Horizontal, widget);

    RemoteProcess remote_process(pid);

    remote_process.on_update = [&] {
        if (!remote_process.process_name().is_null())
            window->set_title(String::format("Inspector: %s (%d)", remote_process.process_name().characters(), remote_process.pid()));
    };

    auto tree_view = GTreeView::construct(splitter);
    tree_view->set_model(remote_process.object_graph_model());
    tree_view->set_activates_on_selection(true);

    auto properties_table_view = GTableView::construct(splitter);
    properties_table_view->set_size_columns_to_fit_content(true);

    tree_view->on_activation = [&](auto& index) {
        auto* remote_object = static_cast<RemoteObject*>(index.internal_data());
        properties_table_view->set_model(remote_object->property_model());
    };

    window->show();
    remote_process.update();
    return app.exec();
}
