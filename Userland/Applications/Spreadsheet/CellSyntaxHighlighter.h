/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Cell.h"
#include <LibJS/SyntaxHighlighter.h>

namespace Spreadsheet {

class CellSyntaxHighlighter final : public JS::SyntaxHighlighter {
public:
    CellSyntaxHighlighter() { }
    virtual ~CellSyntaxHighlighter() override;

    virtual void rehighlight(const Palette&) override;
    void set_cell(const Cell* cell) { m_cell = cell; }

private:
    const Cell* m_cell { nullptr };
};

}
