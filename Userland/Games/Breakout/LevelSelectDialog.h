/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Dialog.h>

namespace Breakout {
class LevelSelectDialog : public GUI::Dialog {
    C_OBJECT(LevelSelectDialog)
public:
    virtual ~LevelSelectDialog() override = default;
    static int show(int& board_number, Window* parent_window);
    int level() const { return m_level; }

private:
    explicit LevelSelectDialog(Window* parent_window);
    void build();

    int m_level;
};
}
