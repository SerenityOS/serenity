/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#import <System/Cocoa.h>

@class LadybirdWebView;
@class Tab;

@interface Console : NSWindow

- (instancetype)init:(Tab*)tab;

- (void)reset;

@property (nonatomic, strong) LadybirdWebView* web_view;

@end
