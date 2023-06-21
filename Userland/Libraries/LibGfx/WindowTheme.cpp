/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/ClassicWindowTheme.h>
#include <LibGfx/WindowTheme.h>

namespace Gfx {

WindowTheme& WindowTheme::current()
{
    static ClassicWindowTheme theme;
    return theme;
}

}
