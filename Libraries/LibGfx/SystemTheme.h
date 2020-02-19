/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Types.h>
#include <LibGfx/Color.h>

namespace Gfx {

enum class ColorRole {
    NoRole,
    DesktopBackground,
    ActiveWindowBorder1,
    ActiveWindowBorder2,
    ActiveWindowTitle,
    InactiveWindowBorder1,
    InactiveWindowBorder2,
    InactiveWindowTitle,
    MovingWindowBorder1,
    MovingWindowBorder2,
    MovingWindowTitle,
    HighlightWindowBorder1,
    HighlightWindowBorder2,
    HighlightWindowTitle,
    MenuStripe,
    MenuBase,
    MenuBaseText,
    MenuSelection,
    MenuSelectionText,
    Window,
    WindowText,
    Button,
    ButtonText,
    Base,
    BaseText,
    ThreedHighlight,
    ThreedShadow1,
    ThreedShadow2,
    HoverHighlight,
    Selection,
    SelectionText,
    InactiveSelection,
    InactiveSelectionText,
    RubberBandFill,
    RubberBandBorder,
    Link,
    ActiveLink,
    VisitedLink,
    Ruler,
    RulerBorder,
    RulerActiveText,
    RulerInactiveText,

    __Count,

    Background = Window,
    DisabledText = ThreedShadow1,
};

struct SystemTheme {
    Color color[(int)ColorRole::__Count];
};

const SystemTheme& current_system_theme();
int current_system_theme_buffer_id();
void set_system_theme(SharedBuffer&);
RefPtr<SharedBuffer> load_system_theme(const String& path);

}

using Gfx::ColorRole;
