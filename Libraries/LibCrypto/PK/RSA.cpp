/*
 * Copyright (c) 2020, Ali Mohammad Pur <ali.mpfard@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Random.h>
#include <LibCrypto/ASN1/ASN1.h>
#include <LibCrypto/ASN1/DER.h>
#include <LibCrypto/ASN1/PEM.h>
#include <LibCrypto/PK/RSA.h>

namespace Crypto {
namespace PK {

RSA::KeyPairType RSA::parse_rsa_key(ReadonlyBytes in)
{
    // we are going to assign to at least one of these
    KeyPairType keypair;
    // TODO: move ASN parsing logic out
    u64 t, x, y, z, tmp_oid[16];
    u8 tmp_buf[4096] { 0 };
    UnsignedBigInteger n, e, d;
    ASN1::List pubkey_hash_oid[2], pubkey[2];

    ASN1::set(pubkey_hash_oid[0], ASN1::Kind::ObjectIdentifier, tmp_oid, sizeof(tmp_oid) / sizeof(tmp_oid[0]));
    ASN1::set(pubkey_hash_oid[1], ASN1::Kind::Null, nullptr, 0);

    // DER is weird in that it stores pubkeys as bitstrings
    // we must first extract that crap
    ASN1::set(pubkey[0], ASN1::Kind::Sequence, &pubkey_hash_oid, 2);
    ASN1::set(pubkey[1], ASN1::Kind::Null, nullptr, 0);

    dbg() << "we were offered " << in.size() << " bytes of input";

    if (der_decode_sequence(in.data(), in.size(), pubkey, 2)) {
        // yay, now we have to reassemble the bitstring to a bytestring
        t = 0;
        y = 0;
        z = 0;
        x = 0;
        for (; x < pubkey[1].size; ++x) {
            y = (y << 1) | tmp_buf[x];
            if (++z == 8) {
                tmp_buf[t++] = (u8)y;
                y = 0;
                z = 0;
            }
        }
        // now the buffer is correct (Sequence { Integer, Integer })
        if (!der_decode_sequence_many<2>(tmp_buf, t,
                ASN1::Kind::Integer, 1, &n,
                ASN1::Kind::Integer, 1, &e)) {
            // something was fucked up
            dbg() << "bad pubkey: " << e << " in " << n;
            return keypair;
        }
        // correct public key
        keypair.public_key.set(n, e);
        return keypair;
    }

    // could be a private key
    if (!der_decode_sequence_many<1>(in.data(), in.size(),
            ASN1::Kind::Integer, 1, &n)) {
        // that's no key
        // that's a death star
        dbg() << "that's a death star";
        return keypair;
    }

    if (n == 0) {
        // it is a private key
        UnsignedBigInteger zero;
        if (!der_decode_sequence_many<4>(in.data(), in.size(),
                ASN1::Kind::Integer, 1, &zero,
                ASN1::Kind::Integer, 1, &n,
                ASN1::Kind::Integer, 1, &e,
                ASN1::Kind::Integer, 1, &d)) {
            dbg() << "bad privkey " << n << " " << e << " " << d;
            return keypair;
        }
        keypair.private_key.set(n, d, e);
        return keypair;
    }
    if (n == 1) {
        // multiprime key, we don't know how to deal with this
        dbg() << "Unsupported key type";
        return keypair;
    }
    // it's a broken public key
    keypair.public_key.set(n, 65537);
    return keypair;
}

void RSA::encrypt(const ByteBuffer& in, ByteBuffer& out)
{
#ifdef CRYPTO_DEBUG
    dbg() << "in size: " << in.size();
#endif
    auto in_integer = UnsignedBigInteger::import_data(in.data(), in.size());
    if (!(in_integer < m_public_key.modulus())) {
        dbg() << "value too large for key";
        out.clear();
        return;
    }
    auto exp = NumberTheory::ModularPower(in_integer, m_public_key.public_exponent(), m_public_key.modulus());
    auto size = exp.export_data(out);
    auto outsize = out.size();
    if (size != outsize) {
        dbg() << "POSSIBLE RSA BUG!!! Size mismatch: " << outsize << " requested but " << size << " bytes generated";
        out = out.slice(outsize - size, size);
    }
}

void RSA::decrypt(const ByteBuffer& in, ByteBuffer& out)
{
    // FIXME: Actually use the private key properly

    auto in_integer = UnsignedBigInteger::import_data(in.data(), in.size());
    auto exp = NumberTheory::ModularPower(in_integer, m_private_key.private_exponent(), m_private_key.modulus());
    auto size = exp.export_data(out);

    auto align = m_private_key.length();
    auto aligned_size = (size + align - 1) / align * align;

    for (auto i = size; i < aligned_size; ++i)
        out[out.size() - i - 1] = 0; // zero the non-aligned values
    out = out.slice(out.size() - aligned_size, aligned_size);
}

void RSA::sign(const ByteBuffer& in, ByteBuffer& out)
{
    auto in_integer = UnsignedBigInteger::import_data(in.data(), in.size());
    auto exp = NumberTheory::ModularPower(in_integer, m_private_key.private_exponent(), m_private_key.modulus());
    auto size = exp.export_data(out);
    out = out.slice(out.size() - size, size);
}

void RSA::verify(const ByteBuffer& in, ByteBuffer& out)
{
    auto in_integer = UnsignedBigInteger::import_data(in.data(), in.size());
    auto exp = NumberTheory::ModularPower(in_integer, m_public_key.public_exponent(), m_public_key.modulus());
    auto size = exp.export_data(out);
    out = out.slice(out.size() - size, size);
}

void RSA::import_private_key(ReadonlyBytes bytes, bool pem)
{
    ByteBuffer buffer;
    if (pem) {
        buffer = decode_pem(bytes);
        bytes = buffer;
    }

    auto key = parse_rsa_key(bytes);
    if (!key.private_key.length()) {
        dbg() << "We expected to see a private key, but we found none";
        ASSERT_NOT_REACHED();
    }
    m_private_key = key.private_key;
}

void RSA::import_public_key(ReadonlyBytes bytes, bool pem)
{
    ByteBuffer buffer;
    if (pem) {
        buffer = decode_pem(bytes);
        bytes = buffer;
    }

    auto key = parse_rsa_key(bytes);
    if (!key.public_key.length()) {
        dbg() << "We expected to see a public key, but we found none";
        ASSERT_NOT_REACHED();
    }
    m_public_key = key.public_key;
}

template<typename HashFunction>
void RSA_EMSA_PSS<HashFunction>::sign(const ByteBuffer& in, ByteBuffer& out)
{
    // -- encode via EMSA_PSS
    auto mod_bits = m_rsa.private_key().modulus().trimmed_length() * sizeof(u32) * 8;

    u8 EM[mod_bits];
    auto EM_buf = ByteBuffer::wrap(EM, mod_bits);
    m_emsa_pss.encode(in, EM_buf, mod_bits - 1);

    // -- sign via RSA
    m_rsa.sign(EM_buf, out);
}

template<typename HashFunction>
VerificationConsistency RSA_EMSA_PSS<HashFunction>::verify(const ByteBuffer& in)
{
    auto mod_bytes = m_rsa.public_key().modulus().trimmed_length() * sizeof(u32);
    if (in.size() != mod_bytes)
        return VerificationConsistency::Inconsistent;

    u8 EM[mod_bytes];
    auto EM_buf = ByteBuffer::wrap(EM, mod_bytes);

    // -- verify via RSA
    m_rsa.verify(in, EM_buf);

    // -- verify via EMSA_PSS
    return m_emsa_pss.verify(in, EM, mod_bytes * 8 - 1);
}

void RSA_PKCS1_EME::encrypt(const ByteBuffer& in, ByteBuffer& out)
{
    auto mod_len = (m_public_key.modulus().trimmed_length() * sizeof(u32) * 8 + 7) / 8;
#ifdef CRYPTO_DEBUG
    dbg() << "key size: " << mod_len;
#endif
    if (in.size() > mod_len - 11) {
        dbg() << "message too long :(";
        out.trim(0);
        return;
    }
    if (out.size() < mod_len) {
        dbg() << "output buffer too small";
        return;
    }

    auto ps_length = mod_len - in.size() - 3;
    u8 ps[ps_length];

    // FIXME: Without this assertion, GCC refuses to compile due to a memcpy overflow(!?)
    ASSERT(ps_length < 16384);

    AK::fill_with_random(ps, ps_length);
    // since arc4random can create zeros (shocking!)
    // we have to go through and un-zero the zeros
    for (size_t i = 0; i < ps_length; ++i)
        while (!ps[i])
            AK::fill_with_random(ps + i, 1);

    u8 paddings[] { 0x00, 0x02 };

    out.overwrite(0, paddings, 2);
    out.overwrite(2, ps, ps_length);
    out.overwrite(2 + ps_length, paddings, 1);
    out.overwrite(3 + ps_length, in.data(), in.size());
    out.trim(3 + ps_length + in.size()); // should be a single block

#ifdef CRYPTO_DEBUG
    dbg() << "padded output size: " << 3 + ps_length + in.size() << " buffer size: " << out.size();
#endif

    RSA::encrypt(out, out);
}
void RSA_PKCS1_EME::decrypt(const ByteBuffer& in, ByteBuffer& out)
{
    auto mod_len = (m_public_key.modulus().trimmed_length() * sizeof(u32) * 8 + 7) / 8;
    if (in.size() != mod_len) {
        dbg() << "decryption error: wrong amount of data: " << in.size();
        out.trim(0);
        return;
    }

    RSA::decrypt(in, out);

    if (out.size() < RSA::output_size()) {
        dbg() << "decryption error: not enough data after decryption: " << out.size();
        out.trim(0);
        return;
    }

    if (out[0] != 0x00) {
        dbg() << "invalid padding byte 0 : " << out[0];
        return;
    }

    if (out[1] != 0x02) {
        dbg() << "invalid padding byte 1" << out[1];
        return;
    }

    size_t offset = 2;
    while (offset < out.size() && out[offset])
        ++offset;

    if (offset == out.size()) {
        dbg() << "garbage data, no zero to split padding";
        return;
    }

    ++offset;

    if (offset - 3 < 8) {
        dbg() << "PS too small";
        return;
    }

    out = out.slice(offset, out.size() - offset);
}

void RSA_PKCS1_EME::sign(const ByteBuffer&, ByteBuffer&)
{
    dbg() << "FIXME: RSA_PKCS_EME::sign";
}
void RSA_PKCS1_EME::verify(const ByteBuffer&, ByteBuffer&)
{
    dbg() << "FIXME: RSA_PKCS_EME::verify";
}
}
}
