add_executable(FuzzBMP FuzzBMP.cpp)
target_compile_options(FuzzBMP
    PRIVATE $<$<C_COMPILER_ID:Clang>:-g -O1 -fsanitize=fuzzer>
    )
target_link_libraries(FuzzBMP
    PUBLIC Lagom
    PRIVATE $<$<C_COMPILER_ID:Clang>:-fsanitize=fuzzer>
    )

add_executable(FuzzELF FuzzELF.cpp)
target_compile_options(FuzzELF
    PRIVATE $<$<C_COMPILER_ID:Clang>:-g -O1 -fsanitize=fuzzer>
    )
target_link_libraries(FuzzELF
    PUBLIC Lagom
    PRIVATE $<$<C_COMPILER_ID:Clang>:-fsanitize=fuzzer>
    )

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
