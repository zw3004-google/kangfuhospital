package cn.hospital.rehab.discharge.domain;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

public final class DischargeAbnormalCalculator {
    private DischargeAbnormalCalculator() {}

    public static List<String> calculate(OffsetDateTime planned, OffsetDateTime actual, OffsetDateTime plannedUpdated) {
        if (actual == null) return List.of();
        List<String> codes = new ArrayList<>();
        if (planned == null) codes.add("MISSING_PLAN");
        else {
            LocalDate actualDate = actual.toLocalDate();
            if (!planned.toLocalDate().equals(actualDate)) codes.add("DATE_MISMATCH");
            OffsetDateTime deadline = actualDate.plusDays(1).atStartOfDay(actual.getOffset()).toOffsetDateTime().minusHours(12);
            if (plannedUpdated != null && !plannedUpdated.isBefore(deadline)) codes.add("LATE_PLAN");
        }
        return List.copyOf(codes);
    }
}
