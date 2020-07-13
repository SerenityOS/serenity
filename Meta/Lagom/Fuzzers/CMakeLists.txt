add_executable(FuzzJs FuzzJs.cpp)
target_compile_options(FuzzJs
    PRIVATE $<$<C_COMPILER_ID:Clang>:-g -O1 -fsanitize=fuzzer>
    )

target_link_libraries(FuzzJs
    PUBLIC Lagom
    PRIVATE $<$<C_COMPILER_ID:Clang>:-fsanitize=fuzzer>
    )

add_executable(FuzzMarkdown FuzzMarkdown.cpp)
target_compile_options(FuzzMarkdown
    PRIVATE $<$<C_COMPILER_ID:Clang>:-g -O1 -fsanitize=fuzzer>
    )

target_link_libraries(FuzzMarkdown
    PUBLIC Lagom
    PRIVATE $<$<C_COMPILER_ID:Clang>:-fsanitize=fuzzer>
    )
