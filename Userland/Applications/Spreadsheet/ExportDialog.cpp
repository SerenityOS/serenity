/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ExportDialog.h"
#include "Spreadsheet.h"
#include "Workbook.h"
#include <AK/DeprecatedString.h>
#include <AK/JsonArray.h>
#include <AK/LexicalPath.h>
#include <AK/MemoryStream.h>
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
extern StringView select_format_page_gml;

namespace Spreadsheet {

CSVExportDialogPage::CSVExportDialogPage(Sheet const& sheet)
    : m_data(sheet.to_xsv())
{
    m_headers.extend(m_data.take_first());

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

    m_quote_escape_combo_box->set_model(GUI::ItemListModel<DeprecatedString>::create(m_quote_escape_items));

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

auto CSVExportDialogPage::make_writer(OutputStream& stream) -> Optional<XSV>
{
    DeprecatedString delimiter;
    DeprecatedString quote;
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
    Vector<DeprecatedString> empty_headers;
    auto* headers = &empty_headers;

    if (should_export_headers) {
        behaviors = behaviors | Writer::WriterBehavior::WriteHeaders;
        headers = &m_headers;
    }

    if (should_quote_all_fields)
        behaviors = behaviors | Writer::WriterBehavior::QuoteAll;

    return XSV(stream, m_data, move(traits), *headers, behaviors);
}

void CSVExportDialogPage::update_preview()
{
    DuplexMemoryStream memory_stream;
    auto maybe_writer = make_writer(memory_stream);
    if (!maybe_writer.has_value()) {
        m_data_preview_text_editor->set_text({});
        return;
    }

    maybe_writer->generate_preview();
    auto buffer = memory_stream.copy_into_contiguous_buffer();
    m_data_preview_text_editor->set_text(StringView(buffer));
    m_data_preview_text_editor->update();
}

Result<void, DeprecatedString> ExportDialog::make_and_run_for(StringView mime, Core::File& file, Workbook& workbook)
{
    auto wizard = GUI::WizardDialog::construct(GUI::Application::the()->active_window());
    wizard->set_title("File Export Wizard");
    wizard->set_icon(GUI::Icon::default_icon("app-spreadsheet"sv).bitmap_for_size(16));

    auto export_xsv = [&]() -> Result<void, DeprecatedString> {
        // FIXME: Prompt for the user to select a specific sheet to export
        //        For now, export the first sheet (if available)
        if (!workbook.has_sheets())
            return DeprecatedString { "The workbook has no sheets to export!" };

        CSVExportDialogPage page { workbook.sheets().first() };
        wizard->replace_page(page.page());
        if (wizard->exec() != GUI::Dialog::ExecResult::OK)
            return DeprecatedString { "CSV Export was cancelled" };

        auto file_stream = Core::OutputFileStream(file);
        auto writer = page.make_writer(file_stream);
        if (!writer.has_value())
            return DeprecatedString::formatted("CSV Export failed");
        writer->generate();
        if (writer->has_error())
            return DeprecatedString::formatted("CSV Export failed: {}", writer->error_string());
        return {};
    };

    auto export_worksheet = [&]() -> Result<void, DeprecatedString> {
        JsonArray array;
        for (auto& sheet : workbook.sheets())
            array.append(sheet.to_json());

        auto file_content = array.to_deprecated_string();
        bool result = file.write(file_content);
        if (!result) {
            int error_number = errno;
            auto const* error = strerror(error_number);

            StringBuilder sb;
            sb.append("Unable to save file. Error: "sv);
            sb.append({ error, strlen(error) });

            return sb.to_deprecated_string();
        }

        return {};
    };

    if (mime == "text/csv") {
        return export_xsv();
    } else if (mime == "application/x-sheets+json") {
        return export_worksheet();
    } else {
        auto page = GUI::WizardPage::construct(
            "Export File Format",
            DeprecatedString::formatted("Select the format you wish to export to '{}' as", LexicalPath::basename(file.filename())));

        page->on_next_page = [] { return nullptr; };

        page->body_widget().load_from_gml(select_format_page_gml);
        auto format_combo_box = page->body_widget().find_descendant_of_type_named<GUI::ComboBox>("select_format_page_format_combo_box");

        Vector<DeprecatedString> supported_formats {
            "CSV (text/csv)",
            "Spreadsheet Worksheet",
        };
        format_combo_box->set_model(GUI::ItemListModel<DeprecatedString>::create(supported_formats));

        wizard->push_page(page);

        if (wizard->exec() != GUI::Dialog::ExecResult::OK)
            return DeprecatedString { "Export was cancelled" };

        if (format_combo_box->selected_index() == 0)
            return export_xsv();

        if (format_combo_box->selected_index() == 1)
            return export_worksheet();

        VERIFY_NOT_REACHED();
    }
}

};
