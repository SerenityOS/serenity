/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SpreadsheetModel.h"
#include "ConditionalFormatting.h"
#include <LibGUI/AbstractView.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/Object.h>
#include <LibURL/URL.h>

namespace Spreadsheet {

GUI::Variant SheetModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    if (!index.is_valid())
        return {};

    if (role == GUI::ModelRole::Display) {
        auto const* cell = m_sheet->at({ (size_t)index.column(), (size_t)index.row() });
        if (!cell)
            return ByteString::empty();

        Function<ByteString(JS::Value)> to_byte_string_as_exception = [&](JS::Value value) {
            auto& vm = cell->sheet().global_object().vm();
            StringBuilder builder;
            builder.append("Error: "sv);
            if (value.is_object()) {
                auto& object = value.as_object();
                if (is<JS::Error>(object)) {
                    auto message = object.get_without_side_effects("message");
                    auto error = message.to_byte_string(vm);
                    if (error.is_throw_completion())
                        builder.append(message.to_string_without_side_effects());
                    else
                        builder.append(error.release_value());
                    return builder.to_byte_string();
                }
            }
            auto error_message = value.to_byte_string(vm);

            if (error_message.is_throw_completion())
                return to_byte_string_as_exception(*error_message.release_error().value());

            builder.append(error_message.release_value());
            return builder.to_byte_string();
        };

        if (cell->kind() == Spreadsheet::Cell::Formula) {
            if (auto opt_throw_value = cell->thrown_value(); opt_throw_value.has_value())
                return to_byte_string_as_exception(*opt_throw_value);
        }

        auto display = cell->typed_display();
        if (display.is_error())
            return to_byte_string_as_exception(*display.release_error().value());

        return display.release_value();
    }

    if (role == GUI::ModelRole::MimeData)
        return Position { (size_t)index.column(), (size_t)index.row() }.to_url(m_sheet).to_byte_string();

    if (role == GUI::ModelRole::TextAlignment) {
        auto const* cell = m_sheet->at({ (size_t)index.column(), (size_t)index.row() });
        if (!cell)
            return {};

        return cell->type_metadata().alignment;
    }

    if (role == GUI::ModelRole::ForegroundColor) {
        auto const* cell = m_sheet->at({ (size_t)index.column(), (size_t)index.row() });
        if (!cell)
            return {};

        if (cell->kind() == Spreadsheet::Cell::Formula) {
            if (cell->thrown_value().has_value())
                return Color(Color::Red);
        }

        if (cell->evaluated_formats().foreground_color.has_value())
            return cell->evaluated_formats().foreground_color.value();

        if (auto color = cell->type_metadata().static_format.foreground_color; color.has_value())
            return color.value();

        return {};
    }

    if (role == GUI::ModelRole::BackgroundColor) {
        auto const* cell = m_sheet->at({ (size_t)index.column(), (size_t)index.row() });
        if (!cell)
            return {};

        if (cell->evaluated_formats().background_color.has_value())
            return cell->evaluated_formats().background_color.value();

        if (auto color = cell->type_metadata().static_format.background_color; color.has_value())
            return color.value();

        return {};
    }

    if (to_underlying(role) == to_underlying(Role::Tooltip)) {
        auto const* cell = m_sheet->at({ (size_t)index.column(), (size_t)index.row() });
        if (!cell || !cell->thrown_value().has_value())
            return {};

        auto value = cell->thrown_value().value();
        if (!value.is_object())
            return {};

        auto& object = value.as_object();
        if (!is<JS::Error>(object))
            return {};

        auto& error = static_cast<JS::Error&>(object);
        auto const& trace = error.traceback();
        StringBuilder builder;
        builder.appendff("{}\n", error.get_without_side_effects(object.vm().names.message).to_string_without_side_effects());
        for (auto const& frame : trace.in_reverse()) {
            if (frame.source_range().filename().contains("runtime.js"sv)) {
                if (frame.function_name == "<unknown>")
                    builder.appendff("  in a builtin function at line {}, column {}\n", frame.source_range().start.line, frame.source_range().start.column);
                else
                    builder.appendff("  while evaluating builtin '{}'\n", frame.function_name);
            } else if (frame.source_range().filename().starts_with("cell "sv)) {
                auto filename = frame.source_range().filename();
                builder.appendff("  in cell '{}', at line {}, column {}\n", filename.substring_view(5), frame.source_range().start.line, frame.source_range().start.column);
            }
        }
        return builder.to_byte_string();
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
    auto mime_data_buffer = mime_data->data("text/x-spreadsheet-data"sv);
    auto new_data = ByteString::formatted("{}\n{}",
        cursor_position.to_url(m_sheet).to_byte_string(),
        StringView(mime_data_buffer));
    mime_data->set_data("text/x-spreadsheet-data"_string, new_data.to_byte_buffer());

    return mime_data;
}

ErrorOr<String> SheetModel::column_name(int index) const
{
    if (index < 0)
        return String {};

    return TRY(String::from_byte_string(m_sheet->column(index)));
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
    auto previous_data = cell.data();
    cell.set_data(value.to_byte_string());
    if (on_cell_data_change)
        on_cell_data_change(cell, previous_data);
    did_update(UpdateFlag::DontInvalidateIndices);
}

void SheetModel::update()
{
    m_sheet->update();
    did_update(UpdateFlag::DontInvalidateIndices | Model::UpdateFlag::DontResizeColumns);
}

CellsUndoCommand::CellsUndoCommand(Vector<CellChange> cell_changes)
{
    m_cell_changes = cell_changes;
}

CellsUndoCommand::CellsUndoCommand(Cell& cell, ByteString const& previous_data)
{
    m_cell_changes.append(CellChange(cell, previous_data));
}

void CellsUndoCommand::undo()
{
    for (size_t i = 0; i < m_cell_changes.size(); ++i) {
        m_cell_changes[i].cell().set_data(m_cell_changes[i].previous_data());
    }
}

void CellsUndoCommand::redo()
{
    for (size_t i = 0; i < m_cell_changes.size(); ++i) {
        m_cell_changes[i].cell().set_data(m_cell_changes[i].new_data());
    }
}

CellsUndoMetadataCommand::CellsUndoMetadataCommand(Vector<CellChange> cell_changes)
{
    m_cell_changes = move(cell_changes);
}

void CellsUndoMetadataCommand::undo()
{
    for (size_t i = 0; i < m_cell_changes.size(); ++i) {
        m_cell_changes[i].cell().set_type_metadata(m_cell_changes[i].previous_type_metadata());
    }
}

void CellsUndoMetadataCommand::redo()
{
    for (size_t i = 0; i < m_cell_changes.size(); ++i) {
        m_cell_changes[i].cell().set_type_metadata(m_cell_changes[i].new_type_metadata());
    }
}

}
