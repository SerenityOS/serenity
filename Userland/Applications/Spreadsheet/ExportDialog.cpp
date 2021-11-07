/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ExportDialog.h"
#include "Spreadsheet.h"
#include "Workbook.h"
#include <AK/JsonArray.h>
#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <Applications/Spreadsheet/CSVExportGML.h>
#include <LibCore/File.h>
#include <LibCore/FileStream.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/Application.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Wizards/WizardDialog.h>
#include <LibGUI/Wizards/WizardPage.h>
#include <string.h>
#include <unistd.h>

// This is defined in ImportDialog.cpp, we can't include it twice, since the generated symbol is exported.
extern const char select_format_page_gml[];

namespace Spreadsheet {

CSVExportDialogPage::CSVExportDialogPage(const Sheet& sheet)
    : m_data(sheet.to_xsv())
{
    m_headers.extend(m_data.take_first());

    auto temp_template = String::formatted("{}/spreadsheet-csv-export.{}.XXXXXX", Core::StandardPaths::tempfile_directory(), getpid());
    auto temp_path = ByteBuffer::create_uninitialized(temp_template.length() + 1).release_value();
    auto buf = reinterpret_cast<char*>(temp_path.data());
    auto copy_ok = temp_template.copy_characters_to_buffer(buf, temp_path.size());
    VERIFY(copy_ok);

    int fd = mkstemp(buf);
    if (fd < 0) {
        perror("mkstemp");
        // Just let the operation fail cleanly later.
    } else {
        unlink(buf);
        m_temp_output_file_path = temp_path;
    }

    m_page = GUI::WizardPage::construct(
        "CSV Export Options",
        "Please select the options for the csv file you wish to export to");

    m_page->body_widget().load_from_gml(csv_export_gml);
    m_page->set_is_final_page(true);

    m_delimiter_comma_radio = m_page->body_widget().find_descendant_of_type_named<GUI::RadioButton>("delimiter_comma_radio");
    m_delimiter_semicolon_radio = m_page->body_widget().find_descendant_of_type_named<GUI::RadioButton>("delimiter_semicolon_radio");
    m_delimiter_tab_radio = m_page->body_widget().find_descendant_of_type_named<GUI::RadioButton>("delimiter_tab_radio");
    m_delimiter_space_radio = m_page->body_widget().find_descendant_of_type_named<GUI::RadioButton>("delimiter_space_radio");
    m_delimiter_other_radio = m_page->body_widget().find_descendant_of_type_named<GUI::RadioButton>("delimiter_other_radio");
    m_delimiter_other_text_box = m_page->body_widget().find_descendant_of_type_named<GUI::TextBox>("delimiter_other_text_box");
    m_quote_single_radio = m_page->body_widget().find_descendant_of_type_named<GUI::RadioButton>("quote_single_radio");
    m_quote_double_radio = m_page->body_widget().find_descendant_of_type_named<GUI::RadioButton>("quote_double_radio");
    m_quote_other_radio = m_page->body_widget().find_descendant_of_type_named<GUI::RadioButton>("quote_other_radio");
    m_quote_other_text_box = m_page->body_widget().find_descendant_of_type_named<GUI::TextBox>("quote_other_text_box");
    m_quote_escape_combo_box = m_page->body_widget().find_descendant_of_type_named<GUI::ComboBox>("quote_escape_combo_box");
    m_export_header_check_box = m_page->body_widget().find_descendant_of_type_named<GUI::CheckBox>("export_header_check_box");
    m_quote_all_fields_check_box = m_page->body_widget().find_descendant_of_type_named<GUI::CheckBox>("quote_all_fields_check_box");
    m_data_preview_text_editor = m_page->body_widget().find_descendant_of_type_named<GUI::TextEditor>("data_preview_text_editor");

    m_data_preview_text_editor->set_should_hide_unnecessary_scrollbars(true);

    m_quote_escape_combo_box->set_model(GUI::ItemListModel<String>::create(m_quote_escape_items));

    // By default, use commas, double quotes with repeat, disable headers, and quote only the fields that require quoting.
    m_delimiter_comma_radio->set_checked(true);
    m_quote_double_radio->set_checked(true);
    m_quote_escape_combo_box->set_selected_index(0); // Repeat

    m_delimiter_comma_radio->on_checked = [&](auto) { update_preview(); };
    m_delimiter_semicolon_radio->on_checked = [&](auto) { update_preview(); };
    m_delimiter_tab_radio->on_checked = [&](auto) { update_preview(); };
    m_delimiter_space_radio->on_checked = [&](auto) { update_preview(); };
    m_delimiter_other_radio->on_checked = [&](auto) { update_preview(); };
    m_delimiter_other_text_box->on_change = [&] {
        if (m_delimiter_other_radio->is_checked())
            update_preview();
    };
    m_quote_single_radio->on_checked = [&](auto) { update_preview(); };
    m_quote_double_radio->on_checked = [&](auto) { update_preview(); };
    m_quote_other_radio->on_checked = [&](auto) { update_preview(); };
    m_quote_other_text_box->on_change = [&] {
        if (m_quote_other_radio->is_checked())
            update_preview();
    };
    m_quote_escape_combo_box->on_change = [&](auto&, auto&) { update_preview(); };
    m_export_header_check_box->on_checked = [&](auto) { update_preview(); };
    m_quote_all_fields_check_box->on_checked = [&](auto) { update_preview(); };

    update_preview();
}

auto CSVExportDialogPage::make_writer() -> Optional<XSV>
{
    String delimiter;
    String quote;
    Writer::WriterTraits::QuoteEscape quote_escape;

    // Delimiter
    if (m_delimiter_other_radio->is_checked())
        delimiter = m_delimiter_other_text_box->text();
    else if (m_delimiter_comma_radio->is_checked())
        delimiter = ",";
    else if (m_delimiter_semicolon_radio->is_checked())
        delimiter = ";";
    else if (m_delimiter_tab_radio->is_checked())
        delimiter = "\t";
    else if (m_delimiter_space_radio->is_checked())
        delimiter = " ";
    else
        return {};

    // Quote separator
    if (m_quote_other_radio->is_checked())
        quote = m_quote_other_text_box->text();
    else if (m_quote_single_radio->is_checked())
        quote = "'";
    else if (m_quote_double_radio->is_checked())
        quote = "\"";
    else
        return {};

    // Quote escape
    auto index = m_quote_escape_combo_box->selected_index();
    if (index == 0)
        quote_escape = Writer::WriterTraits::Repeat;
    else if (index == 1)
        quote_escape = Writer::WriterTraits::Backslash;
    else
        return {};

    auto should_export_headers = m_export_header_check_box->is_checked();
    auto should_quote_all_fields = m_quote_all_fields_check_box->is_checked();

    if (quote.is_empty() || delimiter.is_empty())
        return {};

    Writer::WriterTraits traits {
        move(delimiter),
        move(quote),
        quote_escape,
    };

    auto behaviors = Writer::default_behaviors();
    Vector<String> empty_headers;
    auto* headers = &empty_headers;

    if (should_export_headers) {
        behaviors = behaviors | Writer::WriterBehavior::WriteHeaders;
        headers = &m_headers;
    }

    if (should_quote_all_fields)
        behaviors = behaviors | Writer::WriterBehavior::QuoteAll;

    // Note that the stream is used only by the ctor.
    auto stream = Core::OutputFileStream::open(m_temp_output_file_path);
    if (stream.is_error()) {
        dbgln("Cannot open {} for writing: {}", m_temp_output_file_path, stream.error());
        return {};
    }
    XSV writer(stream.value(), m_data, traits, *headers, behaviors);

    if (stream.value().has_any_error()) {
        dbgln("Write error when making preview");
        return {};
    }

    return writer;
}

void CSVExportDialogPage::update_preview()

{
    m_previously_made_writer = make_writer();
    if (!m_previously_made_writer.has_value()) {
    fail:;
        m_data_preview_text_editor->set_text({});
        return;
    }

    auto file_or_error = Core::File::open(
        m_temp_output_file_path,
        Core::OpenMode::ReadOnly);
    if (file_or_error.is_error())
        goto fail;

    auto& file = *file_or_error.value();
    StringBuilder builder;
    size_t line = 0;
    while (file.can_read_line()) {
        if (++line == 8)
            break;

        builder.append(file.read_line());
        builder.append('\n');
    }
    m_data_preview_text_editor->set_text(builder.string_view());
    m_data_preview_text_editor->update();
}

Result<void, String> CSVExportDialogPage::move_into(const String& target)
{
    auto& source = m_temp_output_file_path;

    // First, try rename().
    auto rc = rename(source.characters(), target.characters());
    if (rc == 0)
        return {};

    auto saved_errno = errno;
    if (saved_errno == EXDEV) {
        // Can't do that, copy it instead.
        auto result = Core::File::copy_file_or_directory(
            target, source,
            Core::File::RecursionMode::Disallowed,
            Core::File::LinkMode::Disallowed,
            Core::File::AddDuplicateFileMarker::No);

        if (result.is_error())
            return String::formatted("{}", static_cast<Error const&>(result.error()));

        return {};
    }

    perror("rename");
    return String { strerror(saved_errno) };
}

Result<void, String> ExportDialog::make_and_run_for(StringView mime, Core::File& file, Workbook& workbook)
{
    auto wizard = GUI::WizardDialog::construct(GUI::Application::the()->active_window());
    wizard->set_title("File Export Wizard");
    wizard->set_icon(GUI::Icon::default_icon("app-spreadsheet").bitmap_for_size(16));

    auto export_xsv = [&]() -> Result<void, String> {
        // FIXME: Prompt for the user to select a specific sheet to export
        //        For now, export the first sheet (if available)
        if (!workbook.has_sheets())
            return String { "The workbook has no sheets to export!" };

        CSVExportDialogPage page { workbook.sheets().first() };
        wizard->replace_page(page.page());
        auto result = wizard->exec();

        if (result == GUI::Dialog::ExecResult::ExecOK) {
            auto& writer = page.writer();
            if (!writer.has_value())
                return String { "CSV Export failed" };
            if (writer->has_error())
                return String::formatted("CSV Export failed: {}", writer->error_string());

            // No error, move the temp file to the expected location
            return page.move_into(file.filename());
        } else {
            return String { "CSV Export was cancelled" };
        }
    };

    auto export_worksheet = [&]() -> Result<void, String> {
        JsonArray array;
        for (auto& sheet : workbook.sheets())
            array.append(sheet.to_json());

        auto file_content = array.to_string();
        bool result = file.write(file_content);
        if (!result) {
            int error_number = errno;
            StringBuilder sb;
            sb.append("Unable to save file. Error: ");
            sb.append(strerror(error_number));

            return sb.to_string();
        }

        return {};
    };

    if (mime == "text/csv") {
        return export_xsv();
    } else if (mime == "text/plain" && file.filename().ends_with(".sheets")) {
        return export_worksheet();
    } else {
        auto page = GUI::WizardPage::construct(
            "Export File Format",
            String::formatted("Select the format you wish to export to '{}' as", LexicalPath::basename(file.filename())));

        page->on_next_page = [] { return nullptr; };

        page->body_widget().load_from_gml(select_format_page_gml);
        auto format_combo_box = page->body_widget().find_descendant_of_type_named<GUI::ComboBox>("select_format_page_format_combo_box");

        Vector<String> supported_formats {
            "CSV (text/csv)",
            "Spreadsheet Worksheet",
        };
        format_combo_box->set_model(GUI::ItemListModel<String>::create(supported_formats));

        wizard->push_page(page);

        if (wizard->exec() != GUI::Dialog::ExecResult::ExecOK)
            return String { "Export was cancelled" };

        if (format_combo_box->selected_index() == 0)
            return export_xsv();

        if (format_combo_box->selected_index() == 1)
            return export_worksheet();

        VERIFY_NOT_REACHED();
    }
}

};
