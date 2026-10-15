package com.watchdoc.prototype.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.watchdoc.prototype.model.*
import kotlinx.coroutines.*
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RepositoryPersistenceTest {
    @Test fun settingsAndObservedAttemptsSurviveClosingAndReopeningStore() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().context
        val file = File(context.cacheDir, "watchdoc-${UUID.randomUUID()}.preferences_pb")
        val state = PrototypeState(selectedSection = 3, bodyBudget = 150, draftBudget = 150, nextNotificationId = 10008,
            metadata = TestSessionMetadata(phoneModel = "Measured phone", watchFirmwareVersion = "Measured firmware"),
            records = listOf(TestRecord("persisted-attempt", 123, 10007, "Lab", "Title", "[01] test |END|", 15, 15,
                "hash", deliveryMode = DeliveryMode.UNIQUE_ID, bodyFormat = BodyFormat.STANDARD, updateRequested = false,
                status = PostStatus.POSTED, phoneLockedAtAttempt = true,
                observation = WatchObservation(seen = Seen.YES, endMarker = Answer.YES, notes = "Manual observation"))))
        var job = SupervisorJob()
        try {
            val first = DataStorePrototypeRepository(PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file })
            first.save(state)
            job.cancelAndJoin()
            job = SupervisorJob()
            val reopened = DataStorePrototypeRepository(PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file })
            assertEquals(state, reopened.load())
        } finally {
            job.cancelAndJoin()
            file.delete()
        }
    }
}
