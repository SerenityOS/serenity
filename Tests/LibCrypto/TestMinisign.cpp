/*
 * Copyright (c) 2025, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/MemoryStream.h>
#include <AK/StringView.h>
#include <LibCore/SecretString.h>
#include <LibCrypto/Minisign.h>
#include <LibTest/Macros.h>
#include <LibTest/TestCase.h>

// All variables labeled "minisign" have content created by minisign, which verifies our compatibility.
constexpr StringView public_key_file_minisign_text = R"""(untrusted comment: minisign public key FDE44BFDD77EC45A
RWRaxH7X/Uvk/etgLk05NOsAT5aNTz1d5DjHD2R3s1/URq3vnQw6R790
)"""sv;
constexpr StringView public_key_minisign_text = "RWRaxH7X/Uvk/etgLk05NOsAT5aNTz1d5DjHD2R3s1/URq3vnQw6R790"sv;
constexpr StringView secret_key_minisign_text = R"""(untrusted comment: minisign encrypted secret key
RWQAAEIyAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAWsR+1/1L5P3AKeEZBVWCT2g7hvHFeF8ALiRqPDSZdINZiB1uSVxyaetgLk05NOsAT5aNTz1d5DjHD2R3s1/URq3vnQw6R790AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=
)"""sv;
constexpr StringView public_key_file_text = R"""(untrusted comment: iffysign public key
RWQ7FcRc9BMU2CTaEuu+FqFwYT5OChWG7ehQgLVIVMeerG1ANDcit9Jx
)"""sv;
constexpr StringView secret_key_text = R"""(untrusted comment: iffysign unencrypted secret key
RWQAAEIyAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAOxXEXPQTFNiUeh0iJJbl84yimedpdQgkFsUitcDSY4S/yAsD1uFPvSTaEuu+FqFwYT5OChWG7ehQgLVIVMeerG1ANDcit9JxAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=
)"""sv;

constexpr StringView example_data = "1234abc\n"sv;
constexpr StringView empty;

constexpr StringView minisign_signature_text = R"""(untrusted comment: signature from minisign secret key
RURaxH7X/Uvk/Q81zoW4nzVrc1gVOQF5PZwD9vxF7TEI6lYC1qvXP4oyPcBiF0QtMDzJZvMj3/M+rm1S0nhxQA0pNtVL3VNFJAg=
trusted comment: timestamp:1745588000	file:1234abc	hashed
bRVIYO+dSrQwLhTY6/kk/0qyIb7xrzPA7qq9RPIpOYBo9hnhL0L/IW5WLSiqaA9tSY+PjYjLJQ8GFqFfZyi4AA==
)"""sv;
constexpr StringView signature_text = R"""(untrusted comment: minisign-compatible signature
RUQ7FcRc9BMU2D9h28o+9Ba3QtfQHYdyLItVT6PWt1/PN66gHjumBCqje+eeLvQckrcKQGOQ8vkKXYtzWrkslGZdH/bHlI9txAE=
trusted comment: {"filename":"Tests/LibCrypto/1234abc"}
sEbAITvxLddnp9pAU3GhMO/02dCeG7V73J8JUN0qyj9z9H7B+6bajwu73sKPcTSOLu5cBxDeX8jNVNPziHTsCQ==
)"""sv;

using namespace Crypto::Minisign;

TEST_CASE(read_write_keys)
{
    auto const minisign_key_from_file = MUST(PublicKey::from_public_key_file(public_key_file_minisign_text));
    EXPECT_EQ(MUST(minisign_key_from_file.to_public_key_file()), public_key_file_minisign_text);

    auto const minisign_key = MUST(PublicKey::from_base64(public_key_minisign_text.bytes()));
    EXPECT_EQ(minisign_key_from_file.public_key(), minisign_key.public_key());

    auto const minisign_secret_key_from_file = MUST(SecretKey::from_secret_key_file(Core::SecretString::take_ownership(secret_key_minisign_text.to_byte_string().to_byte_buffer())));
    auto output = MUST(minisign_secret_key_from_file.to_secret_key_file());
    auto original = Core::SecretString::take_ownership(secret_key_minisign_text.to_byte_string().to_byte_buffer());
    EXPECT_EQ(output.view(), original.view());

    auto const key_from_file = MUST(PublicKey::from_public_key_file(public_key_file_text));
    EXPECT_EQ(MUST(key_from_file.to_public_key_file()), public_key_file_text);

    auto const secret_key_from_file = MUST(SecretKey::from_secret_key_file(Core::SecretString::take_ownership(secret_key_text.to_byte_string().to_byte_buffer())));
    output = MUST(secret_key_from_file.to_secret_key_file());
    original = Core::SecretString::take_ownership(secret_key_text.to_byte_string().to_byte_buffer());
    EXPECT_EQ(output.view(), original.view());
}

TEST_CASE(generate)
{
    auto const secret_key = MUST(SecretKey::generate());
    PublicKey const key = secret_key;
    EXPECT_EQ(key.public_key(), secret_key.public_key());
    EXPECT_EQ(key.untrusted_comment().bytes_as_string_view(), secret_key.untrusted_comment().bytes_as_string_view());

    // Make sure both keys are identical across serialize/deserialize roundtrips.
    auto const roundtrip_secret_key = MUST(SecretKey::from_secret_key_file(MUST(secret_key.to_secret_key_file())));
    EXPECT_EQ(roundtrip_secret_key.public_key(), secret_key.public_key());
    auto const roundtrip_key = MUST(PublicKey::from_public_key_file(MUST(key.to_public_key_file())));
    EXPECT_EQ(roundtrip_key.public_key(), key.public_key());
    EXPECT_EQ(roundtrip_key.untrusted_comment().bytes_as_string_view(), key.untrusted_comment().bytes_as_string_view());
    EXPECT_EQ(roundtrip_key.id(), key.id());

    // Make sure the two key pairs actually belong to each other.
    FixedMemoryStream data_stream { example_data.bytes() };
    auto const signature = MUST(secret_key.sign(data_stream, "i am not trustworthy"_string, "i can be trusted with power and responsibility"_string));
    MUST(data_stream.seek(0));
    EXPECT_EQ(MUST(key.verify(signature, data_stream)), VerificationResult::Valid);
    MUST(data_stream.seek(0));
    EXPECT_EQ(MUST(roundtrip_key.verify(signature, data_stream)), VerificationResult::Valid);
}

TEST_CASE(sign_verify)
{
    auto const minisign_key = MUST(PublicKey::from_base64(public_key_minisign_text.bytes()));
    auto const minisign_secret_key = MUST(SecretKey::from_secret_key_file(Core::SecretString::take_ownership(secret_key_minisign_text.to_byte_string().to_byte_buffer())));
    auto const key = MUST(PublicKey::from_public_key_file(public_key_file_text));
    auto const secret_key = MUST(SecretKey::from_secret_key_file(Core::SecretString::take_ownership(secret_key_text.to_byte_string().to_byte_buffer())));

    FixedMemoryStream data_stream { example_data.bytes() };
    auto signature = MUST(secret_key.sign(data_stream, "i am not trustworthy"_string, "i can be trusted with power and responsibility"_string));
    MUST(data_stream.seek(0));
    EXPECT_EQ(MUST(key.verify(signature, data_stream)), VerificationResult::Valid);
    MUST(data_stream.seek(0));
    EXPECT_NE(MUST(minisign_key.verify(signature, data_stream)), VerificationResult::Valid);

    // Cannot verify signatures with the wrong key.
    FixedMemoryStream minisign_data_stream { example_data.bytes() };
    auto const signature_minisign = MUST(minisign_secret_key.sign(minisign_data_stream, {}, "example trust"_string));
    MUST(minisign_data_stream.seek(0));
    EXPECT_NE(MUST(key.verify(signature_minisign, minisign_data_stream)), VerificationResult::Valid);
    MUST(minisign_data_stream.seek(0));
    EXPECT_EQ(MUST(minisign_key.verify(signature_minisign, minisign_data_stream)), VerificationResult::Valid);

    // Signature from same key does not match against different data.
    FixedMemoryStream empty_data_stream { empty.bytes() };
    auto const signature_empty = MUST(secret_key.sign(minisign_data_stream, {}, "empty data"_string));
    MUST(data_stream.seek(0));
    EXPECT_NE(MUST(key.verify(signature_empty, data_stream)), VerificationResult::Valid);

    // Signature does match if untrusted comment changed.
    signature.untrusted_comment() = "EVIL ATTACKER SAYS HI"_string;
    MUST(data_stream.seek(0));
    EXPECT_EQ(MUST(key.verify(signature, data_stream)), VerificationResult::Valid);

    // Signature does *not* match if trusted comment changed.
    signature.trusted_comment() = "oh no, I changed the trusted comment!"_string;
    MUST(data_stream.seek(0));
    EXPECT_EQ(MUST(key.verify(signature, data_stream)), VerificationResult::GlobalSignatureInvalid);

    // Signature does not match if key id changed.
    signature = MUST(secret_key.sign(data_stream, "i am not trustworthy"_string, "i can be trusted with power and responsibility"_string));
    auto key_id_copy = signature.key_id();
    key_id_copy[0] = ~key_id_copy[0];
    signature.set_key_id(key_id_copy);
    MUST(data_stream.seek(0));
    EXPECT_EQ(MUST(key.verify(signature, data_stream)), VerificationResult::Invalid);

    // Check previously prepared signatures.
    auto const prepared_minisign_signature = MUST(Signature::from_signature_file(minisign_signature_text));
    MUST(data_stream.seek(0));
    EXPECT_EQ(MUST(minisign_key.verify(prepared_minisign_signature, data_stream)), VerificationResult::Valid);
    auto const prepared_signature = MUST(Signature::from_signature_file(signature_text));
    MUST(data_stream.seek(0));
    EXPECT_EQ(MUST(key.verify(prepared_signature, data_stream)), VerificationResult::Valid);
}
