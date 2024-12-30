#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Frame.h>
#include <LibGUI/MessageBox.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath wpath cpath unix"));

    auto app = TRY(GUI::Application::create(arguments));

    TRY(Core::System::pledge("stdio recvfd sendfd rpath"));

    auto window = GUI::Window::construct();
    window->set_title("Example Application");
    window->resize(200, 200);
    window->set_resizable(false);

    auto main_widget = window->set_main_widget<GUI::Widget>();
    main_widget->set_fill_with_background_color(true);

    main_widget->set_layout<GUI::VerticalBoxLayout>(16);

    auto& button = main_widget->add<GUI::Button>("Click me!"_string);
    button.on_click = [&](auto) {
        GUI::MessageBox::show(window, "Hello friends!"sv, ":^)"sv);
    };

    window->show();
    return app->exec();
}
