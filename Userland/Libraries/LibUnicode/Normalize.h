/*
 * Copyright (c) 2022, mat
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Forward.h>
#include <AK/Optional.h>
#include <AK/Span.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibUnicode/Forward.h>

namespace Unicode {

Optional<CodePointDecomposition const> code_point_decomposition(u32 code_point);
Optional<u32> code_point_composition(u32 first_code_point, u32 second_code_point);

enum class NormalizationForm {
    NFD,
    NFC,
    NFKD,
    NFKC
};

NormalizationForm normalization_form_from_string(StringView form);
StringView normalization_form_to_string(NormalizationForm form);

String normalize(StringView string, NormalizationForm form);

}
