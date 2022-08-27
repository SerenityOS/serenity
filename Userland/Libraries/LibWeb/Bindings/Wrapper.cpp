/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Wrapper.h>

namespace Web::Bindings {

Wrapper::Wrapper(Object& prototype)
    : PlatformObject(prototype)
{
}

Wrapper::~Wrapper() = default;

}
