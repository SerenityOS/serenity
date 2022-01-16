/*
 * Copyright (c) 2022, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibSQL/Tuple.h>
#include <LibSQL/Type.h>

namespace SQL {

struct ResultRow {
    Tuple row;
    Tuple sort_key;
};

class ResultSet : public Vector<ResultRow> {
public:
    ResultSet() = default;
    void insert_row(Tuple const& row, Tuple const& sort_key);

private:
    size_t binary_search(Tuple const& sort_key, size_t low, size_t high);
};

}
