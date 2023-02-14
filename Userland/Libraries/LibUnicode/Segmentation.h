/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace Unicode {

Vector<size_t> find_grapheme_segmentation_boundaries(Utf16View const&);

Vector<size_t> find_word_segmentation_boundaries(Utf8View const&);
Vector<size_t> find_word_segmentation_boundaries(Utf16View const&);

Vector<size_t> find_sentence_segmentation_boundaries(Utf16View const&);

}
