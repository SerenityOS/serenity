/*
 * Copyright (c) 2021-2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Unicode {

enum class BidiClass;
enum class Block : u16;
enum class EmojiGroup : u8;
enum class GeneralCategory : u8;
enum class GraphemeBreakProperty : u8;
enum class Property : u8;
enum class Script : u8;
enum class SentenceBreakProperty : u8;
enum class WordBreakProperty : u8;

struct CodePointDecomposition;
struct CurrencyCode;
struct Emoji;
struct SpecialCasing;

}
