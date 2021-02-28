/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
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

#pragma once

#include "Cell.h"
#include "Forward.h"
#include "Readers/XSV.h"
#include <AK/HashMap.h>
#include <AK/HashTable.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Traits.h>
#include <AK/Types.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <LibCore/Object.h>
#include <LibJS/Interpreter.h>

namespace Spreadsheet {

class Sheet : public Core::Object {
    C_OBJECT(Sheet);

public:
    constexpr static size_t default_row_count = 100;
    constexpr static size_t default_column_count = 26;

    ~Sheet();

    Optional<Position> parse_cell_name(const StringView&) const;
    Optional<size_t> column_index(const StringView& column_name) const;
    Optional<String> column_arithmetic(const StringView& column_name, int offset);

    Cell* from_url(const URL&);
    const Cell* from_url(const URL& url) const { return const_cast<Sheet*>(this)->from_url(url); }
    Optional<Position> position_from_url(const URL& url) const;

    /// Resolve 'offset' to an absolute position assuming 'base' is at 'offset_base'.
    /// Effectively, "Walk the distance between 'offset' and 'offset_base' away from 'base'".
    Position offset_relative_to(const Position& base, const Position& offset, const Position& offset_base) const;

    JsonObject to_json() const;
    static RefPtr<Sheet> from_json(const JsonObject&, Workbook&);

    Vector<Vector<String>> to_xsv() const;
    static RefPtr<Sheet> from_xsv(const Reader::XSV&, Workbook&);

    const String& name() const { return m_name; }
    void set_name(const StringView& name) { m_name = name; }

    JsonObject gather_documentation() const;

    const HashTable<Position>& selected_cells() const { return m_selected_cells; }
    HashTable<Position>& selected_cells() { return m_selected_cells; }
    const HashMap<Position, NonnullOwnPtr<Cell>>& cells() const { return m_cells; }
    HashMap<Position, NonnullOwnPtr<Cell>>& cells() { return m_cells; }

    Cell* at(const Position& position);
    const Cell* at(const Position& position) const { return const_cast<Sheet*>(this)->at(position); }

    const Cell* at(const StringView& name) const { return const_cast<Sheet*>(this)->at(name); }
    Cell* at(const StringView&);

    const Cell& ensure(const Position& position) const { return const_cast<Sheet*>(this)->ensure(position); }
    Cell& ensure(const Position& position)
    {
        if (auto cell = at(position))
            return *cell;

        m_cells.set(position, make<Cell>(String::empty(), position, *this));
        return *at(position);
    }

    size_t add_row();
    String add_column();

    size_t row_count() const { return m_rows; }
    size_t column_count() const { return m_columns.size(); }
    const Vector<String>& columns() const { return m_columns; }
    const String& column(size_t index)
    {
        for (size_t i = column_count(); i < index; ++i)
            add_column();

        VERIFY(column_count() > index);
        return m_columns[index];
    }
    const String& column(size_t index) const
    {
        VERIFY(column_count() > index);
        return m_columns[index];
    }

    void update();
    void update(Cell&);
    void disable_updates() { m_should_ignore_updates = true; }
    void enable_updates()
    {
        m_should_ignore_updates = false;
        if (m_update_requested) {
            m_update_requested = false;
            update();
        }
    }

    struct ValueAndException {
        JS::Value value;
        JS::Exception* exception { nullptr };
    };
    ValueAndException evaluate(const StringView&, Cell* = nullptr);
    JS::Interpreter& interpreter() const;
    SheetGlobalObject& global_object() const { return *m_global_object; }

    Cell*& current_evaluated_cell() { return m_current_cell_being_evaluated; }
    bool has_been_visited(Cell* cell) const { return m_visited_cells_in_update.contains(cell); }

    const Workbook& workbook() const { return m_workbook; }

    enum class CopyOperation {
        Copy,
        Cut
    };

    void copy_cells(Vector<Position> from, Vector<Position> to, Optional<Position> resolve_relative_to = {}, CopyOperation copy_operation = CopyOperation::Copy);

    /// Gives the bottom-right corner of the smallest bounding box containing all the written data.
    Position written_data_bounds() const;

    bool columns_are_standard() const;

    String generate_inline_documentation_for(StringView function, size_t argument_index);

private:
    explicit Sheet(Workbook&);
    explicit Sheet(const StringView& name, Workbook&);

    String m_name;
    Vector<String> m_columns;
    size_t m_rows { 0 };
    HashMap<Position, NonnullOwnPtr<Cell>> m_cells;
    HashTable<Position> m_selected_cells;

    Workbook& m_workbook;
    mutable SheetGlobalObject* m_global_object;

    Cell* m_current_cell_being_evaluated { nullptr };

    HashTable<Cell*> m_visited_cells_in_update;
    bool m_should_ignore_updates { false };
    bool m_update_requested { false };
    mutable Optional<JsonObject> m_cached_documentation;
};

}

namespace AK {

template<>
struct Traits<Spreadsheet::Position> : public GenericTraits<Spreadsheet::Position> {
    static constexpr bool is_trivial() { return false; }
    static unsigned hash(const Spreadsheet::Position& p)
    {
        return p.hash();
    }
};

}
