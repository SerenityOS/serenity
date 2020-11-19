function(add_simple_fuzzer name)
  add_executable(${name} "${name}.cpp")
  target_compile_options(${name}
      PRIVATE $<$<C_COMPILER_ID:Clang>:-g -O1 -fsanitize=fuzzer>
      )
  target_link_libraries(${name}
      PUBLIC Lagom
      PRIVATE $<$<C_COMPILER_ID:Clang>:-fsanitize=fuzzer>
      )
endfunction()

add_simple_fuzzer(FuzzBMP)
add_simple_fuzzer(FuzzELF)
add_simple_fuzzer(FuzzGemini)
add_simple_fuzzer(FuzzPPMLoader)
add_simple_fuzzer(FuzzJs)
add_simple_fuzzer(FuzzMarkdown)

add_executable(FuzzilliJs FuzzilliJs.cpp)
target_compile_options(FuzzilliJs
    PRIVATE $<$<C_COMPILER_ID:Clang>:-g -O1 -fsanitize-coverage=trace-pc-guard>
    )
target_link_libraries(FuzzilliJs
    PUBLIC Lagom
    PRIVATE $<$<C_COMPILER_ID:Clang>:-fsanitize-coverage=trace-pc-guard>
    )
