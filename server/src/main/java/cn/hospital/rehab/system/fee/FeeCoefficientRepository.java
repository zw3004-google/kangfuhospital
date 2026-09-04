package cn.hospital.rehab.system.fee;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class FeeCoefficientRepository {
    private static final String SELECT = "SELECT c.*,t.fee_code,t.fee_name FROM sys_fee_coefficient c JOIN sys_fee_type t ON t.id=c.fee_type_id";
    private final JdbcClient jdbcClient;
    public FeeCoefficientRepository(JdbcClient jdbcClient) { this.jdbcClient=jdbcClient; }

    public List<FeeCoefficient> findAll(String feeCode,String feeType,Boolean enabled) {
        String code=feeCode==null?"":feeCode.trim(),name=feeType==null?"":feeType.trim(); int status=enabled==null?2:enabled?1:0;
        return jdbcClient.sql(SELECT+" WHERE (:code='' OR t.fee_code ILIKE :codePattern) AND (:name='' OR t.fee_name ILIKE :namePattern) AND (:status=2 OR c.enabled=(:status=1)) ORDER BY t.fee_code,c.created_at DESC")
                .param("code",code).param("codePattern",code+"%").param("name",name).param("namePattern","%"+name+"%").param("status",status).query(FeeCoefficientRepository::mapRow).list();
    }
    public Optional<FeeCoefficient> findById(long id) { return jdbcClient.sql(SELECT+" WHERE c.id=:id").param("id",id).query(FeeCoefficientRepository::mapRow).optional(); }

    public long findOrCreateFeeType(String code,String name) {
        var existing=jdbcClient.sql("SELECT id,fee_name FROM sys_fee_type WHERE UPPER(fee_code)=UPPER(:code)").param("code",code).query((r,n)->new FeeType(r.getLong("id"),r.getString("fee_name"))).optional();
        if(existing.isPresent()){if(!existing.get().name().equals(name))throw new IllegalArgumentException("费别编码已用于其他费别名称");return existing.get().id();}
        if(jdbcClient.sql("SELECT EXISTS(SELECT 1 FROM sys_fee_type WHERE BTRIM(fee_name)=:name)").param("name",name).query(Boolean.class).single())throw new IllegalArgumentException("费别名称已存在，请从该费别新增系数版本");
        try{return jdbcClient.sql("INSERT INTO sys_fee_type(fee_code,fee_name) VALUES(:code,:name) RETURNING id").param("code",code).param("name",name).query(Long.class).single();}
        catch(DuplicateKeyException e){throw new IllegalArgumentException("费别编码或名称已存在",e);}
    }
    public FeeCoefficient insert(long feeTypeId,String feeType,BigDecimal coefficient,String operator){
        long id=jdbcClient.sql("INSERT INTO sys_fee_coefficient(fee_type_id,fee_type,coefficient,created_by_name) VALUES(:typeId,:name,:coefficient,:operator) RETURNING id").param("typeId",feeTypeId).param("name",feeType).param("coefficient",coefficient).param("operator",operator).query(Long.class).single();return findById(id).orElseThrow();
    }
    public void disableEnabledVersion(long feeTypeId,String operator){jdbcClient.sql("UPDATE sys_fee_coefficient SET enabled=FALSE,disabled_at=CURRENT_TIMESTAMP,disabled_by_name=:operator WHERE fee_type_id=:typeId AND enabled=TRUE").param("typeId",feeTypeId).param("operator",operator).update();}
    public FeeCoefficient enable(long id,String operator){jdbcClient.sql("UPDATE sys_fee_coefficient SET enabled=TRUE,effective_at=CURRENT_TIMESTAMP,disabled_at=NULL,enabled_by_name=:operator,disabled_by_name=NULL WHERE id=:id").param("id",id).param("operator",operator).update();return findById(id).orElseThrow();}
    public FeeCoefficient disable(long id,String operator){jdbcClient.sql("UPDATE sys_fee_coefficient SET enabled=FALSE,disabled_at=CURRENT_TIMESTAMP,disabled_by_name=:operator WHERE id=:id").param("id",id).param("operator",operator).update();return findById(id).orElseThrow();}
    private static FeeCoefficient mapRow(ResultSet r,int n)throws SQLException{return new FeeCoefficient(r.getLong("id"),r.getLong("fee_type_id"),r.getString("fee_code"),r.getString("fee_name"),r.getBigDecimal("coefficient"),r.getBoolean("enabled"),r.getObject("effective_at",java.time.OffsetDateTime.class),r.getObject("disabled_at",java.time.OffsetDateTime.class),r.getObject("created_at",java.time.OffsetDateTime.class),r.getString("created_by_name"),r.getString("enabled_by_name"),r.getString("disabled_by_name"));}
    private record FeeType(long id,String name){}
}
