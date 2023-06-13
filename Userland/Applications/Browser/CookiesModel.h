/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tab.h"
#include <AK/Vector.h>
#include <LibGUI/Model.h>
#include <LibGUI/Widget.h>
#include <LibWeb/Cookie/Cookie.h>

namespace Browser {

class CookiesModel final : public GUI::Model {
public:
    enum Column {
        Domain,
        Path,
        Name,
        Value,
        ExpiryTime,
        SameSite,
        __Count,
    };

    void set_items(AK::Vector<Web::Cookie::Cookie> items);
    void clear_items();
    virtual int row_count(GUI::ModelIndex const&) const override;
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return Column::__Count; }
    virtual ErrorOr<String> column_name(int) const override;
    virtual GUI::ModelIndex index(int row, int column = 0, GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role = GUI::ModelRole::Display) const override;
    virtual GUI::Model::MatchResult data_matches(GUI::ModelIndex const& index, GUI::Variant const& term) const override;

    Web::Cookie::Cookie take_cookie(GUI::ModelIndex const&);
    AK::Vector<Web::Cookie::Cookie> take_all_cookies();

private:
    AK::Vector<Web::Cookie::Cookie> m_cookies;
};

}
