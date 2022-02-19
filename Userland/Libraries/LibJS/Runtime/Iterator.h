/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

// 7.4.1 Iterator Records, https://tc39.es/ecma262/#sec-iterator-records
struct Iterator {
    Object* iterator { nullptr }; // [[Iterator]]
    Value next_method;            // [[NextMethod]]
    bool done { false };          // [[Done]]
};

}
