/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventLoop.h>

#import <Application/Application.h>

#if !__has_feature(objc_arc)
#    error "This project requires ARC"
#endif

@interface Application ()
@end

@implementation Application

- (void)terminate:(id)sender
{
    Core::EventLoop::current().quit(0);
}

@end
