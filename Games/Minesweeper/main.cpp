#include "Field.h"
#include <LibGUI/GApplication.h>
#include <LibGUI/GWindow.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* window = new GWindow;
    window->set_title("Minesweeper");
    window->set_rect(100, 100, 200, 300);
    auto* field = new Field(nullptr);
    window->set_main_widget(field);

    window->show();

    return app.exec();
}
