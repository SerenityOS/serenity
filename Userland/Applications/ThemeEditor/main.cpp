/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Jakob-Niklas See <git@nwex.de>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PreviewWidget.h"
#include <Applications/ThemeEditor/ThemeEditorGML.h>
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
#include <LibGUI/Menubar.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextBox.h>
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

class MetricRoleModel final : public GUI::ItemListModel<Gfx::MetricRole> {
public:
    explicit MetricRoleModel(Vector<Gfx::MetricRole> const& data)
        : ItemListModel<Gfx::MetricRole>(data)
    {
    }

    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role) const override
    {
        if (role == GUI::ModelRole::Display)
            return Gfx::to_string(static_cast<Gfx::MetricRole>(m_data[(size_t)index.row()]));
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

    Vector<Gfx::MetricRole> metric_roles;
#define __ENUMERATE_METRIC_ROLE(role) metric_roles.append(Gfx::MetricRole::role);
    ENUMERATE_METRIC_ROLES(__ENUMERATE_METRIC_ROLE)
#undef __ENUMERATE_METRIC_ROLE

    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.load_from_gml(theme_editor_gml);

    auto& preview_widget = main_widget.find_descendant_of_type_named<GUI::Frame>("preview_frame")
                               ->add<ThemeEditor::PreviewWidget>(startup_preview_palette);
    auto& color_combo_box = *main_widget.find_descendant_of_type_named<GUI::ComboBox>("color_combo_box");
    auto& color_input = *main_widget.find_descendant_of_type_named<GUI::ColorInput>("color_input");
    auto& metric_combo_box = *main_widget.find_descendant_of_type_named<GUI::ComboBox>("metric_combo_box");
    auto& metric_input = *main_widget.find_descendant_of_type_named<GUI::SpinBox>("metric_input");

    color_combo_box.set_model(adopt_ref(*new ColorRoleModel(color_roles)));
    color_combo_box.on_change = [&](auto&, auto& index) {
        auto role = index.model()->data(index, GUI::ModelRole::Custom).to_color_role();
        color_input.set_color(preview_widget.preview_palette().color(role));
    };
    color_combo_box.set_selected_index((size_t)Gfx::ColorRole::Window - 1);

    color_input.on_change = [&] {
        auto role = color_combo_box.model()->index(color_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_color_role();
        auto preview_palette = preview_widget.preview_palette();
        preview_palette.set_color(role, color_input.color());
        preview_widget.set_preview_palette(preview_palette);
    };
    color_input.set_color(startup_preview_palette.color(Gfx::ColorRole::Window));

    metric_combo_box.set_model(adopt_ref(*new MetricRoleModel(metric_roles)));
    metric_combo_box.on_change = [&](auto&, auto& index) {
        auto role = index.model()->data(index, GUI::ModelRole::Custom).to_metric_role();
        metric_input.set_value(preview_widget.preview_palette().metric(role), GUI::AllowCallback::No);
    };
    metric_combo_box.set_selected_index((size_t)Gfx::MetricRole::TitleButtonHeight - 1);

    metric_input.on_change = [&](int value) {
        auto role = metric_combo_box.model()->index(metric_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_metric_role();
        auto preview_palette = preview_widget.preview_palette();
        preview_palette.set_metric(role, value);
        preview_widget.set_preview_palette(preview_palette);
    };
    metric_input.set_value(startup_preview_palette.metric(Gfx::MetricRole::TitleButtonHeight), GUI::AllowCallback::No);

    auto& file_menu = window->add_menu("&File");

    auto update_window_title = [&] {
        window->set_title(String::formatted("{} - Theme Editor", path.value_or("Untitled")));
    };

    preview_widget.on_theme_load_from_file = [&](String const& new_path) {
        path = new_path;
        update_window_title();

        auto selected_color_role = color_combo_box.model()->index(color_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_color_role();
        color_input.set_color(preview_widget.preview_palette().color(selected_color_role));

        auto selected_metric_role = metric_combo_box.model()->index(metric_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_metric_role();
        metric_input.set_value(preview_widget.preview_palette().metric(selected_metric_role), GUI::AllowCallback::No);
    };

    auto save_to_result = [&](FileSystemAccessClient::Result const& result) {
        if (result.error != 0)
            return;

        path = result.chosen_file;
        update_window_title();

        auto theme = Core::ConfigFile::open(*result.chosen_file, *result.fd);
        for (auto role : color_roles) {
            theme->write_entry("Colors", to_string(role), preview_widget.preview_palette().color(role).to_string());
        }

        for (auto role : metric_roles) {
            theme->write_num_entry("Metrics", to_string(role), preview_widget.preview_palette().metric(role));
        }

#define __ENUMERATE_PATH_ROLE(role) \
    theme->write_entry("Paths", #role, preview_widget.preview_palette().path(Gfx::PathRole::role));
        ENUMERATE_PATH_ROLES(__ENUMERATE_PATH_ROLE)
#undef __ENUMERATE_PATH_ROLE

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

    update_window_title();

    window->resize(480, 460);
    window->set_resizable(false);
    window->show();
    window->set_icon(app_icon.bitmap_for_size(16));
    return app->exec();
}
