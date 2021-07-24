/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Model.h>
#include <LibIMAP/Objects.h>

struct InboxEntry {
    String from;
    String subject;
};

class InboxModel final : public GUI::Model {
public:
    enum Column {
        From,
        Subject,
        __Count
    };

    static NonnullRefPtr<InboxModel> create(Vector<InboxEntry> inbox_entries)
    {
        return adopt_ref(*new InboxModel(move(inbox_entries)));
    }

    virtual ~InboxModel() override;

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return Column::__Count; }
    virtual String column_name(int) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;
    virtual void update() override;

private:
    InboxModel(Vector<InboxEntry>);

    Vector<InboxEntry> m_entries;
};
