set(FUZZER_TARGETS
    ASN1
    Base64Roundtrip
    BLAKE2b
    BMPLoader
    Brotli
    DDSLoader
    DNSPacket
    DeflateCompression
    DeflateDecompression
    ELF
    FlacLoader
    Gemini
    GIFLoader
    GzipDecompression
    GzipRoundtrip
    HttpRequest
    ICCProfile
    ICOLoader
    ILBMLoader
    IMAPParser
    JBIG2Loader
    JPEGLoader
    Js
    JsonParser
    LzmaDecompression
    LzmaRoundtrip
    Markdown
    MatroskaReader
    MD5
    MP3Loader
    PAMLoader
    PBMLoader
    PDF
    PEM
    PGMLoader
    PNGLoader
    Poly1305
    PPMLoader
    QOALoader
    QOILoader
    QuotedPrintableParser
    RegexECMA262
    RegexPosixBasic
    RegexPosixExtended
    RSAKeyParsing
    SHA1
    SHA256
    SHA384
    SHA512
    Shell
    ShellPosix
    SQLParser
    Tar
    TextDecoder
    TGALoader
    TIFFLoader
    TTF
    TinyVGLoader
    URL
    VP9Decoder
    WasmParser
    WAVLoader
    WebPLoader
    WOFF
    WOFF2
    XML
    Zip
    ZlibDecompression
)

if (TARGET LibWeb)
    list(APPEND FUZZER_TARGETS CSSParser)
endif()

set(FUZZER_DEPENDENCIES_ASN1 LibCrypto LibTLS)
set(FUZZER_DEPENDENCIES_BLAKE2b LibCrypto)
set(FUZZER_DEPENDENCIES_BMPLoader LibGfx)
set(FUZZER_DEPENDENCIES_Brotli LibCompress)
set(FUZZER_DEPENDENCIES_CSSParser LibWeb)
set(FUZZER_DEPENDENCIES_DDSLoader LibGfx)
set(FUZZER_DEPENDENCIES_DNSPacket LibDNS)
set(FUZZER_DEPENDENCIES_DeflateCompression LibCompress)
set(FUZZER_DEPENDENCIES_DeflateDecompression LibCompress)
set(FUZZER_DEPENDENCIES_ELF LibELF)
set(FUZZER_DEPENDENCIES_FlacLoader LibAudio)
set(FUZZER_DEPENDENCIES_Gemini LibGemini)
set(FUZZER_DEPENDENCIES_GIFLoader LibGfx)
set(FUZZER_DEPENDENCIES_GzipDecompression LibCompress)
set(FUZZER_DEPENDENCIES_GzipRoundtrip LibCompress)
set(FUZZER_DEPENDENCIES_HttpRequest LibHTTP)
set(FUZZER_DEPENDENCIES_ICCProfile LibGfx)
set(FUZZER_DEPENDENCIES_ICOLoader LibGfx)
set(FUZZER_DEPENDENCIES_ILBMLoader LibGfx)
set(FUZZER_DEPENDENCIES_IMAPParser LibIMAP)
set(FUZZER_DEPENDENCIES_JBIG2Loader LibGfx)
set(FUZZER_DEPENDENCIES_JPEGLoader LibGfx)
set(FUZZER_DEPENDENCIES_Js LibJS)
set(FUZZER_DEPENDENCIES_LzmaDecompression LibArchive LibCompress)
set(FUZZER_DEPENDENCIES_LzmaRoundtrip LibCompress)
set(FUZZER_DEPENDENCIES_Markdown LibMarkdown)
set(FUZZER_DEPENDENCIES_MatroskaReader LibVideo)
set(FUZZER_DEPENDENCIES_MD5 LibCrypto)
set(FUZZER_DEPENDENCIES_MP3Loader LibAudio)
set(FUZZER_DEPENDENCIES_PAMLoader LibGfx)
set(FUZZER_DEPENDENCIES_PBMLoader LibGfx)
set(FUZZER_DEPENDENCIES_PDF LibPDF)
set(FUZZER_DEPENDENCIES_PEM LibCrypto)
set(FUZZER_DEPENDENCIES_PGMLoader LibGfx)
set(FUZZER_DEPENDENCIES_PNGLoader LibGfx)
set(FUZZER_DEPENDENCIES_Poly1305 LibCrypto)
set(FUZZER_DEPENDENCIES_PPMLoader LibGfx)
set(FUZZER_DEPENDENCIES_QOALoader LibAudio)
set(FUZZER_DEPENDENCIES_QOILoader LibGfx)
set(FUZZER_DEPENDENCIES_QuotedPrintableParser LibIMAP)
set(FUZZER_DEPENDENCIES_RegexECMA262 LibRegex)
set(FUZZER_DEPENDENCIES_RegexPosixBasic LibRegex)
set(FUZZER_DEPENDENCIES_RegexPosixExtended LibRegex)
set(FUZZER_DEPENDENCIES_RSAKeyParsing LibCrypto)
set(FUZZER_DEPENDENCIES_SHA1 LibCrypto)
set(FUZZER_DEPENDENCIES_SHA256 LibCrypto)
set(FUZZER_DEPENDENCIES_SHA384 LibCrypto)
set(FUZZER_DEPENDENCIES_SHA512 LibCrypto)
set(FUZZER_DEPENDENCIES_Shell LibShell)
set(FUZZER_DEPENDENCIES_ShellPosix LibShell)
set(FUZZER_DEPENDENCIES_SQLParser LibSQL)
set(FUZZER_DEPENDENCIES_Tar LibArchive)
set(FUZZER_DEPENDENCIES_TextDecoder LibTextCodec)
set(FUZZER_DEPENDENCIES_TGALoader LibGfx)
set(FUZZER_DEPENDENCIES_TIFFLoader LibGfx)
set(FUZZER_DEPENDENCIES_TTF LibGfx)
set(FUZZER_DEPENDENCIES_TinyVGLoader LibGfx)
set(FUZZER_DEPENDENCIES_URL LibURL)
set(FUZZER_DEPENDENCIES_VP9Decoder LibVideo)
set(FUZZER_DEPENDENCIES_WasmParser LibWasm)
set(FUZZER_DEPENDENCIES_WAVLoader LibAudio)
set(FUZZER_DEPENDENCIES_WebPLoader LibGfx)
set(FUZZER_DEPENDENCIES_WOFF LibGfx)
set(FUZZER_DEPENDENCIES_WOFF2 LibGfx)
set(FUZZER_DEPENDENCIES_XML LibXML)
set(FUZZER_DEPENDENCIES_Zip LibArchive)
set(FUZZER_DEPENDENCIES_ZlibDecompression LibCompress)
