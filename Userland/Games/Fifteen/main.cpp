#include <LibConfig/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Window.h>
#include <LibGUI/MessageBox.h>
#include "Utilities.h"
#include "BoardWidget.h"

int main (int argc, char **argv)
{
    auto app = GUI::Application::construct(argc, argv);
    auto app_icon = GUI::Icon::default_icon(Constants::AppIconName);

    auto window = GUI::Window::construct();

    Config::pledge_domains({ Constants::AppDomainName });

    if (pledge("stdio rpath recvfd sendfd wpath cpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    i32 rows = Config::read_i32(Constants::AppDomainName, Constants::ConfigGroupSettings, Constants::ConfigNumberOfRows, 4);
    i32 columns = Config::read_i32(Constants::AppDomainName, Constants::ConfigGroupSettings, Constants::ConfigNumberOfColumns, 4);
    i32 cell_size = Config::read_i32(Constants::AppDomainName, Constants::ConfigGroupSettings, Constants::ConfigCellSizeInPixels, 64);
    Gfx::Color cell_color = Gfx::Color::from_rgba(Config::read_i32(Constants::AppDomainName, Constants::ConfigGroupSettings, Constants::ConfigCellColor, Gfx::Color { Gfx::Color::NamedColor::White }.value()));
    Gfx::Color cell_text_color = Gfx::Color::from_rgba(Config::read_i32(Constants::AppDomainName, Constants::ConfigGroupSettings, Constants::ConfigCellTextColor, Gfx::Color { Gfx::Color::NamedColor::Black }.value()));
    auto board_size = Tuple(columns * cell_size, rows * cell_size);

    Config::write_i32(Constants::AppDomainName, Constants::ConfigGroupSettings, Constants::ConfigNumberOfRows, rows);
    Config::write_i32(Constants::AppDomainName, Constants::ConfigGroupSettings, Constants::ConfigNumberOfColumns, columns);
    Config::write_i32(Constants::AppDomainName, Constants::ConfigGroupSettings, Constants::ConfigCellSizeInPixels, cell_size);

    window->set_double_buffering_enabled(false);
    window->set_title(Constants::AppDomainName);

    window->resize(board_size.template get<0>(), board_size.template get<1>());

    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.set_layout<GUI::VerticalBoxLayout>();
    main_widget.set_fill_with_background_color(true);

    auto& board_view = main_widget.add<BoardWidget>(rows, columns, cell_size, cell_color, cell_text_color);
    board_view.set_focus(true);
    AK::Connection con = board_view.on_solved += [&window](auto &&rows, auto &&columns) {
        GUI::MessageBox::show(window, String::formatted("You solved it!\nThe puzzle size is {}x{}", columns, rows), "Information", GUI::MessageBox::Type::Information);
    };

    //auto& statusbar = main_widget.add<GUI::Statusbar>();

    window->show();

    window->set_icon(app_icon.bitmap_for_size(16));

    return app->exec();
}
