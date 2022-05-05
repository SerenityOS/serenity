/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#define JS_DECLARE_NATIVE_FUNCTION(name) \
    static JS::ThrowCompletionOr<JS::Value> name(JS::VM&, JS::GlobalObject&)

#define JS_DEFINE_NATIVE_FUNCTION(name) \
    JS::ThrowCompletionOr<JS::Value> name([[maybe_unused]] JS::VM& vm, [[maybe_unused]] JS::GlobalObject& global_object)

// NOTE: Proxy is not included here as it doesn't have a prototype - m_proxy_constructor is initialized separately.
#define JS_ENUMERATE_NATIVE_OBJECTS_EXCLUDING_TEMPLATES                                                                                        \
    __JS_ENUMERATE(AggregateError, aggregate_error, AggregateErrorPrototype, AggregateErrorConstructor, void)                                  \
    __JS_ENUMERATE(Array, array, ArrayPrototype, ArrayConstructor, void)                                                                       \
    __JS_ENUMERATE(ArrayBuffer, array_buffer, ArrayBufferPrototype, ArrayBufferConstructor, void)                                              \
    __JS_ENUMERATE(AsyncFunction, async_function, AsyncFunctionPrototype, AsyncFunctionConstructor, void)                                      \
    __JS_ENUMERATE(AsyncGeneratorFunction, async_generator_function, AsyncGeneratorFunctionPrototype, AsyncGeneratorFunctionConstructor, void) \
    __JS_ENUMERATE(BigIntObject, bigint, BigIntPrototype, BigIntConstructor, void)                                                             \
    __JS_ENUMERATE(BooleanObject, boolean, BooleanPrototype, BooleanConstructor, void)                                                         \
    __JS_ENUMERATE(DataView, data_view, DataViewPrototype, DataViewConstructor, void)                                                          \
    __JS_ENUMERATE(Date, date, DatePrototype, DateConstructor, void)                                                                           \
    __JS_ENUMERATE(Error, error, ErrorPrototype, ErrorConstructor, void)                                                                       \
    __JS_ENUMERATE(FinalizationRegistry, finalization_registry, FinalizationRegistryPrototype, FinalizationRegistryConstructor, void)          \
    __JS_ENUMERATE(FunctionObject, function, FunctionPrototype, FunctionConstructor, void)                                                     \
    __JS_ENUMERATE(GeneratorFunction, generator_function, GeneratorFunctionPrototype, GeneratorFunctionConstructor, void)                      \
    __JS_ENUMERATE(Map, map, MapPrototype, MapConstructor, void)                                                                               \
    __JS_ENUMERATE(NumberObject, number, NumberPrototype, NumberConstructor, void)                                                             \
    __JS_ENUMERATE(Object, object, ObjectPrototype, ObjectConstructor, void)                                                                   \
    __JS_ENUMERATE(Promise, promise, PromisePrototype, PromiseConstructor, void)                                                               \
    __JS_ENUMERATE(RegExpObject, regexp, RegExpPrototype, RegExpConstructor, void)                                                             \
    __JS_ENUMERATE(Set, set, SetPrototype, SetConstructor, void)                                                                               \
    __JS_ENUMERATE(ShadowRealm, shadow_realm, ShadowRealmPrototype, ShadowRealmConstructor, void)                                              \
    __JS_ENUMERATE(StringObject, string, StringPrototype, StringConstructor, void)                                                             \
    __JS_ENUMERATE(SymbolObject, symbol, SymbolPrototype, SymbolConstructor, void)                                                             \
    __JS_ENUMERATE(WeakMap, weak_map, WeakMapPrototype, WeakMapConstructor, void)                                                              \
    __JS_ENUMERATE(WeakRef, weak_ref, WeakRefPrototype, WeakRefConstructor, void)                                                              \
    __JS_ENUMERATE(WeakSet, weak_set, WeakSetPrototype, WeakSetConstructor, void)

#define JS_ENUMERATE_NATIVE_OBJECTS                 \
    JS_ENUMERATE_NATIVE_OBJECTS_EXCLUDING_TEMPLATES \
    __JS_ENUMERATE(TypedArray, typed_array, TypedArrayPrototype, TypedArrayConstructor, void)

#define JS_ENUMERATE_NATIVE_ERRORS                                                                                                         \
    __JS_ENUMERATE(EvalError, eval_error, EvalErrorPrototype, EvalErrorConstructor, void)                                                  \
    __JS_ENUMERATE(InternalError, internal_error, InternalErrorPrototype, InternalErrorConstructor, void)                                  \
    __JS_ENUMERATE(InvalidCharacterError, invalid_character_error, InvalidCharacterErrorPrototype, InvalidCharacterErrorConstructor, void) \
    __JS_ENUMERATE(RangeError, range_error, RangeErrorPrototype, RangeErrorConstructor, void)                                              \
    __JS_ENUMERATE(ReferenceError, reference_error, ReferenceErrorPrototype, ReferenceErrorConstructor, void)                              \
    __JS_ENUMERATE(SyntaxError, syntax_error, SyntaxErrorPrototype, SyntaxErrorConstructor, void)                                          \
    __JS_ENUMERATE(TypeError, type_error, TypeErrorPrototype, TypeErrorConstructor, void)                                                  \
    __JS_ENUMERATE(URIError, uri_error, URIErrorPrototype, URIErrorConstructor, void)

#define JS_ENUMERATE_TYPED_ARRAYS                                                                                               \
    __JS_ENUMERATE(Uint8Array, uint8_array, Uint8ArrayPrototype, Uint8ArrayConstructor, u8)                                     \
    __JS_ENUMERATE(Uint8ClampedArray, uint8_clamped_array, Uint8ClampedArrayPrototype, Uint8ClampedArrayConstructor, ClampedU8) \
    __JS_ENUMERATE(Uint16Array, uint16_array, Uint16ArrayPrototype, Uint16ArrayConstructor, u16)                                \
    __JS_ENUMERATE(Uint32Array, uint32_array, Uint32ArrayPrototype, Uint32ArrayConstructor, u32)                                \
    __JS_ENUMERATE(BigUint64Array, big_uint64_array, BigUint64ArrayPrototype, BigUint64ArrayConstructor, u64)                   \
    __JS_ENUMERATE(Int8Array, int8_array, Int8ArrayPrototype, Int8ArrayConstructor, i8)                                         \
    __JS_ENUMERATE(Int16Array, int16_array, Int16ArrayPrototype, Int16ArrayConstructor, i16)                                    \
    __JS_ENUMERATE(Int32Array, int32_array, Int32ArrayPrototype, Int32ArrayConstructor, i32)                                    \
    __JS_ENUMERATE(BigInt64Array, big_int64_array, BigInt64ArrayPrototype, BigInt64ArrayConstructor, i64)                       \
    __JS_ENUMERATE(Float32Array, float32_array, Float32ArrayPrototype, Float32ArrayConstructor, float)                          \
    __JS_ENUMERATE(Float64Array, float64_array, Float64ArrayPrototype, Float64ArrayConstructor, double)

#define JS_ENUMERATE_INTL_OBJECTS                                                                                        \
    __JS_ENUMERATE(Collator, collator, CollatorPrototype, CollatorConstructor)                                           \
    __JS_ENUMERATE(DateTimeFormat, date_time_format, DateTimeFormatPrototype, DateTimeFormatConstructor)                 \
    __JS_ENUMERATE(DisplayNames, display_names, DisplayNamesPrototype, DisplayNamesConstructor)                          \
    __JS_ENUMERATE(ListFormat, list_format, ListFormatPrototype, ListFormatConstructor)                                  \
    __JS_ENUMERATE(Locale, locale, LocalePrototype, LocaleConstructor)                                                   \
    __JS_ENUMERATE(NumberFormat, number_format, NumberFormatPrototype, NumberFormatConstructor)                          \
    __JS_ENUMERATE(PluralRules, plural_rules, PluralRulesPrototype, PluralRulesConstructor)                              \
    __JS_ENUMERATE(RelativeTimeFormat, relative_time_format, RelativeTimeFormatPrototype, RelativeTimeFormatConstructor) \
    __JS_ENUMERATE(Segmenter, segmenter, SegmenterPrototype, SegmenterConstructor)

#define JS_ENUMERATE_TEMPORAL_OBJECTS                                                                    \
    __JS_ENUMERATE(Calendar, calendar, CalendarPrototype, CalendarConstructor)                           \
    __JS_ENUMERATE(Duration, duration, DurationPrototype, DurationConstructor)                           \
    __JS_ENUMERATE(Instant, instant, InstantPrototype, InstantConstructor)                               \
    __JS_ENUMERATE(PlainDate, plain_date, PlainDatePrototype, PlainDateConstructor)                      \
    __JS_ENUMERATE(PlainDateTime, plain_date_time, PlainDateTimePrototype, PlainDateTimeConstructor)     \
    __JS_ENUMERATE(PlainMonthDay, plain_month_day, PlainMonthDayPrototype, PlainMonthDayConstructor)     \
    __JS_ENUMERATE(PlainTime, plain_time, PlainTimePrototype, PlainTimeConstructor)                      \
    __JS_ENUMERATE(PlainYearMonth, plain_year_month, PlainYearMonthPrototype, PlainYearMonthConstructor) \
    __JS_ENUMERATE(TimeZone, time_zone, TimeZonePrototype, TimeZoneConstructor)                          \
    __JS_ENUMERATE(ZonedDateTime, zoned_date_time, ZonedDateTimePrototype, ZonedDateTimeConstructor)

#define JS_ENUMERATE_ITERATOR_PROTOTYPES                         \
    __JS_ENUMERATE(Iterator, iterator)                           \
    __JS_ENUMERATE(ArrayIterator, array_iterator)                \
    __JS_ENUMERATE(AsyncIterator, async_iterator)                \
    __JS_ENUMERATE(Intl::SegmentIterator, intl_segment_iterator) \
    __JS_ENUMERATE(MapIterator, map_iterator)                    \
    __JS_ENUMERATE(RegExpStringIterator, regexp_string_iterator) \
    __JS_ENUMERATE(SetIterator, set_iterator)                    \
    __JS_ENUMERATE(StringIterator, string_iterator)

#define JS_ENUMERATE_BUILTIN_TYPES \
    JS_ENUMERATE_NATIVE_OBJECTS    \
    JS_ENUMERATE_NATIVE_ERRORS     \
    JS_ENUMERATE_TYPED_ARRAYS

#define JS_ENUMERATE_WELL_KNOWN_SYMBOLS                      \
    __JS_ENUMERATE(iterator, iterator)                       \
    __JS_ENUMERATE(asyncIterator, async_iterator)            \
    __JS_ENUMERATE(match, match)                             \
    __JS_ENUMERATE(matchAll, match_all)                      \
    __JS_ENUMERATE(replace, replace)                         \
    __JS_ENUMERATE(replaceAll, replace_all)                  \
    __JS_ENUMERATE(search, search)                           \
    __JS_ENUMERATE(split, split)                             \
    __JS_ENUMERATE(hasInstance, has_instance)                \
    __JS_ENUMERATE(isConcatSpreadable, is_concat_spreadable) \
    __JS_ENUMERATE(unscopables, unscopables)                 \
    __JS_ENUMERATE(species, species)                         \
    __JS_ENUMERATE(toPrimitive, to_primitive)                \
    __JS_ENUMERATE(toStringTag, to_string_tag)

#define JS_ENUMERATE_REGEXP_FLAGS              \
    __JS_ENUMERATE(hasIndices, has_indices, d) \
    __JS_ENUMERATE(global, global, g)          \
    __JS_ENUMERATE(ignoreCase, ignore_case, i) \
    __JS_ENUMERATE(multiline, multiline, m)    \
    __JS_ENUMERATE(dotAll, dot_all, s)         \
    __JS_ENUMERATE(unicode, unicode, u)        \
    __JS_ENUMERATE(sticky, sticky, y)

namespace JS {

class ASTNode;
class Accessor;
class BigInt;
class BoundFunction;
class Cell;
class CellAllocator;
class ClassExpression;
struct ClassFieldDefinition;
class Completion;
class Console;
class DeclarativeEnvironment;
class DeferGC;
class ECMAScriptFunctionObject;
class Environment;
class Error;
class ErrorType;
struct ExecutionContext;
class Expression;
class FunctionEnvironment;
class FunctionNode;
class GlobalEnvironment;
class GlobalObject;
class HandleImpl;
class Heap;
class HeapBlock;
class Interpreter;
class Module;
class NativeFunction;
class ObjectEnvironment;
class PrimitiveString;
class PromiseReaction;
class PromiseReactionJob;
class PromiseResolveThenableJob;
class PropertyAttributes;
class PropertyDescriptor;
class PropertyKey;
class Realm;
class Reference;
class ScopeNode;
class Script;
class Shape;
class Statement;
class StringOrSymbol;
class SourceTextModule;
class Symbol;
class Token;
class Utf16String;
class VM;
class Value;
class WeakContainer;
class WrappedFunction;
enum class DeclarationKind;
struct AlreadyResolved;
struct JobCallback;
struct ModuleRequest;
struct PromiseCapability;

// Not included in JS_ENUMERATE_NATIVE_OBJECTS due to missing distinct prototype
class ProxyObject;
class ProxyConstructor;

// Not included in JS_ENUMERATE_NATIVE_OBJECTS due to missing distinct constructor
class AsyncFromSyncIteratorPrototype;
class AsyncGenerator;
class AsyncGeneratorPrototype;
class GeneratorPrototype;

class TypedArrayConstructor;
class TypedArrayPrototype;

// Tag type used to differentiate between u8 as used by Uint8Array and u8 as used by Uint8ClampedArray.
struct ClampedU8;

#define __JS_ENUMERATE(ClassName, snake_name, ConstructorName, PrototypeName, ArrayType) \
    class ClassName;                                                                     \
    class ConstructorName;                                                               \
    class PrototypeName;
JS_ENUMERATE_NATIVE_OBJECTS_EXCLUDING_TEMPLATES
JS_ENUMERATE_NATIVE_ERRORS
JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

namespace Intl {
#define __JS_ENUMERATE(ClassName, snake_name, ConstructorName, PrototypeName) \
    class ClassName;                                                          \
    class ConstructorName;                                                    \
    class PrototypeName;
JS_ENUMERATE_INTL_OBJECTS
#undef __JS_ENUMERATE

// Not included in JS_ENUMERATE_INTL_OBJECTS due to missing distinct constructor
class Segments;
class SegmentsPrototype;
};

namespace Temporal {
#define __JS_ENUMERATE(ClassName, snake_name, ConstructorName, PrototypeName) \
    class ClassName;                                                          \
    class ConstructorName;                                                    \
    class PrototypeName;
JS_ENUMERATE_TEMPORAL_OBJECTS
#undef __JS_ENUMERATE
struct DurationRecord;
struct DateDurationRecord;
struct TimeDurationRecord;
struct PartialDurationRecord;
};

template<typename T>
class ThrowCompletionOr;

template<class T>
class Handle;

template<class T, size_t inline_capacity = 32>
class MarkedVector;

namespace Bytecode {
class BasicBlock;
struct Executable;
class Generator;
class Instruction;
class Interpreter;
class Register;
}

}
