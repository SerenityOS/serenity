

set(AK_TEST_SOURCES
    TestAllOf.cpp
    TestAnyOf.cpp
    TestArray.cpp
    TestAtomic.cpp
    TestBadge.cpp
    TestBase64.cpp
    TestBinaryHeap.cpp
    TestBinarySearch.cpp
    TestBitmap.cpp
    TestByteBuffer.cpp
    TestChecked.cpp
    TestCircularDeque.cpp
    TestCircularDuplexStream.cpp
    TestCircularQueue.cpp
    TestDistinctNumeric.cpp
    TestDoublyLinkedList.cpp
    TestEndian.cpp
    TestEnumBits.cpp
    TestFind.cpp
    TestFormat.cpp
    TestHashFunctions.cpp
    TestHashMap.cpp
    TestIPv4Address.cpp
    TestIndexSequence.cpp
    TestJSON.cpp
    TestLexicalPath.cpp
    TestMACAddress.cpp
    TestMemMem.cpp
    TestMemoryStream.cpp
    TestNeverDestroyed.cpp
    TestNonnullRefPtr.cpp
    TestNumberFormat.cpp
    TestOptional.cpp
    TestQueue.cpp
    TestQuickSort.cpp
    TestRefPtr.cpp
    TestSinglyLinkedList.cpp
    TestSourceGenerator.cpp
    TestSpan.cpp
    TestString.cpp
    TestStringUtils.cpp
    TestStringView.cpp
    TestTime.cpp
    TestTrie.cpp
    TestTypeTraits.cpp
    TestTypedTransfer.cpp
    TestURL.cpp
    TestUtf8.cpp
    TestVector.cpp
    TestWeakPtr.cpp
)

foreach(source ${AK_TEST_SOURCES})
    get_filename_component(name ${source} NAME_WE)
    add_executable(${name} ${source})
    target_link_libraries(${name} LibCore)
    install(TARGETS ${name} RUNTIME DESTINATION usr/Tests/AK)
endforeach()
