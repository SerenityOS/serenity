/*
 * Copyright (c) 2025, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibVT/TerminalWidget.h>

namespace Terminal::Defaults {

static constexpr VT::TerminalWidget::BellMode BELL_MODE = VT::TerminalWidget::BellMode::Visible;
static constexpr VT::TerminalWidget::AutoMarkMode AUTOMARK_MODE = VT::TerminalWidget::AutoMarkMode::MarkInteractiveShellPrompt;
static constexpr bool CONFIRM_CLOSE = true;

static constexpr int OPACITY = 255;
static constexpr VT::CursorShape CURSOR_SHAPE = VT::CursorShape::Block;
static constexpr bool CURSOR_BLINKING = true;
static constexpr int MAX_HISTORY_SIZE = 1024;
static constexpr bool SHOW_SCROLLBAR = true;

}
