package id.belajarbersama.application.platform;

import java.util.Map;

public record PlatformStatus(
        String service, String version, String status, Map<String, ComponentStatus> components) {}
