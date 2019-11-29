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

    auto calc_widget = CalculatorWidget::construct(nullptr);
    window->set_main_widget(calc_widget);

    window->show();
    window->set_icon(GraphicsBitmap::load_from_file("/res/icons/16x16/app-calculator.png"));
    return app.exec();
}
