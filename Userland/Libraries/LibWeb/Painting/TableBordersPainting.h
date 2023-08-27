/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Painting/PaintContext.h>

namespace Web::Painting {

void paint_table_borders(PaintContext&, PaintableBox const&);
void paint_table_box_border(PaintContext&, PaintableBox const&);

}
