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

    auto app = TRY(GUI::Application::try_create(arguments));
    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-catdog"));

    TRY(Core::System::pledge("stdio recvfd sendfd rpath"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto window = TRY(GUI::Window::try_create());
    window->set_title("CatDog Demo");
    window->resize(32, 32);
    window->set_frameless(true);
    window->set_resizable(false);
    window->set_has_alpha_channel(true);
    window->set_alpha_hit_threshold(1.0f);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto catdog_widget = TRY(window->try_set_main_widget<CatDog>());
    (void)TRY(catdog_widget->try_set_layout<GUI::VerticalBoxLayout>());
    catdog_widget->layout()->set_spacing(0);

    auto context_menu = TRY(GUI::Menu::try_create());
    TRY(context_menu->try_add_action(GUI::CommonActions::make_about_action("CatDog Demo", app_icon, window)));
    TRY(context_menu->try_add_separator());
    TRY(context_menu->try_add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); })));

    window->show();
    catdog_widget->start_timer(250, Core::TimerShouldFireWhenNotVisible::Yes);
    catdog_widget->start_the_timer(); // timer for "mouse sleep detection"

    auto advice_window = TRY(GUI::Window::try_create());
    advice_window->set_title("CatDog Advice");
    advice_window->resize(225, 50);
    advice_window->set_frameless(true);
    advice_window->set_resizable(false);
    advice_window->set_has_alpha_channel(true);
    advice_window->set_alpha_hit_threshold(1.0f);

    auto advice_widget = TRY(advice_window->try_set_main_widget<SpeechBubble>());
    (void)TRY(advice_widget->try_set_layout<GUI::VerticalBoxLayout>());
    advice_widget->layout()->set_spacing(0);

    auto advice_timer = TRY(Core::Timer::try_create());
    advice_timer->set_interval(15000);
    advice_timer->set_single_shot(true);
    advice_timer->on_timeout = [&] {
        window->move_to_front();
        advice_window->move_to_front();
        catdog_widget->set_roaming(false);
        advice_window->move_to(window->x() - advice_window->width() / 2, window->y() - advice_window->height());
        advice_window->show();
    };
    advice_timer->start();

    advice_widget->on_dismiss = [&] {
        catdog_widget->set_roaming(true);
        advice_window->hide();
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
