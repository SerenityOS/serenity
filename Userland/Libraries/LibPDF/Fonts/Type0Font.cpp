/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/CommonNames.h>
#include <LibPDF/Fonts/Type0Font.h>

namespace PDF {

PDFErrorOr<NonnullRefPtr<Type0Font>> Type0Font::create(Document* document, NonnullRefPtr<DictObject> dict)
{
    // FIXME: Support arbitrary CMaps
    auto cmap_value = TRY(dict->get_object(document, CommonNames::Encoding));
    if (!cmap_value->is<NameObject>() || cmap_value->cast<NameObject>()->name() != CommonNames::IdentityH)
        TODO();

    auto descendant_font_value = TRY(dict->get_array(document, CommonNames::DescendantFonts));
    auto descendant_font = TRY(descendant_font_value->get_dict_at(document, 0));

    auto system_info_dict = TRY(descendant_font->get_dict(document, CommonNames::CIDSystemInfo));
    auto registry = TRY(system_info_dict->get_string(document, CommonNames::Registry))->string();
    auto ordering = TRY(system_info_dict->get_string(document, CommonNames::Ordering))->string();
    u8 supplement = system_info_dict->get_value(CommonNames::Supplement).get<int>();
    CIDSystemInfo system_info { registry, ordering, supplement };

    auto font_descriptor = TRY(descendant_font->get_dict(document, CommonNames::FontDescriptor));

    u16 default_width = 1000;
    if (descendant_font->contains(CommonNames::DW))
        default_width = descendant_font->get_value(CommonNames::DW).to_int();

    HashMap<u16, u16> widths;

    if (descendant_font->contains(CommonNames::W)) {
        auto widths_array = MUST(descendant_font->get_array(document, CommonNames::W));
        Optional<u16> pending_code;

        for (size_t i = 0; i < widths_array->size(); i++) {
            auto& value = widths_array->at(i);
            if (!pending_code.has_value()) {
                pending_code = value.to_int();
            } else if (value.has<NonnullRefPtr<Object>>()) {
                auto array = value.get<NonnullRefPtr<Object>>()->cast<ArrayObject>();
                auto code = pending_code.release_value();
                for (auto& width : *array)
                    widths.set(code++, width.to_int());
            } else {
                auto first_code = pending_code.release_value();
                auto last_code = value.to_int();
                auto width = widths_array->at(i + 1).to_int();
                for (u16 code = first_code; code <= last_code; code++)
                    widths.set(code, width);

                i++;
            }
        }
    }

    if (dict->contains(CommonNames::CIDToGIDMap)) {
        auto value = TRY(dict->get_object(document, CommonNames::CIDToGIDMap));
        if (value->is<StreamObject>()) {
            TODO();
        } else if (value->cast<NameObject>()->name() != "Identity") {
            TODO();
        }
    }

    return adopt_ref(*new Type0Font(system_info, widths, default_width));
}

Type0Font::Type0Font(CIDSystemInfo const& system_info, HashMap<u16, u16> const& widths, u16 missing_width)
    : m_system_info(system_info)
    , m_widths(widths)
    , m_missing_width(missing_width)
{
}

u32 Type0Font::char_code_to_code_point(u16 char_code) const
{
    return char_code;
}

float Type0Font::get_char_width(u16 char_code, float) const
{
    u16 width;
    if (auto char_code_width = m_widths.get(char_code); char_code_width.has_value()) {
        width = char_code_width.value();
    } else {
        width = m_missing_width;
    }

    return static_cast<float>(width) / 1000.0f;
}

}
