/*
 * Copyright (c) 2018-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/HashTable.h>
#include <LibTest/Macros.h>
#include <LibTest/TestCase.h>
#include <grp.h>

void check_correctness(struct group* gr);

void check_correctness(struct group* gr)
{
    EXPECT_NE(gr, nullptr);
    EXPECT_EQ(gr->gr_gid, (gid_t)3);
    EXPECT_EQ(gr->gr_name, "phys"sv);
    EXPECT_EQ(gr->gr_passwd, "x"sv);

    HashTable<ByteString> members;
    for (char** mem = gr->gr_mem; *mem; ++mem) {
        members.set(ByteString(*mem));
    }

    EXPECT_EQ(true, members.contains("window"sv));
    EXPECT_EQ(true, members.contains("anon"sv));
}

TEST_CASE(getgrid_returns_correct_value)
{
    // From Base/etc/group
    // phys:x:3:window,anon
    struct group* gr = getgrgid(3);

    check_correctness(gr);

    gr = getgrgid(99999);
    EXPECT_EQ(gr, nullptr);
}

TEST_CASE(getgrid_r_uses_provided_buffer)
{
    // From Base/etc/group
    // phys:x:3:window,anon

    struct group g;
    struct group* res;
    AK::Array<char, 1024> buffer;

    setgrent();
    int rc = getgrgid_r(3, &g, buffer.data(), buffer.size(), &res);
    endgrent();
    EXPECT_EQ(rc, 0);
    check_correctness(&g);
    EXPECT_EQ(res, &g);

    auto is_pointer_in_range = [&buffer](void* ptr) {
        return (buffer.data() <= ptr) && (ptr < buffer.data() + buffer.size());
    };

    EXPECT(is_pointer_in_range(g.gr_mem));
    EXPECT(is_pointer_in_range(g.gr_name));

    char** mem = g.gr_mem;
    for (; *mem; ++mem) {
        EXPECT(is_pointer_in_range(mem));
        EXPECT(is_pointer_in_range(*mem));
    }
    EXPECT(is_pointer_in_range(mem));
}

TEST_CASE(getgrname_r_uses_provided_buffer)
{
    // From Base/etc/group
    // phys:x:3:window,anon

    struct group g;
    struct group* res;
    AK::Array<char, 1024> buffer;
    setgrent();
    int rc = getgrnam_r("phys", &g, buffer.data(), buffer.size(), &res);
    endgrent();
    EXPECT_EQ(rc, 0);
    check_correctness(&g);
    EXPECT_EQ(res, &g);

    auto is_pointer_in_range = [&buffer](void* ptr) {
        return (buffer.data() <= ptr) && (ptr < buffer.data() + buffer.size());
    };

    EXPECT(is_pointer_in_range(g.gr_mem));
    EXPECT(is_pointer_in_range(g.gr_name));

    char** mem = g.gr_mem;
    for (; *mem; ++mem) {
        EXPECT(is_pointer_in_range(mem));
        EXPECT(is_pointer_in_range(*mem));
    }
    EXPECT(is_pointer_in_range(mem));
}
