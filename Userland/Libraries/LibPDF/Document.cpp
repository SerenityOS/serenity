/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/CommonNames.h>
#include <LibPDF/Document.h>
#include <LibPDF/Parser.h>

namespace PDF {

DeprecatedString OutlineItem::to_deprecated_string(int indent) const
{
    auto indent_str = DeprecatedString::repeated("  "sv, indent + 1);

    StringBuilder child_builder;
    child_builder.append('[');
    for (auto& child : children)
        child_builder.appendff("{}\n", child->to_deprecated_string(indent + 1));
    child_builder.appendff("{}]", indent_str);

    StringBuilder builder;
    builder.append("OutlineItem {{\n"sv);
    builder.appendff("{}title={}\n", indent_str, title);
    builder.appendff("{}count={}\n", indent_str, count);
    builder.appendff("{}dest={}\n", indent_str, dest);
    builder.appendff("{}color={}\n", indent_str, color);
    builder.appendff("{}italic={}\n", indent_str, italic);
    builder.appendff("{}bold={}\n", indent_str, bold);
    builder.appendff("{}children={}\n", indent_str, child_builder.to_deprecated_string());
    builder.appendff("{}}}", DeprecatedString::repeated("  "sv, indent));

    return builder.to_deprecated_string();
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

    RefPtr<DictObject> resources;
    auto maybe_resources_object = TRY(get_inheritable_object(CommonNames::Resources, raw_page_object));
    if (maybe_resources_object.has_value())
        resources = maybe_resources_object.value()->cast<DictObject>();
    else
        resources = adopt_ref(*new DictObject({}));

    RefPtr<Object> contents;
    if (raw_page_object->contains(CommonNames::Contents))
        contents = TRY(raw_page_object->get_object(this, CommonNames::Contents));

    Rectangle media_box;
    auto maybe_media_box_object = TRY(get_inheritable_object(CommonNames::MediaBox, raw_page_object));
    if (maybe_media_box_object.has_value()) {
        auto media_box_array = maybe_media_box_object.value()->cast<ArrayObject>();
        media_box = Rectangle {
            media_box_array->at(0).to_float(),
            media_box_array->at(1).to_float(),
            media_box_array->at(2).to_float(),
            media_box_array->at(3).to_float(),
        };
    } else {
        // As most other libraries seem to do, we default to the standard
        // US letter size of 8.5" x 11" (612 x 792 Postscript units).
        media_box = Rectangle {
            0, 0,
            612, 792
        };
    }

    Rectangle crop_box;
    auto maybe_crop_box_object = TRY(get_inheritable_object(CommonNames::CropBox, raw_page_object));
    if (maybe_crop_box_object.has_value()) {
        auto crop_box_array = maybe_crop_box_object.value()->cast<ArrayObject>();
        crop_box = Rectangle {
            crop_box_array->at(0).to_float(),
            crop_box_array->at(1).to_float(),
            crop_box_array->at(2).to_float(),
            crop_box_array->at(3).to_float(),
        };
    } else {
        crop_box = media_box;
    }

    float user_unit = 1.0f;
    if (raw_page_object->contains(CommonNames::UserUnit))
        user_unit = raw_page_object->get_value(CommonNames::UserUnit).to_float();

    int rotate = 0;
    auto maybe_rotate = TRY(get_inheritable_value(CommonNames::Rotate, raw_page_object));
    if (maybe_rotate.has_value()) {
        rotate = maybe_rotate.value().to_int();
        VERIFY(rotate % 90 == 0);
    }

    Page page { resources.release_nonnull(), move(contents), media_box, crop_box, user_unit, rotate };
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

PDFErrorOr<NonnullRefPtr<Object>> Document::find_in_name_tree(NonnullRefPtr<DictObject> tree, DeprecatedFlyString name)
{
    if (tree->contains(CommonNames::Kids)) {
        return find_in_name_tree_nodes(tree->get_array(CommonNames::Kids), name);
    }
    if (!tree->contains(CommonNames::Names))
        return Error { Error::Type::MalformedPDF, "name tree has neither Kids nor Names" };
    auto key_value_names_array = TRY(tree->get_array(this, CommonNames::Names));
    return find_in_key_value_array(key_value_names_array, name);
}

PDFErrorOr<NonnullRefPtr<Object>> Document::find_in_name_tree_nodes(NonnullRefPtr<ArrayObject> siblings, DeprecatedFlyString name)
{
    for (size_t i = 0; i < siblings->size(); i++) {
        auto sibling = TRY(resolve_to<DictObject>(siblings->at(i)));
        auto limits = sibling->get_array(CommonNames::Limits);
        if (limits->size() != 2)
            return Error { Error::Type::MalformedPDF, "Expected 2-element Limits array" };
        auto start = limits->get_string_at(0);
        auto end = limits->get_string_at(1);
        if (start->string() <= name && end->string() >= name) {
            return find_in_name_tree(sibling, name);
        }
    }
    return Error { Error::Type::MalformedPDF, DeprecatedString::formatted("Didn't find node in name tree containing name {}", name) };
}

PDFErrorOr<NonnullRefPtr<Object>> Document::find_in_key_value_array(NonnullRefPtr<ArrayObject> key_value_array, DeprecatedFlyString name)
{
    if (key_value_array->size() % 2 == 1)
        return Error { Error::Type::MalformedPDF, "key/value array has dangling key" };
    for (size_t i = 0; i < key_value_array->size() / 2; i++) {
        auto key = key_value_array->get_string_at(2 * i);
        if (key->string() == name) {
            return key_value_array->get_object_at(this, 2 * i + 1);
        }
    }
    return Error { Error::Type::MalformedPDF, DeprecatedString::formatted("Didn't find expected name {} in key/value array", name) };
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

    HashMap<u32, u32> page_number_by_index_ref;
    for (u32 page_number = 0; page_number < m_page_object_indices.size(); ++page_number) {
        page_number_by_index_ref.set(m_page_object_indices[page_number], page_number);
    }

    auto first_ref = outline_dict->get_value(CommonNames::First);

    auto children = TRY(build_outline_item_chain(first_ref, page_number_by_index_ref));

    m_outline = adopt_ref(*new OutlineDict());
    m_outline->children = move(children);
    if (outline_dict->contains(CommonNames::Count))
        m_outline->count = outline_dict->get_value(CommonNames::Count).get<int>();

    return {};
}

PDFErrorOr<Destination> Document::create_destination_from_parameters(NonnullRefPtr<ArrayObject> array, HashMap<u32, u32> const& page_number_by_index_ref)
{
    auto page_ref = array->at(0);
    auto type_name = TRY(array->get_name_at(this, 1))->name();

    Vector<Optional<float>> parameters;
    TRY(parameters.try_ensure_capacity(array->size() - 2));
    for (size_t i = 2; i < array->size(); i++) {
        auto& param = array->at(i);
        if (param.has<nullptr_t>())
            parameters.unchecked_append({});
        else
            parameters.append(param.to_float());
    }

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

    return Destination { type, page_number_by_index_ref.get(page_ref.as_ref_index()), parameters };
}

PDFErrorOr<Optional<NonnullRefPtr<Object>>> Document::get_inheritable_object(DeprecatedFlyString const& name, NonnullRefPtr<DictObject> object)
{
    if (!object->contains(name)) {
        if (!object->contains(CommonNames::Parent))
            return { OptionalNone() };
        auto parent = TRY(object->get_dict(this, CommonNames::Parent));
        return get_inheritable_object(name, parent);
    }
    return TRY(object->get_object(this, name));
}

PDFErrorOr<Optional<Value>> Document::get_inheritable_value(DeprecatedFlyString const& name, NonnullRefPtr<DictObject> object)
{
    if (!object->contains(name)) {
        if (!object->contains(CommonNames::Parent))
            return { OptionalNone() };
        auto parent = TRY(object->get_dict(this, CommonNames::Parent));
        return get_inheritable_value(name, parent);
    }
    return object->get(name);
}

PDFErrorOr<Destination> Document::create_destination_from_dictionary_entry(NonnullRefPtr<Object> const& entry, HashMap<u32, u32> const& page_number_by_index_ref)
{
    if (entry->is<ArrayObject>()) {
        auto entry_array = entry->cast<ArrayObject>();
        return create_destination_from_parameters(entry_array, page_number_by_index_ref);
    }
    auto entry_dictionary = entry->cast<DictObject>();
    auto d_array = MUST(entry_dictionary->get_array(this, CommonNames::D));
    return create_destination_from_parameters(d_array, page_number_by_index_ref);
}

PDFErrorOr<NonnullRefPtr<OutlineItem>> Document::build_outline_item(NonnullRefPtr<DictObject> const& outline_item_dict, HashMap<u32, u32> const& page_number_by_index_ref)
{
    auto outline_item = adopt_ref(*new OutlineItem {});

    if (outline_item_dict->contains(CommonNames::First)) {
        VERIFY(outline_item_dict->contains(CommonNames::Last));
        auto first_ref = outline_item_dict->get_value(CommonNames::First);
        auto children = TRY(build_outline_item_chain(first_ref, page_number_by_index_ref));
        for (auto& child : children) {
            child->parent = outline_item;
        }
        outline_item->children = move(children);
    }

    outline_item->title = TRY(outline_item_dict->get_string(this, CommonNames::Title))->string();

    if (outline_item_dict->contains(CommonNames::Count))
        outline_item->count = outline_item_dict->get_value(CommonNames::Count).get<int>();

    if (outline_item_dict->contains(CommonNames::Dest)) {
        auto dest_obj = TRY(outline_item_dict->get_object(this, CommonNames::Dest));

        if (dest_obj->is<ArrayObject>()) {
            auto dest_arr = dest_obj->cast<ArrayObject>();
            outline_item->dest = TRY(create_destination_from_parameters(dest_arr, page_number_by_index_ref));
        } else if (dest_obj->is<NameObject>() || dest_obj->is<StringObject>()) {
            DeprecatedFlyString dest_name;
            if (dest_obj->is<NameObject>())
                dest_name = dest_obj->cast<NameObject>()->name();
            else
                dest_name = dest_obj->cast<StringObject>()->string();
            if (auto dests_value = m_catalog->get(CommonNames::Dests); dests_value.has_value()) {
                auto dests = dests_value.value().get<NonnullRefPtr<Object>>()->cast<DictObject>();
                auto entry = MUST(dests->get_object(this, dest_name));
                outline_item->dest = TRY(create_destination_from_dictionary_entry(entry, page_number_by_index_ref));
            } else if (auto names_value = m_catalog->get(CommonNames::Names); names_value.has_value()) {
                auto names = TRY(resolve(names_value.release_value())).get<NonnullRefPtr<Object>>()->cast<DictObject>();
                if (!names->contains(CommonNames::Dests))
                    return Error { Error::Type::MalformedPDF, "Missing Dests key in document catalogue's Names dictionary" };
                auto dest_obj = TRY(find_in_name_tree(TRY(names->get_dict(this, CommonNames::Dests)), dest_name));
                outline_item->dest = TRY(create_destination_from_dictionary_entry(dest_obj, page_number_by_index_ref));
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

PDFErrorOr<Vector<NonnullRefPtr<OutlineItem>>> Document::build_outline_item_chain(Value const& first_ref, HashMap<u32, u32> const& page_number_by_index_ref)
{
    // We used to receive a last_ref parameter, which was what the parent of this chain
    // thought was this chain's last child. There are documents out there in the wild
    // where this cross-references don't match though, and it seems like simply following
    // the /First and /Next links is the way to go to construct the whole Outline
    // (we already ignore the /Parent attribute too, which can also be out of sync).
    VERIFY(first_ref.has<Reference>());

    Vector<NonnullRefPtr<OutlineItem>> children;

    auto first_value = TRY(get_or_load_value(first_ref.as_ref_index())).get<NonnullRefPtr<Object>>();
    auto first_dict = first_value->cast<DictObject>();
    auto first = TRY(build_outline_item(first_dict, page_number_by_index_ref));
    children.append(first);

    auto current_child_dict = first_dict;
    u32 current_child_index = first_ref.as_ref_index();

    while (current_child_dict->contains(CommonNames::Next)) {
        auto next_child_dict_ref = current_child_dict->get_value(CommonNames::Next);
        current_child_index = next_child_dict_ref.as_ref_index();
        auto next_child_value = TRY(get_or_load_value(current_child_index)).get<NonnullRefPtr<Object>>();
        auto next_child_dict = next_child_value->cast<DictObject>();
        auto next_child = TRY(build_outline_item(next_child_dict, page_number_by_index_ref));
        children.append(next_child);

        current_child_dict = move(next_child_dict);
    }

    return children;
}

}
