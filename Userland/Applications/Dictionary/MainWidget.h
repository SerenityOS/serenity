/*
 * Copyright (c) 2021, Robin Allen <r@foon.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Widget.h>

#include "DictionaryModel.h"

namespace Dictionary {

class MainWidget final : public GUI::Widget {
    C_OBJECT(MainWidget);

    void focus_search_box();

private:
    MainWidget(StringView initial_query);

    RefPtr<DictionaryModel> m_model;

    RefPtr<GUI::ListView> m_list_view;
    RefPtr<GUI::TextEditor> m_editor;
    RefPtr<GUI::TextDocument> m_document;
    RefPtr<GUI::TextBox> m_search;

    void move_selection_by(int);
};

}
