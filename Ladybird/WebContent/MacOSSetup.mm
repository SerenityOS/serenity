/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MacOSSetup.h"
#import <AppKit/NSApplication.h>

void prohibit_interaction()
{
    // This prevents WebContent from being displayed in the macOS Dock and becoming the focused,
    // interactable application upon launch.
    [NSApp setActivationPolicy:NSApplicationActivationPolicyProhibited];
}
