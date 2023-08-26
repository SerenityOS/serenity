/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibGfx/Forward.h>
#include <LibWeb/CSS/PreferredColorScheme.h>
#include <LibWeb/HTML/ActivateTab.h>

#import <System/Cocoa.h>

@protocol LadybirdWebViewObserver <NSObject>

- (String const&)onCreateNewTab:(URL const&)url
                    activateTab:(Web::HTML::ActivateTab)activate_tab;

- (String const&)onCreateNewTab:(StringView)html
                            url:(URL const&)url
                    activateTab:(Web::HTML::ActivateTab)activate_tab;

- (void)loadURL:(URL const&)url;
- (void)onLoadStart:(URL const&)url isRedirect:(BOOL)is_redirect;

- (void)onTitleChange:(DeprecatedString const&)title;
- (void)onFaviconChange:(Gfx::Bitmap const&)bitmap;

- (void)onNavigateBack;
- (void)onNavigateForward;
- (void)onReload;

@end

@interface LadybirdWebView : NSClipView

- (instancetype)init:(id<LadybirdWebViewObserver>)observer;

- (void)loadURL:(URL const&)url;
- (void)loadHTML:(StringView)html url:(URL const&)url;

- (String const&)handle;

- (void)handleResize;
- (void)handleScroll;
- (void)handleVisibility:(BOOL)is_visible;

- (void)setPreferredColorScheme:(Web::CSS::PreferredColorScheme)color_scheme;

- (void)viewSource;

@end
