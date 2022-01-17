/*
 * Copyright (c) 2022, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/SQLResult.h>

namespace SQL {

void SQLResult::insert(Tuple const& row, Tuple const& sort_key)
{
    m_has_results = true;
    m_result_set.insert_row(row, sort_key);
}

void SQLResult::limit(size_t offset, size_t limit)
{
    if (offset > 0) {
        if (offset > m_result_set.size()) {
            m_result_set.clear();
            return;
        }
        m_result_set.remove(0, offset);
    }
    if (m_result_set.size() > limit) {
        m_result_set.remove(limit, m_result_set.size() - limit);
    }
}

}
