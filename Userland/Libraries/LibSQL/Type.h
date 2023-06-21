/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/StringView.h>

namespace SQL {

// Adding to this list is fine, but changing the order of any value here will result in LibSQL
// becoming unable to read existing .db files. If the order must absolutely be changed, be sure
// to bump Heap::VERSION.
#define ENUMERATE_SQL_TYPES(S) \
    S("null", Null)            \
    S("text", Text)            \
    S("int", Integer)          \
    S("float", Float)          \
    S("bool", Boolean)         \
    S("tuple", Tuple)

enum class SQLType {
#undef __ENUMERATE_SQL_TYPE
#define __ENUMERATE_SQL_TYPE(name, type) type,
    ENUMERATE_SQL_TYPES(__ENUMERATE_SQL_TYPE)
#undef __ENUMERATE_SQL_TYPE
};

constexpr StringView SQLType_name(SQLType t)
{
    switch (t) {
#undef __ENUMERATE_SQL_TYPE
#define __ENUMERATE_SQL_TYPE(name, type) \
    case SQLType::type:                  \
        return name##sv;
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

constexpr StringView Order_name(Order order)
{
    switch (order) {
#undef __ENUMERATE_ORDER
#define __ENUMERATE_ORDER(order) \
    case Order::order:           \
        return #order##sv;
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

using ConnectionID = u64;
using StatementID = u64;
using ExecutionID = u64;

}
