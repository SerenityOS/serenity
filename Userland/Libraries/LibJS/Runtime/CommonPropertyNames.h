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
    P(BYTES_PER_ELEMENT)                     \
    P(BigInt)                                \
    P(Boolean)                               \
    P(E)                                     \
    P(EPSILON)                               \
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
    P(UTC)                                   \
    P(abs)                                   \
    P(acos)                                  \
    P(acosh)                                 \
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
    P(cbrt)                                  \
    P(ceil)                                  \
    P(charAt)                                \
    P(charCodeAt)                            \
    P(clear)                                 \
    P(clz32)                                 \
    P(concat)                                \
    P(configurable)                          \
    P(console)                               \
    P(construct)                             \
    P(constructor)                           \
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
    P(description)                           \
    P(done)                                  \
    P(dotAll)                                \
    P(encodeURI)                             \
    P(encodeURIComponent)                    \
    P(endsWith)                              \
    P(entries)                               \
    P(enumerable)                            \
    P(error)                                 \
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
    P(floor)                                 \
    P(fontcolor)                             \
    P(fontsize)                              \
    P(forEach)                               \
    P(freeze)                                \
    P(from)                                  \
    P(fromCharCode)                          \
    P(fround)                                \
    P(gc)                                    \
    P(get)                                   \
    P(getDate)                               \
    P(getDay)                                \
    P(getFullYear)                           \
    P(getHours)                              \
    P(getMilliseconds)                       \
    P(getMinutes)                            \
    P(getMonth)                              \
    P(getOwnPropertyDescriptor)              \
    P(getOwnPropertyNames)                   \
    P(getPrototypeOf)                        \
    P(getSeconds)                            \
    P(getTime)                               \
    P(getTimezoneOffset)                     \
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
    P(round)                                 \
    P(seal)                                  \
    P(set)                                   \
    P(setDate)                               \
    P(setFullYear)                           \
    P(setHours)                              \
    P(setMilliseconds)                       \
    P(setMinutes)                            \
    P(setMonth)                              \
    P(setPrototypeOf)                        \
    P(setSeconds)                            \
    P(setYear)                               \
    P(shift)                                 \
    P(sign)                                  \
    P(sin)                                   \
    P(sinh)                                  \
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
    P(unshift)                               \
    P(value)                                 \
    P(valueOf)                               \
    P(values)                                \
    P(warn)                                  \
    P(writable)

struct CommonPropertyNames {
    FlyString catch_ { "catch" };
    FlyString for_ { "for" };
#define __ENUMERATE(x) FlyString x { #x };
    ENUMERATE_STANDARD_PROPERTY_NAMES(__ENUMERATE)
#undef __ENUMERATE
#define __JS_ENUMERATE(x, a, b, c, t) FlyString x { #x };
    JS_ENUMERATE_BUILTIN_TYPES
#undef __JS_ENUMERATE
#define __JS_ENUMERATE(x, a) FlyString x { #x };
    JS_ENUMERATE_WELL_KNOWN_SYMBOLS
#undef __JS_ENUMERATE
};

}
