/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "ProjectTemplatesModel.h"

#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <LibCore/DirIterator.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Variant.h>
#include <LibGfx/TextAlignment.h>
#include <ctype.h>
#include <stdio.h>

namespace HackStudio {

ProjectTemplatesModel::ProjectTemplatesModel()
    : m_templates()
    , m_mapping()
{
    auto watcher_or_error = Core::FileWatcher::watch(ProjectTemplate::templates_path());
    if (!watcher_or_error.is_error()) {
        m_file_watcher = watcher_or_error.release_value();
        m_file_watcher->on_change = [&](auto) {
            update();
        };
    } else {
        warnln("Unable to watch templates directory, templates will not automatically refresh. Error: {}", watcher_or_error.error());
    }

    rescan_templates();
}

ProjectTemplatesModel::~ProjectTemplatesModel()
{
}

int ProjectTemplatesModel::row_count(const GUI::ModelIndex&) const
{
    return m_mapping.size();
}

int ProjectTemplatesModel::column_count(const GUI::ModelIndex&) const
{
    return Column::__Count;
}

String ProjectTemplatesModel::column_name(int column) const
{
    switch (column) {
    case Column::Icon:
        return "Icon";
    case Column::Id:
        return "ID";
    case Column::Name:
        return "Name";
    }
    ASSERT_NOT_REACHED();
}

GUI::Variant ProjectTemplatesModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    if (static_cast<size_t>(index.row()) >= m_mapping.size())
        return {};

    if (role == GUI::ModelRole::TextAlignment)
        return Gfx::TextAlignment::CenterLeft;

    if (role == GUI::ModelRole::Display) {
        switch (index.column()) {
        case Column::Name:
            return m_mapping[index.row()]->name();
        case Column::Id:
            return m_mapping[index.row()]->id();
        }
    }

    if (role == GUI::ModelRole::Icon) {
        return m_mapping[index.row()]->icon();
    }

    return {};
}

RefPtr<ProjectTemplate> ProjectTemplatesModel::template_for_index(const GUI::ModelIndex& index)
{
    if (static_cast<size_t>(index.row()) >= m_mapping.size())
        return {};

    return m_mapping[index.row()];
}

void ProjectTemplatesModel::update()
{
    rescan_templates();
    did_update();
}

void ProjectTemplatesModel::rescan_templates()
{
    m_templates.clear();

    // Iterate over template manifest INI files in the templates path
    Core::DirIterator di(ProjectTemplate::templates_path(), Core::DirIterator::SkipDots);
    if (di.has_error()) {
        warnln("DirIterator: {}", di.error_string());
        return;
    }

    while (di.has_next()) {
        auto full_path = LexicalPath(di.next_full_path());
        if (!full_path.has_extension(".ini"))
            continue;

        auto project_template = ProjectTemplate::load_from_manifest(full_path.string());
        if (!project_template) {
            warnln("Template manifest {} is invalid.", full_path.string());
            continue;
        }

        m_templates.append(project_template.release_nonnull());
    }

    // Enumerate the loaded projects into a sorted mapping, by priority value descending.
    m_mapping.clear();
    for (auto& project_template : m_templates)
        m_mapping.append(&project_template);
    quick_sort(m_mapping, [](auto a, auto b) {
        return a->priority() > b->priority();
    });
}

}
