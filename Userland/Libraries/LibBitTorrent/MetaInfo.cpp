/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MetaInfo.h"
#include "Bencode/BDecoder.h"
#include "Bencode/BEncoder.h"
#include <LibCrypto/Hash/HashManager.h>

namespace BitTorrent {

ErrorOr<NonnullOwnPtr<MetaInfo>> MetaInfo::create(Stream& stream)
{
    auto root = TRY(BDecoder::parse<Dict>(stream));
    auto info_dict = root.get<Dict>("info");

    // Calculating the torrent's info hash by hashing the info dict of the torrent file.
    auto encode_stream = AllocatingMemoryStream();
    TRY(BEncoder::bencode(info_dict, encode_stream));
    size_t buffer_size = encode_stream.used_buffer_size();
    auto info_dict_buffer = TRY(ByteBuffer::create_uninitialized(buffer_size));
    TRY(encode_stream.read_until_filled(info_dict_buffer.bytes()));

    Crypto::Hash::Manager sha1_manager;
    sha1_manager.initialize(Crypto::Hash::HashKind::SHA1);
    sha1_manager.update(info_dict_buffer);

    auto meta_info = TRY(adopt_nonnull_own_or_enomem(new (nothrow) MetaInfo(InfoHash(sha1_manager.digest().bytes()))));

    // TODO: support tracker-less torrent (DHT), some torrent files have no announce url.

    // http://bittorrent.org/beps/bep_0012.html
    if (root.contains("announce-list")) {
        for (auto& tier_list : root.get<List>("announce-list")) {
            auto tier = Vector<URL>();
            for (auto& url : tier_list.get<List>()) {
                tier.append(URL(TRY(DeprecatedString::from_utf8(url.get<ByteBuffer>().bytes()))));
                if (!tier.last().is_valid())
                    return Error::from_string_view(TRY(String::formatted("'{}' is not a valid URL", tier.last())).bytes_as_string_view());
            }
            meta_info->m_announce_list.append(move(tier));
        }
    } else {
        meta_info->m_announce = URL(TRY(root.get_string("announce")));
        if (!meta_info->m_announce.is_valid()) {
            return Error::from_string_view(TRY(String::formatted("'{}' is not a valid URL", meta_info->m_announce)).bytes_as_string_view());
        }
    }

    meta_info->m_piece_length = info_dict.get<i64>("piece length");
    if (info_dict.contains("length")) {
        // single file mode
        meta_info->m_files.empend(FileInTorrent(TRY(info_dict.get_string("name")), info_dict.get<i64>("length")));
        meta_info->m_total_length = info_dict.get<i64>("length");
    } else {
        // multi file mode
        meta_info->m_root_dir_name = TRY(info_dict.get_string("name"));
        auto files = info_dict.get<List>("files");
        for (auto& file : files) {
            auto file_dict = file.get<Dict>();
            auto path = file_dict.get<List>("path");
            StringBuilder path_builder;
            for (auto path_element : path) {
                path_builder.append(TRY(DeprecatedString::from_utf8(path_element.get<ByteBuffer>().bytes())));
                path_builder.append('/');
            }
            path_builder.trim(1);
            i64 length = file_dict.get<i64>("length");
            meta_info->m_files.empend(FileInTorrent(path_builder.to_deprecated_string(), length));
            meta_info->m_total_length += length;
        }
    }

    return meta_info;
}

i64 MetaInfo::total_length()
{
    return m_total_length;
}

MetaInfo::MetaInfo(BitTorrent::InfoHash info_hash)
    : m_info_hash(info_hash)
{
}

}
