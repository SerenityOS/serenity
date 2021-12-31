/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "GitRepo.h"
#include <AK/NonnullRefPtr.h>
#include <LibGUI/Model.h>

namespace HackStudio {

class GitFilesModel final : public GUI::Model {
public:
    static NonnullRefPtr<GitFilesModel> create(Vector<String>&& files);

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return m_files.size(); }
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return 1; }

    virtual String column_name(int) const override
    {
        return "";
    }

    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;

    virtual GUI::ModelIndex index(int row, int column, const GUI::ModelIndex&) const override;

private:
    explicit GitFilesModel(Vector<String>&& files);
    Vector<String> m_files;
};
}
