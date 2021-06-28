/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/String.h>

namespace SQL {

#define ENUMERATE_SQL_TYPES(S)                   \
    S("text", 0, Text, String, 64 + sizeof(int)) \
    S("int", 1, Integer, int, sizeof(int))       \
    S("float", 2, Float, double, sizeof(double))

enum class SQLType {
#undef __ENUMERATE_SQL_TYPE
#define __ENUMERATE_SQL_TYPE(name, cardinal, type, impl, size) type = (cardinal),
    ENUMERATE_SQL_TYPES(__ENUMERATE_SQL_TYPE)
#undef __ENUMERATE_SQL_TYPE
};

inline static size_t size_of(SQLType t)
{
    switch (t) {
#undef __ENUMERATE_SQL_TYPE
#define __ENUMERATE_SQL_TYPE(name, cardinal, type, impl, size) \
    case SQLType::type:                                        \
        return size;
        ENUMERATE_SQL_TYPES(__ENUMERATE_SQL_TYPE)
#undef __ENUMERATE_SQL_TYPE
    default:
        VERIFY_NOT_REACHED();
    }
}

enum class Order {
    Ascending,
    Descending,
};

enum class Nulls {
    First,
    Last,
};

}
