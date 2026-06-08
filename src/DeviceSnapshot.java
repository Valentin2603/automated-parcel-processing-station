public record DeviceSnapshot(
        boolean isWorking,
        int direction,
        int number,
        int priority,
        double remainingTime
) {
}
