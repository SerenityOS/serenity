/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Forward.h"
#include "Spreadsheet.h"

namespace Spreadsheet {

class Workbook {
public:
    Workbook(Vector<NonnullRefPtr<Sheet>>&& sheets, GUI::Window& parent_window);

    ErrorOr<void, DeprecatedString> open_file(String const& filename, Core::File&);
    ErrorOr<void> write_to_file(String const& filename, Core::File&);

    ErrorOr<bool, DeprecatedString> import_file(String const& filename, Core::File&);

    DeprecatedString const& current_filename() const { return m_current_filename; }
    bool set_filename(DeprecatedString const& filename);
    bool dirty() { return m_dirty; }
    void set_dirty(bool dirty) { m_dirty = dirty; }

    bool has_sheets() const { return !m_sheets.is_empty(); }

    Vector<NonnullRefPtr<Sheet>> const& sheets() const { return m_sheets; }
    Vector<NonnullRefPtr<Sheet>> sheets() { return m_sheets; }

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
    Vector<NonnullRefPtr<Sheet>> m_sheets;
    NonnullRefPtr<JS::VM> m_vm;
    NonnullOwnPtr<JS::Interpreter> m_interpreter;
    JS::VM::InterpreterExecutionScope m_interpreter_scope;
    JS::GCPtr<WorkbookObject> m_workbook_object;
    JS::ExecutionContext m_main_execution_context;
    GUI::Window& m_parent_window;

    DeprecatedString m_current_filename;
    bool m_dirty { false };
};

}
