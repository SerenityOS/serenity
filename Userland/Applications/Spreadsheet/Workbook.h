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

    Result<bool, String> save(StringView const& filename);
    Result<bool, String> load(StringView const& filename);

    String const& current_filename() const { return m_current_filename; }
    bool set_filename(String const& filename);
    bool dirty() { return m_dirty; }
    void set_dirty(bool dirty) { m_dirty = dirty; }

    bool has_sheets() const { return !m_sheets.is_empty(); }

    const NonnullRefPtrVector<Sheet>& sheets() const { return m_sheets; }
    NonnullRefPtrVector<Sheet> sheets() { return m_sheets; }

    Sheet& add_sheet(StringView const& name)
    {
        auto sheet = Sheet::construct(name, *this);
        m_sheets.append(sheet);
        return *sheet;
    }

    JS::Interpreter& interpreter() { return *m_interpreter; }
    const JS::Interpreter& interpreter() const { return *m_interpreter; }

    JS::GlobalObject& global_object() { return m_interpreter->global_object(); }
    const JS::GlobalObject& global_object() const { return m_interpreter->global_object(); }

    WorkbookObject* workbook_object() { return m_workbook_object; }

private:
    NonnullRefPtrVector<Sheet> m_sheets;
    NonnullOwnPtr<JS::Interpreter> m_interpreter;
    JS::VM::InterpreterExecutionScope m_interpreter_scope;
    WorkbookObject* m_workbook_object { nullptr };

    String m_current_filename;
    bool m_dirty { false };
};

}
