/*
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>

namespace Web::NavigationTiming::EntryNames {

#define ENUMERATE_NAVIGATION_TIMING_ENTRY_NAMES                          \
    __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(navigationStart)            \
    __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(unloadEventStart)           \
    __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(unloadEventEnd)             \
    __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(redirectStart)              \
    __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(redirectEnd)                \
    __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(fetchStart)                 \
    __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(domainLookupStart)          \
    __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(domainLookupEnd)            \
    __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(connectStart)               \
    __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(connectEnd)                 \
    __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(secureConnectionStart)      \
    __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(requestStart)               \
    __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(responseStart)              \
    __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(responseEnd)                \
    __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(domLoading)                 \
    __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(domInteractive)             \
    __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(domContentLoadedEventStart) \
    __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(domContentLoadedEventEnd)   \
    __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(domComplete)                \
    __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(loadEventStart)             \
    __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(loadEventEnd)

#define __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(name) extern FlyString name;
ENUMERATE_NAVIGATION_TIMING_ENTRY_NAMES
#undef __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME

ErrorOr<void> initialize_strings();

}
