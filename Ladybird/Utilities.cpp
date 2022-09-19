/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#define AK_DONT_REPLACE_STD

#include "Utilities.h"

AK::String akstring_from_qstring(QString const& qstring)
{
    return AK::String(qstring.toUtf8().data());
}

QString qstring_from_akstring(AK::String const& akstring)
{
    return QString::fromUtf8(akstring.characters(), akstring.length());
}
