/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProjectTemplatesModel.h"

#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <LibCore/DirIterator.h>
#include <LibGUI/Variant.h>
#include <LibGfx/TextAlignment.h>
#include <stdio.h>

namespace HackStudio {

ProjectTemplatesModel::ProjectTemplatesModel()
    : m_templates()
    , m_mapping()
{
    auto watcher_or_error = Core::FileWatcher::create();
    if (!watcher_or_error.is_error()) {
        m_file_watcher = watcher_or_error.release_value();
        m_file_watcher->on_change = [&](auto) {
            invalidate();
        };

        auto watch_result = m_file_watcher->add_watch(
            ProjectTemplate::templates_path(),
            Core::FileWatcherEvent::Type::ChildCreated
                | Core::FileWatcherEvent::Type::ChildDeleted);

        if (watch_result.is_error()) {
            warnln("Unable to watch templates directory, templates will not automatically refresh. Error: {}", watch_result.error());
        }
    } else {
        warnln("Unable to watch templates directory, templates will not automatically refresh. Error: {}", watcher_or_error.error());
    }

    rescan_templates();
}

int ProjectTemplatesModel::row_count(const GUI::ModelIndex&) const
{
    return m_mapping.size();
}

int ProjectTemplatesModel::column_count(const GUI::ModelIndex&) const
{
    return Column::__Count;
}

ErrorOr<String> ProjectTemplatesModel::column_name(int column) const
{
    switch (column) {
    case Column::Icon:
        return "Icon"_string;
    case Column::Id:
        return "ID"_string;
    case Column::Name:
        return "Name"_string;
    }
    VERIFY_NOT_REACHED();
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
        warnln("DirIterator: {}", di.error());
        return;
    }

    while (di.has_next()) {
        auto full_path = LexicalPath(di.next_full_path());
        if (!full_path.has_extension(".ini"sv))
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
        m_mapping.append(project_template);
    quick_sort(m_mapping, [](auto a, auto b) {
        return a->priority() > b->priority();
    });
}

}
