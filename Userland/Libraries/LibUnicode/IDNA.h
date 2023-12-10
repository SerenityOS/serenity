/*
 * Copyright (c) 2023, Simon Wanner <simon@skyrising.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Utf32View.h>
#include <AK/Utf8View.h>

namespace Unicode::IDNA {

enum class MappingStatus : u8 {
    Valid,
    Ignored,
    Mapped,
    Deviation,
    Disallowed,
    DisallowedStd3Valid,
    DisallowedStd3Mapped,
};

enum class IDNA2008Status : u8 {
    NV8,
    XV8,
};

struct Mapping {
    MappingStatus status;
    IDNA2008Status idna_2008_status;
    Utf32View mapped_to;
};

enum class CheckHyphens {
    No,
    Yes,
};

enum class CheckBidi {
    No,
    Yes,
};

enum class CheckJoiners {
    No,
    Yes,
};

enum class UseStd3AsciiRules {
    No,
    Yes,
};

enum class TransitionalProcessing {
    No,
    Yes,
};

enum class VerifyDnsLength {
    No,
    Yes,
};

struct ToAsciiOptions {
    CheckHyphens check_hyphens { CheckHyphens::Yes };
    CheckBidi check_bidi { CheckBidi::Yes };
    CheckJoiners check_joiners { CheckJoiners::Yes };
    UseStd3AsciiRules use_std3_ascii_rules { UseStd3AsciiRules::No };
    TransitionalProcessing transitional_processing { TransitionalProcessing::No };
    VerifyDnsLength verify_dns_length { VerifyDnsLength::Yes };
};

ErrorOr<String> to_ascii(Utf8View domain_name, ToAsciiOptions const& = {});
Optional<Mapping> get_idna_mapping(u32 code_point);

}
