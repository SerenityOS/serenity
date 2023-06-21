/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Plan9FS/FileSystem.h>
#include <Kernel/FileSystem/Plan9FS/Inode.h>
#include <Kernel/FileSystem/Plan9FS/Message.h>

namespace Kernel {

Plan9FSMessage& Plan9FSMessage::operator<<(u8 number)
{
    return append_number(number);
}

Plan9FSMessage& Plan9FSMessage::operator<<(u16 number)
{
    return append_number(number);
}

Plan9FSMessage& Plan9FSMessage::operator<<(u32 number)
{
    return append_number(number);
}

Plan9FSMessage& Plan9FSMessage::operator<<(u64 number)
{
    return append_number(number);
}

Plan9FSMessage& Plan9FSMessage::operator<<(StringView string)
{
    *this << static_cast<u16>(string.length());
    // FIXME: Handle append failure.
    (void)m_builder.append(string);
    return *this;
}

ErrorOr<void> Plan9FSMessage::append_data(StringView data)
{
    *this << static_cast<u32>(data.length());
    TRY(m_builder.append(data));
    return {};
}

Plan9FSMessage::Decoder& Plan9FSMessage::Decoder::operator>>(u8& number)
{
    return read_number(number);
}

Plan9FSMessage::Decoder& Plan9FSMessage::Decoder::operator>>(u16& number)
{
    return read_number(number);
}

Plan9FSMessage::Decoder& Plan9FSMessage::Decoder::operator>>(u32& number)
{
    return read_number(number);
}

Plan9FSMessage::Decoder& Plan9FSMessage::Decoder::operator>>(u64& number)
{
    return read_number(number);
}

Plan9FSMessage::Decoder& Plan9FSMessage::Decoder::operator>>(Plan9FSQIdentifier& qid)
{
    return *this >> qid.type >> qid.version >> qid.path;
}

Plan9FSMessage::Decoder& Plan9FSMessage::Decoder::operator>>(StringView& string)
{
    u16 length;
    *this >> length;
    VERIFY(length <= m_data.length());
    string = m_data.substring_view(0, length);
    m_data = m_data.substring_view_starting_after_substring(string);
    return *this;
}

StringView Plan9FSMessage::Decoder::read_data()
{
    u32 length;
    *this >> length;
    VERIFY(length <= m_data.length());
    auto data = m_data.substring_view(0, length);
    m_data = m_data.substring_view_starting_after_substring(data);
    return data;
}

Plan9FSMessage::Plan9FSMessage(Plan9FS& fs, Type type)
    : m_builder(KBufferBuilder::try_create().release_value()) // FIXME: Don't assume KBufferBuilder allocation success.
    , m_tag(fs.allocate_tag())
    , m_type(type)
    , m_have_been_built(false)
{
    u32 size_placeholder = 0;
    *this << size_placeholder << (u8)type << m_tag;
}

Plan9FSMessage::Plan9FSMessage(NonnullOwnPtr<KBuffer>&& buffer)
    : m_built { move(buffer), Decoder({ buffer->bytes() }) }
    , m_have_been_built(true)
{
    u32 size;
    u8 raw_type;
    *this >> size >> raw_type >> m_tag;
    m_type = (Type)raw_type;
}

Plan9FSMessage::~Plan9FSMessage()
{
    if (m_have_been_built) {
        m_built.buffer.~NonnullOwnPtr<KBuffer>();
        m_built.decoder.~Decoder();
    } else {
        m_builder.~KBufferBuilder();
    }
}

Plan9FSMessage& Plan9FSMessage::operator=(Plan9FSMessage&& message)
{
    m_tag = message.m_tag;
    m_type = message.m_type;

    if (m_have_been_built) {
        m_built.buffer.~NonnullOwnPtr<KBuffer>();
        m_built.decoder.~Decoder();
    } else {
        m_builder.~KBufferBuilder();
    }

    m_have_been_built = message.m_have_been_built;
    if (m_have_been_built) {
        new (&m_built.buffer) NonnullOwnPtr<KBuffer>(move(message.m_built.buffer));
        new (&m_built.decoder) Decoder(move(message.m_built.decoder));
    } else {
        new (&m_builder) KBufferBuilder(move(message.m_builder));
    }

    return *this;
}

KBuffer const& Plan9FSMessage::build()
{
    VERIFY(!m_have_been_built);

    auto tmp_buffer = m_builder.build();

    // FIXME: We should not assume success here.
    VERIFY(tmp_buffer);

    m_have_been_built = true;
    m_builder.~KBufferBuilder();

    new (&m_built.buffer) NonnullOwnPtr<KBuffer>(tmp_buffer.release_nonnull());
    new (&m_built.decoder) Decoder({ m_built.buffer->data(), m_built.buffer->size() });
    u32* size = reinterpret_cast<u32*>(m_built.buffer->data());
    *size = m_built.buffer->size();
    return *m_built.buffer;
}

}
