set(TEST_SOURCES
    TestPDF.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibPDF LIBS LibCore LibPDF)
endforeach()

install(
    FILES "../../Base/home/anon/Documents/pdf/complex.pdf"
          "../../Base/home/anon/Documents/pdf/linearized.pdf"
          "../../Base/home/anon/Documents/pdf/non-linearized.pdf"
    DESTINATION usr/Tests/LibPDF)
