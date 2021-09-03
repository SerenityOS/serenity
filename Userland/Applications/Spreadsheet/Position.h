/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/String.h>
#include <YAK/Types.h>
#include <YAK/URL.h>

namespace Spreadsheet {

class Sheet;

struct Position {
    Position() = default;

    Position(size_t column, size_t row)
        : column(column)
        , row(row)
        , m_hash(pair_int_hash(column, row))
    {
    }

    ALWAYS_INLINE u32 hash() const
    {
        if (m_hash == 0)
            return m_hash = int_hash(column * 65537 + row);

        return m_hash;
    }

    bool operator==(const Position& other) const
    {
        return row == other.row && column == other.column;
    }

    bool operator!=(const Position& other) const
    {
        return !(other == *this);
    }

    String to_cell_identifier(const Sheet& sheet) const;
    URL to_url(const Sheet& sheet) const;

    size_t column { 0 };
    size_t row { 0 };

private:
    mutable u32 m_hash { 0 };
};

}
