/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "CocoaWrapper.h"

NS_ASSUME_NONNULL_BEGIN

@class MacPDFDocument;

@interface MacPDFWindowController : NSWindowController

- (instancetype)initWithDocument:(MacPDFDocument*)document;

@end

NS_ASSUME_NONNULL_END
