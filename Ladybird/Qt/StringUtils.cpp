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

ErrorOr<String> ak_string_from_qstring(QString const& qstring)
{
    return String::from_utf8(StringView(qstring.toUtf8().data(), qstring.size()));
}

QString qstring_from_ak_deprecated_string(AK::DeprecatedString const& ak_deprecated_string)
{
    return QString::fromUtf8(ak_deprecated_string.characters(), ak_deprecated_string.length());
}

QString qstring_from_ak_string(String const& ak_string)
{
    auto view = ak_string.bytes_as_string_view();
    return QString::fromUtf8(view.characters_without_null_termination(), view.length());
}
