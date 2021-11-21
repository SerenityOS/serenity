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

    Result<bool, String> save(StringView filename);
    Result<bool, String> load(StringView filename);

    const String& current_filename() const { return m_current_filename; }
    bool set_filename(const String& filename);
    bool dirty() { return m_dirty; }
    void set_dirty(bool dirty) { m_dirty = dirty; }

    bool has_sheets() const { return !m_sheets.is_empty(); }

    const NonnullRefPtrVector<Sheet>& sheets() const { return m_sheets; }
    NonnullRefPtrVector<Sheet> sheets() { return m_sheets; }

    Sheet& add_sheet(StringView name)
    {
        auto sheet = Sheet::construct(name, *this);
        m_sheets.append(sheet);
        return *sheet;
    }

    WorkbookObject* workbook_object() { return m_workbook_object; }
    JS::VM& vm() { return *m_vm; }
    JS::VM const& vm() const { return *m_vm; }

private:
    NonnullRefPtrVector<Sheet> m_sheets;
    NonnullRefPtr<JS::VM> m_vm;
    NonnullOwnPtr<JS::Interpreter> m_interpreter;
    JS::VM::InterpreterExecutionScope m_interpreter_scope;
    WorkbookObject* m_workbook_object { nullptr };
    JS::ExecutionContext m_main_execution_context;

    String m_current_filename;
    bool m_dirty { false };
};

}
