/*
 * Copyright (c) 2023, Martin Janiczek <martin@janiczek.cz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibTest/PBT/Chunk.h>
#include <LibTest/PBT/RandomRun.h>

#include <AK/String.h>
#include <AK/Variant.h>
#include <AK/Vector.h>

struct ZeroChunk {
    Chunk chunk;
};
struct SortChunk {
    Chunk chunk;
};
struct DeleteChunkAndMaybeDecPrevious {
    Chunk chunk;
};
struct MinimizeChoice {
    size_t index;
};
struct SwapChunkWithNeighbour {
    Chunk chunk;

    Chunk const neighbour() {
        return Chunk{chunk.size, chunk.index + chunk.size};
    }
};
struct RedistributeChoicesAndMaybeInc {
    size_t left_index;
    size_t right_index;
};

using CmdVariant = Variant<
    ZeroChunk,
    SortChunk,
    DeleteChunkAndMaybeDecPrevious,
    MinimizeChoice,
    SwapChunkWithNeighbour,
    RedistributeChoicesAndMaybeInc
>;

class ShrinkCmd {
public:
    explicit ShrinkCmd(CmdVariant const& cmd)
        : m_cmd(cmd)
    {
    }

    static Vector<ShrinkCmd> for_run(RandomRun const& run)
    {
        size_t run_size = run.size();

        Vector<ShrinkCmd> all;
        // Sorted roughly in the order of effectiveness. Deleting chunks is better
        // than minimizing them.
        all.extend(deletion_cmds(run_size));
        all.extend(zero_cmds(run_size));
        all.extend(sort_cmds(run_size));
        all.extend(swap_chunk_cmds(run_size));
        all.extend(minimize_cmds(run_size));
        all.extend(redistribute_cmds(run_size));
        return all;
    }

    bool has_a_chance(RandomRun const& run) const
    {
        return m_cmd.visit(
            [&](ZeroChunk c) { return run.has_a_chance(c.chunk); },
            [&](SortChunk c) { return run.has_a_chance(c.chunk); },
            [&](DeleteChunkAndMaybeDecPrevious c) { return run.has_a_chance(c.chunk); },
            [&](MinimizeChoice c) { return run.size() > c.index; },
            [&](RedistributeChoicesAndMaybeInc c) { return run.size() > c.right_index; },
            [&](SwapChunkWithNeighbour c) { return run.has_a_chance(c.neighbour());  });
    }

    ErrorOr<String> to_string()
    {
        return m_cmd.visit(
            [](ZeroChunk c) { return String::formatted("ZeroChunk({})", c.chunk); },
            [](SortChunk c) { return String::formatted("SortChunk({})", c.chunk); },
            [](DeleteChunkAndMaybeDecPrevious c) { return String::formatted("DeleteChunkAndMaybeDecPrevious({})", c.chunk); },
            [](MinimizeChoice c) { return String::formatted("MinimizeChoice(i={})", c.index); },
            [](RedistributeChoicesAndMaybeInc c) { return String::formatted("RedistributeChoicesAndMaybeInc(left={},right={})", c.left_index, c.right_index); },
            [](SwapChunkWithNeighbour c) { return String::formatted("SwapChunkWithNeighbour({})", c.chunk); });
    }

    template<typename C1, typename C2, typename C3, typename C4, typename C5, typename C6>
    auto visit(C1 on_zero, C2 on_sort, C3 on_delete, C4 on_minimize, C5 on_redistribute, C6 on_swap_chunk)
    {
        return m_cmd.visit(
            [&](ZeroChunk c) { return on_zero(c); },
            [&](SortChunk c) { return on_sort(c); },
            [&](DeleteChunkAndMaybeDecPrevious c) { return on_delete(c); },
            [&](MinimizeChoice c) { return on_minimize(c); },
            [&](RedistributeChoicesAndMaybeInc c) { return on_redistribute(c); },
            [&](SwapChunkWithNeighbour c) { return on_swap_chunk(c); });
    }

private:
    CmdVariant m_cmd;

    /* Will generate ShrinkCmds for all chunks of sizes 1,2,3,4,8 in bounds of the
     * given RandomRun size.
     *
     * They will be given in a reverse order (largest chunks first), to maximize our
     * chances of saving work (minimizing the RandomRun faster).
     *
     * chunkCmds(10, false, [](Chunk c){ return SortChunk(c); })
     * -->
     * [ // Chunks of size 8
     *   SortChunk { chunk_size = 8, start_index = 0 }, // [XXXXXXXX..]
     *   SortChunk { chunk_size = 8, start_index = 1 }, // [.XXXXXXXX.]
     *   SortChunk { chunk_size = 8, start_index = 2 }, // [..XXXXXXXX]
     *
     *   // Chunks of size 4
     *   SortChunk { chunk_size = 4, start_index = 0 }, // [XXXX......]
     *   SortChunk { chunk_size = 4, start_index = 1 }, // [.XXXX.....]
     *   // ...
     *   SortChunk { chunk_size = 4, start_index = 5 }, // [.....XXXX.]
     *   SortChunk { chunk_size = 4, start_index = 6 }, // [......XXXX]
     *
     *   // Chunks of size 3
     *   SortChunk { chunk_size = 3, start_index = 0 }, // [XXX.......]
     *   SortChunk { chunk_size = 3, start_index = 1 }, // [.XXX......]
     *   // ...
     *   SortChunk { chunk_size = 3, start_index = 6 }, // [......XXX.]
     *   SortChunk { chunk_size = 3, start_index = 7 }, // [.......XXX]
     *
     *   // Chunks of size 2
     *   SortChunk { chunk_size = 2, start_index = 0 }, // [XX........]
     *   SortChunk { chunk_size = 2, start_index = 1 }, // [.XX.......]
     *   // ...
     *   SortChunk { chunk_size = 2, start_index = 7 }, // [.......XX.]
     *   SortChunk { chunk_size = 2, start_index = 8 }, // [........XX]
     * ]
     */
    template<typename FN>
    static Vector<ShrinkCmd> chunk_cmds(size_t run_size, bool allow_chunks_size1, FN chunk_to_cmd)
    {
        Vector<u8> sizes = { 8, 4, 3, 2 };
        if (allow_chunks_size1) {
            sizes.append(1);
        }

        Vector<ShrinkCmd> cmds;
        for (u8 chunk_size : sizes) {
            if (chunk_size > run_size) {
                continue;
            }

            for (size_t i = 0; i < run_size - chunk_size + 1; ++i) {
                ShrinkCmd cmd = chunk_to_cmd(Chunk { chunk_size, i });
                cmds.append(cmd);
            }
        }
        return cmds;
    }

    static Vector<ShrinkCmd> deletion_cmds(size_t run_size)
    {
        return chunk_cmds(
            run_size,
            true,
            [](Chunk c) { return ShrinkCmd(DeleteChunkAndMaybeDecPrevious { c }); });
    }

    static Vector<ShrinkCmd> minimize_cmds(size_t run_size)
    {
        Vector<ShrinkCmd> cmds;
        for (size_t i = 0; i < run_size; ++i) {
            ShrinkCmd cmd = ShrinkCmd(MinimizeChoice { i });
            cmds.append(cmd);
        }
        return cmds;
    }

    static Vector<ShrinkCmd> redistribute_cmds(size_t run_size)
    {
        Vector<ShrinkCmd> cmds;
        for (size_t offset = 3; offset > 0; --offset) {
            if (offset >= run_size) continue;
            for (size_t i = 0; i < run_size - offset; ++i) {
                ShrinkCmd cmd = ShrinkCmd(RedistributeChoicesAndMaybeInc { i, i + offset });
                cmds.append(cmd);
            }
        }
        return cmds;
    }

    static Vector<ShrinkCmd> sort_cmds(size_t run_size)
    {
        bool allow_chunks_size1 = false; // doesn't make sense for sorting
        return chunk_cmds(
            run_size,
            allow_chunks_size1,
            [](Chunk c) { return ShrinkCmd(SortChunk { c }); });
    }

    static Vector<ShrinkCmd> zero_cmds(size_t run_size)
    {
        bool allow_chunks_size1 = false; // already happens in binary search
        return chunk_cmds(
            run_size,
            allow_chunks_size1,
            [](Chunk c) { return ShrinkCmd(ZeroChunk { c }); });
    }

    static Vector<ShrinkCmd> swap_chunk_cmds(size_t run_size)
    {
        bool allow_chunks_size1 = false; // already happens in "redistribute choice"
        return chunk_cmds(
            run_size, // TODO this is not optimal as the later chunks will hit OOB. For now, it will work though.
            allow_chunks_size1,
            [](Chunk c) { return ShrinkCmd(SwapChunkWithNeighbour { c }); });
    }
};

template<>
struct AK::Formatter<ShrinkCmd> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, ShrinkCmd cmd)
    {
        return Formatter<StringView>::format(builder, TRY(cmd.to_string()));
    }
};
