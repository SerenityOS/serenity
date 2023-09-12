#pragma once

#include <LibTest/PBT/GenResult.h>
#include <LibTest/PBT/Generator.h>
#include <LibTest/PBT/RandSource.h>
#include <LibTest/PBT/RandomRun.h>
#include <LibTest/PBT/ShrinkCmd.h>

#include <AK/String.h>

struct ShrinkResult {
    bool was_improvement;
    RandomRun run;
};

// Shrinker

inline ShrinkResult no_improvement(RandomRun run)
{
    return ShrinkResult { false, run };
}

template<typename FN>
ShrinkResult keep_if_better(RandomRun const& new_run, RandomRun const& current_best, FN const& test_function)
{
    if (current_best < new_run) {
        // The new run is worse than the current best. Let's not even try!
        return no_improvement(current_best);
    }

    RandSource source = RandSource::recorded(new_run);
    Test::current_test_case_did_pass();
    test_function();
    if (Test::did_current_test_case_pass()) {
        Test::current_test_case_did_fail();
        return no_improvement(current_best);
    }
    return ShrinkResult { true, new_run };
}

template<typename FN, typename SET_FN>
ShrinkResult binary_shrink(u32 low, u32 high, SET_FN update_run, RandomRun const& orig, FN const& test_function)
{
    RandomRun current_best = orig;

    // Let's try with the best case (low = most shrunk) first
    RandomRun run_with_low = update_run(low, current_best);
    ShrinkResult after_low = keep_if_better(run_with_low, current_best, test_function);
    if (after_low.was_improvement) {
        // We can't do any better
        return after_low;
    }

    // Gotta do the loop!
    ShrinkResult result = after_low;
    while (low + 1 < high) {
        // TODO: do the average in a safer way?
        // https://stackoverflow.com/questions/24920503/what-is-the-right-way-to-find-the-average-of-two-values
        u32 mid = low + (high - low) / 2;
        RandomRun run_with_mid = update_run(mid, current_best);
        ShrinkResult after_mid = keep_if_better(run_with_mid, current_best, test_function);
        if (after_mid.was_improvement) {
            high = mid;
        } else {
            low = mid;
        }
        result = after_mid;
        current_best = after_mid.run;
    }

    if (current_best < orig) {
        result.was_improvement = true;
    }
    return result;
}

template<typename FN>
ShrinkResult shrink_zero(ZeroChunk c, RandomRun const& run, FN const& test_function)
{
    // TODO do we need to copy? or is it done automatically
    RandomRun new_run = run;
    warnln("Run before zeroing: {}", new_run);
    warnln("TODO: figure out if we need to copy here");
    size_t end = c.chunk.index + c.chunk.size;
    for (size_t i = c.chunk.index; i < end; i++) {
        new_run[i] = 0;
    }
    warnln("Run after zeroing: {}", new_run);
    warnln("TODO: is it any different from the following, original state.run?");
    warnln("current_best.run: {}", run);
    return keep_if_better(new_run, run, test_function);
}

template<typename FN>
ShrinkResult shrink_sort(SortChunk c, RandomRun const& run, FN const& test_function)
{
    // TODO do we need to copy? or is it done automatically
    RandomRun new_run = run;
    warnln("Run before sorting: {}", new_run);
    warnln("TODO: figure out if we need to copy here");
    new_run.sort_chunk(c.chunk);
    warnln("Run after sorting: {}", new_run);
    warnln("TODO: is it any different from the following, original state.run?");
    warnln("current_best.run: {}", run);
    return keep_if_better(new_run, run, test_function);
}

template<typename FN>
ShrinkResult shrink_delete(DeleteChunkAndMaybeDecPrevious c, RandomRun const& run, FN const& test_function)
{
    RandomRun run_deleted = run.with_deleted(c.chunk);
    /* Optional: decrement the previous value. This deals with a non-optimal but
       relatively common generation pattern: run length encoding.

       Example: let's say somebody generates lists in this way:
       * generate a random integer >=0 for the length of the list.
       * then, generate that many items

       This results in RandomRuns like this one:
       [ 3 (length), 50 (item 1), 21 (item 2), 1 (item 3) ]

       Then if we tried deleting the second item without decrementing
       the length, it would fail:
       [ 3 (length), 21 (item 1), 1 (item 2) ] ... runs out of randomness when
       trying to generate the third item!

       That's why we try to decrement the number right before the deleted items:
       [ 2 (length), 21 (item 1), 1 (item 2) ] ... generates fine!

       Aside: this is why we're generating lists in a different way that plays
       nicer with shrinking: we flip a coin (generate a bool which is `true` with
       a certain probability) to see whether to generate another item. This makes
       items "local" instead of entangled with the non-local length.
    */
    if (run_deleted.size() > c.chunk.index - 1) {
        RandomRun run_decremented = run_deleted;
        run_decremented[c.chunk.index - 1]--;
        return keep_if_better(run_decremented, run, test_function);
    }

    // Decrementing didn't work; let's try with just the deletion.
    return keep_if_better(run_deleted, run, test_function);
}

template<typename FN>
ShrinkResult shrink_minimize(MinimizeChoice c, RandomRun const& run, FN const& test_function)
{
    u32 value = run[c.index];

    // We can't minimize 0! Already the best possible case.
    if (value == 0) {
        return no_improvement(run);
    }

    return binary_shrink(
        0,
        value,
        [&](u32 new_value, RandomRun const& run) {
            RandomRun copied_run = run;
            copied_run[c.index] = new_value;
            return copied_run;
        },
        run,
        test_function);
}

template<typename FN>
ShrinkResult shrink_with_cmd(ShrinkCmd cmd, RandomRun const& run, FN const& test_function)
{
    return cmd.visit(
        [&](ZeroChunk c) { return shrink_zero(c, run, test_function); },
        [&](SortChunk c) { return shrink_sort(c, run, test_function); },
        [&](DeleteChunkAndMaybeDecPrevious c) { return shrink_delete(c, run, test_function); },
        [&](MinimizeChoice c) { return shrink_minimize(c, run, test_function); });
}

template<typename FN>
RandomRun shrink_once(RandomRun const& run, FN const& test_function)
{
    RandomRun current = run;

    auto cmds = ShrinkCmd::for_run(run);
    for (ShrinkCmd cmd : cmds) {
        /* We're keeping the list of ShrinkCmds we generated from the initial
           RandomRun, as we try to shrink our current best RandomRun.

           That means some of the ShrinkCmds might have no chance to successfully
           finish (eg. the cmd's chunk is out of bounds of the run). That's what
           we check here and based on what we skip those cmds early.

           In the next `shrink -> shrink_once` loop we'll generate a better set
           of Cmds, more tailored to the current best RandomRun.
        */
        if (!cmd.has_a_chance(current)) {
            continue;
        }
        ShrinkResult result = shrink_with_cmd(cmd, current, test_function);
        if (result.was_improvement) {
            current = result.run;
        }
    }
    return current;
}

template<typename FN>
RandomRun shrink(RandomRun const& first_failure, FN const& test_function)
{
    if (first_failure.is_empty()) {
        // We can't do any better
        return first_failure;
    }

    RandomRun next = first_failure;
    RandomRun current;
    do {
        current = next;
        next = shrink_once(current, test_function);
    } while (next != current);

    return next;
}
