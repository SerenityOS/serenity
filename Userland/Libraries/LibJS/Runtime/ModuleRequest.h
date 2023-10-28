/*
 * Copyright (c) 2021-2022, David Tuin <davidot@serenityos.org>
 * Copyright (c) 2023, networkException <networkexception@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <AK/Vector.h>
#include <LibJS/Module.h>

namespace JS {

struct ModuleWithSpecifier {
    DeprecatedString specifier;  // [[Specifier]]
    NonnullGCPtr<Module> module; // [[Module]]
};

// 2.9 ModuleRequest Records, https://tc39.es/proposal-import-assertions/#sec-modulerequest-record
struct ModuleRequest {
    struct Assertion {
        DeprecatedString key;
        DeprecatedString value;
    };

    ModuleRequest() = default;

    explicit ModuleRequest(DeprecatedFlyString specifier)
        : module_specifier(move(specifier))
    {
    }

    ModuleRequest(DeprecatedFlyString module_specifier, Vector<Assertion> assertions);

    void add_assertion(DeprecatedString key, DeprecatedString value)
    {
        assertions.empend(move(key), move(value));
    }

    DeprecatedFlyString module_specifier; // [[Specifier]]
    Vector<Assertion> assertions;         // [[Assertions]]
};

}
