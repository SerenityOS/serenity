/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibJS/Forward.h>

namespace JS {

struct EnvironmentCoordinate {
    size_t hops { 0 };
    size_t index { 0 };
};

}
