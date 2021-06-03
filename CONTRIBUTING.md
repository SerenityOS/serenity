# Contributing to SerenityOS

When contributing to SerenityOS, make sure that the changes you wish to make are in line with the project direction. If you are not sure about this, open an issue first, so we can discuss it.

For your first PR, start with something small to get familiar with the project and its development processes.

Everyone is welcome to work on the project, and while we have lots of fun, it's a serious kind of fun. :^)

## Communication

Discord: [SerenityOS Discord](https://discord.com/invite/29gCcKsXkF)

IRC: `#serenityos` on the Freenode IRC network.

## Issue policy

Unlike many other software projects, SerenityOS is not concerned with gaining the largest possible userbase. Its target audience is its own developers. As such, we have limited interest in feature requests from non-contributors.

That said, please do file any bugs you find, keeping the following in mind:

* One issue per bug. Putting multiple things in the same issue makes both discussion and completion unnecessarily complicated.
* No build issues (or other support requests). If the GitHub Actions CI build succeeds, the build problem is most likely on your side. Work it out locally, or ask in the `#build-problems` channel on Discord.
* Don't comment on issues just to add a joke or irrelevant commentary. Hundreds of people get notified about comments so let's keep them relevant.
* For bare metal issues, please include the complete debug log from the serial console and what you tried to do to solve the issue before opening the issue. Don't forget to add the hardware model of your machine and relevant details about it, to help us diagnose what is the problem.

## Human language policy

In SerenityOS, we treat human language as seriously as we do programming language.

The following applies to all user-facing strings, code, comments, and commit messages:

* The official project language is American English with ISO 8601 dates and metric units.
* Use proper spelling, grammar, and punctuation.
* Write in an authoritative and technical tone.

Everyone is encouraged to make use of tooling (spell checkers, etc) to make this easier.

## Code submission policy

Nobody is perfect, and sometimes we mess things up. That said, here are some good dos & dont's to try and stick to:

**Do:**

* Write in idiomatic SerenityOS C++20, using the `AK` containers in all code.
* Conform to the project coding style found in [CodingStyle.md](https://github.com/SerenityOS/serenity/blob/master/Documentation/CodingStyle.md). Use `clang-format` (version 11 or later) to automatically format C++ files.
* Choose expressive variable, function and class names. Make it as obvious as possible what the code is doing.
* Split your changes into separate, atomic commits (i.e. A commit per feature or fix, where the build, tests and the system are all functioning).
* Make sure your commits are rebased on the master branch.
* Wrap your commit messages at 72 characters.
* The first line of the commit message is the subject line, and should have the format "Category: Brief description of what's being changed". The "category" can be a subdirectory, but also something like "POSIX compliance" or "ClassName". Whatever seems logical.
* Write the commit message subject line in the imperative mood ("Foo: Change the way dates work", not "Foo: Changed the way dates work").
* Write your commit messages in proper English, with care and punctuation.
* Squash your commits when making revisions after a patch review.
* Add your personal copyright line to files when making substantive changes. (Optional but encouraged!)
* Check the spelling of your code, comments and commit messages.

**Don't:**

* Submit code that's incompatible with the project licence (2-clause BSD.)
* Touch anything outside the stated scope of the PR.
* Iterate excessively on your design across multiple commits.
* Use weasel-words like "refactor" or "fix" to avoid explaining what's being changed.
* End commit message subject lines with a period.
* Include commented-out code.
* Write in C. (Instead, take advantage of C++'s amenities, and don't limit yourself to the standard C library.)
* Attempt large architectural changes until you are familiar with the system and have worked on it for a while.
