/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "CellType/Type.h"
#include "ConditionalFormatting.h"
#include "Forward.h"
#include <LibGUI/Dialog.h>

namespace Spreadsheet {

class CellTypeDialog : public GUI::Dialog {
    C_OBJECT(CellTypeDialog);

public:
    CellTypeMetadata metadata() const;
    CellType const* type() const { return m_type; }
    Vector<ConditionalFormat> conditional_formats() { return m_conditional_formats; }

    enum class HorizontalAlignment : int {
        Left = 0,
        Center,
        Right,
    };
    enum class VerticalAlignment : int {
        Top = 0,
        Center,
        Bottom,
    };

private:
    CellTypeDialog(Vector<Position> const&, Sheet&, GUI::Window* parent = nullptr);
    void setup_tabs(GUI::TabWidget&, Vector<Position> const&, Sheet&);

    CellType const* m_type { nullptr };

    int m_length { -1 };
    ByteString m_format;
    HorizontalAlignment m_horizontal_alignment { HorizontalAlignment::Right };
    VerticalAlignment m_vertical_alignment { VerticalAlignment::Center };
    Format m_static_format;
    Vector<ConditionalFormat> m_conditional_formats;
};

}
