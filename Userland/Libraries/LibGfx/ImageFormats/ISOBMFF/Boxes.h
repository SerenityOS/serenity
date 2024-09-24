/*
 * Copyright (c) 2023, Gregory Bertilson <Zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/String.h>
#include <AK/Traits.h>
#include <AK/Vector.h>
#include <LibMedia/DecoderError.h>

#include "BoxStream.h"
#include "Enums.h"

namespace Gfx::ISOBMFF {

// ISO/IEC 14496-12 Fifth Edition

// 4.2 Object Structure
struct BoxHeader {
    BoxType type { BoxType::None };
    u64 contents_size { 0 };
};

ErrorOr<BoxHeader> read_box_header(BoxStream& stream);

struct Box {
    Box() = default;
    virtual ~Box() = default;
    virtual BoxType box_type() const { return BoxType::None; }
    virtual void dump(String const& prepend = {}) const;
};

using BoxList = Vector<NonnullOwnPtr<Box>>;

struct FullBox : public Box {
    ErrorOr<void> read_from_stream(BoxStream& stream);
    virtual void dump(String const& prepend = {}) const override;

    u8 version { 0 };
    u32 flags { 0 };
};

struct UnknownBox final : public Box {
    static ErrorOr<NonnullOwnPtr<UnknownBox>> create_from_stream(BoxType type, BoxStream& stream)
    {
        auto box = TRY(try_make<UnknownBox>(type, stream.remaining()));
        TRY(box->read_from_stream(stream));
        return box;
    }
    UnknownBox(BoxType type, size_t contents_size)
        : m_box_type(type)
        , m_contents_size(contents_size)
    {
    }
    virtual ~UnknownBox() override = default;
    ErrorOr<void> read_from_stream(BoxStream&);
    virtual BoxType box_type() const override { return m_box_type; }
    virtual void dump(String const& prepend = {}) const override;

private:
    BoxType m_box_type { BoxType::None };
    size_t m_contents_size { 0 };
};

#define BOX_SUBTYPE(BoxName)                                                     \
    static ErrorOr<NonnullOwnPtr<BoxName>> create_from_stream(BoxStream& stream) \
    {                                                                            \
        auto box = TRY(try_make<BoxName>());                                     \
        TRY(box->read_from_stream(stream));                                      \
        return box;                                                              \
    }                                                                            \
    BoxName() = default;                                                         \
    virtual ~BoxName() override = default;                                       \
    ErrorOr<void> read_from_stream(BoxStream& stream);                           \
    virtual BoxType box_type() const override                                    \
    {                                                                            \
        return BoxType::BoxName;                                                 \
    }                                                                            \
    virtual void dump(String const& prepend = {}) const override;

// 4.3 File Type Box
struct FileTypeBox final : public Box {
    BOX_SUBTYPE(FileTypeBox);

    BrandIdentifier major_brand { BrandIdentifier::None };
    u32 minor_version;
    Vector<BrandIdentifier> compatible_brands;
};

// A box that contains other boxes.
struct SuperBox : public Box {
    SuperBox() = default;

    using BoxCallback = Function<ErrorOr<Optional<NonnullOwnPtr<Box>>>(BoxType, BoxStream&)>;
    ErrorOr<void> read_from_stream(BoxStream&, BoxCallback);
    virtual void dump(String const& prepend = {}) const override;

    BoxList const& child_boxes() const { return m_child_boxes; }

private:
    BoxList m_child_boxes;
};

struct UserExtensionBox final : public Box {
    BOX_SUBTYPE(UserExtensionBox);

    Array<u8, 16> uuid;
    ByteBuffer data;
};

}
