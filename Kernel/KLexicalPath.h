/*
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>

namespace Kernel::KLexicalPath {

bool is_absolute(StringView const&);
bool is_canonical(StringView const&);
StringView basename(StringView const&);
StringView dirname(StringView const&);
Vector<StringView> parts(StringView const&);

}
