/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Vector.h>

namespace Web {
namespace HTML {

struct EntityMatch {
    Vector<u32, 2> code_points;
    StringView entity;
};

Optional<EntityMatch> code_points_from_entity(StringView);

}
}
