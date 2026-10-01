package cn.parentchess

import org.junit.Assert.*
import org.junit.Test

class LessonSpeechTest {
    @Test fun readsCoordinatesInChineseWithoutJoiningNumbers() {
        assertEquals("用E 四的兵吃掉F 五的马。", LessonSpeechText.normalize("用E4的兵吃掉F5的马。"))
        assertEquals("A 一，走到，H 八", LessonSpeechText.normalize("a1→h8"))
        assertEquals("第 100 关，白方两步江杀。", LessonSpeechText.normalize("第 100 关，白方两步将杀。"))
    }
    @Test fun chessTermsUseFirstToneWithoutChangingOtherUsesOfJiang() {
        val visible = "黑王被将军。白方一步将杀，将来再练。"
        assertEquals("黑王被江军。白方一步江杀，将来再练。", LessonSpeechText.normalize(visible))
        assertEquals("黑王被将军。白方一步将杀，将来再练。", visible)
    }
    @Test fun newestLessonReplacesSpeechWaitingForEngine() {
        val queue = NarrationQueue()
        queue.replace("旧关卡")
        queue.replace("新关卡")
        val request = queue.take()!!
        assertEquals("新关卡", request.second)
        assertTrue(queue.isCurrent(request.first.toString()))
        assertNull(queue.take())
    }
    @Test fun everyRookMentionUsesJuIncludingHintsAndCelebration() {
        assertEquals("白居从 E 四到 F 四，吃掉黑居。双居配合江杀！",
            LessonSpeechText.normalize("白车从 E4到 F4，吃掉黑车。双车配合将杀！"))
        assertEquals("居的一步江杀", LessonSpeechText.normalize("车的一步将杀"))
    }
    @Test fun leavingOrMutingCancelsPendingSpeechAndLateCallbacks() {
        val queue = NarrationQueue()
        queue.replace("挑战成功")
        val id = queue.generation.toString()
        queue.clear()
        assertNull(queue.take())
        assertFalse(queue.isCurrent(id))
        queue.replace("下一关")
        assertFalse(queue.isCurrent(id))
    }
}
