#include "DisplayProperties.h"
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);
    DisplayPropertiesWidget instance;

    auto window = GWindow::construct();
    window->set_title("Display Properties");
    window->move_to(100,100);
    window->resize(400, 448);
    window->set_resizable(false);
    window->set_main_widget(instance.root_widget());
    window->set_icon(load_png("/res/icons/16x16/app-display-properties.png"));

    window->show();
    return app.exec();
}
