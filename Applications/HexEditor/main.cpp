#include "HexEditorWidget.h"
#include <LibDraw/PNGLoader.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (pledge("stdio shared_buffer unix rpath cpath wpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GApplication app(argc, argv);

    if (pledge("stdio shared_buffer rpath cpath wpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto window = GWindow::construct();
    window->set_title("Hex Editor");
    window->set_rect(20, 200, 640, 400);

    auto hex_editor_widget = HexEditorWidget::construct();
    window->set_main_widget(hex_editor_widget);

    window->on_close_request = [&]() -> GWindow::CloseRequestDecision {
        if (hex_editor_widget->request_close())
            return GWindow::CloseRequestDecision::Close;
        return GWindow::CloseRequestDecision::StayOpen;
    };

    window->show();
    window->set_icon(load_png("/res/icons/16x16/app-hexeditor.png"));

    if (argc >= 2)
        hex_editor_widget->open_file(argv[1]);

    return app.exec();
}
