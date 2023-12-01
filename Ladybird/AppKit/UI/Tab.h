/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#import <System/Cocoa.h>

@class LadybirdWebView;

@interface Tab : NSWindow

- (void)tabWillClose;

- (void)openInspector:(id)sender;
- (void)onInspectorClosed;

@property (nonatomic, strong) LadybirdWebView* web_view;

@end
