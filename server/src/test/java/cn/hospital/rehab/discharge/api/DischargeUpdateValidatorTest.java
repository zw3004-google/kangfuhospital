package cn.hospital.rehab.discharge.api;

import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DischargeUpdateValidatorTest {
 @Test void specialPatientRequiresReason(){var current=mock(DischargeSummary.class);var request=new UpdateDischargeRequest(null,true," ",null,null,null,null,null,null,null,null,null,null,null);assertThatThrownBy(()->DischargeUpdateValidator.validateBefore(request,current)).hasMessage("特殊患者必须填写特殊原因");}
 @Test void dischargedPatientCannotChangePlan(){var current=mock(DischargeSummary.class);when(current.actualDischargeAt()).thenReturn(OffsetDateTime.parse("2026-09-03T08:00:00+08:00"));when(current.plannedDischargeAt()).thenReturn(OffsetDateTime.parse("2026-09-02T08:00:00+08:00"));var request=new UpdateDischargeRequest("2026-09-04T08:00:00+08:00",false,null,null,null,null,null,null,null,null,null,null,null,null);assertThatThrownBy(()->DischargeUpdateValidator.validateBefore(request,current)).hasMessage("患者出院后不可修改预计出院时间");}
 @Test void dischargedPatientMaySubmitUnchangedPlan(){var current=mock(DischargeSummary.class);var plan=OffsetDateTime.parse("2026-09-02T08:00:00+08:00");when(current.actualDischargeAt()).thenReturn(OffsetDateTime.parse("2026-09-03T08:00:00+08:00"));when(current.plannedDischargeAt()).thenReturn(plan);var request=new UpdateDischargeRequest(plan.toString(),false,null,null,null,null,null,null,null,null,null,null,null,null);assertThatCode(()->DischargeUpdateValidator.validateBefore(request,current)).doesNotThrowAnyException();}
 @Test void abnormalPatientRequiresDoctorReason(){var updated=mock(DischargeSummary.class);when(updated.abnormalCodes()).thenReturn(List.of("DATE_MISMATCH"));when(updated.abnormalReason()).thenReturn("");assertThatThrownBy(()->DischargeUpdateValidator.validateAfter(updated)).hasMessage("异常患者必须填写异常原因");}
}
