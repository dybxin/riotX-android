/*
 * Copyright 2018 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.crypto.keysbackup.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.internal.di.MoshiProvider

/**
 * Data model for [org.matrix.androidsdk.rest.model.keys.KeysAlgorithmAndData.authData] in case
 * of [org.matrix.androidsdk.crypto.MXCRYPTO_ALGORITHM_MEGOLM_BACKUP].
 */
@JsonClass(generateAdapter = true)
data class MegolmBackupAuthData(
        /**
         * The curve25519 public key used to encrypt the backups.
         */
        @Json(name = "public_key")
        var publicKey: String = "",

        /**
         * In case of a backup created from a password, the salt associated with the backup
         * private key.
         */
        @Json(name = "private_key_salt")
        var privateKeySalt: String? = null,

        /**
         * In case of a backup created from a password, the number of key derivations.
         */
        @Json(name = "private_key_iterations")
        var privateKeyIterations: Int? = null,

        /**
         * Signatures of the public key.
         * userId -> (deviceSignKeyId -> signature)
         */
        var signatures: Map<String, Map<String, String>>? = null
) {

    fun toJsonString(): String {
        return MoshiProvider.providesMoshi()
                .adapter(MegolmBackupAuthData::class.java)
                .toJson(this)
    }

    /**
     * Same as the parent [MXJSONModel JSONDictionary] but return only
     * data that must be signed.
     */
    fun signalableJSONDictionary(): Map<String, Any> = HashMap<String, Any>().apply {
        put("public_key", publicKey)

        privateKeySalt?.let {
            put("private_key_salt", it)
        }
        privateKeyIterations?.let {
            put("private_key_iterations", it)
        }
    }
}
