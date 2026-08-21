package com.medicine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medicine.entity.PersonalTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface PersonalTransactionMapper extends BaseMapper<PersonalTransaction> {
    @Select("SELECT COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE -amount END), 0) " +
            "FROM personal_transaction WHERE owner_user_id = #{userId} AND ledger_id = #{ledgerId} AND deleted = 0")
    BigDecimal selectHistoricalNet(@Param("userId") Long userId, @Param("ledgerId") Long ledgerId);

    @Select("SELECT category_name AS name, COUNT(*) AS useCount, MAX(transaction_time) AS lastUsedTime " +
            "FROM personal_transaction WHERE owner_user_id = #{userId} AND ledger_id = #{ledgerId} " +
            "AND transaction_type = #{transactionType} AND deleted = 0 " +
            "GROUP BY category_name ORDER BY useCount DESC, lastUsedTime DESC LIMIT #{limit}")
    List<Map<String, Object>> selectFrequentCategories(@Param("userId") Long userId,
                                                        @Param("ledgerId") Long ledgerId,
                                                        @Param("transactionType") String transactionType,
                                                        @Param("limit") int limit);

    @Select("SELECT counterparty AS name, COUNT(*) AS useCount, MAX(transaction_time) AS lastUsedTime " +
            "FROM personal_transaction WHERE owner_user_id = #{userId} AND ledger_id = #{ledgerId} " +
            "AND transaction_type = 'EXPENSE' AND counterparty IS NOT NULL AND counterparty <> '' AND deleted = 0 " +
            "GROUP BY counterparty ORDER BY useCount DESC, lastUsedTime DESC LIMIT #{limit}")
    List<Map<String, Object>> selectFrequentCounterparties(@Param("userId") Long userId,
                                                            @Param("ledgerId") Long ledgerId,
                                                            @Param("limit") int limit);
}
