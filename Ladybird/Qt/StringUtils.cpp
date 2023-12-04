/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "StringUtils.h"

AK::DeprecatedString ak_deprecated_string_from_qstring(QString const& qstring)
{
    return AK::DeprecatedString(qstring.toUtf8().data());
}

String ak_string_from_qstring(QString const& qstring)
{
    auto utf8_data = qstring.toUtf8();
    return MUST(String::from_utf8(StringView(utf8_data.data(), utf8_data.size())));
}

QString qstring_from_ak_string(StringView ak_string)
{
    return QString::fromUtf8(ak_string.characters_without_null_termination(), static_cast<qsizetype>(ak_string.length()));
}
