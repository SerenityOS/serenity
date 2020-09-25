/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "SpreadsheetModel.h"
#include "ConditionalFormatting.h"
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/Object.h>

namespace Spreadsheet {

SheetModel::~SheetModel()
{
}

static inline JS::Object* as_error(JS::Value value)
{
    if (value.is_object()) {
        auto& object = value.as_object();
        return object.is_error() ? &object : nullptr;
    }

    return nullptr;
}

GUI::Variant SheetModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    if (!index.is_valid())
        return {};

    if (role == GUI::ModelRole::Display) {
        const auto* cell = m_sheet->at({ m_sheet->column(index.column()), (size_t)index.row() });
        if (!cell)
            return String::empty();

        if (cell->kind == Spreadsheet::Cell::Formula) {
            if (auto object = as_error(cell->evaluated_data)) {
                StringBuilder builder;
                auto error = object->get("message").to_string_without_side_effects();
                builder.append("Error: ");
                builder.append(error);
                return builder.to_string();
            }
        }

        return cell->typed_display();
    }

    if (role == GUI::ModelRole::TextAlignment) {
        const auto* cell = m_sheet->at({ m_sheet->column(index.column()), (size_t)index.row() });
        if (!cell)
            return {};

        return cell->type_metadata().alignment;
    }

    if (role == GUI::ModelRole::ForegroundColor) {
        const auto* cell = m_sheet->at({ m_sheet->column(index.column()), (size_t)index.row() });
        if (!cell)
            return {};

        if (cell->kind == Spreadsheet::Cell::Formula) {
            if (as_error(cell->evaluated_data))
                return Color(Color::Red);
        }

        if (cell->evaluated_formats().foreground_color.has_value())
            return cell->evaluated_formats().foreground_color.value();

        if (auto color = cell->type_metadata().static_format.foreground_color; color.has_value())
            return color.value();

        return {};
    }

    if (role == GUI::ModelRole::BackgroundColor) {
        const auto* cell = m_sheet->at({ m_sheet->column(index.column()), (size_t)index.row() });
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

    auto& cell = m_sheet->ensure({ m_sheet->column(index.column()), (size_t)index.row() });
    cell.set_data(value.to_string());
    update();
}

void SheetModel::update()
{
    m_sheet->update();
}

}
