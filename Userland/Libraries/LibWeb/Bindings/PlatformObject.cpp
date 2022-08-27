/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::Bindings {

PlatformObject::PlatformObject(JS::Object& prototype)
    : JS::Object(prototype)
{
}

PlatformObject::~PlatformObject() = default;

}
