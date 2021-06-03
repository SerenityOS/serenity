/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/URL.h>
#include <AK/WeakPtr.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Color.h>
#include <LibWeb/CSS/Length.h>
#include <LibWeb/CSS/PropertyID.h>
#include <LibWeb/CSS/ValueID.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Loader/ImageResource.h>

namespace Web::CSS {

enum class Position {
    Static,
    Relative,
    Absolute,
    Fixed,
    Sticky,
};

enum class TextAlign {
    Left,
    Center,
    Right,
    Justify,
    LibwebCenter,
};

enum class TextDecorationLine {
    None,
    Underline,
    Overline,
    LineThrough,
    Blink,
};

enum class TextTransform {
    None,
    Capitalize,
    Uppercase,
    Lowercase,
    FullWidth,
    FullSizeKana,
};

enum class Display {
    None,
    Block,
    Inline,
    InlineBlock,
    ListItem,
    Table,
    TableRow,
    TableCell,
    TableHeaderGroup,
    TableRowGroup,
    TableFooterGroup,
    TableColumn,
    TableColumnGroup,
    TableCaption,
    Flex,
};

enum class FlexDirection {
    Row,
    RowReverse,
    Column,
    ColumnReverse,
};

enum class FlexWrap {
    Nowrap,
    Wrap,
    WrapReverse
};

enum class FlexBasis {
    Content,
    Length
};

enum class WhiteSpace {
    Normal,
    Pre,
    Nowrap,
    PreLine,
    PreWrap,
};

enum class Float {
    None,
    Left,
    Right,
};

enum class Clear {
    None,
    Left,
    Right,
    Both,
};

enum class Cursor {
    Auto,
    Default,
    None,
    ContextMenu,
    Help,
    Pointer,
    Progress,
    Wait,
    Cell,
    Crosshair,
    Text,
    VerticalText,
    Alias,
    Copy,
    Move,
    NoDrop,
    NotAllowed,
    Grab,
    Grabbing,
    EResize,
    NResize,
    NeResize,
    NwResize,
    SResize,
    SeResize,
    SwResize,
    WResize,
    EwResize,
    NsResize,
    NeswResize,
    NwseResize,
    ColResize,
    RowResize,
    AllScroll,
    ZoomIn,
    ZoomOut,
};

enum class LineStyle {
    None,
    Hidden,
    Dotted,
    Dashed,
    Solid,
    Double,
    Groove,
    Ridge,
    Inset,
    Outset,
};

enum class ListStyleType {
    None,
    Disc,
    Circle,
    Square,
    Decimal,
    DecimalLeadingZero,
    LowerAlpha,
    LowerLatin,
    UpperAlpha,
    UpperLatin,
};

enum class Overflow : u8 {
    Auto,
    Clip,
    Hidden,
    Scroll,
    Visible,
};

enum class Repeat : u8 {
    NoRepeat,
    Repeat,
    Round,
    Space,
};

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
        Position,
        CustomProperty,
        Numeric,
    };

    Type type() const { return m_type; }

    bool is_inherit() const { return type() == Type::Inherit; }
    bool is_initial() const { return type() == Type::Initial; }
    bool is_color() const { return type() == Type::Color; }
    bool is_identifier() const { return type() == Type::Identifier; }
    bool is_image() const { return type() == Type::Image; }
    bool is_string() const { return type() == Type::String; }
    bool is_length() const { return type() == Type::Length; }
    bool is_position() const { return type() == Type::Position; }
    bool is_custom_property() const { return type() == Type::CustomProperty; }
    bool is_numeric() const { return type() == Type::Numeric; }

    virtual String to_string() const = 0;
    virtual Length to_length() const { return Length::make_auto(); }
    virtual Color to_color(const DOM::Document&) const { return {}; }

    CSS::ValueID to_identifier() const;

    virtual bool is_auto() const { return false; }

    bool operator==(const StyleValue& other) const { return equals(other); }
    bool operator!=(const StyleValue& other) const { return !(*this == other); }

    virtual bool equals(const StyleValue& other) const
    {
        if (type() != other.type())
            return false;
        return to_string() == other.to_string();
    }

protected:
    explicit StyleValue(Type);

private:
    Type m_type { Type::Invalid };
};

// FIXME: Allow for fallback
class CustomStyleValue : public StyleValue {
public:
    static NonnullRefPtr<CustomStyleValue> create(const String& custom_property_name)
    {
        return adopt_ref(*new CustomStyleValue(custom_property_name));
    }
    String custom_property_name() const { return m_custom_property_name; }
    String to_string() const override { return m_custom_property_name; }

private:
    explicit CustomStyleValue(const String& custom_property_name)
        : StyleValue(Type::CustomProperty)
        , m_custom_property_name(custom_property_name)
    {
    }

    String m_custom_property_name {};
};

class NumericStyleValue : public StyleValue {
public:
    static NonnullRefPtr<NumericStyleValue> create(float value)
    {
        return adopt_ref(*new NumericStyleValue(value));
    }

    float value() const { return m_value; }
    String to_string() const override { return String::formatted("{}", m_value); }

private:
    explicit NumericStyleValue(float value)
        : StyleValue(Type::Numeric)
        , m_value(value)
    {
    }

    float m_value { 0 };
};

class StringStyleValue : public StyleValue {
public:
    static NonnullRefPtr<StringStyleValue> create(const String& string)
    {
        return adopt_ref(*new StringStyleValue(string));
    }
    virtual ~StringStyleValue() override { }

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
        return adopt_ref(*new LengthStyleValue(length));
    }
    virtual ~LengthStyleValue() override { }

    virtual String to_string() const override { return m_length.to_string(); }
    virtual Length to_length() const override { return m_length; }

    const Length& length() const { return m_length; }

    virtual bool is_auto() const override { return m_length.is_auto(); }

    virtual bool equals(const StyleValue& other) const override
    {
        if (type() != other.type())
            return false;
        return m_length == static_cast<const LengthStyleValue&>(other).m_length;
    }

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
    static NonnullRefPtr<InitialStyleValue> create() { return adopt_ref(*new InitialStyleValue); }
    virtual ~InitialStyleValue() override { }

    String to_string() const override { return "initial"; }

private:
    InitialStyleValue()
        : StyleValue(Type::Initial)
    {
    }
};

class InheritStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<InheritStyleValue> create() { return adopt_ref(*new InheritStyleValue); }
    virtual ~InheritStyleValue() override { }

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
        return adopt_ref(*new ColorStyleValue(color));
    }
    virtual ~ColorStyleValue() override { }

    Color color() const { return m_color; }
    String to_string() const override { return m_color.to_string(); }
    Color to_color(const DOM::Document&) const override { return m_color; }

    virtual bool equals(const StyleValue& other) const override
    {
        if (type() != other.type())
            return false;
        return m_color == static_cast<const ColorStyleValue&>(other).m_color;
    }

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
        return adopt_ref(*new IdentifierStyleValue(id));
    }
    virtual ~IdentifierStyleValue() override { }

    CSS::ValueID id() const { return m_id; }

    virtual String to_string() const override;
    virtual Color to_color(const DOM::Document&) const override;

    virtual bool equals(const StyleValue& other) const override
    {
        if (type() != other.type())
            return false;
        return m_id == static_cast<const IdentifierStyleValue&>(other).m_id;
    }

private:
    explicit IdentifierStyleValue(CSS::ValueID id)
        : StyleValue(Type::Identifier)
        , m_id(id)
    {
    }

    CSS::ValueID m_id { CSS::ValueID::Invalid };
};

class ImageStyleValue final
    : public StyleValue
    , public ImageResourceClient {
public:
    static NonnullRefPtr<ImageStyleValue> create(const URL& url, DOM::Document& document) { return adopt_ref(*new ImageStyleValue(url, document)); }
    virtual ~ImageStyleValue() override { }

    String to_string() const override { return String::formatted("Image({})", m_url.to_string()); }

    const Gfx::Bitmap* bitmap() const { return m_bitmap; }

private:
    ImageStyleValue(const URL&, DOM::Document&);

    // ^ResourceClient
    virtual void resource_did_load() override;

    URL m_url;
    WeakPtr<DOM::Document> m_document;
    RefPtr<Gfx::Bitmap> m_bitmap;
};

inline CSS::ValueID StyleValue::to_identifier() const
{
    if (is_identifier())
        return static_cast<const IdentifierStyleValue&>(*this).id();
    return CSS::ValueID::Invalid;
}

}
