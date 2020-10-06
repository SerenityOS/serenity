/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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
    void name([[maybe_unused]] JS::VM& vm, [[maybe_unused]] JS::GlobalObject& global_object, JS::Value value)

#define JS_ENUMERATE_NATIVE_OBJECTS                                              \
    __JS_ENUMERATE(Array, array, ArrayPrototype, ArrayConstructor)               \
    __JS_ENUMERATE(BigIntObject, bigint, BigIntPrototype, BigIntConstructor)     \
    __JS_ENUMERATE(BooleanObject, boolean, BooleanPrototype, BooleanConstructor) \
    __JS_ENUMERATE(Date, date, DatePrototype, DateConstructor)                   \
    __JS_ENUMERATE(Error, error, ErrorPrototype, ErrorConstructor)               \
    __JS_ENUMERATE(Function, function, FunctionPrototype, FunctionConstructor)   \
    __JS_ENUMERATE(NumberObject, number, NumberPrototype, NumberConstructor)     \
    __JS_ENUMERATE(Object, object, ObjectPrototype, ObjectConstructor)           \
    __JS_ENUMERATE(ProxyObject, proxy, ProxyPrototype, ProxyConstructor)         \
    __JS_ENUMERATE(RegExpObject, regexp, RegExpPrototype, RegExpConstructor)     \
    __JS_ENUMERATE(StringObject, string, StringPrototype, StringConstructor)     \
    __JS_ENUMERATE(SymbolObject, symbol, SymbolPrototype, SymbolConstructor)

#define JS_ENUMERATE_ERROR_SUBCLASSES                                                                                                \
    __JS_ENUMERATE(EvalError, eval_error, EvalErrorPrototype, EvalErrorConstructor)                                                  \
    __JS_ENUMERATE(InternalError, internal_error, InternalErrorPrototype, InternalErrorConstructor)                                  \
    __JS_ENUMERATE(InvalidCharacterError, invalid_character_error, InvalidCharacterErrorPrototype, InvalidCharacterErrorConstructor) \
    __JS_ENUMERATE(RangeError, range_error, RangeErrorPrototype, RangeErrorConstructor)                                              \
    __JS_ENUMERATE(ReferenceError, reference_error, ReferenceErrorPrototype, ReferenceErrorConstructor)                              \
    __JS_ENUMERATE(SyntaxError, syntax_error, SyntaxErrorPrototype, SyntaxErrorConstructor)                                          \
    __JS_ENUMERATE(TypeError, type_error, TypeErrorPrototype, TypeErrorConstructor)                                                  \
    __JS_ENUMERATE(URIError, uri_error, URIErrorPrototype, URIErrorConstructor)

#define JS_ENUMERATE_ITERATOR_PROTOTYPES          \
    __JS_ENUMERATE(Iterator, iterator)            \
    __JS_ENUMERATE(ArrayIterator, array_iterator) \
    __JS_ENUMERATE(StringIterator, string_iterator)

#define JS_ENUMERATE_BUILTIN_TYPES \
    JS_ENUMERATE_NATIVE_OBJECTS    \
    JS_ENUMERATE_ERROR_SUBCLASSES

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

namespace JS {

class ASTNode;
class Allocator;
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
class NativeProperty;
class PrimitiveString;
class Reference;
class ScopeNode;
class Shape;
class Statement;
class Symbol;
class Token;
class Uint8ClampedArray;
class VM;
class Value;
enum class DeclarationKind;

#define __JS_ENUMERATE(ClassName, snake_name, ConstructorName, PrototypeName) \
    class ClassName;                                                          \
    class ConstructorName;                                                    \
    class PrototypeName;
JS_ENUMERATE_BUILTIN_TYPES
#undef __JS_ENUMERATE

struct Argument;

template<class T>
class Handle;

}
