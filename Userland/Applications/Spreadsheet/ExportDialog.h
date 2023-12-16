/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Writers/XSV.h"
#include <AK/Result.h>
#include <AK/StringView.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Wizards/WizardPage.h>

namespace Spreadsheet {

class Sheet;
class Workbook;

struct CSVExportDialogPage {
    explicit CSVExportDialogPage(Sheet const&);

    NonnullRefPtr<GUI::WizardPage> page() { return *m_page; }

    enum class GenerationType {
        Normal,
        Preview
    };

    ErrorOr<void> generate(Stream&, GenerationType);

protected:
    void update_preview();

private:
    Vector<Vector<ByteString>> m_data;
    Vector<ByteString> m_headers;
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
    RefPtr<GUI::CheckBox> m_export_header_check_box;
    RefPtr<GUI::CheckBox> m_quote_all_fields_check_box;
    RefPtr<GUI::TextEditor> m_data_preview_text_editor;
    Vector<ByteString> m_quote_escape_items {
        // Note: Keep in sync with Writer::WriterTraits::QuoteEscape.
        "Repeat",
        "Backslash",
    };
};

struct ExportDialog {
    static ErrorOr<void> make_and_run_for(StringView mime, Core::File&, ByteString filename, Workbook&);
};

}
