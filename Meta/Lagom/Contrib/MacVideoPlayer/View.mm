/*
 * Copyright (c) 2026, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#import "View.h"

#include <LibMedia/PlaybackManager.h>

@interface View ()
{
    Media::PlaybackManager* _manager;
}
@end

@implementation View

- (void)setPlaybackManager:(Media::PlaybackManager*)manager
{
    _manager = manager;
}

- (void)drawRect:(NSRect)rect
{
    [[NSColor blackColor] setFill];
    NSRectFill(rect);
}

@end
