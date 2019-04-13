#include "Field.h"
#include <LibGUI/GApplication.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* window = new GWindow;
    window->set_title("Minesweeper");
    window->set_rect(100, 100, 135, 171);

    auto* widget = new GWidget;
    window->set_main_widget(widget);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));

    auto* container = new GWidget(widget);
    container->set_fill_with_background_color(true);
    container->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    container->set_preferred_size({ 0, 36 });
    container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    auto* face_button = new GButton(container);
    face_button->set_icon(GraphicsBitmap::load_from_file("/res/icons/minesweeper/face-default.png"));

    auto* field = new Field(widget);

    face_button->on_click = [field] (auto&) {
        field->reset();
    };

    window->show();

    return app.exec();
}
