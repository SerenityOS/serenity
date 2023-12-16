/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Vector.h>
#include <pwd.h>

struct PasswdEntry {
    ByteString name;
    uid_t uid {};
};

static Vector<PasswdEntry> get_all_passwd_entries()
{
    Vector<PasswdEntry> entries;
    setpwent();
    while (auto* pwd = getpwent()) {
        entries.append(PasswdEntry {
            .name = pwd->pw_name,
            .uid = pwd->pw_uid,
        });
    }
    return entries;
}

TEST_CASE(getpwuid_r)
{
    // Verify that all known passwd entries can be found with getpwuid_r()
    for (auto const& entry : get_all_passwd_entries()) {
        struct passwd pwd_buffer = {};
        char buffer[4096] = {};
        struct passwd* result = nullptr;
        auto rc = getpwuid_r(entry.uid, &pwd_buffer, buffer, sizeof(buffer), &result);
        EXPECT_EQ(rc, 0);
        EXPECT_EQ(entry.uid, result->pw_uid);
        EXPECT_EQ(entry.name, result->pw_name);
    }

    // Verify that a bogus UID can't be found with getpwuid_r()
    {
        struct passwd pwd_buffer = {};
        char buffer[4096] = {};
        struct passwd* result = nullptr;
        auto rc = getpwuid_r(99991999, &pwd_buffer, buffer, sizeof(buffer), &result);
        EXPECT_EQ(rc, ENOENT);
        EXPECT_EQ(result, nullptr);
    }

    // Verify that two calls to getpwuid_r() don't clobber each other.
    {
        struct passwd pwd_buffer1 = {};
        char buffer1[4096] = {};
        struct passwd* result1 = nullptr;
        auto rc1 = getpwuid_r(0, &pwd_buffer1, buffer1, sizeof(buffer1), &result1);
        EXPECT_EQ(rc1, 0);
        EXPECT_NE(result1, nullptr);
        EXPECT_EQ(result1, &pwd_buffer1);

        struct passwd pwd_buffer2 = {};
        char buffer2[4096] = {};
        struct passwd* result2 = nullptr;
        auto rc2 = getpwuid_r(0, &pwd_buffer2, buffer2, sizeof(buffer2), &result2);
        EXPECT_EQ(rc2, 0);
        EXPECT_NE(result2, nullptr);
        EXPECT_EQ(result2, &pwd_buffer2);

        EXPECT_NE(result1, result2);
    }
}

TEST_CASE(getpwnam_r)
{
    // Verify that all known passwd entries can be found with getpwnam_r()
    for (auto const& entry : get_all_passwd_entries()) {
        struct passwd pwd_buffer = {};
        char buffer[4096] = {};
        struct passwd* result = nullptr;
        auto rc = getpwnam_r(entry.name.characters(), &pwd_buffer, buffer, sizeof(buffer), &result);
        EXPECT_EQ(rc, 0);
        EXPECT_EQ(entry.uid, result->pw_uid);
        EXPECT_EQ(entry.name, result->pw_name);
    }

    // Verify that a bogus name can't be found with getpwnam_r()
    {
        struct passwd pwd_buffer = {};
        char buffer[4096] = {};
        struct passwd* result = nullptr;
        auto rc = getpwnam_r("99991999", &pwd_buffer, buffer, sizeof(buffer), &result);
        EXPECT_EQ(rc, ENOENT);
        EXPECT_EQ(result, nullptr);
    }

    // Verify that two calls to getpwnam_r() don't clobber each other.
    {
        struct passwd pwd_buffer1 = {};
        char buffer1[4096] = {};
        struct passwd* result1 = nullptr;
        auto rc1 = getpwnam_r("root", &pwd_buffer1, buffer1, sizeof(buffer1), &result1);
        EXPECT_EQ(rc1, 0);
        EXPECT_NE(result1, nullptr);
        EXPECT_EQ(result1, &pwd_buffer1);

        struct passwd pwd_buffer2 = {};
        char buffer2[4096] = {};
        struct passwd* result2 = nullptr;
        auto rc2 = getpwnam_r("root", &pwd_buffer2, buffer2, sizeof(buffer2), &result2);
        EXPECT_EQ(rc2, 0);
        EXPECT_NE(result2, nullptr);
        EXPECT_EQ(result2, &pwd_buffer2);

        EXPECT_NE(result1, result2);
    }
}
