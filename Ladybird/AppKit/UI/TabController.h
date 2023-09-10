/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/URL.h>

#import <System/Cocoa.h>

struct TabSettings {
    BOOL should_show_line_box_borders { NO };
    BOOL scripting_enabled { YES };
    BOOL block_popups { YES };
    BOOL same_origin_policy_enabled { NO };
    DeprecatedString user_agent_name { "Disabled"sv };
};

@interface TabController : NSWindowController <NSWindowDelegate>

- (instancetype)init;

- (void)loadURL:(URL const&)url;
- (void)loadHTML:(StringView)html url:(URL const&)url;

- (void)onLoadStart:(URL const&)url isRedirect:(BOOL)isRedirect;
- (void)onTitleChange:(DeprecatedString const&)title;

- (void)navigateBack:(id)sender;
- (void)navigateForward:(id)sender;
- (void)reload:(id)sender;
- (void)clearHistory;

- (void)debugRequest:(DeprecatedString const&)request argument:(DeprecatedString const&)argument;

- (void)focusLocationToolbarItem;

@end
