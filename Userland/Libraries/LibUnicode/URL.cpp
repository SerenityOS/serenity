/*
 * Copyright (c) 2023, Simon Wanner <simon@skyrising.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibUnicode/IDNA.h>
#include <LibUnicode/URL.h>

namespace Unicode {

// https://url.spec.whatwg.org/#concept-domain-to-ascii
static ErrorOr<String> domain_to_ascii(StringView domain, bool be_strict)
{
    // 1. Let result be the result of running Unicode ToASCII with domain_name set to domain, UseSTD3ASCIIRules set to beStrict, CheckHyphens set to false, CheckBidi set to true, CheckJoiners set to true, Transitional_Processing set to false, and VerifyDnsLength set to beStrict. [UTS46]
    // 2. If result is a failure value, domain-to-ASCII validation error, return failure.
    Unicode::IDNA::ToAsciiOptions const options {
        Unicode::IDNA::CheckHyphens::No,
        Unicode::IDNA::CheckBidi::Yes,
        Unicode::IDNA::CheckJoiners::Yes,
        be_strict ? Unicode::IDNA::UseStd3AsciiRules::Yes : Unicode::IDNA::UseStd3AsciiRules::No,
        Unicode::IDNA::TransitionalProcessing::No,
        be_strict ? Unicode::IDNA::VerifyDnsLength::Yes : Unicode::IDNA::VerifyDnsLength::No
    };
    auto result = TRY(Unicode::IDNA::to_ascii(Utf8View(domain), options));

    // 3. If result is the empty string, domain-to-ASCII validation error, return failure.
    if (result.is_empty())
        return Error::from_string_literal("Empty domain");

    // 4. Return result.
    return result;
}

// https://url.spec.whatwg.org/#concept-host-parser
ErrorOr<URL::URL> create_unicode_url(String const& url_string)
{
    // NOTE: 1.-4. are implemented in URL::Parser::parse_host

    URL::URL url = url_string;
    if (!url.is_valid() || !url.host().has<String>())
        return url;

    auto& domain = url.host().get<String>();
    if (domain.is_empty())
        return url;

    // 5. Let asciiDomain be the result of running domain to ASCII with domain and false.
    // 6. If asciiDomain is failure, then return failure.
    auto ascii_domain = TRY(domain_to_ascii(domain.bytes_as_string_view(), false));

    // FIXME: Reimplement 7. or call into URL::Parser::parse_host using ascii_domain (8. & 9. do not apply)
    url.set_host(ascii_domain);
    return url;
}

}
