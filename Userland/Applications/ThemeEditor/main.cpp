/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Jakob-Niklas See <git@nwex.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PreviewWidget.h"
#include <LibCore/ConfigFile.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/ColorInput.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Model.h>
#include <LibGUI/Window.h>
#include <unistd.h>

class ColorRoleModel final : public GUI::Model {
public:
    virtual int row_count(const GUI::ModelIndex&) const { return m_color_roles.size(); }
    virtual int column_count(const GUI::ModelIndex&) const { return 1; }
    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role = GUI::ModelRole::Display) const
    {
        if (role == GUI::ModelRole::Display)
            return Gfx::to_string(m_color_roles[(size_t)index.row()]);
        return {};
    }

    explicit ColorRoleModel(const Vector<Gfx::ColorRole>& color_roles)
        : m_color_roles(color_roles)
    {
    }

    Gfx::ColorRole color_role(const GUI::ModelIndex& index) const
    {
        return m_color_roles[index.row()];
    }

    Gfx::ColorRole color_role(size_t index) const
    {
        return m_color_roles[index];
    }

private:
    const Vector<Gfx::ColorRole>& m_color_roles;
};

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd thread rpath cpath wpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

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

    Gfx::Palette preview_palette = app->palette();

    auto window = GUI::Window::construct();

    Vector<Gfx::ColorRole> color_roles;
#define __ENUMERATE_COLOR_ROLE(role) color_roles.append(Gfx::ColorRole::role);
    ENUMERATE_COLOR_ROLES(__ENUMERATE_COLOR_ROLE)
#undef __ENUMERATE_COLOR_ROLE

    auto& file_menu = window->add_menu("&File");

    Optional<String> path = {};

    auto save_to_result = [&](FileSystemAccessClient::Result result) {
        if (result.error != 0)
            return;

        path = result.chosen_file;

        auto theme = Core::ConfigFile::open(*result.chosen_file, *result.fd);
        for (auto role : color_roles) {
            theme->write_entry("Colors", to_string(role), preview_palette.color(role).to_string());
        }

        theme->sync();
    };

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

    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();

    auto& preview_widget = main_widget.add<ThemeEditor::PreviewWidget>(app->palette());
    preview_widget.set_fixed_size(480, 360);

    auto& horizontal_container = main_widget.add<GUI::Widget>();
    horizontal_container.set_layout<GUI::HorizontalBoxLayout>();
    horizontal_container.set_fixed_size(480, 20);

    auto& combo_box = horizontal_container.add<GUI::ComboBox>();
    auto& color_input = horizontal_container.add<GUI::ColorInput>();

    combo_box.set_only_allow_values_from_model(true);
    combo_box.set_model(adopt_ref(*new ColorRoleModel(color_roles)));
    combo_box.on_change = [&](auto&, auto& index) {
        auto role = static_cast<ColorRoleModel const*>(index.model())->color_role(index);
        color_input.set_color(preview_palette.color(role));
    };

    combo_box.set_selected_index((size_t)Gfx::ColorRole::Window - 1);

    color_input.on_change = [&] {
        auto role = static_cast<ColorRoleModel const*>(combo_box.model())->color_role(combo_box.selected_index());
        preview_palette.set_color(role, color_input.color());
        preview_widget.set_preview_palette(preview_palette);
    };
    color_input.set_color(preview_palette.color(Gfx::ColorRole::Window));

    window->resize(480, 385);
    window->set_resizable(false);
    window->show();
    window->set_title("Theme Editor");
    window->set_icon(app_icon.bitmap_for_size(16));
    return app->exec();
}
