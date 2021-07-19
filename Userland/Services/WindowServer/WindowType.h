/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace WindowServer {

enum class WindowType {
    Invalid = 0,
    Normal,
    Menu,
    WindowSwitcher,
    Taskbar,
    Tooltip,
    Applet,
    Notification,
    Desktop,
    AppletArea,
    _Count
};

enum class WindowStyle : u8 {
    Invalid = 0,
    Normal,
    ToolWindow,
    Fullscreen,
    Frameless,
    _Count
};

}
