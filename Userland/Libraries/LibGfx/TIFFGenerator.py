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


class TIFFType(Enum):
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


class Predictor(Enum):
    NoPrediction = 1
    HorizontalDifferencing = 2


class Compression(Enum):
    NoCompression = 1
    CCITT = 2
    Group3Fax = 3
    Group4Fax = 4
    LZW = 5
    JPEG = 6
    PackBits = 32773


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
    Tag('273', [TIFFType.UnsignedShort, TIFFType.UnsignedLong], [], None, "StripOffsets"),
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


def export_enum_to_cpp(e: Type[Enum], special_name: Optional[str] = None) -> str:
    output = f'enum class {special_name if special_name else e.__name__} {{\n'

    for entry in e:
        output += f'    {entry.name} = {entry.value},\n'

    output += "};\n"
    return output


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

{export_enum_to_cpp(TIFFType, 'Type')}

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

{HANDLE_TAG_SIGNATURE};

}}

"""

    output += generate_metadata_class(tags)

    output += '\n}\n'

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
        {pre_condition}
        {check_value}
        metadata.add_entry("{tag.name}"sv, move(value));
        break;
"""

    return output


def generate_tag_handler_file(tags: List[Tag]) -> str:
    output = fR"""{LICENSE}

#include <AK/Debug.h>
#include <AK/String.h>
#include <LibGfx/ImageFormats/TIFFMetadata.h>

namespace Gfx::TIFF {{

{HANDLE_TAG_SIGNATURE}
{{
    switch (tag) {{
"""

    output += '\n'.join([generate_tag_handler(t) for t in tags])

    output += R"""
    default:
        dbgln_if(TIFF_DEBUG, "Unknown tag: {}", tag);
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
