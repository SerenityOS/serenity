/*
 * Copyright (c) 2024, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#import <System/Cocoa.h>

@interface SearchPanel : NSStackView

- (void)find:(id)selector;
- (void)findNextMatch:(id)selector;
- (void)findPreviousMatch:(id)selector;
- (void)useSelectionForFind:(id)selector;

@end
