package id.belajarbersama.application.platform;

public record ComponentStatus(String status, String provider, String detail) {
    public static ComponentStatus up(String provider) {
        return new ComponentStatus("UP", provider, null);
    }

    public static ComponentStatus down(String provider, String detail) {
        return new ComponentStatus("DOWN", provider, detail);
    }
}
