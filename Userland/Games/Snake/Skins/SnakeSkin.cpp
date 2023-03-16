/*
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SnakeSkin.h"
#include "ClassicSkin.h"
#include "ImageSkin.h"
#include <AK/String.h>
#include <LibCore/DeprecatedFile.h>

namespace Snake {

ErrorOr<NonnullOwnPtr<SnakeSkin>> SnakeSkin::create(StringView skin_name, Color color)
{
    if (skin_name == "classic"sv)
        return try_make<ClassicSkin>(color);

    // Try to find an image-based skin matching the name.
    if (Core::DeprecatedFile::exists(TRY(String::formatted("/res/graphics/snake/skins/{}", skin_name))))
        return ImageSkin::create(skin_name);

    // Fall-back on classic
    return try_make<ClassicSkin>(color);
}

}
