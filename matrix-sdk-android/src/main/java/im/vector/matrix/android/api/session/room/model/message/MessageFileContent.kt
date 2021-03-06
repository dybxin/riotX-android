/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.api.session.room.model.message

import android.content.ClipDescription
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.api.session.room.model.relation.RelationDefaultContent
import im.vector.matrix.android.internal.crypto.model.rest.EncryptedFileInfo

@JsonClass(generateAdapter = true)
data class MessageFileContent(
        /**
         * Not documented
         */
        @Json(name = "msgtype") override val type: String,

        /**
         * Required. A human-readable description of the file. This is recommended to be the filename of the original upload.
         */
        @Json(name = "body") override val body: String,

        /**
         * The original filename of the uploaded file.
         */
        @Json(name = "filename") val filename: String? = null,

        /**
         * Information about the file referred to in url.
         */
        @Json(name = "info") val info: FileInfo? = null,

        /**
         * Required. Required if the file is unencrypted. The URL (typically MXC URI) to the file.
         */
        @Json(name = "url") override val url: String? = null,

        @Json(name = "m.relates_to") override val relatesTo: RelationDefaultContent? = null,
        @Json(name = "m.new_content") override val newContent: Content? = null,

        @Json(name = "file") override val encryptedFileInfo: EncryptedFileInfo? = null
) : MessageEncryptedContent {

    fun getMimeType(): String {
        // Mimetype default to plain text, should not be used
        return encryptedFileInfo?.mimetype
                ?: info?.mimeType
                ?: ClipDescription.MIMETYPE_TEXT_PLAIN
    }

    fun getFileName(): String {
        return filename ?: body
    }
}
