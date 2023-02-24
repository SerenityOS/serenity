/*
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <Kernel/Library/KString.h>

namespace Kernel::KLexicalPath {

bool is_absolute(StringView);
bool is_canonical(StringView);
StringView basename(StringView);
StringView dirname(StringView);
Vector<StringView> parts(StringView);

ErrorOr<NonnullOwnPtr<KString>> try_join(StringView, StringView);

}
