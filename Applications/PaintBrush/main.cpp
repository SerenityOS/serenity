#include "PaintableWidget.h"
#include <LibGUI/GApplication.h>
#include <LibGUI/GWindow.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* window = new GWindow;
    window->set_title("PaintBrush");
    window->set_rect(100, 100, 600, 400);

    auto* paintable_widget = new PaintableWidget(nullptr);
    window->set_main_widget(paintable_widget);

    window->show();
    return app.exec();
}
