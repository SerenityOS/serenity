/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/WeakPtr.h>
#include <DevTools/HackStudio/ProjectTemplate.h>
#include <LibCore/FileWatcher.h>
#include <LibGUI/Model.h>

namespace HackStudio {

class ProjectTemplatesModel final : public GUI::Model {
public:
    static NonnullRefPtr<ProjectTemplatesModel> create()
    {
        return adopt_ref(*new ProjectTemplatesModel());
    }

    enum Column {
        Icon = 0,
        Id,
        Name,
        __Count
    };

    virtual ~ProjectTemplatesModel() override = default;

    RefPtr<ProjectTemplate> template_for_index(const GUI::ModelIndex& index);

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual ErrorOr<String> column_name(int) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;

    void update();
    void rescan_templates();

private:
    explicit ProjectTemplatesModel();

    Vector<NonnullRefPtr<ProjectTemplate>> m_templates;
    Vector<ProjectTemplate*> m_mapping;

    RefPtr<Core::FileWatcher> m_file_watcher;
};

}
