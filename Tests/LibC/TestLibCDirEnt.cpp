/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <dirent.h>
#include <string.h>

TEST_CASE(scandir_basic_scenario)
{
    struct dirent** namelist = nullptr;
    auto entries = scandir("/etc", &namelist, nullptr, nullptr);
    EXPECT(entries > 0);
    EXPECT_NE(namelist, nullptr);

    bool found_passwd = false;
    for (auto i = 0; i < entries; i++) {
        if (strcmp(namelist[i]->d_name, "passwd") == 0) {
            found_passwd = true;
        }
        free(namelist[i]);
    }
    EXPECT(found_passwd);
    free(namelist);
}
