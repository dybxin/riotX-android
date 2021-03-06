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

package im.vector.riotx.features.roomprofile.members

import androidx.annotation.StringRes
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import im.vector.matrix.android.api.session.room.model.RoomMemberSummary
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotx.R
import im.vector.riotx.features.roomprofile.RoomProfileArgs

data class RoomMemberListViewState(
        val roomId: String,
        val roomSummary: Async<RoomSummary> = Uninitialized,
        val roomMemberSummaries: Async<RoomMemberSummaries> = Uninitialized
) : MvRxState {

    constructor(args: RoomProfileArgs) : this(roomId = args.roomId)
}

typealias RoomMemberSummaries = List<Pair<PowerLevelCategory, List<RoomMemberSummary>>>

enum class PowerLevelCategory(@StringRes val titleRes: Int) {
    ADMIN(R.string.room_member_power_level_admins),
    MODERATOR(R.string.room_member_power_level_moderators),
    CUSTOM(R.string.room_member_power_level_custom),
    INVITE(R.string.room_member_power_level_invites),
    USER(R.string.room_member_power_level_users)
}
