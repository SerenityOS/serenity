/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Files.h"
#include "FixedSizeByteString.h"
#include <AK/DeprecatedString.h>
#include <AK/Stream.h>
#include <AK/URL.h>

namespace BitTorrent {

class MetaInfo {
public:
    static ErrorOr<NonnullOwnPtr<MetaInfo>> create(Stream&);
    URL announce() { return m_announce; }
    Vector<Vector<URL>> announce_list() { return m_announce_list; }
    InfoHash info_hash() const { return m_info_hash; }
    i64 piece_length() { return m_piece_length; }
    Vector<FileInTorrent> files() { return m_files; }
    Optional<DeprecatedString> const& root_dir_name() const { return m_root_dir_name; }

    i64 total_length();

private:
    MetaInfo(InfoHash info_hash);
    URL m_announce;
    Vector<Vector<URL>> m_announce_list;
    InfoHash m_info_hash;
    i64 m_piece_length;
    Vector<FileInTorrent> m_files;
    Optional<DeprecatedString> m_root_dir_name;
    i64 m_total_length = 0;
};
}
