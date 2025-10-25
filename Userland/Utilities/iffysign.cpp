/*
 * Copyright (c) 2025, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/ByteString.h>
#include <AK/Format.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/SecretString.h>
#include <LibCore/StandardPaths.h>
#include <LibCrypto/Minisign.h>
#include <LibMain/Main.h>

enum class Operation : u8 {
    Unspecified,
    Sign,
    Verify,
    GenerateKey,
};

using namespace Crypto::Minisign;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Operation operation { Operation::Unspecified };
    ByteString public_key_file;
    ByteString public_key;
    ByteString secret_key_file;
    ByteString signature_file;
    ByteString operand_file;
    String untrusted_comment;
    String trusted_comment;
    bool force_overwrite { false };

    // As noted in the manpage, all options that are supported by minisign(1) as well are (almost) compatible with it.
    // Some of our defaults are different to reflect the missing functionality and how iffysign is used in Serenity.
    // minisign doesn’t use long options, so our long options are freely chosen.
    Core::ArgsParser args_parser;
    args_parser.set_general_help("Sign files and verify signatures. iffysign has a partially minisign-compatible command line interface and key/signature file formats.");

    // Base operations.
    args_parser.add_option(operation, Operation::GenerateKey, "Generate a new key pair", "generate", 'G');
    args_parser.add_option(operation, Operation::Sign, "Sign a file", "sign", 'S');
    args_parser.add_option(operation, Operation::Verify, "Verify that a file's signature is valid", "verify", 'V');

    // Options needed by some or all operations.
    args_parser.add_option(public_key_file, "Path to the public key file, default `iffysign.pub`", "pubkey-file", 'p', "FILE");
    args_parser.add_option(public_key, "Public key as base64", "pubkey", 'P', "PUBLIC_KEY");
    args_parser.add_option(secret_key_file, "Secret key file, default `~/.config/iffysign/iffysign.sec`", "secret-key-file", 's', "FILE");
    args_parser.add_option(signature_file, "Signature file, default `<file>.iffy`", "signature", 'x', "FILE");
    args_parser.add_option(operand_file, "File to sign or verify", "file", 'm', "FILE");
    args_parser.add_option(force_overwrite, "Force overwrite files if they already exist.", "force", 'f');

    // Comment options.
    args_parser.add_option(untrusted_comment, "UNTRUSTED (not signed) comment to add when signing. DO NOT USE THIS OPTION unless you know what you’re doing.", "untrusted-comment", 'c', "COMMENT");
    args_parser.add_option(trusted_comment, "Trusted comment to add when signing.", "comment", 't', "COMMENT");

    args_parser.parse(arguments);

    if (operation == Operation::Unspecified) {
        warnln("iffysign: error: no operation specified, use one of -G, -V, -S.");
        return 2;
    }
    if (!public_key_file.is_empty() && !public_key.is_empty()) {
        warnln("iffysign: error: only one of -p, -P is allowed");
        return 2;
    }
    if (public_key_file.is_empty() && public_key.is_empty())
        public_key_file = "./iffysign.pub";

    // Users may be used to the `-c` option, which is not really what you should use.
    if (!untrusted_comment.is_empty() && trusted_comment.is_empty())
        warnln("iffysign: warning: Only untrusted comment provided. This comment is not signed and recipients of the signature can not validate its authenticity! Consider providing a trusted comment with the `-t` option.");
    if (untrusted_comment.is_empty())
        untrusted_comment = "minisign-compatible signature"_string;

    if (signature_file.is_empty() && !operand_file.is_empty())
        signature_file = ByteString::formatted("{}.minisig", operand_file);

    if (secret_key_file.is_empty())
        secret_key_file = ByteString::formatted("{}/.config/iffysign/iffysign.sec", Core::StandardPaths::home_directory());

    auto const write_mode = force_overwrite ? Core::File::OpenMode::Write
                                            : Core::File::OpenMode::MustBeNew | Core::File::OpenMode::Write;

    auto const get_public_key = [&] -> ErrorOr<PublicKey> {
        if (!public_key_file.is_empty()) {
            auto public_key_file_object = TRY(Core::File::open(public_key_file, Core::File::OpenMode::Read));
            auto const public_key_data = TRY(public_key_file_object->read_until_eof());
            return TRY(PublicKey::from_public_key_file({ public_key_data }));
        }
        if (!public_key.is_empty()) {
            // We made sure earlier that in this case, a literal key must have been given.
            return TRY(PublicKey::from_base64(public_key.bytes()));
        }

        warnln("iffysign: error: no public key specified");
        exit(1);
    };

    switch (operation) {
    case Operation::Verify: {
        auto signature_file_object = TRY(Core::File::open(signature_file, Core::File::OpenMode::Read));
        auto const signature_data = TRY(signature_file_object->read_until_eof());
        auto const signature = TRY(Signature::from_signature_file(StringView { signature_data }));

        outln("untrusted comment is: {}", signature.untrusted_comment());

        auto const public_key = TRY(get_public_key());
        auto operand = TRY(Core::File::open_file_or_standard_stream(operand_file, Core::File::OpenMode::Read));
        auto const signature_validity = TRY(public_key.verify(signature, *operand));

        switch (signature_validity) {
        case VerificationResult::Invalid:
            outln("iffysign: error: invalid signature for file {}!", operand_file);
            return 1;
        case VerificationResult::Valid:
            outln("valid signature for file {}\ntrusted comment is: {}\n", operand_file, signature.trusted_comment());
            return 0;
        case VerificationResult::GlobalSignatureInvalid:
            outln("iffysign: error: file signature is valid for file {} but trusted comment signature is not valid!", operand_file);
            return 1;
        }
        break;
    }

    case Operation::Sign: {
        if (trusted_comment.is_empty()) {
            JsonObject info_structure;
            // TODO: Add more information, like signing and file modification timestamps, file hash, etc.
            info_structure.set("filename", JsonValue { operand_file.view() });
            trusted_comment = TRY(TRY(String::from_byte_string(info_structure.to_byte_string())).replace("\n"sv, " "sv, ReplaceMode::All));
        }

        auto secret_key_file_object = TRY(Core::File::open(secret_key_file, Core::File::OpenMode::Read));
        auto temporary_secret_key_data = TRY(secret_key_file_object->read_until_eof());
        auto const secret_key_data = Core::SecretString::take_ownership(move(temporary_secret_key_data));
        auto const secret_key = TRY(SecretKey::from_secret_key_file(secret_key_data));

        auto operand = TRY(Core::File::open_file_or_standard_stream(operand_file, Core::File::OpenMode::Read));
        auto const signature = TRY(secret_key.sign(*operand, untrusted_comment, trusted_comment));
        auto const signature_data = TRY(signature.to_signature_file());
        auto const signature_file_object = TRY(Core::File::open(signature_file, write_mode));
        TRY(signature_file_object->write_until_depleted(signature_data.bytes()));
        break;
    }
    case Operation::GenerateKey: {
        auto const secret_key = TRY(SecretKey::generate());
        auto const secret_key_text = TRY(secret_key.to_secret_key_file());
        auto public_key = PublicKey { secret_key };
        public_key.set_untrusted_comment(TRY(String::formatted("iffysign public key")));
        auto const public_key_text = TRY(public_key.to_public_key_file());

        auto secret_key_file_object = TRY(Core::File::open(secret_key_file, write_mode));
        auto public_key_file_object = TRY(Core::File::open(public_key_file, write_mode));
        TRY(secret_key_file_object->write_until_depleted(secret_key_text.view().trim("\0"sv, TrimMode::Right)));
        TRY(public_key_file_object->write_until_depleted(public_key_text));
        outln("Generated new key pair to {} and {}.", secret_key_file, public_key_file);

        break;
    }
    case Operation::Unspecified:
        VERIFY_NOT_REACHED();
    }

    return 0;
}
