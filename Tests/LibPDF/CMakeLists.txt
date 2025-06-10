set(TEST_SOURCES
    BenchmarkPDF.cpp
    TestPDF.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibPDF LIBS LibCore LibGfx LibPDF)
endforeach()

set(TEST_FILES
    clip.pdf
    colorspaces.pdf
    complex.pdf
    encoding.pdf
    encryption_nocopy.pdf
    jbig2-globals.pdf
    jpeg2000-indexed-small.pdf
    linearized.pdf
    non-linearized.pdf
    offset.pdf
    oss-fuzz-testcase-62065.pdf
    password-is-sup.pdf
    paths.pdf
    pattern.pdf
    rotate.pdf
    shade-coons.pdf
    shade-function.pdf
    shade-gouraud-free.pdf
    shade-gouraud-lattice.pdf
    shade-radial.pdf
    shade-tensor.pdf
    standard-14-fonts.pdf
    text.pdf
    type1.pdf
    type3.pdf
    wide-gamut-only.pdf
)
install(FILES ${TEST_FILES} DESTINATION home/anon/Documents/pdf)
install(FILES ${TEST_FILES} DESTINATION usr/Tests/LibPDF)
