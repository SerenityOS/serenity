/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2022, Julian Offenhäuser <offenhaeuser@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf8View.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Encoding.h>

namespace PDF {

NonnullRefPtr<Encoding> Encoding::create()
{
    return adopt_ref(*new Encoding());
}

PDFErrorOr<NonnullRefPtr<Encoding>> Encoding::from_object(Document* document, NonnullRefPtr<Object> const& obj)
{
    if (obj->is<NameObject>()) {
        auto name = obj->cast<NameObject>()->name();
        if (name == "StandardEncoding")
            return standard_encoding();
        if (name == "MacRomanEncoding")
            return mac_encoding();
        if (name == "WinAnsiEncoding")
            return windows_encoding();

        VERIFY_NOT_REACHED();
    }

    // Make a custom encoding
    auto dict = obj->cast<DictObject>();

    RefPtr<Encoding> base_encoding;
    if (dict->contains(CommonNames::BaseEncoding)) {
        auto base_encoding_obj = MUST(dict->get_object(document, CommonNames::BaseEncoding));
        base_encoding = TRY(Encoding::from_object(document, base_encoding_obj));
    } else {
        base_encoding = Encoding::standard_encoding();
    }

    auto encoding = adopt_ref(*new Encoding());

    encoding->m_descriptors = TRY(base_encoding->m_descriptors.clone());
    encoding->m_name_mapping = TRY(base_encoding->m_name_mapping.clone());

    auto differences_array = TRY(dict->get_array(document, CommonNames::Differences));

    u16 current_code_point = 0;
    bool first = true;

    for (auto& item : *differences_array) {
        if (item.has_u32()) {
            current_code_point = item.to_int();
            first = false;
        } else {
            VERIFY(item.has<NonnullRefPtr<Object>>());
            VERIFY(!first);
            auto& object = item.get<NonnullRefPtr<Object>>();
            auto name = object->cast<NameObject>()->name();
            encoding->set(current_code_point, name);
            current_code_point++;
        }
    }

    return encoding;
}

void Encoding::set(CharCodeType char_code, DeprecatedFlyString const& glyph_name)
{
    m_descriptors.set(char_code, glyph_name);
    m_name_mapping.set(glyph_name, char_code);
}

NonnullRefPtr<Encoding> Encoding::standard_encoding()
{
    static NonnullRefPtr<Encoding> encoding = adopt_ref(*new Encoding());
    if (encoding->m_descriptors.is_empty()) {
#define ENUMERATE(name, standard_code, mac_code, win_code, pdf_code) \
    encoding->set(standard_code, #name);
        ENUMERATE_LATIN_CHARACTER_SET(ENUMERATE)
#undef ENUMERATE
    }

    return encoding;
}

NonnullRefPtr<Encoding> Encoding::mac_encoding()
{
    static NonnullRefPtr<Encoding> encoding = adopt_ref(*new Encoding());
    if (encoding->m_descriptors.is_empty()) {
#define ENUMERATE(name, standard_code, mac_code, win_code, pdf_code) \
    encoding->set(mac_code, #name);
        ENUMERATE_LATIN_CHARACTER_SET(ENUMERATE)
#undef ENUMERATE
    }

    return encoding;
}

NonnullRefPtr<Encoding> Encoding::windows_encoding()
{
    static NonnullRefPtr<Encoding> encoding = adopt_ref(*new Encoding());
    if (encoding->m_descriptors.is_empty()) {
#define ENUMERATE(name, standard_code, mac_code, win_code, pdf_code) \
    encoding->set(win_code, #name);
        ENUMERATE_LATIN_CHARACTER_SET(ENUMERATE)
#undef ENUMERATE

        // PDF Annex D table D.2, note 3:
        // In WinAnsiEncoding, all unused codes greater than 40 (octal) map to the bullet character. However, only
        // code 225 (octal) shall be specifically assigned to the bullet character; other codes are subject to future re-assignment.
        //
        // Since CharCodeType is u8 *and* we need to include 255, we iterate in reverse order to have more readable code.
        for (CharCodeType char_code = 255; char_code > 040; char_code--) {
            if (!encoding->m_descriptors.contains(char_code))
                encoding->set(char_code, "bullet");
        }
    }
    return encoding;
}

NonnullRefPtr<Encoding> Encoding::pdf_doc_encoding()
{
    static NonnullRefPtr<Encoding> encoding = adopt_ref(*new Encoding());
    if (encoding->m_descriptors.is_empty()) {
#define ENUMERATE(name, standard_code, mac_code, win_code, pdf_code) \
    encoding->set(pdf_code, #name);
        ENUMERATE_LATIN_CHARACTER_SET(ENUMERATE)
#undef ENUMERATE
    }

    return encoding;
}

NonnullRefPtr<Encoding> Encoding::symbol_encoding()
{
    static NonnullRefPtr<Encoding> encoding = adopt_ref(*new Encoding());
    if (encoding->m_descriptors.is_empty()) {
#define ENUMERATE(name, code) \
    encoding->set(code, #name);
        ENUMERATE_SYMBOL_CHARACTER_SET(ENUMERATE)
#undef ENUMERATE
    }

    return encoding;
}

NonnullRefPtr<Encoding> Encoding::zapf_encoding()
{
    static NonnullRefPtr<Encoding> encoding = adopt_ref(*new Encoding());
    if (encoding->m_descriptors.is_empty()) {
#define ENUMERATE(name, code) \
    encoding->set(code, #name);
        ENUMERATE_ZAPF_DINGBATS_CHARACTER_SET(ENUMERATE)
#undef ENUMERATE
    }
    return encoding;
}

u16 Encoding::get_char_code(DeprecatedString const& name) const
{
    auto code_iterator = m_name_mapping.find(name);
    if (code_iterator != m_name_mapping.end())
        return code_iterator->value;
    return 0;
}

DeprecatedFlyString Encoding::get_name(u8 char_code) const
{
    auto name_iterator = m_descriptors.find(char_code);
    if (name_iterator != m_descriptors.end())
        return name_iterator->value;
    return 0;
}

}
