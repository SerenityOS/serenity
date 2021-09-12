/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Jakob-Niklas See <git@nwex.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PreviewWidget.h"
#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/ColorInput.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Icon.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Window.h>
#include <unistd.h>

class ColorRoleModel final : public GUI::ItemListModel<Gfx::ColorRole> {
public:
    explicit ColorRoleModel(const Vector<Gfx::ColorRole>& data)
        : ItemListModel<Gfx::ColorRole>(data)
    {
    }

    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role) const override
    {
        if (role == GUI::ModelRole::Display)
            return Gfx::to_string(m_data[(size_t)index.row()]);
        if (role == GUI::ModelRole::Custom)
            return m_data[(size_t)index.row()];

        return ItemListModel::data(index, role);
    }
};

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd thread rpath cpath wpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    char const* file_to_edit = nullptr;

    Core::ArgsParser parser;
    parser.add_positional_argument(file_to_edit, "Theme file to edit", "file", Core::ArgsParser::Required::No);
    parser.parse(argc, argv);

    Optional<String> path = {};

    if (file_to_edit) {
        path = Core::File::absolute_path(file_to_edit);
        if (Core::File::exists(*path)) {
            dbgln("unveil for: {}", *path);
            if (unveil(path->characters(), "r") < 0) {
                perror("unveil");
                return 1;
            }
        }
    }

    if (pledge("stdio recvfd sendfd thread rpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/tmp/portal/filesystemaccess", "rw") < 0) {
        perror("unveil");
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

    auto app_icon = GUI::Icon::default_icon("app-theme-editor");

    Gfx::Palette startup_preview_palette = file_to_edit ? Gfx::Palette(Gfx::PaletteImpl::create_with_anonymous_buffer(Gfx::load_system_theme(*path))) : app->palette();

    auto window = GUI::Window::construct();

    Vector<Gfx::ColorRole> color_roles;
#define __ENUMERATE_COLOR_ROLE(role) color_roles.append(Gfx::ColorRole::role);
    ENUMERATE_COLOR_ROLES(__ENUMERATE_COLOR_ROLE)
#undef __ENUMERATE_COLOR_ROLE

    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();

    auto& preview_widget = main_widget.add<ThemeEditor::PreviewWidget>(startup_preview_palette);
    preview_widget.set_fixed_size(480, 360);

    auto& horizontal_container = main_widget.add<GUI::Widget>();
    horizontal_container.set_layout<GUI::HorizontalBoxLayout>();
    horizontal_container.set_fixed_size(480, 20);

    auto& combo_box = horizontal_container.add<GUI::ComboBox>();
    auto& color_input = horizontal_container.add<GUI::ColorInput>();

    combo_box.set_only_allow_values_from_model(true);
    combo_box.set_model(adopt_ref(*new ColorRoleModel(color_roles)));
    combo_box.on_change = [&](auto&, auto& index) {
        auto role = index.model()->data(index, GUI::ModelRole::Custom).to_color_role();
        color_input.set_color(preview_widget.preview_palette().color(role));
    };

    combo_box.set_selected_index((size_t)Gfx::ColorRole::Window - 1);

    color_input.on_change = [&] {
        auto role = combo_box.model()->index(combo_box.selected_index()).data(GUI::ModelRole::Custom).to_color_role();
        auto preview_palette = preview_widget.preview_palette();
        preview_palette.set_color(role, color_input.color());
        preview_widget.set_preview_palette(preview_palette);
    };
    color_input.set_color(startup_preview_palette.color(Gfx::ColorRole::Window));

    auto& file_menu = window->add_menu("&File");

    preview_widget.on_theme_load_from_file = [&](String const& new_path) {
        path = new_path;
    };

    auto save_to_result = [&](FileSystemAccessClient::Result const& result) {
        if (result.error != 0)
            return;

        path = result.chosen_file;

        auto theme = Core::ConfigFile::open(*result.chosen_file, *result.fd);
        for (auto role : color_roles) {
            theme->write_entry("Colors", to_string(role), preview_widget.preview_palette().color(role).to_string());
        }

        theme->sync();
    };

    file_menu.add_action(GUI::CommonActions::make_open_action([&](auto&) {
        auto result = FileSystemAccessClient::Client::the().open_file(window->window_id(), "Select theme file", "/res/themes");
        if (result.error != 0)
            return;

        preview_widget.set_theme_from_file(*result.chosen_file, *result.fd);
    }));

    file_menu.add_action(GUI::CommonActions::make_save_action([&](auto&) {
        if (path.has_value()) {
            save_to_result(FileSystemAccessClient::Client::the().request_file(window->window_id(), *path, Core::OpenMode::WriteOnly | Core::OpenMode::Truncate));
        } else {
            save_to_result(FileSystemAccessClient::Client::the().save_file(window->window_id(), "Theme", "ini"));
        }
    }));

    file_menu.add_action(GUI::CommonActions::make_save_as_action([&](auto&) {
        save_to_result(FileSystemAccessClient::Client::the().save_file(window->window_id(), "Theme", "ini"));
    }));

    file_menu.add_separator();
    file_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); }));

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Theme Editor", app_icon, window));

    window->resize(480, 385);
    window->set_resizable(false);
    window->show();
    window->set_title("Theme Editor");
    window->set_icon(app_icon.bitmap_for_size(16));
    return app->exec();
}
