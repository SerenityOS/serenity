/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Jakob-Niklas See <git@nwex.de>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
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
#include <LibGUI/MessageBox.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Window.h>
#include <LibGfx/Filters/ColorBlindnessFilter.h>
#include <LibMain/Main.h>
#include <unistd.h>

template<typename T>
class RoleModel final : public GUI::ItemListModel<T> {
public:
    static ErrorOr<NonnullRefPtr<RoleModel>> try_create(Vector<T> const& data)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) RoleModel<T>(data));
    }

    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role) const override
    {
        if (role == GUI::ModelRole::Display)
            return Gfx::to_string(this->m_data[index.row()]);
        if (role == GUI::ModelRole::Custom)
            return this->m_data[index.row()];

        return GUI::ItemListModel<T>::data(index, role);
    }

private:
    explicit RoleModel(Vector<T> const& data)
        : GUI::ItemListModel<T>(data)
    {
    }
};

class AlignmentModel final : public GUI::Model {
public:
    static ErrorOr<NonnullRefPtr<AlignmentModel>> try_create()
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) AlignmentModel());
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
    AlignmentModel() = default;

    struct AlignmentValue {
        String title;
        Gfx::TextAlignment setting_value;
    };
    Vector<AlignmentValue> m_alignments {
        { "Center", Gfx::TextAlignment::Center },
        { "Left", Gfx::TextAlignment::CenterLeft },
        { "Right", Gfx::TextAlignment::CenterRight },
    };
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd thread rpath cpath wpath unix"));

    auto app = TRY(GUI::Application::try_create(arguments));

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

    TRY(Core::System::pledge("stdio recvfd sendfd thread rpath unix"));
    TRY(Core::System::unveil("/tmp/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("app-theme-editor");

    Gfx::Palette startup_preview_palette = file_to_edit ? Gfx::Palette(Gfx::PaletteImpl::create_with_anonymous_buffer(Gfx::load_system_theme(*path))) : app->palette();

    auto window = GUI::Window::construct();
    auto last_modified_time = Time::now_monotonic();

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

    color_combo_box.set_model(TRY(RoleModel<Gfx::ColorRole>::try_create(color_roles)));
    color_combo_box.on_change = [&](auto&, auto& index) {
        auto role = index.model()->data(index, GUI::ModelRole::Custom).to_color_role();
        color_input.set_color(preview_widget.preview_palette().color(role), GUI::AllowCallback::No);
    };
    color_combo_box.set_selected_index((size_t)Gfx::ColorRole::Window - 1);

    color_input.on_change = [&] {
        auto role = color_combo_box.model()->index(color_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_color_role();
        auto preview_palette = preview_widget.preview_palette();
        preview_palette.set_color(role, color_input.color());
        preview_widget.set_preview_palette(preview_palette);
    };
    color_input.set_color(startup_preview_palette.color(Gfx::ColorRole::Window), GUI::AllowCallback::No);

    alignment_combo_box.set_model(TRY(RoleModel<Gfx::AlignmentRole>::try_create(alignment_roles)));
    alignment_combo_box.on_change = [&](auto&, auto& index) {
        auto role = index.model()->data(index, GUI::ModelRole::Custom).to_alignment_role();
        alignment_input.set_selected_index((size_t)preview_widget.preview_palette().alignment(role), GUI::AllowCallback::No);
    };
    alignment_combo_box.set_selected_index((size_t)Gfx::AlignmentRole::TitleAlignment - 1);

    alignment_input.set_only_allow_values_from_model(true);
    alignment_input.set_model(TRY(AlignmentModel::try_create()));
    alignment_input.set_selected_index((size_t)startup_preview_palette.alignment(Gfx::AlignmentRole::TitleAlignment), GUI::AllowCallback::No);
    alignment_input.on_change = [&](auto&, auto& index) {
        auto role = alignment_combo_box.model()->index(alignment_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_alignment_role();
        auto preview_palette = preview_widget.preview_palette();

        preview_palette.set_alignment(role, index.data(GUI::ModelRole::Custom).to_text_alignment(Gfx::TextAlignment::CenterLeft));
        preview_widget.set_preview_palette(preview_palette);
    };

    flag_combo_box.set_model(TRY(RoleModel<Gfx::FlagRole>::try_create(flag_roles)));
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

    metric_combo_box.set_model(TRY(RoleModel<Gfx::MetricRole>::try_create(metric_roles)));
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

    path_combo_box.set_model(TRY(RoleModel<Gfx::PathRole>::try_create(path_roles)));
    path_combo_box.on_change = [&](auto&, auto& index) {
        auto role = index.model()->data(index, GUI::ModelRole::Custom).to_path_role();
        path_input.set_text(preview_widget.preview_palette().path(role), GUI::AllowCallback::No);
    };
    path_combo_box.set_selected_index((size_t)Gfx::PathRole::TitleButtonIcons - 1);

    path_input.on_change = [&] {
        auto role = path_combo_box.model()->index(path_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_path_role();
        auto preview_palette = preview_widget.preview_palette();
        preview_palette.set_path(role, path_input.text());
        preview_widget.set_preview_palette(preview_palette);
    };
    path_input.set_text(startup_preview_palette.path(Gfx::PathRole::TitleButtonIcons), GUI::AllowCallback::No);

    path_picker_button.on_click = [&](auto) {
        auto role = path_combo_box.model()->index(path_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_path_role();
        bool open_folder = (role == Gfx::PathRole::TitleButtonIcons);
        auto window_title = String::formatted(open_folder ? "Select {} folder" : "Select {} file", path_combo_box.text());
        auto target_path = path_input.text();
        if (Core::File::exists(target_path)) {
            if (!Core::File::is_directory(target_path))
                target_path = LexicalPath::dirname(target_path);
        } else {
            target_path = "/res/icons";
        }
        auto result = GUI::FilePicker::get_open_filepath(window, window_title, target_path, open_folder);
        if (!result.has_value())
            return;
        path_input.set_text(*result);
    };

    auto file_menu = TRY(window->try_add_menu("&File"));

    auto update_window_title = [&] {
        window->set_title(String::formatted("{}[*] - Theme Editor", path.value_or("Untitled")));
    };

    preview_widget.on_palette_change = [&] {
        window->set_modified(true);
    };

    preview_widget.on_theme_load_from_file = [&](String const& new_path) {
        path = new_path;
        update_window_title();

        auto selected_color_role = color_combo_box.model()->index(color_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_color_role();
        color_input.set_color(preview_widget.preview_palette().color(selected_color_role), GUI::AllowCallback::No);

        auto selected_alignment_role = alignment_combo_box.model()->index(alignment_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_alignment_role();
        alignment_input.set_selected_index((size_t)(preview_widget.preview_palette().alignment(selected_alignment_role), GUI::AllowCallback::No));

        auto selected_flag_role = flag_combo_box.model()->index(flag_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_flag_role();
        flag_input.set_checked(preview_widget.preview_palette().flag(selected_flag_role), GUI::AllowCallback::No);

        auto selected_metric_role = metric_combo_box.model()->index(metric_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_metric_role();
        metric_input.set_value(preview_widget.preview_palette().metric(selected_metric_role), GUI::AllowCallback::No);

        auto selected_path_role = path_combo_box.model()->index(path_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_path_role();
        path_input.set_text(preview_widget.preview_palette().path(selected_path_role), GUI::AllowCallback::No);

        last_modified_time = Time::now_monotonic();
        window->set_modified(false);
    };

    auto save_to_result = [&](auto const& response) {
        if (response.is_error())
            return;

        update_window_title();
        auto file = response.value();
        auto theme = Core::ConfigFile::open(file->filename(), file->leak_fd()).release_value_but_fixme_should_propagate_errors();
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

        auto sync_result = theme->sync();
        if (sync_result.is_error()) {
            GUI::MessageBox::show_error(window, String::formatted("Failed to save theme file: {}", sync_result.error()));
        } else {
            last_modified_time = Time::now_monotonic();
            window->set_modified(false);
        }
    };

    TRY(file_menu->try_add_action(GUI::CommonActions::make_open_action([&](auto&) {
        auto response = FileSystemAccessClient::Client::the().try_open_file(window, "Select theme file", "/res/themes");
        if (response.is_error())
            return;
        preview_widget.set_theme_from_file(*response.value());
    })));

    auto save_action = GUI::CommonActions::make_save_action([&](auto&) {
        if (path.has_value()) {
            save_to_result(FileSystemAccessClient::Client::the().try_request_file(window, *path, Core::OpenMode::ReadWrite | Core::OpenMode::Truncate));
        } else {
            save_to_result(FileSystemAccessClient::Client::the().try_save_file(window, "Theme", "ini", Core::OpenMode::ReadWrite | Core::OpenMode::Truncate));
        }
    });
    TRY(file_menu->try_add_action(save_action));

    TRY(file_menu->try_add_action(GUI::CommonActions::make_save_as_action([&](auto&) {
        save_to_result(FileSystemAccessClient::Client::the().try_save_file(window, "Theme", "ini", Core::OpenMode::ReadWrite | Core::OpenMode::Truncate));
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

    window->on_close_request = [&]() -> GUI::Window::CloseRequestDecision {
        if (!window->is_modified())
            return GUI::Window::CloseRequestDecision::Close;

        auto result = GUI::MessageBox::ask_about_unsaved_changes(window, path.value_or(""), last_modified_time);
        if (result == GUI::MessageBox::ExecResult::Yes) {
            save_action->activate();
            if (window->is_modified())
                return GUI::Window::CloseRequestDecision::StayOpen;
            return GUI::Window::CloseRequestDecision::Close;
        }

        if (result == GUI::MessageBox::ExecResult::No)
            return GUI::Window::CloseRequestDecision::Close;

        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    window->resize(480, 520);
    window->set_resizable(false);
    window->show();
    window->set_icon(app_icon.bitmap_for_size(16));
    return app->exec();
}
