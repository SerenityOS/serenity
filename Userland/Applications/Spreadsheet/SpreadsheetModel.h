/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Spreadsheet.h"
#include <LibGUI/Model.h>

namespace Spreadsheet {

class SheetModel final : public GUI::Model {
public:
    static NonnullRefPtr<SheetModel> create(Sheet& sheet) { return adopt_ref(*new SheetModel(sheet)); }
    virtual ~SheetModel() override;

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return m_sheet->row_count(); }
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return m_sheet->column_count(); }
    virtual String column_name(int) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;
    virtual RefPtr<Core::MimeData> mime_data(const GUI::ModelSelection&) const override;
    virtual bool is_editable(const GUI::ModelIndex&) const override;
    virtual void set_data(const GUI::ModelIndex&, const GUI::Variant&) override;
    virtual bool is_column_sortable(int) const override { return false; }
    virtual StringView drag_data_type() const override { return "text/x-spreadsheet-data"; }
    Sheet& sheet() { return *m_sheet; }

    void update();

private:
    explicit SheetModel(Sheet& sheet)
        : m_sheet(sheet)
    {
    }

    NonnullRefPtr<Sheet> m_sheet;
};

}
