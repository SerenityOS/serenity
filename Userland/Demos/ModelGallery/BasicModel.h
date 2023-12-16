/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Vector.h>
#include <LibGUI/Model.h>

class BasicModel final : public GUI::Model {
public:
    static NonnullRefPtr<BasicModel> create()
    {
        return adopt_ref(*new BasicModel());
    }

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return m_items.size(); }
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return 1; }
    virtual ErrorOr<String> column_name(int) const override { return "Item"_string; }

    virtual GUI::Variant data(GUI::ModelIndex const&, GUI::ModelRole = GUI::ModelRole::Display) const override;
    virtual GUI::Model::MatchResult data_matches(GUI::ModelIndex const&, GUI::Variant const&) const override;
    virtual void invalidate() override;
    virtual GUI::ModelIndex index(int row, int column = 0, GUI::ModelIndex const& parent = GUI::ModelIndex()) const override;

    Function<void()> on_invalidate;

    void add_item(ByteString const& item);
    void remove_item(GUI::ModelIndex const&);

private:
    BasicModel()
    {
    }

    Vector<ByteString> m_items;
};
