/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ExportDialog.h"
#include "Spreadsheet.h"
#include "Workbook.h"
#include <AK/ByteString.h>
#include <AK/JsonArray.h>
#include <AK/LexicalPath.h>
#include <AK/MemoryStream.h>
#include <Applications/Spreadsheet/CSVExportGML.h>
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

    m_page = GUI::WizardPage::create(
        "CSV Export Options"sv,
        "Please select the options for the csv file you wish to export to"sv)
                 .release_value_but_fixme_should_propagate_errors();

    m_page->body_widget().load_from_gml(csv_export_gml).release_value_but_fixme_should_propagate_errors();
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

    m_quote_escape_combo_box->set_model(GUI::ItemListModel<ByteString>::create(m_quote_escape_items));

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

auto CSVExportDialogPage::generate(Stream& stream, GenerationType type) -> ErrorOr<void>
{
    auto delimiter = TRY([this]() -> ErrorOr<ByteString> {
        if (m_delimiter_other_radio->is_checked()) {
            if (m_delimiter_other_text_box->text().is_empty())
                return Error::from_string_literal("Delimiter unset");
            return m_delimiter_other_text_box->text();
        }
        if (m_delimiter_comma_radio->is_checked())
            return ",";
        if (m_delimiter_semicolon_radio->is_checked())
            return ";";
        if (m_delimiter_tab_radio->is_checked())
            return "\t";
        if (m_delimiter_space_radio->is_checked())
            return " ";
        return Error::from_string_literal("Delimiter unset");
    }());

    auto quote = TRY([this]() -> ErrorOr<ByteString> {
        if (m_quote_other_radio->is_checked()) {
            if (m_quote_other_text_box->text().is_empty())
                return Error::from_string_literal("Quote separator unset");
            return m_quote_other_text_box->text();
        }
        if (m_quote_single_radio->is_checked())
            return "'";
        if (m_quote_double_radio->is_checked())
            return "\"";
        return Error::from_string_literal("Quote separator unset");
    }());

    auto quote_escape = [this]() {
        auto index = m_quote_escape_combo_box->selected_index();
        if (index == 0)
            return Writer::WriterTraits::Repeat;
        if (index == 1)
            return Writer::WriterTraits::Backslash;
        VERIFY_NOT_REACHED();
    }();

    auto should_export_headers = m_export_header_check_box->is_checked();
    auto should_quote_all_fields = m_quote_all_fields_check_box->is_checked();

    Writer::WriterTraits traits {
        move(delimiter),
        move(quote),
        quote_escape,
    };

    auto behaviors = Writer::default_behaviors();
    Vector<ByteString> empty_headers;
    auto* headers = &empty_headers;

    if (should_export_headers) {
        behaviors = behaviors | Writer::WriterBehavior::WriteHeaders;
        headers = &m_headers;
    }

    if (should_quote_all_fields)
        behaviors = behaviors | Writer::WriterBehavior::QuoteAll;

    switch (type) {
    case GenerationType::Normal:
        TRY((Writer::XSV<decltype(m_data), Vector<ByteString>>::generate(stream, m_data, move(traits), *headers, behaviors)));
        break;
    case GenerationType::Preview:
        TRY((Writer::XSV<decltype(m_data), decltype(*headers)>::generate_preview(stream, m_data, move(traits), *headers, behaviors)));
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    return {};
}

void CSVExportDialogPage::update_preview()
{
    auto maybe_error = [this]() -> ErrorOr<void> {
        AllocatingMemoryStream memory_stream;
        TRY(generate(memory_stream, GenerationType::Preview));
        auto buffer = TRY(memory_stream.read_until_eof());
        m_data_preview_text_editor->set_text(StringView(buffer));
        m_data_preview_text_editor->update();
        return {};
    }();
    if (maybe_error.is_error())
        m_data_preview_text_editor->set_text(ByteString::formatted("Cannot update preview: {}", maybe_error.error()));
}

ErrorOr<void> ExportDialog::make_and_run_for(StringView mime, Core::File& file, ByteString filename, Workbook& workbook)
{
    auto wizard = TRY(GUI::WizardDialog::create(GUI::Application::the()->active_window()));
    wizard->set_title("File Export Wizard");
    wizard->set_icon(GUI::Icon::default_icon("app-spreadsheet"sv).bitmap_for_size(16));

    auto export_xsv = [&]() -> ErrorOr<void> {
        // FIXME: Prompt for the user to select a specific sheet to export
        //        For now, export the first sheet (if available)
        if (!workbook.has_sheets())
            return Error::from_string_literal("The workbook has no sheets to export!");

        CSVExportDialogPage page { workbook.sheets().first() };
        wizard->replace_page(page.page());
        if (wizard->exec() != GUI::Dialog::ExecResult::OK)
            return Error::from_string_literal("CSV Export was cancelled");

        TRY(page.generate(file, CSVExportDialogPage::GenerationType::Normal));
        return {};
    };

    auto export_worksheet = [&]() -> ErrorOr<void> {
        JsonArray array;
        for (auto& sheet : workbook.sheets())
            array.must_append(sheet->to_json());

        auto file_content = array.to_byte_string();
        return file.write_until_depleted(file_content.bytes());
    };

    if (mime == "text/csv") {
        return export_xsv();
    } else if (mime == "application/x-sheets+json") {
        return export_worksheet();
    } else {
        auto page = TRY(GUI::WizardPage::create(
            "Export File Format"sv,
            TRY(String::formatted("Select the format you wish to export to '{}' as", LexicalPath::basename(filename)))));

        page->on_next_page = [] { return nullptr; };

        TRY(page->body_widget().load_from_gml(select_format_page_gml));
        auto format_combo_box = page->body_widget().find_descendant_of_type_named<GUI::ComboBox>("select_format_page_format_combo_box");

        Vector<ByteString> supported_formats {
            "CSV (text/csv)",
            "Spreadsheet Worksheet",
        };
        format_combo_box->set_model(GUI::ItemListModel<ByteString>::create(supported_formats));

        wizard->push_page(page);

        if (wizard->exec() != GUI::Dialog::ExecResult::OK)
            return Error::from_string_literal("Export was cancelled");

        if (format_combo_box->selected_index() == 0)
            return export_xsv();

        if (format_combo_box->selected_index() == 1)
            return export_worksheet();

        VERIFY_NOT_REACHED();
    }
}

};
