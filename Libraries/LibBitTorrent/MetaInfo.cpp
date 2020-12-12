/*
 * Copyright (c) 2020, The SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/ByteBuffer.h>
#include <AK/Vector.h>
#include <LibBencode/Dictionary.h>
#include <LibBencode/List.h>
#include <LibBencode/Value.h>
#include <LibBitTorrent/MetaInfo.h>
#include <LibCrypto/Hash/SHA1.h>
#include <string.h>

Optional<MetaInfo> MetaInfo::from_value(const Bencode::Value& value)
{
    MetaInfo meta_info;

    if (!value.is_dictionary())
        return {};

    auto announce = value.as_dictionary().get("announce");
    if (!announce.is_string())
        return {};
    meta_info.set_announce(announce.as_string());

    if (value.as_dictionary().has("announce-list")) {
        auto announce_list = value.as_dictionary().get("announce-list");
        if (!announce_list.is_list())
            return {};
        Vector<Vector<String>> announce_list_vector;
        for (auto& announce_list_tier : announce_list.as_list().values()) {
            if (!announce_list_tier.is_list())
                return {};
            Vector<String> announce_list_tier_vector;
            for (auto& announce_list_url : announce_list_tier.as_list().values()) {
                if (!announce_list_url.is_string())
                    return {};
                announce_list_tier_vector.append(announce_list_url.as_string());
            }
            announce_list_vector.append(announce_list_tier_vector);
        }
        meta_info.set_announce_list(announce_list_vector);
    }

    if (value.as_dictionary().has("creation date")) {
        auto creation_date = value.as_dictionary().get("creation date");
        if (!creation_date.is_integer())
            return {};
        meta_info.set_creation_date(creation_date.as_integer());
    }

    if (value.as_dictionary().has("comment")) {
        auto comment = value.as_dictionary().get("comment");
        if (!comment.is_string())
            return {};
        meta_info.set_comment(comment.as_string());
    }

    if (value.as_dictionary().has("created by")) {
        auto created_by = value.as_dictionary().get("created by");
        if (!created_by.is_string())
            return {};
        meta_info.set_created_by(created_by.as_string());
    }

    if (value.as_dictionary().has("encoding")) {
        auto encoding = value.as_dictionary().get("encoding");
        if (!encoding.is_string())
            return {};
        meta_info.set_encoding(encoding.as_string());
    }

    auto info = value.as_dictionary().get("info");
    if (!info.is_dictionary())
        return {};

    auto info_hash = Crypto::Hash::SHA1::hash(info.to_string());
    meta_info.set_info_hash(ByteBuffer::copy(info_hash.immutable_data(), info_hash.data_length()));

    auto piece_length = info.as_dictionary().get("piece length");
    if (!piece_length.is_integer())
        return {};
    meta_info.set_piece_length(piece_length.as_integer());

    auto pieces = info.as_dictionary().get("pieces");
    if (!pieces.is_string())
        return {};
    if (pieces.as_string().length() % 20 != 0)
        return {};
    auto pieces_buffer = pieces.as_string().to_byte_buffer();
    Vector<ByteBuffer> pieces_vector;
    for (size_t i = 0; i < pieces_buffer.size() / 20; i++)
        pieces_vector.append(pieces_buffer.slice(i * 20, 20));
    meta_info.set_pieces(pieces_vector);

    if (info.as_dictionary().has("private")) {
        auto private_ = info.as_dictionary().get("private");
        if (!private_.is_integer())
            return {};
        meta_info.set_private(private_.as_integer());
    }

    auto name = info.as_dictionary().get("name");
    if (!name.is_string())
        return {};
    meta_info.set_name(name.as_string());

    Vector<File> files;
    if (info.as_dictionary().has("files")) {
        auto info_files = info.as_dictionary().get("files");
        if (!info_files.is_list())
            return {};
        for (auto& file_entry : info_files.as_list().values()) {
            if (!file_entry.is_dictionary())
                return {};
            if (!file_entry.as_dictionary().get("path").is_list())
                return {};
            if (!file_entry.as_dictionary().get("length").is_integer())
                return {};
            File file;
            file.set_path(file_entry.as_dictionary().get("path").as_string());
            file.set_length(file_entry.as_dictionary().get("length").as_i64());
            files.append(file);
        }
    } else if (info.as_dictionary().has("length")) {
        if (!info.as_dictionary().get("length").is_integer())
            return {};
        File file;
        file.set_length(info.as_dictionary().get("length").as_i64());
        file.set_path(meta_info.name());
        files.append(file);
    } else {
        return {};
    }
    meta_info.set_files(files);

    return meta_info;
}
