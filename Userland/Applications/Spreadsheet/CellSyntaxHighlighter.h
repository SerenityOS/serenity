/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Cell.h"
#include <LibJS/SyntaxHighlighter.h>

namespace Spreadsheet {

class CellSyntaxHighlighter final : public JS::SyntaxHighlighter {
public:
    CellSyntaxHighlighter() = default;
    virtual ~CellSyntaxHighlighter() override = default;

    virtual void rehighlight(Palette const&) override;
    void set_cell(Cell const* cell) { m_cell = cell; }

private:
    Cell const* m_cell { nullptr };
};

}
