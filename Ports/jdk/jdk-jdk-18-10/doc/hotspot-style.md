% HotSpot Coding Style

## Introduction

This is a collection of rules, guidelines, and suggestions for writing
HotSpot code.  Following these will help new code fit in with existing
HotSpot code, making it easier to read and maintain.  Failure to
follow these guidelines may lead to discussion during code reviews, if
not outright rejection of a change.

### Why Care About Style?

Some programmers seem to have lexers and even C preprocessors
installed directly behind their eyeballs. The rest of us require code
that is not only functionally correct but also easy to read. More than
that, since there is no one style for easy-to-read code, and since a
mashup of many styles is just as confusing as no style at all, it is
important for coders to be conscious of the many implicit stylistic
choices that historically have gone into the HotSpot code base.

Some of these guidelines are driven by the cross-platform requirements
for HotSpot.  Shared code must work on a variety of platforms, and may
encounter deficiencies in some.  Using platform conditionalization in
shared code is usually avoided, while shared code is strongly
preferred to multiple platform-dependent implementations, so some
language features may be recommended against.

Some of the guidelines here are relatively arbitrary choices among
equally plausible alternatives.  The purpose of stating and enforcing
these rules is largely to provide a consistent look to the code.  That
consistency makes the code more readable by avoiding non-functional
distractions from the interesting functionality.

When changing pre-existing code, it is reasonable to adjust it to
match these conventions. Exception: If the pre-existing code clearly
conforms locally to its own peculiar conventions, it is not worth
reformatting the whole thing.  Also consider separating changes that
make extensive stylistic updates from those which make functional
changes.

### Counterexamples and Updates

Many of the guidelines mentioned here have (sometimes widespread)
counterexamples in the HotSpot code base. Finding a counterexample is
not sufficient justification for new code to follow the counterexample
as a precedent, since readers of your code will rightfully expect your
code to follow the greater bulk of precedents documented here.

Occasionally a guideline mentioned here may be just out of synch with
the actual HotSpot code base. If you find that a guideline is
consistently contradicted by a large number of counterexamples, please
bring it up for discussion and possible change. The architectural
rule, of course, is "When in Rome do as the Romans". Sometimes in the
suburbs of Rome the rules are a little different; these differences
can be pointed out here.

Proposed changes should be discussed on the
[HotSpot Developers](mailto:hotspot-dev@openjdk.java.net) mailing
list, and approved by
[rough consensus](https://en.wikipedia.org/wiki/Rough_consensus) of
the [HotSpot Group](https://openjdk.java.net/census#hotspot) Members.
The Group Lead determines whether consensus has been reached.
Changes are likely to be cautious and incremental, since HotSpot
coders have been using these guidelines for years.

## Structure and Formatting

### Factoring and Class Design

* Group related code together, so readers can concentrate on one
section of one file.

* Classes are the primary code structuring mechanism.  Place related
functionality in a class, or a set of related classes.  Use of either
namespaces or public non-member functions is rare in HotSpot code.
Static non-member functions are not uncommon.

* If a class `FooBar` is going to be used in more than one place, put it
a file named fooBar.hpp and fooBar.cpp. If the class is a sidekick
to a more important class `BazBat`, it can go in bazBat.hpp.

* Put a member function `FooBar::bang` into the same file that defined
`FooBar`, or its associated *.inline.hpp or *.cpp file.

* Use public accessor functions for member variables accessed
outside the class.

* Assign names to constant literals and use the names instead.

* Keep functions small, a screenful at most.  Split out chunks of
logic into file-local classes or static functions if needed.

* Factor away nonessential complexity into local inline helper
functions and helper classes.

* Think clearly about internal invariants that apply to each class,
and document them in the form of asserts within member functions.

* Make simple, self-evident contracts for member functions.  If you cannot
communicate a simple contract, redesign the class.

* Implement classes as if expecting rough usage by clients. Check for
incorrect usage of a class using `assert(...)`, `guarantee(...)`,
`ShouldNotReachHere()` and comments wherever needed.  Performance is
almost never a reason to omit asserts.

* When possible, design as if for reusability.  This forces a clear
design of the class's externals, and clean hiding of its internals.

* Initialize all variables and data structures to a known state. If a
class has a constructor, initialize it there.

* Do no optimization before its time. Prove the need to optimize.

* When you must defactor to optimize, preserve as much structure as
possible. If you must hand-inline some name, label the local copy with
the original name.

* If you need to use a hidden detail (e.g., a structure offset), name
it (as a constant or function) in the class that owns it.

* Don't use the Copy and Paste keys to replicate more than a couple
lines of code.  Name what you must repeat.

* If a class needs a member function to change a user-visible attribute, the
change should be done with a "setter" accessor matched to the simple
"getter".

### Source Files

* All source files must have a globally unique basename.  The build
system depends on this uniqueness.

* Do not put non-trivial function implementations in .hpp files. If
the implementation depends on other .hpp files, put it in a .cpp or
a .inline.hpp file.

* .inline.hpp files should only be included in .cpp or .inline.hpp
files.

* All .inline.hpp files should include their corresponding .hpp file as
the first include line. Declarations needed by other files should be put
in the .hpp file, and not in the .inline.hpp file. This rule exists to
resolve problems with circular dependencies between .inline.hpp files.

* All .cpp files include precompiled.hpp as the first include line.

* precompiled.hpp is just a build time optimization, so don't rely on
it to resolve include problems.

* Keep the include lines alphabetically sorted.

* Put conditional inclusions (`#if ...`) at the end of the include list.

### JTReg Tests

* JTReg tests should have meaningful names.

* JTReg tests associated with specific bugs should be tagged with the
`@bug` keyword in the test description.

* JTReg tests should be organized by component or feature under
`test/`, in a directory hierarchy that generally follows that of the
`src/` directory. There may be additional subdirectories to further
categorize tests by feature. This structure makes it easy to run a
collection of tests associated with a specific feature by specifying
the associated directory as the source of the tests to run.

    * Some (older) tests use the associated bug number in the directory
    name, the test name, or both.  That naming style should no longer be
    used, with existing tests using that style being candidates for migration.

### Naming

* The length of a name may be correlated to the size of its scope.  In
particular, short names (even single letter names) may be fine in a
small scope, but are usually inappropriate for larger scopes.

* Prefer whole words rather than abbreviations, unless the
abbreviation is more widely used than the long form in the code's
domain.

* Choose names consistently. Do not introduce spurious
variations. Abbreviate corresponding terms to a consistent length.

* Global names must be unique, to avoid [One Definition Rule][ODR] (ODR)
violations.  A common prefixing scheme for related global names is
often used.  (This is instead of using namespaces, which are mostly
avoided in HotSpot.)

* Don't give two names to the semantically same thing.  But use
different names for semantically different things, even if they are
representationally the same.  (So use meaningful `typedef` or template
alias names where appropriate.)

* When choosing names, avoid categorical nouns like "variable",
"field", "parameter", "value", and verbs like "compute", "get".
(`storeValue(int param)` is bad.)

* Type names and global names should use mixed-case with the first
letter of each word capitalized (`FooBar`).

* Embedded abbreviations in
otherwise mixed-case names are usually capitalized entirely rather
than being treated as a single word with only the initial letter
capitalized, e.g. "HTML" rather than "Html".

* Function and local variable names use lowercase with words separated
by a single underscore (`foo_bar`).

* Class data member names have a leading underscore, and use lowercase
with words separated by a single underscore (`_foo_bar`).

* Constant names may be upper-case or mixed-case, according to
historical necessity.  (Note: There are many examples of constants
with lowercase names.)

* Constant names should follow an existing pattern, and must have a
distinct appearance from other names in related APIs.

* Class and type names should be noun phrases. Consider an "er" suffix
for a class that represents an action.

* Function names should be verb phrases that reflect changes of state
known to a class's user, or else noun phrases if they cause no change
of state visible to the class's user.

* Getter accessor names are noun phrases, with no "`get_`" noise
word. Boolean getters can also begin with "`is_`" or "`has_`".  Member
function for reading data members usually have the same name as the
data member, exclusive of the leading underscore.

* Setter accessor names prepend "`set_`" to the getter name.

* Other member function names are verb phrases, as if commands to the receiver.

* Avoid leading underscores (as "`_oop`") except in cases required
above. (Names with leading underscores can cause portability
problems.)

### Commenting

* Clearly comment subtle fixes.

* Clearly comment tricky classes and functions.

* If you have to choose between commenting code and writing wiki
content, comment the code. Link from the wiki to the source file if
it makes sense.

* As a general rule don't add bug numbers to comments (they would soon
overwhelm the code). But if the bug report contains significant
information that can't reasonably be added as a comment, then refer to
the bug report.

* Personal names are discouraged in the source code, which is a team
product.

### Macros

* You can almost always use an inline function or class instead of a
macro. Use a macro only when you really need it.

* Templates may be preferable to multi-line macros. (There may be
subtle performance effects with templates on some platforms; revert
to macros if absolutely necessary.)

* `#ifdef`s should not be used to introduce platform-specific code
into shared code (except for `_LP64`). They must be used to manage
header files, in the pattern found at the top of every source
file. They should be used mainly for major build features, including
`PRODUCT`, `ASSERT`, `_LP64`, `INCLUDE_SERIALGC`, `COMPILER1`, etc.

* For build features such as `PRODUCT`, use `#ifdef PRODUCT` for
multiple-line inclusions or exclusions.

* For short inclusions or exclusions based on build features, use
macros like `PRODUCT_ONLY` and `NOT_PRODUCT`. But avoid using them
with multiple-line arguments, since debuggers do not handle that
well.

* Use `CATCH`, `THROW`, etc. for HotSpot-specific exception processing.

### Whitespace

* In general, don't change whitespace unless it improves readability
or consistency.  Gratuitous whitespace changes will make integrations
and backports more difficult.

* Use One-True-Brace-Style. The opening brace for a function or class
is normally at the end of the line; it is sometimes moved to the
beginning of the next line for emphasis.  Substatements are enclosed
in braces, even if there is only a single statement.  Extremely simple
one-line statements may drop braces around a substatement.

* Indentation levels are two columns.

* There is no hard line length limit.  That said, bear in mind that
excessively long lines can cause difficulties.  Some people like to
have multiple side-by-side windows in their editors, and long lines
may force them to choose among unpleasant options. They can use wide
windows, reducing the number that can fit across the screen, and
wasting a lot of screen real estate because most lines are not that
long.  Alternatively, they can have more windows across the screen,
with long lines wrapping (or worse, requiring scrolling to see in
their entirety), which is harder to read.  Similar issues exist for
side-by-side code reviews.

* Tabs are not allowed in code. Set your editor accordingly.<br>
(Emacs: `(setq-default indent-tabs-mode nil)`.)

* Use good taste to break lines and align corresponding tokens on
adjacent lines.

* Use spaces around operators, especially comparisons and
assignments. (Relaxable for boolean expressions and high-precedence
operators in classic math-style formulas.)

* Put spaces on both sides of control flow keywords `if`, `else`,
`for`, `switch`, etc.  Don't add spaces around the associated
_control_ expressions.  Examples:

    ```
    while (test_foo(args...)) {   // Yes
    while(test_foo(args...)) {    // No, missing space after while
    while ( test_foo(args...) ) { // No, excess spaces around control
    ```

* Use extra parentheses in expressions whenever operator precedence
seems doubtful. Always use parentheses in shift/mask expressions
(`<<`, `&`, `|`).  Don't add whitespace immediately inside
parentheses.

* Use more spaces and blank lines between larger constructs, such as
classes or function definitions.

* If the surrounding code has any sort of vertical organization,
adjust new lines horizontally to be consistent with that
organization. (E.g., trailing backslashes on long macro definitions
often align.)

### Miscellaneous

* Use the [Resource Acquisition Is Initialization][RAII] (RAII)
design pattern to manage bracketed critical
sections. See class `ResourceMark` for an example.

* Avoid implicit conversions to `bool`.
    * Use `bool` for boolean values.
    * Do not use ints or pointers as (implicit) booleans with `&&`, `||`,
      `if`, `while`. Instead, compare explicitly, i.e. `if (x != 0)` or
      `if (ptr != nullptr)`, etc.
    * Do not use declarations in _condition_ forms, i.e. don't use
      `if (T v = value) { ... }`.

* Use functions from globalDefinitions.hpp and related files when
performing bitwise
operations on integers. Do not code directly as C operators, unless
they are extremely simple. (Examples: `align_up`, `is_power_of_2`,
`exact_log2`.)

* Use arrays with abstractions supporting range checks.

* Always enumerate all cases in a switch statement or provide a default
case. It is ok to have an empty default with comment.


## Use of C++ Features

HotSpot was originally written in a subset of the C++98/03 language.
More recently, support for C++14 is provided, though again,
HotSpot only uses a subset.  (Backports to JDK versions lacking
support for more recent Standards must of course stick with the
original C++98/03 subset.)

This section describes that subset.  Features from the C++98/03
language may be used unless explicitly excluded here.  Features from
C++11 and C++14 may be explicitly permitted or explicitly excluded,
and discussed accordingly here.  There is a third category, undecided
features, about which HotSpot developers have not yet reached a
consensus, or perhaps have not discussed at all.  Use of these
features is also excluded.

(The use of some features may not be immediately obvious and may slip
in anyway, since the compiler will accept them.  The code review
process is the main defense against this.)

Some features are discussed in their own subsection, typically to provide
more extensive discussion or rationale for limitations.  Features that
don't have their own subsection are listed in omnibus feature sections
for permitted, excluded, and undecided features.

Lists of new features for C++11 and C++14, along with links to their
descriptions, can be found in the online documentation for some of the
compilers and libraries.  The C++14 Standard is the definitive
description.

* [C++ Standards Support in GCC](https://gcc.gnu.org/projects/cxx-status.html)
* [C++ Support in Clang](https://clang.llvm.org/cxx_status.html)
* [Visual C++ Language Conformance](https://docs.microsoft.com/en-us/cpp/visual-cpp-language-conformance)
* [libstdc++ Status](https://gcc.gnu.org/onlinedocs/libstdc++/manual/status.html)
* [libc++ Status](https://libcxx.llvm.org/cxx1y_status.html)

As a rule of thumb, permitting features which simplify writing code
and, especially, reading code, is encouraged.

Similar discussions for some other projects:

* [Google C++ Style Guide](https://google.github.io/styleguide/cppguide.html) &mdash;
Currently (2020) targeting C++17.

* [C++11 and C++14 use in Chromium](https://chromium-cpp.appspot.com) &mdash;
Categorizes features as allowed, banned, or to be discussed.

* [llvm Coding Standards](https://llvm.org/docs/CodingStandards.html) &mdash;
Currently (2020) targeting C++14.

* [Using C++ in Mozilla code](https://firefox-source-docs.mozilla.org/code-quality/coding-style/using_cxx_in_firefox_code.html) &mdash;
C++17 support is required for recent versions (2020).

### Error Handling

Do not use exceptions. Exceptions are disabled by the build configuration
for some platforms.

Rationale: There is significant concern over the performance cost of
exceptions and their usage model and implications for maintainable code.
That's not just a matter of history that has been fixed; there remain
questions and problems even today (2019). See, for example, [Zero cost
deterministic
exceptions](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2018/p0709r0.pdf).
Because of this, HotSpot has always used a build configuration that disables
exceptions where that is available. As a result, HotSpot code uses error
handling mechanisms such as two-phase construction, factory functions,
returning error codes, and immediate termination. Even if the cost of
exceptions were not a concern, the existing body of code was not written with
exception safety in mind. Making HotSpot exception safe would be a very large
undertaking.

In addition to the usual alternatives to exceptions, HotSpot provides its
own exception mechanism. This is based on a set of macros defined in
utilities/exceptions.hpp.

### RTTI (Runtime Type Information)

Do not use [Runtime Type Information][RTTI] (RTTI).
[RTTI][] is disabled by the build configuration for some
platforms.  Among other things, this means `dynamic_cast` cannot be used.

Rationale: Other than to implement exceptions (which HotSpot doesn't use),
most potential uses of [RTTI][] are better done via virtual functions.  Some of
the remainder can be replaced by bespoke mechanisms.  The cost of the
additional runtime data structures needed to support [RTTI][] are deemed not
worthwhile, given the alternatives.

### Memory Allocation

Do not use the standard global allocation and deallocation functions
(operator new and related functions).  Use of these functions by HotSpot
code is disabled for some platforms.

Rationale: HotSpot often uses "resource" or "arena" allocation.  Even
where heap allocation is used, the standard global functions are
avoided in favor of wrappers around malloc and free that support the
VM's Native Memory Tracking (NMT) feature.

Native memory allocation failures are often treated as non-recoverable.
The place where "out of memory" is (first) detected may be an innocent
bystander, unrelated to the actual culprit.

### Class Inheritance

Use public single inheritance.

Prefer composition rather than non-public inheritance.

Restrict inheritance to the "is-a" case; use composition rather than
non-is-a related inheritance.

Avoid multiple inheritance.  Never use virtual inheritance.

### Namespaces

Avoid using namespaces.  HotSpot code normally uses "all static"
classes rather than namespaces for grouping.  An "all static" class is
not instantiable, has only static members, and is normally derived
(possibly indirectly) from the helper class `AllStatic`.

Benefits of using such classes include:

* Provides access control for members, which is unavailable with
namespaces.

* Avoids [Argument Dependent Lookup][ADL] (ADL).

* Closed for additional members.  Namespaces allow names to be added in
multiple contexts, making it harder to see the complete API.

Namespaces should be used only in cases where one of those "benefits"
is actually a hindrance.

In particular, don't use anonymous namespaces.  They seem like they should
be useful, and indeed have some real benefits for naming and generated code
size on some platforms.  Unfortunately, debuggers don't seem to like them at
all.

<https://groups.google.com/forum/#!topic/mozilla.dev.platform/KsaG3lEEaRM><br>
Suggests Visual Studio debugger might not be able to refer to
anonymous namespace symbols, so can't set breakpoints in them.
Though the discussion seems to go back and forth on that.

<https://firefox-source-docs.mozilla.org/code-quality/coding-style/coding_style_cpp.html><br>
Search for "Anonymous namespaces"
Suggests preferring "static" to anonymous namespaces where applicable,
because of poor debugger support for anonymous namespaces.

<https://sourceware.org/bugzilla/show_bug.cgi?id=16874><br>
Bug for similar gdb problems.

### C++ Standard Library

Avoid using the C++ Standard Library.

Historically, HotSpot has mostly avoided use of the Standard
Library.

(It used to be impossible to use most of it in shared code,
because the build configuration for Solaris with Solaris Studio made
all but a couple of pieces inaccessible.  Support for header-only
parts was added in mid-2017.  Support for Solaris was removed
in 2020.)

Some reasons for this include

* Exceptions. Perhaps the largest core issue with adopting the use of
Standard Library facilities is exceptions. HotSpot does not use
exceptions and, for platforms which allow doing so, builds with them
turned off.  Many Standard Library facilities implicitly or explicitly
use exceptions.

* `assert`.  An issue that is quickly encountered is the `assert` macro name
collision ([JDK-8007770](https://bugs.openjdk.java.net/browse/JDK-8007770)).
Some mechanism for addressing this would be needed before much of the
Standard Library could be used.  (Not all Standard Library implementations
use assert in header files, but some do.)

* Memory allocation. HotSpot requires explicit control over where
allocations occur. The C++98/03 `std::allocator` class is too limited
to support our usage.  (Changes in more recent Standards may remove
this limitation.)

* Implementation vagaries. Bugs, or simply different implementation choices,
can lead to different behaviors among the various Standard Libraries we need
to deal with.

* Inconsistent naming conventions. HotSpot and the C++ Standard use
different naming conventions. The coexistence of those different conventions
might appear jarring and reduce readability.

There are a few exceptions to this rule.

* `#include <new>` to use placement `new`, `std::nothrow`, and `std::nothrow_t`.
* `#include <limits>` to use `std::numeric_limits`.
* `#include <type_traits>`.
* `#include <cstddef>` to use `std::nullptr_t`.

TODO: Rather than directly \#including (permitted) Standard Library
headers, use a convention of \#including wrapper headers (in some
location like hotspot/shared/stdcpp).  This provides a single place
for dealing with issues we might have for any given header, esp.
platform-specific issues.

### Type Deduction

Use type deduction only if it makes the code clearer or safer.  Do not
use it merely to avoid the inconvenience of writing an explicit type,
unless that type is itself difficult to write.  An example of the
latter is a function template return type that depends on template
parameters in a non-trivial way.

There are several contexts where types are deduced.

* Function argument deduction.  This is always permitted, and indeed
encouraged.  It is nearly always better to allow the type of a
function template argument to be deduced rather than explicitly
specified.

* `auto` variable declarations
([n1984](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2006/n1984.pdf))<br>
For local variables, this can be used to make the code clearer by
eliminating type information that is obvious or irrelevant.  Excessive
use can make code much harder to understand.

* Function return type deduction
([n3638](https://isocpp.org/files/papers/N3638.html))<br>
Only use if the function body has a very small number of `return`
statements, and generally relatively little other code.

* Generic lambdas.  Lambdas are not (yet) permitted.

* Lambda init captures.  Lambdas are not (yet) permitted.

### Expression SFINAE

[Substitution Failure Is Not An Error][SFINAE] (SFINAE)
is a template metaprogramming technique that makes use of
template parameter substitution failures to make compile-time decisions.

C++11 relaxed the rules for what constitutes a hard-error when
attempting to substitute template parameters with template arguments,
making most deduction errors be substitution errors; see
([n2634](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2008/n2634.html)).
This makes [SFINAE][] more powerful and easier to use.  However, the
implementation complexity for this change is significant, and this
seems to be a place where obscure corner-case bugs in various
compilers can be found.  So while this feature can (and indeed should)
be used (and would be difficult to avoid), caution should be used when
pushing to extremes.

Here are a few closely related example bugs:<br>
<https://gcc.gnu.org/bugzilla/show_bug.cgi?id=95468><br>
<https://developercommunity.visualstudio.com/content/problem/396562/sizeof-deduced-type-is-sometimes-not-a-constant-ex.html>

### enum

Where appropriate, _scoped-enums_ should be used.
([n2347](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2007/n2347.pdf)) 

Use of _unscoped-enums_ is permitted, though ordinary constants may be
preferable when the automatic initializer feature isn't used.

The underlying type (the _enum-base_) of an unscoped enum type should
always be specified explicitly.  When unspecified, the underlying type
is dependent on the range of the enumerator values and the platform.

The underlying type of a _scoped-enum_ should also be specified
explicitly if conversions may be applied to values of that type.

Due to bugs in certain (very old) compilers, there is widespread use
of enums and avoidance of in-class initialization of static integral
constant members.  Compilers having such bugs are no longer supported.
Except where an enum is semantically appropriate, new code should use
integral constants.

### thread_local

Do not use `thread_local`
([n2659](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2008/n2659.htm));
instead, use the HotSpot macro `THREAD_LOCAL`.  The initializer must
be a constant expression.

As was discussed in the review for
[JDK-8230877](https://mail.openjdk.java.net/pipermail/hotspot-dev/2019-September/039487.html),
`thread_local` allows dynamic initialization and destruction
semantics.  However, that support requires a run-time penalty for
references to non-function-local `thread_local` variables defined in a
different translation unit, even if they don't need dynamic
initialization.  Dynamic initialization and destruction of
namespace-scoped thread local variables also has the same ordering
problems as for ordinary namespace-scoped variables.

### nullptr

Prefer `nullptr`
([n2431](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2007/n2431.pdf))
to `NULL`.  Don't use (constexpr or literal) 0 for pointers. 

For historical reasons there are widespread uses of both `NULL` and of
integer 0 as a pointer value.

### &lt;atomic&gt;

Do not use facilities provided by the `<atomic>` header
([n2427](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2007/n2427.html)),
([n2752](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2008/n2752.htm));
instead, use the HotSpot `Atomic` class and related facilities.

Atomic operations in HotSpot code must have semantics which are
consistent with those provided by the JDK's compilers for Java.  There
are platform-specific implementation choices that a C++ compiler might
make or change that are outside the scope of the C++ Standard, and
might differ from what the Java compilers implement.

In addition, HotSpot `Atomic` has a concept of "conservative" memory
ordering, which may differ from (may be stronger than) sequentially
consistent.  There are algorithms in HotSpot that are believed to rely
on that ordering.

### Uniform Initialization

The use of _uniform initialization_
([n2672](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2008/n2672.htm)),
also known as _brace initialization_, is permitted.

Some relevant sections from cppreference.com:

* [initialization](https://en.cppreference.com/w/cpp/language/initialization)
* [value initialization](https://en.cppreference.com/w/cpp/language/value_initialization)
* [direct initialization](https://en.cppreference.com/w/cpp/language/direct_initialization)
* [list initialization](https://en.cppreference.com/w/cpp/language/list_initialization)
* [aggregate initialization](https://en.cppreference.com/w/cpp/language/aggregate_initialization)

Although related, the use of `std::initializer_list` remains forbidden, as
part of the avoidance of the C++ Standard Library in HotSpot code.

### Additional Permitted Features

* `constexpr`
([n2235](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2007/n2235.pdf)) 
([n3652](https://isocpp.org/files/papers/N3652.html))

* Sized deallocation
([n3778](https://isocpp.org/files/papers/n3778.html))

* Variadic templates
([n2242](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2007/n2242.pdf))
([n2555](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2008/n2555.pdf))

* Static assertions
([n1720](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2004/n1720.html))

* `decltype`
([n2343](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2007/n2343.pdf))
([n3276](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2011/n3276.pdf))

* Right angle brackets
([n1757](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2005/n1757.html))

* Default template arguments for function templates
([CWG D226](http://www.open-std.org/jtc1/sc22/wg21/docs/cwg_defects.html#226))

* Template aliases
([n2258](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2007/n2258.pdf))

* Delegating constructors
([n1986](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2006/n1986.pdf))

* Explicit conversion operators
([n2437](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2007/n2437.pdf))

* Standard Layout Types
([n2342](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2007/n2342.htm))

* Defaulted and deleted functions
([n2346](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2007/n2346.htm))

* Dynamic initialization and destruction with concurrency
([n2660](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2008/n2660.htm))

* `final` virtual specifiers for classes and virtual functions
([n2928](http://www.open-std.org/JTC1/SC22/WG21/docs/papers/2009/n2928.htm)),
([n3206](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2010/n3206.htm)),
([n3272](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2011/n3272.htm))

* `override` virtual specifiers for virtual functions
([n2928](http://www.open-std.org/JTC1/SC22/WG21/docs/papers/2009/n2928.htm)),
([n3206](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2010/n3206.htm)),
([n3272](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2011/n3272.htm))

* Local and unnamed types as template parameters
([n2657](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2008/n2657.htm))

* Range-based `for` loops
([n2930](http://www.open-std.org/JTC1/SC22/WG21/docs/papers/2009/n2930.html))
([range-for](https://en.cppreference.com/w/cpp/language/range-for))

### Excluded Features

* New string and character literals
    * New character types
    ([n2249](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2007/n2249.html))
    * Unicode string literals
    ([n2442](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2007/n2442.htm))
    * Raw string literals
    ([n2442](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2007/n2442.htm))
    * Universal character name literals
    ([n2170](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2007/n2170.html))

    HotSpot doesn't need any of the new character and string literal
    types.

* User-defined literals
([n2765](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2008/n2765.pdf)) &mdash;
User-defined literals should not be added casually, but only
through a proposal to add a specific UDL.

* Inline namespaces
([n2535](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2008/n2535.htm)) &mdash;
HotSpot makes very limited use of namespaces.

* `using namespace` directives.  In particular, don't use `using
namespace std;` to avoid needing to qualify Standard Library names.

* Propagating exceptions
([n2179](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2007/n2179.html)) &mdash;
HotSpot does not permit the use of exceptions, so this feature isn't useful.

* Avoid namespace-scoped variables with non-constexpr initialization.
In particular, avoid variables with types requiring non-trivial
initialization or destruction.  Initialization order problems can be
difficult to deal with and lead to surprises, as can destruction
ordering.  HotSpot doesn't generally try to cleanup on exit, and
running destructors at exit can also lead to problems.

* `[[deprecated]]` attribute
([n3760](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2013/n3760.html)) &mdash;
Not relevant in HotSpot code.

* Avoid most operator overloading, preferring named functions.  When
operator overloading is used, ensure the semantics conform to the
normal expected behavior of the operation.

* Avoid most implicit conversion constructors and (implicit or explicit)
conversion operators.  (Note that conversion to `bool` isn't needed
in HotSpot code because of the "no implicit boolean" guideline.)

* Avoid covariant return types.

* Avoid `goto` statements. 

### Undecided Features

This list is incomplete; it serves to explicitly call out some
features that have not yet been discussed.

* Trailing return type syntax for functions 
([n2541](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2008/n2541.htm))

* Variable templates
([n3651](https://isocpp.org/files/papers/N3651.pdf))

* Member initializers and aggregates
([n3653](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2013/n3653.html))

* `[[noreturn]]` attribute
([n2761](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2008/n2761.pdf))

* Rvalue references and move semantics

* Lambdas


[ADL]: https://en.cppreference.com/w/cpp/language/adl 
  "Argument Dependent Lookup"

[ODR]: https://en.cppreference.com/w/cpp/language/definition
  "One Definition Rule"

[RAII]: https://en.cppreference.com/w/cpp/language/raii
  "Resource Acquisition Is Initialization"

[RTTI]: https://en.wikipedia.org/wiki/Run-time_type_information
  "Runtime Type Information"

[SFINAE]: https://en.cppreference.com/w/cpp/language/sfinae
  "Substitution Failure Is Not An Error"
