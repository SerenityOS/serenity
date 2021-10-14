/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Forward.h"
#include "Spreadsheet.h"
#include <AK/NonnullOwnPtrVector.h>
#include <AK/Result.h>

namespace Spreadsheet {

class Workbook {
public:
    Workbook(NonnullRefPtrVector<Sheet>&& sheets);

    Result<bool, String> save(const StringView& filename);
    Result<bool, String> load(const StringView& filename);

    const String& current_filename() const { return m_current_filename; }
    bool set_filename(const String& filename);
    bool dirty() { return m_dirty; }
    void set_dirty(bool dirty) { m_dirty = dirty; }

    bool has_sheets() const { return !m_sheets.is_empty(); }

    const NonnullRefPtrVector<Sheet>& sheets() const { return m_sheets; }
    NonnullRefPtrVector<Sheet> sheets() { return m_sheets; }

    Sheet& add_sheet(const StringView& name)
    {
        auto sheet = Sheet::construct(name, *this);
        m_sheets.append(sheet);
        return *sheet;
    }

private:
    NonnullRefPtrVector<Sheet> m_sheets;

    String m_current_filename;
    bool m_dirty { false };
};

}
