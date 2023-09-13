/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#import <System/Cocoa.h>

@interface NSString (Ladybird)

- (NSString*)stringByCollapsingConsecutiveWhitespace;

@end
