package com.medicine.service;

import com.medicine.common.BusinessException;
import com.medicine.dto.HumanChallengeVO;
import com.medicine.dto.HumanVerifyVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HumanVerificationServiceTest {

    @Test
    void verificationTokenCanOnlyBeConsumedOnce() throws Exception {
        HumanVerificationService service = new HumanVerificationService();
        HumanChallengeVO challenge = service.createChallenge("127.0.0.1");

        Thread.sleep(650L);
        HumanVerifyVO verified = service.verify(challenge.getChallengeId(), "127.0.0.1");
        assertNotNull(verified.getHumanToken());

        service.consume(verified.getHumanToken(), "127.0.0.1");
        assertThrows(BusinessException.class,
                () -> service.consume(verified.getHumanToken(), "127.0.0.1"));
    }

    @Test
    void challengeCannotBeUsedFromAnotherAddress() throws Exception {
        HumanVerificationService service = new HumanVerificationService();
        HumanChallengeVO challenge = service.createChallenge("127.0.0.1");

        Thread.sleep(650L);
        assertThrows(BusinessException.class,
                () -> service.verify(challenge.getChallengeId(), "127.0.0.2"));
    }
}
