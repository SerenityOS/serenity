/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibUnicode/Forward.h>

namespace Unicode::Detail {

struct Symbols {
    static Symbols const& ensure_loaded();

    // Loaded from UnicodeData.cpp:

    Optional<String> (*code_point_display_name)(u32) { nullptr };

    u32 (*canonical_combining_class)(u32 code_point) { nullptr };

    u32 (*simple_uppercase_mapping)(u32) { nullptr };
    u32 (*simple_lowercase_mapping)(u32) { nullptr };
    Span<SpecialCasing const* const> (*special_case_mapping)(u32 code_point) { nullptr };

    Optional<GeneralCategory> (*general_category_from_string)(StringView) { nullptr };
    bool (*code_point_has_general_category)(u32, GeneralCategory) { nullptr };

    Optional<Property> (*property_from_string)(StringView) { nullptr };
    bool (*code_point_has_property)(u32, Property) { nullptr };

    Optional<Script> (*script_from_string)(StringView) { nullptr };
    bool (*code_point_has_script)(u32, Script) { nullptr };
    bool (*code_point_has_script_extension)(u32, Script) { nullptr };

private:
    Symbols() = default;
};

}
