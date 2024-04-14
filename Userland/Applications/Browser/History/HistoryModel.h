/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Vector.h>
#include <LibGUI/Model.h>
#include <LibGUI/Widget.h>
#include <LibURL/URL.h>

namespace Browser {

// FIXME: Reimplement viewing history entries using WebContent's history.
struct URLTitlePair {
    URL::URL url;
    ByteString title;
};

class HistoryModel final : public GUI::Model {
public:
    enum Column {
        Title,
        URL,
        __Count,
    };

    void set_items(Vector<URLTitlePair> items);
    void clear_items();
    virtual int row_count(GUI::ModelIndex const&) const override;
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return Column::__Count; }
    virtual ErrorOr<String> column_name(int) const override;
    virtual GUI::ModelIndex index(int row, int column = 0, GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role = GUI::ModelRole::Display) const override;
    virtual GUI::Model::MatchResult data_matches(GUI::ModelIndex const& index, GUI::Variant const& term) const override;

private:
    Vector<URLTitlePair> m_entries;
};

}
