/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>

namespace Web::DOM::MutationType {

#define ENUMERATE_MUTATION_TYPES             \
    __ENUMERATE_MUTATION_TYPE(attributes)    \
    __ENUMERATE_MUTATION_TYPE(characterData) \
    __ENUMERATE_MUTATION_TYPE(childList)

#define __ENUMERATE_MUTATION_TYPE(name) extern DeprecatedFlyString name;
ENUMERATE_MUTATION_TYPES
#undef __ENUMERATE_MUTATION_TYPE

}
