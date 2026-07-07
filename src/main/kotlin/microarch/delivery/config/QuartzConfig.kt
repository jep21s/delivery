package microarch.delivery.config

import microarch.delivery.adapters.`in`.scheduler.AssignOrderJob
import org.quartz.JobBuilder
import org.quartz.JobDetail
import org.quartz.SimpleScheduleBuilder
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "app.scheduler.assign-order", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class QuartzConfig {
    @Bean
    fun assignOrderJobDetail(): JobDetail =
        JobBuilder
            .newJob(AssignOrderJob::class.java)
            .withIdentity("assignOrderJob")
            .storeDurably()
            .build()

    @Bean
    fun assignOrderTrigger(assignOrderJobDetail: JobDetail): Trigger =
        TriggerBuilder
            .newTrigger()
            .forJob(assignOrderJobDetail)
            .withIdentity("assignOrderTrigger")
            .withSchedule(
                SimpleScheduleBuilder
                    .simpleSchedule()
                    .withIntervalInSeconds(1)
                    .repeatForever(),
            ).build()
}
