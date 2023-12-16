/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "CellType/Type.h"
#include "ConditionalFormatting.h"
#include "Forward.h"
#include "JSIntegration.h"
#include "Position.h"
#include <AK/ByteString.h>
#include <AK/Types.h>
#include <AK/WeakPtr.h>
#include <LibGUI/Command.h>

namespace Spreadsheet {

struct Cell : public Weakable<Cell> {
    enum Kind {
        LiteralString,
        Formula,
    };

    Cell(ByteString data, Position position, WeakPtr<Sheet> sheet)
        : m_dirty(false)
        , m_data(move(data))
        , m_kind(LiteralString)
        , m_sheet(sheet)
        , m_position(move(position))
    {
    }

    Cell(ByteString source, JS::Value&& cell_value, Position position, WeakPtr<Sheet> sheet)
        : m_dirty(false)
        , m_data(move(source))
        , m_evaluated_data(move(cell_value))
        , m_kind(Formula)
        , m_sheet(sheet)
        , m_position(move(position))
    {
    }

    void reference_from(Cell*);

    void set_data(ByteString new_data);
    void set_data(JS::Value new_data);
    bool dirty() const { return m_dirty; }
    void clear_dirty() { m_dirty = false; }

    StringView name_for_javascript(Sheet const& sheet) const
    {
        if (!m_name_for_javascript.is_empty())
            return m_name_for_javascript;

        m_name_for_javascript = ByteString::formatted("cell {}", m_position.to_cell_identifier(sheet));
        return m_name_for_javascript;
    }

    void set_thrown_value(JS::Value value) { m_thrown_value = value; }
    Optional<JS::Value> thrown_value() const
    {
        if (m_thrown_value.is_empty())
            return {};
        return m_thrown_value;
    }

    ByteString const& data() const { return m_data; }
    const JS::Value& evaluated_data() const { return m_evaluated_data; }
    Kind kind() const { return m_kind; }
    Vector<WeakPtr<Cell>> const& referencing_cells() const { return m_referencing_cells; }

    void set_type(StringView name);
    void set_type(CellType const*);
    void set_type_metadata(CellTypeMetadata const&);
    void set_type_metadata(CellTypeMetadata&&);

    Position const& position() const { return m_position; }
    void set_position(Position position, Badge<Sheet>)
    {
        if (position != m_position) {
            m_dirty = true;
            m_position = move(position);
        }
    }

    Format const& evaluated_formats() const { return m_evaluated_formats; }
    Format& evaluated_formats() { return m_evaluated_formats; }
    Vector<ConditionalFormat> const& conditional_formats() const { return m_conditional_formats; }
    void set_conditional_formats(Vector<ConditionalFormat>&& fmts)
    {
        m_dirty = true;
        m_conditional_formats = move(fmts);
    }

    JS::ThrowCompletionOr<ByteString> typed_display() const;
    JS::ThrowCompletionOr<JS::Value> typed_js_data() const;

    CellType const& type() const;
    CellTypeMetadata const& type_metadata() const { return m_type_metadata; }
    CellTypeMetadata& type_metadata() { return m_type_metadata; }

    ByteString source() const;

    JS::Value js_data();

    void update();
    void update_data(Badge<Sheet>);

    Sheet const& sheet() const { return *m_sheet; }
    Sheet& sheet() { return *m_sheet; }

    void copy_from(Cell const&);

private:
    bool m_dirty { false };
    bool m_evaluated_externally { false };
    ByteString m_data;
    JS::Value m_evaluated_data;
    JS::Value m_thrown_value;
    Kind m_kind { LiteralString };
    WeakPtr<Sheet> m_sheet;
    Vector<WeakPtr<Cell>> m_referencing_cells;
    CellType const* m_type { nullptr };
    CellTypeMetadata m_type_metadata;
    Position m_position;
    mutable ByteString m_name_for_javascript;

    Vector<ConditionalFormat> m_conditional_formats;
    Format m_evaluated_formats;
};

}
