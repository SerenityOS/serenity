/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Workbook.h"
#include "ExportDialog.h"
#include "ImportDialog.h"
#include "JSIntegration.h"
#include <AK/ByteBuffer.h>
#include <AK/StringView.h>
#include <LibCore/MimeData.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Window.h>
#include <LibJS/Runtime/GlobalEnvironment.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace Spreadsheet {

Workbook::Workbook(Vector<NonnullRefPtr<Sheet>>&& sheets, GUI::Window& parent_window)
    : m_sheets(move(sheets))
    , m_vm(JS::VM::create().release_value_but_fixme_should_propagate_errors())
    , m_root_execution_context(JS::create_simple_execution_context<JS::GlobalObject>(m_vm))
    , m_main_execution_context(m_vm->heap())
    , m_parent_window(parent_window)
{
    auto& realm = *m_root_execution_context->realm;
    auto& vm = realm.vm();
    m_workbook_object = vm.heap().allocate<WorkbookObject>(realm, realm, *this);
    realm.global_object().define_direct_property("workbook", workbook_object(), JS::default_attributes);

    m_main_execution_context.this_value = &realm.global_object();
    m_main_execution_context.function_name = "(global execution context)"sv;
    m_main_execution_context.lexical_environment = &realm.global_environment();
    m_main_execution_context.variable_environment = &realm.global_environment();
    m_main_execution_context.realm = &realm;
    m_main_execution_context.is_strict_mode = true;
    m_vm->push_execution_context(m_main_execution_context);
    m_vm->enable_default_host_import_module_dynamically_hook();
}

bool Workbook::set_filename(DeprecatedString const& filename)
{
    if (m_current_filename == filename)
        return false;

    m_current_filename = filename;
    return true;
}

ErrorOr<void, DeprecatedString> Workbook::open_file(String const& filename, Core::File& file)
{
    auto mime = Core::guess_mime_type_based_on_filename(filename);

    // Make an import dialog, we might need to import it.
    m_sheets = TRY(ImportDialog::make_and_run_for(m_parent_window, mime, filename, file, *this));

    set_filename(filename.to_deprecated_string());

    return {};
}

ErrorOr<void> Workbook::write_to_file(String const& filename, Core::File& stream)
{
    auto mime = Core::guess_mime_type_based_on_filename(filename);

    // Make an export dialog, we might need to import it.
    TRY(ExportDialog::make_and_run_for(mime, stream, filename.to_deprecated_string(), *this));

    set_filename(filename.to_deprecated_string());
    set_dirty(false);
    return {};
}

ErrorOr<bool, DeprecatedString> Workbook::import_file(String const& filename, Core::File& file)
{
    auto mime = Core::guess_mime_type_based_on_filename(filename);

    auto sheets = TRY(ImportDialog::make_and_run_for(m_parent_window, mime, filename, file, *this));
    auto has_any_changes = !sheets.is_empty();
    m_sheets.extend(move(sheets));

    return has_any_changes;
}

}
