#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include "UI_HelloWorld2.h"

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* window = new GWindow;
    window->set_rect(100, 100, 240, 160);
    window->set_title("Hello World!");

    auto* ui = new UI_HelloWorld2;
    window->set_main_widget(ui->main_widget);

    ui->w3->on_click = [&](auto&) {
        app.quit();
    };

    window->show();
    return app.exec();
}
