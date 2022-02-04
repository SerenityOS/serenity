/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibGUI/GML/AST.h>

namespace GUI::GML {

ErrorOr<NonnullRefPtr<GMLFile>> parse_gml(StringView);

}
