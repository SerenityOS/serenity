/*
 * Copyright (c) 2021-2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/Vector.h>

namespace JS {

// 2.9 ModuleRequest Records, https://tc39.es/proposal-import-assertions/#sec-modulerequest-record
struct ModuleRequest {
    struct Assertion {
        DeprecatedString key;
        DeprecatedString value;
    };

    ModuleRequest() = default;

    explicit ModuleRequest(FlyString specifier)
        : module_specifier(move(specifier))
    {
    }

    ModuleRequest(FlyString module_specifier, Vector<Assertion> assertions);

    void add_assertion(DeprecatedString key, DeprecatedString value)
    {
        assertions.empend(move(key), move(value));
    }

    FlyString module_specifier;   // [[Specifier]]
    Vector<Assertion> assertions; // [[Assertions]]
};

}
