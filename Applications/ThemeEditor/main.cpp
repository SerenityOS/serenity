#include "PreviewWidget.h"
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Window.h>

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);

    auto window = GUI::Window::construct();
    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();

    auto& preview_widget = main_widget.add<ThemeEditor::PreviewWidget>(app->palette());
    preview_widget.set_preferred_size(480, 360);
    preview_widget.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);

    window->resize(500, 400);
    window->show();
    return app->exec();
}
