#
# Options specific to the Lagom (host) build
#

include(${CMAKE_CURRENT_LIST_DIR}/common_options.cmake)

serenity_option(ENABLE_ADDRESS_SANITIZER OFF CACHE BOOL "Enable address sanitizer testing in gcc/clang")
serenity_option(ENABLE_MEMORY_SANITIZER OFF CACHE BOOL "Enable memory sanitizer testing in gcc/clang")
serenity_option(ENABLE_FUZZERS OFF CACHE BOOL "Build fuzzing targets")
serenity_option(ENABLE_FUZZERS_LIBFUZZER OFF CACHE BOOL "Build fuzzers using Clang's libFuzzer")
serenity_option(ENABLE_FUZZERS_OSSFUZZ OFF CACHE BOOL "Build OSS-Fuzz compatible fuzzers")
serenity_option(BUILD_LAGOM OFF CACHE BOOL "Build parts of the system targeting the host OS for fuzzing/testing")
serenity_option(ENABLE_LAGOM_CCACHE ON CACHE BOOL "Enable ccache for Lagom builds")
