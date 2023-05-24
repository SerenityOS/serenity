/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "PreviewWidget.h"
#include <AK/FixedArray.h>
#include <AK/Time.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ColorInput.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Window.h>
#include <LibGfx/SystemTheme.h>

namespace ThemeEditor {

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

    size_t index_of(Gfx::TextAlignment alignment) const
    {
        auto match = m_alignments.find_if([&](auto& it) { return it.setting_value == alignment; });
        return match.index();
    }

private:
    AlignmentModel() = default;

    struct AlignmentValue {
        DeprecatedString title;
        Gfx::TextAlignment setting_value;
    };
    Vector<AlignmentValue> m_alignments {
        { "Center", Gfx::TextAlignment::Center },
        { "Left", Gfx::TextAlignment::CenterLeft },
        { "Right", Gfx::TextAlignment::CenterRight },
    };
};

struct Property {
    Variant<Gfx::AlignmentRole, Gfx::ColorRole, Gfx::FlagRole, Gfx::MetricRole, Gfx::PathRole> role;
};

struct PropertyGroup {
    DeprecatedString title;
    Vector<Property> properties;
};

struct PropertyTab {
    StringView title;
    Vector<PropertyGroup> property_groups;
};

class MainWidget final : public GUI::Widget {
    C_OBJECT_ABSTRACT(MainWidget);

public:
    static ErrorOr<NonnullRefPtr<MainWidget>> try_create();
    virtual ~MainWidget() override = default;

    ErrorOr<void> initialize_menubar(GUI::Window&);
    GUI::Window::CloseRequestDecision request_close();
    void update_title();
    ErrorOr<void> load_from_file(String const& filename, NonnullOwnPtr<Core::File> file);

private:
    explicit MainWidget(NonnullRefPtr<AlignmentModel>);

    virtual void drag_enter_event(GUI::DragEvent&) override;
    virtual void drop_event(GUI::DropEvent&) override;

    void save_to_file(String const& filename, NonnullOwnPtr<Core::File> file);
    ErrorOr<Core::AnonymousBuffer> encode();
    void set_path(DeprecatedString);

    void build_override_controls();

    ErrorOr<void> add_property_tab(PropertyTab const&);
    void set_alignment(Gfx::AlignmentRole, Gfx::TextAlignment);
    void set_color(Gfx::ColorRole, Gfx::Color);
    void set_flag(Gfx::FlagRole, bool);
    void set_metric(Gfx::MetricRole, int);
    void set_path(Gfx::PathRole, DeprecatedString);

    void set_palette(Gfx::Palette);

    enum class PathPickerTarget {
        File,
        Folder,
    };
    void show_path_picker_dialog(StringView property_display_name, GUI::TextBox&, PathPickerTarget);

    RefPtr<PreviewWidget> m_preview_widget;
    RefPtr<GUI::TabWidget> m_property_tabs;
    RefPtr<GUI::Action> m_save_action;

    RefPtr<GUI::Button> m_theme_override_apply;
    RefPtr<GUI::Button> m_theme_override_reset;

    Optional<DeprecatedString> m_path;
    Gfx::Palette m_current_palette;
    Duration m_last_modified_time { Duration::now_monotonic() };

    RefPtr<AlignmentModel> m_alignment_model;

    Array<RefPtr<GUI::ComboBox>, to_underlying(Gfx::AlignmentRole::__Count)> m_alignment_inputs;
    Array<RefPtr<GUI::ColorInput>, to_underlying(Gfx::ColorRole::__Count)> m_color_inputs;
    Array<RefPtr<GUI::CheckBox>, to_underlying(Gfx::FlagRole::__Count)> m_flag_inputs;
    Array<RefPtr<GUI::SpinBox>, to_underlying(Gfx::MetricRole::__Count)> m_metric_inputs;
    Array<RefPtr<GUI::TextBox>, to_underlying(Gfx::PathRole::__Count)> m_path_inputs;

    OwnPtr<GUI::ActionGroup> m_preview_type_action_group;
};

}
