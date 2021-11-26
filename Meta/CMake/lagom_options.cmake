#
# Options specific to the Lagom (host) build
#

include(${CMAKE_CURRENT_LIST_DIR}/common_options.cmake)

serenity_option(ENABLE_ADDRESS_SANITIZER OFF CACHE BOOL "Enable address sanitizer testing in gcc/clang")
serenity_option(ENABLE_MEMORY_SANITIZER OFF CACHE BOOL "Enable memory sanitizer testing in gcc/clang")
serenity_option(ENABLE_FUZZER_SANITIZER OFF CACHE BOOL "Enable fuzzer sanitizer testing in clang")
serenity_option(BUILD_LAGOM OFF CACHE BOOL "Build parts of the system targeting the host OS for fuzzing/testing")
serenity_option(ENABLE_LAGOM_CCACHE ON CACHE BOOL "Enable ccache for Lagom builds")
