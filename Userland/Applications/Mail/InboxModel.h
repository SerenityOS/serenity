/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Model.h>
#include <LibIMAP/Objects.h>

enum class MailStatus {
    Unseen,
    Seen,
};

struct InboxEntry {
    u32 sequence_number;
    ByteString date;
    ByteString from;
    ByteString subject;
    MailStatus status;
};

enum class InboxModelCustomRole {
    __DONOTUSE = (int)GUI::ModelRole::Custom,
    Sequence,
};

class InboxModel final : public GUI::Model {
public:
    enum Column {
        Date,
        From,
        Subject,
        __Count
    };

    static NonnullRefPtr<InboxModel> create(Vector<InboxEntry> inbox_entries)
    {
        return adopt_ref(*new InboxModel(move(inbox_entries)));
    }

    virtual ~InboxModel() override = default;

    MailStatus mail_status(int row);
    void set_mail_status(int row, MailStatus status);

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return Column::__Count; }
    virtual ErrorOr<String> column_name(int) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;

private:
    InboxModel(Vector<InboxEntry>);

    Vector<InboxEntry> m_entries;
};
