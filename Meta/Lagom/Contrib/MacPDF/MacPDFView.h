/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "CocoaWrapper.h"

#include <AK/WeakPtr.h>
#include <LibPDF/Document.h>

@protocol MacPDFViewDelegate
- (void)pageChanged;
@end

@interface MacPDFView : NSView

- (void)setDocument:(WeakPtr<PDF::Document>)doc;
- (void)goToPage:(int)page;
- (int)page;

- (void)setDelegate:(id<MacPDFViewDelegate>)delegate;

- (IBAction)goToNextPage:(id)sender;
- (IBAction)goToPreviousPage:(id)sender;

- (IBAction)toggleShowClippingPaths:(id)sender;
- (IBAction)toggleClipImages:(id)sender;
- (IBAction)toggleClipPaths:(id)sender;
- (IBAction)toggleClipText:(id)sender;
- (IBAction)toggleShowImages:(id)sender;
- (IBAction)toggleShowHiddenText:(id)sender;

@end
