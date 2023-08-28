/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibWeb/CSS/PreferredColorScheme.h>

#import <System/Cocoa.h>

@interface LadybirdWebView : NSClipView

- (void)loadURL:(URL const&)url;
- (void)loadHTML:(StringView)html url:(URL const&)url;

- (void)handleResize;
- (void)handleScroll;
- (void)handleVisibility:(BOOL)is_visible;

- (void)setPreferredColorScheme:(Web::CSS::PreferredColorScheme)color_scheme;

@end
