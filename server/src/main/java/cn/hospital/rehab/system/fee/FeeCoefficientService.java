package cn.hospital.rehab.system.fee;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FeeCoefficientService {

    private final FeeCoefficientRepository repository;

    public FeeCoefficientService(FeeCoefficientRepository repository) {
        this.repository = repository;
    }

    public List<FeeCoefficient> list(String feeCode, String feeType, Boolean enabled) {
        return repository.findAll(feeCode, feeType, enabled);
    }

    @Transactional
    public FeeCoefficient create(CreateFeeCoefficientRequest request, String operator) {
        String code = request.feeCode().trim().toUpperCase();
        String name = request.feeType().trim();
        long feeTypeId = repository.findOrCreateFeeType(code, name);
        return repository.insert(feeTypeId, name, request.coefficient(), operator);
    }

    @Transactional
    public FeeCoefficient enable(long id, String operator) {
        FeeCoefficient target = requireExisting(id);
        if (target.enabled()) {
            return target;
        }
        repository.disableEnabledVersion(target.feeTypeId(), operator);
        return repository.enable(id, operator);
    }

    @Transactional
    public FeeCoefficient disable(long id, String operator) {
        FeeCoefficient target = requireExisting(id);
        return target.enabled() ? repository.disable(id, operator) : target;
    }

    private FeeCoefficient requireExisting(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("费别系数记录不存在"));
    }
}
