/*
 * Copyright (c) 2022, Mart G <martg_@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace WindowServer {

enum class ResizeDirection {
    None,
    Left,
    UpLeft,
    Up,
    UpRight,
    Right,
    DownRight,
    Down,
    DownLeft,
    __Count
};

}
