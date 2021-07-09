/*
 * Copyright (c) 2021, Richard Gráčik <r.gracik@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CatDog.h"
#include "SpeechBubble.h"
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <LibCore/DirIterator.h>
#include <LibCore/Timer.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>

struct SkinMetadata {
    String name;
    String path;
};

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd rpath wpath cpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);
    auto app_icon = GUI::Icon::default_icon("app-catdog");

    if (pledge("stdio recvfd sendfd rpath", nullptr) < 0) {
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

    auto window = GUI::Window::construct();
    window->set_title("CatDog Demo");
    window->resize(32, 32);
    window->set_frameless(true);
    window->set_resizable(false);
    window->set_has_alpha_channel(true);
    window->set_alpha_hit_threshold(1.0f);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto& catdog_widget = window->set_main_widget<CatDog>();
    catdog_widget.set_layout<GUI::VerticalBoxLayout>();
    catdog_widget.layout()->set_spacing(0);

    auto context_menu = GUI::Menu::construct();

    Vector<SkinMetadata> g_skins;
    RefPtr<GUI::Menu> g_skins_menu;
    GUI::ActionGroup g_skins_group;
    g_skins_group.set_exclusive(true);
    g_skins_group.set_unchecking_allowed(false);

    g_skins_menu = &context_menu->add_submenu("Skins");

    {
        Core::DirIterator dt("/res/icons/catdog", Core::DirIterator::SkipDots);
        while (dt.has_next()) {
            auto skin_name = dt.next_path();
            auto skin_path = String::formatted("/res/icons/catdog/{}", skin_name);
            g_skins.append({ LexicalPath::title(skin_name), skin_path });
        }
        quick_sort(g_skins, [](auto& a, auto& b) { return a.name < b.name; });
    }

    auto current_skin_name = catdog_widget.skin_name();

    {
        int skin_identifier = 0;
        for (auto& skin : g_skins) {
            auto action = GUI::Action::create_checkable(skin.name, [&, skin_identifier](auto&) {
                auto& skin = g_skins[skin_identifier];
                dbgln("Skin switched to {} at path {}", skin.name, skin.path);
                catdog_widget.set_skin(skin.name, skin.path);
            });
            if (skin.name == current_skin_name)
                action->set_checked(true);
            g_skins_group.add_action(action);
            g_skins_menu->add_action(action);
            ++skin_identifier;
        }
    }
    context_menu->add_action(GUI::CommonActions::make_about_action("CatDog Demo", app_icon, window));
    context_menu->add_separator();
    context_menu->add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); }));

    window->show();
    catdog_widget.track_cursor_globally();
    catdog_widget.start_timer(250, Core::TimerShouldFireWhenNotVisible::Yes);
    catdog_widget.start_the_timer(); // timer for "mouse sleep detection"

    auto advice_window = GUI::Window::construct();
    advice_window->set_title("CatDog Advice");
    advice_window->resize(225, 50);
    advice_window->set_frameless(true);
    advice_window->set_resizable(false);
    advice_window->set_has_alpha_channel(true);
    advice_window->set_alpha_hit_threshold(1.0f);

    auto& advice_widget = advice_window->set_main_widget<SpeechBubble>();
    advice_widget.set_layout<GUI::VerticalBoxLayout>();
    advice_widget.layout()->set_spacing(0);

    auto advice_timer = Core::Timer::construct();
    advice_timer->set_interval(15000);
    advice_timer->set_single_shot(true);
    advice_timer->on_timeout = [&] {
        window->move_to_front();
        advice_window->move_to_front();
        catdog_widget.set_roaming(false);
        advice_window->move_to(window->x() - advice_window->width() / 2, window->y() - advice_window->height());
        advice_window->show();
    };
    advice_timer->start();

    advice_widget.on_dismiss = [&] {
        catdog_widget.set_roaming(true);
        advice_window->hide();
        advice_timer->start();
    };

    // Let users toggle the advice functionality by clicking on catdog.
    catdog_widget.on_click = [&] {
        if (advice_timer->is_active())
            advice_timer->stop();
        else
            advice_timer->start();
    };

    catdog_widget.on_context_menu_request = [&](GUI::ContextMenuEvent& event) {
        if (catdog_widget.rect().contains(event.position()))
            context_menu->popup(event.screen_position());
    };

    return app->exec();
}
