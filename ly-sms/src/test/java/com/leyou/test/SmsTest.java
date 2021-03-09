package com.leyou.test;

import com.leyou.sms.LySmsApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LySmsApplication.class)
public class SmsTest {

    @Autowired
    private AmqpTemplate template;

    @Test
    public void testSend() throws InterruptedException {
        Map<String,String> msg = new HashMap<>();
        msg.put("phone", "13600527634");
        msg.put("code", "123");
        template.convertAndSend("ly.sms.exchange", "sms.verify.code", msg);

        Thread.sleep(1000l);
    }
}
