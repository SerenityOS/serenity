#!/usr/bin/env python3

# Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
#
# SPDX-License-Identifier: BSD-2-Clause

import argparse
import re
from enum import Enum
from collections import namedtuple
from pathlib import Path
from typing import List, Optional, Type


class EnumWithExportName(Enum):
    @classmethod
    def export_name(cls) -> str:
        return cls.__name__


class TIFFType(EnumWithExportName):
    @classmethod
    def export_name(cls) -> str:
        return "Type"
    Byte = 1
    ASCII = 2
    UnsignedShort = 3
    UnsignedLong = 4
    UnsignedRational = 5
    Undefined = 7
    SignedLong = 9
    SignedRational = 10
    Float = 11
    Double = 12
    UTF8 = 129


class Predictor(EnumWithExportName):
    NoPrediction = 1
    HorizontalDifferencing = 2


class Compression(EnumWithExportName):
    NoCompression = 1
    CCITT = 2
    Group3Fax = 3
    Group4Fax = 4
    LZW = 5
    JPEG = 6
    PackBits = 32773


class PhotometricInterpretation(EnumWithExportName):
    WhiteIsZero = 0
    BlackIsZero = 1
    RGB = 2
    RGBPalette = 3
    TransparencyMask = 4
    CMYK = 5
    YCbCr = 6
    CIELab = 8


tag_fields = ['id', 'types', 'counts', 'default', 'name', 'associated_enum']

Tag = namedtuple(
    'Tag',
    field_names=tag_fields,
    defaults=(None,) * len(tag_fields)
)

# FIXME: Some tag have only a few allowed values, we should ensure that
known_tags: List[Tag] = [
    Tag('256', [TIFFType.UnsignedShort, TIFFType.UnsignedLong], [1], None, "ImageWidth"),
    Tag('257', [TIFFType.UnsignedShort, TIFFType.UnsignedLong], [1], None, "ImageHeight"),
    Tag('258', [TIFFType.UnsignedShort], [], None, "BitsPerSample"),
    Tag('259', [TIFFType.UnsignedShort], [1], None, "Compression", Compression),
    Tag('262', [TIFFType.UnsignedShort], [1], None, "PhotometricInterpretation", PhotometricInterpretation),
    Tag('273', [TIFFType.UnsignedShort, TIFFType.UnsignedLong], [], None, "StripOffsets"),
    Tag('277', [TIFFType.UnsignedShort], [1], None, "SamplesPerPixel"),
    Tag('278', [TIFFType.UnsignedShort, TIFFType.UnsignedLong], [1], None, "RowsPerStrip"),
    Tag('279', [TIFFType.UnsignedShort, TIFFType.UnsignedLong], [], None, "StripByteCounts"),
    Tag('317', [TIFFType.UnsignedShort], [1], Predictor.NoPrediction, "Predictor", Predictor),
    Tag('34675', [TIFFType.Undefined], [], None, "ICCProfile"),
]

HANDLE_TAG_SIGNATURE_TEMPLATE = ("ErrorOr<void> {namespace}handle_tag(Metadata& metadata, u16 tag,"
                                 " {namespace}Type type, u32 count, Vector<{namespace}Value>&& value)")
HANDLE_TAG_SIGNATURE = HANDLE_TAG_SIGNATURE_TEMPLATE.format(namespace="")
HANDLE_TAG_SIGNATURE_TIFF_NAMESPACE = HANDLE_TAG_SIGNATURE_TEMPLATE.format(namespace="TIFF::")

LICENSE = R"""/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */"""


def export_enum_to_cpp(e: Type[EnumWithExportName], special_name: Optional[str] = None) -> str:
    output = f'enum class {e.export_name()} {{\n'

    for entry in e:
        output += f'    {entry.name} = {entry.value},\n'

    output += "};\n"
    return output


def export_enum_to_string_converter(enums: List[Type[EnumWithExportName]]) -> str:
    stringifier_internals = []
    for e in enums:
        single_stringifier = fR"""    if constexpr (IsSame<E, {e.export_name()}>) {{
        switch (value) {{
            default:
                return "Invalid value for {e.export_name()}"sv;"""
        for entry in e:
            single_stringifier += fR"""
            case {e.export_name()}::{entry.name}:
                return "{entry.name}"sv;"""

        single_stringifier += R"""
        }
    }"""
        stringifier_internals.append(single_stringifier)

    stringifier_internals_str = '\n'.join(stringifier_internals)

    out = fR"""template<Enum E>
StringView name_for_enum_tag_value(E value) {{
{stringifier_internals_str}
    VERIFY_NOT_REACHED();
}}"""

    return out


def export_tag_related_enums(tags: List[Tag]) -> str:
    exported_enums = []
    for tag in tags:
        if tag.associated_enum:
            exported_enums.append(export_enum_to_cpp(tag.associated_enum))

    return '\n'.join(exported_enums)


def promote_type(t: TIFFType) -> TIFFType:
    if t == TIFFType.UnsignedShort:
        return TIFFType.UnsignedLong
    return t


def tiff_type_to_cpp(t: TIFFType, without_promotion: bool = False) -> str:
    # To simplify the code generator and the Metadata class API, all u16 are promoted to u32
    # Note that the Value<> type doesn't include u16 for this reason
    if not without_promotion:
        t = promote_type(t)
    if t in [TIFFType.ASCII, TIFFType.UTF8]:
        return 'String'
    if t == TIFFType.Undefined:
        return 'ByteBuffer'
    if t == TIFFType.UnsignedShort:
        return 'u16'
    if t == TIFFType.UnsignedLong:
        return 'u32'
    raise RuntimeError(f'Type "{t}" not recognized, please update tiff_type_to_read_only_cpp()')


def is_container(t: TIFFType) -> bool:
    """
        Some TIFF types are defined on the unit scale but are intended to be used within a collection.
        An example of that are ASCII strings defined as N * byte. Let's intercept that and generate
        a nice API instead of Vector<u8>.
    """
    return t in [TIFFType.ASCII, TIFFType.Byte, TIFFType.Undefined, TIFFType.UTF8]


def export_promoter() -> str:
    output = R"""template<typename T>
struct TypePromoter {
    using Type = T;
};
"""
    specialization_template = R"""template<>
struct TypePromoter<{}> {{
    using Type = {};
}};
"""
    for t in TIFFType:
        if promote_type(t) != t:
            output += specialization_template.format(tiff_type_to_cpp(t, without_promotion=True), tiff_type_to_cpp(t))

    return output


def retrieve_biggest_type(types: List[TIFFType]) -> TIFFType:
    return TIFFType(max([t.value for t in types]))


def pascal_case_to_snake_case(name: str) -> str:
    name = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', name)
    return re.sub('([a-z0-9])([A-Z])', r'\1_\2', name).lower()


def generate_getter(tag: Tag) -> str:
    biggest_type = retrieve_biggest_type(tag.types)
    variant_inner_type = tiff_type_to_cpp(biggest_type)

    extracted_value_template = f"(*possible_value)[{{}}].get<{variant_inner_type}>()"

    tag_final_type = variant_inner_type
    if tag.associated_enum:
        tag_final_type = f"TIFF::{tag.associated_enum.__name__}"
        extracted_value_template = f"static_cast<{tag_final_type}>({extracted_value_template})"

    if len(tag.counts) == 1 and tag.counts[0] == 1 or is_container(biggest_type):
        return_type = tag_final_type
        if is_container(biggest_type):
            return_type += ' const&'
        unpacked_if_needed = f"return {extracted_value_template.format(0)};"
    else:
        if len(tag.counts) == 1:
            container_type = f'Array<{tag_final_type}, {tag.counts[0]}>'
            container_initialization = f'{container_type} tmp{{}};'
        else:
            container_type = f'Vector<{tag_final_type}>'
            container_initialization = fR"""{container_type} tmp{{}};
        auto maybe_failure = tmp.try_resize(possible_value->size());
        if (maybe_failure.is_error())
            return OptionalNone {{}};
        """

        return_type = container_type
        unpacked_if_needed = fR"""
        {container_initialization}
        for (u32 i = 0; i < possible_value->size(); ++i)
            tmp[i] = {extracted_value_template.format('i')};

        return tmp;"""

    signature = fR"    Optional<{return_type}> {pascal_case_to_snake_case(tag.name)}() const"

    body = fR"""
    {{
        auto const& possible_value = m_data.get("{tag.name}"sv);
        if (!possible_value.has_value())
            return OptionalNone {{}};
        {unpacked_if_needed}
    }}
"""

    return signature + body


def generate_metadata_class(tags: List[Tag]) -> str:
    getters = '\n'.join([generate_getter(tag) for tag in tags])

    output = fR"""class Metadata {{
public:
{getters}
private:
    friend {HANDLE_TAG_SIGNATURE_TIFF_NAMESPACE};

    void add_entry(StringView key, Vector<TIFF::Value>&& value) {{
        m_data.set(key, move(value));
    }}

    HashMap<StringView, Vector<TIFF::Value>> m_data;
}};
"""

    return output


def generate_metadata_file(tags: List[Tag]) -> str:
    output = fR"""{LICENSE}

#pragma once

#include <AK/HashMap.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibGfx/Size.h>

namespace Gfx {{

class Metadata;

namespace TIFF {{

{export_enum_to_cpp(TIFFType)}

template<OneOf<u32, i32> x32>
struct Rational {{
    using Type = x32;
    x32 numerator;
    x32 denominator;
}};

{export_promoter()}

// Note that u16 is not include on purpose
using Value = Variant<ByteBuffer, String, u32, Rational<u32>, i32, Rational<i32>>;

{export_tag_related_enums(known_tags)}

{export_enum_to_string_converter([tag.associated_enum for tag in known_tags if tag.associated_enum] + [TIFFType])}

{HANDLE_TAG_SIGNATURE};

}}

{generate_metadata_class(tags)}

}}

template<typename T>
struct AK::Formatter<Gfx::TIFF::Rational<T>> : Formatter<FormatString> {{
    ErrorOr<void> format(FormatBuilder& builder, Gfx::TIFF::Rational<T> value)
    {{
        return Formatter<FormatString>::format(builder, "{{}} ({{}}/{{}})"sv,
            static_cast<double>(value.numerator) / value.denominator, value.numerator, value.denominator);
    }}
}};

template<>
struct AK::Formatter<Gfx::TIFF::Value> : Formatter<FormatString> {{
    ErrorOr<void> format(FormatBuilder& builder, Gfx::TIFF::Value const& value)
    {{
        String content;
        value.visit(
            [&](ByteBuffer const& buffer) {{
                content = MUST(String::formatted("Buffer of size: {{}}"sv, buffer.size()));
            }},
            [&](auto const& other) {{
                content = MUST(String::formatted("{{}}", other));
            }}
        );

        return Formatter<FormatString>::format(builder, "{{}}"sv, content);
    }}
}};
"""
    return output


def generate_tag_handler(tag: Tag) -> str:
    not_in_type_list = f"({' && '.join([f'type != Type::{t.name}' for t in tag.types])})"

    not_in_count_list = ''
    if len(tag.counts) != 0:
        not_in_count_list = f"|| ({' && '.join([f'count != {c}' for c in tag.counts])})"
    pre_condition = fR"""if ({not_in_type_list}
            {not_in_count_list})
            return Error::from_string_literal("TIFFImageDecoderPlugin: Tag {tag.name} invalid");"""

    check_value = ''
    if tag.associated_enum is not None:
        not_in_value_list = f"({' && '.join([f'v != {v.value}' for v in tag.associated_enum])})"
        check_value = fR"""TRY(value[0].visit(
            []({tiff_type_to_cpp(tag.types[0])} const& v) -> ErrorOr<void> {{
                if ({not_in_value_list})
                    return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid value for tag {tag.name}");
                return {{}};
            }},
            [&](auto const&) -> ErrorOr<void> {{
                VERIFY_NOT_REACHED();
            }}));
"""

    output = fR"""    case {tag.id}:
        // {tag.name}

        dbgln_if(TIFF_DEBUG, "{tag.name}({{}}): {{}}", name_for_enum_tag_value(type), format_tiff_value(tag, value));

        {pre_condition}
        {check_value}
        metadata.add_entry("{tag.name}"sv, move(value));
        break;
"""

    return output


def generate_tag_handler_file(tags: List[Tag]) -> str:

    formatter_for_tag_with_enum = '\n'.join([fR"""        case {tag.id}:
            return MUST(String::from_utf8(
                name_for_enum_tag_value(static_cast<{tag.associated_enum.export_name()}>(v.get<u32>()))));"""
                                             for tag in tags if tag.associated_enum])

    output = fR"""{LICENSE}

#include <AK/Debug.h>
#include <AK/String.h>
#include <LibGfx/ImageFormats/TIFFMetadata.h>

namespace Gfx::TIFF {{

static String value_formatter(u32 tag_id, Value const& v) {{
    switch (tag_id) {{
{formatter_for_tag_with_enum}
        default:
            return MUST(String::formatted("{{}}", v));
    }}
}}

[[maybe_unused]] static String format_tiff_value(u32 tag_id, Vector<Value> const& values) {{
    if (values.size() == 1)
        return MUST(String::formatted("{{}}", value_formatter(tag_id, values[0])));

    StringBuilder builder;
    builder.append('[');

    for (u32 i = 0; i < values.size(); ++i) {{
        builder.appendff("{{}}", value_formatter(tag_id, values[i]));
        if (i != values.size() - 1)
            builder.append(", "sv);
    }}

    builder.append(']');
    return MUST(builder.to_string());
}}

{HANDLE_TAG_SIGNATURE}
{{
    switch (tag) {{
"""

    output += '\n'.join([generate_tag_handler(t) for t in tags])

    output += R"""
    default:
        dbgln_if(TIFF_DEBUG, "UnknownTag({}, {}): {}",
                tag, name_for_enum_tag_value(type), format_tiff_value(tag, value));
    }

    return {};
}

}
"""
    return output


def update_file(target: Path, new_content: str):
    should_update = True

    if target.exists():
        with target.open('r') as file:
            content = file.read()
            if content == new_content:
                should_update = False

    if should_update:
        with target.open('w') as file:
            file.write(new_content)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('-o', '--output')
    args = parser.parse_args()

    output_path = Path(args.output)

    update_file(output_path / 'TIFFMetadata.h', generate_metadata_file(known_tags))
    update_file(output_path / 'TIFFTagHandler.cpp', generate_tag_handler_file(known_tags))


if __name__ == '__main__':
    main()
