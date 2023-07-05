/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Painting/PaintContext.h>

namespace Web::Painting {

void paint_table_collapsed_borders(PaintContext&, Layout::Node const&);

}
