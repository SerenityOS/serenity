/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "CookiesModel.h"
#include "LocalStorageModel.h"
#include "Tab.h"
#include <LibGUI/FilteringProxyModel.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibWeb/Cookie/Cookie.h>

namespace Browser {

class StorageWidget final : public GUI::Widget {
    C_OBJECT(StorageWidget);

public:
    virtual ~StorageWidget() override = default;
    void set_cookies_entries(Vector<Web::Cookie::Cookie> entries);
    void clear_cookies();

    void set_local_storage_entries(OrderedHashMap<String, String> entries);
    void clear_local_storage_entries();

private:
    StorageWidget();

    RefPtr<GUI::TableView> m_cookies_table_view;
    RefPtr<GUI::TextBox> m_cookies_textbox;
    RefPtr<CookiesModel> m_cookies_model;
    RefPtr<GUI::FilteringProxyModel> m_cookies_filtering_model;

    RefPtr<GUI::TableView> m_local_storage_table_view;
    RefPtr<GUI::TextBox> m_local_storage_textbox;
    RefPtr<LocalStorageModel> m_local_storage_model;
    RefPtr<GUI::FilteringProxyModel> m_local_storage_filtering_model;
};

}
