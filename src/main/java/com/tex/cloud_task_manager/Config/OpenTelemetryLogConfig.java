package com.tex.cloud_task_manager.Config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryLogConfig {

  public OpenTelemetryLogConfig(OpenTelemetry openTelemetry) {
    OpenTelemetryAppender.install(openTelemetry);
  }
}
