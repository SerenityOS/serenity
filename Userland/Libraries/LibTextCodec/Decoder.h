/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Forward.h>
#include <YAK/Function.h>

namespace TextCodec {

class Decoder {
public:
    virtual void process(StringView const&, Function<void(u32)> on_code_point) = 0;
    virtual String to_utf8(StringView const&);

protected:
    virtual ~Decoder() = default;
};

class UTF8Decoder final : public Decoder {
public:
    virtual void process(StringView const&, Function<void(u32)> on_code_point) override;
    virtual String to_utf8(StringView const&) override;
};

class UTF16BEDecoder final : public Decoder {
public:
    virtual void process(StringView const&, Function<void(u32)> on_code_point) override;
    virtual String to_utf8(StringView const&) override;
};

class Latin1Decoder final : public Decoder {
public:
    virtual void process(StringView const&, Function<void(u32)> on_code_point) override;
};

class Latin2Decoder final : public Decoder {
public:
    virtual void process(StringView const&, Function<void(u32)> on_code_point) override;
};

class HebrewDecoder final : public Decoder {
public:
    virtual void process(StringView const&, Function<void(u32)> on_code_point) override;
};

class CyrillicDecoder final : public Decoder {
public:
    virtual void process(StringView const&, Function<void(u32)> on_code_point) override;
};

class Latin9Decoder final : public Decoder {
public:
    virtual void process(StringView const&, Function<void(u32)> on_code_point) override;
};

class TurkishDecoder final : public Decoder {
public:
    virtual void process(StringView const&, Function<void(u32)> on_code_point) override;
};

Decoder* decoder_for(String const& encoding);
Optional<String> get_standardized_encoding(const String& encoding);

}
