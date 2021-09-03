/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Types.h>

namespace Gfx {

class Bitmap;

class Emoji {
public:
    static const Gfx::Bitmap* emoji_for_code_point(u32 code_point);
};

}
