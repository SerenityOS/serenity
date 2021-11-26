/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Namespace.h>

namespace Web::Namespace {

#define __ENUMERATE_NAMESPACE(name, namespace_) FlyString name;
ENUMERATE_NAMESPACES
#undef __ENUMERATE_NAMESPACE

[[gnu::constructor]] static void initialize()
{
    static bool s_initialized = false;
    if (s_initialized)
        return;

#define __ENUMERATE_NAMESPACE(name, namespace_) \
    name = namespace_;
    ENUMERATE_NAMESPACES
#undef __ENUMERATE_NAMESPACE

    s_initialized = true;
}

}
