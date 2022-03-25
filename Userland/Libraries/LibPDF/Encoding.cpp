/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf8View.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Encoding.h>

namespace PDF {

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

    // Build a String -> Character mapping for handling the differences map
    HashMap<String, CharDescriptor> base_encoding_name_mapping;

    for (auto& [code_point, descriptor] : base_encoding->descriptors()) {
        encoding->m_descriptors.set(code_point, descriptor);
        base_encoding_name_mapping.set(descriptor.name, descriptor);
    }

    auto differences_array = TRY(dict->get_array(document, CommonNames::Differences));
    HashMap<u16, String> differences_map;

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

            auto character = base_encoding_name_mapping.get(name);
            // FIXME: This should always have a value. This does cause crashes in certain
            //        documents, so we must be missing something here.
            if (character.has_value())
                encoding->m_descriptors.set(current_code_point, character.value());

            current_code_point++;
        }
    }

    return encoding;
}

NonnullRefPtr<Encoding> Encoding::standard_encoding()
{
    static NonnullRefPtr<Encoding> encoding = adopt_ref(*new Encoding());
    if (encoding->m_descriptors.is_empty()) {
#define ENUMERATE(string, name, standard_code, mac_code, win_code, pdf_code) \
    auto name##_code_point = *Utf8View(StringView(string)).begin();          \
    encoding->m_descriptors.set(standard_code, { string, name##_code_point });
        ENUMERATE_LATIN_CHARACTER_SET(ENUMERATE)
#undef ENUMERATE
    }

    return encoding;
}

NonnullRefPtr<Encoding> Encoding::mac_encoding()
{
    static NonnullRefPtr<Encoding> encoding = adopt_ref(*new Encoding());
    if (encoding->m_descriptors.is_empty()) {
#define ENUMERATE(string, name, standard_code, mac_code, win_code, pdf_code) \
    auto name##_code_point = *Utf8View(StringView(string)).begin();          \
    encoding->m_descriptors.set(mac_code, { string, name##_code_point });
        ENUMERATE_LATIN_CHARACTER_SET(ENUMERATE)
#undef ENUMERATE
    }

    return encoding;
}

NonnullRefPtr<Encoding> Encoding::windows_encoding()
{
    static NonnullRefPtr<Encoding> encoding = adopt_ref(*new Encoding());
    if (encoding->m_descriptors.is_empty()) {
#define ENUMERATE(string, name, standard_code, mac_code, win_code, pdf_code) \
    auto name##_code_point = *Utf8View(StringView(string)).begin();          \
    encoding->m_descriptors.set(win_code, { string, name##_code_point });
        ENUMERATE_LATIN_CHARACTER_SET(ENUMERATE)
#undef ENUMERATE
    }

    return encoding;
}

NonnullRefPtr<Encoding> Encoding::pdf_doc_encoding()
{
    static NonnullRefPtr<Encoding> encoding = adopt_ref(*new Encoding());
    if (encoding->m_descriptors.is_empty()) {
#define ENUMERATE(string, name, standard_code, mac_code, win_code, pdf_code) \
    auto name##_code_point = *Utf8View(StringView(string)).begin();          \
    encoding->m_descriptors.set(pdf_code, { string, name##_code_point });
        ENUMERATE_LATIN_CHARACTER_SET(ENUMERATE)
#undef ENUMERATE
    }

    return encoding;
}

NonnullRefPtr<Encoding> Encoding::symbol_encoding()
{
    static NonnullRefPtr<Encoding> encoding = adopt_ref(*new Encoding());
    if (encoding->m_descriptors.is_empty()) {
#define ENUMERATE(string, name, code)                               \
    auto name##_code_point = *Utf8View(StringView(string)).begin(); \
    encoding->m_descriptors.set(code, { string, name##_code_point });
        ENUMERATE_SYMBOL_CHARACTER_SET(ENUMERATE)
#undef ENUMERATE
    }

    return encoding;
}

NonnullRefPtr<Encoding> Encoding::zapf_encoding()
{
    static NonnullRefPtr<Encoding> encoding = adopt_ref(*new Encoding());
    if (encoding->m_descriptors.is_empty()) {
#define ENUMERATE(string, name, code)                               \
    auto name##_code_point = *Utf8View(StringView(string)).begin(); \
    encoding->m_descriptors.set(code, { string, name##_code_point });
        ENUMERATE_ZAPF_DINGBATS_CHARACTER_SET(ENUMERATE)
#undef ENUMERATE
    }

    return encoding;
}

CharDescriptor const& Encoding::get_char_code_descriptor(u16 char_code) const
{
    return const_cast<Encoding*>(this)->m_descriptors.ensure(char_code);
}

}
