add_executable(FuzzJs FuzzJs.cpp)
target_compile_options(FuzzJs
    PRIVATE $<$<C_COMPILER_ID:Clang>:-g -O1 -fsanitize=fuzzer>
    )

target_link_libraries(FuzzJs
    PUBLIC lagom
    PRIVATE $<$<C_COMPILER_ID:Clang>:-fsanitize=fuzzer>
    )
