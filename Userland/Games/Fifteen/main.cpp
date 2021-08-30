#include <LibConfig/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Window.h>
#include <LibGUI/MessageBox.h>
#include "Utilities.h"
#include "BoardWidget.h"
#include "SettingsDialog.h"

int main (int argc, char **argv)
{
    auto app = GUI::Application::construct(argc, argv);
    auto app_icon = GUI::Icon::default_icon(Constants::AppIconName);

    auto window = GUI::Window::construct();

    Config::pledge_domains({ Constants::AppDomainName });

    if (pledge("stdio thread recvfd sendfd cpath rpath wpath fattr unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    i32 rows = Config::read_i32(Constants::AppDomainName, Constants::ConfigGroupSettings, Constants::ConfigNumberOfRows, 4);
    i32 columns = Config::read_i32(Constants::AppDomainName, Constants::ConfigGroupSettings, Constants::ConfigNumberOfColumns, 4);
    i32 cell_size = Config::read_i32(Constants::AppDomainName, Constants::ConfigGroupSettings, Constants::ConfigCellSizeInPixels, 64);
    Gfx::Color cell_color = Gfx::Color::from_rgba(Config::read_i32(Constants::AppDomainName, Constants::ConfigGroupSettings, Constants::ConfigCellColor, Gfx::Color { Gfx::Color::NamedColor::LightGray }.value()));
    Gfx::Color cell_text_color = Gfx::Color::from_rgba(Config::read_i32(Constants::AppDomainName, Constants::ConfigGroupSettings, Constants::ConfigCellTextColor, Gfx::Color { Gfx::Color::NamedColor::DarkCyan }.value()));

    Config::write_i32(Constants::AppDomainName, Constants::ConfigGroupSettings, Constants::ConfigNumberOfRows, rows);
    Config::write_i32(Constants::AppDomainName, Constants::ConfigGroupSettings, Constants::ConfigNumberOfColumns, columns);
    Config::write_i32(Constants::AppDomainName, Constants::ConfigGroupSettings, Constants::ConfigCellSizeInPixels, cell_size);
    Config::write_i32(Constants::AppDomainName, Constants::ConfigGroupSettings, Constants::ConfigCellColor, cell_color.value());
    Config::write_i32(Constants::AppDomainName, Constants::ConfigGroupSettings, Constants::ConfigCellTextColor, cell_text_color.value());

    window->set_resizable(false);
    window->set_double_buffering_enabled(false);
    window->set_title(Constants::AppDomainName);

    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.set_layout<GUI::VerticalBoxLayout>();
    main_widget.set_fill_with_background_color(true);

    auto& game_menu = window->add_menu("&Game");
    auto move_count { 0 };
    auto& board_view = main_widget.add<BoardWidget>(rows, columns, cell_size, cell_color, cell_text_color);
    board_view.set_focus(true);
    auto& statusbar = main_widget.add<GUI::Statusbar>(2);
    auto scon = board_view.on_solved += [&window, &move_count, &statusbar](auto &&rows, auto &&columns) {
        Config::write_i32(Constants::AppDomainName, Constants::ConfigGroupScore, String::formatted("{}x{}", columns, rows), move_count);
        GUI::MessageBox::show(window, String::formatted("You solved it in {} moves!", move_count), "Information", GUI::MessageBox::Type::Information);
        move_count = 0;
        statusbar.set_text(1, String::formatted("{}", move_count));
    };
    auto mcon = board_view.on_cell_moved += [&move_count, &statusbar]() {
        ++move_count;
        statusbar.set_text(1, String::formatted("{}", move_count));
    };

    statusbar.set_text(0, "Moves:");
    statusbar.set_text(1, "0");

    game_menu.add_action(GUI::Action::create("&Settings", { Mod_None, Key_F9 }, [&](auto&) {
            
            auto settings_dialog = SettingsDialog::construct(window, rows, columns, cell_size);

            if (settings_dialog->exec() || settings_dialog->result() != GUI::Dialog::ExecOK)
                return;

            // ------------------------------------------------------------

    }));

    game_menu.add_separator();
    game_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    window->resize(columns * cell_size, rows * cell_size + statusbar.max_height() + 3);
    window->show();
    window->set_icon(app_icon.bitmap_for_size(16));

    return app->exec();
}
