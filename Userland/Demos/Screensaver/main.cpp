/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibCore/System.h>
#include <LibDesktop/AppFile.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Icon.h>
#include <LibGUI/IconView.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Model.h>
#include <LibGUI/Process.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

class ScreensaverAppsModel final : public GUI::Model {
public:
    ScreensaverAppsModel()
    {
        Desktop::AppFile::for_each([&](Desktop::AppFile& app_file) {
            if (app_file.category() != "Demos/Screensaver"sv)
                return;
            m_apps.append(app_file);
        });

        quick_sort(m_apps, [](auto& a, auto& b) { return a->name() < b->name(); });
    }

    virtual int row_count(GUI::ModelIndex const&) const override { return static_cast<int>(m_apps.size()); }
    virtual int column_count(GUI::ModelIndex const&) const override { return 1; }

    virtual GUI::ModelIndex index(int row, int column, GUI::ModelIndex const&) const override
    {
        if (row < 0 || row >= static_cast<int>(m_apps.size()))
            return {};
        return create_index(row, column, &m_apps[row]);
    }

    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role) const override
    {
        auto const& app = m_apps[index.row()];
        if (role == GUI::ModelRole::Icon)
            return app->icon();

        if (role == GUI::ModelRole::Display) {
            if (app->name().ends_with(" Screensaver"sv))
                return app->name().substring(0, app->name().length() - " Screensaver"sv.length());
            return app->name();
        }

        if (role == GUI::ModelRole::Custom)
            return app->executable();

        return {};
    }

private:
    Vector<NonnullRefPtr<Desktop::AppFile>> m_apps;
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio thread recvfd sendfd rpath cpath wpath unix proc exec"));

    auto app = TRY(GUI::Application::create(arguments));

    TRY(Core::System::pledge("stdio thread recvfd sendfd rpath cpath wpath proc exec"));

    auto app_icon = GUI::Icon::default_icon("app-screensaver"sv);

    auto window = GUI::Window::construct();
    window->set_title("Screensaver");
    window->resize(360, 240);

    auto file_menu = window->add_menu("&File"_string);
    file_menu->add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    }));

    auto view_menu = window->add_menu("&View"_string);
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    }));

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_about_action("Screensaver"_string, app_icon, window));

    auto main_widget = window->set_main_widget<GUI::Widget>();
    main_widget->set_fill_with_background_color(true);
    main_widget->set_layout<GUI::VerticalBoxLayout>();

    auto& icon_view = main_widget->add<GUI::IconView>();
    icon_view.set_should_hide_unnecessary_scrollbars(true);
    auto model = adopt_ref(*new ScreensaverAppsModel);
    icon_view.set_model(*model);

    icon_view.on_activation = [&](GUI::ModelIndex const& index) {
        auto executable = model->data(index, GUI::ModelRole::Custom).as_string();
        GUI::Process::spawn_or_show_error(window, executable);
    };

    window->set_icon(app_icon.bitmap_for_size(16));
    window->show();

    return app->exec();
}
