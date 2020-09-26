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

#pragma once

#include "Cell.h"
#include "Forward.h"
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
    ~Sheet();

    static Optional<Position> parse_cell_name(const StringView&);

    JsonObject to_json() const;
    static RefPtr<Sheet> from_json(const JsonObject&, Workbook&);

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

        m_cells.set(position, make<Cell>(String::empty(), position, make_weak_ptr()));
        return *at(position);
    }

    size_t add_row();
    String add_column();

    size_t row_count() const { return m_rows; }
    size_t column_count() const { return m_columns.size(); }
    const Vector<String>& columns() const { return m_columns; }
    const String& column(size_t index) const
    {
        ASSERT(column_count() > index);
        return m_columns[index];
    }

    void update();
    void update(Cell&);

    JS::Value evaluate(const StringView&, Cell* = nullptr);
    JS::Interpreter& interpreter() const;
    SheetGlobalObject& global_object() const { return *m_global_object; }

    Cell*& current_evaluated_cell() { return m_current_cell_being_evaluated; }
    bool has_been_visited(Cell* cell) const { return m_visited_cells_in_update.contains(cell); }

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

    size_t m_current_column_name_length { 0 };

    HashTable<Cell*> m_visited_cells_in_update;
};

}

namespace AK {

template<>
struct Traits<Spreadsheet::Position> : public GenericTraits<Spreadsheet::Position> {
    static constexpr bool is_trivial() { return false; }
    static unsigned hash(const Spreadsheet::Position& p)
    {
        return pair_int_hash(
            string_hash(p.column.characters(), p.column.length()),
            u64_hash(p.row));
    }
};

}
