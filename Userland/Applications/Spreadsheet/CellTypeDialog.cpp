/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CellTypeDialog.h"
#include "Cell.h"
#include "Spreadsheet.h"
#include <AK/StringBuilder.h>
#include <Applications/Spreadsheet/CondFormattingGML.h>
#include <Applications/Spreadsheet/CondFormattingViewGML.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ColorInput.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/ListView.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibJS/SyntaxHighlighter.h>

REGISTER_WIDGET(Spreadsheet, ConditionsView);

namespace Spreadsheet {

CellTypeDialog::CellTypeDialog(Vector<Position> const& positions, Sheet& sheet, GUI::Window* parent)
    : GUI::Dialog(parent)
{
    VERIFY(!positions.is_empty());

    StringBuilder builder;

    if (positions.size() == 1)
        builder.appendff("Format cell {}", positions.first().to_cell_identifier(sheet));
    else
        builder.appendff("Format {} cells", positions.size());

    set_title(builder.string_view());
    set_icon(parent->icon());
    resize(285, 360);

    auto main_widget = set_main_widget<GUI::Widget>();
    main_widget->set_layout<GUI::VerticalBoxLayout>(4);
    main_widget->set_fill_with_background_color(true);

    auto& tab_widget = main_widget->add<GUI::TabWidget>();
    setup_tabs(tab_widget, positions, sheet);

    auto& buttonbox = main_widget->add<GUI::Widget>();
    buttonbox.set_shrink_to_fit(true);
    buttonbox.set_layout<GUI::HorizontalBoxLayout>(GUI::Margins {}, 10);
    buttonbox.add_spacer();
    auto& ok_button = buttonbox.add<GUI::Button>("OK"_string);
    ok_button.set_fixed_width(80);
    ok_button.on_click = [&](auto) { done(ExecResult::OK); };
}

Vector<ByteString> const g_horizontal_alignments { "Left", "Center", "Right" };
Vector<ByteString> const g_vertical_alignments { "Top", "Center", "Bottom" };
Vector<ByteString> g_types;

constexpr static CellTypeDialog::VerticalAlignment vertical_alignment_from(Gfx::TextAlignment alignment)
{
    switch (alignment) {
    case Gfx::TextAlignment::CenterRight:
    case Gfx::TextAlignment::CenterLeft:
    case Gfx::TextAlignment::Center:
        return CellTypeDialog::VerticalAlignment::Center;

    case Gfx::TextAlignment::TopCenter:
    case Gfx::TextAlignment::TopRight:
    case Gfx::TextAlignment::TopLeft:
        return CellTypeDialog::VerticalAlignment::Top;

    case Gfx::TextAlignment::BottomCenter:
    case Gfx::TextAlignment::BottomLeft:
    case Gfx::TextAlignment::BottomRight:
        return CellTypeDialog::VerticalAlignment::Bottom;
    }

    return CellTypeDialog::VerticalAlignment::Center;
}

constexpr static CellTypeDialog::HorizontalAlignment horizontal_alignment_from(Gfx::TextAlignment alignment)
{
    switch (alignment) {
    case Gfx::TextAlignment::BottomCenter:
    case Gfx::TextAlignment::Center:
    case Gfx::TextAlignment::TopCenter:
        return CellTypeDialog::HorizontalAlignment::Center;

    case Gfx::TextAlignment::TopRight:
    case Gfx::TextAlignment::CenterRight:
    case Gfx::TextAlignment::BottomRight:
        return CellTypeDialog::HorizontalAlignment::Right;

    case Gfx::TextAlignment::TopLeft:
    case Gfx::TextAlignment::CenterLeft:
    case Gfx::TextAlignment::BottomLeft:
        return CellTypeDialog::HorizontalAlignment::Left;
    }

    return CellTypeDialog::HorizontalAlignment::Right;
}

void CellTypeDialog::setup_tabs(GUI::TabWidget& tabs, Vector<Position> const& positions, Sheet& sheet)
{
    g_types.clear();
    for (auto& type_name : CellType::names())
        g_types.append(type_name);

    Vector<Cell&> cells;
    for (auto& position : positions) {
        if (auto cell = sheet.at(position))
            cells.append(*cell);
    }

    if (cells.size() == 1) {
        auto& cell = cells.first();
        m_format = cell.type_metadata().format;
        m_length = cell.type_metadata().length;
        m_type = &cell.type();
        m_vertical_alignment = vertical_alignment_from(cell.type_metadata().alignment);
        m_horizontal_alignment = horizontal_alignment_from(cell.type_metadata().alignment);
        m_static_format = cell.type_metadata().static_format;
        m_conditional_formats = cell.conditional_formats();
    }

    auto& type_tab = tabs.add_tab<GUI::Widget>("Type"_string);
    type_tab.set_layout<GUI::HorizontalBoxLayout>(4);
    {
        auto& left_side = type_tab.add<GUI::Widget>();
        left_side.set_layout<GUI::VerticalBoxLayout>();
        auto& right_side = type_tab.add<GUI::Widget>();
        right_side.set_layout<GUI::VerticalBoxLayout>();
        right_side.set_fixed_width(170);

        auto& type_list = left_side.add<GUI::ListView>();
        type_list.set_model(*GUI::ItemListModel<ByteString>::create(g_types));
        type_list.set_should_hide_unnecessary_scrollbars(true);
        type_list.on_selection_change = [&] {
            auto const& index = type_list.selection().first();
            if (!index.is_valid()) {
                m_type = nullptr;
                return;
            }

            m_type = CellType::get_by_name(g_types.at(index.row()));
            if (auto* editor = right_side.find_descendant_of_type_named<GUI::TextEditor>("format_editor"))
                editor->set_tooltip(m_type->metadata_hint(MetadataName::Format));
        };

        {
            auto& checkbox = right_side.add<GUI::CheckBox>("Override max length"_string);
            auto& spinbox = right_side.add<GUI::SpinBox>();
            checkbox.set_checked(m_length != -1);
            spinbox.set_min(0);
            spinbox.set_enabled(m_length != -1);
            if (m_length > -1)
                spinbox.set_value(m_length);

            checkbox.on_checked = [&](auto checked) {
                spinbox.set_enabled(checked);
                if (!checked) {
                    m_length = -1;
                    spinbox.set_value(0);
                }
            };
            spinbox.on_change = [&](auto value) {
                m_length = value;
            };
        }
        {
            auto& checkbox = right_side.add<GUI::CheckBox>("Override display format"_string);
            auto& editor = right_side.add<GUI::TextEditor>();
            checkbox.set_checked(!m_format.is_empty());
            editor.set_name("format_editor");
            editor.set_should_hide_unnecessary_scrollbars(true);
            editor.set_enabled(!m_format.is_empty());
            editor.set_text(m_format);

            checkbox.on_checked = [&](auto checked) {
                editor.set_enabled(checked);
                if (!checked)
                    m_format = ByteString::empty();
                editor.set_text(m_format);
            };
            editor.on_change = [&] {
                m_format = editor.text();
            };
        }
    }

    auto& alignment_tab = tabs.add_tab<GUI::Widget>("Alignment"_string);
    alignment_tab.set_layout<GUI::VerticalBoxLayout>(4);
    {
        // FIXME: Frame?
        // Horizontal alignment
        {
            auto& horizontal_alignment_selection_container = alignment_tab.add<GUI::Widget>();
            horizontal_alignment_selection_container.set_layout<GUI::HorizontalBoxLayout>(GUI::Margins { 4, 0, 0 });
            horizontal_alignment_selection_container.set_fixed_height(22);

            auto& horizontal_alignment_label = horizontal_alignment_selection_container.add<GUI::Label>();
            horizontal_alignment_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
            horizontal_alignment_label.set_text("Horizontal text alignment"_string);

            auto& horizontal_combobox = alignment_tab.add<GUI::ComboBox>();
            horizontal_combobox.set_only_allow_values_from_model(true);
            horizontal_combobox.set_model(*GUI::ItemListModel<ByteString>::create(g_horizontal_alignments));
            horizontal_combobox.set_selected_index((int)m_horizontal_alignment);
            horizontal_combobox.on_change = [&](auto&, const GUI::ModelIndex& index) {
                switch (index.row()) {
                case 0:
                    m_horizontal_alignment = HorizontalAlignment::Left;
                    break;
                case 1:
                    m_horizontal_alignment = HorizontalAlignment::Center;
                    break;
                case 2:
                    m_horizontal_alignment = HorizontalAlignment::Right;
                    break;
                default:
                    VERIFY_NOT_REACHED();
                }
            };
        }

        // Vertical alignment
        {
            auto& vertical_alignment_container = alignment_tab.add<GUI::Widget>();
            vertical_alignment_container.set_layout<GUI::HorizontalBoxLayout>(GUI::Margins { 4, 0, 0 });
            vertical_alignment_container.set_fixed_height(22);

            auto& vertical_alignment_label = vertical_alignment_container.add<GUI::Label>();
            vertical_alignment_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
            vertical_alignment_label.set_text("Vertical text alignment"_string);

            auto& vertical_combobox = alignment_tab.add<GUI::ComboBox>();
            vertical_combobox.set_only_allow_values_from_model(true);
            vertical_combobox.set_model(*GUI::ItemListModel<ByteString>::create(g_vertical_alignments));
            vertical_combobox.set_selected_index((int)m_vertical_alignment);
            vertical_combobox.on_change = [&](auto&, const GUI::ModelIndex& index) {
                switch (index.row()) {
                case 0:
                    m_vertical_alignment = VerticalAlignment::Top;
                    break;
                case 1:
                    m_vertical_alignment = VerticalAlignment::Center;
                    break;
                case 2:
                    m_vertical_alignment = VerticalAlignment::Bottom;
                    break;
                default:
                    VERIFY_NOT_REACHED();
                }
            };
        }
    }

    auto& colors_tab = tabs.add_tab<GUI::Widget>("Color"_string);
    colors_tab.set_layout<GUI::VerticalBoxLayout>(4);
    {
        // Static formatting
        {
            auto& static_formatting_container = colors_tab.add<GUI::Widget>();
            static_formatting_container.set_layout<GUI::VerticalBoxLayout>();

            // Foreground
            {
                // FIXME: Somehow allow unsetting these.
                auto& foreground_container = static_formatting_container.add<GUI::Widget>();
                foreground_container.set_layout<GUI::HorizontalBoxLayout>(GUI::Margins { 4, 0, 0 });
                foreground_container.set_preferred_height(GUI::SpecialDimension::Fit);

                auto& foreground_label = foreground_container.add<GUI::Label>();
                foreground_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
                foreground_label.set_text("Static foreground color"_string);

                auto& foreground_selector = foreground_container.add<GUI::ColorInput>();
                if (m_static_format.foreground_color.has_value())
                    foreground_selector.set_color(m_static_format.foreground_color.value());
                foreground_selector.on_change = [&]() {
                    m_static_format.foreground_color = foreground_selector.color();
                };
            }

            // Background
            {
                // FIXME: Somehow allow unsetting these.
                auto& background_container = static_formatting_container.add<GUI::Widget>();
                background_container.set_layout<GUI::HorizontalBoxLayout>(GUI::Margins { 4, 0, 0 });
                background_container.set_preferred_height(GUI::SpecialDimension::Fit);

                auto& background_label = background_container.add<GUI::Label>();
                background_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
                background_label.set_text("Static background color"_string);

                auto& background_selector = background_container.add<GUI::ColorInput>();
                if (m_static_format.background_color.has_value())
                    background_selector.set_color(m_static_format.background_color.value());
                background_selector.on_change = [&]() {
                    m_static_format.background_color = background_selector.color();
                };
            }
        }
    }

    auto& conditional_fmt_tab = tabs.add_tab<GUI::Widget>("Conditional format"_string);
    conditional_fmt_tab.load_from_gml(cond_fmt_gml).release_value_but_fixme_should_propagate_errors();
    {
        auto& view = *conditional_fmt_tab.find_descendant_of_type_named<Spreadsheet::ConditionsView>("conditions_view");
        view.set_formats(&m_conditional_formats);

        auto& add_button = *conditional_fmt_tab.find_descendant_of_type_named<GUI::Button>("add_button");
        add_button.on_click = [&](auto) {
            view.add_format();
        };

        // FIXME: Disable this when empty.
        auto& remove_button = *conditional_fmt_tab.find_descendant_of_type_named<GUI::Button>("remove_button");
        remove_button.on_click = [&](auto) {
            view.remove_top();
        };
    }
}

CellTypeMetadata CellTypeDialog::metadata() const
{
    CellTypeMetadata metadata;
    metadata.format = m_format;
    metadata.length = m_length;
    metadata.static_format = m_static_format;

    switch (m_vertical_alignment) {
    case VerticalAlignment::Top:
        switch (m_horizontal_alignment) {
        case HorizontalAlignment::Left:
            metadata.alignment = Gfx::TextAlignment::TopLeft;
            break;
        case HorizontalAlignment::Center:
            metadata.alignment = Gfx::TextAlignment::TopCenter;
            break;
        case HorizontalAlignment::Right:
            metadata.alignment = Gfx::TextAlignment::TopRight;
            break;
        }
        break;
    case VerticalAlignment::Center:
        switch (m_horizontal_alignment) {
        case HorizontalAlignment::Left:
            metadata.alignment = Gfx::TextAlignment::CenterLeft;
            break;
        case HorizontalAlignment::Center:
            metadata.alignment = Gfx::TextAlignment::Center;
            break;
        case HorizontalAlignment::Right:
            metadata.alignment = Gfx::TextAlignment::CenterRight;
            break;
        }
        break;
    case VerticalAlignment::Bottom:
        switch (m_horizontal_alignment) {
        case HorizontalAlignment::Left:
            metadata.alignment = Gfx::TextAlignment::CenterLeft; // BottomLeft?
            break;
        case HorizontalAlignment::Center:
            metadata.alignment = Gfx::TextAlignment::Center;
            break;
        case HorizontalAlignment::Right:
            metadata.alignment = Gfx::TextAlignment::BottomRight;
            break;
        }
        break;
    }

    return metadata;
}

ConditionView::ConditionView(ConditionalFormat& fmt)
    : m_format(fmt)
{
    load_from_gml(cond_fmt_view_gml).release_value_but_fixme_should_propagate_errors();

    auto& fg_input = *find_descendant_of_type_named<GUI::ColorInput>("foreground_input");
    auto& bg_input = *find_descendant_of_type_named<GUI::ColorInput>("background_input");
    auto& formula_editor = *find_descendant_of_type_named<GUI::TextEditor>("formula_editor");

    if (m_format.foreground_color.has_value())
        fg_input.set_color(m_format.foreground_color.value());

    if (m_format.background_color.has_value())
        bg_input.set_color(m_format.background_color.value());

    formula_editor.set_text(m_format.condition);

    // FIXME: Allow unsetting these.
    fg_input.on_change = [&] {
        m_format.foreground_color = fg_input.color();
    };

    bg_input.on_change = [&] {
        m_format.background_color = bg_input.color();
    };

    formula_editor.set_syntax_highlighter(make<JS::SyntaxHighlighter>());
    formula_editor.set_should_hide_unnecessary_scrollbars(true);
    formula_editor.on_change = [&] {
        m_format.condition = formula_editor.text();
    };
}

ConditionView::~ConditionView()
{
}

ConditionsView::ConditionsView()
{
    set_layout<GUI::VerticalBoxLayout>(6, 4);
}

void ConditionsView::set_formats(Vector<ConditionalFormat>* formats)
{
    VERIFY(!m_formats);

    m_formats = formats;

    for (auto& entry : *m_formats)
        m_widgets.append(add<ConditionView>(entry));
}

void ConditionsView::add_format()
{
    VERIFY(m_formats);

    m_formats->empend();
    auto& last = m_formats->last();

    m_widgets.append(add<ConditionView>(last));

    update();
}

void ConditionsView::remove_top()
{
    VERIFY(m_formats);

    if (m_formats->is_empty())
        return;

    m_formats->take_last();
    m_widgets.take_last()->remove_from_parent();
    update();
}

ConditionsView::~ConditionsView()
{
}

}
