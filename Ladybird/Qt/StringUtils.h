/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Error.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <QString>

AK::ByteString ak_byte_string_from_qstring(QString const&);
String ak_string_from_qstring(QString const&);
QString qstring_from_ak_string(StringView);
