/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <LibAudio/LoaderError.h>
#include <LibAudio/Metadata.h>

namespace Audio {

// https://www.xiph.org/vorbis/doc/v-comment.html
ErrorOr<Metadata, LoaderError> load_vorbis_comment(ByteBuffer const& vorbis_comment);
ErrorOr<void> write_vorbis_comment(Metadata const& metadata, Stream& target);

}
