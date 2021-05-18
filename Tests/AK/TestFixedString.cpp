/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FixedString.h>
#include <AK/StringView.h>

using namespace AK;

static_assert(sizeof("") == sizeof(""_fs));
static_assert(sizeof("SerenityOS") == sizeof("SerenityOS"_fs));
static_assert(AK::StringView { "SerenityOS" } == AK::StringView { "SerenityOS"_fs });

static_assert(sizeof("SerenityOS") == sizeof("Serenity"_fs + "OS"_fs));
static_assert(sizeof("OS") == sizeof(""_fs + "OS"_fs));
static_assert(sizeof("Serenity") == sizeof("Serenity"_fs + ""_fs));
