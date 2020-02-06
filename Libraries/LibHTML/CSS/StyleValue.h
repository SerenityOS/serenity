/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#pragma once

#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/URL.h>
#include <AK/WeakPtr.h>
#include <LibDraw/Color.h>
#include <LibDraw/GraphicsBitmap.h>
#include <LibHTML/CSS/Length.h>
#include <LibHTML/CSS/PropertyID.h>

class Document;

namespace CSS {
enum class ValueID {
    Invalid,
    VendorSpecificLink,
    Center,
    Left,
    Right,
    Justify,
};
}

class StyleValue : public RefCounted<StyleValue> {
public:
    virtual ~StyleValue();

    enum class Type {
        Invalid,
        Inherit,
        Initial,
        String,
        Length,
        Color,
        Identifier,
        Image,
    };

    Type type() const { return m_type; }

    bool is_inherit() const { return type() == Type::Inherit; }
    bool is_initial() const { return type() == Type::Initial; }
    bool is_color() const { return type() == Type::Color; }
    bool is_identifier() const { return type() == Type::Identifier; }
    bool is_image() const { return type() == Type::Image; }
    bool is_string() const { return type() == Type::String; }
    bool is_length() const { return type() == Type::Length; }

    virtual String to_string() const = 0;
    virtual Length to_length() const { return {}; }
    virtual Color to_color(const Document&) const { return {}; }

    virtual bool is_auto() const { return false; }

protected:
    explicit StyleValue(Type);

private:
    Type m_type { Type::Invalid };
};

class StringStyleValue : public StyleValue {
public:
    static NonnullRefPtr<StringStyleValue> create(const String& string)
    {
        return adopt(*new StringStyleValue(string));
    }
    virtual ~StringStyleValue() override {}

    String to_string() const override { return m_string; }

private:
    explicit StringStyleValue(const String& string)
        : StyleValue(Type::String)
        , m_string(string)
    {
    }

    String m_string;
};

class LengthStyleValue : public StyleValue {
public:
    static NonnullRefPtr<LengthStyleValue> create(const Length& length)
    {
        return adopt(*new LengthStyleValue(length));
    }
    virtual ~LengthStyleValue() override {}

    virtual String to_string() const override { return m_length.to_string(); }
    virtual Length to_length() const override { return m_length; }

    const Length& length() const { return m_length; }

    virtual bool is_auto() const override { return m_length.is_auto(); }

private:
    explicit LengthStyleValue(const Length& length)
        : StyleValue(Type::Length)
        , m_length(length)
    {
    }

    Length m_length;
};

class InitialStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<InitialStyleValue> create() { return adopt(*new InitialStyleValue); }
    virtual ~InitialStyleValue() override {}

    String to_string() const override { return "initial"; }

private:
    InitialStyleValue()
        : StyleValue(Type::Initial)
    {
    }
};

class InheritStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<InheritStyleValue> create() { return adopt(*new InheritStyleValue); }
    virtual ~InheritStyleValue() override {}

    String to_string() const override { return "inherit"; }

private:
    InheritStyleValue()
        : StyleValue(Type::Inherit)
    {
    }
};

class ColorStyleValue : public StyleValue {
public:
    static NonnullRefPtr<ColorStyleValue> create(Color color)
    {
        return adopt(*new ColorStyleValue(color));
    }
    virtual ~ColorStyleValue() override {}

    Color color() const { return m_color; }
    String to_string() const override { return m_color.to_string(); }
    Color to_color(const Document&) const override { return m_color; }

private:
    explicit ColorStyleValue(Color color)
        : StyleValue(Type::Color)
        , m_color(color)
    {
    }

    Color m_color;
};

class IdentifierStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<IdentifierStyleValue> create(CSS::ValueID id)
    {
        return adopt(*new IdentifierStyleValue(id));
    }
    virtual ~IdentifierStyleValue() override {}

    CSS::ValueID id() const { return m_id; }

    virtual String to_string() const override;
    virtual Color to_color(const Document&) const override;

private:
    explicit IdentifierStyleValue(CSS::ValueID id)
        : StyleValue(Type::Identifier)
        , m_id(id)
    {
    }

    CSS::ValueID m_id { CSS::ValueID::Invalid };
};

class ImageStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<ImageStyleValue> create(const URL& url, Document& document) { return adopt(*new ImageStyleValue(url, document)); }
    virtual ~ImageStyleValue() override {}

    String to_string() const override { return String::format("Image{%s}", m_url.to_string().characters()); }

    const Gfx::Bitmap* bitmap() const { return m_bitmap; }

private:
    ImageStyleValue(const URL&, Document&);

    URL m_url;
    WeakPtr<Document> m_document;
    RefPtr<Gfx::Bitmap> m_bitmap;
};
