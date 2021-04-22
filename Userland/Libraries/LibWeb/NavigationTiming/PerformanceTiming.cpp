/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Window.h>
#include <LibWeb/NavigationTiming/PerformanceTiming.h>

namespace Web::NavigationTiming {

PerformanceTiming::PerformanceTiming(DOM::Window& window)
    : m_window(window)
{
}

PerformanceTiming::~PerformanceTiming()
{
}

void PerformanceTiming::ref()
{
    m_window.ref();
}

void PerformanceTiming::unref()
{
    m_window.unref();
}

}
