# LibTest/PBT

LibTest/PBT is a property based testing library for SerenityOS.

Property based testing involves generating random test inputs and checking that
they satisfy certain properties. This allows testing a large number of possible
cases automatically, and is exceptionally good at finding edge cases.

When a failure is found, it's automatically shrunk to a minimal reproducing
case. (This is good, because random values contain a lot of unimportant details
that only hamper understanding of the root cause. See http://sscce.org)

This particular implementation belongs to the "internal shrinking" family of PBT
libraries (Hypothesis, minithesis, elm-test, ...). This is important for the DX
as it gives the most resilient shrinkers without any user effort, compared to
the "integrated shrinking" lazy-tree based implementations (Hedgehog etc.) or
the manual shrinking ones (original QuickCheck etc.). (More information in this
talk: https://www.youtube.com/watch?v=WE5bmt0zBxg)

## Implementation

On a very high level, the PBT runner remembers the random bits (`RandomRun`)
used when generating the random value, and when it finds a value that fails the
user's test, it takes this sequence of random bits and tries to shrink it and
then generate a new value from it. (Generators are implemented in such a way
that a smaller/shorter `RandomRun` will result in a simpler generated value.) If
successful and if the test still fails on this new simpler value, we keep this
as our new best and try again. Once we can't improve things anymore, we report
the failing value to the user.

Here are the important concepts and their descriptions:

- `TestResult<T>`
  - A result of running a property based test.
  - Three cases:
    - `Passes`
      - "We were able to generate values, and no value failed the test."
    - `FailsWith<T>`
      - "We were able to generate values, and one of them failed the test. Here
        it is, shrunk to a minimal example."
    - `CannotGenerateValues`
      - "We weren't able to generate values. Here are the reasons the generators
        gave us."

- `Generator<T>`
  - Knows how to generate a value, given some random bits.
  - Can fail to generate.
  - Not purely type-based ("give me an unsigned int") - in practice gets very
    granular ("give me a number in range 1..10", "give me a vector of ASCII
    strings of length between 3 and 5", etc.)
  - Takes `RandSource` and returns `GenResult<T>`.
  - Example:
    `Gen::vector_of_length(3, Gen::int(-5,5).map([](int n){return n * 2;}))`
    - Would generate values like: {-6, 2, 8}, {4, -10, 0}, {2, 2, 10}.

- `RandSource`
  - A source of random bits.
  - There are two variants of `RandSource`:
    - Live: contains and advances a PRNG
    - Recorded: contains and advances a pre-determined `RandomRun`

- `RandomRun`
  - A finite sequence of random bits (in practice, of unsigned ints).
  - Initially recorded as random values are taken from the Live `RandSource`,
    then subject to shrinking if the run led to failing values.
  - Example: `{2,5,0,11,8,0,0,1}`

- `ShrinkCmd`
  - A high-level recipe for how to try and minimize a given `RandomRun`.
  - For example, "zero this contiguous chunk of it" or "minimize the number on
    this index using binary search".
  - These later get interpreted by the PBT runner on a specific `RandomRun`.

- `Chunk`
  - A description of a contiguous `RandomRun` slice.
  - Example: `Chunk{size = 4, start_index = 2}`
