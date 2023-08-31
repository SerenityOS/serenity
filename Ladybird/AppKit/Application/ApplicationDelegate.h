/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/StringView.h>
#include <AK/URL.h>
#include <LibWeb/CSS/PreferredColorScheme.h>
#include <LibWeb/HTML/ActivateTab.h>
#include <LibWebView/CookieJar.h>

#import <System/Cocoa.h>

@class Tab;
@class TabController;

@interface ApplicationDelegate : NSObject <NSApplicationDelegate>

- (nullable instancetype)init:(Optional<URL>)initial_url
                withCookieJar:(WebView::CookieJar)cookie_jar
      webdriverContentIPCPath:(StringView)webdriver_content_ipc_path;

- (nonnull TabController*)createNewTab:(Optional<URL> const&)url
                               fromTab:(nullable Tab*)tab
                           activateTab:(Web::HTML::ActivateTab)activate_tab;

- (nonnull TabController*)createNewTab:(StringView)html
                                   url:(URL const&)url
                               fromTab:(nullable Tab*)tab
                           activateTab:(Web::HTML::ActivateTab)activate_tab;

- (void)removeTab:(nonnull TabController*)controller;

- (WebView::CookieJar&)cookieJar;
- (Optional<StringView> const&)webdriverContentIPCPath;
- (Web::CSS::PreferredColorScheme)preferredColorScheme;

@end
