/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Unicode {

enum class CalendarFormatType : u8;
enum class CalendarPatternStyle : u8;
enum class CompactNumberFormatType : u8;
enum class Condition : u8;
enum class GeneralCategory : u8;
enum class HourCycle : u8;
enum class Language : u8;
enum class ListPatternStyle : u8;
enum class ListPatternType : u8;
enum class Locale : u16;
enum class Property : u8;
enum class Script : u8;
enum class StandardNumberFormatType : u8;
enum class Style : u8;
enum class Territory : u8;
enum class WordBreakProperty : u8;

struct CalendarFormat;
struct CalendarPattern;
struct CurrencyCode;
struct Keyword;
struct LanguageID;
struct ListPatterns;
struct LocaleExtension;
struct LocaleID;
struct NumberFormat;
struct NumberGroupings;
struct OtherExtension;
struct SpecialCasing;
struct TransformedExtension;
struct TransformedField;
struct UnicodeData;

}
