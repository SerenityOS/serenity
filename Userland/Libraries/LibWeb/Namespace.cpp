/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Namespace.h>

namespace Web::Namespace {

#define __ENUMERATE_NAMESPACE(name, namespace_) DeprecatedFlyString name;
ENUMERATE_NAMESPACES
#undef __ENUMERATE_NAMESPACE

ErrorOr<void> initialize_strings()
{
    static bool s_initialized = false;
    VERIFY(!s_initialized);

#define __ENUMERATE_NAMESPACE(name, namespace_) \
    name = namespace_;
    ENUMERATE_NAMESPACES
#undef __ENUMERATE_NAMESPACE

    s_initialized = true;
    return {};
}

}
