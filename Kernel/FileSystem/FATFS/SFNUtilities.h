/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/StringView.h>

namespace Kernel::SFNUtils {

class SFN : public RefCounted<SFN> {
public:
    static ErrorOr<NonnullRefPtr<SFN>> try_create(ByteBuffer name, ByteBuffer extension, size_t unique);

    ErrorOr<ByteBuffer> serialize_name() const;
    ErrorOr<ByteBuffer> serialize_extension() const;

    size_t digits() const;
    size_t& unique() { return m_unique; }

private:
    SFN(ByteBuffer name, ByteBuffer extension, size_t unique);

    ByteBuffer m_name;
    ByteBuffer m_extension;
    size_t m_unique;
};

bool is_valid_sfn(StringView sfn);
ErrorOr<NonnullRefPtr<SFN>> create_sfn_from_lfn(StringView lfn);

}
