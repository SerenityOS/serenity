/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Cell.h"
#include "Forward.h"
#include "Readers/XSV.h"
#include <AK/ByteString.h>
#include <AK/HashMap.h>
#include <AK/HashTable.h>
#include <AK/StringBuilder.h>
#include <AK/Traits.h>
#include <AK/Types.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <LibCore/EventReceiver.h>

namespace Spreadsheet {

class CellChange {
public:
    CellChange(Cell&, ByteString const&);
    CellChange(Cell&, CellTypeMetadata const&);

    auto& cell() { return m_cell; }
    auto& previous_data() { return m_previous_data; }
    auto& new_data() { return m_new_data; }
    auto& previous_type_metadata() { return m_previous_type_metadata; }
    auto& new_type_metadata() { return m_new_type_metadata; }

private:
    Cell& m_cell;
    ByteString m_previous_data;
    ByteString m_new_data;
    CellTypeMetadata m_previous_type_metadata;
    CellTypeMetadata m_new_type_metadata;
};

class Sheet : public Core::EventReceiver {
    C_OBJECT(Sheet);

public:
    constexpr static size_t default_row_count = 100;
    constexpr static size_t default_column_count = 26;

    virtual ~Sheet() override = default;

    Optional<Position> parse_cell_name(StringView) const;
    Optional<size_t> column_index(StringView column_name) const;
    Optional<ByteString> column_arithmetic(StringView column_name, int offset);

    Cell* from_url(const URL::URL&);
    Cell const* from_url(const URL::URL& url) const { return const_cast<Sheet*>(this)->from_url(url); }
    Optional<Position> position_from_url(const URL::URL& url) const;

    /// Resolve 'offset' to an absolute position assuming 'base' is at 'offset_base'.
    /// Effectively, "Walk the distance between 'offset' and 'offset_base' away from 'base'".
    Position offset_relative_to(Position const& base, Position const& offset, Position const& offset_base) const;

    JsonObject to_json() const;
    static RefPtr<Sheet> from_json(JsonObject const&, Workbook&);

    Vector<Vector<ByteString>> to_xsv() const;
    static RefPtr<Sheet> from_xsv(Reader::XSV const&, Workbook&);

    ByteString const& name() const { return m_name; }
    void set_name(StringView name) { m_name = name; }

    JsonObject gather_documentation() const;

    HashTable<Position> const& selected_cells() const { return m_selected_cells; }
    HashTable<Position>& selected_cells() { return m_selected_cells; }
    HashMap<Position, NonnullOwnPtr<Cell>> const& cells() const { return m_cells; }
    HashMap<Position, NonnullOwnPtr<Cell>>& cells() { return m_cells; }

    Cell* at(Position const& position);
    Cell const* at(Position const& position) const { return const_cast<Sheet*>(this)->at(position); }

    Cell const* at(StringView name) const { return const_cast<Sheet*>(this)->at(name); }
    Cell* at(StringView);

    Cell const& ensure(Position const& position) const { return const_cast<Sheet*>(this)->ensure(position); }
    Cell& ensure(Position const& position)
    {
        if (auto cell = at(position))
            return *cell;

        m_cells.set(position, make<Cell>(ByteString::empty(), position, *this));
        return *at(position);
    }

    size_t add_row();
    ByteString add_column();

    size_t row_count() const { return m_rows; }
    size_t column_count() const { return m_columns.size(); }
    Vector<ByteString> const& columns() const { return m_columns; }
    ByteString const& column(size_t index)
    {
        for (size_t i = column_count(); i < index; ++i)
            add_column();

        VERIFY(column_count() > index);
        return m_columns[index];
    }
    ByteString const& column(size_t index) const
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

    JS::ThrowCompletionOr<JS::Value> evaluate(StringView, Cell* = nullptr);
    SheetGlobalObject& global_object() const { return *m_global_object; }

    Cell*& current_evaluated_cell() { return m_current_cell_being_evaluated; }
    bool has_been_visited(Cell* cell) const { return m_visited_cells_in_update.contains(cell); }

    Workbook const& workbook() const { return m_workbook; }

    enum class CopyOperation {
        Copy,
        Cut
    };

    Vector<CellChange> copy_cells(Vector<Position> from, Vector<Position> to, Optional<Position> resolve_relative_to = {}, CopyOperation copy_operation = CopyOperation::Copy);

    /// Gives the bottom-right corner of the smallest bounding box containing all the written data, optionally limited to the given column.
    Position written_data_bounds(Optional<size_t> column_index = {}) const;

    bool columns_are_standard() const;

    ByteString generate_inline_documentation_for(StringView function, size_t argument_index);

    JS::Realm& realm() const { return *m_root_execution_context->realm; }
    JS::VM& vm() const { return realm().vm(); }

private:
    explicit Sheet(Workbook&);
    explicit Sheet(StringView name, Workbook&);

    ByteString m_name;
    Vector<ByteString> m_columns;
    size_t m_rows { 0 };
    HashMap<Position, NonnullOwnPtr<Cell>> m_cells;
    HashTable<Position> m_selected_cells;

    Workbook& m_workbook;
    mutable JS::GCPtr<SheetGlobalObject> m_global_object;

    NonnullRefPtr<JS::VM> m_vm;
    NonnullOwnPtr<JS::ExecutionContext> m_root_execution_context;

    Cell* m_current_cell_being_evaluated { nullptr };

    HashTable<Cell*> m_visited_cells_in_update;
    bool m_should_ignore_updates { false };
    bool m_update_requested { false };
    mutable Optional<JsonObject> m_cached_documentation;
};

}

namespace AK {

template<>
struct Traits<Spreadsheet::Position> : public DefaultTraits<Spreadsheet::Position> {
    static constexpr bool is_trivial() { return false; }
    static unsigned hash(Spreadsheet::Position const& p)
    {
        return p.hash();
    }
};

}
