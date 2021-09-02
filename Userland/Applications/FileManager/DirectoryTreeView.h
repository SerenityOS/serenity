/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DirectoryView.h"
#include <LibGUI/TreeView.h>

namespace FileManager {

class DirectoryTreeView final
    : public GUI::TreeView {
    C_OBJECT(DirectoryTreeView)

public:
    void set_view(RefPtr<DirectoryView> directory_view) { m_directory_view = directory_view; }

private:
    // ^GUI::ModelClient
    virtual void model_did_update(unsigned) override;

    RefPtr<DirectoryView> m_directory_view;
};

}
