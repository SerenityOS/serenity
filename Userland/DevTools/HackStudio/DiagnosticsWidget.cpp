/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DiagnosticsWidget.h"
#include "HackStudio.h"
#include <LibGUI/BoxLayout.h>

namespace HackStudio {

DiagnosticsData DiagnosticsData::s_this {};

class DiagnosticsModel final : public GUI::Model {
public:
    enum Column {
        Filename,
        Level,
        Text,
        Line,
        Column,
        __Count,
    };

    DiagnosticsModel() = default;

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return DiagnosticsData::the().diagnostics().size(); }
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return Column::__Count; }

    virtual String column_name(int column) const override
    {
        switch (column) {
        case Column::Filename:
            return "Filename";
        case Column::Level:
            return "Level";
        case Column::Text:
            return "Text";
        case Column::Line:
            return "Line";
        case Column::Column:
            return "Column";
        default:
            VERIFY_NOT_REACHED();
        }
    }

    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role) const override
    {
        if (role == GUI::ModelRole::TextAlignment)
            return Gfx::TextAlignment::CenterLeft;
        if (role == GUI::ModelRole::Display) {
            auto const& value = DiagnosticsData::the().diagnostics().at(index.row());
            switch (index.column()) {
            case Column::Filename:
                return value.start_position.file;
            case Column::Level:
                return value.level == Diagnostic::Level::Error  ? "Error"
                    : value.level == Diagnostic::Level::Note    ? "Note"
                    : value.level == Diagnostic::Level::Info    ? "Info"
                    : value.level == Diagnostic::Level::Warning ? "Warning"
                                                                : "Unknown";
            case Column::Text:
                return value.text;
            case Column::Line:
                return String::number(value.start_position.line);
            case Column::Column:
                return String::number(value.start_position.column);
            }
        }
        return {};
    }

    virtual GUI::ModelIndex index(int row, int column = 0, const GUI::ModelIndex& = GUI::ModelIndex()) const override
    {
        if (row < 0 || row >= (int)DiagnosticsData::the().diagnostics().size())
            return {};
        if (column < 0 || column >= Column::__Count)
            return {};
        return create_index(row, column, &DiagnosticsData::the().diagnostics().at(row));
    }
};

DiagnosticsWidget::DiagnosticsWidget()
{
    set_layout<GUI::VerticalBoxLayout>();

    m_result_view = add<GUI::TableView>();
    m_result_view->set_model(make_ref_counted<DiagnosticsModel>());

    m_result_view->on_activation = [](auto& index) {
        auto const& value = *(Diagnostic const*)index.internal_data();
        open_file(value.start_position.file, value.start_position.line - 1, value.start_position.column - 1);
    };
}

}
