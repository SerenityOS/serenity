/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include "SpreadsheetWidget.h"
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonObjectSerializer.h>
#include <LibCore/File.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/TabWidget.h>
#include <string.h>

namespace Spreadsheet {

SpreadsheetWidget::SpreadsheetWidget()
{
    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>().set_margins({ 2, 2, 2, 2 });
    m_tab_widget = add<GUI::TabWidget>();
    m_tab_widget->set_tab_position(GUI::TabWidget::TabPosition::Bottom);

    m_sheets.append(Sheet::construct("Sheet 1"));
    m_tab_widget->add_tab<SpreadsheetView>(m_sheets.first().name(), m_sheets.first());
}

SpreadsheetWidget::~SpreadsheetWidget()
{
}

void SpreadsheetWidget::save(const StringView& filename)
{
    JsonArray array;
    m_tab_widget->for_each_child_of_type<SpreadsheetView>([&](auto& view) {
        array.append(view.sheet().to_json());
        return IterationDecision::Continue;
    });

    auto file_content = array.to_string();

    auto file = Core::File::construct(filename);
    file->open(Core::IODevice::WriteOnly);
    if (!file->is_open()) {
        StringBuilder sb;
        sb.append("Failed to open ");
        sb.append(filename);
        sb.append(" for write. Error: ");
        sb.append(file->error_string());

        GUI::MessageBox::show(window(), sb.to_string(), "Error", GUI::MessageBox::Type::Error);
        return;
    }

    bool result = file->write(file_content);
    if (!result) {
        int error_number = errno;
        StringBuilder sb;
        sb.append("Unable to save file. Error: ");
        sb.append(strerror(error_number));

        GUI::MessageBox::show(window(), sb.to_string(), "Error", GUI::MessageBox::Type::Error);
        return;
    }
}

}
