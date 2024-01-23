/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Readers/XSV.h"
#include <AK/StringView.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Wizards/WizardPage.h>

namespace Spreadsheet {

class Sheet;
class Workbook;

struct CSVImportDialogPage {
    explicit CSVImportDialogPage(StringView csv);

    NonnullRefPtr<GUI::WizardPage> page() { return *m_page; }
    Optional<Reader::XSV>& reader() { return m_previously_made_reader; }

protected:
    void update_preview();
    Optional<Reader::XSV> make_reader();

private:
    StringView m_csv;
    Optional<Reader::XSV> m_previously_made_reader;
    RefPtr<GUI::WizardPage> m_page;
    RefPtr<GUI::RadioButton> m_delimiter_comma_radio;
    RefPtr<GUI::RadioButton> m_delimiter_semicolon_radio;
    RefPtr<GUI::RadioButton> m_delimiter_tab_radio;
    RefPtr<GUI::RadioButton> m_delimiter_space_radio;
    RefPtr<GUI::RadioButton> m_delimiter_other_radio;
    RefPtr<GUI::TextBox> m_delimiter_other_text_box;
    RefPtr<GUI::RadioButton> m_quote_single_radio;
    RefPtr<GUI::RadioButton> m_quote_double_radio;
    RefPtr<GUI::RadioButton> m_quote_other_radio;
    RefPtr<GUI::TextBox> m_quote_other_text_box;
    RefPtr<GUI::ComboBox> m_quote_escape_combo_box;
    RefPtr<GUI::CheckBox> m_read_header_check_box;
    RefPtr<GUI::CheckBox> m_trim_leading_field_spaces_check_box;
    RefPtr<GUI::CheckBox> m_trim_trailing_field_spaces_check_box;
    RefPtr<GUI::TableView> m_data_preview_table_view;
    RefPtr<GUI::Label> m_data_preview_error_label;
    RefPtr<GUI::StackWidget> m_data_preview_widget;
    Vector<ByteString> m_quote_escape_items {
        // Note: Keep in sync with Reader::ParserTraits::QuoteEscape.
        "Repeat",
        "Backslash",
    };
};

struct ImportDialog {
    static ErrorOr<Vector<NonnullRefPtr<Sheet>>, ByteString> make_and_run_for(GUI::Window& parent, StringView mime, ByteString const& filename, Core::File& file, Workbook&);
};

}
