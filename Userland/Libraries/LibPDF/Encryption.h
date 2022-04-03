/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <LibPDF/ObjectDerivatives.h>

namespace PDF {

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

    StandardSecurityHandler(Document*, size_t revision, String const& o_entry, String const& u_entry, u32 flags, bool encrypt_metadata, size_t length);

    ~StandardSecurityHandler() override = default;

    bool try_provide_user_password(StringView password_string) override;

    bool has_user_password() const override { return m_encryption_key.has_value(); }

protected:
    void encrypt(NonnullRefPtr<Object>, Reference reference) const override;
    void decrypt(NonnullRefPtr<Object>, Reference reference) const override;

private:
    template<bool is_revision_2>
    ByteBuffer compute_user_password_value(ByteBuffer password_string);

    ByteBuffer compute_encryption_key(ByteBuffer password_string);

    Document* m_document;
    size_t m_revision;
    Optional<ByteBuffer> m_encryption_key;
    String m_o_entry;
    String m_u_entry;
    u32 m_flags;
    bool m_encrypt_metadata;
    size_t m_length;
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
