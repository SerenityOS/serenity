#include "ClipboardHistoryModel.h"
#include "IconWidget.h"
#include <LibGUI/Application.h>
#include <LibGUI/TableView.h>
#include <LibGUI/Window.h>

int main(int argc, char* argv[])
{
    GUI::Application app(argc, argv);

    auto main_window = GUI::Window::construct();
    main_window->set_title("Clipboard history");
    main_window->set_rect(670, 65, 325, 500);

    auto& table_view = main_window->set_main_widget<GUI::TableView>();
    auto model = ClipboardHistoryModel::create();
    table_view.set_model(model);

    GUI::Clipboard::the().on_change = [&](const String&) {
        auto item = GUI::Clipboard::the().data_and_type();
        model->add_item(item);
    };

    table_view.on_activation = [&](const GUI::ModelIndex& index) {
        auto& data_and_type = model->item_at(index.row());
        GUI::Clipboard::the().set_data(data_and_type.data, data_and_type.type);
    };

    auto applet_window = GUI::Window::construct();
    applet_window->set_title("Clipboard history");
    applet_window->set_window_type(GUI::WindowType::MenuApplet);
    auto& icon = applet_window->set_main_widget<IconWidget>();
    icon.on_click = [&main_window = *main_window] {
        main_window.show();
    };
    applet_window->resize(16, 16);
    applet_window->show();

    return app.exec();
}
