/*
 * Copyright (c) 2025, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Color.h>
#include <LibGfx/Palette.h>

#pragma once

class SampleEditorPalette {

public:
    SampleEditorPalette(Gfx::Palette theme);

    Gfx::Color black;
    Gfx::Color light_blue;
    Gfx::Color dark_blue;
    Gfx::Color light_gray;
    Gfx::Color dark_gray;
    Gfx::Color window_color;
    Gfx::Color selection_color;
    Gfx::Color cursor_color;
    Gfx::Color timeline_selection_color;
    Gfx::Color timeline_cursor_color;
    Gfx::Color timeline_background_color;
    Gfx::Color timeline_main_mark_color;
    Gfx::Color timeline_sub_mark_color;
};
