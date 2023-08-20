/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/URL.h>

#import <System/Cocoa.h>

@interface TabController : NSWindowController <NSWindowDelegate>

- (instancetype)init:(URL)url;

- (void)load:(URL const&)url;

- (void)onLoadStart:(URL const&)url isRedirect:(BOOL)isRedirect;
- (void)onTitleChange:(DeprecatedString const&)title;

- (void)navigateBack:(id)sender;
- (void)navigateForward:(id)sender;
- (void)reload:(id)sender;
- (void)clearHistory;

- (void)focusLocationToolbarItem;

@end
