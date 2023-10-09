/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "CocoaWrapper.h"

#include <LibPDF/Document.h>

NS_ASSUME_NONNULL_BEGIN

// Objective-C wrapper of PDF::OutlineItem, to launder it through the NSOutlineViewDataSource protocol.
@interface OutlineItemWrapper : NSObject

- (BOOL)isGroupItem;
- (Optional<u32>)page;

@end

@interface MacPDFOutlineViewDataSource : NSObject <NSOutlineViewDataSource>

- (instancetype)initWithOutline:(RefPtr<PDF::OutlineDict>)outline;

@end

NS_ASSUME_NONNULL_END
