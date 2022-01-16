/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CalculatorWidget.h"
#include "RoundingDialog.h"
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
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix"));
    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Core::System::pledge("stdio recvfd sendfd rpath"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("app-calculator");

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
                auto const number = StringView(clipboard.data);
                widget->set_entry(Crypto::BigFraction(number));
                auto const fraction_length = number.length() - number.find('.').value_or(number.length() + 1) - 1;
                widget->update_rounding(fraction_length);
            }
        }
    }));

    auto& constants_menu = window->add_menu("&Constants");
    auto const power = Crypto::NumberTheory::Power("10"_bigint, "10"_bigint);

    constants_menu.add_action(GUI::Action::create("&Pi", TRY(Gfx::Bitmap::try_load_from_file("/res/icons/calculator/pi.png")), [&](auto&) {
        widget->set_entry(Crypto::BigFraction { Crypto::SignedBigInteger::create_from(31415926535), power });
    }));
    constants_menu.add_action(GUI::Action::create("&Euler's Number", TRY(Gfx::Bitmap::try_load_from_file("/res/icons/calculator/eulers_number.png")), [&](auto&) {
        widget->set_entry(Crypto::BigFraction { Crypto::SignedBigInteger::create_from(27182818284), power });
    }));
    constants_menu.add_action(GUI::Action::create("&Phi", TRY(Gfx::Bitmap::try_load_from_file("/res/icons/calculator/phi.png")), [&](auto&) {
        widget->set_entry(Crypto::BigFraction { Crypto::SignedBigInteger::create_from(16180339887), power });
    }));

    auto& round_menu = window->add_menu("&Round");
    GUI::ActionGroup preview_actions;

    auto round_0 = GUI::Action::create_checkable("To &zero digits", [&](auto&) {
        widget->set_rounding_length(0);
    });

    auto round_2 = GUI::Action::create_checkable("To &two digits", [&](auto&) {
        widget->set_rounding_length(2);
    });
    round_2->activate(round_2);

    auto round_4 = GUI::Action::create_checkable("To &four digits", [&](auto&) {
        widget->set_rounding_length(4);
    });

    constexpr StringView format { "&Custom - {} ..." };
    auto round_custom = GUI::Action::create_checkable(String::formatted(format, 0), [&](auto& action) {
        unsigned custom_rounding_length = 0;
        RoundingDialog::show(window, "Choose custom rounding"sv, custom_rounding_length);

        action.set_text(String::formatted(format, custom_rounding_length));
        widget->set_rounding_length(custom_rounding_length);
    });

    widget->set_rounding_custom(round_custom, format);

    auto shrink_action = GUI::Action::create("&Shrink...", TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/edit-cut.png")), [&](auto&) {
        unsigned shrink_length {};
        RoundingDialog::show(window, "Choose shrinking length"sv, shrink_length);

        round_custom->set_checked(true);
        round_custom->set_text(String::formatted(format, shrink_length));
        widget->set_rounding_length(shrink_length);
        widget->shrink(shrink_length);
    });

    preview_actions.add_action(*round_0);
    preview_actions.add_action(*round_2);
    preview_actions.add_action(*round_4);
    preview_actions.add_action(*round_custom);
    preview_actions.set_exclusive(true);

    round_menu.add_action(*round_0);
    round_menu.add_action(*round_2);
    round_menu.add_action(*round_4);
    round_menu.add_action(*round_custom);
    round_menu.add_action(*shrink_action);

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Calculator", app_icon, window));

    window->show();

    return app->exec();
}
