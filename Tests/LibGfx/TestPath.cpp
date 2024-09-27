/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <LibGfx/Path.h>

TEST_CASE(path_to_fill_short_wide_line_with_butt_linecap)
{

    // Test drawing a horizontal rect by stroking a vertical short wide line.
    {
        int width = 100;
        int height = 1;
        Gfx::Path path;
        path.move_to({ width / 2, 0 });
        path.line_to({ width / 2, height });
        auto fill = path.stroke_to_fill(width, Gfx::Path::CapStyle::Butt);

        // FIXME: This should be true.
        // EXPECT_EQ(fill.bounding_box(), Gfx::FloatRect({ 0, 0 }, { width, height }));
    }

    // Test drawing a vertical rect by stroking a horizontal short wide line.
    {
        int width = 1;
        int height = 100;
        Gfx::Path path;
        path.move_to({ 0, height / 2 });
        path.line_to({ width, height / 2 });
        auto fill = path.stroke_to_fill(height, Gfx::Path::CapStyle::Butt);

        // FIXME: This should be true.
        // EXPECT_EQ(fill.bounding_box(), Gfx::FloatRect({ 0, 0 }, { width, height }));
    }
}
