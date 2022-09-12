/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, networkException <networkexception@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Antonio Di Stefano <tonio9681@gmail.com>
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include <Applications/ThemeEditor/AlignmentPropertyGML.h>
#include <Applications/ThemeEditor/ColorPropertyGML.h>
#include <Applications/ThemeEditor/FlagPropertyGML.h>
#include <Applications/ThemeEditor/MetricPropertyGML.h>
#include <Applications/ThemeEditor/PathPropertyGML.h>
#include <Applications/ThemeEditor/ThemeEditorGML.h>
// FIXME: LibIPC Decoder and Encoder are sensitive to include order here
// clang-format off
#include <LibGUI/ConnectionToWindowServer.h>
// clang-format on
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Frame.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/Icon.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/ScrollableContainerWidget.h>
#include <LibGfx/Filters/ColorBlindnessFilter.h>

namespace ThemeEditor {

static const PropertyTab window_tab {
    "Windows",
    {
        { "General",
            { { Gfx::FlagRole::IsDark },
                { Gfx::AlignmentRole::TitleAlignment },
                { Gfx::MetricRole::TitleHeight },
                { Gfx::MetricRole::TitleButtonWidth },
                { Gfx::MetricRole::TitleButtonHeight },
                { Gfx::PathRole::TitleButtonIcons },
                { Gfx::FlagRole::TitleButtonsIconOnly } } },

        { "Border",
            { { Gfx::MetricRole::BorderThickness },
                { Gfx::MetricRole::BorderRadius } } },

        { "Active Window",
            { { Gfx::ColorRole::ActiveWindowBorder1 },
                { Gfx::ColorRole::ActiveWindowBorder2 },
                { Gfx::ColorRole::ActiveWindowTitle },
                { Gfx::ColorRole::ActiveWindowTitleShadow },
                { Gfx::ColorRole::ActiveWindowTitleStripes },
                { Gfx::PathRole::ActiveWindowShadow } } },

        { "Inactive Window",
            { { Gfx::ColorRole::InactiveWindowBorder1 },
                { Gfx::ColorRole::InactiveWindowBorder2 },
                { Gfx::ColorRole::InactiveWindowTitle },
                { Gfx::ColorRole::InactiveWindowTitleShadow },
                { Gfx::ColorRole::InactiveWindowTitleStripes },
                { Gfx::PathRole::InactiveWindowShadow } } },

        { "Highlighted Window",
            { { Gfx::ColorRole::HighlightWindowBorder1 },
                { Gfx::ColorRole::HighlightWindowBorder2 },
                { Gfx::ColorRole::HighlightWindowTitle },
                { Gfx::ColorRole::HighlightWindowTitleShadow },
                { Gfx::ColorRole::HighlightWindowTitleStripes } } },

        { "Moving Window",
            { { Gfx::ColorRole::MovingWindowBorder1 },
                { Gfx::ColorRole::MovingWindowBorder2 },
                { Gfx::ColorRole::MovingWindowTitle },
                { Gfx::ColorRole::MovingWindowTitleShadow },
                { Gfx::ColorRole::MovingWindowTitleStripes } } },

        { "Contents",
            { { Gfx::ColorRole::Window },
                { Gfx::ColorRole::WindowText } } },

        { "Desktop",
            { { Gfx::ColorRole::DesktopBackground },
                { Gfx::PathRole::TaskbarShadow } } },
    }
};

static const PropertyTab widgets_tab {
    "Widgets",
    {
        { "General",
            { { Gfx::ColorRole::Accent },
                { Gfx::ColorRole::Base },
                { Gfx::ColorRole::ThreedHighlight },
                { Gfx::ColorRole::ThreedShadow1 },
                { Gfx::ColorRole::ThreedShadow2 },
                { Gfx::ColorRole::HoverHighlight } } },

        { "Text",
            { { Gfx::ColorRole::BaseText },
                { Gfx::ColorRole::DisabledTextFront },
                { Gfx::ColorRole::DisabledTextBack },
                { Gfx::ColorRole::PlaceholderText } } },

        { "Links",
            { { Gfx::ColorRole::Link },
                { Gfx::ColorRole::ActiveLink },
                { Gfx::ColorRole::VisitedLink } } },

        { "Buttons",
            { { Gfx::ColorRole::Button },
                { Gfx::ColorRole::ButtonText } } },

        { "Tooltips",
            { { Gfx::ColorRole::Tooltip },
                { Gfx::ColorRole::TooltipText },
                { Gfx::PathRole::TooltipShadow } } },

        { "Trays",
            { { Gfx::ColorRole::Tray },
                { Gfx::ColorRole::TrayText } } },

        { "Ruler",
            { { Gfx::ColorRole::Ruler },
                { Gfx::ColorRole::RulerBorder },
                { Gfx::ColorRole::RulerActiveText },
                { Gfx::ColorRole::RulerInactiveText } } },

        { "Gutter",
            { { Gfx::ColorRole::Gutter },
                { Gfx::ColorRole::GutterBorder } } },

        { "Rubber Band",
            { { Gfx::ColorRole::RubberBandBorder },
                { Gfx::ColorRole::RubberBandFill } } },

        { "Menus",
            { { Gfx::ColorRole::MenuBase },
                { Gfx::ColorRole::MenuBaseText },
                { Gfx::ColorRole::MenuSelection },
                { Gfx::ColorRole::MenuSelectionText },
                { Gfx::ColorRole::MenuStripe },
                { Gfx::PathRole::MenuShadow } } },

        { "Selection",
            { { Gfx::ColorRole::FocusOutline },
                { Gfx::ColorRole::TextCursor },
                { Gfx::ColorRole::Selection },
                { Gfx::ColorRole::SelectionText },
                { Gfx::ColorRole::InactiveSelection },
                { Gfx::ColorRole::InactiveSelectionText },
                { Gfx::ColorRole::HighlightSearching },
                { Gfx::ColorRole::HighlightSearchingText } } },
    }
};

static const PropertyTab syntax_highlighting_tab {
    "Syntax Highlighting",
    {
        { "General",
            { { Gfx::ColorRole::SyntaxComment },
                { Gfx::ColorRole::SyntaxControlKeyword },
                { Gfx::ColorRole::SyntaxIdentifier },
                { Gfx::ColorRole::SyntaxKeyword },
                { Gfx::ColorRole::SyntaxNumber },
                { Gfx::ColorRole::SyntaxOperator },
                { Gfx::ColorRole::SyntaxPreprocessorStatement },
                { Gfx::ColorRole::SyntaxPreprocessorValue },
                { Gfx::ColorRole::SyntaxPunctuation },
                { Gfx::ColorRole::SyntaxString },
                { Gfx::ColorRole::SyntaxType },
                { Gfx::ColorRole::SyntaxFunction },
                { Gfx::ColorRole::SyntaxVariable },
                { Gfx::ColorRole::SyntaxCustomType },
                { Gfx::ColorRole::SyntaxNamespace },
                { Gfx::ColorRole::SyntaxMember },
                { Gfx::ColorRole::SyntaxParameter } } },
    }
};

MainWidget::MainWidget()
    : m_current_palette(GUI::Application::the()->palette())
{
    load_from_gml(theme_editor_gml);

    m_alignment_model = MUST(AlignmentModel::try_create());

    m_preview_widget = find_descendant_of_type_named<GUI::Frame>("preview_frame")
                           ->add<ThemeEditor::PreviewWidget>(m_current_palette);
    m_property_tabs = find_descendant_of_type_named<GUI::TabWidget>("property_tabs");
    add_property_tab(window_tab);
    add_property_tab(widgets_tab);
    add_property_tab(syntax_highlighting_tab);

    build_override_controls();
}

ErrorOr<void> MainWidget::initialize_menubar(GUI::Window& window)
{
    auto file_menu = TRY(window.try_add_menu("&File"));
    TRY(file_menu->try_add_action(GUI::CommonActions::make_open_action([&](auto&) {
        auto response = FileSystemAccessClient::Client::the().try_open_file(&window, "Select theme file", "/res/themes"sv);
        if (response.is_error())
            return;
        load_from_file(*response.value());
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
    TRY(file_menu->try_add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        if (request_close() == GUI::Window::CloseRequestDecision::Close)
            GUI::Application::the()->quit();
    })));

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
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Theme Editor", GUI::Icon::default_icon("app-theme-editor"sv), &window)));

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

#define __ENUMERATE_ALIGNMENT_ROLE(role) theme->write_entry("Alignments", to_string(Gfx::AlignmentRole::role), to_string(m_current_palette.alignment(Gfx::AlignmentRole::role)));
    ENUMERATE_ALIGNMENT_ROLES(__ENUMERATE_ALIGNMENT_ROLE)
#undef __ENUMERATE_ALIGNMENT_ROLE

#define __ENUMERATE_COLOR_ROLE(role) theme->write_entry("Colors", to_string(Gfx::ColorRole::role), m_current_palette.color(Gfx::ColorRole::role).to_string());
    ENUMERATE_COLOR_ROLES(__ENUMERATE_COLOR_ROLE)
#undef __ENUMERATE_COLOR_ROLE

#define __ENUMERATE_FLAG_ROLE(role) theme->write_bool_entry("Flags", to_string(Gfx::FlagRole::role), m_current_palette.flag(Gfx::FlagRole::role));
    ENUMERATE_FLAG_ROLES(__ENUMERATE_FLAG_ROLE)
#undef __ENUMERATE_FLAG_ROLE

#define __ENUMERATE_METRIC_ROLE(role) theme->write_num_entry("Metrics", to_string(Gfx::MetricRole::role), m_current_palette.metric(Gfx::MetricRole::role));
    ENUMERATE_METRIC_ROLES(__ENUMERATE_METRIC_ROLE)
#undef __ENUMERATE_METRIC_ROLE

#define __ENUMERATE_PATH_ROLE(role) theme->write_entry("Paths", to_string(Gfx::PathRole::role), m_current_palette.path(Gfx::PathRole::role));
    ENUMERATE_PATH_ROLES(__ENUMERATE_PATH_ROLE)
#undef __ENUMERATE_PATH_ROLE

    auto sync_result = theme->sync();
    if (sync_result.is_error()) {
        GUI::MessageBox::show_error(window(), String::formatted("Failed to save theme file: {}", sync_result.error()));
    } else {
        m_last_modified_time = Time::now_monotonic();
        set_path(file.filename());
        window()->set_modified(false);
    }
}

ErrorOr<Core::AnonymousBuffer> MainWidget::encode()
{
    auto buffer = TRY(Core::AnonymousBuffer::create_with_size(sizeof(Gfx::SystemTheme)));
    auto* data = buffer.data<Gfx::SystemTheme>();

#define __ENUMERATE_ALIGNMENT_ROLE(role) \
    data->alignment[(int)Gfx::AlignmentRole::role] = m_current_palette.alignment(Gfx::AlignmentRole::role);
    ENUMERATE_ALIGNMENT_ROLES(__ENUMERATE_ALIGNMENT_ROLE)
#undef __ENUMERATE_ALIGNMENT_ROLE

#define __ENUMERATE_COLOR_ROLE(role) \
    data->color[(int)Gfx::ColorRole::role] = m_current_palette.color(Gfx::ColorRole::role).value();
    ENUMERATE_COLOR_ROLES(__ENUMERATE_COLOR_ROLE)
#undef __ENUMERATE_COLOR_ROLE

#define __ENUMERATE_FLAG_ROLE(role) \
    data->flag[(int)Gfx::FlagRole::role] = m_current_palette.flag(Gfx::FlagRole::role);
    ENUMERATE_FLAG_ROLES(__ENUMERATE_FLAG_ROLE)
#undef __ENUMERATE_FLAG_ROLE

#define __ENUMERATE_METRIC_ROLE(role) \
    data->metric[(int)Gfx::MetricRole::role] = m_current_palette.metric(Gfx::MetricRole::role);
    ENUMERATE_METRIC_ROLES(__ENUMERATE_METRIC_ROLE)
#undef __ENUMERATE_METRIC_ROLE

#define ENCODE_PATH(role, allow_empty)                                                                                                       \
    do {                                                                                                                                     \
        auto path = m_current_palette.path(Gfx::PathRole::role);                                                                             \
        char const* characters;                                                                                                              \
        if (path.is_empty()) {                                                                                                               \
            switch (Gfx::PathRole::role) {                                                                                                   \
            case Gfx::PathRole::TitleButtonIcons:                                                                                            \
                characters = "/res/icons/16x16/";                                                                                            \
                break;                                                                                                                       \
            default:                                                                                                                         \
                characters = allow_empty ? "" : "/res/";                                                                                     \
            }                                                                                                                                \
        }                                                                                                                                    \
        characters = path.characters();                                                                                                      \
        memcpy(data->path[(int)Gfx::PathRole::role], characters, min(strlen(characters) + 1, sizeof(data->path[(int)Gfx::PathRole::role]))); \
        data->path[(int)Gfx::PathRole::role][sizeof(data->path[(int)Gfx::PathRole::role]) - 1] = '\0';                                       \
    } while (0)

    ENCODE_PATH(TitleButtonIcons, false);
    ENCODE_PATH(ActiveWindowShadow, true);
    ENCODE_PATH(InactiveWindowShadow, true);
    ENCODE_PATH(TaskbarShadow, true);
    ENCODE_PATH(MenuShadow, true);
    ENCODE_PATH(TooltipShadow, true);

    return buffer;
}

void MainWidget::build_override_controls()
{
    auto* theme_override_controls = find_descendant_of_type_named<GUI::Widget>("theme_override_controls");

    m_theme_override_apply = theme_override_controls->find_child_of_type_named<GUI::Button>("apply");
    m_theme_override_reset = theme_override_controls->find_child_of_type_named<GUI::Button>("reset");

    m_theme_override_apply->on_click = [&](auto) {
        auto encoded = encode();
        if (encoded.is_error())
            return;
        GUI::ConnectionToWindowServer::the().async_set_system_theme_override(encoded.value());
    };

    m_theme_override_reset->on_click = [&](auto) {
        GUI::ConnectionToWindowServer::the().async_clear_system_theme_override();
    };

    GUI::Application::the()->on_theme_change = [&]() {
        auto override_active = GUI::ConnectionToWindowServer::the().is_system_theme_overridden();
        m_theme_override_apply->set_enabled(!override_active && window()->is_modified());
        m_theme_override_reset->set_enabled(override_active);
    };
}

void MainWidget::add_property_tab(PropertyTab const& property_tab)
{
    auto& scrollable_container = m_property_tabs->add_tab<GUI::ScrollableContainerWidget>(property_tab.title);
    scrollable_container.set_should_hide_unnecessary_scrollbars(true);

    auto properties_list = GUI::Widget::construct();
    scrollable_container.set_widget(properties_list);
    properties_list->set_layout<GUI::VerticalBoxLayout>();
    properties_list->layout()->set_spacing(12);
    properties_list->layout()->set_margins({ 8 });

    for (auto const& group : property_tab.property_groups) {
        NonnullRefPtr<GUI::GroupBox> group_box = properties_list->add<GUI::GroupBox>(group.title);
        group_box->set_layout<GUI::VerticalBoxLayout>();
        group_box->layout()->set_spacing(12);
        // 1px less on the left makes the text line up with the group title.
        group_box->layout()->set_margins({ 8, 8, 8, 7 });
        group_box->set_preferred_height(GUI::SpecialDimension::Fit);

        for (auto const& property : group.properties) {
            NonnullRefPtr<GUI::Widget> row_widget = group_box->add<GUI::Widget>();
            row_widget->set_fixed_height(22);
            property.role.visit(
                [&](Gfx::AlignmentRole role) {
                    row_widget->load_from_gml(alignment_property_gml);

                    auto& name_label = *row_widget->find_descendant_of_type_named<GUI::Label>("name");
                    name_label.set_text(to_string(role));

                    auto& alignment_picker = *row_widget->find_descendant_of_type_named<GUI::ComboBox>("combo_box");
                    alignment_picker.set_model(*m_alignment_model);
                    alignment_picker.on_change = [&, role](auto&, auto& index) {
                        set_alignment(role, index.data(GUI::ModelRole::Custom).to_text_alignment(Gfx::TextAlignment::CenterLeft));
                    };
                    alignment_picker.set_selected_index(m_alignment_model->index_of(m_current_palette.alignment(role)), GUI::AllowCallback::No);

                    VERIFY(m_alignment_inputs[to_underlying(role)].is_null());
                    m_alignment_inputs[to_underlying(role)] = alignment_picker;
                },
                [&](Gfx::ColorRole role) {
                    row_widget->load_from_gml(color_property_gml);

                    auto& name_label = *row_widget->find_descendant_of_type_named<GUI::Label>("name");
                    name_label.set_text(to_string(role));

                    auto& color_input = *row_widget->find_descendant_of_type_named<GUI::ColorInput>("color_input");
                    color_input.on_change = [&, role] {
                        set_color(role, color_input.color());
                    };
                    color_input.set_color(m_current_palette.color(role), GUI::AllowCallback::No);

                    VERIFY(m_color_inputs[to_underlying(role)].is_null());
                    m_color_inputs[to_underlying(role)] = color_input;
                },
                [&](Gfx::FlagRole role) {
                    row_widget->load_from_gml(flag_property_gml);

                    auto& checkbox = *row_widget->find_descendant_of_type_named<GUI::CheckBox>("checkbox");
                    checkbox.set_text(to_string(role));
                    checkbox.on_checked = [&, role](bool checked) {
                        set_flag(role, checked);
                    };
                    checkbox.set_checked(m_current_palette.flag(role), GUI::AllowCallback::No);

                    VERIFY(m_flag_inputs[to_underlying(role)].is_null());
                    m_flag_inputs[to_underlying(role)] = checkbox;
                },
                [&](Gfx::MetricRole role) {
                    row_widget->load_from_gml(metric_property_gml);

                    auto& name_label = *row_widget->find_descendant_of_type_named<GUI::Label>("name");
                    name_label.set_text(to_string(role));

                    auto& spin_box = *row_widget->find_descendant_of_type_named<GUI::SpinBox>("spin_box");
                    spin_box.on_change = [&, role](int value) {
                        set_metric(role, value);
                    };
                    spin_box.set_value(m_current_palette.metric(role), GUI::AllowCallback::No);

                    VERIFY(m_metric_inputs[to_underlying(role)].is_null());
                    m_metric_inputs[to_underlying(role)] = spin_box;
                },
                [&](Gfx::PathRole role) {
                    row_widget->load_from_gml(path_property_gml);

                    auto& name_label = *row_widget->find_descendant_of_type_named<GUI::Label>("name");
                    name_label.set_text(to_string(role));

                    auto& path_input = *row_widget->find_descendant_of_type_named<GUI::TextBox>("path_input");
                    path_input.on_change = [&, role] {
                        set_path(role, path_input.text());
                    };
                    path_input.set_text(m_current_palette.path(role), GUI::AllowCallback::No);

                    auto& path_picker_button = *row_widget->find_descendant_of_type_named<GUI::Button>("path_picker_button");
                    auto picker_target = (role == Gfx::PathRole::TitleButtonIcons) ? PathPickerTarget::Folder : PathPickerTarget::File;
                    path_picker_button.on_click = [&, role, picker_target](auto) {
                        show_path_picker_dialog(to_string(role), path_input, picker_target);
                    };

                    VERIFY(m_path_inputs[to_underlying(role)].is_null());
                    m_path_inputs[to_underlying(role)] = path_input;
                });
        }
    }
}

void MainWidget::set_alignment(Gfx::AlignmentRole role, Gfx::TextAlignment value)
{
    auto preview_palette = m_current_palette;
    preview_palette.set_alignment(role, value);
    set_palette(preview_palette);
}

void MainWidget::set_color(Gfx::ColorRole role, Gfx::Color value)
{
    auto preview_palette = m_current_palette;
    preview_palette.set_color(role, value);
    set_palette(preview_palette);
}

void MainWidget::set_flag(Gfx::FlagRole role, bool value)
{
    auto preview_palette = m_current_palette;
    preview_palette.set_flag(role, value);
    set_palette(preview_palette);
}

void MainWidget::set_metric(Gfx::MetricRole role, int value)
{
    auto preview_palette = m_current_palette;
    preview_palette.set_metric(role, value);
    set_palette(preview_palette);
}

void MainWidget::set_path(Gfx::PathRole role, String value)
{
    auto preview_palette = m_current_palette;
    preview_palette.set_path(role, value);
    set_palette(preview_palette);
}

void MainWidget::set_palette(Gfx::Palette palette)
{
    m_current_palette = move(palette);
    m_preview_widget->set_preview_palette(m_current_palette);
    m_theme_override_apply->set_enabled(true);
    window()->set_modified(true);
}

void MainWidget::show_path_picker_dialog(StringView property_display_name, GUI::TextBox& path_input, PathPickerTarget path_picker_target)
{
    bool open_folder = path_picker_target == PathPickerTarget::Folder;
    auto window_title = String::formatted(open_folder ? "Select {} folder"sv : "Select {} file"sv, property_display_name);
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
}

void MainWidget::load_from_file(Core::File& file)
{
    auto config_file = Core::ConfigFile::open(file.filename(), file.leak_fd()).release_value_but_fixme_should_propagate_errors();
    auto theme = Gfx::load_system_theme(config_file);
    VERIFY(theme.is_valid());

    auto new_palette = Gfx::Palette(Gfx::PaletteImpl::create_with_anonymous_buffer(theme));
    set_palette(move(new_palette));
    set_path(file.filename());

#define __ENUMERATE_ALIGNMENT_ROLE(role)                                                    \
    if (auto alignment_input = m_alignment_inputs[to_underlying(Gfx::AlignmentRole::role)]) \
        alignment_input->set_selected_index(m_alignment_model->index_of(m_current_palette.alignment(Gfx::AlignmentRole::role)), GUI::AllowCallback::No);
    ENUMERATE_ALIGNMENT_ROLES(__ENUMERATE_ALIGNMENT_ROLE)
#undef __ENUMERATE_ALIGNMENT_ROLE

#define __ENUMERATE_COLOR_ROLE(role)                                            \
    if (auto color_input = m_color_inputs[to_underlying(Gfx::ColorRole::role)]) \
        color_input->set_color(m_current_palette.color(Gfx::ColorRole::role), GUI::AllowCallback::No);
    ENUMERATE_COLOR_ROLES(__ENUMERATE_COLOR_ROLE)
#undef __ENUMERATE_COLOR_ROLE

#define __ENUMERATE_FLAG_ROLE(role)                                          \
    if (auto flag_input = m_flag_inputs[to_underlying(Gfx::FlagRole::role)]) \
        flag_input->set_checked(m_current_palette.flag(Gfx::FlagRole::role), GUI::AllowCallback::No);
    ENUMERATE_FLAG_ROLES(__ENUMERATE_FLAG_ROLE)
#undef __ENUMERATE_FLAG_ROLE

#define __ENUMERATE_METRIC_ROLE(role)                                              \
    if (auto metric_input = m_metric_inputs[to_underlying(Gfx::MetricRole::role)]) \
        metric_input->set_value(m_current_palette.metric(Gfx::MetricRole::role), GUI::AllowCallback::No);
    ENUMERATE_METRIC_ROLES(__ENUMERATE_METRIC_ROLE)
#undef __ENUMERATE_METRIC_ROLE

#define __ENUMERATE_PATH_ROLE(role)                                          \
    if (auto path_input = m_path_inputs[to_underlying(Gfx::PathRole::role)]) \
        path_input->set_text(m_current_palette.path(Gfx::PathRole::role), GUI::AllowCallback::No);
    ENUMERATE_PATH_ROLES(__ENUMERATE_PATH_ROLE)
#undef __ENUMERATE_PATH_ROLE

    m_last_modified_time = Time::now_monotonic();
    window()->set_modified(false);
}

}
