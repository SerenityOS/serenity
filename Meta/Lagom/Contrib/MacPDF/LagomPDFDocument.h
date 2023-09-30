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

#import "LagomPDFView.h"

NS_ASSUME_NONNULL_BEGIN

@interface LagomPDFDocument : NSDocument <LagomPDFViewDelegate>
{
    IBOutlet LagomPDFView* _pdfView;
}

- (IBAction)showGoToPageDialog:(id)sender;

@end

NS_ASSUME_NONNULL_END
