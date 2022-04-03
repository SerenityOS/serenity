/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Workbook.h"
#include "ExportDialog.h"
#include "ImportDialog.h"
#include "JSIntegration.h"
#include "LibGUI/MessageBox.h"
#include "Readers/CSV.h"
#include <AK/ByteBuffer.h>
#include <AK/StringView.h>
#include <LibCore/File.h>
#include <LibCore/MimeData.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Window.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace Spreadsheet {

Workbook::Workbook(NonnullRefPtrVector<Sheet>&& sheets, GUI::Window& parent_window)
    : m_sheets(move(sheets))
    , m_vm(JS::VM::create())
    , m_interpreter(JS::Interpreter::create<JS::GlobalObject>(m_vm))
    , m_interpreter_scope(*m_interpreter)
    , m_main_execution_context(m_vm->heap())
    , m_parent_window(parent_window)
{
    m_workbook_object = m_vm->heap().allocate<WorkbookObject>(m_interpreter->global_object(), *this, m_interpreter->global_object());
    m_interpreter->global_object().define_direct_property("workbook", workbook_object(), JS::default_attributes);

    m_main_execution_context.current_node = nullptr;
    m_main_execution_context.this_value = &m_interpreter->global_object();
    m_main_execution_context.function_name = "(global execution context)"sv;
    m_main_execution_context.lexical_environment = &m_interpreter->realm().global_environment();
    m_main_execution_context.variable_environment = &m_interpreter->realm().global_environment();
    m_main_execution_context.realm = &m_interpreter->realm();
    m_main_execution_context.is_strict_mode = true;
    m_vm->push_execution_context(m_main_execution_context);
    m_vm->enable_default_host_import_module_dynamically_hook();
}

bool Workbook::set_filename(String const& filename)
{
    if (m_current_filename == filename)
        return false;

    m_current_filename = filename;
    return true;
}

Result<bool, String> Workbook::open_file(Core::File& file)
{
    auto mime = Core::guess_mime_type_based_on_filename(file.filename());

    // Make an import dialog, we might need to import it.
    auto result = ImportDialog::make_and_run_for(m_parent_window, mime, file, *this);
    if (result.is_error())
        return result.error();

    m_sheets = result.release_value();

    set_filename(file.filename());

    return true;
}

Result<bool, String> Workbook::load(StringView filename)
{
    auto response = FileSystemAccessClient::Client::the().try_request_file_read_only_approved(&m_parent_window, filename);
    if (response.is_error()) {
        StringBuilder sb;
        sb.append("Failed to open ");
        sb.append(filename);
        sb.append(" for reading. Error: ");
        sb.appendff("{}", response.error());
        return sb.to_string();
    }

    return open_file(*response.value());
}

Result<bool, String> Workbook::save(StringView filename)
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
