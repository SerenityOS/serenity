/*
 * Copyright (c) 2021, Richard Gráčik <r.gracik@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CatDog.h"
#include "SpeechBubble.h"
#include <LibCore/System.h>
#include <LibCore/Timer.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath wpath cpath unix"));

    auto app = TRY(GUI::Application::create(arguments));
    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-catdog"sv));

    auto catdog_icon_sleep = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/catdog-sleeping.png"sv));
    auto catdog_icon_wake = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/catdog-wake-up.png"sv));

    TRY(Core::System::pledge("stdio recvfd sendfd rpath"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/sys/kernel/processes", "r"));
    // FIXME: For some reason, this is needed in the /sys/kernel/processes shenanigans.
    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto window = GUI::Window::construct();
    window->set_title("CatDog Demo");
    window->resize(32, 32);
    window->set_frameless(true);
    window->set_resizable(false);
    window->set_has_alpha_channel(true);
    window->set_alpha_hit_threshold(1.0f);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto catdog_widget = TRY(CatDog::create());
    window->set_main_widget(catdog_widget);
    catdog_widget->set_layout<GUI::VerticalBoxLayout>(GUI::Margins {}, 0);

    auto context_menu = GUI::Menu::construct();
    context_menu->add_action(GUI::CommonActions::make_about_action("CatDog Demo"_string, app_icon, window));
    auto sleep_action = GUI::Action::create("Put CatDog to sleep...", catdog_icon_sleep, [&](GUI::Action&) {
        catdog_widget->set_sleeping(!catdog_widget->is_sleeping());
    });
    context_menu->add_action(sleep_action);

    context_menu->add_separator();
    context_menu->add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); }, GUI::CommonActions::QuitAltShortcut::None));

    window->show();
    window->set_always_on_top();
    catdog_widget->start_timer(250, Core::TimerShouldFireWhenNotVisible::Yes);

    auto advice_window = GUI::Window::construct();
    advice_window->set_title("CatDog Advice");
    advice_window->resize(225, 50);
    advice_window->set_frameless(true);
    advice_window->set_resizable(false);
    advice_window->set_has_alpha_channel(true);
    advice_window->set_alpha_hit_threshold(1.0f);

    auto advice_widget = advice_window->set_main_widget<SpeechBubble>(catdog_widget);
    advice_widget->set_layout<GUI::VerticalBoxLayout>(GUI::Margins {}, 0);

    auto advice_timer = Core::Timer::create_single_shot(15'000, [&] {
        window->move_to_front();
        advice_window->move_to_front();
        catdog_widget->set_roaming(false);
        advice_window->move_to(window->x() - advice_window->width() / 2, window->y() - advice_window->height());
        advice_window->show();
        advice_window->set_always_on_top();
    });
    advice_timer->start();

    advice_widget->on_dismiss = [&] {
        catdog_widget->set_roaming(true);
        advice_window->hide();
        advice_timer->start();
    };

    catdog_widget->on_state_change = [&] {
        sleep_action->set_text(catdog_widget->is_sleeping() ? "Wake CatDog..." : "Put CatDog to sleep...");
        sleep_action->set_icon(catdog_widget->is_sleeping() ? catdog_icon_wake : catdog_icon_sleep);

        // Reset advice timer to prevent waking too quickly.
        if (catdog_widget->is_sleeping())
            advice_timer->start();
    };

    // Let users toggle the advice functionality by clicking on catdog.
    catdog_widget->on_click = [&] {
        if (advice_timer->is_active())
            advice_timer->stop();
        else
            advice_timer->start();
    };

    catdog_widget->on_context_menu_request = [&](GUI::ContextMenuEvent& event) {
        if (catdog_widget->rect().contains(event.position()))
            context_menu->popup(event.screen_position());
    };

    return app->exec();
}
