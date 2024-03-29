/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibGfx/Forward.h>
#include <LibURL/Forward.h>
#include <LibWeb/CSS/PreferredColorScheme.h>
#include <LibWeb/HTML/ActivateTab.h>
#include <LibWeb/HTML/AudioPlayState.h>
#include <LibWeb/HTML/HistoryHandlingBehavior.h>
#include <LibWebView/Forward.h>

#import <System/Cocoa.h>

@protocol LadybirdWebViewObserver <NSObject>

- (String const&)onCreateNewTab:(URL::URL const&)url
                    activateTab:(Web::HTML::ActivateTab)activate_tab;

- (String const&)onCreateNewTab:(StringView)html
                            url:(URL::URL const&)url
                    activateTab:(Web::HTML::ActivateTab)activate_tab;

- (void)loadURL:(URL::URL const&)url;
- (void)onLoadStart:(URL::URL const&)url isRedirect:(BOOL)is_redirect;
- (void)onLoadFinish:(URL::URL const&)url;
- (void)onURLUpdated:(URL::URL const&)url
     historyBehavior:(Web::HTML::HistoryHandlingBehavior)history_behavior;

- (void)onTitleChange:(ByteString const&)title;
- (void)onFaviconChange:(Gfx::Bitmap const&)bitmap;
- (void)onAudioPlayStateChange:(Web::HTML::AudioPlayState)play_state;

- (void)onNavigateBack;
- (void)onNavigateForward;
- (void)onReload;

@end

@interface LadybirdWebView : NSClipView <NSMenuDelegate>

- (instancetype)init:(id<LadybirdWebViewObserver>)observer;

- (void)loadURL:(URL::URL const&)url;
- (void)loadHTML:(StringView)html;

- (WebView::ViewImplementation&)view;
- (String const&)handle;

- (void)handleResize;
- (void)handleDevicePixelRatioChange;
- (void)handleScroll;
- (void)handleVisibility:(BOOL)is_visible;

- (void)setPreferredColorScheme:(Web::CSS::PreferredColorScheme)color_scheme;

- (void)zoomIn;
- (void)zoomOut;
- (void)resetZoom;
- (float)zoomLevel;

- (void)debugRequest:(ByteString const&)request argument:(ByteString const&)argument;

- (void)viewSource;

@end
