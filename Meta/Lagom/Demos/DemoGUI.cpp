/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/System.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/TableView.h>
#include <LibGUI/Window.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <LibGfx/SystemTheme.h>
#include <LibMain/Main.h>
#include <stdlib.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    char const* serenity_source_dir = getenv("SERENITY_SOURCE_DIR");
    if (serenity_source_dir == nullptr) {
        // Assume running from Build/lagom.
        serenity_source_dir = "../..";
    }

    // Set default font path.
    Gfx::FontDatabase::set_default_fonts_lookup_path(String::formatted("{}/Base/res/fonts", serenity_source_dir));
    // Set the default font, this is required here since we're not running in a Serenity env
    Gfx::FontDatabase::set_default_font_query("Katica 10 400 0"sv);
    Gfx::FontDatabase::set_fixed_width_font_query("Katica 10 400 0"sv);
    // Ditto but for the theme and palette
    Gfx::set_system_theme(Gfx::load_system_theme(String::formatted("{}/Base/res/themes/Default.ini", serenity_source_dir)));

    auto app = TRY(GUI::Application::try_create(arguments));
    app->set_system_palette(Gfx::current_system_theme_buffer());

    auto window = TRY(GUI::Window::try_create());
    window->set_title("Hello LibGUI World");
    window->resize(600, 400);
    window->set_minimum_size(300, 245);

    auto widget = TRY(window->try_set_main_widget<GUI::Widget>());
    widget->set_fill_with_background_color(true);

    auto layout = TRY(widget->try_set_layout<GUI::VerticalBoxLayout>());
    layout->set_margins(16);
    layout->set_spacing(5);

    auto label = TRY(widget->try_add<GUI::Label>("Hello World :^)"));
    label->set_tooltip("Well howdy friend!");
    label->set_fixed_height(25);

    auto frame = TRY(widget->try_add<GUI::Frame>());
    auto frame_layout = TRY(frame->try_set_layout<GUI::VerticalBoxLayout>());
    frame_layout->set_margins(16);
    frame_layout->set_spacing(5);

    auto center_button = TRY(frame->try_add<GUI::Button>("Center"));
    center_button->on_click = [&](auto) {
        window->center_on_screen();
    };

    auto opacity_button = TRY(frame->try_add<GUI::Button>("Toggle opacity"));
    opacity_button->on_click = [&](auto) {
        window->set_opacity(window->opacity() < 1 ? 1 : 0.45);
    };

    auto disabled_button = TRY(frame->try_add<GUI::Button>("This button is disabled :^("));
    disabled_button->set_enabled(false);
    disabled_button->set_shrink_to_fit(true);

    auto buttons = TRY(widget->try_add<GUI::Widget>());
    buttons->set_fixed_height(25);
    auto buttons_layout = TRY(buttons->try_set_layout<GUI::HorizontalBoxLayout>());
    buttons_layout->set_spacing(5);

    auto popup_button = TRY(buttons->try_add<GUI::Button>("A very cool button"));
    popup_button->on_click = [&](auto) {
        GUI::MessageBox::show(window, "Hello friends!", ":^)");
    };

    auto about_button = TRY(buttons->try_add<GUI::Button>("About"));
    about_button->on_click = [&](auto) {
        GUI::AboutDialog::show("SerenityOS", nullptr, nullptr, nullptr, Core::Version::read_long_version_string());
    };

    auto exit_button = TRY(buttons->try_add<GUI::Button>("Exit"));
    exit_button->on_click = [&](auto) {
        app->quit(0);
    };

    window->show();
    return app->exec();
}
