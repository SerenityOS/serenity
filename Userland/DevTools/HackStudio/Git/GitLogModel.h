/*
 * Copyright (c) 2023, Abhishek R. <raturiabhi1000@gmail.com>
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "GitFilesView.h"
#include "GitRepo.h"

namespace HackStudio {

class GitLogModel final : public GUI::Model {
public:
    static NonnullRefPtr<GitLogModel> create(Vector<DeprecatedString>&& logs);

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return m_logs.size(); }
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return 1; }

    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;

    virtual GUI::ModelIndex index(int row, int column, const GUI::ModelIndex&) const override;

private:
    explicit GitLogModel(Vector<DeprecatedString>&& logs);
    Vector<DeprecatedString> m_logs;
};

}
