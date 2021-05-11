/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>

namespace TextCodec {

class Decoder {
public:
    virtual String to_utf8(const StringView&) = 0;

protected:
    virtual ~Decoder() = default;
};

class UTF8Decoder final : public Decoder {
public:
    virtual String to_utf8(const StringView&) override;
};

class UTF16BEDecoder final : public Decoder {
public:
    virtual String to_utf8(const StringView&) override;
};

class Latin1Decoder final : public Decoder {
public:
    virtual String to_utf8(const StringView&) override;
};

class Latin2Decoder final : public Decoder {
public:
    virtual String to_utf8(const StringView&) override;
};

class HebrewDecoder final : public Decoder {
public:
    virtual String to_utf8(const StringView&) override;
};

class CyrillicDecoder final : public Decoder {
public:
    virtual String to_utf8(const StringView&) override;
};

Decoder* decoder_for(const String& encoding);
Optional<String> get_standardized_encoding(const String& encoding);
bool is_standardized_encoding(const String& encoding);

}
