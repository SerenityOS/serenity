/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include "Spreadsheet.h"
#include <LibGUI/Model.h>

namespace Spreadsheet {

class SheetModel final : public GUI::Model {
public:
    static NonnullRefPtr<SheetModel> create(Sheet& sheet) { return adopt(*new SheetModel(sheet)); }
    virtual ~SheetModel() override;

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return m_sheet->row_count(); }
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return m_sheet->column_count(); }
    virtual String column_name(int) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;
    virtual bool is_editable(const GUI::ModelIndex&) const override;
    virtual void set_data(const GUI::ModelIndex&, const GUI::Variant&) override;
    virtual void update() override;
    virtual bool is_column_sortable(int) const override { return false; }

private:
    explicit SheetModel(Sheet& sheet)
        : m_sheet(sheet)
    {
    }

    NonnullRefPtr<Sheet> m_sheet;
};

}
