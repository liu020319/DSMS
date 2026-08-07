package com.medicine.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medicine.entity.FamilyFundTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.math.BigDecimal;
@Mapper
public interface FamilyFundTransactionMapper extends BaseMapper<FamilyFundTransaction> {
    @Select("SELECT COALESCE(SUM(amount),0) FROM family_fund_transaction WHERE elder_id=#{elderId} AND deleted=0")
    BigDecimal selectBalance(Long elderId);
}
