/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/AbstractView.h>
#include <LibGUI/Model.h>

namespace GUI {

Model::Model()
{
}

Model::~Model()
{
}

void Model::register_view(Badge<AbstractView>, AbstractView& view)
{
    m_views.set(&view);
    m_clients.set(&view);
}

void Model::unregister_view(Badge<AbstractView>, AbstractView& view)
{
    m_views.remove(&view);
    m_clients.remove(&view);
}

void Model::for_each_view(Function<void(AbstractView&)> callback)
{
    for (auto* view : m_views)
        callback(*view);
}

void Model::did_update(unsigned flags)
{
    for (auto* client : m_clients)
        client->model_did_update(flags);
}

ModelIndex Model::create_index(int row, int column, const void* data) const
{
    return ModelIndex(*this, row, column, const_cast<void*>(data));
}

ModelIndex Model::index(int row, int column, const ModelIndex&) const
{
    return create_index(row, column);
}

bool Model::accepts_drag(const ModelIndex&, const Vector<String>&) const
{
    return false;
}

void Model::register_client(ModelClient& client)
{
    m_clients.set(&client);
}

void Model::unregister_client(ModelClient& client)
{
    m_clients.remove(&client);
}

RefPtr<Core::MimeData> Model::mime_data(const ModelSelection& selection) const
{
    auto mime_data = Core::MimeData::construct();
    RefPtr<Gfx::Bitmap> bitmap;

    StringBuilder text_builder;
    StringBuilder data_builder;
    bool first = true;
    selection.for_each_index([&](auto& index) {
        auto text_data = index.data();
        if (!first)
            text_builder.append(", ");
        text_builder.append(text_data.to_string());

        if (!first)
            data_builder.append('\n');
        auto data = index.data(ModelRole::MimeData);
        data_builder.append(data.to_string());

        first = false;

        if (!bitmap) {
            Variant icon_data = index.data(ModelRole::Icon);
            if (icon_data.is_icon())
                bitmap = icon_data.as_icon().bitmap_for_size(32);
        }
    });

    mime_data->set_data(drag_data_type(), data_builder.to_byte_buffer());
    mime_data->set_text(text_builder.to_string());
    if (bitmap)
        mime_data->set_data("image/x-raw-bitmap", bitmap->serialize_to_byte_buffer());

    return mime_data;
}

}
