/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <LibCrypto/Hash/MD5.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Document.h>
#include <LibPDF/Encryption.h>

namespace PDF {

static constexpr Array<u8, 32> standard_encryption_key_padding_bytes = {
    0x28,
    0xBF,
    0x4E,
    0x5E,
    0x4E,
    0x75,
    0x8A,
    0x41,
    0x64,
    0x00,
    0x4E,
    0x56,
    0xFF,
    0xFA,
    0x01,
    0x08,
    0x2E,
    0x2E,
    0x00,
    0xB6,
    0xD0,
    0x68,
    0x3E,
    0x80,
    0x2F,
    0x0C,
    0xA9,
    0xFE,
    0x64,
    0x53,
    0x69,
    0x7A,
};

PDFErrorOr<NonnullRefPtr<SecurityHandler>> SecurityHandler::create(Document* document, NonnullRefPtr<DictObject> encryption_dict)
{
    auto filter = TRY(encryption_dict->get_name(document, CommonNames::Filter))->name();
    if (filter == "Standard")
        return TRY(StandardSecurityHandler::create(document, encryption_dict));

    dbgln("Unrecognized security handler filter: {}", filter);
    TODO();
}

PDFErrorOr<NonnullRefPtr<StandardSecurityHandler>> StandardSecurityHandler::create(Document* document, NonnullRefPtr<DictObject> encryption_dict)
{
    auto revision = encryption_dict->get_value(CommonNames::R).get<int>();
    auto o = TRY(encryption_dict->get_string(document, CommonNames::O))->string();
    auto u = TRY(encryption_dict->get_string(document, CommonNames::U))->string();
    auto p = encryption_dict->get_value(CommonNames::P).get<int>();
    auto length = encryption_dict->get_value(CommonNames::Length).get<int>() / 8;
    bool encrypt_metadata = true;
    if (encryption_dict->contains(CommonNames::EncryptMetadata))
        encryption_dict->get_value(CommonNames::EncryptMetadata).get<bool>();
    return adopt_ref(*new StandardSecurityHandler(document, revision, o, u, p, encrypt_metadata, length));
}

StandardSecurityHandler::StandardSecurityHandler(Document* document, size_t revision, String const& o_entry, String const& u_entry, u32 flags, bool encrypt_metadata, size_t length)
    : m_document(document)
    , m_revision(revision)
    , m_o_entry(o_entry)
    , m_u_entry(u_entry)
    , m_flags(flags)
    , m_encrypt_metadata(encrypt_metadata)
    , m_length(length)
{
}

template<>
ByteBuffer StandardSecurityHandler::compute_user_password_value<true>(ByteBuffer password_string)
{
    // Algorithm 4: Computing the encryption dictionary's U (user password)
    //              value (Security handlers of revision 2)

    // a) Create an encryption key based on the user password string, as
    //    described in [Algorithm 2]
    auto encryption_key = compute_encryption_key(password_string);

    // b) Encrypt the 32-byte padding string shown in step (a) of [Algorithm 2],
    //    using an RC4 encryption function with the encryption key from the
    //    preceding step.
    RC4 rc4(encryption_key);
    auto output = rc4.encrypt(standard_encryption_key_padding_bytes);

    // c) Store the result of step (b) as the value of the U entry in the
    //    encryption dictionary.
    return output;
}

template<>
ByteBuffer StandardSecurityHandler::compute_user_password_value<false>(ByteBuffer password_string)
{
    // Algorithm 5: Computing the encryption dictionary's U (user password)
    //              value (Security handlers of revision 3 or greater)

    // a) Create an encryption key based on the user password string, as
    //    described in [Algorithm 2]
    auto encryption_key = compute_encryption_key(password_string);

    // b) Initialize the MD5 hash functino and pass the 32-byte padding string
    //    shown in step (a) of [Algorithm 2] as input to this function
    Crypto::Hash::MD5 md5;
    md5.update(standard_encryption_key_padding_bytes);

    // e) Pass the first element of the file's file identifier array to the MD5
    //    hash function.
    auto id_array = MUST(m_document->trailer()->get_array(m_document, CommonNames::ID));
    auto first_element_string = MUST(id_array->get_string_at(m_document, 0))->string();
    md5.update(first_element_string);

    // d) Encrypt the 16-byte result of the hash, using an RC4 encryption function
    //    with the encryption key from step (a).
    RC4 rc4(encryption_key);
    auto out = md5.peek();
    auto buffer = rc4.encrypt(out.bytes());

    // e) Do the following 19 times:
    //
    //    Take the output from the previous invocation of the RC4 function and pass
    //    it as input to a new invocation of the function; use an encryption key generated
    //    by taking each byte of the original encryption key obtained in step (a) and
    //    performing an XOR operation between the that byte and the single-byte value of
    //    the iteration counter (from 1 to 19).
    auto new_encryption_key = MUST(ByteBuffer::create_uninitialized(encryption_key.size()));
    for (size_t i = 1; i <= 19; i++) {
        for (size_t j = 0; j < encryption_key.size(); j++)
            new_encryption_key[j] = encryption_key[j] ^ i;

        RC4 new_rc4(new_encryption_key);
        buffer = new_rc4.encrypt(buffer);
    }

    // f) Append 16 bytes of the arbitrary padding to the output from the final invocation
    //    of the RC4 function and store the 32-byte result as the value of the U entry in
    //    the encryption dictionary.
    VERIFY(buffer.size() == 16);
    for (size_t i = 0; i < 16; i++)
        buffer.append(0xab);

    return buffer;
}

bool StandardSecurityHandler::try_provide_user_password(StringView password_string)
{
    // Algorithm 6: Authenticating the user password

    // a) Perform all but the last step of [Algorithm 4] or [Algorithm 5] using the
    //    supplied password string.
    ByteBuffer password_buffer = MUST(ByteBuffer::copy(password_string.bytes()));
    if (m_revision == 2) {
        password_buffer = compute_user_password_value<true>(password_buffer);
    } else {
        password_buffer = compute_user_password_value<false>(password_buffer);
    }

    // b) If the result of step (a) is equal to the value of the encryption
    //    dictionary's "U" entry (comparing the first 16 bytes in the case of security
    //    handlers of revision 3 or greater), the password supplied is the correct user
    //    password.
    auto u_bytes = m_u_entry.bytes();
    if (m_revision >= 3)
        return u_bytes.slice(0, 16) == password_buffer.bytes().slice(0, 16);
    return u_bytes == password_buffer.bytes();
}

ByteBuffer StandardSecurityHandler::compute_encryption_key(ByteBuffer password_string)
{
    // This function should never be called after we have a valid encryption key.
    VERIFY(!m_encryption_key.has_value());

    // 7.6.3.3 Encryption Key Algorithm

    // Algorithm 2: Computing an encryption key

    // a) Pad or truncate the password string to exactly 32 bytes. If the password string
    //    is more than 32 bytes long, use only its first 32 bytes; if it is less than 32
    //    bytes long, pad it by appending the required number of additional bytes from the
    //    beginning of the following padding string: [omitted]

    if (password_string.size() > 32) {
        password_string.resize(32);
    } else {
        password_string.append(standard_encryption_key_padding_bytes.data(), 32 - password_string.size());
    }

    // b) Initialize the MD5 hash function and pass the result of step (a) as input to
    //    this function.
    Crypto::Hash::MD5 md5;
    md5.update(password_string);

    // c) Pass the value of the encryption dictionary's "O" entry to the MD5 hash function.
    md5.update(m_o_entry);

    // d) Convert the integer value of the P entry to a 32-bit unsigned binary number and pass
    //    these bytes to the MD5 hash function, low-order byte first.
    md5.update(reinterpret_cast<u8 const*>(&m_flags), sizeof(m_flags));

    // e) Pass the first element of the file's file identifier array to the MD5 hash function.
    auto id_array = MUST(m_document->trailer()->get_array(m_document, CommonNames::ID));
    auto first_element_string = MUST(id_array->get_string_at(m_document, 0))->string();
    md5.update(first_element_string);

    // f) (Security handlers of revision 4 or greater) if the document metadata is not being
    //    encrypted, pass 4 bytes with the value 0xffffffff to the MD5 hash function.
    if (m_revision >= 4 && !m_encrypt_metadata) {
        u32 value = 0xffffffff;
        md5.update(reinterpret_cast<u8 const*>(&value), 4);
    }

    // g) Finish the hash.
    // h) (Security handlers of revision 3 or greater) Do the following 50 times:
    //
    //    Take the output from the previous MD5 hash and pass the first n bytes
    //    of the output as input into a new MD5 hash, where n is the number of
    //    bytes of the encryption key as defined by the value of the encryption
    //    dictionary's Length entry.
    if (m_revision >= 3) {
        ByteBuffer n_bytes;

        for (u32 i = 0; i < 50; i++) {
            Crypto::Hash::MD5 new_md5;
            n_bytes.ensure_capacity(m_length);

            while (n_bytes.size() < m_length) {
                auto out = md5.peek().bytes();
                for (size_t j = 0; j < out.size() && n_bytes.size() < m_length; j++)
                    n_bytes.append(out[j]);
            }

            VERIFY(n_bytes.size() == m_length);
            new_md5.update(n_bytes);
            md5 = move(new_md5);
            n_bytes.clear();
        }
    }

    // i) Set the encryption key to the first n bytes of the output from the final MD5
    //    hash, where n shall always be 5 for security handlers of revision 2 but, for
    //    security handlers of revision 3 or greater, shall depend on the value of the
    //    encryption dictionary's Length entry.
    size_t n;
    if (m_revision == 2) {
        n = 5;
    } else if (m_revision >= 3) {
        n = m_length;
    } else {
        VERIFY_NOT_REACHED();
    }

    ByteBuffer encryption_key;
    encryption_key.ensure_capacity(n);
    while (encryption_key.size() < n) {
        auto out = md5.peek();
        for (size_t i = 0; encryption_key.size() < n && i < out.data_length(); i++)
            encryption_key.append(out.bytes()[i]);
    }

    m_encryption_key = encryption_key;

    return encryption_key;
}

void StandardSecurityHandler::encrypt(NonnullRefPtr<Object> object, Reference reference) const
{
    // 7.6.2 General Encryption Algorithm
    // Algorithm 1: Encryption of data using the RC3 or AES algorithms

    // FIXME: Support AES

    VERIFY(m_encryption_key.has_value());

    // a) Obtain the object number and generation number from the object identifier of
    //    the string or stream to be encrypted. If the string is a direct object, use
    //    the identifier of the indirect object containing it.
    //
    // Note: This is always passed in at parse time because objects don't know their own
    //       object number.

    // b) For all strings and streams with crypt filter specifier; treating the object
    //    number as binary integers, extends the origin n-byte encryption key to n + 5
    //    bytes by appending the low-order 3 bytes of the object number and the low-order
    //    2 bytes of the generation number in that order, low-order byte first. ...

    auto encryption_key = m_encryption_key.value();
    ReadonlyBytes bytes;
    Function<void(ByteBuffer const&)> assign;

    if (object->is<StreamObject>()) {
        auto stream = object->cast<StreamObject>();
        bytes = stream->bytes();

        assign = [&stream](ByteBuffer const& buffer) {
            stream->buffer() = buffer;
        };

        if (stream->dict()->contains(CommonNames::Filter)) {
            auto filter = MUST(stream->dict()->get_name(m_document, CommonNames::Filter))->name();
            if (filter == "Crypt")
                TODO();
        }
    } else if (object->is<StringObject>()) {
        auto string = object->cast<StringObject>();
        bytes = string->string().bytes();
        assign = [&string](ByteBuffer const& buffer) {
            string->set_string(String(buffer.bytes()));
        };
    } else {
        VERIFY_NOT_REACHED();
    }

    auto index = reference.as_ref_index();
    auto generation = reference.as_ref_generation_index();

    encryption_key.append(index & 0xff);
    encryption_key.append((index >> 8) & 0xff);
    encryption_key.append((index >> 16) & 0xff);
    encryption_key.append(generation & 0xff);
    encryption_key.append((generation >> 8) & 0xff);

    // c) Initialize the MD5 hash function and pass the result of step (b) as input to this
    //    function.
    Crypto::Hash::MD5 md5;
    md5.update(encryption_key);

    // d) Use the first (n + 5) bytes, up to a maximum of 16, of the output from the MD5
    //    hash as the key for the RC4 or AES symmetric key algorithms, along with the string
    //    or stream data to be encrypted.
    auto key = MUST(ByteBuffer::copy(md5.peek().bytes()));

    if (key.size() > min(encryption_key.size(), 16))
        key.resize(encryption_key.size());

    RC4 rc4(key);
    auto output = rc4.encrypt(bytes);

    assign(output);
}

void StandardSecurityHandler::decrypt(NonnullRefPtr<Object> object, Reference reference) const
{
    // AES and RC4 are both symmetric, so decryption is the same as encryption
    encrypt(object, reference);
}

static constexpr auto identity_permutation = iota_array<size_t, 256>(0);

RC4::RC4(ReadonlyBytes key)
    : m_bytes(identity_permutation)
{
    size_t j = 0;
    for (size_t i = 0; i < 256; i++) {
        j = (j + m_bytes[i] + key[i % key.size()]) & 0xff;
        swap(m_bytes[i], m_bytes[j]);
    }
}

void RC4::generate_bytes(ByteBuffer& bytes)
{
    size_t i = 0;
    size_t j = 0;

    for (size_t count = 0; count < bytes.size(); count++) {
        i = (i + 1) % 256;
        j = (j + m_bytes[i]) % 256;
        swap(m_bytes[i], m_bytes[j]);
        bytes[count] = m_bytes[(m_bytes[i] + m_bytes[j]) % 256];
    }
}

ByteBuffer RC4::encrypt(ReadonlyBytes bytes)
{
    auto output = MUST(ByteBuffer::create_uninitialized(bytes.size()));
    generate_bytes(output);
    for (size_t i = 0; i < bytes.size(); i++)
        output[i] ^= bytes[i];
    return output;
}

}
