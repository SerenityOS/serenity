/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <QString>

AK::DeprecatedString ak_deprecated_string_from_qstring(QString const&);
QString qstring_from_ak_deprecated_string(AK::DeprecatedString const&);
void platform_init();

extern DeprecatedString s_serenity_resource_root;
