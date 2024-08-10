/*
 * Copyright (c) 2024, Logkos <65683493+logkos@users.noreply.github.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel {

// https://trustedcomputinggroup.org/wp-content/uploads/TCG_PCClientTPMInterfaceSpecification_TIS__1-3_27_03212013.pdf
struct [[gnu::packed]] TPM12_STS {
    u64 reserved1 : 1;
    u64 responseRetry : 1;
    u64 selfTestDone : 1;
    u64 Expect : 1;
    u64 dataAvail : 1;
    u64 tpmGo : 1;
    u64 commandReady : 1;
    u64 stsValid : 1;
    u16 burstCount;
};

struct [[gnu::packed]] TPM12MMIORegistersLocality0 {
    u8 TPM_ACCESS_0;
    u32 TPM_INT_ENABLE_0;
    u32 TPM_INT_STATUS_0;
    u32 TPM_INTF_CAPABILITY_0;
    TPM12_STS TPM_STS_0;
    u8 TPM_DATA_FIFO_0;
    u8 TPM_XDATA_FIFO_0;
    u8 reserved1;
    u32 TPM_DID_VID_0;
    u8 TPM_RID_x_0;
};

struct [[gnu::packed]] TPMCommandHeader {
    u16 tag;
    u32 commandSize;
    u32 commandCode;
};

struct [[gnu::packed]] TPMPower {
    TPMCommandHeader header;
    u16 actionType;
};
}
