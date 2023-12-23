/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Cell.h"
#include "Spreadsheet.h"
#include <AK/StringBuilder.h>
#include <AK/TemporaryChange.h>
#include <LibJS/Runtime/ValueInlines.h>

namespace Spreadsheet {

void Cell::set_data(ByteString new_data)
{
    // If we are a formula, we do not save the beginning '=', if the new_data is "" we can simply change our kind
    if (m_kind == Formula && m_data.is_empty() && new_data.is_empty()) {
        m_kind = LiteralString;
        return;
    }

    if (m_data == new_data)
        return;

    if (new_data.starts_with('=')) {
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
    m_data = builder.to_byte_string();

    m_evaluated_data = move(new_data);
}

void Cell::set_type(CellType const* type)
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

void Cell::set_type_metadata(CellTypeMetadata const& metadata)
{
    m_type_metadata = metadata;
}

void Cell::set_type_metadata(CellTypeMetadata&& metadata)
{
    m_type_metadata = move(metadata);
}

CellType const& Cell::type() const
{
    if (m_type)
        return *m_type;

    if (m_kind == LiteralString) {
        if (m_data.to_number<int>().has_value())
            return *CellType::get_by_name("Numeric"sv);
    }

    return *CellType::get_by_name("Identity"sv);
}

JS::ThrowCompletionOr<ByteString> Cell::typed_display() const
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

    if (m_dirty) {
        m_dirty = false;
        if (m_kind == Formula) {
            if (!m_evaluated_externally) {
                auto value_or_error = m_sheet->evaluate(m_data, this);
                if (value_or_error.is_error()) {
                    m_evaluated_data = JS::js_undefined();
                    m_thrown_value = *value_or_error.release_error().release_value();
                } else {
                    m_evaluated_data = value_or_error.release_value();
                    m_thrown_value = {};
                }
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
    if (m_thrown_value.is_empty()) {
        for (auto& fmt : m_conditional_formats) {
            if (!fmt.condition.is_empty()) {
                auto value_or_error = m_sheet->evaluate(fmt.condition, this);
                if (value_or_error.is_error()) {
                    m_thrown_value = *value_or_error.release_error().release_value();
                } else {
                    if (value_or_error.release_value().to_boolean()) {
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

    auto& vm = m_sheet->vm();
    return JS::PrimitiveString::create(vm, m_data);
}

ByteString Cell::source() const
{
    StringBuilder builder;
    if (m_kind == Formula)
        builder.append('=');
    builder.append(m_data);
    return builder.to_byte_string();
}

// FIXME: Find a better way to figure out dependencies
void Cell::reference_from(Cell* other)
{
    if (!other || other == this)
        return;

    if (!m_referencing_cells.find_if([other](auto const& ptr) { return ptr.ptr() == other; }).is_end())
        return;

    m_referencing_cells.append(other->make_weak_ptr());
}

void Cell::copy_from(Cell const& other)
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
    m_thrown_value = other.m_thrown_value;
}

}
