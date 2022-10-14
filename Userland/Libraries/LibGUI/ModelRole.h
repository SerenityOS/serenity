/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace GUI {

enum class ModelRole {
    Display,
    Sort,
    ForegroundColor,
    BackgroundColor,
    Icon,
    IconOpacity,
    Font,
    MimeData,
    TextAlignment,
    Search,
    Custom = 0x100, // Applications are free to use roles above this number as they please
};

}
