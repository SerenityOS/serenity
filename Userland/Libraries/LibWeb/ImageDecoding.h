/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibImageDecoderClient/Client.h>

namespace Web {

ImageDecoderClient::Client& image_decoder_client();

}
