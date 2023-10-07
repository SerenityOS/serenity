/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "CocoaWrapper.h"

#import "MacPDFView.h"
#include <LibPDF/Document.h>

NS_ASSUME_NONNULL_BEGIN

@class MacPDFDocument;

@interface MacPDFWindowController : NSWindowController <MacPDFViewDelegate, NSToolbarDelegate>

- (instancetype)initWithDocument:(MacPDFDocument*)document;
- (IBAction)showGoToPageDialog:(id)sender;
- (void)pdfDidInitialize;

@end

NS_ASSUME_NONNULL_END
