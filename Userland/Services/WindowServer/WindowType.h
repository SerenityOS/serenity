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
    Popup,
    Autocomplete,
    _Count
};

}
