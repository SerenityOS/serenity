/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/URL.h>

#import <System/Cocoa.h>

@class Tab;

@interface SourceView : NSWindow

- (instancetype)init:(Tab*)tab
                 url:(URL const&)url
              source:(StringView)source;

@end
