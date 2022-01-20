/*
 * Copyright (c) 2022, Kenneth Myhra <kennethmyhra@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <grp.h>

namespace Core {

class Group {
public:
#ifndef AK_OS_BSD_GENERIC
    static ErrorOr<void> add_group(Group& group);
#endif

    Group() = default;
    Group(String name, gid_t id = 0, Vector<String> members = {});

    ~Group() = default;

    String const& name() const { return m_name; }
    void set_name(String const& name) { m_name = name; }

    gid_t id() const { return m_id; }
    void set_group_id(gid_t const id) { m_id = id; }

    Vector<String>& members() { return m_members; }

private:
    static ErrorOr<bool> name_exists(StringView name);
    static ErrorOr<bool> id_exists(gid_t id);
    ErrorOr<struct group> to_libc_group();

    String m_name;
    gid_t m_id { 0 };
    Vector<String> m_members;
};

}
