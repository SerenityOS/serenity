/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Jakob-Niklas See <git@nwex.de>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Antonio Di Stefano <tonio9681@gmail.com>
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include <Applications/ThemeEditor/ThemeEditorGML.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ColorInput.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Icon.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Filters/ColorBlindnessFilter.h>

namespace ThemeEditor {

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

MainWidget::MainWidget(Optional<String> path, Gfx::Palette startup_preview_palette)
    : m_path(path)
{
    load_from_gml(theme_editor_gml);

#define __ENUMERATE_COLOR_ROLE(role) m_color_roles.append(Gfx::ColorRole::role);
    ENUMERATE_COLOR_ROLES(__ENUMERATE_COLOR_ROLE)
#undef __ENUMERATE_COLOR_ROLE

#define __ENUMERATE_ALIGNMENT_ROLE(role) m_alignment_roles.append(Gfx::AlignmentRole::role);
    ENUMERATE_ALIGNMENT_ROLES(__ENUMERATE_ALIGNMENT_ROLE)
#undef __ENUMERATE_ALIGNMENT_ROLE

#define __ENUMERATE_FLAG_ROLE(role) m_flag_roles.append(Gfx::FlagRole::role);
    ENUMERATE_FLAG_ROLES(__ENUMERATE_FLAG_ROLE)
#undef __ENUMERATE_FLAG_ROLE

#define __ENUMERATE_METRIC_ROLE(role) m_metric_roles.append(Gfx::MetricRole::role);
    ENUMERATE_METRIC_ROLES(__ENUMERATE_METRIC_ROLE)
#undef __ENUMERATE_METRIC_ROLE

#define __ENUMERATE_PATH_ROLE(role) m_path_roles.append(Gfx::PathRole::role);
    ENUMERATE_PATH_ROLES(__ENUMERATE_PATH_ROLE)
#undef __ENUMERATE_PATH_ROLE

    m_preview_widget = find_descendant_of_type_named<GUI::Frame>("preview_frame")
                           ->add<ThemeEditor::PreviewWidget>(startup_preview_palette);
    auto& color_combo_box = *find_descendant_of_type_named<GUI::ComboBox>("color_combo_box");
    auto& color_input = *find_descendant_of_type_named<GUI::ColorInput>("color_input");

    auto& alignment_combo_box = *find_descendant_of_type_named<GUI::ComboBox>("alignment_combo_box");
    auto& alignment_input = *find_descendant_of_type_named<GUI::ComboBox>("alignment_input");

    auto& flag_combo_box = *find_descendant_of_type_named<GUI::ComboBox>("flag_combo_box");
    auto& flag_input = *find_descendant_of_type_named<GUI::CheckBox>("flag_input");

    auto& metric_combo_box = *find_descendant_of_type_named<GUI::ComboBox>("metric_combo_box");
    auto& metric_input = *find_descendant_of_type_named<GUI::SpinBox>("metric_input");

    auto& path_combo_box = *find_descendant_of_type_named<GUI::ComboBox>("path_combo_box");
    auto& path_input = *find_descendant_of_type_named<GUI::TextBox>("path_input");
    auto& path_picker_button = *find_descendant_of_type_named<GUI::Button>("path_picker_button");

    color_combo_box.set_model(MUST(RoleModel<Gfx::ColorRole>::try_create(m_color_roles)));
    color_combo_box.on_change = [&](auto&, auto& index) {
        auto role = index.model()->data(index, GUI::ModelRole::Custom).to_color_role();
        color_input.set_color(m_preview_widget->preview_palette().color(role), GUI::AllowCallback::No);
    };
    color_combo_box.set_selected_index((size_t)Gfx::ColorRole::Window - 1);

    color_input.on_change = [&] {
        auto role = color_combo_box.model()->index(color_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_color_role();
        auto preview_palette = m_preview_widget->preview_palette();
        preview_palette.set_color(role, color_input.color());
        m_preview_widget->set_preview_palette(preview_palette);
    };
    color_input.set_color(startup_preview_palette.color(Gfx::ColorRole::Window), GUI::AllowCallback::No);

    alignment_combo_box.set_model(MUST(RoleModel<Gfx::AlignmentRole>::try_create(m_alignment_roles)));
    alignment_combo_box.on_change = [&](auto&, auto& index) {
        auto role = index.model()->data(index, GUI::ModelRole::Custom).to_alignment_role();
        alignment_input.set_selected_index((size_t)m_preview_widget->preview_palette().alignment(role), GUI::AllowCallback::No);
    };
    alignment_combo_box.set_selected_index((size_t)Gfx::AlignmentRole::TitleAlignment - 1);

    alignment_input.set_only_allow_values_from_model(true);
    alignment_input.set_model(MUST(AlignmentModel::try_create()));
    alignment_input.set_selected_index((size_t)startup_preview_palette.alignment(Gfx::AlignmentRole::TitleAlignment), GUI::AllowCallback::No);
    alignment_input.on_change = [&](auto&, auto& index) {
        auto role = alignment_combo_box.model()->index(alignment_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_alignment_role();
        auto preview_palette = m_preview_widget->preview_palette();

        preview_palette.set_alignment(role, index.data(GUI::ModelRole::Custom).to_text_alignment(Gfx::TextAlignment::CenterLeft));
        m_preview_widget->set_preview_palette(preview_palette);
    };

    flag_combo_box.set_model(MUST(RoleModel<Gfx::FlagRole>::try_create(m_flag_roles)));
    flag_combo_box.on_change = [&](auto&, auto& index) {
        auto role = index.model()->data(index, GUI::ModelRole::Custom).to_flag_role();
        flag_input.set_checked(m_preview_widget->preview_palette().flag(role), GUI::AllowCallback::No);
    };
    flag_combo_box.set_selected_index((size_t)Gfx::FlagRole::IsDark - 1);

    flag_input.on_checked = [&](bool checked) {
        auto role = flag_combo_box.model()->index(flag_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_flag_role();
        auto preview_palette = m_preview_widget->preview_palette();
        preview_palette.set_flag(role, checked);
        m_preview_widget->set_preview_palette(preview_palette);
    };
    flag_input.set_checked(startup_preview_palette.flag(Gfx::FlagRole::IsDark), GUI::AllowCallback::No);

    metric_combo_box.set_model(MUST(RoleModel<Gfx::MetricRole>::try_create(m_metric_roles)));
    metric_combo_box.on_change = [&](auto&, auto& index) {
        auto role = index.model()->data(index, GUI::ModelRole::Custom).to_metric_role();
        metric_input.set_value(m_preview_widget->preview_palette().metric(role), GUI::AllowCallback::No);
    };
    metric_combo_box.set_selected_index((size_t)Gfx::MetricRole::TitleButtonHeight - 1);

    metric_input.on_change = [&](int value) {
        auto role = metric_combo_box.model()->index(metric_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_metric_role();
        auto preview_palette = m_preview_widget->preview_palette();
        preview_palette.set_metric(role, value);
        m_preview_widget->set_preview_palette(preview_palette);
    };
    metric_input.set_value(startup_preview_palette.metric(Gfx::MetricRole::TitleButtonHeight), GUI::AllowCallback::No);

    path_combo_box.set_model(MUST(RoleModel<Gfx::PathRole>::try_create(m_path_roles)));
    path_combo_box.on_change = [&](auto&, auto& index) {
        auto role = index.model()->data(index, GUI::ModelRole::Custom).to_path_role();
        path_input.set_text(m_preview_widget->preview_palette().path(role), GUI::AllowCallback::No);
    };
    path_combo_box.set_selected_index((size_t)Gfx::PathRole::TitleButtonIcons - 1);

    path_input.on_change = [&] {
        auto role = path_combo_box.model()->index(path_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_path_role();
        auto preview_palette = m_preview_widget->preview_palette();
        preview_palette.set_path(role, path_input.text());
        m_preview_widget->set_preview_palette(preview_palette);
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
        auto result = GUI::FilePicker::get_open_filepath(window(), window_title, target_path, open_folder);
        if (!result.has_value())
            return;
        path_input.set_text(*result);
    };

    m_preview_widget->on_palette_change = [&] {
        window()->set_modified(true);
    };

    m_preview_widget->on_theme_load_from_file = [&](String const& new_path) {
        set_path(new_path);

        auto selected_color_role = color_combo_box.model()->index(color_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_color_role();
        color_input.set_color(m_preview_widget->preview_palette().color(selected_color_role), GUI::AllowCallback::No);

        auto selected_alignment_role = alignment_combo_box.model()->index(alignment_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_alignment_role();
        alignment_input.set_selected_index((size_t)(m_preview_widget->preview_palette().alignment(selected_alignment_role), GUI::AllowCallback::No));

        auto selected_flag_role = flag_combo_box.model()->index(flag_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_flag_role();
        flag_input.set_checked(m_preview_widget->preview_palette().flag(selected_flag_role), GUI::AllowCallback::No);

        auto selected_metric_role = metric_combo_box.model()->index(metric_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_metric_role();
        metric_input.set_value(m_preview_widget->preview_palette().metric(selected_metric_role), GUI::AllowCallback::No);

        auto selected_path_role = path_combo_box.model()->index(path_combo_box.selected_index()).data(GUI::ModelRole::Custom).to_path_role();
        path_input.set_text(m_preview_widget->preview_palette().path(selected_path_role), GUI::AllowCallback::No);

        m_last_modified_time = Time::now_monotonic();
        window()->set_modified(false);
    };
}

ErrorOr<void> MainWidget::initialize_menubar(GUI::Window& window)
{
    auto file_menu = TRY(window.try_add_menu("&File"));
    TRY(file_menu->try_add_action(GUI::CommonActions::make_open_action([&](auto&) {
        auto response = FileSystemAccessClient::Client::the().try_open_file(&window, "Select theme file", "/res/themes");
        if (response.is_error())
            return;
        m_preview_widget->set_theme_from_file(*response.value());
    })));

    m_save_action = GUI::CommonActions::make_save_action([&](auto&) {
        if (m_path.has_value()) {
            auto result = FileSystemAccessClient::Client::the().try_request_file(&window, *m_path, Core::OpenMode::ReadWrite | Core::OpenMode::Truncate);
            if (result.is_error())
                return;
            save_to_file(result.value());
        } else {
            auto result = FileSystemAccessClient::Client::the().try_save_file(&window, "Theme", "ini", Core::OpenMode::ReadWrite | Core::OpenMode::Truncate);
            if (result.is_error())
                return;
            save_to_file(result.value());
        }
    });
    TRY(file_menu->try_add_action(*m_save_action));

    TRY(file_menu->try_add_action(GUI::CommonActions::make_save_as_action([&](auto&) {
        auto result = FileSystemAccessClient::Client::the().try_save_file(&window, "Theme", "ini", Core::OpenMode::ReadWrite | Core::OpenMode::Truncate);
        if (result.is_error())
            return;
        save_to_file(result.value());
    })));

    TRY(file_menu->try_add_separator());
    TRY(file_menu->try_add_action(GUI::CommonActions::make_quit_action([&](auto&) { GUI::Application::the()->quit(); })));

    auto accessibility_menu = TRY(window.try_add_menu("&Accessibility"));

    auto default_accessibility_action = GUI::Action::create_checkable("Default - non-impaired", { Mod_AltGr, Key_1 }, [&](auto&) {
        m_preview_widget->set_color_filter(nullptr);
    });
    default_accessibility_action->set_checked(true);

    auto pratanopia_accessibility_action = GUI::Action::create_checkable("Protanopia", { Mod_AltGr, Key_2 }, [&](auto&) {
        m_preview_widget->set_color_filter(Gfx::ColorBlindnessFilter::create_protanopia());
    });

    auto pratanomaly_accessibility_action = GUI::Action::create_checkable("Protanomaly", { Mod_AltGr, Key_3 }, [&](auto&) {
        m_preview_widget->set_color_filter(Gfx::ColorBlindnessFilter::create_protanomaly());
    });

    auto tritanopia_accessibility_action = GUI::Action::create_checkable("Tritanopia", { Mod_AltGr, Key_4 }, [&](auto&) {
        m_preview_widget->set_color_filter(Gfx::ColorBlindnessFilter::create_tritanopia());
    });

    auto tritanomaly_accessibility_action = GUI::Action::create_checkable("Tritanomaly", { Mod_AltGr, Key_5 }, [&](auto&) {
        m_preview_widget->set_color_filter(Gfx::ColorBlindnessFilter::create_tritanomaly());
    });

    auto deuteranopia_accessibility_action = GUI::Action::create_checkable("Deuteranopia", { Mod_AltGr, Key_6 }, [&](auto&) {
        m_preview_widget->set_color_filter(Gfx::ColorBlindnessFilter::create_deuteranopia());
    });

    auto deuteranomaly_accessibility_action = GUI::Action::create_checkable("Deuteranomaly", { Mod_AltGr, Key_7 }, [&](auto&) {
        m_preview_widget->set_color_filter(Gfx::ColorBlindnessFilter::create_deuteranomaly());
    });

    auto achromatopsia_accessibility_action = GUI::Action::create_checkable("Achromatopsia", { Mod_AltGr, Key_8 }, [&](auto&) {
        m_preview_widget->set_color_filter(Gfx::ColorBlindnessFilter::create_achromatopsia());
    });

    auto achromatomaly_accessibility_action = GUI::Action::create_checkable("Achromatomaly", { Mod_AltGr, Key_9 }, [&](auto&) {
        m_preview_widget->set_color_filter(Gfx::ColorBlindnessFilter::create_achromatomaly());
    });

    m_preview_type_action_group = make<GUI::ActionGroup>();
    m_preview_type_action_group->set_exclusive(true);
    m_preview_type_action_group->add_action(*default_accessibility_action);
    m_preview_type_action_group->add_action(*pratanopia_accessibility_action);
    m_preview_type_action_group->add_action(*pratanomaly_accessibility_action);
    m_preview_type_action_group->add_action(*tritanopia_accessibility_action);
    m_preview_type_action_group->add_action(*tritanomaly_accessibility_action);
    m_preview_type_action_group->add_action(*deuteranopia_accessibility_action);
    m_preview_type_action_group->add_action(*deuteranomaly_accessibility_action);
    m_preview_type_action_group->add_action(*achromatopsia_accessibility_action);
    m_preview_type_action_group->add_action(*achromatomaly_accessibility_action);

    TRY(accessibility_menu->try_add_action(default_accessibility_action));
    TRY(accessibility_menu->try_add_action(pratanopia_accessibility_action));
    TRY(accessibility_menu->try_add_action(pratanomaly_accessibility_action));
    TRY(accessibility_menu->try_add_action(tritanopia_accessibility_action));
    TRY(accessibility_menu->try_add_action(tritanomaly_accessibility_action));
    TRY(accessibility_menu->try_add_action(deuteranopia_accessibility_action));
    TRY(accessibility_menu->try_add_action(deuteranomaly_accessibility_action));
    TRY(accessibility_menu->try_add_action(achromatopsia_accessibility_action));
    TRY(accessibility_menu->try_add_action(achromatomaly_accessibility_action));

    auto help_menu = TRY(window.try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Theme Editor", GUI::Icon::default_icon("app-theme-editor"), &window)));

    return {};
}

void MainWidget::update_title()
{
    window()->set_title(String::formatted("{}[*] - Theme Editor", m_path.value_or("Untitled")));
}

GUI::Window::CloseRequestDecision MainWidget::request_close()
{
    if (!window()->is_modified())
        return GUI::Window::CloseRequestDecision::Close;

    auto result = GUI::MessageBox::ask_about_unsaved_changes(window(), m_path.value_or(""), m_last_modified_time);
    if (result == GUI::MessageBox::ExecResult::Yes) {
        m_save_action->activate();
        if (window()->is_modified())
            return GUI::Window::CloseRequestDecision::StayOpen;
        return GUI::Window::CloseRequestDecision::Close;
    }

    if (result == GUI::MessageBox::ExecResult::No)
        return GUI::Window::CloseRequestDecision::Close;

    return GUI::Window::CloseRequestDecision::StayOpen;
}

void MainWidget::set_path(String path)
{
    m_path = path;
    update_title();
}

void MainWidget::save_to_file(Core::File& file)
{
    auto theme = Core::ConfigFile::open(file.filename(), file.leak_fd()).release_value_but_fixme_should_propagate_errors();
    for (auto role : m_color_roles) {
        theme->write_entry("Colors", to_string(role), m_preview_widget->preview_palette().color(role).to_string());
    }

    for (auto role : m_flag_roles) {
        theme->write_bool_entry("Flags", to_string(role), m_preview_widget->preview_palette().flag(role));
    }

    for (auto role : m_metric_roles) {
        theme->write_num_entry("Metrics", to_string(role), m_preview_widget->preview_palette().metric(role));
    }

    for (auto role : m_path_roles) {
        theme->write_entry("Paths", to_string(role), m_preview_widget->preview_palette().path(role));
    }

    auto sync_result = theme->sync();
    if (sync_result.is_error()) {
        GUI::MessageBox::show_error(window(), String::formatted("Failed to save theme file: {}", sync_result.error()));
    } else {
        m_last_modified_time = Time::now_monotonic();
        set_path(file.filename());
        window()->set_modified(false);
    }
}

}
