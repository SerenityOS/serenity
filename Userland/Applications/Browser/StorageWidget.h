/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "CookiesModel.h"
#include "Tab.h"
#include <LibGUI/Widget.h>
#include <LibWeb/Cookie/Cookie.h>

namespace Browser {

class StorageWidget final : public GUI::Widget {
    C_OBJECT(StorageWidget);

public:
    virtual ~StorageWidget() override = default;
    void add_cookie(Web::Cookie::Cookie const& cookie);
    void clear_cookies();

private:
    StorageWidget();

    RefPtr<GUI::TableView> m_cookies_table_view;
    RefPtr<CookiesModel> m_cookies_model;
};

}
