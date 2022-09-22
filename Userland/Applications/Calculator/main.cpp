/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CalculatorWidget.h"
#include <LibCore/System.h>
#include <LibCrypto/NumberTheory/ModularFunctions.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix"));
    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Core::System::pledge("stdio recvfd sendfd rpath"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("app-calculator"sv);

    auto window = TRY(GUI::Window::try_create());
    window->set_title("Calculator");
    window->set_resizable(false);
    window->resize(250, 215);

    auto widget = TRY(window->try_set_main_widget<CalculatorWidget>());

    window->set_icon(app_icon.bitmap_for_size(16));

    auto& file_menu = window->add_menu("&File");
    file_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& edit_menu = window->add_menu("&Edit");
    edit_menu.add_action(GUI::CommonActions::make_copy_action([&](auto&) {
        GUI::Clipboard::the().set_plain_text(widget->get_entry());
    }));
    edit_menu.add_action(GUI::CommonActions::make_paste_action([&](auto&) {
        auto clipboard = GUI::Clipboard::the().fetch_data_and_type();
        if (clipboard.mime_type == "text/plain") {
            if (!clipboard.data.is_empty()) {
                widget->set_entry(Crypto::BigFraction(StringView(clipboard.data)));
            }
        }
    }));

    auto& constants_menu = window->add_menu("&Constants");
    auto const power = Crypto::NumberTheory::Power("10"_bigint, "10"_bigint);

    constants_menu.add_action(GUI::Action::create("&Pi", TRY(Gfx::Bitmap::try_load_from_file("/res/icons/calculator/pi.png"sv)), [&](auto&) {
        widget->set_entry(Crypto::BigFraction { Crypto::SignedBigInteger(31415926535), power });
    }));
    constants_menu.add_action(GUI::Action::create("&Euler's Number", TRY(Gfx::Bitmap::try_load_from_file("/res/icons/calculator/eulers_number.png"sv)), [&](auto&) {
        widget->set_entry(Crypto::BigFraction { Crypto::SignedBigInteger(27182818284), power });
    }));
    constants_menu.add_action(GUI::Action::create("&Phi", TRY(Gfx::Bitmap::try_load_from_file("/res/icons/calculator/phi.png"sv)), [&](auto&) {
        widget->set_entry(Crypto::BigFraction { Crypto::SignedBigInteger(16180339887), power });
    }));

    auto& round_menu = window->add_menu("&Round");
    GUI::ActionGroup preview_actions;

    static constexpr auto rounding_modes = Array { 0, 2, 4 };

    for (auto const rounding_mode : rounding_modes) {
        auto round_action = GUI::Action::create_checkable(String::formatted("To &{} digits", rounding_mode), [&widget, rounding_mode](auto&) {
            widget->set_rounding_length(rounding_mode);
        });

        preview_actions.add_action(*round_action);
        round_menu.add_action(*round_action);
    }

    preview_actions.set_exclusive(true);

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Calculator", app_icon, window));

    window->show();

    return app->exec();
}
