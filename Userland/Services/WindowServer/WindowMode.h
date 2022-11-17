/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace WindowServer {

// WindowMode sets modal behavior for windows in a modal chain
//
// - Modeless:      No modal effect (default mode for parentless windows)
// - Passive:       Joins the modal chain but has no modal effect (default mode for child windows)
// - RenderAbove:   Renders above its parent
// - CaptureInput:  Captures input from its parent
// - Blocking:      Preempts all interaction with its modal chain excepting descendants (default mode for Dialogs)
enum class WindowMode {
    Modeless = 0,
    Passive,
    RenderAbove,
    CaptureInput,
    Blocking,
    _Count,
};

}
