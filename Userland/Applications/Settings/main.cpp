/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/System.h>
#include <LibDesktop/AppFile.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Icon.h>
#include <LibGUI/IconView.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Model.h>
#include <LibGUI/Process.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <serenity.h>
#include <stdio.h>
#include <unistd.h>

// Modeled after PlaylistWidget.
enum class SettingsAppsModelCustomRole {
    _DONOTUSE = (int)GUI::ModelRole::Custom,
    RequiresRoot
};

class SettingsAppsModel final : public GUI::Model {
public:
    SettingsAppsModel()
    {
        Desktop::AppFile::for_each([&](Desktop::AppFile& app_file) {
            if (app_file.category() != "Settings")
                return;
            m_apps.append(app_file);
        });
        quick_sort(m_apps, [](auto& a, auto& b) { return a->name() < b->name(); });
    }
    virtual int row_count(GUI::ModelIndex const&) const override { return m_apps.size(); }
    virtual int column_count(GUI::ModelIndex const&) const override { return 1; }

    virtual GUI::ModelIndex index(int row, int column, GUI::ModelIndex const&) const override
    {
        if (row < 0 || row >= (int)m_apps.size())
            return {};
        return create_index(row, column, &m_apps[row]);
    }

    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role) const override
    {
        auto& app = m_apps[index.row()];
        if (role == GUI::ModelRole::Icon)
            return app->icon();

        if (role == GUI::ModelRole::Display) {
            DeprecatedString name;

            if (app->name().ends_with(" Settings"sv))
                name = app->name().substring(0, app->name().length() - " Settings"sv.length());
            else
                name = app->name();

            return name;
        }

        if (role == GUI::ModelRole::Custom)
            return app->executable();

        if (role == static_cast<GUI::ModelRole>(SettingsAppsModelCustomRole::RequiresRoot))
            return app->requires_root();

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

    auto app_icon = GUI::Icon::default_icon("app-settings"sv);

    auto window = TRY(GUI::Window::try_create());
    window->set_title("Settings");
    window->resize(420, 265);

    auto file_menu = TRY(window->try_add_menu("&File"_short_string));
    file_menu->add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    }));

    auto help_menu = TRY(window->try_add_menu("&Help"_short_string));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_command_palette_action(window)));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Settings", app_icon, window)));

    auto main_widget = TRY(window->set_main_widget<GUI::Widget>());
    main_widget->set_fill_with_background_color(true);
    main_widget->set_layout<GUI::VerticalBoxLayout>();

    auto icon_view = TRY(main_widget->try_add<GUI::IconView>());
    icon_view->set_should_hide_unnecessary_scrollbars(true);
    auto model = adopt_ref(*new SettingsAppsModel);
    icon_view->set_model(*model);

    auto activation_func = [&](GUI::ModelIndex const& index) {
        auto executable = model->data(index, GUI::ModelRole::Custom).as_string();
        auto requires_root = model->data(index, static_cast<GUI::ModelRole>(SettingsAppsModelCustomRole::RequiresRoot)).as_bool();

        auto launch_origin_rect = icon_view->to_widget_rect(icon_view->content_rect(index)).translated(icon_view->screen_relative_rect().location());
        setenv("__libgui_launch_origin_rect", DeprecatedString::formatted("{},{},{},{}", launch_origin_rect.x(), launch_origin_rect.y(), launch_origin_rect.width(), launch_origin_rect.height()).characters(), 1);

        if (requires_root)
            GUI::Process::spawn_or_show_error(window, "/bin/Escalator"sv, Array { executable });
        else
            GUI::Process::spawn_or_show_error(window, executable);
    };

    auto settings_item_context_menu = TRY(GUI::Menu::try_create(TRY("Directory View Directory"_string)));
    auto settings_item_open_action
        = GUI::Action::create(
            "Open",
            {},
            TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-settings.png"sv)),
            [&](auto&) {
                activation_func(icon_view->cursor_index());
            });

    auto settings_item_create_shortcut_action
        = GUI::Action::create(
            "Create Desktop &Shortcut",
            {},
            TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-symlink.png"sv)),
            [&](auto&) {
                auto executable = model->data(icon_view->cursor_index(), GUI::ModelRole::Custom).as_string();
                auto destination = DeprecatedString::formatted("{}/{} Settings", Core::StandardPaths::desktop_directory(), model->data(icon_view->cursor_index(), GUI::ModelRole::Display).as_string());
                if (auto result = FileSystem::link_file(destination, executable); result.is_error()) {
                    GUI::MessageBox::show(window, DeprecatedString::formatted("Could not create settings item desktop shortcut:\n{}", result.error()), "Settings"sv,
                        GUI::MessageBox::Type::Error);
                }
            });

    TRY(settings_item_context_menu->try_add_action(settings_item_open_action));
    TRY(settings_item_context_menu->try_add_action(settings_item_create_shortcut_action));

    icon_view->on_context_menu_request = [&](GUI::ModelIndex const& index, GUI::ContextMenuEvent const& event) {
        (void)index;
        if (icon_view->selection().is_empty())
            return;
        settings_item_context_menu->popup(event.screen_position());
    };

    icon_view->on_activation = [&](GUI::ModelIndex const& index) {
        activation_func(index);
    };

    auto statusbar = TRY(main_widget->try_add<GUI::Statusbar>());

    icon_view->on_selection_change = [&] {
        auto index = icon_view->selection().first();
        if (!index.is_valid()) {
            statusbar->set_text({});
            return;
        }

        auto& app = *(NonnullRefPtr<Desktop::AppFile>*)index.internal_data();
        statusbar->set_text(app->description());
    };

    window->set_icon(app_icon.bitmap_for_size(16));

    window->show();
    return app->exec();
}
