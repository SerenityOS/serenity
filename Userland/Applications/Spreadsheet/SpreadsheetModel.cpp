/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SpreadsheetModel.h"
#include "ConditionalFormatting.h"
#include <AK/URL.h>
#include <LibGUI/AbstractView.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/Object.h>

namespace Spreadsheet {

SheetModel::~SheetModel()
{
}

GUI::Variant SheetModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    if (!index.is_valid())
        return {};

    if (role == GUI::ModelRole::Display) {
        const auto* cell = m_sheet->at({ (size_t)index.column(), (size_t)index.row() });
        if (!cell)
            return String::empty();

        if (cell->kind() == Spreadsheet::Cell::Formula) {
            if (auto exception = cell->exception()) {
                StringBuilder builder;
                builder.append("Error: ");
                auto value = exception->value();
                if (value.is_object()) {
                    auto& object = value.as_object();
                    if (is<JS::Error>(object)) {
                        auto error = object.get_without_side_effects("message").to_string_without_side_effects();
                        builder.append(error);
                        return builder.to_string();
                    }
                }
                auto error = value.to_string_without_side_effects();
                // This is annoying, but whatever.
                cell->sheet().interpreter().vm().clear_exception();

                builder.append(error);
                return builder.to_string();
            }
        }

        return cell->typed_display();
    }

    if (role == GUI::ModelRole::MimeData)
        return Position { (size_t)index.column(), (size_t)index.row() }.to_url(m_sheet).to_string();

    if (role == GUI::ModelRole::TextAlignment) {
        const auto* cell = m_sheet->at({ (size_t)index.column(), (size_t)index.row() });
        if (!cell)
            return {};

        return cell->type_metadata().alignment;
    }

    if (role == GUI::ModelRole::ForegroundColor) {
        const auto* cell = m_sheet->at({ (size_t)index.column(), (size_t)index.row() });
        if (!cell)
            return {};

        if (cell->kind() == Spreadsheet::Cell::Formula) {
            if (cell->exception())
                return Color(Color::Red);
        }

        if (cell->evaluated_formats().foreground_color.has_value())
            return cell->evaluated_formats().foreground_color.value();

        if (auto color = cell->type_metadata().static_format.foreground_color; color.has_value())
            return color.value();

        return {};
    }

    if (role == GUI::ModelRole::BackgroundColor) {
        const auto* cell = m_sheet->at({ (size_t)index.column(), (size_t)index.row() });
        if (!cell)
            return {};

        if (cell->evaluated_formats().background_color.has_value())
            return cell->evaluated_formats().background_color.value();

        if (auto color = cell->type_metadata().static_format.background_color; color.has_value())
            return color.value();

        return {};
    }

    return {};
}

RefPtr<Core::MimeData> SheetModel::mime_data(const GUI::ModelSelection& selection) const
{
    auto mime_data = GUI::Model::mime_data(selection);

    bool first = true;
    const GUI::ModelIndex* cursor = nullptr;
    const_cast<SheetModel*>(this)->for_each_view([&](const GUI::AbstractView& view) {
        if (!first)
            return;
        cursor = &view.cursor_index();
        first = false;
    });

    VERIFY(cursor);

    Position cursor_position { (size_t)cursor->column(), (size_t)cursor->row() };
    auto mime_data_buffer = mime_data->data("text/x-spreadsheet-data");
    auto new_data = String::formatted("{}\n{}",
        cursor_position.to_url(m_sheet).to_string(),
        StringView(mime_data_buffer));
    mime_data->set_data("text/x-spreadsheet-data", new_data.to_byte_buffer());

    return mime_data;
}

String SheetModel::column_name(int index) const
{
    if (index < 0)
        return {};

    return m_sheet->column(index);
}

bool SheetModel::is_editable(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return false;

    return true;
}

void SheetModel::set_data(const GUI::ModelIndex& index, const GUI::Variant& value)
{
    if (!index.is_valid())
        return;

    auto& cell = m_sheet->ensure({ (size_t)index.column(), (size_t)index.row() });
    cell.set_data(value.to_string());
    did_update(UpdateFlag::DontInvalidateIndices);
}

void SheetModel::update()
{
    m_sheet->update();
    did_update(UpdateFlag::DontInvalidateIndices);
}

}
