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

package im.vector.matrix.android.internal.database.helper

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.mapper.toEntity
import im.vector.matrix.android.internal.database.model.*
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.database.query.getOrCreate
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.extensions.assertIsManaged
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import io.realm.Sort
import io.realm.kotlin.createObject

internal fun ChunkEntity.deleteOnCascade() {
    assertIsManaged()
    this.timelineEvents.deleteAllFromRealm()
    this.deleteFromRealm()
}

internal fun ChunkEntity.merge(roomId: String,
                               chunkToMerge: ChunkEntity,
                               direction: PaginationDirection): List<TimelineEventEntity> {
    assertIsManaged()
    val isChunkToMergeUnlinked = chunkToMerge.isUnlinked
    val isCurrentChunkUnlinked = isUnlinked

    if (isCurrentChunkUnlinked && !isChunkToMergeUnlinked) {
        this.timelineEvents.forEach { it.root?.isUnlinked = false }
    }
    val eventsToMerge: List<TimelineEventEntity>
    if (direction == PaginationDirection.FORWARDS) {
        this.nextToken = chunkToMerge.nextToken
        this.isLastForward = chunkToMerge.isLastForward
        eventsToMerge = chunkToMerge.timelineEvents.sort(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, Sort.ASCENDING)
    } else {
        this.prevToken = chunkToMerge.prevToken
        this.isLastBackward = chunkToMerge.isLastBackward
        eventsToMerge = chunkToMerge.timelineEvents.sort(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, Sort.DESCENDING)
    }
    return eventsToMerge
            .mapNotNull {
                val event = it.root?.asDomain() ?: return@mapNotNull null
                add(roomId, event, direction)
            }
}

internal fun ChunkEntity.add(roomId: String,
                             event: Event,
                             direction: PaginationDirection,
                             stateIndexOffset: Int = 0
): TimelineEventEntity? {
    assertIsManaged()
    if (event.eventId != null && timelineEvents.find(event.eventId) != null) {
        return null
    }
    var currentDisplayIndex = lastDisplayIndex(direction, 0)
    if (direction == PaginationDirection.FORWARDS) {
        currentDisplayIndex += 1
        forwardsDisplayIndex = currentDisplayIndex
    } else {
        currentDisplayIndex -= 1
        backwardsDisplayIndex = currentDisplayIndex
    }
    var currentStateIndex = lastStateIndex(direction, defaultValue = stateIndexOffset)
    if (direction == PaginationDirection.FORWARDS && EventType.isStateEvent(event.type)) {
        currentStateIndex += 1
        forwardsStateIndex = currentStateIndex
    } else if (direction == PaginationDirection.BACKWARDS && timelineEvents.isNotEmpty()) {
        val lastEventType = timelineEvents.last()?.root?.type ?: ""
        if (EventType.isStateEvent(lastEventType)) {
            currentStateIndex -= 1
            backwardsStateIndex = currentStateIndex
        }
    }

    val isChunkUnlinked = isUnlinked
    val localId = TimelineEventEntity.nextId(realm)
    val eventId = event.eventId ?: ""
    val senderId = event.senderId ?: ""

    val readReceiptsSummaryEntity = ReadReceiptsSummaryEntity.where(realm, eventId).findFirst()
            ?: realm.createObject<ReadReceiptsSummaryEntity>(eventId).apply {
                this.roomId = roomId
            }

    // Update RR for the sender of a new message with a dummy one

    if (event.originServerTs != null) {
        val timestampOfEvent = event.originServerTs.toDouble()
        val readReceiptOfSender = ReadReceiptEntity.getOrCreate(realm, roomId = roomId, userId = senderId)
        // If the synced RR is older, update
        if (timestampOfEvent > readReceiptOfSender.originServerTs) {
            val previousReceiptsSummary = ReadReceiptsSummaryEntity.where(realm, eventId = readReceiptOfSender.eventId).findFirst()
            readReceiptOfSender.eventId = eventId
            readReceiptOfSender.originServerTs = timestampOfEvent
            previousReceiptsSummary?.readReceipts?.remove(readReceiptOfSender)
            readReceiptsSummaryEntity.readReceipts.add(readReceiptOfSender)
        }
    }

    val rootEvent = event.toEntity(roomId).apply {
        this.stateIndex = currentStateIndex
        this.displayIndex = currentDisplayIndex
        this.sendState = SendState.SYNCED
        this.isUnlinked = isChunkUnlinked
    }
    val eventEntity = realm.createObject<TimelineEventEntity>().also {
        it.localId = localId
        it.root = realm.copyToRealm(rootEvent)
        it.eventId = eventId
        it.roomId = roomId
        it.annotations = EventAnnotationsSummaryEntity.where(realm, eventId).findFirst()
        it.readReceipts = readReceiptsSummaryEntity
    }
    val position = if (direction == PaginationDirection.FORWARDS) 0 else this.timelineEvents.size
    timelineEvents.add(position, eventEntity)
    return eventEntity
}

internal fun ChunkEntity.lastDisplayIndex(direction: PaginationDirection, defaultValue: Int = 0): Int {
    return when (direction) {
        PaginationDirection.FORWARDS  -> forwardsDisplayIndex
        PaginationDirection.BACKWARDS -> backwardsDisplayIndex
    } ?: defaultValue
}

internal fun ChunkEntity.lastStateIndex(direction: PaginationDirection, defaultValue: Int = 0): Int {
    return when (direction) {
        PaginationDirection.FORWARDS  -> forwardsStateIndex
        PaginationDirection.BACKWARDS -> backwardsStateIndex
    } ?: defaultValue
}
