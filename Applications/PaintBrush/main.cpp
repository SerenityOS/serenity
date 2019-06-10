#include "PaintableWidget.h"
#include "PaletteWidget.h"
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GWindow.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* window = new GWindow;
    window->set_title("PaintBrush");
    window->set_rect(100, 100, 600, 432);

    auto* main_widget = new GWidget(nullptr);
    window->set_main_widget(main_widget);
    main_widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    main_widget->layout()->set_spacing(0);


    auto* paintable_widget = new PaintableWidget(main_widget);
    auto* palette_widget = new PaletteWidget(*paintable_widget, main_widget);

    window->show();
    return app.exec();
}
