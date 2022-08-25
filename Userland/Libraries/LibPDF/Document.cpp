/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/CommonNames.h>
#include <LibPDF/Document.h>
#include <LibPDF/Parser.h>

namespace PDF {

String OutlineItem::to_string(int indent) const
{
    auto indent_str = String::repeated("  "sv, indent + 1);

    StringBuilder child_builder;
    child_builder.append('[');
    for (auto& child : children)
        child_builder.appendff("{}\n", child.to_string(indent + 1));
    child_builder.appendff("{}]", indent_str);

    StringBuilder builder;
    builder.append("OutlineItem {{\n"sv);
    builder.appendff("{}title={}\n", indent_str, title);
    builder.appendff("{}count={}\n", indent_str, count);
    builder.appendff("{}dest={}\n", indent_str, dest);
    builder.appendff("{}color={}\n", indent_str, color);
    builder.appendff("{}italic={}\n", indent_str, italic);
    builder.appendff("{}bold={}\n", indent_str, bold);
    builder.appendff("{}children={}\n", indent_str, child_builder.to_string());
    builder.appendff("{}}}", String::repeated("  "sv, indent));

    return builder.to_string();
}

PDFErrorOr<NonnullRefPtr<Document>> Document::create(ReadonlyBytes bytes)
{
    auto parser = adopt_ref(*new DocumentParser({}, bytes));
    auto document = adopt_ref(*new Document(parser));

    TRY(parser->initialize());

    document->m_trailer = parser->trailer();
    document->m_catalog = TRY(parser->trailer()->get_dict(document, CommonNames::Root));

    if (document->m_trailer->contains(CommonNames::Encrypt)) {
        auto encryption_dict = TRY(document->m_trailer->get_dict(document, CommonNames::Encrypt));
        document->m_security_handler = TRY(SecurityHandler::create(document, encryption_dict));

        // Automatically attempt to unencrypt the document with the empty string. The
        // result is not important; it is the caller's responsibility to ensure the
        // document is unencrypted before calling initialize().
        document->m_security_handler->try_provide_user_password(""sv);
    }

    return document;
}

Document::Document(NonnullRefPtr<DocumentParser> const& parser)
    : m_parser(parser)
{
    m_parser->set_document(this);
}

PDFErrorOr<void> Document::initialize()
{
    if (m_security_handler)
        VERIFY(m_security_handler->has_user_password());

    TRY(build_page_tree());
    TRY(build_outline());

    return {};
}

PDFErrorOr<Value> Document::get_or_load_value(u32 index)
{
    auto value = get_value(index);
    if (!value.has<Empty>()) // FIXME: Use Optional instead?
        return value;

    auto object = TRY(m_parser->parse_object_with_index(index));
    m_values.set(index, object);
    return object;
}

u32 Document::get_first_page_index() const
{
    // FIXME: A PDF can have a different default first page, which
    // should be fetched and returned here
    return 0;
}

u32 Document::get_page_count() const
{
    return m_page_object_indices.size();
}

PDFErrorOr<Page> Document::get_page(u32 index)
{
    VERIFY(index < m_page_object_indices.size());

    auto cached_page = m_pages.get(index);
    if (cached_page.has_value())
        return cached_page.value();

    auto page_object_index = m_page_object_indices[index];
    auto page_object = TRY(get_or_load_value(page_object_index));
    auto raw_page_object = TRY(resolve_to<DictObject>(page_object));

    auto resources = TRY(get_inheritable_object(CommonNames::Resources, raw_page_object))->cast<DictObject>();
    auto contents = TRY(raw_page_object->get_object(this, CommonNames::Contents));

    auto media_box_array = TRY(get_inheritable_object(CommonNames::MediaBox, raw_page_object))->cast<ArrayObject>();
    auto media_box = Rectangle {
        media_box_array->at(0).to_float(),
        media_box_array->at(1).to_float(),
        media_box_array->at(2).to_float(),
        media_box_array->at(3).to_float(),
    };

    auto crop_box = media_box;
    if (raw_page_object->contains(CommonNames::CropBox)) {
        auto crop_box_array = TRY(raw_page_object->get_array(this, CommonNames::CropBox));
        crop_box = Rectangle {
            crop_box_array->at(0).to_float(),
            crop_box_array->at(1).to_float(),
            crop_box_array->at(2).to_float(),
            crop_box_array->at(3).to_float(),
        };
    }

    float user_unit = 1.0f;
    if (raw_page_object->contains(CommonNames::UserUnit))
        user_unit = raw_page_object->get_value(CommonNames::UserUnit).to_float();

    int rotate = 0;
    if (raw_page_object->contains(CommonNames::Rotate)) {
        rotate = raw_page_object->get_value(CommonNames::Rotate).get<int>();
        VERIFY(rotate % 90 == 0);
    }

    Page page { move(resources), move(contents), media_box, crop_box, user_unit, rotate };
    m_pages.set(index, page);
    return page;
}

PDFErrorOr<Value> Document::resolve(Value const& value)
{
    if (value.has<Reference>()) {
        // FIXME: Surely indirect PDF objects can't contain another indirect PDF object,
        // right? Unsure from the spec, but if they can, these return values would have
        // to be wrapped with another resolve() call.
        return get_or_load_value(value.as_ref_index());
    }

    if (!value.has<NonnullRefPtr<Object>>())
        return value;

    auto& obj = value.get<NonnullRefPtr<Object>>();

    if (obj->is<IndirectValue>())
        return static_ptr_cast<IndirectValue>(obj)->value();

    return value;
}

PDFErrorOr<void> Document::build_page_tree()
{
    auto page_tree = TRY(m_catalog->get_dict(this, CommonNames::Pages));
    return add_page_tree_node_to_page_tree(page_tree);
}

PDFErrorOr<void> Document::add_page_tree_node_to_page_tree(NonnullRefPtr<DictObject> const& page_tree)
{
    auto kids_array = TRY(page_tree->get_array(this, CommonNames::Kids));
    auto page_count = page_tree->get(CommonNames::Count).value().get<int>();

    if (static_cast<size_t>(page_count) != kids_array->elements().size()) {
        // This page tree contains child page trees, so we recursively add
        // these pages to the overall page tree

        for (auto& value : *kids_array) {
            auto reference_index = value.as_ref_index();
            auto maybe_page_tree_node = TRY(m_parser->conditionally_parse_page_tree_node(reference_index));
            if (maybe_page_tree_node) {
                TRY(add_page_tree_node_to_page_tree(maybe_page_tree_node.release_nonnull()));
            } else {
                m_page_object_indices.append(reference_index);
            }
        }
    } else {
        // We know all of the kids are leaf nodes
        for (auto& value : *kids_array)
            m_page_object_indices.append(value.as_ref_index());
    }

    return {};
}

PDFErrorOr<void> Document::build_outline()
{
    if (!m_catalog->contains(CommonNames::Outlines))
        return {};

    auto outline_dict = TRY(m_catalog->get_dict(this, CommonNames::Outlines));
    if (!outline_dict->contains(CommonNames::First))
        return {};
    if (!outline_dict->contains(CommonNames::Last))
        return {};

    auto first_ref = outline_dict->get_value(CommonNames::First);
    auto last_ref = outline_dict->get_value(CommonNames::Last);

    auto children = TRY(build_outline_item_chain(first_ref, last_ref));

    m_outline = adopt_ref(*new OutlineDict());
    m_outline->children = move(children);

    if (outline_dict->contains(CommonNames::Count))
        m_outline->count = outline_dict->get_value(CommonNames::Count).get<int>();

    return {};
}

PDFErrorOr<Destination> Document::create_destination_from_parameters(NonnullRefPtr<ArrayObject> array)
{
    auto page_ref = array->at(0);
    auto type_name = TRY(array->get_name_at(this, 1))->name();

    Vector<float> parameters;
    for (size_t i = 2; i < array->size(); i++)
        parameters.append(array->at(i).to_float());

    Destination::Type type;
    if (type_name == CommonNames::XYZ) {
        type = Destination::Type::XYZ;
    } else if (type_name == CommonNames::Fit) {
        type = Destination::Type::Fit;
    } else if (type_name == CommonNames::FitH) {
        type = Destination::Type::FitH;
    } else if (type_name == CommonNames::FitV) {
        type = Destination::Type::FitV;
    } else if (type_name == CommonNames::FitR) {
        type = Destination::Type::FitR;
    } else if (type_name == CommonNames::FitB) {
        type = Destination::Type::FitB;
    } else if (type_name == CommonNames::FitBH) {
        type = Destination::Type::FitBH;
    } else if (type_name == CommonNames::FitBV) {
        type = Destination::Type::FitBV;
    } else {
        VERIFY_NOT_REACHED();
    }

    return Destination { type, page_ref, parameters };
}

PDFErrorOr<NonnullRefPtr<Object>> Document::get_inheritable_object(FlyString const& name, NonnullRefPtr<DictObject> object)
{
    if (!object->contains(name)) {
        auto parent = TRY(object->get_dict(this, CommonNames::Parent));
        return get_inheritable_object(name, parent);
    }
    return object->get_object(this, name);
}

PDFErrorOr<NonnullRefPtr<OutlineItem>> Document::build_outline_item(NonnullRefPtr<DictObject> const& outline_item_dict)
{
    auto outline_item = adopt_ref(*new OutlineItem {});

    if (outline_item_dict->contains(CommonNames::First)) {
        VERIFY(outline_item_dict->contains(CommonNames::Last));
        auto first_ref = outline_item_dict->get_value(CommonNames::First);
        auto last_ref = outline_item_dict->get_value(CommonNames::Last);

        auto children = TRY(build_outline_item_chain(first_ref, last_ref));
        outline_item->children = move(children);
    }

    outline_item->title = TRY(outline_item_dict->get_string(this, CommonNames::Title))->string();

    if (outline_item_dict->contains(CommonNames::Count))
        outline_item->count = outline_item_dict->get_value(CommonNames::Count).get<int>();

    if (outline_item_dict->contains(CommonNames::Dest)) {
        auto dest_obj = TRY(outline_item_dict->get_object(this, CommonNames::Dest));

        if (dest_obj->is<ArrayObject>()) {
            auto dest_arr = dest_obj->cast<ArrayObject>();
            outline_item->dest = TRY(create_destination_from_parameters(dest_arr));
        } else if (dest_obj->is<NameObject>()) {
            auto dest_name = dest_obj->cast<NameObject>()->name();
            if (auto dests_value = m_catalog->get(CommonNames::Dests); dests_value.has_value()) {
                auto dests = dests_value.value().get<NonnullRefPtr<Object>>()->cast<DictObject>();
                auto entry = MUST(dests->get_object(this, dest_name));
                if (entry->is<ArrayObject>()) {
                    auto entry_array = entry->cast<ArrayObject>();
                    outline_item->dest = TRY(create_destination_from_parameters(entry_array));
                } else {
                    auto entry_dictionary = entry->cast<DictObject>();
                    auto d_array = MUST(entry_dictionary->get_array(this, CommonNames::D));
                    outline_item->dest = TRY(create_destination_from_parameters(d_array));
                }
            } else {
                return Error { Error::Type::MalformedPDF, "Malformed outline destination" };
            }
        }
    }

    if (outline_item_dict->contains(CommonNames::C)) {
        auto color_array = TRY(outline_item_dict->get_array(this, CommonNames::C));
        auto r = static_cast<int>(255.0f * color_array->at(0).get<float>());
        auto g = static_cast<int>(255.0f * color_array->at(1).get<float>());
        auto b = static_cast<int>(255.0f * color_array->at(2).get<float>());
        outline_item->color = Color(r, g, b);
    }

    if (outline_item_dict->contains(CommonNames::F)) {
        auto bitfield = outline_item_dict->get_value(CommonNames::F).get<int>();
        outline_item->italic = bitfield & 0x1;
        outline_item->bold = bitfield & 0x2;
    }

    return outline_item;
}

PDFErrorOr<NonnullRefPtrVector<OutlineItem>> Document::build_outline_item_chain(Value const& first_ref, Value const& last_ref)
{
    VERIFY(first_ref.has<Reference>());
    VERIFY(last_ref.has<Reference>());

    NonnullRefPtrVector<OutlineItem> children;

    auto first_value = TRY(get_or_load_value(first_ref.as_ref_index())).get<NonnullRefPtr<Object>>();
    auto first_dict = first_value->cast<DictObject>();
    auto first = TRY(build_outline_item(first_dict));
    children.append(first);

    auto current_child_dict = first_dict;
    u32 current_child_index = first_ref.as_ref_index();

    while (current_child_dict->contains(CommonNames::Next)) {
        auto next_child_dict_ref = current_child_dict->get_value(CommonNames::Next);
        current_child_index = next_child_dict_ref.as_ref_index();
        auto next_child_value = TRY(get_or_load_value(current_child_index)).get<NonnullRefPtr<Object>>();
        auto next_child_dict = next_child_value->cast<DictObject>();
        auto next_child = TRY(build_outline_item(next_child_dict));
        children.append(next_child);

        current_child_dict = move(next_child_dict);
    }

    VERIFY(last_ref.as_ref_index() == current_child_index);

    return children;
}

}
