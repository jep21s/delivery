package microarch.delivery.adapters.`in`.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import microarch.delivery.application.commands.AssignOrderCommand
import microarch.delivery.application.commands.AssignOrderCommandHandler
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "app.scheduler.assign-order", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@DisallowConcurrentExecution
class AssignOrderJob(
    private val handler: AssignOrderCommandHandler,
) : Job {
    override fun execute(context: JobExecutionContext) {
        val result = handler.handle(AssignOrderCommand.create().getOrNull()!!)
        result.fold(
            ifLeft = { err ->
                log.warn { "AssignOrder failed: code=${err.code}, message=${err.message}" }
            },
            ifRight = { log.debug { "AssignOrder tick ok" } },
        )
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}
