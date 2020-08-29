function(add_simple_fuzzer name)
  add_executable(${name} "${name}.cpp")

  if (ENABLE_OSS_FUZZ)
      target_link_libraries(${name}
          PUBLIC Lagom)
  else()
    target_compile_options(${name}
      PRIVATE $<$<C_COMPILER_ID:Clang>:-g -O1 -fsanitize=fuzzer>
      )
    target_link_libraries(${name}
      PUBLIC Lagom
      PRIVATE $<$<C_COMPILER_ID:Clang>:-fsanitize=fuzzer>
      )
  endif()
endfunction()

add_simple_fuzzer(FuzzBMPLoader)
add_simple_fuzzer(FuzzELF)
add_simple_fuzzer(FuzzGemini)
add_simple_fuzzer(FuzzGIFLoader)
add_simple_fuzzer(FuzzICOLoader)
add_simple_fuzzer(FuzzJPGLoader)
add_simple_fuzzer(FuzzPNGLoader)
add_simple_fuzzer(FuzzPBMLoader)
add_simple_fuzzer(FuzzPGMLoader)
add_simple_fuzzer(FuzzPPMLoader)
add_simple_fuzzer(FuzzHttpRequest)
add_simple_fuzzer(FuzzJs)
add_simple_fuzzer(FuzzMarkdown)
add_simple_fuzzer(FuzzRegexECMA262)
add_simple_fuzzer(FuzzRegexPosixExtended)
add_simple_fuzzer(FuzzShell)
add_simple_fuzzer(FuzzURL)

if (NOT ENABLE_OSS_FUZZ AND NOT ENABLE_FUZZER_SANITIZER)
add_executable(FuzzilliJs FuzzilliJs.cpp)
# FIXME: For some reason, these option overrides are ignored and FuzzilliJs gets treated
#        as a regular fuzzer. Once fixed, please remove the "AND NOT ENABLE_FUZZER_SANITIZER" above.
target_compile_options(FuzzilliJs
    PRIVATE $<$<C_COMPILER_ID:Clang>:-g -O1 -fsanitize-coverage=trace-pc-guard>
    )
target_link_libraries(FuzzilliJs
    PUBLIC Lagom
    PRIVATE $<$<C_COMPILER_ID:Clang>:-fsanitize-coverage=trace-pc-guard>
    )
endif()
