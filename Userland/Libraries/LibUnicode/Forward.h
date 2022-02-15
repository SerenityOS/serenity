/*
 * Copyright (c) 2021-2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Unicode {

enum class Block : u16;
enum class CalendarFormatType : u8;
enum class CalendarPatternStyle : u8;
enum class CalendarSymbol : u8;
enum class CompactNumberFormatType : u8;
enum class Condition : u8;
enum class Currency : u16;
enum class DateField : u8;
enum class DayPeriod : u8;
enum class Era : u8;
enum class GeneralCategory : u8;
enum class GraphemeBreakProperty : u8;
enum class HourCycle : u8;
enum class HourCycleRegion : u8;
enum class Key : u8;
enum class KeywordCalendar : u8;
enum class KeywordColCaseFirst : u8;
enum class KeywordColNumeric : u8;
enum class KeywordNumbers : u8;
enum class Language : u16;
enum class ListPatternStyle : u8;
enum class ListPatternType : u8;
enum class Locale : u16;
enum class Month : u8;
enum class NumericSymbol : u8;
enum class Property : u8;
enum class Script : u8;
enum class ScriptTag : u8;
enum class SentenceBreakProperty : u8;
enum class StandardNumberFormatType : u8;
enum class Style : u8;
enum class Territory : u8;
enum class Weekday : u8;
enum class WordBreakProperty : u8;

struct CalendarFormat;
struct CalendarPattern;
struct CalendarRangePattern;
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
