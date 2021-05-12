/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Workbook.h"
#include "ExportDialog.h"
#include "ImportDialog.h"
#include "JSIntegration.h"
#include "Readers/CSV.h"
#include "Writers/CSV.h"
#include <AK/ByteBuffer.h>
#include <AK/JsonArray.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/Stream.h>
#include <LibCore/File.h>
#include <LibCore/FileStream.h>
#include <LibCore/MimeData.h>
#include <LibGUI/TextBox.h>
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
    auto file_or_error = Core::File::open(filename, Core::OpenMode::ReadOnly);
    if (file_or_error.is_error()) {
        StringBuilder sb;
        sb.append("Failed to open ");
        sb.append(filename);
        sb.append(" for reading. Error: ");
        sb.append(file_or_error.error());

        return sb.to_string();
    }

    auto mime = Core::guess_mime_type_based_on_filename(filename);

    // Make an import dialog, we might need to import it.
    auto result = ImportDialog::make_and_run_for(mime, file_or_error.value(), *this);
    if (result.is_error())
        return result.error();

    m_sheets = result.release_value();

    set_filename(filename);

    return true;
}

Result<bool, String> Workbook::save(const StringView& filename)
{
    auto mime = Core::guess_mime_type_based_on_filename(filename);
    auto file = Core::File::construct(filename);
    file->open(Core::OpenMode::WriteOnly);
    if (!file->is_open()) {
        StringBuilder sb;
        sb.append("Failed to open ");
        sb.append(filename);
        sb.append(" for write. Error: ");
        sb.append(file->error_string());

        return sb.to_string();
    }

    // Make an export dialog, we might need to import it.
    auto result = ExportDialog::make_and_run_for(mime, *file, *this);
    if (result.is_error())
        return result.error();

    set_filename(filename);
    set_dirty(false);
    return true;
}

}
