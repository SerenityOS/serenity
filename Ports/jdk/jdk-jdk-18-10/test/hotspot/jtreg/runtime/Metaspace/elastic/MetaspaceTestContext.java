
import sun.hotspot.WhiteBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MetaspaceTestContext {

    long context;

    final long commitLimit;
    final long reserveLimit;

    int numArenasCreated;
    int numArenasDestroyed;

    HashSet<MetaspaceTestArena> arenaList = new HashSet<>();

    long allocatedWords;
    long numAllocated;
    long deallocatedWords;
    long numDeallocated;
    long allocationFailures;

    public MetaspaceTestContext(long commitLimit, long reserveLimit) {
        this.commitLimit = commitLimit;
        this.reserveLimit = reserveLimit;
        WhiteBox wb = WhiteBox.getWhiteBox();
        context = wb.createMetaspaceTestContext(commitLimit, reserveLimit);
        if (context == 0) {
            throw new RuntimeException("Failed to create context");
        }
    }

    // no limits
    public MetaspaceTestContext() {
        this(0, 0);
    }

    public void destroy() {
        if (context != 0) {
            WhiteBox wb = WhiteBox.getWhiteBox();
            wb.destroyMetaspaceTestContext(context);
            context = 0;
        }
    }

    public void purge() {
        if (context != 0) {
            WhiteBox wb = WhiteBox.getWhiteBox();
            wb.purgeMetaspaceTestContext(context);
        }
    }

    public MetaspaceTestArena createArena(boolean is_micro, long ceiling) {
        MetaspaceTestArena arena = null;
        if (context != 0) {
            WhiteBox wb = WhiteBox.getWhiteBox();
            long arena0 = wb.createArenaInTestContext(context, is_micro);
            if (arena0 == 0) {
                throw new RuntimeException("Failed to create arena");
            }
            numArenasCreated++;
            arena = new MetaspaceTestArena(arena0, ceiling);
            arenaList.add(arena);
        }
        return arena;
    }

    public void destroyArena(MetaspaceTestArena a) {
        if (context != 0) {
            if (a.isLive()) {
                WhiteBox wb = WhiteBox.getWhiteBox();
                wb.destroyMetaspaceTestArena(a.arena);
                numArenasDestroyed++;
            }
            arenaList.remove(a);
        }
    }

    public long committedWords() {
        long l = 0;
        if (context != 0) {
            WhiteBox wb = WhiteBox.getWhiteBox();
            l = wb.getTotalCommittedWordsInMetaspaceTestContext(context);
        }
        return l;
    }

    public long usedWords() {
        long l = 0;
        if (context != 0) {
            WhiteBox wb = WhiteBox.getWhiteBox();
            l = wb.getTotalUsedWordsInMetaspaceTestContext(context);
        }
        return l;
    }

    public int numLiveArenas() {
        return arenaList.size();
    }

    public void updateTotals() {
        allocatedWords = deallocatedWords = numAllocated = numDeallocated = 0;
        for (MetaspaceTestArena a : arenaList) {
            allocatedWords += a.allocatedWords;
            deallocatedWords += a.deallocatedWords;
            numAllocated += a.numAllocated;
            numDeallocated += a.numDeallocated;
            allocationFailures += a.numAllocationFailures;
        }
    }

    public void printToTTY() {
        if (context != 0) {
            WhiteBox wb = WhiteBox.getWhiteBox();
            wb.printMetaspaceTestContext(context);
        }
    }

    /**
     * Given usage and some context information for current live arenas, do a heuristic about whether the
     * Usage seems right for this case.
     */
    public void checkStatistics() {

        // Note:
        // Estimating Used and Committed is fuzzy, and we only have limited information here
        // (we know the current state, but not the history, which determines fragmentation and
        //  freelist occupancy).
        //
        // We do not want test which constantly generate false positives, so these checks are
        // somewhat loose and only meant to check for clear outliers, e.g. leaks.

        ///// used /////

        updateTotals();

        long usageMeasured = usedWords();
        long committedMeasured = committedWords();

        if (usageMeasured > committedMeasured) {
            throw new RuntimeException("Weirdness.");
        }

        if (deallocatedWords > allocatedWords) {
            throw new RuntimeException("Weirdness.");
        }

        // If no arenas are alive, usage should be zero and committed too (in reclaiming mode)
        if (numLiveArenas() == 0) {
            if (usageMeasured > 0) {
                throw new RuntimeException("Usage > 0, expected 0");
            }
            if (Settings.settings().doesReclaim()) {
                if (committedMeasured > 0) {
                    throw new RuntimeException("Committed > 0, expected 0");
                }
            }
        }

        long expectedMinUsage = allocatedWords - deallocatedWords;

        if (usageMeasured < expectedMinUsage) {
            throw new RuntimeException("Usage too low: " + usageMeasured + " expected at least " + expectedMinUsage);
        }

        long expectedMaxUsage = allocatedWords;

        // This is necessary a bit fuzzy, since Metaspace usage consists of:
        // - whatever we allocated
        // - deallocated blocks in fbl
        // - remains of retired chunks in fbl
        // - overhead per allocation (padding for alignment, possibly allocation guards)

        // Overhead per allocation (see metaspaceArena.cpp, get_raw_allocation_word_size() )
        // Any allocation is 3 words least
        expectedMaxUsage += (numAllocated * 3);
        if (Settings.settings().usesAllocationGuards) {
            // Guards need space.
            expectedMaxUsage += (numAllocated * 2);
            // Also, they disable the fbl, so deallocated still counts as used.
            expectedMaxUsage += deallocatedWords;
        }

        // Lets add a overhead per arena. Each arena carries a free block list containing
        // deallocated/retired blocks. We do not know how much. In general, the free block list should not
        // accumulate a lot of memory but be drained in the course of allocating memory from the arena.
        long overheadPerArena = 1024 * 1024 * numLiveArenas();
        expectedMaxUsage += overheadPerArena;

        if (expectedMaxUsage < usageMeasured) {
            throw new RuntimeException("Usage seems high: " + usageMeasured + " expected at most " + expectedMaxUsage);
        }

        ///// Committed //////

        if (committedMeasured < expectedMinUsage) {
            throw new RuntimeException("Usage too low: " + usageMeasured + " expected at least " + expectedMinUsage);
        }

        // Max committed:
        // This is difficult to estimate, so just a rough guess.
        //
        // Committed space depends on:
        // 1) Usage (how much we allocated + overhead per allocation + free block list content)
        // 2) free space in used chunks
        // 3) committed chunks in freelist.
        //
        // Having only live usage numbers without history, (2) and (3) can only be roughly estimated. Since these
        // are stress tests,
        //
        long expectedMaxCommitted = usageMeasured;
        expectedMaxCommitted += Settings.rootChunkWordSize;
        if (Settings.settings().doesReclaim()) {
            expectedMaxCommitted *= 10.0;
        } else {
            expectedMaxCommitted *= 100.0;
        }

        if (committedMeasured > expectedMaxCommitted) {
            throw new RuntimeException("Committed seems high: " + committedMeasured + " expected at most " + expectedMaxCommitted);
        }

    }

    @java.lang.Override
    public java.lang.String toString() {
        return "MetaspaceTestContext{" +
                "context=" + context +
                ", commitLimit=" + commitLimit +
                ", reserveLimit=" + reserveLimit +
                ", numArenasCreated=" + numArenasCreated +
                ", numArenasDestroyed=" + numArenasDestroyed +
                ", numLiveArenas=" + numLiveArenas() +
                ", allocatedWords=" + allocatedWords +
                ", numAllocated=" + numAllocated +
                ", deallocatedWords=" + deallocatedWords +
                ", numDeallocated=" + numDeallocated +
                ", allocationFailures=" + allocationFailures +
                '}';
    }
}
