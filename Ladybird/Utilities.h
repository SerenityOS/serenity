/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <QString>

AK::String akstring_from_qstring(QString const&);
QString qstring_from_akstring(AK::String const&);
void platform_init();

extern String s_serenity_resource_root;
