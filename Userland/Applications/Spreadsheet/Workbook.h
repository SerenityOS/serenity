/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Forward.h"
#include "Spreadsheet.h"
#include <YAK/NonnullOwnPtrVector.h>
#include <YAK/Result.h>

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
