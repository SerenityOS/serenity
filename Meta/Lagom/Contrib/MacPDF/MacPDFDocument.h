/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "CocoaWrapper.h"

#import "MacPDFWindowController.h"

NS_ASSUME_NONNULL_BEGIN

@interface MacPDFDocument : NSDocument

- (PDF::Document*)pdf;
- (void)windowIsReady;

@end

NS_ASSUME_NONNULL_END
