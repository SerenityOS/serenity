/*
 * Copyright (c) 2023, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Firmware/ACPI/AML/Namespace.h>

namespace Kernel::ACPI::AML {

ErrorOr<void> Namespace::add_level(Vector<StringView> const& path)
{
    auto* current_level = &m_root_level;
    for (auto path_part : path) {
        auto next_level = current_level->sub_levels.find(path_part);
        if (next_level == current_level->sub_levels.end()) {
            auto level_name = TRY(KString::try_create(path_part));
            TRY(current_level->sub_levels.try_set(move(level_name), {}));
            next_level = current_level->sub_levels.find(path_part);
        }
        current_level = &next_level->value;
    }
    return {};
}

ErrorOr<void> Namespace::insert_node(Vector<StringView> const& path, NonnullRefPtr<ASTNode> node)
{
    auto* target_level = &m_root_level;
    for (auto i = 0u; i < path.size() - 1; ++i) {
        auto next_level = target_level->sub_levels.find(path[i]);
        if (next_level == target_level->sub_levels.end()) {
            auto level_name = TRY(KString::try_create(path[i]));
            TRY(target_level->sub_levels.try_set(move(level_name), {}));
            next_level = target_level->sub_levels.find(path[i]);
        }
        target_level = &next_level->value;
    }
    auto object_name = TRY(KString::try_create(path.last()));
    auto result = TRY(target_level->objects.try_set(move(object_name), move(node)));
    if (result != HashSetResult::InsertedNewEntry) {
        dbgln("AML Error: Duplicate object definition at path {}", path);
        return EEXIST;
    }
    return {};
}

ErrorOr<NonnullRefPtr<ASTNode>> Namespace::get_node(Vector<StringView> const& path) const
{
    auto const* target_level = &m_root_level;
    for (auto i = 0u; i < path.size() - 1; ++i) {
        auto next_level = target_level->sub_levels.find(path[i]);
        if (next_level == target_level->sub_levels.end()) {
            dbgln("AML Error: Path {} references non-existent level {}", path, path[i]);
            return ENOENT;
        }
        target_level = &next_level->value;
    }
    auto object = target_level->objects.find(path.last());
    if (object == target_level->objects.end()) {
        dbgln("AML Error: Path {} references non-existent object {}", path, path.last());
        return ENOENT;
    }
    return object->value;
}

ErrorOr<NonnullRefPtr<ASTNode>> Namespace::search_node(Vector<StringView> const& path, StringView name) const
{
    Vector<Level const&> path_levels;
    TRY(path_levels.try_append(m_root_level));

    auto const* current_level = &m_root_level;
    for (auto path_part : path) {
        auto next_level = current_level->sub_levels.find(path_part);
        if (next_level == current_level->sub_levels.end()) {
            dbgln("AML Error: Path {} references non-existent level {}", path, path_part);
            return ENOENT;
        }
        current_level = &next_level->value;
        TRY(path_levels.try_append(*current_level));
    }

    for (ssize_t i = path_levels.size() - 1; i >= 0; --i) {
        auto object = path_levels[i].objects.find(name);
        if (object == path_levels[i].objects.end())
            continue;
        return object->value;
    }
    dbgln("AML Error: Path {} references non-existent object {}", path, name);
    return ENOENT;
}

bool Namespace::contains_node(Vector<StringView> const& path) const
{
    auto const* target_level = &m_root_level;
    for (auto i = 0u; i < path.size() - 1; ++i) {
        auto next_level = target_level->sub_levels.find(path[i]);
        if (next_level == target_level->sub_levels.end())
            return false;
        target_level = &next_level->value;
    }
    auto object = target_level->objects.find(path.last());
    return object != target_level->objects.end();
}

}
