package id.familyguard.boot

import android.content.Intent
import org.junit.Assert.*
import org.junit.Test

class BootCompletedReceiverTest {

    @Test
    fun testBootActionsConstants_MatchStandardAndroidBroadcasting() {
        assertEquals("android.intent.action.QUICKBOOT_POWERON", BootCompletedReceiver.ACTION_QUICKBOOT_POWERON)
        assertEquals("com.htc.intent.action.QUICKBOOT_POWERON", BootCompletedReceiver.ACTION_HTC_QUICKBOOT)
    }

    @Test
    fun testIsHandledAction_RecognizesAllManufacturerBootActions() {
        val validActions = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            BootCompletedReceiver.ACTION_QUICKBOOT_POWERON,
            BootCompletedReceiver.ACTION_HTC_QUICKBOOT
        )

        for (action in validActions) {
            assertTrue(isValidBootAction(action))
        }

        val invalidActions = listOf(
            Intent.ACTION_SCREEN_ON,
            Intent.ACTION_POWER_CONNECTED,
            "random.custom.action"
        )

        for (action in invalidActions) {
            assertFalse(isValidBootAction(action))
        }
    }

    private fun isValidBootAction(action: String?): Boolean {
        return action == Intent.ACTION_BOOT_COMPLETED ||
                action == BootCompletedReceiver.ACTION_QUICKBOOT_POWERON ||
                action == BootCompletedReceiver.ACTION_HTC_QUICKBOOT
    }
}
