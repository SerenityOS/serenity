/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CalculatorWidget.h"
#include <LibCore/System.h>
#include <LibCrypto/NumberTheory/ModularFunctions.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Icon.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibMain/Main.h>
#include <LibURL/URL.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix"));
    auto app = TRY(GUI::Application::create(arguments));

    auto const man_file = "/usr/share/man/man1/Applications/Calculator.md";

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme(man_file) }));
    TRY(Desktop::Launcher::seal_allowlist());

    TRY(Core::System::pledge("stdio recvfd sendfd rpath"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("app-calculator"sv);

    auto window = GUI::Window::construct();
    window->set_title("Calculator");
    window->set_resizable(false);
    window->resize(250, 215);

    auto widget = TRY(Calculator::CalculatorWidget::try_create());
    window->set_main_widget(widget.ptr());

    window->set_icon(app_icon.bitmap_for_size(16));

    auto file_menu = window->add_menu("&File"_string);
    file_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto edit_menu = window->add_menu("&Edit"_string);
    edit_menu->add_action(GUI::CommonActions::make_copy_action([&](auto&) {
        GUI::Clipboard::the().set_plain_text(widget->get_entry());
    }));
    edit_menu->add_action(GUI::CommonActions::make_paste_action([&](auto&) {
        auto clipboard = GUI::Clipboard::the().fetch_data_and_type();
        if (clipboard.mime_type == "text/plain") {
            if (!clipboard.data.is_empty()) {
                auto number_or_error = Crypto::BigFraction::from_string(StringView(clipboard.data));
                if (!number_or_error.is_error())
                    widget->set_typed_entry(number_or_error.release_value());
            }
        }
    }));

    auto constants_menu = window->add_menu("&Constants"_string);
    auto const power = Crypto::NumberTheory::Power("10"_bigint, "10"_bigint);

    constants_menu->add_action(GUI::Action::create("&Pi", TRY(Gfx::Bitmap::load_from_file("/res/icons/calculator/pi.png"sv)), [&](auto&) {
        widget->set_typed_entry(Crypto::BigFraction { Crypto::SignedBigInteger(31415926535), power });
    }));
    constants_menu->add_action(GUI::Action::create("&Euler's Number", TRY(Gfx::Bitmap::load_from_file("/res/icons/calculator/eulers_number.png"sv)), [&](auto&) {
        widget->set_typed_entry(Crypto::BigFraction { Crypto::SignedBigInteger(27182818284), power });
    }));
    constants_menu->add_action(GUI::Action::create("&Phi", TRY(Gfx::Bitmap::load_from_file("/res/icons/calculator/phi.png"sv)), [&](auto&) {
        widget->set_typed_entry(Crypto::BigFraction { Crypto::SignedBigInteger(16180339887), power });
    }));

    auto round_menu = window->add_menu("&Round"_string);
    GUI::ActionGroup preview_actions;

    static constexpr auto rounding_modes = Array { 0, 2, 4 };

    Optional<unsigned> last_rounding_mode = 1;
    for (unsigned i {}; i < rounding_modes.size(); ++i) {
        auto round_action = GUI::Action::create_checkable(ByteString::formatted("To &{} Digits", rounding_modes[i]),
            [&widget, rounding_mode = rounding_modes[i], &last_rounding_mode, i](auto&) {
                widget->set_rounding_length(rounding_mode);
                last_rounding_mode = i;
            });

        preview_actions.add_action(*round_action);
        round_menu->add_action(*round_action);
    }

    constexpr auto format { "&Custom - {}..."sv };
    auto round_custom = GUI::Action::create_checkable(ByteString::formatted(format, 0), [&](auto& action) {
        int custom_rounding_length = widget->rounding_length();
        auto result = GUI::InputBox::show_numeric(window, custom_rounding_length, 0, 100, "Digits to Round"sv);
        if (!result.is_error() && result.value() == GUI::Dialog::ExecResult::OK) {
            action.set_text(ByteString::formatted(format, custom_rounding_length));
            widget->set_rounding_length(custom_rounding_length);
            last_rounding_mode.clear();
        } else if (last_rounding_mode.has_value())
            round_menu->action_at(last_rounding_mode.value())
                ->activate();
    });

    widget->set_rounding_custom(round_custom, format);

    auto shrink_action = GUI::Action::create("&Shrink...", TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/edit-cut.png"sv)), [&](auto&) {
        int shrink_length = widget->rounding_length();
        auto result = GUI::InputBox::show_numeric(window, shrink_length, 0, 100, "Digits to Shrink"sv);
        if (!result.is_error() && result.value() == GUI::Dialog::ExecResult::OK) {
            round_custom->set_checked(true);
            round_custom->set_text(ByteString::formatted(format, shrink_length));
            widget->set_rounding_length(shrink_length);
            widget->shrink(shrink_length);
        }
    });

    preview_actions.add_action(*round_custom);
    preview_actions.set_exclusive(true);
    round_menu->add_action(*round_custom);
    round_menu->add_action(*shrink_action);

    round_menu->action_at(last_rounding_mode.value())->activate();

    auto view_menu = window->add_menu("&View"_string);
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    }));

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_help_action([&man_file](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme(man_file), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("Calculator"_string, app_icon, window));

    window->show();

    return app->exec();
}
