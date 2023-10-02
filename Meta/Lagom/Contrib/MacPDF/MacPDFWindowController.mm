/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#import "MacPDFWindowController.h"

@implementation MacPDFWindowController

- (instancetype)initWithDocument:(MacPDFDocument*)document
{
    if (self = [super initWithWindowNibName:@"MacPDFDocument" owner:self]; !self)
        return nil;

    return self;
}


@end
