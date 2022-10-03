/*
 * Copyright (c) 2022, mat
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Optional.h>
#include <AK/Span.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibUnicode/Forward.h>

namespace Unicode {

Optional<CodePointDecomposition const&> code_point_decomposition(u32 code_point);
Span<CodePointDecomposition const> code_point_decompositions();

enum class NormalizationForm {
    NFD,
    NFC,
    NFKD,
    NFKC
};

[[nodiscard]] String normalize(StringView string, NormalizationForm form);

}
