package cz.my2n.nativ.bug

import mu.KotlinLogging
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.nativex.hint.TypeHint
import software.amazon.awssdk.services.sts.internal.StsWebIdentityCredentialsProviderFactory

private val logger = KotlinLogging.logger {}

@SpringBootApplication
@TypeHint(types = [StsWebIdentityCredentialsProviderFactory::class])
class NativeApplication {

    @Bean
    fun appRunner(): ApplicationRunner {
        return ApplicationRunner {
            logger.info { "Hello" }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<NativeApplication>(*args)
}
