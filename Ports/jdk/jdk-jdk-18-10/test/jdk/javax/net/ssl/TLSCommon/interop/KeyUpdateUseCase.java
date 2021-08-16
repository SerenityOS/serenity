/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * The SSL/TLS communication parameters on TLS 1.3 key update.
 */
public class KeyUpdateUseCase extends UseCase {

    private KeyUpdateRequest keyUpdateRequest = KeyUpdateRequest.NOT_REQUESTED;

    public static KeyUpdateUseCase newInstance() {
        return new KeyUpdateUseCase();
    }

    public KeyUpdateRequest getKeyUpdateRequest() {
        return keyUpdateRequest;
    }

    public KeyUpdateUseCase setKeyUpdateRequest(
            KeyUpdateRequest keyUpdateRequest) {
        this.keyUpdateRequest = keyUpdateRequest;
        return this;
    }

    @Override
    public String toString() {
        return Utilities.join(Utilities.PARAM_DELIMITER,
                super.toString(),
                Utilities.joinNameValue("KeyUpdateRequest", keyUpdateRequest.name));
    }
}
