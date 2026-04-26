package example.grpcclient;

import io.grpc.stub.StreamObserver;
import service.*;


public class ConverterImpl extends ConverterGrpc.ConverterImplBase {

    @Override
    public void convert(ConversionRequest request, StreamObserver<ConversionResponse> responseObserver) {
        double value = request.getValue();
        String from = request.getFromUnit();
        String to = request.getToUnit();

        //validate inputs
        if (from == null || from.isEmpty()) {
            responseObserver.onNext(error("from_unit can't be empty"));
            responseObserver.onCompleted();
            return;

        }
        if (to == null || to.isEmpty()) {
            responseObserver.onNext(error("to_unit can't be empty"));
            responseObserver.onCompleted();
            return;
        }
        if (from.equals(to)) {
            responseObserver.onNext(error("Same unit. Nothing to convert"));
            responseObserver.onCompleted();
            return;
        }
        //in case user enters units in  lowercase
        from = from.toUpperCase();
        to = to.toUpperCase();

        //validate matching type
        if ((isLength(from) && !isLength(to)) ||
                (isWeight(from) && !isWeight(to)) ||
                (isTemperature(from) && !isTemperature(to))) {
            responseObserver.onNext(error("Units don't match! Can't convert from " + from + " to " + to));
            responseObserver.onCompleted();
        }

        try {
            double result;

            if (isLength(from)) {
                double meters = lengthToMeters(value, from);
                result = lengthFromMeters(meters, to);
            } else if (isWeight(from)) {
                result = convertWeight(value, from, to);
            } else if (isTemperature(from)) {
                result = convertTemperature(value, from, to);
            } else {
                responseObserver.onNext(error("Invalid unit: " + from));
                responseObserver.onCompleted();
                return;
            }

            responseObserver.onNext(ConversionResponse.newBuilder()
                    .setIsSuccess(true)
                    .setResult(Math.round(result * 100.0) / 100.0) // 2 decimals places
                    .build()
            );
        } catch (Exception e) {
            responseObserver.onNext(error(e.getMessage()));
        }
        
        responseObserver.onCompleted();
    }

    //Conversion functions

    private double convertWeight(double value, String from, String to) {
        if (from.equals("KILOGRAM") && to.equals("POUND")) {
            return value * 2.20462;
        }
        if (from.equals("POUND") && to.equals("KILOGRAM")) {
            return value / 2.20462;
        }
        throw new IllegalArgumentException("Unsupported unit: " + from);
    }
    private double convertTemperature(double value, String from, String to) {
        //Absolute zero check
        if (from.equals("CELSIUS") && value < -273.15) {
            throw new IllegalArgumentException("Temperature is below absolute zero");
        }
        if (from.equals("FAHRENHEIT") && value < -459.67) {
            throw new IllegalArgumentException("Temperature is below absolute zero");
        }
        if (from.equals("CELSIUS") && to.equals("FAHRENHEIT")) {
            return (value * 9/5) +32;
        }
        if (from.equals("FAHRENHEIT") && to.equals("CELSIUS")) {
            return (value -32) * 5/9;
        }
        throw new IllegalArgumentException("Unsupported unit: " + from);
    }
    //length conversions using meters as base
    private double lengthToMeters(double value, String unit) {
        switch (unit) {
            case "KILOMETER": return value * 1000;
            case "MILE" : return value * 1609.34;
            case "YARD" : return value * 0.9144;
            case "FOOT" : return value * 0.3048;
            default : throw new IllegalArgumentException("Unsupported unit: " + unit);
        }
    }
    private double lengthFromMeters(double meters, String unit) {
        switch (unit) {
            case "KILOMETER": return meters / 1000;
            case "MILE": return meters / 1609.34;
            case "YARD": return meters / 0.9144;
            case "FOOT": return meters / 0.3048;
            default : throw new IllegalArgumentException("Unsupported unit: " + unit);
        }
    }

    //helper method for error messages
    private ConversionResponse error(String message) {
        return ConversionResponse.newBuilder()
                .setIsSuccess(false)
                .setError(message)
                .build();
    }
    //helper methods for unit types
    private boolean isLength(String unit) {
        return unit.equals("KILOMETER") || unit.equals("MILE") ||
                unit.equals("YARD") || unit.equals("FOOT");
    }
    private boolean isWeight(String unit) {
        return unit.equals("KILOGRAM") || unit.equals("POUND");
    }
    private boolean isTemperature(String unit) {
        return unit.equals("CELSIUS") || unit.equals("FAHRENHEIT");
    }
}
