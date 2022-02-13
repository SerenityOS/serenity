/*
 * Copyright (c) 2021, Valtteri Koskivuori <vkoskiv@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MagnifierWidget.h"
#include <LibCore/System.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Filters/ColorBlindnessFilter.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio cpath rpath recvfd sendfd unix", nullptr));
    auto app = GUI::Application::construct(arguments);

    TRY(Core::System::pledge("stdio cpath rpath recvfd sendfd", nullptr));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("app-magnifier");

    // 4px on each side for padding
    constexpr int window_dimensions = 200 + 4 + 4;
    auto window = GUI::Window::construct();
    window->set_title("Magnifier");
    window->resize(window_dimensions, window_dimensions);
    window->set_minimizable(false);
    window->set_icon(app_icon.bitmap_for_size(16));
    auto magnifier = TRY(window->try_set_main_widget<MagnifierWidget>());

    auto file_menu = TRY(window->try_add_menu("&File"));
    TRY(file_menu->try_add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    })));

    auto size_action_group = make<GUI::ActionGroup>();

    auto two_x_action = GUI::Action::create_checkable(
        "&2x", { Key_2 }, [&](auto&) {
            magnifier->set_scale_factor(2);
        });

    auto four_x_action = GUI::Action::create_checkable(
        "&4x", { Key_4 }, [&](auto&) {
            magnifier->set_scale_factor(4);
        });

    auto eight_x_action = GUI::Action::create_checkable(
        "&8x", { Key_8 }, [&](auto&) {
            magnifier->set_scale_factor(8);
        });

    auto pause_action = GUI::Action::create_checkable(
        "&Pause Capture", { Key_Space }, [&](auto& action) {
            magnifier->pause_capture(action.is_checked());
        });

    size_action_group->add_action(two_x_action);
    size_action_group->add_action(four_x_action);
    size_action_group->add_action(eight_x_action);
    size_action_group->set_exclusive(true);

    auto view_menu = TRY(window->try_add_menu("&View"));
    TRY(view_menu->try_add_action(two_x_action));
    TRY(view_menu->try_add_action(four_x_action));
    TRY(view_menu->try_add_action(eight_x_action));
    two_x_action->set_checked(true);

    TRY(view_menu->try_add_separator());
    TRY(view_menu->try_add_action(pause_action));

    auto timeline_menu = TRY(window->try_add_menu("&Timeline"));
    auto previous_frame_action = GUI::Action::create(
        "&Previous frame", { Key_Left }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-back.png")), [&](auto&) {
            pause_action->set_checked(true);
            magnifier->pause_capture(true);
            magnifier->display_previous_frame();
        });
    auto next_frame_action = GUI::Action::create(
        "&Next frame", { Key_Right }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-forward.png")), [&](auto&) {
            pause_action->set_checked(true);
            magnifier->pause_capture(true);
            magnifier->display_next_frame();
        });
    TRY(timeline_menu->try_add_action(previous_frame_action));
    TRY(timeline_menu->try_add_action(next_frame_action));

    auto accessibility_menu = TRY(window->try_add_menu("&Accessibility"));

    auto default_accessibility_action = GUI::Action::create_checkable("Default - non-impaired", { Mod_AltGr, Key_1 }, [&](auto&) {
        magnifier->set_color_filter(nullptr);
    });
    default_accessibility_action->set_checked(true);

    auto pratanopia_accessibility_action = GUI::Action::create_checkable("Protanopia", { Mod_AltGr, Key_2 }, [&](auto&) {
        magnifier->set_color_filter(Gfx::ColorBlindnessFilter::create_protanopia());
    });

    auto pratanomaly_accessibility_action = GUI::Action::create_checkable("Protanomaly", { Mod_AltGr, Key_3 }, [&](auto&) {
        magnifier->set_color_filter(Gfx::ColorBlindnessFilter::create_protanomaly());
    });

    auto tritanopia_accessibility_action = GUI::Action::create_checkable("Tritanopia", { Mod_AltGr, Key_4 }, [&](auto&) {
        magnifier->set_color_filter(Gfx::ColorBlindnessFilter::create_tritanopia());
    });

    auto tritanomaly_accessibility_action = GUI::Action::create_checkable("Tritanomaly", { Mod_AltGr, Key_5 }, [&](auto&) {
        magnifier->set_color_filter(Gfx::ColorBlindnessFilter::create_tritanomaly());
    });

    auto deuteranopia_accessibility_action = GUI::Action::create_checkable("Deuteranopia", { Mod_AltGr, Key_6 }, [&](auto&) {
        magnifier->set_color_filter(Gfx::ColorBlindnessFilter::create_deuteranopia());
    });

    auto deuteranomaly_accessibility_action = GUI::Action::create_checkable("Deuteranomaly", { Mod_AltGr, Key_7 }, [&](auto&) {
        magnifier->set_color_filter(Gfx::ColorBlindnessFilter::create_deuteranomaly());
    });

    auto achromatopsia_accessibility_action = GUI::Action::create_checkable("Achromatopsia", { Mod_AltGr, Key_8 }, [&](auto&) {
        magnifier->set_color_filter(Gfx::ColorBlindnessFilter::create_achromatopsia());
    });

    auto achromatomaly_accessibility_action = GUI::Action::create_checkable("Achromatomaly", { Mod_AltGr, Key_9 }, [&](auto&) {
        magnifier->set_color_filter(Gfx::ColorBlindnessFilter::create_achromatomaly());
    });

    auto preview_type_action_group = make<GUI::ActionGroup>();
    preview_type_action_group->set_exclusive(true);
    preview_type_action_group->add_action(*default_accessibility_action);
    preview_type_action_group->add_action(*pratanopia_accessibility_action);
    preview_type_action_group->add_action(*pratanomaly_accessibility_action);
    preview_type_action_group->add_action(*tritanopia_accessibility_action);
    preview_type_action_group->add_action(*tritanomaly_accessibility_action);
    preview_type_action_group->add_action(*deuteranopia_accessibility_action);
    preview_type_action_group->add_action(*deuteranomaly_accessibility_action);
    preview_type_action_group->add_action(*achromatopsia_accessibility_action);
    preview_type_action_group->add_action(*achromatomaly_accessibility_action);

    TRY(accessibility_menu->try_add_action(default_accessibility_action));
    TRY(accessibility_menu->try_add_action(pratanopia_accessibility_action));
    TRY(accessibility_menu->try_add_action(pratanomaly_accessibility_action));
    TRY(accessibility_menu->try_add_action(tritanopia_accessibility_action));
    TRY(accessibility_menu->try_add_action(tritanomaly_accessibility_action));
    TRY(accessibility_menu->try_add_action(deuteranopia_accessibility_action));
    TRY(accessibility_menu->try_add_action(deuteranomaly_accessibility_action));
    TRY(accessibility_menu->try_add_action(achromatopsia_accessibility_action));
    TRY(accessibility_menu->try_add_action(achromatomaly_accessibility_action));

    auto help_menu = TRY(window->try_add_menu("&Help"));
    help_menu->add_action(GUI::CommonActions::make_about_action("Magnifier", app_icon, window));

    window->show();

    return app->exec();
}
