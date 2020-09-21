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

#include "Workbook.h"
#include "JSIntegration.h"
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/JsonParser.h>
#include <LibCore/File.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <string.h>

namespace Spreadsheet {

static JS::VM& global_vm()
{
    static RefPtr<JS::VM> vm;
    if (!vm)
        vm = JS::VM::create();
    return *vm;
}

Workbook::Workbook(NonnullRefPtrVector<Sheet>&& sheets)
    : m_sheets(move(sheets))
    , m_interpreter(JS::Interpreter::create<JS::GlobalObject>(global_vm()))
    , m_interpreter_scope(JS::VM::InterpreterExecutionScope(interpreter()))
{
    m_workbook_object = interpreter().heap().allocate<WorkbookObject>(global_object(), *this);
    global_object().put("workbook", workbook_object());
}

bool Workbook::set_filename(const String& filename)
{
    if (m_current_filename == filename)
        return false;

    m_current_filename = filename;
    return true;
}

Result<bool, String> Workbook::load(const StringView& filename)
{
    auto file_or_error = Core::File::open(filename, Core::IODevice::OpenMode::ReadOnly);
    if (file_or_error.is_error()) {
        StringBuilder sb;
        sb.append("Failed to open ");
        sb.append(filename);
        sb.append(" for reading. Error: ");
        sb.append(file_or_error.error());

        return sb.to_string();
    }

    auto json_value_option = JsonParser(file_or_error.value()->read_all()).parse();
    if (!json_value_option.has_value()) {
        StringBuilder sb;
        sb.append("Failed to parse ");
        sb.append(filename);

        return sb.to_string();
    }

    auto& json_value = json_value_option.value();
    if (!json_value.is_array()) {
        StringBuilder sb;
        sb.append("Did not find a spreadsheet in ");
        sb.append(filename);

        return sb.to_string();
    }

    NonnullRefPtrVector<Sheet> sheets;

    auto& json_array = json_value.as_array();
    json_array.for_each([&](auto& sheet_json) {
        if (!sheet_json.is_object())
            return IterationDecision::Continue;

        auto sheet = Sheet::from_json(sheet_json.as_object(), *this);
        if (!sheet)
            return IterationDecision::Continue;

        sheets.append(sheet.release_nonnull());

        return IterationDecision::Continue;
    });

    m_sheets.clear();
    m_sheets = move(sheets);

    set_filename(filename);

    return true;
}

Result<bool, String> Workbook::save(const StringView& filename)
{
    JsonArray array;

    for (auto& sheet : m_sheets)
        array.append(sheet.to_json());

    auto file_content = array.to_string();

    auto file = Core::File::construct(filename);
    file->open(Core::IODevice::WriteOnly);
    if (!file->is_open()) {
        StringBuilder sb;
        sb.append("Failed to open ");
        sb.append(filename);
        sb.append(" for write. Error: ");
        sb.append(file->error_string());

        return sb.to_string();
    }

    bool result = file->write(file_content);
    if (!result) {
        int error_number = errno;
        StringBuilder sb;
        sb.append("Unable to save file. Error: ");
        sb.append(strerror(error_number));

        return sb.to_string();
    }

    set_filename(filename);
    return true;
}

}
