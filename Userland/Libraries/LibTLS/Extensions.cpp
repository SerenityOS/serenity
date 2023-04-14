/*
 * Copyright (c) 2022, stelar7 <dudedbz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTLS/Extensions.h>
#include <LibTLS/TLSRecord.h>

namespace TLS {

SupportedGroups::SupportedGroups()
    : TLSExtension(ExtensionType::SUPPORTED_GROUPS)
{
}

u16 SupportedGroups::size()
{
    return sizeof(ExtensionType) + sizeof(u16) + sizeof(u16) + (sizeof(SupportedGroup) * groups.size());
}

ErrorOr<ByteBuffer> SupportedGroups::encode()
{
    ByteBuffer buffer = TRY(ByteBuffer::create_zeroed(size()));
    ByteReader::store(buffer.offset_pointer(0), static_cast<u16>(type));
    ByteReader::store(buffer.offset_pointer(2), size() - 4);

    ByteReader::store(buffer.offset_pointer(4), (u16)groups.size());
    for (size_t i = 0; i < groups.size(); i++) {
        ByteReader::store(buffer.offset_pointer(5 + (i * 2)), static_cast<u16>(groups[i]));
    }

    return buffer;
}

ErrorOr<RefPtr<TLSExtension>> SupportedGroups::decode(ReadonlyBytes buffer)
{
    size_t offset = 0;

    auto type = static_cast<ExtensionType>(TLSRecord::read_u16(buffer, offset));
    VERIFY(type == ExtensionType::SUPPORTED_GROUPS);
    offset += sizeof(ExtensionType);

    u16 length = TLSRecord::read_u16(buffer, offset);
    offset += sizeof(length);
    if (buffer.size() < length) {
        return AK::Error::from_string_view(enum_to_string(GenericError::NeedMoreData));
    }

    if (length == 0) {
        return TRY(try_make_ref_counted<SupportedGroups>());
    }

    u16 list_bytes = TLSRecord::read_u16(buffer, offset);
    offset += sizeof(list_bytes);
    if (buffer.size() < list_bytes) {
        return AK::Error::from_string_view(enum_to_string(GenericError::NeedMoreData));
    }

    auto curves = TRY(try_make_ref_counted<SupportedGroups>());

    size_t count = list_bytes / sizeof(u16);
    for (size_t i = 0; i < count; i++) {
        auto curve = static_cast<SupportedGroup>(TLSRecord::read_u16(buffer, offset));
        offset += sizeof(SupportedGroup);

        curves->groups.append(curve);
    }

    return curves;
}

ErrorOr<String> SupportedGroups::to_string(size_t indent = 0)
{
    StringBuilder builder;

    builder.append(TRY(String::repeated('\t', indent)));
    builder.appendff("Supported Groups:\n");

    for (auto group : groups) {
        builder.append(TRY(String::repeated('\t', indent + 1)));
        builder.appendff("{}\n", enum_to_string(group));
    }

    return builder.to_string();
}

ECPointFormats::ECPointFormats()
    : TLSExtension(ExtensionType::EC_POINT_FORMATS)
{
}

ErrorOr<RefPtr<TLSExtension>> ECPointFormats::decode(ReadonlyBytes buffer)
{
    auto type = static_cast<ExtensionType>(TLSRecord::read_u16(buffer, 0));
    VERIFY(type == ExtensionType::EC_POINT_FORMATS);

    size_t length = TLSRecord::read_u16(buffer, sizeof(ExtensionType));
    if (buffer.size() < length) {
        return AK::Error::from_string_view(enum_to_string(GenericError::NeedMoreData));
    }

    size_t offset = { 3 };

    auto points = TRY(try_make_ref_counted<ECPointFormats>());
    auto format_count = buffer[offset++];
    for (auto i = 0; i < format_count; i++) {
        auto format = static_cast<ECPointFormat>(buffer[offset++]);
        points->formats.append(format);
    }

    return points;
}
u16 ECPointFormats::size()
{
    return sizeof(ExtensionType) + sizeof(u16) + sizeof(u8) + (sizeof(ECPointFormat) * formats.size());
}

ErrorOr<ByteBuffer> ECPointFormats::encode()
{
    ByteBuffer buffer = TRY(ByteBuffer::create_zeroed(size()));
    ByteReader::store(buffer.offset_pointer(0), static_cast<u16>(type));
    ByteReader::store(buffer.offset_pointer(2), size() - 4);

    ByteReader::store(buffer.offset_pointer(4), (u8)formats.size());
    for (size_t i = 0; i < formats.size(); i++) {
        ByteReader::store(buffer.offset_pointer(5 + i), static_cast<u8>(formats[i]));
    }

    return buffer;
}

ErrorOr<String> ECPointFormats::to_string(size_t indent = 0)
{
    StringBuilder builder;

    builder.append(TRY(String::repeated('\t', indent)));
    builder.appendff("EC Point Formats:\n");

    for (auto format : formats) {
        builder.append(TRY(String::repeated('\t', indent + 1)));
        builder.appendff("{}\n", enum_to_string(format));
    }

    return builder.to_string();
}

SignatureSchemes::SignatureSchemes()
    : TLSExtension(ExtensionType::SIGNATURE_ALGORITHMS)
{
}

u16 SignatureSchemes::size()
{
    return sizeof(ExtensionType) + sizeof(u16) + sizeof(u16) + (sizeof(SignatureScheme) * signatures.size());
}

ErrorOr<ByteBuffer> SignatureSchemes::encode()
{
    ByteBuffer buffer = TRY(ByteBuffer::create_zeroed(size()));
    ByteReader::store(buffer.offset_pointer(0), static_cast<u16>(type));
    ByteReader::store(buffer.offset_pointer(2), size() - 4);

    ByteReader::store(buffer.offset_pointer(4), (u16)signatures.size());
    for (size_t i = 0; i < signatures.size(); i++) {
        ByteReader::store(buffer.offset_pointer(6 + (i * 2)), static_cast<u16>(signatures[i]));
    }

    return buffer;
}

ErrorOr<RefPtr<TLSExtension>> SignatureSchemes::decode(ReadonlyBytes buffer)
{
    size_t offset = 0;

    auto type = static_cast<ExtensionType>(TLSRecord::read_u16(buffer, offset));
    VERIFY(type == ExtensionType::SIGNATURE_ALGORITHMS);
    offset += sizeof(ExtensionType);

    u16 length = TLSRecord::read_u16(buffer, offset);
    offset += sizeof(length);
    if (buffer.size() < length) {
        return AK::Error::from_string_view(enum_to_string(GenericError::NeedMoreData));
    }

    if (length == 0) {
        return TRY(try_make_ref_counted<SignatureSchemes>());
    }

    u16 list_bytes = TLSRecord::read_u16(buffer, offset);
    offset += sizeof(list_bytes);
    if (buffer.size() < list_bytes) {
        return AK::Error::from_string_view(enum_to_string(GenericError::NeedMoreData));
    }

    auto signature_schemes = TRY(try_make_ref_counted<SignatureSchemes>());

    size_t count = list_bytes / sizeof(u16);
    for (size_t i = 0; i < count; i++) {
        auto scheme = static_cast<SignatureScheme>(TLSRecord::read_u16(buffer, offset));
        offset += sizeof(SignatureScheme);

        signature_schemes->signatures.append(scheme);
    }

    return signature_schemes;
}

ErrorOr<String> SignatureSchemes::to_string(size_t indent = 0)
{
    StringBuilder builder;

    builder.append(TRY(String::repeated('\t', indent)));
    builder.appendff("Signature Schemes:\n");

    for (auto signature : signatures) {
        builder.append(TRY(String::repeated('\t', indent + 1)));
        builder.appendff("{}\n", enum_to_string(signature));
    }

    return builder.to_string();
}

EncryptThenMac::EncryptThenMac()
    : TLSExtension(ExtensionType::ENCRYPT_THEN_MAC)
{
}

u16 EncryptThenMac::size()
{
    return sizeof(ExtensionType) + sizeof(u16);
}

ErrorOr<ByteBuffer> EncryptThenMac::encode()
{
    ByteBuffer buffer = TRY(ByteBuffer::create_zeroed(size()));
    ByteReader::store(buffer.offset_pointer(0), static_cast<u16>(type));
    ByteReader::store(buffer.offset_pointer(2), size() - 4);
    return buffer;
}

ErrorOr<String> EncryptThenMac::to_string(size_t indent = 0)
{
    StringBuilder builder;

    builder.append(TRY(String::repeated('\t', indent)));
    builder.appendff("Encrypt Then MAC\n");

    return builder.to_string();
}

SessionTicket::SessionTicket()
    : TLSExtension(ExtensionType::SESSION_TICKET)
{
}

u16 SessionTicket::size()
{
    return sizeof(ExtensionType) + sizeof(u16);
}

ErrorOr<ByteBuffer> SessionTicket::encode()
{
    ByteBuffer buffer = TRY(ByteBuffer::create_zeroed(size()));
    ByteReader::store(buffer.offset_pointer(0), static_cast<u16>(type));
    ByteReader::store(buffer.offset_pointer(2), size() - 4);
    return buffer;
}

ErrorOr<String> SessionTicket::to_string(size_t indent = 0)
{
    StringBuilder builder;

    builder.append(TRY(String::repeated('\t', indent)));
    builder.appendff("Session Ticket\n");

    return builder.to_string();
}

ExtendMasterSecret::ExtendMasterSecret()
    : TLSExtension(ExtensionType::EXTENDED_MASTER_SECRET)
{
}

u16 ExtendMasterSecret::size()
{
    return sizeof(ExtensionType) + sizeof(u16);
}

ErrorOr<ByteBuffer> ExtendMasterSecret::encode()
{
    ByteBuffer buffer = TRY(ByteBuffer::create_zeroed(size()));
    ByteReader::store(buffer.offset_pointer(0), static_cast<u16>(type));
    ByteReader::store(buffer.offset_pointer(2), size() - 4);
    return buffer;
}

ErrorOr<String> ExtendMasterSecret::to_string(size_t indent = 0)
{
    StringBuilder builder;

    builder.append(TRY(String::repeated('\t', indent)));
    builder.appendff("Extend Master Secret\n");

    return builder.to_string();
}

u16 KeyShareEntry::size()
{
    return sizeof(SupportedGroup) + sizeof(u16) + key.size();
}

ErrorOr<String> KeyShareEntry::to_string()
{
    return String::formatted("{}: {:hex-dump}", enum_to_string(group), key.bytes());
}

KeyShares::KeyShares()
    : TLSExtension(ExtensionType::KEY_SHARE)
{
}

u16 KeyShares::size()
{
    auto content_size = 0;
    for (auto& key : keys) {
        content_size += key.size();
    }

    return sizeof(ExtensionType) + sizeof(u16) + content_size;
}

ErrorOr<ByteBuffer> KeyShares::encode()
{
    ByteBuffer buffer = TRY(ByteBuffer::create_zeroed(size()));
    ByteReader::store(buffer.offset_pointer(0), static_cast<u16>(type));
    ByteReader::store(buffer.offset_pointer(2), size() - 4);

    ByteReader::store(buffer.offset_pointer(4), (u16)size() - 4);
    size_t offset = 6;
    for (size_t i = 0; i < keys.size(); i++) {
        KeyShareEntry entry = keys[i];
        ByteReader::store(buffer.offset_pointer(offset), static_cast<u16>(entry.group));
        ByteReader::store(buffer.offset_pointer(offset + 2), (u16)entry.key.size());
        ByteReader::store(buffer.offset_pointer(offset + 4), entry.key.span());
        offset += entry.size();
    }

    return buffer;
}

ErrorOr<String> KeyShares::to_string(size_t indent = 0)
{
    StringBuilder builder;

    builder.append(TRY(String::repeated('\t', indent)));
    builder.appendff("Key Shares:\n");

    for (auto key : keys) {
        builder.append(TRY(String::repeated('\t', indent + 1)));
        builder.appendff("{}\n", TRY(key.to_string()));
    }

    return builder.to_string();
}

SupportedVersions::SupportedVersions()
    : TLSExtension(ExtensionType::SUPPORTED_VERSIONS)
{
}

u16 SupportedVersions::size()
{
    return sizeof(ExtensionType) + sizeof(u16) + (sizeof(ProtocolVersion) * versions.size());
}

ErrorOr<ByteBuffer> SupportedVersions::encode()
{
    ByteBuffer buffer = TRY(ByteBuffer::create_zeroed(size()));
    ByteReader::store(buffer.offset_pointer(0), static_cast<u16>(type));
    ByteReader::store(buffer.offset_pointer(2), size() - 4);

    ByteReader::store(buffer.offset_pointer(4), (u16)versions.size());
    for (size_t i = 0; i < versions.size(); i++) {
        ByteReader::store(buffer.offset_pointer(6 + (i * 2)), static_cast<u16>(versions[i]));
    }

    return buffer;
}

ErrorOr<String> SupportedVersions::to_string(size_t indent = 0)
{
    StringBuilder builder;

    builder.append(TRY(String::repeated('\t', indent)));
    builder.appendff("Supported Versions:\n");

    for (auto version : versions) {
        builder.append(TRY(String::repeated('\t', indent + 1)));
        builder.appendff("{}\n", enum_to_string(version));
    }

    return builder.to_string();
}

RenegotiationInfo::RenegotiationInfo()
    : TLSExtension(ExtensionType::RENEGOTIATION_INFO)
{
}

u16 RenegotiationInfo::size()
{
    return sizeof(ExtensionType) + sizeof(u16) + sizeof(u8) + data.size();
}

ErrorOr<ByteBuffer> RenegotiationInfo::encode()
{
    ByteBuffer buffer = TRY(ByteBuffer::create_zeroed(size()));
    ByteReader::store(buffer.offset_pointer(0), static_cast<u16>(type));
    ByteReader::store(buffer.offset_pointer(2), size() - 4);

    ByteReader::store(buffer.offset_pointer(4), (u8)data.size());
    ByteReader::store(buffer.offset_pointer(5), data.span());

    return buffer;
}

ErrorOr<String> RenegotiationInfo::to_string(size_t indent = 0)
{
    StringBuilder builder;

    builder.append(TRY(String::repeated('\t', indent)));
    builder.appendff("Renegotiation Info:\n");

    builder.append(TRY(String::repeated('\t', indent + 1)));
    builder.appendff("{:hex-dump}\n", data.bytes());

    return builder.to_string();
}

PSKKeyExchangeModes::PSKKeyExchangeModes()
    : TLSExtension(ExtensionType::PSK_KEY_EXCHANGE_MODES)
{
}

u16 PSKKeyExchangeModes::size()
{
    return sizeof(ExtensionType) + sizeof(u16) + sizeof(u8) + (sizeof(PSKKeyExchangeMode) * modes.size());
}

ErrorOr<ByteBuffer> PSKKeyExchangeModes::encode()
{
    ByteBuffer buffer = TRY(ByteBuffer::create_zeroed(size()));
    ByteReader::store(buffer.offset_pointer(0), static_cast<u16>(type));
    ByteReader::store(buffer.offset_pointer(2), size() - 4);

    ByteReader::store(buffer.offset_pointer(4), (u8)modes.size());
    for (size_t i = 0; i < modes.size(); i++) {
        ByteReader::store(buffer.offset_pointer(5 + i), static_cast<u8>(modes[i]));
    }

    return buffer;
}

ErrorOr<String> PSKKeyExchangeModes::to_string(size_t indent = 0)
{
    StringBuilder builder;

    builder.append(TRY(String::repeated('\t', indent)));
    builder.appendff("PSK Key Exchange Modes:\n");

    for (auto mode : modes) {
        builder.append(TRY(String::repeated('\t', indent + 1)));
        builder.appendff("{}\n", enum_to_string(mode));
    }

    return builder.to_string();
}

RecordSizeLimit::RecordSizeLimit()
    : TLSExtension(ExtensionType::RECORD_SIZE_LIMIT)
{
}

u16 RecordSizeLimit::size()
{
    return sizeof(ExtensionType) + sizeof(u16) + sizeof(u16);
}

ErrorOr<ByteBuffer> RecordSizeLimit::encode()
{
    ByteBuffer buffer = TRY(ByteBuffer::create_zeroed(size()));
    ByteReader::store(buffer.offset_pointer(0), static_cast<u16>(type));
    ByteReader::store(buffer.offset_pointer(2), size() - 4);

    ByteReader::store(buffer.offset_pointer(4), 2);
    ByteReader::store(buffer.offset_pointer(6), limit);

    return buffer;
}

ErrorOr<String> RecordSizeLimit::to_string(size_t indent = 0)
{
    StringBuilder builder;

    builder.append(TRY(String::repeated('\t', indent)));
    builder.appendff("Record Size Limit: {}\n", limit);

    return builder.to_string();
}

u16 ServerNameEntry::size()
{
    return sizeof(NameType) + 2 + name.bytes().size();
}

ErrorOr<String> ServerNameEntry::to_string()
{
    return String::formatted("{}: {}", enum_to_string(type), name);
}

ServerNameList::ServerNameList()
    : TLSExtension(ExtensionType::SERVER_NAME)
{
}

ErrorOr<RefPtr<TLSExtension>> ServerNameList::decode(ReadonlyBytes buffer)
{
    size_t offset = 0;

    auto type = static_cast<ExtensionType>(TLSRecord::read_u16(buffer, offset));
    VERIFY(type == ExtensionType::SERVER_NAME);
    offset += sizeof(ExtensionType);

    u16 length = TLSRecord::read_u16(buffer, offset);
    offset += sizeof(length);
    if (buffer.size() < length) {
        return AK::Error::from_string_view(enum_to_string(GenericError::NeedMoreData));
    }

    if (length == 0) {
        return TRY(try_make_ref_counted<ServerNameList>());
    }

    auto name_list_size = TLSRecord::read_u16(buffer, offset);
    offset += sizeof(u16);
    auto name_type = static_cast<NameType>(buffer[offset++]);
    auto name_length = TLSRecord::read_u16(buffer, offset);
    offset += sizeof(u16);

    if (name_type != NameType::HOST_NAME)
        return AK::Error::from_string_view(enum_to_string(GenericError::NotUnderstood));

    // Version 1.2 only allows for a single entry in this list,
    // but earlier versions did not have this limitation.
    auto current_extension_offset = sizeof(name_type) + 2 + name_length;
    if (current_extension_offset != name_list_size)
        return AK::Error::from_string_view(enum_to_string(GenericError::BrokenPacket));

    auto name_view = StringView((char const*)buffer.offset_pointer(offset), (size_t)name_length);
    auto name = TRY(String::from_utf8(name_view));

    auto name_list = TRY(try_make_ref_counted<ServerNameList>());
    name_list->names.append(ServerNameEntry { name_type, name });

    return name_list;
}

u16 ServerNameList::size()
{
    auto content_size = 0;
    for (auto& name : names) {
        content_size += name.size();
    }

    return sizeof(ExtensionType) + sizeof(u16) + content_size;
}

ErrorOr<ByteBuffer> ServerNameList::encode()
{
    ByteBuffer buffer = TRY(ByteBuffer::create_zeroed(size()));
    ByteReader::store(buffer.offset_pointer(0), static_cast<u16>(type));
    ByteReader::store(buffer.offset_pointer(2), size() - 4);

    ByteReader::store(buffer.offset_pointer(4), (u16)names.size());
    auto offset = 6;
    for (size_t i = 0; i < names.size(); i++) {
        ServerNameEntry entry = names[i];
        ByteReader::store(buffer.offset_pointer(offset), static_cast<u8>(entry.type));
        ByteReader::store(buffer.offset_pointer(offset + 1), (u16)entry.name.bytes().size());
        ByteReader::store(buffer.offset_pointer(offset + 3), entry.name.bytes());
        offset += entry.size();
    }

    return buffer;
}

ErrorOr<String> ServerNameList::to_string(size_t indent = 0)
{
    StringBuilder builder;

    builder.append(TRY(String::repeated('\t', indent)));
    builder.appendff("Server Name List:\n");

    for (auto entry : names) {
        builder.append(TRY(String::repeated('\t', indent + 1)));
        builder.appendff("{}: {}\n", enum_to_string(entry.type), entry.name);
    }

    return builder.to_string();
}
}
