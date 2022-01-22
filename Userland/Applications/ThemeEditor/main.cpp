/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Jakob-Niklas See <git@nwex.de>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Antonio Di Stefano <tonio9681@gmail.com>
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PreviewWidget.h"
#include <Applications/ThemeEditor/ThemeEditorGML.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/System.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ColorInput.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Icon.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Window.h>
#include <LibGfx/Filters/ColorBlindnessFilter.h>
#include <LibMain/Main.h>
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

struct AlignmentValue {
    String title;
    Gfx::TextAlignment setting_value;
};

class AlignmentModel final : public GUI::Model {

public:
    AlignmentModel()
    {
        m_alignments.empend("Center", Gfx::TextAlignment::Center);
        m_alignments.empend("Left", Gfx::TextAlignment::CenterLeft);
        m_alignments.empend("Right", Gfx::TextAlignment::CenterRight);
    }

    virtual ~AlignmentModel() = default;

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return 3; }
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return 2; }

    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role) const override
    {
        if (role == GUI::ModelRole::Display)
            return m_alignments[index.row()].title;
        if (role == GUI::ModelRole::Custom)
            return m_alignments[index.row()].setting_value;

        return {};
    }

private:
    Vector<AlignmentValue> m_alignments;
};

class AlignmentRoleModel final : public GUI::ItemListModel<Gfx::AlignmentRole> {
public:
    explicit AlignmentRoleModel(Vector<Gfx::AlignmentRole> const& data)
        : ItemListModel<Gfx::AlignmentRole>(data)
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

class FlagRoleModel final : public GUI::ItemListModel<Gfx::FlagRole> {
public:
    explicit FlagRoleModel(Vector<Gfx::FlagRole> const& data)
        : ItemListModel<Gfx::FlagRole>(data)
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
            return Gfx::to_string(m_data[(size_t)index.row()]);
        if (role == GUI::ModelRole::Custom)
            return m_data[(size_t)index.row()];

        return ItemListModel::data(index, role);
    }
};

class PathRoleModel final : public GUI::ItemListModel<Gfx::PathRole> {
public:
    explicit PathRoleModel(Vector<Gfx::PathRole> const& data)
        : ItemListModel<Gfx::PathRole>(data)
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

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd thread rpath cpath wpath unix", nullptr));

    auto app = GUI::Application::construct(arguments);

    char const* file_to_edit = nullptr;

    Core::ArgsParser parser;
    parser.add_positional_argument(file_to_edit, "Theme file to edit", "file", Core::ArgsParser::Required::No);
    parser.parse(arguments);

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

    TRY(Core::System::pledge("stdio recvfd sendfd thread rpath unix", nullptr));
    TRY(Core::System::unveil("/tmp/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("app-theme-editor");

    Gfx::Palette startup_preview_palette = file_to_edit ? Gfx::Palette(Gfx::PaletteImpl::create_with_anonymous_buffer(Gfx::load_system_theme(*path))) : app->palette();

    auto window = GUI::Window::construct();

    Vector<Gfx::ColorRole> color_roles;
#define __ENUMERATE_COLOR_ROLE(role) color_roles.append(Gfx::ColorRole::role);
    ENUMERATE_COLOR_ROLES(__ENUMERATE_COLOR_ROLE)
#undef __ENUMERATE_COLOR_ROLE

    Vector<Gfx::AlignmentRole> alignment_roles;
#define __ENUMERATE_ALIGNMENT_ROLE(role) alignment_roles.append(Gfx::AlignmentRole::role);
    ENUMERATE_ALIGNMENT_ROLES(__ENUMERATE_ALIGNMENT_ROLE)
#undef __ENUMERATE_ALIGNMENT_ROLE

    Vector<Gfx::FlagRole> flag_roles;
#define __ENUMERATE_FLAG_ROLE(role) flag_roles.append(Gfx::FlagRole::role);
    ENUMERATE_FLAG_ROLES(__ENUMERATE_FLAG_ROLE)
#undef __ENUMERATE_FLAG_ROLE

    Vector<Gfx::MetricRole> metric_roles;
#define __ENUMERATE_METRIC_ROLE(role) metric_roles.append(Gfx::MetricRole::role);
    ENUMERATE_METRIC_ROLES(__ENUMERATE_METRIC_ROLE)
#undef __ENUMERATE_METRIC_ROLE

    Vector<Gfx::PathRole> path_roles;
#define __ENUMERATE_PATH_ROLE(role) path_roles.append(Gfx::PathRole::role);
    ENUMERATE_PATH_ROLES(__ENUMERATE_PATH_ROLE)
#undef __ENUMERATE_PATH_ROLE

    auto main_widget = TRY(window->try_set_main_widget<GUI::Widget>());
    main_widget->load_from_gml(theme_editor_gml);

    auto& preview_widget = main_widget->find_descendant_of_type_named<GUI::Frame>("preview_frame")
                               ->add<ThemeEditor::PreviewWidget>(startup_preview_palette);
    auto& color_combo_box = *main_widget->find_descendant_of_type_named<GUI::ComboBox>("color_combo_box");
    auto& color_input = *main_widget->find_descendant_of_type_named<GUI::ColorInput>("color_input");

    auto& alignment_combo_box = *main_widget->find_descendant_of_type_named<GUI::ComboBox>("alignment_combo_box");
    auto& alignment_input = *main_widget->find_descendant_of_type_named<GUI::ComboBox>("alignment_input");

    auto& flag_combo_box = *main_widget->find_descendant_of_type_named<GUI::ComboBox>("flag_combo_box");
    auto& flag_input = *main_widget->find_descendant_of_type_named<GUI::CheckBox>("flag_input");

    auto& metric_combo_box = *main_widget->find_descendant_of_type_named<GUI::ComboBox>("metric_combo_box");
    auto& metric_input = *main_widget->find_descendant_of_type_named<GUI::SpinBox>("metric_input");

    auto& path_combo_box = *main_widget->find_descendant_of_type_named<GUI::ComboBox>("path_combo_box");
    auto& path_input = *main_widget->find_descendant_of_type_named<GUI::TextBox>("path_input");
    auto& path_picker_button = *main_widget->find_descendant_of_type_named<GUI::Button>("path_picker_button");

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

    alignment_combo_box.set_model(adopt_ref(*new AlignmentRoleModel(alignment_roles)));
    alignment_combo_box.on_change = [&](auto&, auto& index) {
        auto role = index.model()->data(index, GUI::ModelRole::Custom).to_alignment_role();
        alignment_input.set_selected_index((size_t)preview_widget.preview_palette().alignment(role), GUI::AllowCallback::No);
    };
    alignment_combo_box.set_selected_index((size_t)Gfx::AlignmentRole::TitleAlignment - 1);

    alignment_input.set_only_allow_values_from_model(true);
    alignment_input.set_model(adopt_ref(*new AlignmentModel()));
    alignment_input.set_selected_index((size_t)startup_preview_palette.alignment(Gfx::AlignmentRole::TitleAlignment));
    alignment_input.on_change = [&](auto&, auto& index) {
        auto role = alignment_combo_box.model()->index(alignment_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_alignment_role();
        auto preview_palette = preview_widget.preview_palette();

        preview_palette.set_alignment(role, index.data(GUI::ModelRole::Custom).to_text_alignment(Gfx::TextAlignment::CenterLeft));
        preview_widget.set_preview_palette(preview_palette);
    };

    flag_combo_box.set_model(adopt_ref(*new FlagRoleModel(flag_roles)));
    flag_combo_box.on_change = [&](auto&, auto& index) {
        auto role = index.model()->data(index, GUI::ModelRole::Custom).to_flag_role();
        flag_input.set_checked(preview_widget.preview_palette().flag(role), GUI::AllowCallback::No);
    };
    flag_combo_box.set_selected_index((size_t)Gfx::FlagRole::IsDark - 1);

    flag_input.on_checked = [&](bool checked) {
        auto role = flag_combo_box.model()->index(flag_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_flag_role();
        auto preview_palette = preview_widget.preview_palette();
        preview_palette.set_flag(role, checked);
        preview_widget.set_preview_palette(preview_palette);
    };
    flag_input.set_checked(startup_preview_palette.flag(Gfx::FlagRole::IsDark), GUI::AllowCallback::No);

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

    path_combo_box.set_model(adopt_ref(*new PathRoleModel(path_roles)));
    path_combo_box.on_change = [&](auto&, auto& index) {
        auto role = index.model()->data(index, GUI::ModelRole::Custom).to_path_role();
        path_input.set_text(preview_widget.preview_palette().path(role));
    };
    path_combo_box.set_selected_index((size_t)Gfx::PathRole::TitleButtonIcons - 1);

    path_input.on_change = [&] {
        auto role = path_combo_box.model()->index(path_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_path_role();
        auto preview_palette = preview_widget.preview_palette();
        preview_palette.set_path(role, path_input.text());
        preview_widget.set_preview_palette(preview_palette);
    };
    path_input.set_text(startup_preview_palette.path(Gfx::PathRole::TitleButtonIcons));

    path_picker_button.on_click = [&](auto) {
        // FIXME: Open at the path_input location. Right now that's panicking the kernel though! :^(
        auto role = path_combo_box.model()->index(path_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_path_role();
        bool open_folder = (role == Gfx::PathRole::TitleButtonIcons);
        auto window_title = String::formatted(open_folder ? "Select {} folder" : "Select {} file", path_combo_box.text());
        auto result = GUI::FilePicker::get_open_filepath(window, window_title, "/res/icons", open_folder);
        if (!result.has_value())
            return;
        path_input.set_text(*result);
    };

    auto file_menu = TRY(window->try_add_menu("&File"));

    auto update_window_title = [&] {
        window->set_title(String::formatted("{} - Theme Editor", path.value_or("Untitled")));
    };

    preview_widget.on_theme_load_from_file = [&](String const& new_path) {
        path = new_path;
        update_window_title();

        auto selected_color_role = color_combo_box.model()->index(color_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_color_role();
        color_input.set_color(preview_widget.preview_palette().color(selected_color_role));

        auto selected_flag_role = flag_combo_box.model()->index(flag_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_flag_role();
        flag_input.set_checked(preview_widget.preview_palette().flag(selected_flag_role), GUI::AllowCallback::No);

        auto selected_metric_role = metric_combo_box.model()->index(metric_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_metric_role();
        metric_input.set_value(preview_widget.preview_palette().metric(selected_metric_role), GUI::AllowCallback::No);

        auto selected_path_role = path_combo_box.model()->index(path_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_path_role();
        path_input.set_text(preview_widget.preview_palette().path(selected_path_role));
    };

    auto save_to_result = [&](auto const& response) {
        if (response.is_error())
            return;

        update_window_title();
        auto file = response.value();
        auto theme = Core::ConfigFile::open(file->filename(), file->leak_fd());
        for (auto role : color_roles) {
            theme->write_entry("Colors", to_string(role), preview_widget.preview_palette().color(role).to_string());
        }

        for (auto role : flag_roles) {
            theme->write_bool_entry("Flags", to_string(role), preview_widget.preview_palette().flag(role));
        }

        for (auto role : metric_roles) {
            theme->write_num_entry("Metrics", to_string(role), preview_widget.preview_palette().metric(role));
        }

        for (auto role : path_roles) {
            theme->write_entry("Paths", to_string(role), preview_widget.preview_palette().path(role));
        }

        theme->sync();
    };

    TRY(file_menu->try_add_action(GUI::CommonActions::make_open_action([&](auto&) {
        auto response = FileSystemAccessClient::Client::the().try_open_file(window, "Select theme file", "/res/themes");
        if (response.is_error())
            return;
        preview_widget.set_theme_from_file(*response.value());
    })));

    TRY(file_menu->try_add_action(GUI::CommonActions::make_save_action([&](auto&) {
        if (path.has_value()) {
            save_to_result(FileSystemAccessClient::Client::the().try_request_file(window, *path, Core::OpenMode::WriteOnly | Core::OpenMode::Truncate));
        } else {
            save_to_result(FileSystemAccessClient::Client::the().try_save_file(window, "Theme", "ini"));
        }
    })));

    TRY(file_menu->try_add_action(GUI::CommonActions::make_save_as_action([&](auto&) {
        save_to_result(FileSystemAccessClient::Client::the().try_save_file(window, "Theme", "ini"));
    })));

    TRY(file_menu->try_add_separator());
    TRY(file_menu->try_add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); })));

    auto accessibility_menu = TRY(window->try_add_menu("&Accessibility"));

    auto default_accessibility_action = GUI::Action::create_checkable("Default - non-impaired", { Mod_AltGr, Key_1 }, [&](auto&) {
        preview_widget.set_color_filter(nullptr);
    });
    default_accessibility_action->set_checked(true);

    auto pratanopia_accessibility_action = GUI::Action::create_checkable("Protanopia", { Mod_AltGr, Key_2 }, [&](auto&) {
        preview_widget.set_color_filter(Gfx::ColorBlindnessFilter::create_protanopia());
    });

    auto pratanomaly_accessibility_action = GUI::Action::create_checkable("Protanomaly", { Mod_AltGr, Key_3 }, [&](auto&) {
        preview_widget.set_color_filter(Gfx::ColorBlindnessFilter::create_protanomaly());
    });

    auto tritanopia_accessibility_action = GUI::Action::create_checkable("Tritanopia", { Mod_AltGr, Key_4 }, [&](auto&) {
        preview_widget.set_color_filter(Gfx::ColorBlindnessFilter::create_tritanopia());
    });

    auto tritanomaly_accessibility_action = GUI::Action::create_checkable("Tritanomaly", { Mod_AltGr, Key_5 }, [&](auto&) {
        preview_widget.set_color_filter(Gfx::ColorBlindnessFilter::create_tritanomaly());
    });

    auto deuteranopia_accessibility_action = GUI::Action::create_checkable("Deuteranopia", { Mod_AltGr, Key_6 }, [&](auto&) {
        preview_widget.set_color_filter(Gfx::ColorBlindnessFilter::create_deuteranopia());
    });

    auto deuteranomaly_accessibility_action = GUI::Action::create_checkable("Deuteranomaly", { Mod_AltGr, Key_7 }, [&](auto&) {
        preview_widget.set_color_filter(Gfx::ColorBlindnessFilter::create_deuteranomaly());
    });

    auto achromatopsia_accessibility_action = GUI::Action::create_checkable("Achromatopsia", { Mod_AltGr, Key_8 }, [&](auto&) {
        preview_widget.set_color_filter(Gfx::ColorBlindnessFilter::create_achromatopsia());
    });

    auto achromatomaly_accessibility_action = GUI::Action::create_checkable("Achromatomaly", { Mod_AltGr, Key_9 }, [&](auto&) {
        preview_widget.set_color_filter(Gfx::ColorBlindnessFilter::create_achromatomaly());
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
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Theme Editor", app_icon, window)));

    update_window_title();

    window->resize(480, 520);
    window->set_resizable(false);
    window->show();
    window->set_icon(app_icon.bitmap_for_size(16));
    return app->exec();
}
