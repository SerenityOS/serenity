/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <LibJS/Forward.h>

namespace JS {

#define ENUMERATE_STANDARD_PROPERTY_NAMES(P) \
    P(__proto__)                             \
    P(__defineGetter__)                      \
    P(__defineSetter__)                      \
    P(__lookupGetter__)                      \
    P(__lookupSetter__)                      \
    P(BYTES_PER_ELEMENT)                     \
    P(BigInt)                                \
    P(Boolean)                               \
    P(E)                                     \
    P(EPSILON)                               \
    P(Function)                              \
    P(Infinity)                              \
    P(JSON)                                  \
    P(LN10)                                  \
    P(LN2)                                   \
    P(LOG10E)                                \
    P(LOG2E)                                 \
    P(MAX_SAFE_INTEGER)                      \
    P(MAX_VALUE)                             \
    P(MIN_SAFE_INTEGER)                      \
    P(MIN_VALUE)                             \
    P(Math)                                  \
    P(NEGATIVE_INFINITY)                     \
    P(NaN)                                   \
    P(Number)                                \
    P(PI)                                    \
    P(POSITIVE_INFINITY)                     \
    P(Proxy)                                 \
    P(Reflect)                               \
    P(RegExp)                                \
    P(SQRT1_2)                               \
    P(SQRT2)                                 \
    P(String)                                \
    P(Symbol)                                \
    P(Temporal)                              \
    P(UTC)                                   \
    P(abs)                                   \
    P(acos)                                  \
    P(acosh)                                 \
    P(add)                                   \
    P(all)                                   \
    P(allSettled)                            \
    P(anchor)                                \
    P(any)                                   \
    P(apply)                                 \
    P(arguments)                             \
    P(asIntN)                                \
    P(asUintN)                               \
    P(asin)                                  \
    P(asinh)                                 \
    P(assert)                                \
    P(assign)                                \
    P(at)                                    \
    P(atan)                                  \
    P(atan2)                                 \
    P(atanh)                                 \
    P(big)                                   \
    P(bind)                                  \
    P(blink)                                 \
    P(bold)                                  \
    P(buffer)                                \
    P(byteLength)                            \
    P(byteOffset)                            \
    P(call)                                  \
    P(callee)                                \
    P(caller)                                \
    P(cause)                                 \
    P(cbrt)                                  \
    P(ceil)                                  \
    P(charAt)                                \
    P(charCodeAt)                            \
    P(cleanupSome)                           \
    P(clear)                                 \
    P(clz32)                                 \
    P(codePointAt)                           \
    P(concat)                                \
    P(configurable)                          \
    P(console)                               \
    P(construct)                             \
    P(constructor)                           \
    P(copyWithin)                            \
    P(cos)                                   \
    P(cosh)                                  \
    P(count)                                 \
    P(countReset)                            \
    P(create)                                \
    P(debug)                                 \
    P(decodeURI)                             \
    P(decodeURIComponent)                    \
    P(defineProperties)                      \
    P(defineProperty)                        \
    P(deleteProperty)                        \
    P(deref)                                 \
    P(description)                           \
    P(done)                                  \
    P(dotAll)                                \
    P(encodeURI)                             \
    P(encodeURIComponent)                    \
    P(endsWith)                              \
    P(entries)                               \
    P(enumerable)                            \
    P(error)                                 \
    P(errors)                                \
    P(escape)                                \
    P(eval)                                  \
    P(every)                                 \
    P(exec)                                  \
    P(exp)                                   \
    P(expm1)                                 \
    P(fill)                                  \
    P(filter)                                \
    P(finally)                               \
    P(find)                                  \
    P(findIndex)                             \
    P(fixed)                                 \
    P(flags)                                 \
    P(flat)                                  \
    P(flatMap)                               \
    P(floor)                                 \
    P(fontcolor)                             \
    P(fontsize)                              \
    P(forEach)                               \
    P(freeze)                                \
    P(from)                                  \
    P(fromCharCode)                          \
    P(fromCodePoint)                         \
    P(fromEntries)                           \
    P(fround)                                \
    P(gc)                                    \
    P(get)                                   \
    P(getBigInt64)                           \
    P(getBigUint64)                          \
    P(getDate)                               \
    P(getDay)                                \
    P(getFloat32)                            \
    P(getFloat64)                            \
    P(getFullYear)                           \
    P(getHours)                              \
    P(getInt8)                               \
    P(getInt16)                              \
    P(getInt32)                              \
    P(getMilliseconds)                       \
    P(getMinutes)                            \
    P(getMonth)                              \
    P(getOwnPropertyDescriptor)              \
    P(getOwnPropertyDescriptors)             \
    P(getOwnPropertyNames)                   \
    P(getOwnPropertySymbols)                 \
    P(getPrototypeOf)                        \
    P(getSeconds)                            \
    P(getTime)                               \
    P(getTimezoneOffset)                     \
    P(getUint8)                              \
    P(getUint16)                             \
    P(getUint32)                             \
    P(getUTCDate)                            \
    P(getUTCDay)                             \
    P(getUTCFullYear)                        \
    P(getUTCHours)                           \
    P(getUTCMilliseconds)                    \
    P(getUTCMinutes)                         \
    P(getUTCMonth)                           \
    P(getUTCSeconds)                         \
    P(getYear)                               \
    P(global)                                \
    P(globalThis)                            \
    P(groups)                                \
    P(has)                                   \
    P(hasOwn)                                \
    P(hasOwnProperty)                        \
    P(hypot)                                 \
    P(ignoreCase)                            \
    P(imul)                                  \
    P(includes)                              \
    P(index)                                 \
    P(indexOf)                               \
    P(info)                                  \
    P(input)                                 \
    P(is)                                    \
    P(isArray)                               \
    P(isExtensible)                          \
    P(isFinite)                              \
    P(isFrozen)                              \
    P(isInteger)                             \
    P(isNaN)                                 \
    P(isPrototypeOf)                         \
    P(isSafeInteger)                         \
    P(isSealed)                              \
    P(isView)                                \
    P(italics)                               \
    P(join)                                  \
    P(keyFor)                                \
    P(keys)                                  \
    P(lastIndex)                             \
    P(lastIndexOf)                           \
    P(length)                                \
    P(link)                                  \
    P(log)                                   \
    P(log1p)                                 \
    P(log2)                                  \
    P(log10)                                 \
    P(map)                                   \
    P(max)                                   \
    P(message)                               \
    P(min)                                   \
    P(multiline)                             \
    P(name)                                  \
    P(next)                                  \
    P(now)                                   \
    P(of)                                    \
    P(ownKeys)                               \
    P(padEnd)                                \
    P(padStart)                              \
    P(parse)                                 \
    P(parseFloat)                            \
    P(parseInt)                              \
    P(pop)                                   \
    P(pow)                                   \
    P(preventExtensions)                     \
    P(propertyIsEnumerable)                  \
    P(prototype)                             \
    P(proxy)                                 \
    P(push)                                  \
    P(race)                                  \
    P(random)                                \
    P(raw)                                   \
    P(reduce)                                \
    P(reduceRight)                           \
    P(reject)                                \
    P(repeat)                                \
    P(resolve)                               \
    P(reverse)                               \
    P(revocable)                             \
    P(revoke)                                \
    P(round)                                 \
    P(seal)                                  \
    P(set)                                   \
    P(setBigInt64)                           \
    P(setBigUint64)                          \
    P(setDate)                               \
    P(setFloat32)                            \
    P(setFloat64)                            \
    P(setFullYear)                           \
    P(setHours)                              \
    P(setInt8)                               \
    P(setInt16)                              \
    P(setInt32)                              \
    P(setMilliseconds)                       \
    P(setMinutes)                            \
    P(setMonth)                              \
    P(setPrototypeOf)                        \
    P(setSeconds)                            \
    P(setTime)                               \
    P(setUint8)                              \
    P(setUint16)                             \
    P(setUint32)                             \
    P(setUTCDate)                            \
    P(setUTCFullYear)                        \
    P(setUTCHours)                           \
    P(setUTCMilliseconds)                    \
    P(setUTCMinutes)                         \
    P(setUTCMonth)                           \
    P(setUTCSeconds)                         \
    P(setYear)                               \
    P(shift)                                 \
    P(sign)                                  \
    P(sin)                                   \
    P(sinh)                                  \
    P(size)                                  \
    P(slice)                                 \
    P(small)                                 \
    P(some)                                  \
    P(sort)                                  \
    P(source)                                \
    P(splice)                                \
    P(sqrt)                                  \
    P(startsWith)                            \
    P(sticky)                                \
    P(strike)                                \
    P(stringify)                             \
    P(sub)                                   \
    P(substr)                                \
    P(substring)                             \
    P(sup)                                   \
    P(tan)                                   \
    P(tanh)                                  \
    P(test)                                  \
    P(then)                                  \
    P(toDateString)                          \
    P(toFixed)                               \
    P(toGMTString)                           \
    P(toISOString)                           \
    P(toJSON)                                \
    P(toLocaleDateString)                    \
    P(toLocaleString)                        \
    P(toLocaleTimeString)                    \
    P(toLowerCase)                           \
    P(toString)                              \
    P(toTimeString)                          \
    P(toUpperCase)                           \
    P(toUTCString)                           \
    P(trace)                                 \
    P(trim)                                  \
    P(trimEnd)                               \
    P(trimLeft)                              \
    P(trimRight)                             \
    P(trimStart)                             \
    P(trunc)                                 \
    P(undefined)                             \
    P(unescape)                              \
    P(unicode)                               \
    P(unregister)                            \
    P(unshift)                               \
    P(value)                                 \
    P(valueOf)                               \
    P(values)                                \
    P(warn)                                  \
    P(writable)

struct CommonPropertyNames {
    PropertyName catch_ { "catch", PropertyName::StringMayBeNumber::No };
    PropertyName delete_ { "delete", PropertyName::StringMayBeNumber::No };
    PropertyName for_ { "for", PropertyName::StringMayBeNumber::No };
    PropertyName register_ { "register", PropertyName::StringMayBeNumber::No };
    PropertyName return_ { "return", PropertyName::StringMayBeNumber::No };
    PropertyName throw_ { "throw", PropertyName::StringMayBeNumber::No };
#define __ENUMERATE(x) PropertyName x { #x, PropertyName::StringMayBeNumber::No };
    ENUMERATE_STANDARD_PROPERTY_NAMES(__ENUMERATE)
#undef __ENUMERATE
#define __JS_ENUMERATE(x, a, b, c, t) PropertyName x { #x, PropertyName::StringMayBeNumber::No };
    JS_ENUMERATE_BUILTIN_TYPES
#undef __JS_ENUMERATE
#define __JS_ENUMERATE(x, a, b, c) PropertyName x { #x, PropertyName::StringMayBeNumber::No };
    JS_ENUMERATE_TEMPORAL_OBJECTS
#undef __JS_ENUMERATE
#define __JS_ENUMERATE(x, a) PropertyName x { #x, PropertyName::StringMayBeNumber::No };
    JS_ENUMERATE_WELL_KNOWN_SYMBOLS
#undef __JS_ENUMERATE
};

}
