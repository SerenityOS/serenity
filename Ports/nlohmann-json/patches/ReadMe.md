# Patches for nlohmann-json on SerenityOS

## `0001-Provide-fallback-for-missing-char8_t-support.patch`

Provide fallback for missing `char8_t` support

Clang 11.0.x with libc++ fails to compile tests in C++20 mode due to
incomplete char8_t support in std::filesystem::path.

