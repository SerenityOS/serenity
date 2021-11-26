/*
 * Copyright (c) 2021, Mahmoud Mandour <ma.mandourr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ConfigFile.h>
#include <LibCore/Version.h>

namespace Core::Version {

String read_long_version_string()
{
    auto version_config = Core::ConfigFile::open("/res/version.ini");
    auto major_version = version_config->read_entry("Version", "Major", "0");
    auto minor_version = version_config->read_entry("Version", "Minor", "0");

    StringBuilder builder;
    builder.appendff("Version {}.{}", major_version, minor_version);
    if (auto git_version = version_config->read_entry("Version", "Git", ""); git_version != "")
        builder.appendff(".g{}", git_version);
    return builder.to_string();
}

}
