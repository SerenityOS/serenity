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
};

}
