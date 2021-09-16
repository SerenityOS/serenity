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
    S("null", 1, Null, int, sizeof(int))         \
    S("text", 2, Text, String, 65 + sizeof(u32)) \
    S("int", 4, Integer, int, sizeof(int))       \
    S("float", 8, Float, double, sizeof(double)) \
    S("bool", 16, Boolean, bool, sizeof(bool))   \
    S("tuple", 32, Tuple, int, sizeof(int))      \
    S("array", 64, Array, int, sizeof(int))

enum class SQLType {
#undef __ENUMERATE_SQL_TYPE
#define __ENUMERATE_SQL_TYPE(name, cardinal, type, impl, size) type = cardinal,
    ENUMERATE_SQL_TYPES(__ENUMERATE_SQL_TYPE)
#undef __ENUMERATE_SQL_TYPE
};

inline static String SQLType_name(SQLType t)
{
    switch (t) {
#undef __ENUMERATE_SQL_TYPE
#define __ENUMERATE_SQL_TYPE(name, cardinal, type, impl, size) \
    case SQLType::type:                                        \
        return name;
        ENUMERATE_SQL_TYPES(__ENUMERATE_SQL_TYPE)
#undef __ENUMERATE_SQL_TYPE
    default:
        VERIFY_NOT_REACHED();
    }
}

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

#define ENUMERATE_ORDERS(S) \
    S(Ascending)            \
    S(Descending)

enum class Order {
#undef __ENUMERATE_ORDER
#define __ENUMERATE_ORDER(order) order,
    ENUMERATE_ORDERS(__ENUMERATE_ORDER)
#undef __ENUMERATE_ORDER
};

inline static String Order_name(Order order)
{
    switch (order) {
#undef __ENUMERATE_ORDER
#define __ENUMERATE_ORDER(order) \
    case Order::order:           \
        return #order;
        ENUMERATE_ORDERS(__ENUMERATE_ORDER)
#undef __ENUMERATE_ORDER
    default:
        VERIFY_NOT_REACHED();
    }
}

enum class Nulls {
    First,
    Last,
};

}
