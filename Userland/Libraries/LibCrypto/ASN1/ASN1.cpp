/*
 * Copyright (c) 2021, the SerenityOS developers.
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

#include <LibCrypto/ASN1/ASN1.h>

namespace Crypto::ASN1 {

String kind_name(Kind kind)
{
    switch (kind) {
    case Kind::Eol:
        return "EndOfList";
    case Kind::Boolean:
        return "Boolean";
    case Kind::Integer:
        return "Integer";
    case Kind::BitString:
        return "BitString";
    case Kind::OctetString:
        return "OctetString";
    case Kind::Null:
        return "Null";
    case Kind::ObjectIdentifier:
        return "ObjectIdentifier";
    case Kind::IA5String:
        return "IA5String";
    case Kind::PrintableString:
        return "PrintableString";
    case Kind::Utf8String:
        return "UTF8String";
    case Kind::UTCTime:
        return "UTCTime";
    case Kind::Sequence:
        return "Sequence";
    case Kind::Set:
        return "Set";
    }

    return "InvalidKind";
}

String class_name(Class class_)
{
    switch (class_) {
    case Class::Application:
        return "Application";
    case Class::Context:
        return "Context";
    case Class::Private:
        return "Private";
    case Class::Universal:
        return "Universal";
    }

    return "InvalidClass";
}

String type_name(Type type)
{
    switch (type) {
    case Type::Constructed:
        return "Constructed";
    case Type::Primitive:
        return "Primitive";
    }

    return "InvalidType";
}

}
