/*
 * Copyright (c) 2022, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibSQL/Result.h>

namespace SQL {

void Result::insert(Tuple const& row, Tuple const& sort_key)
{
    if (!m_result_set.has_value())
        m_result_set = ResultSet {};
    m_result_set->insert_row(row, sort_key);
}

void Result::limit(size_t offset, size_t limit)
{
    VERIFY(has_results());

    if (offset > 0) {
        if (offset > m_result_set->size()) {
            m_result_set->clear();
            return;
        }

        m_result_set->remove(0, offset);
    }

    if (m_result_set->size() > limit)
        m_result_set->remove(limit, m_result_set->size() - limit);
}

String Result::error_string() const
{
    VERIFY(is_error());

    StringView error_code;
    StringView error_description;

    switch (m_error) {
#undef __ENUMERATE_SQL_ERROR
#define __ENUMERATE_SQL_ERROR(error, description) \
    case SQLErrorCode::error:                     \
        error_code = #error##sv;                  \
        error_description = description##sv;      \
        break;
        ENUMERATE_SQL_ERRORS(__ENUMERATE_SQL_ERROR)
#undef __ENUMERATE_SQL_ERROR
    default:
        VERIFY_NOT_REACHED();
    }

    StringBuilder builder;
    builder.appendff("{}: ", error_code);

    if (m_error_message.has_value()) {
        if (error_description.find("{}"sv).has_value())
            builder.appendff(error_description, *m_error_message);
        else
            builder.appendff("{}: {}", error_description, *m_error_message);
    } else {
        builder.append(error_description);
    }

    return builder.build();
}

}
