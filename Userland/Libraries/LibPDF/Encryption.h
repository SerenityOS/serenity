/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <LibPDF/ObjectDerivatives.h>

namespace Crypto::Cipher {
enum class Intent;
}

namespace PDF {

enum class CryptFilterMethod {
    None,
    V2, // RC4
    AESV2,
    AESV3,
};

class SecurityHandler : public RefCounted<SecurityHandler> {
public:
    static PDFErrorOr<NonnullRefPtr<SecurityHandler>> create(Document*, NonnullRefPtr<DictObject> encryption_dict);

    virtual ~SecurityHandler() = default;

    virtual bool try_provide_user_password(StringView password) = 0;
    virtual bool has_user_password() const = 0;

    virtual void encrypt(NonnullRefPtr<Object>, Reference reference) const = 0;
    virtual void decrypt(NonnullRefPtr<Object>, Reference reference) const = 0;
};

class StandardSecurityHandler : public SecurityHandler {
public:
    static PDFErrorOr<NonnullRefPtr<StandardSecurityHandler>> create(Document*, NonnullRefPtr<DictObject> encryption_dict);

    StandardSecurityHandler(Document*, size_t revision, ByteString const& o_entry, ByteString const& oe_entry, ByteString const& u_entry, ByteString const& ue_entry, ByteString const& perms, u32 flags, bool encrypt_metadata, size_t length, CryptFilterMethod method);

    ~StandardSecurityHandler() override = default;

    bool try_provide_user_password(StringView password_string) override;

    bool has_user_password() const override { return m_encryption_key.has_value(); }

protected:
    void encrypt(NonnullRefPtr<Object>, Reference reference) const override;
    void decrypt(NonnullRefPtr<Object>, Reference reference) const override;

private:
    void crypt(NonnullRefPtr<Object>, Reference reference, Crypto::Cipher::Intent) const;

    ByteBuffer compute_user_password_value_r2(ByteBuffer password_string);
    ByteBuffer compute_user_password_value_r3_to_r5(ByteBuffer password_string);

    bool authenticate_user_password_r2_to_r5(StringView password_string);
    bool authenticate_user_password_r6_and_later(StringView password_string);
    bool authenticate_owner_password_r6_and_later(StringView password_string);

    ByteBuffer compute_encryption_key_r2_to_r5(ByteBuffer password_string);
    bool compute_encryption_key_r6_and_later(ByteBuffer password_string);

    enum class HashKind {
        Owner,
        User,
    };
    ByteBuffer computing_a_hash_r6_and_later(ByteBuffer input, StringView input_password, HashKind);

    Document* m_document;
    size_t m_revision;
    Optional<ByteBuffer> m_encryption_key;
    ByteString m_o_entry;
    ByteString m_oe_entry;
    ByteString m_u_entry;
    ByteString m_ue_entry;
    ByteString m_perms_entry;
    u32 m_flags;
    bool m_encrypt_metadata;
    size_t m_length;
    CryptFilterMethod m_method;
};

class RC4 {
public:
    RC4(ReadonlyBytes key);

    void generate_bytes(ByteBuffer&);
    ByteBuffer encrypt(ReadonlyBytes bytes);

private:
    Array<size_t, 256> m_bytes;
};

}
