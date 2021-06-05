/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define JS_DECLARE_NATIVE_FUNCTION(name) \
    static JS::Value name(JS::VM&, JS::GlobalObject&)

#define JS_DECLARE_NATIVE_GETTER(name) \
    static JS::Value name(JS::VM&, JS::GlobalObject&)

#define JS_DECLARE_NATIVE_SETTER(name) \
    static void name(JS::VM&, JS::GlobalObject&, JS::Value)

#define JS_DEFINE_NATIVE_FUNCTION(name) \
    JS::Value name([[maybe_unused]] JS::VM& vm, [[maybe_unused]] JS::GlobalObject& global_object)

#define JS_DEFINE_NATIVE_GETTER(name) \
    JS::Value name([[maybe_unused]] JS::VM& vm, [[maybe_unused]] JS::GlobalObject& global_object)

#define JS_DEFINE_NATIVE_SETTER(name) \
    void name([[maybe_unused]] JS::VM& vm, [[maybe_unused]] JS::GlobalObject& global_object, [[maybe_unused]] JS::Value value)

// NOTE: Proxy is not included here as it doesn't have a prototype - m_proxy_constructor is initialized separately.
#define JS_ENUMERATE_NATIVE_OBJECTS_EXCLUDING_TEMPLATES                                           \
    __JS_ENUMERATE(Array, array, ArrayPrototype, ArrayConstructor, void)                          \
    __JS_ENUMERATE(ArrayBuffer, array_buffer, ArrayBufferPrototype, ArrayBufferConstructor, void) \
    __JS_ENUMERATE(BigIntObject, bigint, BigIntPrototype, BigIntConstructor, void)                \
    __JS_ENUMERATE(BooleanObject, boolean, BooleanPrototype, BooleanConstructor, void)            \
    __JS_ENUMERATE(Date, date, DatePrototype, DateConstructor, void)                              \
    __JS_ENUMERATE(Error, error, ErrorPrototype, ErrorConstructor, void)                          \
    __JS_ENUMERATE(Function, function, FunctionPrototype, FunctionConstructor, void)              \
    __JS_ENUMERATE(NumberObject, number, NumberPrototype, NumberConstructor, void)                \
    __JS_ENUMERATE(Object, object, ObjectPrototype, ObjectConstructor, void)                      \
    __JS_ENUMERATE(Promise, promise, PromisePrototype, PromiseConstructor, void)                  \
    __JS_ENUMERATE(RegExpObject, regexp, RegExpPrototype, RegExpConstructor, void)                \
    __JS_ENUMERATE(StringObject, string, StringPrototype, StringConstructor, void)                \
    __JS_ENUMERATE(SymbolObject, symbol, SymbolPrototype, SymbolConstructor, void)

#define JS_ENUMERATE_NATIVE_OBJECTS                 \
    JS_ENUMERATE_NATIVE_OBJECTS_EXCLUDING_TEMPLATES \
    __JS_ENUMERATE(TypedArray, typed_array, TypedArrayPrototype, TypedArrayConstructor, void)

#define JS_ENUMERATE_ERROR_SUBCLASSES                                                                                                      \
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
    __JS_ENUMERATE(Int8Array, int8_array, Int8ArrayPrototype, Int8ArrayConstructor, i8)                                         \
    __JS_ENUMERATE(Int16Array, int16_array, Int16ArrayPrototype, Int16ArrayConstructor, i16)                                    \
    __JS_ENUMERATE(Int32Array, int32_array, Int32ArrayPrototype, Int32ArrayConstructor, i32)                                    \
    __JS_ENUMERATE(Float32Array, float32_array, Float32ArrayPrototype, Float32ArrayConstructor, float)                          \
    __JS_ENUMERATE(Float64Array, float64_array, Float64ArrayPrototype, Float64ArrayConstructor, double)

#define JS_ENUMERATE_ITERATOR_PROTOTYPES          \
    __JS_ENUMERATE(Iterator, iterator)            \
    __JS_ENUMERATE(ArrayIterator, array_iterator) \
    __JS_ENUMERATE(StringIterator, string_iterator)

#define JS_ENUMERATE_BUILTIN_TYPES \
    JS_ENUMERATE_NATIVE_OBJECTS    \
    JS_ENUMERATE_ERROR_SUBCLASSES  \
    JS_ENUMERATE_TYPED_ARRAYS

#define JS_ENUMERATE_WELL_KNOWN_SYMBOLS                      \
    __JS_ENUMERATE(iterator, iterator)                       \
    __JS_ENUMERATE(asyncIterator, async_iterator)            \
    __JS_ENUMERATE(match, match)                             \
    __JS_ENUMERATE(matchAll, match_all)                      \
    __JS_ENUMERATE(replace, replace)                         \
    __JS_ENUMERATE(search, search)                           \
    __JS_ENUMERATE(split, split)                             \
    __JS_ENUMERATE(hasInstance, has_instance)                \
    __JS_ENUMERATE(isConcatSpreadable, is_concat_spreadable) \
    __JS_ENUMERATE(unscopables, unscopables)                 \
    __JS_ENUMERATE(species, species)                         \
    __JS_ENUMERATE(toPrimitive, to_primitive)                \
    __JS_ENUMERATE(toStringTag, to_string_tag)

#define JS_ENUMERATE_REGEXP_FLAGS                           \
    __JS_ENUMERATE(global, global, g, Global)               \
    __JS_ENUMERATE(ignoreCase, ignore_case, i, Insensitive) \
    __JS_ENUMERATE(multiline, multiline, m, Multiline)      \
    __JS_ENUMERATE(dotAll, dot_all, s, SingleLine)          \
    __JS_ENUMERATE(unicode, unicode, u, Unicode)            \
    __JS_ENUMERATE(sticky, sticky, y, Sticky)

namespace JS {

class ASTNode;
class CellAllocator;
class BigInt;
class BoundFunction;
class Cell;
class Console;
class DeferGC;
class Error;
class Exception;
class Expression;
class Accessor;
class GlobalObject;
class HandleImpl;
class Heap;
class HeapBlock;
class Interpreter;
class LexicalEnvironment;
class MarkedValueList;
class NativeFunction;
class NativeProperty;
class PrimitiveString;
class PromiseReaction;
class PromiseReactionJob;
class PromiseResolveThenableJob;
class PropertyName;
class Reference;
class ScopeNode;
class ScopeObject;
class Shape;
class Statement;
class StringOrSymbol;
class Symbol;
class Token;
class VM;
class Value;
enum class DeclarationKind;
struct AlreadyResolved;
struct JobCallback;
struct PromiseCapability;

// Not included in JS_ENUMERATE_NATIVE_OBJECTS due to missing distinct prototype
class ProxyObject;
class ProxyConstructor;

class TypedArrayConstructor;
class TypedArrayPrototype;

// Tag type used to differentiate between u8 as used by Uint8Array and u8 as used by Uint8ClampedArray.
struct ClampedU8;

#define __JS_ENUMERATE(ClassName, snake_name, ConstructorName, PrototypeName, ArrayType) \
    class ClassName;                                                                     \
    class ConstructorName;                                                               \
    class PrototypeName;
JS_ENUMERATE_NATIVE_OBJECTS_EXCLUDING_TEMPLATES
JS_ENUMERATE_ERROR_SUBCLASSES
JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

template<class T>
class Handle;

}
