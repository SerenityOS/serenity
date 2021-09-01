/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Unicode {

enum class Condition : u8;
enum class GeneralCategory : u8;
enum class Language : u8;
enum class Locale : u16;
enum class Property : u8;
enum class Script : u8;
enum class Territory : u8;
enum class WordBreakProperty : u8;

struct Keyword;
struct LanguageID;
struct LocaleExtension;
struct LocaleID;
struct OtherExtension;
struct SpecialCasing;
struct TransformedExtension;
struct TransformedField;
struct UnicodeData;

}
