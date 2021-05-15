/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>

namespace AK {

String urlencode(const StringView&, const StringView& exclude = {});
String urldecode(const StringView&);

}

using AK::urldecode;
using AK::urlencode;
