# Contributing to Serenity

When contributing to the Serenity operating system, please be sure that the change(s) you wish to make are in line with the project vision. If you are unsure about this, open an issue first, so we can discuss it. If you are already confident that it's a good fit, you can proceed directly to a Pull Request.

Everyone is welcome to work on the project, and while we have lots of fun, it's a serious kind of fun. :^)

## Communication

The easiest way to get in touch is by joining the `#serenityos` channel on the Freenode IRC network.

## Code submission policy

Nobody is perfect, and sometimes we mess things up. That said, here are some good dos & dont's to try and stick to:

**Do:**

* Write in idiomatic Serenity C++17, using the `AK` containers in all code.
* Conform to the project coding style found in [CodingStyle.md](https://github.com/SerenityOS/serenity/blob/master/Documentation/CodingStyle.md). Please use `clang-format` (version 8 or newer) to automatically format C++ files.
* Choose expressive variable, function and class names. Make it as obvious as possible what the code is doing.
* Split your changes into separate, atomic commits.
* Make sure your commits are rebased on the master branch.
* Wrap your commit messages at 72 characters.
* The first line of the commit message should have the format "Category: Brief description of what's being changed". The "category" can be a subdirectory, but also something like "POSIX compliance" or "ClassName". Whatever seems logical.
* Write your commit messages in proper English, with care and punctuation.
* Squash your commits when making revisions after a patch review.

**Don't:**

* Submit code that's incompatible with the project licence (2-clause BSD.)
* Touch anything outside the stated scope of the PR.
* Iterate excessively on your design across multiple commits.
* Use weasel-words like "refactor" or "fix" to avoid explaining what's being changed.
* Include commented-out code.
* Write in C.
