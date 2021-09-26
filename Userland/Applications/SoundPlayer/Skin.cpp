/*
 * Copyright (c) 2021, Leandro A. F. Pereira <leandro@tia.mat.br>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Skin.h"

#include <AK/Format.h>
#include <AK/Function.h>
#include <AK/IterationDecision.h>
#include <AK/MappedFile.h>
#include <LibArchive/Zip.h>
#include <LibCompress/Deflate.h>
#include <LibGfx/BMPLoader.h>

void Skin::load_from_file(const String& path)
{
    auto skin_or_error = MappedFile::map(path);
    if (skin_or_error.is_error()) {
        warnln("Failed to open base skin file");
        return;
    }
    auto skin = Archive::Zip::try_create(skin_or_error.value()->bytes());
    if (!skin.has_value()) {
        warnln("Couldn't open WSZ file");
        return;
    }

    auto decompress_and_call = [](const Archive::ZipMember& member, Function<IterationDecision(const ReadonlyBytes&)> func) -> IterationDecision {
        switch (member.compression_method) {
        case Archive::ZipCompressionMethod::Store:
            return func(member.compressed_data);
        case Archive::ZipCompressionMethod::Deflate: {
            auto decompressed_data = Compress::DeflateDecompressor::decompress_all(member.compressed_data);
            if (!decompressed_data.has_value()) {
                warnln("Couldn't inflate member {}", member.name);
                return IterationDecision::Break;
            }
            if (decompressed_data.value().size() != member.uncompressed_size) {
                warnln("Size mismatch for {}: got {} expected {}", member.name, decompressed_data.value().size(), member.uncompressed_size);
                return IterationDecision::Break;
            }
            return func(ReadonlyBytes(decompressed_data.value().data(), decompressed_data.value().size()));
        }
        default:
            VERIFY_NOT_REACHED();
        }
    };

    auto load_skin_bitmap = [&](RefPtr<Gfx::Bitmap>& bitmap, const Archive::ZipMember& member) {
        return decompress_and_call(member, [&](const ReadonlyBytes& data) {
            bitmap = Gfx::load_bmp_from_memory(data.data(), data.size());
            return bitmap ? IterationDecision::Continue : IterationDecision::Break;
        });
    };

    auto load_viscolor = [&](const Archive::ZipMember& member) {
        return decompress_and_call(member, [&](const ReadonlyBytes& data) {
            int current = 0;
            for (auto& line : String(data).split('\n')) {
                if (current == 24) {
                    warnln("Malformed VISCOLOR file");
                    return IterationDecision::Break;
                }
                auto rgb = line.split(',');
                if (rgb.size() < 3) {
                    warnln("VISCOLOR has malformed line with {} elements", rgb.size());
                    return IterationDecision::Break;
                }
                u8 r = rgb[0].to_int().value_or(0);
                u8 g = rgb[1].to_int().value_or(0);
                u8 b = rgb[2].to_int().value_or(0);
                m_viscolor[current] = Gfx::Color(r, g, b);
                current++;
            }
            if (current != 24) {
                warnln("Expecting 24 viscolors, got {}", current);
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
    };

    skin->for_each_member([&](auto member) {
        if (member.name.equals_ignoring_case("MAIN.BMP"))
            return load_skin_bitmap(m_main, member);
        if (member.name.equals_ignoring_case("CBUTTONS.BMP"))
            return load_skin_bitmap(m_cbuttons, member);
        if (member.name.equals_ignoring_case("NUMBERS.BMP"))
            return load_skin_bitmap(m_numbers, member);
        if (member.name.equals_ignoring_case("PLAYPAUS.BMP"))
            return load_skin_bitmap(m_playpaus, member);
        if (member.name.equals_ignoring_case("POSBAR.BMP"))
            return load_skin_bitmap(m_posbar, member);
        if (member.name.equals_ignoring_case("SHUFREP.BMP"))
            return load_skin_bitmap(m_shufrep, member);
        if (member.name.equals_ignoring_case("TITLEBAR.BMP"))
            return load_skin_bitmap(m_titlebar, member);
        if (member.name.equals_ignoring_case("VOLUME.BMP"))
            return load_skin_bitmap(m_volume, member);
        if (member.name.equals_ignoring_case("BALANCE.BMP"))
            return load_skin_bitmap(m_balance, member);
        if (member.name.equals_ignoring_case("MONOSTER.BMP"))
            return load_skin_bitmap(m_monoster, member);
        if (member.name.equals_ignoring_case("TEXT.BMP"))
            return load_skin_bitmap(m_text, member);
        if (member.name.equals_ignoring_case("VISCOLOR.TXT"))
            return load_viscolor(member);
        return IterationDecision::Continue;
    });
}
