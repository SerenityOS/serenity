/*
 * Copyright (c) 2022, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Error.h>
#include <AK/Vector.h>
#include <grp.h>

namespace Core {

class Group {
public:
#if !defined(AK_OS_BSD_GENERIC) && !defined(AK_OS_HAIKU)
    static ErrorOr<void> add_group(Group& group);
#endif

    static ErrorOr<Vector<Group>> all();

    Group() = default;
    Group(ByteString name, gid_t id = 0, Vector<ByteString> members = {});

    ~Group() = default;

    ByteString const& name() const { return m_name; }
    void set_name(ByteString const& name) { m_name = name; }

    gid_t id() const { return m_id; }
    void set_group_id(gid_t const id) { m_id = id; }

    Vector<ByteString>& members() { return m_members; }

    ErrorOr<void> sync();

private:
    static ErrorOr<bool> name_exists(StringView name);
    static ErrorOr<bool> id_exists(gid_t id);
    ErrorOr<struct group> to_libc_group();

    ErrorOr<ByteString> generate_group_file() const;

    ByteString m_name;
    gid_t m_id { 0 };
    Vector<ByteString> m_members;
};

}
