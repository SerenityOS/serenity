/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/URL.h>

#import <System/Cocoa.h>

@class TabController;

@interface SourceViewController : NSWindowController <NSWindowDelegate>

- (instancetype)init:(TabController*)tab_controller
                 url:(URL)url
              source:(DeprecatedString)source;

@end
