/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Cell.h"
#include "Spreadsheet.h"
#include <AK/StringBuilder.h>
#include <AK/TemporaryChange.h>

namespace Spreadsheet {

void Cell::set_data(String new_data)
{
    if (m_data == new_data)
        return;

    if (new_data.starts_with("=")) {
        new_data = new_data.substring(1, new_data.length() - 1);
        m_kind = Formula;
    } else {
        m_kind = LiteralString;
    }

    m_data = move(new_data);
    m_dirty = true;
    m_evaluated_externally = false;
}

void Cell::set_data(JS::Value new_data)
{
    m_dirty = true;
    m_evaluated_externally = true;

    StringBuilder builder;

    builder.append(new_data.to_string_without_side_effects());
    m_data = builder.build();

    m_evaluated_data = move(new_data);
}

void Cell::set_type(const CellType* type)
{
    m_type = type;
}

void Cell::set_type(StringView name)
{
    auto* cell_type = CellType::get_by_name(name);
    if (cell_type) {
        return set_type(cell_type);
    }

    VERIFY_NOT_REACHED();
}

void Cell::set_type_metadata(CellTypeMetadata&& metadata)
{
    m_type_metadata = move(metadata);
}

const CellType& Cell::type() const
{
    if (m_type)
        return *m_type;

    if (m_kind == LiteralString) {
        if (m_data.to_int().has_value())
            return *CellType::get_by_name("Numeric");
    }

    return *CellType::get_by_name("Identity");
}

JS::ThrowCompletionOr<String> Cell::typed_display() const
{
    return type().display(const_cast<Cell&>(*this), m_type_metadata);
}

JS::ThrowCompletionOr<JS::Value> Cell::typed_js_data() const
{
    return type().js_value(const_cast<Cell&>(*this), m_type_metadata);
}

void Cell::update_data(Badge<Sheet>)
{
    TemporaryChange cell_change { m_sheet->current_evaluated_cell(), this };
    if (!m_dirty)
        return;

    m_js_exception = {};

    if (m_dirty) {
        m_dirty = false;
        if (m_kind == Formula) {
            if (!m_evaluated_externally) {
                auto [value, exception] = m_sheet->evaluate(m_data, this);
                m_evaluated_data = value;
                m_js_exception = move(exception);
            }
        }

        for (auto& ref : m_referencing_cells) {
            if (ref) {
                ref->m_dirty = true;
                ref->update();
            }
        }
    }

    m_evaluated_formats.background_color.clear();
    m_evaluated_formats.foreground_color.clear();
    if (!m_js_exception) {
        for (auto& fmt : m_conditional_formats) {
            if (!fmt.condition.is_empty()) {
                auto [value, exception] = m_sheet->evaluate(fmt.condition, this);
                if (exception) {
                    m_js_exception = move(exception);
                } else {
                    if (value.to_boolean()) {
                        if (fmt.background_color.has_value())
                            m_evaluated_formats.background_color = fmt.background_color;
                        if (fmt.foreground_color.has_value())
                            m_evaluated_formats.foreground_color = fmt.foreground_color;
                    }
                }
            }
        }
    }
}

void Cell::update()
{
    m_sheet->update(*this);
}

JS::Value Cell::js_data()
{
    if (m_dirty)
        update();

    if (m_kind == Formula)
        return m_evaluated_data;

    return JS::js_string(m_sheet->interpreter().heap(), m_data);
}

String Cell::source() const
{
    StringBuilder builder;
    if (m_kind == Formula)
        builder.append('=');
    builder.append(m_data);
    return builder.to_string();
}

// FIXME: Find a better way to figure out dependencies
void Cell::reference_from(Cell* other)
{
    if (!other || other == this)
        return;

    if (!m_referencing_cells.find_if([other](const auto& ptr) { return ptr.ptr() == other; }).is_end())
        return;

    m_referencing_cells.append(other->make_weak_ptr());
}

void Cell::copy_from(const Cell& other)
{
    m_dirty = true;
    m_evaluated_externally = other.m_evaluated_externally;
    m_data = other.m_data;
    m_evaluated_data = other.m_evaluated_data;
    m_kind = other.m_kind;
    m_type = other.m_type;
    m_type_metadata = other.m_type_metadata;
    m_conditional_formats = other.m_conditional_formats;
    m_evaluated_formats = other.m_evaluated_formats;
    if (!other.m_js_exception)
        m_js_exception = other.m_js_exception;
}

}
