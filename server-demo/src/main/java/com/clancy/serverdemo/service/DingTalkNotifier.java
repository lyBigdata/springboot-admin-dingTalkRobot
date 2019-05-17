package com.clancy.serverdemo.service;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.dingtalk.api.response.OapiRobotSendResponse;
import com.taobao.api.ApiException;
import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.events.InstanceEvent;
import de.codecentric.boot.admin.server.domain.events.InstanceStatusChangedEvent;
import de.codecentric.boot.admin.server.notify.AbstractEventNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

import java.util.Arrays;

/**
 * @author liugang
 * @date 2019/5/16 18:34
 */
@Slf4j
public class DingTalkNotifier extends AbstractEventNotifier {


    @Value("${notify.dingTalk.webhook.token}")
    private String webhookToken;

    @Value("${notify.dingTalk.webhook.atMobiles}")
    private String[] atMobiles;

    private String dingTalkMessageFormat = "Instance %s (%s) is %s";

    protected DingTalkNotifier(InstanceRepository repository) {
        super(repository);
    }

    @Override
    protected Mono<Void> doNotify(InstanceEvent event, Instance instance) {
        return Mono.fromRunnable(() -> {
            String message;
            if (event instanceof InstanceStatusChangedEvent) {
                log.info("Instance {} ({}) is {}", instance.getRegistration().getName(), event.getInstance(),
                        ((InstanceStatusChangedEvent) event).getStatusInfo().getStatus());
                message = String.format(dingTalkMessageFormat, instance.getRegistration().getName(), event.getInstance(),
                        ((InstanceStatusChangedEvent) event).getStatusInfo().getStatus());

            } else {
                log.info("Instance {} ({}) {}", instance.getRegistration().getName(), event.getInstance(),
                        event.getType());
                message = String.format(dingTalkMessageFormat, instance.getRegistration().getName(), event.getInstance(),
                        event.getType());
            }
            try {
                sendDingTalkMessage(message);
            } catch (ApiException e) {
                log.error(e.getMessage());
            }
        });

    }

    /**
     * 钉钉官方例子
     * https://open-doc.dingtalk.com/microapp/serverapi2/qf2nxq
     * @param message 自定义消息内容
     * @throws ApiException
     */
    private void sendDingTalkMessage(String message) throws ApiException {
        String serverUrl =  "https://oapi.dingtalk.com/robot/send?access_token=" + webhookToken;
        DingTalkClient client = new DefaultDingTalkClient(serverUrl);
        OapiRobotSendRequest request = new OapiRobotSendRequest();
        request.setMsgtype("text");
        OapiRobotSendRequest.Text text = new OapiRobotSendRequest.Text();
        text.setContent(message);
        request.setText(text);
        OapiRobotSendRequest.At at = new OapiRobotSendRequest.At();
        // 消息指定@谁
        at.setAtMobiles(Arrays.asList(atMobiles));
        request.setAt(at);

        /*request.setMsgtype("link");
        OapiRobotSendRequest.Link link = new OapiRobotSendRequest.Link();
        link.setMessageUrl("https://www.dingtalk.com/");
        link.setPicUrl("");
        link.setTitle("时代的火车向前开");
        link.setText("这个即将发布的新版本，创始人陈航（花名“无招”）称它为“红树林”。\n" +
                "而在此之前，每当面临重大升级，产品经理们都会取一个应景的代号，这一次，为什么是“红树林");
        request.setLink(link);

        request.setMsgtype("markdown");
        OapiRobotSendRequest.Markdown markdown = new OapiRobotSendRequest.Markdown();
        markdown.setTitle("杭州天气");
        markdown.setText("#### 杭州天气 @156xxxx8827\n" +
                "> 9度，西北风1级，空气良89，相对温度73%\n\n" +
                "> ![screenshot](https://gw.alipayobjects.com/zos/skylark-tools/public/files/84111bbeba74743d2771ed4f062d1f25.png)\n"  +
                "> ###### 10点20分发布 [天气](http://www.thinkpage.cn/) \n");
        request.setMarkdown(markdown);*/
        OapiRobotSendResponse response = client.execute(request);
    }

}
