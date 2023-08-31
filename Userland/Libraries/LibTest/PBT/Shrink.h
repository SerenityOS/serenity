#pragma once

#include <LibTest/PBT/GenResult.h>
#include <LibTest/PBT/Generator.h>
#include <LibTest/PBT/RandSource.h>
#include <LibTest/PBT/RandomRun.h>
#include <LibTest/PBT/ShrinkCmd.h>

#include <AK/String.h>

template<typename T>
struct ShrinkResult {
    bool was_improvement;
    Generated<T> generated;
};

// Shrinker

template<typename T>
ShrinkResult<T> no_improvement(Generated<T> current_best)
{
    return ShrinkResult<T> { false, current_best };
}

template<typename T, typename FN>
ShrinkResult<T> keep_if_better(RandomRun const& new_run, Generated<T> current_best, Generator<T> const& generator, FN const& test_function)
{
    if (current_best.run < new_run) {
        // The new run is worse than the current best. Let's not even try!
        return no_improvement(current_best);
    }

    RandSource source = RandSource::recorded(new_run);
    GenResult<T> gen_result = generator(source);

    return gen_result.visit(
        [&](Generated<T> new_generated) {
            Test::current_test_case_did_pass();
            test_function(new_generated.value);
            if (Test::did_current_test_case_pass()) {
                Test::current_test_case_did_fail();
                return no_improvement(current_best);
            }
            return ShrinkResult<T> { true, new_generated };
        },
        [&](Rejected) {
            return no_improvement(current_best);
        });
}

template<typename T, typename FN, typename SET_FN>
ShrinkResult<T> binary_shrink(u32 low, u32 high, SET_FN update_run, Generated<T> orig, Generator<T> const& generator, FN const& test_function)
{
    Generated<T> current_best = orig;

    // Let's try with the best case (low = most shrunk) first
    RandomRun run_with_low = update_run(low, current_best.run);
    ShrinkResult<T> after_low = keep_if_better(run_with_low, current_best, generator, test_function);
    if (after_low.was_improvement) {
        // We can't do any better
        return after_low;
    }
    // Gotta do the loop!
    ShrinkResult<T> result = after_low;
    while (low + 1 < high) {
        // TODO: do the average in a safer way?
        // https://stackoverflow.com/questions/24920503/what-is-the-right-way-to-find-the-average-of-two-values
        u32 mid = low + (high - low) / 2;
        RandomRun run_with_mid = update_run(mid, current_best.run);
        ShrinkResult<T> after_mid = keep_if_better(run_with_mid, current_best, generator, test_function);
        if (after_mid.was_improvement) {
            high = mid;
        } else {
            low = mid;
        }
        result = after_mid;
        current_best = after_mid.generated;
    }

    if (current_best.run < orig.run) {
        result.was_improvement = true;
    }
    return result;
}

template<typename T, typename FN>
ShrinkResult<T> shrink_zero(ZeroChunk c, Generated<T> current_best, Generator<T> const& generator, FN const& test_function)
{
    // TODO do we need to copy? or is it done automatically
    RandomRun new_run = current_best.run;
    warnln("Run before zeroing: {}", new_run);
    warnln("TODO: figure out if we need to copy here");
    size_t end = c.chunk.index + c.chunk.size;
    for (size_t i = c.chunk.index; i < end; i++) {
        new_run[i] = 0;
    }
    warnln("Run after zeroing: {}", new_run);
    warnln("TODO: is it any different from the following, original state.run?");
    warnln("current_best.run: {}", current_best.run);
    return keep_if_better(new_run, current_best, generator, test_function);
}

template<typename T, typename FN>
ShrinkResult<T> shrink_sort(SortChunk c, Generated<T> current_best, Generator<T> const& generator, FN const& test_function)
{
    // TODO do we need to copy? or is it done automatically
    RandomRun new_run = current_best.run;
    warnln("Run before sorting: {}", new_run);
    warnln("TODO: figure out if we need to copy here");
    new_run.sort_chunk(c.chunk);
    warnln("Run after sorting: {}", new_run);
    warnln("TODO: is it any different from the following, original state.run?");
    warnln("current_best.run: {}", current_best.run);
    return keep_if_better(new_run, current_best, generator, test_function);
}

template<typename T, typename FN>
ShrinkResult<T> shrink_delete(DeleteChunkAndMaybeDecPrevious c, Generated<T> current_best, Generator<T> const& generator, FN const& test_function)
{
    RandomRun run_deleted = current_best.run.with_deleted(c.chunk);
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
        return keep_if_better(run_decremented, current_best, generator, test_function);
    }

    // Decrementing didn't work; let's try with just the deletion.
    return keep_if_better(run_deleted, current_best, generator, test_function);
}

template<typename T, typename FN>
ShrinkResult<T> shrink_minimize(MinimizeChoice c, Generated<T> current_best, Generator<T> const& generator, FN const& test_function)
{
    RandomRun new_run = current_best.run;
    u32 value = current_best.run[c.index];

    // We can't minimize 0! Already the best possible case.
    if (value == 0) {
        return no_improvement(current_best);
    }

    return binary_shrink(
        0,
        value,
        [c](u32 new_value, RandomRun const& run) {
            RandomRun copied_run = run;
            copied_run[c.index] = new_value;
            return copied_run;
        },
        current_best,
        generator,
        test_function);
}

template<typename T, typename FN>
ShrinkResult<T> shrink_with_cmd(ShrinkCmd cmd, Generated<T> current_best, Generator<T> const& generator, FN const& test_function)
{
    return cmd.visit(
        [&](ZeroChunk c) { return shrink_zero(c, current_best, generator, test_function); },
        [&](SortChunk c) { return shrink_sort(c, current_best, generator, test_function); },
        [&](DeleteChunkAndMaybeDecPrevious c) { return shrink_delete(c, current_best, generator, test_function); },
        [&](MinimizeChoice c) { return shrink_minimize(c, current_best, generator, test_function); });
}

template<typename T, typename FN>
Generated<T> shrink_once(Generated<T> current_best, Generator<T> const& generator, FN const& test_function)
{
    auto cmds = ShrinkCmd::for_run(current_best.run);
    for (ShrinkCmd cmd : cmds) {
        /* We're keeping the list of ShrinkCmds we generated from the initial
           RandomRun, as we try to shrink our current best RandomRun.

           That means some of the ShrinkCmds might have no chance to successfully
           finish (eg. the cmd's chunk is out of bounds of the run). That's what
           we check here and based on what we skip those cmds early.

           In the next `shrink -> shrink_once` loop we'll generate a better set
           of Cmds, more tailored to the current best RandomRun.
        */
        if (!cmd.has_a_chance(current_best.run)) {
            continue;
        }
        ShrinkResult<T> result = shrink_with_cmd(cmd, current_best, generator, test_function);
        if (result.was_improvement) {
            current_best = result.generated;
        }
    }
    return current_best;
}

template<typename T, typename FN>
Generated<T> shrink(Generated<T> generated, Generator<T> const& generator, FN const& test_function)
{
    if (generated.run.is_empty()) {
        // We can't do any better
        return generated;
    }

    Generated<T> next = generated;
    Generated<T> current;
    do {
        current = next;
        next = shrink_once(current, generator, test_function);
    } while (next.run != current.run);

    return next;
}
