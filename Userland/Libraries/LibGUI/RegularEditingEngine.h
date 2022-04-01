/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/EditingEngine.h>

namespace GUI {

class RegularEditingEngine final : public EditingEngine {

public:
    virtual CursorWidth cursor_width() const override;

    virtual bool on_key(KeyEvent const& event) override;

private:
    void sort_selected_lines();
    virtual EngineType engine_type() const override { return EngineType::Regular; }
};

}
