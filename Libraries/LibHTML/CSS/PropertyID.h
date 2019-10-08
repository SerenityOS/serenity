#pragma once

#include <AK/Traits.h>

namespace CSS {
enum class PropertyID {
    Invalid,

    BackgroundColor,
    BorderBottomColor,
    BorderBottomStyle,
    BorderBottomWidth,
    BorderCollapse,
    BorderLeftColor,
    BorderLeftStyle,
    BorderLeftWidth,
    BorderRightColor,
    BorderRightStyle,
    BorderRightWidth,
    BorderSpacing,
    BorderTopColor,
    BorderTopStyle,
    BorderTopWidth,
    Color,
    Display,
    FontFamily,
    FontSize,
    FontStyle,
    FontVariant,
    FontWeight,
    Height,
    LetterSpacing,
    LineHeight,
    ListStyle,
    ListStyleImage,
    ListStylePosition,
    ListStyleType,
    MarginBottom,
    MarginLeft,
    MarginRight,
    MarginTop,
    PaddingBottom,
    PaddingLeft,
    PaddingRight,
    PaddingTop,
    TextAlign,
    TextDecoration,
    TextIndent,
    TextTransform,
    Visibility,
    WhiteSpace,
    Width,
    WordSpacing,
};
}

namespace AK {
template<>
struct Traits<CSS::PropertyID> : public GenericTraits<CSS::PropertyID> {
    static unsigned hash(CSS::PropertyID property_id) { return int_hash((unsigned)property_id); }
};
}
