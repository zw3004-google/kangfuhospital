package cn.hospital.rehab.system.department;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {
    private final DepartmentRepository repository;

    public DepartmentService(DepartmentRepository repository) {
        this.repository = repository;
    }

    public List<Department> list(String keyword, Boolean enabled) {
        return repository.findAll(keyword, enabled);
    }

    @Transactional
    public Department create(CreateDepartmentRequest request) {
        return repository.insert(request.departmentCode().trim().toUpperCase(), request.departmentName().trim());
    }

    @Transactional
    public Department setEnabled(long id, boolean enabled) {
        Department department = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("科室不存在"));
        return department.enabled() == enabled ? department : repository.setEnabled(id, enabled);
    }
    @Transactional public void setEnabled(java.util.Set<Long> ids, boolean enabled) { for (Long id : ids) setEnabled(id, enabled); }
    @Transactional public void delete(long id) { repository.findById(id).orElseThrow(() -> new IllegalArgumentException("科室不存在")); repository.delete(id); }
    @Transactional public void delete(java.util.Set<Long> ids) { for (Long id : ids) delete(id); }
}
