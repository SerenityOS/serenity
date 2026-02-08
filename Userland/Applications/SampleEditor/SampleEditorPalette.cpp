/*
 * Copyright (c) 2025, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SampleEditorPalette.h"

SampleEditorPalette::SampleEditorPalette(Gfx::Palette theme)
{
    black = theme.black();
    light_blue = theme.blue().lightened(1.1);
    light_gray = light_blue.to_grayscale();
    dark_blue = theme.blue().darkened(0.9);
    dark_gray = dark_blue.to_grayscale();
    window_color = theme.window();
    selection_color = window_color.to_grayscale().lightened();
    cursor_color = theme.red();
    timeline_selection_color = theme.white().lightened();
    timeline_selection_color.set_alpha(128);
    timeline_cursor_color = theme.red().lightened();
    timeline_cursor_color.set_alpha(128);
    timeline_background_color = theme.white();
    timeline_main_mark_color = theme.black();
    timeline_sub_mark_color = timeline_main_mark_color;
    timeline_sub_mark_color.set_alpha(96);
}
