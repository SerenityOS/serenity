/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
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
    using XSV = Writer::XSV<Vector<Vector<String>>, Vector<String>>;

    explicit CSVExportDialogPage(const Sheet&);

    NonnullRefPtr<GUI::WizardPage> page() { return *m_page; }
    Optional<XSV>& writer() { return m_previously_made_writer; }
    Result<void, String> move_into(const String& target);

protected:
    void update_preview();
    Optional<XSV> make_writer();

private:
    Vector<Vector<String>> m_data;
    Vector<String> m_headers;
    Optional<XSV> m_previously_made_writer;
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
    Vector<String> m_quote_escape_items {
        // Note: Keep in sync with Writer::WriterTraits::QuoteEscape.
        "Repeat",
        "Backslash",
    };

    String m_temp_output_file_path;
};

struct ExportDialog {
    static Result<void, String> make_and_run_for(StringView mime, Core::File& file, Workbook&);
};

}
