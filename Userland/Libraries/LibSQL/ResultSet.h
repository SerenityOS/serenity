/*
 * Copyright (c) 2022, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibSQL/Result.h>
#include <LibSQL/Tuple.h>
#include <LibSQL/Type.h>

namespace SQL {

struct ResultRow {
    Tuple row;
    Tuple sort_key;
};

class ResultSet : public Vector<ResultRow> {
public:
    ALWAYS_INLINE ResultSet(SQLCommand command)
        : m_command(command)
    {
    }

    ALWAYS_INLINE ResultSet(SQLCommand command, Vector<ByteString> column_names)
        : m_command(command)
        , m_column_names(move(column_names))
    {
    }

    SQLCommand command() const { return m_command; }
    Vector<ByteString> const& column_names() const { return m_column_names; }

    void insert_row(Tuple const& row, Tuple const& sort_key);
    void limit(size_t offset, size_t limit);

private:
    size_t binary_search(Tuple const& sort_key, size_t low, size_t high);

    SQLCommand m_command { SQLCommand::Unknown };
    Vector<ByteString> m_column_names;
};

}
