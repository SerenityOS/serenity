/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/String.h>

namespace SQL {

#define ENUMERATE_SQL_TYPES(S)  \
    S("text", 0, Text, String)  \
    S("int", 1, Integer, int)   \
    S("float", 2, Float, double)

enum SQLType {
#undef __ENUMERATE_SQL_TYPE
#define __ENUMERATE_SQL_TYPE(name, cardinal, type, impl) type = (cardinal),
    ENUMERATE_SQL_TYPES(__ENUMERATE_SQL_TYPE)
#undef __ENUMERATE_SQL_TYPE
};

}
