/*
 * Copyright (c) 2026, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventLoop.h>

#include "CocoaWrapper.h"

#include "EventLoopImplementation.h"

int main()
{
    Core::EventLoopManager::install(*new Mac::CFEventLoopManager);
    Core::EventLoop event_loop;

    @autoreleasepool {
        NSArray* top_level_objects;
        [[NSBundle mainBundle] loadNibNamed:@"MainMenu" owner:[NSApplication sharedApplication] topLevelObjects:&top_level_objects];
        [NSApp finishLaunching];
    }

    return event_loop.exec();
}
