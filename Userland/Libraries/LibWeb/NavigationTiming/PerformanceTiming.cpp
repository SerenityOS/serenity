/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/NavigationTiming/PerformanceTiming.h>

namespace Web::NavigationTiming {

PerformanceTiming::PerformanceTiming(HTML::Window& window)
    : m_window(JS::make_handle(window))
{
}

PerformanceTiming::~PerformanceTiming() = default;

}
