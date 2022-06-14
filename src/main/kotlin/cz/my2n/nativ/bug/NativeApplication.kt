package cz.my2n.nativ.bug

import mu.KotlinLogging
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.Repository
import org.springframework.nativex.hint.TypeHint
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.*
import software.amazon.awssdk.services.sts.internal.StsWebIdentityCredentialsProviderFactory
import java.util.*

private val logger = KotlinLogging.logger {}

@SpringBootApplication
@EnableConfigurationProperties(ReportProperties::class)
@TypeHint(types = [StsWebIdentityCredentialsProviderFactory::class])
class NativeApplication {

    @Bean
    fun appRunner(repository: DataRepository, templateEngine: SpringTemplateEngine, reportProperties: ReportProperties): ApplicationRunner {
        return ApplicationRunner {
            val environment = System.getenv("ENVIRONMENT_NAME") ?: "unknown"
            logger.info { "In environment $environment" }
            logger.info { "Fetching data from database" }
            val data = repository.getReportData()

            val context = mapOf(
                "environment" to environment,
                "data" to data
            )
            logger.info { "Rendering template" }
            val renderedTemplate = templateEngine.process("report", Context(Locale.ENGLISH, context))

            logger.info { "Sending email" }
            val sesClient = SesClient.create()
            sesClient.sendEmail(
                SendEmailRequest
                    .builder()
                    .destination(
                        Destination
                            .builder()
                            .toAddresses(reportProperties.email.to)
                            .build()
                    )
                    .source(reportProperties.email.from)
                    .message(
                        Message
                            .builder()
                            .body(
                                Body
                                    .builder()
                                    .html(Content.builder().data(renderedTemplate).build())
                                    .build()
                            )
                            .subject(Content.builder().data("Automatic report from $environment").build())
                            .build()
                    )
                    .build()
            )
        }
    }
}

interface DataRepository : Repository<ReportData, Long> {

    @Query("select * from foo")
    fun getReportData(): List<ReportData>

}

data class ReportData(
    val column1: String,
    val column2: String
)

@ConfigurationProperties("report")
@ConstructorBinding
data class ReportProperties(
    val email: Email
) {

    data class Email(
        val to: List<String>,
        val from: String
    )

}

fun main(args: Array<String>) {
    runApplication<NativeApplication>(*args)
}
