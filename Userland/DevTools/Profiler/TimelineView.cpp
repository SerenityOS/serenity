/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TimelineView.h"
#include <LibGUI/BoxLayout.h>

namespace Profiler {

TimelineView::TimelineView()
{
    set_layout<GUI::VerticalBoxLayout>();
    set_shrink_to_fit(true);
}

TimelineView::~TimelineView()
{
}

}
