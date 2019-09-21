#include "CalculatorWidget.h"
#include <LibGUI/GApplication.h>
#include <LibGUI/GWindow.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto window = GWindow::construct();
    window->set_title("Calculator");
    window->set_resizable(false);
    window->set_rect({ 300, 200, 254, 213 });

    auto* calc_widget = new CalculatorWidget(nullptr);
    window->set_main_widget(calc_widget);

    window->show();
    return app.exec();
}
