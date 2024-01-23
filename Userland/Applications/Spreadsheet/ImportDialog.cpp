/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ImportDialog.h"
#include "Spreadsheet.h"
#include <AK/JsonParser.h>
#include <AK/LexicalPath.h>
#include <Applications/Spreadsheet/CSVImportGML.h>
#include <Applications/Spreadsheet/FormatSelectionPageGML.h>
#include <LibGUI/Application.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/StackWidget.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Window.h>
#include <LibGUI/Wizards/WizardDialog.h>
#include <LibGUI/Wizards/WizardPage.h>

namespace Spreadsheet {

CSVImportDialogPage::CSVImportDialogPage(StringView csv)
    : m_csv(csv)
{
    m_page = GUI::WizardPage::create(
        "CSV Import Options"sv,
        "Please select the options for the csv file you wish to import"sv)
                 .release_value_but_fixme_should_propagate_errors();

    m_page->body_widget().load_from_gml(csv_import_gml).release_value_but_fixme_should_propagate_errors();
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
    m_read_header_check_box = m_page->body_widget().find_descendant_of_type_named<GUI::CheckBox>("read_header_check_box");
    m_trim_leading_field_spaces_check_box = m_page->body_widget().find_descendant_of_type_named<GUI::CheckBox>("trim_leading_field_spaces_check_box");
    m_trim_trailing_field_spaces_check_box = m_page->body_widget().find_descendant_of_type_named<GUI::CheckBox>("trim_trailing_field_spaces_check_box");
    m_data_preview_table_view = m_page->body_widget().find_descendant_of_type_named<GUI::TableView>("data_preview_table_view");
    m_data_preview_error_label = m_page->body_widget().find_descendant_of_type_named<GUI::Label>("data_preview_error_label");
    m_data_preview_widget = m_page->body_widget().find_descendant_of_type_named<GUI::StackWidget>("data_preview_widget");

    m_quote_escape_combo_box->set_model(GUI::ItemListModel<ByteString>::create(m_quote_escape_items));

    // By default, use commas, double quotes with repeat, and disable headers.
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
    m_read_header_check_box->on_checked = [&](auto) { update_preview(); };
    m_trim_leading_field_spaces_check_box->on_checked = [&](auto) { update_preview(); };
    m_trim_trailing_field_spaces_check_box->on_checked = [&](auto) { update_preview(); };

    update_preview();
}

auto CSVImportDialogPage::make_reader() -> Optional<Reader::XSV>
{
    ByteString delimiter;
    ByteString quote;
    Reader::ParserTraits::QuoteEscape quote_escape;

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
        quote_escape = Reader::ParserTraits::Repeat;
    else if (index == 1)
        quote_escape = Reader::ParserTraits::Backslash;
    else
        return {};

    auto should_read_headers = m_read_header_check_box->is_checked();
    auto should_trim_leading = m_trim_leading_field_spaces_check_box->is_checked();
    auto should_trim_trailing = m_trim_trailing_field_spaces_check_box->is_checked();

    if (quote.is_empty() || delimiter.is_empty())
        return {};

    Reader::ParserTraits traits {
        move(delimiter),
        move(quote),
        quote_escape,
    };

    auto behaviors = Reader::default_behaviors() | Reader::ParserBehavior::Lenient;

    if (should_read_headers)
        behaviors = behaviors | Reader::ParserBehavior::ReadHeaders;
    if (should_trim_leading)
        behaviors = behaviors | Reader::ParserBehavior::TrimLeadingFieldSpaces;
    if (should_trim_trailing)
        behaviors = behaviors | Reader::ParserBehavior::TrimTrailingFieldSpaces;

    return Reader::XSV(m_csv, move(traits), behaviors);
}

void CSVImportDialogPage::update_preview()

{
    m_previously_made_reader = make_reader();
    if (!m_previously_made_reader.has_value()) {
        m_data_preview_table_view->set_model(nullptr);
        m_data_preview_error_label->set_text("Could not read the given file"_string);
        m_data_preview_widget->set_active_widget(m_data_preview_error_label);
        return;
    }

    auto& reader = *m_previously_made_reader;
    if (reader.has_error()) {
        m_data_preview_table_view->set_model(nullptr);
        m_data_preview_error_label->set_text(String::formatted("XSV parse error:\n{}", reader.error_string()).release_value_but_fixme_should_propagate_errors());
        m_data_preview_widget->set_active_widget(m_data_preview_error_label);
        return;
    }

    Vector<String> headers;
    for (auto const& header : reader.headers())
        headers.append(String::from_byte_string(header).release_value_but_fixme_should_propagate_errors());

    m_data_preview_table_view->set_model(
        GUI::ItemListModel<Reader::XSV::Row, Reader::XSV, Vector<String>>::create(reader, headers, min(8ul, reader.size())));
    m_data_preview_widget->set_active_widget(m_data_preview_table_view);
    m_data_preview_table_view->update();
}

ErrorOr<Vector<NonnullRefPtr<Sheet>>, ByteString> ImportDialog::make_and_run_for(GUI::Window& parent, StringView mime, ByteString const& filename, Core::File& file, Workbook& workbook)
{
    auto wizard = GUI::WizardDialog::create(&parent).release_value_but_fixme_should_propagate_errors();
    wizard->set_title("File Import Wizard");
    wizard->set_icon(GUI::Icon::default_icon("app-spreadsheet"sv).bitmap_for_size(16));

    auto import_xsv = [&]() -> ErrorOr<Vector<NonnullRefPtr<Sheet>>, ByteString> {
        auto contents_or_error = file.read_until_eof();
        if (contents_or_error.is_error())
            return ByteString::formatted("{}", contents_or_error.release_error());
        CSVImportDialogPage page { contents_or_error.value() };
        wizard->replace_page(page.page());
        auto result = wizard->exec();

        if (result == GUI::Dialog::ExecResult::OK) {
            auto& reader = page.reader();

            Vector<NonnullRefPtr<Sheet>> sheets;

            if (reader.has_value()) {
                reader->parse();
                if (reader.value().has_error())
                    return ByteString::formatted("CSV Import failed: {}", reader.value().error_string());

                auto sheet = Sheet::from_xsv(reader.value(), workbook);
                if (sheet)
                    sheets.append(sheet.release_nonnull());
            }

            return sheets;
        }

        return ByteString { "CSV Import was cancelled" };
    };

    auto import_worksheet = [&]() -> ErrorOr<Vector<NonnullRefPtr<Sheet>>, ByteString> {
        auto contents_or_error = file.read_until_eof();
        if (contents_or_error.is_error())
            return ByteString::formatted("{}", contents_or_error.release_error());
        auto json_value_option = JsonParser(contents_or_error.release_value()).parse();
        if (json_value_option.is_error())
            return ByteString::formatted("Failed to parse {}", filename);

        auto& json_value = json_value_option.value();
        if (!json_value.is_array())
            return ByteString::formatted("Did not find a spreadsheet in {}", filename);

        Vector<NonnullRefPtr<Sheet>> sheets;

        auto& json_array = json_value.as_array();
        json_array.for_each([&](auto& sheet_json) {
            if (!sheet_json.is_object())
                return IterationDecision::Continue;

            if (auto sheet = Sheet::from_json(sheet_json.as_object(), workbook))
                sheets.append(sheet.release_nonnull());

            return IterationDecision::Continue;
        });

        return sheets;
    };

    if (mime == "text/csv") {
        return import_xsv();
    } else if (mime == "application/x-sheets+json") {
        return import_worksheet();
    } else {
        auto page = GUI::WizardPage::create(
            "Import File Format"sv,
            ByteString::formatted("Select the format you wish to import '{}' as", LexicalPath::basename(filename)))
                        .release_value_but_fixme_should_propagate_errors();

        page->on_next_page = [] { return nullptr; };

        page->body_widget().load_from_gml(select_format_page_gml).release_value_but_fixme_should_propagate_errors();
        auto format_combo_box = page->body_widget().find_descendant_of_type_named<GUI::ComboBox>("select_format_page_format_combo_box");

        Vector<ByteString> supported_formats {
            "CSV (text/csv)",
            "Spreadsheet Worksheet",
        };
        format_combo_box->set_model(GUI::ItemListModel<ByteString>::create(supported_formats));

        wizard->push_page(page);

        if (wizard->exec() != GUI::Dialog::ExecResult::OK)
            return ByteString { "Import was cancelled" };

        if (format_combo_box->selected_index() == 0)
            return import_xsv();

        if (format_combo_box->selected_index() == 1)
            return import_worksheet();

        VERIFY_NOT_REACHED();
    }
}

};
