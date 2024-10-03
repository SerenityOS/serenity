// LittleEndianInputBitStreamEnhanced.cpp

#include "LittleEndianInputBitStream.h"  // Include the necessary header file

class LittleEndianInputBitStream {
public:
    // Existing members...

    // Method to check how many bits are left
    size_t bits_remaining() const {
        // Implementation to calculate the remaining bits in the stream
        // (You will need to define how to calculate this based on your existing implementation)
    }

    // Modified peek_bits method
    template<typename T>
    Result<T, ReadError> peek_bits(size_t count) {
        size_t available_bits = bits_remaining();
        size_t bits_to_peek = std::min(count, available_bits);

        // Original method to peek bits
        auto result = original_peek_bits(bits_to_peek);
        if (bits_to_peek < count) {
            result.value <<= (count - bits_to_peek); // Shift left to fill in zeros
        }

        return result;
    }

    // Placeholder for the original peek implementation
    Result<T, ReadError> original_peek_bits(size_t count) {
        // Original implementation for peeking bits
    }
};
