/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// Several AK types conflict with MacOS types.
#define FixedPoint FixedPointMacOS
#import <Cocoa/Cocoa.h>
#undef FixedPoint

#include <AK/WeakPtr.h>
#include <LibPDF/Document.h>

@protocol LagomPDFViewDelegate
- (void)pageChanged;
@end

@interface LagomPDFView : NSView

- (void)setDocument:(WeakPtr<PDF::Document>)doc;
- (void)goToPage:(int)page;
- (int)page;

- (void)setDelegate:(id<LagomPDFViewDelegate>)delegate;

@end
