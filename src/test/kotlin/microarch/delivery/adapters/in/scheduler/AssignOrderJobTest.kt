package microarch.delivery.adapters.`in`.scheduler

import arrow.core.left
import arrow.core.right
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import libs.errs.LogicError
import microarch.delivery.application.commands.AssignOrderCommand
import microarch.delivery.application.commands.AssignOrderCommandHandler
import org.junit.jupiter.api.Test
import org.quartz.JobExecutionContext

class AssignOrderJobTest {
    @Test
    fun `execute delegates to handler with AssignOrderCommand`() {
        val handler = mockk<AssignOrderCommandHandler>()
        every { handler.handle(any()) } returns Unit.right()

        AssignOrderJob(handler).execute(mockk<JobExecutionContext>(relaxed = true))

        verify(exactly = 1) { handler.handle(any<AssignOrderCommand>()) }
    }

    @Test
    fun `execute swallows Either Left without throwing`() {
        val handler = mockk<AssignOrderCommandHandler>()
        every { handler.handle(any()) } returns
            LogicError.of("test.code", "test message").left()

        AssignOrderJob(handler).execute(mockk<JobExecutionContext>(relaxed = true))

        verify(exactly = 1) { handler.handle(any<AssignOrderCommand>()) }
    }
}
