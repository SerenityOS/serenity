/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/NeverDestroyed.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullOwnPtr.h>
#include <Kernel/ACPI/Definitions.h>
#include <Kernel/KString.h>

namespace Kernel::ACPI {

class DataRefObject : public RefCounted<DataRefObject> {
public:
    static NonnullRefPtr<DataRefObject> must_create(Span<u8 const> object_bytes);

    // FIXME: Find a way to translate the stored bytes to DDBHandle or ObjectReference.

private:
    explicit DataRefObject(Span<u8 const> object_bytes);
    ByteBuffer m_stored_encoded_bytes;
}

}
