function(add_simple_fuzzer name)
  add_executable(${name} "${name}.cpp")

  if (ENABLE_OSS_FUZZ)
      target_link_libraries(${name}
          PUBLIC ${ARGN} LibCore)
  else()
    target_compile_options(${name}
      PRIVATE $<$<CXX_COMPILER_ID:Clang>:-g -O1 -fsanitize=fuzzer>
      )
    target_link_libraries(${name}
      PUBLIC ${ARGN} LibCore
      PRIVATE $<$<CXX_COMPILER_ID:Clang>:-fsanitize=fuzzer>
      )
  endif()
endfunction()

add_simple_fuzzer(FuzzBMPLoader LibGfx LibIPC)
add_simple_fuzzer(FuzzCyrillicDecoder LibTextCodec)
add_simple_fuzzer(FuzzDeflateCompression LibCompress)
add_simple_fuzzer(FuzzDeflateDecompression LibCompress)
add_simple_fuzzer(FuzzELF)
add_simple_fuzzer(FuzzGemini LibGemini)
add_simple_fuzzer(FuzzGIFLoader LibGfx LibIPC)
add_simple_fuzzer(FuzzGzipCompression LibCompress)
add_simple_fuzzer(FuzzGzipDecompression LibCompress)
add_simple_fuzzer(FuzzICOLoader LibGfx LibIPC)
add_simple_fuzzer(FuzzJPGLoader LibGfx LibIPC)
add_simple_fuzzer(FuzzPNGLoader LibGfx LibIPC)
add_simple_fuzzer(FuzzPBMLoader LibGfx LibIPC)
add_simple_fuzzer(FuzzPGMLoader LibGfx LibIPC)
add_simple_fuzzer(FuzzPPMLoader LibGfx LibIPC)
add_simple_fuzzer(FuzzHebrewDecoder LibTextCodec)
add_simple_fuzzer(FuzzHttpRequest LibHTTP)
add_simple_fuzzer(FuzzJs LibJS)
add_simple_fuzzer(FuzzLatin1Decoder LibTextCodec)
add_simple_fuzzer(FuzzLatin2Decoder LibTextCodec)
add_simple_fuzzer(FuzzMarkdown LibMarkdown)
add_simple_fuzzer(FuzzRegexECMA262 LibRegex)
add_simple_fuzzer(FuzzRegexPosixExtended LibRegex)
add_simple_fuzzer(FuzzShell LibShell)
add_simple_fuzzer(FuzzTTF LibTTF LibGfx LibIPC)
add_simple_fuzzer(FuzzURL)
add_simple_fuzzer(FuzzUTF16BEDecoder LibTextCodec)
add_simple_fuzzer(FuzzRSAKeyParsing LibCrypto)
add_simple_fuzzer(FuzzWAVLoader LibAudio)
add_simple_fuzzer(FuzzWasmParser LibWasm)
add_simple_fuzzer(FuzzZip LibArchive)
add_simple_fuzzer(FuzzZlibDecompression LibCompress)

if (NOT ENABLE_OSS_FUZZ)
set(CMAKE_EXE_LINKER_FLAGS "${ORIGINAL_CMAKE_EXE_LINKER_FLAGS} -fsanitize=address")
set(CMAKE_SHARED_LINKER_FLAGS "${ORIGINAL_CMAKE_SHARED_LINKER_FLAGS} -fsanitize=address")
set(CMAKE_MODULE_LINKER_FLAGS "${ORIGINAL_CMAKE_MODULE_LINKER_FLAGS} -fsanitize=address")
add_executable(FuzzilliJs FuzzilliJs.cpp)
target_compile_options(FuzzilliJs
    PRIVATE $<$<CXX_COMPILER_ID:Clang>:-g -O1 -fsanitize-coverage=trace-pc-guard>
    )
target_link_libraries(FuzzilliJs
    PUBLIC LibCore
    PRIVATE $<$<CXX_COMPILER_ID:Clang>:-fsanitize-coverage=trace-pc-guard>
    )
endif()
