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

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Hex.h>
#include <AK/Vector.h>
#include <LibBencode/Dictionary.h>
#include <LibBencode/List.h>
#include <LibBencode/Value.h>

class MetaInfo {
public:
    class File {
    public:
        String path() const { return m_path; }
        i64 length() const { return m_length; }

        void set_path(String path) { m_path = path; }
        void set_length(i64 length) { m_length = length; }

    private:
        String m_path;
        i64 m_length { 0 };
    };

    String announce() const { return m_announce; }
    const Vector<Vector<String>> announce_list() const { return m_announce_list; }
    const ByteBuffer info_hash() const { return m_info_hash; }
    String info_hash_hex() const { return m_info_hash_hex; }
    int creation_date() const { return m_creation_date; }
    String comment() const { return m_comment; }
    String created_by() const { return m_created_by; }
    String encoding() const { return m_encoding; }
    int piece_length() const { return m_piece_length; }
    const Vector<ByteBuffer> pieces() const { return m_pieces; }
    int private_() const { return m_private; }
    String name() const { return m_name; }
    const Vector<File> files() const { return m_files; }

    void set_announce(String announce) { m_announce = announce; }
    void set_announce_list(const Vector<Vector<String>> announce_list) { m_announce_list = announce_list; }
    void set_creation_date(int creation_date) { m_creation_date = creation_date; }
    void set_comment(String comment) { m_comment = comment; }
    void set_created_by(String created_by) { m_created_by = created_by; }
    void set_encoding(String encoding) { m_encoding = encoding; }
    void set_info_hash(ByteBuffer info_hash)
    {
        m_info_hash = info_hash;
        m_info_hash_hex = encode_hex(info_hash);
    }
    void set_piece_length(int piece_length) { m_piece_length = piece_length; }
    void set_pieces(const Vector<ByteBuffer> pieces) { m_pieces = pieces; }
    void set_private(int private_) { m_private = private_; }
    void set_name(String name) { m_name = name; }
    void set_files(const Vector<File> files) { m_files = files; }

    static Optional<MetaInfo> from_value(const Bencode::Value&);

private:
    String m_announce;
    Vector<Vector<String>> m_announce_list;
    int m_creation_date { 0 };
    String m_comment;
    String m_created_by;
    String m_encoding;
    ByteBuffer m_info_hash;
    String m_info_hash_hex;
    int m_piece_length { 0 };
    Vector<ByteBuffer> m_pieces;
    int m_private { 0 };
    String m_name;
    Vector<File> m_files;
};
