/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SelectItem.h"
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>

template<>
ErrorOr<void> IPC::encode(Encoder& encoder, Web::HTML::SelectItem const& select_item)
{
    TRY(encoder.encode(select_item.type));
    TRY(encoder.encode(select_item.label));
    TRY(encoder.encode(select_item.value));
    TRY(encoder.encode(select_item.items));
    TRY(encoder.encode(select_item.selected));
    return {};
}

template<>
ErrorOr<Web::HTML::SelectItem> IPC::decode(Decoder& decoder)
{
    auto type = TRY(decoder.decode<Web::HTML::SelectItem::Type>());
    auto label = TRY(decoder.decode<Optional<String>>());
    auto value = TRY(decoder.decode<Optional<String>>());
    auto items = TRY(decoder.decode<Optional<Vector<Web::HTML::SelectItem>>>());
    auto selected = TRY(decoder.decode<bool>());
    return Web::HTML::SelectItem { type, move(label), move(value), move(items), selected };
}
