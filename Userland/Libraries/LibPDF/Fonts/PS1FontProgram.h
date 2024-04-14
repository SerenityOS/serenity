/*
 * Copyright (c) 2022, Julian Offenhäuser <offenhaeuser@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibGfx/AffineTransform.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Path.h>
#include <LibPDF/Error.h>
#include <LibPDF/Fonts/Type1FontProgram.h>

namespace PDF {

class Reader;
class Encoding;

class PS1FontProgram : public Type1FontProgram {
public:
    static PDFErrorOr<NonnullRefPtr<Type1FontProgram>> create(ReadonlyBytes const&, RefPtr<Encoding>, size_t cleartext_length, size_t encrypted_length);

private:
    static Error error(
        ByteString const& message
#ifdef PDF_DEBUG
        ,
        SourceLocation loc = SourceLocation::current()
#endif
    );

    PDFErrorOr<void> parse_encrypted_portion(ByteBuffer const&);
    PDFErrorOr<Vector<ByteBuffer>> parse_subroutines(Reader&) const;
    static PDFErrorOr<Vector<float>> parse_number_array(Reader&, size_t length);
    static PDFErrorOr<ByteString> parse_word(Reader&);
    static PDFErrorOr<float> parse_float(Reader&);
    static PDFErrorOr<int> parse_int(Reader&);

    static PDFErrorOr<ByteBuffer> decrypt(ReadonlyBytes const&, u16 key, size_t skip);
    static bool seek_name(Reader&, ByteString const&);

    u16 m_encryption_key { 4330 };
    int m_lenIV { 4 };
};

}
