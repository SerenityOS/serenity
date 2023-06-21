/*
 * Copyright (c) 2022, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/ResultSet.h>

namespace SQL {

size_t ResultSet::binary_search(Tuple const& sort_key, size_t low, size_t high)
{
    if (high <= low) {
        auto compare = sort_key.compare(at(low).sort_key);
        return (compare > 0) ? low + 1 : low;
    }

    auto mid = (low + high) / 2;
    auto compare = sort_key.compare(at(mid).sort_key);
    if (compare == 0)
        return mid + 1;

    if (compare > 0)
        return binary_search(sort_key, mid + 1, high);
    return binary_search(sort_key, low, mid);
}

void ResultSet::insert_row(Tuple const& row, Tuple const& sort_key)
{
    if ((sort_key.size() == 0) || is_empty()) {
        empend(row, sort_key);
        return;
    }
    auto ix = binary_search(sort_key, 0, size() - 1);
    insert(ix, ResultRow { row, sort_key });
}

void ResultSet::limit(size_t offset, size_t limit)
{
    if (offset > 0) {
        if (offset > size()) {
            clear();
            return;
        }

        remove(0, offset);
    }

    if (size() > limit)
        remove(limit, size() - limit);
}

}
