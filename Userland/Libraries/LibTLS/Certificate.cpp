/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Certificate.h"
#include <AK/Debug.h>
#include <LibCrypto/ASN1/ASN1.h>
#include <LibCrypto/ASN1/DER.h>
#include <LibCrypto/ASN1/PEM.h>

namespace TLS {

constexpr static Array<int, 4>
    common_name_oid { 2, 5, 4, 3 },
    country_name_oid { 2, 5, 4, 6 },
    locality_name_oid { 2, 5, 4, 7 },
    organization_name_oid { 2, 5, 4, 10 },
    organizational_unit_name_oid { 2, 5, 4, 11 };

constexpr static Array<int, 7>
    rsa_encryption_oid { 1, 2, 840, 113549, 1, 1, 1 },
    rsa_md5_encryption_oid { 1, 2, 840, 113549, 1, 1, 4 },
    rsa_sha1_encryption_oid { 1, 2, 840, 113549, 1, 1, 5 },
    rsa_sha256_encryption_oid { 1, 2, 840, 113549, 1, 1, 11 },
    rsa_sha384_encryption_oid { 1, 2, 840, 113549, 1, 1, 12 },
    rsa_sha512_encryption_oid { 1, 2, 840, 113549, 1, 1, 13 };

constexpr static Array<int, 4>
    subject_alternative_name_oid { 2, 5, 29, 17 };

Optional<Certificate> Certificate::parse_asn1(ReadonlyBytes buffer, bool)
{
#define ENTER_SCOPE_WITHOUT_TYPECHECK(scope)                                               \
    do {                                                                                   \
        if (auto result = decoder.enter(); result.has_value()) {                           \
            dbgln_if(TLS_DEBUG, "Failed to enter object (" scope "): {}", result.value()); \
            return {};                                                                     \
        }                                                                                  \
    } while (0)

#define ENTER_SCOPE_OR_FAIL(kind_name, scope)                                                                 \
    do {                                                                                                      \
        if (auto tag = decoder.peek(); tag.is_error() || tag.value().kind != Crypto::ASN1::Kind::kind_name) { \
            if constexpr (TLS_DEBUG) {                                                                        \
                if (tag.is_error())                                                                           \
                    dbgln(scope " data was invalid: {}", tag.error());                                        \
                else                                                                                          \
                    dbgln(scope " data was not of kind " #kind_name);                                         \
            }                                                                                                 \
            return {};                                                                                        \
        }                                                                                                     \
        ENTER_SCOPE_WITHOUT_TYPECHECK(scope);                                                                 \
    } while (0)

#define EXIT_SCOPE(scope)                                                                  \
    do {                                                                                   \
        if (auto error = decoder.leave(); error.has_value()) {                             \
            dbgln_if(TLS_DEBUG, "Error while exiting scope " scope ": {}", error.value()); \
            return {};                                                                     \
        }                                                                                  \
    } while (0)

#define ENSURE_OBJECT_KIND(_kind_name, scope)                                                                                   \
    do {                                                                                                                        \
        if (auto tag = decoder.peek(); tag.is_error() || tag.value().kind != Crypto::ASN1::Kind::_kind_name) {                  \
            if constexpr (TLS_DEBUG) {                                                                                          \
                if (tag.is_error())                                                                                             \
                    dbgln(scope " data was invalid: {}", tag.error());                                                          \
                else                                                                                                            \
                    dbgln(scope " data was not of kind " #_kind_name ", it was {}", Crypto::ASN1::kind_name(tag.value().kind)); \
            }                                                                                                                   \
            return {};                                                                                                          \
        }                                                                                                                       \
    } while (0)

#define READ_OBJECT_OR_FAIL(kind_name, type_name, value_name, scope)                                                   \
    auto value_name##_result = decoder.read<type_name>(Crypto::ASN1::Class::Universal, Crypto::ASN1::Kind::kind_name); \
    if (value_name##_result.is_error()) {                                                                              \
        dbgln_if(TLS_DEBUG, scope " read of kind " #kind_name " failed: {}", value_name##_result.error());             \
        return {};                                                                                                     \
    }                                                                                                                  \
    auto value_name = value_name##_result.release_value();

#define DROP_OBJECT_OR_FAIL(scope)                                        \
    do {                                                                  \
        if (auto error = decoder.drop(); error.has_value()) {             \
            dbgln_if(TLS_DEBUG, scope " read failed: {}", error.value()); \
        }                                                                 \
    } while (0)

    Certificate certificate;
    Crypto::ASN1::Decoder decoder { buffer };
    // Certificate ::= Sequence {
    //     certificate          TBSCertificate,
    //     signature_algorithm  AlgorithmIdentifier,
    //     signature_value      BitString
    // }
    ENTER_SCOPE_OR_FAIL(Sequence, "Certificate");

    // TBSCertificate ::= Sequence {
    //     version                  (0) EXPLICIT Version DEFAULT v1,
    //     serial_number                CertificateSerialNumber,
    //     signature                    AlgorithmIdentifier,
    //     issuer                       Name,
    //     validity                     Validity,
    //     subject                      Name,
    //     subject_public_key_info      SubjectPublicKeyInfo,
    //     issuer_unique_id         (1) IMPLICIT UniqueIdentifier OPTIONAL (if present, version > v1),
    //     subject_unique_id        (2) IMPLICIT UniqueIdentifier OPTIONAL (if present, version > v1),
    //     extensions               (3) EXPLICIT Extensions OPTIONAL      (if present, version > v2)
    // }
    ENTER_SCOPE_OR_FAIL(Sequence, "Certificate::TBSCertificate");

    // version
    {
        // Version :: Integer { v1(0), v2(1), v3(2) } (Optional)
        if (auto tag = decoder.peek(); !tag.is_error() && tag.value().type == Crypto::ASN1::Type::Constructed) {
            ENTER_SCOPE_WITHOUT_TYPECHECK("Certificate::version");
            READ_OBJECT_OR_FAIL(Integer, Crypto::UnsignedBigInteger, value, "Certificate::version");
            if (!(value < 3)) {
                dbgln_if(TLS_DEBUG, "Certificate::version Invalid value for version: {}", value.to_base(10));
                return {};
            }
            certificate.version = value.words()[0];
            EXIT_SCOPE("Certificate::version");
        } else {
            certificate.version = 0;
        }
    }

    // serial_number
    {
        // CertificateSerialNumber :: Integer
        READ_OBJECT_OR_FAIL(Integer, Crypto::UnsignedBigInteger, value, "Certificate::serial_number");
        certificate.serial_number = move(value);
    }

    auto parse_algorithm_identifier = [&](CertificateKeyAlgorithm& field) -> Optional<bool> {
        // AlgorithmIdentifier ::= Sequence {
        //     algorithm   ObjectIdentifier,
        //     parameters  ANY OPTIONAL
        // }
        ENTER_SCOPE_OR_FAIL(Sequence, "AlgorithmIdentifier");
        READ_OBJECT_OR_FAIL(ObjectIdentifier, Vector<int>, identifier, "AlgorithmIdentifier::algorithm");
        if (identifier == rsa_encryption_oid)
            field = CertificateKeyAlgorithm ::RSA_RSA;
        else if (identifier == rsa_md5_encryption_oid)
            field = CertificateKeyAlgorithm ::RSA_MD5;
        else if (identifier == rsa_sha1_encryption_oid)
            field = CertificateKeyAlgorithm ::RSA_SHA1;
        else if (identifier == rsa_sha256_encryption_oid)
            field = CertificateKeyAlgorithm ::RSA_SHA256;
        else if (identifier == rsa_sha384_encryption_oid)
            field = CertificateKeyAlgorithm ::RSA_SHA384;
        else if (identifier == rsa_sha512_encryption_oid)
            field = CertificateKeyAlgorithm ::RSA_SHA512;
        else
            return {};

        EXIT_SCOPE("AlgorithmIdentifier");
        return true;
    };

    // signature
    {
        if (!parse_algorithm_identifier(certificate.algorithm).has_value())
            return {};
    }

    auto parse_name = [&](auto& name_struct) -> Optional<bool> {
        // Name ::= Choice {
        //     rdn_sequence RDNSequence
        // } // NOTE: since this is the only alternative, there's no index
        // RDNSequence ::= Sequence OF RelativeDistinguishedName
        ENTER_SCOPE_OR_FAIL(Sequence, "Certificate::TBSCertificate::issuer/subject");

        // RelativeDistinguishedName ::= Set OF AttributeTypeAndValue
        // AttributeTypeAndValue ::= Sequence {
        //     type   AttributeType,
        //     value  AttributeValue
        // }
        // AttributeType ::= ObjectIdentifier
        // AttributeValue ::= Any
        while (!decoder.eof()) {
            // Parse only the the required fields, and ignore the rest.
            ENTER_SCOPE_OR_FAIL(Set, "Certificate::TBSCertificate::issuer/subject::$::RelativeDistinguishedName");
            while (!decoder.eof()) {
                ENTER_SCOPE_OR_FAIL(Sequence, "Certificate::TBSCertificate::issuer/subject::$::RelativeDistinguishedName::$::AttributeTypeAndValue");
                ENSURE_OBJECT_KIND(ObjectIdentifier, "Certificate::TBSCertificate::issuer/subject::$::RelativeDistinguishedName::$::AttributeTypeAndValue::type");

                if (auto type_identifier_or_error = decoder.read<Vector<int>>(); !type_identifier_or_error.is_error()) {
                    // Figure out what type of identifier this is
                    auto& identifier = type_identifier_or_error.value();
                    if (identifier == common_name_oid) {
                        READ_OBJECT_OR_FAIL(PrintableString, StringView, name,
                            "Certificate::TBSCertificate::issuer/subject::$::RelativeDistinguishedName::$::AttributeTypeAndValue::Value");
                        name_struct.subject = name;
                    } else if (identifier == country_name_oid) {
                        READ_OBJECT_OR_FAIL(PrintableString, StringView, name,
                            "Certificate::TBSCertificate::issuer/subject::$::RelativeDistinguishedName::$::AttributeTypeAndValue::Value");
                        name_struct.country = name;
                    } else if (identifier == locality_name_oid) {
                        READ_OBJECT_OR_FAIL(PrintableString, StringView, name,
                            "Certificate::TBSCertificate::issuer/subject::$::RelativeDistinguishedName::$::AttributeTypeAndValue::Value");
                        name_struct.location = name;
                    } else if (identifier == organization_name_oid) {
                        READ_OBJECT_OR_FAIL(PrintableString, StringView, name,
                            "Certificate::TBSCertificate::issuer/subject::$::RelativeDistinguishedName::$::AttributeTypeAndValue::Value");
                        name_struct.entity = name;
                    } else if (identifier == organizational_unit_name_oid) {
                        READ_OBJECT_OR_FAIL(PrintableString, StringView, name,
                            "Certificate::TBSCertificate::issuer/subject::$::RelativeDistinguishedName::$::AttributeTypeAndValue::Value");
                        name_struct.unit = name;
                    }
                } else {
                    dbgln_if(TLS_DEBUG, "Certificate::TBSCertificate::issuer/subject::$::RelativeDistinguishedName::$::AttributeTypeAndValue::type data was invalid: {}", type_identifier_or_error.error());
                    return {};
                }

                EXIT_SCOPE("Certificate::TBSCertificate::issuer/subject::$::RelativeDistinguishedName::$::AttributeTypeAndValue");
            }
            EXIT_SCOPE("Certificate::TBSCertificate::issuer/subject::$::RelativeDistinguishedName");
        }

        EXIT_SCOPE("Certificate::TBSCertificate::issuer/subject");
        return true;
    };

    // issuer
    {
        if (!parse_name(certificate.issuer).has_value())
            return {};
    }

    // validity
    {
        ENTER_SCOPE_OR_FAIL(Sequence, "Certificate::TBSCertificate::Validity");

        auto parse_time = [&](Core::DateTime& datetime) -> Optional<bool> {
            // Time ::= Choice {
            //     utc_time     UTCTime,
            //     general_time GeneralizedTime
            // }
            auto tag = decoder.peek();
            if (tag.is_error()) {
                dbgln_if(1, "Certificate::TBSCertificate::Validity::$::Time failed to read tag: {}", tag.error());
                return {};
            };

            if (tag.value().kind == Crypto::ASN1::Kind::UTCTime) {
                READ_OBJECT_OR_FAIL(UTCTime, StringView, time, "Certificate::TBSCertificate::Validity::$");
                auto result = Crypto::ASN1::parse_utc_time(time);
                if (!result.has_value()) {
                    dbgln_if(1, "Certificate::TBSCertificate::Validity::$::Time Invalid UTC Time: {}", time);
                    return {};
                }
                datetime = result.release_value();
                return true;
            }

            if (tag.value().kind == Crypto::ASN1::Kind::GeneralizedTime) {
                READ_OBJECT_OR_FAIL(UTCTime, StringView, time, "Certificate::TBSCertificate::Validity::$");
                auto result = Crypto::ASN1::parse_generalized_time(time);
                if (!result.has_value()) {
                    dbgln_if(1, "Certificate::TBSCertificate::Validity::$::Time Invalid Generalized Time: {}", time);
                    return {};
                }
                datetime = result.release_value();
                return true;
            }

            dbgln_if(1, "Unrecognised Time format {}", Crypto::ASN1::kind_name(tag.value().kind));
            return {};
        };

        if (!parse_time(certificate.not_before).has_value())
            return {};

        if (!parse_time(certificate.not_after).has_value())
            return {};

        EXIT_SCOPE("Certificate::TBSCertificate::Validity");
    }

    // subject
    {
        if (!parse_name(certificate.subject).has_value())
            return {};
    }

    // subject_public_key_info
    {
        // SubjectPublicKeyInfo ::= Sequence {
        //     algorithm           AlgorithmIdentifier,
        //     subject_public_key  BitString
        // }
        ENTER_SCOPE_OR_FAIL(Sequence, "Certificate::TBSCertificate::subject_public_key_info");

        if (!parse_algorithm_identifier(certificate.key_algorithm).has_value())
            return {};

        READ_OBJECT_OR_FAIL(BitString, const BitmapView, value, "Certificate::TBSCertificate::subject_public_key_info::subject_public_key_info");
        // Note: Once we support other kinds of keys, make sure to check the kind here!
        auto key = Crypto::PK::RSA::parse_rsa_key({ value.data(), value.size_in_bytes() });
        if (!key.public_key.length()) {
            dbgln_if(TLS_DEBUG, "Certificate::TBSCertificate::subject_public_key_info::subject_public_key_info: Invalid key");
            return {};
        }
        certificate.public_key = move(key.public_key);
        EXIT_SCOPE("Certificate::TBSCertificate::subject_public_key_info");
    }

    auto parse_unique_identifier = [&]() -> Optional<bool> {
        if (certificate.version == 0)
            return true;

        auto tag = decoder.peek();
        if (tag.is_error()) {
            dbgln_if(TLS_DEBUG, "Certificate::TBSCertificate::*::UniqueIdentifier could not read tag: {}", tag.error());
            return {};
        }

        // The spec says to just ignore these.
        if (static_cast<u8>(tag.value().kind) == 1 || static_cast<u8>(tag.value().kind) == 2)
            DROP_OBJECT_OR_FAIL("UniqueIdentifier");

        return true;
    };

    // issuer_unique_identifier
    {
        if (!parse_unique_identifier().has_value())
            return {};
    }

    // subject_unique_identifier
    {
        if (!parse_unique_identifier().has_value())
            return {};
    }

    // extensions
    {
        if (certificate.version == 2) {
            auto tag = decoder.peek();
            if (tag.is_error()) {
                dbgln_if(TLS_DEBUG, "Certificate::TBSCertificate::*::UniqueIdentifier could not read tag: {}", tag.error());
                return {};
            }
            if (static_cast<u8>(tag.value().kind) == 3) {
                // Extensions ::= Sequence OF Extension
                // Extension ::= Sequence {
                //     extension_id     ObjectIdentifier,
                //     critical         Boolean DEFAULT false,
                //     extension_value  OctetString (DER-encoded)
                // }
                ENTER_SCOPE_WITHOUT_TYPECHECK("Certificate::TBSCertificate::Extensions(IMPLICIT)");
                ENTER_SCOPE_OR_FAIL(Sequence, "Certificate::TBSCertificate::Extensions");

                while (!decoder.eof()) {
                    ENTER_SCOPE_OR_FAIL(Sequence, "Certificate::TBSCertificate::Extensions::$::Extension");
                    READ_OBJECT_OR_FAIL(ObjectIdentifier, Vector<int>, extension_id, "Certificate::TBSCertificate::Extensions::$::Extension::extension_id");
                    bool is_critical = false;
                    if (auto tag = decoder.peek(); !tag.is_error() && tag.value().kind == Crypto::ASN1::Kind::Boolean) {
                        // Read the 'critical' property
                        READ_OBJECT_OR_FAIL(Boolean, bool, critical, "Certificate::TBSCertificate::Extensions::$::Extension::critical");
                        is_critical = critical;
                    }
                    READ_OBJECT_OR_FAIL(OctetString, StringView, extension_value, "Certificate::TBSCertificate::Extensions::$::Extension::extension_value");

                    // Figure out what this extension is.
                    if (extension_id == subject_alternative_name_oid) {
                        Crypto::ASN1::Decoder decoder { extension_value.bytes() };
                        // SubjectAlternativeName ::= GeneralNames
                        // GeneralNames ::= Sequence OF GeneralName
                        // GeneralName ::= CHOICE {
                        //     other_name     (0) OtherName,
                        //     rfc_822_name   (1) IA5String,
                        //     dns_name       (2) IA5String,
                        //     x400Address    (3) ORAddress,
                        //     directory_name (4) Name,
                        //     edi_party_name (5) EDIPartyName,
                        //     uri            (6) IA5String,
                        //     ip_address     (7) OctetString,
                        //     registered_id  (8) ObjectIdentifier,
                        // }
                        ENTER_SCOPE_OR_FAIL(Sequence, "Certificate::TBSCertificate::Extensions::$::Extension::extension_value::SubjectAlternativeName");

                        while (!decoder.eof()) {
                            auto tag = decoder.peek();
                            if (tag.is_error()) {
                                dbgln_if(TLS_DEBUG, "Certificate::TBSCertificate::Extensions::$::Extension::extension_value::SubjectAlternativeName::$ could not read tag: {}", tag.error());
                                return {};
                            }

                            auto tag_value = static_cast<u8>(tag.value().kind);
                            switch (tag_value) {
                            case 0:
                                // OtherName
                                // We don't know how to use this.
                                DROP_OBJECT_OR_FAIL("Certificate::TBSCertificate::Extensions::$::Extension::extension_value::SubjectAlternativeName::$::OtherName");
                                break;
                            case 1:
                                // RFC 822 name
                                // We don't know how to use this.
                                DROP_OBJECT_OR_FAIL("Certificate::TBSCertificate::Extensions::$::Extension::extension_value::SubjectAlternativeName::$::RFC822Name");
                                break;
                            case 2: {
                                // DNS Name
                                READ_OBJECT_OR_FAIL(IA5String, StringView, name, "Certificate::TBSCertificate::Extensions::$::Extension::extension_value::SubjectAlternativeName::$::DNSName");
                                certificate.SAN.append(name);
                                break;
                            }
                            case 3:
                                // x400Address
                                // We don't know how to use this.
                                DROP_OBJECT_OR_FAIL("Certificate::TBSCertificate::Extensions::$::Extension::extension_value::SubjectAlternativeName::$::X400Address");
                                break;
                            case 4:
                                // Directory name
                                // We don't know how to use this.
                                DROP_OBJECT_OR_FAIL("Certificate::TBSCertificate::Extensions::$::Extension::extension_value::SubjectAlternativeName::$::DirectoryName");
                                break;
                            case 5:
                                // edi party name
                                // We don't know how to use this.
                                DROP_OBJECT_OR_FAIL("Certificate::TBSCertificate::Extensions::$::Extension::extension_value::SubjectAlternativeName::$::EDIPartyName");
                                break;
                            case 6: {
                                // URI
                                READ_OBJECT_OR_FAIL(IA5String, StringView, name, "Certificate::TBSCertificate::Extensions::$::Extension::extension_value::SubjectAlternativeName::$::URI");
                                certificate.SAN.append(name);
                                break;
                            }
                            case 7:
                                // IP Address
                                // We can't handle these.
                                DROP_OBJECT_OR_FAIL("Certificate::TBSCertificate::Extensions::$::Extension::extension_value::SubjectAlternativeName::$::IPAddress");
                                break;
                            case 8:
                                // Registered ID
                                // We can't handle these.
                                DROP_OBJECT_OR_FAIL("Certificate::TBSCertificate::Extensions::$::Extension::extension_value::SubjectAlternativeName::$::RegisteredID");
                                break;
                            default:
                                dbgln_if(TLS_DEBUG, "Unknown tag in SAN choice {}", tag_value);
                                if (is_critical)
                                    return {};
                                else
                                    DROP_OBJECT_OR_FAIL("Certificate::TBSCertificate::Extensions::$::Extension::extension_value::SubjectAlternativeName::$::???");
                            }
                        }
                    }

                    EXIT_SCOPE("Certificate::TBSCertificate::Extensions::$::Extension");
                }

                EXIT_SCOPE("Certificate::TBSCertificate::Extensions");
                EXIT_SCOPE("Certificate::TBSCertificate::Extensions(IMPLICIT)");
            }
        }
    }

    // Just ignore the rest of the data for now.
    EXIT_SCOPE("Certificate::TBSCertificate");
    EXIT_SCOPE("Certificate");

    dbgln_if(TLS_DEBUG, "Certificate issued for {} by {}", certificate.subject.subject, certificate.issuer.subject);

    return certificate;

#undef DROP_OBJECT_OR_FAIL
#undef ENSURE_OBJECT_KIND
#undef ENTER_SCOPE_OR_FAIL
#undef ENTER_SCOPE_WITHOUT_TYPECHECK
#undef EXIT_SCOPE
#undef READ_OBJECT_OR_FAIL
}

}
