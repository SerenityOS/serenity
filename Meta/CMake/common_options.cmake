#
# Options common for the Serenity (target) and Lagom (host) builds
#

serenity_option(ENABLE_COMPILETIME_FORMAT_CHECK ON CACHE BOOL "Enable compiletime format string checks")
serenity_option(ENABLE_UNDEFINED_SANITIZER OFF CACHE BOOL "Enable undefined behavior sanitizer testing in gcc/clang")

serenity_option(ENABLE_ALL_THE_DEBUG_MACROS OFF CACHE BOOL "Enable all debug macros to validate they still compile")
serenity_option(ENABLE_ALL_DEBUG_FACILITIES OFF CACHE BOOL "Enable all noisy debug symbols and options. Not recommended for normal developer use")
serenity_option(ENABLE_COMPILETIME_HEADER_CHECK OFF CACHE BOOL "Enable compiletime check that each library header compiles stand-alone")

serenity_option(ENABLE_UNICODE_DATABASE_DOWNLOAD ON CACHE BOOL "Enable download of Unicode UCD and CLDR files at build time")
serenity_option(INCLUDE_WASM_SPEC_TESTS OFF CACHE BOOL "Download and include the WebAssembly spec testsuite")
