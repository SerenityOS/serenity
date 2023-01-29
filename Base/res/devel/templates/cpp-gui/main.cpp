#include <LibGUI/Application.h>
#include <LibGUI/Button.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);

    auto window = GUI::Window::construct();
    window->set_title("Hello friends!");
    window->resize(200, 100);

    auto button = GUI::Button::construct();
    button->set_text("Click me!");
    button->on_click = [&](auto) {
        GUI::MessageBox::show(window, "Hello friends!"sv, ":^)"sv);
    };

    window->set_main_widget(button);

    window->show();

    return app->exec();
}
