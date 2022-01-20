/*
 * Copyright (c) 2022, Kenneth Myhra <kennethmyhra@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/ScopeGuard.h>
#include <LibCore/Group.h>
#include <LibCore/System.h>

namespace Core {

#ifndef AK_OS_BSD_GENERIC
ErrorOr<void> Group::add_group(Group& group)
{
    if (group.name().is_empty())
        return Error::from_string_literal("Group name can not be empty.");

    // A quick sanity check on group name
    if (strpbrk(group.name().characters(), "\\/!@#$%^&*()~+=`:\n"))
        return Error::from_string_literal("Group name has invalid characters.");

    // Disallow names starting with '_', '-' or other non-alpha characters.
    if (group.name().starts_with('_') || group.name().starts_with('-') || !is_ascii_alpha(group.name().characters()[0]))
        return Error::from_string_literal("Group name has invalid characters.");

    // Verify group name does not already exist
    if (TRY(name_exists(group.name())))
        return Error::from_string_literal("Group name already exists.");

    // Sort out the group id for the group
    if (group.id() > 0) {
        if (TRY(id_exists(group.id())))
            return Error::from_string_literal("Group ID already exists.");
    } else {
        gid_t group_id = 100;
        while (true) {
            if (!TRY(id_exists(group_id)))
                break;
            group_id++;
        }
        group.set_group_id(group_id);
    }

    auto gr = TRY(group.to_libc_group());

    FILE* file = fopen("/etc/group", "a");
    if (!file)
        return Error::from_errno(errno);

    ScopeGuard file_guard { [&] {
        fclose(file);
    } };

    if (putgrent(&gr, file) < 0)
        return Error::from_errno(errno);

    return {};
}
#endif

Group::Group(String name, gid_t id, Vector<String> members)
    : m_name(move(name))
    , m_id(id)
    , m_members(move(members))
{
}

ErrorOr<bool> Group::name_exists(StringView name)
{
    return TRY(Core::System::getgrnam(name)).has_value();
}

ErrorOr<bool> Group::id_exists(gid_t id)
{
    return TRY(Core::System::getgrgid(id)).has_value();
}

// NOTE: struct group returned from this function cannot outlive an instance of Group.
ErrorOr<struct group> Group::to_libc_group()
{
    struct group gr;
    gr.gr_name = const_cast<char*>(m_name.characters());
    gr.gr_passwd = const_cast<char*>("x");
    gr.gr_gid = m_id;
    gr.gr_mem = nullptr;

    // FIXME: A better solution would surely be not using a static here
    // NOTE: This now means that there cannot be multiple struct groups at the same time, because only one gr.gr_mem can ever be valid at the same time.
    // NOTE: Not using a static here would result in gr.gr_mem being freed up on exit from this function.
    static Vector<char*> members;
    members.clear_with_capacity();
    if (m_members.size() > 0) {
        TRY(members.try_ensure_capacity(m_members.size() + 1));
        for (auto member : m_members)
            members.unchecked_append(const_cast<char*>(member.characters()));
        members.unchecked_append(nullptr);

        gr.gr_mem = const_cast<char**>(members.data());
    }

    return gr;
}

}
