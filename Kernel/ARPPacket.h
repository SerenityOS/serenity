#pragma once

class [[gnu::packed]] ARPPacket {
public:
    word hardware_type;
	word protocol_type;
	word hardware_address_length;
	word protocol_address_length;
	word operation;
	uint8_t sender_hardware_address[6];        // Sender hardware address.
    uint8_t sender_protocol_address[4];        // Sender protocol address.
    uint8_t target_hardware_address[6];        // target hardware address.
    uint8_t target_protocol_address[4];        // target protocol address.
};
