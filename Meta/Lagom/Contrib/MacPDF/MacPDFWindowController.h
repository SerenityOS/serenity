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

@interface MacPDFWindowController : NSWindowController <MacPDFViewDelegate, NSOutlineViewDelegate, NSToolbarDelegate>

- (instancetype)initWithDocument:(MacPDFDocument*)document;

- (IBAction)goToNextPage:(id)sender;
- (IBAction)goToPreviousPage:(id)sender;
- (IBAction)toggleShowClippingPaths:(id)sender;
- (IBAction)toggleClipImages:(id)sender;
- (IBAction)toggleClipPaths:(id)sender;
- (IBAction)toggleClipText:(id)sender;
- (IBAction)toggleShowImages:(id)sender;
- (IBAction)toggleShowHiddenText:(id)sender;
- (IBAction)showGoToPageDialog:(id)sender;

- (void)pdfDidInitialize;

@end

NS_ASSUME_NONNULL_END
