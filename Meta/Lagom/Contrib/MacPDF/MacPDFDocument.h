/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "CocoaWrapper.h"

#import "MacPDFView.h"

NS_ASSUME_NONNULL_BEGIN

@interface MacPDFDocument : NSDocument <MacPDFViewDelegate>
{
    IBOutlet MacPDFView* _pdfView;
}

- (IBAction)showGoToPageDialog:(id)sender;

@end

NS_ASSUME_NONNULL_END
