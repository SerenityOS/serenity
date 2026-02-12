/*
 * Copyright (c) 2026, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "CocoaWrapper.h"

namespace Media {
class PlaybackManager;
}

@interface View : NSView

- (void)setPlaybackManager:(Media::PlaybackManager*)manager;

@end
